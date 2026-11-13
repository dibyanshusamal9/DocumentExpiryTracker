@echo off
rem Compiles the app into out\ (and packages DocumentExpiryTracker.jar if the jar tool is available)
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > sources.tmp
javac -encoding UTF-8 -d out @sources.tmp
set ERR=%ERRORLEVEL%
del sources.tmp
if not %ERR%==0 exit /b %ERR%
where jar >nul 2>nul && jar --create --file DocumentExpiryTracker.jar --main-class com.doctracker.Main -C out . && echo Built DocumentExpiryTracker.jar
echo Compiled to out\
