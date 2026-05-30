# scripts/ci

本目录用于持续集成与持续部署脚本模板。

当前约定：

- `build-user-center-image.sh`：构建并推送 `user-center` 镜像
- `deploy-user-center.sh`：通过 SSH 在 Docker 主机拉取镜像并执行 `docker compose up -d`
- `build-bl-center-image.sh`：构建并推送 `bl-center` 镜像
- `deploy-bl-center.sh`：通过 SSH 在 Docker 主机拉取镜像并执行 `docker compose up -d`
- `deploy-gitlab-runner.sh`：通过 SSH 下发 `GitLab Runner` compose 模板并完成非交互注册
- `run-m1-m5-api-regression.sh`：按 M1-M5 分组执行接口自动化回归，并输出中文 Markdown 报告
- `generate-largest-files-report.sh/.ps1`：生成仓库大文件与豁免报告
- `generate-code-health-checklist.sh/.ps1`：运行健康度门禁、热点采样并输出代码健康度清单

设计要求：

- 优先写成可被 GitLab CI 与本地手工执行复用的 POSIX Shell
- 复杂逻辑下沉到脚本中，避免直接堆在 `.gitlab-ci.yml`
- 任何新增镜像、系统包、脚本工具的变更都要补信创兼容说明

## M1-M5 回归产物

- GitLab CI 任务：`verify_m1_m5_api_regression`
- Markdown 报告：`docs/reports/m1-m5-api-test-report-YYYYMMDD.md`
- Surefire 原始结果：
  - `auth-center/target/surefire-reports`
  - `user-center/target/surefire-reports`
  - `bl-center/target/surefire-reports`

本地可直接执行：

```bash
bash scripts/ci/run-m1-m5-api-regression.sh
```

如需仅基于现有 Surefire XML 重生报告，可执行：

```bash
bash scripts/ci/run-m1-m5-api-regression.sh --skip-execution --report-date 20260521
```
