# ARCHITECTURE.md

## Architecture Snapshot

- Repository: `SYBaseProject`
- Shape: Java 17 / Spring Boot 3 Maven multi-module backend
- Core modules: `auth-center`, `user-center`, `bl-center`
- Shared modules: `common/common-core`, `common/common-security`, `common/common-web`, `common/common-test`
- Tooling module: `tools/app-cli`
- Frontend sibling: `../SYBaseProjectWeb`

## Backend Boundaries

- `interfaces` adapts HTTP/API input and output.
- `application` orchestrates use cases and transaction boundaries.
- `domain` owns business rules, aggregates, value objects, domain services, and repository contracts.
- `infrastructure` owns persistence, external integrations, mapper implementations, caches, and technical adapters.

## Cross-Repo Interfaces

- Frontend request/permission/menu changes must be checked against `SYBaseProjectWeb`.
- Database, patient, report, and permission changes must update backend and frontend memory files when they affect durable context.
- System log management is served by `bl-center` for sibling frontend `/system/logs`: login logs are queried from `user_login_logs`, operation logs from `operation_logs`, and APIs are `/api/v1/system/logs/login`, `/login/{id}`, `/operations`, and `/operations/{id}`. Menu/permissions are `SYS_LOG_MANAGEMENT`, `PERM_SYS_LOG_QUERY`, and `PERM_SYS_LOG_DETAIL`.
- Operation audit is cross-cutting in `ApiAuditInterceptor`: authenticated handler methods with `@RequirePermission` are audited for non-GET requests, and sensitive GET requests opt in through `@AuditOperation(sensitiveQuery = true)`. Audit content/failure fields must be sanitized/truncated and audit writes must not change the original HTTP outcome or recurse.
- M5 archive cabinet maintenance is served by `bl-center` for sibling frontend `/operation-support/archive`: `POST /api/v1/archive-cabinets/batch` creates cabinets/positions from prefix + serials using create permission, and `DELETE /api/v1/archive-cabinets/{id}` requires `PERM_M5_ARCHIVE_CABINET_DELETE`. Deletion must reject occupied/current-object positions and any storage/loan references before deleting cabinet positions.
- M5 reagent management is served by `bl-center` for sibling frontend `/operation-resources/reagents`: `reagents` stores reagent templates, `reagent_stocks` stores batch inventory, and `reagent_stock_events` stores inbound/test/consume/start-use/finish-use/status/import events. `/api/v1/reagents`, `/api/v1/reagent-stocks`, stock action/event endpoints, and UTF-8 BOM CSV import/export reuse existing M5 reagent permission codes; operator identity is resolved from the authenticated request context, not reagent request body fields.
- `GET /api/v1/applications` returns optional `items[].pathologyNo`, resolved from `pathology_cases.pathology_no`; application-list consumers should treat `null` as “not generated yet”.
- Technical specimen registration completion accepts optional `pathologyNo` on `POST /api/v1/technical-specimen-registrations/{caseId}/complete`; backend validates non-empty candidates by selected application type rule and uniqueness, and generates only when empty.
- Slicing workflow requires backend-confirmed slide printing through `POST /api/v1/slicings/slide-print` before `POST /api/v1/slicings/complete`; printed slide records are linked to `slicings.task_id`.
- `GET /api/v1/slicings/workbench` accepts optional `applicationType=ROUTINE/FROZEN` and returns split pending-print, pending-slice, and completed lists with slide print/merge fields; row responses also expose `embeddingBoxNo`, `embeddingRemarks`, and `submittingDepartmentName` for frontend slide-print display. Pending merge-group rows are virtual rows with `printGroupId`, `mergedPrintGroup`, `taskIds`, `embeddingBoxIds`, and merged `embeddingBoxNo` values such as `A1+A2`.
- Pending slide-print merge operations are persisted through `POST /api/v1/slicings/slide-print-merge-groups`, `/slide-print-merge-groups/cancel`, and `/slide-print-merge-groups/print`; creation pairs only unprinted slicing tasks within the same patient, same case/pathology number, and same embedding-box letter prefix.
- M3 embedding box numbers are unique within a pathology case only; `embedding_boxes` is constrained by `case_id + embedding_box_no`, and sibling frontend grossing submits short box numbers such as `A1/B1/C1`.
- Embedding workflow is two-step with a narrow rollback before final completion: `POST /api/v1/embeddings/start` persists `EMBEDDING_CONFIRM_PENDING`, `POST /api/v1/embeddings/cancel` can return only that middle state to `PENDING`, and only `POST /api/v1/embeddings/complete` writes final `COMPLETED` embedding records and creates slicing pending tasks; legacy `IN_PROGRESS` embedding tasks remain completable but are not cancellable through the rollback endpoint.
- `GET /api/v1/technical-tasks/pending` defaults to active technical tasks, but accepts optional `includeAllStatuses` and `taskId` for explicit historical lookup contexts.
- `GET /api/v1/specimen-outbounds` applies outbound-ready filtering for ordinary list semantics, but explicit `applicationId`, `specimenNo`, or `identifier` lookups may return all matching specimen statuses; `identifier` is an exact specimen-number-or-barcode condition.
- M2 specimen check-in and transport endpoints may accept blank `operatorVerificationToken` only for the current authenticated user; non-current selected operators still resolve through `OperatorVerificationService`, and confirmation endpoints keep their verification requirement.
- M2 `specimens.barcode` is nullable after registration; null/blank barcode means `UNBOUND`, and `POST /api/v1/specimens/{specimenId}/barcode-binding` is the first-write target barcode operation. Barcode-based downstream APIs should only receive bound non-blank barcodes.
- M2 fixation, specimen confirmation, check-in, receipt, and outbound transport progression do not require a bound barcode; backend DTOs accept preferred row `specimenId` plus compatible `specimenBarcode` / `specimenNo`, resolve in that priority order, receipt uses the same resolver for normal/direct paths, and transport creation accepts `specimenIds` before legacy `specimenBarcodes`.

## Constraints

- Do not introduce `domain -> infrastructure`, `domain -> interfaces`, or `interfaces -> repository` shortcuts.
- Do not modify database migrations, security core, production config, or CI/CD release scripts without explicit risk explanation and human confirmation.
- Do not record temporary implementation details here; use `PROJECT_STATE.md` for current phase and `DECISIONS.md` for durable choices.
