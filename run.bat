@echo off
setlocal
cd /d "%~dp0"

set "JAVA_EXE="
if exist "C:\Program Files\Java\jdk-26.0.2\bin\javaw.exe" (
    set "JAVA_EXE=C:\Program Files\Java\jdk-26.0.2\bin\javaw.exe"
) else if exist "C:\Program Files\Java\latest\bin\javaw.exe" (
    set "JAVA_EXE=C:\Program Files\Java\latest\bin\javaw.exe"
) else (
    set "JAVA_EXE=javaw"
)

start "" "%JAVA_EXE%" --enable-native-access=ALL-UNNAMED -jar "IDM-Java.jar" %*
exit
