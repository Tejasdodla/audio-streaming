package com.fifthsense.audiostream.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.fifthsense.audiostream.theme.CoralAccent
import com.fifthsense.audiostream.theme.CyanNeon
import com.fifthsense.audiostream.theme.TextMuted
import com.fifthsense.audiostream.theme.VioletElectric
import kotlin.math.sin

@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 28
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_anim")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val totalSpacing = (barCount - 1) * 4.dp.toPx()
            val barWidth = ((canvasWidth - totalSpacing) / barCount).coerceAtLeast(3.dp.toPx())

            for (i in 0 until barCount) {
                val x = i * (barWidth + 4.dp.toPx())

                val normalizedHeight = if (isPlaying) {
                    val wave1 = sin(phase + (i * 0.4f)) * 0.4f + 0.5f
                    val wave2 = sin(phase * 1.5f + (i * 0.25f)) * 0.3f + 0.3f
                    ((wave1 + wave2) / 1.7f).coerceIn(0.12f, 1.0f)
                } else {
                    0.08f
                }

                val barHeight = canvasHeight * normalizedHeight
                val y = canvasHeight - barHeight

                val barBrush = Brush.verticalGradient(
                    colors = listOf(CyanNeon, VioletElectric, CoralAccent),
                    startY = y,
                    endY = canvasHeight
                )

                drawRoundRect(
                    brush = barBrush,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("20 Hz", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(if (isPlaying) "STREAMING ACTIVE" else "IDLE / BUFFERED",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = if (isPlaying) CyanNeon else TextMuted)
            Text("20 kHz", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}
