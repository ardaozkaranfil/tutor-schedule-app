@echo off
chcp 65001 >nul
cd /d "%~dp0"

REM ============================================================
REM  Shuts both containers down. The database volume and the
REM  backups/ folder survive this - only the containers go away.
REM ============================================================

REM --- Nothing to stop if the daemon isn't even running ---
docker info >nul 2>&1
if errorlevel 1 (
    echo.
    echo Docker Desktop zaten kapali, durdurulacak bir sey yok.
    echo.
    pause
    exit /b 0
)

docker compose down
if errorlevel 1 (
    echo.
    echo Uygulama durdurulamadi.
    echo.
    pause
    exit /b 1
)

echo.
echo Uygulama durduruldu.
echo.
pause
exit /b 0