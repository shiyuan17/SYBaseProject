# deploy/docker

本目录用于容器镜像与 Docker 部署资源。

## 目录说明

- `docker-compose.observability.yml`：本地可观测性联调编排
- `gitlab/`：`GitLab CE` Docker Compose 启动模板
- `user-center/`：`user-center` Docker 主机部署模板

约定：

- 服务级 `Dockerfile` 放在各服务目录下
- `deploy/docker/` 只放部署编排、环境模板和说明文档
- GitLab CI/CD 通过 `scripts/ci/` 下的脚本调用本目录模板，不在远程主机本地构建源码

## 本地监控运行

可观测性本地运行编排文件：

- `docker-compose.observability.yml`

启动方式：

```bash
docker compose -f deploy/docker/docker-compose.observability.yml up --build
```

依赖前提：

- `user-center` 会在 compose 内直接构建并运行在容器端口 `8080`
- 宿主机默认映射到 `18080`
- `Prometheus` 会抓取 `http://user-center:8080/actuator/prometheus`
- `Loki` 默认地址为 `http://localhost:3100`
- `Grafana` 默认地址为 `http://localhost:3000`

端口覆盖：

- 如需改宿主机访问端口，可在启动前设置 `USER_CENTER_HOST_PORT`

默认账号：

- 用户名：`admin`
- 密码：`admin123`

## Docker 主机部署

`user-center` 标准模板位于：

- `deploy/docker/user-center/docker-compose.yml`
- `deploy/docker/user-center/.env`
- `deploy/docker/user-center/.env.local`

目标主机默认目录：

```text
/opt/sybase/user-center
```

启动方式：

```bash
docker compose --env-file /opt/sybase/user-center/.env -f /opt/sybase/user-center/docker-compose.yml pull
docker compose --env-file /opt/sybase/user-center/.env -f /opt/sybase/user-center/docker-compose.yml up -d
```

本地镜像测试可使用：

```bash
docker compose --env-file deploy/docker/user-center/.env.local -f deploy/docker/user-center/docker-compose.yml up -d
```

回滚方式：

- 将 `.env` 中的 `IMAGE_TAG` 切回上一稳定版本
- 重新执行 `docker compose up -d`

## GitLab 部署

`GitLab` 标准模板位于：

- `deploy/docker/gitlab/docker-compose.yml`
- `deploy/docker/gitlab/.env`
- `deploy/docker/gitlab/README.md`

标准启动命令：

```bash
docker compose --env-file deploy/docker/gitlab/.env -f deploy/docker/gitlab/docker-compose.yml up -d
```

本地默认访问地址：

- Web：`http://127.0.0.1:8929`
- SSH：`ssh://git@127.0.0.1:2424/<group>/<project>.git`
- Registry：`127.0.0.1:5050`
