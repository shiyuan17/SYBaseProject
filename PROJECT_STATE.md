# PROJECT_STATE.md

## Current State

- Last updated: 2026-06-08
- Repository: `SYBaseProject`
- Current phase: backend governance hardening with AI memory layer enabled
- Active focus: native Git hooks, dynamic MR Workflow Packet, Memory Update Packet, AI memory files
- Frontend sibling repo: `../SYBaseProjectWeb`

## Active Work

- Dynamic workflow rules now route tasks into execution packets that include expert agents, tests, simulation, security/DB checks, red-team questions, and memory updates.
- AI memory files are now the durable repo-local state layer for future agents and must be checked before final delivery.
- Recent backend delivery added an embedding-box numbering fallback fix and kept it separated from governance documentation commits.

## Validation Baseline

- Latest backend governance validation known in this thread:
  - PowerShell hook scripts parsed successfully on 2026-06-08 after memory-layer rule updates.
  - `commit-msg.ps1` positive/negative samples passed on 2026-06-08.
  - `pre-commit.ps1` passed with no staged files on 2026-06-08.
  - `./mvnw.cmd --% -B -ntp clean test -Dsurefire.excludedGroups=slow` passed on 2026-06-08, matching `pre-push.ps1` fast verification.

## Cross-Repo Dependencies

- Frontend governance and memory files live in sibling repo `../SYBaseProjectWeb`.
- API, permission, patient, report, and menu changes must update memory files in both repos when they change durable context.

## Handoff Notes

- Start future sessions by reading this file, `DECISIONS.md`, and `KNOWN_BUGS.md`, then inspect `git status`.
- If governance docs and business code are both dirty, split them into separate commits before pushing.
