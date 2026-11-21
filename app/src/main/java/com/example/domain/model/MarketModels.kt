package com.example.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CandlePoint(
    val timestampMs: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

@Serializable
data class MarketTelemetry(
    val pair: String,
    val lastPriceMyr: Double,
    val bidMyr: Double,
    val askMyr: Double,
    val spreadPct: Double,
    val volume24hBase: Double,
    val change24hPct: Double,
    val priceChange1hPct: Double,
    val rsi1h: Double,
    val rsi4h: Double,
    val ema20_1h: Double,
    val ema50_1h: Double,
    val ema200_1h: Double,
    val ema20_4h: Double,
    val ema50_4h: Double,
    val atr1hPct: Double,
    val globalPriceMyr: Double,
    val myrPremiumPct: Double,
    val closed1hCloses: List<Double>,
    val hardVetoPassed: Boolean,
    val antiFomoTriggered: Boolean,
    val flatMarketSkipped: Boolean,
    val eventTriggered: Boolean,
    val vetoReasons: List<String>,
    val candleSource: String,
    val scannedAtMyt: String,
    val scannedAtEpochMs: Long
)

@Serializable
data class ScoreBreakdown(
    @SerialName("support_30") val support30: Int = 0,
    @SerialName("bounce_25") val bounce25: Int = 0,
    @SerialName("macro_20") val macro20: Int = 0,
    @SerialName("liquidity_15") val liquidity15: Int = 0,
    @SerialName("rr_path_10") val rrPath10: Int = 0
) {
    val total: Int
        get() = (support30 + bounce25 + macro20 + liquidity15 + rrPath10).coerceIn(0, 100)
}

@Serializable
data class SixChecks(
    @SerialName("data_valid") val dataValid: Boolean = false,
    @SerialName("vetoes_clear") val vetoesClear: Boolean = false,
    @SerialName("setup_confirmed") val setupConfirmed: Boolean = false,
    @SerialName("ev_targets_ok") val evTargetsOk: Boolean = false,
    @SerialName("sl_and_maker_ok") val slAndMakerOk: Boolean = false,
    @SerialName("score_ge_min") val scoreGeMin: Boolean = false
) {
    fun allPassed(): Boolean =
        dataValid && vetoesClear && setupConfirmed && evTargetsOk && slAndMakerOk && scoreGeMin
}

@Serializable
data class TradeLevels(
    @SerialName("entry_zone_myr") val entryZoneMyr: String = "",
    @SerialName("stop_loss_myr") val stopLossMyr: Double = 0.0,
    @SerialName("tp1_myr") val tp1Myr: Double = 0.0,
    @SerialName("tp2_myr") val tp2Myr: Double = 0.0,
    @SerialName("invalidation_myr") val invalidationMyr: Double = 0.0
)

@Serializable
data class ScenarioProbabilities(
    @SerialName("upside_pct_chance") val upsidePctChance: Int = 0,
    @SerialName("sideways_pct_chance") val sidewaysPctChance: Int = 0,
    @SerialName("downside_pct_chance") val downsidePctChance: Int = 0
)

@Serializable
data class AiAnalysisResult(
    val pair: String,
    @SerialName("timestamp_myt") val timestampMyt: String,
    @SerialName("global_bias") val globalBias: String = "NEUTRAL",
    val setup: String = "NONE",
    val scores: ScoreBreakdown = ScoreBreakdown(),
    @SerialName("confidence_score") val confidenceScore: Int = 0,
    val checks: SixChecks = SixChecks(),
    val action: String = "HOLD",
    val levels: TradeLevels? = null,
    val probabilities: ScenarioProbabilities = ScenarioProbabilities(),
    @SerialName("summary_bm") val summaryBm: String = "",
    val modelUsed: String = "DETERMINISTIC_CODE",
    val isFallbackUsed: Boolean = false,
    val aiCalled: Boolean = false
)

enum class OpportunityBadge {
    GRADE_A,     // Hijau >= min threshold (82 or 88 night mode) & all 6 checks true
    WATCH,       // Kuning: Pantau (score 60..81 or setup developing)
    HOLD,        // Kelabu: HOLD (mendatar / RSI > 58 Anti-FOMO / checks false)
    VETO         // Merah: VETO / Overbought / Risk Warning
}

data class GlobalMacroState(
    val btcMyr: Double = 0.0,
    val btcUsd: Double = 0.0,
    val btcChange24hPct: Double = 0.0,
    val fearGreedValue: Int = 50,
    val fearGreedLabel: String = "Neutral",
    val macroContext: String = "Menunggu sinkronisasi data global...",
    val lastMacroSyncMyt: String = "--",
    val nextHourlyScanMyt: String = "--:02 MYT",
    val nextMacroSyncMyt: String = "--:05 MYT"
)

data class CoinScanUiModel(
    val pairInfo: SupportedPair,
    val telemetry: MarketTelemetry?,
    val analysis: AiAnalysisResult?,
    val badge: OpportunityBadge,
    val statusLabelBm: String
) {
    val sortScore: Int
        get() = analysis?.confidenceScore ?: 0

    val change24h: Double
        get() = telemetry?.change24hPct ?: 0.0
}
