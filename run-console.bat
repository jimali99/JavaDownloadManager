@echo off
setlocal
cd /d "%~dp0"
title Java Download Manager (Console Mode)

set "JAVA_EXE="
if exist "C:\Program Files\Java\jdk-26.0.2\bin\java.exe" (
    set "JAVA_EXE=C:\Program Files\Java\jdk-26.0.2\bin\java.exe"
) else if exist "C:\Program Files\Java\latest\bin\java.exe" (
    set "JAVA_EXE=C:\Program Files\Java\latest\bin\java.exe"
) else (
    set "JAVA_EXE=java"
)

echo Starting Java Download Manager...
"%JAVA_EXE%" --enable-native-access=ALL-UNNAMED -jar "IDM-Java.jar" %*
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Application exited with code %errorlevel%.
    pause
)
