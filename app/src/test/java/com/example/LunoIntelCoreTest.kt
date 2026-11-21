package com.example

import com.example.domain.math.AiJsonSanitizer
import com.example.domain.math.MytTimeUtils
import com.example.domain.math.TechnicalIndicators
import com.example.domain.model.CandlePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class LunoIntelCoreTest {

    @Test
    fun `spread percentage matches exact ask-bid formula`() {
        val spread = TechnicalIndicators.calculateSpreadPct(bid = 99.0, ask = 100.0)
        assertEquals(1.0, spread, 0.0001)
    }

    @Test
    fun `anti-fomo gate blocks AI call when RSI 1H exceeds 58`() {
        val now = 1_700_000_000_000L
        val risingCandles = (0 until 30).map { idx ->
            val base = 100.0 + idx * 2.5
            CandlePoint(
                timestampMs = now - ((30 - idx) * 3_600_000L),
                open = base - 1.0,
                high = base + 1.0,
                low = base - 1.5,
                close = base,
                volume = 100.0
            )
        }
        val telemetry = TechnicalIndicators.buildTelemetry(
            pair = "SOLMYR",
            lastPriceMyr = 172.5,
            bidMyr = 172.0,
            askMyr = 172.5,
            volume24hBase = 5000.0,
            globalPriceMyr = 171.0,
            raw1hCandles = risingCandles,
            raw4hCandles = risingCandles,
            candleSource = "Test",
            nowMs = now
        )
        assertTrue(telemetry.rsi1h > 58.0)
        assertTrue(telemetry.antiFomoTriggered)
        assertFalse(telemetry.hardVetoPassed)
    }

    @Test
    fun `sanitizer strips think block and enforces Kunci Mutlak when any check is false`() {
        val rawAi = """
            <think>
            Checking RSI and spread carefully...
            </think>
            ```json
            {
              "pair": "XBTMYR",
              "timestamp_myt": "2026-11-21 14:02:00 MYT",
              "global_bias": "BULLISH",
              "setup": "SETUP_A_PULLBACK",
              "scores": {
                "support_30": 26,
                "bounce_25": 22,
                "macro_20": 18,
                "liquidity_15": 14,
                "rr_path_10": 9
              },
              "confidence_score": 89,
              "checks": {
                "data_valid": true,
                "vetoes_clear": true,
                "setup_confirmed": true,
                "ev_targets_ok": false,
                "sl_and_maker_ok": true,
                "score_ge_min": true
              },
              "action": "STRONG_WATCH_BUY",
              "levels": {
                "entry_zone_myr": "RM 400,000 - RM 402,000",
                "stop_loss_myr": 395000.0,
                "tp1_myr": 412000.0,
                "tp2_myr": 420000.0,
                "invalidation_myr": 393000.0
              },
              "probabilities": {
                "upside_pct_chance": 65,
                "sideways_pct_chance": 25,
                "downside_pct_chance": 10
              },
              "summary_bm": "Struktur sokongan kukuh tetapi sasaran EV tidak melepasi yuran maker."
            }
            ```
        """.trimIndent()

        val result = AiJsonSanitizer.parseAndEnforceAbsoluteLock(
            rawAiContent = rawAi,
            expectedPair = "XBTMYR",
            minScoreThreshold = 82,
            modelUsed = "deepseek-v4.1-flash-1017-mod",
            isFallbackUsed = false
        )

        assertEquals(89, result.confidenceScore)
        // Because ev_targets_ok = false, Kunci Mutlak MUST force HOLD and null levels!
        assertEquals("HOLD", result.action)
        assertNull(result.levels)
    }

    @Test
    fun `sanitizer keeps STRONG_WATCH_BUY and levels when all 6 checks pass`() {
        val rawAi = """
            {
              "pair": "ETHMYR",
              "timestamp_myt": "2026-11-21 14:02:00 MYT",
              "global_bias": "BULLISH",
              "setup": "SETUP_B_OVERSOLD",
              "scores": {
                "support_30": 27,
                "bounce_25": 21,
                "macro_20": 17,
                "liquidity_15": 13,
                "rr_path_10": 8
              },
              "confidence_score": 86,
              "checks": {
                "data_valid": true,
                "vetoes_clear": true,
                "setup_confirmed": true,
                "ev_targets_ok": true,
                "sl_and_maker_ok": true,
                "score_ge_min": true
              },
              "action": "STRONG_WATCH_BUY",
              "levels": {
                "entry_zone_myr": "RM 11,100 - RM 11,250",
                "stop_loss_myr": 10850.0,
                "tp1_myr": 11650.0,
                "tp2_myr": 12000.0,
                "invalidation_myr": 10750.0
              },
              "probabilities": {
                "upside_pct_chance": 68,
                "sideways_pct_chance": 22,
                "downside_pct_chance": 10
              },
              "summary_bm": "Lantunan oversold di sokongan utama dengan kecairan maker Luno yang baik."
            }
        """.trimIndent()

        val result = AiJsonSanitizer.parseAndEnforceAbsoluteLock(
            rawAiContent = rawAi,
            expectedPair = "ETHMYR",
            minScoreThreshold = 82,
            modelUsed = "deepseek-v4.1-flash-1017-mod",
            isFallbackUsed = false
        )

        assertEquals("STRONG_WATCH_BUY", result.action)
        assertNotNull(result.levels)
        assertEquals(86, result.confidenceScore)
    }

    @Test
    fun `strict night mode raises threshold from 82 to 88 between 02 and 07 MYT`() {
        val nightTime = ZonedDateTime.of(2026, 11, 21, 3, 30, 0, 0, MytTimeUtils.MYT_ZONE)
        val dayTime = ZonedDateTime.of(2026, 11, 21, 14, 30, 0, 0, MytTimeUtils.MYT_ZONE)

        assertEquals(88, MytTimeUtils.getActiveScoreThreshold(true, nightTime))
        assertEquals(82, MytTimeUtils.getActiveScoreThreshold(true, dayTime))
        assertEquals(82, MytTimeUtils.getActiveScoreThreshold(false, nightTime))
    }

    @Test
    fun `cooldown blocks duplicate notifications within 2 hours`() {
        val now = 1_700_000_000_000L
        assertTrue(MytTimeUtils.isNotificationCooldownActive(now - 3_600_000L, now))
        assertFalse(MytTimeUtils.isNotificationCooldownActive(now - 7_200_001L, now))
    }

    @Test
    fun `custom API endpoint normalizes base domains and full paths correctly`() {
        val client = com.example.data.remote.StatelessAiClient()
        assertEquals(
            "https://api.deepseek.com/v1/chat/completions",
            client.normalizeEndpointUrl("https://api.deepseek.com")
        )
        assertEquals(
            "https://openrouter.ai/api/v1/chat/completions",
            client.normalizeEndpointUrl("https://openrouter.ai/api/v1/")
        )
        assertEquals(
            "https://my.custom-proxy.com/v1/chat/completions",
            client.normalizeEndpointUrl("https://my.custom-proxy.com/v1/chat/completions")
        )
    }
}
