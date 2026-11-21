package com.example.domain.math

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object MytTimeUtils {
    val MYT_ZONE: ZoneId = ZoneId.of("Asia/Kuala_Lumpur")
    private val DISPLAY_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM HH:mm 'MYT'")
    private val ISO_MYT_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'MYT'")
    private val SHORT_TIME_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm 'MYT'")

    const val COOLDOWN_MS: Long = 2L * 60L * 60L * 1000L // Minimum 2 hours per pair
    const val STANDARD_MIN_SCORE: Int = 82
    const val STRICT_NIGHT_MIN_SCORE: Int = 88

    fun nowMyt(): ZonedDateTime = ZonedDateTime.now(MYT_ZONE)

    fun formatDisplayMyt(epochMs: Long = System.currentTimeMillis()): String {
        val zdt = Instant.ofEpochMilli(epochMs).atZone(MYT_ZONE)
        return DISPLAY_FORMATTER.format(zdt)
    }

    fun formatIsoMyt(epochMs: Long = System.currentTimeMillis()): String {
        val zdt = Instant.ofEpochMilli(epochMs).atZone(MYT_ZONE)
        return ISO_MYT_FORMATTER.format(zdt)
    }

    /**
     * Mod Malam Ketat: 02:00 - 07:00 MYT (thin MYR orderbook liquidity)
     */
    fun isStrictNightWindow(now: ZonedDateTime = nowMyt()): Boolean {
        val hour = now.hour
        return hour in 2..6
    }

    fun getActiveScoreThreshold(
        strictNightModeEnabled: Boolean,
        now: ZonedDateTime = nowMyt()
    ): Int {
        return if (strictNightModeEnabled && isStrictNightWindow(now)) {
            STRICT_NIGHT_MIN_SCORE
        } else {
            STANDARD_MIN_SCORE
        }
    }

    /**
     * Layer 2: Next :02 MYT hourly mark
     */
    fun nextMinute02Zoned(now: ZonedDateTime = nowMyt()): ZonedDateTime {
        val candidate = now.withMinute(2).withSecond(0).withNano(0)
        return if (candidate.isAfter(now)) candidate else candidate.plusHours(1)
    }

    fun nextMinute02DelayMs(now: ZonedDateTime = nowMyt()): Long {
        val next = nextMinute02Zoned(now)
        return (next.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()).coerceAtLeast(1000L)
    }

    fun nextMinute02Label(now: ZonedDateTime = nowMyt()): String {
        return SHORT_TIME_FORMATTER.format(nextMinute02Zoned(now))
    }

    /**
     * Layer 3: Every 4 hours at 00:05, 08:05, 12:05, 16:05, 20:05 MYT
     * (Also supports 04:05 if inside midnight->08:05 gap, prioritizing exact spec hours 00, 08, 12, 16, 20)
     */
    private val MACRO_SYNC_HOURS = listOf(0, 8, 12, 16, 20)

    fun nextMacroSyncZoned(now: ZonedDateTime = nowMyt()): ZonedDateTime {
        for (h in MACRO_SYNC_HOURS) {
            val candidate = now.withHour(h).withMinute(5).withSecond(0).withNano(0)
            if (candidate.isAfter(now)) return candidate
        }
        return now.plusDays(1).withHour(0).withMinute(5).withSecond(0).withNano(0)
    }

    fun nextMacroSyncDelayMs(now: ZonedDateTime = nowMyt()): Long {
        val next = nextMacroSyncZoned(now)
        return (next.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()).coerceAtLeast(1000L)
    }

    fun nextMacroSyncLabel(now: ZonedDateTime = nowMyt()): String {
        return SHORT_TIME_FORMATTER.format(nextMacroSyncZoned(now))
    }

    fun isDailyOpeningWindow(now: ZonedDateTime = nowMyt()): Boolean {
        return now.hour == 8 && now.minute in 0..20
    }

    fun isNotificationCooldownActive(lastNotifiedMs: Long, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (lastNotifiedMs <= 0L) return false
        return (nowMs - lastNotifiedMs) < COOLDOWN_MS
    }
}
