#!/usr/bin/env sh

ensure_dev_java_home() {
  if [ -z "${JAVA_HOME:-}" ]; then
    mkdir -p "$(dirname "$BACKEND_LOG_FILE")"
    echo "JAVA_HOME is not set. Please point it to JDK 17." | tee -a "$BACKEND_LOG_FILE" >&2
    return 1
  fi

  return 0
}

set_dev_common_env() {
  export SECURITY_AUTH_JWT_SM2_PRIVATE_KEY="${SECURITY_AUTH_JWT_SM2_PRIVATE_KEY:-MIGTAgEAMBMGByqGSM49AgEGCCqBHM9VAYItBHkwdwIBAQQgFRpOClbX4u9Hpc+YDvXV7ShD1lfZk4EH0oyGF59PXQGgCgYIKoEcz1UBgi2hRANCAARrIcTZ4A4T3M55LVirPjtxhusNEndt2SmwXo5DfmQ+MHysYOCcFIOqI7YXUlrJ/e3D6owhe9Fo4nF7oZFzafRg}"
  export SECURITY_AUTH_JWT_SM2_PUBLIC_KEY="${SECURITY_AUTH_JWT_SM2_PUBLIC_KEY:-MFkwEwYHKoZIzj0CAQYIKoEcz1UBgi0DQgAEayHE2eAOE9zOeS1Yqz47cYbrDRJ3bdkpsF6OQ35kPjB8rGDgnBSDqiO2F1Jayf3tw+qMIXvRaOJxe6GRc2n0YA==}"
}

set_dev_service_env() {
  service_key=$1
  set_dev_common_env

  case "$service_key" in
    bl)
      export BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME="${BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME:-dm.jdbc.driver.DmDriver}"
      export BL_CENTER_DATASOURCE_URL="${BL_CENTER_DATASOURCE_URL:-jdbc:dm://127.0.0.1:5236}"
      export BL_CENTER_DATASOURCE_USERNAME="${BL_CENTER_DATASOURCE_USERNAME:-SYSDBA}"
      export BL_CENTER_DATASOURCE_PASSWORD="${BL_CENTER_DATASOURCE_PASSWORD:-Dm.2027.Pwd.}"
      ;;
    auth)
      export AUTH_CENTER_DATASOURCE_DRIVER_CLASS_NAME="${AUTH_CENTER_DATASOURCE_DRIVER_CLASS_NAME:-dm.jdbc.driver.DmDriver}"
      export AUTH_CENTER_DATASOURCE_URL="${AUTH_CENTER_DATASOURCE_URL:-jdbc:dm://127.0.0.1:5236}"
      export AUTH_CENTER_DATASOURCE_USERNAME="${AUTH_CENTER_DATASOURCE_USERNAME:-SYSDBA}"
      export AUTH_CENTER_DATASOURCE_PASSWORD="${AUTH_CENTER_DATASOURCE_PASSWORD:-Dm.2027.Pwd.}"
      ;;
    *)
      echo "Unsupported dev service key: $service_key" >&2
      return 1
      ;;
  esac

  return 0
}
