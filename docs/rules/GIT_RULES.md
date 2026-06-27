# GIT_RULES.md — Git 协作与分支规范

## 目标与适用范围

本文件定义项目的 Git 分支模型、提交规范、MR 规则与合并流程。

- 适用对象：所有开发者、审查者、发布负责人、AI 助手
- 适用范围：日常开发、缺陷修复、发布分支、热修复分支、文档分支
- 规范定位：以 `Git Flow` 作为默认协作模型，支撑 `RELEASE.md` 的发布流程

## 强制规则

### 1. 分支模型

默认分支模型：

- `main`：生产稳定分支
- `develop`：日常集成分支
- `feature/*`：功能开发
- `fix/*`：非线上紧急缺陷修复
- `hotfix/*`：生产紧急修复
- `release/*`：发布准备
- `docs/*`：文档更新
- `refactor/*`：重构治理

### 2. 分支命名

推荐格式：

- `feature/ENG-123-user-authentication`
- `fix/ENG-456-order-status`
- `hotfix/ENG-789-payment-timeout`
- `release/1.2.0`
- `docs/refresh-engineering-rules`
- `refactor/split-user-application-service`

要求：

- 使用英文小写短语，单词间用 `-`
- 工单前缀以任务来源系统为准，示例 `ENG-123`；任务起始信息见 `TASK_INTAKE.md`
- 如有工单号，必须带上工单号
- 一个分支只承载一类目标，不得混合多项无关改动

### 3. 提交规范

- 提交信息统一遵循 Conventional Commits
- 推荐类型：`feat`、`fix`、`refactor`、`docs`、`test`、`build`、`chore`
- 建议格式：`type(scope): subject`

示例：

- `feat(user): add user registration flow`
- `fix(order): handle duplicated payment callback`
- `docs(rules): split release and git conventions`

### 4. MR 与评审规则

- 禁止直接向 `main`、`develop`、`release/*` 推送
- 所有变更必须通过 MR 合入
- MR 必须包含变更目的、影响范围、验证方式、风险说明
- 涉及公共接口、数据库、权限、发布流程的变更必须重点标注
- 涉及依赖、基础设施、数据库、镜像的变更，MR 必须附带信创兼容说明或引用验证记录

默认门禁：

- `main`：至少 2 人 Review，CI 全绿
- `develop`：至少 1 人 Review，CI 全绿
- `release/*`：至少 2 人 Review，仅允许发布相关修复

### 5. 合并规则

- 默认使用非快进合并或团队指定策略，保留分支上下文
- 合并前必须解决冲突并重新验证
- `hotfix/*` 合入 `main` 后必须回合并到 `develop`
- `release/*` 完成后必须同步回 `develop`

### 6. 工作树（Worktree）与任务隔离

- **实现类任务按风险决定是否使用独立 `git worktree`**。命中并行开发、脏工作区、跨层/红区、持续多提交、跨仓验证或下方“必须使用 worktree”条件时，一个任务项对应一个 worktree + 一个分支；低风险单点任务可使用本节例外，避免把小改变成环境管理任务
- 工作树目录统一放在主仓库的同级目录 `../SYBaseProject-worktrees/<task-id>`，不得置于仓库内部，防止被构建产物（`target/`）或脚本影响
- 分支命名仍遵循「2. 分支命名」，从 `develop` 切出，例如 `feature/ENG-123-user-authentication`
- 每个 worktree 是独立工作区，Maven 本地仓库 `~/.m2` 默认共享、无需重复下载依赖，但构建产物 `target/` 与验证（`./mvnw verify`，见 `CODING_RULES.md`）均在对应 worktree 内独立执行
- 任务来源可以是 issue、工单、卡片、MR、文档需求或会话交接；是否创建 worktree 由任务风险和当前工作区状态决定，而不是由平台名称决定

以下场景**必须**使用独立 worktree，不得套用例外：

- 运行时代码、共享契约、接口、数据库、迁移、构建/脚本、依赖、锁文件或记忆文件与实现代码一起改动
- 多人 / 多 Agent 并行推进，或当前主工作区本身已脏且无法明确隔离归属
- 需要构建、联调、跨仓验证、目标环境验证，或预期会持续多次提交

