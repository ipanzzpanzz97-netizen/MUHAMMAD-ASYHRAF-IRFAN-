package com.example.domain.math

import com.example.domain.model.AiAnalysisResult
import com.example.domain.model.CandlePoint
import com.example.domain.model.MarketTelemetry
import com.example.domain.model.OpportunityBadge
import com.example.domain.model.ScenarioProbabilities
import com.example.domain.model.ScoreBreakdown
import com.example.domain.model.SixChecks
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 100% Deterministic quantitative calculation engine (Cost: $0 AI).
 * Computes spread_pct, Wilder RSI(14), EMA 20/50/200, ATR 1H %, myr_premium_pct,
 * and enforces hard vetoes / Anti-FOMO / Event-Driven Trigger rules.
 */
object TechnicalIndicators {

    const val ANTI_FOMO_RSI_THRESHOLD = 58.0
    const val OVERBOUGHT_VETO_RSI = 70.0
    const val EVENT_DROP_1H_THRESHOLD = -4.0
    const val EVENT_OVERSOLD_RSI_THRESHOLD = 32.0
    const val MAX_ACCEPTABLE_SPREAD_PCT = 2.50

    /**
     * Exact formula: spread_pct = ((ask - bid) / ask) * 100
     */
    fun calculateSpreadPct(bid: Double, ask: Double): Double {
        if (ask <= 0.0 || bid <= 0.0 || ask < bid) return 99.99
        return ((ask - bid) / ask) * 100.0
    }

    /**
     * Exact MYR Premium vs global reference converted to MYR
     */
    fun calculateMyrPremiumPct(lunoPriceMyr: Double, globalPriceMyr: Double): Double {
        if (globalPriceMyr <= 0.0 || lunoPriceMyr <= 0.0) return 0.0
        return ((lunoPriceMyr - globalPriceMyr) / globalPriceMyr) * 100.0
    }

    /**
     * Filters out the currently open (unclosed) candle so indicators never repaint.
     */
    fun filterClosedCandles(
        candles: List<CandlePoint>,
        durationMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): List<CandlePoint> {
        if (candles.isEmpty()) return emptyList()
        val sorted = candles.sortedBy { it.timestampMs }
        return sorted.filter { it.timestampMs + durationMs <= nowMs }
    }

    /**
     * Wilder's RSI(14) on closed candles.
     */
    fun calculateRsi(closes: List<Double>, period: Int = 14): Double {
        if (closes.size <= period) return 50.0
        var gainSum = 0.0
        var lossSum = 0.0
        for (i in 1..period) {
            val diff = closes[i] - closes[i - 1]
            if (diff >= 0) gainSum += diff else lossSum += -diff
        }
        var avgGain = gainSum / period
        var avgLoss = lossSum / period

        for (i in (period + 1) until closes.size) {
            val diff = closes[i] - closes[i - 1]
            val gain = if (diff > 0) diff else 0.0
            val loss = if (diff < 0) -diff else 0.0
            avgGain = ((avgGain * (period - 1)) + gain) / period
            avgLoss = ((avgLoss * (period - 1)) + loss) / period
        }

        if (avgLoss == 0.0) return if (avgGain == 0.0) 50.0 else 100.0
        val rs = avgGain / avgLoss
        return (100.0 - (100.0 / (1.0 + rs))).coerceIn(0.0, 100.0)
    }

    /**
     * Standard Exponential Moving Average (EMA).
     */
    fun calculateEma(closes: List<Double>, period: Int): Double {
        if (closes.isEmpty()) return 0.0
        if (closes.size < period) return closes.average()
        val k = 2.0 / (period + 1.0)
        var ema = closes.take(period).average()
        for (i in period until closes.size) {
            ema = (closes[i] - ema) * k + ema
        }
        return ema
    }

