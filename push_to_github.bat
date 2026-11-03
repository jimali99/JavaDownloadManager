@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ========================================================
echo   Sync / Update IDM Software to GitHub
echo ========================================================
echo.

:: 1. Check Git configuration
set GIT_USER=
for /f "tokens=*" %%i in ('git config --get user.name 2^>nul') do set GIT_USER=%%i
if "!GIT_USER!"=="" (
    echo [Git Setup] Git user name is not set yet.
    set /p NEW_USER="Enter your GitHub username or name: "
    git config --global user.name "!NEW_USER!"
)

set GIT_EMAIL=
for /f "tokens=*" %%i in ('git config --get user.email 2^>nul') do set GIT_EMAIL=%%i
if "!GIT_EMAIL!"=="" (
    echo [Git Setup] Git email is not set yet.
    set /p NEW_EMAIL="Enter your GitHub email address: "
    git config --global user.email "!NEW_EMAIL!"
)

:: 2. Check if remote origin is already linked
set REMOTE_URL=
for /f "tokens=*" %%i in ('git remote get-url origin 2^>nul') do set REMOTE_URL=%%i

if "!REMOTE_URL!"=="" (
    echo [Setup] No GitHub repository linked yet.
    echo   1. Open https://github.com/new in your browser.
    echo   2. Create an empty repository named JavaDownloadManager.
    echo   3. Copy the URL (e.g. https://github.com/jimali99/JavaDownloadManager.git)
    echo.
    set /p REPO_URL="Paste your GitHub repository URL here: "
    if "!REPO_URL!"=="" (
        echo [ERROR] No URL provided. Aborted.
        pause
        exit /b 1
    )
    git remote add origin !REPO_URL!
    git branch -M main
) else (
    echo [Info] Connected to: !REMOTE_URL!
)

echo.
echo [1/3] Staging updated files...
git add .

echo.
echo [2/3] Checking commits...
git status -s > nul 2>&1
git commit -m "Update JDM software, icons, and documentation" 2>nul || (
    echo [Info] Local files already committed.
)

echo.
echo [2.5/3] Synchronizing with GitHub...
git pull --no-rebase origin main --no-edit >nul 2>&1

echo.
echo [3/3] Uploading (pushing) to GitHub...
echo.
git push -u origin main

if %errorlevel% neq 0 (
    echo.
    echo ========================================================
    echo  [!] Push could not complete automatically.
    echo  If a browser window opened, please click "Authorize"
    echo  or log in with your GitHub account.
    echo ========================================================
) else (
    echo.
    echo ========================================================
    echo  SUCCESS! Your GitHub repository is 100%% up to date!
    echo  View it at: https://github.com/jimali99/JavaDownloadManager
    echo ========================================================
)

echo.
pause
