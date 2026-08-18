package com.fifthsense.audiostream.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.fifthsense.audiostream.theme.CyanNeon
import com.fifthsense.audiostream.theme.DarkBackground
import com.fifthsense.audiostream.theme.DarkSurfaceVariant
import com.fifthsense.audiostream.theme.TextMuted
import com.fifthsense.audiostream.theme.TextPrimary
import com.fifthsense.audiostream.theme.TextSecondary
import com.fifthsense.audiostream.theme.VioletElectric

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TtsScreen(
    ttsText: String,
    speechRate: Float,
    speechPitch: Float,
    isSpeaking: Boolean,
    volume: Int,
    onTextChanged: (String) -> Unit,
    onRateChanged: (Float) -> Unit,
    onPitchChanged: (Float) -> Unit,
    onSpeakClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onVolumeChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val samplePhrases = listOf(
        "Hello from nRF54L15 and MAX98357A!",
        "Audio streaming online with low latency.",
        "Wireless speech broadcast test successful."
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // TTS Input Card
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
                        Text(
                            text = "TEXT TO SPEECH",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanNeon
                        )
                        if (ttsText.isNotEmpty()) {
                            IconButton(
                                onClick = { onTextChanged("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = ttsText,
                        onValueChange = onTextChanged,
                        placeholder = {
                            Text("Type text to speak wirelessly to nRF54L15...", color = TextMuted)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = Color(0x2200F5D4),
                            focusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.3f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )

                    // Quick Phrases
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        samplePhrases.forEach { phrase ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x15FFFFFF))
                                    .clickable { onTextChanged(phrase) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = phrase,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Speak Action Button
                    Button(
                        onClick = { if (isSpeaking) onStopClicked() else onSpeakClicked() },
                        enabled = ttsText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSpeaking) com.fifthsense.audiostream.theme.CoralAccent else CyanNeon,
                            contentColor = DarkBackground,
                            disabledContainerColor = DarkSurfaceVariant,
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isSpeaking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = DarkBackground,
                                    strokeWidth = 2.dp
                                )
                                Text("Stop Speaking")
                            } else {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Speak on nRF54L15")
                            }
                        }
                    }

                    AudioVisualizer(
                        isPlaying = isSpeaking,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Voice Sliders (Speed & Pitch)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "VOICE CONTROLS",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanNeon
                    )

                    // Speech Rate
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Speed", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("${(speechRate * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = CyanNeon)
                        }
                        Slider(
                            value = speechRate,
                            onValueChange = onRateChanged,
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanNeon,
                                activeTrackColor = CyanNeon,
                                inactiveTrackColor = DarkSurfaceVariant
                            )
                        )
                    }

                    // Speech Pitch
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pitch", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("${(speechPitch * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = CyanNeon)
                        }
                        Slider(
                            value = speechPitch,
                            onValueChange = onPitchChanged,
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanNeon,
                                activeTrackColor = CyanNeon,
                                inactiveTrackColor = DarkSurfaceVariant
                            )
                        )
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
    }
}
