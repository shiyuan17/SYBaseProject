# deploy/docker/gitlab

本目录用于通过 `docker compose` 启动 `GitLab CE`，同时覆盖以下能力：

- GitLab Web
- Git over SSH
- GitLab Container Registry

模板既可用于开发者本机，也可用于普通 Linux 服务器；默认值优先适配本地联调。

## 目录内容

- `docker-compose.yml`：GitLab 单容器 Omnibus 编排模板
- `.env`：本地默认启动参数示例

## 目录约定

推荐每个环境在目标主机固定使用以下目录：

```text
/opt/sybase/gitlab
├── config/
├── logs/
└── data/
```

说明：

- `config/` 持久化 `/etc/gitlab`
- `logs/` 持久化 `/var/log/gitlab`
- `data/` 持久化 `/var/opt/gitlab`
- root 初始密码不写入仓库模板，首次启动后从容器内读取

## 关键变量

- `GITLAB_IMAGE_TAG`：GitLab CE 镜像版本
- `GITLAB_CONTAINER_NAME`：容器名称
- `GITLAB_HOSTNAME`：容器 hostname
- `GITLAB_EXTERNAL_URL`：GitLab Web 对外访问地址
- `GITLAB_HTTP_PORT`：宿主机 HTTP 端口
- `GITLAB_HTTPS_PORT`：宿主机 HTTPS 端口
- `GITLAB_SSH_PORT`：宿主机 SSH 端口
- `GITLAB_REGISTRY_PORT`：宿主机 Registry 端口
- `GITLAB_REGISTRY_EXTERNAL_URL`：Registry 对外访问地址
- `GITLAB_CONFIG_DIR`：宿主机配置目录
- `GITLAB_LOGS_DIR`：宿主机日志目录
- `GITLAB_DATA_DIR`：宿主机数据目录
- `GITLAB_SHM_SIZE`：共享内存大小
- `GITLAB_TIMEZONE`：GitLab 时区

## 本地开发示例

首次启动：

```bash
mkdir -p /Users/hsy/Documents/GitLab/config /Users/hsy/Documents/GitLab/logs /Users/hsy/Documents/GitLab/data
docker compose --env-file deploy/docker/gitlab/.env -f deploy/docker/gitlab/docker-compose.yml up -d
docker compose --env-file deploy/docker/gitlab/.env -f deploy/docker/gitlab/docker-compose.yml logs -f gitlab
```

等待日志中不再持续出现初始化失败信息后，读取 root 初始密码：

```bash
docker compose --env-file deploy/docker/gitlab/.env -f deploy/docker/gitlab/docker-compose.yml exec -T gitlab \
  grep 'Password:' /etc/gitlab/initial_root_password
```

本地默认访问地址：

- Web：`http://127.0.0.1:8929`
- SSH：`ssh://git@127.0.0.1:2424/<group>/<project>.git`
- Registry：`127.0.0.1:5050`

Registry 登录验证：

```bash
docker login 127.0.0.1:5050
curl -I http://127.0.0.1:5050/v2/
```

提示：

- `/etc/gitlab/initial_root_password` 只在首次初始化后一段时间内可用，建议首次启动成功后立即保存
- 如本机已经占用了 `8929`、`2424` 或 `5050`，可直接修改 `.env`

## 服务器部署示例

服务器部署时，复制 `.env` 并按实际域名、端口和目录调整：

```bash
mkdir -p /srv/gitlab/config /srv/gitlab/logs /srv/gitlab/data
cp deploy/docker/gitlab/.env /srv/gitlab/.env
```

建议至少覆盖以下变量：

- `GITLAB_HOSTNAME=gitlab.example.com`
- `GITLAB_EXTERNAL_URL=http://gitlab.example.com:8929`
- `GITLAB_SSH_PORT=22` 或真实映射端口
- `GITLAB_REGISTRY_EXTERNAL_URL=http://gitlab.example.com:5050`
- `GITLAB_CONFIG_DIR=/srv/gitlab/config`
- `GITLAB_LOGS_DIR=/srv/gitlab/logs`
- `GITLAB_DATA_DIR=/srv/gitlab/data`

启动：

```bash
docker compose --env-file /srv/gitlab/.env -f deploy/docker/gitlab/docker-compose.yml up -d
```

## 项目创建与 Registry 启用

1. 使用 root 登录 GitLab Web
2. 创建 group / project
3. 在项目中确认 `Packages and registries -> Container Registry` 可用
4. 为 CI/CD 准备项目变量、访问令牌和 Runner

## 升级、停止与排障

升级：

1. 修改 `.env` 中的 `GITLAB_IMAGE_TAG`
2. 执行 `docker compose --env-file ... pull`
3. 执行 `docker compose --env-file ... up -d`

停止：

```bash
docker compose --env-file deploy/docker/gitlab/.env -f deploy/docker/gitlab/docker-compose.yml stop
```

排障：

- 查看启动日志：`docker compose --env-file deploy/docker/gitlab/.env -f deploy/docker/gitlab/docker-compose.yml logs -f gitlab`
- 查看服务状态：`docker compose --env-file deploy/docker/gitlab/.env -f deploy/docker/gitlab/docker-compose.yml ps`
- 进入容器：`docker compose --env-file deploy/docker/gitlab/.env -f deploy/docker/gitlab/docker-compose.yml exec gitlab bash`
- 若 Registry 无法访问，优先检查 `GITLAB_REGISTRY_EXTERNAL_URL`、`GITLAB_REGISTRY_PORT` 与宿主机防火墙
