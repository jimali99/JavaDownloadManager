@echo off
setlocal
echo ========================================================
echo  Building Internet Download Manager (IDM Pro - Java)
echo ========================================================

set "JDK_BIN=C:\Program Files\Java\jdk-26.0.2\bin"
if not exist "%JDK_BIN%\javac.exe" (
    where javac >nul 2>&1
    if %errorlevel% neq 0 (
        echo [ERROR] javac.exe not found! Please check your Java JDK installation.
        pause
        exit /b 1
    )
    set "JAVAC_CMD=javac"
    set "JAR_CMD=jar"
) else (
    set "JAVAC_CMD=%JDK_BIN%\javac.exe"
    set "JAR_CMD=%JDK_BIN%\jar.exe"
)

if not exist bin mkdir bin

echo [1/3] Compiling Java source files...
dir /s /b src\*.java > sources.txt
"%JAVAC_CMD%" -cp ".;flatlaf-3.5.4.jar" -d bin @sources.txt
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed!
    del sources.txt >nul 2>&1
    pause
    exit /b 1
)
del sources.txt >nul 2>&1

echo [2/3] Extracting FlatLaf modern UI library...
cd bin
"%JAR_CMD%" xf "..\flatlaf-3.5.4.jar"
cd ..

echo [2.5/3] Bundling application icons and logo resources...
if not exist "bin\resources" mkdir "bin\resources"
if exist "resources" (
    xcopy /s /y /q "resources\*" "bin\resources\" >nul
    xcopy /y /q "resources\*.png" "bin\" >nul
)

echo [3/3] Packaging standalone IDM-Java.jar...
"%JAR_CMD%" cfe "IDM-Java.jar" com.idm.Main -C bin .
if %errorlevel% neq 0 (
    echo [ERROR] JAR packaging failed!
    if "%1"=="" pause
    exit /b 1
)

echo.
echo ========================================================
echo  BUILD SUCCESSFUL! 
echo  Output: IDM-Java.jar
echo  You can now launch the app using run.bat or double clicking IDM-Java.jar
echo ========================================================
if "%1"=="" pause
