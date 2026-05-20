# GIT_RULES.md — Git 协作与分支规范

## 目标与适用范围

本文件定义项目的 Git 分支模型、提交规范、PR 规则与合并流程。

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

- `feature/JIRA-123-user-authentication`
- `fix/JIRA-456-order-status`
- `hotfix/JIRA-789-payment-timeout`
- `release/1.2.0`
- `docs/refresh-engineering-rules`
- `refactor/split-user-application-service`

要求：

- 使用英文小写短语，单词间用 `-`
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

### 4. PR 与评审规则

- 禁止直接向 `main`、`develop`、`release/*` 推送
- 所有变更必须通过 PR 合入
- PR 必须包含变更目的、影响范围、验证方式、风险说明
- 涉及公共接口、数据库、权限、发布流程的变更必须重点标注
- 涉及依赖、基础设施、数据库、镜像的变更，PR 必须附带信创兼容说明或引用验证记录

默认门禁：

- `main`：至少 2 人 Review，CI 全绿
- `develop`：至少 1 人 Review，CI 全绿
- `release/*`：至少 2 人 Review，仅允许发布相关修复

### 5. 合并规则

- 默认使用非快进合并或团队指定策略，保留分支上下文
- 合并前必须解决冲突并重新验证
- `hotfix/*` 合入 `main` 后必须回合并到 `develop`
- `release/*` 完成后必须同步回 `develop`

## 推荐实践

- 保持分支短生命周期，尽量小步提交、频繁同步 `develop`
- 在 PR 描述中附带测试结果、截图、接口示例或迁移说明
- 把重构与功能变更拆成独立提交，提升审查效率
- 在临近发布时减少大规模重构，降低集成风险
- 对高风险分支设置明确 owner 和 reviewer

## 反例/禁用项

- 一个分支同时混入需求开发、重构、格式化、依赖升级
- 提交信息写成 `update`、`fix bug`、`misc`
- 直接在 `main` 上开发后再强行补 PR
- 没有验证结果就请求合并
- `release/*` 分支中继续开发新功能

## 检查清单

- [ ] 分支类型与命名符合 Git Flow 规范
- [ ] 提交信息符合 Conventional Commits
- [ ] PR 描述包含目的、影响、验证和风险
- [ ] 涉及依赖、基础设施、数据库、镜像的变更已附信创兼容说明
- [ ] Review、CI、冲突处理已完成
- [ ] `release/*` 与 `hotfix/*` 的回合并路径已执行
- [ ] 发布协作与 `RELEASE.md` 保持一致

## 关联文档

- [RELEASE.md](./RELEASE.md)
- [AGENTS.md](../AGENTS.md)
- [DB_RULES.md](./DB_RULES.md)
- [XINCHUANG_RULES.md](./XINCHUANG_RULES.md)
