package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.math.TechnicalIndicators
import com.example.domain.model.CoinScanUiModel
import com.example.domain.model.OpportunityBadge
import com.example.domain.model.TradeLevels
import com.example.ui.components.LunoFeeCalculatorCard
import com.example.ui.components.ProbabilityMatrixCard
import com.example.ui.components.ScoreBreakdownBars
import com.example.ui.components.SixChecksPanel
import com.example.ui.components.SparklineChart
import com.example.ui.theme.AmberBright
import com.example.ui.theme.AmberTint
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonTint
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinDetailScreen(
    coin: CoinScanUiModel?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = coin?.let { "${it.pairInfo.baseSymbol}/MYR · ${it.pairInfo.displayName}" }
                            ?: "Butiran Analisis",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Radar",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianSurface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (coin == null) {
                Text(
                    text = "Memuatkan analisis koin...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                val telemetry = coin.telemetry
                val analysis = coin.analysis
                val (badgeColor, badgeBg) = when (coin.badge) {
                    OpportunityBadge.GRADE_A -> EmeraldBright to EmeraldTint
                    OpportunityBadge.WATCH -> AmberBright to AmberTint
                    OpportunityBadge.HOLD -> SlateHold to SlateHoldTint
                    OpportunityBadge.VETO -> CrimsonBright to CrimsonTint
                }

                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 840.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Price & Executive Summary Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                            border = BorderStroke(1.dp, ObsidianBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column {
                                        Text(
                                            text = if (telemetry != null) TechnicalIndicators.formatMyr(telemetry.lastPriceMyr) else "RM --",
                                            style = MaterialTheme.typography.displaySmall,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = telemetry?.let {
                                                "Bid ${TechnicalIndicators.formatMyr(it.bidMyr)} · Ask ${TechnicalIndicators.formatMyr(it.askMyr)}"
                                            } ?: "Menunggu data buku pesanan",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }

                                    Surface(
                                        color = badgeBg,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = analysis?.action ?: coin.statusLabelBm,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                if (telemetry != null && telemetry.closed1hCloses.size >= 2) {
                                    SparklineChart(
                                        closes = telemetry.closed1hCloses,
                                        lineColor = badgeColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(74.dp)
                                    )
                                }

                                if (analysis != null) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TagPill("Bias: ${analysis.globalBias}", CyberCyanBright, CyberCyanTint)
                                        TagPill("Setup: ${analysis.setup}", TextPrimary, ObsidianCardElevated)
                                    }
                                    Text(
                                        text = analysis.summaryBm,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Enjin: ${analysis.modelUsed} · ${analysis.timestampMyt}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }

                    // Deterministic Technical Indicators Grid
                    if (telemetry != null) {
                        item {
                            DeterministicMetricsCard(telemetry = telemetry)
                        }
                    }

                    // 5-Pillar Score Bars
                    if (analysis != null) {
                        item {
                            ScoreBreakdownBars(
                                scores = analysis.scores,
                                confidenceScore = analysis.confidenceScore
                            )
                        }

                        // 6 Kunci Mutlak Checkmarks
                        item {
                            SixChecksPanel(
                                checks = analysis.checks,
                                enforcedAction = analysis.action
                            )
                        }

                        // 3-Scenario Probability Matrix
                        item {
                            ProbabilityMatrixCard(probabilities = analysis.probabilities)
                        }

                        // Support / Entry / TP1 / TP2 / SL / Invalidation MYR Zones
                        item {
                            PriceLevelsZoneCard(
                                levels = analysis.levels,
                                action = analysis.action,
                                telemetry = telemetry
                            )
                        }
                    }

                    // Built-in Luno Fee Calculator (Maker 0.35% vs Instant 2%)
                    item {
                        LunoFeeCalculatorCard(
                            telemetry = telemetry,
                            levels = analysis?.levels
                        )
                    }

                    item {
                        Text(
                            text = "Penafian (NFA): Analisis kuantitatif & AI ini untuk tujuan pendidikan sahaja, bukan nasihat kewangan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagPill(text: String, fg: Color, bg: Color) {
    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DeterministicMetricsCard(telemetry: com.example.domain.model.MarketTelemetry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Indikator Teknikal (1H / 4H)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = telemetry.candleSource,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyanBright
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCell(
                    label = "RSI(14) 1H",
                    value = "%.2f".format(telemetry.rsi1h),
                    color = when {
                        telemetry.rsi1h < 32.0 -> EmeraldBright
                        telemetry.rsi1h > 58.0 -> CrimsonBright
                        else -> TextPrimary
                    },
                    modifier = Modifier.weight(1f)
                )
                MetricCell(
                    label = "RSI(14) 4H",
                    value = "%.2f".format(telemetry.rsi4h),
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricCell(
                    label = "Spread Luno",
                    value = "${"%.2f".format(telemetry.spreadPct)}%",
                    color = if (telemetry.spreadPct > 1.5) CrimsonBright else EmeraldBright,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCell(
                    label = "Perubahan 1J",
                    value = "${if (telemetry.priceChange1hPct >= 0) "+" else ""}${"%.2f".format(telemetry.priceChange1hPct)}%",
                    color = if (telemetry.priceChange1hPct >= 0) EmeraldBright else CrimsonBright,
                    modifier = Modifier.weight(1f)
                )
                MetricCell(
                    label = "ATR 1H %",
                    value = "${"%.2f".format(telemetry.atr1hPct)}%",
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricCell(
                    label = "Premium MYR",
                    value = "${if (telemetry.myrPremiumPct >= 0) "+" else ""}${"%.2f".format(telemetry.myrPremiumPct)}%",
                    color = AmberBright,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCell(
                    label = "EMA 20 (1H)",
                    value = TechnicalIndicators.formatMyr(telemetry.ema20_1h),
                    color = CyberCyanBright,
                    modifier = Modifier.weight(1f)
                )
                MetricCell(
                    label = "EMA 50 (1H)",
                    value = TechnicalIndicators.formatMyr(telemetry.ema50_1h),
                    color = AmberBright,
                    modifier = Modifier.weight(1f)
                )
                MetricCell(
                    label = "EMA 200 (1H)",
                    value = TechnicalIndicators.formatMyr(telemetry.ema200_1h),
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricCell(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = ObsidianSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PriceLevelsZoneCard(
    levels: TradeLevels?,
    action: String,
    telemetry: com.example.domain.model.MarketTelemetry?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, if (levels != null) EmeraldGradeA.copy(alpha = 0.6f) else ObsidianBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (levels != null) Icons.Filled.TrendingUp else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (levels != null) EmeraldBright else AmberBright,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Zon Sokongan, Sasaran & Pembatalan (MYR)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            if (levels == null) {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Paras Entri & TP Dikunci ($action)",
                            style = MaterialTheme.typography.titleSmall,
                            color = AmberBright
                        )
                        Text(
                            text = "Kunci Mutlak aktif: sekurang-kurangnya 1 daripada 6 semakan tidak dipenuhi atau pasaran belum sesuai. Rujukan teknikal EMA50/EMA200: ${
                                telemetry?.let {
                                    "${TechnicalIndicators.formatMyr(it.ema50_1h)} / ${TechnicalIndicators.formatMyr(it.ema200_1h)}"
                                } ?: "--"
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LevelRow("Zon Sokongan / Entri Maker", levels.entryZoneMyr, CyberCyanBright)
                LevelRow("Sasaran 1 (TP1)", TechnicalIndicators.formatMyr(levels.tp1Myr), EmeraldBright)
                LevelRow("Sasaran 2 (TP2)", TechnicalIndicators.formatMyr(levels.tp2Myr), EmeraldBright)
                LevelRow("Henti Rugi (Stop Loss)", TechnicalIndicators.formatMyr(levels.stopLossMyr), CrimsonBright)
                LevelRow("Pembatalan Struktur (Invalidation)", TechnicalIndicators.formatMyr(levels.invalidationMyr), AmberBright)
            }
        }
    }
}

@Composable
private fun LevelRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianSurface, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.labelMedium, color = valueColor, fontWeight = FontWeight.Bold)
    }
}
