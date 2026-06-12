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
  [ -f "$file" ] || return 0
  awk -F '=' -v wanted="$key" '
    /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
    $1 == wanted {
      print $2
      exit
    }
  ' "$file"
}

markdown_cell() {
  printf '%s' "$1" | tr '\r\n' '  ' | sed 's/|/\//g'
}

invoke_and_measure() {
  name="$1"
  shift
  log_file="$(mktemp)"
  start="$(date +%s)"
  if "$@" >"$log_file" 2>&1; then
    exit_code=0
  else
    exit_code=$?
  fi
  end="$(date +%s)"
  duration="$((end - start))"
  cat "$log_file"
  summary="$(tail -n 12 "$log_file" | tr '\r' '\n' | sed '/^[[:space:]]*$/d')"
  rm -f "$log_file"
  printf '\n__CODE_HEALTH_RESULT__\t%s\t%s\t%s\t%s\n' "$name" "$exit_code" "$duration" "$(printf '%s' "$summary" | tr '\n' '\001')"
}

run_command_result() {
  name="$1"
  shift
  result="$(invoke_and_measure "$name" "$@")"
  meta_line="$(printf '%s\n' "$result" | awk -F '\t' '/^__CODE_HEALTH_RESULT__/ { print $0 }' | tail -n 1)"
  body="$(printf '%s\n' "$result" | sed '/^__CODE_HEALTH_RESULT__\t/d')"
  if [ -n "$body" ]; then
    printf '%s\n' "$body"
  fi
  exit_code="$(printf '%s' "$meta_line" | awk -F '\t' '{ print $3 + 0 }')"
  duration="$(printf '%s' "$meta_line" | awk -F '\t' '{ print $4 + 0 }')"
  summary="$(printf '%s' "$meta_line" | awk -F '\t' '{ print $5 }' | tr '\001' '\n')"
  if [ "$exit_code" -eq 0 ]; then
    status="PASS"
  else
    status="FAIL"
  fi
  printf '%s\t%s\t%s\t%s\n' "$status" "$exit_code" "$duration" "$summary"
}

if [ "${skip_execution:-}" = "--skip-execution" ]; then
  gate_status="${CODE_HEALTH_GATE_STATUS:-UNKNOWN}"
  gate_exit="${CODE_HEALTH_GATE_EXIT:-0}"
  gate_duration="${CODE_HEALTH_GATE_DURATION:-0}"
  gate_summary="${CODE_HEALTH_GATE_SUMMARY:-}"
  fast_status="${CODE_HEALTH_FAST_STATUS:-UNKNOWN}"
  fast_exit="${CODE_HEALTH_FAST_EXIT:-0}"
  fast_duration="${CODE_HEALTH_FAST_DURATION:-0}"
  fast_summary="${CODE_HEALTH_FAST_SUMMARY:-}"
else
  gate_fields="$(run_command_result gate ./mvnw -pl common/common-test -Dtest=RepositoryFileHealthGateTest test)"
  gate_status="$(printf '%s' "$gate_fields" | awk -F '\t' '{ print $1 }')"
  gate_exit="$(printf '%s' "$gate_fields" | awk -F '\t' '{ print $2 }')"
  gate_duration="$(printf '%s' "$gate_fields" | awk -F '\t' '{ print $3 }')"
  gate_summary="$(printf '%s' "$gate_fields" | awk -F '\t' '{ print $4 }')"

  fast_fields="$(run_command_result fast ./mvnw test "-Dsurefire.excludedGroups=slow")"
  fast_status="$(printf '%s' "$fast_fields" | awk -F '\t' '{ print $1 }')"
  fast_exit="$(printf '%s' "$fast_fields" | awk -F '\t' '{ print $2 }')"
  fast_duration="$(printf '%s' "$fast_fields" | awk -F '\t' '{ print $3 }')"
  fast_summary="$(printf '%s' "$fast_fields" | awk -F '\t' '{ print $4 }')"
fi

active_exemptions=0
baseline_exemptions=""
if [ -f "$exemption_config" ]; then
  active_exemptions="$(awk -F '=' '
    /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
    /^exemption\.[0-9]+\.glob=/ { count++ }
    END { print count + 0 }
  ' "$exemption_config")"
fi
baseline_exemptions="$(read_property "$baseline_config" "file.health.max-exemptions" || true)"

largest_available=false
java_over_300_count=""
java_over_500_count=""
hard_limit_count=0
top_hotspots="$(mktemp)"
if [ -f "$largest_files_report" ]; then
  largest_available=true
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
  awk '
    /^## Top 20 Java Files$/ { seen=1; next }
    /^## / && $0 !~ /^## Top 20 Java Files$/ { seen=0 }
    seen && /^\| [0-9]+ \| `.*` \|$/ {
      if (count < 5) {
        print
        count++
      }
    }
  ' "$largest_files_report" > "$top_hotspots"
