# GITLAB_LOCAL_TEST_FLOW.md — 本地 GitLab 开发环境与开发服务器测试环境操作手册

## 目标

本手册用于指导以下部署拓扑：

- `local`：开发者本机上的 GitLab、GitLab Runner 与 `user-center` 本地联调环境
- `test`：开发服务器上的共享测试环境

日常发布链路为：

1. 本机启动并维护本地 GitLab
2. 本机部署并注册 GitLab Runner
3. 提交到 `develop` 后自动执行 `verify`、`package`、`image`
4. 同一条流水线自动部署到本机 `local`
5. 本机联调通过后，手动触发 `deploy_test`
6. `deploy_test` 将相同镜像 tag 部署到开发服务器

## 一次性初始化

### 1. 本机准备基础环境

安装以下依赖：

- Docker
- Docker Compose
- OpenSSH Client
- OpenSSH Server

检查命令：

```bash
docker version
docker compose version
ssh 127.0.0.1
```

要求：

- 本机 `ssh` 到自己可连通
- 当前登录用户具备 Docker 使用权限
- 本机预留 GitLab Web、SSH、Registry 所需端口

### 2. 本机部署 GitLab

使用仓库内模板启动：

```bash
mkdir -p /Users/hsy/Documents/GitLab/config /Users/hsy/Documents/GitLab/logs /Users/hsy/Documents/GitLab/data
docker compose --env-file deploy/docker/gitlab/.env -f deploy/docker/gitlab/docker-compose.yml up -d
docker compose --env-file deploy/docker/gitlab/.env -f deploy/docker/gitlab/docker-compose.yml logs -f gitlab
```

初始化完成后，读取 root 初始密码：

```bash
docker compose --env-file deploy/docker/gitlab/.env -f deploy/docker/gitlab/docker-compose.yml exec -T gitlab \
  grep 'Password:' /etc/gitlab/initial_root_password
```

建议固定以下本地访问信息：

- GitLab Web：`http://127.0.0.1:8929`
- GitLab SSH：`127.0.0.1:2424`
- GitLab Registry：`127.0.0.1:5050`

如 GitLab 未启用内置 Container Registry，不要继续后续 CI 部署链路，需先补齐本地镜像仓库能力。

首次登录后，继续完成：

1. 使用 root 登录 GitLab Web
2. 在 `Admin Area` 中确认 Container Registry 已启用
3. 创建当前项目仓库，将本仓库代码推送到本地 GitLab
4. 执行本地 Registry 登录验证

Registry 登录验证：

```bash
docker login 127.0.0.1:5050
curl -I http://127.0.0.1:5050/v2/
```

### 3. 本机创建 GitLab Runner

在本地 GitLab 项目或组内创建 Runner，获取 `glrt-` 开头的 authentication token。

要求：

- GitLab Web 已可正常访问
- Container Registry 已可正常登录
- 本地 GitLab 地址与 `deploy/docker/gitlab/.env` 中 `GITLAB_EXTERNAL_URL` 保持一致

执行部署：

```bash
DEPLOY_HOST=127.0.0.1 \
DEPLOY_USER="$USER" \
DEPLOY_APP_DIR=/opt/sybase/gitlab-runner \
GITLAB_URL=http://127.0.0.1:8929 \
RUNNER_AUTH_TOKEN=glrt-xxxxxxxxxxxxxxxxxxxx \
RUNNER_NAME=sybase-local-docker-runner \
RUNNER_DOCKER_IMAGE=alpine:3.20 \
scripts/ci/deploy-gitlab-runner.sh
```

验证：

```bash
cd /opt/sybase/gitlab-runner
docker compose --env-file .env ps
docker compose exec -T gitlab-runner gitlab-runner verify
```

通过标准：

- GitLab UI 中 Runner 状态显示为 `online`
- Runner 能领取项目 job

### 4. 配置 GitLab Variables

在项目 `Settings -> CI/CD -> Variables` 中录入以下变量。

本地 `local` 环境变量：

- `DEPLOY_LOCAL_HOST=127.0.0.1`
- `DEPLOY_LOCAL_USER=<本机登录用户>`
- `DEPLOY_LOCAL_SSH_KEY=<本机可登录自己的私钥>`
- `DEPLOY_LOCAL_APP_DIR=/opt/sybase/user-center`
- `DEPLOY_LOCAL_PORT=8080`
- `DEPLOY_LOCAL_SPRING_PROFILE=local`

测试 `test` 环境变量：

- `DEPLOY_TEST_HOST=<开发服务器地址>`
- `DEPLOY_TEST_USER=<部署用户>`
- `DEPLOY_TEST_SSH_KEY=<开发服务器 SSH 私钥>`
- `DEPLOY_TEST_APP_DIR=/opt/sybase/user-center`
- `DEPLOY_TEST_PORT=8080`
- `DEPLOY_TEST_SPRING_PROFILE=test`

Runner 部署变量：

