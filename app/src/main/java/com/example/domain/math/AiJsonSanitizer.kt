package com.example.domain.math

import com.example.domain.model.AiAnalysisResult
import com.example.domain.model.ScenarioProbabilities
import com.example.domain.model.ScoreBreakdown
import com.example.domain.model.SixChecks
import com.example.domain.model.TradeLevels
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Strips <think>...</think> blocks and markdown code fences before parsing AI JSON,
 * and strictly enforces KUNCI MUTLAK (Absolute Lock).
 */
object AiJsonSanitizer {

    private val THINK_BLOCK_REGEX = Regex("(?is)<think>.*?</think>")
    private val CODE_FENCE_REGEX = Regex("(?is)```(?:json)?\\s*|```")
    private val jsonParser = Json { ignoreUnknownKeys = true }

    fun cleanRawResponse(rawText: String): String {
        val withoutThink = rawText.replace(THINK_BLOCK_REGEX, "")
        val withoutFences = withoutThink.replace(CODE_FENCE_REGEX, "").trim()
        val firstBrace = withoutFences.indexOf('{')
        val lastBrace = withoutFences.lastIndexOf('}')
        if (firstBrace == -1 || lastBrace == -1 || lastBrace <= firstBrace) {
            throw IllegalArgumentException("Respons AI tidak mengandungi objek JSON yang sah.")
        }
        return withoutFences.substring(firstBrace, lastBrace + 1)
    }

    private fun JsonObject.optInt(key: String, default: Int = 0): Int =
        this[key]?.let { runCatching { it.jsonPrimitive.intOrNull }.getOrNull() } ?: default

    private fun JsonObject.optDouble(key: String, default: Double = 0.0): Double =
        this[key]?.let { runCatching { it.jsonPrimitive.doubleOrNull }.getOrNull() } ?: default

    private fun JsonObject.optBoolean(key: String, default: Boolean = false): Boolean =
        this[key]?.let { runCatching { it.jsonPrimitive.booleanOrNull }.getOrNull() } ?: default

    private fun JsonObject.optString(key: String, default: String = ""): String =
        this[key]?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() } ?: default

    private fun JsonObject.optObj(key: String): JsonObject? =
        this[key]?.let { runCatching { it.jsonObject }.getOrNull() }

    fun parseAndEnforceAbsoluteLock(
        rawAiContent: String,
        expectedPair: String,
        minScoreThreshold: Int,
        modelUsed: String,
        isFallbackUsed: Boolean
    ): AiAnalysisResult {
        val cleanedJson = cleanRawResponse(rawAiContent)
        val root = jsonParser.parseToJsonElement(cleanedJson).jsonObject

        val scoresObj = root.optObj("scores") ?: JsonObject(emptyMap())
        val scores = ScoreBreakdown(
            support30 = scoresObj.optInt("support_30", 0).coerceIn(0, 30),
            bounce25 = scoresObj.optInt("bounce_25", 0).coerceIn(0, 25),
            macro20 = scoresObj.optInt("macro_20", 0).coerceIn(0, 20),
            liquidity15 = scoresObj.optInt("liquidity_15", 0).coerceIn(0, 15),
            rrPath10 = scoresObj.optInt("rr_path_10", 0).coerceIn(0, 10)
        )
        val computedScore = scores.total

        val checksObj = root.optObj("checks") ?: JsonObject(emptyMap())
        val checks = SixChecks(
            dataValid = checksObj.optBoolean("data_valid", false),
            vetoesClear = checksObj.optBoolean("vetoes_clear", false),
            setupConfirmed = checksObj.optBoolean("setup_confirmed", false),
            evTargetsOk = checksObj.optBoolean("ev_targets_ok", false),
            slAndMakerOk = checksObj.optBoolean("sl_and_maker_ok", false),
            scoreGeMin = checksObj.optBoolean("score_ge_min", false) && (computedScore >= minScoreThreshold)
        )

        val rawAction = root.optString("action", "HOLD").uppercase()
        val allChecksPass = checks.allPassed()

        // KUNCI MUTLAK: If any 1 of 6 checks is false -> MUST force action = "HOLD" (unless RISK_WARNING) and levels = null
        val enforcedAction = when {
            !allChecksPass && rawAction == "RISK_WARNING" -> "RISK_WARNING"
            !allChecksPass -> "HOLD"
            rawAction in setOf("STRONG_WATCH_BUY", "HOLD", "RISK_WARNING") -> rawAction
            else -> "HOLD"
        }

        val parsedLevels: TradeLevels? = if (!allChecksPass || enforcedAction == "HOLD") {
            null
        } else {
            val lvl = root.optObj("levels")
            if (lvl == null) {
                null
            } else {
                val sl = lvl.optDouble("stop_loss_myr", 0.0)
                val tp1 = lvl.optDouble("tp1_myr", 0.0)
                val tp2 = lvl.optDouble("tp2_myr", 0.0)
                val inv = lvl.optDouble("invalidation_myr", 0.0)
                val zone = lvl.optString("entry_zone_myr", "").trim()
                if (zone.isEmpty() || sl <= 0.0 || tp1 <= 0.0) null
                else TradeLevels(
                    entryZoneMyr = zone,
                    stopLossMyr = sl,
                    tp1Myr = tp1,
                    tp2Myr = if (tp2 > 0.0) tp2 else tp1,
                    invalidationMyr = if (inv > 0.0) inv else sl
                )
            }
        }

        val finalAction = if (enforcedAction == "STRONG_WATCH_BUY" && parsedLevels == null) {
            "HOLD"
        } else {
            enforcedAction
        }

        val probObj = root.optObj("probabilities") ?: JsonObject(emptyMap())
        val up = probObj.optInt("upside_pct_chance", 30).coerceIn(0, 100)
        val side = probObj.optInt("sideways_pct_chance", 45).coerceIn(0, 100)
        val down = probObj.optInt("downside_pct_chance", (100 - up - side).coerceAtLeast(0)).coerceIn(0, 100)

        val rawSummary = root.optString("summary_bm", "Analisis selesai. Pantau paras sokongan MYR.")
            .trim()
        val cappedSummary = rawSummary.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(25)
            .joinToString(" ")

        val rawBias = root.optString("global_bias", "NEUTRAL").uppercase()
        val globalBias = if (rawBias in setOf("BULLISH", "NEUTRAL", "BEARISH")) rawBias else "NEUTRAL"

        val rawSetup = root.optString("setup", "NONE").uppercase()
        val setup = if (rawSetup in setOf("SETUP_A_PULLBACK", "SETUP_B_OVERSOLD", "NONE")) rawSetup else "NONE"

        return AiAnalysisResult(
            pair = root.optString("pair", expectedPair).ifBlank { expectedPair },
            timestampMyt = root.optString("timestamp_myt", MytTimeUtils.formatIsoMyt()),
            globalBias = globalBias,
            setup = setup,
            scores = scores,
            confidenceScore = computedScore,
            checks = checks,
            action = finalAction,
            levels = parsedLevels,
            probabilities = ScenarioProbabilities(
                upsidePctChance = up,
                sidewaysPctChance = side,
                downsidePctChance = down
            ),
            summaryBm = cappedSummary,
            modelUsed = modelUsed,
            isFallbackUsed = isFallbackUsed,
            aiCalled = true
        )
    }
}
