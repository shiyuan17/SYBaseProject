#!/usr/bin/env bash
# Governance baseline validator for the backend repo.
#
# Mirrors the sibling frontend `../SYBaseProjectWeb/scripts/validate-governance.mjs`
# baseline (see DECISIONS.md DEC-20260612-002): it rejects duplicate ledger IDs in
# the memory files and enforces the shared PROJECT_STATE.md structure/length budget.
#
# Usage: scripts/ci/validate-governance.sh
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$repo_root"

errors=()

# --- Duplicate ledger ID detection -----------------------------------------
# Each ledger row starts with `| <ID> |`; collect IDs matching the pattern and
# report any that appear more than once.
check_duplicate_ids() {
  local file="$1"
  local pattern="$2"
  local label="$3"

  [ -f "$file" ] || return 0

  local duplicates
  duplicates="$(
    grep -oE "\| *${pattern} *\|" "$file" \
      | sed -E 's/^\| *//; s/ *\|$//' \
      | sort \
      | uniq -d
  )"

  if [ -n "$duplicates" ]; then
    while IFS= read -r id; do
      [ -n "$id" ] && errors+=("Duplicate ${label} ID: ${id}")
    done <<<"$duplicates"
  fi
}

check_duplicate_ids "DECISIONS.md" "DEC-[0-9]{8}-[0-9]{3}" "decision"
check_duplicate_ids "KNOWN_BUGS.md" "BUG-[0-9]{8}-[0-9]{3}" "bug"
check_duplicate_ids "TECH_DEBT.md" "TD-[0-9]{8}-[0-9]{3}" "tech debt"

# --- Ledger soft line budget -------------------------------------------------
# Mirrors the frontend LEDGER_MAX_LINES guardrail: oversized ledgers should have
# resolved/historical entries archived (e.g. under docs/reports/), not deleted.
ledger_max_lines=200
for ledger in DECISIONS.md KNOWN_BUGS.md TECH_DEBT.md; do
  [ -f "$ledger" ] || continue
  ledger_lines="$(wc -l <"$ledger" | tr -d ' ')"
  if [ "$ledger_lines" -gt "$ledger_max_lines" ]; then
    errors+=("${ledger} is too long: ${ledger_lines} lines (limit ${ledger_max_lines}). Archive resolved/historical entries instead of deleting them.")
  fi
done

# --- PROJECT_STATE.md structure + line budget ------------------------------
project_state="PROJECT_STATE.md"
project_state_max_lines=120
required_sections=(
  "## Current State"
  "## Active Work"
  "## Validation Baseline"
  "## Cross-Repo Dependencies"
  "## Handoff Notes"
)

if [ -f "$project_state" ]; then
  line_count="$(wc -l <"$project_state" | tr -d ' ')"
  if [ "$line_count" -gt "$project_state_max_lines" ]; then
    errors+=("PROJECT_STATE.md is too long: ${line_count} lines (limit ${project_state_max_lines}).")
  fi

  for section in "${required_sections[@]}"; do
    if ! grep -qF "$section" "$project_state"; then
      errors+=("PROJECT_STATE.md is missing required section: ${section}")
    fi
  done
fi

# --- Report ----------------------------------------------------------------
if [ "${#errors[@]}" -gt 0 ]; then
  echo "Governance validation failed:" >&2
  for error in "${errors[@]}"; do
    echo "- ${error}" >&2
  done
  exit 1
fi

echo "Governance validation passed."
