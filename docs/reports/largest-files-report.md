# Largest Files Report

Generated at `2026-05-30T10:39:26Z`.

## Temporary Exemptions

- Active exemptions: `3`
- Exemption baseline: `3`
- MAX_LINES waivers: `3`
- MAX_SIZE waivers: `2`

| # | Waive | Target |
| ---: | --- | --- |
| 1 | `MAX_LINES,MAX_SIZE` | `docs/database/*.sql` |
| 2 | `MAX_LINES,MAX_SIZE` | `infrastructure/monitor/grafana/dashboards/*.json` |
| 3 | `MAX_LINES` | `docs/plans/*.md` |

### Exemption Rationale

1. `docs/database/*.sql`: Historical database reference scripts stay consolidated for traceability and deployment diff review; keep them intact until the SQL reference set is moved into versioned, topic-scoped bundles.
2. `infrastructure/monitor/grafana/dashboards/*.json`: Grafana dashboard exports are machine-generated artifacts; keep the raw export shape until dashboard provisioning switches to a templated source plus generated output flow.
3. `docs/plans/*.md`: Planning documents are maintained as complete narrative artifacts; keep them whole until the milestone archive flow moves historical plans into dated subpackages with summary indexes.

## Files Exceeding Current Hard Limits

| Lines | Limit | File |
| ---: | ---: | --- |
| 596 | 200 | `infrastructure/monitor/grafana/dashboards/bl-center-overview.json` |
| 356 | 200 | `infrastructure/monitor/grafana/dashboards/user-center-overview.json` |

## Java Files Over 300 Lines

Count: `49`

| Lines | File |
| ---: | --- |
| 913 | `bl-center/src/main/java/com/company/bl/integration/infrastructure/M6JdbcRepository.java` |
| 911 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/AbstractJdbcSpecimenWorkflowProjectionSupport.java` |
| 768 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcApplicationRegistrationWorkbenchRepository.java` |
| 747 | `bl-center/src/main/java/db/migration/V11__reconcile_legacy_dm_schema.java` |
| 691 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcSpecimenWorkflowRepository.java` |
| 591 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcDiagnosticReportRepository.java` |
| 508 | `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowModels.java` |
| 475 | `bl-center/src/main/java/com/company/bl/tools/BlCenterFlywayCli.java` |
| 475 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/AbstractJdbcSpecimenWorkflowReadSupport.java` |
| 461 | `bl-center/src/main/java/com/company/bl/masterdata/application/SamplingService.java` |
| 452 | `bl-center/src/main/java/db/migration/V44__seed_workflow_reference_options.java` |
| 439 | `bl-center/src/main/java/com/company/bl/masterdata/application/MedicalOrderService.java` |
| 436 | `bl-center/src/test/java/com/company/bl/interfaces/SpecimenWorkflowClosureIntegrationTest.java` |
| 427 | `bl-center/src/main/java/com/company/bl/system/application/SystemUserManagementService.java` |
| 418 | `bl-center/src/test/java/com/company/bl/interfaces/MasterDataControllerIntegrationTest.java` |
| 406 | `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowQueryService.java` |
| 405 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcOperationSupportRepository.java` |
| 404 | `bl-center/src/main/java/com/company/bl/interfaces/controller/SpecimenControllerAssembler.java` |
| 400 | `bl-center/src/main/java/db/migration/V54__reconcile_workflow_reference_options.java` |
| 394 | `bl-center/src/main/java/com/company/bl/integration/application/BillingManagementService.java` |
| 394 | `bl-center/src/test/java/com/company/bl/interfaces/SystemManagementRoleAndMenuIntegrationTest.java` |
| 391 | `bl-center/src/main/java/db/migration/V17__reconcile_m1_permission_codes.java` |
| 391 | `bl-center/src/main/java/com/company/bl/integration/application/StatisticsService.java` |
| 387 | `bl-center/src/main/java/com/company/bl/masterdata/infrastructure/SamplingJdbcRepository.java` |
| 387 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcArchiveQueries.java` |
| 376 | `bl-center/src/test/java/com/company/bl/interfaces/ApplicationRegistrationWorkbenchIntegrationTest.java` |
| 375 | `bl-center/src/main/java/com/company/bl/masterdata/interfaces/MedicalOrderController.java` |
| 364 | `bl-center/src/main/java/com/company/bl/system/infrastructure/SystemRoleJdbcRepository.java` |
| 363 | `bl-center/src/main/java/com/company/bl/notification/infrastructure/NotificationCenterJdbcRepository.java` |
| 362 | `bl-center/src/main/java/com/company/bl/system/interfaces/SystemManagementController.java` |

## Java Files Over 500 Lines

Count: `7`

| Lines | File |
| ---: | --- |
| 913 | `bl-center/src/main/java/com/company/bl/integration/infrastructure/M6JdbcRepository.java` |
| 911 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/AbstractJdbcSpecimenWorkflowProjectionSupport.java` |
| 768 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcApplicationRegistrationWorkbenchRepository.java` |
| 747 | `bl-center/src/main/java/db/migration/V11__reconcile_legacy_dm_schema.java` |
| 691 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcSpecimenWorkflowRepository.java` |
| 591 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcDiagnosticReportRepository.java` |
| 508 | `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowModels.java` |

## Top 20 Java Files

| Lines | File |
| ---: | --- |
| 913 | `bl-center/src/main/java/com/company/bl/integration/infrastructure/M6JdbcRepository.java` |
| 911 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/AbstractJdbcSpecimenWorkflowProjectionSupport.java` |
| 768 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcApplicationRegistrationWorkbenchRepository.java` |
| 747 | `bl-center/src/main/java/db/migration/V11__reconcile_legacy_dm_schema.java` |
| 691 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcSpecimenWorkflowRepository.java` |
| 591 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcDiagnosticReportRepository.java` |
| 508 | `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowModels.java` |
| 475 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/AbstractJdbcSpecimenWorkflowReadSupport.java` |
| 475 | `bl-center/src/main/java/com/company/bl/tools/BlCenterFlywayCli.java` |
| 461 | `bl-center/src/main/java/com/company/bl/masterdata/application/SamplingService.java` |
| 452 | `bl-center/src/main/java/db/migration/V44__seed_workflow_reference_options.java` |
| 439 | `bl-center/src/main/java/com/company/bl/masterdata/application/MedicalOrderService.java` |
| 436 | `bl-center/src/test/java/com/company/bl/interfaces/SpecimenWorkflowClosureIntegrationTest.java` |
| 427 | `bl-center/src/main/java/com/company/bl/system/application/SystemUserManagementService.java` |
| 418 | `bl-center/src/test/java/com/company/bl/interfaces/MasterDataControllerIntegrationTest.java` |
| 406 | `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowQueryService.java` |
| 405 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcOperationSupportRepository.java` |
| 404 | `bl-center/src/main/java/com/company/bl/interfaces/controller/SpecimenControllerAssembler.java` |
| 400 | `bl-center/src/main/java/db/migration/V54__reconcile_workflow_reference_options.java` |
| 394 | `bl-center/src/main/java/com/company/bl/integration/application/BillingManagementService.java` |
