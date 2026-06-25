#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$ROOT_DIR"

timestamp() {
  date '+%Y%m%d_%H%M%S'
}

sanitize_file_part() {
  printf '%s' "$1" | tr ' /:\\' '____' | tr ',' '-'
}

service_prefix() {
  case "$1" in
    auth-center) printf '%s\n' AUTH_CENTER ;;
    bl-center) printf '%s\n' BL_CENTER ;;
    *) printf '%s\n' "$1" | tr '[:lower:]-' '[:upper:]_' ;;
  esac
}

locate_dexp() {
  if [ -n "${DM_EXPORT_TOOL:-}" ] && [ -f "${DM_EXPORT_TOOL}" ]; then
    printf '%s\n' "${DM_EXPORT_TOOL}"
    return 0
  fi

  for candidate in dexp dexp.exe dexp.cmd; do
    if command -v "$candidate" >/dev/null 2>&1; then
      command -v "$candidate"
      return 0
    fi
  done

  if [ -n "${DM_HOME:-}" ]; then
    for candidate in "${DM_HOME}/bin/dexp" "${DM_HOME}/bin/dexp.exe" "${DM_HOME}/bin/dexp.cmd"; do
      if [ -f "$candidate" ]; then
        printf '%s\n' "$candidate"
        return 0
      fi
    done
  fi

  return 1
}

jdbc_to_connect_target() {
  jdbc_url=$1
  case "$jdbc_url" in
    jdbc:dm://*)
      target=${jdbc_url#jdbc:dm://}
      target=${target%%\?*}
      printf '%s\n' "$target"
      ;;
    *)
      return 1
      ;;
  esac
}

append_summary() {
  current=$1
  next_value=$2
  if [ -n "$current" ]; then
    printf '%s, %s\n' "$current" "$next_value"
  else
    printf '%s\n' "$next_value"
  fi
}

apply_local_defaults() {
  service_label=$1
  echo "Using default local datasource for ${service_label}."
  printf 'jdbc:dm://127.0.0.1:5236\tSYSDBA\tDm.2027.Pwd.\n'
}

OUTPUT_DIR=${1:-${DB_EXPORT_OUTPUT_DIR:-"tmp/db-export/$(timestamp)"}}
mkdir -p "$OUTPUT_DIR"

if ! DEXP_CMD=$(locate_dexp); then
  echo "Unable to locate dexp. Set DM_EXPORT_TOOL or DM_HOME, or add dexp to PATH." >&2
  exit 1
fi

echo "dexp_command=${DEXP_CMD}"

CONFIG_COUNT=0
MANIFEST_FILE="${OUTPUT_DIR}/.db-export-manifest.tmp"
: > "$MANIFEST_FILE"

collect_service() {
  service_label=$1
  jdbc_url=$2
  username=$3
  password=$4

  if [ -z "${jdbc_url}${username}${password}" ]; then
    local_defaults=$(apply_local_defaults "$service_label")
    jdbc_url=$(printf '%s' "$local_defaults" | awk -F '\t' 'END {print $1}')
    username=$(printf '%s' "$local_defaults" | awk -F '\t' 'END {print $2}')
    password=$(printf '%s' "$local_defaults" | awk -F '\t' 'END {print $3}')
  fi

  missing_vars=
  prefix=$(service_prefix "$service_label")
  if [ -z "$jdbc_url" ]; then
    missing_vars="${prefix}_DATASOURCE_URL"
  fi
  if [ -z "$username" ]; then
    missing_vars=$(append_summary "$missing_vars" "${prefix}_DATASOURCE_USERNAME")
  fi
  if [ -z "$password" ]; then
    missing_vars=$(append_summary "$missing_vars" "${prefix}_DATASOURCE_PASSWORD")
  fi
  if [ -n "$missing_vars" ]; then
    echo "${service_label} datasource configuration is incomplete. Missing: ${missing_vars}" >&2
    return 1
  fi

  if [ -s "$MANIFEST_FILE" ]; then
    dedupe_line=$(awk -F '\t' -v url="$jdbc_url" -v user="$username" '$2 == url && $3 == user {print NR; exit}' "$MANIFEST_FILE")
  else
    dedupe_line=
  fi
  if [ -n "$dedupe_line" ]; then
    existing_labels=$(awk -F '\t' -v line="$dedupe_line" 'NR == line {print $1}' "$MANIFEST_FILE")
    updated_labels="${existing_labels}, ${service_label}"
    awk -F '\t' -v OFS='\t' -v line="$dedupe_line" -v labels="$updated_labels" 'NR == line {$1 = labels} {print}' "$MANIFEST_FILE" > "${MANIFEST_FILE}.next"
    mv "${MANIFEST_FILE}.next" "$MANIFEST_FILE"
    echo "Deduplicating ${service_label} with ${existing_labels} because URL and username match."
    return 0
  fi

  CONFIG_COUNT=$((CONFIG_COUNT + 1))
  printf '%s\t%s\t%s\t%s\n' "$service_label" "$jdbc_url" "$username" "$password" >> "$MANIFEST_FILE"
  return 0
}

