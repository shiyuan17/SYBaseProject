# Largest Files Report

Generated at `2026-05-30T19:48:45Z`.

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

Count: `58`

| Lines | File |
| ---: | --- |
| 473 | `bl-center/src/test/java/com/company/bl/interfaces/SpecimenWorkflowClosureIntegrationTest.java` |
| 468 | `bl-center/src/test/java/com/company/bl/interfaces/MasterDataControllerIntegrationTest.java` |
| 438 | `bl-center/src/main/java/com/company/bl/masterdata/infrastructure/SamplingJdbcRepository.java` |
| 437 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcSpecimenWorkflowSpecimenMutationSupport.java` |
| 437 | `bl-center/src/test/java/com/company/bl/interfaces/SystemManagementRoleAndMenuIntegrationTest.java` |
| 434 | `bl-center/src/main/java/com/company/bl/integration/application/StatisticsService.java` |
| 433 | `bl-center/src/main/java/com/company/bl/interfaces/controller/SpecimenControllerAssembler.java` |
| 432 | `bl-center/src/main/java/db/migration/V11LegacyDmSchemaSupport.java` |
| 432 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcOperationSupportRepository.java` |
| 431 | `bl-center/src/main/java/db/migration/V54__reconcile_workflow_reference_options.java` |
| 429 | `bl-center/src/test/java/com/company/bl/interfaces/M2CollectionAndLabelIntegrationTest.java` |
| 426 | `bl-center/src/main/java/db/migration/V17__reconcile_m1_permission_codes.java` |
| 421 | `bl-center/src/main/java/com/company/bl/masterdata/application/MedicalOrderChargeService.java` |
| 419 | `bl-center/src/main/java/com/company/bl/integration/application/BillingManagementService.java` |
| 413 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcArchiveQueries.java` |
| 412 | `bl-center/src/test/java/com/company/bl/interfaces/ApplicationRegistrationWorkbenchIntegrationTest.java` |
| 411 | `bl-center/src/main/java/com/company/bl/masterdata/interfaces/MedicalOrderController.java` |
| 401 | `bl-center/src/main/java/com/company/bl/system/infrastructure/SystemRoleJdbcRepository.java` |
| 394 | `bl-center/src/main/java/com/company/bl/notification/infrastructure/NotificationCenterJdbcRepository.java` |
| 394 | `bl-center/src/main/java/com/company/bl/system/interfaces/SystemManagementController.java` |
| 393 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcSpecimenWorkflowPendingProjectionSupport.java` |
| 383 | `bl-center/src/main/java/com/company/bl/masterdata/infrastructure/MedicalOrderJdbcRepository.java` |
| 377 | `bl-center/src/main/java/com/company/bl/interfaces/controller/ApplicationController.java` |
| 376 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcTechnicalWorkflowTaskMutations.java` |
| 375 | `bl-center/src/main/java/com/company/bl/tools/BlCenterFlywayCliSupport.java` |
| 372 | `bl-center/src/test/java/com/company/bl/interfaces/ApplicationQueryAndDetailIntegrationTest.java` |
| 370 | `bl-center/src/test/java/com/company/bl/interfaces/DiagnosticWorkflowReportLifecycleIntegrationTest.java` |
| 370 | `bl-center/src/main/java/com/company/bl/application/service/TechnicalWorkflowModels.java` |
| 355 | `bl-center/src/test/java/com/company/bl/interfaces/SpecimenWorkflowQueueAndVerificationIntegrationTest.java` |
| 354 | `bl-center/src/main/java/com/company/bl/application/service/ArchiveWorkflowService.java` |

## Java Files Over 500 Lines

Count: `0`


## Top 20 Java Files

| Lines | File |
| ---: | --- |
| 473 | `bl-center/src/test/java/com/company/bl/interfaces/SpecimenWorkflowClosureIntegrationTest.java` |
| 468 | `bl-center/src/test/java/com/company/bl/interfaces/MasterDataControllerIntegrationTest.java` |
| 438 | `bl-center/src/main/java/com/company/bl/masterdata/infrastructure/SamplingJdbcRepository.java` |
| 437 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcSpecimenWorkflowSpecimenMutationSupport.java` |
| 437 | `bl-center/src/test/java/com/company/bl/interfaces/SystemManagementRoleAndMenuIntegrationTest.java` |
| 434 | `bl-center/src/main/java/com/company/bl/integration/application/StatisticsService.java` |
| 433 | `bl-center/src/main/java/com/company/bl/interfaces/controller/SpecimenControllerAssembler.java` |
| 432 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcOperationSupportRepository.java` |
| 432 | `bl-center/src/main/java/db/migration/V11LegacyDmSchemaSupport.java` |
| 431 | `bl-center/src/main/java/db/migration/V54__reconcile_workflow_reference_options.java` |
| 429 | `bl-center/src/test/java/com/company/bl/interfaces/M2CollectionAndLabelIntegrationTest.java` |
| 426 | `bl-center/src/main/java/db/migration/V17__reconcile_m1_permission_codes.java` |
| 421 | `bl-center/src/main/java/com/company/bl/masterdata/application/MedicalOrderChargeService.java` |
| 419 | `bl-center/src/main/java/com/company/bl/integration/application/BillingManagementService.java` |
| 413 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcArchiveQueries.java` |
| 412 | `bl-center/src/test/java/com/company/bl/interfaces/ApplicationRegistrationWorkbenchIntegrationTest.java` |
| 411 | `bl-center/src/main/java/com/company/bl/masterdata/interfaces/MedicalOrderController.java` |
| 401 | `bl-center/src/main/java/com/company/bl/system/infrastructure/SystemRoleJdbcRepository.java` |
| 394 | `bl-center/src/main/java/com/company/bl/system/interfaces/SystemManagementController.java` |
| 394 | `bl-center/src/main/java/com/company/bl/notification/infrastructure/NotificationCenterJdbcRepository.java` |
