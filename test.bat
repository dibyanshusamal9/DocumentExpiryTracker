@echo off
rem Compiles and runs the self-test (no MySQL needed).
cd /d "%~dp0"
if exist out-test rmdir /s /q out-test
mkdir out-test
dir /s /b src\*.java test\*.java > out-test\sources.txt
javac -encoding UTF-8 -d out-test @out-test\sources.txt || exit /b 1
java -cp out-test com.doctracker.SelfTest
