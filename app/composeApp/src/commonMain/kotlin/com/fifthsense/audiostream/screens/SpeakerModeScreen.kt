package com.fifthsense.audiostream.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fifthsense.audiostream.components.AudioVisualizer
import com.fifthsense.audiostream.components.GlassCard
import com.fifthsense.audiostream.components.StatsPanel
import com.fifthsense.audiostream.components.VolumeSlider
import com.fifthsense.audiostream.model.StreamStats
import com.fifthsense.audiostream.theme.CoralAccent
import com.fifthsense.audiostream.theme.CyanNeon
import com.fifthsense.audiostream.theme.DarkBackground
import com.fifthsense.audiostream.theme.DarkSurface
import com.fifthsense.audiostream.theme.DarkSurfaceVariant
import com.fifthsense.audiostream.theme.GreenSuccess
import com.fifthsense.audiostream.theme.TextMuted
import com.fifthsense.audiostream.theme.TextPrimary
import com.fifthsense.audiostream.theme.TextSecondary
import com.fifthsense.audiostream.theme.VioletElectric

@Composable
fun SpeakerModeScreen(
    isCapturing: Boolean,
    stats: StreamStats,
    volume: Int,
    onToggleCapture: () -> Unit,
    onVolumeChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonBgColor by animateColorAsState(
        if (isCapturing) CoralAccent else CyanNeon
    )
    val buttonScale by animateFloatAsState(
        if (isCapturing) 1.05f else 1.0f
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Minimal Hero Broadcast Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (isCapturing) CoralAccent.copy(alpha = 0.5f) else Color(0x2200F5D4)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SPEAKER BROADCAST",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanNeon
                            )
                            Text(
                                text = if (isCapturing) "Streaming Phone Audio Live" else "Ready to Stream Phone Audio",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCapturing) GreenSuccess.copy(alpha = 0.15f) else Color(0x15FFFFFF))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isCapturing) "LC3 HD • Oboe Engine" else "IDLE",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCapturing) GreenSuccess else TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Minimalist Round Broadcast Button
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .scale(buttonScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = if (isCapturing) listOf(
                                        CoralAccent.copy(alpha = 0.4f),
                                        Color.Transparent
                                    ) else listOf(
                                        CyanNeon.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(buttonBgColor)
                                .clickable { onToggleCapture() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCapturing) Icons.Default.CastConnected else Icons.Default.Speaker,
                                contentDescription = "Toggle Stream",
                                tint = DarkBackground,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isCapturing) "Tap to Stop Stream" else "Tap to Stream Any App Audio",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isCapturing) CoralAccent else TextPrimary
                    )

                    // Audio Visualizer
                    AudioVisualizer(
                        isPlaying = isCapturing,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Minimalist Volume Slider
        item {
            VolumeSlider(
                volume = volume,
                onVolumeChanged = onVolumeChanged,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Stream Metrics Stats Panel
        item {
            StatsPanel(
                stats = stats,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
