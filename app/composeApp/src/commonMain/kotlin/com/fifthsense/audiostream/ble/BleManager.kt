package com.fifthsense.audiostream.ble

import com.fifthsense.audiostream.model.BleAudioDevice
import com.fifthsense.audiostream.model.BleConnectionState
import com.fifthsense.audiostream.model.StreamStats
import kotlinx.coroutines.flow.StateFlow

interface BleManager {
    val scannedDevices: StateFlow<List<BleAudioDevice>>
    val isScanning: StateFlow<Boolean>
    val connectionState: StateFlow<BleConnectionState>
    val connectedDevice: StateFlow<BleAudioDevice?>
    val streamStats: StateFlow<StreamStats>
    val currentMtu: StateFlow<Int>

    fun startScan()
    fun stopScan()
    fun connect(device: BleAudioDevice)
    fun disconnect()

    suspend fun sendAudioPacket(packet: ByteArray): Boolean
    suspend fun sendControlCommand(commandPacket: ByteArray): Boolean
    fun setVolume(volume: Int)
}
