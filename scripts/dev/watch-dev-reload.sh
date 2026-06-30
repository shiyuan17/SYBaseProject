#!/usr/bin/env sh
set -eu

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <module-name> <log-file>" >&2
  exit 1
fi

MODULE_NAME="$1"
BACKEND_LOG_FILE="$2"
ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
SERVICE_SOURCE_PATH="$ROOT_DIR/$MODULE_NAME/src/main"
TRIGGER_FILE="$ROOT_DIR/$MODULE_NAME/target/classes/.reloadtrigger"
SHARED_SOURCE_PATHS="
$ROOT_DIR/common/common-core/src/main
$ROOT_DIR/common/common-security/src/main
$ROOT_DIR/common/common-web/src/main
"

if command -v shasum >/dev/null 2>&1; then
  HASH_CMD="shasum"
elif command -v sha1sum >/dev/null 2>&1; then
  HASH_CMD="sha1sum"
else
  echo "Missing shasum/sha1sum for hot reload watcher." >>"$BACKEND_LOG_FILE"
  exit 1
fi

log() {
  printf '[%s] [hot-reload] %s\n' "$(date -u '+%Y-%m-%d %H:%M:%SZ')" "$1" >>"$BACKEND_LOG_FILE"
}

fingerprint_path() {
  path="$1"
  if [ ! -d "$path" ]; then
    return 0
  fi

  find "$path" -type f \
    \( -name '*.java' -o -name '*.properties' -o -name '*.yml' -o -name '*.yaml' -o -name '*.xml' -o -name '*.sql' \) \
    -print 2>/dev/null \
    | LC_ALL=C sort \
    | while IFS= read -r file; do
        if [ -f "$file" ]; then
          case "$HASH_CMD" in
            shasum)
              shasum "$file"
              ;;
            sha1sum)
              sha1sum "$file"
              ;;
          esac
        fi
      done
}

compute_fingerprint() {
  (
    fingerprint_path "$SERVICE_SOURCE_PATH"
    printf '%s' "$SHARED_SOURCE_PATHS" | while IFS= read -r shared_path; do
      if [ -n "$shared_path" ]; then
        fingerprint_path "$shared_path"
      fi
    done
  ) | "$HASH_CMD" | awk '{print $1}'
}

touch_trigger_file() {
  mkdir -p "$(dirname "$TRIGGER_FILE")"
  : >"$TRIGGER_FILE"
}

run_compile() {
  ./mvnw -Dmaven.repo.local=.m2/repository -pl "$MODULE_NAME" -am -DskipTests compile >>"$BACKEND_LOG_FILE" 2>&1
}

LAST_FINGERPRINT=$(compute_fingerprint)
log "Watching $MODULE_NAME sources for hot reload changes."

while true; do
  sleep 2
  NEXT_FINGERPRINT=$(compute_fingerprint)

  if [ "$NEXT_FINGERPRINT" = "$LAST_FINGERPRINT" ]; then
    continue
  fi

  log "Detected source change. Running incremental compile."
  if run_compile; then
    touch_trigger_file
    log "Compile succeeded and restart trigger updated."
  else
    log "Compile failed."
  fi

  LAST_FINGERPRINT="$NEXT_FINGERPRINT"
done
