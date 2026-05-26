# Phase 1 联调与 Phase 1.5 现场演练验收记录

## 1. 记录目标

- 记录范围：`M5` 前端联调、四类角色试点演练、`M5` 后端测试、`M1-M6` 选择性回归
- 记录日期：2026-05-25
- 不在本次范围：
  - 采购流程
  - 收费业务扩展
  - 真实文件上传
  - Excel 导出
  - 统计报表

## 2. 关联 SOP

- 前端仓库 `D:\Github\JW\SYBaseProjectWeb\docs\phase1_5\护士岗位SOP.md`
- 前端仓库 `D:\Github\JW\SYBaseProjectWeb\docs\phase1_5\技师岗位SOP.md`
- 前端仓库 `D:\Github\JW\SYBaseProjectWeb\docs\phase1_5\医生岗位SOP.md`
- 前端仓库 `D:\Github\JW\SYBaseProjectWeb\docs\phase1_5\管理员岗位SOP.md`
- 现场签到与签字主档 `D:\Github\JW\SYBaseProject\docs\reports\phase1-rehearsal-signoff-20260526.html`

## 3. 现场演练范围

| 角色 | 演练主线 | 关键验收点 | 当前记录 |
| --- | --- | --- | --- |
| 护士 | 申请单、标本登记、固定、转运 | 标签一致、时间完整、交接可追溯 | 使用 `m2.register / m2.fixation / m2.transport` 复演 |
| 技师 | 接收、取材、脱水、包埋、切片、染色 | 工序流转完整、返工闭环、异常上报 | 使用 `m3.grossing / m3.dehydration / m3.embedding / m3.slicing / m3.staining` 复演 |
| 医生 | 分配、工作台、报告、追踪 | 报告闭环、归档回流、借阅状态核对 | 使用 `m4.diagnosis / m4.review / m4.sign` 复演 |
| 管理员 | 账号权限、M5 页面、验收留档 | 菜单正确、权限正确、问题建账 | 使用 `m1.admin / m1.archive / m1.reagent` 复演 |

## 4. M5 前端联调验收记录

| 项目 | 说明 |
| --- | --- |
| 页面范围 | `/operation-support/entry`、`/operation-support/archive`、`/operation-support/reagents`、`/operation-support/equipment` |
| 联调要点 | 菜单映射正确、页面可访问、权限缺失提示明确、归档/借阅/预警数据能落到页面 |
| 现场关注 | 归档柜、归档记录、借阅归还、试剂预警、设备保养预警 |
| 当前结论 | 前端 `typecheck` 与系统管理前端测试均已通过，当前前端阻断项为 `0` |

## 5. Phase 1 后端联调验收记录

### 5.1 M5 后端专项

| 测试组 | 目标 | 当前结论 |
| --- | --- | --- |
| `ArchiveWorkflowIntegrationTest` | 归档、借阅、归还、回流 | 通过 |
| `ArchiveRoleAuthorizationIntegrationTest` | M5 角色权限边界 | 通过 |
| `OperationSupportIntegrationTest` | 试剂、库存、设备、预警 | 通过 |
| `M5SingleApiIntegrationTest` | M5 单接口烟测 | 通过 |

### 5.2 M1-M6 选择性回归

| 里程碑 | 代表测试 | 当前结论 |
| --- | --- | --- |
| `M1` | `SystemManagementUserIntegrationTest`、`SystemManagementRoleAndMenuIntegrationTest`、`M1RoleAuthorizationIntegrationTest`、`M1SingleApiLifecycleIntegrationTest` | 通过 |
| `M2` | `ApplicationControllerIntegrationTest`、`SpecimenWorkflowHappyPathIntegrationTest`、`SpecimenWorkflowClosureIntegrationTest`、`M2RoleAuthorizationIntegrationTest`、`M2RoleScenarioIntegrationTest`、`M2CollectionAndLabelIntegrationTest` | 通过 |
| `M3` | `TechnicalWorkflowIntegrationTest`、`TechnicalWorkflowQueryEnhancementIntegrationTest`、`M3RoleAuthorizationMatrixIntegrationTest` | 通过 |
| `M4` | `DiagnosticWorkflowIntegrationTest`、`DiagnosticRevisionIntegrationTest`、`InternalConsultationIntegrationTest`、`MedicalOrderIntegrationTest`、`M4RoleAuthorizationIntegrationTest`、`M4Batch2AuthorizationIntegrationTest` | 通过 |
| `M5` | `ArchiveWorkflowIntegrationTest`、`ArchiveRoleAuthorizationIntegrationTest`、`OperationSupportIntegrationTest`、`M5SingleApiIntegrationTest` | 通过 |
| `M6` | `M6ClinicalIntegrationTest`、`M6BillingIntegrationTest`、`M6HistoricalReportIntegrationTest`、`M6StatisticsIntegrationTest`、`M6AuthorizationIntegrationTest`、`M6ObservabilityIntegrationTest` | 通过 |
| 门禁 | `FlywayTableCoverageTest`、`LegacyDmFlywayOnboardingTest` | 通过 |

## 6. 问题台账使用要求

- 所有权限缺失、页面跳转异常、接口报错、回流缺失、数据冲突都必须登记问题台账
- 阻断项定义：
  - 无法完成岗位主流程
  - 无法进入关键页面或关键接口全部失败
  - 影响归档/借阅/报告主闭环
- 问题台账模板：`D:\Github\JW\SYBaseProject\docs\reports\phase1-issue-ledger-template-20260525.md`

## 6.1 现场复演与签字留档约定

