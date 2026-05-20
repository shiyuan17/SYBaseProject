## 当前仓库状态

本文件描述两件事：

1. 当前仓库里已经落地并纳入构建的目录
2. 作为脚手架保留、但暂未实现的扩展位

不要把预留目录误认为当前已交付能力。

## 已落地并纳入构建

```text
project-root
├── pom.xml
├── README.md
├── .gitlab-ci.yml
├── docs/
│   ├── README.md
│   ├── AGENTS.md
│   ├── rules/
│   ├── guides/
│   ├── plans/
│   ├── database/
│   └── detailed_list/
├── common/
│   ├── common-core
│   ├── common-security
│   ├── common-web
│   └── common-test
├── auth-center/
│   ├── pom.xml
│   └── src/
├── bl-center/
│   ├── pom.xml
│   └── src/
├── user-center/
│   ├── pom.xml
│   └── src/
└── tools/
    └── app-cli/
```

说明：

- 根 `pom.xml` 当前聚合 `common-core`、`common-security`、`common-web`、`common-test`、`auth-center`、`bl-center`、`user-center`、`tools/app-cli`
- `bl-center` 是当前病理主业务模块，`user-center` 保留为示例业务样板
- `tools/app-cli` 是当前 CLI 工具模块

## 预留目录与启用条件

以下目录当前保留为扩展位，通常只有 README 或少量辅助文件：

| 目录 | 当前状态 | 用途 | 何时启用 |
|---|---|---|---|
| `gateway/` | 占位 | API 网关或 BFF | 需要统一入口、路由、限流、鉴权前置时 |
| `auth-center/` | 占位 | 认证授权中心 | 开始建设统一登录、令牌、权限域时 |
| `order-center/` | 占位 | 第二个业务中心样板候选 | 需要验证多业务中心协作时 |
| `ai-center/` | 占位 | AI 能力编排与模型接入 | 业务真的需要 AI 服务边界时 |
| `admin-web/` | 占位 | 管理端前端 | 至少两个后端模块稳定后再启动 |
| `infrastructure/*` | 占位 | 平台级组件沉淀 | 出现跨模块复用需求后再抽离 |
| `common/common-security` | 占位 | 安全公共模块 | 需要共享鉴权、安全工具时 |
| `common/common-redis` | 占位 | Redis 公共模块 | 需要统一缓存封装时 |
| `common/common-mq` | 占位 | 消息队列公共模块 | 需要统一事件通信时 |
| `common/common-ai` | 占位 | AI 公共模块 | 需要统一模型调用抽象时 |
| `common/common-utils` | 占位 | 通用工具集 | 有稳定复用价值再沉淀 |

## `user-center` 目录契约

`user-center` 仍是新增业务模块的结构模板：

```text
user-center/src/main/java/com/company/user
├── interfaces
├── application
├── domain
└── infrastructure
```

约束：

- `interfaces` 放控制器、DTO、VO、Assembler、Facade
- `application` 放用例编排、事务边界、Command、Query、Task、Event
- `domain` 放聚合、值对象、领域服务、仓储抽象
- `infrastructure` 放持久化、缓存、MQ、RPC、配置、转换器

新增真实数据库实现时，只在 `infrastructure.persistence` 落代码；`domain.repository` 保持稳定抽象。

## 目录治理原则

- 当前已纳入根 `pom.xml` 的模块，才算脚手架已交付能力
- 仅有 README 的目录属于规划边界，不属于现成功能
- 新增模块前，优先复用 `user-center` 的分层和 `common/*` 的已有基线
- 只有当能力被至少两个模块复用时，才考虑从业务模块抽到顶层 `infrastructure/*` 或 `common/*`
