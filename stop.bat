@echo off
cd /d "%~dp0"
docker compose down
echo.
echo Uygulama durduruldu.
echo.
pause