package com.fifthsense.audiostream.audio

/**
 * IMA-ADPCM (Adaptive Differential Pulse Code Modulation) Audio Encoder
 * 4:1 compression ratio (16-bit PCM @ 16kHz -> 4-bit ADPCM @ 16kHz = 64 kbps)
 */
class ImaAdpcmEncoder {

    companion object {
        private val STEP_TABLE = intArrayOf(
            7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
            19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
            50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
            130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
            337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
            876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066,
            2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
            5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
            15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
        )

        private val INDEX_TABLE = intArrayOf(
            -1, -1, -1, -1, 2, 4, 6, 8,
            -1, -1, -1, -1, 2, 4, 6, 8
        )
    }

    private var predictor: Int = 0
    private var stepIndex: Int = 0

    fun reset() {
        predictor = 0
        stepIndex = 0
    }

    data class AdpcmFrame(
        val initialPredictor: Short,
        val initialIndex: Byte,
        val data: ByteArray
    )

    /**
     * Encodes a chunk of 16-bit linear PCM samples into 4-bit IMA-ADPCM bytes.
     * 240 samples (480 bytes PCM) -> 120 bytes ADPCM (15ms of audio at 16kHz)
     */
    fun encode(pcmSamples: ShortArray, offset: Int = 0, length: Int = pcmSamples.size): AdpcmFrame {
        val numSamples = length
        val adpcmLen = (numSamples + 1) / 2
        val adpcmData = ByteArray(adpcmLen)

        val frameInitPred = predictor.coerceIn(-32768, 32767).toShort()
        val frameInitIndex = stepIndex.coerceIn(0, 88).toByte()

        var adpcmIdx = 0
        var i = 0

        while (i < numSamples) {
            val sample0 = pcmSamples[offset + i].toInt()
            val nibble0 = encodeSample(sample0)

            val nibble1 = if (i + 1 < numSamples) {
                val sample1 = pcmSamples[offset + i + 1].toInt()
                encodeSample(sample1)
            } else {
                0
            }

            adpcmData[adpcmIdx++] = ((nibble1 shl 4) or (nibble0 and 0x0F)).toByte()
            i += 2
        }

        return AdpcmFrame(
            initialPredictor = frameInitPred,
            initialIndex = frameInitIndex,
            data = adpcmData
        )
    }

    private fun encodeSample(sample: Int): Int {
        val step = STEP_TABLE[stepIndex]
        var diff = sample - predictor
        var nibble = 0

        if (diff < 0) {
            nibble = 8 // Sign bit
            diff = -diff
        }

        var delta = step shr 3

        if (diff >= step) {
            nibble = nibble or 4
            diff -= step
            delta += step
        }
        if (diff >= (step shr 1)) {
            nibble = nibble or 2
            diff -= (step shr 1)
            delta += (step shr 1)
        }
        if (diff >= (step shr 2)) {
            nibble = nibble or 1
            delta += (step shr 2)
        }

        if ((nibble and 8) != 0) {
            predictor -= delta
        } else {
            predictor += delta
        }

        predictor = predictor.coerceIn(-32768, 32767)

        stepIndex += INDEX_TABLE[nibble and 0x0F]
        stepIndex = stepIndex.coerceIn(0, 88)

        return nibble
    }
}
