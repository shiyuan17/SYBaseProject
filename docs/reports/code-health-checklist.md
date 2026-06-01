# Repository Code Health Checklist

Generated at `2026-05-31T03:37:39Z`.

## Overview

| Area | Status | Conclusion |
| --- | --- | --- |
| File Health | FAIL | RepositoryFileHealthGateTest failed; see the Testability section for command output. |
| Testability | FAIL | At least one verification command failed; the report was still generated with recorded evidence. |
| Maintainability | WATCH | Current repository state has 58 Java files over 300 lines and 0 over 500 lines. |
| Naming and Boundaries | PASS | No generic utility filenames found; TODO/FIXME hits only matched the domain constant TODO_TASK. |
| Errors and Encoding | PASS | No implicit charset conversions or empty catch blocks found. |

## File Health

| Check Item | Basis | Status | Evidence | Suggested Action |
| --- | --- | --- | --- | --- |
| UTF-8 / BOM / line endings | docs/rules/CODING_RULES.md section 7; common/common-test/.../RepositoryFileHealthGateTest.java | FAIL | The repository file-health gate failed. Exit code: 1. Summary: [INFO] <br>[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0<br>[INFO] <br>[INFO] ------------------------------------------------------------------------<br>[INFO] BUILD FAILURE<br>[INFO] ------------------------------------------------------------------------<br>[INFO] Total time:  2.570 s<br>[INFO] Finished at: 2026-05-31T11:37:34+08:00<br>[INFO] ------------------------------------------------------------------------<br>[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test (default-test) on project common-test: There are test failures.<br>[ERROR] <br>[ERROR] Please refer to D:\Github\JW\SYBaseProject\common\common-test\target\surefire-reports for the individual test results.<br>[ERROR] Please refer to dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.<br>[ERROR] -> [Help 1]<br>[ERROR] <br>[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.<br>[ERROR] Re-run Maven using the -X switch to enable full debug logging.<br>[ERROR] <br>[ERROR] For more information about the errors and possible solutions, please read the following articles:<br>[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException | Keep UTF-8 without BOM and LF as the default. |
| File size / line limits / exemptions | docs/rules/CODING_RULES.md section 7; docs/file-health-exemptions.properties; docs/file-health-baseline.properties | WATCH | Active exemptions: 3; exemption baseline: 3; oversized files still covered by approved exemptions: 5. | Keep the exemption cap flat and move generated large artifacts away from hard-limit paths. |

## Testability

| Check Item | Basis | Status | Evidence | Suggested Action |
| --- | --- | --- | --- | --- |
| Fast feedback and gate checks | docs/rules/PROJECT_HEALTH_RULES.md; common/common-test/.../RepositoryFileHealthGateTest.java | FAIL | Gate status: FAIL in 3.853s (exit 1); fast feedback status: FAIL in 4.368s (exit 1). Gate summary: [INFO] <br>[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0<br>[INFO] <br>[INFO] ------------------------------------------------------------------------<br>[INFO] BUILD FAILURE<br>[INFO] ------------------------------------------------------------------------<br>[INFO] Total time:  2.570 s<br>[INFO] Finished at: 2026-05-31T11:37:34+08:00<br>[INFO] ------------------------------------------------------------------------<br>[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test (default-test) on project common-test: There are test failures.<br>[ERROR] <br>[ERROR] Please refer to D:\Github\JW\SYBaseProject\common\common-test\target\surefire-reports for the individual test results.<br>[ERROR] Please refer to dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.<br>[ERROR] -> [Help 1]<br>[ERROR] <br>[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.<br>[ERROR] Re-run Maven using the -X switch to enable full debug logging.<br>[ERROR] <br>[ERROR] For more information about the errors and possible solutions, please read the following articles:<br>[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException. Fast summary: [INFO] ------------------------------------------------------------------------<br>[INFO] BUILD FAILURE<br>[INFO] ------------------------------------------------------------------------<br>[INFO] Total time:  3.125 s<br>[INFO] Finished at: 2026-05-31T11:37:39+08:00<br>[INFO] ------------------------------------------------------------------------<br>[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test (default-test) on project common-test: There are test failures.<br>[ERROR] <br>[ERROR] Please refer to D:\Github\JW\SYBaseProject\common\common-test\target\surefire-reports for the individual test results.<br>[ERROR] Please refer to dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.<br>[ERROR] -> [Help 1]<br>[ERROR] <br>[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.<br>[ERROR] Re-run Maven using the -X switch to enable full debug logging.<br>[ERROR] <br>[ERROR] For more information about the errors and possible solutions, please read the following articles:<br>[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException<br>[ERROR] <br>[ERROR] After correcting the problems, you can resume the build with the command<br>[ERROR]   mvn <args> -rf :common-test. | Keep slow excluded from fast feedback and preserve the static gate. |
| Coverage baseline | bl-center/jacoco-baseline.properties; docs/reports/code-health-baseline-20260530.md | PASS | line baseline: 0.858216; branch baseline: 0.557996. | Keep coverage changes inside the existing baseline-governance flow. |

## Maintainability

| Check Item | Basis | Status | Evidence | Suggested Action |
| --- | --- | --- | --- | --- |
| Structural hotspots | docs/rules/AI_CODE_HEALTH_CORE.md; docs/rules/CODING_RULES.md; docs/reports/largest-files-report.md | WATCH | Java files over 300 lines: 58; Java files over 500 lines: 0. | Prioritize breaking up persistence, service, and controller files with heavy line counts. |

### Priority Hotspots

| Lines | File | Why It Matters |
| ---: | --- | --- |
| 473 | bl-center/src/test/java/com/company/bl/interfaces/SpecimenWorkflowClosureIntegrationTest.java | Structural hotspot |
| 468 | bl-center/src/test/java/com/company/bl/interfaces/MasterDataControllerIntegrationTest.java | Interface-layer mapping is too heavy |
| 438 | bl-center/src/main/java/com/company/bl/masterdata/infrastructure/SamplingJdbcRepository.java | Persistence responsibilities are too concentrated |
| 437 | bl-center/src/test/java/com/company/bl/interfaces/SystemManagementRoleAndMenuIntegrationTest.java | Structural hotspot |
| 437 | bl-center/src/main/java/com/company/bl/infrastructure/persistence/JdbcSpecimenWorkflowSpecimenMutationSupport.java | Persistence responsibilities are too concentrated |

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
- Verification commands were supplied by the caller rather than re-executed inside this script.
- Untracked files were present during report generation: bl-center/src/test/java/com/company/bl/infrastructure/persistence/, docs/reports/code-health-report-20260531.html, docs/reports/code-health-report-latest.html, docs/rules/AI-CODE-HEALTH.md, linear-setting.json.
