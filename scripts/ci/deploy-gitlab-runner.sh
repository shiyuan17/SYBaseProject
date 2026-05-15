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
require_var GITLAB_URL
require_var RUNNER_AUTH_TOKEN
require_var RUNNER_NAME

REMOTE="${DEPLOY_USER}@${DEPLOY_HOST}"
SSH_OPTS="${SSH_OPTS:--o StrictHostKeyChecking=accept-new}"
DEPLOY_APP_DIR="${DEPLOY_APP_DIR:-/opt/sybase/gitlab-runner}"
RUNNER_IMAGE="${RUNNER_IMAGE:-gitlab/gitlab-runner:latest}"
RUNNER_CONTAINER_NAME="${RUNNER_CONTAINER_NAME:-sybase-gitlab-runner}"
RUNNER_EXECUTOR="${RUNNER_EXECUTOR:-docker}"
RUNNER_DOCKER_IMAGE="${RUNNER_DOCKER_IMAGE:-alpine:3.20}"
TZ="${TZ:-Asia/Shanghai}"
DOCKER_SOCKET_PATH="${DOCKER_SOCKET_PATH:-/var/run/docker.sock}"
RUNNER_CONFIG_DIR="${RUNNER_CONFIG_DIR:-${DEPLOY_APP_DIR}/config}"
RUNNER_CACHE_DIR="${RUNNER_CACHE_DIR:-${DEPLOY_APP_DIR}/cache}"
FORCE_RE_REGISTER="${FORCE_RE_REGISTER:-0}"
LOCAL_COMPOSE_FILE="deploy/docker/gitlab-runner/docker-compose.yml"
LOCAL_TEMPLATE_FILE="deploy/docker/gitlab-runner/config.template.toml"
TMP_ENV_FILE="$(mktemp "${TMPDIR:-/tmp}/gitlab-runner-deploy-env.XXXXXX")"

cleanup() {
  rm -f "${TMP_ENV_FILE}"
}

trap cleanup EXIT INT TERM

if [ "${RUNNER_EXECUTOR}" != "docker" ]; then
  echo "Unsupported RUNNER_EXECUTOR: ${RUNNER_EXECUTOR}. This script currently supports only docker." >&2
  exit 1
fi

cat > "${TMP_ENV_FILE}" <<EOF
RUNNER_IMAGE=${RUNNER_IMAGE}
RUNNER_CONTAINER_NAME=${RUNNER_CONTAINER_NAME}
TZ=${TZ}
DOCKER_SOCKET_PATH=${DOCKER_SOCKET_PATH}
RUNNER_CONFIG_DIR=${RUNNER_CONFIG_DIR}
RUNNER_CACHE_DIR=${RUNNER_CACHE_DIR}
EOF

echo "Preparing remote directory ${DEPLOY_APP_DIR} on ${REMOTE}"
# shellcheck disable=SC2086
ssh ${SSH_OPTS} "${REMOTE}" "test -S '${DOCKER_SOCKET_PATH}' && mkdir -p '${DEPLOY_APP_DIR}' '${RUNNER_CONFIG_DIR}' '${RUNNER_CACHE_DIR}'"

echo "Uploading compose template and environment files"
# shellcheck disable=SC2086
scp ${SSH_OPTS} "${LOCAL_COMPOSE_FILE}" "${REMOTE}:${DEPLOY_APP_DIR}/docker-compose.yml"
# shellcheck disable=SC2086
scp ${SSH_OPTS} "${LOCAL_TEMPLATE_FILE}" "${REMOTE}:${DEPLOY_APP_DIR}/config.template.toml"
# shellcheck disable=SC2086
scp ${SSH_OPTS} "${TMP_ENV_FILE}" "${REMOTE}:${DEPLOY_APP_DIR}/.env"

echo "Starting ${RUNNER_CONTAINER_NAME}"
# shellcheck disable=SC2086
ssh ${SSH_OPTS} "${REMOTE}" "cd '${DEPLOY_APP_DIR}' && docker compose --env-file .env up -d"

CURRENT_TOKEN=""
# shellcheck disable=SC2086
CURRENT_TOKEN="$(ssh ${SSH_OPTS} "${REMOTE}" "if [ -f '${RUNNER_CONFIG_DIR}/config.toml' ]; then sed -n 's/^[[:space:]]*token = \"\\([^\"]*\\)\"$/\\1/p' '${RUNNER_CONFIG_DIR}/config.toml' | head -n 1; fi")"

if [ "${FORCE_RE_REGISTER}" = "1" ] || [ -z "${CURRENT_TOKEN}" ] || [ "${CURRENT_TOKEN}" != "${RUNNER_AUTH_TOKEN}" ]; then
  echo "Registering ${RUNNER_NAME}"
  # shellcheck disable=SC2086
  ssh ${SSH_OPTS} "${REMOTE}" "if [ -f '${RUNNER_CONFIG_DIR}/config.toml' ]; then cp '${RUNNER_CONFIG_DIR}/config.toml' '${RUNNER_CONFIG_DIR}/config.toml.bak.'\"\$(date +%Y%m%d%H%M%S)\"; rm -f '${RUNNER_CONFIG_DIR}/config.toml'; fi"
  # shellcheck disable=SC2086
  ssh ${SSH_OPTS} "${REMOTE}" "cd '${DEPLOY_APP_DIR}' && docker compose exec -T gitlab-runner gitlab-runner register --non-interactive --template-config /opt/gitlab-runner/config.template.toml --url '${GITLAB_URL}' --token '${RUNNER_AUTH_TOKEN}' --name '${RUNNER_NAME}' --executor '${RUNNER_EXECUTOR}' --docker-image '${RUNNER_DOCKER_IMAGE}'"
  echo "Restarting ${RUNNER_CONTAINER_NAME}"
  # shellcheck disable=SC2086
  ssh ${SSH_OPTS} "${REMOTE}" "cd '${DEPLOY_APP_DIR}' && docker compose restart gitlab-runner"
else
  echo "Runner ${RUNNER_NAME} is already registered with the current token, skipping registration"
fi

echo "Current service status"
# shellcheck disable=SC2086
ssh ${SSH_OPTS} "${REMOTE}" "cd '${DEPLOY_APP_DIR}' && docker compose ps"

echo "Next step: verify the runner in GitLab UI and run a minimal CI job to confirm Docker executor scheduling"
