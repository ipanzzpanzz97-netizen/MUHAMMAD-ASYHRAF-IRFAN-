package com.example.data.remote

import com.example.data.local.ScannerSettings
import com.example.domain.math.AiJsonSanitizer
import com.example.domain.math.MytTimeUtils
import com.example.domain.model.AiAnalysisResult
import com.example.domain.model.GlobalMacroState
import com.example.domain.model.MarketTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class AiHttpException(val statusCode: Int, message: String) : Exception(message)

/**
 * Strictly STATELESS AI analysis client (temperature = 0.05, no chat history).
 * Supports fully custom API keys, custom endpoints (auto-normalized), custom models,
 * and custom auth headers, with 15s timeout and automatic fallback on HTTP 503 / timeout.
 */
class StatelessAiClient {

    private val aiHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun testConnection(
        endpoint: String,
        apiKey: String,
        model: String,
        authHeaderName: String = "Authorization"
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (apiKey.isBlank()) {
                throw IllegalArgumentException("Sila masukkan API Key terlebih dahulu.")
            }
            if (endpoint.isBlank() || (!endpoint.startsWith("http://") && !endpoint.startsWith("https://"))) {
                throw IllegalArgumentException("URL Endpoint mesti bermula dengan https://")
            }
            if (model.isBlank()) {
                throw IllegalArgumentException("Sila masukkan nama model.")
            }
            val reply = executeCompletionCall(
                endpoint = endpoint,
                apiKey = apiKey,
                model = model,
                authHeaderName = authHeaderName,
                systemPrompt = "Balas dengan satu perkataan sahaja: OK",
                userPayloadJson = "{\"ping\":true}"
            )
            "Berjaya berhubung dengan $model (${reply.trim().take(24)})"
        }
    }

    suspend fun analyzeMarketStateless(
        telemetry: MarketTelemetry,
        macro: GlobalMacroState,
        settings: ScannerSettings,
        minScoreThreshold: Int,
        onLogEvent: suspend (level: String, pair: String, message: String) -> Unit
    ): AiAnalysisResult = withContext(Dispatchers.IO) {
        val systemPrompt = buildSystemPrompt(minScoreThreshold)
        val marketPayloadJson = buildMarketPayloadJson(telemetry, macro, minScoreThreshold)

        // Primary call
        try {
            val rawPrimary = executeCompletionCall(
                endpoint = settings.primaryEndpoint,
                apiKey = settings.primaryApiKey,
                model = settings.primaryModel,
                authHeaderName = settings.primaryAuthHeader,
                systemPrompt = systemPrompt,
                userPayloadJson = marketPayloadJson
            )
            return@withContext AiJsonSanitizer.parseAndEnforceAbsoluteLock(
                rawAiContent = rawPrimary,
                expectedPair = telemetry.pair,
                minScoreThreshold = minScoreThreshold,
                modelUsed = settings.primaryModel,
                isFallbackUsed = false
            )
        } catch (primaryErr: Throwable) {
            val isTimeoutOr503 = primaryErr is SocketTimeoutException ||
                primaryErr is InterruptedIOException ||
                (primaryErr is AiHttpException && (primaryErr.statusCode == 503 || primaryErr.statusCode >= 500 || primaryErr.statusCode == 429))

            val reason = when {
                primaryErr is SocketTimeoutException || primaryErr is InterruptedIOException ->
                    "Timeout > 15s pada ${settings.primaryModel}"
                primaryErr is AiHttpException ->
                    "HTTP ${primaryErr.statusCode} pada ${settings.primaryModel}"
                else ->
                    "Ralat ${settings.primaryModel}: ${primaryErr.message ?: "Tidak diketahui"}"
            }

            if (isTimeoutOr503) {
                onLogEvent(
                    "FALLBACK",
                    telemetry.pair,
                    "Auto-Fallback aktif ($reason) -> beralih ke ${settings.fallbackModel}"
                )
            } else {
                onLogEvent(
                    "ERROR",
                    telemetry.pair,
                    "$reason -> mencuba model sandaran ${settings.fallbackModel}"
                )
            }

            val fallbackKey = settings.fallbackApiKey.ifBlank { settings.primaryApiKey }
            if (fallbackKey.isBlank()) {
                throw IllegalStateException("Tiada kunci API untuk model sandaran ${settings.fallbackModel}")
            }

            val rawFallback = executeCompletionCall(
                endpoint = settings.fallbackEndpoint,
                apiKey = fallbackKey,
                model = settings.fallbackModel,
                authHeaderName = settings.fallbackAuthHeader,
                systemPrompt = systemPrompt,
                userPayloadJson = marketPayloadJson
            )

            return@withContext AiJsonSanitizer.parseAndEnforceAbsoluteLock(
                rawAiContent = rawFallback,
                expectedPair = telemetry.pair,
                minScoreThreshold = minScoreThreshold,
                modelUsed = "${settings.fallbackModel} (Fallback)",
                isFallbackUsed = true
            )
        }
    }

    /**
     * Normalizes custom OpenAI-compatible base URLs so users can paste either:
     * - https://api.deepseek.com
     * - https://api.deepseek.com/v1
     * - https://api.deepseek.com/v1/chat/completions
     */
    internal fun normalizeEndpointUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim().trimEnd('/')
        return when {
            trimmed.endsWith("/chat/completions", ignoreCase = true) -> trimmed
            trimmed.endsWith("/completions", ignoreCase = true) -> trimmed
            trimmed.endsWith("/v1", ignoreCase = true) ||
                trimmed.endsWith("/openai", ignoreCase = true) -> "$trimmed/chat/completions"
            trimmed.contains("generativelanguage.googleapis.com", ignoreCase = true) &&
                !trimmed.contains("/openai", ignoreCase = true) -> "$trimmed/v1beta/openai/chat/completions"
            else -> "$trimmed/v1/chat/completions"
        }
    }

    private fun executeCompletionCall(
        endpoint: String,
        apiKey: String,
        model: String,
        authHeaderName: String,
        systemPrompt: String,
        userPayloadJson: String
    ): String {
        val normalizedUrl = normalizeEndpointUrl(endpoint)
        val headerName = authHeaderName.trim().ifBlank { "Authorization" }
        val headerValue = if (headerName.equals("Authorization", ignoreCase = true)) {
            if (apiKey.startsWith("Bearer ", ignoreCase = true)) apiKey else "Bearer $apiKey"
        } else {
            apiKey
        }

        val bodyJson = JSONObject().apply {
            put("model", model.trim())
            put("temperature", 0.1)
            put("max_tokens", 350)
            put(
                "messages",
                JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", systemPrompt))
                    put(JSONObject().put("role", "user").put("content", userPayloadJson))
                }
            )
        }

        val request = Request.Builder()
            .url(normalizedUrl)
            .header(headerName, headerValue)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        aiHttpClient.newCall(request).execute().use { response ->
            val respBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AiHttpException(
                    response.code,
                    "AI HTTP ${response.code}: ${respBody.take(180)}"
                )
            }
            val root = JSONObject(respBody)
            val choices = root.optJSONArray("choices")
                ?: throw IllegalStateException("Tiada pilihan dalam respons AI")
            val content = choices.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content", "")
                .orEmpty()
            if (content.isBlank()) {
                throw IllegalStateException("Kandungan respons AI kosong")
            }
            return content
        }
    }

    private fun buildSystemPrompt(minScoreThreshold: Int): String = """
Anda ialah enjin penilaian kebarangkalian lantunan kripto Luno MYR dengan prinsip SIFAR HALUSINASI dan pemeliharaan modal.
PERATURAN MUTLAK:
1. Guna HANYA data angka yang dibekalkan dalam payload JSON pengguna. Jangan reka harga atau indikator baru.
2. Pulangkan SATU objek JSON sah sahaja (tiada teks luar JSON, tiada markdown).
3. Skema wajib:
{
  "pair": "XBTMYR",
  "timestamp_myt": "YYYY-MM-DD HH:mm:ss MYT",
  "global_bias": "BULLISH | NEUTRAL | BEARISH",
  "setup": "SETUP_A_PULLBACK | SETUP_B_OVERSOLD | NONE",
  "scores": {
    "support_30": 0..30,
    "bounce_25": 0..25,
    "macro_20": 0..20,
    "liquidity_15": 0..15,
    "rr_path_10": 0..10
  },
  "confidence_score": 0..100,
  "checks": {
    "data_valid": true|false,
    "vetoes_clear": true|false,
    "setup_confirmed": true|false,
    "ev_targets_ok": true|false,
    "sl_and_maker_ok": true|false,
    "score_ge_min": true|false
  },
  "action": "STRONG_WATCH_BUY | HOLD | RISK_WARNING",
  "levels": {
    "entry_zone_myr": "RM xxx - RM yyy",
    "stop_loss_myr": number,
    "tp1_myr": number,
    "tp2_myr": number,
    "invalidation_myr": number
  } | null,
  "probabilities": {
    "upside_pct_chance": 0..100,
    "sideways_pct_chance": 0..100,
    "downside_pct_chance": 0..100
  },
  "summary_bm": "Ringkasan padat Bahasa Melayu maksimum 25 patah perkataan."
}
4. confidence_score MESTI tepat = support_30 + bounce_25 + macro_20 + liquidity_15 + rr_path_10.
5. score_ge_min hanya true jika confidence_score >= $minScoreThreshold.
6. KUNCI MUTLAK: Jika mana-mana 1 daripada 6 checks bernilai false, WAJIB tetapkan action = "HOLD" dan levels = null.
7. Pertimbangkan kos yuran Luno Maker 0.35% (0.70% pusingan penuh) untuk ev_targets_ok & sl_and_maker_ok.
    """.trimIndent()

    private fun buildMarketPayloadJson(
        t: MarketTelemetry,
        macro: GlobalMacroState,
        minScoreThreshold: Int
    ): String {
        val payload = JSONObject().apply {
            put("pair", t.pair)
            put("timestamp_myt", MytTimeUtils.formatIsoMyt(t.scannedAtEpochMs))
            put("min_score_required", minScoreThreshold)
            put("luno_last_myr", t.lastPriceMyr)
            put("luno_bid_myr", t.bidMyr)
            put("luno_ask_myr", t.askMyr)
            put("spread_pct", "%.3f".format(t.spreadPct).toDouble())
            put("myr_premium_pct", "%.3f".format(t.myrPremiumPct).toDouble())
            put("price_change_1h_pct", "%.3f".format(t.priceChange1hPct).toDouble())
            put("price_change_24h_pct", "%.3f".format(t.change24hPct).toDouble())
            put("rsi14_1h", "%.2f".format(t.rsi1h).toDouble())
            put("rsi14_4h", "%.2f".format(t.rsi4h).toDouble())
            put("ema20_1h", "%.2f".format(t.ema20_1h).toDouble())
            put("ema50_1h", "%.2f".format(t.ema50_1h).toDouble())
            put("ema200_1h", "%.2f".format(t.ema200_1h).toDouble())
            put("ema20_4h", "%.2f".format(t.ema20_4h).toDouble())
            put("ema50_4h", "%.2f".format(t.ema50_4h).toDouble())
            put("atr14_1h_pct", "%.2f".format(t.atr1hPct).toDouble())
            put("event_triggered", t.eventTriggered)
            put("hard_veto_passed", t.hardVetoPassed)
            put("btc_myr", macro.btcMyr)
            put("btc_24h_change_pct", "%.2f".format(macro.btcChange24hPct).toDouble())
            put("fear_and_greed_index", macro.fearGreedValue)
            put("fear_and_greed_label", macro.fearGreedLabel)
            put("recent_closed_1h_closes", JSONArray(t.closed1hCloses.takeLast(12)))
        }
        return payload.toString()
    }
}
