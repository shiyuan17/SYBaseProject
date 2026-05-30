#!/usr/bin/env sh
set -eu

report_path="${1:-bl-center/target/site/jacoco/jacoco.xml}"
output_path="${2:-bl-center/jacoco-baseline.properties}"

if [ ! -f "$report_path" ]; then
  printf '%s\n' "Jacoco report not found: $report_path" >&2
  exit 1
fi

line_counter="$(grep -o 'counter type="LINE" missed="[0-9]*" covered="[0-9]*"' "$report_path" | tail -n 1 || true)"
branch_counter="$(grep -o 'counter type="BRANCH" missed="[0-9]*" covered="[0-9]*"' "$report_path" | tail -n 1 || true)"

if [ -z "$line_counter" ] || [ -z "$branch_counter" ]; then
  printf '%s\n' "Jacoco counters not found in report: $report_path" >&2
  exit 1
fi

extract_ratio() {
  counter="$1"
  missed="$(printf '%s' "$counter" | sed -n 's/.*missed="\([0-9][0-9]*\)".*/\1/p')"
  covered="$(printf '%s' "$counter" | sed -n 's/.*covered="\([0-9][0-9]*\)".*/\1/p')"
  awk -v missed="$missed" -v covered="$covered" 'BEGIN {
    total = missed + covered
    if (total <= 0) {
      printf "0.000000"
    } else {
      printf "%.6f", covered / total
    }
  }'
}

mkdir -p "$(dirname "$output_path")"

line_ratio="$(extract_ratio "$line_counter")"
branch_ratio="$(extract_ratio "$branch_counter")"

{
  printf '%s\n' "# Generated from $report_path"
  printf '%s\n' "jacoco.minimum.line.coverage=$line_ratio"
  printf '%s\n' "jacoco.minimum.branch.coverage=$branch_ratio"
  printf '\n'
} > "$output_path"
