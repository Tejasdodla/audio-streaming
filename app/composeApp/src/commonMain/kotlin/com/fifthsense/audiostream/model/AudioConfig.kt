package com.fifthsense.audiostream.model

enum class SampleRate(val hz: Int, val label: String) {
    RATE_16000(16000, "16 kHz (Voice/Fast)"),
    RATE_24000(24000, "24 kHz (HQ Speech)"),
    RATE_44100(44100, "44.1 kHz (CD Audio)"),
    RATE_48000(48000, "48 kHz (Studio Audio)")
}

enum class AudioCodec(val codecId: Byte, val label: String) {
    LC3(0x04, "LC3 (Bluetooth LE Audio Standard)"),
    ADPCM(0x01, "IMA-ADPCM (Legacy)"),
    PCM(0x00, "Raw PCM 16-bit")
}

enum class Lc3Bitrate(val kbps: Int, val frameBytes: Int, val label: String) {
    BITRATE_24K(24, 30, "24 kbps (Fast Voice)"),
    BITRATE_32K(32, 40, "32 kbps (Standard HQ - Recommended)"),
    BITRATE_48K(48, 60, "48 kbps (High Fidelity)"),
    BITRATE_64K(64, 80, "64 kbps (Studio Audio)")
}

data class AudioConfig(
    val sampleRate: SampleRate = SampleRate.RATE_16000,
    val channels: Int = 1, // 1 = Mono, 2 = Stereo
    val bitDepth: Int = 16,
    val codec: AudioCodec = AudioCodec.LC3,
    val lc3Bitrate: Lc3Bitrate = Lc3Bitrate.BITRATE_32K,
    val volume: Int = 80,
    val chunkSizeBytes: Int = 480
)
