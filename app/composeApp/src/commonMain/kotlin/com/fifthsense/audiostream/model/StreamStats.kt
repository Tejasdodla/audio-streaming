package com.fifthsense.audiostream.model

data class StreamStats(
    val bufferPercent: Int = 0,
    val underrunCount: Int = 0,
    val packetsSent: Long = 0L,
    val packetsReceivedBySink: Long = 0L,
    val bytesSent: Long = 0L,
    val kbps: Double = 0.0,
    val latencyMs: Int = 20,
    val isStreaming: Boolean = false,
    val activeSampleRate: Int = 16000,
    val volume: Int = 80
)
