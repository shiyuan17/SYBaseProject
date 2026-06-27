#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$ROOT_DIR"

OUTPUT_PATH=
if [ "${1:-}" != "" ] && [ "${1#--}" = "$1" ]; then
  OUTPUT_PATH=$1
  shift
fi

if [ -z "${JAVA_HOME:-}" ]; then
  echo "JAVA_HOME is not set. Please point it to JDK 17." >&2
  exit 1
fi

export AUTH_CENTER_DATASOURCE_DRIVER_CLASS_NAME="${AUTH_CENTER_DATASOURCE_DRIVER_CLASS_NAME:-dm.jdbc.driver.DmDriver}"
export AUTH_CENTER_DATASOURCE_URL="${AUTH_CENTER_DATASOURCE_URL:-jdbc:dm://127.0.0.1:5236}"
export AUTH_CENTER_DATASOURCE_USERNAME="${AUTH_CENTER_DATASOURCE_USERNAME:-SYSDBA}"
export AUTH_CENTER_DATASOURCE_PASSWORD="${AUTH_CENTER_DATASOURCE_PASSWORD:-Dm.2027.Pwd.}"

export BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME="${BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME:-dm.jdbc.driver.DmDriver}"
export BL_CENTER_DATASOURCE_URL="${BL_CENTER_DATASOURCE_URL:-jdbc:dm://127.0.0.1:5236}"
export BL_CENTER_DATASOURCE_USERNAME="${BL_CENTER_DATASOURCE_USERNAME:-SYSDBA}"
export BL_CENTER_DATASOURCE_PASSWORD="${BL_CENTER_DATASOURCE_PASSWORD:-Dm.2027.Pwd.}"

MVNW_BIN=${MVNW_BIN:-./mvnw}

echo "Preparing app-cli dependencies..."
"$MVNW_BIN" -Dmaven.repo.local=.m2/repository -pl tools/app-cli -am -Dmaven.test.skip=true install

RUN_ARGUMENTS="database dictionary-html"
if [ -n "$OUTPUT_PATH" ]; then
  RUN_ARGUMENTS="$RUN_ARGUMENTS --output=\"$OUTPUT_PATH\""
fi
if [ "$#" -gt 0 ]; then
  RUN_ARGUMENTS="$RUN_ARGUMENTS $*"
fi

echo "Running database dictionary HTML generation..."
"$MVNW_BIN" -Dmaven.repo.local=.m2/repository -f tools/app-cli/pom.xml -Dmaven.test.skip=true -Dspring-boot.run.useTestClasspath=false "-Dspring-boot.run.arguments=${RUN_ARGUMENTS}" spring-boot:run
