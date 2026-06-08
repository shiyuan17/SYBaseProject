#!/usr/bin/env bash
set -euo pipefail

message_file="${1:-}"

if [ -z "$message_file" ] || [ ! -f "$message_file" ]; then
  echo "[hooks] commit-msg requires the Git commit message file path." >&2
  exit 1
fi

first_line="$(head -n 1 "$message_file" | tr -d '\r')"

case "$first_line" in
  Merge\ *|Revert\ *|fixup!\ *|squash!\ *)
    exit 0
    ;;
esac

if [[ "$first_line" =~ ^(feat|fix|refactor|docs|test|build|chore|ci|perf)(\([a-z0-9._/-]+\))?:\ .+ ]]; then
  exit 0
fi

cat >&2 <<'EOF'
[hooks] Invalid commit message.

Expected Conventional Commits format:
  type(scope): subject

Allowed types:
  feat, fix, refactor, docs, test, build, chore, ci, perf

Examples:
  feat(user): add role binding API
  fix(bl-center): handle duplicated specimen receipt
EOF

exit 1
