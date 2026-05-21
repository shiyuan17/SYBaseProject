#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

REPORT_DATE="${REPORT_DATE:-$(date +%Y%m%d)}"
SKIP_EXECUTION="${SKIP_EXECUTION:-false}"
TZ_VALUE="${TZ:-Asia/Shanghai}"
export TZ="${TZ_VALUE}"

if [[ $# -gt 0 ]]; then
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --report-date)
        REPORT_DATE="$2"
        shift 2
        ;;
      --skip-execution)
        SKIP_EXECUTION="true"
        shift
        ;;
      *)
        echo "Unsupported argument: $1" >&2
        exit 2
        ;;
    esac
  done
fi

cd "${REPO_ROOT}"

MAVEN_CLI_OPTS_VALUE="${MAVEN_CLI_OPTS:--B -ntp}"
# shellcheck disable=SC2206
MAVEN_CLI_OPTS_ARRAY=(${MAVEN_CLI_OPTS_VALUE})

REPORT_DIR="${REPO_ROOT}/docs/reports"
REPORT_PATH="${REPORT_DIR}/m1-m5-api-test-report-${REPORT_DATE}.md"
mkdir -p "${REPORT_DIR}"

RUN_START="$(date '+%Y-%m-%d %H:%M:%S')"

GROUP_DEFINITIONS=(
  "AUTH|auth-center|cross-cutting|cross-cutting|AuthControllerIntegrationTest"
  "USER|user-center|cross-cutting|cross-cutting|UserControllerIntegrationTest"
  "M1|bl-center|M1|single-api / scenario / cross-cutting|SystemManagementUserIntegrationTest,SystemManagementRoleAndMenuIntegrationTest,MasterDataControllerIntegrationTest,M1RoleAuthorizationIntegrationTest,M1SingleApiLifecycleIntegrationTest"
  "M2|bl-center|M2|single-api / scenario / cross-cutting|ApplicationControllerIntegrationTest,SpecimenWorkflowHappyPathIntegrationTest,SpecimenWorkflowClosureIntegrationTest,M2RoleAuthorizationIntegrationTest,M2RoleScenarioIntegrationTest,M2CollectionAndLabelIntegrationTest"
  "M3|bl-center|M3|single-api / scenario / cross-cutting|TechnicalWorkflowIntegrationTest,TechnicalWorkflowQueryEnhancementIntegrationTest,M3RoleAuthorizationMatrixIntegrationTest"
  "M4|bl-center|M4|single-api / scenario / cross-cutting|DiagnosticWorkflowIntegrationTest,DiagnosticRevisionIntegrationTest,InternalConsultationIntegrationTest,MedicalOrderIntegrationTest,M4RoleAuthorizationIntegrationTest,M4Batch2AuthorizationIntegrationTest"
  "M5|bl-center|M5|single-api / scenario / cross-cutting|ArchiveWorkflowIntegrationTest,ArchiveRoleAuthorizationIntegrationTest,OperationSupportIntegrationTest,M5SingleApiIntegrationTest"
  "GATE|bl-center|quality-gate|migration gate|FlywayTableCoverageTest,LegacyDmFlywayOnboardingTest"
)

declare -a GROUP_NAMES
declare -A GROUP_MODULES
declare -A GROUP_MILESTONES
declare -A GROUP_TYPES
declare -A GROUP_TESTS
declare -A GROUP_COMMANDS
declare -A GROUP_RESULTS
declare -A GROUP_TOTALS
declare -A GROUP_FAILURE_CLASSES
declare -A GROUP_STATUS

declare -a COMMAND_LINES
declare -a FAILURES

OVERALL_STATUS="PASS"
HAS_FAILURE="false"

build_test_file_path() {
  local module="$1"
  local class_name="$2"

  case "${module}" in
    auth-center)
      printf '%s/target/surefire-reports/TEST-com.company.auth.interfaces.%s.xml' "${module}" "${class_name}"
      ;;
    user-center)
      printf '%s/target/surefire-reports/TEST-com.company.user.interfaces.%s.xml' "${module}" "${class_name}"
      ;;
    bl-center)
      case "${class_name}" in
        FlywayTableCoverageTest|LegacyDmFlywayOnboardingTest)
          printf '%s/target/surefire-reports/TEST-com.company.bl.infrastructure.migration.%s.xml' "${module}" "${class_name}"
          ;;
        *)
          printf '%s/target/surefire-reports/TEST-com.company.bl.interfaces.%s.xml' "${module}" "${class_name}"
          ;;
      esac
      ;;
    *)
      return 1
      ;;
  esac
}

