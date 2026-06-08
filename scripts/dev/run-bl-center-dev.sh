#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
BACKEND_LOG_FILE="${BACKEND_LOG_FILE:-$ROOT_DIR/.logs/backend.log}"
cd "$ROOT_DIR"

run_with_backend_log() {
  mkdir -p "$(dirname "$BACKEND_LOG_FILE")"
  status_file=$(mktemp)
  (
    printf '\n[%s] START %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*"
    set +e
    "$@"
    code=$?
    set -e
    printf '[%s] END EXIT %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$code"
    printf '%s' "$code" >"$status_file"
  ) 2>&1 | tee -a "$BACKEND_LOG_FILE"
  code=$(cat "$status_file" 2>/dev/null || printf '1')
  rm -f "$status_file"
  return "$code"
}

if [ -z "${JAVA_HOME:-}" ]; then
  mkdir -p "$(dirname "$BACKEND_LOG_FILE")"
  echo "JAVA_HOME is not set. Please point it to JDK 17." | tee -a "$BACKEND_LOG_FILE" >&2
  exit 1
fi

export BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME="${BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME:-dm.jdbc.driver.DmDriver}"
export BL_CENTER_DATASOURCE_URL="${BL_CENTER_DATASOURCE_URL:-jdbc:dm://127.0.0.1:5236}"
export BL_CENTER_DATASOURCE_USERNAME="${BL_CENTER_DATASOURCE_USERNAME:-SYSDBA}"
export BL_CENTER_DATASOURCE_PASSWORD="${BL_CENTER_DATASOURCE_PASSWORD:-Dm.2027.Pwd.}"
export SECURITY_AUTH_JWT_SM2_PRIVATE_KEY="${SECURITY_AUTH_JWT_SM2_PRIVATE_KEY:-MIGTAgEAMBMGByqGSM49AgEGCCqBHM9VAYItBHkwdwIBAQQgFRpOClbX4u9Hpc+YDvXV7ShD1lfZk4EH0oyGF59PXQGgCgYIKoEcz1UBgi2hRANCAARrIcTZ4A4T3M55LVirPjtxhusNEndt2SmwXo5DfmQ+MHysYOCcFIOqI7YXUlrJ/e3D6owhe9Fo4nF7oZFzafRg}"
export SECURITY_AUTH_JWT_SM2_PUBLIC_KEY="${SECURITY_AUTH_JWT_SM2_PUBLIC_KEY:-MFkwEwYHKoZIzj0CAQYIKoEcz1UBgi0DQgAEayHE2eAOE9zOeS1Yqz47cYbrDRJ3bdkpsF6OQ35kPjB8rGDgnBSDqiO2F1Jayf3tw+qMIXvRaOJxe6GRc2n0YA==}"

run_with_backend_log ./mvnw -Dmaven.repo.local=.m2/repository -f bl-center/pom.xml -DskipTests spring-boot:run
