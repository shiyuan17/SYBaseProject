# QUICKSTART.md — 规范最小阅读路径

协作边界以 `docs/AGENTS.md` 为准。本文件只回答：**最少读什么、跑什么**。

## 三层阅读路径

| 层级 | 何时 | 读什么 |
| --- | --- | --- |
| **入口层** | 每次续接 | `AGENTS.md` → `PROJECT_STATE` → `ARCHITECTURE` → 本文件 |
| **任务层** | 动手前 | 下方场景表 + 模块源码（2–4 份） |
| **底座层** | 跨层/共享层/发布前 | `CODING_RULES` + `GIT_RULES` + `DYNAMIC_WORKFLOW_RULES` |

`DECISIONS` / `KNOWN_BUGS` / `TECH_DEBT`：**按任务按需读**，非入口层默认。

绿区任务默认**不写** memory；会话续接优先 agentmemory / 模块 README（见 `AGENTS.md` §8）。

## 场景最小阅读

| 场景信号 | 最少追加阅读 | 最少验证 |
| --- | --- | --- |
| 绿区文案 / 测试-only / 纯文档 | 无 | 对应测试或 `bash scripts/ci/validate-governance.sh` |
| Java / Spring Boot 业务逻辑 | `JAVA_RULES`、模块源码 | `./mvnw -pl <module> -am test` 或更高 |
| DDD 分层 / 领域模型 / 仓储边界 | `DDD_RULES`、`CODING_RULES` | 相关模块测试 + 架构影响说明 |
| REST API / DTO / 错误码 / 分页 | `API_RULES`；涉前端加 `DYNAMIC_WORKFLOW_RULES` | Controller / service 测试；跨仓附前端验证 |
| DB / Flyway / SQL / 种子 / 回滚 | `DB_RULES`、`DYNAMIC_WORKFLOW_RULES` | 迁移/回滚证据 + 相关集成测试 |
| 权限 / 患者 / 报告 / 脱敏 / 审计 | `DYNAMIC_WORKFLOW_RULES` Security | Full Packet + 安全/权限测试 |
| 可观测性 / 日志 / 指标 / 告警 | `OBSERVABILITY_RULES` | 指标/日志验证或目标环境说明 |
| CLI / 批处理 / 运维工具 | `CLI_RULES` | CLI 成功/失败分支验证 |
| Git / MR / worktree | `GIT_RULES` | hook/CI 或对应本地验证 |
| 任务来源 / issue / MR / 工单 | `TASK_INTAKE` | 任务验收与起始信息 |
| 发布 | `RELEASE`、`GIT_RULES`、`GITLAB_CI_RULES` | `./mvnw clean verify` 或发布门禁 |

## 与 AGENTS 三档的关系

| 档位 | 阅读 |
| --- | --- |
| Fast Path | 入口层 |
| Lightweight | 入口层 + 任务层 |
| Full | 入口层 + 任务层 + 底座层 |

## 协作底座（底座层分包）

跨层/红区/发布前至少读：`CODING_RULES` → `GIT_RULES` → `DYNAMIC_WORKFLOW_RULES`。

索引：[README.md](./README.md)
