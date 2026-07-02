# scripts/prod

本目录用于生产环境相关脚本模板。

`run-centers.sh`
- 默认会读取同目录的 `run-centers.conf`
- 配置文件格式是 shell 变量赋值，例如 `SPRING_PROFILES_ACTIVE=prod`
- 仓库已提供默认的 `run-centers.conf`，可以直接按环境修改数据库和路径参数
- `run-centers.conf.example` 用于保留一份模板

最小示例：

```sh
./run-centers.sh start
```

`run-centers-prod-06.sh`
- `application-prod-06` 环境的快捷入口
- 默认先加载 `run-centers.conf` 里的共享路径和数据源配置，再把 `SPRING_PROFILES_ACTIVE` 覆盖成 `prod-06`
- 内部仍然复用 `run-centers.sh`，所以支持相同的动作：`start`、`stop`、`restart`、`status`、`log`

最小示例：

```sh
./run-centers-prod-06.sh start all
./run-centers-prod-06.sh restart all
./run-centers-prod-06.sh status all
./run-centers-prod-06.sh log bl -f
./run-centers-prod-06.sh log auth -f
```

Windows 可用原生脚本：

- `run-centers-prod-06.ps1`
- `run-centers-prod-06.cmd`

它们会读取同目录的 `run-centers.conf`，然后强制使用 `prod-06` profile。

- `run-centers-prod-06.cmd` 是 Windows 入口包装脚本，会转发到同目录下的 `run-centers-prod-06.ps1`

最小示例：

```powershell
.\run-centers-prod-06.ps1 start all
.\run-centers-prod-06.ps1 restart all
.\run-centers-prod-06.ps1 status all
.\run-centers-prod-06.ps1 log bl
.\run-centers-prod-06.cmd log auth -f
```

## SMB 同步发布脚本

`sync-centers-from-smb.sh`
- 默认从 `/media/fsuser-pc/smbmounts/smb-share:server=10.46.14.76,share=pacssoft/XC` 复制 `bl-center.jar` 和 `auth-center.jar`
- 默认复制到 `/data/home/fsuser-pc/XC/online/api`
- 复制完成后在目标目录执行 `./run-centers.sh restart all`
- 会输出带时间戳的详细日志，包含源目录、目标目录、文件复制结果和启动脚本执行结果

运行示例：

```sh
sh scripts/prod/sync-centers-from-smb.sh
```

`sync-web-from-smb.sh`
- 默认从同一 SMB 目录复制 `xc_web.zip`
- 默认复制到 `/data/home/fsuser-pc/XC/online/web`
- 解压成功后用压缩包中的 `xc_web` 目录替换目标目录下的旧 `xc_web`
- 兼容两种压缩包结构：包内包含顶层 `xc_web/` 目录，或包根目录直接就是前端文件
- 会输出带时间戳的详细日志，包含 `xc_web.zip` 复制、解压、`xc_web` 目录替换和最终同步结果

运行示例：

```sh
sh scripts/prod/sync-web-from-smb.sh
```

可覆盖环境变量：
- `SOURCE_DIR`
- `API_TARGET_DIR`
- `WEB_TARGET_DIR`
- `RUN_CENTERS_ACTION`，默认 `restart`
- `RUN_CENTERS_SERVICE`，默认 `all`
