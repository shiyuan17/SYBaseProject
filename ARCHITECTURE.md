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
- `GET /api/v1/applications` returns optional `items[].pathologyNo`, resolved from `pathology_cases.pathology_no`; application-list consumers should treat `null` as “not generated yet”.
- Technical specimen registration completion accepts optional `pathologyNo` on `POST /api/v1/technical-specimen-registrations/{caseId}/complete`; backend validates non-empty candidates by selected application type rule and uniqueness, and generates only when empty.
- Slicing workflow requires backend-confirmed slide printing through `POST /api/v1/slicings/slide-print` before `POST /api/v1/slicings/complete`; printed slide records are linked to `slicings.task_id`.
- `GET /api/v1/slicings/workbench` accepts optional `applicationType=ROUTINE/FROZEN` and returns split pending-print, pending-slice, and completed lists with slide print/merge fields.
- `GET /api/v1/technical-tasks/pending` defaults to active technical tasks, but accepts optional `includeAllStatuses` and `taskId` for explicit historical lookup contexts.
- `GET /api/v1/specimen-outbounds` applies outbound-ready filtering for ordinary list semantics, but explicit `applicationId` or `specimenNo` lookups may return all matching specimen statuses.
- M2 specimen check-in and transport endpoints may accept blank `operatorVerificationToken` only for the current authenticated user; non-current selected operators still resolve through `OperatorVerificationService`, and confirmation endpoints keep their verification requirement.
- M2 `specimens.barcode` is nullable after registration; null/blank barcode means `UNBOUND`, and `POST /api/v1/specimens/{specimenId}/barcode-binding` is the first-write target barcode operation. Barcode-based downstream APIs should only receive bound non-blank barcodes.

## Constraints

- Do not introduce `domain -> infrastructure`, `domain -> interfaces`, or `interfaces -> repository` shortcuts.
- Do not modify database migrations, security core, production config, or CI/CD release scripts without explicit risk explanation and human confirmation.
- Do not record temporary implementation details here; use `PROJECT_STATE.md` for current phase and `DECISIONS.md` for durable choices.
