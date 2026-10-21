@echo off
rem Updates to the latest version, then starts RuneLite with all the plugins.
cd /d "%~dp0"

set GRADLE_ARGS=
echo Updating...
git pull
if errorlevel 1 (
    echo.
    echo Couldn't update ^(offline?^). Starting the version you already have.
    echo.
    set GRADLE_ARGS=--offline
)

rem Gradle also fetches the newest RuneLite release here, so the client matches the official one.
call gradlew.bat run %GRADLE_ARGS%
if errorlevel 1 (
    echo.
    echo RuneLite closed with an error. See the messages above.
    pause
)
