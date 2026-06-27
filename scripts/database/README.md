# scripts/database

用于达梦整库备份和数据库字典生成的运维脚本。

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

`run-database-dictionary.cmd`
- Windows 下一键生成 `auth-center` / `bl-center` 真实达梦库的数据库字典 HTML
- 默认读取以下环境变量：
  - `AUTH_CENTER_DATASOURCE_DRIVER_CLASS_NAME`
  - `AUTH_CENTER_DATASOURCE_URL`
  - `AUTH_CENTER_DATASOURCE_USERNAME`
  - `AUTH_CENTER_DATASOURCE_PASSWORD`
  - `BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME`
  - `BL_CENTER_DATASOURCE_URL`
  - `BL_CENTER_DATASOURCE_USERNAME`
  - `BL_CENTER_DATASOURCE_PASSWORD`
  - `JAVA_HOME`
- 用法：
  - `scripts\database\run-database-dictionary.cmd`
  - `scripts\database\run-database-dictionary.cmd D:\reports\database-dictionary.html`
  - `scripts\database\run-database-dictionary.cmd D:\reports\database-dictionary.html --targets auth-center --scope visible-all`

`run-database-dictionary.sh`
- Linux/macOS/Git Bash 下一键生成同样的数据库字典 HTML
- 用法：
  - `./scripts/database/run-database-dictionary.sh`
  - `./scripts/database/run-database-dictionary.sh /data/reports/database-dictionary.html`
  - `./scripts/database/run-database-dictionary.sh /data/reports/database-dictionary.html --targets auth-center --scope visible-all`

`run-database-dictionary-legacy.cmd`
- Windows 下一键生成旧版 `DM数据表.html` 风格的数据库字典 HTML
- 默认输出到仓库 `docs/reports/DM数据表.html`
- 用法：
  - `scripts\database\run-database-dictionary-legacy.cmd`
  - `scripts\database\run-database-dictionary-legacy.cmd D:\reports\DM数据表.html`
  - `scripts\database\run-database-dictionary-legacy.cmd D:\reports\DM数据表.html --targets bl-center --scope visible-all`

`run-database-dictionary-legacy.sh`
- Linux/macOS/Git Bash 下一键生成同样的旧版数据库字典 HTML
- 默认输出到仓库 `docs/reports/DM数据表.html`
- 用法：
  - `./scripts/database/run-database-dictionary-legacy.sh`
  - `./scripts/database/run-database-dictionary-legacy.sh /data/reports/DM数据表.html`
  - `./scripts/database/run-database-dictionary-legacy.sh /data/reports/DM数据表.html --targets bl-center --scope visible-all`

说明
- 脚本依赖达梦原生导出工具 `dexp`，不通过 JDBC 手工导出 SQL。
- 无参数运行时，默认输出到 `tmp/db-export/<timestamp>/`。
- 如果某套数据源变量完全未设置，脚本会自动回落到本地默认达梦连接：`jdbc:dm://127.0.0.1:5236` / `SYSDBA` / `Dm.2027.Pwd.`。
- 如果 `auth-center` 与 `bl-center` 的 `url + username` 完全一致，脚本会自动去重，只导出一次，并在日志中说明该备份同时覆盖两者。
- 如果某套数据源变量只设置了部分字段，脚本会直接失败并指出缺失项。
- 任一唯一数据源导出失败时，脚本返回非零退出码；已成功生成的备份文件会保留。
- 这是整库备份脚本，不负责 Flyway 迁移、按表导出、结构对比、脱敏或业务报表导出。
- 数据库字典脚本通过 `tools/app-cli` 连接真实达梦库，默认生成 `docs/reports/database-dictionary-<timestamp>.html`。
- 旧版数据库字典脚本同样通过 `tools/app-cli` 连接真实达梦库，但默认覆盖输出到 `docs/reports/DM数据表.html`，用于保留历史版式。
- 如果两套服务连接的 `url + username` 完全一致，HTML 中会自动按同一真实库去重展示，并保留 `auth-center + bl-center` 来源标签。
- 数据库字典脚本会在执行前自动为 `auth-center` / `bl-center` 补齐本地默认达梦连接：`jdbc:dm://127.0.0.1:5236` / `SYSDBA` / `Dm.2027.Pwd.`。
- 数据库字典脚本依赖 `JAVA_HOME` 指向 JDK 17，并通过仓库根目录的 Maven Wrapper 构建运行。

常见失败场景
- `Unable to locate dexp`：请设置 `DM_EXPORT_TOOL`、`DM_HOME`，或把 `dexp` 加入 `PATH`
- `datasource configuration is incomplete`：请补齐对应服务的 URL、用户名、密码
- `Failed to convert JDBC URL`：当前只支持 `jdbc:dm://...` 形式的达梦 JDBC 地址
- `JAVA_HOME is not set`：请设置到 JDK 17，再执行数据库字典脚本
