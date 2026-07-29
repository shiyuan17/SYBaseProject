#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
RUN_SCRIPT="$SCRIPT_DIR/run-centers.sh"
PROD_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
BASE_CONFIG_FILE="${RUN_CENTERS_BASE_CONFIG_FILE:-$PROD_DIR/config/run-centers.conf}"
PROD06_CONFIG_FILE="${RUN_CENTERS_PROD06_CONFIG_FILE:-$PROD_DIR/config/run-centers-prod-06.conf}"

require_file() {
  if [ ! -f "$1" ]; then
    echo "Required file not found: $1" >&2
    exit 1
  fi
}

require_file "$RUN_SCRIPT"
require_file "$PROD06_CONFIG_FILE"

export RUN_CENTERS_BASE_CONFIG_FILE="$BASE_CONFIG_FILE"
export RUN_CENTERS_CONFIG_FILE="$PROD06_CONFIG_FILE"

exec "$RUN_SCRIPT" "$@"
