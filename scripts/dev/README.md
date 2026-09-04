# scripts/dev

本目录用于本地开发启动、热重载与开发态 JAR 管理。

## 目录说明

- `windows/`：Windows 实际实现脚本
- `unix/`：Unix / macOS 实际实现脚本

## Windows 推荐入口

常用脚本：

- `scripts/dev/windows/run-bl-center-dev.cmd`：启动 `bl-center`
- `scripts/dev/windows/run-auth-center-dev.cmd`：启动 `auth-center`
- `scripts/dev/windows/run-centers-dev.cmd`：一次性启动 `bl-center` 与 `auth-center`
- `scripts/dev/windows/run-centers-dev.ps1`：`run-centers-dev.cmd` 对应的 PowerShell 实现
- `scripts/dev/windows/rebuild-and-clean-report-ofds.cmd`：仅限 localhost DM 开发库，重建新版报告快照并清理全部 OFD

示例：

```powershell
.\scripts\dev\windows\run-bl-center-dev.cmd
.\scripts\dev\windows\run-auth-center-dev.cmd
.\scripts\dev\windows\run-centers-dev.cmd
.\scripts\dev\windows\rebuild-and-clean-report-ofds.cmd
```

## Unix 推荐入口

常用脚本：

- `scripts/dev/unix/run-bl-center-dev.sh`：启动 `bl-center`
- `scripts/dev/unix/run-auth-center-dev.sh`：启动 `auth-center`
- `scripts/dev/unix/run-bl-center-jar.sh` / `run-auth-center-jar.sh`：管理已构建 JAR 进程

示例：

```bash
./scripts/dev/unix/run-bl-center-dev.sh
./scripts/dev/unix/run-auth-center-dev.sh
./scripts/dev/unix/run-bl-center-jar.sh status
./scripts/dev/unix/run-auth-center-jar.sh -ky status
./scripts/dev/unix/run-auth-center-jar.sh -prod-06 status
./scripts/dev/unix/run-bl-center-jar.sh -local status
```

## 启动行为

- 所有开发启动脚本都会先执行一次 `-pl <module> -am compile`
- 运行时统一使用仓库内 Maven 缓存：`-Dmaven.repo.local=.m2/repository`
- 会把 `common/*/target/classes` 加入运行时 classpath，便于共享模块热更新
- 后台热重载监听器会在源码变更后重新编译，并触发 Spring Boot DevTools 重启

## 日志与运行文件

- 热重载与启动输出默认追加到 `.logs/backend.log`
- 如果保存后编译失败，优先查看 `.logs/backend.log`
- JAR 启动脚本默认从脚本同目录读取 `bl-center.jar`、`auth-center.jar`
- JAR 启动脚本默认把日志写到脚本目录下的 `log/`，例如 `scripts/dev/unix/log/bl-center.log`
- JAR 启动器的 PID 默认写在脚本所在目录，可通过 `BL_CENTER_RUNTIME_DIR`、`AUTH_CENTER_RUNTIME_DIR` 覆盖
- JAR 启动脚本支持通过 `-<profile>` 或 `--profile <profile>` 指定 Spring Profile，例如 `./run-auth-center-jar.sh -ky` 或 `./run-bl-center-jar.sh --profile ky status`
- 常见环境示例：`-local`、`-dev`、`-ky`、`-prod`、`-prod-06`

## 常见环境变量

- `JAVA_HOME`：必须指向 JDK 17
- `BL_CENTER_DATASOURCE_*`：覆盖 `bl-center` 本地数据源
- `AUTH_CENTER_DATASOURCE_*`：覆盖 `auth-center` 本地数据源
- `SECURITY_AUTH_JWT_SM2_PRIVATE_KEY`
- `SECURITY_AUTH_JWT_SM2_PUBLIC_KEY`

## IntelliJ 热重载

- `bl-center` 与 `auth-center` 已包含 `spring-boot-devtools`
- 建议使用共享的 `BlCenter Dev` / `AuthCenter Dev` 运行配置
- 运行中开启 IntelliJ 自动构建后，保存文件即可刷新 `target/classes` 并触发重启
- 若未开启自动构建，也可以通过手动 `Build Project` 触发重启
