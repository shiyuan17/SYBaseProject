# AGENTS.md - AI 与开发协作规范

## 目标与适用范围

本文件定义 AI 智能体与人工开发者在本项目中的协作边界、执行流程、风险升级机制与交付要求。

- 适用对象：Claude、Copilot、Cursor、IDE 内置助手、脚本化代码生成工具、人工开发者。
- 适用范围：需求分析、文档编写、代码实现、测试补充、缺陷修复、发布准备。
- 规范定位：这里只说明“如何协作”，不重复 Java、DDD、API、数据库、Git、发布等细则。

## 强制规则

### 1. 必读顺序

开始任务前，必须按以下顺序读取上下文：

1. `AGENTS.md`
2. `rules/AI-CODE-HEALTH.md`
3. `rules/CODING_RULES.md`
4. `rules/XINCHUANG_RULES.md`
5. `rules/OBSERVABILITY_RULES.md`
6. `rules/JAVA_RULES.md`
7. `rules/CLI_RULES.md`
8. `rules/DDD_RULES.md`
9. `rules/API_RULES.md`
10. `rules/DB_RULES.md`
11. `rules/GIT_RULES.md`
12. `rules/RELEASE.md`
13. 任务涉及模块的说明文档与现有源代码

### 2. 规范映射表

| 场景 | 必读文档 |
|---|---|
| AI 健康度、代码可维护性、文件健康 | `rules/AI-CODE-HEALTH.md` |
| 通用编码与测试基线 | `rules/CODING_RULES.md` |
| 国产化兼容、替代评估、例外审批 | `rules/XINCHUANG_RULES.md` |
| 监控、指标、告警、可观测性 | `rules/OBSERVABILITY_RULES.md` |
| Java 17 / Spring Boot 3 / Lombok / Slf4j + Logback / Picocli | `rules/JAVA_RULES.md` |
| CLI 应用、批处理命令、工具命令 | `rules/CLI_RULES.md` |
| 领域建模与分层边界 | `rules/DDD_RULES.md` |
| REST API 与接口文档 | `rules/API_RULES.md` |
| 数据库、SQL、迁移与回滚 | `rules/DB_RULES.md` |
| 分支、提交、PR、合并协作 | `rules/GIT_RULES.md` |
| 版本、环境、上线与回滚 | `rules/RELEASE.md` |

### 3. 任务开始模板

开始执行前，AI 必须先给出任务确认，至少包含以下内容：

```markdown
## 任务确认
- 任务目标: [对需求的理解]
- 影响范围: [计划修改的文件或模块]
- 依赖检查: [涉及的组件、环境、外部服务]
- 风险等级: [低 / 中 / 高]
- 关键假设: [默认采用的前提]
```

### 4. 文件操作边界

| 区域 | 说明 | 默认策略 |
|---|---|---|
| 绿区 | 应用层、接口层、测试、文档、非敏感配置 | 可以直接修改 |
| 黄区 | 领域模型、仓储接口、基础设施实现、共享组件 | 先说明影响，再修改 |
| 红区 | 安全核心、数据库迁移脚本、生产配置、CI/CD 发布脚本 | 必须人工确认 |

补充约束：

- 修改 `domain`、`repository`、`db/migration`、`application-prod.yml`、`.github/workflows/` 前，必须明确说明风险。
- 不得为了赶进度绕过已有安全校验、审计日志、异常处理链路。
- 不得擅自删除现有测试来“让构建通过”。

### 5. 必须升级人工确认的场景

出现以下任一情况时，必须暂停并请求人工确认：

- 需要修改数据库表结构、索引、约束或迁移脚本。
- 需要引入新的中间件、外部依赖或基础设施组件。
- 需要引入未经验证的兼容性依赖、驱动、镜像、Agent 或脚本工具。
- 需要调整监控端点暴露、告警阈值、采样或抓取策略、指标标签模型。
- 需求描述与现有业务逻辑冲突。
- 需要调整权限模型、认证机制、数据脱敏策略。
- 无法仅从上下文确定业务规则，且不同实现会改变外部行为。
- 需要执行高风险操作，例如删除数据、回滚生产配置、重写公共接口。

### 6. 输出与交接要求

每次交付必须包含：

- 变更摘要：做了什么、为什么这样做。
- 影响说明：是否涉及配置、数据、接口、兼容性。
- 验证结果：已执行的测试、检查或未验证项。
- 风险提示：需要人工继续跟进的事项。

任务交接时必须提供：

```markdown
## 交接摘要
- 已完成
- 进行中
- 待处理
- 关键决策:
- 已知风险:
- 建议下一步:
```

### 7. 语言与提交约束

- 与用户沟通：默认使用用户当前语言。
- 代码注释：遵循模块既有风格，无统一风格时优先中文。
- Git 提交信息：遵循 `rules/GIT_RULES.md` 中的 Conventional Commits。
- 发布说明：遵循 `rules/RELEASE.md` 的版本与变更说明要求。

## 推荐实践

- 先读现有实现，再决定新增还是复用。
- 优先做最小可验证改动，避免一轮内同时改架构、接口和持久化。
- 明确默认假设，例如“当前以 DDD 四层结构为基线”。
- 交付时显式列出“已验证”和“未验证”项，降低协作不确定性。
- 给下一位执行者完整上下文，而不是只给结论。

## 反例 / 禁用项

- 不读上下文就直接生成整套实现。
- 在 `domain` 中直接调用外部 SDK、Mapper、HTTP 客户端。
- 在未说明风险的情况下修改数据库迁移脚本或生产配置。
- 用“为了尽快完成任务”为理由跳过测试、审查、自检。
- 直接覆盖用户已有修改，或把不理解的代码整体删除重写。

## 检查清单

- [ ] 已阅读本文件及相关专项规范
- [ ] 已输出任务确认和关键假设
- [ ] 已识别本次修改属于绿区、黄区还是红区
- [ ] 涉及高风险变更时已人工确认
- [ ] 交付内容包含变更摘要、验证结果和风险提示
- [ ] 如需交接，已附带完整交接摘要

## 关联文档

- [AI-CODE-HEALTH.md](./rules/AI-CODE-HEALTH.md)
- [CODING_RULES.md](./rules/CODING_RULES.md)
- [XINCHUANG_RULES.md](./rules/XINCHUANG_RULES.md)
- [OBSERVABILITY_RULES.md](./rules/OBSERVABILITY_RULES.md)
- [JAVA_RULES.md](./rules/JAVA_RULES.md)
- [CLI_RULES.md](./rules/CLI_RULES.md)
- [DDD_RULES.md](./rules/DDD_RULES.md)
- [API_RULES.md](./rules/API_RULES.md)
- [DB_RULES.md](./rules/DB_RULES.md)
- [GIT_RULES.md](./rules/GIT_RULES.md)
- [RELEASE.md](./rules/RELEASE.md)
