package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.SignalHistoryEntity
import com.example.domain.math.TechnicalIndicators
import com.example.ui.theme.AmberBright
import com.example.ui.theme.AmberTint
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonTint
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberCyanTint
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldTint
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private enum class HistoryFilter(val label: String) {
    ALL("Semua"),
    OPEN("Aktif"),
    WIN("Menang (TP)"),
    LOSS("Rugi (SL)")
}

@Composable
fun SignalHistoryScreen(
    signals: List<SignalHistoryEntity>,
    onUpdateOutcome: (Long, String) -> Unit,
    onDeleteSignal: (Long) -> Unit,
    onClearAll: () -> Unit,
    onOpenCoin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by rememberSaveable { mutableStateOf(HistoryFilter.ALL) }

    val winsCount = signals.count { it.outcomeStatus == "WIN_TP1" || it.outcomeStatus == "WIN_TP2" }
    val lossCount = signals.count { it.outcomeStatus == "LOSS_SL" }
    val openCount = signals.count { it.outcomeStatus == "OPEN" }
    val closedTotal = winsCount + lossCount
    val winrateText = if (closedTotal > 0) {
        "${"%.1f".format((winsCount.toDouble() / closedTotal) * 100.0)}%"
    } else {
        "--%"
    }

    val filtered = when (selectedFilter) {
        HistoryFilter.ALL -> signals
        HistoryFilter.OPEN -> signals.filter { it.outcomeStatus == "OPEN" }
        HistoryFilter.WIN -> signals.filter { it.outcomeStatus == "WIN_TP1" || it.outcomeStatus == "WIN_TP2" }
        HistoryFilter.LOSS -> signals.filter { it.outcomeStatus == "LOSS_SL" }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sejarah Isyarat",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Rekod amaran STRONG_WATCH_BUY & semakan winrate",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (signals.isNotEmpty()) {
                        TextButton(
                            onClick = onClearAll,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Padam semua sejarah",
                                tint = CrimsonBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Padam", color = CrimsonBright)
                        }
                    }
                }
            }

            // Winrate Summary Card
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Kadar Menang (Winrate)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = winrateText,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = if (winsCount >= lossCount && closedTotal > 0) EmeraldBright else TextPrimary
                                )
                            }
                            Surface(
                                color = ObsidianSurface,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ObsidianBorder)
                            ) {
                                Text(
                                    text = "Jumlah: ${signals.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = ObsidianBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatBox("Menang (TP)", "$winsCount", EmeraldBright, EmeraldTint, Modifier.weight(1f))
                            StatBox("Rugi (SL)", "$lossCount", CrimsonBright, CrimsonTint, Modifier.weight(1f))
                            StatBox("Aktif", "$openCount", AmberBright, AmberTint, Modifier.weight(1f))
                        }
                    }
                }
            }

            // Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(HistoryFilter.entries) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) },
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
                            )
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Belum ada rekod isyarat STRONG_WATCH_BUY.",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "Setiap amaran beli berkualiti tinggi (6/6 semakan lulus) akan direkodkan secara automatik di sini untuk semakan winrate.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { item ->
                    SignalHistoryCard(
                        item = item,
                        onUpdateOutcome = { outcome -> onUpdateOutcome(item.id, outcome) },
                        onDelete = { onDeleteSignal(item.id) },
                        onOpenCoin = { onOpenCoin(item.pair) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    fg: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = fg, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SignalHistoryCard(
    item: SignalHistoryEntity,
    onUpdateOutcome: (String) -> Unit,
    onDelete: () -> Unit,
    onOpenCoin: () -> Unit
) {
    val (statusLabel, statusFg, statusBg) = when (item.outcomeStatus) {
        "WIN_TP2" -> Triple("MENANG TP2", EmeraldBright, EmeraldTint)
        "WIN_TP1" -> Triple("MENANG TP1", EmeraldBright, EmeraldTint)
        "LOSS_SL" -> Triple("KENA SL", CrimsonBright, CrimsonTint)
        else -> Triple("AKTIF", AmberBright, AmberTint)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("signal_history_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianBorder),
        shape = RoundedCornerShape(12.dp),
        onClick = onOpenCoin
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${item.baseSymbol}/MYR · Skor ${item.confidenceScore}/100",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${item.triggeredAtMyt} · ${item.setup}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = statusBg, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Padam rekod isyarat",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PricePairLine("Zon Entri", item.entryZoneMyr, CyberCyanBright)
                    PricePairLine("Harga Rujukan", TechnicalIndicators.formatMyr(item.entryRefPriceMyr), TextPrimary)
                    PricePairLine("TP1", TechnicalIndicators.formatMyr(item.tp1Myr), EmeraldBright)
                    PricePairLine("TP2", TechnicalIndicators.formatMyr(item.tp2Myr), EmeraldBright)
                    PricePairLine("Stop Loss (SL)", TechnicalIndicators.formatMyr(item.stopLossMyr), CrimsonBright)
                }
            }

            Text(
                text = item.summaryBm,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Text(
                text = "Kemaskini Status Winrate:",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "OPEN" to "Aktif",
                    "WIN_TP1" to "TP1",
                    "WIN_TP2" to "TP2",
                    "LOSS_SL" to "SL"
                ).forEach { (code, label) ->
                    FilterChip(
                        selected = item.outcomeStatus == code,
                        onClick = { onUpdateOutcome(code) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyanTint,
                            selectedLabelColor = CyberCyanBright,
                            containerColor = ObsidianSurface,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PricePairLine(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.labelMedium, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}
