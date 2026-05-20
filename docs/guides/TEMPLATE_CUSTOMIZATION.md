# TEMPLATE_CUSTOMIZATION.md — 脚手架落地与模板化约定

本文件定义如何把当前示例脚手架落地为真实项目，避免在多个模块里做零散替换。

## 1. 构建基线

- 本仓库默认基线是 `JDK 17 + Maven Wrapper`
- 本地执行一律优先使用 `./mvnw`
- 提交前至少执行一次：

```bash
./mvnw -version
./mvnw test
```

- 如果 `Java version` 不是 `17.x`，先修正 `JAVA_HOME` 或本机默认 `java`

## 2. 一次性模板替换项

新项目初始化时统一替换以下占位内容，不做散点式手工修改：

- 根 `groupId`：`com.company`
- 基础包名：`com.company`
- 服务名：如 `user-center`
- 镜像仓库名前缀、部署环境变量命名
- 文档中的示例组织名、模块名、域对象名

建议把这些值沉淀为项目初始化清单，在第一次复制模板时一次改完。

## 3. 业务模块标准分层

以 `user-center` 为参考模块：

- `interfaces`：Controller、DTO、VO、Assembler、Facade
- `application`：用例编排、事务边界、Command、Query、Task、Event
- `domain`：聚合、值对象、领域服务、仓储抽象、Factory、Enum
- `infrastructure`：持久化、缓存、MQ、RPC、配置、对象转换

新增真实持久化实现时：

- 仓储接口保留在 `domain.repository`
- 数据源、Mapper、DO、SQL、Flyway、方言兼容逻辑只放在 `infrastructure`
- 不允许把数据库细节倒灌到 `domain` 或 `application`

## 4. 数据库与 Flyway 落地路径

- 数据库迁移脚本统一放在业务模块资源目录：`src/main/resources/db/migration`
- 脚本命名遵循 Flyway 约定：`V{version}__{description}.sql`
- 达梦兼容性验证记录放在 `docs/` 或模块内补充文档中，不散落在提交说明里
- 在数据库尚未选型前，可以继续保留内存仓储实现，但要保持 `domain.repository` 接口稳定

推荐演进顺序：

1. 增加持久化配置与 DO/Mapper
2. 引入首个 `V1__baseline.sql`
3. 用测试 profile 验证迁移和仓储实现
4. 记录达梦兼容性差异

## 5. Profile 约定

默认保留以下 profile 语义：

- `default`：最小可运行配置，适合样板演示
- `dev`：本地联调配置，可接数据库、日志增强、调试开关
- `test`：自动化测试配置，要求可重复、可隔离

如果模块需要拆分配置，优先采用：

- `application.yml`
- `application-dev.yml`
- `application-test.yml`

不要把环境差异写死在 Java 代码里。

## 6. OpenAPI / Apifox 产出约定

- 对外 REST 接口变更后，同步维护 OpenAPI 文档
- OpenAPI 产物建议按模块归档，例如：`docs/api/user-center-openapi.yaml`
- Apifox 项目导出文件与共享链接说明放在 `docs/api/`
- 文档中的示例请求、响应、错误码必须与代码实现一致

当前 `user-center` 仍以代码与测试作为契约来源；引入新模块前，先补齐首份 OpenAPI 文档。
