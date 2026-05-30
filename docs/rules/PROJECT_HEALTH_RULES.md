# Project Health Rules

## Purpose

These rules keep structural debt visible while the repository is still evolving.
They are intentionally lighter than the hard coding rules: the goal is to steer refactors and CI reporting without blocking delivery for every large historical file.

## Architecture Expectations

- `bl-center` should keep controllers thin and push orchestration into application services.
- Large application services should be split by query, write, import/export, or subdomain responsibility instead of growing generic helper classes.
- Large repositories should follow the same rule: separate read, write, and specialty persistence flows behind a thin facade when the public Spring bean should stay stable.
- Transport and response models should move into dedicated domain-named model files when they are the main reason a service crosses the hotspot threshold.
- For read-heavy persistence code, separate query entrypoints from schema-probing and row-mapping responsibilities when those concerns become the main source of file growth.
- DTO/VO mapping belongs in explicit assemblers or mappers, not mixed into routing controllers.

## Shared Module Boundaries

- Shared web infrastructure belongs in `common/common-web`.
- Shared testing infrastructure belongs in `common/common-test`.
- Do not let `common` absorb domain-specific logic from `bl-center`; prefer extracting small stable contracts or utilities only when they are truly cross-module.

## Test Shapes

- `BaseWebIntegrationTest` remains the default slow web/database integration base and stays tagged with `@Tag("slow")`.
- `BaseJdbcWebIntegrationTest` is for JDBC-backed web flows that still need the full web stack.
- `BaseMockMvcIntegrationTest` is for MVC/API behavior that does not need the full JDBC runtime.
- Domain and application services should prefer focused unit or slice tests before adding more slow end-to-end coverage.

## Verification Commands

- Fast feedback: `./mvnw test "-Dsurefire.excludedGroups=slow"`
- Full verification: `./mvnw verify`
- Repository file-health reporting: refresh `docs/reports/largest-files-report.md` from the current working tree whenever a hotspot split materially changes the shape of the top files.

## CI Modes

- `verify_fast` should run the fast feedback command above and exclude `slow`.
- `verify_full` should run full `verify`.
- API regression and domain regression suites can be grouped separately, but the repository should keep one easy fast path for day-to-day feedback.
- File-health reporting is an alert/report step; the hard blockers remain the file-health gate, coverage baseline, and existing fast-feedback path.

## Reporting Rules

- Any new file-health exemption must update `docs/file-health-exemptions.properties` and include a rationale in `docs/reports/largest-files-report.md`.
- Use `docs/reports/code-health-trend-20260530.md` to explain why a split increased the number of support files even when it reduced concentrated hotspots.
- Use `docs/reports/code-health-checklist.md` to summarize the current health snapshot for reviewers.
- Treat `docs/reports/largest-files-report.md` as the source of truth for current line-count hotspots.

## Current Hotspot Targets

- This list supersedes older service-first target lists when they differ.
- `SamplingJdbcRepository`
- `JdbcSpecimenWorkflowSpecimenMutationSupport`
- `StatisticsService`
- `SpecimenControllerAssembler`
- `JdbcOperationSupportRepository`
- `ArchiveWorkflowService`

## Practical Guidance

- Prefer many small, domain-named files over one oversized service or repository.
- Keep facades stable when external Spring wiring should not change.
- When a split only shifts lines around, document the tradeoff clearly in the trend report.
- Do not add generic names such as `Utils`, `Helper`, or `CommonService` just to move code out of a hotspot.
