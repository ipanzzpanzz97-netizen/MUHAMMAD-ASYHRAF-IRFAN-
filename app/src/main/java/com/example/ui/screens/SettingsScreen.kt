package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.ApiProviderPreset
import com.example.data.local.ErrorLogEntity
import com.example.data.local.ScannerSettings
import com.example.data.notifications.NotificationHelper
import com.example.ui.theme.AmberBright
import com.example.ui.theme.AmberTint
import com.example.ui.theme.AmberWatch
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonTint
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberCyanTint
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldGradeA
import com.example.ui.theme.EmeraldTint
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: ScannerSettings,
    errorLogs: List<ErrorLogEntity>,
    isTestingApi: Boolean,
    apiTestResult: Pair<Boolean, String>?,
    onSaveSettings: (ScannerSettings) -> Unit,
    onTestApi: (endpoint: String, apiKey: String, model: String, authHeader: String) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var primaryProvider by rememberSaveable { mutableStateOf(settings.primaryProvider) }
    var primaryKey by rememberSaveable { mutableStateOf(settings.primaryApiKey) }
    var primaryModel by rememberSaveable { mutableStateOf(settings.primaryModel) }
    var primaryEndpoint by rememberSaveable { mutableStateOf(settings.primaryEndpoint) }
    var primaryAuthHeader by rememberSaveable { mutableStateOf(settings.primaryAuthHeader) }

    var fallbackProvider by rememberSaveable { mutableStateOf(settings.fallbackProvider) }
    var fallbackKey by rememberSaveable { mutableStateOf(settings.fallbackApiKey) }
    var fallbackModel by rememberSaveable { mutableStateOf(settings.fallbackModel) }
    var fallbackEndpoint by rememberSaveable { mutableStateOf(settings.fallbackEndpoint) }
    var fallbackAuthHeader by rememberSaveable { mutableStateOf(settings.fallbackAuthHeader) }

    var lunoKeyId by rememberSaveable { mutableStateOf(settings.lunoKeyId) }
    var lunoKeySecret by rememberSaveable { mutableStateOf(settings.lunoKeySecret) }
    var strictNight by rememberSaveable { mutableStateOf(settings.strictNightModeEnabled) }
    var notifEnabled by rememberSaveable { mutableStateOf(settings.notificationsEnabled) }

    var showPrimaryKey by remember { mutableStateOf(false) }
    var showFallbackKey by remember { mutableStateOf(false) }
    var showFallbackSection by remember { mutableStateOf(false) }
    var showLunoSection by remember { mutableStateOf(false) }
    var savedBanner by remember { mutableStateOf(false) }
    var hasNotifPerm by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }
    var isIgnoringBattery by remember {
        mutableStateOf(isBatteryOptimizationIgnored(context))
    }

    LaunchedEffect(settings) {
        primaryProvider = settings.primaryProvider
        primaryKey = settings.primaryApiKey
        primaryModel = settings.primaryModel
        primaryEndpoint = settings.primaryEndpoint
        primaryAuthHeader = settings.primaryAuthHeader
        fallbackProvider = settings.fallbackProvider
        fallbackKey = settings.fallbackApiKey
        fallbackModel = settings.fallbackModel
        fallbackEndpoint = settings.fallbackEndpoint
        fallbackAuthHeader = settings.fallbackAuthHeader
        lunoKeyId = settings.lunoKeyId
        lunoKeySecret = settings.lunoKeySecret
        strictNight = settings.strictNightModeEnabled
        notifEnabled = settings.notificationsEnabled
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPerm = granted
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tetapan",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Konfigurasi API tersuai, jadual & log",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Surface(
                        color = ObsidianCard,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = EmeraldBright,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (settings.isHardwareEncrypted) "Terenkripsi" else "Storan Selamat",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Custom Primary API Configuration Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Kunci API Utama (Boleh Diubah Suai)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Pilih pembekal atau pilih 'Custom' untuk masukkan URL Endpoint, Nama Model dan Header anda sendiri.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ApiProviderPreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = primaryProvider == preset.name,
                                    onClick = {
                                        primaryProvider = preset.name
                                        if (preset != ApiProviderPreset.CUSTOM) {
                                            primaryEndpoint = preset.defaultBaseUrl
                                            primaryModel = preset.defaultModel
                                            if (preset == ApiProviderPreset.DATTIO_AI) {
                                                fallbackProvider = ApiProviderPreset.DATTIO_AI.name
                                                fallbackModel = preset.defaultFallbackModel
                                                fallbackEndpoint = preset.defaultFallbackUrl
                                            }
                                        }
                                        savedBanner = false
                                    },
                                    label = { Text(preset.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberCyanTint,
                                        selectedLabelColor = CyberCyanBright,
                                        containerColor = ObsidianSurface,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = primaryProvider == preset.name,
                                        borderColor = ObsidianBorder,
                                        selectedBorderColor = CyberCyan
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = primaryKey,
                            onValueChange = { primaryKey = it; savedBanner = false },
                            label = { Text("API Key") },
                            placeholder = { Text("Masukkan kunci API anda") },
                            visualTransformation = if (showPrimaryKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(
                                    onClick = { showPrimaryKey = !showPrimaryKey },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = if (showPrimaryKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = "Tunjuk atau sembunyi kunci API",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            singleLine = true,
                            colors = textFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("primary_api_key_input")
                        )

                        OutlinedTextField(
                            value = primaryEndpoint,
                            onValueChange = {
                                primaryEndpoint = it
                                primaryProvider = ApiProviderPreset.CUSTOM.name
                                savedBanner = false
                            },
                            label = { Text("Base URL / Endpoint") },
                            placeholder = { Text("https://api.example.com/v1/chat/completions") },
                            supportingText = { Text("Boleh masukkan domain asas (/v1) atau URL penuh", color = TextMuted) },
                            singleLine = true,
                            colors = textFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("primary_endpoint_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = primaryModel,
                                onValueChange = { primaryModel = it; savedBanner = false },
                                label = { Text("Nama Model") },
                                singleLine = true,
                                colors = textFieldColors(),
                                modifier = Modifier
                                    .weight(1.4f)
                                    .testTag("primary_model_input")
                            )
                            OutlinedTextField(
                                value = primaryAuthHeader,
                                onValueChange = { primaryAuthHeader = it; savedBanner = false },
                                label = { Text("Header Auth") },
                                placeholder = { Text("Authorization") },
                                singleLine = true,
                                colors = textFieldColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("primary_header_input")
                            )
                        }

                        // Collapsible Fallback Custom API Section
                        HorizontalDivider(color = ObsidianBorder)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFallbackSection = !showFallbackSection }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "API Sandaran / Auto-Fallback (Pilihan)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Aktif automatik jika ralat HTTP 503 atau tamat masa > 15s",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Icon(
                                imageVector = if (showFallbackSection) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = "Buka tetapan API sandaran",
                                tint = TextSecondary
                            )
                        }

                        AnimatedVisibility(visible = showFallbackSection) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ApiProviderPreset.entries.forEach { preset ->
                                        FilterChip(
                                            selected = fallbackProvider == preset.name,
                                            onClick = {
                                                fallbackProvider = preset.name
                                                if (preset != ApiProviderPreset.CUSTOM) {
                                                    fallbackEndpoint = preset.defaultBaseUrl
                                                    fallbackModel = preset.defaultModel
                                                }
                                                savedBanner = false
                                            },
                                            label = { Text(preset.label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CyberCyanTint,
                                                selectedLabelColor = CyberCyanBright
                                            )
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = fallbackKey,
                                    onValueChange = { fallbackKey = it; savedBanner = false },
                                    label = { Text("API Key Sandaran") },
                                    placeholder = { Text("Kosongkan untuk guna kunci utama") },
                                    visualTransformation = if (showFallbackKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { showFallbackKey = !showFallbackKey },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = if (showFallbackKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = "Tunjuk atau sembunyi kunci sandaran",
                                                tint = TextSecondary
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    colors = textFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("fallback_api_key_input")
                                )

                                OutlinedTextField(
                                    value = fallbackEndpoint,
                                    onValueChange = {
                                        fallbackEndpoint = it
                                        fallbackProvider = ApiProviderPreset.CUSTOM.name
                                        savedBanner = false
                                    },
                                    label = { Text("Endpoint Sandaran") },
                                    singleLine = true,
                                    colors = textFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = fallbackModel,
                                        onValueChange = { fallbackModel = it; savedBanner = false },
                                        label = { Text("Model Sandaran") },
                                        singleLine = true,
                                        colors = textFieldColors(),
                                        modifier = Modifier.weight(1.4f)
                                    )
                                    OutlinedTextField(
                                        value = fallbackAuthHeader,
                                        onValueChange = { fallbackAuthHeader = it; savedBanner = false },
                                        label = { Text("Header Auth") },
                                        singleLine = true,
                                        colors = textFieldColors(),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Optional Luno Read-Only Keys
                        HorizontalDivider(color = ObsidianBorder)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLunoSection = !showLunoSection }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Kunci Luno Read-Only (Pilihan)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Untuk tarik data lilin terus daripada Luno Exchange",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Icon(
                                imageVector = if (showLunoSection) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = "Buka tetapan Luno API",
                                tint = TextSecondary
                            )
                        }

                        AnimatedVisibility(visible = showLunoSection) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = lunoKeyId,
                                    onValueChange = { lunoKeyId = it; savedBanner = false },
                                    label = { Text("Luno Key ID") },
                                    singleLine = true,
                                    colors = textFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = lunoKeySecret,
                                    onValueChange = { lunoKeySecret = it; savedBanner = false },
                                    label = { Text("Luno Secret") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    colors = textFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onTestApi(primaryEndpoint, primaryKey, primaryModel, primaryAuthHeader)
                                },
                                enabled = !isTestingApi,
                                border = BorderStroke(1.dp, ObsidianBorder),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("test_api_button")
                            ) {
                                if (isTestingApi) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Menguji...", color = TextPrimary)
                                } else {
                                    Text("Uji Sambungan", color = TextPrimary)
                                }
                            }

                            Button(
                                onClick = {
                                    onSaveSettings(
                                        settings.copy(
                                            primaryProvider = primaryProvider,
                                            primaryApiKey = primaryKey,
                                            primaryModel = primaryModel,
                                            primaryEndpoint = primaryEndpoint,
                                            primaryAuthHeader = primaryAuthHeader,
                                            fallbackProvider = fallbackProvider,
                                            fallbackApiKey = fallbackKey,
                                            fallbackModel = fallbackModel,
                                            fallbackEndpoint = fallbackEndpoint,
                                            fallbackAuthHeader = fallbackAuthHeader,
                                            lunoKeyId = lunoKeyId,
                                            lunoKeySecret = lunoKeySecret,
                                            strictNightModeEnabled = strictNight,
                                            notificationsEnabled = notifEnabled
                                        )
                                    )
                                    savedBanner = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberCyan,
                                    contentColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("save_settings_button")
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simpan")
                            }
                        }

                        if (savedBanner) {
                            Text(
                                text = "Tetapan berjaya disimpan secara terenkripsi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldBright
                            )
                        }

                        apiTestResult?.let { (ok, msg) ->
                            Surface(
                                color = if (ok) EmeraldTint else CrimsonTint,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (ok) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (ok) EmeraldBright else CrimsonBright,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (ok) EmeraldBright else CrimsonBright
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Schedule & Notification Preferences
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Jadual & Notifikasi (MYT)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Mod Malam Ketat (02:00 – 07:00 MYT)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Naikkan syarat skor notifikasi daripada ≥ 82 kepada ≥ 88 semasa kecairan rendah.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = strictNight,
                                onCheckedChange = { checked ->
                                    strictNight = checked
                                    onSaveSettings(settings.copy(strictNightModeEnabled = checked, notificationsEnabled = notifEnabled))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = CyberCyan
                                ),
                                modifier = Modifier.testTag("strict_night_mode_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Notifikasi Peluang & Cooldown 2 Jam",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Hanya hantar amaran apabila skor melepasi had dan minimum jarak 2 jam setiap koin.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = notifEnabled,
                                onCheckedChange = { checked ->
                                    notifEnabled = checked
                                    onSaveSettings(settings.copy(strictNightModeEnabled = strictNight, notificationsEnabled = checked))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = EmeraldGradeA
                                ),
                                modifier = Modifier.testTag("notifications_switch")
                            )
                        }

                        Surface(
                            color = ObsidianSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ObsidianBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Kebenaran Notifikasi (Android 13+)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                    Surface(
                                        color = if (hasNotifPerm) EmeraldTint else AmberTint,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (hasNotifPerm) "Dibenarkan" else "Belum Aktif",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (hasNotifPerm) EmeraldBright else AmberBright,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm) {
                                    OutlinedButton(
                                        onClick = {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        },
                                        border = BorderStroke(1.dp, CyberCyan),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("request_notification_permission_button")
                                    ) {
                                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = CyberCyanBright)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Benarkan Notifikasi", color = CyberCyanBright)
                                    }
                                }

                                HorizontalDivider(color = ObsidianBorder)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = "Pengoptimuman Bateri Latar Belakang",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Elakkan sistem Android mematikan jadual imbasan WorkManager di latar belakang.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Surface(
                                        color = if (isIgnoringBattery) EmeraldTint else AmberTint,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isIgnoringBattery) "Diabaikan" else "Terhad",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isIgnoringBattery) EmeraldBright else AmberBright,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        openBatteryOptimizationSettings(context)
                                        isIgnoringBattery = isBatteryOptimizationIgnored(context)
                                    },
                                    border = BorderStroke(1.dp, ObsidianBorder),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("battery_optimization_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.BatteryChargingFull,
                                        contentDescription = null,
                                        tint = EmeraldBright
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Abai Pengoptimuman Bateri", color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // Error & Auto-Fallback Log
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Log Sistem (${errorLogs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    if (errorLogs.isNotEmpty()) {
                        TextButton(
                            onClick = onClearLogs,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_logs_button")
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Padam log", tint = CrimsonBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Padam", color = CrimsonBright)
                        }
                    }
                }
            }

            if (errorLogs.isEmpty()) {
                item {
                    Surface(
                        color = ObsidianCard,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tiada rekod ralat atau auto-fallback setakat ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(errorLogs, key = { it.id }) { log ->
                    val (lvlColor, lvlBg) = when (log.level) {
                        "FALLBACK" -> AmberBright to AmberTint
                        "TRIGGER" -> EmeraldBright to EmeraldTint
                        "ERROR" -> CrimsonBright to CrimsonTint
                        else -> CyberCyanBright to CyberCyanTint
                    }
                    Surface(
                        color = ObsidianCard,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(color = lvlBg, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = "${log.level} · ${log.pair}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = lvlColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = log.timestampMyt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            Text(
                                text = log.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // NFA Disclaimer
            item {
                Text(
                    text = "Penafian (NFA): Aplikasi ini adalah alat analisis data baca-sahaja untuk tujuan pendidikan sahaja, bukan nasihat kewangan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyberCyan,
    unfocusedBorderColor = ObsidianBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = CyberCyanBright,
    unfocusedLabelColor = TextSecondary
)

private fun isBatteryOptimizationIgnored(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isIgnoringBatteryOptimizations(context.packageName) == true
    } else {
        true
    }
}

private fun openBatteryOptimizationSettings(context: Context) {
    val batteryIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }
    runCatching {
        context.startActivity(batteryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(fallback) }
    }
}
