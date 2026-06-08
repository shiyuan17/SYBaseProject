# Backend Git Hooks

This directory stores the versioned backend Git hook scripts. Install them into the local `.git/hooks` directory before committing backend changes.

## Install

On Git Bash or Linux/macOS:

```bash
bash scripts/hooks/install.sh
```

On Windows PowerShell:

```powershell
.\scripts\hooks\install.ps1
```

## Hooks

- `pre-commit`: checks staged text files for strict UTF-8, no UTF-8 BOM, and LF line endings except `.cmd` / `.bat`.
- `commit-msg`: validates Conventional Commits with a lightweight native script.
- `pre-push`: runs `./mvnw -B -ntp clean test -Dsurefire.excludedGroups=slow`.

The `.sh` files are used by Git Bash/Linux-style installs. The `.ps1` files are the Windows command implementations used by `install.ps1`.

These hooks are local safeguards. They do not replace the repository-wide `RepositoryFileHealthGateTest`, module-level `./mvnw -pl <module> -am verify`, full `./mvnw clean verify`, GitLab CI, or target-environment acceptance.
