## 当前仓库结构

本文档说明两个问题：

1. 当前哪些目录已经纳入构建并承载实际能力
2. 哪些目录仍是预留扩展位，不应误判为已交付模块

## 已纳入构建的主要模块

```text
project-root
├─ pom.xml
├─ README.md
├─ docs/
├─ common/
│  ├─ common-core
│  ├─ common-security
│  ├─ common-web
│  └─ common-test
├─ auth-center/
├─ bl-center/
├─ user-center/
└─ tools/
   └─ app-cli/
```

说明：

- 根 `pom.xml` 当前聚合 `common/*`、`auth-center`、`bl-center`、`user-center` 与 `tools/app-cli`
- `bl-center` 是当前病理业务主模块，承载 M1-M4 业务能力
- `auth-center` 已不是占位目录，而是实际参与本地启动与鉴权支持的模块
- `user-center` 保留为结构示例模块，可作为新业务模块分层参考
- `tools/app-cli` 是可运行的 CLI 模块示例

## 已落地但不属于独立业务中心的目录

- `scripts/dev/`：本地开发启动脚本
- `scripts/migration/`：Flyway 与迁移辅助脚本
- `deploy/`：本地 GitLab 与环境样例
- `.run/`：共享 IDE 运行配置

这些目录已经服务于日常开发，但不代表新的业务中心边界。

## 仍为预留扩展位的目录

以下目录当前仍属于扩展边界或规划入口，通常只有 README、空目录或少量说明文件：

| 目录 | 当前状态 | 预期用途 |
|---|---|---|
| `gateway/` | 占位 | API 网关或 BFF |
| `order-center/` | 占位 | 其他业务中心样板 |
| `ai-center/` | 占位 | AI 能力编排与模型接入 |
| `admin-web/` | 占位 | 管理端前端 |
| 顶层 `infrastructure/` | 占位 | 平台级共享基础设施沉淀 |
| `common/common-redis` | 占位 | Redis 公共模块 |
| `common/common-mq` | 占位 | 消息队列公共模块 |
| `common/common-ai` | 占位 | AI 公共模块 |
| `common/common-utils` | 占位 | 稳定复用后的通用工具集 |

## `user-center` 目录契约

`user-center` 仍作为推荐分层模板：

```text
user-center/src/main/java/com/company/user
├─ interfaces
├─ application
├─ domain
└─ infrastructure
```

约束：

- `interfaces`：控制器、DTO、VO、Assembler、Facade
- `application`：用例编排、事务边界、Command、Query、Task、Event
- `domain`：聚合、值对象、领域服务、仓储抽象
- `infrastructure`：持久化、缓存、MQ、RPC、配置与转换器

## 目录治理原则

- 只有纳入根 `pom.xml` 的模块，才视为当前已交付能力
- 只有目录存在但未纳入构建，不应被描述为“已完成模块”
- 公共能力只有在被至少两个模块稳定复用后，才考虑抽到 `common/*` 或顶层 `infrastructure/*`
- 文档、脚本、运行配置的存在不等于业务边界已经实现
