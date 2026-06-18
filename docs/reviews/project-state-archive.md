# Project State Archive

Historical `PROJECT_STATE.md` validation logs and delivery changelog archived on 2026-06-18. Active handoff uses the slim root `PROJECT_STATE.md` + `ARCHITECTURE.md`.

## Archived Validation Baseline (through 2026-06-17)

- Latest M5 specimen archive (2026-06-11): `.\mvnw.cmd -pl bl-center "-Dtest=ArchiveWorkflowIntegrationTest,ArchiveRoleAuthorizationIntegrationTest" test` passed (7 tests). Frontend targeted Vitest (4 files, 28 tests), `pnpm lint`, `pnpm check:type` passed.
- Latest M5 reagent inventory (2026-06-11): `.\mvnw.cmd -pl bl-center -am test "-Dtest=OperationSupportIntegrationTest,M5SingleApiIntegrationTest,ArchiveRoleAuthorizationIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"` passed (10 tests). Frontend targeted Vitest (13 files, 63 tests), `pnpm check:type` passed; full frontend `pnpm lint` blocked by unrelated dirty docs formatting (frontend `TD-20260611-002`).
- Latest M5 equipment old-system alignment (2026-06-16): `mvnw.cmd -pl bl-center -Dtest=OperationSupportIntegrationTest,M5SingleApiIntegrationTest test` passed after expanding equipment create/update fixtures and aligning batch-status request validation with business-layer `INVALID_ARGUMENT`. Sibling frontend targeted equipment Vitest and ESLint passed; browser verification there was partially blocked by login slider automation, and project-wide frontend lint/typecheck later exposed unrelated pre-existing issues in other medical-waste/reagent dialogs.
- Latest M5 medical waste management (2026-06-16): `mvnw.cmd -pl bl-center clean -Dtest=MedicalWasteIntegrationTest test` passed, covering specimen options/preview/print/destroy, reagent save/handover, permission reuse, empty-label print rejection, and repeated handover/destroy conflict handling. Sibling frontend targeted Vitest, targeted ESLint, and `pnpm check:type --filter=@vben/web-ele` passed; browser verification could open the route but remained login-slider blocked before authenticated in-page checks.
- Latest M6 statistics report workbench (2026-06-16): `.\mvnw.cmd -pl bl-center -Dtest=M6StatisticsIntegrationTest test` passed, covering report query/export, workload period trends, quality trend/breakdown rows, and quality detail query/export. Sibling frontend targeted M6/menu Vitest, targeted ESLint, `pnpm check:type`, and `pnpm run check:governance` passed; authenticated browser verification remained limited by the login slider captcha.
- Latest M3 visible-page date-range upgrade (2026-06-17): `.\mvnw.cmd -pl bl-center "-Dtest=TechnicalWorkflowQueryEnhancementIntegrationTest,MedicalOrderIntegrationTest,SlicingWorkbenchIntegrationTest" test` passed, covering `dateFrom/dateTo`, legacy `workDate`, and precedence semantics for tracking, embedding summary, slicing workbench, and pending medical orders. Sibling frontend targeted Vitest passed, while frontend `pnpm check:type` remained blocked by unrelated pre-existing errors and authenticated browser verification remained blocked by the login slider captcha at the login page.
- Latest system log management (2026-06-10): `bl-center` log/audit suite (19 tests) and `auth-center` `AuthControllerIntegrationTest` (9 tests) passed. Frontend system-management Vitest (11 files, 43 tests) + `menu.test.ts` (16 tests) passed.
- Latest technical workflow (2026-06-09): `TechnicalWorkflowExecutionIntegrationTest` (9 tests), `SlicingWorkbenchIntegrationTest` (6 tests), specimen workflow suite (47 tests) passed at various points; some reruns blocked by unrelated dirty-worktree signature drift.
- Known recurring blocker: dirty-worktree signature drift in specimen/slicing/system-management tests can block `testCompile` before targeted assertions run; isolate via worktree before full verify.
- Governance check (2026-06-12): `bash scripts/ci/validate-governance.sh` passed locally after trimming this file and de-duplicating `DEC-20260608-004`.

## Archived Active Work Detail (through 2026-06-17)

- M6 dashboard contract corrected on 2026-06-12: sibling frontend `../SYBaseProjectWeb` `/m6/dashboard` no longer calls the nonexistent `POST /api/v1/stat-dashboard/query`. See `DEC-20260612-001`.
- M6 statistics report workbench delivery active on 2026-06-16: sibling frontend `/m6/custom-analysis` uses `stat-reports/query/export` plus `details/query/export`. See `DEC-20260616-004` through `DEC-20260616-007`.
- Governance guardrail parity added on 2026-06-12. See `DEC-20260612-002`, `DEC-20260612-003`.
- M5 specimen archive, reagent inventory/template, archive object pagination, cabinet batch/delete, equipment, medical waste, role naming — see `DEC-20260611-*`, `DEC-20260616-001`, `DEC-20260616-003`, `DEC-20260616-008`.
- System log management (2026-06-10): `DEC-20260610-001`.
- Technical workflow embedding/slicing/specimen contracts (2026-06-08..09).
- M3 visible technical-workflow date-range upgrade (2026-06-17): `DEC-20260617-005`.
- Flyway sync tooling: `DEC-20260608-006`.