- 复演账号统一使用内置测试账号，口令由管理员在现场复演前统一预置并发放。
- 复演顺序固定为：管理员预检 -> 护士 -> 技师 -> 医生 -> 管理员复核 M5 页面。
- 签字留档采用双留痕：
  - HTML 主档 `D:\Github\JW\SYBaseProject\docs\reports\phase1-rehearsal-signoff-20260526.html`
  - 现场填写后的截图，或打印签字后拍照/扫描件
- 若现场真实复演中出现异常，统一登记到问题台账并在 HTML 主档“异常摘要”中填写问题编号。

## 7. 验收命令

### 7.1 前端

```powershell
pnpm -F @vben/web-ele run typecheck
pnpm test:system-management:frontend
```

### 7.2 后端

```powershell
.\mvnw.cmd -B -ntp -pl bl-center -am test "-Dtest=ArchiveWorkflowIntegrationTest,ArchiveRoleAuthorizationIntegrationTest,OperationSupportIntegrationTest,M5SingleApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
.\mvnw.cmd -B -ntp -pl bl-center -am test "-Dtest=SystemManagementUserIntegrationTest,SystemManagementRoleAndMenuIntegrationTest,MasterDataControllerIntegrationTest,M1RoleAuthorizationIntegrationTest,M1SingleApiLifecycleIntegrationTest,ApplicationControllerIntegrationTest,SpecimenWorkflowHappyPathIntegrationTest,SpecimenWorkflowClosureIntegrationTest,M2RoleAuthorizationIntegrationTest,M2RoleScenarioIntegrationTest,M2CollectionAndLabelIntegrationTest,TechnicalWorkflowIntegrationTest,TechnicalWorkflowQueryEnhancementIntegrationTest,M3RoleAuthorizationMatrixIntegrationTest,DiagnosticWorkflowIntegrationTest,DiagnosticRevisionIntegrationTest,InternalConsultationIntegrationTest,MedicalOrderIntegrationTest,M4RoleAuthorizationIntegrationTest,M4Batch2AuthorizationIntegrationTest,ArchiveWorkflowIntegrationTest,ArchiveRoleAuthorizationIntegrationTest,OperationSupportIntegrationTest,M5SingleApiIntegrationTest,M6ClinicalIntegrationTest,M6BillingIntegrationTest,M6HistoricalReportIntegrationTest,M6StatisticsIntegrationTest,M6AuthorizationIntegrationTest,M6ObservabilityIntegrationTest,FlywayTableCoverageTest,LegacyDmFlywayOnboardingTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

## 8. 执行结果

| 命令 | 结果 | 摘要 |
| --- | --- | --- |
| `pnpm -F @vben/web-ele run typecheck` | 通过 | 2026-05-26 06:33 至 2026-05-26 06:35（Asia/Shanghai）执行；清理 `menu.test.ts` 未使用导入后，`vue-tsc --noEmit --skipLibCheck` 成功 |
| `pnpm test:system-management:frontend` | 通过 | 2026-05-26 06:35 至 2026-05-26 06:36（Asia/Shanghai）执行；10 个测试文件、34 个测试全部通过，生成前端 HTML 报告与覆盖率，语句覆盖率 `94.4%`，报告目录为 `D:\Github\JW\SYBaseProjectWeb\tests\reports\system-management\frontend` |
| `M5` 后端专项测试 | 通过 | 2026-05-25 23:59 至 2026-05-25 23:59（Asia/Shanghai）执行；`ArchiveWorkflowIntegrationTest`、`ArchiveRoleAuthorizationIntegrationTest`、`OperationSupportIntegrationTest`、`M5SingleApiIntegrationTest` 共 9 个测试全部通过，`BUILD SUCCESS` |
| `M1-M6` 选择性回归 | 通过 | 2026-05-25 23:59 至 2026-05-26 00:00（Asia/Shanghai）执行；共 120 个测试全部通过，覆盖 `M1-M6` 与 `FlywayTableCoverageTest`、`LegacyDmFlywayOnboardingTest`，`BUILD SUCCESS` |

## 8.1 当前现场复演准备状态

| 项目 | 状态 | 说明 |
| --- | --- | --- |
| 四类岗位 SOP | 已完成 | 已落盘到前端仓库 `docs/phase1_5/` |
| 现场签到/签字主档 | 已完成 | 已新增 HTML 主档，待现场填写与截图 |
| 复演账号分组 | 已完成 | 已按护士、技师、医生、管理员四类固化 |
| 真实现场截图/签字件 | 待现场执行 | 需人工组织复演后补回仓库或留路径说明 |

## 9. 阻断问题统计

| 级别 | 数量 | 说明 |
| --- | ---: | --- |
| 阻断问题 | 0 | 当前命令验证已无阻断项 |
| 非阻断问题 | 0 | 本轮命令执行未发现新增非阻断问题 |

## 10. 移交摘要

- 已完成: 四类岗位 SOP、问题台账模板、验收记录、HTML 现场签到/签字主档、前端 `typecheck` 收口与前端测试复跑。
- 进行中: 结合现场账号和病例执行真实复演，并补充截图、签字和最终通过结论。
- 待处理: 现场组织人使用 HTML 主档完成签到、签字和异常留痕回填。
- 关键决策: 本轮只覆盖 `M5` 前端联调和 Phase 1.5 试点演练，不扩展采购、收费、真实文件上传、Excel 导出和统计报表。
- 已知风险: 当前真实“现场签字截图/扫描件”仍未生成，需要人工在复演当天完成最终留档。
- 建议下一步: 组织一次按既定账号分组的现场复演，完成 HTML 主档填写、截图签字，并把最终结果回填到本记录和问题台账。
