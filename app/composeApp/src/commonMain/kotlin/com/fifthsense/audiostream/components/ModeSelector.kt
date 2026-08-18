package com.fifthsense.audiostream.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fifthsense.audiostream.model.AudioStreamMode
import com.fifthsense.audiostream.theme.CyanNeon
import com.fifthsense.audiostream.theme.DarkCardBg
import com.fifthsense.audiostream.theme.DarkSurfaceVariant
import com.fifthsense.audiostream.theme.TextMuted
import com.fifthsense.audiostream.theme.TextPrimary
import com.fifthsense.audiostream.theme.VioletElectric

@Composable
fun ModeSelector(
    currentMode: AudioStreamMode,
    onModeSelected: (AudioStreamMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.6f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AudioStreamMode.entries.forEach { mode ->
            val isSelected = mode == currentMode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) CyanNeon.copy(alpha = 0.15f) else Color.Transparent
                    )
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 10.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = when (mode) {
                            AudioStreamMode.DEVICES -> Icons.Default.GraphicEq
                            AudioStreamMode.FILE_STREAM -> Icons.Default.InsertDriveFile
                            AudioStreamMode.SYSTEM_SPEAKER -> Icons.Default.Speaker
                            AudioStreamMode.TTS_ENGINE -> Icons.Default.RecordVoiceOver
                        },
                        contentDescription = mode.title,
                        tint = if (isSelected) CyanNeon else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = when (mode) {
                            AudioStreamMode.DEVICES -> "Devices"
                            AudioStreamMode.FILE_STREAM -> "Files"
                            AudioStreamMode.SYSTEM_SPEAKER -> "Speaker"
                            AudioStreamMode.TTS_ENGINE -> "TTS"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) CyanNeon else TextMuted
                    )
                }
            }
        }
    }
}