低风险例外（可不新建 worktree）：

- 纯文档、规范审计、评审结论整理、只读分析类任务
- 单一绿区小改、测试-only、低风险静态文案或局部脚本说明调整，且满足：当前分支/工作区干净或可明确隔离归属、无并行实现任务、无依赖/构建/共享契约改动

使用低风险例外时，必须同时满足：

- 在任务确认或交付说明中用一句话写明“为什么这次不需要独立 worktree”
- 先执行 `git status --short`，确认不会混入无关改动
- 一旦任务范围扩大到上述“必须使用 worktree”的条件，立即切回独立 worktree

#### 6.1 Merge-Back 完成定义

- worktree 中的采纳提交，必须由主 Agent 合并回当前集成分支或任务声明的目标分支
- **本地目标分支已经包含 merge-back 结果**，任务才算完成
- 合并回目标分支前，不得宣称任务已交付完成
- 合并回目标分支前，不得删除 worktree，不得删除对应分支
- 清理 worktree 与分支发生在 merge-back 完成并完成统一验证之后

#### 6.2 主 Agent 责任

- 审查各 worktree 的 diff 与提交边界
- 决定采纳 / 驳回 / 返工
- 处理冲突并排除无关改动
- 将采纳的提交合并回当前集成分支或任务目标分支
- 在目标分支运行统一验证
- 在交付说明中记录 merge-back 结果、剩余风险和清理状态

#### 6.3 暂停条件

- 目标分支已脏且无法安全合并
- worktree 分支与目标分支存在冲突，且无法在当前任务范围内解决
- merge-back 需要覆盖他人未确认改动

命中暂停条件时，先停止清理和交付，说明影响范围、冲突点、验证现状与建议处理方式。

#### 6.4 多 Agent 并行协作

- 多个子 Agent 并行实现时，必须为每个实现切片使用独立 worktree 和独立分支；写作用域应在分配任务时明确到文件、模块或验收点
- 只读探索、只读审查或 Checker 任务可以不新建 worktree，但不得改文件、暂存、提交或运行会改写仓库文件的命令
- 主 Agent 负责最终 merge-back：检查各 worktree 的 diff、处理冲突、排除无关改动、将采纳的提交合并回当前集成分支或任务目标分支、运行统一验证并完成交付说明；不得把已完成任务只停留在孤立 worktree 分支中
- 若子 Agent 产出与主线已有改动冲突，先由主 Agent 判定采纳 / 驳回 / 返工，不得让子 Agent 直接覆盖主线或他人改动

标准操作：

```bash
# 1. 同步 develop 并为任务创建 worktree + 分支
git fetch origin
git worktree add -b feature/ENG-123-user-authentication ../SYBaseProject-worktrees/ENG-123 origin/develop

# 2. 进入 worktree 执行验证（Maven 依赖共享 ~/.m2，无需额外安装）
cd ../SYBaseProject-worktrees/ENG-123
./mvnw -pl <module> -am verify

# 3. 查看当前所有工作树
git worktree list

# 4. 在主工作区或目标分支执行 merge-back 与统一验证
git switch develop
git merge --no-ff feature/ENG-123-user-authentication
./mvnw -pl <module> -am verify

# 5. merge-back 完成后再清理 worktree 与分支
git worktree remove ../SYBaseProject-worktrees/ENG-123
git branch -d feature/ENG-123-user-authentication
```

### 7. Git Hooks 与动态 MR Workflow 剧本

仓库通过 `scripts/hooks/` 提供版本化的原生 Git hook 脚本，开发者需在本地执行安装脚本接入 `.git/hooks`：

- `pre-commit`：检查暂存文本文件，拦截 UTF-8 解码、BOM 与换行符问题；全仓文件健康度仍由 `RepositoryFileHealthGateTest` 与 CI 兜底
- `commit-msg`：校验 Conventional Commits，不符合 `type(scope): subject` 的提交信息直接拦截
- `pre-push`：执行 `./mvnw -B -ntp clean test -Dsurefire.excludedGroups=slow`，对齐 GitLab CI `verify_fast` 的快速反馈口径

