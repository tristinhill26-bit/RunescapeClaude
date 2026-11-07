@echo off
rem Checks GitHub for updates to the plugins, then builds them and opens RuneLite.
title RuneLite with Claude's plugins

rem git pull can rewrite this file while it runs, and cmd reads batch files line by line as it goes, so a
rem changed file gets read from the wrong place. Run from a copy in the temp folder instead.
if not "%~1"=="--from-copy" (
	copy /y "%~f0" "%TEMP%\start-runelite-running.bat" >nul
	"%TEMP%\start-runelite-running.bat" --from-copy "%~dp0"
)
cd /d "%~2"
set PULL_FAILED=

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
	set PULL_FAILED=1
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
call gradlew.bat run
if errorlevel 1 if defined PULL_FAILED (
	echo.
	echo Trying again with the RuneLite version that's already downloaded, in case you're offline...
	call gradlew.bat run --offline
)
if errorlevel 1 (
	echo.
	echo Something went wrong. Copy the message above and send it to Claude.
	pause
)
