# SY Base Project

`SYBaseProject` 是一个基于 `Java 17 + Spring Boot 3 + Maven Wrapper` 的多模块病理业务工程。当前仓库已经不再只是 DDD 脚手架示例，`bl-center`、`auth-center`、公共基础模块、Flyway 迁移和多条 M1-M4 业务链路都已落地。

## 当前模块

- `bl-center`：病理业务主模块，已覆盖 M1-M4 多阶段能力
- `auth-center`：认证鉴权与登录支持模块
- `common/common-core`：通用错误码、异常和值对象约定
- `common/common-security`：鉴权、安全与密码能力
- `common/common-web`：统一响应、异常处理、Web 支撑
- `common/common-test`：测试基类、仓库治理校验与集成测试支撑
- `user-center`：保留的分层结构示例模块
- `tools/app-cli`：命令行工具示例模块

## 当前状态

- M1：系统管理、菜单权限、系统配置、编号规则已可运行
- M2：申请单、标本登记、固定、接收、运送主流程已落地
- M3：技术流程主链路已落地，支持待办查询、技术追踪、返工与 QC 历史可视化
- M4：诊断报告、修订、医嘱、会诊等诊断闭环能力已接入

详细目录说明见 [docs/guides/PROJECT_DIRECTORY.md](./docs/guides/PROJECT_DIRECTORY.md)，文档导航见 [docs/README.md](./docs/README.md)。

## 启动方式

前置条件：

- `JDK 17`
- `JAVA_HOME` 指向 JDK 17，或本机 `java` 默认就是 JDK 17

建议优先使用 Maven Wrapper，避免本机 Maven/JDK 版本漂移。

校验构建环境：

```bash
./mvnw -version
```

首次克隆后安装依赖到仓库内本地缓存：

```bash
./mvnw -B -ntp -Dmaven.repo.local=.m2/repository install -DskipTests
```

运行全部测试：

```bash
./mvnw test
```

## Local Dev Startup

- Prefer Maven Wrapper for local service startup instead of relying on IDE incremental compilation outputs.
- Shared IntelliJ Spring Boot run configurations for `bl-center` and `auth-center` now run a Maven before-launch compile step so `target/classes` is refreshed before `Run` or `Debug`.
- Command line launchers are available in `scripts/dev/`.

```bash
./scripts/dev/run-bl-center-dev.sh
./scripts/dev/run-auth-center-dev.sh
```

On Windows, use:

```powershell
.\scripts\dev\run-bl-center-dev.cmd
.\scripts\dev\run-auth-center-dev.cmd
```

If you hit `ClassNotFoundException: com.company.bl.BlCenterApplication`, rebuild the module output with:

```powershell
.\mvnw.cmd -pl bl-center -am compile -DskipTests
```

If you hit `ClassNotFoundException: com.company.auth.AuthCenterApplication`, rebuild with:

```powershell
.\mvnw.cmd -pl auth-center -am compile -DskipTests
```

## 目录与治理说明

- `scripts/dev/`：本地启动脚本
- `scripts/migration/`：Flyway 与迁移辅助脚本
- `docs/`：协作规范、工程说明、计划与治理文档
- `deploy/`：本地 GitLab 与部署相关样例
- `gateway/`、`order-center/`、`ai-center/`、`admin-web/`：当前仍为预留扩展位
- 顶层 `infrastructure/`：平台级沉淀预留目录，尚未作为独立可运行模块交付

## 示例接口

- `POST /api/v1/applications`
- `POST /api/v1/specimens/register`
- `GET /api/v1/technical-tasks/pending`
- `GET /api/v1/pathology-cases/{id}/technical-tracking`
- `GET /api/v1/pathology-cases/{id}/diagnostic-workbench`

业务接口默认会自动包装为统一返回体；如需返回原始内容，可使用 `@IgnoreApiResponseWrap` 跳过包装。
