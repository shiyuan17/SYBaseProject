# scripts/prod

本目录用于生产环境启动、配置模板与 SMB 同步发布。

## 目录说明

- `windows/`：Windows 实际实现脚本
- `unix/`：Unix 实际实现脚本
- `config/`：生产配置模板与环境覆盖文件

## 配置文件

推荐直接维护：

- `scripts/prod/config/run-centers.conf`：通用生产配置
- `scripts/prod/config/run-centers.conf.example`：模板示例
- `scripts/prod/config/run-centers-prod-06.conf`：`prod-06` 额外覆盖

配置文件格式为 shell 变量赋值，例如：

```sh
SPRING_PROFILES_ACTIVE=prod
BL_CENTER_DATASOURCE_URL=jdbc:dm://127.0.0.1:5236
```

## Unix 推荐入口

常用脚本：

- `scripts/prod/unix/run-centers.sh`
- `scripts/prod/unix/run-centers-prod-06.sh`
- `scripts/prod/unix/sync-centers-from-smb.sh`
- `scripts/prod/unix/sync-web-from-smb.sh`

示例：

```sh
./scripts/prod/unix/run-centers.sh start all
./scripts/prod/unix/run-centers-prod-06.sh restart all
./scripts/prod/unix/run-centers-prod-06.sh status all
./scripts/prod/unix/run-centers-prod-06.sh log bl -f
./scripts/prod/unix/sync-centers-from-smb.sh
./scripts/prod/unix/sync-web-from-smb.sh
```

## Windows 推荐入口

常用脚本：

- `scripts/prod/windows/run-centers-prod-06.cmd`
- `scripts/prod/windows/run-centers-prod-06.ps1`

示例：

```powershell
.\scripts\prod\windows\run-centers-prod-06.cmd start all
.\scripts\prod\windows\run-centers-prod-06.cmd restart all
.\scripts\prod\windows\run-centers-prod-06.cmd status all
.\scripts\prod\windows\run-centers-prod-06.cmd log bl
.\scripts\prod\windows\run-centers-prod-06.ps1 log auth -f
```

## 启动与日志约定

- `run-centers.sh` 默认读取 `scripts/prod/config/run-centers.conf`
- `run-centers-prod-06.sh` 会先加载通用配置，再叠加 `scripts/prod/config/run-centers-prod-06.conf`
- 支持动作：`start`、`stop`、`pause`、`resume`、`restart`、`status`、`log`
- 默认日志与 PID 写入 `scripts/prod/` 根目录，可通过 `LOG_DIR`、`RUNTIME_DIR` 覆盖

## SMB 同步发布脚本

`sync-centers-from-smb.sh`

- 默认从 `/media/fsuser-pc/smbmounts/smb-share:server=10.46.14.76,share=pacssoft/XC` 复制 `bl-center.jar` 和 `auth-center.jar`
- 默认复制到 `/data/home/fsuser-pc/XC/online/api`
- 复制完成后在目标目录执行 `./run-centers.sh restart all`
- 会输出带时间戳的详细日志，包含源目录、目标目录、文件复制结果和启动脚本执行结果

`sync-web-from-smb.sh`

- 默认从同一 SMB 目录复制 `xc_web.zip`
- 默认复制到 `/data/home/fsuser-pc/XC/online/web`
- 解压成功后用压缩包中的 `xc_web` 目录替换目标目录下的旧 `xc_web`
- 兼容两种压缩包结构：包内包含顶层 `xc_web/` 目录，或包根目录直接就是前端文件
- 会输出带时间戳的详细日志，包含 `xc_web.zip` 复制、解压、目录替换和最终同步结果

## 可覆盖环境变量

- `SOURCE_DIR`
- `API_TARGET_DIR`
- `WEB_TARGET_DIR`
- `RUN_CENTERS_ACTION`，默认 `restart`
- `RUN_CENTERS_SERVICE`，默认 `all`
- `RUN_CENTERS_CONFIG_FILE`
- `RUN_CENTERS_BASE_CONFIG_FILE`
- `RUN_CENTERS_PROD06_CONFIG_FILE`
