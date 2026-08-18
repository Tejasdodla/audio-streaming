package com.fifthsense.audiostream.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fifthsense.audiostream.components.AudioVisualizer
import com.fifthsense.audiostream.components.GlassCard
import com.fifthsense.audiostream.components.VolumeSlider
import com.fifthsense.audiostream.model.FileAudioItem
import com.fifthsense.audiostream.theme.CoralAccent
import com.fifthsense.audiostream.theme.CyanNeon
import com.fifthsense.audiostream.theme.DarkBackground
import com.fifthsense.audiostream.theme.DarkSurfaceVariant
import com.fifthsense.audiostream.theme.TextMuted
import com.fifthsense.audiostream.theme.TextPrimary
import com.fifthsense.audiostream.theme.TextSecondary
import com.fifthsense.audiostream.theme.VioletElectric

@Composable
fun FileStreamScreen(
    currentTrack: FileAudioItem?,
    playlist: List<FileAudioItem>,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    volume: Int,
    onOpenFilePicker: () -> Unit,
    onTrackSelected: (FileAudioItem) -> Unit,
    onPlayClicked: () -> Unit,
    onPauseClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onSeekPosition: (Long) -> Unit,
    onVolumeChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Player Glass Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FILE PLAYER",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanNeon
                            )
                            Text(
                                text = currentTrack?.title ?: "No track selected",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }

                        Button(
                            onClick = onOpenFilePicker,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanNeon.copy(alpha = 0.15f),
                                contentColor = CyanNeon
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Open File")
                        }
                    }

                    // Waveform visualizer
                    AudioVisualizer(
                        isPlaying = isPlaying,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Progress Scrubber
                    val totalDuration = (currentTrack?.durationMs ?: 1000L).coerceAtLeast(1L)
                    val progress = (playbackPositionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Slider(
                            value = progress,
                            onValueChange = { frac ->
                                onSeekPosition((frac * totalDuration).toLong())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = CyanNeon,
                                activeTrackColor = CyanNeon,
                                inactiveTrackColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(playbackPositionMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Text(
                                text = formatTime(currentTrack?.durationMs ?: 0L),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    // Playback Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onStopClicked,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x15FFFFFF))
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = CoralAccent)
                        }

                        Spacer(modifier = Modifier.size(20.dp))

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CyanNeon)
                                .clickable {
                                    if (isPlaying) onPauseClicked() else onPlayClicked()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = DarkBackground,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        // Volume Slider
        item {
            VolumeSlider(
                volume = volume,
                onVolumeChanged = onVolumeChanged,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Track List
        if (playlist.isNotEmpty()) {
            item {
                Text(
                    text = "PLAYLIST",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanNeon
                )
            }

            items(playlist) { track ->
                val isSelected = track.id == currentTrack?.id
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrackSelected(track) },
                    borderColor = if (isSelected) CyanNeon.copy(alpha = 0.6f) else Color(0x15FFFFFF)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else Color(0x10FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = null,
                                    tint = if (isSelected) CyanNeon else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) CyanNeon else TextPrimary
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Text(
                            text = formatTime(track.durationMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${if (seconds < 10) "0$seconds" else "$seconds"}"
}
