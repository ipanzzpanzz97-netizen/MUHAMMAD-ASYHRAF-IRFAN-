package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ApiProviderPreset(
    val label: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val defaultFallbackModel: String = "gemini-4-argon",
    val defaultFallbackUrl: String = ""
) {
    DATTIO_AI(
        label = "Dattio AI",
        defaultBaseUrl = "https://api.dattio.ai/v1/chat/completions",
        defaultModel = "deepseek-v4.1-flash-1017-mod",
        defaultFallbackModel = "gemini-4-argon",
        defaultFallbackUrl = "https://api.dattio.ai/v1/chat/completions"
    ),
    DEEPSEEK(
        label = "DeepSeek",
        defaultBaseUrl = "https://api.deepseek.com/v1/chat/completions",
        defaultModel = "deepseek-v4.1-flash-1017-mod"
    ),
    GEMINI(
        label = "Gemini",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
        defaultModel = "gemini-4-argon"
    ),
    OPENROUTER(
        label = "OpenRouter",
        defaultBaseUrl = "https://openrouter.ai/api/v1/chat/completions",
        defaultModel = "deepseek/deepseek-chat"
    ),
    OPENAI(
        label = "OpenAI",
        defaultBaseUrl = "https://api.openai.com/v1/chat/completions",
        defaultModel = "gpt-4o-mini"
    ),
    CUSTOM(
        label = "Custom",
        defaultBaseUrl = "",
        defaultModel = ""
    )
}

data class ScannerSettings(
    val primaryProvider: String = ApiProviderPreset.DATTIO_AI.name,
    val primaryApiKey: String = "",
    val primaryModel: String = DEFAULT_PRIMARY_MODEL,
    val primaryEndpoint: String = DEFAULT_PRIMARY_ENDPOINT,
    val primaryAuthHeader: String = "Authorization",
    val fallbackProvider: String = ApiProviderPreset.GEMINI.name,
    val fallbackApiKey: String = "",
    val fallbackModel: String = DEFAULT_FALLBACK_MODEL,
    val fallbackEndpoint: String = DEFAULT_FALLBACK_ENDPOINT,
    val fallbackAuthHeader: String = "Authorization",
    val lunoKeyId: String = "",
    val lunoKeySecret: String = "",
    val strictNightModeEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val isHardwareEncrypted: Boolean = true
) {
    companion object {
        const val DEFAULT_PRIMARY_MODEL = "deepseek-v4.1-flash-1017-mod"
        const val DEFAULT_PRIMARY_ENDPOINT = "https://api.dattio.ai/v1/chat/completions"
        const val DEFAULT_FALLBACK_MODEL = "gemini-4-argon"
        const val DEFAULT_FALLBACK_ENDPOINT = "https://api.dattio.ai/v1/chat/completions"
    }
}

class SecurePreferencesManager(context: Context) {

    private val appContext = context.applicationContext

    private val configPrefs: SharedPreferences =
        appContext.getSharedPreferences("lunointel_config_prefs", Context.MODE_PRIVATE)

    private var hardwareEncrypted = true

