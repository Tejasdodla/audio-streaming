package com.fifthsense.audiostream

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.fifthsense.audiostream.audio.AndroidTtsEngine
import com.fifthsense.audiostream.audio.AudioStreamer
import com.fifthsense.audiostream.audio.FileAudioDecoder
import com.fifthsense.audiostream.audio.SystemAudioCaptureService
import com.fifthsense.audiostream.ble.AndroidBleManager
import com.fifthsense.audiostream.model.FileAudioItem
import com.fifthsense.audiostream.viewmodel.AudioStreamViewModel
import java.util.UUID

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var bleManager: AndroidBleManager
    private lateinit var audioStreamer: AudioStreamer
    private lateinit var fileDecoder: FileAudioDecoder
    private lateinit var ttsEngine: AndroidTtsEngine
    private lateinit var viewModel: AudioStreamViewModel

    private lateinit var mediaProjectionManager: MediaProjectionManager

    // Permission launcher for Bluetooth & Audio
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            bleManager.startScan()
        } else {
            Toast.makeText(this, "Bluetooth & Audio permissions required for streaming", Toast.LENGTH_LONG).show()
        }
    }

    // MediaProjection launcher for System Audio (Spotify/Music) Capture
    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            SystemAudioCaptureService.audioStreamer = audioStreamer

            val serviceIntent = Intent(this, SystemAudioCaptureService::class.java).apply {
                action = SystemAudioCaptureService.ACTION_START
                putExtra(SystemAudioCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(SystemAudioCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            viewModel.setSystemAudioCapturing(true)
            Toast.makeText(this, "Speaker Mode Active! Streaming audio to nRF54L15", Toast.LENGTH_LONG).show()
        } else {
            viewModel.setSystemAudioCapturing(false)
            Toast.makeText(this, "Audio capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Storage file picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val track = resolveAudioFile(uri)
            viewModel.addCustomFileTrack(track)
            Toast.makeText(this, "Loaded: ${track.title}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // Initialize Audio & BLE Engine
        bleManager = AndroidBleManager(applicationContext, lifecycleScope)
        audioStreamer = AudioStreamer(bleManager, lifecycleScope)
        fileDecoder = FileAudioDecoder(applicationContext, audioStreamer, lifecycleScope)
        ttsEngine = AndroidTtsEngine(applicationContext, audioStreamer, lifecycleScope)

        viewModel = AudioStreamViewModel(bleManager, audioStreamer, fileDecoder, ttsEngine)

        // Request Permissions
        checkAndRequestPermissions()

        setContent {
            App(
                viewModel = viewModel,
                onOpenFilePicker = { filePickerLauncher.launch("audio/*") },
                onToggleSystemAudioCapture = { toggleSystemAudioCapture() }
            )
        }
    }

    private fun toggleSystemAudioCapture() {
        if (viewModel.isCapturingSystemAudio.value) {
            val stopIntent = Intent(this, SystemAudioCaptureService::class.java).apply {
                action = SystemAudioCaptureService.ACTION_STOP
            }
            startService(stopIntent)
            viewModel.setSystemAudioCapturing(false)
        } else {
            // Launch Android MediaProjection consent prompt
            val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
            mediaProjectionLauncher.launch(captureIntent)
        }
    }

    private fun resolveAudioFile(uri: Uri): FileAudioItem {
        var name = "Selected Audio Track"
        var size = 0L

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = cursor.getString(nameIndex)
                if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
            }
        }

        var durationMs = 0L
        var artist = "Local File"
        try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(applicationContext, uri)
            val dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val art = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            if (dur != null) durationMs = dur.toLong()
            if (art != null) artist = art
            mmr.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to retrieve audio metadata", e)
        }

        return FileAudioItem(
            id = UUID.randomUUID().toString(),
            title = name.substringBeforeLast("."),
            artist = artist,
            uriString = uri.toString(),
            durationMs = durationMs,
            sizeBytes = size
        )
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            bleManager.startScan()
        }
    }

    override fun onDestroy() {
        ttsEngine.release()
        super.onDestroy()
    }
}
