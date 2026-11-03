@echo off
setlocal
cd /d "%~dp0"

start javaw --enable-native-access=ALL-UNNAMED -jar "IDM-Java.jar" %*
exit
