#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CONFIG_FILE="${RUN_CENTERS_CONFIG_FILE:-$SCRIPT_DIR/run-centers.conf}"

if [ -f "$CONFIG_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  . "$CONFIG_FILE"
  set +a
fi

RUNTIME_DIR="${RUNTIME_DIR:-$SCRIPT_DIR}"
LOG_DIR="${LOG_DIR:-$SCRIPT_DIR}"
ACTION="${1:-start}"
SERVICE_ARG="${2:-all}"

BL_JAR_NAME="${BL_JAR_NAME:-bl-center.jar}"
AUTH_JAR_NAME="${AUTH_JAR_NAME:-auth-center.jar}"
BL_JAR_PATH="${BL_JAR_PATH:-$SCRIPT_DIR/$BL_JAR_NAME}"
AUTH_JAR_PATH="${AUTH_JAR_PATH:-$SCRIPT_DIR/$AUTH_JAR_NAME}"

if [ "$#" -gt 0 ]; then
  shift
fi

if [ "$#" -gt 0 ]; then
  shift
fi

usage() {
  cat <<'EOF'
Usage:
  ./run-centers.sh start [all|bl|auth] [extra java args...]
  ./run-centers.sh stop [all|bl|auth]
  ./run-centers.sh pause [all|bl|auth]
  ./run-centers.sh resume [all|bl|auth]
  ./run-centers.sh restart [all|bl|auth] [extra java args...]
  ./run-centers.sh status [all|bl|auth]
  ./run-centers.sh log [all|bl|auth] [-f]

Default jar location:
  bl-center.jar and auth-center.jar must be in the same directory as this script.

Default log location:
  bl-center.log and auth-center.log will be written to the same directory as this script.

Default pid location:
  bl-center.pid and auth-center.pid will be written to the same directory as this script.

Optional environment variables:
  RUN_CENTERS_CONFIG_FILE Override the config file path, default: ./run-centers.conf
  JAVA_CMD     Java command, default: java
  JAVA_OPTS    Shared JVM options for both services
  BL_JAVA_OPTS Extra JVM options for bl-center
  AUTH_JAVA_OPTS Extra JVM options for auth-center
  SPRING_PROFILES_ACTIVE Shared Spring profile, default: prod
  BL_CENTER_SPRING_PROFILES_ACTIVE Override Spring profile for bl-center
  AUTH_CENTER_SPRING_PROFILES_ACTIVE Override Spring profile for auth-center
  RUNTIME_DIR  PID file directory
  LOG_DIR      Log file directory
  BL_JAR_PATH  Override bl-center jar path
  AUTH_JAR_PATH Override auth-center jar path
  TAIL_LINES   Tail lines for log command, default: 200

Optional config file:
  Place ./run-centers.conf next to this script using shell-style KEY=VALUE lines.
EOF
}

ensure_dirs() {
  mkdir -p "$RUNTIME_DIR" "$LOG_DIR"
}

require_java() {
  if ! command -v "${JAVA_CMD:-java}" >/dev/null 2>&1; then
    echo "java is not available in PATH." >&2
    exit 1
  fi
}

service_name() {
  case "$1" in
    bl) printf '%s\n' "bl-center" ;;
    auth) printf '%s\n' "auth-center" ;;
    *)
      echo "Unsupported service: $1" >&2
      exit 1
      ;;
  esac
}

jar_path() {
  case "$1" in
    bl) printf '%s\n' "$BL_JAR_PATH" ;;
    auth) printf '%s\n' "$AUTH_JAR_PATH" ;;
    *)
      echo "Unsupported service: $1" >&2
      exit 1
      ;;
  esac
}

pid_file() {
  printf '%s\n' "$RUNTIME_DIR/$(service_name "$1").pid"
}

log_file() {
  printf '%s\n' "$LOG_DIR/$(service_name "$1").log"
}

java_opts() {
  case "$1" in
    bl) printf '%s\n' "${BL_JAVA_OPTS:-}" ;;
    auth) printf '%s\n' "${AUTH_JAVA_OPTS:-}" ;;
    *)
      echo "Unsupported service: $1" >&2
      exit 1
      ;;
  esac
}

