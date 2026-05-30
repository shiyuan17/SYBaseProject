# Code Health Trend Report

Generated at `2026-05-30`.

## Scope

This report records the first health-governance trend after the specimen workflow refactor.
It complements `docs/reports/code-health-baseline-20260530.md` and keeps the next governance slices visible without tightening CI beyond the existing release gates.

## Trend Summary

| Metric | Baseline | Current | Trend |
| --- | ---: | ---: | ---: |
| Java files over 300 lines | 49 | 49 | 0 |
| Java files over 500 lines | 8 | 7 | -1 |
| Active file-health exemptions | 3 | 3 | 0 |
| Total Surefire tests | 325 | 327 | +2 |
| JaCoCo line coverage baseline | 0.858216 | 0.858216 | 0 |
| JaCoCo branch coverage baseline | 0.557996 | 0.557996 | 0 |

## Refactor Impact

| File | Baseline lines | Current lines | Change |
| --- | ---: | ---: | ---: |
| `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowSupport.java` | 533 | 151 | -382 |
| `bl-center/src/main/java/com/company/bl/interfaces/controller/SpecimenController.java` | 494 | 153 | -341 |
| `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcSpecimenWorkflowRepository.java` | 922 | 691 | -231 |
| `bl-center/src/main/java/com/company/bl/interfaces/controller/SpecimenControllerAssembler.java` | 0 | 404 | +404 |
| `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcSpecimenWorkflowQueryRepository.java` | 109 | 330 | +221 |

The growth in assembler/query adapter files is intentional locality: controller mapping and application-list SQL now live outside the routing controller and command repository.

## Current Hotspots

| Lines | File |
| ---: | --- |
| 913 | `bl-center/src/main/java/com/company/bl/integration/infrastructure/M6JdbcRepository.java` |
| 911 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/AbstractJdbcSpecimenWorkflowProjectionSupport.java` |
| 768 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcApplicationRegistrationWorkbenchRepository.java` |
| 747 | `bl-center/src/main/java/db/migration/V11__reconcile_legacy_dm_schema.java` |
| 691 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcSpecimenWorkflowRepository.java` |
| 591 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcDiagnosticReportRepository.java` |

## Next Governance Targets

1. `SamplingService`
2. `MedicalOrderService`
3. `SystemUserManagementService`
4. `StatisticsService`
5. `BillingManagementService`

Each target should follow the same sequence used here: baseline first, extract domain/application responsibilities second, keep external API and database behavior stable, then verify with focused tests plus fast feedback.

## CI Integration Mode

- Keep `RepositoryFileHealthGateTest` as the hard static gate for encoding, line endings, exemptions, and hard limits.
- Keep JaCoCo thresholds at the current baseline in `bl-center/jacoco-baseline.properties`.
- Use `scripts/ci/generate-largest-files-report.ps1` or `.sh` as a report artifact/alert step first; do not fail builds solely because the trend report worsens until the team explicitly raises the baseline.
- Treat any new exemption as an explicit governance decision requiring `docs/file-health-exemptions.properties` and a matching rationale.

## Verification

- `.\mvnw.cmd -pl bl-center -am "-Dtest=M2CollectionAndLabelIntegrationTest,ApplicationRegistrationWorkbenchIntegrationTest,SpecimenWorkflowQueueAndVerificationIntegrationTest,SpecimenWorkflowEndToEndIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` passed.
- `.\mvnw.cmd test "-Dsurefire.excludedGroups=slow"` passed.
- `scripts/ci/generate-largest-files-report.ps1` refreshed `docs/reports/largest-files-report.md`.
