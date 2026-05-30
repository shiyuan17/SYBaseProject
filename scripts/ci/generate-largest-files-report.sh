#!/usr/bin/env sh
set -eu

output_path="${1:-docs/reports/largest-files-report.md}"
output_dir="$(dirname "$output_path")"
tmp_dir="$(mktemp -d)"
report_time="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

cleanup() {
  rm -rf "$tmp_dir"
}
trap cleanup EXIT INT TERM

mkdir -p "$output_dir"

list_repo_files() {
  if command -v git >/dev/null 2>&1 && git rev-parse --show-toplevel >/dev/null 2>&1; then
    git -c core.quotepath=false ls-files --cached --modified --others --exclude-standard \
      | LC_ALL=C sort -u \
      | while IFS= read -r file; do
          [ -f "$file" ] || continue
          case "$file" in
            .git/*|.idea/*|.m2/*|.mvn/*|target/*|*/target/*) continue ;;
          esac
          printf '%s\n' "$file"
        done
  else
    find . \
      \( -path "./.git" -o -path "./.idea" -o -path "./.m2" -o -path "./.mvn" -o -path "./target" -o -path "*/target" \) -prune \
      -o -type f -print \
      | sed 's#^\./##'
  fi
}

java_counts="$tmp_dir/java-counts.tsv"
text_violations="$tmp_dir/text-violations.tsv"
exemptions_tsv="$tmp_dir/exemptions.tsv"
baseline_max_exemptions=""

list_repo_files \
  | LC_ALL=C sort \
  | awk '/\.java$/' \
  | while IFS= read -r file; do
      lines="$(wc -l < "$file" | tr -d ' ')"
      printf '%s\t%s\n' "$lines" "$file"
    done > "$java_counts"

list_repo_files \
  | LC_ALL=C sort \
  | while IFS= read -r file; do
      limit=0
      case "$file" in
        *.java) limit=1000 ;;
        *.md) limit=300 ;;
        *.yml|*.yaml|*.json|*.properties|*.cmd|*.bat) limit=200 ;;
        *.xml) limit=300 ;;
        *.sh|*.ps1) limit=500 ;;
      esac
      [ "$limit" -gt 0 ] || continue
      lines="$(wc -l < "$file" | tr -d ' ')"
      if [ "$lines" -gt "$limit" ]; then
        printf '%s\t%s\t%s\n' "$lines" "$limit" "$file"
      fi
    done > "$text_violations"

awk '
  BEGIN {
    FS = "="
  }
  /^[[:space:]]*#/ || /^[[:space:]]*$/ {
    next
  }
  {
    line = $0
    key = line
    sub(/=.*/, "", key)
    value = line
    sub(/^[^=]*=/, "", value)
    if (match(key, /^exemption\.([0-9]+)\.(glob|waive|reason)$/, captures)) {
      exemption_index = captures[1]
      field = captures[2]
      data[exemption_index, field] = value
      if (exemption_index + 0 > max_index) {
        max_index = exemption_index + 0
      }
    }
  }
  END {
    for (exemption_index = 1; exemption_index <= max_index; exemption_index++) {
      if ((exemption_index SUBSEP "glob") in data) {
        glob = data[exemption_index, "glob"]
        waive = data[exemption_index, "waive"]
        reason = data[exemption_index, "reason"]
        gsub(/\t/, "    ", glob)
        gsub(/\t/, "    ", waive)
        gsub(/\t/, "    ", reason)
        printf "%s\t%s\t%s\t%s\n", exemption_index, glob, waive, reason
      }
    }
  }
' "docs/file-health-exemptions.properties" > "$exemptions_tsv"

if [ -f "docs/file-health-baseline.properties" ]; then
  baseline_max_exemptions="$(awk -F '=' '
    /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
    $1 == "file.health.max-exemptions" {
      print $2
      exit
    }
  ' "docs/file-health-baseline.properties")"
