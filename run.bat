@echo off
rem Runs the tracker. Put mysql-connector-j-*.jar in lib\ for MySQL; add --offline to use local file storage.
cd /d "%~dp0"
if not exist out\com\doctracker\Main.class call build.bat || exit /b 1
java -cp "out;lib\*" com.doctracker.Main %*
