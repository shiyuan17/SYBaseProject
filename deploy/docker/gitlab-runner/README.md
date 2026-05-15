# deploy/docker/gitlab-runner

本目录用于 `GitLab Runner` 在 Docker 主机上的标准部署模板。

## 目录内容

- `docker-compose.yml`：远程主机运行编排模板
- `.env`：Docker 主机部署参数示例
- `config.template.toml`：Runner 注册模板

## 目录约定

推荐每个环境在目标主机固定使用以下目录：

```text
/opt/sybase/gitlab-runner
├── docker-compose.yml
├── .env
├── config.template.toml
├── cache/
└── config/
```

说明：

- `docker-compose.yml` 与 `config.template.toml` 由仓库模板下发
- 目标主机上的 `.env` 由 GitLab CI 或手工脚本生成
- 仓库内的 `.env` 仅作为非敏感示例，不包含真实 token
- `config/` 持久化 `/etc/gitlab-runner/config.toml`
- `cache/` 持久化 Docker executor 的 Runner 缓存

## 关键变量

- `RUNNER_IMAGE`：Runner 容器镜像，默认使用 `gitlab/gitlab-runner:latest`
- `RUNNER_CONTAINER_NAME`：容器名称
- `TZ`：容器时区
- `DOCKER_SOCKET_PATH`：宿主机 Docker Socket 路径
- `RUNNER_CONFIG_DIR`：宿主机 Runner 配置目录
- `RUNNER_CACHE_DIR`：宿主机 Runner 缓存目录

以下变量由部署脚本消费，不写入仓库示例 `.env`：

- `DEPLOY_HOST`
- `DEPLOY_USER`
- `DEPLOY_APP_DIR`
- `GITLAB_URL`
- `RUNNER_AUTH_TOKEN`
- `RUNNER_NAME`
- `RUNNER_EXECUTOR`
- `RUNNER_DOCKER_IMAGE`
- `FORCE_RE_REGISTER`：可选，设为 `1` 时强制重注册并刷新 Runner 配置

## 手工部署示例

```bash
mkdir -p /opt/sybase/gitlab-runner/config /opt/sybase/gitlab-runner/cache
cp deploy/docker/gitlab-runner/.env /opt/sybase/gitlab-runner/.env
cp deploy/docker/gitlab-runner/docker-compose.yml /opt/sybase/gitlab-runner/docker-compose.yml
cp deploy/docker/gitlab-runner/config.template.toml /opt/sybase/gitlab-runner/config.template.toml

cd /opt/sybase/gitlab-runner
docker compose --env-file .env up -d
docker compose exec -T gitlab-runner gitlab-runner register \
  --non-interactive \
  --template-config /opt/gitlab-runner/config.template.toml \
  --url "https://gitlab.example.com" \
  --token "glrt-xxxxxxxxxxxxxxxxxxxx" \
  --name "sybase-docker-runner" \
  --executor "docker" \
  --docker-image "alpine:3.20"
docker compose restart gitlab-runner
```

## 升级与重注册

升级 Runner 镜像：

1. 修改 `.env` 中的 `RUNNER_IMAGE`
2. 执行 `docker compose --env-file .env up -d`
3. 执行 `docker compose ps` 确认容器正常

更换 token 或强制重注册：

1. 更新部署变量中的 `RUNNER_AUTH_TOKEN`
2. 如需刷新 `RUNNER_DOCKER_IMAGE` 等注册期配置，额外设置 `FORCE_RE_REGISTER=1`
3. 重新执行 `scripts/ci/deploy-gitlab-runner.sh`
4. 如 GitLab UI 中残留旧 Runner 记录，手工删除失效条目

## 自签证书说明

如 GitLab 使用自签证书，可额外挂载证书目录到容器内，并在 Runner 配置中补充证书信任。首版模板未默认启用该挂载，避免把证书路径策略写死。

## 排障建议

- `docker compose logs gitlab-runner`：查看 Runner 启动与注册日志
- `docker compose exec -T gitlab-runner gitlab-runner verify`：校验 Runner 在线状态
- 确认宿主机 `DOCKER_SOCKET_PATH` 存在且当前部署用户可访问
- 确认 `RUNNER_AUTH_TOKEN` 来自 GitLab 新版 Runner 创建流程，而不是 legacy registration token
