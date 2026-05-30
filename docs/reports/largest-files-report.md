# Largest Files Report

Generated at `2026-05-30T09:10:28Z`.

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
| 337 | 300 | `docs/plans/病理全流程系统里程碑规划.md` |

## Java Files Over 300 Lines

Count: `62`

| Lines | File |
| ---: | --- |
| 975 | `bl-center/src/main/java/com/company/bl/integration/infrastructure/M6JdbcRepository.java` |
| 956 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcSpecimenWorkflowRepository.java` |
| 952 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/AbstractJdbcSpecimenWorkflowProjectionSupport.java` |
| 802 | `bl-center/src/main/java/db/migration/V11__reconcile_legacy_dm_schema.java` |
| 797 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcApplicationRegistrationWorkbenchRepository.java` |
| 630 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcDiagnosticReportRepository.java` |
| 588 | `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowSupport.java` |
| 557 | `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowModels.java` |
| 528 | `bl-center/src/main/java/com/company/bl/tools/BlCenterFlywayCli.java` |
| 527 | `bl-center/src/main/java/com/company/bl/interfaces/controller/SpecimenController.java` |
| 518 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/AbstractJdbcSpecimenWorkflowReadSupport.java` |
| 506 | `bl-center/src/main/java/com/company/bl/masterdata/application/SamplingService.java` |
| 486 | `bl-center/src/main/java/com/company/bl/masterdata/application/MedicalOrderService.java` |
| 481 | `bl-center/src/test/java/com/company/bl/interfaces/SpecimenWorkflowClosureIntegrationTest.java` |
| 480 | `bl-center/src/main/java/db/migration/V44__seed_workflow_reference_options.java` |
| 460 | `bl-center/src/main/java/com/company/bl/system/application/SystemUserManagementService.java` |
| 450 | `bl-center/src/test/java/com/company/bl/interfaces/MasterDataControllerIntegrationTest.java` |
| 438 | `bl-center/src/main/java/com/company/bl/masterdata/infrastructure/SamplingJdbcRepository.java` |
| 437 | `bl-center/src/test/java/com/company/bl/interfaces/SystemManagementRoleAndMenuIntegrationTest.java` |
| 432 | `bl-center/src/main/java/com/company/bl/integration/application/StatisticsService.java` |
| 432 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcOperationSupportRepository.java` |
| 431 | `bl-center/src/main/java/db/migration/V54__reconcile_workflow_reference_options.java` |
| 427 | `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowQueryService.java` |
| 426 | `bl-center/src/main/java/db/migration/V17__reconcile_m1_permission_codes.java` |
| 417 | `bl-center/src/main/java/com/company/bl/integration/application/BillingManagementService.java` |
| 413 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcArchiveQueries.java` |
| 412 | `bl-center/src/test/java/com/company/bl/interfaces/ApplicationRegistrationWorkbenchIntegrationTest.java` |
| 411 | `bl-center/src/main/java/com/company/bl/masterdata/interfaces/MedicalOrderController.java` |
| 401 | `bl-center/src/main/java/com/company/bl/system/infrastructure/SystemRoleJdbcRepository.java` |
| 394 | `bl-center/src/main/java/com/company/bl/system/interfaces/SystemManagementController.java` |

## Java Files Over 500 Lines

Count: `12`

| Lines | File |
| ---: | --- |
| 975 | `bl-center/src/main/java/com/company/bl/integration/infrastructure/M6JdbcRepository.java` |
| 956 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcSpecimenWorkflowRepository.java` |
| 952 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/AbstractJdbcSpecimenWorkflowProjectionSupport.java` |
| 802 | `bl-center/src/main/java/db/migration/V11__reconcile_legacy_dm_schema.java` |
| 797 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcApplicationRegistrationWorkbenchRepository.java` |
| 630 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcDiagnosticReportRepository.java` |
| 588 | `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowSupport.java` |
| 557 | `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowModels.java` |
| 528 | `bl-center/src/main/java/com/company/bl/tools/BlCenterFlywayCli.java` |
| 527 | `bl-center/src/main/java/com/company/bl/interfaces/controller/SpecimenController.java` |
| 518 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/AbstractJdbcSpecimenWorkflowReadSupport.java` |
| 506 | `bl-center/src/main/java/com/company/bl/masterdata/application/SamplingService.java` |

## Top 20 Java Files

| Lines | File |
| ---: | --- |
| 975 | `bl-center/src/main/java/com/company/bl/integration/infrastructure/M6JdbcRepository.java` |
| 956 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcSpecimenWorkflowRepository.java` |
| 952 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/AbstractJdbcSpecimenWorkflowProjectionSupport.java` |
| 802 | `bl-center/src/main/java/db/migration/V11__reconcile_legacy_dm_schema.java` |
| 797 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcApplicationRegistrationWorkbenchRepository.java` |
| 630 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcDiagnosticReportRepository.java` |
| 588 | `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowSupport.java` |
| 557 | `bl-center/src/main/java/com/company/bl/application/service/SpecimenWorkflowModels.java` |
| 528 | `bl-center/src/main/java/com/company/bl/tools/BlCenterFlywayCli.java` |
| 527 | `bl-center/src/main/java/com/company/bl/interfaces/controller/SpecimenController.java` |
| 518 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/AbstractJdbcSpecimenWorkflowReadSupport.java` |
| 506 | `bl-center/src/main/java/com/company/bl/masterdata/application/SamplingService.java` |
| 486 | `bl-center/src/main/java/com/company/bl/masterdata/application/MedicalOrderService.java` |
| 481 | `bl-center/src/test/java/com/company/bl/interfaces/SpecimenWorkflowClosureIntegrationTest.java` |
| 480 | `bl-center/src/main/java/db/migration/V44__seed_workflow_reference_options.java` |
| 460 | `bl-center/src/main/java/com/company/bl/system/application/SystemUserManagementService.java` |
| 450 | `bl-center/src/test/java/com/company/bl/interfaces/MasterDataControllerIntegrationTest.java` |
| 438 | `bl-center/src/main/java/com/company/bl/masterdata/infrastructure/SamplingJdbcRepository.java` |
| 437 | `bl-center/src/test/java/com/company/bl/interfaces/SystemManagementRoleAndMenuIntegrationTest.java` |
| 432 | `bl-center/src/main/java/com/company/bl/integration/application/StatisticsService.java` |
