# CLI_RULES.md — CLI 应用规范

## 目标与适用范围

本文件定义本项目中 Java CLI 应用的实现规范、职责边界与测试要求。

- 适用对象：后端开发者、运维开发者、AI 编码助手、代码审查人员
- 适用范围：离线工具、管理命令、批处理命令、诊断命令、命令行样板应用
- 规范定位：统一 CLI 应用技术基线与实现方式；Java 通用细则以 `JAVA_RULES.md` 为准

## 强制规则

### 1. 技术基线

- CLI 技术栈统一为 `Picocli + Spring Boot + Slf4j + Logback`
- CLI 应用默认使用独立模块承载，不得直接把命令入口混入现有 Web 服务主启动类
- CLI 必须优先复用 `application` 层能力，不得绕过应用服务直连 `repository`、`mapper` 或外部系统 SDK
- CLI 启动默认采用非 Web 模式，不暴露 HTTP 端口

### 2. 命令组织与命名

- 根命令必须使用清晰、稳定的英文名，例如 `sybase`
- 子命令统一采用 `verb` 或 `resource verb` 风格，例如 `version`、`health`、`user create`
- 参数名统一使用长选项形式，如 `--name`、`--email`、`--output`
- 帮助信息必须包含命令说明、参数说明、默认值和示例

### 3. 分层与职责边界

- 命令类只负责参数解析、调用应用服务、结果渲染和退出码映射
- 业务规则必须继续放在 `application` / `domain`
- CLI 输出模型不得直接复用 HTTP `ApiResponse`
- Web 层对象如 `HttpServletRequest`、Controller DTO、Filter、`RestControllerAdvice` 不得进入 CLI 依赖链

### 4. 输出、日志与退出码

- 标准输出用于命令结果
- 标准错误用于参数错误、业务错误和系统错误提示
- 日志仍统一走 `Slf4j + Logback`
- 命令必须支持 `--output=text|json`，默认 `text`
- 退出码固定为：
  - `0`：成功
  - `2`：参数错误
  - `3`：业务失败
  - `4`：系统异常

### 5. 异常与可观测性

- 业务异常必须映射为稳定的错误输出与退出码，不得直接打印堆栈到标准输出
- 未预期异常必须输出到标准错误并记录日志
- CLI 如复用现有应用服务，必须兼容既有日志与指标基线
- 不得把 `traceId`、`userId`、邮箱等高基数字段写成 Prometheus label

### 6. 测试要求

- 每个公开命令必须覆盖至少一个成功场景和一个失败场景
- `--help`、缺失必填参数、非法输入、业务异常、系统异常必须有测试
- CLI 模块必须支持标准 Maven 构建与 jar 运行

## 推荐实践

- 将公共输出渲染、异常映射、命令工厂沉淀为 `support` 层
- 使用 `Picocli + Spring Boot` 复用配置、日志、事务和应用服务
- 为数据维护、诊断、运维型命令保留 JSON 输出，便于脚本或 CI 消费
- 将 CLI 视为正式接口层之一，与 Web 接口共同复用同一套用例服务

## 反例/禁用项

- 在命令类中直接访问仓储、数据库驱动或 HTTP 客户端
- 为了省事复用 Controller、HTTP DTO 或 `ApiResponse`
- 将错误信息和业务结果混杂输出到标准输出
- 在 CLI 中直接使用 `System.exit` 到处散落退出逻辑而不统一映射
- 让命令名、参数名、帮助信息与实际行为不一致

## 检查清单

- [ ] 已采用 `Picocli + Spring Boot` 统一技术基线
- [ ] CLI 命令只复用 `application` 层，不依赖 Web 层对象
- [ ] 已支持 `--output=text|json`
- [ ] 已统一退出码与错误输出
- [ ] 已覆盖帮助、成功、参数错误、业务错误和系统错误测试
- [ ] 已补充使用说明或示例命令

## 关联文档

- [AGENTS.md](../AGENTS.md)
- [CODING_RULES.md](./CODING_RULES.md)
- [JAVA_RULES.md](./JAVA_RULES.md)
- [DDD_RULES.md](./DDD_RULES.md)
- [OBSERVABILITY_RULES.md](./OBSERVABILITY_RULES.md)
- [XINCHUANG_RULES.md](./XINCHUANG_RULES.md)