fi

{
  printf '# Largest Files Report\n\n'
  printf 'Generated at `%s`.\n\n' "$report_time"

  printf '## Temporary Exemptions\n\n'
  exemption_count="$(wc -l < "$exemptions_tsv" | tr -d ' ')"
  max_lines_waivers="$(awk -F '\t' 'index($3, "MAX_LINES") > 0 { count++ } END { print count + 0 }' "$exemptions_tsv")"
  max_size_waivers="$(awk -F '\t' 'index($3, "MAX_SIZE") > 0 { count++ } END { print count + 0 }' "$exemptions_tsv")"
  printf -- '- Active exemptions: `%s`\n' "$exemption_count"
  if [ -n "$baseline_max_exemptions" ]; then
    printf -- '- Exemption baseline: `%s`\n' "$baseline_max_exemptions"
  fi
  printf -- '- MAX_LINES waivers: `%s`\n' "$max_lines_waivers"
  printf -- '- MAX_SIZE waivers: `%s`\n\n' "$max_size_waivers"
  if [ "$exemption_count" -gt 0 ]; then
    printf '| # | Waive | Target |\n'
    printf '| ---: | --- | --- |\n'
    while IFS="$(printf '\t')" read -r index glob waive reason; do
      printf '| %s | `%s` | `%s` |\n' "$index" "$waive" "$glob"
    done < "$exemptions_tsv"
    printf '\n'
    printf '### Exemption Rationale\n\n'
    while IFS="$(printf '\t')" read -r index glob waive reason; do
      printf '%s. `%s`: %s\n' "$index" "$glob" "$reason"
    done < "$exemptions_tsv"
  else
    printf 'No temporary exemptions are currently configured.\n'
  fi
  printf '\n'

  printf '## Files Exceeding Current Hard Limits\n\n'
  if [ -s "$text_violations" ]; then
    printf '| Lines | Limit | File |\n'
    printf '| ---: | ---: | --- |\n'
    sort -nr "$text_violations" | while IFS="$(printf '\t')" read -r lines limit file; do
      printf '| %s | %s | `%s` |\n' "$lines" "$limit" "$file"
    done
  else
    printf 'No managed text files currently exceed the configured hard limits.\n'
  fi
  printf '\n'

  printf '## Java Files Over 300 Lines\n\n'
  over_300_count="$(awk -F '\t' '$1 > 300 { count++ } END { print count + 0 }' "$java_counts")"
  printf 'Count: `%s`\n\n' "$over_300_count"
  if [ "$over_300_count" -gt 0 ]; then
    printf '| Lines | File |\n'
    printf '| ---: | --- |\n'
    awk -F '\t' '$1 > 300 { print $0 }' "$java_counts" | sort -nr | head -n 30 | while IFS="$(printf '\t')" read -r lines file; do
      printf '| %s | `%s` |\n' "$lines" "$file"
    done
  fi
  printf '\n'

  printf '## Java Files Over 500 Lines\n\n'
  over_500_count="$(awk -F '\t' '$1 > 500 { count++ } END { print count + 0 }' "$java_counts")"
  printf 'Count: `%s`\n\n' "$over_500_count"
  if [ "$over_500_count" -gt 0 ]; then
    printf '| Lines | File |\n'
    printf '| ---: | --- |\n'
    awk -F '\t' '$1 > 500 { print $0 }' "$java_counts" | sort -nr | while IFS="$(printf '\t')" read -r lines file; do
      printf '| %s | `%s` |\n' "$lines" "$file"
    done
  fi
  printf '\n'

  printf '## Top 20 Java Files\n\n'
  printf '| Lines | File |\n'
  printf '| ---: | --- |\n'
  sort -nr "$java_counts" | head -n 20 | while IFS="$(printf '\t')" read -r lines file; do
    printf '| %s | `%s` |\n' "$lines" "$file"
  done
} > "$output_path"
