# KNOWN_BUGS.md

## Purpose

Track known backend bugs and reproducible failures that future agents should not rediscover from scratch.

## Entries

| ID | Status | Reproduction | Impact | Workaround | Verification |
| --- | --- | --- | --- | --- | --- |
| BUG-20260608-001 | Resolved | Run `.\scripts\hooks\pre-push.ps1`; the prior failure path was `RepositoryFileHealthGateTest` during fast verification. | The former local backend `pre-push` blocker is no longer reproducing in the current baseline. | None required. | `./mvnw.cmd --% -B -ntp clean test -Dsurefire.excludedGroups=slow` passed on 2026-06-08. |

## Update Rules

- Record only concrete, reproducible, or user-reported bugs.
- Keep production issues linked to logs, reproduction steps, and regression tests.
- Move fixed bugs to a resolved status instead of deleting them.
