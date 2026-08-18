# 5th Sense Audio Streaming Android Application (Kotlin Multiplatform)

A modern Android application built with **Compose Multiplatform** and Kotlin to stream audio over Bluetooth Low Energy (BLE 2M PHY / DLE) to the **nRF54L15 Dev Module + MAX98357A I2S DAC**.

---

## 3 Audio Streaming Modes

### 1. 📁 Local File Player (`FILE_STREAM`)
- Open any local audio file (**MP3, WAV, AAC, FLAC**) from device storage using the system file picker.
- Decodes frames via Android's `MediaExtractor` / `MediaCodec` into 16-bit PCM.
- Real-time scrubber timeline, seek bar, play/pause controls, and sample preset tracks.

### 2. 🔊 System Speaker Mode (`SYSTEM_SPEAKER` - Spotify & YouTube)
- Emulates a Bluetooth speaker by intercepting **all system audio playback** (Spotify, YouTube, Apple Music, Games, browser audio) using Android 10+ `AudioPlaybackCaptureConfiguration` and `MediaProjection`.
- Runs in a background Foreground Service so you can switch apps to Spotify, press play, and hear your music out of the MAX98357A speaker in real-time!

### 3. 🗣️ Text-To-Speech Generator (`TTS_ENGINE`)
- Type any custom sentence or select from quick preset phrases.
- Modulate voice pitch (0.5x - 1.8x) and speech rate (0.5x - 2.0x).
- Synthesizes audio using Android's `TextToSpeech` engine and streams raw PCM chunks directly to the nRF54L15 speaker.

---

## UI & Design Highlights

- **Dark Glassmorphic Cyber Theme**: Deep obsidian background, frosted card borders (`#00F5D4`), glowing badges, and gradient accents.
- **Live Animated Audio Visualizer**: 28-band dynamic frequency visualizer with real-time waveform animation during active playback.
- **Hardware Telemetry Dashboard**: Real-time display of nRF54L15 Jitter Buffer Fill %, underrun counter, BLE throughput (kbps), and packet loss detection.
- **Digital Volume Slider**: Synchronized with the nRF54L15 DSP volume scaler.

---

## BLE Architecture & Protocol

- **Target Service UUID**: `5f550001-8b43-4f1e-9827-00554c150000`
- **Audio Data Char** (`5f550002-...`): High-throughput write without response with 4-byte sequence header.
- **Audio Control Char** (`5f550003-...`): Start, Stop, Pause, Resume, Sample Rate Config, Set Volume, Play Flash Asset.
- **Audio Stats Char** (`5f550004-...`): Real-time notifications of buffer occupancy and underruns.
- **BLE Parameters**:
  - ATT MTU: **517 bytes**
  - PHY: **LE 2M PHY** (2 Mbps)
  - Data Length Extension (DLE): **251 bytes**
  - Connection Priority: **High** (11.25ms - 15ms interval)

---

## Building and Running

### Prerequisites
- Android Studio Ladybug / Koala or IntelliJ IDEA with Kotlin Multiplatform plugin.
- Android SDK 34 (Minimum SDK 29 for AudioPlaybackCapture).
- JDK 17.

### Build via Gradle
```bash
cd app
./gradlew :composeApp:assembleDebug
```

### Install APK onto Android Phone
```bash
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk
```