xml_attr() {
  local file="$1"
  local attr="$2"
  sed -n "s/.*${attr}=\"\\([0-9][0-9]*\\)\".*/\\1/p" "${file}" | head -n 1
}

clean_group_reports() {
  local module="$1"
  local test_csv="$2"
  local class_name
  local xml_file
  local txt_file

  IFS=',' read -r -a test_array <<< "${test_csv}"
  for class_name in "${test_array[@]}"; do
    xml_file="$(build_test_file_path "${module}" "${class_name}")" || continue
    txt_file="${xml_file%.xml}.txt"
    rm -f "${xml_file}" "${txt_file}"
  done
}

summarize_group() {
  local name="$1"
  local module="$2"
  local test_csv="$3"
  local tests=0
  local failures=0
  local errors=0
  local skipped=0
  local missing=0
  local failed_classes=()
  local class_name
  local xml_file
  local class_tests
  local class_failures
  local class_errors
  local class_skipped

  IFS=',' read -r -a test_array <<< "${test_csv}"
  for class_name in "${test_array[@]}"; do
    xml_file="$(build_test_file_path "${module}" "${class_name}")"
    if [[ ! -f "${xml_file}" ]]; then
      missing=$((missing + 1))
      failed_classes+=("${class_name}(missing-report)")
      continue
    fi

    class_tests="$(xml_attr "${xml_file}" "tests")"
    class_failures="$(xml_attr "${xml_file}" "failures")"
    class_errors="$(xml_attr "${xml_file}" "errors")"
    class_skipped="$(xml_attr "${xml_file}" "skipped")"

    class_tests="${class_tests:-0}"
    class_failures="${class_failures:-0}"
    class_errors="${class_errors:-0}"
    class_skipped="${class_skipped:-0}"

    tests=$((tests + class_tests))
    failures=$((failures + class_failures))
    errors=$((errors + class_errors))
    skipped=$((skipped + class_skipped))

    if (( class_failures > 0 || class_errors > 0 )); then
      failed_classes+=("${class_name}")
    fi
  done

  GROUP_TOTALS["${name}"]="${tests},${failures},${errors},${skipped},${missing}"
  GROUP_FAILURE_CLASSES["${name}"]="${failed_classes[*]}"
}

if [[ "${SKIP_EXECUTION}" != "true" ]]; then
  ./mvnw "${MAVEN_CLI_OPTS_ARRAY[@]}" clean
fi

for definition in "${GROUP_DEFINITIONS[@]}"; do
  IFS='|' read -r name module milestone type test_csv <<< "${definition}"

  GROUP_NAMES+=("${name}")
  GROUP_MODULES["${name}"]="${module}"
  GROUP_MILESTONES["${name}"]="${milestone}"
  GROUP_TYPES["${name}"]="${type}"
  GROUP_TESTS["${name}"]="${test_csv}"

  command="./mvnw ${MAVEN_CLI_OPTS_VALUE} -pl ${module} -am test \"-Dtest=${test_csv}\" \"-Dsurefire.failIfNoSpecifiedTests=false\""
  GROUP_COMMANDS["${name}"]="${command}"
  COMMAND_LINES+=("${command}")

  if [[ "${SKIP_EXECUTION}" != "true" ]]; then
    clean_group_reports "${module}" "${test_csv}"
    set +e
    ./mvnw "${MAVEN_CLI_OPTS_ARRAY[@]}" -pl "${module}" -am test "-Dtest=${test_csv}" "-Dsurefire.failIfNoSpecifiedTests=false"
    exit_code=$?
    set -e
  else
    exit_code=0
  fi

  summarize_group "${name}" "${module}" "${test_csv}"
  IFS=',' read -r tests failures errors skipped missing <<< "${GROUP_TOTALS[${name}]}"

  status="PASS"
  if (( exit_code != 0 || failures > 0 || errors > 0 || missing > 0 )); then
    status="FAIL"
    OVERALL_STATUS="FAIL"
    HAS_FAILURE="true"
    FAILURES+=("${name}")
  fi

  GROUP_RESULTS["${name}"]="${exit_code}"
  GROUP_STATUS["${name}"]="${status}"
done

RUN_END="$(date '+%Y-%m-%d %H:%M:%S')"

AUTH_TESTS=0
AUTH_FAILURES=0
AUTH_ERRORS=0
AUTH_SKIPPED=0

USER_TESTS=0
USER_FAILURES=0
USER_ERRORS=0
USER_SKIPPED=0

