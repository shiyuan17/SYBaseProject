@echo off
setlocal EnableExtensions EnableDelayedExpansion
goto :main

:apply_local_defaults
set "SERVICE_LABEL=%~1"
set "JDBC_URL=jdbc:dm://127.0.0.1:5236"
set "USERNAME=SYSDBA"
set "PASSWORD=Dm.2027.Pwd."
echo Using default local datasource for %SERVICE_LABEL%.
exit /b 0

:collect_service
set "SERVICE_LABEL=%~1"
set "URL_VAR=%~2"
set "USERNAME_VAR=%~3"
set "PASSWORD_VAR=%~4"
call set "JDBC_URL=%%%URL_VAR%%%"
call set "USERNAME=%%%USERNAME_VAR%%%"
call set "PASSWORD=%%%PASSWORD_VAR%%%"

set "HAS_URL=0"
set "HAS_USERNAME=0"
set "HAS_PASSWORD=0"
if defined JDBC_URL set "HAS_URL=1"
if defined USERNAME set "HAS_USERNAME=1"
if defined PASSWORD set "HAS_PASSWORD=1"

if "%HAS_URL%%HAS_USERNAME%%HAS_PASSWORD%"=="000" (
  call :apply_local_defaults "%SERVICE_LABEL%"
  set "HAS_URL=1"
  set "HAS_USERNAME=1"
  set "HAS_PASSWORD=1"
)

set "MISSING_VARS="
if not defined JDBC_URL set "MISSING_VARS=%URL_VAR%"
if not defined USERNAME (
  if defined MISSING_VARS (
    set "MISSING_VARS=%MISSING_VARS%, %USERNAME_VAR%"
  ) else (
    set "MISSING_VARS=%USERNAME_VAR%"
  )
)
if not defined PASSWORD (
  if defined MISSING_VARS (
    set "MISSING_VARS=%MISSING_VARS%, %PASSWORD_VAR%"
  ) else (
    set "MISSING_VARS=%PASSWORD_VAR%"
  )
)
if defined MISSING_VARS (
  echo %SERVICE_LABEL% datasource configuration is incomplete. Missing: %MISSING_VARS%
  set "EXIT_CODE=1"
  exit /b 1
)

set "MATCH_INDEX="
for /L %%I in (1,1,%CONFIG_COUNT%) do (
  if /I "!CONFIG_%%I_URL!"=="%JDBC_URL%" if /I "!CONFIG_%%I_USERNAME!"=="%USERNAME%" (
    set "MATCH_INDEX=%%I"
  )
)

if defined MATCH_INDEX (
  echo Deduplicating %SERVICE_LABEL% with !CONFIG_%MATCH_INDEX%_LABELS! because URL and username match.
  set "CONFIG_%MATCH_INDEX%_LABELS=!CONFIG_%MATCH_INDEX%_LABELS!, %SERVICE_LABEL%"
  exit /b 0
)

set /a CONFIG_COUNT+=1
set "CONFIG_%CONFIG_COUNT%_LABELS=%SERVICE_LABEL%"
set "CONFIG_%CONFIG_COUNT%_URL=%JDBC_URL%"
set "CONFIG_%CONFIG_COUNT%_USERNAME=%USERNAME%"
set "CONFIG_%CONFIG_COUNT%_PASSWORD=%PASSWORD%"
exit /b 0

:resolve_dexp
set "DEXP_CMD="
if defined DM_EXPORT_TOOL (
  if exist "%DM_EXPORT_TOOL%" set "DEXP_CMD=%DM_EXPORT_TOOL%"
)
if not defined DEXP_CMD (
  for %%C in (dexp.exe dexp.cmd dexp) do (
    if not defined DEXP_CMD (
      for /f "delims=" %%I in ('where %%C 2^>nul') do (
        if not defined DEXP_CMD set "DEXP_CMD=%%I"
      )
    )
  )
)
if not defined DEXP_CMD if defined DM_HOME (
  for %%C in ("%DM_HOME%\bin\dexp.exe" "%DM_HOME%\bin\dexp.cmd" "%DM_HOME%\bin\dexp") do (
    if not defined DEXP_CMD if exist %%~fC set "DEXP_CMD=%%~fC"
  )
)
if not defined DEXP_CMD (
  echo Unable to locate dexp. Set DM_EXPORT_TOOL or DM_HOME, or add dexp to PATH.
  set "EXIT_CODE=1"
  exit /b 1
)
echo dexp_command=%DEXP_CMD%
exit /b 0

:jdbc_to_connect_target
set "JDBC_URL=%~1"
set "TARGET_VAR=%~2"
set "CONNECT_TARGET=%JDBC_URL:jdbc:dm://=%"
if "%CONNECT_TARGET%"=="%JDBC_URL%" (
  set "%TARGET_VAR%="
  exit /b 0
)
for /f "tokens=1 delims=?" %%I in ("%CONNECT_TARGET%") do set "CONNECT_TARGET=%%I"
set "%TARGET_VAR%=%CONNECT_TARGET%"
exit /b 0

:build_file_stem
set "INDEX=%~1"
set "TARGET_VAR=%~2"
if not defined TIMESTAMP (
  for /f %%I in ('powershell -NoProfile -Command "(Get-Date).ToString('yyyyMMdd_HHmmss')"') do set "TIMESTAMP=%%I"
)
set "RAW_LABELS=!CONFIG_%INDEX%_LABELS!"
set "SANITIZED_LABELS=!RAW_LABELS:, =-!"
set "SANITIZED_LABELS=!SANITIZED_LABELS: =!"
set "SANITIZED_USERNAME=!CONFIG_%INDEX%_USERNAME!"
set "SANITIZED_USERNAME=!SANITIZED_USERNAME:\=_!"
set "SANITIZED_USERNAME=!SANITIZED_USERNAME:/=_!"
set "SANITIZED_USERNAME=!SANITIZED_USERNAME::=_!"
set "SANITIZED_USERNAME=!SANITIZED_USERNAME: =_!"
set "%TARGET_VAR%=!SANITIZED_LABELS!-!SANITIZED_USERNAME!-!TIMESTAMP!"
exit /b 0

