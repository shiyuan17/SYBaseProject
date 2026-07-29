# AGENTS.md — AI 与开发协作规范

本文件是 `SYBaseProject` 的协作入口：边界、风险、交付与 Memory 总规则在此；实现细则见 `docs/rules/`。

## 快速命令

本项目为 Maven 多模块（Java 17 / Spring Boot 3），统一使用仓库自带 `./mvnw`（Windows 用 `mvnw.cmd`）。本节只列高频入口，完整标准验证命令以 `rules/CODING_RULES.md` 为准，CI 流水线见 `rules/GITLAB_CI_RULES.md`。

| 用途 | 命令 |
| --- | --- |
| 构建 + 测试 + 质量校验（对齐 CI `verify`） | `./mvnw clean verify` |
| 仅单元测试 | `./mvnw test` |
| 指定模块测试 | `./mvnw -pl <module> test`（如 `-pl bl-center`） |
| 指定模块完整验证 | `./mvnw -pl <module> -am verify` |
| 打包 | `./mvnw -pl <module> -am package` |
| 本地运行某服务 | `./mvnw -pl <module> spring-boot:run` |
| 治理文档验证 | `bash scripts/ci/validate-governance.sh` |

改动逻辑、接口、持久化或配置时，交付前至少在受影响模块运行 `./mvnw -pl <module> -am verify`，并在交付说明中回填结论。治理、记忆或规范变更须额外运行 `bash scripts/ci/validate-governance.sh`。

## 日志读取规则

- 日志：`.logs/backend.log`、`frontend.log`、`build.log`、`test.log`
- `scripts/dev/` 下本地后端启动脚本默认会追加写入 `.logs/backend.log`，并保留控制台输出
- `scripts/dev/` 与 `scripts/prod/` 已按 `windows/`、`unix/`、`config/` 分层；新增脚本优先进入这些子目录，不再回到根目录平铺
- 排查先读最近日志，修改后重跑命令并核对输出

## 一页式执行入口

| 档位 | 适用 | 最低输出 | 最低验证 |
| --- | --- | --- | --- |
| **Fast Path** | 纯文档、只读、测试-only、低风险文案；不改运行时 | 精简任务确认；Workflow 不适用原因 | 对应测试或 `validate-governance` |
| **Lightweight** | 低风险实现；未触发强制修饰器 | 任务确认 + 轻量 Workflow Packet | `CODING_RULES.md` 最小集 |
| **Full** | 中高风险、跨层、权限/数据、生产、红区、跨仓 | 完整任务确认 + 完整 Packet | `DYNAMIC_WORKFLOW_RULES.md` |

权限/患者报告/接口契约/数据库/构建发布/生产问题 → Full；绿区文案或文档 → Fast Path。

**Worktree 完成门槛**：一旦为任务创建独立 `git worktree`，采纳提交必须由主 Agent merge-back 到当前集成分支或任务声明的目标分支；本地目标分支未包含 merge-back 结果前，不得宣称任务完成、不得清理 worktree 或对应分支。merge-back 后在目标分支运行统一验证，再清理 worktree 与已合并分支。

### 规范单一来源矩阵

| 主题 | 唯一来源 |
| --- | --- |
| 协作入口、风险、交付、Memory | `AGENTS.md` |
| 最小阅读路径 | `rules/QUICKSTART.md` |
| Workflow、修饰器、Packet | `rules/DYNAMIC_WORKFLOW_RULES.md` |
| 验证命令、编码基线、文件健康 | `rules/CODING_RULES.md` |
| Java / Spring Boot / Lombok / 日志 | `rules/JAVA_RULES.md` |
| DDD 分层与领域边界 | `rules/DDD_RULES.md` |
| REST API 与接口契约 | `rules/API_RULES.md` |
| 数据库、SQL、迁移、回滚 | `rules/DB_RULES.md` |
| Git、worktree、MR | `rules/GIT_RULES.md` |
| 任务来源与启动 | `rules/TASK_INTAKE.md` |
| 发布 | `rules/RELEASE.md` |

## 强制规则

### 1. 必读顺序