    private val secretPrefs: SharedPreferences = run {
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                "lunointel_encrypted_secrets",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Throwable) {
            hardwareEncrypted = false
            appContext.getSharedPreferences("lunointel_secrets_fallback", Context.MODE_PRIVATE)
        }
    }

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<ScannerSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): ScannerSettings {
        return ScannerSettings(
            primaryProvider = configPrefs.getString(KEY_PRIMARY_PROVIDER, ApiProviderPreset.DATTIO_AI.name)
                ?: ApiProviderPreset.DATTIO_AI.name,
            primaryApiKey = secretPrefs.getString(KEY_PRIMARY_API, "") ?: "",
            primaryModel = configPrefs.getString(KEY_PRIMARY_MODEL, ScannerSettings.DEFAULT_PRIMARY_MODEL)
                ?: ScannerSettings.DEFAULT_PRIMARY_MODEL,
            primaryEndpoint = configPrefs.getString(KEY_PRIMARY_ENDPOINT, ScannerSettings.DEFAULT_PRIMARY_ENDPOINT)
                ?: ScannerSettings.DEFAULT_PRIMARY_ENDPOINT,
            primaryAuthHeader = configPrefs.getString(KEY_PRIMARY_AUTH_HEADER, "Authorization")
                ?: "Authorization",
            fallbackProvider = configPrefs.getString(KEY_FALLBACK_PROVIDER, ApiProviderPreset.GEMINI.name)
                ?: ApiProviderPreset.GEMINI.name,
            fallbackApiKey = secretPrefs.getString(KEY_FALLBACK_API, "") ?: "",
            fallbackModel = configPrefs.getString(KEY_FALLBACK_MODEL, ScannerSettings.DEFAULT_FALLBACK_MODEL)
                ?: ScannerSettings.DEFAULT_FALLBACK_MODEL,
            fallbackEndpoint = configPrefs.getString(KEY_FALLBACK_ENDPOINT, ScannerSettings.DEFAULT_FALLBACK_ENDPOINT)
                ?: ScannerSettings.DEFAULT_FALLBACK_ENDPOINT,
            fallbackAuthHeader = configPrefs.getString(KEY_FALLBACK_AUTH_HEADER, "Authorization")
                ?: "Authorization",
            lunoKeyId = secretPrefs.getString(KEY_LUNO_ID, "") ?: "",
            lunoKeySecret = secretPrefs.getString(KEY_LUNO_SECRET, "") ?: "",
            strictNightModeEnabled = configPrefs.getBoolean(KEY_STRICT_NIGHT, true),
            notificationsEnabled = configPrefs.getBoolean(KEY_NOTIFICATIONS, true),
            isHardwareEncrypted = hardwareEncrypted
        )
    }

    fun saveSettings(updated: ScannerSettings) {
        secretPrefs.edit()
            .putString(KEY_PRIMARY_API, updated.primaryApiKey.trim())
            .putString(KEY_FALLBACK_API, updated.fallbackApiKey.trim())
            .putString(KEY_LUNO_ID, updated.lunoKeyId.trim())
            .putString(KEY_LUNO_SECRET, updated.lunoKeySecret.trim())
            .apply()

        configPrefs.edit()
            .putString(KEY_PRIMARY_PROVIDER, updated.primaryProvider)
            .putString(KEY_PRIMARY_MODEL, updated.primaryModel.trim().ifBlank { ScannerSettings.DEFAULT_PRIMARY_MODEL })
            .putString(KEY_PRIMARY_ENDPOINT, updated.primaryEndpoint.trim().ifBlank { ScannerSettings.DEFAULT_PRIMARY_ENDPOINT })
            .putString(KEY_PRIMARY_AUTH_HEADER, updated.primaryAuthHeader.trim().ifBlank { "Authorization" })
            .putString(KEY_FALLBACK_PROVIDER, updated.fallbackProvider)
            .putString(KEY_FALLBACK_MODEL, updated.fallbackModel.trim().ifBlank { ScannerSettings.DEFAULT_FALLBACK_MODEL })
            .putString(KEY_FALLBACK_ENDPOINT, updated.fallbackEndpoint.trim().ifBlank { ScannerSettings.DEFAULT_FALLBACK_ENDPOINT })
            .putString(KEY_FALLBACK_AUTH_HEADER, updated.fallbackAuthHeader.trim().ifBlank { "Authorization" })
            .putBoolean(KEY_STRICT_NIGHT, updated.strictNightModeEnabled)
            .putBoolean(KEY_NOTIFICATIONS, updated.notificationsEnabled)
            .apply()

        _settingsFlow.value = loadSettings()
    }

    fun getLastManualScanEpochMs(): Long = configPrefs.getLong(KEY_LAST_MANUAL_SCAN, 0L)

    fun setLastManualScanEpochMs(epochMs: Long) {
        configPrefs.edit().putLong(KEY_LAST_MANUAL_SCAN, epochMs).apply()
    }

    fun current(): ScannerSettings = _settingsFlow.value

    companion object {
        private const val KEY_PRIMARY_PROVIDER = "primary_provider"
        private const val KEY_PRIMARY_API = "primary_ai_api_key"
        private const val KEY_PRIMARY_MODEL = "primary_model"
        private const val KEY_PRIMARY_ENDPOINT = "primary_endpoint"
        private const val KEY_PRIMARY_AUTH_HEADER = "primary_auth_header"
        private const val KEY_FALLBACK_PROVIDER = "fallback_provider"
        private const val KEY_FALLBACK_API = "fallback_ai_api_key"
        private const val KEY_FALLBACK_MODEL = "fallback_model"
        private const val KEY_FALLBACK_ENDPOINT = "fallback_endpoint"
        private const val KEY_FALLBACK_AUTH_HEADER = "fallback_auth_header"
        private const val KEY_LUNO_ID = "luno_api_key_id"
        private const val KEY_LUNO_SECRET = "luno_api_secret"
        private const val KEY_STRICT_NIGHT = "strict_night_mode"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_LAST_MANUAL_SCAN = "last_manual_scan_ms"
    }
}
