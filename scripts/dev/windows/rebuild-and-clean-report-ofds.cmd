@echo off
setlocal

set "ROOT_DIR=%~dp0..\..\.."
pushd "%ROOT_DIR%" >nul

if /I not "%BL_CENTER_DATASOURCE_URL%"=="" if /I not "%BL_CENTER_DATASOURCE_URL%"=="jdbc:dm://127.0.0.1:5236" (
  >&2 echo Refusing to run: BL_CENTER_DATASOURCE_URL is not the local development DM database.
  popd
  exit /b 3
)
if /I not "%BL_REPORT_STORAGE_ROOT%"=="" (
  >&2 echo Refusing to run: BL_REPORT_STORAGE_ROOT must be unset so the default local report root is used.
  popd
  exit /b 3
)

call "%~dp0set-dev-service-env.cmd" bl
if errorlevel 1 (
  popd
  exit /b 3
)

call .\mvnw.cmd -pl bl-center -DskipTests "-Dspring-boot.run.profiles=dev" "-Dspring-boot.run.arguments=--spring.main.web-application-type=none --bl.report-maintenance.rebuild-and-clean=true --bl.report-maintenance.confirm=LOCAL_DEV_ONLY" spring-boot:run
set "EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %EXIT_CODE%
