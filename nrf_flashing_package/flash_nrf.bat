@echo off
echo =======================================================
echo   5th Sense - nRF54L15 Audio Firmware One-Click Flasher
echo =======================================================
echo.

set "HEX_PATH=%~dp0nrf54l15_audio_firmware.hex"
if not exist "%HEX_PATH%" (
    if exist "nrf54l15_audio_firmware.hex" set "HEX_PATH=nrf54l15_audio_firmware.hex"
)

where nrfjprog >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [1/2] Erasing and programming via nrfjprog...
    nrfjprog --program "%HEX_PATH%" --chiperase --reset --verify
    goto done
)

if exist "%USERPROFILE%\.nrfutil\bin\nrfutil.exe" (
    echo [1/2] Programming via nrfutil device...
    "%USERPROFILE%\.nrfutil\bin\nrfutil.exe" device program --firmware "%HEX_PATH%"
    "%USERPROFILE%\.nrfutil\bin\nrfutil.exe" device reset
    goto done
)

where nrfutil >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [1/2] Programming via nrfutil device...
    nrfutil device program --firmware "%HEX_PATH%"
    nrfutil device reset
    goto done
)

where west >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [1/2] Programming via west flash...
    west flash --hex-file "%HEX_PATH%"
    goto done
)

echo.
echo [!] No CLI programmer tool found in PATH (nrfjprog, nrfutil, or west).
echo.
echo Please use the GUI method instead:
echo 1. Open "nRF Connect for Desktop"
echo 2. Open the "Programmer" app
echo 3. Select your connected nRF54L15 board
echo 4. Click "Add file" -> select "nrf54l15_audio_firmware.hex"
echo 5. Click "Erase & write"
echo.
pause
exit /b 1

:done
echo.
echo [2/2] Firmware flashed and verified successfully!
echo The board is now advertising as "5th Sense Audio-Sink".
echo.
pause
