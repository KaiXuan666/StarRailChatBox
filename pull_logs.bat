@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo StarRailChatBox Android Log Puller
echo ===================================================

rem Check if adb is available in PATH
where adb >nul 2>nul
if %errorlevel% neq 0 (
    echo Error: adb command not found. Please make sure Android SDK platform-tools is in your PATH.
    pause
    exit /b 1
)

rem Check if any android device is connected
adb devices | findstr /r /c:"[a-zA-Z0-9].*device$" >nul
if %errorlevel% neq 0 (
    echo Error: No Android device found. Please connect your device and enable USB debugging.
    pause
    exit /b 1
)

rem Create local log directory if not exists
if not exist "log" (
    mkdir log
)

echo Pulling logs from device via run-as cat...

set PACKAGE_NAME=com.kaixuan.starrailchatbox
set HAS_LOGS=0

rem Get file list and iterate
for /f "tokens=*" %%f in ('adb shell "run-as !PACKAGE_NAME! ls files/log" 2^>nul') do (
    set "RAW_NAME=%%f"
    rem Truncate to exact 27 characters to remove any carriage return (\r) or trailing junk
    set "FILE_NAME=!RAW_NAME:~0,27!"
    
    rem Verify it is indeed a log file (starts with req_ or res_)
    set "IS_VALID=0"
    if "!FILE_NAME:~0,4!" equ "req_" set "IS_VALID=1"
    if "!FILE_NAME:~0,4!" equ "res_" set "IS_VALID=1"
    
    if "!IS_VALID!" equ "1" (
        set "HAS_LOGS=1"
        echo Pulling !FILE_NAME!...
        adb shell "run-as !PACKAGE_NAME! cat files/log/!FILE_NAME!" > "log\!FILE_NAME!"
    )
)

if "!HAS_LOGS!" equ "0" (
    echo No network logs found on the device. Make sure the app has run and made network requests.
) else (
    echo ===================================================
    echo Pull completed successfully. Logs saved in "log" folder.
    echo ===================================================
)
pause
