@echo off
setlocal EnableExtensions DisableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
set "PS1_SCRIPT=%SCRIPT_DIR%\run-centers-prod-06.ps1"

if not exist "%PS1_SCRIPT%" (
    echo Required file not found: %PS1_SCRIPT%
    exit /b 1
)

set "POWERSHELL_CMD="
for %%P in (pwsh.exe powershell.exe) do (
    if not defined POWERSHELL_CMD where %%P >nul 2>&1 && set "POWERSHELL_CMD=%%P"
)

if not defined POWERSHELL_CMD (
    echo PowerShell is required to run run-centers-prod-06.cmd
    exit /b 1
)

"%POWERSHELL_CMD%" -NoProfile -ExecutionPolicy Bypass -File "%PS1_SCRIPT%" %*
exit /b %ERRORLEVEL%
