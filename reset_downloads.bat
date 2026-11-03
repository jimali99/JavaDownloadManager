@echo off
title JDM - Reset Download Queue
echo ========================================================
echo       Java Download Manager (JDM) - Queue Cleaner
echo ========================================================
echo.
echo Stopping any running JDM instances...
taskkill /F /IM javaw.exe /T >nul 2>&1
taskkill /F /IM java.exe /T >nul 2>&1
echo.
echo Clearing queued downloads history...
if exist "%APPDATA%\JavaDownloadManager\downloads.dat" (
    del /f /q "%APPDATA%\JavaDownloadManager\downloads.dat"
    echo [OK] Removed pending download queue.
) else (
    echo [INFO] Download queue is already completely clean.
)
echo.
echo ========================================================
echo All running downloads and queues have been stopped.
echo You can now restart JDM safely with an empty queue.
echo ========================================================
echo.
pause
