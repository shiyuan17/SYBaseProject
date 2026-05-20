# CONTAINER_RULES.md — 容器化与镜像规范

## 目标与适用范围

本文件定义项目服务的容器化目录约定、镜像构建要求、运行时契约与发布门禁。

- 适用对象：研发、测试、运维、发布负责人、AI 助手
- 适用范围：服务级 `Dockerfile`、镜像构建脚本、Docker 主机部署模板、容器运行参数
- 规范定位：聚焦“如何构建和运行容器镜像”；发布节奏与审批流程以 `RELEASE.md` 为准

## 强制规则

### 1. 目录与职责边界

- 服务级 `Dockerfile` 必须放在各服务目录下，例如 `user-center/Dockerfile`
- `deploy/docker/` 只允许放运行编排、环境模板和运行说明，不得承载服务源码构建逻辑
- `scripts/ci/` 中的容器脚本必须可被 GitLab CI 复用，也应支持本地手工执行
- 新增服务时必须按“服务目录内定义镜像、`deploy/docker/<service>` 定义部署模板”的方式扩展

### 2. 镜像构建基线

- Java 服务统一采用多阶段构建：构建阶段使用 `Maven 3.9 + JDK 17`，运行阶段使用 `JRE 17`
- 构建上下文必须受 `.dockerignore` 约束，避免把 `target/`、日志、IDE 文件和 Git 元数据打进镜像
- 镜像必须支持通过 `ARG` 注入版本号、提交号、构建时间，并写入 OCI Label
- 镜像标签至少保留三类：
  - `commit sha`
  - `branch slug`
  - `release tag`
- 镜像仓库默认使用 GitLab Container Registry；如切换 Harbor，仅允许替换登录与仓库变量，不得改动整体命名策略

### 3. 运行时契约

- 运行镜像必须使用非 root 用户启动
- 容器时区必须明确，默认使用 `Asia/Shanghai`；如环境要求 UTC，必须在部署模板中显式覆盖
- 应用日志必须输出到 stdout/stderr，不得默认写容器内本地文件作为唯一日志出口
- 容器启动参数必须支持通过环境变量覆盖，例如 `JAVA_OPTS`、`SPRING_PROFILES_ACTIVE`、`SERVER_PORT`
- 不得把数据库密码、访问令牌、私钥、证书等敏感信息写死在 Dockerfile、镜像层或示例 `.env` 文件中

### 4. 健康检查与可观测性

- Spring Boot 服务必须保留 `/actuator/health` 健康检查入口
- Spring Boot 服务必须保留 `/actuator/prometheus` 指标抓取入口
- 镜像或部署模板必须定义健康检查策略，默认以 `/actuator/health` 作为探测目标
- 部署模板中的监控、日志、端口暴露方式必须与 `OBSERVABILITY_RULES.md` 保持一致

### 5. Docker 主机部署约束

- Docker 主机部署只允许拉取仓库中已构建完成的镜像，不得在目标主机直接执行源码构建
- 每个环境必须有独立的 `.env` 文件，并通过 `docker compose --env-file` 启动
- 目标主机目录必须固定，默认使用 `/opt/sybase/<service>`
- 发布与回滚必须基于镜像 tag 切换；默认回滚方式为“切回上一稳定 tag 并重新执行 `docker compose up -d`”
- 远程主机必须预装 Docker 与 Docker Compose，并具备仓库登录能力

### 6. 信创兼容门禁

- 新增基础镜像、系统包、脚本工具时，必须补充信创兼容说明
- 涉及镜像、脚本、JVM 参数、时区、字符集的调整，必须同步遵循 `XINCHUANG_RULES.md`
- 未完成信创兼容评估的镜像和脚本，不得直接进入候选发布流程

## 推荐实践

- 优先选择长期支持的基础镜像，并固定大版本
- 为镜像打上 `org.opencontainers.image.*` 标签，便于追溯来源
- 通过环境变量而不是修改镜像来区分 `test`、`staging`、`prod`
- 在部署模板中显式限制日志轮转，避免单容器日志无限增长
- 将镜像构建、推送、部署逻辑拆分到 `scripts/ci/`，减少 `.gitlab-ci.yml` 中的重复 Shell

## 反例/禁用项

- 把 `Dockerfile` 放到 `deploy/docker/` 并在里面复制服务源码
- 在运行镜像中使用 root 用户处理常规应用启动
- 将生产凭据写入镜像层、示例 `.env`、Git 历史或流水线脚本
- 在目标主机直接 `git pull && docker build`
- 没有健康检查和可观测性验证就宣称镜像可发布

## 检查清单

- [ ] 服务级 `Dockerfile` 与 `deploy/docker/` 职责边界清晰
- [ ] 镜像采用多阶段构建并使用 JDK/JRE 17 基线
- [ ] 运行镜像使用非 root 用户，时区与日志出口已明确
- [ ] 敏感信息通过环境变量或密钥平台注入
- [ ] `/actuator/health`、`/actuator/prometheus` 已在容器环境验证
- [ ] 已定义 `commit sha`、`branch slug`、`release tag` 三类镜像标签
- [ ] 新增基础镜像、系统包、脚本工具已完成信创兼容评估

## 关联文档

- [GITLAB_CI_RULES.md](./GITLAB_CI_RULES.md)
- [RELEASE.md](./RELEASE.md)
- [XINCHUANG_RULES.md](./XINCHUANG_RULES.md)
- [OBSERVABILITY_RULES.md](./OBSERVABILITY_RULES.md)
- [GIT_RULES.md](./GIT_RULES.md)
