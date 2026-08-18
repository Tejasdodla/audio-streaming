# nRF54L15 Audio Receiver Firmware

Embedded Zephyr / nRF Connect SDK firmware for the **nRF54L15 Development Module / DK**, featuring I2S audio output to the **MAX98357A** Class-D DAC/Amplifier, external SPI/QSPI NOR Flash caching, and high-throughput BLE audio streaming.

---

## Hardware Pinout & Wiring

| MAX98357A Pin | nRF54L15 Pin | Function | Notes |
| :--- | :--- | :--- | :--- |
| **BCLK** | `P1.08` | I2S Bit Clock (Master Output) | Connect directly |
| **LRCK / WS** | `P1.09` | I2S Word Select / Frame Sync | 16kHz - 48kHz frame clock |
| **DIN / SDATA** | `P1.10` | I2S Serial Data Out | Connects to MAX98357A DIN |
| **GAIN** | `GND` / Unconnected | Gain Setting | GND = 15dB (recommended), Floating = 9dB |
| **SD_MODE** | Floating (or VDD via 100k) | Shutdown / Channel Select | Unconnected mixes (L+R)/2 mono |
| **VIN / VDD** | `3.3V` / `5V` (VDD/VBUS) | Power Supply | 2.5V - 5.5V input |
| **GND** | `GND` | Ground Reference | Common ground |

---

## Features

- **High-Throughput BLE Pipeline**:
  - BLE 5.4 with 2M PHY (`BT_GAP_LE_PHY_2M`).
  - Data Length Extension (DLE: 251 byte MTU payload).
  - Fast connection interval (15ms).
  - Custom 128-bit Audio Streaming Service (`5f550001-8b43-4f1e-9827-00554c150000`).
- **Jitter Buffer**:
  - 16 KB thread-safe circular buffer.
  - Pre-buffering threshold to eliminate wireless transmission packet jitter.
  - Sequence number tracking and packet loss detection.
- **Audio DSP**:
  - Logarithmic software volume control with smooth transitions.
  - Soft-limiting and anti-clipping protection for the speaker.
  - Stereo to Mono mixing.
- **External Flash Storage**:
  - Support for onboard QSPI/SPI NOR Flash (e.g. MX25R6435F).
  - Store and trigger offline audio assets and voice prompts.
- **MAX98357A I2S Driver**:
  - Dedicated real-time I2S feeder thread with 4-block DMA memory slab.
  - Dynamically configurable sample rate (16kHz, 24kHz, 44.1kHz, 48kHz).

---

## Building and Flashing

### Prerequisites
- [nRF Connect SDK v2.7+ / v2.8+](https://www.nordicsemi.com/Products/Development-software/nrf-connect-sdk)
- Zephyr `west` toolchain installed

### 1. Build Firmware
```bash
# In the firmware directory:
west build -b nrf54l15dk/nrf54l15/cpuapp .
```

### 2. Flash to nRF54L15 DK
```bash
west flash
```

### 3. Monitor Serial Console
```bash
west espressif / JLinkRTTClient
# Or use serial terminal at 115200 baud (e.g. PuTTY or nRF Connect Serial Terminal)
```

---

## GATT Service Specification

- **Service UUID**: `5f550001-8b43-4f1e-9827-00554c150000`
- **Audio Data Char** (`5f550002-...`):
  - Property: `WRITE_WITHOUT_RESPONSE`
  - Format: `[Opcode: 0x01][SeqNum: uint8][Len: uint16_le][PCM Samples: 16-bit LE...]`
- **Audio Control Char** (`5f550003-...`):
  - Property: `WRITE`, `NOTIFY`
  - Opcodes:
    - `0x10`: Start Playback
    - `0x11`: Stop Playback
    - `0x12`: Pause
    - `0x13`: Resume
    - `0x14`: Configure `[sample_rate: uint32, channels: uint8, bit_depth: uint8, codec: uint8, mode: uint8]`
    - `0x15`: Set Volume `[vol: 0-100]`
    - `0x16`: Play Flash Asset `[asset_id: uint8]`
- **Audio Stats Char** (`5f550004-...`):
  - Property: `READ`, `NOTIFY`
  - Notifies buffer fill level %, volume, underrun counts, total packets.