fi

coverage_line="$(read_property "$jacoco_baseline" 'jacoco.minimum.line.coverage' || true)"
coverage_branch="$(read_property "$jacoco_baseline" 'jacoco.minimum.branch.coverage' || true)"

generic_hits=""
if command -v git >/dev/null 2>&1 && git rev-parse --show-toplevel >/dev/null 2>&1; then
  generic_hits="$(git -c core.quotepath=false ls-files --cached --modified --others --exclude-standard \
    | awk '
      {
        n=split($0, parts, "/")
        name=parts[n]
        sub(/\.[^.]+$/, "", name)
        lower=tolower(name)
        if (lower ~ /^(utils|common|helper|helpers|tools|tmp|temp)$/) {
          print $0
        }
      }
    ' | head -n 5)"
fi

if command -v rg >/dev/null 2>&1; then
  todo_hits="$(rg -n --glob '*.java' 'TODO|FIXME' common bl-center user-center auth-center tools 2>/dev/null | grep -v 'TODO_TASK' | head -n 5 || true)"
  encoding_hits="$(rg -n --glob '*.java' 'new String\([^,)]*\)|FileReader\(|FileWriter\(' common bl-center user-center auth-center tools 2>/dev/null | grep -vE 'StandardCharsets|Charset' | head -n 5 || true)"
  empty_catch_hits="$(rg -n -U --glob '*.java' 'catch\s*\([^)]*\)\s*\{\s*\}' common bl-center user-center auth-center tools 2>/dev/null | head -n 5 || true)"
else
  todo_hits=""
  encoding_hits=""
  empty_catch_hits=""
fi

if [ "$gate_status" = "FAIL" ]; then
  file_health_status="FAIL"
elif [ "$largest_available" = "true" ] && [ "$hard_limit_count" -eq 0 ]; then
  file_health_status="PASS"
else
  file_health_status="WATCH"
fi

if [ "$gate_status" = "FAIL" ] || [ "$fast_status" = "FAIL" ]; then
  testability_status="FAIL"
elif [ -z "$coverage_line" ] || [ -z "$coverage_branch" ] || [ "$gate_status" = "UNKNOWN" ] || [ "$fast_status" = "UNKNOWN" ]; then
  testability_status="WATCH"
else
  testability_status="PASS"
fi

if [ -n "$generic_hits" ] || [ -n "$todo_hits" ]; then
  naming_status="WATCH"
else
  naming_status="PASS"
fi

if [ -n "$encoding_hits" ] || [ -n "$empty_catch_hits" ]; then
  errors_status="WATCH"
else
  errors_status="PASS"
fi

if [ "$largest_available" = "true" ] && [ "${java_over_300_count:-0}" -eq 0 ] 2>/dev/null; then
  maintainability_status="PASS"
else
  maintainability_status="WATCH"
fi

if [ "$largest_available" = "true" ]; then
  maintainability_summary="Current repository state has ${java_over_300_count:-0} Java files over 300 lines and ${java_over_500_count:-0} over 500 lines."
else
  maintainability_summary="largest-files-report.md is unavailable, so hotspot counts could not be refreshed."
fi

