package com.fifthsense.audiostream.model

data class FileAudioItem(
    val id: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val uriString: String,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val sampleRate: Int = 44100,
    val channels: Int = 2
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "$minutes:${seconds.toString().padStart(2, '0')}"
        }
}