if ! collect_service "auth-center" "${AUTH_CENTER_DATASOURCE_URL:-}" "${AUTH_CENTER_DATASOURCE_USERNAME:-}" "${AUTH_CENTER_DATASOURCE_PASSWORD:-}"; then
  exit 1
fi
if ! collect_service "bl-center" "${BL_CENTER_DATASOURCE_URL:-}" "${BL_CENTER_DATASOURCE_USERNAME:-}" "${BL_CENTER_DATASOURCE_PASSWORD:-}"; then
  exit 1
fi

if [ "$CONFIG_COUNT" -eq 0 ]; then
  echo "No datasource configuration found. Set AUTH_CENTER_DATASOURCE_* and/or BL_CENTER_DATASOURCE_*." >&2
  exit 1
fi

echo "output_dir=${OUTPUT_DIR}"
echo "unique_export_targets=${CONFIG_COUNT}"

EXPORT_COUNT=0
FAIL_COUNT=0
SUCCESS_SUMMARY=
FAIL_SUMMARY=
RUN_TIMESTAMP=$(timestamp)

while IFS="$(printf '\t')" read -r service_labels jdbc_url username password; do
  [ -n "$service_labels" ] || continue
  if ! connect_target=$(jdbc_to_connect_target "$jdbc_url"); then
    echo "Failed to convert JDBC URL to DM export target: ${jdbc_url}" >&2
    FAIL_COUNT=$((FAIL_COUNT + 1))
    FAIL_SUMMARY=$(append_summary "$FAIL_SUMMARY" "$service_labels")
    continue
  fi

  file_stem="$(sanitize_file_part "$service_labels")-$(sanitize_file_part "$username")-${RUN_TIMESTAMP}"
  dump_file="${file_stem}.dmp"
  log_file="${file_stem}.log"

  echo "Starting export for ${service_labels}"
  echo "  jdbc_url=${jdbc_url}"
  echo "  dump_file=${OUTPUT_DIR}/${dump_file}"
  echo "  log_file=${OUTPUT_DIR}/${log_file}"

  if "$DEXP_CMD" "USERID=${username}/${password}@${connect_target}" "FULL=Y" "DIRECTORY=${OUTPUT_DIR}" "FILE=${dump_file}" "LOG=${log_file}"; then
    echo "Export completed for ${service_labels}"
    EXPORT_COUNT=$((EXPORT_COUNT + 1))
    SUCCESS_SUMMARY=$(append_summary "$SUCCESS_SUMMARY" "${service_labels} -> ${OUTPUT_DIR}/${dump_file}")
  else
    echo "Export failed for ${service_labels}" >&2
    FAIL_COUNT=$((FAIL_COUNT + 1))
    FAIL_SUMMARY=$(append_summary "$FAIL_SUMMARY" "$service_labels")
  fi
done < "$MANIFEST_FILE"

echo "export_count=${EXPORT_COUNT}"
echo "failure_count=${FAIL_COUNT}"
[ -n "$SUCCESS_SUMMARY" ] && echo "successful_exports=${SUCCESS_SUMMARY}"
[ -n "$FAIL_SUMMARY" ] && echo "failed_exports=${FAIL_SUMMARY}"

rm -f "$MANIFEST_FILE"

if [ "$FAIL_COUNT" -ne 0 ]; then
  exit 1
fi
