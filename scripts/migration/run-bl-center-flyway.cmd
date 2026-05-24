@echo off
setlocal

set "ROOT_DIR=%~dp0..\.."
pushd "%ROOT_DIR%" >nul
set "EXIT_CODE=0"

set "MODE=%~1"
if not defined MODE set "MODE=sync"

if not defined JAVA_HOME (
  echo JAVA_HOME is not set. Please point it to JDK 17.
  set "EXIT_CODE=1"
  goto :finish
)

if not defined BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME set "BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME=dm.jdbc.driver.DmDriver"
if not defined BL_CENTER_DATASOURCE_URL set "BL_CENTER_DATASOURCE_URL=jdbc:dm://127.0.0.1:5236"
if not defined BL_CENTER_DATASOURCE_USERNAME set "BL_CENTER_DATASOURCE_USERNAME=SYSDBA"
if not defined BL_CENTER_DATASOURCE_PASSWORD set "BL_CENTER_DATASOURCE_PASSWORD=Dm.2027.Pwd."

echo Running bl-center Flyway %MODE%...
call mvnw.cmd -Dmaven.repo.local=.m2/repository -f bl-center/pom.xml -DskipTests -Dspring-boot.run.main-class=com.company.bl.tools.BlCenterFlywayCli -Dspring-boot.run.arguments=%MODE% spring-boot:run
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
