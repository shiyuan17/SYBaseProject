#!/usr/bin/env sh
set -eu

require_var() {
  var_name="$1"
  eval "var_value=\${$var_name:-}"
  if [ -z "$var_value" ]; then
    echo "Missing required variable: $var_name" >&2
    exit 1
  fi
}

require_var DEPLOY_HOST
require_var DEPLOY_USER
require_var DEPLOY_APP_DIR
require_var DEPLOY_PORT
require_var DEPLOY_SPRING_PROFILE
require_var IMAGE_REPO
require_var IMAGE_TAG
require_var CI_REGISTRY
require_var CI_REGISTRY_USER
require_var CI_REGISTRY_PASSWORD

REMOTE="${DEPLOY_USER}@${DEPLOY_HOST}"
SSH_OPTS="${SSH_OPTS:--o StrictHostKeyChecking=accept-new}"
SERVER_PORT="${SERVER_PORT:-8080}"
CONTAINER_NAME="${CONTAINER_NAME:-sybase-user-center}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m -XX:+UseContainerSupport}"
TZ="${TZ:-Asia/Shanghai}"
APP_DATA_DIR="${APP_DATA_DIR:-${DEPLOY_APP_DIR}/data}"
LOG_MAX_SIZE="${LOG_MAX_SIZE:-10m}"
LOG_MAX_FILE="${LOG_MAX_FILE:-5}"
LOCAL_COMPOSE_FILE="deploy/docker/user-center/docker-compose.yml"
TMP_ENV_FILE="$(mktemp "${TMPDIR:-/tmp}/user-center-deploy-env.XXXXXX")"

cleanup() {
  rm -f "${TMP_ENV_FILE}"
}

trap cleanup EXIT INT TERM

cat > "${TMP_ENV_FILE}" <<EOF
IMAGE_REPO=${IMAGE_REPO}
IMAGE_TAG=${IMAGE_TAG}
CONTAINER_NAME=${CONTAINER_NAME}
HOST_PORT=${DEPLOY_PORT}
SERVER_PORT=${SERVER_PORT}
SPRING_PROFILES_ACTIVE=${DEPLOY_SPRING_PROFILE}
JAVA_OPTS=${JAVA_OPTS}
TZ=${TZ}
APP_DATA_DIR=${APP_DATA_DIR}
LOG_MAX_SIZE=${LOG_MAX_SIZE}
LOG_MAX_FILE=${LOG_MAX_FILE}
EOF

echo "Preparing remote directory ${DEPLOY_APP_DIR} on ${REMOTE}"
# shellcheck disable=SC2086
ssh ${SSH_OPTS} "${REMOTE}" "mkdir -p '${DEPLOY_APP_DIR}' '${APP_DATA_DIR}'"

echo "Uploading compose template and environment file"
# shellcheck disable=SC2086
scp ${SSH_OPTS} "${LOCAL_COMPOSE_FILE}" "${REMOTE}:${DEPLOY_APP_DIR}/docker-compose.yml"
# shellcheck disable=SC2086
scp ${SSH_OPTS} "${TMP_ENV_FILE}" "${REMOTE}:${DEPLOY_APP_DIR}/.env"

echo "Logging in remote host to ${CI_REGISTRY}"
# shellcheck disable=SC2086
printf '%s\n' "${CI_REGISTRY_PASSWORD}" | ssh ${SSH_OPTS} "${REMOTE}" "docker login '${CI_REGISTRY}' --username '${CI_REGISTRY_USER}' --password-stdin"

echo "Pulling and starting ${CONTAINER_NAME}"
# shellcheck disable=SC2086
ssh ${SSH_OPTS} "${REMOTE}" "cd '${DEPLOY_APP_DIR}' && docker compose --env-file .env pull && docker compose --env-file .env up -d --remove-orphans && docker compose ps"
