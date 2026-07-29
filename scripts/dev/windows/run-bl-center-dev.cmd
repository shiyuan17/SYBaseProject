@echo off
setlocal

set "ROOT_DIR=%~dp0..\..\.."
pushd "%ROOT_DIR%" >nul
set "EXIT_CODE=0"
set "BACKEND_LOG_FILE=%ROOT_DIR%\.logs\backend.log"

call "%~dp0set-dev-service-env.cmd" bl
if errorlevel 1 (
  set "EXIT_CODE=1"
  goto :finish
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-dev-service.ps1" -ModuleName "bl-center" -LogFile "%BACKEND_LOG_FILE%"
set "EXIT_CODE=%ERRORLEVEL%"

:finish
popd >nul
if not "%EXIT_CODE%"=="0" call :pause_on_error %EXIT_CODE%
exit /b %EXIT_CODE%

:pause_on_error
set "ERROR_CODE=%~1"
if "%PAUSE_ON_ERROR%"=="0" goto :eof
if defined CI goto :eof
echo.
echo Script failed with exit code %ERROR_CODE%.
echo Press any key to close this window...
pause >nul
goto :eof
