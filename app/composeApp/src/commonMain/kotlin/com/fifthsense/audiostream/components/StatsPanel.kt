package com.fifthsense.audiostream.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fifthsense.audiostream.model.StreamStats
import com.fifthsense.audiostream.theme.AmberWarning
import com.fifthsense.audiostream.theme.CyanNeon
import com.fifthsense.audiostream.theme.DarkSurfaceVariant
import com.fifthsense.audiostream.theme.GreenSuccess
import com.fifthsense.audiostream.theme.TextMuted
import com.fifthsense.audiostream.theme.TextPrimary
import com.fifthsense.audiostream.theme.TextSecondary
import com.fifthsense.audiostream.theme.VioletElectric

@Composable
fun StatsPanel(
    stats: StreamStats,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE SINK TELEMETRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanNeon
                )
                Text(
                    text = if (stats.isStreaming) "STREAMING ACTIVE" else "IDLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (stats.isStreaming) GreenSuccess else TextMuted
                )
            }

            // Buffer fill bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "nRF54L15 Jitter Buffer Health",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${stats.bufferPercent}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (stats.bufferPercent > 20) GreenSuccess else AmberWarning
                    )
                }

                LinearProgressIndicator(
                    progress = { stats.bufferPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (stats.bufferPercent > 20) CyanNeon else AmberWarning,
                    trackColor = DarkSurfaceVariant
                )
            }

            // Grid Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricItem(
                    label = "Throughput",
                    value = "${stats.kbps.toInt()} kbps",
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Packets Sent",
                    value = "${stats.packetsSent}",
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Underruns",
                    value = "${stats.underrunCount}",
                    valueColor = if (stats.underrunCount > 0) AmberWarning else TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Latency",
                    value = "${stats.latencyMs} ms",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = valueColor
            )
        }
    }
}
