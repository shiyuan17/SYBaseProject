# AGENTS.md - AI 与开发协作规范

## 目标与适用范围

本文件定义 AI 智能体与人工开发者在本项目中的协作边界、执行流程、风险升级机制与交付要求。

- 适用对象：Claude、Copilot、Cursor、IDE 内置助手、脚本化代码生成工具、人工开发者。
- 适用范围：需求分析、文档编写、代码实现、测试补充、缺陷修复、发布准备。
- 规范定位：这里只说明“如何协作”，不重复 Java、DDD、API、数据库、Git、发布等细则。

## 快速命令

本项目为 Maven 多模块（Java 17 / Spring Boot 3），统一使用仓库自带 `./mvnw`（Windows 用 `mvnw.cmd`）。常用命令如下，完整验证策略见 `rules/CODING_RULES.md`，CI 流水线见 `rules/GITLAB_CI_RULES.md`：

| 用途 | 命令 |
| --- | --- |
| 构建 + 测试 + 质量校验（对齐 CI `verify`） | `./mvnw clean verify` |
| 仅单元测试 | `./mvnw test` |
| 指定模块测试 | `./mvnw -pl <module> test`（如 `-pl bl-center`） |
| 打包 | `./mvnw -pl <module> -am package` |
| 本地运行某服务 | `./mvnw -pl <module> spring-boot:run` |
| 健康检查 | `curl -f http://127.0.0.1:8080/actuator/health` |

> 改动逻辑、接口、持久化或配置时，交付前至少在受影响模块运行 `./mvnw -pl <module> -am verify`，并在交付说明中回填结论；文件编码与文件健康度校验也是 CI `verify` 门禁项。

## 日志读取规则

- 后端日志固定输出到 `.logs/backend.log`
- 前端开发日志固定输出到 `.logs/frontend.log`
- 构建日志固定输出到 `.logs/build.log`
- 测试日志固定输出到 `.logs/test.log`
- `scripts/dev/` 下本地后端启动脚本默认会追加写入 `.logs/backend.log`，并保留控制台输出
- AI 排查问题时，必须先读取 `.logs/` 下最近日志，再修改代码
- 修改后必须重新运行对应命令，并把新错误继续写入日志

## 强制规则

### 1. 必读顺序

阅读采用分级策略，避免在小改动上空转，同时保证关键改动有足够上下文：

- **始终必读**：`AGENTS.md` 本文件，以及下方「2. 规范映射表」中本次任务场景命中的文档；Linear 任务还须先填写 `rules/LINEAR_TASK.md`。
- **日常任务（绿区小改动）**：读 `AGENTS.md` + 映射表命中的专项文档即可开工。
- **首次进入项目 / 中大型改动 / 跨层（领域 + 接口 + 持久化 + 基础设施等）改动**：按以下顺序一次性通读全部规范，建立完整上下文：
- **续接历史任务 / 接手脏工作区**：先读根目录 `PROJECT_STATE.md`、`ARCHITECTURE.md`，再按需读 `DECISIONS.md`、`KNOWN_BUGS.md`、`TECH_DEBT.md`，并结合 `git status` 与任务相关规范恢复上下文。

1. `AGENTS.md`
2. 若任务来源于 Linear issue，先阅读并填写 `rules/LINEAR_TASK.md`
3. `rules/AI-CODE-HEALTH.md`
4. `rules/CODING_RULES.md`
5. `rules/XINCHUANG_RULES.md`
6. `rules/OBSERVABILITY_RULES.md`
7. `rules/JAVA_RULES.md`
8. `rules/CLI_RULES.md`
9. `rules/DDD_RULES.md`
10. `rules/API_RULES.md`
11. `rules/DB_RULES.md`
12. `rules/GIT_RULES.md`
13. `rules/RELEASE.md`
14. 任务涉及模块的说明文档与现有源代码

> 无论走哪一档，涉及红区或「5. 必须升级人工确认的场景」时，相关专项文档均为必读。

