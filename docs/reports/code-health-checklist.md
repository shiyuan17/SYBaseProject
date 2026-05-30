# Repository Code Health Checklist

Generated at `2026-05-31T03:50:46+08:00`.

## Overview

| Area | Status | Conclusion |
| --- | --- | --- |
| File Health | WATCH | Hard limits are still only exceeded by approved large artifacts, and the exemption cap remains fully used. |
| Testability | WATCH | The latest split was not fully re-verified yet because the current branch still has an unrelated compile blocker on missing `com.company.common.web.observability` types. |
| Maintainability | WATCH | Current repository state has 58 Java files over 300 lines and 0 over 500 lines after the line-count script was corrected and the V11/V44 migration slices landed. |
| Naming and Boundaries | PASS | Recent splits continue to use domain-specific query/write/model and schema/mapper names rather than generic helper buckets. |
| Errors and Encoding | PASS | No new silent-exception or implicit-charset hotspots were introduced in the scanned source tree. |

## File Health

| Check Item | Basis | Status | Evidence | Suggested Action |
| --- | --- | --- | --- | --- |
| UTF-8 / BOM / line endings | docs/rules/CODING_RULES.md section 7; common/common-test/.../RepositoryFileHealthGateTest.java | PASS | No new file-health exceptions were introduced in this update, and the repository still relies on the existing static gate for encoding and line endings. | Keep UTF-8 without BOM and LF as the default. |
| File size / line limits / exemptions | docs/rules/CODING_RULES.md section 7; docs/file-health-exemptions.properties; docs/file-health-baseline.properties | WATCH | Active exemptions: 3; exemption baseline: 3; oversized files still covered by approved exemptions: 2. | Keep the exemption cap flat and move generated large artifacts away from hard-limit paths. |

## Testability

| Check Item | Basis | Status | Evidence | Suggested Action |
| --- | --- | --- | --- | --- |
| Fast feedback and gate checks | docs/rules/PROJECT_HEALTH_RULES.md; common/common-test/.../RepositoryFileHealthGateTest.java | WATCH | No fresh Maven verification cleared end to end after the latest split, and `.\mvnw.cmd -pl bl-center -DskipTests compile` still fails on the existing missing `com.company.common.web.observability` classes. | Clear the unrelated compile issue first, then rerun fast feedback and the targeted regression set. |
| Coverage baseline | bl-center/jacoco-baseline.properties; docs/reports/code-health-baseline-20260530.md | PASS | Line baseline: 0.858216; branch baseline: 0.557996. | Keep coverage changes inside the existing baseline-governance flow. |

## Maintainability

| Check Item | Basis | Status | Evidence | Suggested Action |
| --- | --- | --- | --- | --- |
| Structural hotspots | docs/rules/AI_CODE_HEALTH_CORE.md; docs/rules/CODING_RULES.md; docs/reports/largest-files-report.md | WATCH | Java files over 300 lines: 58; Java files over 500 lines: 0. The corrected report script replaced a previously undercounted snapshot, so newer totals are higher but more accurate. | Prioritize splitting large repositories, services, controllers, and oversized historical migrations by query/write and subdomain instead of adding generic helpers. |

### Priority Hotspots

| Lines | File | Why It Matters |
| ---: | --- | --- |
| 438 | `bl-center/src/main/java/com/company/bl/masterdata/infrastructure/SamplingJdbcRepository.java` | Sampling persistence has become the largest active domain repository hotspot after the recent service split. |
| 437 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcSpecimenWorkflowSpecimenMutationSupport.java` | Specimen mutation persistence is now isolated, but it is still large enough to merit another subdomain split. |
| 434 | `bl-center/src/main/java/com/company/bl/integration/application/StatisticsService.java` | Integration-side reporting orchestration is still concentrated in one service. |
| 433 | `bl-center/src/main/java/com/company/bl/interfaces/controller/SpecimenControllerAssembler.java` | Interface-layer mapping remains dense and is now the main controller-side hotspot. |
| 432 | `bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcOperationSupportRepository.java` | Operation support persistence still mixes multiple workflow support paths in one repository. |
| 354 | `bl-center/src/main/java/com/company/bl/application/service/ArchiveWorkflowService.java` | Archive write-side orchestration now sits at the hotspot threshold and is a good candidate for a subdomain split. |

## Naming and Boundaries

| Check Item | Basis | Status | Evidence | Suggested Action |
| --- | --- | --- | --- | --- |
| Generic filenames | docs/rules/AI_CODE_HEALTH_CONTRACTS.md; docs/rules/CODING_RULES.md | PASS | Recent extractions use names such as `AbstractJdbcSpecimenWorkflowSchemaSupport`, `SamplingQuerySupport`, and `SystemUserManagementImportSupport` instead of generic utility buckets. | Keep file and module names anchored in domain language. |
| TODO / FIXME noise | docs/rules/CODING_RULES.md; docs/rules/AI_CODE_HEALTH_CORE.md | PASS | The scan still only matches the domain constant `TODO_TASK`; no stray TODO/FIXME markers were introduced. | Keep domain constants distinct from comment-based follow-up markers. |

## Errors and Encoding

| Check Item | Basis | Status | Evidence | Suggested Action |
| --- | --- | --- | --- | --- |
| Implicit charset conversions | docs/rules/CODING_RULES.md; docs/rules/AI_CODE_HEALTH_CONTRACTS.md | PASS | No new raw `new String(byte[])`, `FileReader`, or `FileWriter` hotspots were added outside the intentional import/export paths already under review. | Keep charsets explicit, especially on import, export, and log-writing paths. |
| Empty catch / silent exception swallowing | docs/rules/CODING_RULES.md; docs/rules/AI_CODE_HEALTH_CORE.md | PASS | No empty catch blocks were found in the scanned source tree. | Keep exceptions structured and propagate them at the right layer. |

## Notes

- This checklist separates the hard file-health gate from structural debt: passing the gate does not mean the repository is debt-free.
- `scripts/ci/generate-largest-files-report.ps1` was corrected to use true file line counts; compare older report snapshots with caution.
- `BlCenterFlywayCli` is no longer a hotspot; command execution and managed-table inventory now live in dedicated Flyway support classes.
- `AbstractJdbcSpecimenWorkflowReadSupport` is no longer a hotspot; schema-probing and row-mapping responsibilities now live in dedicated supports.
- `SpecimenWorkflowModels` is no longer a hotspot; query-side records now live in `SpecimenWorkflowQueryModels`.
- `SpecimenWorkflowQueryService` is no longer a hotspot; query orchestration now fans out into dedicated query supports.
- `V11__reconcile_legacy_dm_schema` is no longer a hotspot facade; the migration now delegates into focused metadata and seed-data supports, and the repository currently has no 500+ Java file left.
- `V44__seed_workflow_reference_options` is no longer a hotspot facade; reference-option seeding now lives in dedicated metadata and seed-data supports.
- `SamplingService` and `SystemUserManagementService` remain below the hotspot threshold after the recent splits.
- The untracked file `linear-setting.json` was intentionally excluded from the conclusions.
