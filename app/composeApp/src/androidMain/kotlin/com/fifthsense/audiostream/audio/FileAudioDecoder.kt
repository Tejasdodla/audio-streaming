package com.fifthsense.audiostream.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.fifthsense.audiostream.model.FileAudioItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class FileAudioDecoder(
    private val context: Context,
    private val audioStreamer: AudioStreamer,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "FileAudioDecoder"
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private var decodeJob: Job? = null
    private var isPaused = false

    fun playTrack(track: FileAudioItem) {
        stop()
        _isPlaying.value = true
        isPaused = false

        decodeJob = scope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse(track.uriString)

                // 1. WAV files (assets or local files)
                if (track.uriString.endsWith(".wav", ignoreCase = true)) {
                    val inputStream = if (track.uriString.startsWith("content://") || track.uriString.startsWith("file://")) {
                        context.contentResolver.openInputStream(uri)
                    } else {
                        context.assets.open(track.uriString)
                    }

                    if (inputStream != null) {
                        inputStream.use { stream ->
                            val bytes = stream.readBytes()
                            if (bytes.size > 44 && bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte()) {
                                val channels = (bytes[22].toInt() and 0xFF) or ((bytes[23].toInt() and 0xFF) shl 8)
                                val sampleRate = (bytes[24].toInt() and 0xFF) or
                                        ((bytes[25].toInt() and 0xFF) shl 8) or
                                        ((bytes[26].toInt() and 0xFF) shl 16) or
                                        ((bytes[27].toInt() and 0xFF) shl 24)

                                var dataOffset = 12
                                while (dataOffset < bytes.size - 8) {
                                    if (bytes[dataOffset] == 'd'.code.toByte() &&
                                        bytes[dataOffset + 1] == 'a'.code.toByte() &&
                                        bytes[dataOffset + 2] == 't'.code.toByte() &&
                                        bytes[dataOffset + 3] == 'a'.code.toByte()
                                    ) {
                                        dataOffset += 8
                                        break
                                    }
                                    dataOffset++
                                }
                                if (dataOffset >= bytes.size) dataOffset = 44

                                val pcmLen = bytes.size - dataOffset
                                if (pcmLen > 0) {
                                    if (sampleRate == 16000 && channels == 1) {
                                        var offset = dataOffset
                                        val chunkSize = 320 // 10.0 ms @ 16kHz
                                        while (isActive && offset < bytes.size) {
                                            if (isPaused) {
                                                delay(50)
                                                continue
                                            }
                                            val len = minOf(chunkSize, bytes.size - offset)
                                            audioStreamer.enqueuePcmChunk(bytes.copyOfRange(offset, offset + len))
                                            offset += len
                                            _playbackPositionMs.value = ((offset - dataOffset) * 1000L) / 32000L
                                            delay(9)
                                        }
                                    } else {
                                        // Resample & Downmix to 16kHz Mono
                                        val numSamples = pcmLen / (2 * maxOf(1, channels))
                                        val mono16Bit = ShortArray(numSamples)
                                        var bIdx = dataOffset
                                        for (i in 0 until numSamples) {
                                            var sum = 0
                                            for (c in 0 until channels) {
                                                if (bIdx + 1 < bytes.size) {
                                                    val s = ((bytes[bIdx].toInt() and 0xFF) or (bytes[bIdx + 1].toInt() shl 8)).toShort()
                                                    sum += s
                                                    bIdx += 2
                                                }
                                            }
                                            mono16Bit[i] = (sum / maxOf(1, channels)).toShort()
                                        }

                                        val targetCount = (numSamples.toLong() * 16000L / maxOf(8000, sampleRate)).toInt()
                                        val outPcm = ByteArray(targetCount * 2)
                                        for (i in 0 until targetCount) {
                                            val sIdx = (i.toFloat() * sampleRate.toFloat() / 16000f).toInt().coerceIn(0, numSamples - 1)
                                            val sample = mono16Bit[sIdx]
                                            outPcm[i * 2] = (sample.toInt() and 0xFF).toByte()
                                            outPcm[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                                        }

                                        var offset = 0
                                        val chunkSize = 320 // 10.0 ms @ 16kHz
                                        while (isActive && offset < outPcm.size) {
                                            if (isPaused) {
                                                delay(50)
                                                continue
                                            }
                                            val len = minOf(chunkSize, outPcm.size - offset)
                                            audioStreamer.enqueuePcmChunk(outPcm.copyOfRange(offset, offset + len))
                                            offset += len
                                            _playbackPositionMs.value = (offset * 1000L) / 32000L
                                            delay(9)
                                        }
                                    }
                                }
                            }
                        }
                        return@launch
                    }
                }

                // 2. MP3, AAC, FLAC, OGG via MediaExtractor + MediaCodec
                val extractor = MediaExtractor()
                if (track.uriString.startsWith("content://") || track.uriString.startsWith("file://")) {
                    extractor.setDataSource(context, uri, null)
                } else {
                    val afd = context.assets.openFd(track.uriString)
                    extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }

                var audioTrackIndex = -1
                var format: MediaFormat? = null

                for (i in 0 until extractor.trackCount) {
                    val trackFormat = extractor.getTrackFormat(i)
                    val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("audio/")) {
                        audioTrackIndex = i
                        format = trackFormat
                        break
                    }
                }

                if (audioTrackIndex < 0 || format == null) {
                    Log.e(TAG, "No audio track found in file")
                    _isPlaying.value = false
                    extractor.release()
                    return@launch
                }

                extractor.selectTrack(audioTrackIndex)
                val mime = format.getString(MediaFormat.KEY_MIME)!!
                val decoder = MediaCodec.createDecoderByType(mime)
                decoder.configure(format, null, null, 0)
                decoder.start()

                val bufferInfo = MediaCodec.BufferInfo()
                var isEOS = false
                val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
                val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2

                while (isActive && !isEOS) {
                    if (isPaused) {
                        delay(50)
                        continue
                    }

                    val inputBufIndex = decoder.dequeueInputBuffer(10000)
                    if (inputBufIndex >= 0) {
                        val inputBuf = decoder.getInputBuffer(inputBufIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuf, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
                            decoder.queueInputBuffer(inputBufIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }

                    val outputBufIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
                    if (outputBufIndex >= 0) {
                        val outputBuf = decoder.getOutputBuffer(outputBufIndex)!!
                        val pcmChunk = ByteArray(bufferInfo.size)
                        outputBuf.position(bufferInfo.offset)
                        outputBuf.get(pcmChunk, 0, bufferInfo.size)

                        _playbackPositionMs.value = bufferInfo.presentationTimeUs / 1000

                        // Downmix & resample to 16kHz mono
                        val numFrames = bufferInfo.size / (2 * maxOf(1, channelCount))
                        if (numFrames > 0) {
                            val monoBuffer = ShortArray(numFrames)
                            var inByteIdx = 0
                            for (i in 0 until numFrames) {
                                var sum = 0
                                for (ch in 0 until channelCount) {
                                    if (inByteIdx + 1 < pcmChunk.size) {
                                        val s = ((pcmChunk[inByteIdx].toInt() and 0xFF) or (pcmChunk[inByteIdx + 1].toInt() shl 8)).toShort()
                                        sum += s
                                        inByteIdx += 2
                                    }
                                }
                                monoBuffer[i] = (sum / maxOf(1, channelCount)).toShort()
                            }

                            val targetFrames = (numFrames.toLong() * 16000L / maxOf(8000, sampleRate)).toInt()
                            val outBytes = ByteArray(targetFrames * 2)
                            for (i in 0 until targetFrames) {
                                val sIdx = (i.toFloat() * sampleRate.toFloat() / 16000f).toInt().coerceIn(0, numFrames - 1)
                                val s = monoBuffer[sIdx]
                                outBytes[i * 2] = (s.toInt() and 0xFF).toByte()
                                outBytes[i * 2 + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
                            }

                            // Stream decoded frame in paced 320-byte chunks (10.0ms @ 16kHz)
                            var offset = 0
                            val chunkSize = 320
                            while (isActive && offset < outBytes.size && !isPaused && _isPlaying.value) {
                                val len = minOf(chunkSize, outBytes.size - offset)
                                audioStreamer.enqueuePcmChunk(outBytes.copyOfRange(offset, offset + len))
                                offset += len
                                delay(9)
                            }
                        }

                        decoder.releaseOutputBuffer(outputBufIndex, false)
                    }
                }

                decoder.stop()
                decoder.release()
                extractor.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding audio file", e)
            } finally {
                _isPlaying.value = false
            }
        }
    }

    fun pause() {
        isPaused = true
        _isPlaying.value = false
    }

    fun resume() {
        isPaused = false
        _isPlaying.value = true
    }

    fun stop() {
        decodeJob?.cancel()
        decodeJob = null
        isPaused = false
        _isPlaying.value = false
        _playbackPositionMs.value = 0L
    }

    fun seekTo(positionMs: Long) {
        _playbackPositionMs.value = positionMs
    }
}
