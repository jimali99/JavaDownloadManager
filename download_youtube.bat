@echo off
setlocal enabledelayedexpansion
title JDM YouTube Video Downloader
echo ========================================================
echo   JDM — YouTube Video & Audio Downloader
echo ========================================================
echo.

:: 1. Check if yt-dlp is installed
set "YTDL_CMD="
where yt-dlp >nul 2>&1
if %errorlevel% equ 0 (
    set "YTDL_CMD=yt-dlp"
) else (
    python -m yt-dlp --version >nul 2>&1
    if !errorlevel! equ 0 (
        set "YTDL_CMD=python -m yt-dlp"
    ) else (
        echo [Setup] yt-dlp engine is required for YouTube stream merging.
        echo Installing yt-dlp via Python pip (takes ~5 seconds)...
        echo.
        python -m pip install --upgrade yt-dlp
        if !errorlevel! neq 0 (
            echo.
            echo [!] Could not auto-install yt-dlp.
            echo Please open a terminal and run: pip install yt-dlp
            pause
            exit /b 1
        )
        set "YTDL_CMD=python -m yt-dlp"
    )
)

:: 2. Read URL from command line or prompt user
set "VIDEO_URL=%~1"
if "%VIDEO_URL%"=="" (
    set /p VIDEO_URL="Enter or paste YouTube video link: "
)

if "%VIDEO_URL%"=="" (
    echo [Info] No URL provided. Exiting.
    pause
    exit /b 1
)

echo.
echo [1/2] Connecting to YouTube...
echo [2/2] Downloading highest quality video + audio and merging...
echo [Save Location]: %USERPROFILE%\Downloads
echo.

%YTDL_CMD% -f "bv*+ba/b" --merge-output-format mp4 -P "%USERPROFILE%\Downloads" "%VIDEO_URL%"

if %errorlevel% equ 0 (
    echo.
    echo ========================================================
    echo  SUCCESS! Video downloaded to your Downloads folder!
    echo ========================================================
    echo Auto-closing in 3 seconds...
    timeout /t 3 >nul
    exit /b 0
) else (
    echo.
    echo ========================================================
    echo  [!] Download failed.
    echo  Please ensure the YouTube link is public and valid.
    echo ========================================================
    pause
)
