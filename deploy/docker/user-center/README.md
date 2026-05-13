# deploy/docker/user-center

本目录用于 `user-center` 在 Docker 主机上的标准部署模板。

## 目录内容

- `docker-compose.yml`：远程主机运行编排模板
- `.env`：Docker 主机部署参数示例
- `env.local`：本地镜像联调参数示例

## 目录约定

推荐每个环境在目标主机固定使用以下目录：

```text
/opt/sybase/user-center
├── docker-compose.yml
├── .env
└── data/
```

说明：

- `docker-compose.yml` 由仓库模板下发
- 目标主机上的 `.env` 由 GitLab CI 根据环境变量生成
- 仓库内的 `.env` 使用占位镜像地址，仅作为模板示例
- `data/` 预留给应用持久化文件或后续挂载使用

## 关键变量

- `IMAGE_REPO`：镜像仓库地址，默认指向 GitLab Container Registry
- `IMAGE_TAG`：部署目标 tag，可取 `commit sha`、`branch slug`、`release tag`
- `HOST_PORT`：宿主机对外暴露端口
- `SERVER_PORT`：容器内 Spring Boot 监听端口
- `SPRING_PROFILES_ACTIVE`：运行环境 profile
- `JAVA_OPTS`：JVM 参数
- `TZ`：容器时区

## 手工部署示例

```bash
cp deploy/docker/user-center/.env /opt/sybase/user-center/.env
docker login registry.example.com
docker compose --env-file /opt/sybase/user-center/.env -f deploy/docker/user-center/docker-compose.yml pull
docker compose --env-file /opt/sybase/user-center/.env -f deploy/docker/user-center/docker-compose.yml up -d
```

注意：

- `deploy/docker/user-center/.env` 中的 `registry.example.com` 是示例地址，使用前需要替换为真实镜像仓库

## 本地镜像测试

先在仓库根目录构建本地镜像：

```bash
docker build -f user-center/Dockerfile -t sybase-user-center:local .
```

再使用 `env.local` 启动：

```bash
cd deploy/docker/user-center
docker compose --env-file .env.local -f docker-compose.yml up -d
```

## 回滚方式

1. 将 `.env` 中的 `IMAGE_TAG` 切回上一稳定版本
2. 重新执行 `docker compose up -d`
3. 验证 `/actuator/health`、核心接口与监控状态
