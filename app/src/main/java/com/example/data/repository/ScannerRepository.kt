package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.CoinScanEntity
import com.example.data.local.ErrorLogEntity
import com.example.data.local.MacroSnapshotEntity
import com.example.data.local.ScannerSettings
import com.example.data.local.SecurePreferencesManager
import com.example.data.notifications.NotificationHelper
import com.example.data.remote.CoinGeckoGlobalPrice
import com.example.data.remote.GlobalMarketApiService
import com.example.data.remote.LunoApiService
import com.example.data.remote.LunoTickerSnapshot
import com.example.data.remote.StatelessAiClient
import com.example.domain.math.MytTimeUtils
import com.example.domain.math.TechnicalIndicators
import com.example.domain.model.AiAnalysisResult
import com.example.domain.model.CoinScanUiModel
import com.example.domain.model.GlobalMacroState
import com.example.domain.model.MarketTelemetry
import com.example.domain.model.OpportunityBadge
import com.example.domain.model.SupportedPair
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ScannerRepository(
    private val context: Context,
    private val db: AppDatabase = AppDatabase.getInstance(context),
    val prefs: SecurePreferencesManager = SecurePreferencesManager(context),
    private val lunoApi: LunoApiService = LunoApiService(),
    private val globalApi: GlobalMarketApiService = GlobalMarketApiService(),
    private val aiClient: StatelessAiClient = StatelessAiClient()
) {
    private val dao = db.scanDao()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val settingsFlow = prefs.settingsFlow

    val macroStateFlow: Flow<GlobalMacroState> = dao.observeMacroSnapshot().map { entity ->
        if (entity == null) {
            GlobalMacroState(
                nextHourlyScanMyt = MytTimeUtils.nextMinute02Label(),
                nextMacroSyncMyt = MytTimeUtils.nextMacroSyncLabel()
            )
        } else {
            GlobalMacroState(
                btcMyr = entity.btcMyr,
                btcUsd = entity.btcUsd,
                btcChange24hPct = entity.btcChange24hPct,
                fearGreedValue = entity.fearGreedValue,
                fearGreedLabel = entity.fearGreedLabel,
                macroContext = entity.macroContext,
                lastMacroSyncMyt = entity.lastSyncMyt,
                nextHourlyScanMyt = MytTimeUtils.nextMinute02Label(),
                nextMacroSyncMyt = MytTimeUtils.nextMacroSyncLabel()
            )
        }
    }

    val coinScansFlow: Flow<List<CoinScanUiModel>> =
        combine(dao.observeAllCoinScans(), prefs.settingsFlow) { entities, settings ->
            val minScore = MytTimeUtils.getActiveScoreThreshold(settings.strictNightModeEnabled)
            val entityMap = entities.associateBy { it.pair }
            SupportedPair.entries.map { pairInfo ->
                val entity = entityMap[pairInfo.lunoPair]
                if (entity == null) {
                    CoinScanUiModel(
                        pairInfo = pairInfo,
                        telemetry = null,
                        analysis = null,
                        badge = OpportunityBadge.HOLD,
                        statusLabelBm = "MENUNGGU IMBASAN"
                    )
                } else {
                    val telemetry = runCatching {
                        json.decodeFromString<MarketTelemetry>(entity.telemetryJson)
                    }.getOrNull()
                    val analysis = runCatching {
                        json.decodeFromString<AiAnalysisResult>(entity.analysisJson)
                    }.getOrNull()
                    val (badge, label) = TechnicalIndicators.classifyBadge(telemetry, analysis, minScore)
                    CoinScanUiModel(
                        pairInfo = pairInfo,
                        telemetry = telemetry,
                        analysis = analysis,
                        badge = badge,
                        statusLabelBm = label
                    )
                }
            }.sortedWith(
                compareByDescending<CoinScanUiModel> { it.sortScore }
                    .thenByDescending { it.change24h }
            )
        }

    fun observeCoinDetail(pair: String): Flow<CoinScanUiModel?> {
        val pairInfo = SupportedPair.fromLunoPair(pair) ?: return kotlinx.coroutines.flow.flowOf(null)
        return combine(dao.observeCoinScan(pairInfo.lunoPair), prefs.settingsFlow) { entity, settings ->
            if (entity == null) {
                CoinScanUiModel(pairInfo, null, null, OpportunityBadge.HOLD, "MENUNGGU")
            } else {
                val minScore = MytTimeUtils.getActiveScoreThreshold(settings.strictNightModeEnabled)
                val telemetry = runCatching {
                    json.decodeFromString<MarketTelemetry>(entity.telemetryJson)
                }.getOrNull()
                val analysis = runCatching {
                    json.decodeFromString<AiAnalysisResult>(entity.analysisJson)
                }.getOrNull()
                val (badge, label) = TechnicalIndicators.classifyBadge(telemetry, analysis, minScore)
                CoinScanUiModel(pairInfo, telemetry, analysis, badge, label)
            }
        }
    }

    val errorLogsFlow: Flow<List<ErrorLogEntity>> = dao.observeErrorLogs()

    suspend fun clearLogs() {
        dao.clearErrorLogs()
    }

    suspend fun testApiConnection(
        endpoint: String,
        apiKey: String,
        model: String,
        authHeaderName: String
    ): Result<String> {
        val result = aiClient.testConnection(
            endpoint = endpoint,
            apiKey = apiKey,
            model = model,
            authHeaderName = authHeaderName
        )
        if (result.isSuccess) {
            logEvent("INFO", "API_TEST", result.getOrNull().orEmpty())
        } else {
            logEvent("ERROR", "API_TEST", result.exceptionOrNull()?.message ?: "Ujian sambungan gagal")
        }
        return result
    }

    suspend fun logEvent(level: String, pair: String, message: String) {
        val now = System.currentTimeMillis()
        dao.insertErrorLog(
            ErrorLogEntity(
                timestampMyt = MytTimeUtils.formatDisplayMyt(now),
                epochMs = now,
                level = level,
                pair = pair,
                message = message
            )
        )
        dao.trimOldLogs()
    }

    /**
     * LAYER 3: Global Macro Sync (08:05, 12:05, 16:05, 20:05, 00:05 MYT)
     */
    suspend fun runLayer3MacroSync(): GlobalMacroState {
        val nowMs = System.currentTimeMillis()
        val fng = runCatching { globalApi.fetchFearAndGreed() }.getOrElse { err ->
            logEvent("ERROR", "GLOBAL", "Gagal tarik Fear & Greed: ${err.message}")
            null
        }
        val geckoPrices = runCatching { globalApi.fetchCoinGeckoGlobalPrices() }.getOrElse { err ->
            logEvent("ERROR", "GLOBAL", "Gagal tarik CoinGecko MYR: ${err.message}")
            emptyMap()
        }
        val lunoBtc = runCatching { lunoApi.fetchSingleTicker("XBTMYR") }.getOrNull()

        val btcGecko = geckoPrices["XBTMYR"]
        val btcMyr = lunoBtc?.lastTrade?.takeIf { it > 0.0 } ?: btcGecko?.myrPrice ?: 0.0
        val btcUsd = btcGecko?.usdPrice ?: 0.0
        val btcChange = btcGecko?.change24hPct ?: 0.0
        val fgVal = fng?.value ?: dao.getMacroSnapshot()?.fearGreedValue ?: 50
        val fgLabel = fng?.let { "${it.value} · ${it.labelBm}" }
            ?: dao.getMacroSnapshot()?.fearGreedLabel ?: "50 · Neutral"

        val isDailyOpen = MytTimeUtils.isDailyOpeningWindow()
        val contextNote = buildString {
            if (isDailyOpen) append("Laporan Pembukaan Harian 08:05 MYT · ")
            append("Sentimen Global: $fgLabel. ")
            append(
                when {
                    btcChange >= 2.0 -> "Aliran BTC kukuh (+${"%.2f".format(btcChange)}%)."
                    btcChange <= -2.0 -> "Tekanan jualan BTC (${"%.2f".format(btcChange)}%)."
                    else -> "Struktur makro BTC stabil (${"%.2f".format(btcChange)}%)."
                }
            )
        }

        val snapshot = MacroSnapshotEntity(
            id = 1,
            btcMyr = btcMyr,
            btcUsd = btcUsd,
            btcChange24hPct = btcChange,
            fearGreedValue = fgVal,
            fearGreedLabel = fgLabel,
            macroContext = contextNote,
            lastSyncMyt = MytTimeUtils.formatDisplayMyt(nowMs),
            updatedAtMs = nowMs
        )
        dao.upsertMacroSnapshot(snapshot)
        logEvent("INFO", "GLOBAL", "Sinkronisasi Makro Lapisan 3 selesai ($fgLabel)")

        return GlobalMacroState(
            btcMyr = btcMyr,
            btcUsd = btcUsd,
            btcChange24hPct = btcChange,
            fearGreedValue = fgVal,
            fearGreedLabel = fgLabel,
            macroContext = contextNote,
            lastMacroSyncMyt = snapshot.lastSyncMyt,
            nextHourlyScanMyt = MytTimeUtils.nextMinute02Label(),
            nextMacroSyncMyt = MytTimeUtils.nextMacroSyncLabel()
        )
    }

    /**
     * LAYER 1 (15m / 5m foreground) + LAYER 2 (:02 MYT / Manual / Event-Driven Trigger)
     *
     * @param allowAiCalls true for Layer 2 (:02 MYT), Manual Scan, or Event-Driven Trigger.
     *                     false for routine Layer 1 radar unless Event-Driven Trigger fires!
     */
    suspend fun runFullRadarScan(
        allowAiCalls: Boolean,
        sourceLabel: String
    ): Result<Unit> = runCatching {
        val settings = prefs.current()
        val nowMs = System.currentTimeMillis()
        val minScore = MytTimeUtils.getActiveScoreThreshold(settings.strictNightModeEnabled)

        // Ensure macro state is fresh if stale (> 30 min) or absent
        val existingMacro = dao.getMacroSnapshot()
        val macroState = if (existingMacro == null || (nowMs - existingMacro.updatedAtMs) > 30 * 60_000L) {
            runLayer3MacroSync()
        } else {
            GlobalMacroState(
                btcMyr = existingMacro.btcMyr,
                btcUsd = existingMacro.btcUsd,
                btcChange24hPct = existingMacro.btcChange24hPct,
                fearGreedValue = existingMacro.fearGreedValue,
                fearGreedLabel = existingMacro.fearGreedLabel,
                macroContext = existingMacro.macroContext,
                lastMacroSyncMyt = existingMacro.lastSyncMyt,
                nextHourlyScanMyt = MytTimeUtils.nextMinute02Label(),
                nextMacroSyncMyt = MytTimeUtils.nextMacroSyncLabel()
            )
        }

        val tickersMap = runCatching { lunoApi.fetchAllTickers() }.getOrElse { err ->
            logEvent("ERROR", "LUNO", "Gagal tarik /api/1/tickers: ${err.message}")
            emptyMap()
        }
        val geckoMap = runCatching { globalApi.fetchCoinGeckoGlobalPrices() }.getOrElse { emptyMap() }

        val hasAiKey = settings.primaryApiKey.isNotBlank() || settings.fallbackApiKey.isNotBlank()
        if (allowAiCalls && !hasAiKey) {
            logEvent("INFO", "AI", "Kunci API AI belum ditetapkan di Tetapan. Imbasan deterministik $0 dijalankan.")
        }

        coroutineScope {
            SupportedPair.entries.map { pairInfo ->
                async {
                    scanSinglePair(
                        pairInfo = pairInfo,
                        cachedTicker = tickersMap[pairInfo.lunoPair],
                        globalPrice = geckoMap[pairInfo.lunoPair],
                        macroState = macroState,
                        settings = settings,
                        minScore = minScore,
                        allowAiCalls = allowAiCalls,
                        hasAiKey = hasAiKey,
                        sourceLabel = sourceLabel
                    )
                }
            }.awaitAll()
        }
    }

    private suspend fun scanSinglePair(
        pairInfo: SupportedPair,
        cachedTicker: LunoTickerSnapshot?,
        globalPrice: CoinGeckoGlobalPrice?,
        macroState: GlobalMacroState,
        settings: ScannerSettings,
        minScore: Int,
        allowAiCalls: Boolean,
        hasAiKey: Boolean,
        sourceLabel: String
    ) {
        val nowMs = System.currentTimeMillis()
        val ticker = cachedTicker ?: runCatching {
            lunoApi.fetchSingleTicker(pairInfo.lunoPair)
        }.getOrElse { err ->
            logEvent("ERROR", pairInfo.lunoPair, "Gagal tarik ticker Luno: ${err.message}")
            return
        }

        val orderBook = lunoApi.fetchOrderBookTop(pairInfo.lunoPair)
        val effectiveBid = orderBook?.bestBid?.takeIf { it > 0.0 } ?: ticker.bid
        val effectiveAsk = orderBook?.bestAsk?.takeIf { it > 0.0 } ?: ticker.ask
        val lastPrice = ticker.lastTrade.takeIf { it > 0.0 } ?: ((effectiveBid + effectiveAsk) / 2.0)

        val globalMyr = globalPrice?.myrPrice?.takeIf { it > 0.0 } ?: lastPrice
        val globalUsd = globalPrice?.usdPrice?.takeIf { it > 0.0 } ?: (lastPrice / 4.45)
        val myrPerUsd = if (globalUsd > 0.0 && lastPrice > 0.0) lastPrice / globalUsd else 4.45

        // Fetch 1H and 4H candles (Luno primary -> Binance scaled to MYR fallback if Luno requires auth)
        var candleSource = "Luno Exchange 1H/4H"
        val candles1h = runCatching {
            lunoApi.fetchLunoCandles(
                pair = pairInfo.lunoPair,
                sinceMs = nowMs - (240L * 3_600_000L),
                durationSeconds = 3600,
                lunoKeyId = settings.lunoKeyId,
                lunoKeySecret = settings.lunoKeySecret
            )
        }.getOrElse {
            candleSource = "Binance OHLC (Skala MYR)"
            runCatching {
                globalApi.fetchBinanceCandlesScaledToMyr(pairInfo.binanceSymbol, "1h", myrPerUsd, 250)
            }.getOrElse { emptyList() }
        }

        val candles4h = runCatching {
            lunoApi.fetchLunoCandles(
                pair = pairInfo.lunoPair,
                sinceMs = nowMs - (240L * 14_400_000L),
                durationSeconds = 14400,
                lunoKeyId = settings.lunoKeyId,
                lunoKeySecret = settings.lunoKeySecret
            )
        }.getOrElse {
            runCatching {
                globalApi.fetchBinanceCandlesScaledToMyr(pairInfo.binanceSymbol, "4h", myrPerUsd, 150)
            }.getOrElse { emptyList() }
        }

        val telemetry = TechnicalIndicators.buildTelemetry(
            pair = pairInfo.lunoPair,
            lastPriceMyr = lastPrice,
            bidMyr = effectiveBid,
            askMyr = effectiveAsk,
            volume24hBase = ticker.volume24h,
            globalPriceMyr = globalMyr,
            raw1hCandles = candles1h,
            raw4hCandles = candles4h,
            candleSource = candleSource,
            nowMs = nowMs
        )

        if (telemetry.eventTriggered) {
            logEvent(
                "TRIGGER",
                pairInfo.lunoPair,
                "Pencetus Kecemasan ($sourceLabel): 1J ${"%.2f".format(telemetry.priceChange1hPct)}%, RSI 1H ${"%.1f".format(telemetry.rsi1h)}"
            )
        }

        // Anti-FOMO / Flat Market / Hard Veto Gate: NEVER call AI if hard_veto_passed == false
        val shouldInvokeAi = hasAiKey &&
            telemetry.hardVetoPassed &&
            (allowAiCalls || telemetry.eventTriggered)

        val analysis = if (!shouldInvokeAi) {
            TechnicalIndicators.buildDeterministicHoldResult(
                telemetry = telemetry,
                fearGreedValue = macroState.fearGreedValue,
                minScoreThreshold = minScore
            )
        } else {
            runCatching {
                aiClient.analyzeMarketStateless(
                    telemetry = telemetry,
                    macro = macroState,
                    settings = settings,
                    minScoreThreshold = minScore,
                    onLogEvent = { level, pair, msg -> logEvent(level, pair, msg) }
                )
            }.getOrElse { err ->
                logEvent("ERROR", pairInfo.lunoPair, "Analisis AI gagal: ${err.message}")
                TechnicalIndicators.buildDeterministicHoldResult(
                    telemetry = telemetry,
                    fearGreedValue = macroState.fearGreedValue,
                    minScoreThreshold = minScore
                )
            }
        }

        val existing = dao.getCoinScan(pairInfo.lunoPair)
        val prevNotifiedMs = existing?.lastNotifiedMs ?: 0L
        val (badge, statusLabel) = TechnicalIndicators.classifyBadge(telemetry, analysis, minScore)

        val entity = CoinScanEntity(
            pair = pairInfo.lunoPair,
            telemetryJson = json.encodeToString(telemetry),
            analysisJson = json.encodeToString(analysis),
            confidenceScore = analysis.confidenceScore,
            badgeName = badge.name,
            statusLabelBm = statusLabel,
            lastNotifiedMs = prevNotifiedMs,
            updatedAtMs = nowMs
        )
        dao.upsertCoinScan(entity)

        // Dispatch notification if Grade A OR Emergency Event Trigger, respecting 2-hour cooldown & Strict Night Mode
        val canNotify = settings.notificationsEnabled &&
            !MytTimeUtils.isNotificationCooldownActive(prevNotifiedMs, nowMs)

        if (canNotify && (badge == OpportunityBadge.GRADE_A || telemetry.eventTriggered)) {
            val isEmergency = telemetry.eventTriggered && badge != OpportunityBadge.GRADE_A
            val title = if (badge == OpportunityBadge.GRADE_A) {
                "Peluang Gred A: ${pairInfo.baseSymbol}/MYR (Skor ${analysis.confidenceScore})"
            } else {
                "Pencetus Kecemasan: ${pairInfo.baseSymbol}/MYR"
            }
            val body = "${TechnicalIndicators.formatMyr(telemetry.lastPriceMyr)} · RSI 1H ${"%.1f".format(telemetry.rsi1h)} · ${analysis.summaryBm}"
            NotificationHelper.sendRadarAlert(
                context = context,
                pair = pairInfo.lunoPair,
                title = title,
                body = body,
                isEmergency = isEmergency
            )
            dao.updateLastNotified(pairInfo.lunoPair, nowMs)
            logEvent("INFO", pairInfo.lunoPair, "Notifikasi dihantar ($title)")
        }
    }
}
