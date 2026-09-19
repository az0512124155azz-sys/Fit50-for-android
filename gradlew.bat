@echo off
setlocal

set "GRADLE_VERSION=9.3.1"
set "ROOT=%~dp0"
set "DIST_DIR=%ROOT%.gradle-dist"
set "GRADLE_HOME=%DIST_DIR%\gradle-%GRADLE_VERSION%"
set "GRADLE_BAT=%GRADLE_HOME%\bin\gradle.bat"
set "ZIP=%DIST_DIR%\gradle-%GRADLE_VERSION%-bin.zip"

if exist "%GRADLE_BAT%" goto run

echo Gradle %GRADLE_VERSION% is not installed for this project. Downloading...
if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop';" ^
  "$url='https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip';" ^
  "$zip='%ZIP%';" ^
  "Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $zip;" ^
  "Expand-Archive -Path $zip -DestinationPath '%DIST_DIR%' -Force;" ^
  "Remove-Item $zip -Force"

if errorlevel 1 (
  echo.
  echo Failed to download Gradle %GRADLE_VERSION%.
  echo Check your internet connection and try again.
  exit /b 1
)

if not exist "%GRADLE_BAT%" (
  echo Gradle bootstrap failed: %GRADLE_BAT% was not created.
  exit /b 1
)

:run
call "%GRADLE_BAT%" %*
exit /b %ERRORLEVEL%
