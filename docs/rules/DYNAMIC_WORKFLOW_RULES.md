# DYNAMIC_WORKFLOW_RULES.md — 动态任务路由剧本

## 目标与适用范围

本文件定义后端任务如何按类型选择不同 AI Workflow。动态 Workflow 不只是 Review，而是决定本次任务应该启用哪些专家 Agent、测试、模拟、安全检查、数据库验证与红队对抗。

适用范围：

- Java 17 / Spring Boot 3 多模块后端
- DDD 四层结构、REST API、数据库迁移、权限安全、CI/CD 与生产问题排查
- 与 `SYBaseProjectWeb` 前端联动的字段、菜单、权限与流程闭环

## 总规则

每个任务必须先选择一个主 Workflow，再按风险叠加强制修饰器。修饰器全集只在本节定义，其他章节与文档只引用本节：

- 主 Workflow：API、DB、Security、Architecture、Production Debug、Workflow/Infra 之一
- Security 修饰器：涉及认证、授权、患者信息、报告信息、脱敏、审计、敏感日志时必须叠加
- DB 修饰器：涉及迁移、种子、SQL、表结构、索引、约束、数据兼容或回滚时必须叠加
- Red Team 修饰器：高风险、跨层、生产问题、权限/数据/报告相关任务必须叠加
- Frontend Cross-check 修饰器（跨仓）：需要对照前端 `SYBaseProjectWeb` 消费方式或验证时必须叠加；后端 MR 必须引用前端 PR/验证结果，前端 PR 必须引用后端 MR/验证结果

MR 必须填写 Workflow Packet，说明为什么选择该 Workflow、启用哪些专家 Agent、跑了哪些动态测试和模拟、红队攻击结论是什么。

跨仓口径对齐（与前端 `SYBaseProjectWeb/docs/DYNAMIC_WORKFLOW_RULES.md` 互为镜像）：

- 两仓主 Workflow 分类与修饰器语义保持一致；前端额外有 UI Workflow 与 Browser 验证修饰器，后端无 UI 类任务
- 平台差异：前端走 GitHub PR（`pr-packet.yml` 自动校验 Workflow Packet 字段），后端走 GitLab MR（`verify` 阶段含 `verify_governance` 治理校验，Packet 由 MR 模板与人工审查把关）
- 跨仓任务的双向引用是硬要求：后端 MR 描述中写明前端 PR 链接与前端验证结论，前端 PR 同理；记忆文件（`DECISIONS.md` 等）中的跨仓条目也必须双向引用
- 修改任一仓的 Workflow 分类、修饰器全集或 Packet 字段语义时，必须同步评估另一仓对应文档并在 MR/PR 中说明

## 触发信号速查表

任务开始时先按"改动路径 / 需求信号"查下表选主 Workflow，再按命中行叠加必叠修饰器。一个任务可能命中多行：主 Workflow 取最贴近核心改动的一项，其余命中项作为修饰器叠加。判断有歧义或跨多类时，从严就高（优先叠加 Security / DB / Red Team）。

| 改动路径 / 需求信号 | 主 Workflow | 必叠修饰器 |
| --- | --- | --- |
| Controller、Request/Response、错误码、分页、接口路径、字段映射、前端联调 | API | Frontend Cross-check（跨仓时） |
| `db/migration`、Flyway、SQL、种子数据、索引/约束、回滚、历史数据兼容 | DB | DB、Red Team |
| 认证、授权、角色、数据范围、患者信息、报告信息、脱敏、审计日志 | Security | Security、Red Team |
| `domain` / `repository` 边界、共享模块、大文件重构、跨模块依赖 | Architecture | Red Team（跨层时） |
| 生产问题、线上故障、性能回退、`.logs/` 中已有错误 | Production Debug | Red Team、Frontend Cross-check（跨仓时） |
| Git hooks、GitLab CI、镜像、部署脚本、环境变量、发布路径 | Workflow/Infra | Red Team（红区时） |
| 纯文档、注释、闲聊或信息查询，不改运行时行为 | 不适用（标注原因即可） | 无 |

## Workflow Packet

MR 中必须包含以下信息：

- 主 Workflow：
- 触发信号：改动路径、需求类型、风险点
- 专家 Agent：本次使用的专家角色
- 动态测试：实际运行的命令和结果
- 动态模拟：请求、数据、角色、迁移、日志回放或目标环境差异
- 动态安全：是否触发 Security 修饰器，结论是什么
- 动态数据库：是否触发 DB 修饰器，迁移/回滚/兼容验证是什么
- Red Team：攻击路径、失败/成功结论、剩余风险
- Memory Update：更新的记忆文件、未更新文件与原因、相关记忆项 ID、跨仓引用

