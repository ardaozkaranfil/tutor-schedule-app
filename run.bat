@echo off
cd /d "%~dp0"
docker compose up -d

set MAX_TRIES=30
set COUNT=0

:CHECK
for /f %%i in ('curl -s -o nul -w "%%{http_code}" http://localhost:8080 2^>nul') do set STATUS=%%i

if "%STATUS%"=="200" goto READY

set /a COUNT+=1
if %COUNT% GEQ %MAX_TRIES% goto READY

timeout /t 2 /nobreak >nul
goto CHECK

:READY
start http://localhost:8080