# M1-M5 接口自动化测试报告

## 1. 执行信息

- 报告时间：2026-05-21
- 执行窗口：2026-05-21 03:55:57 至 2026-05-21 03:56:04（Asia/Shanghai）
- 执行目录：`/d/Github/JW/SYBaseProject`
- 执行环境：
  - MINGW64_NT-10.0-22621 3.6.7-fb42d713.x86_64 x86_64
  - java version "17.0.12" 2024-07-16 LTS
  - Maven Wrapper
  - SpringBootTest + MockMvc + H2 `test` profile
- 原始产物：
  - `auth-center/target/surefire-reports`
  - `user-center/target/surefire-reports`
  - `bl-center/target/surefire-reports`

## 2. 执行命令

```powershell
& './mvnw' -B -ntp -pl auth-center -am test "-Dtest=AuthControllerIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
& './mvnw' -B -ntp -pl user-center -am test "-Dtest=UserControllerIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
& './mvnw' -B -ntp -pl bl-center -am test "-Dtest=SystemManagementUserIntegrationTest,SystemManagementRoleAndMenuIntegrationTest,MasterDataControllerIntegrationTest,M1RoleAuthorizationIntegrationTest,M1SingleApiLifecycleIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
& './mvnw' -B -ntp -pl bl-center -am test "-Dtest=ApplicationControllerIntegrationTest,SpecimenWorkflowHappyPathIntegrationTest,SpecimenWorkflowClosureIntegrationTest,M2RoleAuthorizationIntegrationTest,M2RoleScenarioIntegrationTest,M2CollectionAndLabelIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
& './mvnw' -B -ntp -pl bl-center -am test "-Dtest=TechnicalWorkflowIntegrationTest,TechnicalWorkflowQueryEnhancementIntegrationTest,M3RoleAuthorizationMatrixIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
& './mvnw' -B -ntp -pl bl-center -am test "-Dtest=DiagnosticWorkflowIntegrationTest,DiagnosticRevisionIntegrationTest,InternalConsultationIntegrationTest,MedicalOrderIntegrationTest,M4RoleAuthorizationIntegrationTest,M4Batch2AuthorizationIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
& './mvnw' -B -ntp -pl bl-center -am test "-Dtest=ArchiveWorkflowIntegrationTest,ArchiveRoleAuthorizationIntegrationTest,OperationSupportIntegrationTest,M5SingleApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
& './mvnw' -B -ntp -pl bl-center -am test "-Dtest=FlywayTableCoverageTest,LegacyDmFlywayOnboardingTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

## 3. 结果汇总

### 3.1 按模块汇总

| 模块 | 通过 | 失败 | 错误 | 跳过 | 备注 |
| --- | ---: | ---: | ---: | ---: | --- |
| auth-center | 8 | 0 | 0 | 0 | 基础认证能力 |
| user-center | 16 | 0 | 0 | 0 | 基础示例接口与观测性 |
| bl-center（M1-M5 业务） | 88 | 0 | 0 | 0 | 主业务回归 |
| bl-center（迁移前置门禁） | 3 | 0 | 0 | 0 | 不计入业务接口条目数 |
| 合计 | 115 | 0 | 0 | 0 | PASS |

### 3.2 按里程碑/分组汇总

| 分组 | 模块 | 类型 | 通过 | 失败 | 错误 | 跳过 | 结论 |
| --- | --- | --- | ---: | ---: | ---: | ---: | --- |
| AUTH | auth-center | cross-cutting | 8 | 0 | 0 | 0 | PASS |
| USER | user-center | cross-cutting | 16 | 0 | 0 | 0 | PASS |
| M1 | bl-center | single-api / scenario / cross-cutting | 17 | 0 | 0 | 0 | PASS |
| M2 | bl-center | single-api / scenario / cross-cutting | 40 | 0 | 0 | 0 | PASS |
| M3 | bl-center | single-api / scenario / cross-cutting | 9 | 0 | 0 | 0 | PASS |
| M4 | bl-center | single-api / scenario / cross-cutting | 13 | 0 | 0 | 0 | PASS |
| M5 | bl-center | single-api / scenario / cross-cutting | 9 | 0 | 0 | 0 | PASS |
| GATE | bl-center | migration gate | 3 | 0 | 0 | 0 | PASS |

## 4. 覆盖清单

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

## 5. 未覆盖或延期项

- 无。

## 6. 失败用例摘要与根因

- 最终正式回归执行无失败用例。
- 最终结论以本次 Surefire 结果为准。

## 7. 最终结论

- 本轮 M1-M5 接口自动化回归满足既定验收标准。
- 单接口清单已具备自动化用例。
- 每个里程碑至少 1 条主场景自动化通过。
- 补充测试类型已全部落地。
- “未覆盖项”为空。
- 最终结论：PASS。
