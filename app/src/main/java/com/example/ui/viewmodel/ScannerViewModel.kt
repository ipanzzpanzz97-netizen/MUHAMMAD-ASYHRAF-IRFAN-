package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ErrorLogEntity
import com.example.data.local.ScannerSettings
import com.example.data.repository.ScannerRepository
import com.example.domain.math.MytTimeUtils
import com.example.domain.model.CoinScanUiModel
import com.example.domain.model.GlobalMacroState
import com.example.domain.model.OpportunityBadge
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RadarFilter(val labelBm: String) {
    SEMUA("Semua"),
    GRED_A("Gred A"),
    PANTAU("Pantau"),
    HOLD_VETO("HOLD / VETO")
}

data class RadarUiState(
    val coins: List<CoinScanUiModel> = emptyList(),
    val macro: GlobalMacroState = GlobalMacroState(),
    val settings: ScannerSettings = ScannerSettings(),
    val selectedFilter: RadarFilter = RadarFilter.SEMUA,
    val isScanning: Boolean = false,
    val cooldownSecondsLeft: Int = 0,
    val activeMinScore: Int = MytTimeUtils.STANDARD_MIN_SCORE,
    val isStrictNightWindowActive: Boolean = false,
    val statusMessage: String? = null
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScannerRepository(application)

    private val _selectedFilter = MutableStateFlow(RadarFilter.SEMUA)
    private val _isScanning = MutableStateFlow(false)
    private val _cooldownSeconds = MutableStateFlow(0)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _isTestingApi = MutableStateFlow(false)
    private val _apiTestResult = MutableStateFlow<Pair<Boolean, String>?>(null)

    val isTestingApi: StateFlow<Boolean> = _isTestingApi.asStateFlow()
    val apiTestResult: StateFlow<Pair<Boolean, String>?> = _apiTestResult.asStateFlow()

    val errorLogs: StateFlow<List<ErrorLogEntity>> = repository.errorLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settingsFlow: StateFlow<ScannerSettings> = repository.settingsFlow

    val uiState: StateFlow<RadarUiState> = combine(
        repository.coinScansFlow,
        repository.macroStateFlow,
        repository.settingsFlow,
        _selectedFilter,
        _isScanning,
        _cooldownSeconds,
        _statusMessage
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allCoins = values[0] as List<CoinScanUiModel>
        val macro = values[1] as GlobalMacroState
        val settings = values[2] as ScannerSettings
        val filter = values[3] as RadarFilter
        val scanning = values[4] as Boolean
        val cooldown = values[5] as Int
        val msg = values[6] as String?

        val filtered = when (filter) {
            RadarFilter.SEMUA -> allCoins
            RadarFilter.GRED_A -> allCoins.filter { it.badge == OpportunityBadge.GRADE_A }
            RadarFilter.PANTAU -> allCoins.filter { it.badge == OpportunityBadge.WATCH }
            RadarFilter.HOLD_VETO -> allCoins.filter {
                it.badge == OpportunityBadge.HOLD || it.badge == OpportunityBadge.VETO
            }
        }

        val nowMyt = MytTimeUtils.nowMyt()
        val nightWindow = MytTimeUtils.isStrictNightWindow(nowMyt)
        val minScore = MytTimeUtils.getActiveScoreThreshold(settings.strictNightModeEnabled, nowMyt)

        RadarUiState(
            coins = filtered,
            macro = macro,
            settings = settings,
            selectedFilter = filter,
            isScanning = scanning,
            cooldownSecondsLeft = cooldown,
            activeMinScore = minScore,
            isStrictNightWindowActive = nightWindow && settings.strictNightModeEnabled,
            statusMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RadarUiState()
    )

    init {
        restoreCooldownTimer()
        startForeground5MinLoop()
    }

    private fun restoreCooldownTimer() {
        val lastMs = repository.prefs.getLastManualScanEpochMs()
        val elapsedSec = ((System.currentTimeMillis() - lastMs) / 1000L).toInt()
        if (lastMs > 0L && elapsedSec in 0 until MANUAL_COOLDOWN_SEC) {
            startCooldownCountdown(MANUAL_COOLDOWN_SEC - elapsedSec)
        }
    }

    private fun startForeground5MinLoop() {
        viewModelScope.launch {
            // Immediate Layer 1+3 refresh on app open (AI only if event-triggered or key present on first load)
            performScan(allowAi = true, sourceLabel = "Pembukaan Aplikasi")
            while (isActive) {
                delay(5L * 60L * 1000L) // Every 5 minutes while app is open
                performScan(allowAi = false, sourceLabel = "Radar Latar Hadapan (5m)")
            }
        }
    }

    fun setFilter(filter: RadarFilter) {
        _selectedFilter.value = filter
    }

    fun triggerManualScan() {
        if (_isScanning.value || _cooldownSeconds.value > 0) return
        val nowMs = System.currentTimeMillis()
        repository.prefs.setLastManualScanEpochMs(nowMs)
        startCooldownCountdown(MANUAL_COOLDOWN_SEC)
        viewModelScope.launch {
            performScan(allowAi = true, sourceLabel = "Imbasan Manual")
        }
    }

    private suspend fun performScan(allowAi: Boolean, sourceLabel: String) {
        if (_isScanning.value) return
        _isScanning.value = true
        _statusMessage.value = "Mengimbas Luno MYR & data global..."
        val result = repository.runFullRadarScan(allowAiCalls = allowAi, sourceLabel = sourceLabel)
        _isScanning.value = false
        _statusMessage.value = if (result.isSuccess) {
            "Imbasan selesai (${MytTimeUtils.formatDisplayMyt()})"
        } else {
            "Ralat imbasan: ${result.exceptionOrNull()?.message ?: "Semak sambungan"}"
        }
    }

    private fun startCooldownCountdown(seconds: Int) {
        viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                _cooldownSeconds.value = remaining
                delay(1000L)
                remaining--
            }
            _cooldownSeconds.value = 0
        }
    }

    fun observeCoinDetail(pair: String): Flow<CoinScanUiModel?> =
        repository.observeCoinDetail(pair)

    fun saveSettings(updated: ScannerSettings) {
        repository.prefs.saveSettings(updated)
        viewModelScope.launch {
            repository.logEvent("INFO", "TETAPAN", "Tetapan keselamatan & model AI dikemas kini.")
        }
    }

    fun testCustomApi(
        endpoint: String,
        apiKey: String,
        model: String,
        authHeaderName: String
    ) {
        if (_isTestingApi.value) return
        viewModelScope.launch {
            _isTestingApi.value = true
            _apiTestResult.value = null
            val res = repository.testApiConnection(
                endpoint = endpoint,
                apiKey = apiKey,
                model = model,
                authHeaderName = authHeaderName
            )
            _isTestingApi.value = false
            _apiTestResult.value = if (res.isSuccess) {
                true to (res.getOrNull() ?: "Sambungan berjaya")
            } else {
                false to (res.exceptionOrNull()?.message ?: "Gagal berhubung ke pelayan API")
            }
        }
    }

    fun clearApiTestResult() {
        _apiTestResult.value = null
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun dismissStatusMessage() {
        _statusMessage.value = null
    }

    companion object {
        const val MANUAL_COOLDOWN_SEC = 30
    }
}
