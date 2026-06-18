@echo off
REM Run Placement Platform with H2 In-Memory Database (no Docker needed)

setlocal enabledelayedexpansion

REM Set Java environment
set JAVA_HOME=D:\clode 222\jdk\jdk-17.0.19+10
set PATH=!JAVA_HOME!\bin;!PATH!

REM Change to backend directory
cd /d "d:\clode 222\files\placement-platform-foundation\placement-platform\backend"

if errorlevel 1 (
    echo ERROR: Could not change to backend directory
    pause
    exit /b 1
)

echo.
echo ========================================
echo Placement Platform - Development Server
echo ========================================
echo.
echo Using: H2 In-Memory Database (no Docker)
echo Port:  8080
echo.
echo Starting...
echo.

REM Run the application
call .\.m2\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run -Dspring-boot.run.profiles=test

pause
