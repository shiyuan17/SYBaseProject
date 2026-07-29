#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
BACKEND_LOG_FILE="${BACKEND_LOG_FILE:-$ROOT_DIR/.logs/backend.log}"

DEV_HELPER="$ROOT_DIR/scripts/dev/unix/dev-env.sh"

# shellcheck disable=SC1090
. "$DEV_HELPER"

if ! ensure_dev_java_home; then
  exit 1
fi

set_dev_service_env auth

"$ROOT_DIR/scripts/dev/unix/run-dev-service.sh" "auth-center" "$BACKEND_LOG_FILE"
