# SY Base Project

`SYBaseProject` 是一个基于 `Java 17 + Spring Boot 3 + Maven Wrapper` 的多模块 DDD 脚手架，按照 `docs/` 下的工程规范预置目录结构、公共基础模块和一个 `user-center` 示例业务模块。

## 当前已落地模块

- `common/common-core`：通用错误码、异常基类和值对象约定
- `common/common-web`：统一响应、自动返回体包装、全局异常处理、`traceId` 过滤器
- `common/common-test`：测试依赖和测试支撑基类
- `user-center`：示例业务模块，演示 `interfaces -> application -> domain -> infrastructure` 四层分离
- `tools/app-cli`：基于 `Picocli + Spring Boot` 的标准 CLI 模块，演示命令式应用入口

## 预留目录与扩展点

以下目录目前是平台扩展位或占位目录，用于后续迭代，不应视为已交付能力：

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

完整文档导航见 [docs/README.md](./docs/README.md)。
当前目录契约与占位说明见 [docs/guides/PROJECT_DIRECTORY.md](./docs/guides/PROJECT_DIRECTORY.md)。
模板初始化、包名替换、数据库/Flyway、profile 与接口文档约定见 [docs/guides/TEMPLATE_CUSTOMIZATION.md](./docs/guides/TEMPLATE_CUSTOMIZATION.md)。
本地 GitLab 启动模板见 [deploy/docker/gitlab/README.md](./deploy/docker/gitlab/README.md)。
如需按“本地 GitLab 开发环境 + 开发服务器测试环境”运行 CI/CD，请参考 [docs/guides/GITLAB_LOCAL_TEST_FLOW.md](./docs/guides/GITLAB_LOCAL_TEST_FLOW.md)。

## 启动方式

前置条件：

- `JDK 17`
- `JAVA_HOME` 指向 JDK 17，或本机 `java` 默认就是 JDK 17

建议优先使用 Maven Wrapper，避免本机安装的 `mvn` 绑定到其他 JDK 版本。

校验当前构建基线：

```bash
./mvnw -version
```

输出中的 `Java version` 应为 `17.x`。

首次克隆后，先让多模块依赖安装到仓库内本地缓存：

```bash
./mvnw -B -ntp -Dmaven.repo.local=.m2/repository install -DskipTests
```

启动示例模块：

```bash
./mvnw -Dmaven.repo.local=.m2/repository -f user-center/pom.xml spring-boot:run
```

运行 CLI 样例：

```bash
./mvnw -Dmaven.repo.local=.m2/repository -f tools/app-cli/pom.xml spring-boot:run -Dspring-boot.run.arguments="version"
```

执行测试：

```bash
./mvnw test
```

## 示例接口

- `POST /api/v1/users`
- `GET /api/v1/users/{id}`

业务接口默认会自动包装为统一返回体；如需返回原始内容，可使用 `@IgnoreApiResponseWrap` 跳过包装。

## 当前实现取舍

- 使用占位 `groupId` / `base package`：`com.company`
- `UserRepository` 当前为内存实现，后续可替换为数据库仓储实现
- 暂未接入鉴权、数据库、缓存、消息队列和 Flyway
- `gateway`、`auth-center`、`order-center`、`ai-center`、`admin-web` 当前仅为占位目录
- `infrastructure/*` 与多数 `common/*` 扩展模块当前只有边界说明，尚未落实现代码

## 后续替换建议

1. 将 `com.company` 替换为真实组织域名倒序包名
2. 为 `user-center` 引入真实 `infrastructure.persistence` 实现，并保留 `domain.repository` 作为稳定抽象
3. 按 `docs/rules/DB_RULES.md` 增加数据库迁移与达梦兼容验证记录
4. 按 `docs/rules/API_RULES.md` 补充 OpenAPI / Apifox 文档
