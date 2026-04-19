@echo off
setlocal
set APP_HOME=%~dp0
set GRADLE_VERSION=8.7
set DIST_NAME=gradle-%GRADLE_VERSION%-bin.zip
set DIST_URL=https://services.gradle.org/distributions/%DIST_NAME%
set INSTALL_DIR=%APP_HOME%\.gradle-dist
set GRADLE_HOME=%INSTALL_DIR%\gradle-%GRADLE_VERSION%
set GRADLE_BIN=%GRADLE_HOME%\bin\gradle.bat
set ZIP_PATH=%INSTALL_DIR%\%DIST_NAME%

if exist "%GRADLE_BIN%" goto run
if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
if not exist "%ZIP_PATH%" (
  echo Baixando Gradle %GRADLE_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%ZIP_PATH%'"
  if errorlevel 1 exit /b 1
)
if not exist "%GRADLE_HOME%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP_PATH%' -DestinationPath '%INSTALL_DIR%' -Force"
  if errorlevel 1 exit /b 1
)

:run
call "%GRADLE_BIN%" %*
