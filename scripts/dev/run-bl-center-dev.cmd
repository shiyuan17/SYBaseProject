@echo off
setlocal

set "ROOT_DIR=%~dp0..\.."
pushd "%ROOT_DIR%" >nul

if not defined JAVA_HOME (
  echo JAVA_HOME is not set. Please point it to JDK 17.
  popd >nul
  exit /b 1
)

if not defined BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME set "BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME=dm.jdbc.driver.DmDriver"
if not defined BL_CENTER_DATASOURCE_URL set "BL_CENTER_DATASOURCE_URL=jdbc:dm://127.0.0.1:5236"
if not defined BL_CENTER_DATASOURCE_USERNAME set "BL_CENTER_DATASOURCE_USERNAME=SYSDBA"
if not defined BL_CENTER_DATASOURCE_PASSWORD set "BL_CENTER_DATASOURCE_PASSWORD=Dm.2027.Pwd."
if not defined SECURITY_AUTH_JWT_SM2_PRIVATE_KEY set "SECURITY_AUTH_JWT_SM2_PRIVATE_KEY=MIGTAgEAMBMGByqGSM49AgEGCCqBHM9VAYItBHkwdwIBAQQgFRpOClbX4u9Hpc+YDvXV7ShD1lfZk4EH0oyGF59PXQGgCgYIKoEcz1UBgi2hRANCAARrIcTZ4A4T3M55LVirPjtxhusNEndt2SmwXo5DfmQ+MHysYOCcFIOqI7YXUlrJ/e3D6owhe9Fo4nF7oZFzafRg"
if not defined SECURITY_AUTH_JWT_SM2_PUBLIC_KEY set "SECURITY_AUTH_JWT_SM2_PUBLIC_KEY=MFkwEwYHKoZIzj0CAQYIKoEcz1UBgi0DQgAEayHE2eAOE9zOeS1Yqz47cYbrDRJ3bdkpsF6OQ35kPjB8rGDgnBSDqiO2F1Jayf3tw+qMIXvRaOJxe6GRc2n0YA=="

call mvnw.cmd -Dmaven.repo.local=.m2/repository -f bl-center/pom.xml -DskipTests spring-boot:run
set "EXIT_CODE=%ERRORLEVEL%"

popd >nul
exit /b %EXIT_CODE%
