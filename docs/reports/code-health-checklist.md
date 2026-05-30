# Repository Code Health Checklist

Generated at 2026-05-30T11:28:58Z.

## Overview

| Area | Status | Conclusion |
| --- | --- | --- |
| File Health | WATCH | Hard gate passed, but 2 oversized files still rely on approved exemptions. |
| Testability | PASS | RepositoryFileHealthGateTest and fast feedback both passed; JaCoCo baseline file exists. |
| Maintainability | WATCH | Current repository state has 49 Java files over 300 lines and 7 over 500 lines. |
| Naming and Boundaries | PASS | No generic utility filenames found; TODO/FIXME hits only matched the domain constant TODO_TASK. |
| Errors and Encoding | PASS | No implicit charset conversions or empty catch blocks found. |

## File Health

| Check Item | Basis | Status | Evidence | Suggested Action |
| --- | --- | --- | --- | --- |
| UTF-8 / BOM / line endings | docs/rules/CODING_RULES.md section 7; common/common-test/.../RepositoryFileHealthGateTest.java | PASS | The repository file-health gate passed, so the current tree does not expose encoding or line-ending violations. | Keep UTF-8 without BOM and LF as the default. |
| File size / line limits / exemptions | docs/rules/CODING_RULES.md section 7; docs/file-health-exemptions.properties; docs/file-health-baseline.properties | WATCH | Active exemptions: 3; exemption baseline: 3; oversized files still covered by approved exemptions: 2. | Keep the exemption cap flat and move generated large artifacts away from hard-limit paths. |

## Testability

| Check Item | Basis | Status | Evidence | Suggested Action |
| --- | --- | --- | --- | --- |
| Fast feedback and gate checks | docs/rules/PROJECT_HEALTH_RULES.md; common/common-test/.../RepositoryFileHealthGateTest.java | PASS | Gate passed: True in 2.2s; fast feedback passed: True in 23.363s. | Keep slow excluded from fast feedback and preserve the static gate. |
| Coverage baseline | bl-center/jacoco-baseline.properties; docs/reports/code-health-baseline-20260530.md | PASS | line baseline: 0.858216; branch baseline: 0.557996. | Keep coverage changes inside the existing baseline-governance flow. |

## Maintainability

| Check Item | Basis | Status | Evidence | Suggested Action |
| --- | --- | --- | --- | --- |
| Structural hotspots | docs/rules/AI_CODE_HEALTH_CORE.md; docs/rules/CODING_RULES.md; docs/reports/largest-files-report.md | WATCH | Java files over 300 lines: 49; Java files over 500 lines: 7. | Prioritize breaking up persistence, service, and controller files with heavy line counts. |

### Priority Hotspots

| Lines | File | Why It Matters |
| ---: | --- | --- |
| 913 | bl-center/src/main/java/com/company/bl/integration/infrastructure/M6JdbcRepository.java | Persistence responsibilities are too concentrated |
| 911 | bl-center/src/main/java/com/company/bl/infrastructure/persistence/AbstractJdbcSpecimenWorkflowProjectionSupport.java | Persistence responsibilities are too concentrated |
| 768 | bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcApplicationRegistrationWorkbenchRepository.java | Persistence responsibilities are too concentrated |
| 747 | bl-center/src/main/java/db/migration/V11__reconcile_legacy_dm_schema.java | Structural hotspot |
| 691 | bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcSpecimenWorkflowRepository.java | Persistence responsibilities are too concentrated |

## Naming and Boundaries

| Check Item | Basis | Status | Evidence | Suggested Action |
| --- | --- | --- | --- | --- |
| Generic filenames | docs/rules/AI_CODE_HEALTH_CONTRACTS.md; docs/rules/CODING_RULES.md | PASS | No tracked files matched generic basenames such as utils, common, helper, helpers, tools, tmp, or temp. | Keep file and module names anchored in domain language. |
| TODO / FIXME noise | docs/rules/CODING_RULES.md; docs/rules/AI_CODE_HEALTH_CORE.md | PASS | The scan only matched the domain constant TODO_TASK; no stray TODO/FIXME markers were found. | Keep domain constants distinct from comment-based follow-up markers. |

## Errors and Encoding

| Check Item | Basis | Status | Evidence | Suggested Action |
| --- | --- | --- | --- | --- |
| Implicit charset conversions | docs/rules/CODING_RULES.md; docs/rules/AI_CODE_HEALTH_CONTRACTS.md | PASS | No raw new String(byte[]), FileReader, or FileWriter usage was found in the scanned source tree. | Keep charsets explicit, especially on import, export, and log-writing paths. |
| Empty catch / silent exception swallowing | docs/rules/CODING_RULES.md; docs/rules/AI_CODE_HEALTH_CORE.md | PASS | No empty catch blocks were found by the regex scan. | Keep exceptions structured and propagate them at the right layer. |

## Notes

- This checklist separates the hard file-health gate from structural debt: passing the gate does not mean the repository is debt-free.
- The untracked file linear-setting.json was intentionally excluded from the conclusions.
