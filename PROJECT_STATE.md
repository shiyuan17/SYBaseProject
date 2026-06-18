# PROJECT_STATE.md

## Current State

- Last updated: 2026-06-18
- Repository: `SYBaseProject` (Maven multi-module, Java 17 / Spring Boot 3)
- Frontend sibling: `../SYBaseProjectWeb`
- Phase: Active delivery across `bl-center` specimen/M3/M5/M6 APIs and master data
- Cross-repo contracts: see `ARCHITECTURE.md` (not duplicated here)

## Active Work

- Specimen workflow: operating-room reference options, specimen dictionary system-config APIs, frozen-reminder-related contracts
- M6 `stat-reports/*` workbench contracts (stable; no `stat-dashboard/query`)
- M5 archive / borrow / reagent / medical-waste / equipment APIs (stable)
- M3 technical-workflow `dateFrom`/`dateTo` query support (stable)
- Governance P2: green-zone default skip Memory writes; slim `PROJECT_STATE` + archive under `docs/reviews/`
- Dirty worktree may include in-flight changes — verify with `git status`

## Validation Baseline

- After governance/memory edits: `bash scripts/ci/validate-governance.sh`
- Feature validation: `./mvnw -pl bl-center -Dtest=<RelevantIntegrationTest> test`
- Full `./mvnw verify` may hit dirty-worktree signature drift — isolate via worktree when needed

## Cross-Repo Dependencies

- Durable API and permission contracts live in `ARCHITECTURE.md`
- Frontend consumers under `../SYBaseProjectWeb/apps/web-ele/src/modules/`
- Symmetric memory layer with sibling `../SYBaseProjectWeb/docs/memory/`

## Handoff Notes

- **Entry read**: this file + `ARCHITECTURE.md`
- **On demand**: `DECISIONS.md`, `KNOWN_BUGS.md`, `TECH_DEBT.md` when task touches contracts, bugs, or debt
- **Session handoff**: prefer agentmemory `handoff` / `recall` / `session-history` or `agent-transcripts/` for task-local context; do not duplicate in `PROJECT_STATE`
- **Never** use this file for dirty-worktree truth — run `git status --short`
- Historical validation and delivery changelog: `docs/reviews/project-state-archive.md`