- `RUNNER_DEPLOY_HOST=127.0.0.1`
- `RUNNER_DEPLOY_USER=<本机登录用户>`
- `RUNNER_DEPLOY_APP_DIR=/opt/sybase/gitlab-runner`
- `RUNNER_GITLAB_URL=http://127.0.0.1:8929`
- `RUNNER_AUTH_TOKEN=<GitLab Runner token>`
- `RUNNER_NAME=sybase-local-docker-runner`

配置要求：

- SSH 私钥、token、密码类变量必须设置为 `Masked`
- 测试及以上环境敏感变量建议设置为 `Protected`
- 本地开发专用变量可不设为 `Protected`

### 5. 准备本机 local 目标目录

首次部署前检查：

```bash
mkdir -p /opt/sybase/user-center
ssh 127.0.0.1 "mkdir -p /opt/sybase/user-center"
```

如果本机 SSH 使用非默认端口或用户名，需要同步调整 GitLab Variables 或 `SSH_OPTS`。

## 日常开发发布流程

### 1. 提交 develop 分支

开发者将代码推送到 `develop` 后，GitLab 自动执行：

1. `verify`
2. `package_user_center`
3. `image_user_center`
4. `deploy_local`

镜像 tag 规则：

- 本地与测试环境默认使用 `CI_COMMIT_REF_SLUG`
- 生产仍使用 `CI_COMMIT_TAG`

### 2. 自动部署到本机 local

`deploy_local` 会执行以下动作：

1. 通过 SSH 登录本机
2. 生成本机 `.env`
3. 下发 `deploy/docker/user-center/docker-compose.yml`
4. 登录本地 GitLab Registry
5. 执行 `docker compose pull`
6. 执行 `docker compose up -d --remove-orphans`
7. 输出 `docker compose ps`

联调验证命令：

```bash
curl -f http://127.0.0.1:8080/actuator/health
docker ps --filter "name=sybase-user-center"
```

### 3. 本地联调验收

本地至少完成以下检查：

- `/actuator/health` 返回成功
- 创建用户、查询用户等核心接口 smoke test
- 容器日志无明显启动错误
- `SPRING_PROFILES_ACTIVE=local` 生效
- 端口映射符合预期

如本地联调失败，不要推进 `test`，应直接修复并重新提交 `develop`。

### 4. 手动推进到 test

当 `deploy_local` 通过且本地联调完成后，在同一条 `develop` 流水线中手动点击 `deploy_test`。

`deploy_test` 会：

1. 通过 SSH 登录开发服务器
2. 生成开发服务器 `.env`
3. 登录 GitLab Registry
4. 拉取同一 `CI_COMMIT_REF_SLUG` 对应镜像
5. 执行 `docker compose up -d --remove-orphans`

### 5. 测试环境验收

在开发服务器至少验证：

- `curl -f http://<test-host>:<port>/actuator/health`
- 核心接口 smoke test
- `docker compose ps`
- 应用日志与启动参数
- 当前部署镜像 tag 已记录

## 回滚与重试

### local 回滚

在本机执行：

1. 修改 `${DEPLOY_LOCAL_APP_DIR}/.env` 中的 `IMAGE_TAG`
2. 切回上一稳定 tag
3. 执行 `docker compose --env-file .env up -d`

### test 回滚

在开发服务器执行：

1. 修改 `${DEPLOY_TEST_APP_DIR}/.env` 中的 `IMAGE_TAG`
2. 切回上一稳定 tag
3. 执行 `docker compose --env-file .env up -d`

### Runner 重注册

当 Runner token 变更或需要刷新注册期配置时执行：

```bash
DEPLOY_HOST=127.0.0.1 \
DEPLOY_USER="$USER" \
DEPLOY_APP_DIR=/opt/sybase/gitlab-runner \
GITLAB_URL=http://127.0.0.1:8929 \
RUNNER_AUTH_TOKEN=glrt-xxxxxxxxxxxxxxxxxxxx \
RUNNER_NAME=sybase-local-docker-runner \
FORCE_RE_REGISTER=1 \
scripts/ci/deploy-gitlab-runner.sh
```

## 故障排查

- Runner 离线：
  - 检查 `docker compose logs gitlab-runner`
  - 检查 GitLab Web 地址与 token 是否仍有效
- 本地部署失败：
  - 检查本机 SSH 是否可自连
  - 检查本机是否能登录 GitLab Registry
- 测试环境部署失败：
  - 检查开发服务器 SSH、Docker、Registry 登录能力
  - 检查开发服务器上 `.env` 中的镜像 tag 与仓库 tag 是否一致

## 关联文档

- [GITLAB_CI_RULES.md](../rules/GITLAB_CI_RULES.md)
- [RELEASE.md](../rules/RELEASE.md)
- [../../deploy/docker/gitlab/README.md](../../deploy/docker/gitlab/README.md)
- [../../deploy/docker/README.md](../../deploy/docker/README.md)
