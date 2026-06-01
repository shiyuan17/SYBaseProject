#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$ROOT_DIR"

MODE=${1:-sync}

if [ -z "${JAVA_HOME:-}" ]; then
  echo "JAVA_HOME is not set. Please point it to JDK 17." >&2
  exit 1
fi

export BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME="${BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME:-dm.jdbc.driver.DmDriver}"
export BL_CENTER_DATASOURCE_URL="${BL_CENTER_DATASOURCE_URL:-jdbc:dm://127.0.0.1:5236}"
export BL_CENTER_DATASOURCE_USERNAME="${BL_CENTER_DATASOURCE_USERNAME:-SYSDBA}"
export BL_CENTER_DATASOURCE_PASSWORD="${BL_CENTER_DATASOURCE_PASSWORD:-Dm.2027.Pwd.}"
export SECURITY_AUTH_JWT_SM2_PRIVATE_KEY="${SECURITY_AUTH_JWT_SM2_PRIVATE_KEY:-MIGTAgEAMBMGByqGSM49AgEGCCqBHM9VAYItBHkwdwIBAQQgFRpOClbX4u9Hpc+YDvXV7ShD1lfZk4EH0oyGF59PXQGgCgYIKoEcz1UBgi2hRANCAARrIcTZ4A4T3M55LVirPjtxhusNEndt2SmwXo5DfmQ+MHysYOCcFIOqI7YXUlrJ/e3D6owhe9Fo4nF7oZFzafRg}"
export SECURITY_AUTH_JWT_SM2_PUBLIC_KEY="${SECURITY_AUTH_JWT_SM2_PUBLIC_KEY:-MFkwEwYHKoZIzj0CAQYIKoEcz1UBgi0DQgAEayHE2eAOE9zOeS1Yqz47cYbrDRJ3bdkpsF6OQ35kPjB8rGDgnBSDqiO2F1Jayf3tw+qMIXvRaOJxe6GRc2n0YA==}"

echo "Preparing bl-center dependencies..."
./mvnw -Dmaven.repo.local=.m2/repository -pl bl-center -am -Dmaven.test.skip=true -Djacoco.skip=true install

echo "Running bl-center Flyway ${MODE}..."
./mvnw -Dmaven.repo.local=.m2/repository -f bl-center/pom.xml -Dmaven.test.skip=true -Djacoco.skip=true spring-boot:run \
  -Dspring-boot.run.useTestClasspath=false \
  -Dspring-boot.run.main-class=com.company.bl.tools.BlCenterFlywayCli \
  -Dspring-boot.run.arguments="${MODE}"
