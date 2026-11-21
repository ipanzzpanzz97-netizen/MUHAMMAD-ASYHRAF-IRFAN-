package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coin_scans")
data class CoinScanEntity(
    @PrimaryKey val pair: String,
    val telemetryJson: String,
    val analysisJson: String,
    val confidenceScore: Int,
    val badgeName: String,
    val statusLabelBm: String,
    val lastNotifiedMs: Long = 0L,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "macro_snapshot")
data class MacroSnapshotEntity(
    @PrimaryKey val id: Int = 1,
    val btcMyr: Double,
    val btcUsd: Double,
    val btcChange24hPct: Double,
    val fearGreedValue: Int,
    val fearGreedLabel: String,
    val macroContext: String,
    val lastSyncMyt: String,
    val updatedAtMs: Long
)

@Entity(tableName = "error_logs")
data class ErrorLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMyt: String,
    val epochMs: Long,
    val level: String, // INFO, FALLBACK, TRIGGER, ERROR
    val pair: String,
    val message: String
)

@Entity(tableName = "signal_history")
data class SignalHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pair: String,
    val baseSymbol: String,
    val triggeredAtMyt: String,
    val triggeredAtMs: Long,
    val confidenceScore: Int,
    val setup: String,
    val entryZoneMyr: String,
    val entryRefPriceMyr: Double,
    val stopLossMyr: Double,
    val tp1Myr: Double,
    val tp2Myr: Double,
    val invalidationMyr: Double,
    val summaryBm: String,
    val outcomeStatus: String = "OPEN" // OPEN, WIN_TP1, WIN_TP2, LOSS_SL
)
