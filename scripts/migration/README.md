# scripts/migration

用于数据库迁移辅助脚本。

`run-bl-center-flyway.cmd`
- Windows 下一键执行 `bl-center` 的 Flyway `repair + migrate`
- 默认连接本地达梦：
  - `BL_CENTER_DATASOURCE_URL=jdbc:dm://127.0.0.1:5236`
  - `BL_CENTER_DATASOURCE_USERNAME=SYSDBA`
  - `BL_CENTER_DATASOURCE_PASSWORD=Dm.2027.Pwd.`

`run-bl-center-flyway.sh`
- Linux/macOS 下一键执行同样的 Flyway 同步

说明
- `bl-center` 正常启动默认不执行 Flyway。
- 需要同步数据库时，再单独运行这里的脚本。
- 脚本会先执行 `repair`，再执行 `migrate`。
- 针对已知的本地达梦异常状态，如果检测到 `V12` 失败且 M4 残留表为空，会自动清理后再继续同步。
