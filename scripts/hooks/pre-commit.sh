#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

if command -v pwsh >/dev/null 2>&1; then
  pwsh -NoProfile -File "$repo_root/scripts/hooks/pre-commit.ps1"
elif command -v powershell.exe >/dev/null 2>&1; then
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$repo_root/scripts/hooks/pre-commit.ps1"
else
  echo "[hooks] PowerShell is required for the staged file-health pre-commit check." >&2
  exit 1
fi
