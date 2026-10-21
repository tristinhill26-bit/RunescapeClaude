@echo off
rem Checks GitHub for updates to the plugins, then builds them and opens RuneLite.
title RuneLite with Claude's plugins
cd /d "%~dp0"
set GRADLE_ARGS=

where git >nul 2>nul
if errorlevel 1 (
	echo Git isn't installed, so skipping the update check.
	goto run
)
if not exist ".git" (
	echo This folder wasn't downloaded with Git, so skipping the update check.
	goto run
)

echo Checking for updates...
git pull --ff-only
if errorlevel 1 (
	set GRADLE_ARGS=--offline
	echo.
	echo Couldn't update, so opening the version you already have.
	echo If this keeps happening, copy the message above and send it to Claude.
	echo.
	timeout /t 5 >nul
)

:run
echo.
echo Building and opening RuneLite. The first start after an update takes a little longer...
rem Gradle also fetches the newest RuneLite release here, so the client matches the official one.
call gradlew.bat run %GRADLE_ARGS%
if errorlevel 1 (
	echo.
	echo Something went wrong. Copy the message above and send it to Claude.
	pause
)
