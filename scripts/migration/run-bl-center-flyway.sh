#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$ROOT_DIR"

if [ -z "${JAVA_HOME:-}" ]; then
  echo "JAVA_HOME is not set. Please point it to JDK 17." >&2
  exit 1
fi

export BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME="${BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME:-dm.jdbc.driver.DmDriver}"
export BL_CENTER_DATASOURCE_URL="${BL_CENTER_DATASOURCE_URL:-jdbc:dm://127.0.0.1:5236}"
export BL_CENTER_DATASOURCE_USERNAME="${BL_CENTER_DATASOURCE_USERNAME:-SYSDBA}"
export BL_CENTER_DATASOURCE_PASSWORD="${BL_CENTER_DATASOURCE_PASSWORD:-Dm.2027.Pwd.}"

./mvnw -Dmaven.repo.local=.m2/repository -f bl-center/pom.xml -DskipTests spring-boot:run \
  -Dspring-boot.run.mainClass=com.company.bl.tools.BlCenterFlywayCli
