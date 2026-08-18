package com.fifthsense.audiostream.model

enum class AudioStreamMode(
    val title: String,
    val subtitle: String,
    val modeId: Byte
) {
    DEVICES(
        title = "Devices",
        subtitle = "Scan & Connect nRF54L15 Sink",
        modeId = 0xFF.toByte()
    ),
    FILE_STREAM(
        title = "File Player",
        subtitle = "Stream WAV, MP3 & FLAC from device storage",
        modeId = 0x00
    ),
    SYSTEM_SPEAKER(
        title = "Bluetooth Speaker",
        subtitle = "Broadcast all phone media (Spotify, YouTube, Games)",
        modeId = 0x01
    ),
    TTS_ENGINE(
        title = "Text To Speech",
        subtitle = "Synthesize live speech and broadcast to nRF54L15",
        modeId = 0x02
    )
}