### 2. 规范映射表

| 场景 | 必读文档 |
|---|---|
| Linear 任务起始信息、验收标准、实施计划与风险回滚 | `rules/LINEAR_TASK.md`（仅 Linear 任务强制） |
| AI 健康度、代码可维护性、文件健康 | `rules/CODING_RULES.md` 附录（`rules/AI-CODE-HEALTH.md` 为兼容桩，按需） |
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

- 若任务来源于 Linear issue，开始执行前还必须先填写 `rules/LINEAR_TASK.md`，或在回复中完整覆盖其字段。
- 两者关系：`任务确认` 适用于所有任务，`LINEAR_TASK` 仅用于补充 Linear 任务的来源、验收标准、实施计划与回滚信息。
- **规格先行（硬约束）**：验收标准为空、缺失或存在歧义，且不同理解会改变外部行为 / 接口契约 / 数据流时，必须先澄清确认，不得凭推测进入编码（与「5. 必须升级人工确认的场景」中“无法仅从上下文确定业务规则”一致）。对中大型任务，应先与用户对齐验收标准与非目标，再动手实现。

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

- 修改 `domain`、`repository`、`db/migration`、`application-prod.yml`、`.gitlab-ci.yml`、`scripts/ci/` 前，必须明确说明风险。
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

**Memory 判定**：仅 durable context 变更时写入交付或 MR；绿区 Fast Path / Lightweight **默认省略**（见 §7）。

**Loop Packet**：仅用户显式要求 loop 时填写（见 `rules/LOOP_ENGINEERING_RULES.md`）。

执行后验证是强制回路，不得只声称完成：

- 凡涉及逻辑、领域模型、接口、持久化或配置的改动，交付前必须**实际运行**相关验证命令（见「快速命令」与 `rules/CODING_RULES.md` 标准验证命令），并在「验证结果」中粘贴真实结论。
- 验证失败时必须先修复再重新验证，形成「执行 → 验证 → 修复」闭环，未通过不得宣称完成。
- 确实未运行某项验证时，必须在「验证结果」中显式标注为“未验证”并说明原因，不得默认略过。
- 提交后由 GitLab CI `verify` 阶段作为机器门禁兜底（测试 + 文件编码与文件健康度校验，见 `rules/GITLAB_CI_RULES.md`）；本地验证不得依赖 CI 代跑，禁止为通过而临时删除校验。

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

- 续接历史任务前，先读取 `PROJECT_STATE.md`、`ARCHITECTURE.md`，再按需读 `DECISIONS.md`、`KNOWN_BUGS.md`、`TECH_DEBT.md`；会话续接优先 agentmemory `handoff` / `recall` / `session-history` 或 `agent-transcripts/`，并结合当前 `git status` 还原“上次进行到哪里”。

### 7. AI Memory Update

根目录五类记忆文件是仓内长期上下文层，不替代 agentmemory、MR 描述、测试报告或 ADR：

- `PROJECT_STATE.md`：当前阶段、活跃任务、交接重点（保持短小）。
- `TECH_DEBT.md`：技术债台账。
- `KNOWN_BUGS.md`：已知问题台账。
- `DECISIONS.md`：决策日志。
- `ARCHITECTURE.md`：稳定架构快照。

**默认不写**：绿区 Fast Path / Lightweight 且无下列触发项时，不更新 memory，交付可省略 Memory 判定。

**必须更新**（任一命中）：阶段/交接变化 → `PROJECT_STATE`；Open/Resolved 债务 → `TECH_DEBT`；bug → `KNOWN_BUGS`；新决策 → `DECISIONS`；边界/跨仓契约 → `ARCHITECTURE`。

**交接渠道**：durable 事实 → 五类 memory；会话续接 → agentmemory 或 `agent-transcripts/`；模块局部 → 模块 README。

更新规则：

