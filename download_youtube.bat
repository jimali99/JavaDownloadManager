@echo off
setlocal enabledelayedexpansion
title JDM YouTube Video Downloader
cd /d "%~dp0"

echo ========================================================
echo   JDM -- YouTube Video and Audio Downloader
echo ========================================================
echo.

:: Locate yt-dlp executable or Python module
set "YTDL_CMD="

if exist "%LOCALAPPDATA%\Programs\Python\Python312\Scripts\yt-dlp.exe" (
    set "YTDL_CMD=%LOCALAPPDATA%\Programs\Python\Python312\Scripts\yt-dlp.exe"
)

if "!YTDL_CMD!"=="" (
    where yt-dlp >nul 2>&1
    if !errorlevel! equ 0 set "YTDL_CMD=yt-dlp"
)

if "!YTDL_CMD!"=="" (
    python -m yt_dlp --version >nul 2>&1
    if !errorlevel! equ 0 set "YTDL_CMD=python -m yt_dlp"
)

if "!YTDL_CMD!"=="" (
    py -3 -m yt_dlp --version >nul 2>&1
    if !errorlevel! equ 0 set "YTDL_CMD=py -3 -m yt_dlp"
)

if "!YTDL_CMD!"=="" (
    echo [Setup] yt-dlp engine not found. Installing...
    python -m pip install --upgrade yt-dlp 2>nul || py -3 -m pip install --upgrade yt-dlp
    set "YTDL_CMD=python -m yt_dlp"
)

:: Read URL from command line or prompt user
set "VIDEO_URL=%~1"
if "!VIDEO_URL!"=="" (
    set /p VIDEO_URL="Enter or paste YouTube video link: "
)

if "!VIDEO_URL!"=="" (
    echo [Info] No URL provided. Exiting.
    timeout /t 3 >nul
    exit /b 1
)

echo.
echo [1/2] Connecting to YouTube...
echo [2/2] Downloading highest quality video and audio and merging with FFmpeg...
echo [Save Location]: %USERPROFILE%\Downloads
echo.

"!YTDL_CMD!" -f "bv*+ba/b" --merge-output-format mp4 -P "%USERPROFILE%\Downloads" "!VIDEO_URL!"

if %errorlevel% equ 0 (
    echo.
    echo ========================================================
    echo  SUCCESS! Video downloaded to your Downloads folder!
    echo ========================================================
    echo Auto-closing in 5 seconds...
    timeout /t 5 >nul
    exit /b 0
) else (
    echo.
    echo ========================================================
    echo  [!] Download failed.
    echo  Please ensure the YouTube link is public and valid.
    echo ========================================================
    pause
)
