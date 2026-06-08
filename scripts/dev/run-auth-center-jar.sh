#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
APP_NAME="auth-center"
JAR_NAME="${AUTH_CENTER_JAR_NAME:-auth-center-0.1.0-SNAPSHOT-exec.jar}"
JAR_PATH="${AUTH_CENTER_JAR_PATH:-$ROOT_DIR/auth-center/target/$JAR_NAME}"
RUNTIME_DIR="${AUTH_CENTER_RUNTIME_DIR:-$ROOT_DIR/tmp/dev-services}"
PID_FILE="$RUNTIME_DIR/$APP_NAME.pid"
LOG_FILE="${AUTH_CENTER_LOG_FILE:-$ROOT_DIR/.logs/backend.log}"
ACTION="${1:-start}"

if [ "$#" -gt 0 ]; then
  shift
fi

if [ ! -f "$JAR_PATH" ] && [ -f "$PWD/$JAR_NAME" ]; then
  JAR_PATH="$PWD/$JAR_NAME"
fi

usage() {
  echo "Usage: $0 {start|stop|pause|resume|restart|status|logs} [args]"
}

require_java() {
  if ! command -v java >/dev/null 2>&1; then
    echo "java is not available in PATH." >&2
    exit 1
  fi
}

require_jar() {
  if [ ! -f "$JAR_PATH" ]; then
    echo "Auth jar not found: $JAR_PATH" >&2
    echo "Set AUTH_CENTER_JAR_PATH or place $JAR_NAME in the current directory." >&2
    exit 1
  fi
}

ensure_runtime_dir() {
  mkdir -p "$RUNTIME_DIR"
  mkdir -p "$(dirname "$LOG_FILE")"
}

running_pid() {
  if [ ! -f "$PID_FILE" ]; then
    return 1
  fi

  pid=$(cat "$PID_FILE")
  if [ -z "$pid" ]; then
    rm -f "$PID_FILE"
    return 1
  fi

  if kill -0 "$pid" >/dev/null 2>&1; then
    printf '%s\n' "$pid"
    return 0
  fi

  rm -f "$PID_FILE"
  return 1
}

process_state() {
  if ! pid=$(running_pid); then
    return 1
  fi

  state=$(ps -o stat= -p "$pid" 2>/dev/null | tr -d ' ')
  if [ -z "$state" ]; then
    rm -f "$PID_FILE"
    return 1
  fi

  printf '%s\n' "$state"
}

start_app() {
  require_java
  require_jar
  ensure_runtime_dir

  if pid=$(running_pid); then
    echo "$APP_NAME is already running with PID $pid"
    return 0
  fi

  touch "$LOG_FILE"
  nohup java ${JAVA_OPTS:-} -jar "$JAR_PATH" "$@" >>"$LOG_FILE" 2>&1 &
  pid=$!
  printf '%s\n' "$pid" >"$PID_FILE"
  echo "Started $APP_NAME with PID $pid"
  echo "Log file: $LOG_FILE"
}

stop_app() {
  if ! pid=$(running_pid); then
    echo "$APP_NAME is not running"
    return 0
  fi

  kill "$pid" >/dev/null 2>&1 || true

  attempts=0
  while kill -0 "$pid" >/dev/null 2>&1; do
    attempts=$((attempts + 1))
    if [ "$attempts" -ge 20 ]; then
      kill -9 "$pid" >/dev/null 2>&1 || true
      break
    fi
    sleep 1
  done

  rm -f "$PID_FILE"
  echo "Stopped $APP_NAME"
}

pause_app() {
  if ! pid=$(running_pid); then
    echo "$APP_NAME is not running"
    return 0
  fi

  state=$(process_state || true)
  if [ "${state#T}" != "$state" ]; then
    echo "$APP_NAME is already paused with PID $pid"
    return 0
  fi

  kill -STOP "$pid"
  echo "Paused $APP_NAME with PID $pid"
}

resume_app() {
  if ! pid=$(running_pid); then
    echo "$APP_NAME is not running"
    return 0
  fi

  state=$(process_state || true)
  if [ "${state#T}" = "$state" ]; then
    echo "$APP_NAME is already running with PID $pid"
    return 0
  fi

  kill -CONT "$pid"
  echo "Resumed $APP_NAME with PID $pid"
}

status_app() {
  if pid=$(running_pid); then
    state=$(process_state || true)
    if [ "${state#T}" != "$state" ]; then
      echo "$APP_NAME is paused with PID $pid"
    else
      echo "$APP_NAME is running with PID $pid"
    fi
    echo "Jar: $JAR_PATH"
    echo "Log: $LOG_FILE"
    return 0
  fi

  echo "$APP_NAME is not running"
  echo "Jar: $JAR_PATH"
  echo "Log: $LOG_FILE"
}

logs_app() {
  ensure_runtime_dir
  if [ ! -f "$LOG_FILE" ]; then
    echo "Log file not found: $LOG_FILE" >&2
    exit 1
  fi

  if [ "${1:-}" = "-f" ] || [ "${1:-}" = "--follow" ]; then
    tail -n "${TAIL_LINES:-200}" -f "$LOG_FILE"
    return 0
  fi

  tail -n "${TAIL_LINES:-200}" "$LOG_FILE"
}

case "$ACTION" in
  start)
    start_app "$@"
    ;;
  stop|pause)
    if [ "$ACTION" = "pause" ]; then
      pause_app
    else
      stop_app
    fi
    ;;
  resume)
    resume_app
    ;;
  restart)
    stop_app
    start_app "$@"
    ;;
  status)
    status_app
    ;;
  logs)
    logs_app "$@"
    ;;
  *)
    usage >&2
    exit 1
    ;;
esac