- 按需更新，不写“无变化”流水账。
- `PROJECT_STATE.md` 保持短小；历史验证日志归档到 `docs/reviews/project-state-archive.md`。
- `TECH_DEBT.md`、`KNOWN_BUGS.md`、`DECISIONS.md` 采用台账式追加或更新状态，不删除历史项。
- `ARCHITECTURE.md` 只记录稳定架构事实和边界约束。
- 跨仓事项双向引用前后端路径与验证。
- Full MR 在 memory 有变更时填写 Memory Update Packet 并引用 `TD-*` / `BUG-*` / `DEC-*`。

### 8. 语言与提交约束

- 与用户沟通：默认使用用户当前语言。
- 代码注释：遵循模块既有风格，无统一风格时优先中文。
- Git 提交信息：遵循 `rules/GIT_RULES.md` 中的 Conventional Commits。
- 发布说明：遵循 `rules/RELEASE.md` 的版本与变更说明要求。

### 9. 多 Agent 与子 Agent 协作

针对大型或可并行任务，推荐按“探索 → 规划 → 并行执行 → 汇总核验”组织协作：

- **探索阶段**：优先用只读 / 探索型子 Agent 收集上下文（DDD 分层边界、模块依赖、调用关系、影响面），结构性问题优先走 codegraph，纯文本检索才用 grep；探索 Agent 不得直接改动代码。
- **规划阶段**：由主 Agent 汇总探索结果，输出「3. 任务开始模板」中的任务确认，再拆分子任务。
- **并行执行**：相互独立的子任务（尤其 Linear 任务）应在各自独立 `git worktree` 中进行，互不污染工作区（worktree 规范见 `rules/GIT_RULES.md`）。
- **汇总核验**：子 Agent 产出必须由主 Agent 汇总、去重并完成交付前验证（`./mvnw verify`）后才允许进入主线，不得直接把多个子 Agent 的结果未经核验拼接提交。
- **边界继承**：子 Agent 同样受绿/黄/红区与「5. 必须升级人工确认的场景」约束；涉及红区时一律升级人工确认，不因“由子 Agent 执行”而放宽。

### 10. 与工具规则的关系

- 仓库内 `.cursor/rules/*`（如 `codegraph.mdc`）与 `.codegraph/` 索引等属于 IDE / AI 工具的执行辅助规则，用于提升检索与编码效率
- 工具规则不改变本协作规范的约束力：协作边界、风险分区、升级确认、交付与交接要求一律以 `AGENTS.md` 体系与 `rules/` 专项规范为准
- 工具规则与协作规范出现冲突时，以协作规范为准

工具选择优先级（提升检索效率、降低上下文消耗）：

- **结构性问题优先 codegraph**：查找符号定义、调用关系（谁调用 / 调用谁）、改动影响面、DDD 分层依赖、签名与上下文，应优先使用 codegraph，而不是先 grep 再逐文件阅读
- **文本性问题才用 grep**：字符串内容、日志文案、注释、配置字面值等纯文本匹配再用 grep / 全文搜索
- **避免重复探索**：codegraph 已是预建索引，不要把它能直接回答的问题再委派给额外的文件阅读子任务
- 索引存在约 1 秒写入延迟，刚改完文件不要立即查询；codegraph 结果用于结构导航，正确性仍以测试（`./mvnw verify`）、编译与门禁为准

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
- [ ] 已检查五类 AI 记忆文件，并按需更新或说明未更新原因
- [ ] 涉及高风险变更时已人工确认
- [ ] 交付内容包含变更摘要、验证结果和风险提示
- [ ] 如需交接，已附带完整交接摘要

## 关联文档

- [LINEAR_TASK.md](./rules/LINEAR_TASK.md)
- [PROJECT_STATE.md](../PROJECT_STATE.md)
- [TECH_DEBT.md](../TECH_DEBT.md)
- [KNOWN_BUGS.md](../KNOWN_BUGS.md)
- [DECISIONS.md](../DECISIONS.md)
- [ARCHITECTURE.md](../ARCHITECTURE.md)
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