    /**
     * Average True Range (ATR 14) expressed as percentage of latest close price.
     */
    fun calculateAtrPct(candles: List<CandlePoint>, period: Int = 14): Double {
        if (candles.size <= period) return 0.0
        val trueRanges = ArrayList<Double>(candles.size - 1)
        for (i in 1 until candles.size) {
            val cur = candles[i]
            val prevClose = candles[i - 1].close
            val tr = max(
                cur.high - cur.low,
                max(abs(cur.high - prevClose), abs(cur.low - prevClose))
            )
            trueRanges.add(tr)
        }
        if (trueRanges.size < period) return 0.0
        var atr = trueRanges.take(period).average()
        for (i in period until trueRanges.size) {
            atr = ((atr * (period - 1)) + trueRanges[i]) / period
        }
        val latestClose = candles.last().close
        if (latestClose <= 0.0) return 0.0
        return (atr / latestClose) * 100.0
    }

    fun calculatePriceChange1hPct(closed1hCandles: List<CandlePoint>, currentLastPrice: Double): Double {
        if (closed1hCandles.isEmpty()) return 0.0
        val referenceClose = closed1hCandles.last().open
        val latest = if (currentLastPrice > 0.0) currentLastPrice else closed1hCandles.last().close
        if (referenceClose <= 0.0) return 0.0
        return ((latest - referenceClose) / referenceClose) * 100.0
    }

    fun buildTelemetry(
        pair: String,
        lastPriceMyr: Double,
        bidMyr: Double,
        askMyr: Double,
        volume24hBase: Double,
        globalPriceMyr: Double,
        raw1hCandles: List<CandlePoint>,
        raw4hCandles: List<CandlePoint>,
        candleSource: String,
        nowMs: Long = System.currentTimeMillis()
    ): MarketTelemetry {
        val closed1h = filterClosedCandles(raw1hCandles, 3_600_000L, nowMs)
            .ifEmpty { raw1hCandles.dropLast(1).ifEmpty { raw1hCandles } }
        val closed4h = filterClosedCandles(raw4hCandles, 14_400_000L, nowMs)
            .ifEmpty { raw4hCandles.dropLast(1).ifEmpty { raw4hCandles } }

        val closes1h = closed1h.map { it.close }
        val closes4h = closed4h.map { it.close }

        val spreadPct = calculateSpreadPct(bidMyr, askMyr)
        val rsi1h = calculateRsi(closes1h, 14)
        val rsi4h = calculateRsi(closes4h, 14)
        val ema20_1h = calculateEma(closes1h, 20)
        val ema50_1h = calculateEma(closes1h, 50)
        val ema200_1h = calculateEma(closes1h, 200)
        val ema20_4h = calculateEma(closes4h, 20)
        val ema50_4h = calculateEma(closes4h, 50)
        val atr1hPct = calculateAtrPct(closed1h, 14)
        val priceChange1hPct = calculatePriceChange1hPct(closed1h, lastPriceMyr)
        val change24hPct = if (closes1h.size >= 24 && closes1h[closes1h.size - 24] > 0.0) {
            ((lastPriceMyr - closes1h[closes1h.size - 24]) / closes1h[closes1h.size - 24]) * 100.0
        } else 0.0
        val myrPremiumPct = calculateMyrPremiumPct(lastPriceMyr, globalPriceMyr)

        val vetoReasons = mutableListOf<String>()
        val dataValid = lastPriceMyr > 0.0 && askMyr >= bidMyr && bidMyr > 0.0 && closes1h.size >= 20
        if (!dataValid) vetoReasons.add("Data harga/lilin tidak mencukupi")

        if (spreadPct > MAX_ACCEPTABLE_SPREAD_PCT) {
            vetoReasons.add("Spread Luno terlalu lebar (${"%.2f".format(spreadPct)}%)")
        }
        if (myrPremiumPct > 4.5) {
            vetoReasons.add("Premium MYR terlalu tinggi (+${"%.2f".format(myrPremiumPct)}%)")
        }

        val eventTriggered = (priceChange1hPct <= EVENT_DROP_1H_THRESHOLD) ||
            (rsi1h < EVENT_OVERSOLD_RSI_THRESHOLD && dataValid)

        val antiFomoTriggered = rsi1h > ANTI_FOMO_RSI_THRESHOLD
        if (antiFomoTriggered) {
            vetoReasons.add(
                if (rsi1h > OVERBOUGHT_VETO_RSI) {
                    "Overbought ekstrem (RSI 1H ${"%.1f".format(rsi1h)} > 70)"
                } else {
                    "Anti-FOMO aktif (RSI 1H ${"%.1f".format(rsi1h)} > 58)"
                }
            )
        }

        val flatMarketSkipped = !eventTriggered &&
            !antiFomoTriggered &&
            atr1hPct < 0.35 &&
            abs(priceChange1hPct) < 0.40 &&
            rsi1h > 44.0
        if (flatMarketSkipped) {
            vetoReasons.add("Pasaran mendatar (ATR ${"%.2f".format(atr1hPct)}% · jimat token AI)")
        }

        val hardVetoPassed = dataValid &&
            spreadPct <= MAX_ACCEPTABLE_SPREAD_PCT &&
            myrPremiumPct <= 4.5 &&
            !antiFomoTriggered &&
            !flatMarketSkipped

        return MarketTelemetry(
            pair = pair,
            lastPriceMyr = lastPriceMyr,
            bidMyr = bidMyr,
            askMyr = askMyr,
            spreadPct = spreadPct,
            volume24hBase = volume24hBase,
            change24hPct = change24hPct,
            priceChange1hPct = priceChange1hPct,
            rsi1h = rsi1h,
            rsi4h = rsi4h,
            ema20_1h = ema20_1h,
            ema50_1h = ema50_1h,
            ema200_1h = ema200_1h,
            ema20_4h = ema20_4h,
            ema50_4h = ema50_4h,
            atr1hPct = atr1hPct,
            globalPriceMyr = globalPriceMyr,
            myrPremiumPct = myrPremiumPct,
            closed1hCloses = closes1h.takeLast(24),
            hardVetoPassed = hardVetoPassed,
            antiFomoTriggered = antiFomoTriggered,
            flatMarketSkipped = flatMarketSkipped,
            eventTriggered = eventTriggered,
            vetoReasons = vetoReasons,
            candleSource = candleSource,
            scannedAtMyt = MytTimeUtils.formatDisplayMyt(nowMs),
            scannedAtEpochMs = nowMs
        )
    }