- **三层阅读路径**（详情见 `rules/QUICKSTART.md`）：
  1. **入口层**：本文件 → `PROJECT_STATE.md` → `ARCHITECTURE.md` → `QUICKSTART.md`
  2. **任务层**：场景表 + 2–4 份专项规范 + 模块源码
  3. **底座层**：`CODING_RULES`、`GIT_RULES`、`DYNAMIC_WORKFLOW_RULES`
- **按需阅读**：`DECISIONS`、`KNOWN_BUGS`、`TECH_DEBT` — 仅当任务涉及时读
- 续接任务：`git status` 为脏工作区事实来源，不以 `PROJECT_STATE` 为准
- 红区或 §6 场景：命中专项文档必读

### 2. 规范映射表

| 场景 | 必读 |
| --- | --- |
| 首次进入 / 不确定读什么 | `QUICKSTART.md` |
| 任务来源 / issue / MR / 工单 | `TASK_INTAKE.md` |
| 编码与验证 | `CODING_RULES.md` |
| Java / Spring Boot / 日志 | `JAVA_RULES.md` |
| 领域建模与分层边界 | `DDD_RULES.md` |
| REST API / DTO / 错误码 / 分页 / 前端联调 | `API_RULES.md` |
| 数据库 / SQL / Flyway / 种子 / 回滚 | `DB_RULES.md` |
| Git / MR / worktree | `GIT_RULES.md` |
| 选 Workflow | `DYNAMIC_WORKFLOW_RULES.md` |
| 国产化兼容、替代评估、例外审批 | `XINCHUANG_RULES.md` |
| 监控、指标、告警、可观测性 | `OBSERVABILITY_RULES.md` |
| CLI 应用 / 批处理工具 | `CLI_RULES.md` |
| Memory 更新 | 根目录五类 memory 文件 |
| 发布 | `RELEASE.md` |

### 3. 前端联动

前端为同级 `SYBaseProjectWeb`。涉及接口、字段、菜单、权限、统计口径、业务规则或联调时**必须对照前端消费方式或验证证据**，并在 MR/交付中写明跨仓影响。

### 4. 任务开始模板

**完整模板**：

```markdown
## 任务确认

- 任务目标:
- 影响范围:
- 主 Workflow:
- 强制修饰器:
- 风险等级:
- 成功标准:
- 非目标:
```

**Fast Path**：

```markdown
## 任务确认（Fast Path）

- 任务目标:
- 影响范围:
- 主 Workflow: 不适用（原因）
- 成功标准:
```

规格先行；验收歧义且会改变行为 / 接口契约 / 数据流时先澄清。任务来源模板见 `rules/TASK_INTAKE.md`。

### 5. 文件操作边界

| 区域 | 策略 |
| --- | --- |
| 绿区（应用层、接口层、测试、文档、非敏感配置） | 可直接改 |
| 黄区（领域模型、仓储接口、基础设施实现、共享模块） | 先说明影响 |
| 红区（安全核心、数据库迁移、生产配置、CI/CD 发布脚本） | **人工确认** |

### 6. 必须升级人工确认

数据库表结构 / 索引 / 约束 / 迁移脚本；新中间件、外部依赖或基础设施；未验证的兼容性依赖、驱动、镜像、Agent 或脚本工具；监控端点、告警阈值、采样、指标标签；权限模型、认证机制、数据脱敏；业务规则无法确定；删除数据、回滚生产配置、重写公共接口。

**红区确认协议**：须说明范围、原因、验证、回滚；范围扩大须重新确认。

### 7. 输出与交接

交付含：**变更摘要**、**影响**、**验证结果**、**Workflow Packet**（按档位）、**风险**。

- **Memory 判定**：仅当产生 durable context 时写入交付或 MR；绿区 Fast Path / Lightweight **默认省略**（见 §8）
- **Loop Packet**：仅用户显式要求 loop 时填写；普通任务不填
- Fast Path 文档类：可省略 Workflow Packet
- Full：含 Red Team 四要素 + Memory 判定（有变更时）

禁止以「应该没问题」替代实际验证。未运行某项验证时，必须标注“未验证”并说明原因。

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

### 8. AI Memory Update

五类文件：`PROJECT_STATE`、`TECH_DEBT`、`KNOWN_BUGS`、`DECISIONS`、`ARCHITECTURE`。

**默认不写**：绿区 Fast Path / Lightweight 且无下列触发项时，不更新 memory，交付可省略 Memory 判定。