## Memory Layer

所有 Workflow 都必须在交付前检查根目录五类记忆文件，并判断本次任务是否产生持久上下文：

- 项目状态变化：更新 `PROJECT_STATE.md`
- 技术债发现或状态变化：追加或更新 `TECH_DEBT.md`
- 已知 bug 发现、复现或修复：追加或更新 `KNOWN_BUGS.md`
- 影响后续协作的决策：追加 `DECISIONS.md`
- 稳定架构边界、跨仓接口或约束变化：更新 `ARCHITECTURE.md`

强制重点：

- Security Workflow 必须特别检查 `KNOWN_BUGS.md`、`DECISIONS.md`、`ARCHITECTURE.md`
- DB Workflow 必须特别检查 `TECH_DEBT.md`、`KNOWN_BUGS.md`、`DECISIONS.md`、`ARCHITECTURE.md`
- Production Debug Workflow 必须特别检查 `PROJECT_STATE.md`、`KNOWN_BUGS.md`、`DECISIONS.md`
- Architecture Workflow 必须特别检查 `DECISIONS.md`、`ARCHITECTURE.md`，必要时更新 `TECH_DEBT.md`
- 跨仓任务必须双向引用前后端记忆项和验证证据

不做流水账式“无变化”更新；未更新的文件只在交付摘要和 MR Memory Update Packet 中说明原因。

## API Workflow

触发条件：

- Controller、Request/Response、错误码、分页、接口路径、字段映射、前后端联调

专家 Agent：

- API Contract Agent：检查 REST 契约、错误模型、兼容性和前端字段映射
- Frontend Cross-check Agent：需要时对照 `SYBaseProjectWeb` 请求模型与页面消费方式

动态测试：

- `./mvnw -pl <module> -am test`
- 受影响模块 `./mvnw -pl <module> -am verify`
- 必要时 `bash scripts/ci/run-m1-m5-api-regression.sh`

动态模拟：

- 成功响应、错误响应、空数据、重复提交、超时、旧字段兼容
- 前端页面真实消费路径或请求 payload

红队问题：

- 是否破坏旧接口或前端字段映射
- 是否错误码让前端误判成功/失败
- 是否把内部异常、敏感字段或持久化对象泄露到接口

交付证据：

- 接口变更说明、请求/响应样例、前端影响、验证命令

记忆层重点：

- REST 契约、错误模型、分页或前端字段映射形成稳定结论时更新 `DECISIONS.md`
- 接口兼容债务或已知联调缺陷更新 `TECH_DEBT.md` / `KNOWN_BUGS.md`
- 涉及前端消费变化时在前后端记忆文件中双向引用

## DB Workflow

触发条件：

- Flyway 迁移、SQL、种子数据、索引/约束、回滚、历史数据兼容

专家 Agent：

- DB/Migration Agent：检查迁移幂等、回滚、兼容查询、数据安全
- Red Team Agent：攻击数据丢失、重复迁移、约束冲突和回滚失败

动态测试：

- `./mvnw -pl bl-center -am verify` 或受影响模块 verify
- `scripts/migration/run-bl-center-flyway.sh` / `.cmd` 目标环境前演练
- 必要时 API 回归验证迁移后的业务路径

动态模拟：

- 空库、旧数据、新数据、重复执行、部分失败、回滚后查询
- 达梦/目标数据库兼容差异

红队问题：

- 是否可能丢数据、重复写、破坏索引/约束
- 是否迁移不可重复执行或无法回滚
- 是否旧版本前端/接口读取新结构失败

交付证据：

- 迁移文件、回滚方案、兼容验证、目标环境待验项

记忆层重点：

- 数据库兼容、迁移、回滚、索引/约束或目标环境约束必须检查 `TECH_DEBT.md`、`KNOWN_BUGS.md`、`DECISIONS.md`、`ARCHITECTURE.md`
- 可复现迁移失败或回滚失败进入 `KNOWN_BUGS.md`，长期数据治理缺口进入 `TECH_DEBT.md`
- 影响前端展示或接口兼容时双向引用前端记忆文件

## Security Workflow

触发条件：

- 认证、授权、角色、数据范围、患者信息、报告信息、脱敏、审计日志

专家 Agent：

- Security/Privacy Agent：检查最小权限、数据范围、敏感字段和审计
- Red Team Agent：主动尝试越权、泄密、绕过审计

动态测试：

