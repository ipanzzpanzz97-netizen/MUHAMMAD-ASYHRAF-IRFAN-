package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.math.TechnicalIndicators
import com.example.domain.model.CoinScanUiModel
import com.example.domain.model.GlobalMacroState
import com.example.domain.model.OpportunityBadge
import com.example.ui.components.SparklineChart
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
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SlateHold
import com.example.ui.theme.SlateHoldTint
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.RadarFilter
import com.example.ui.viewmodel.RadarUiState

@Composable
fun RadarHomeScreen(
    uiState: RadarUiState,
    onSelectFilter: (RadarFilter) -> Unit,
    onManualRefresh: () -> Unit,
    onCoinClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val eventTriggeredCoins = uiState.coins.filter { it.telemetry?.eventTriggered == true }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWide = maxWidth >= 700.dp

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MarketTopHeader(
                    isNightWindow = uiState.isStrictNightWindowActive,
                    minScore = uiState.activeMinScore,
                    hasApiKey = uiState.settings.primaryApiKey.isNotBlank() ||
                        uiState.settings.fallbackApiKey.isNotBlank()
                )
            }

            item {
                MarketSummaryCard(
                    macro = uiState.macro,
                    isNightWindow = uiState.isStrictNightWindowActive,
                    minScore = uiState.activeMinScore
                )
            }

            if (eventTriggeredCoins.isNotEmpty()) {
                item {
                    PriceAlertNotice(
                        pairs = eventTriggeredCoins.joinToString(", ") { it.pairInfo.baseSymbol }
                    )
                }
            }

            item {
                ActionAndFilterSection(
                    isScanning = uiState.isScanning,
                    cooldownSecondsLeft = uiState.cooldownSecondsLeft,
                    selectedFilter = uiState.selectedFilter,
                    statusMessage = uiState.statusMessage,
                    onManualRefresh = onManualRefresh,
                    onSelectFilter = onSelectFilter
                )
            }

            if (uiState.coins.isEmpty()) {
                item {
                    EmptyMarketCard(isScanning = uiState.isScanning)
                }
            } else if (isWide) {
                val rows = uiState.coins.chunked(2)
                items(rows, key = { chunk -> chunk.joinToString("-") { it.pairInfo.lunoPair } }) { pairRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pairRow.forEach { coin ->
                            CoinRowCard(
                                coin = coin,
                                onClick = { onCoinClick(coin.pairInfo.lunoPair) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pairRow.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(uiState.coins, key = { it.pairInfo.lunoPair }) { coin ->
                    CoinRowCard(
                        coin = coin,
                        onClick = { onCoinClick(coin.pairInfo.lunoPair) }
                    )
                }
            }

            item {
                Text(
                    text = "Penafian: Untuk tujuan analisis data & pendidikan sahaja, bukan nasihat kewangan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun MarketTopHeader(
    isNightWindow: Boolean,
    minScore: Int,
    hasApiKey: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Pasaran Luno MYR",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = if (hasApiKey) "Mod baca-sahaja aktif" else "Mod teknikal sahaja · Kunci API belum diisi",
                style = MaterialTheme.typography.bodySmall,
                color = if (hasApiKey) TextSecondary else AmberBright
            )
        }

        Surface(
            color = if (isNightWindow) AmberTint else ObsidianCard,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, if (isNightWindow) AmberWatch.copy(alpha = 0.5f) else ObsidianBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (isNightWindow) {
                    Icon(
                        imageVector = Icons.Filled.NightsStay,
                        contentDescription = null,
                        tint = AmberBright,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = if (isNightWindow) "Malam · ≥ $minScore" else "Had Skor ≥ $minScore",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isNightWindow) AmberBright else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun MarketSummaryCard(
    macro: GlobalMacroState,
    isNightWindow: Boolean,
    minScore: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ObsidianBorder),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Bitcoin (BTC/MYR)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = if (macro.btcMyr > 0.0) TechnicalIndicators.formatMyr(macro.btcMyr) else "RM --",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                }

                val chgColor = if (macro.btcChange24hPct >= 0) EmeraldBright else CrimsonBright
                val chgBg = if (macro.btcChange24hPct >= 0) EmeraldTint else CrimsonTint
                val prefix = if (macro.btcChange24hPct >= 0) "+" else ""
                Surface(color = chgBg, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = "$prefix${"%.2f".format(macro.btcChange24hPct)}% (24J)",
                        style = MaterialTheme.typography.labelMedium,
                        color = chgColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            HorizontalDivider(color = ObsidianBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Indeks Fear & Greed",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Text(
                        text = macro.fearGreedLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Imbasan Seterusnya",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Text(
                        text = if (isNightWindow) "${macro.nextHourlyScanMyt} (Malam ≥$minScore)" else macro.nextHourlyScanMyt,
                        style = MaterialTheme.typography.titleSmall,
                        color = CyberCyanBright
                    )
                }
            }

            Text(
                text = macro.macroContext,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PriceAlertNotice(pairs: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AmberTint,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, AmberWatch.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = "Amaran Pergerakan",
                tint = AmberBright,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pergerakan Pantas Dikesan ($pairs)",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Kejatuhan melebihi 4% (1J) atau RSI < 32. Semakan segera dijalankan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberBright
                )
            }
        }
    }
}

@Composable
private fun ActionAndFilterSection(
    isScanning: Boolean,
    cooldownSecondsLeft: Int,
    selectedFilter: RadarFilter,
    statusMessage: String?,
    onManualRefresh: () -> Unit,
    onSelectFilter: (RadarFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onManualRefresh,
                enabled = !isScanning && cooldownSecondsLeft == 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = TextPrimary,
                    disabledContainerColor = ObsidianCardElevated,
                    disabledContentColor = TextSecondary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("manual_scan_button")
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mengemas kini...", style = MaterialTheme.typography.labelLarge)
                } else if (cooldownSecondsLeft > 0) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tunggu ${cooldownSecondsLeft}s", style = MaterialTheme.typography.labelLarge)
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Imbas Sekarang", style = MaterialTheme.typography.labelLarge)
                }
            }

            Text(
                text = "Susunan: Skor Tertinggi",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        AnimatedVisibility(visible = !statusMessage.isNullOrBlank()) {
            Text(
                text = statusMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(RadarFilter.entries) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onSelectFilter(filter) },
                    label = { Text(filter.labelBm) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyanTint,
                        selectedLabelColor = CyberCyanBright,
                        containerColor = ObsidianCard,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedFilter == filter,
                        borderColor = ObsidianBorder,
                        selectedBorderColor = CyberCyan
                    ),
                    modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
private fun CoinRowCard(
    coin: CoinScanUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (accentColor, badgeBg) = when (coin.badge) {
        OpportunityBadge.GRADE_A -> EmeraldBright to EmeraldTint
        OpportunityBadge.WATCH -> AmberBright to AmberTint
        OpportunityBadge.HOLD -> SlateHold to SlateHoldTint
        OpportunityBadge.VETO -> CrimsonBright to CrimsonTint
    }

    val telemetry = coin.telemetry
    val analysis = coin.analysis
    val score = analysis?.confidenceScore ?: 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("coin_card_${coin.pairInfo.lunoPair.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ObsidianCardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = coin.pairInfo.baseSymbol,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "${coin.pairInfo.baseSymbol}/MYR",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = coin.pairInfo.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (telemetry != null) TechnicalIndicators.formatMyr(telemetry.lastPriceMyr) else "RM --",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "${coin.statusLabelBm} · $score/100",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (telemetry != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        val chg24 = telemetry.change24hPct
                        val chgColor = if (chg24 >= 0) EmeraldBright else CrimsonBright
                        MetricMini("24J", "${if (chg24 >= 0) "+" else ""}${"%.2f".format(chg24)}%", chgColor)
                        MetricMini("RSI 1H", "${"%.1f".format(telemetry.rsi1h)}", TextPrimary)
                        MetricMini("Spread", "${"%.2f".format(telemetry.spreadPct)}%", TextSecondary)
                    }

                    SparklineChart(
                        closes = telemetry.closed1hCloses,
                        lineColor = accentColor,
                        modifier = Modifier
                            .width(84.dp)
                            .height(26.dp)
                    )
                }
            }

            if (analysis != null) {
                Text(
                    text = analysis.summaryBm,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MetricMini(label: String, value: String, valueColor: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(text = value, style = MaterialTheme.typography.labelMedium, color = valueColor)
    }
}

@Composable
private fun EmptyMarketCard(isScanning: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.TrendingUp,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = if (isScanning) "Memuatkan data pasaran Luno MYR..." else "Tiada pasangan koin untuk penapis ini.",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = "Tekan 'Imbas Sekarang' atau pilih penapis 'Semua'.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
