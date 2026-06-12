# PROJECT_STATE.md

## Current State

- Last updated: 2026-06-12
- Repository: `SYBaseProject` (Maven multi-module, Java 17 / Spring Boot 3)
- Frontend sibling repo: `../SYBaseProjectWeb`
- Current phase: M5/M6 full-stack delivery (reagent inventory, specimen/archive workflows, statistics) plus governance guardrail parity
- AI memory files are the durable repo-local state layer for future agents and must be checked before final delivery.

## Active Work

- M6 dashboard contract corrected on 2026-06-12: sibling frontend `../SYBaseProjectWeb` `/m6/dashboard` no longer calls the nonexistent `POST /api/v1/stat-dashboard/query` (user-reported 404, `traceId=126a212ff088424d96d7d9c8081671b5`). `M6ManagementController` exposes `POST /api/v1/stat-reports/query`; the frontend composes `QUALITY` / `OPERATION` / `WORKLOAD` report queries. See `DEC-20260612-001`.
- Governance guardrail parity added on 2026-06-12: `scripts/ci/validate-governance.sh` rejects duplicate `DEC-*` / `BUG-*` / `TD-*` ledger IDs and enforces `PROJECT_STATE.md` required sections plus a 120-line budget, wired into local hooks and a GitLab CI `verify_governance` job. Mirrors frontend `DEC-20260612-008`. See `DEC-20260612-002`.
- M5 specimen archive (2026-06-11): `POST /api/v1/archive/specimens` (perm `PERM_M5_SPECIMEN_ARCHIVE`) and `GET /api/v1/archive-objects?objectType=SPECIMEN` serve the sibling frontend `/operation-support/archive`. Specimens are stored in `specimen_storage_records` as `object_type='SPECIMEN'` and do not participate in material loans. See `DEC-20260611-004`.
- M5 reagent inventory/template (2026-06-11): migration `V86__extend_reagent_inventory_legacy_fields` splits templates (`reagents`), batch stock (`reagent_stocks`), and the action trail (`reagent_stock_events`). `/api/v1/reagents` and `/api/v1/reagent-stocks` plus `/test`, `/consume`, `/start-use`, `/finish-use`, events, and UTF-8 BOM CSV import/export back `/operation-resources/reagents`. Operators resolve from authenticated request context. See `DEC-20260611-002`.
- M5 archive object pagination (2026-06-11): `GET /api/v1/archive-objects` returns paged application-form / wax-block / slide lists (perm `PERM_M5_ARCHIVE_QUERY`, `objectType=APPLICATION_FORM|EMBEDDING_BOX|SLIDE`). `archive-records/search` stays a storage-record/history query. See `DEC-20260611-003`.
- M5 archive cabinet batch/delete (2026-06-11): `POST /api/v1/archive-cabinets/batch` and `DELETE /api/v1/archive-cabinets/{id}` (perm `PERM_M5_ARCHIVE_CABINET_DELETE`, empty-only). Migration `V85`. See `DEC-20260611-001`.
- System log management (2026-06-10): `GET /api/v1/system/logs/login|operations(/{id})` with sanitized/truncated detail, `ApiAuditInterceptor` + `OperationAuditService`, migration `V84`. See `DEC-20260610-001`.
- Technical workflow (2026-06-08..09): embedding confirmation middle state `EMBEDDING_CONFIRM_PENDING` with `start`/`cancel`/`complete` (`DEC-20260609-003`); slicing print-before-slice flow (`DEC-20260608-004`) and pending slide-print merge groups (`DEC-20260609-005`); specimen workflow `specimenId`-first progression (`DEC-20260609-004`) and nullable barcode binding via `V81` (`DEC-20260609-002`); current-user operator token relaxation (`DEC-20260609-001`); historical-status lookups (`DEC-20260608-005`); application list `pathologyNo` (`DEC-20260608-007`).
- Flyway sync tooling: `scripts/migration/run-bl-center-flyway.*` defaults `BL_CENTER_FLYWAY_OUT_OF_ORDER=true` to backfill repaired gaps such as `V75`. See `DEC-20260608-006`.

## Validation Baseline

- Latest M5 specimen archive (2026-06-11): `.\mvnw.cmd -pl bl-center "-Dtest=ArchiveWorkflowIntegrationTest,ArchiveRoleAuthorizationIntegrationTest" test` passed (7 tests). Frontend targeted Vitest (4 files, 28 tests), `pnpm lint`, `pnpm check:type` passed.
- Latest M5 reagent inventory (2026-06-11): `.\mvnw.cmd -pl bl-center -am test "-Dtest=OperationSupportIntegrationTest,M5SingleApiIntegrationTest,ArchiveRoleAuthorizationIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"` passed (10 tests). Frontend targeted Vitest (13 files, 63 tests), `pnpm check:type` passed; full frontend `pnpm lint` blocked by unrelated dirty docs formatting (frontend `TD-20260611-002`).
- Latest system log management (2026-06-10): `bl-center` log/audit suite (19 tests) and `auth-center` `AuthControllerIntegrationTest` (9 tests) passed. Frontend system-management Vitest (11 files, 43 tests) + `menu.test.ts` (16 tests) passed.
- Latest technical workflow (2026-06-09): `TechnicalWorkflowExecutionIntegrationTest` (9 tests), `SlicingWorkbenchIntegrationTest` (6 tests), specimen workflow suite (47 tests) passed at various points; some reruns blocked by unrelated dirty-worktree signature drift.
- Known recurring blocker: dirty-worktree signature drift in specimen/slicing/system-management tests can block `testCompile` before targeted assertions run; isolate via worktree before full verify.
- Governance check (2026-06-12): `bash scripts/ci/validate-governance.sh` passed locally after trimming this file and de-duplicating `DEC-20260608-004`.

## Cross-Repo Dependencies

- Frontend governance and memory files live in sibling repo `../SYBaseProjectWeb`; governance guardrails are now symmetric (frontend `DEC-20260612-008` ↔ backend `DEC-20260612-002`).
- Frontend M5/M6 consumers live under `../SYBaseProjectWeb/apps/web-ele/src/modules/{operation-support,operation-resources,specimen-workflow,technical-workflow,system-management}` and consume the archive, reagent, system-log, statistics, and workflow APIs above.
- Frontend must keep omitting legacy reagent `operatorName` / `operatorUserId` fields and let backend auth resolve operators; it treats null specimen barcodes as `UNBOUND` until barcode binding succeeds.
- API, permission, patient, report, and menu changes must update memory files in both repos when they change durable context (bi-directional references by memory ID).

## Handoff Notes

- Start future sessions by reading this file, `DECISIONS.md`, and `KNOWN_BUGS.md`, then inspect `git status`.
- If governance docs and business code are both dirty, split them into separate commits before pushing.
- Run `bash scripts/ci/validate-governance.sh` before delivering memory/governance changes; CI `verify_governance` is the hard gate.
