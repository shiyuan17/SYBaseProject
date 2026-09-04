#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
APP_NAME="bl-center"
JAR_NAME="${BL_CENTER_JAR_NAME:-bl-center.jar}"
JAR_PATH="${BL_CENTER_JAR_PATH:-$SCRIPT_DIR/$JAR_NAME}"
RUNTIME_DIR="${BL_CENTER_RUNTIME_DIR:-$SCRIPT_DIR}"
PID_FILE="$RUNTIME_DIR/$APP_NAME.pid"
LOG_FILE="${BL_CENTER_LOG_FILE:-$SCRIPT_DIR/log/$APP_NAME.log}"
PROFILE_OVERRIDE=""

case "${1:-}" in
  --profile)
    if [ "$#" -lt 2 ]; then
      echo "Missing profile name after --profile." >&2
      exit 1
    fi
    PROFILE_OVERRIDE=$2
    shift 2
    ;;
  -?*)
    PROFILE_OVERRIDE=${1#-}
    shift
    ;;
esac

SPRING_PROFILE="${PROFILE_OVERRIDE:-${BL_CENTER_SPRING_PROFILES_ACTIVE:-${SPRING_PROFILES_ACTIVE:-dev}}}"
ACTION="${1:-start}"
JAVA_BIN="${BL_CENTER_JAVA_BIN:-${JAVA_BIN:-}}"

if [ "$#" -gt 0 ]; then
  shift
fi

if [ ! -f "$JAR_PATH" ] && [ -f "$PWD/$JAR_NAME" ]; then
  JAR_PATH="$PWD/$JAR_NAME"
fi

usage() {
  echo "Usage: $0 [-<profile>|--profile <profile>] {start|stop|pause|resume|restart|status|logs} [args]"
}

display_profile() {
  if [ -n "$SPRING_PROFILE" ]; then
    printf '%s\n' "$SPRING_PROFILE"
    return 0
  fi

  printf '%s\n' "default"
}

resolve_java() {
  if [ -n "$JAVA_BIN" ] && [ -x "$JAVA_BIN" ]; then
    return 0
  fi

  for candidate in /opt/jdk-21/bin/java /opt/jdk-17/bin/java "${JAVA_HOME:-}/bin/java"; do
    if [ -x "$candidate" ]; then
      JAVA_BIN="$candidate"
      return 0
    fi
  done

  JAVA_BIN=$(command -v java || true)
}

require_java() {
  resolve_java
  if [ -z "$JAVA_BIN" ] || [ ! -x "$JAVA_BIN" ]; then
    echo "Java 17 or newer is not available." >&2
    exit 1
  fi
}

require_jar() {
  if [ ! -f "$JAR_PATH" ]; then
    echo "BL jar not found: $JAR_PATH" >&2
    echo "Set BL_CENTER_JAR_PATH or place $JAR_NAME in the script directory." >&2
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
  set -- "$@"
  if [ -n "$SPRING_PROFILE" ]; then
    set -- "--spring.profiles.active=$SPRING_PROFILE" "$@"
  fi
  nohup "$JAVA_BIN" ${JAVA_OPTS:-} -jar "$JAR_PATH" "$@" >>"$LOG_FILE" 2>&1 &
  pid=$!
  printf '%s\n' "$pid" >"$PID_FILE"
  echo "Started $APP_NAME with PID $pid"
  echo "Profile: $(display_profile)"
  echo "Java: $JAVA_BIN"
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
    echo "Profile: $(display_profile)"
    echo "Jar: $JAR_PATH"
    echo "Log: $LOG_FILE"
    return 0
  fi

  echo "$APP_NAME is not running"
  echo "Profile: $(display_profile)"
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
