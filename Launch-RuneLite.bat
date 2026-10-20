@echo off
rem Updates to the latest version, then starts RuneLite with all the plugins.
cd /d "%~dp0"

echo Updating...
git pull
if errorlevel 1 (
    echo.
    echo Couldn't update ^(offline?^). Starting the version you already have.
    echo.
)

call gradlew.bat run
if errorlevel 1 (
    echo.
    echo RuneLite closed with an error. See the messages above.
    pause
)
