@echo off
cd /d "%~dp0.."
where node >nul 2>nul
if errorlevel 1 (
  echo Please install Node.js 22 or newer, then open this file again.
  pause
  exit /b 1
)
if not exist "tools\coach\node_modules\esbuild" (
  call npm --prefix tools/coach ci
  if errorlevel 1 (pause & exit /b 1)
)
start "Fit50 library browser" powershell -NoProfile -WindowStyle Hidden -Command "Start-Sleep -Seconds 3; Start-Process 'http://127.0.0.1:4174'"
node exercise-library/tools/server.mjs
pause
