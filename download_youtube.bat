@echo off
setlocal enabledelayedexpansion
title JDM YouTube Video Downloader
cd /d "%~dp0"

echo ========================================================
echo   JDM -- YouTube Video and Audio Downloader
echo ========================================================
echo.

:: Read URL from temp file or command line argument
set "VIDEO_URL="
if exist "%TEMP%\jdm_yt_url.txt" (
    set /p VIDEO_URL=<"%TEMP%\jdm_yt_url.txt"
    del /f /q "%TEMP%\jdm_yt_url.txt" 2>nul
)
if "!VIDEO_URL!"=="" (
    set "VIDEO_URL=%~1"
)
if "!VIDEO_URL!"=="" (
    set /p VIDEO_URL="Enter or paste YouTube video link: "
)

if "!VIDEO_URL!"=="" (
    echo [Info] No URL provided.
    echo Press any key to exit.
    pause >nul
    exit /b 1
)

echo [Target Video]: !VIDEO_URL!
echo [Save Folder]:  %USERPROFILE%\Downloads
echo.

:: 1. Locate yt-dlp executable
set "YTDL_BIN="
if exist "%LOCALAPPDATA%\Programs\Python\Python312\Scripts\yt-dlp.exe" (
    set "YTDL_BIN=%LOCALAPPDATA%\Programs\Python\Python312\Scripts\yt-dlp.exe"
)

:: 2. Check if yt-dlp is in PATH
if "!YTDL_BIN!"=="" (
    where yt-dlp >nul 2>&1
    if !errorlevel! equ 0 set "YTDL_BIN=yt-dlp"
)

:: 3. Test python module
set "USE_PYTHON=0"
if "!YTDL_BIN!"=="" (
    python -m yt_dlp --version >nul 2>&1
    if !errorlevel! equ 0 (
        set "USE_PYTHON=1"
    ) else (
        echo [Setup] Installing yt-dlp YouTube engine...
        python -m pip install --upgrade yt-dlp 2>nul || py -3 -m pip install --upgrade yt-dlp
        set "USE_PYTHON=1"
    )
)

echo [1/2] Connecting to YouTube and analyzing streams...
echo [2/2] Downloading Full HD video + audio and merging with FFmpeg...
echo.

if "!USE_PYTHON!"=="1" (
    python -m yt_dlp --js-runtimes node -f "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/bv*+ba/b" --merge-output-format mp4 -P "%USERPROFILE%\Downloads" "!VIDEO_URL!"
) else (
    "!YTDL_BIN!" --js-runtimes node -f "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/bv*+ba/b" --merge-output-format mp4 -P "%USERPROFILE%\Downloads" "!VIDEO_URL!"
)

if %errorlevel% equ 0 (
    echo.
    echo ========================================================
    echo  SUCCESS! Video downloaded to your Downloads folder!
    echo ========================================================
    echo.
    echo File saved in: %USERPROFILE%\Downloads
    echo.
    timeout /t 5
    exit /b 0
) else (
    echo.
    echo ========================================================
    echo  [!] Download encountered an error.
    echo  See error messages above.
    echo ========================================================
    echo.
    pause
    exit /b 1
)
