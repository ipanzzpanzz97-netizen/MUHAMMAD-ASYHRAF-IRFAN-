package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM coin_scans ORDER BY confidenceScore DESC")
    fun observeAllCoinScans(): Flow<List<CoinScanEntity>>

    @Query("SELECT * FROM coin_scans WHERE pair = :pair LIMIT 1")
    suspend fun getCoinScan(pair: String): CoinScanEntity?

    @Query("SELECT * FROM coin_scans WHERE pair = :pair LIMIT 1")
    fun observeCoinScan(pair: String): Flow<CoinScanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCoinScan(entity: CoinScanEntity)

    @Query("UPDATE coin_scans SET lastNotifiedMs = :notifiedMs WHERE pair = :pair")
    suspend fun updateLastNotified(pair: String, notifiedMs: Long)

    @Query("SELECT * FROM macro_snapshot WHERE id = 1 LIMIT 1")
    fun observeMacroSnapshot(): Flow<MacroSnapshotEntity?>

    @Query("SELECT * FROM macro_snapshot WHERE id = 1 LIMIT 1")
    suspend fun getMacroSnapshot(): MacroSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMacroSnapshot(entity: MacroSnapshotEntity)

    @Query("SELECT * FROM error_logs ORDER BY epochMs DESC LIMIT 100")
    fun observeErrorLogs(): Flow<List<ErrorLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertErrorLog(log: ErrorLogEntity)

    @Query("DELETE FROM error_logs")
    suspend fun clearErrorLogs()

    @Query("DELETE FROM error_logs WHERE id NOT IN (SELECT id FROM error_logs ORDER BY epochMs DESC LIMIT 100)")
    suspend fun trimOldLogs()
}
