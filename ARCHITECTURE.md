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

## Constraints

- Do not introduce `domain -> infrastructure`, `domain -> interfaces`, or `interfaces -> repository` shortcuts.
- Do not modify database migrations, security core, production config, or CI/CD release scripts without explicit risk explanation and human confirmation.
- Do not record temporary implementation details here; use `PROJECT_STATE.md` for current phase and `DECISIONS.md` for durable choices.
