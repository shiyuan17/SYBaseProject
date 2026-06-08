#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

echo "[hooks] Running backend fast verification..."
./mvnw -B -ntp clean test -Dsurefire.excludedGroups=slow
