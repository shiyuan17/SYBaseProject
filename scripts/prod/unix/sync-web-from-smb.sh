#!/usr/bin/env sh
set -eu

SOURCE_DIR="${SOURCE_DIR:-/media/fsuser-pc/smbmounts/smb-share:server=10.46.14.76,share=pacssoft/XC}"
WEB_TARGET_DIR="${WEB_TARGET_DIR:-/data/home/fsuser-pc/XC/online/web}"
WEB_ZIP_NAME="${WEB_ZIP_NAME:-xc_web.zip}"
WEB_DIR_NAME="${WEB_DIR_NAME:-xc_web}"

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

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    error "Required command not found: $1"
    exit 1
  fi
}

cleanup() {
  if [ -n "${TMP_UNZIP_DIR:-}" ] && [ -d "$TMP_UNZIP_DIR" ]; then
    rm -rf "$TMP_UNZIP_DIR"
  fi
  if [ -n "${OLD_WEB_DIR:-}" ] && [ -d "$OLD_WEB_DIR" ]; then
    rm -rf "$OLD_WEB_DIR"
  fi
}

trap cleanup EXIT HUP INT TERM

require_command unzip

zip_source="$SOURCE_DIR/$WEB_ZIP_NAME"
zip_target="$WEB_TARGET_DIR/$WEB_ZIP_NAME"
final_web_dir="$WEB_TARGET_DIR/$WEB_DIR_NAME"
OLD_WEB_DIR=""

log "Start Web sync"
log "Source directory: $SOURCE_DIR"
log "Web target directory: $WEB_TARGET_DIR"

require_file "$zip_source"
log "$WEB_ZIP_NAME source found: $zip_source"
mkdir -p "$WEB_TARGET_DIR"
log "Web target directory ready: $WEB_TARGET_DIR"

cp "$zip_source" "$zip_target"
log "$WEB_ZIP_NAME copy success: $zip_source -> $zip_target"

TMP_UNZIP_DIR=$(mktemp -d "$WEB_TARGET_DIR/.${WEB_DIR_NAME}.tmp.XXXXXX")
log "Unzip $WEB_ZIP_NAME to temporary directory: $TMP_UNZIP_DIR"
if unzip -q "$zip_target" -d "$TMP_UNZIP_DIR"; then
  log "$WEB_ZIP_NAME unzip success"
else
  error "$WEB_ZIP_NAME unzip failed"
  exit 1
fi

if [ -d "$TMP_UNZIP_DIR/$WEB_DIR_NAME" ]; then
  log "$WEB_DIR_NAME directory found in zip"
else
  if ! find "$TMP_UNZIP_DIR" -mindepth 1 -maxdepth 1 | grep -q .; then
    error "Zip is empty: $WEB_ZIP_NAME"
    exit 1
  fi

  log "Zip root content will be deployed as $WEB_DIR_NAME"
  mkdir "$TMP_UNZIP_DIR/$WEB_DIR_NAME"
  find "$TMP_UNZIP_DIR" -mindepth 1 -maxdepth 1 ! -name "$WEB_DIR_NAME" -exec mv {} "$TMP_UNZIP_DIR/$WEB_DIR_NAME/" \;
fi

if [ -d "$final_web_dir" ]; then
  OLD_WEB_DIR="$WEB_TARGET_DIR/.${WEB_DIR_NAME}.old.$$"
  log "Move existing $WEB_DIR_NAME directory to temporary backup: $OLD_WEB_DIR"
  mv "$final_web_dir" "$OLD_WEB_DIR"
fi

log "Replace $WEB_DIR_NAME directory: $final_web_dir"
if ! mv "$TMP_UNZIP_DIR/$WEB_DIR_NAME" "$final_web_dir"; then
  if [ -n "$OLD_WEB_DIR" ] && [ -d "$OLD_WEB_DIR" ] && [ ! -e "$final_web_dir" ]; then
    mv "$OLD_WEB_DIR" "$final_web_dir"
    OLD_WEB_DIR=""
  fi
  error "Failed to replace $final_web_dir"
  exit 1
fi

if [ -n "$OLD_WEB_DIR" ] && [ -d "$OLD_WEB_DIR" ]; then
  rm -rf "$OLD_WEB_DIR"
  OLD_WEB_DIR=""
fi

log "$WEB_DIR_NAME directory replace success: $final_web_dir"
log "$WEB_DIR_NAME sync success"
