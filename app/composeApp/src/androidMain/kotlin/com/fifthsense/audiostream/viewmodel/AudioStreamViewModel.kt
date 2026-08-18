package com.fifthsense.audiostream.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fifthsense.audiostream.audio.AudioStreamer
import com.fifthsense.audiostream.audio.FileAudioDecoder
import com.fifthsense.audiostream.audio.AndroidTtsEngine
import com.fifthsense.audiostream.ble.BleAudioProtocol
import com.fifthsense.audiostream.ble.BleManager
import com.fifthsense.audiostream.model.AudioConfig
import com.fifthsense.audiostream.model.AudioStreamMode
import com.fifthsense.audiostream.model.BleAudioDevice
import com.fifthsense.audiostream.model.FileAudioItem
import com.fifthsense.audiostream.model.SampleRate
import com.fifthsense.audiostream.model.StreamStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AudioStreamViewModel(
    private val bleManager: BleManager,
    private val audioStreamer: AudioStreamer,
    private val fileDecoder: FileAudioDecoder,
    private val ttsEngine: AndroidTtsEngine
) : ViewModel() {

    val scannedDevices = bleManager.scannedDevices
    val isScanning = bleManager.isScanning
    val connectionState = bleManager.connectionState
    val connectedDevice = bleManager.connectedDevice

    // Merge BLE sink telemetry and local transmitter metrics
    val streamStats: StateFlow<StreamStats> = combine(
        bleManager.streamStats,
        audioStreamer.streamStats
    ) { sinkStats, txStats ->
        StreamStats(
            bufferPercent = sinkStats.bufferPercent,
            underrunCount = sinkStats.underrunCount,
            packetsSent = txStats.packetsSent,
            packetsReceivedBySink = sinkStats.packetsReceivedBySink,
            bytesSent = txStats.bytesSent,
            kbps = txStats.kbps,
            latencyMs = 15,
            isStreaming = txStats.isStreaming,
            activeSampleRate = txStats.activeSampleRate,
            volume = sinkStats.volume
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StreamStats())

    // Active Audio Config (LC3 Codec & Bitrate)
    private val _audioConfig = MutableStateFlow(AudioConfig())
    val audioConfig: StateFlow<AudioConfig> = _audioConfig.asStateFlow()

    fun setLc3Bitrate(bitrate: com.fifthsense.audiostream.model.Lc3Bitrate) {
        _audioConfig.value = _audioConfig.value.copy(lc3Bitrate = bitrate)
    }

    fun setSampleRate(sampleRate: SampleRate) {
        _audioConfig.value = _audioConfig.value.copy(sampleRate = sampleRate)
    }

    // Active Mode
    private val _currentMode = MutableStateFlow(AudioStreamMode.DEVICES)
    val currentMode: StateFlow<AudioStreamMode> = _currentMode.asStateFlow()

    // Volume
    private val _volume = MutableStateFlow(80)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    // File Streaming State
    private val _currentTrack = MutableStateFlow<FileAudioItem?>(null)
    val currentTrack: StateFlow<FileAudioItem?> = _currentTrack.asStateFlow()

    val isFilePlaying = fileDecoder.isPlaying
    val playbackPositionMs = fileDecoder.playbackPositionMs

    private val _playlist = MutableStateFlow<List<FileAudioItem>>(
        listOf(
            FileAudioItem(
                id = "sample_1",
                title = "Cyber Synth Pulse 16kHz",
                artist = "5th Sense Audio Lab",
                uriString = "sample_cyber_pulse.wav",
                durationMs = 45000,
                sizeBytes = 1440000,
                sampleRate = 16000,
                channels = 1
            ),
            FileAudioItem(
                id = "sample_2",
                title = "Nordic Horizon Ambient",
                artist = "Nordic nRF54L15 Showcase",
                uriString = "sample_nordic_horizon.wav",
                durationMs = 60000,
                sizeBytes = 1920000,
                sampleRate = 16000,
                channels = 1
            )
        )
    )
    val playlist: StateFlow<List<FileAudioItem>> = _playlist.asStateFlow()

    // System Speaker State
    private val _isCapturingSystemAudio = MutableStateFlow(false)
    val isCapturingSystemAudio: StateFlow<Boolean> = _isCapturingSystemAudio.asStateFlow()

    // TTS State
    private val _ttsText = MutableStateFlow("Hello from nRF54L15 and MAX98357A!")
    val ttsText: StateFlow<String> = _ttsText.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(1.0f)
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    val isTtsSpeaking = ttsEngine.isSpeaking

    // BLE Actions
    fun startBleScan() = bleManager.startScan()
    fun stopBleScan() = bleManager.stopScan()
    fun connectDevice(device: BleAudioDevice) = bleManager.connect(device)
    fun disconnectDevice() = bleManager.disconnect()

    fun selectMode(mode: AudioStreamMode) {
        if (_currentMode.value != mode) {
            stopAllStreaming()
            _currentMode.value = mode
        }
    }

    fun setVolume(volume: Int) {
        _volume.value = volume
        bleManager.setVolume(volume)
    }

    fun playFlashAsset(assetId: Int) {
        viewModelScope.launch {
            val packet = BleAudioProtocol.createPlayFlashAssetPacket(assetId)
            bleManager.sendControlCommand(packet)
        }
    }

    // File Actions
    fun selectTrack(track: FileAudioItem) {
        _currentTrack.value = track
        playFile()
    }

    fun addCustomFileTrack(item: FileAudioItem) {
        _playlist.value = listOf(item) + _playlist.value
        selectTrack(item)
    }

    fun playFile() {
        val track = _currentTrack.value ?: _playlist.value.firstOrNull() ?: return
        _currentTrack.value = track
        audioStreamer.startStreaming(
            config = _audioConfig.value,
            mode = AudioStreamMode.FILE_STREAM
        )
        fileDecoder.playTrack(track)
    }

    fun pauseFile() {
        fileDecoder.pause()
    }

    fun resumeFile() {
        fileDecoder.resume()
    }

    fun stopFile() {
        fileDecoder.stop()
        audioStreamer.stopStreaming()
    }

    fun seekFile(positionMs: Long) {
        fileDecoder.seekTo(positionMs)
    }

    // Speaker Capture Actions
    fun setSystemAudioCapturing(capturing: Boolean) {
        _isCapturingSystemAudio.value = capturing
        if (capturing) {
            audioStreamer.startStreaming(
                config = _audioConfig.value,
                mode = AudioStreamMode.SYSTEM_SPEAKER
            )
        } else {
            audioStreamer.stopStreaming()
        }
    }

    // TTS Actions
    fun setTtsText(text: String) { _ttsText.value = text }
    fun setSpeechRate(rate: Float) { _speechRate.value = rate }
    fun setSpeechPitch(pitch: Float) { _speechPitch.value = pitch }

    fun speakTts() {
        if (_ttsText.value.isNotBlank()) {
            audioStreamer.startStreaming(
                config = _audioConfig.value,
                mode = AudioStreamMode.TTS_ENGINE
            )
            ttsEngine.speak(_ttsText.value, _speechRate.value, _speechPitch.value)
        }
    }

    fun stopTts() {
        ttsEngine.stop()
        audioStreamer.stopStreaming()
    }

    private fun stopAllStreaming() {
        stopFile()
        setSystemAudioCapturing(false)
        stopTts()
    }
}
