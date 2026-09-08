package com.fifthsense.audiostream.audio

import com.fifthsense.audiostream.ble.BleAudioProtocol
import com.fifthsense.audiostream.ble.BleManager
import com.fifthsense.audiostream.model.AudioConfig
import com.fifthsense.audiostream.model.AudioStreamMode
import com.fifthsense.audiostream.model.StreamStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class AudioStreamer(
    private val bleManager: BleManager,
    private val scope: CoroutineScope
) {
    private val _streamStats = MutableStateFlow(StreamStats())
    val streamStats: StateFlow<StreamStats> = _streamStats.asStateFlow()

    private val audioPacketQueue = Channel<ByteArray>(capacity = 32)
    private var sequenceNumber = AtomicInteger(0)
    private var senderJob: Job? = null
    private var throughputTimerJob: Job? = null

    private var bytesSentWindow = 0L
    private var totalBytesSent = 0L
    private var totalPacketsSent = 0L

    private var activeConfig = AudioConfig()
    private var lc3Encoder = Lc3Encoder(16000, 10000, 32)
    private val adpcmEncoder = ImaAdpcmEncoder()
    private val oboeEngine = OboeAudioEngine(16000, 10.0)

    private val sampleAccumulator = ArrayList<Short>(1024)
    private val accumulatorLock = Any()

    fun startStreaming(config: AudioConfig, mode: AudioStreamMode) {
        scope.launch(Dispatchers.IO) {
            senderJob?.cancel()
            throughputTimerJob?.cancel()
            while (audioPacketQueue.tryReceive().isSuccess) {}

            synchronized(accumulatorLock) {
                sampleAccumulator.clear()
            }

            activeConfig = config
            lc3Encoder = Lc3Encoder(
                sampleRateHz = config.sampleRate.hz,
                frameDurationUs = 10000,
                targetBitrateKbps = config.lc3Bitrate.kbps
            )
            lc3Encoder.reset()
            adpcmEncoder.reset()
            oboeEngine.reset()

            // 1. Send Config Packet to nRF54L15
            val configPacket = BleAudioProtocol.createConfigPacket(
                sampleRate = config.sampleRate.hz,
                channels = config.channels,
                bitDepth = config.bitDepth,
                streamMode = mode.modeId
            )
            bleManager.sendControlCommand(configPacket)
            delay(25)

            // 2. Send Start Command
            val startPacket = BleAudioProtocol.createCommandPacket(BleAudioProtocol.CMD_START)
            bleManager.sendControlCommand(startPacket)

            sequenceNumber.set(0)
            bytesSentWindow = 0L
            totalBytesSent = 0L
            totalPacketsSent = 0L

            _streamStats.update {
                it.copy(
                    isStreaming = true,
                    activeSampleRate = config.sampleRate.hz,
                    packetsSent = 0,
                    bytesSent = 0,
                    kbps = 0.0
                )
            }

            startSenderLoop()
            startThroughputMonitor()
        }
    }

    fun stopStreaming() {
        scope.launch(Dispatchers.IO) {
            val stopPacket = BleAudioProtocol.createCommandPacket(BleAudioProtocol.CMD_STOP)
            bleManager.sendControlCommand(stopPacket)

            senderJob?.cancel()
            throughputTimerJob?.cancel()
            senderJob = null
            throughputTimerJob = null

            synchronized(accumulatorLock) {
                sampleAccumulator.clear()
            }
            oboeEngine.reset()

            while (audioPacketQueue.tryReceive().isSuccess) {}

            _streamStats.update { it.copy(isStreaming = false, kbps = 0.0) }
        }
    }

    fun enqueuePcmChunk(pcmData: ByteArray, length: Int = pcmData.size) {
        val totalSamples = length / 2
        if (totalSamples == 0) return

        val incomingShorts = ShortArray(totalSamples)
        java.nio.ByteBuffer.wrap(pcmData, 0, length)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
            .get(incomingShorts)

        if (senderJob == null || senderJob?.isActive != true) {
            startSenderLoop()
            startThroughputMonitor()
            _streamStats.update { it.copy(isStreaming = true, activeSampleRate = activeConfig.sampleRate.hz) }
        }

        val framesToSend = ArrayList<ByteArray>()
        val samplesPerFrame = lc3Encoder.samplesPerFrame

        synchronized(accumulatorLock) {
            for (s in incomingShorts) {
                sampleAccumulator.add(s)
            }

            while (sampleAccumulator.size >= samplesPerFrame) {
                val frameSamples = ShortArray(samplesPerFrame)
                for (i in 0 until samplesPerFrame) {
                    frameSamples[i] = sampleAccumulator[i]
                }
                sampleAccumulator.subList(0, samplesPerFrame).clear()

                val seq = sequenceNumber.getAndIncrement() and 0xFF

                if (activeConfig.codec == com.fifthsense.audiostream.model.AudioCodec.LC3) {
                    val frameBytes = activeConfig.lc3Bitrate.frameBytes
                    val lc3Data = lc3Encoder.encodeFrame(frameSamples, 0, frameBytes)
                    val packet = BleAudioProtocol.createLc3DataPacket(
                        seqNum = seq,
                        lc3Data = lc3Data
                    )
                    framesToSend.add(packet)
                } else if (activeConfig.codec == com.fifthsense.audiostream.model.AudioCodec.ADPCM) {
                    val adpcmFrame = adpcmEncoder.encode(frameSamples, 0, samplesPerFrame)
                    val packet = BleAudioProtocol.createAdpcmDataPacket(
                        seqNum = seq,
                        initIndex = adpcmFrame.initialIndex,
                        initPred = adpcmFrame.initialPredictor,
                        adpcmData = adpcmFrame.data
                    )
                    framesToSend.add(packet)
                } else {
                    val pcmBytes = ByteArray(samplesPerFrame * 2)
                    for (i in 0 until samplesPerFrame) {
                        pcmBytes[i * 2] = (frameSamples[i].toInt() and 0xFF).toByte()
                        pcmBytes[i * 2 + 1] = ((frameSamples[i].toInt() shr 8) and 0xFF).toByte()
                    }
                    val packet = BleAudioProtocol.createAudioDataPacket(seq, pcmBytes, 0, pcmBytes.size)
                    framesToSend.add(packet)
                }
            }
        }

        for (packet in framesToSend) {
            val sent = audioPacketQueue.trySend(packet)
            if (!sent.isSuccess) {
                audioPacketQueue.tryReceive()
                audioPacketQueue.trySend(packet)
            }
        }
    }

    private fun startSenderLoop() {
        senderJob?.cancel()
        senderJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val packet = audioPacketQueue.receive()
                val success = bleManager.sendAudioPacket(packet)

                if (success) {
                    totalPacketsSent++
                    totalBytesSent += packet.size
                    bytesSentWindow += packet.size

                    _streamStats.update {
                        it.copy(
                            packetsSent = totalPacketsSent,
                            bytesSent = totalBytesSent
                        )
                    }
                }

                // Pace at 9.5 ms (stays ahead of 10.0 ms DAC output without overflow)
                delay(9)
            }
        }
    }

    private fun startThroughputMonitor() {
        throughputTimerJob?.cancel()
        throughputTimerJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(1000)
                val kbps = (bytesSentWindow * 8.0) / 1000.0
                bytesSentWindow = 0L

                _streamStats.update { it.copy(kbps = kbps) }
            }
        }
    }
}
