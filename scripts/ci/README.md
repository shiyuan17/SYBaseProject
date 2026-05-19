# scripts/ci

本目录用于持续集成与持续部署脚本模板。

当前约定：

- `build-user-center-image.sh`：构建并推送 `user-center` 镜像
- `deploy-user-center.sh`：通过 SSH 在 Docker 主机拉取镜像并执行 `docker compose up -d`
- `build-bl-center-image.sh`：构建并推送 `bl-center` 镜像
- `deploy-bl-center.sh`：通过 SSH 在 Docker 主机拉取镜像并执行 `docker compose up -d`
- `deploy-gitlab-runner.sh`：通过 SSH 下发 `GitLab Runner` compose 模板并完成非交互注册

设计要求：

- 优先写成可被 GitLab CI 与本地手工执行复用的 POSIX Shell
- 复杂逻辑下沉到脚本中，避免直接堆在 `.gitlab-ci.yml`
- 任何新增镜像、系统包、脚本工具的变更都要补信创兼容说明
