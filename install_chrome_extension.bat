@echo off
setlocal
echo ========================================================
echo  Java Download Manager (JDM) Chrome Extension Quick Installer
echo ========================================================
echo.
set "EXT_DIR=%~dp0chrome-extension"

echo [1/3] Copying extension folder path to clipboard...
powershell -Command "Set-Clipboard -Value '%EXT_DIR%'"
echo       Copied: %EXT_DIR%
echo.

echo [2/3] Easy 3-Step Installation in Chrome:
echo   1. Enable "Developer mode" toggle in top-right corner.
echo   2. Click "Load unpacked" button in top-left.
echo   3. In the folder prompt, press Ctrl+V to paste the path and click "Select Folder".
echo.

echo [3/3] Opening Chrome Extensions page...
start chrome "chrome://extensions"

echo.
echo ========================================================
echo  Extension is ready! 
echo  Once loaded, click any download or right-click any link to download with IDM!
echo ========================================================
pause
