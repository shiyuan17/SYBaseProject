@echo off
setlocal

set "ROOT_DIR=%~dp0..\..\.."
pushd "%ROOT_DIR%" >nul
set "BACKEND_LOG_FILE=%ROOT_DIR%\.logs\backend.log"

call "%~dp0set-dev-service-env.cmd" auth
if errorlevel 1 (
  popd >nul
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-dev-service.ps1" -ModuleName "auth-center" -LogFile "%BACKEND_LOG_FILE%"
set "EXIT_CODE=%ERRORLEVEL%"

popd >nul
exit /b %EXIT_CODE%
