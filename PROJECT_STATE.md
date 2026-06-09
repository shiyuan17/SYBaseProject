# PROJECT_STATE.md

## Current State

- Last updated: 2026-06-09
- Repository: `SYBaseProject`
- Current phase: backend M2 specimen barcode binding semantics aligned with nullable specimen barcode storage
- Active focus: specimen registration no longer auto-generates barcodes when none are provided; barcode binding is the first-write path for target specimen barcodes.
- Frontend sibling repo: `../SYBaseProjectWeb`

## Active Work

- Barcode binding semantics fixed on 2026-06-09: `SpecimenRegistrationService` now persists `barcode=null` and `labelPrintStatus=PENDING` when registration items omit barcode, avoids printing empty barcode labels, and leaves `POST /api/v1/specimens/{specimenId}/barcode-binding` as the first-write target barcode operation. Flyway Java migration `V81__relax_specimen_barcode_nullable` relaxes `specimens.barcode` nullability while preserving non-null barcode uniqueness. Sibling frontend `../SYBaseProjectWeb` now treats null barcodes as `UNBOUND`, moves the “标本条码” column after “标本编号”, and excludes unbound specimens from barcode-based transport creation.
- Specimen check-in / transport operator fix on 2026-06-09: check-in, transport order create/outbound, and quick outbound requests no longer require `operatorVerificationToken` when the submitted operator user ID matches the current request user; non-current operators still resolve through `OperatorVerificationService`.
- Application patient auto-create age normalization fixed on 2026-06-08: `ApplicationPatientIdentityResolver` now stores leading numeric age when frontend/manual callers send display-style age strings such as `35岁0月0天`, preventing DM numeric conversion failures in `patients.age`; sibling frontend `../SYBaseProjectWeb` sends numeric age on the application-number auto-create path and suppresses lookup 404 global toasts.
- Historical-status query support changed on 2026-06-08: `GET /api/v1/technical-tasks/pending` accepts optional `includeAllStatuses` and `taskId`; default calls still return active `PENDING/IN_PROGRESS` technical tasks unless a specific `taskStatus` or all-status lookup is requested.
- Specimen outbound query changed on 2026-06-08: `GET /api/v1/specimen-outbounds` treats `applicationId` or `specimenNo` as explicit lookup conditions and returns matching rows outside the outbound-ready subset, so frontend can show old statuses while action gating remains status-based.
- Technical specimen registration list responses changed on 2026-06-08: `PendingTechnicalSpecimenRegistrationResponse` and related query/service records now include `patientGender` and `patientAge`, sourced from `bl_application` list queries.
- Technical specimen registration completion changed on 2026-06-08: `TechnicalSpecimenRegistrationCompleteRequest.pathologyNo` is optional; non-empty candidates must match the normalized selected application type and be unique except for the current case, and empty candidates continue to use backend generation.
- Application list responses changed on 2026-06-08: `GET /api/v1/applications` `items[]` now includes `pathologyNo`, sourced from `pathology_cases.pathology_no`, so frontend tracking and abnormal lists can display the pathology number without detail fan-out.
- Slicing workflow changed on 2026-06-08: `POST /api/v1/slicings/slide-print` creates and confirms printed slides before slicing completion; `completeSlicing` now updates the existing printed slicing record and creates downstream staining tasks from printed slides.
- Slicing workbench changed on 2026-06-08: `GET /api/v1/slicings/workbench` supports `applicationType=ROUTINE/FROZEN`, returns `pendingPrintList`, `pendingSliceList`, and `completedTodayList`, and exposes slide print status, printed count, and adjacent merge flags.
- Flyway sync tooling changed on 2026-06-08: `scripts/migration/run-bl-center-flyway.*` defaults `BL_CENTER_FLYWAY_OUT_OF_ORDER=true`, and `BlCenterFlywayCliSupport` passes it into Flyway so legacy/repair-created gaps such as `V75` can be backfilled before newer pending migrations.
- AI memory files are now the durable repo-local state layer for future agents and must be checked before final delivery.
- This supports frontend sibling repo `../SYBaseProjectWeb` technical registration display where the “检查登记” pending/registered lists show sex, age, type, application no, and submitting department.

