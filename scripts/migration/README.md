# scripts/migration

用于数据库迁移辅助脚本。

`run-bl-center-flyway.cmd`
- Windows 下一键执行 `bl-center` 的 Flyway 检查/同步
- 默认连接本地达梦：
  - `BL_CENTER_DATASOURCE_URL=jdbc:dm://127.0.0.1:5236`
  - `BL_CENTER_DATASOURCE_USERNAME=SYSDBA`
  - `BL_CENTER_DATASOURCE_PASSWORD=Dm.2027.Pwd.`
- 用法：
  - `run-bl-center-flyway.cmd` 或 `run-bl-center-flyway.cmd sync`
  - `run-bl-center-flyway.cmd inspect`

`run-bl-center-flyway.sh`
- Linux/macOS 下一键执行同样的 Flyway 检查/同步
- 用法：
  - `./scripts/migration/run-bl-center-flyway.sh` 或 `./scripts/migration/run-bl-center-flyway.sh sync`
  - `./scripts/migration/run-bl-center-flyway.sh inspect`

说明
- `bl-center` 正常启动默认不执行 Flyway。
- 需要同步数据库时，再单独运行这里的脚本。
- 脚本默认执行 `sync`，也可以显式传 `inspect`。
- `inspect` 只检查，不改库；会输出连接状态、已安装版本、待执行迁移、校验结果，以及未纳入当前 `bl-center` Flyway 范围的历史/规划表。
- `sync` 会执行 `inspect -> repair -> migrate -> inspect`，并返回明确退出码，便于本地和 CI 一键接入。
- 针对已知的本地达梦异常状态，如果检测到 `V12` 失败且 M4 残留表为空，会自动清理后再继续同步。
- 如果本地达梦库没有 `flyway_schema_history`，但只缺少部分 M1/M4 表，不要直接按 `baselineOnMigrate` 接管这类半初始化库。
- 遇到这类开发库时，先按现有 DDL 补齐缺失表或依赖，再决定是否用 Flyway 脚本做完整纳管，避免把不完整旧库错误标记为已完成基线。
