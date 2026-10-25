// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.telemetry.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.arinara.arntcli.ui.theme.ElectricSlateTokens

@Composable
fun SparklineGraph(
    data: List<Long>,
    lineColor: Color = ElectricSlateTokens.PrimaryAccent,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(36.dp)
) {
    if (data.isEmpty()) {
        Canvas(modifier = modifier) {
            drawLine(
                color = ElectricSlateTokens.TrackInactive,
                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                strokeWidth = 2.dp.toPx()
            )
        }
        return
    }

    Canvas(modifier = modifier) {
        val maxVal = (data.maxOrNull() ?: 1L).coerceAtLeast(1L).toFloat()
        val minVal = (data.minOrNull() ?: 0L).toFloat()
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val path = Path()
        val stepX = if (data.size > 1) size.width / (data.size - 1) else size.width

        for (i in data.indices) {
            val normalizedY = size.height - (((data[i] - minVal) / range) * (size.height * 0.8f) + (size.height * 0.1f))
            val x = i * stepX
            if (i == 0) {
                path.moveTo(x, normalizedY)
            } else {
                path.lineTo(x, normalizedY)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}
