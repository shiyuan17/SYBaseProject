# 文档导航

`docs/` 目录按“协作规范、工程说明、计划文档、专题资料、治理清单”组织，方便在持续迭代中快速定位。

## 快速入口

- [AGENTS 协作约定](./AGENTS.md)
- [项目目录说明](./guides/PROJECT_DIRECTORY.md)
- [模板定制说明](./guides/TEMPLATE_CUSTOMIZATION.md)
- [本地 GitLab 与测试流程](./guides/GITLAB_LOCAL_TEST_FLOW.md)
- [项目健康治理规则](./rules/PROJECT_HEALTH_RULES.md)
- [文件健康豁免清单](./file-health-exemptions.properties)

## 当前里程碑状态

- M1：系统管理、权限、配置、编号规则已落地
- M2：标本工作流主流程已落地
- M3：技术流程待办、追踪、返工与 QC 历史已接入
- M4：诊断报告、修订、医嘱、会诊闭环已接入
- M5：归档、借阅、试剂与设备台账能力已接入
- M6：临床导入、收费回写、历史报告、统计分析代码与测试已接入，按 `M7` 前置门禁收口

## 目录说明

- `rules/`：工程规则、DDD/API/数据库/Git/CI 等协作规范
- `guides/`：项目结构、初始化、运行与操作说明
- `plans/`：阶段规划、完成说明与治理收口记录
- `database/`：数据库设计草案与辅助 SQL 资料
- `detailed_list/`：业务功能拆解清单

## 维护约定

- 新增规范类文档优先放入 `rules/`
- 新增操作手册、环境说明优先放入 `guides/`
- 新增里程碑计划、收口记录优先放入 `plans/`
- 结构性治理例外必须同步更新 `file-health-exemptions.properties`
