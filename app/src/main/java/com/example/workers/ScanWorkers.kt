package com.example.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.repository.ScannerRepository
import com.example.domain.math.MytTimeUtils
import java.util.concurrent.TimeUnit

/**
 * LAYER 1: Radar Harga & Pencetus (Every 15 minutes via WorkManager)
 * Deterministic code only, unless Event-Driven Trigger (>4% 1H drop or RSI 1H < 32) fires.
 */
class Layer1RadarWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repo = ScannerRepository(applicationContext)
        val res = repo.runFullRadarScan(
            allowAiCalls = false, // Code-only unless eventTriggered bypasses
            sourceLabel = "Lapisan 1 (15m)"
        )
        return if (res.isSuccess) Result.success() else Result.retry()
    }
}

/**
 * LAYER 2: Analisis Teknikal AI (Hourly at exact :02 MYT on closed 1H candle)
 */
class Layer2HourlyAiWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repo = ScannerRepository(applicationContext)
        val res = repo.runFullRadarScan(
            allowAiCalls = true,
            sourceLabel = "Lapisan 2 (:02 MYT)"
        )
        RadarScheduler.scheduleNextHourlyAt02(applicationContext)
        return if (res.isSuccess) Result.success() else Result.retry()
    }
}

/**
 * LAYER 3: Sinkronisasi Data Global & Makro (00:05, 08:05, 12:05, 16:05, 20:05 MYT)
 */
class Layer3MacroSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repo = ScannerRepository(applicationContext)
        runCatching { repo.runLayer3MacroSync() }
        RadarScheduler.scheduleNextMacroSync(applicationContext)
        return Result.success()
    }
}

object RadarScheduler {

    private const val WORK_LAYER_1 = "lunointel_layer1_radar_15m"
    private const val WORK_LAYER_2 = "lunointel_layer2_hourly_02_myt"
    private const val WORK_LAYER_3 = "lunointel_layer3_macro_4h_myt"

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun scheduleAllLayers(context: Context) {
        runCatching {
            if (!WorkManager.isInitialized()) {
                WorkManager.initialize(
                    context.applicationContext,
                    androidx.work.Configuration.Builder().build()
                )
            }
            scheduleLayer1Periodic(context)
            scheduleNextHourlyAt02(context)
            scheduleNextMacroSync(context)
        }
    }

    private fun scheduleLayer1Periodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<Layer1RadarWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_LAYER_1,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleNextHourlyAt02(context: Context) {
        val delayMs = MytTimeUtils.nextMinute02DelayMs()
        val request = OneTimeWorkRequestBuilder<Layer2HourlyAiWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setConstraints(networkConstraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_LAYER_2,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleNextMacroSync(context: Context) {
        val delayMs = MytTimeUtils.nextMacroSyncDelayMs()
        val request = OneTimeWorkRequestBuilder<Layer3MacroSyncWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setConstraints(networkConstraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_LAYER_3,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
