====================================================================
  5th Sense: nRF54L15 + MAX98357A Audio Streaming Client Package
====================================================================

This folder contains everything needed to flash the nRF54L15 board
and install the Android Audio Streaming App without needing to build
from source code.

FILES IN THIS FOLDER:
---------------------
1. nrf54l15_audio_firmware.hex  -> Pre-compiled firmware binary for nRF54L15
2. 5thSense-AudioStream.apk     -> Pre-compiled Android Companion App
3. flash_nrf.bat                -> 1-click automatic flashing script
4. README.txt                   -> This guide


HOW TO FLASH THE nRF54L15:
--------------------------

OPTION A: 1-Click Flash Script (Fastest)
1. Connect your nRF54L15 DK to your PC via USB cable.
2. Double-click "flash_nrf.bat".
3. Wait 5 seconds until "Firmware flashed and verified successfully!" appears.

OPTION B: nRF Connect Programmer GUI (Visual)
1. Open "nRF Connect for Desktop" on your PC.
2. Launch the "Programmer" app.
3. In the top-left dropdown, select your connected nRF54L15 board.
4. In the right panel, click "Add file" -> Browse and select "nrf54l15_audio_firmware.hex".
5. Click the "Erase & write" button.


HARDWARE WIRING: MAX98357A I2S DAC -> nRF54L15 DK
--------------------------------------------------
  MAX98357A Pin         nRF54L15 DK Pin
  ------------------------------------------------
  DIN (Data In)      -> P1.13
  BCLK (Bit Clock)   -> P1.12
  LRC (Left/Right)   -> P1.11
  VIN (Power)        -> 5V (VBUS / 5V pin on DK)
  GND (Ground)       -> GND
  GAIN               -> GND (Sets 15 dB default gain)
  SD_MODE            -> Left floating / connected to 3.3V


HOW TO INSTALL THE ANDROID APP:
--------------------------------
1. Copy "5thSense-AudioStream.apk" to your Android phone.
2. Open the file on your phone and tap "Install".
3. Grant Bluetooth & Audio capture permissions when prompted.
4. Tap "SCAN" -> Connect to "nRF54L15-Audio-Sink".
5. Select "Spotify / Speaker Mode", "File Player", or "Text-To-Speech".
====================================================================