    /**
     * Deterministic HOLD/VETO card when hard_veto_passed == false (Zero AI cost).
     */
    fun buildDeterministicHoldResult(
        telemetry: MarketTelemetry,
        fearGreedValue: Int,
        minScoreThreshold: Int
    ): AiAnalysisResult {
        val dataValid = telemetry.lastPriceMyr > 0.0 && telemetry.closed1hCloses.size >= 20
        val vetoesClear = !telemetry.antiFomoTriggered && telemetry.spreadPct <= MAX_ACCEPTABLE_SPREAD_PCT
        val isRiskWarning = telemetry.rsi1h > OVERBOUGHT_VETO_RSI || telemetry.spreadPct > MAX_ACCEPTABLE_SPREAD_PCT

        val liquidityScore = when {
            telemetry.spreadPct <= 0.35 -> 14
            telemetry.spreadPct <= 0.75 -> 11
            telemetry.spreadPct <= 1.5 -> 7
            else -> 3
        }
        val macroScore = when {
            fearGreedValue in 20..45 -> 14
            fearGreedValue in 46..65 -> 11
            else -> 7
        }
        val supportScore = if (telemetry.lastPriceMyr >= telemetry.ema200_1h && telemetry.ema200_1h > 0.0) 14 else 9
        val bounceScore = if (telemetry.rsi1h <= 58.0) 10 else 4
        val rrScore = 3

        val breakdown = ScoreBreakdown(
            support30 = supportScore,
            bounce25 = bounceScore,
            macro20 = macroScore,
            liquidity15 = liquidityScore,
            rrPath10 = rrScore
        )

        val summary = when {
            telemetry.rsi1h > OVERBOUGHT_VETO_RSI ->
                "VETO Overbought: RSI 1H tinggi (${"%.1f".format(telemetry.rsi1h)}). Elak kejar harga untuk lindungi modal."
            telemetry.antiFomoTriggered ->
                "Anti-FOMO HOLD: RSI 1H ${"%.1f".format(telemetry.rsi1h)} melebihi 58. Panggilan AI dijimatkan."
            telemetry.spreadPct > MAX_ACCEPTABLE_SPREAD_PCT ->
                "VETO Kecairan: Spread Luno ${"%.2f".format(telemetry.spreadPct)}% terlalu lebar. Tunggu buku pesanan pulih."
            telemetry.flatMarketSkipped ->
                "Pasaran mendatar (ATR ${"%.2f".format(telemetry.atr1hPct)}%). Kekal HOLD sehingga lantunan jelas."
            else ->
                "Syarat keselamatan belum dipenuhi. Status kekal HOLD demi pemeliharaan modal."
        }

        val upside = if (telemetry.rsi1h > OVERBOUGHT_VETO_RSI) 20 else 30
        val sideways = if (telemetry.flatMarketSkipped) 55 else 45
        val downside = (100 - upside - sideways).coerceAtLeast(10)

        return AiAnalysisResult(
            pair = telemetry.pair,
            timestampMyt = MytTimeUtils.formatIsoMyt(telemetry.scannedAtEpochMs),
            globalBias = if (fearGreedValue >= 55) "BULLISH" else if (fearGreedValue <= 35) "BEARISH" else "NEUTRAL",
            setup = "NONE",
            scores = breakdown,
            confidenceScore = breakdown.total,
            checks = SixChecks(
                dataValid = dataValid,
                vetoesClear = vetoesClear,
                setupConfirmed = false,
                evTargetsOk = false,
                slAndMakerOk = telemetry.spreadPct <= 1.2,
                scoreGeMin = breakdown.total >= minScoreThreshold
            ),
            action = if (isRiskWarning) "RISK_WARNING" else "HOLD",
            levels = null, // Kunci Mutlak: Tiada paras harga bila checks ada false
            probabilities = ScenarioProbabilities(
                upsidePctChance = upside,
                sidewaysPctChance = sideways,
                downsidePctChance = downside
            ),
            summaryBm = summary,
            modelUsed = "KOD_DETERMINISTIK ($0 AI)",
            isFallbackUsed = false,
            aiCalled = false
        )
    }

