@echo off
chcp 65001 >nul
cd /d "%~dp0"

REM ============================================================
REM  Starts Docker Desktop if needed, brings up the containers,
REM  waits for the app, then opens it in the browser.
REM
REM  Exit codes (read by run.vbs to pick an error message):
REM    0  success
REM    1  Docker Desktop not installed / executable not found
REM    2  Docker daemon did not come up in time
REM    3  docker compose failed to start the containers
REM    4  containers are up but the app never answered
REM
REM  Pass "quiet" as the first argument to suppress the final
REM  pause - run.vbs does this, since a hidden window cannot be
REM  dismissed by the user.
REM ============================================================

set "ERRMSG="
set "ERRCODE=1"
set "QUIET=0"
if /i "%~1"=="quiet" set "QUIET=1"

REM --- Is the Docker daemon already reachable? ---
docker info >nul 2>&1
if not errorlevel 1 goto DOCKER_READY

echo Docker Desktop kapali, baslatiliyor...

REM --- Look for Docker Desktop in its known install locations ---
if exist "%LocalAppData%\Programs\DockerDesktop\Docker Desktop.exe" (
    start "" "%LocalAppData%\Programs\DockerDesktop\Docker Desktop.exe"
) else if exist "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" (
    start "" "%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
) else if exist "%LocalAppData%\Docker\Docker Desktop.exe" (
    start "" "%LocalAppData%\Docker\Docker Desktop.exe"
) else (
    set "ERRMSG=Docker Desktop bulunamadi. Lutfen elle baslatip tekrar deneyin."
    set "ERRCODE=1"
    goto FAIL
)

REM --- Wait for the Docker daemon to come up (up to ~3 minutes) ---
set DOCKER_TRIES=60
set DCOUNT=0

:WAIT_DOCKER
docker info >nul 2>&1
if not errorlevel 1 goto DOCKER_READY
set /a DCOUNT+=1
if %DCOUNT% GEQ %DOCKER_TRIES% (
    set "ERRMSG=Docker Desktop zamaninda acilmadi. Bilgisayari yeniden baslatmayi deneyin."
    set "ERRCODE=2"
    goto FAIL
)
timeout /t 3 /nobreak >nul
goto WAIT_DOCKER

:DOCKER_READY
echo Uygulama baslatiliyor...
docker compose up -d
if errorlevel 1 (
    set "ERRMSG=Uygulama baslatilamadi. Docker Desktop calisiyor mu kontrol edin."
    set "ERRCODE=3"
    goto FAIL
)

REM --- Wait for the app to answer with HTTP 200 (up to ~60 seconds) ---
set MAX_TRIES=30
set COUNT=0

:CHECK
REM Reset STATUS first: if curl fails outright the for loop yields
REM nothing and STATUS would otherwise keep its previous value.
set "STATUS="
for /f %%i in ('curl -s -o nul -w "%%{http_code}" http://localhost:8080/appointments 2^>nul') do set STATUS=%%i
if "%STATUS%"=="200" goto READY

set /a COUNT+=1
if %COUNT% GEQ %MAX_TRIES% (
    set "ERRMSG=Uygulama basladi ama yanit vermiyor. Birkac dakika sonra tekrar deneyin."
    set "ERRCODE=4"
    goto FAIL
)

timeout /t 2 /nobreak >nul
goto CHECK

:READY
start http://localhost:8080/appointments
exit /b 0

:FAIL
echo.
echo %ERRMSG%
echo.
if "%QUIET%"=="0" pause
exit /b %ERRCODE%