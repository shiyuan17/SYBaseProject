# scripts

本目录统一存放项目脚本，按环境与用途拆分：

- `dev/`：本地开发启动、热重载与开发态 JAR 管理
- `prod/`：生产运行、配置模板与 SMB 同步发布
- `ci/`：CI/CD 与治理检查
- `database/`：数据库字典与导出
- `migration/`：Flyway 迁移辅助
- `hooks/`：Git Hooks 安装与校验
- `test/`：脚本级回归测试

## 目录分层约定

- `scripts/dev/windows/` 与 `scripts/dev/unix/`：按操作系统存放开发脚本
- `scripts/prod/windows/`、`scripts/prod/unix/`、`scripts/prod/config/`：分别存放 Windows 启动脚本、Unix 启动脚本与生产配置模板

## 推荐入口

- 本地开发：
  - Windows：`scripts/dev/windows/run-bl-center-dev.cmd`
  - Unix：`scripts/dev/unix/run-bl-center-dev.sh`
- 生产脚本：
  - Windows：`scripts/prod/windows/run-centers-prod-06.cmd`
  - Unix：`scripts/prod/unix/run-centers.sh`
  - 配置：`scripts/prod/config/run-centers.conf`

后续新增脚本请继续放到 `windows/`、`unix/`、`config/` 子目录，不再回到 `scripts/dev`、`scripts/prod` 根目录平铺。
