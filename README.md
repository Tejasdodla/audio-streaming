# 5th Sense • nRF54L15 Audio Streaming Ecosystem

End-to-end Bluetooth Low Energy audio streaming system featuring the **Nordic nRF54L15 Dev Module**, **MAX98357A I2S Class-D Amplifier**, onboard **SPI/QSPI NOR Flash**, and a **Kotlin Multiplatform (Android)** application supporting File Streaming, System Audio / Spotify capture ("Speaker Mode"), and Text-to-Speech (TTS).

---

## Repository Structure

```
audio-streaming/
├── firmware/                              # nRF54L15 Zephyr / nRF Connect SDK Project
│   ├── CMakeLists.txt                     # CMake build definition
│   ├── prj.conf                           # Kconfig (BLE 5.4, I2S, Flash, LittleFS)
│   ├── app.overlay                        # Devicetree overlay for I2S pins & Flash
│   ├── boards/
│   │   └── nrf54l15dk_nrf54l15_cpuapp.overlay
│   ├── src/
│   │   ├── main.c                         # BLE advertising, LED indicators, main loop
│   │   ├── audio_protocol.h               # Shared 128-bit UUIDs, opcodes, packet formats
│   │   ├── audio_i2s.h / .c               # Zephyr I2S driver wrapper for MAX98357A
│   │   ├── jitter_buffer.h / .c           # 16 KB ring buffer with underrun recovery
│   │   ├── audio_dsp.h / .c               # Volume scaling, soft limiting, stereo downmix
│   │   ├── flash_storage.h / .c           # Onboard NOR flash asset caching & playback
│   │   └── audio_stream_service.h / .c   # BLE Custom Audio GATT Streaming Service
│   └── README.md                          # Firmware build & flash guide
│
└── app/                                   # Kotlin Multiplatform Android App
    ├── build.gradle.kts                   # Root gradle build script
    ├── settings.gradle.kts                # KMP settings
    ├── gradle/
    │   └── libs.versions.toml             # Version catalog (Compose 1.6+, Kotlin 2.0+)
    ├── composeApp/
    │   ├── build.gradle.kts               # Compose Multiplatform config
    │   └── src/
    │       ├── commonMain/kotlin/com/fifthsense/audiostream/
    │       │   ├── App.kt                 # Compose root UI
    │       │   ├── theme/                 # Cyber glassmorphism dark theme
    │       │   ├── components/            # Visualizer, GlassCard, DeviceCard, StatsPanel
    │       │   ├── model/                 # Stream modes, devices, stats, config
    │       │   ├── ble/                   # Protocol packet encoders & BleManager interface
    │       │   ├── viewmodel/             # StateFlow AudioStreamViewModel
    │       │   └── screens/               # Dashboard, FileStream, SpeakerMode, TtsScreen
    │       └── androidMain/kotlin/com/fifthsense/audiostream/
    │           ├── MainActivity.kt        # Permissions & MediaProjection launcher
    │           ├── ble/AndroidBleManager.kt # Android BluetoothGatt (2M PHY, DLE, 512 MTU)
    │           └── audio/
    │               ├── SystemAudioCaptureService.kt # Spotify / System media capture
    │               ├── FileAudioDecoder.kt          # MP3 / WAV / FLAC storage decoder
    │               ├── AndroidTtsEngine.kt          # Text-to-Speech synthesis streamer
    │               └── AudioStreamer.kt             # Audio packet queue & throughput pacer
    └── README.md                          # App installation & usage guide
```

---

## Hardware Connection (nRF54L15 -> MAX98357A)

| MAX98357A Pin | nRF54L15 Pin | Function |
| :--- | :--- | :--- |
| **DIN / SDATA** | `P1.13` | I2S Serial Data Out |
| **BCLK** | `P1.12` | I2S Bit Clock (Master Output) |
| **LRCK / WS** | `P1.11` | I2S Word Select (Frame Clock) |
| **GAIN** | `GND` | Gain: 15dB |
| **SD_MODE** | Floating (or VDD via 100k) | Mono Mix (Left + Right)/2 |
| **VIN / VDD** | `5V` (VBUS / 5V) | Power Supply |
| **GND** | `GND` | Ground Reference |

---

## Quick Start

### 1. Build and Flash Firmware
```bash
cd firmware
west build -b nrf54l15dk/nrf54l15/cpuapp .
west flash
```

### 2. Build and Run Android App
```bash
cd ../app
./gradlew :composeApp:assembleDebug
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk
```
