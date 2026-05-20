@ECHO OFF
SETLOCAL EnableDelayedExpansion

SET "BASE_DIR=%~dp0"
IF "%BASE_DIR:~-1%"=="\" SET "BASE_DIR=%BASE_DIR:~0,-1%"
SET "WRAPPER_DIR=%BASE_DIR%\.mvn\wrapper"
SET WRAPPER_PROPS=%WRAPPER_DIR%\maven-wrapper.properties
SET WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar

IF NOT EXIST "%WRAPPER_PROPS%" (
  ECHO Missing %WRAPPER_PROPS%
  EXIT /B 1
)

IF NOT EXIST "%WRAPPER_JAR%" (
  FOR /F "tokens=1,* delims==" %%A IN (%WRAPPER_PROPS%) DO (
    IF "%%A"=="wrapperUrl" SET WRAPPER_URL=%%B
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -UseBasicParsing '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"
)

SET "JAVA_CMD=java"
IF DEFINED JAVA_HOME SET "JAVA_CMD=%JAVA_HOME%\bin\java.exe"

FOR /F "delims=" %%A IN ('"%JAVA_CMD%" -version 2^>^&1') DO (
  SET "JAVA_VERSION_LINE=%%A"
  GOTO check_java_version
)

:check_java_version
echo(!JAVA_VERSION_LINE! | findstr /C:"17." >NUL
IF ERRORLEVEL 1 (
  ECHO SY Base Project requires JDK 17 for Maven Wrapper builds.
  ECHO Resolved Java: %JAVA_CMD%
  ECHO Version line: !JAVA_VERSION_LINE!
  ECHO Set JAVA_HOME to a JDK 17 installation and retry.
  EXIT /B 1
)

"%JAVA_CMD%" -Dmaven.multiModuleProjectDirectory="%BASE_DIR%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
