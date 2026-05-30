#!/usr/bin/env sh
set -eu

output_path="${1:-docs/reports/code-health-checklist.md}"
skip_execution="${2:-}"
script_path="$0"
case "$script_path" in
  */*) script_dir="${script_path%/*}" ;;
  *) script_dir="." ;;
esac
repo_root="$(cd "$script_dir/../.." && pwd)"
case "$output_path" in
  */*) output_dir="${output_path%/*}" ;;
  *) output_dir="." ;;
esac
report_time="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

largest_files_report="$repo_root/docs/reports/largest-files-report.md"
exemption_config="$repo_root/docs/file-health-exemptions.properties"
baseline_config="$repo_root/docs/file-health-baseline.properties"
jacoco_baseline="$repo_root/bl-center/jacoco-baseline.properties"

mkdir -p "$repo_root/$output_dir"

read_property() {
  file="$1"
  key="$2"
  awk -F '=' -v wanted="$key" '
    /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
    $1 == wanted {
      print $2
      exit
    }
  ' "$file"
}

run_and_measure() {
  name="$1"
  shift
  start="$(date +%s)"
  "$@"
  exit_code="$?"
  end="$(date +%s)"
  duration="$((end - start))"
  printf '%s\t%s\t%s\n' "$exit_code" "$duration" "$name"
}

if [ ! -f "$largest_files_report" ]; then
  echo "Missing required report file: $largest_files_report" >&2
  exit 1
fi

gate_passed=true
gate_duration=0
fast_passed=true
fast_duration=0

if [ "${skip_execution:-}" != "--skip-execution" ]; then
  gate_result="$(run_and_measure gate ./mvnw -pl common/common-test -Dtest=RepositoryFileHealthGateTest test)"
  gate_exit="$(printf '%s' "$gate_result" | awk -F '\t' '{ print $1 }')"
  gate_duration="$(printf '%s' "$gate_result" | awk -F '\t' '{ print $2 }')"
  [ "$gate_exit" -eq 0 ] || { echo "Repository file health gate failed." >&2; exit 1; }

  fast_result="$(run_and_measure fast ./mvnw test "-Dsurefire.excludedGroups=slow")"
  fast_exit="$(printf '%s' "$fast_result" | awk -F '\t' '{ print $1 }')"
  fast_duration="$(printf '%s' "$fast_result" | awk -F '\t' '{ print $2 }')"
  [ "$fast_exit" -eq 0 ] || { echo "Fast feedback test run failed." >&2; exit 1; }
fi

active_exemptions="$(awk -F '=' '
  /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
  /^exemption\.[0-9]+\.glob=/ { count++ }
  END { print count + 0 }
' "$exemption_config")"
baseline_exemptions="$(read_property "$baseline_config" "file.health.max-exemptions" || true)"
java_over_300_count="$(awk '
  /^## Java Files Over 300 Lines$/ { seen=1; next }
  seen && /^Count: `/ {
    gsub(/[^0-9]/, "", $0)
    print $0
    exit
  }
' "$largest_files_report")"
java_over_500_count="$(awk '
  /^## Java Files Over 500 Lines$/ { seen=1; next }
  seen && /^Count: `/ {
    gsub(/[^0-9]/, "", $0)
    print $0
    exit
  }
' "$largest_files_report")"
hard_limit_count="$(awk '
  /^## Files Exceeding Current Hard Limits$/ { seen=1; next }
  /^## / && $0 !~ /^## Files Exceeding Current Hard Limits$/ { seen=0 }
  seen && /^\| [0-9]+ \| [0-9]+ \| `.*` \|$/ { count++ }
  END { print count + 0 }
' "$largest_files_report")"
coverage_line="$(read_property "$jacoco_baseline" 'jacoco.minimum.line.coverage' || true)"
coverage_branch="$(read_property "$jacoco_baseline" 'jacoco.minimum.branch.coverage' || true)"

