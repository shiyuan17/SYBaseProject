#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
hook_source_dir="$repo_root/scripts/hooks"
git_hooks_dir="$repo_root/.git/hooks"

mkdir -p "$git_hooks_dir"

install_hook() {
  local source_name="$1"
  local target_name="$2"

  cp "$hook_source_dir/$source_name" "$git_hooks_dir/$target_name"
  chmod +x "$git_hooks_dir/$target_name"
  echo "[hooks] Installed $target_name"
}

install_hook pre-commit.sh pre-commit
install_hook commit-msg.sh commit-msg
install_hook pre-push.sh pre-push

echo "[hooks] Backend Git hooks installed."