## Validation Baseline

- Latest barcode binding semantics validation known in this thread:
  - `.\mvnw.cmd -pl bl-center -Dtest=M2CollectionAndLabelIntegrationTest test`: passed on 2026-06-09 (8 tests), covering nullable registration barcodes, unbound management summaries, first barcode binding, and duplicate non-null barcode rejection.
  - Frontend sibling validation in `../SYBaseProjectWeb`: barcode binding/component/service targeted Vitest (3 files, 22 tests), `pnpm check:type`, and `pnpm lint` passed on 2026-06-09.
- Latest specimen operator validation known in this thread:
  - `.\mvnw.cmd -pl bl-center -Dtest=SpecimenWorkflowClosureIntegrationTest test`: passed on 2026-06-09 (21 tests) after making check-in and transport operator tokens optional for the current request user.
  - Frontend sibling validation in `../SYBaseProjectWeb`: targeted Vitest for specimen check-in / transport (4 files, 28 tests), `pnpm lint`, `pnpm check:type`, and targeted `pnpm exec oxfmt --check` passed on 2026-06-09.
- Latest application patient auto-create age validation:
  - `.\mvnw.cmd -pl bl-center "-Dtest=ApplicationCrudAndWorkflowLockIntegrationTest#shouldNormalizeDisplayAgeWhenAutoCreatingPatient" test`: passed on 2026-06-08 after normalizing display-style patient ages before writing patient registry rows.
  - Frontend sibling validation in `../SYBaseProjectWeb`: targeted Vitest for application-registration auto-create/workbench service/panel, `pnpm check:type`, and `pnpm lint` passed on 2026-06-08.
- Latest Flyway sync validation known in this thread:
  - `.\scripts\migration\run-bl-center-flyway.cmd inspect`: reproduced the local DM state on 2026-06-08 with `current_schema_version=78`, `V75` marked `DELETED`, and pending `V79/V80`.
  - `.\scripts\migration\run-bl-center-flyway.cmd sync`: passed on 2026-06-08 after enabling out-of-order sync; Flyway applied `V75` as `OUT_OF_ORDER`, then applied `V79` and `V80`, ending at `current_schema_version=80`, `pending_migration_count=0`, `validation_success=true`.
- Latest historical-status query validation known in this thread:
  - `.\mvnw.cmd -pl bl-center "-Dtest=SpecimenWorkflowClosureIntegrationTest#shouldListSpecimenOutboundsWithMixedStatesAndReflectOutboundUpdates" test`: passed on 2026-06-08 after covering explicit `specimenNo` lookup for registered/ineligible, ready, and already-outbound rows.
  - `.\mvnw.cmd -pl bl-center "-Dtest=TechnicalWorkflowQueryEnhancementIntegrationTest#shouldHideCompletedTechnicalTasksFromPendingListByDefault" test`: passed on 2026-06-08 after covering default active-only technical tasks and `includeAllStatuses=true` completed-task lookup.