:append_summary
set "SUMMARY_VAR=%~1"
set "SUMMARY_VALUE=%~2"
call set "CURRENT_VALUE=%%%SUMMARY_VAR%%%"
if defined CURRENT_VALUE (
  set "%SUMMARY_VAR%=%CURRENT_VALUE%, %SUMMARY_VALUE%"
) else (
  set "%SUMMARY_VAR%=%SUMMARY_VALUE%"
)
exit /b 0

:run_export
set "INDEX=%~1"
set "SERVICE_LABELS=!CONFIG_%INDEX%_LABELS!"
set "JDBC_URL=!CONFIG_%INDEX%_URL!"
set "USERNAME=!CONFIG_%INDEX%_USERNAME!"
set "PASSWORD=!CONFIG_%INDEX%_PASSWORD!"
set "CONNECT_TARGET="
call :jdbc_to_connect_target "!JDBC_URL!" CONNECT_TARGET
if not defined CONNECT_TARGET (
  echo Failed to convert JDBC URL to DM export target: !JDBC_URL!
  set /a FAIL_COUNT+=1
  call :append_summary FAIL_SUMMARY "!SERVICE_LABELS!"
  set "EXIT_CODE=1"
  exit /b 0
)

set "FILE_STEM="
call :build_file_stem "%INDEX%" FILE_STEM
set "DUMP_FILE=!FILE_STEM!.dmp"
set "LOG_FILE=!FILE_STEM!.log"

echo Starting export for !SERVICE_LABELS!
echo   jdbc_url=!JDBC_URL!
echo   dump_file=%OUTPUT_DIR%\!DUMP_FILE!
echo   log_file=%OUTPUT_DIR%\!LOG_FILE!

call "%DEXP_CMD%" USERID=!USERNAME!/!PASSWORD!@!CONNECT_TARGET! FULL=Y DIRECTORY="%OUTPUT_DIR%" FILE=!DUMP_FILE! LOG=!LOG_FILE!
if errorlevel 1 (
  echo Export failed for !SERVICE_LABELS!
  set /a FAIL_COUNT+=1
  call :append_summary FAIL_SUMMARY "!SERVICE_LABELS!"
  set "EXIT_CODE=1"
) else (
  echo Export completed for !SERVICE_LABELS!
  set /a EXPORT_COUNT+=1
  call :append_summary SUCCESS_SUMMARY "!SERVICE_LABELS! -> %OUTPUT_DIR%\!DUMP_FILE!"
)
exit /b 0

:pause_on_error
set "ERROR_CODE=%~1"
if "%PAUSE_ON_ERROR%"=="0" goto :eof
if defined CI goto :eof
echo.
echo Script failed with exit code %ERROR_CODE%.
echo Press any key to close this window...
pause >nul
goto :eof

:main
set "ROOT_DIR=%~dp0..\.."
pushd "%ROOT_DIR%" >nul
set "EXIT_CODE=0"

set "OUTPUT_DIR=%~1"
if not defined OUTPUT_DIR set "OUTPUT_DIR=%DB_EXPORT_OUTPUT_DIR%"
if not defined OUTPUT_DIR (
  for /f %%I in ('powershell -NoProfile -Command "(Get-Date).ToString('yyyyMMdd_HHmmss')"') do set "TIMESTAMP=%%I"
  set "OUTPUT_DIR=tmp\db-export\!TIMESTAMP!"
)
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%" >nul 2>nul
if errorlevel 1 (
  echo Failed to create output directory: %OUTPUT_DIR%
  set "EXIT_CODE=1"
  goto :finish
)

call :resolve_dexp
if errorlevel 1 goto :finish

set "CONFIG_COUNT=0"
set "EXPORT_COUNT=0"
set "FAIL_COUNT=0"
set "SUCCESS_SUMMARY="
set "FAIL_SUMMARY="

call :collect_service "auth-center" "AUTH_CENTER_DATASOURCE_URL" "AUTH_CENTER_DATASOURCE_USERNAME" "AUTH_CENTER_DATASOURCE_PASSWORD"
if errorlevel 1 goto :finish
call :collect_service "bl-center" "BL_CENTER_DATASOURCE_URL" "BL_CENTER_DATASOURCE_USERNAME" "BL_CENTER_DATASOURCE_PASSWORD"
if errorlevel 1 goto :finish

if "%CONFIG_COUNT%"=="0" (
  echo No datasource configuration found. Set AUTH_CENTER_DATASOURCE_* and/or BL_CENTER_DATASOURCE_*.
  set "EXIT_CODE=1"
  goto :finish
)

echo output_dir=%OUTPUT_DIR%
echo unique_export_targets=%CONFIG_COUNT%

for /L %%I in (1,1,%CONFIG_COUNT%) do call :run_export %%I

echo export_count=%EXPORT_COUNT%
echo failure_count=%FAIL_COUNT%
if defined SUCCESS_SUMMARY echo successful_exports=!SUCCESS_SUMMARY!
if defined FAIL_SUMMARY echo failed_exports=!FAIL_SUMMARY!

:finish
popd >nul
if not "%EXIT_CODE%"=="0" call :pause_on_error %EXIT_CODE%
exit /b %EXIT_CODE%