**必须更新**（任一命中）：

- 阶段/活跃任务/交接重点变化 → `PROJECT_STATE`
- 新 Open 技术债或已解决债务 → `TECH_DEBT`
- 可复现缺陷或已修复 bug → `KNOWN_BUGS`
- 影响后续行为的决策 → `DECISIONS`
- 模块边界/跨仓契约变化 → `ARCHITECTURE`

**交接渠道**：仓内 durable 事实 → 五类 memory；会话/任务续接 → agentmemory 或 `agent-transcripts/`；模块局部约定 → 模块内 `README.md` 或源码注释。

更新规则：

- 按需更新，不写“无变化”流水账。
- `PROJECT_STATE.md` 保持短小；历史验证日志归档到 `docs/reviews/project-state-archive.md`。
- `TECH_DEBT.md`、`KNOWN_BUGS.md`、`DECISIONS.md` 采用台账式追加或更新状态，不删除历史项。
- `ARCHITECTURE.md` 只记录稳定架构事实和边界约束。
- 跨仓事项双向引用前后端路径与验证。

### 9. 语言与提交

默认使用用户当前语言；代码注释遵循模块既有风格，无统一风格时优先中文。Git 提交信息遵循 `rules/GIT_RULES.md` 中的 Conventional Commits。

### 10. 工具与子 Agent

结构性问题优先 codegraph；大型任务可并行 worktree（`GIT_RULES.md` §6）。

多 Agent 协作时，探索 Agent 只读；执行 Agent 在独立 worktree 中处理；主 Agent 负责汇总、审查、merge-back、统一验证与交付，不得把已完成任务只停留在孤立 worktree 分支中。

工具规则不改变本协作规范的约束力：协作边界、风险分区、升级确认、交付与交接要求一律以 `AGENTS.md` 体系与 `rules/` 专项规范为准。

## 推荐实践 / 反例

先读现有实现；最小 diff；列出已验证与未验证项。反例：不读上下文就生成结构、未说明风险改迁移/权限/生产配置、删测试过关、直接覆盖用户已有修改。

## 检查清单

- [ ] 已阅读本文件及相关专项规范
- [ ] 已输出任务确认和关键假设
- [ ] 已识别本次修改属于绿区、黄区还是红区
- [ ] 已按 `GIT_RULES.md` 完成 worktree 决策；如创建 worktree，已完成 merge-back 与统一验证
- [ ] 已检查五类 AI 记忆文件，并按需更新或说明未更新原因
- [ ] 涉及高风险变更时已人工确认
- [ ] 交付内容包含变更摘要、验证结果和风险提示
- [ ] 如需交接，已附带完整交接摘要

## 关联文档

- [PROJECT_STATE.md](../PROJECT_STATE.md)
- [TECH_DEBT.md](../TECH_DEBT.md)
- [KNOWN_BUGS.md](../KNOWN_BUGS.md)
- [DECISIONS.md](../DECISIONS.md)
- [ARCHITECTURE.md](../ARCHITECTURE.md)
- [rules/QUICKSTART.md](./rules/QUICKSTART.md)
- [rules/TASK_INTAKE.md](./rules/TASK_INTAKE.md)
- [rules/AI-CODE-HEALTH.md](./rules/AI-CODE-HEALTH.md)
- [rules/CODING_RULES.md](./rules/CODING_RULES.md)
- [rules/XINCHUANG_RULES.md](./rules/XINCHUANG_RULES.md)
- [rules/OBSERVABILITY_RULES.md](./rules/OBSERVABILITY_RULES.md)
- [rules/JAVA_RULES.md](./rules/JAVA_RULES.md)
- [rules/CLI_RULES.md](./rules/CLI_RULES.md)
- [rules/DDD_RULES.md](./rules/DDD_RULES.md)
- [rules/API_RULES.md](./rules/API_RULES.md)
- [rules/DB_RULES.md](./rules/DB_RULES.md)
- [rules/GIT_RULES.md](./rules/GIT_RULES.md)
- [rules/DYNAMIC_WORKFLOW_RULES.md](./rules/DYNAMIC_WORKFLOW_RULES.md)
- [rules/RELEASE.md](./rules/RELEASE.md)
