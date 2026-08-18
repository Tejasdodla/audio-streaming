package com.fifthsense.audiostream.ble

object BleAudioProtocol {
    // 128-bit UUID Strings matching Firmware
    const val AUDIO_SERVICE_UUID = "5f550001-8b43-4f1e-9827-00554c150000"
    const val AUDIO_DATA_CHAR_UUID = "5f550002-8b43-4f1e-9827-00554c150000"
    const val AUDIO_CTRL_CHAR_UUID = "5f550003-8b43-4f1e-9827-00554c150000"
    const val AUDIO_STATS_CHAR_UUID = "5f550004-8b43-4f1e-9827-00554c150000"

    // Protocol Opcodes
    const val OP_DATA: Byte = 0x01
    const val OP_CONTROL: Byte = 0x02
    const val OP_STATS_REQ: Byte = 0x03

    // Control Commands
    const val CMD_START: Byte = 0x10
    const val CMD_STOP: Byte = 0x11
    const val CMD_PAUSE: Byte = 0x12
    const val CMD_RESUME: Byte = 0x13
    const val CMD_CONFIG: Byte = 0x14
    const val CMD_SET_VOLUME: Byte = 0x15
    const val CMD_PLAY_FLASH_ASSET: Byte = 0x16

    // Audio Codecs
    const val CODEC_RAW_PCM: Byte = 0x00
    const val CODEC_ADPCM: Byte = 0x01
    const val CODEC_OPUS: Byte = 0x02
    const val CODEC_SBC: Byte = 0x03
    const val CODEC_LC3: Byte = 0x04

    /**
     * Builds an LC3 standard audio data packet (6-byte header + N bytes LC3 compressed frame):
     * [0: OP_DATA (0x01)] [1: seq_num (0-255)] [2: CODEC_LC3 (0x04)] [3: frame_len] [4..5: 0] [6..N: LC3 bytes]
     */
    fun createLc3DataPacket(seqNum: Int, lc3Data: ByteArray, offset: Int = 0, length: Int = lc3Data.size): ByteArray {
        val packet = ByteArray(6 + length)
        packet[0] = OP_DATA
        packet[1] = (seqNum and 0xFF).toByte()
        packet[2] = CODEC_LC3
        packet[3] = (length and 0xFF).toByte()
        packet[4] = 0
        packet[5] = 0
        System.arraycopy(lc3Data, offset, packet, 6, length)
        return packet
    }

    /**
     * Builds an ADPCM audio data packet (6-byte header + N bytes ADPCM):
     * [0: OP_DATA (0x01)] [1: seq_num (0-255)] [2: CODEC_ADPCM (0x01)] [3: init_index] [4..5: init_pred LE] [6..N: ADPCM bytes]
     */
    fun createAdpcmDataPacket(seqNum: Int, initIndex: Byte, initPred: Short, adpcmData: ByteArray, offset: Int = 0, length: Int = adpcmData.size): ByteArray {
        val packet = ByteArray(6 + length)
        packet[0] = OP_DATA
        packet[1] = (seqNum and 0xFF).toByte()
        packet[2] = CODEC_ADPCM
        packet[3] = initIndex
        packet[4] = (initPred.toInt() and 0xFF).toByte()
        packet[5] = ((initPred.toInt() shr 8) and 0xFF).toByte()
        System.arraycopy(adpcmData, offset, packet, 6, length)
        return packet
    }

    /**
     * Builds a raw PCM audio data packet with sequence header:
     * [0: OP_DATA (0x01)] [1: seq_num (0-255)] [2: CODEC_RAW_PCM (0x00)] [3: 0] [4..5: 0] [6..N: PCM bytes]
     */
    fun createAudioDataPacket(seqNum: Int, pcmData: ByteArray, offset: Int, length: Int): ByteArray {
        val packet = ByteArray(6 + length)
        packet[0] = OP_DATA
        packet[1] = (seqNum and 0xFF).toByte()
        packet[2] = CODEC_RAW_PCM
        packet[3] = 0
        packet[4] = 0
        packet[5] = 0
        System.arraycopy(pcmData, offset, packet, 6, length)
        return packet
    }

    /**
     * Builds a configuration control packet:
     * [CMD_CONFIG] [sampleRate: uint32 LE] [channels: uint8] [bitDepth: uint8] [codec: uint8] [streamMode: uint8]
     */
    fun createConfigPacket(sampleRate: Int, channels: Int, bitDepth: Int, streamMode: Byte): ByteArray {
        val packet = ByteArray(9)
        packet[0] = CMD_CONFIG
        // Sample rate (32-bit LE)
        packet[1] = (sampleRate and 0xFF).toByte()
        packet[2] = ((sampleRate shr 8) and 0xFF).toByte()
        packet[3] = ((sampleRate shr 16) and 0xFF).toByte()
        packet[4] = ((sampleRate shr 24) and 0xFF).toByte()
        // Channels (1 = Mono, 2 = Stereo)
        packet[5] = (channels and 0xFF).toByte()
        // Bit depth (16)
        packet[6] = (bitDepth and 0xFF).toByte()
        // Codec (0 = PCM)
        packet[7] = 0x00
        // Stream mode
        packet[8] = streamMode
        return packet
    }

    /**
     * Builds a volume command packet: [CMD_SET_VOLUME] [volume: 0..100]
     */
    fun createVolumePacket(volumePercent: Int): ByteArray {
        return byteArrayOf(CMD_SET_VOLUME, (volumePercent.coerceIn(0, 100) and 0xFF).toByte())
    }

    /**
     * Builds standard playback control packet (START, STOP, PAUSE, RESUME)
     */
    fun createCommandPacket(command: Byte): ByteArray {
        return byteArrayOf(command)
    }

    /**
     * Builds flash asset playback command
     */
    fun createPlayFlashAssetPacket(assetId: Int): ByteArray {
        return byteArrayOf(CMD_PLAY_FLASH_ASSET, (assetId and 0xFF).toByte())
    }
}