MR 必须根据任务类型填写 Workflow Packet，并参考 `docs/rules/DYNAMIC_WORKFLOW_RULES.md` 选择专家 Agent、动态测试、动态模拟、安全/数据库修饰器与红队对抗点。不得所有任务套用同一套 AI 流程：

- 接口任务：启用 API Contract Agent，检查 REST 契约、错误码、兼容性、前后端字段映射和失败响应
- 数据库任务：启用 DB/Migration Agent，检查迁移、回滚、索引/约束、种子数据、兼容查询和幂等
- 权限、患者信息、报告信息：叠加 Security 修饰器，检查认证授权、数据范围、脱敏、审计和敏感日志
- 大文件重构、DDD 边界、共享模块：启用 Architecture Agent，检查分层依赖、领域模型、仓储契约和测试面
- 生产问题：启用 Execution Driven Debug，必须先读日志、复现问题、建立反馈环、确认修复证据与回滚路径
- 高风险变更：叠加 Red Team，主动攻击代码并证明是否存在越权、数据破坏、错误吞噬、迁移失败或回滚缺口

MR 合入前还必须填写 Memory Update Packet：

- AI / 开发者交付前按需更新根目录 `PROJECT_STATE.md`、`TECH_DEBT.md`、`KNOWN_BUGS.md`、`DECISIONS.md`、`ARCHITECTURE.md`
- MR 必须说明已更新文件、未更新文件及原因、相关记忆项 ID、跨仓引用和剩余风险
- CI 与 hook 只负责机器门禁；动态 Workflow 和 AI Memory Update 负责任务级治理与长期上下文维护
- 跨仓事项必须双向引用前端 `SYBaseProjectWeb` 的记忆项或验证证据

Hook 与 MR 审查边界：

- 本地 hook 只提供快速阻断，不替代 `./mvnw -pl <module> -am verify`、完整 `./mvnw clean verify` 或 GitLab CI
- MR 模板提供动态 Workflow Packet，不替代人工 Review、红区人工确认和目标环境验收
- 禁止使用 `--no-verify` 绕过 hook；确有特殊情况必须在 MR 中说明原因并人工确认

## 推荐实践

- 保持分支短生命周期，尽量小步提交、频繁同步 `develop`
- 在 MR 描述中附带测试结果、截图、接口示例或迁移说明
- 把重构与功能变更拆成独立提交，提升审查效率
- 在临近发布时减少大规模重构，降低集成风险
- 对高风险分支设置明确 owner 和 reviewer

## 反例/禁用项

- 一个分支同时混入需求开发、重构、格式化、依赖升级
- 提交信息写成 `update`、`fix bug`、`misc`
- 直接在 `main` 上开发后再强行补 MR
- 没有验证结果就请求合并
- `release/*` 分支中继续开发新功能

## 检查清单

- [ ] 任务已在独立 worktree 中处理，或已命中低风险例外并在任务确认/交付中说明原因
- [ ] 分支类型与命名符合 Git Flow 规范
- [ ] 提交信息符合 Conventional Commits
- [ ] 本地 Git hooks 已安装或已在 MR 中说明未安装原因
- [ ] 任务已完成本地 merge-back，且对应 worktree 与已合并分支已在验证后清理
- [ ] MR 描述包含目的、影响、验证和风险
- [ ] MR 已填写 Workflow Packet，高风险变更已执行 Red Team
- [ ] MR 已填写 Memory Update Packet，并引用相关记忆项 ID 或说明未更新原因
- [ ] 涉及依赖、基础设施、数据库、镜像的变更已附信创兼容说明
- [ ] Review、CI、冲突处理已完成
- [ ] `release/*` 与 `hotfix/*` 的回合并路径已执行
- [ ] 发布协作与 `RELEASE.md` 保持一致

## 关联文档

- [RELEASE.md](./RELEASE.md)
- [AGENTS.md](../AGENTS.md)
- [DB_RULES.md](./DB_RULES.md)
- [XINCHUANG_RULES.md](./XINCHUANG_RULES.md)
