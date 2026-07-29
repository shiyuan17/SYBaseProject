@echo off
setlocal

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-centers-dev.ps1" %*
exit /b %ERRORLEVEL%
