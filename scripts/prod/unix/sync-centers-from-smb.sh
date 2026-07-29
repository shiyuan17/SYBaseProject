#!/usr/bin/env sh
set -eu

SOURCE_DIR="${SOURCE_DIR:-/media/fsuser-pc/smbmounts/smb-share:server=10.46.14.76,share=pacssoft/XC}"
API_TARGET_DIR="${API_TARGET_DIR:-/data/home/fsuser-pc/XC/online/api}"
BL_JAR_NAME="${BL_JAR_NAME:-bl-center.jar}"
AUTH_JAR_NAME="${AUTH_JAR_NAME:-auth-center.jar}"
RUN_CENTERS_SCRIPT="${RUN_CENTERS_SCRIPT:-run-centers.sh}"
RUN_CENTERS_ACTION="${RUN_CENTERS_ACTION:-restart}"
RUN_CENTERS_SERVICE="${RUN_CENTERS_SERVICE:-all}"

log() {
  printf '[%s] [INFO] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

error() {
  printf '[%s] [ERROR] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*" >&2
}

require_file() {
  if [ ! -f "$1" ]; then
    error "Required file not found: $1"
    exit 1
  fi
}

copy_file() {
  source_path=$1
  target_path=$2

  cp "$source_path" "$target_path"
  log "$(basename "$target_path") copy success: $source_path -> $target_path"
}

bl_source="$SOURCE_DIR/$BL_JAR_NAME"
auth_source="$SOURCE_DIR/$AUTH_JAR_NAME"
run_centers_path="$API_TARGET_DIR/$RUN_CENTERS_SCRIPT"

log "Start Java center sync"
log "Source directory: $SOURCE_DIR"
log "API target directory: $API_TARGET_DIR"

require_file "$bl_source"
log "$BL_JAR_NAME source found: $bl_source"
require_file "$auth_source"
log "$AUTH_JAR_NAME source found: $auth_source"

mkdir -p "$API_TARGET_DIR"
log "API target directory ready: $API_TARGET_DIR"
copy_file "$bl_source" "$API_TARGET_DIR/$BL_JAR_NAME"
copy_file "$auth_source" "$API_TARGET_DIR/$AUTH_JAR_NAME"

require_file "$run_centers_path"
if [ ! -x "$run_centers_path" ]; then
  error "run-centers script is not executable: $run_centers_path"
  exit 1
fi

log "Run $RUN_CENTERS_SCRIPT $RUN_CENTERS_ACTION $RUN_CENTERS_SERVICE in $API_TARGET_DIR"
if (
  cd "$API_TARGET_DIR"
  "./$RUN_CENTERS_SCRIPT" "$RUN_CENTERS_ACTION" "$RUN_CENTERS_SERVICE"
); then
  log "$RUN_CENTERS_SCRIPT $RUN_CENTERS_ACTION $RUN_CENTERS_SERVICE success"
  log "Java center sync success"
else
  error "$RUN_CENTERS_SCRIPT $RUN_CENTERS_ACTION $RUN_CENTERS_SERVICE failed"
  exit 1
fi