{
  printf '# Repository Code Health Checklist\n\n'
  printf 'Generated at `%s`.\n\n' "$report_time"
  printf '## Overview\n\n'
  printf '| Area | Status | Conclusion |\n'
  printf '| --- | --- | --- |\n'
  if [ "$gate_status" = "FAIL" ]; then
    file_health_overview='RepositoryFileHealthGateTest failed; see the Testability section for command output.'
  elif [ "$largest_available" != "true" ]; then
    file_health_overview='largest-files-report.md is unavailable, so hard-limit conclusions are provisional.'
  elif [ "$hard_limit_count" -gt 0 ]; then
    file_health_overview='Hard gate status is separated from structural debt; oversized files still rely on approved exemptions.'
  else
    file_health_overview='The file-health gate passed and no managed text files currently exceed the configured hard limits.'
  fi
  printf '| File Health | %s | %s |\n' "$file_health_status" "$file_health_overview"

  if [ "$gate_status" = "FAIL" ] || [ "$fast_status" = "FAIL" ]; then
    testability_overview='At least one verification command failed; the report was still generated with recorded evidence.'
  elif [ -z "$coverage_line" ] || [ -z "$coverage_branch" ]; then
    testability_overview='Verification commands completed, but the JaCoCo baseline file is missing or incomplete.'
  elif [ "$gate_status" = "UNKNOWN" ] || [ "$fast_status" = "UNKNOWN" ]; then
    testability_overview='Verification commands were skipped; the report relies on existing repository artifacts.'
  else
    testability_overview='RepositoryFileHealthGateTest and fast feedback both passed; JaCoCo baseline file exists.'
  fi
  printf '| Testability | %s | %s |\n' "$testability_status" "$testability_overview"
  printf '| Maintainability | %s | %s |\n' "$maintainability_status" "$maintainability_summary"
  if [ -n "$generic_hits" ]; then
    naming_overview="Tracked files still include generic names: $(markdown_cell "$generic_hits")."
  elif [ -n "$todo_hits" ]; then
    naming_overview='TODO/FIXME markers were found outside the approved TODO_TASK domain constant.'
  else
    naming_overview='No generic utility filenames found; TODO/FIXME hits only matched the domain constant TODO_TASK.'
  fi
  printf '| Naming and Boundaries | %s | %s |\n' "$naming_status" "$naming_overview"
  if [ -n "$encoding_hits" ] || [ -n "$empty_catch_hits" ]; then
    errors_overview='Potential implicit charset conversions or empty catch blocks were found in the current source tree.'
  else
    errors_overview='No implicit charset conversions or empty catch blocks found.'
  fi
  printf '| Errors and Encoding | %s | %s |\n\n' "$errors_status" "$errors_overview"

  printf '## File Health\n\n'
  printf '| Check Item | Basis | Status | Evidence | Suggested Action |\n'
  printf '| --- | --- | --- | --- | --- |\n'
  if [ "$gate_status" = "PASS" ]; then
    gate_evidence='The repository file-health gate passed, so the current tree does not expose encoding or line-ending violations.'
  elif [ "$gate_status" = "FAIL" ]; then
    gate_evidence="The repository file-health gate failed. Exit code: $gate_exit. Summary: $(markdown_cell "$gate_summary")"
  else
    gate_evidence='The repository file-health gate was not executed in this run.'
  fi
  printf '| UTF-8 / BOM / line endings | docs/rules/CODING_RULES.md section 7; common/common-test/.../RepositoryFileHealthGateTest.java | %s | %s | Keep UTF-8 without BOM and LF as the default. |\n' "$file_health_status" "$gate_evidence"
  if [ "$largest_available" = "true" ]; then
    hard_limit_evidence="Active exemptions: $active_exemptions; exemption baseline: ${baseline_exemptions:-}; oversized files still covered by approved exemptions: $hard_limit_count."
  else
    hard_limit_evidence='largest-files-report.md is unavailable, so exemption and hard-limit counts could not be refreshed.'
  fi
  if [ "$largest_available" = "true" ] && [ "$hard_limit_count" -eq 0 ]; then
    hard_limit_status='PASS'
  else
    hard_limit_status='WATCH'
  fi
  printf '| File size / line limits / exemptions | docs/rules/CODING_RULES.md section 7; docs/file-health-exemptions.properties; docs/file-health-baseline.properties | %s | %s | Keep the exemption cap flat and move generated large artifacts away from hard-limit paths. |\n\n' "$hard_limit_status" "$hard_limit_evidence"

  printf '## Testability\n\n'
  printf '| Check Item | Basis | Status | Evidence | Suggested Action |\n'
  printf '| --- | --- | --- | --- | --- |\n'
  gate_summary_cell="$(markdown_cell "$gate_summary")"
  fast_summary_cell="$(markdown_cell "$fast_summary")"
  printf '| Fast feedback and gate checks | docs/rules/PROJECT_HEALTH_RULES.md; common/common-test/.../RepositoryFileHealthGateTest.java | %s | Gate status: %s in %ss (exit %s); fast feedback status: %s in %ss (exit %s).%s%s | Keep slow excluded from fast feedback and preserve the static gate. |\n' \
    "$testability_status" "$gate_status" "$gate_duration" "$gate_exit" "$fast_status" "$fast_duration" "$fast_exit" \
    "$( [ "$gate_status" = "FAIL" ] && printf ' Gate summary: %s.' "$gate_summary_cell" || printf '' )" \
    "$( [ "$fast_status" = "FAIL" ] && printf ' Fast summary: %s.' "$fast_summary_cell" || printf '' )"
  if [ -n "$coverage_line" ] && [ -n "$coverage_branch" ]; then
    coverage_evidence="line baseline: $coverage_line; branch baseline: $coverage_branch."
    coverage_status='PASS'
  else
    coverage_evidence='The JaCoCo baseline file is missing or incomplete.'
    coverage_status='WATCH'
  fi
  printf '| Coverage baseline | bl-center/jacoco-baseline.properties; docs/reports/code-health-baseline-20260530.md | %s | %s | Keep coverage changes inside the existing baseline-governance flow. |\n\n' "$coverage_status" "$coverage_evidence"

  printf '## Maintainability\n\n'
  printf '| Check Item | Basis | Status | Evidence | Suggested Action |\n'
  printf '| --- | --- | --- | --- | --- |\n'
  printf '| Structural hotspots | docs/rules/AI-CODE-HEALTH.md; docs/rules/CODING_RULES.md; docs/reports/largest-files-report.md | %s | %s | Prioritize breaking up persistence, service, and controller files with heavy line counts. |\n\n' "$maintainability_status" "$maintainability_summary"

  printf '### Priority Hotspots\n\n'
  printf '| Lines | File | Why It Matters |\n'
  printf '| ---: | --- | --- |\n'
  if [ "$largest_available" = "true" ]; then
    awk '
      {
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
      }
    ' "$top_hotspots"
  else
    printf '| - | - | largest-files-report.md is unavailable or did not return hotspot rows. |\n'
  fi
  printf '\n'

  printf '## Naming and Boundaries\n\n'
  printf '| Check Item | Basis | Status | Evidence | Suggested Action |\n'
  printf '| --- | --- | --- | --- | --- |\n'
  if [ -n "$generic_hits" ]; then
    generic_evidence="Matched generic basenames: $(markdown_cell "$generic_hits")."
  else
    generic_evidence='No tracked files matched generic basenames such as utils, common, helper, helpers, tools, tmp, or temp.'
  fi
  printf '| Generic filenames | docs/rules/AI-CODE-HEALTH.md; docs/rules/CODING_RULES.md | %s | %s | Keep file and module names anchored in domain language. |\n' "$naming_status" "$generic_evidence"
  if [ -n "$todo_hits" ]; then
    todo_evidence="TODO/FIXME hits: $(markdown_cell "$todo_hits")."
    todo_status='WATCH'
  else
    todo_evidence='The scan only matched the domain constant TODO_TASK; no stray TODO/FIXME markers were found.'
    todo_status='PASS'
  fi
  printf '| TODO / FIXME noise | docs/rules/CODING_RULES.md; docs/rules/AI-CODE-HEALTH.md | %s | %s | Keep domain constants distinct from comment-based follow-up markers. |\n\n' "$todo_status" "$todo_evidence"

  printf '## Errors and Encoding\n\n'
  printf '| Check Item | Basis | Status | Evidence | Suggested Action |\n'
  printf '| --- | --- | --- | --- | --- |\n'
  if [ -n "$encoding_hits" ]; then
    encoding_evidence="Potential hotspots: $(markdown_cell "$encoding_hits")."
    encoding_status='WATCH'
  else
    encoding_evidence='No raw new String(byte[]), FileReader, or FileWriter usage was found in the scanned source tree.'
    encoding_status='PASS'
  fi
  printf '| Implicit charset conversions | docs/rules/CODING_RULES.md; docs/rules/AI-CODE-HEALTH.md | %s | %s | Keep charsets explicit, especially on import, export, and log-writing paths. |\n' "$encoding_status" "$encoding_evidence"
  if [ -n "$empty_catch_hits" ]; then
    empty_catch_evidence="Empty catch hits: $(markdown_cell "$empty_catch_hits")."
    empty_catch_status='WATCH'
  else
    empty_catch_evidence='No empty catch blocks were found by the regex scan.'
    empty_catch_status='PASS'
  fi
  printf '| Empty catch / silent exception swallowing | docs/rules/CODING_RULES.md; docs/rules/AI-CODE-HEALTH.md | %s | %s | Keep exceptions structured and propagate them at the right layer. |\n\n' "$empty_catch_status" "$empty_catch_evidence"

  printf '## Notes\n\n'
  printf -- '- This checklist separates the hard file-health gate from structural debt: passing the gate does not mean the repository is debt-free.\n'
  if [ "${skip_execution:-}" = "--skip-execution" ]; then
    printf -- '- Verification commands were supplied by the caller rather than re-executed inside this script.\n'
  fi
  if command -v git >/dev/null 2>&1 && git rev-parse --show-toplevel >/dev/null 2>&1; then
    untracked="$(git status --short | awk '/^\?\?/ { print substr($0, 4) }' | head -n 5 || true)"
    if [ -n "$untracked" ]; then
      printf -- '- Untracked files were present during report generation: %s.\n' "$(markdown_cell "$untracked")"
    fi
  fi
} > "$repo_root/$output_path"

rm -f "$top_hotspots"