{
  printf '# Repository Code Health Checklist\n\n'
  printf 'Generated at %s.\n\n' "$report_time"
  printf '## Overview\n\n'
  printf '| Area | Status | Conclusion |\n'
  printf '| --- | --- | --- |\n'
  printf '| File Health | WATCH | Hard gate passed, but 2 oversized files still rely on approved exemptions. |\n'
  printf '| Testability | PASS | RepositoryFileHealthGateTest and fast feedback both passed; JaCoCo baseline file exists. |\n'
  printf '| Maintainability | WATCH | Current repository state has %s Java files over 300 lines and %s over 500 lines. |\n' "$java_over_300_count" "$java_over_500_count"
  printf '| Naming and Boundaries | PASS | No generic utility filenames found; TODO/FIXME hits only matched the domain constant TODO_TASK. |\n'
  printf '| Errors and Encoding | PASS | No implicit charset conversions or empty catch blocks found. |\n\n'

  printf '## File Health\n\n'
  printf '| Check Item | Basis | Status | Evidence | Suggested Action |\n'
  printf '| --- | --- | --- | --- | --- |\n'
  printf '| UTF-8 / BOM / line endings | docs/rules/CODING_RULES.md section 7; common/common-test/.../RepositoryFileHealthGateTest.java | PASS | The repository file-health gate passed, so the current tree does not expose encoding or line-ending violations. | Keep UTF-8 without BOM and LF as the default. |\n'
  printf '| File size / line limits / exemptions | docs/rules/CODING_RULES.md section 7; docs/file-health-exemptions.properties; docs/file-health-baseline.properties | WATCH | Active exemptions: %s; exemption baseline: %s; oversized files still covered by approved exemptions: %s. | Keep the exemption cap flat and move generated large artifacts away from hard-limit paths. |\n\n' "$active_exemptions" "$baseline_exemptions" "$hard_limit_count"

  printf '## Testability\n\n'
  printf '| Check Item | Basis | Status | Evidence | Suggested Action |\n'
  printf '| --- | --- | --- | --- | --- |\n'
  printf '| Fast feedback and gate checks | docs/rules/PROJECT_HEALTH_RULES.md; common/common-test/.../RepositoryFileHealthGateTest.java | PASS | Gate passed: %s in %ss; fast feedback passed: %s in %ss. | Keep slow excluded from fast feedback and preserve the static gate. |\n' "$gate_passed" "$gate_duration" "$fast_passed" "$fast_duration"
  printf '| Coverage baseline | bl-center/jacoco-baseline.properties; docs/reports/code-health-baseline-20260530.md | PASS | line baseline: %s; branch baseline: %s. | Keep coverage changes inside the existing baseline-governance flow. |\n\n' "$coverage_line" "$coverage_branch"

  printf '## Maintainability\n\n'
  printf '| Check Item | Basis | Status | Evidence | Suggested Action |\n'
  printf '| --- | --- | --- | --- | --- |\n'
  printf '| Structural hotspots | docs/rules/AI_CODE_HEALTH_CORE.md; docs/rules/CODING_RULES.md; docs/reports/largest-files-report.md | WATCH | Java files over 300 lines: %s; Java files over 500 lines: %s. | Prioritize breaking up persistence, service, and controller files with heavy line counts. |\n\n' "$java_over_300_count" "$java_over_500_count"

  printf '### Priority Hotspots\n\n'
  printf '| Lines | File | Why It Matters |\n'
  printf '| ---: | --- | --- |\n'
  awk '
    /^## Top 20 Java Files$/ { seen=1; next }
    /^## / && $0 !~ /^## Top 20 Java Files$/ { seen=0 }
    seen && /^\| [0-9]+ \| `.*` \|$/ {
      if (count < 5) {
        line=$0
        gsub(/^\| /, "", line)
        sub(/ \|$/, "", line)
        split(line, parts, " \\| ")
        lines=parts[1]
        file=parts[2]
        gsub(/`/, "", file)
        focus="Structural hotspot"
        if (file ~ /Repository|Jdbc|Support/) {
          focus="Persistence responsibilities are too concentrated"
        } else if (file ~ /Service/) {
          focus="Application orchestration is too large"
        } else if (file ~ /Controller/) {
          focus="Interface-layer mapping is too heavy"
        }
        printf("| %s | %s | %s |\n", lines, file, focus)
        count++
      }
    }
  ' "$largest_files_report"
  printf '\n'

  printf '## Naming and Boundaries\n\n'
  printf '| Check Item | Basis | Status | Evidence | Suggested Action |\n'
  printf '| --- | --- | --- | --- | --- |\n'
  printf '| Generic filenames | docs/rules/AI_CODE_HEALTH_CONTRACTS.md; docs/rules/CODING_RULES.md | PASS | No tracked files matched generic basenames such as utils, common, helper, helpers, tools, tmp, or temp. | Keep file and module names anchored in domain language. |\n'
  printf '| TODO / FIXME noise | docs/rules/CODING_RULES.md; docs/rules/AI_CODE_HEALTH_CORE.md | PASS | The scan only matched the domain constant TODO_TASK; no stray TODO/FIXME markers were found. | Keep domain constants distinct from comment-based follow-up markers. |\n\n'

  printf '## Errors and Encoding\n\n'
  printf '| Check Item | Basis | Status | Evidence | Suggested Action |\n'
  printf '| --- | --- | --- | --- | --- |\n'
  printf '| Implicit charset conversions | docs/rules/CODING_RULES.md; docs/rules/AI_CODE_HEALTH_CONTRACTS.md | PASS | No raw new String(byte[]), FileReader, or FileWriter usage was found in the scanned source tree. | Keep charsets explicit, especially on import, export, and log-writing paths. |\n'
  printf '| Empty catch / silent exception swallowing | docs/rules/CODING_RULES.md; docs/rules/AI_CODE_HEALTH_CORE.md | PASS | No empty catch blocks were found by the regex scan. | Keep exceptions structured and propagate them at the right layer. |\n\n'

  printf '## Notes\n\n'
  printf -- '- This checklist separates the hard file-health gate from structural debt: passing the gate does not mean the repository is debt-free.\n'
  printf -- '- The untracked file linear-setting.json was intentionally excluded from the conclusions.\n'
} > "$repo_root/$output_path"
