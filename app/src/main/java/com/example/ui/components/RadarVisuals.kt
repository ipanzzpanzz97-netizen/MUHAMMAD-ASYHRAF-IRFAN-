package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.math.TechnicalIndicators
import com.example.domain.model.MarketTelemetry
import com.example.domain.model.ScenarioProbabilities
import com.example.domain.model.ScoreBreakdown
import com.example.domain.model.SixChecks
import com.example.domain.model.TradeLevels
import com.example.ui.theme.AmberBright
import com.example.ui.theme.AmberTint
import com.example.ui.theme.AmberWatch
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonTint
import com.example.ui.theme.CrimsonVeto
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldGradeA
import com.example.ui.theme.EmeraldTint
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SlateHold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SparklineChart(
    closes: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (closes.size < 2) {
            drawLine(
                color = lineColor.copy(alpha = 0.4f),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 3f
            )
            return@Canvas
        }
        val minVal = closes.minOrNull() ?: 0.0
        val maxVal = closes.maxOrNull() ?: 1.0
        val range = (maxVal - minVal).takeIf { it > 0.0 } ?: 1.0
        val stepX = size.width / (closes.size - 1).coerceAtLeast(1)

        val linePath = Path()
        val fillPath = Path()

        closes.forEachIndexed { idx, value ->
            val x = idx * stepX
            val normalized = ((value - minVal) / range).toFloat()
            val y = size.height - (normalized * size.height * 0.85f) - (size.height * 0.075f)
            if (idx == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)
            )
        )
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun ScoreBreakdownBars(
    scores: ScoreBreakdown,
    confidenceScore: Int,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        Triple("Sokongan Struktur (Support)", scores.support30, 30),
        Triple("Momentum Lantunan (Bounce)", scores.bounce25, 25),
        Triple("Konteks Makro Global", scores.macro20, 20),
        Triple("Kecairan & Spread Luno", scores.liquidity15, 15),
        Triple("Nisbah Risiko : Ganjaran", scores.rrPath10, 10)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pecahan Skor Keyakinan (5 Pilar)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Surface(
                    color = if (confidenceScore >= 82) EmeraldTint else ObsidianCardElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (confidenceScore >= 82) EmeraldGradeA else ObsidianBorder
                    )
                ) {
                    Text(
                        text = "$confidenceScore / 100",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (confidenceScore >= 82) EmeraldBright else TextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            rows.forEach { (label, score, maxVal) ->
                val ratio = (score.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                val animatedRatio by animateFloatAsState(
                    targetValue = ratio,
                    animationSpec = tween(durationMillis = 500),
                    label = label
                )
                val barColor = when {
                    ratio >= 0.8f -> EmeraldGradeA
                    ratio >= 0.55f -> CyberCyan
                    ratio >= 0.35f -> AmberWatch
                    else -> CrimsonVeto
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            text = "$score / $maxVal",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ObsidianSurface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedRatio)
                                .clip(RoundedCornerShape(4.dp))
                                .background(barColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SixChecksPanel(
    checks: SixChecks,
    enforcedAction: String,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        "Data Pasaran Sah (data_valid)" to checks.dataValid,
        "Bebas Veto & Anti-FOMO (vetoes_clear)" to checks.vetoesClear,
        "Setup Teknikal Sah (setup_confirmed)" to checks.setupConfirmed,
        "Sasaran EV Lepas Yuran (ev_targets_ok)" to checks.evTargetsOk,
        "Stop Loss & Maker OK (sl_and_maker_ok)" to checks.slAndMakerOk,
        "Skor >= Had Minimum (score_ge_min)" to checks.scoreGeMin
    )
    val allOk = checks.allPassed()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, if (allOk) EmeraldGradeA.copy(alpha = 0.6f) else ObsidianBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "6 Semakan Kunci Mutlak",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Surface(
                    color = if (allOk) EmeraldTint else AmberTint,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (allOk) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                            contentDescription = null,
                            tint = if (allOk) EmeraldBright else AmberBright,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (allOk) "SEMUA LULUS" else "DIKUNCI: $enforcedAction",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (allOk) EmeraldBright else AmberBright
                        )
                    }
                }
            }

            items.forEach { (label, passed) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (passed) Icons.Filled.CheckCircle else Icons.Outlined.Cancel,
                            contentDescription = if (passed) "Lulus" else "Gagal",
                            tint = if (passed) EmeraldGradeA else CrimsonBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (passed) TextPrimary else TextSecondary
                        )
                    }
                    Text(
                        text = if (passed) "LULUS" else "GAGAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (passed) EmeraldBright else CrimsonBright
                    )
                }
            }
        }
    }
}

