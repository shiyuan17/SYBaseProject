#!/usr/bin/env sh
set -eu

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <module-name> <log-file>" >&2
  exit 1
fi

MODULE_NAME="$1"
BACKEND_LOG_FILE="$2"
ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
COMMON_CLASSPATH="../common/common-core/target/classes,../common/common-security/target/classes,../common/common-web/target/classes"

mkdir -p "$(dirname "$BACKEND_LOG_FILE")"
cd "$ROOT_DIR"

run_with_backend_log() {
  status_file=$(mktemp)
  (
    printf '\n[%s] START %s\n' "$(date -u '+%Y-%m-%d %H:%M:%SZ')" "$*"
    set +e
    "$@"
    code=$?
    set -e
    printf '[%s] END EXIT %s\n' "$(date -u '+%Y-%m-%d %H:%M:%SZ')" "$code"
    printf '%s' "$code" >"$status_file"
  ) 2>&1 | tee -a "$BACKEND_LOG_FILE"
  code=$(cat "$status_file" 2>/dev/null || printf '1')
  rm -f "$status_file"
  return "$code"
}

run_with_backend_log ./mvnw -Dmaven.repo.local=.m2/repository -pl "$MODULE_NAME" -am -DskipTests compile

"$ROOT_DIR/scripts/dev/unix/watch-dev-reload.sh" "$MODULE_NAME" "$BACKEND_LOG_FILE" >>"$BACKEND_LOG_FILE" 2>&1 &
WATCHER_PID=$!

cleanup() {
  if kill -0 "$WATCHER_PID" 2>/dev/null; then
    kill "$WATCHER_PID" 2>/dev/null || true
    wait "$WATCHER_PID" 2>/dev/null || true
  fi
}

trap cleanup EXIT INT TERM

set +e
run_with_backend_log ./mvnw -Dmaven.repo.local=.m2/repository -f "$MODULE_NAME/pom.xml" -DskipTests "-Dspring-boot.run.profiles=dev" "-Dspring-boot.run.additional-classpath-elements=${COMMON_CLASSPATH}" spring-boot:run
EXIT_CODE=$?
set -e

exit "$EXIT_CODE"
