package com.fifthsense.audiostream.model

enum class BleConnectionState {
    DISCONNECTED,
    CONNECTING,
    DISCOVERING_SERVICES,
    CONNECTED,
    DISCONNECTING
}

data class BleAudioDevice(
    val name: String,
    val address: String,
    val rssi: Int = -60,
    val isTargetDevice: Boolean = false,
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED
)