BL_BUSINESS_TESTS=0
BL_BUSINESS_FAILURES=0
BL_BUSINESS_ERRORS=0
BL_BUSINESS_SKIPPED=0

GATE_TESTS=0
GATE_FAILURES=0
GATE_ERRORS=0
GATE_SKIPPED=0

TOTAL_TESTS=0
TOTAL_FAILURES=0
TOTAL_ERRORS=0
TOTAL_SKIPPED=0

GROUP_SUMMARY_ROWS=""
for name in "${GROUP_NAMES[@]}"; do
  IFS=',' read -r tests failures errors skipped missing <<< "${GROUP_TOTALS[${name}]}"
  module="${GROUP_MODULES[${name}]}"
  milestone="${GROUP_MILESTONES[${name}]}"
  type="${GROUP_TYPES[${name}]}"
  status="${GROUP_STATUS[${name}]}"

  TOTAL_TESTS=$((TOTAL_TESTS + tests))
  TOTAL_FAILURES=$((TOTAL_FAILURES + failures))
  TOTAL_ERRORS=$((TOTAL_ERRORS + errors))
  TOTAL_SKIPPED=$((TOTAL_SKIPPED + skipped))

  case "${name}" in
    AUTH)
      AUTH_TESTS=$((AUTH_TESTS + tests))
      AUTH_FAILURES=$((AUTH_FAILURES + failures))
      AUTH_ERRORS=$((AUTH_ERRORS + errors))
      AUTH_SKIPPED=$((AUTH_SKIPPED + skipped))
      ;;
    USER)
      USER_TESTS=$((USER_TESTS + tests))
      USER_FAILURES=$((USER_FAILURES + failures))
      USER_ERRORS=$((USER_ERRORS + errors))
      USER_SKIPPED=$((USER_SKIPPED + skipped))
      ;;
    GATE)
      GATE_TESTS=$((GATE_TESTS + tests))
      GATE_FAILURES=$((GATE_FAILURES + failures))
      GATE_ERRORS=$((GATE_ERRORS + errors))
      GATE_SKIPPED=$((GATE_SKIPPED + skipped))
      ;;
    *)
      BL_BUSINESS_TESTS=$((BL_BUSINESS_TESTS + tests))
      BL_BUSINESS_FAILURES=$((BL_BUSINESS_FAILURES + failures))
      BL_BUSINESS_ERRORS=$((BL_BUSINESS_ERRORS + errors))
      BL_BUSINESS_SKIPPED=$((BL_BUSINESS_SKIPPED + skipped))
      ;;
  esac

  GROUP_SUMMARY_ROWS="${GROUP_SUMMARY_ROWS}| ${name} | ${module} | ${type} | ${tests} | ${failures} | ${errors} | ${skipped} | ${status} |\n"
done

if [[ "${HAS_FAILURE}" == "true" ]]; then
  FAILURE_SECTION=""
  for name in "${FAILURES[@]}"; do
    failed_classes="${GROUP_FAILURE_CLASSES[${name}]}"
    exit_code="${GROUP_RESULTS[${name}]}"
    if [[ -z "${failed_classes}" ]]; then
      failed_classes="请检查 Surefire XML 与控制台输出"
    fi
    FAILURE_SECTION="${FAILURE_SECTION}- ${name}：退出码 ${exit_code}；失败类/缺失报告：${failed_classes}\n"
  done
  FAILURE_SECTION="${FAILURE_SECTION}- 本轮存在失败，请优先检查对应模块的 Surefire XML、TXT 和 GitLab Job 日志。"
else
  FAILURE_SECTION=$'- 最终正式回归执行无失败用例。\n- 最终结论以本次 Surefire 结果为准。'
fi

read -r -d '' COVERAGE_SECTION <<'EOF' || true
### 4.1 单接口覆盖

- `auth-center`
  - 登录成功/失败、连续失败锁定、禁用用户登录
  - `me`、`access-codes`、登出
  - 缺失 token、无效/过期/撤销 token
- `user-center`
  - 用户创建、按 ID 查询
  - 校验失败、资源不存在
  - `/actuator/health`、`/actuator/prometheus`
  - 响应包装与流式/空响应回归
- `M1`
  - 系统用户、角色、角色授权、菜单/权限
  - 消息主题、统计分类、编号规则、系统配置
  - 部位字典、取材模板、取材规范
  - 医嘱字典分类/条目、收费项目、套餐
  - 已补齐：`system-users/{id}/enabled`、系统配置分类/条目删除、部位启停/删除、取材模板与规范启停/删除、医嘱字典分类/条目 CRUD、收费项目导入导出、套餐启停/删除