- Latest technical specimen registration validation known in this thread:
  - `.\mvnw.cmd "-Dmaven.repo.local=.m2/repository" -pl bl-center -DskipTests compile`: passed on 2026-06-08 after adding application-list `pathologyNo`.
  - `.\mvnw.cmd "-Dmaven.repo.local=.m2/repository" -pl bl-center "-Dtest=ApplicationQueryAndDetailIntegrationTest" test`: blocked on 2026-06-08 by existing duplicate Flyway migration version `V78__add_slicing_task_link.sql` in the dirty worktree.
  - `.\mvnw.cmd "-Dmaven.repo.local=.m2/repository" -pl bl-center -DskipTests test-compile`: blocked on 2026-06-08 by existing `TechnicalTaskTimeoutPolicyTest` constructor mismatch from dirty technical workflow changes.
  - `.\mvnw.cmd -pl bl-center -DskipTests compile`: passed on 2026-06-08.
  - `.\mvnw.cmd -pl bl-center "-Dtest=TechnicalSpecimenRegistrationIntegrationTest#shouldCompleteRegistrationWithManualPathologyNoWhenCandidateIsValidAndUnique+shouldRejectManualPathologyNoWhenCandidateDoesNotMatchSelectedApplicationType+shouldRejectManualPathologyNoWhenCandidateAlreadyBelongsToAnotherCase+shouldAllowManualPathologyNoWhenCandidateAlreadyBelongsToCurrentCase+shouldGenerateConsultationPathologyNoAndPersistApplicationType+shouldRegeneratePathologyNoWhenSelectedTypeDoesNotMatchExistingRule" test`: passed on 2026-06-08.
  - `.\mvnw.cmd -pl bl-center -Dtest=TechnicalSpecimenRegistrationIntegrationTest test`: blocked by an existing unrelated assertion in `shouldCompleteRegistrationIdempotentlyAndCreateSingleGrossingTask` where expected patient id `P-001` differs from an existing UUID-like value.
  - `.\mvnw.cmd -pl bl-center -Dtest=NumberingServiceTest test`: blocked by unrelated dirty slicing workflow compile errors in `TechnicalWorkflowQueryService`, `TechnicalProcessingWorkflowService`, and `JdbcTechnicalWorkflowRepository`.
- Latest slicing workstation validation known in this thread:
  - `.\mvnw.cmd -pl bl-center -DskipTests compile`: passed on 2026-06-08.
  - `.\mvnw.cmd -pl bl-center -Dtest=SlicingWorkbenchIntegrationTest -DskipITs test`: passed on 2026-06-08 (3 tests).

## Cross-Repo Dependencies

- Frontend governance and memory files live in sibling repo `../SYBaseProjectWeb`.
- Frontend application-number auto-create changes live in `../SYBaseProjectWeb/apps/web-ele/src/modules/specimen-workflow`: `POST /api/v1/applications` receives numeric `patientAge`, workbench patient info keeps display age, and lookup 404 suppresses the global toast before auto-create.
- Frontend tracking and abnormal list display changes live in `../SYBaseProjectWeb/apps/web-ele/src/modules/specimen-workflow`; they consume `GET /api/v1/applications` `items[].pathologyNo` and localize current-node codes.
- Frontend technical registration display changes live in `../SYBaseProjectWeb/apps/web-ele/src/modules/technical-workflow`.
- Frontend completion draft UI/type changes live in `../SYBaseProjectWeb/apps/web-ele/src/modules/technical-workflow`; the frontend sends optional `pathologyNo` only when completing registration.
- Frontend slicing workstation changes live in `../SYBaseProjectWeb/apps/web-ele/src/modules/technical-workflow`; the frontend treats successful `POST /api/v1/slicings/slide-print` as the transition from `玻片打印` to `切片`.
- Frontend historical-status query changes live in `../SYBaseProjectWeb/apps/web-ele/src/modules/specimen-workflow` and `../SYBaseProjectWeb/apps/web-ele/src/modules/technical-workflow`; ordinary entry is empty, explicit business-key lookup can show all-status rows, and operations remain gated by row status.
- Frontend specimen check-in / transport changes live in `../SYBaseProjectWeb/apps/web-ele/src/modules/specimen-workflow`; current selected operators omit `operatorVerificationToken`, while selected non-current operators still call the verification prompt.
- Frontend barcode binding changes live in `../SYBaseProjectWeb/apps/web-ele/src/modules/specimen-workflow`; frontend types allow nullable specimen tracking barcodes, and UI/downstream payload builders treat null/blank barcode as `UNBOUND` until barcode binding succeeds.
- API, permission, patient, report, and menu changes must update memory files in both repos when they change durable context.

## Handoff Notes

- Start future sessions by reading this file, `DECISIONS.md`, and `KNOWN_BUGS.md`, then inspect `git status`.
- If governance docs and business code are both dirty, split them into separate commits before pushing.
