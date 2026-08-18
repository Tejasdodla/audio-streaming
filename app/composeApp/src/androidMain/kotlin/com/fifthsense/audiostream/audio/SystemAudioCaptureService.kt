package com.fifthsense.audiostream.audio

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import com.fifthsense.audiostream.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SystemAudioCaptureService : Service() {

    companion object {
        private const val TAG = "SystemAudioCapture"
        private const val CHANNEL_ID = "5th_sense_audio_capture_channel"
        private const val NOTIFICATION_ID = 55415

        const val ACTION_START = "ACTION_START_CAPTURE"
        const val ACTION_STOP = "ACTION_STOP_CAPTURE"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"

        var mediaProjection: MediaProjection? = null
        var audioStreamer: AudioStreamer? = null
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var recordingJob: Job? = null
    private var audioRecord: AudioRecord? = null

    inner class LocalBinder : Binder() {
        fun getService(): SystemAudioCaptureService = this@SystemAudioCaptureService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            buildNotification(),
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, buildNotification())
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                        val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(EXTRA_RESULT_DATA)
                        }

                        if (resultCode != 0 && resultData != null) {
                            val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            val proj = mpManager.getMediaProjection(resultCode, resultData)
                            proj.registerCallback(object : MediaProjection.Callback() {
                                override fun onStop() {
                                    Log.i(TAG, "MediaProjection session stopped")
                                    stopAudioCapture()
                                }
                            }, null)
                            mediaProjection = proj
                        }

                        startAudioCapture()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting foreground service", e)
                }
            }
            ACTION_STOP -> {
                stopAudioCapture()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    private fun startAudioCapture() {
        val projection = mediaProjection ?: run {
            Log.e(TAG, "MediaProjection is null. Cannot start system audio capture.")
            return
        }

        recordingJob?.cancel()
        recordingJob = serviceScope.launch {
            try {
                // PlaybackCapture natively supports 48000Hz or 44100Hz Stereo
                val captureRate = 48000
                val channelConfig = AudioFormat.CHANNEL_IN_STEREO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT

                val config = AudioPlaybackCaptureConfiguration.Builder(projection)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .build()

                val minBufferSize = AudioRecord.getMinBufferSize(captureRate, channelConfig, audioFormat)
                val bufferSize = maxOf(minBufferSize, 8192)

                val format = AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(captureRate)
                    .setChannelMask(channelConfig)
                    .build()

                audioRecord = AudioRecord.Builder()
                    .setAudioPlaybackCaptureConfig(config)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferSize)
                    .build()

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord failed to initialize for playback capture")
                    return@launch
                }

                audioRecord?.startRecording()
                Log.i(TAG, "System Audio Capture started successfully at $captureRate Hz Stereo -> Oboe FIR Engine -> 10ms LC3 Slicer")

                // 10ms buffer size: 480 stereo frames (960 shorts)
                val rawBuffer = ShortArray(960)
                val oboeEngine = OboeAudioEngine(outputSampleRate = 16000, frameDurationMs = 10.0)
                val frame10ms = ShortArray(oboeEngine.samplesPerFrame)
                val outputPcmMono = ByteArray(oboeEngine.samplesPerFrame * 2)

                while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val shortsRead = audioRecord?.read(rawBuffer, 0, rawBuffer.size) ?: 0
                    if (shortsRead > 0) {
                        oboeEngine.pushStereo48k(rawBuffer, shortsRead)

                        while (oboeEngine.hasCompleteFrame()) {
                            if (oboeEngine.popFrame(frame10ms)) {
                                for (i in frame10ms.indices) {
                                    val s = frame10ms[i].toInt()
                                    outputPcmMono[i * 2] = (s and 0xFF).toByte()
                                    outputPcmMono[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
                                }
                                audioStreamer?.enqueuePcmChunk(outputPcmMono, outputPcmMono.size)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in system audio capture loop", e)
            } finally {
                stopAudioCaptureInternal()
            }
        }
    }

    private fun stopAudioCapture() {
        recordingJob?.cancel()
        recordingJob = null
        stopAudioCaptureInternal()
    }

    private fun stopAudioCaptureInternal() {
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
            audioRecord = null
            Log.i(TAG, "System Audio Capture stopped and released")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "5th Sense Audio Streaming",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Capturing phone media playback and streaming to nRF54L15"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("5th Sense • Bluetooth Speaker Active")
            .setContentText("Broadcasting Spotify / Media to nRF54L15 MAX98357A")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopAudioCapture()
        super.onDestroy()
    }
}
