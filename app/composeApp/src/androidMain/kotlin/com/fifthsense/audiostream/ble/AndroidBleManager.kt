package com.fifthsense.audiostream.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.fifthsense.audiostream.model.BleAudioDevice
import com.fifthsense.audiostream.model.BleConnectionState
import com.fifthsense.audiostream.model.StreamStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBleManager(
    private val context: Context,
    private val scope: CoroutineScope
) : BleManager {

    companion object {
        private const val TAG = "AndroidBleManager"
        private val CCC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _scannedDevices = MutableStateFlow<List<BleAudioDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<BleAudioDevice>> = _scannedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BleAudioDevice?>(null)
    override val connectedDevice: StateFlow<BleAudioDevice?> = _connectedDevice.asStateFlow()

    private val _streamStats = MutableStateFlow(StreamStats())
    override val streamStats: StateFlow<StreamStats> = _streamStats.asStateFlow()

    private val _currentMtu = MutableStateFlow(517)
    override val currentMtu: StateFlow<Int> = _currentMtu.asStateFlow()

    private var activeGatt: BluetoothGatt? = null
    private var dataCharacteristic: BluetoothGattCharacteristic? = null
    private var ctrlCharacteristic: BluetoothGattCharacteristic? = null
    private var statsCharacteristic: BluetoothGattCharacteristic? = null

    private val discoveredDevicesMap = java.util.concurrent.ConcurrentHashMap<String, BleAudioDevice>()
    private var scanThrottleJob: kotlinx.coroutines.Job? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val record = result.scanRecord
            val name = record?.deviceName ?: device.name ?: "Unknown Device"
            val address = device.address ?: return
            val rssi = result.rssi

            val isTarget = name.contains("nRF54L15", ignoreCase = true) ||
                    name.contains("Audio-Sink", ignoreCase = true) ||
                    name.contains("5th Sense", ignoreCase = true) ||
                    record?.serviceUuids?.any {
                        it.uuid.toString().equals(BleAudioProtocol.AUDIO_SERVICE_UUID, ignoreCase = true)
                    } == true

            val isNew = !discoveredDevicesMap.containsKey(address)
            val bleDevice = BleAudioDevice(
                name = name,
                address = address,
                rssi = rssi,
                isTargetDevice = isTarget,
                connectionState = if (_connectedDevice.value?.address == address) _connectionState.value else BleConnectionState.DISCONNECTED
            )

            discoveredDevicesMap[address] = bleDevice

            if (isNew && isTarget) {
                flushDiscoveredDevices()
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE Scan failed with code: $errorCode")
            _isScanning.value = false
            scanThrottleJob?.cancel()
        }
    }

    private fun flushDiscoveredDevices() {
        val list = discoveredDevicesMap.values.toList().sortedWith(
            compareByDescending<BleAudioDevice> { it.isTargetDevice }.thenByDescending { it.rssi }
        )
        _scannedDevices.value = list
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "GATT connection state: status=$status, newState=$newState")
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.value = BleConnectionState.CONNECTING
                _connectedDevice.update { it?.copy(connectionState = BleConnectionState.CONNECTING) }
                // Discover services directly
                gatt.discoverServices()
            } else {
                Log.w(TAG, "GATT disconnected or failed with status: $status, newState: $newState")
                _connectionState.value = BleConnectionState.DISCONNECTED
                _connectedDevice.value = null
                cleanupGatt()
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.i(TAG, "MTU changed to: $mtu (status=$status)")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _currentMtu.value = mtu
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Service discovery failed with status: $status")
                _connectionState.value = BleConnectionState.DISCONNECTED
                cleanupGatt()
                return
            }

            val audioService = gatt.getService(UUID.fromString(BleAudioProtocol.AUDIO_SERVICE_UUID))
            if (audioService != null) {
                dataCharacteristic = audioService.getCharacteristic(UUID.fromString(BleAudioProtocol.AUDIO_DATA_CHAR_UUID))
                ctrlCharacteristic = audioService.getCharacteristic(UUID.fromString(BleAudioProtocol.AUDIO_CTRL_CHAR_UUID))
                statsCharacteristic = audioService.getCharacteristic(UUID.fromString(BleAudioProtocol.AUDIO_STATS_CHAR_UUID))

                // Configure characteristics for fast write without response
                dataCharacteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                ctrlCharacteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

                _connectionState.value = BleConnectionState.CONNECTED
                _connectedDevice.update { it?.copy(connectionState = BleConnectionState.CONNECTED) }
                Log.i(TAG, "Audio Service connected and configured successfully!")

                // Request high priority interval (11.25 - 15ms) and 517 MTU
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                gatt.requestMtu(517)
            } else {
                Log.w(TAG, "Audio Service UUID not found in device GATT table")
                _connectionState.value = BleConnectionState.CONNECTED
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            parseStatsCharacteristic(characteristic.value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            parseStatsCharacteristic(value)
        }
    }

    private fun parseStatsCharacteristic(data: ByteArray?) {
        if (data == null || data.size < 8) return
        val bufferPercent = data[0].toInt() and 0xFF
        val volume = data[1].toInt() and 0xFF
        val underruns = (data[2].toInt() and 0xFF) or ((data[3].toInt() and 0xFF) shl 8)
        val packetsRecv = (data[4].toLong() and 0xFF) or
                ((data[5].toLong() and 0xFF) shl 8) or
                ((data[6].toLong() and 0xFF) shl 16) or
                ((data[7].toLong() and 0xFF) shl 24)

        _streamStats.update {
            it.copy(
                bufferPercent = bufferPercent,
                volume = volume,
                underrunCount = underruns,
                packetsReceivedBySink = packetsRecv
            )
        }
    }

    override fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w(TAG, "Bluetooth is not enabled")
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner ?: return
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        _isScanning.value = true
        discoveredDevicesMap.clear()
        _scannedDevices.value = emptyList()

        scanThrottleJob?.cancel()
        scanThrottleJob = scope.launch(Dispatchers.Default) {
            while (isActive && _isScanning.value) {
                delay(250)
                flushDiscoveredDevices()
            }
        }

        try {
            // Passing null allows scanner to capture all discoverable BLE advertising packets without hardware filter blockage
            scanner.startScan(null, settings, scanCallback)
            Log.d(TAG, "BLE Scan started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting BLE scan", e)
            _isScanning.value = false
            scanThrottleJob?.cancel()
        }
    }

    override fun stopScan() {
        scanThrottleJob?.cancel()
        scanThrottleJob = null
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        if (_isScanning.value) {
            scanner.stopScan(scanCallback)
            _isScanning.value = false
            Log.d(TAG, "BLE Scan stopped")
        }
    }

    override fun connect(device: BleAudioDevice) {
        stopScan()
        cleanupGatt()
        val btDevice = bluetoothAdapter?.getRemoteDevice(device.address) ?: return

        _connectionState.value = BleConnectionState.CONNECTING
        _connectedDevice.value = device.copy(connectionState = BleConnectionState.CONNECTING)

        activeGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            btDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            btDevice.connectGatt(context, false, gattCallback)
        }
    }

    override fun disconnect() {
        activeGatt?.disconnect()
    }

    private fun cleanupGatt() {
        try {
            activeGatt?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing GATT", e)
        }
        activeGatt = null
        dataCharacteristic = null
        ctrlCharacteristic = null
        statsCharacteristic = null
    }

    override suspend fun sendAudioPacket(packet: ByteArray): Boolean {
        val gatt = activeGatt ?: return false
        val char = dataCharacteristic ?: return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val result = gatt.writeCharacteristic(
                char,
                packet,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            )
            result == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            char.value = packet
            @Suppress("DEPRECATION")
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(char)
        }
    }

    override suspend fun sendControlCommand(commandPacket: ByteArray): Boolean {
        val gatt = activeGatt ?: return false
        val char = ctrlCharacteristic ?: return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val result = gatt.writeCharacteristic(
                char,
                commandPacket,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            )
            result == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            char.value = commandPacket
            @Suppress("DEPRECATION")
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(char)
        }
    }

    override fun setVolume(volume: Int) {
        scope.launch(Dispatchers.IO) {
            val packet = BleAudioProtocol.createVolumePacket(volume)
            sendControlCommand(packet)
            _streamStats.update { it.copy(volume = volume) }
        }
    }
}
