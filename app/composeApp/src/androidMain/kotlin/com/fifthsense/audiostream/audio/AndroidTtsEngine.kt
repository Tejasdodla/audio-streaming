package com.fifthsense.audiostream.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.util.UUID

class AndroidTtsEngine(
    private val context: Context,
    private val audioStreamer: AudioStreamer,
    private val scope: CoroutineScope
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "AndroidTtsEngine"
    }

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "TTS Language US not supported or missing data")
            } else {
                isInitialized = true
                Log.i(TAG, "Android Text-To-Speech engine initialized successfully")
            }
        } else {
            Log.e(TAG, "Failed to initialize Text-To-Speech engine")
        }
    }

    fun speak(text: String, rate: Float = 1.0f, pitch: Float = 1.0f) {
        if (!isInitialized || textToSpeech == null || text.isBlank()) {
            Log.w(TAG, "TTS not ready or text is empty")
            return
        }

        _isSpeaking.value = true

        scope.launch(Dispatchers.IO) {
            try {
                textToSpeech?.setSpeechRate(rate)
                textToSpeech?.setPitch(pitch)

                val utteranceId = UUID.randomUUID().toString()
                val tempFile = File(context.cacheDir, "tts_synth_${utteranceId}.wav")

                val params = Bundle()
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "TTS Synthesis started")
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "TTS Synthesis completed, streaming PCM to nRF54L15...")
                        streamSynthesizedFile(tempFile)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "TTS Synthesis error")
                        _isSpeaking.value = false
                        tempFile.delete()
                    }
                })

                val result = textToSpeech?.synthesizeToFile(text, params, tempFile, utteranceId)
                if (result != TextToSpeech.SUCCESS) {
                    Log.e(TAG, "synthesizeToFile returned error code: $result")
                    _isSpeaking.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in TTS speak", e)
                _isSpeaking.value = false
            }
        }
    }

    private fun streamSynthesizedFile(file: File) {
        scope.launch(Dispatchers.IO) {
            try {
                if (!file.exists() || file.length() < 44) {
                    _isSpeaking.value = false
                    return@launch
                }

                val bytes = file.readBytes()
                file.delete()

                // Parse WAV Header
                if (bytes.size < 44 || bytes[0] != 'R'.code.toByte() || bytes[1] != 'I'.code.toByte()) {
                    // Raw streaming fallback
                    audioStreamer.enqueuePcmChunk(bytes)
                    return@launch
                }

                val channels = (bytes[22].toInt() and 0xFF) or ((bytes[23].toInt() and 0xFF) shl 8)
                val sampleRate = (bytes[24].toInt() and 0xFF) or
                        ((bytes[25].toInt() and 0xFF) shl 8) or
                        ((bytes[26].toInt() and 0xFF) shl 16) or
                        ((bytes[27].toInt() and 0xFF) shl 24)

                // Find 'data' chunk
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

                val pcmDataLength = bytes.size - dataOffset
                if (pcmDataLength <= 0) {
                    _isSpeaking.value = false
                    return@launch
                }

                // If native 16kHz mono, stream in paced 10ms chunks
                if (sampleRate == 16000 && channels == 1) {
                    val rawPcm = bytes.copyOfRange(dataOffset, bytes.size)
                    var offset = 0
                    val chunkSize = 320 // 10.0ms @ 16kHz
                    while (offset < rawPcm.size && _isSpeaking.value) {
                        val len = minOf(chunkSize, rawPcm.size - offset)
                        audioStreamer.enqueuePcmChunk(rawPcm.copyOfRange(offset, offset + len))
                        offset += len
                        delay(9)
                    }
                } else {
                    // Resample to 16kHz mono
                    val numSamples = pcmDataLength / (2 * maxOf(1, channels))
                    val mono16BitSamples = ShortArray(numSamples)
                    var byteIdx = dataOffset

                    for (i in 0 until numSamples) {
                        var sum = 0
                        for (ch in 0 until channels) {
                            if (byteIdx + 1 < bytes.size) {
                                val s = ((bytes[byteIdx].toInt() and 0xFF) or (bytes[byteIdx + 1].toInt() shl 8)).toShort()
                                sum += s
                                byteIdx += 2
                            }
                        }
                        mono16BitSamples[i] = (sum / maxOf(1, channels)).toShort()
                    }

                    // Resample from sampleRate to 16000
                    val targetSampleCount = (numSamples.toLong() * 16000L / maxOf(8000, sampleRate)).toInt()
                    val outputBytes = ByteArray(targetSampleCount * 2)

                    for (i in 0 until targetSampleCount) {
                        val srcIdx = (i.toFloat() * sampleRate.toFloat() / 16000f).toInt().coerceIn(0, numSamples - 1)
                        val sample = mono16BitSamples[srcIdx]
                        outputBytes[i * 2] = (sample.toInt() and 0xFF).toByte()
                        outputBytes[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                    }

                    // Paced streaming in 320-byte chunks (10.0ms @ 16kHz)
                    var offset = 0
                    val chunkSize = 320
                    while (offset < outputBytes.size && _isSpeaking.value) {
                        val len = minOf(chunkSize, outputBytes.size - offset)
                        audioStreamer.enqueuePcmChunk(outputBytes.copyOfRange(offset, offset + len))
                        offset += len
                        delay(9)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error streaming synthesized TTS file", e)
            } finally {
                _isSpeaking.value = false
            }
        }
    }

    fun stop() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }

    fun release() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
