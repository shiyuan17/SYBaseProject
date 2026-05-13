# SY Base Project

`SYBaseProject` 是一个基于 `Java 17 + Spring Boot 3 + Maven` 的多模块 DDD 脚手架，按照 `docs/` 下的工程规范预置目录结构、公共基础模块和一个 `user-center` 示例业务模块。

## 模块说明

- `common/common-core`：通用错误码、异常基类和值对象约定
- `common/common-web`：统一响应、全局异常处理、`traceId` 过滤器
- `common/common-test`：测试依赖和测试支撑基类
- `user-center`：示例业务模块，演示 `interfaces -> application -> domain -> infrastructure` 四层分离

## 目录骨架

项目目录遵循 [docs/PROJECT_DIRECTORY.md](./docs/PROJECT_DIRECTORY.md) 中的约定，并预留以下平台目录：

- `scripts/`
- `deploy/`
- `sql/`
- `tools/`
- `test/`
- `gateway/`
- `auth-center/`
- `order-center/`
- `ai-center/`
- `admin-web/`
- 顶层 `infrastructure/`

## 启动方式

前置条件：

- `JDK 17+`
- `Maven 3.9+`

启动示例模块：

```bash
mvn -pl user-center spring-boot:run
```

执行测试：

```bash
mvn test
```

## 示例接口

- `POST /api/v1/users`
- `GET /api/v1/users/{id}`

## 当前实现取舍

- 使用占位 `groupId` / `base package`：`com.company`
- `UserRepository` 当前为内存实现，后续可替换为数据库仓储实现
- 暂未接入鉴权、数据库、缓存、消息队列和 Flyway

## 后续替换建议

1. 将 `com.company` 替换为真实组织域名倒序包名
2. 为 `user-center` 引入真实 `infrastructure.persistence` 实现，并保留 `domain.repository` 作为稳定抽象
3. 按 `docs/DB_RULES.md` 增加数据库迁移与达梦兼容验证记录
4. 按 `docs/API_RULES.md` 补充 OpenAPI / Apifox 文档
