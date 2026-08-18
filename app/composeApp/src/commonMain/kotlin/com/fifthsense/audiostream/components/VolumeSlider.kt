package com.fifthsense.audiostream.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fifthsense.audiostream.theme.CyanNeon
import com.fifthsense.audiostream.theme.DarkSurfaceVariant
import com.fifthsense.audiostream.theme.TextMuted
import com.fifthsense.audiostream.theme.TextPrimary
import com.fifthsense.audiostream.theme.VioletElectric

@Composable
fun VolumeSlider(
    volume: Int,
    onVolumeChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when {
                            volume == 0 -> Icons.Default.VolumeMute
                            volume < 50 -> Icons.Default.VolumeDown
                            else -> Icons.Default.VolumeUp
                        },
                        contentDescription = "Volume",
                        tint = CyanNeon
                    )
                    Text(
                        text = "MAX98357A Output Volume",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "$volume%",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyanNeon
                )
            }

            Slider(
                value = volume.toFloat(),
                onValueChange = { onVolumeChanged(it.toInt()) },
                valueRange = 0f..100f,
                steps = 100,
                colors = SliderDefaults.colors(
                    thumbColor = CyanNeon,
                    activeTrackColor = CyanNeon,
                    inactiveTrackColor = DarkSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