    fun classifyBadge(
        telemetry: MarketTelemetry?,
        analysis: AiAnalysisResult?,
        minScoreThreshold: Int
    ): Pair<OpportunityBadge, String> {
        if (telemetry == null || analysis == null) {
            return OpportunityBadge.HOLD to "MENUNGGU"
        }
        val isVetoOrOverbought = analysis.action == "RISK_WARNING" ||
            telemetry.rsi1h > OVERBOUGHT_VETO_RSI ||
            telemetry.spreadPct > MAX_ACCEPTABLE_SPREAD_PCT

        return when {
            isVetoOrOverbought -> OpportunityBadge.VETO to "VETO / RISIKO"
            analysis.action == "STRONG_WATCH_BUY" &&
                analysis.checks.allPassed() &&
                analysis.confidenceScore >= minScoreThreshold ->
                OpportunityBadge.GRADE_A to "PELUANG GRED A"
            analysis.confidenceScore >= 65 && telemetry.hardVetoPassed ->
                OpportunityBadge.WATCH to "PANTAU"
            else -> OpportunityBadge.HOLD to "HOLD"
        }
    }

    fun formatMyr(amount: Double): String {
        return when {
            amount >= 1000.0 -> "RM %,.2f".format(amount)
            amount >= 10.0 -> "RM %.2f".format(amount)
            else -> "RM %.4f".format(amount)
        }
    }

    fun roundTo2(value: Double): Double = (value * 100.0).roundToInt() / 100.0
}
