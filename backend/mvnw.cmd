@REM ----------------------------------------------------------------------------
@REM Maven startup script for Windows - uses bundled Maven 3.9.6
@REM ----------------------------------------------------------------------------
@echo off
setlocal enabledelayedexpansion

set "MAVEN_PROJECTBASEDIR=%~dp0"
if "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"

set "MAVEN_HOME=%MAVEN_PROJECTBASEDIR%\.m2\apache-maven-3.9.6"
set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"

if not exist "%MAVEN_CMD%" (
    echo ERROR: Bundled Maven not found at: %MAVEN_CMD%
    echo Run: powershell -File "%MAVEN_PROJECTBASEDIR%\mvn.ps1" to install it.
    exit /b 1
)

if not "%JAVA_HOME%"=="" (
    if exist "%JAVA_HOME%\bin\java.exe" goto run
)
for /f "tokens=*" %%i in ('where java 2^>nul') do goto run
echo ERROR: JAVA_HOME is not set and java is not on PATH.
exit /b 1

:run
if "%SERVER_PORT%"=="" set "SERVER_PORT=8081"
"%MAVEN_CMD%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %*
set ERROR_CODE=%ERRORLEVEL%
endlocal
exit /b %ERROR_CODE%
