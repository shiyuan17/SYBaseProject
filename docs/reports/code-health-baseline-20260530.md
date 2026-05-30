# Code Health Baseline Report

Generated at `2026-05-30`.

## Scope

This report captures the current baseline for the backend health work tracked in Linear.
It is meant to be a stable reference point for the four follow-up refactors and the wider governance work.

## Repository Snapshot

| Metric | Value |
| --- | ---: |
| Java files | 560 |
| Test files | 94 |
| Total Surefire tests | 325 |
| Surefire failures/errors/skipped | 0 / 0 / 0 |
| Surefire runtime | 86.264 s |
| Fast feedback command | `.\mvnw.cmd test "-Dsurefire.excludedGroups=slow"` |
| Fast feedback status | PASS |
| Fast feedback runtime | 21.786 s |

## Coverage Baseline

| Metric | Value |
| --- | ---: |
| JaCoCo line coverage baseline | 0.858216 |
| JaCoCo branch coverage baseline | 0.557996 |

## File Health Baseline

| Metric | Value |
| --- | ---: |
| Java files over 300 lines | 49 |
| Java files over 500 lines | 8 |
| Active file-health exemptions | 3 |
| Exemption baseline | 3 |

Current exemptions remain limited to the approved historical SQL, Grafana dashboard, and planning document bundles.

## Top Hotspots

| Lines | File |
| ---: | --- |
| 922 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcSpecimenWorkflowRepository.java` |
| 913 | `bl-center/src/main/java/com/company/bl/integration/infrastructure/M6JdbcRepository.java` |
| 911 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/AbstractJdbcSpecimenWorkflowProjectionSupport.java` |
| 768 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcApplicationRegistrationWorkbenchRepository.java` |
| 747 | `bl-center/src/main/java/db/migration/V11__reconcile_legacy_dm_schema.java` |
| 591 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcDiagnosticReportRepository.java` |
| 533 | `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowSupport.java` |
| 508 | `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowModels.java` |
| 494 | `bl-center/src/main/java/com/company/bl/interfaces/controller/SpecimenController.java` |
| 475 | `bl-center/src/main/java/com/company/bl/tools/BlCenterFlywayCli.java` |
| 475 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/AbstractJdbcSpecimenWorkflowReadSupport.java` |
| 461 | `bl-center/src/main/java/com/company/bl/masterdata/application/SamplingService.java` |

## Package Distribution

### Main Code

| Area | Count |
| --- | ---: |
| interfaces | 158 |
| application | 49 |
| db | 45 |
| infrastructure | 40 |
| domain | 36 |
| masterdata | 20 |
| system | 7 |
| integration | 6 |
| support | 4 |
| notification | 3 |
| tools | 1 |

### Test Code

| Area | Count |
| --- | ---: |
| interfaces | 52 |
| infrastructure | 15 |
| application | 14 |
| masterdata | 3 |
| support | 1 |
| domain | 1 |

## Observations

- `bl-center` remains the main complexity hotspot, especially around JDBC repositories, workflow support, and controller mapping.
- The health gate is already enforceable, so the next gain comes from reducing depth in the largest modules rather than inventing new rules.
- The current baseline is good enough to start slice-by-slice refactoring without changing external API or database behavior.

## Verification

- `.\mvnw.cmd test "-Dsurefire.excludedGroups=slow"` passed.
- File-health checks passed on the current repository state.