- `M2`
  - 申请单创建/查询/追踪
  - 标本登记、固定开始/完成
  - 运送单创建/打印/交接
  - 签收、按条码直收、待办查询、条码追踪、标签补打
  - 已补齐：`specimen-collections` 与标签补打正反路径
- `M3`
  - 技术待办查询、取材开始/完成
  - 脱水篮创建/开始/完成、包埋开始/完成
  - 切片开始/完成、染色开始/完成
  - 返工单创建/执行、技术追踪、超时/增强查询
  - 已补齐：M3 独立权限矩阵
- `M4`
  - 诊断待办查询、分配/接受/开始诊断
  - 病理报告创建、保存草稿、提交、审核、驳回、签发、发布
  - 修订申请创建/审批/驳回、会诊创建/评论/完成
  - 医嘱创建、待办、接单、完成、取消
  - 工作台与报告追踪查询，及关键反向权限断言
- `M5`
  - 档案柜查询/创建/更新、可用柜位查询
  - 申请单、蜡块、玻片归档
  - 档案检索、借阅创建、待归还查询、归还
  - 试剂台账/库存/预警、设备台账/保养记录/预警
  - 已补齐：档案柜查询/更新、借阅待办查询、试剂与设备更新接口

### 4.2 场景接口覆盖

- `M1`
  - 管理员创建用户 -> 分配角色 -> 查询授权 -> 打印登录标签 -> 导出用户
  - 编号规则更新 -> 留痕相关回归
- `M2`
  - 完整收样主链路
  - 条码直收场景
  - 部分签收/无权限场景
- `M3`
  - 技术主链路
  - 单病例多蜡块分流
  - 自动匹配模板
  - 返工重染
  - 超时查询
- `M4`
  - 最小闭环发布
  - 驳回后重提
  - 修订回路
  - 会诊闭环
  - 医嘱闭环
- `M5`
  - 归档 -> 借出 -> 归还 -> 工作台/追踪回流
  - 试剂/设备维护 -> 预警变化

### 4.3 补充测试类型覆盖

- 权限与认证：未登录、无权限、跨角色误用
- 校验与边界：必填缺失、非法请求、资源不存在、参数校验失败
- 状态冲突与幂等：重复接收、重复归档、非法状态迁移、冲突写入
- 导入导出与文件类接口：用户导入导出、收费项目导入导出、运送/标签打印、标签补打
- 观测性与平台烟测：`/actuator/health`、`/actuator/prometheus`、关键响应包装回归
- 数据库迁移前置门禁：`FlywayTableCoverageTest`、`LegacyDmFlywayOnboardingTest`
EOF

read -r -d '' COMMAND_SECTION <<EOF || true
& './mvnw' ${MAVEN_CLI_OPTS_VALUE} -pl auth-center -am test "-Dtest=AuthControllerIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
& './mvnw' ${MAVEN_CLI_OPTS_VALUE} -pl user-center -am test "-Dtest=UserControllerIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
& './mvnw' ${MAVEN_CLI_OPTS_VALUE} -pl bl-center -am test "-Dtest=SystemManagementUserIntegrationTest,SystemManagementRoleAndMenuIntegrationTest,MasterDataControllerIntegrationTest,M1RoleAuthorizationIntegrationTest,M1SingleApiLifecycleIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
& './mvnw' ${MAVEN_CLI_OPTS_VALUE} -pl bl-center -am test "-Dtest=ApplicationControllerIntegrationTest,SpecimenWorkflowHappyPathIntegrationTest,SpecimenWorkflowClosureIntegrationTest,M2RoleAuthorizationIntegrationTest,M2RoleScenarioIntegrationTest,M2CollectionAndLabelIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
& './mvnw' ${MAVEN_CLI_OPTS_VALUE} -pl bl-center -am test "-Dtest=TechnicalWorkflowIntegrationTest,TechnicalWorkflowQueryEnhancementIntegrationTest,M3RoleAuthorizationMatrixIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
& './mvnw' ${MAVEN_CLI_OPTS_VALUE} -pl bl-center -am test "-Dtest=DiagnosticWorkflowIntegrationTest,DiagnosticRevisionIntegrationTest,InternalConsultationIntegrationTest,MedicalOrderIntegrationTest,M4RoleAuthorizationIntegrationTest,M4Batch2AuthorizationIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
& './mvnw' ${MAVEN_CLI_OPTS_VALUE} -pl bl-center -am test "-Dtest=ArchiveWorkflowIntegrationTest,ArchiveRoleAuthorizationIntegrationTest,OperationSupportIntegrationTest,M5SingleApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
& './mvnw' ${MAVEN_CLI_OPTS_VALUE} -pl bl-center -am test "-Dtest=FlywayTableCoverageTest,LegacyDmFlywayOnboardingTest" "-Dsurefire.failIfNoSpecifiedTests=false"
EOF

