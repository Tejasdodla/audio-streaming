package com.fifthsense.audiostream

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fifthsense.audiostream.components.ModeSelector
import com.fifthsense.audiostream.components.TopHeader
import com.fifthsense.audiostream.model.AudioStreamMode
import com.fifthsense.audiostream.screens.DashboardScreen
import com.fifthsense.audiostream.screens.FileStreamScreen
import com.fifthsense.audiostream.screens.SpeakerModeScreen
import com.fifthsense.audiostream.screens.TtsScreen
import com.fifthsense.audiostream.theme.AudioStreamingTheme
import com.fifthsense.audiostream.theme.DarkBackground
import com.fifthsense.audiostream.viewmodel.AudioStreamViewModel

@Composable
fun App(
    viewModel: AudioStreamViewModel,
    onOpenFilePicker: () -> Unit,
    onToggleSystemAudioCapture: () -> Unit
) {
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val streamStats by viewModel.streamStats.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()
    val volume by viewModel.volume.collectAsState()

    val currentTrack by viewModel.currentTrack.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val isFilePlaying by viewModel.isFilePlaying.collectAsState()
    val playbackPositionMs by viewModel.playbackPositionMs.collectAsState()

    val isCapturingSystemAudio by viewModel.isCapturingSystemAudio.collectAsState()

    val ttsText by viewModel.ttsText.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val speechPitch by viewModel.speechPitch.collectAsState()
    val isTtsSpeaking by viewModel.isTtsSpeaking.collectAsState()

    AudioStreamingTheme {
        Scaffold(
            containerColor = DarkBackground
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(DarkBackground)
            ) {
                // Top App Header
                TopHeader(
                    connectedDevice = connectedDevice,
                    isScanning = isScanning,
                    onScanClicked = {
                        if (isScanning) viewModel.stopBleScan() else viewModel.startBleScan()
                    },
                    onStatusPillClicked = {
                        viewModel.selectMode(AudioStreamMode.DEVICES)
                    }
                )

                // 4 Modes Selector Tab (Devices, Files, Speaker, TTS)
                ModeSelector(
                    currentMode = currentMode,
                    onModeSelected = { viewModel.selectMode(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Active Mode Screen Content
                Box(modifier = Modifier.weight(1f)) {
                    when (currentMode) {
                        AudioStreamMode.DEVICES -> {
                            DashboardScreen(
                                scannedDevices = scannedDevices,
                                connectedDevice = connectedDevice,
                                isScanning = isScanning,
                                stats = streamStats,
                                volume = volume,
                                onScanClicked = {
                                    if (isScanning) viewModel.stopBleScan() else viewModel.startBleScan()
                                },
                                onConnectDevice = { viewModel.connectDevice(it) },
                                onDisconnectDevice = { viewModel.disconnectDevice() },
                                onVolumeChanged = { viewModel.setVolume(it) },
                                onPlayFlashAsset = { viewModel.playFlashAsset(it) }
                            )
                        }

                        AudioStreamMode.FILE_STREAM -> {
                            FileStreamScreen(
                                currentTrack = currentTrack,
                                playlist = playlist,
                                isPlaying = isFilePlaying,
                                playbackPositionMs = playbackPositionMs,
                                volume = volume,
                                onOpenFilePicker = onOpenFilePicker,
                                onTrackSelected = { viewModel.selectTrack(it) },
                                onPlayClicked = { viewModel.playFile() },
                                onPauseClicked = { viewModel.pauseFile() },
                                onStopClicked = { viewModel.stopFile() },
                                onSeekPosition = { viewModel.seekFile(it) },
                                onVolumeChanged = { viewModel.setVolume(it) }
                            )
                        }

                        AudioStreamMode.SYSTEM_SPEAKER -> {
                            SpeakerModeScreen(
                                isCapturing = isCapturingSystemAudio,
                                stats = streamStats,
                                volume = volume,
                                onToggleCapture = onToggleSystemAudioCapture,
                                onVolumeChanged = { viewModel.setVolume(it) }
                            )
                        }

                        AudioStreamMode.TTS_ENGINE -> {
                            TtsScreen(
                                ttsText = ttsText,
                                speechRate = speechRate,
                                speechPitch = speechPitch,
                                isSpeaking = isTtsSpeaking,
                                volume = volume,
                                onTextChanged = { viewModel.setTtsText(it) },
                                onRateChanged = { viewModel.setSpeechRate(it) },
                                onPitchChanged = { viewModel.setSpeechPitch(it) },
                                onSpeakClicked = { viewModel.speakTts() },
                                onStopClicked = { viewModel.stopTts() },
                                onVolumeChanged = { viewModel.setVolume(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