- 受影响模块权限/接口测试
- `./mvnw -pl auth-center -am test`、`./mvnw -pl user-center -am test` 或 `./mvnw -pl bl-center -am test`
- 前端联动时引用路由/菜单/角色模拟结果

动态模拟：

- 未登录、低权限、跨角色、跨数据范围、报告导出、错误日志

红队问题：

- 是否能绕过认证授权或数据范围
- 是否患者/报告信息进入日志、异常、接口、导出或审计盲区
- 是否只在前端隐藏入口，后端没有授权兜底

交付证据：

- 权限矩阵、角色模拟、敏感字段检查、审计/日志结论

记忆层重点：

- 认证、授权、患者信息、报告信息、脱敏、审计相关结论必须检查 `KNOWN_BUGS.md`、`DECISIONS.md`、`ARCHITECTURE.md`
- 可复现越权、泄露、审计缺口必须进入 `KNOWN_BUGS.md`，安全债务进入 `TECH_DEBT.md`

## Architecture Workflow

触发条件：

- DDD 分层、领域模型、仓储契约、共享模块、大文件重构、跨模块依赖

专家 Agent：

- Architecture Agent：检查分层、接口深度、影响面和测试面
- Codegraph Agent：优先查询调用关系、依赖方向和影响半径

动态测试：

- `./mvnw -pl <module> -am verify`
- 必要时 `./mvnw clean verify`
- 代码健康报告或文件健康报告仅作为治理证据，不替代测试

动态模拟：

- 旧调用方、新调用方、仓储替换、事务边界、失败分支

红队问题：

- 是否出现 `domain -> infrastructure`、`controller -> repository` 等越层依赖
- 是否制造浅模块、万能 service 或共享模块业务耦合
- 是否重构后没有可回归的公共接口测试

交付证据：

- 影响面、分层说明、调用关系、验证命令、兼容风险

记忆层重点：

- DDD 分层、模块边界、仓储契约、共享模块约束变化必须更新 `ARCHITECTURE.md`
- 形成架构取舍时追加 `DECISIONS.md`
- 发现大文件、越层依赖、万能 service 等持久问题时更新 `TECH_DEBT.md`

## Production Debug Workflow

触发条件：

- 生产问题、线上故障、性能回退、用户现场阻塞、日志中已有错误

专家 Agent：

- Diagnose Agent：复现、最小化、假设、插桩、修复、回归
- Execution Agent：记录命令、环境、证据和回滚路径

动态测试：

- 先构造可重复反馈环，再写回归测试
- 必须先读取 `.logs/backend.log`
- 前端相关问题同步读取前端日志并引用前端验证

动态模拟：

- 日志回放、请求重放、目标环境配置差异、慢查询、异常数据

红队问题：

- 是否未复现就修复
- 是否只修表象没有验证原始故障
- 是否缺少回滚、监控信号或失败证据

交付证据：

- 复现步骤、日志片段、修复前后对比、回归测试、回滚说明

记忆层重点：

- 生产问题必须检查 `PROJECT_STATE.md`、`KNOWN_BUGS.md`、`DECISIONS.md`
- 已复现故障进入 `KNOWN_BUGS.md`，回滚或处置决策进入 `DECISIONS.md`
- 当前阻塞、验证基线和交接重点更新 `PROJECT_STATE.md`

## Workflow/Infra Workflow

触发条件：

- Git hooks、GitLab CI、镜像、部署脚本、环境变量、发布路径、运行脚本

专家 Agent：

- Workflow/Infra Agent：检查本地命令、CI/CD、跨平台、信创兼容和发布风险

动态测试：

- 对应 hook/CI/脚本命令
- `./mvnw clean verify` 或与 CI 同口径的 verify
- 涉及部署时引用 GitLab CI 或脚本 dry-run 结果

动态模拟：

- Windows/Linux 命令差异、缺变量、缺权限、目标环境路径、镜像 tag 回滚

红队问题：

- 是否能绕过 hook、CI、部署审批或 protected 环境
- 是否脚本会改写非目标文件、泄露密钥或硬编码本机路径
- 是否失败后无法定位镜像 tag、日志和回滚入口

交付证据：

- 命令结果、CI 影响、环境变量说明、信创兼容与回滚路径

记忆层重点：

- hook、CI、脚本、环境约束变化时更新 `PROJECT_STATE.md` 或 `DECISIONS.md`
- 稳定工具边界、信创兼容或跨平台约束变化时更新 `ARCHITECTURE.md`
- 已知 hook/CI 阻塞进入 `KNOWN_BUGS.md`，长期治理缺口进入 `TECH_DEBT.md`