JAVA_VERSION="$(java -version 2>&1 | head -n 1 | tr -d '\r')"
MAVEN_VERSION="$(./mvnw -version 2>/dev/null | grep -m1 '^Apache Maven ' | tr -d '\r' || true)"
if [[ -z "${MAVEN_VERSION//[[:space:]]/}" ]]; then
  MAVEN_VERSION="Maven Wrapper"
fi
OS_VERSION="$(uname -srm)"

cat > "${REPORT_PATH}" <<EOF
# M1-M5 接口自动化测试报告

## 1. 执行信息

- 报告时间：$(date '+%Y-%m-%d')
- 执行窗口：${RUN_START} 至 ${RUN_END}（${TZ_VALUE}）
- 执行目录：\`${REPO_ROOT}\`
- 执行环境：
  - ${OS_VERSION}
  - ${JAVA_VERSION}
  - ${MAVEN_VERSION}
  - SpringBootTest + MockMvc + H2 \`test\` profile
- 原始产物：
  - \`auth-center/target/surefire-reports\`
  - \`user-center/target/surefire-reports\`
  - \`bl-center/target/surefire-reports\`

## 2. 执行命令

\`\`\`powershell
${COMMAND_SECTION}
\`\`\`

## 3. 结果汇总

### 3.1 按模块汇总

| 模块 | 通过 | 失败 | 错误 | 跳过 | 备注 |
| --- | ---: | ---: | ---: | ---: | --- |
| auth-center | ${AUTH_TESTS} | ${AUTH_FAILURES} | ${AUTH_ERRORS} | ${AUTH_SKIPPED} | 基础认证能力 |
| user-center | ${USER_TESTS} | ${USER_FAILURES} | ${USER_ERRORS} | ${USER_SKIPPED} | 基础示例接口与观测性 |
| bl-center（M1-M5 业务） | ${BL_BUSINESS_TESTS} | ${BL_BUSINESS_FAILURES} | ${BL_BUSINESS_ERRORS} | ${BL_BUSINESS_SKIPPED} | 主业务回归 |
| bl-center（迁移前置门禁） | ${GATE_TESTS} | ${GATE_FAILURES} | ${GATE_ERRORS} | ${GATE_SKIPPED} | 不计入业务接口条目数 |
| 合计 | ${TOTAL_TESTS} | ${TOTAL_FAILURES} | ${TOTAL_ERRORS} | ${TOTAL_SKIPPED} | ${OVERALL_STATUS} |

### 3.2 按里程碑/分组汇总

| 分组 | 模块 | 类型 | 通过 | 失败 | 错误 | 跳过 | 结论 |
| --- | --- | --- | ---: | ---: | ---: | ---: | --- |
$(printf "%b" "${GROUP_SUMMARY_ROWS}")

## 4. 覆盖清单

${COVERAGE_SECTION}

## 5. 未覆盖或延期项

$(if [[ "${OVERALL_STATUS}" == "PASS" ]]; then printf '%s\n' '- 无。'; else printf '%s\n' '- 存在失败组，延期项请结合失败摘要与 Surefire 明细复核。'; fi)

## 6. 失败用例摘要与根因

$(printf "%b" "${FAILURE_SECTION}")

## 7. 最终结论

- 本轮 M1-M5 接口自动化回归$(if [[ "${OVERALL_STATUS}" == "PASS" ]]; then printf '%s' '满足'; else printf '%s' '未满足'; fi)既定验收标准。
- 单接口清单$(if [[ "${OVERALL_STATUS}" == "PASS" ]]; then printf '%s' '已具备'; else printf '%s' '需结合失败项进一步确认'; fi)自动化用例。
- 每个里程碑至少 1 条主场景自动化$(if [[ "${OVERALL_STATUS}" == "PASS" ]]; then printf '%s' '通过'; else printf '%s' '需复核失败组'; fi)。
- 补充测试类型已全部落地。
- “未覆盖项”$(if [[ "${OVERALL_STATUS}" == "PASS" ]]; then printf '%s' '为空'; else printf '%s' '需结合失败情况重新判定'; fi)。
- 最终结论：${OVERALL_STATUS}。
EOF

echo "${REPORT_PATH}"

if [[ "${HAS_FAILURE}" == "true" ]]; then
  exit 1
fi
