@echo off
setlocal EnableExtensions

set "ROOT_DIR=%~dp0..\.."
pushd "%ROOT_DIR%" >nul
set "EXIT_CODE=0"

set "OUTPUT_PATH="
if not "%~1"=="" if not "%~1:~0,2%"=="--" (
  set "OUTPUT_PATH=%~1"
  shift
)

set "EXTRA_ARGS="
:collect_args
if "%~1"=="" goto after_args
if defined EXTRA_ARGS (
  set "EXTRA_ARGS=%EXTRA_ARGS% %~1"
) else (
  set "EXTRA_ARGS=%~1"
)
shift
goto collect_args

:after_args
if not defined JAVA_HOME (
  echo JAVA_HOME is not set. Please point it to JDK 17.
  set "EXIT_CODE=1"
  goto :finish
)

if not defined AUTH_CENTER_DATASOURCE_DRIVER_CLASS_NAME set "AUTH_CENTER_DATASOURCE_DRIVER_CLASS_NAME=dm.jdbc.driver.DmDriver"
if not defined AUTH_CENTER_DATASOURCE_URL set "AUTH_CENTER_DATASOURCE_URL=jdbc:dm://127.0.0.1:5236"
if not defined AUTH_CENTER_DATASOURCE_USERNAME set "AUTH_CENTER_DATASOURCE_USERNAME=SYSDBA"
if not defined AUTH_CENTER_DATASOURCE_PASSWORD set "AUTH_CENTER_DATASOURCE_PASSWORD=Dm.2027.Pwd."

if not defined BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME set "BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME=dm.jdbc.driver.DmDriver"
if not defined BL_CENTER_DATASOURCE_URL set "BL_CENTER_DATASOURCE_URL=jdbc:dm://127.0.0.1:5236"
if not defined BL_CENTER_DATASOURCE_USERNAME set "BL_CENTER_DATASOURCE_USERNAME=SYSDBA"
if not defined BL_CENTER_DATASOURCE_PASSWORD set "BL_CENTER_DATASOURCE_PASSWORD=Dm.2027.Pwd."

if not defined MVNW_CMD set "MVNW_CMD=mvnw.cmd"

echo Preparing app-cli dependencies...
call "%MVNW_CMD%" -Dmaven.repo.local=.m2/repository -pl tools/app-cli -am -Dmaven.test.skip=true install
if errorlevel 1 (
  set "EXIT_CODE=%ERRORLEVEL%"
  goto :finish
)

set "RUN_ARGUMENTS=database dictionary-html"
if defined OUTPUT_PATH set "RUN_ARGUMENTS=%RUN_ARGUMENTS% --output=""%OUTPUT_PATH%"""
if defined EXTRA_ARGS set "RUN_ARGUMENTS=%RUN_ARGUMENTS% %EXTRA_ARGS%"

echo Running database dictionary HTML generation...
call "%MVNW_CMD%" -Dmaven.repo.local=.m2/repository -f tools/app-cli/pom.xml -Dmaven.test.skip=true -Dspring-boot.run.useTestClasspath=false "-Dspring-boot.run.arguments=%RUN_ARGUMENTS%" spring-boot:run
set "EXIT_CODE=%ERRORLEVEL%"

:finish
popd >nul
exit /b %EXIT_CODE%