service_env_prefix() {
  case "$1" in
    bl) printf '%s\n' "BL_CENTER" ;;
    auth) printf '%s\n' "AUTH_CENTER" ;;
    *)
      echo "Unsupported service: $1" >&2
      exit 1
      ;;
  esac
}

service_profile_var() {
  printf '%s_SPRING_PROFILES_ACTIVE\n' "$(service_env_prefix "$1")"
}

effective_profile() {
  service=$1
  profile_var=$(service_profile_var "$service")
  eval "service_profile=\${$profile_var:-}"

  if [ -n "$service_profile" ]; then
    printf '%s\n' "$service_profile"
    return 0
  fi

  if [ -n "${SPRING_PROFILES_ACTIVE:-}" ]; then
    printf '%s\n' "$SPRING_PROFILES_ACTIVE"
    return 0
  fi

  printf '%s\n' "prod"
}

profile_requires_external_datasource() {
  normalized_profile=$(printf '%s' "$1" | tr '[:upper:]' '[:lower:]' | tr -d ' ')
  case ",$normalized_profile," in
    *",local,"*|*",test,"*)
      return 1
      ;;
    *)
      return 0
      ;;
  esac
}

cli_datasource_value() {
  property_name=$1
  shift

  for arg in "$@"; do
    case "$arg" in
      --"$property_name"=*)
        printf '%s\n' "${arg#--$property_name=}"
        return 0
        ;;
    esac
  done

  return 1
}

require_service_runtime_config() {
  service=$1
  shift

  profile=$(effective_profile "$service")
  if ! profile_requires_external_datasource "$profile"; then
    return 0
  fi

  prefix=$(service_env_prefix "$service")
  for key in URL USERNAME PASSWORD; do
    env_name="${prefix}_DATASOURCE_${key}"
    eval "env_value=\${$env_name:-}"
    if [ -n "$env_value" ]; then
      continue
    fi

    property_name=$(printf 'spring.datasource.%s' "$(printf '%s' "$key" | tr '[:upper:]' '[:lower:]')")
    if cli_datasource_value "$property_name" "$@" >/dev/null 2>&1; then
      continue
    fi

    echo "Missing required datasource setting for $(service_name "$service") under profile '$profile': $env_name" >&2
    echo "Set $env_name or pass --$property_name=... when starting the service." >&2
    exit 1
  done
}

require_jar() {
  service=$1
  path=$(jar_path "$service")
  if [ ! -f "$path" ]; then
    echo "$(service_name "$service") jar not found: $path" >&2
    exit 1
  fi
}

running_pid() {
  file=$(pid_file "$1")
  if [ ! -f "$file" ]; then
    return 1
  fi

  pid=$(cat "$file")
  if [ -z "$pid" ]; then
    rm -f "$file"
    return 1
  fi

  if kill -0 "$pid" >/dev/null 2>&1; then
    printf '%s\n' "$pid"
    return 0
  fi

  rm -f "$file"
  return 1
}

process_state() {
  if ! pid=$(running_pid "$1"); then
    return 1
  fi

  state=$(ps -o stat= -p "$pid" 2>/dev/null | tr -d ' ')
  if [ -z "$state" ]; then
    rm -f "$(pid_file "$1")"
    return 1
  fi

  printf '%s\n' "$state"
}

start_service() {
  service=$1
  shift

  require_jar "$service"
  require_service_runtime_config "$service" "$@"

  if pid=$(running_pid "$service"); then
    echo "$(service_name "$service") is already running with PID $pid"
    return 0
  fi

  log=$(log_file "$service")
  pid_path=$(pid_file "$service")
  jar=$(jar_path "$service")
  profile=$(effective_profile "$service")
  touch "$log"

  # shellcheck disable=SC2086
  nohup "${JAVA_CMD:-java}" ${JAVA_OPTS:-} $(java_opts "$service") -jar "$jar" --spring.profiles.active="$profile" "$@" >>"$log" 2>&1 &
  pid=$!
  printf '%s\n' "$pid" >"$pid_path"
  echo "Started $(service_name "$service") with PID $pid"
  echo "Profile: $profile"
  echo "Config: $CONFIG_FILE"
  echo "Jar: $jar"
  echo "Log: $log"
}

