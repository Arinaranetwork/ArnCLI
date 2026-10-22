// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.telemetry.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import java.util.Locale

@Composable
fun MetricGaugeCard(
    title: String,
    percent: Float,
    subtitle: String,
    detailText: String,
    modifier: Modifier = Modifier,
    accentColor: Color = ElectricSlateTokens.PrimaryAccent
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        isGlow = percent >= 85f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ElectricSlateTokens.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = ElectricSlateTokens.TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = detailText,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextPrimary
                )
            }

            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(56.dp)) {
                    // Track
                    drawArc(
                        color = ElectricSlateTokens.TrackInactive,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Active Sweep
                    val sweep = (percent.coerceIn(0f, 100f) / 100f) * 270f
                    drawArc(
                        color = accentColor,
                        startAngle = 135f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = String.format(Locale.US, "%.0f%%", percent),
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextPrimary
                )
            }
        }
    }
}

@Composable
fun ProgressBarIndicator(
    label: String,
    percent: Int,
    detailText: String,
    modifier: Modifier = Modifier,
    fillColor: Color = ElectricSlateTokens.PrimaryAccent
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = ElectricSlateTokens.TextPrimary
            )
            Text(
                text = detailText,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Light,
                fontSize = 11.sp,
                color = ElectricSlateTokens.TextMuted
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(ElectricSlateTokens.TrackInactive)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (percent.coerceIn(0, 100) / 100f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(fillColor)
            )
        }
    }
}
