# Code Health Trend Report

Generated at `2026-05-31`.

## Scope

This report tracks the current structural-health snapshot for the repository after the latest specimen workflow, system-user, sampling, workflow-read-support, workflow-query-model, Flyway CLI, V11 migration, and V44 migration splits.
It also reflects a corrected line-count script, so the current totals are not directly comparable with earlier undercounted snapshots.
It complements `docs/reports/code-health-baseline-20260530.md` and keeps the next governance slices visible without tightening CI beyond the existing release gates.

## Trend Summary

| Metric | Baseline | Current | Trend |
| --- | ---: | ---: | ---: |
| Java files over 300 lines | 49 | 58 | counter fixed |
| Java files over 500 lines | 8 | 0 | -8 with corrected counter |
| Active file-health exemptions | 3 | 3 | 0 |
| Total Surefire tests | 325 | 327 | +2 |
| JaCoCo line coverage baseline | 0.858216 | 0.858216 | 0 |
| JaCoCo branch coverage baseline | 0.557996 | 0.557996 | 0 |

## Refactor Impact

| File | Baseline lines | Current lines | Change |
| --- | ---: | ---: | ---: |
| `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcSpecimenWorkflowRepository.java` | 922 | 9 | -913 |
| `bl-center/src/main/java/com/company/bl/infrastructure/persistence/AbstractJdbcSpecimenWorkflowProjectionSupport.java` | 911 | 46 | -865 |
| `bl-center/src/main/java/com/company/bl/integration/infrastructure/M6JdbcRepository.java` | 913 | 135 | -778 |
| `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcApplicationRegistrationWorkbenchRepository.java` | 768 | 210 | -558 |
| `bl-center/src/main/java/com/company/bl/system/application/SystemUserManagementService.java` | 478 | 61 | -417 |
| `bl-center/src/main/java/com/company/bl/masterdata/application/SamplingService.java` | 461 | 85 | -376 |
| `bl-center/src/main/java/com/company/bl/infrastructure/persistence/AbstractJdbcSpecimenWorkflowReadSupport.java` | 475 | 153 | -322 |
| `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowModels.java` | 508 | 234 | -274 |
| `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowQueryService.java` | 406 | 79 | -327 |
| `bl-center/src/main/java/db/migration/V44__seed_workflow_reference_options.java` | 480 | 12 | -468 |
| `bl-center/src/main/java/db/migration/V44WorkflowReferenceMetadataSupport.java` | 0 | 278 | +278 |
| `bl-center/src/main/java/db/migration/V44WorkflowReferenceSeedData.java` | 0 | 65 | +65 |
| `bl-center/src/main/java/db/migration/V44WorkflowReferenceSupport.java` | 0 | 43 | +43 |
| `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowQueryModels.java` | 0 | 273 | +273 |
| `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowPendingQuerySupport.java` | 0 | 115 | +115 |
| `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowApplicationQuerySupport.java` | 0 | 139 | +139 |
| `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowTrackingQuerySupport.java` | 0 | 85 | +85 |
| `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowRemovalQuerySupport.java` | 0 | 151 | +151 |
| `bl-center/src/main/java/com/company/bl/tools/BlCenterFlywayCli.java` | 528 | 18 | -510 |
| `bl-center/src/main/java/com/company/bl/tools/BlCenterFlywayCliSupport.java` | 0 | 375 | +375 |
| `bl-center/src/main/java/com/company/bl/tools/BlCenterFlywayTableSupport.java` | 0 | 161 | +161 |

The current trend is still intentionally split-heavy: the repository is trading oversized facades for a few cohesive query/write/model or schema/mapper files so each hotspot stays easier to reason about.
The workflow read side now follows the same pattern as the recent service refactors: `SpecimenWorkflowQueryService` is now a thin facade over query-specific supports, and schema-probing/row-mapping responsibilities stay outside the application layer.
The latest workflow slice finished the remaining query-model extraction: `SpecimenWorkflowModels` now keeps write-side commands/results, while query/list/summary contracts live in `SpecimenWorkflowQueryModels`.
The latest operations slice also reduced the last non-migration 500+ Java class: `BlCenterFlywayCli` is now a thin entrypoint backed by focused support classes for command execution and managed-table inventory.
The latest migration slice removed the final 500+ Java file from the repository view: `V11__reconcile_legacy_dm_schema` now delegates into `V11LegacyDmSchemaSupport`, and the remaining migration support sits below the 500-line threshold.
The latest reference-data slice split `V44__seed_workflow_reference_options` into a thin Flyway entrypoint plus metadata and seed-data supports, which dropped the historical migration out of the top hotspot list.

## Current Hotspots

| Lines | File |
| ---: | --- |
| 473 | `bl-center/src/test/java/com/company/bl/interfaces/SpecimenWorkflowClosureIntegrationTest.java` |
| 468 | `bl-center/src/test/java/com/company/bl/interfaces/MasterDataControllerIntegrationTest.java` |
| 438 | `bl-center/src/main/java/com/company/bl/masterdata/infrastructure/SamplingJdbcRepository.java` |
| 437 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcSpecimenWorkflowSpecimenMutationSupport.java` |
| 437 | `bl-center/src/test/java/com/company/bl/interfaces/SystemManagementRoleAndMenuIntegrationTest.java` |
| 434 | `bl-center/src/main/java/com/company/bl/integration/application/StatisticsService.java` |

## Next Governance Targets

1. `SamplingJdbcRepository`
2. `JdbcSpecimenWorkflowSpecimenMutationSupport`
3. `StatisticsService`
4. `SpecimenControllerAssembler`
5. `JdbcOperationSupportRepository`
6. `ArchiveWorkflowService`

Each target should follow the same sequence used here: baseline first, extract domain/application responsibilities second, keep external API and database behavior stable, then verify with focused tests plus fast feedback.

## CI Integration Mode

- Keep `RepositoryFileHealthGateTest` as the hard static gate for encoding, line endings, exemptions, and hard limits.
- Keep JaCoCo thresholds at the current baseline in `bl-center/jacoco-baseline.properties`.
- Use `scripts/ci/generate-largest-files-report.ps1` or `.sh` as a report artifact/alert step first; do not fail builds solely because the trend report worsens until the team explicitly raises the baseline.
- Treat any new exemption as an explicit governance decision requiring `docs/file-health-exemptions.properties` and a matching rationale.

## Verification

- `.\mvnw.cmd -pl bl-center -DskipTests compile` still fails on the pre-existing missing `com.company.common.web.observability` dependency chain.
- No new compile errors from the latest V44 migration split surfaced before that existing blocker stopped the build.
- `docs/reports/largest-files-report.md` was refreshed from the corrected line-count snapshot after the latest V44 migration split.
