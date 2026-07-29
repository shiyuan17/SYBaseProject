@echo off

set "ROOT_DIR=%~dp0"
if "%ROOT_DIR:~-1%"=="\" set "ROOT_DIR=%ROOT_DIR:~0,-1%"
set "TARGET_SCRIPT=%ROOT_DIR%\scripts\dev\windows\run-bl-center-dev.cmd"

if not exist "%TARGET_SCRIPT%" (
  >&2 echo Unable to find backend launcher: "%TARGET_SCRIPT%"
  >&2 echo Run this command from the SYBaseProject repository root:
  >&2 echo   %ROOT_DIR%
  >&2 echo This launcher does not exist in SYBaseProjectWeb.
  exit /b 1
)

call "%TARGET_SCRIPT%"
exit /b %ERRORLEVEL%