@Composable
fun ProbabilityMatrixCard(
    probabilities: ScenarioProbabilities,
    modifier: Modifier = Modifier
) {
    val total = (probabilities.upsidePctChance + probabilities.sidewaysPctChance + probabilities.downsidePctChance)
        .coerceAtLeast(1)
    val upWeight = (probabilities.upsidePctChance.toFloat() / total).coerceAtLeast(0.05f)
    val sideWeight = (probabilities.sidewaysPctChance.toFloat() / total).coerceAtLeast(0.05f)
    val downWeight = (probabilities.downsidePctChance.toFloat() / total).coerceAtLeast(0.05f)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Matriks 3 Senario Kebarangkalian",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            ) {
                Box(modifier = Modifier.weight(upWeight).fillMaxHeight().background(EmeraldGradeA))
                Box(modifier = Modifier.weight(sideWeight).fillMaxHeight().background(SlateHold))
                Box(modifier = Modifier.weight(downWeight).fillMaxHeight().background(CrimsonVeto))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScenarioPill(
                    label = "Naik (Lantun)",
                    pct = "${probabilities.upsidePctChance}%",
                    color = EmeraldBright,
                    bg = EmeraldTint,
                    modifier = Modifier.weight(1f)
                )
                ScenarioPill(
                    label = "Mendatar",
                    pct = "${probabilities.sidewaysPctChance}%",
                    color = TextSecondary,
                    bg = ObsidianCardElevated,
                    modifier = Modifier.weight(1f)
                )
                ScenarioPill(
                    label = "Turun (Risiko)",
                    pct = "${probabilities.downsidePctChance}%",
                    color = CrimsonBright,
                    bg = CrimsonTint,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ScenarioPill(
    label: String,
    pct: String,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = pct, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LunoFeeCalculatorCard(
    telemetry: MarketTelemetry?,
    levels: TradeLevels?,
    modifier: Modifier = Modifier
) {
    var capitalInput by rememberSaveable { mutableStateOf("1000") }
    val capitalMyr = capitalInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    val makerFeeRate = 0.0035 // 0.35% Luno Exchange Maker
    val instantFeeRate = 0.0200 // 2.00% Luno Instant Buy/Sell

    val makerEntryFee = capitalMyr * makerFeeRate
    val makerRoundTripFee = capitalMyr * (makerFeeRate * 2.0)
    val instantEntryFee = capitalMyr * instantFeeRate
    val instantRoundTripFee = capitalMyr * (instantFeeRate * 2.0)
    val savingsOnEntry = (instantEntryFee - makerEntryFee).coerceAtLeast(0.0)
    val savingsRoundTrip = (instantRoundTripFee - makerRoundTripFee).coerceAtLeast(0.0)

    val refPrice = telemetry?.lastPriceMyr ?: 0.0
    val tp1Pct = if (levels != null && refPrice > 0.0 && levels.tp1Myr > refPrice) {
        ((levels.tp1Myr - refPrice) / refPrice)
    } else 0.04 // Default 4% illustration if locked

    val makerNetProfitTp1 = (capitalMyr * tp1Pct) - makerRoundTripFee
    val instantNetProfitTp1 = (capitalMyr * tp1Pct) - instantRoundTripFee

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Calculate,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Kalkulator Yuran Luno (Maker vs Instant)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            OutlinedTextField(
                value = capitalInput,
                onValueChange = { next ->
                    if (next.length <= 9 && next.all { it.isDigit() || it == '.' }) {
                        capitalInput = next
                    }
                },
                label = { Text("Modal Dagangan (RM MYR)") },
                prefix = { Text("RM ", color = CyberCyan) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fee_calculator_input")
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("500", "1000", "5000", "10000").forEach { preset ->
                    FilterChip(
                        selected = capitalInput == preset,
                        onClick = { capitalInput = preset },
                        label = { Text("RM $preset") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CyberCyan
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Maker Column
                Surface(
                    modifier = Modifier.weight(1f),
                    color = EmeraldTint.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, EmeraldGradeA.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "LUNO MAKER (0.35%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldBright
                        )
                        Text(
                            text = "Masuk: ${TechnicalIndicators.formatMyr(makerEntryFee)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Pusingan: ${TechnicalIndicators.formatMyr(makerRoundTripFee)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "Untung @${"%.1f".format(tp1Pct * 100)}%: ${TechnicalIndicators.formatMyr(makerNetProfitTp1)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldBright
                        )
                    }
                }

                // Instant Column
                Surface(
                    modifier = Modifier.weight(1f),
                    color = CrimsonTint.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CrimsonVeto.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "INSTANT BUY (2.00%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrimsonBright
                        )
                        Text(
                            text = "Masuk: ${TechnicalIndicators.formatMyr(instantEntryFee)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Pusingan: ${TechnicalIndicators.formatMyr(instantRoundTripFee)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "Untung @${"%.1f".format(tp1Pct * 100)}%: ${TechnicalIndicators.formatMyr(instantNetProfitTp1)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (instantNetProfitTp1 >= 0) AmberBright else CrimsonBright
                        )
                    }
                }
            }

            Surface(
                color = ObsidianCardElevated,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Penjimatan guna Exchange Maker: ${TechnicalIndicators.formatMyr(savingsOnEntry)} (masuk) · ${TechnicalIndicators.formatMyr(savingsRoundTrip)} (pusingan penuh)",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldBright,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}
