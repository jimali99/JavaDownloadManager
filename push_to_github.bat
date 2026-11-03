@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ========================================================
echo   Sync / Update IDM Software to GitHub
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

:: 2. Check if remote origin is already linked
for /f "tokens=*" %%i in ('git remote get-url origin 2^>nul') do set REMOTE_URL=%%i

if "%REMOTE_URL%"=="" (
    echo [Setup] No GitHub repository linked yet.
    echo   1. Open https://github.com/new in your browser.
    echo   2. Create an empty repository.
    echo   3. Copy the URL (e.g. https://github.com/username/JavaDownloadManager.git)
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
    echo [Info] Connected to: %REMOTE_URL%
)

echo.
echo [1/3] Staging updated files...
git add .

echo.
set COMMIT_MSG=Update IDM UI with modern vector icons and styling
set /p USER_MSG="Enter commit message (Press Enter for default: '!COMMIT_MSG!'): "
if not "!USER_MSG!"=="" set COMMIT_MSG=!USER_MSG!

echo.
echo [2/3] Committing changes...
git commit -m "!COMMIT_MSG!" 2>nul || (
    echo No new changes to commit.
)

echo.
echo [3/3] Pushing updates to GitHub...
git push origin main

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Push failed. If prompted for password, use a GitHub Personal Access Token (PAT).
) else (
    echo.
    echo ========================================================
    echo  SUCCESS! Your GitHub repository is updated!
    echo ========================================================
)

pause