stop_service() {
  service=$1
  if ! pid=$(running_pid "$service"); then
    echo "$(service_name "$service") is not running"
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

  rm -f "$(pid_file "$service")"
  echo "Stopped $(service_name "$service")"
}

pause_service() {
  service=$1
  if ! pid=$(running_pid "$service"); then
    echo "$(service_name "$service") is not running"
    return 0
  fi

  state=$(process_state "$service" || true)
  if [ "${state#T}" != "$state" ]; then
    echo "$(service_name "$service") is already paused with PID $pid"
    return 0
  fi

  kill -STOP "$pid"
  echo "Paused $(service_name "$service") with PID $pid"
}

resume_service() {
  service=$1
  if ! pid=$(running_pid "$service"); then
    echo "$(service_name "$service") is not running"
    return 0
  fi

  state=$(process_state "$service" || true)
  if [ "${state#T}" = "$state" ]; then
    echo "$(service_name "$service") is already running with PID $pid"
    return 0
  fi

  kill -CONT "$pid"
  echo "Resumed $(service_name "$service") with PID $pid"
}

status_service() {
  service=$1
  jar=$(jar_path "$service")
  log=$(log_file "$service")
  profile=$(effective_profile "$service")

  if pid=$(running_pid "$service"); then
    state=$(process_state "$service" || true)
    if [ "${state#T}" != "$state" ]; then
      echo "$(service_name "$service") is paused with PID $pid"
    else
      echo "$(service_name "$service") is running with PID $pid"
    fi
  else
    echo "$(service_name "$service") is not running"
  fi

  echo "Profile: $profile"
  echo "Config: $CONFIG_FILE"
  echo "Jar: $jar"
  echo "Log: $log"
}

log_service() {
  service=$1
  shift
  log=$(log_file "$service")

  if [ ! -f "$log" ]; then
    echo "Log file not found: $log" >&2
    exit 1
  fi

  if [ "${1:-}" = "-f" ] || [ "${1:-}" = "--follow" ]; then
    tail -n "${TAIL_LINES:-200}" -f "$log"
    return 0
  fi

  tail -n "${TAIL_LINES:-200}" "$log"
}

for_each_service() {
  target=$1
  shift

  case "$target" in
    all)
      "$@" bl
      "$@" auth
      ;;
    bl|auth)
      "$@" "$target"
      ;;
    *)
      echo "Unsupported service selector: $target" >&2
      usage >&2
      exit 1
      ;;
  esac
}

start_action() {
  target=$1
  shift
  for_each_service "$target" start_service "$@"
}

stop_action() {
  for_each_service "$1" stop_service
}

pause_action() {
  for_each_service "$1" pause_service
}

resume_action() {
  for_each_service "$1" resume_service
}

restart_action() {
  target=$1
  shift
  for_each_service "$target" stop_service
  for_each_service "$target" start_service "$@"
}

status_action() {
  for_each_service "$1" status_service
}

log_action() {
  target=$1
  shift
  if [ "$target" = "all" ]; then
    echo "log command only supports one service at a time: bl or auth" >&2
    exit 1
  fi
  log_service "$target" "$@"
}

ensure_dirs

case "$ACTION" in
  start)
    require_java
    start_action "$SERVICE_ARG" "$@"
    ;;
  stop)
    stop_action "$SERVICE_ARG"
    ;;
  pause)
    pause_action "$SERVICE_ARG"
    ;;
  resume)
    resume_action "$SERVICE_ARG"
    ;;
  restart)
    require_java
    restart_action "$SERVICE_ARG" "$@"
    ;;
  status)
    status_action "$SERVICE_ARG"
    ;;
  log|logs)
    log_action "$SERVICE_ARG" "$@"
    ;;
  *)
    usage >&2
    exit 1
    ;;
esac
