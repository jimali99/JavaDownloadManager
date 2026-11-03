@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ========================================================
echo   Upload IDM Software to GitHub
echo ========================================================
echo.

:: 1. Check Git configuration
for /f "tokens=*" %%i in ('git config --get user.name 2^>nul') do set GIT_USER=%%i
for /f "tokens=*" %%i in ('git config --get user.email 2^>nul') do set GIT_EMAIL=%%i

if "%GIT_USER%"=="" (
    echo [Git Setup] Git user name is not set yet.
    set /p NEW_USER="Enter your GitHub username or name: "
    git config --global user.name "!NEW_USER!"
)

if "%GIT_EMAIL%"=="" (
    echo [Git Setup] Git email is not set yet.
    set /p NEW_EMAIL="Enter your GitHub email address: "
    git config --global user.email "!NEW_EMAIL!"
)

echo.
echo [1/4] Staging files...
git add .

echo.
echo [2/4] Creating initial commit...
git commit -m "Initial commit of Java Download Manager and Chrome Extension" 2>nul || echo Commit already up to date.

echo.
echo [3/4] Connect to your GitHub repository:
echo   1. Open https://github.com/new in your browser.
echo   2. Create an empty repository (do NOT check README, .gitignore, or license).
echo   3. Copy the repository URL (e.g., https://github.com/username/JavaDownloadManager.git)
echo.
set /p REPO_URL="Paste your GitHub repository URL here: "

if "%REPO_URL%"=="" (
    echo [ERROR] No URL provided. Aborted.
    pause
    exit /b 1
)

:: Remove existing origin if any
git remote remove origin 2>nul
git remote add origin %REPO_URL%
git branch -M main

echo.
echo [4/4] Pushing code to GitHub...
git push -u origin main

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Push failed. If this is a private repo or you need authentication,
    echo sign in with your GitHub Personal Access Token or GitHub CLI.
) else (
    echo.
    echo ========================================================
    echo  SUCCESS! Your project has been uploaded to GitHub!
    echo ========================================================
)

pause
