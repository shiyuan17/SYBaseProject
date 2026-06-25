# scripts/database

用于达梦整库备份的运维脚本。

`run-database-export.cmd`
- Windows 下一键导出 `auth-center` / `bl-center` 关联的达梦整库备份
- 默认读取以下环境变量：
  - `AUTH_CENTER_DATASOURCE_URL`
  - `AUTH_CENTER_DATASOURCE_USERNAME`
  - `AUTH_CENTER_DATASOURCE_PASSWORD`
  - `BL_CENTER_DATASOURCE_URL`
  - `BL_CENTER_DATASOURCE_USERNAME`
  - `BL_CENTER_DATASOURCE_PASSWORD`
  - `DB_EXPORT_OUTPUT_DIR`：可选，覆盖默认输出目录
  - `DM_EXPORT_TOOL`：可选，显式指定 `dexp` 可执行文件
  - `DM_HOME`：可选，从 `${DM_HOME}/bin` 探测 `dexp`
- 用法：
  - `scripts\database\run-database-export.cmd`
  - `scripts\database\run-database-export.cmd D:\backup\dm-export`

`run-database-export.sh`
- Linux/macOS/Git Bash 下一键执行同样的整库备份
- 用法：
  - `./scripts/database/run-database-export.sh`
  - `./scripts/database/run-database-export.sh /data/backup/dm-export`

说明
- 脚本依赖达梦原生导出工具 `dexp`，不通过 JDBC 手工导出 SQL。
- 无参数运行时，默认输出到 `tmp/db-export/<timestamp>/`。
- 如果某套数据源变量完全未设置，脚本会自动回落到本地默认达梦连接：`jdbc:dm://127.0.0.1:5236` / `SYSDBA` / `Dm.2027.Pwd.`。
- 如果 `auth-center` 与 `bl-center` 的 `url + username` 完全一致，脚本会自动去重，只导出一次，并在日志中说明该备份同时覆盖两者。
- 如果某套数据源变量只设置了部分字段，脚本会直接失败并指出缺失项。
- 任一唯一数据源导出失败时，脚本返回非零退出码；已成功生成的备份文件会保留。
- 这是整库备份脚本，不负责 Flyway 迁移、按表导出、结构对比、脱敏或业务报表导出。

常见失败场景
- `Unable to locate dexp`：请设置 `DM_EXPORT_TOOL`、`DM_HOME`，或把 `dexp` 加入 `PATH`
- `datasource configuration is incomplete`：请补齐对应服务的 URL、用户名、密码
- `Failed to convert JDBC URL`：当前只支持 `jdbc:dm://...` 形式的达梦 JDBC 地址
