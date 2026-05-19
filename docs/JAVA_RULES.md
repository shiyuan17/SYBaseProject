# JAVA_RULES.md — Java 17 与 Spring Boot 3 实现规范

## 目标与适用范围

本文件定义本项目在 `Java 17`、`Spring Boot 3`、`DDD` 四层结构下的实现规则。

- 适用对象：Java 开发者、AI 编码助手、代码审查人员
- 适用范围：`controller`、`application`、`domain`、`infrastructure`、测试代码
- 规范定位：聚焦 Java 与 Spring 实现细则；通用工程规则以 `CODING_RULES.md` 为准

## 强制规则

### 1. 技术基线

- Java 版本统一为 `Java 17+`
- Spring Boot 版本统一为 `3.x`
- `Picocli` 作为标准 CLI 技术栈的一部分
- `Lombok` 仅作为减少样板代码的工具，不能替代边界设计
- 日志技术栈统一为 `Slf4j + Logback`
- 监控技术栈统一为 `Spring Boot Actuator + Micrometer + Prometheus + Grafana`
- 代码组织默认遵循 `interfaces -> application -> domain -> infrastructure`
- 通用编码基线、命名与复杂度规则优先继承 `CODING_RULES.md`
- JDK、JVM 参数、字符集、时区、文件 IO、线程池配置、容器镜像必须满足国产环境兼容要求

### 2. 命名与文件结构

- 类名使用 `PascalCase`
- 方法名、变量名使用 `camelCase`
- 常量使用 `UPPER_SNAKE_CASE`
- 包名使用全小写英文，按业务域拆分，不得使用拼音缩写
- 一个类只承载一个主要职责；超出单一职责时必须拆分

推荐的类内顺序：

1. `package`
2. `import`
3. 类注解
4. 类说明
5. 常量
6. 依赖字段
7. 构造器
8. 对外公开方法
9. 内部辅助方法

### 3. 分层实现要求

- `controller` 只做协议适配、参数校验、调用应用服务、组装响应
- `application` 负责用例编排、事务边界、命令与查询协同
- `domain` 负责业务规则、领域对象行为、领域服务与仓储抽象
- `infrastructure` 负责数据库、缓存、MQ、RPC、对象转换与外部系统接入

明确禁止：

- `controller` 直接访问数据库或 Mapper
- `domain` 直接依赖 Spring Web、数据库驱动、HTTP 客户端、缓存客户端
- `infrastructure` 反向调用 `controller`

### 4. Lombok 使用规范

- 仓库通过父 `pom.xml` 统一继承 `Lombok` 依赖与注解处理配置
- 新增 Maven 子模块默认沿用仓库级 `Lombok` 基线，不得私自覆盖版本或注解处理链路
- 本地开发、CI 与国产 JDK 环境统一通过 `./mvnw` 验证 `Lombok` 注解处理结果
- 允许使用 `@RequiredArgsConstructor` 实现构造器注入
- 允许使用 `@Slf4j` 管理日志组件
- 允许在简单 DTO、VO、配置对象、持久化对象中使用 `@Getter`、`@Setter`
- 允许在只读、纯字段型、无业务行为的测试数据对象上使用 `@Builder`

限制使用：

- 聚合根、值对象以及有业务行为或状态约束的领域对象默认不使用 `@Data`
- 领域对象禁止通过 `@Setter` 暴露关键业务状态的任意修改能力
- 使用 `@EqualsAndHashCode`、`@ToString` 时必须评估循环引用、敏感字段泄露和代理对象问题

### 5. Spring Boot 组件使用

- 依赖注入统一优先构造器注入
- 事务注解优先放在应用服务层公开方法
- 查询接口优先使用 `@Transactional(readOnly = true)`
- 参数校验统一使用 `jakarta.validation`
- 全局异常处理统一通过 `@RestControllerAdvice`
- 数据库驱动、连接池、任务调度、对象存储、MQ 客户端不得默认假设特定 `OS`、`CPU` 架构或 `glibc` 环境

### 6. Java CLI 实现

- Java CLI 应用统一采用 `Picocli + Spring Boot`
- CLI 模块默认独立于 Web 服务模块，不得把命令入口和 `Spring MVC` 主入口混在同一应用中
- CLI 只负责命令参数绑定、帮助输出、调用应用服务与结果渲染
- CLI 如需复用业务能力，必须依赖 `application` 层，不得直接复用 Controller 或 HTTP 响应对象
- CLI 详细规范以 `CLI_RULES.md` 为准

### 7. 字符集与文件处理

- 字符集相关代码统一使用 `StandardCharsets.UTF_8`
- 禁止使用省略字符集的 `String#getBytes()`、`new String(byte[])`
- 禁止直接使用 `FileReader`、`FileWriter`、无字符集参数的 IO 包装器处理文本
- 文件导入导出、日志落盘、模板读取、CLI 输出、HTTP 文本附件生成、脚本模板渲染等场景必须显式指定字符集
- 与字节数组、流、缓冲区互转时，必须把字符集视为接口契约的一部分
- 读取仓库内文本资源时，默认按 `UTF-8` 处理；如需兼容外部非 `UTF-8` 数据，必须在接口层显式转换并记录原因

### 8. 日志规范

- 应用日志统一通过 `Slf4j` 门面输出，底层实现统一为 `Logback`
- 业务代码中禁止直接使用 `System.out.println`、`printStackTrace()`、`java.util.logging`、`Log4j API`
- 日志级别必须语义清晰：
  - `DEBUG`：开发排障细节
  - `INFO`：关键业务节点和状态变化
  - `WARN`：可恢复异常、重试、降级、脏数据提示
  - `ERROR`：失败结果、异常堆栈、需要告警的问题
- `Logback` 配置应统一管理格式、级别、滚动策略与异步输出策略
- 日志内容必须包含业务主键、链路标识、操作结果等必要上下文，但不得输出敏感信息

### 9. 可观测性规范

- 运行时指标统一通过 `Micrometer` 输出，`Prometheus` 抓取，`Grafana` 展示
- 默认暴露的管理端点仅限 `health`、`info`、`prometheus` 等必要端点
- 指标至少覆盖 `JVM`、进程、HTTP、数据库连接池、消息队列、缓存和关键业务链路
- 自定义指标必须控制 `label` 基数，禁止将 `userId`、`orderId`、`traceId` 等高基数字段直接作为标签
- 核心业务流程必须具备成功量、失败量、耗时、重试、降级或积压类指标

### 10. 对象设计与映射

- 请求对象、响应对象、领域对象、持久化对象必须分层定义
- DTO / Command / Query / VO 不得直接复用持久化对象
- 对象转换统一放在 `assembler`、`converter`、`mapper` 中，不得散落在 `controller` 和 `domain`
- 领域对象必须优先通过行为方法维护状态，不得在外部随意 `set`

### 11. 异常、日志与校验

- 业务异常必须使用项目统一异常体系，不得直接抛裸 `RuntimeException`
- 参数非法、资源不存在、权限不足、并发冲突必须区分异常类型
- 日志必须通过 `Slf4j` 占位符输出，不得手动字符串拼接
- Controller 入参必须显式校验；跨字段复杂校验放到应用层或自定义校验器

### 12. 测试要求

- 新增业务逻辑必须补充测试
- 领域规则优先写单元测试
- 接口契约、参数校验、序列化与鉴权优先写集成测试
- 测试命名必须体现“场景 + 条件 + 预期结果”
- 新增与字符集、导入导出、文件处理相关的能力时，必须至少覆盖 `UTF-8` 正常路径和常见异常路径

## 推荐实践

- `controller` 方法尽量保持瘦，只做协议层拼装
- 应用服务使用 `*AppService`，命令使用 `*Command`，查询使用 `*Query`
- 领域服务只承载无法自然归属到单个聚合的方法
- 使用 `enum`、值对象和工厂方法表达业务语义，减少字符串和魔法值
- 通过统一 assembler 或 `MapStruct` 维护跨层映射规则
- 对并发写场景优先考虑乐观锁、幂等键、状态机校验
- 文件读写和模板处理优先基于 `InputStreamReader`、`OutputStreamWriter`、`Files.newBufferedReader`、`Files.newBufferedWriter` 并显式传入 `StandardCharsets.UTF_8`

## 反例/禁用项

- 在 `controller` 中编排复杂业务流程
- 在 `service` 中混写参数校验、事务、领域规则、SQL 组装和返回对象映射
- 直接复用持久化实体作为 API 返回值
- 在领域对象外部直接修改关键状态而不经过业务行为
- 通过复制粘贴新增多个近似实现而不抽象公共逻辑
- 在业务代码中混用 `System.out.println`、`printStackTrace()`、`java.util.logging`、`Log4j API`
- 把 `userId`、`orderId`、`traceId` 等高基数字段直接作为 Prometheus label
- 使用默认平台字符集处理仓库文件、模板、导出文件或 CLI 输出

## 检查清单

- [ ] 代码符合 Java 17 与 Spring Boot 3 基线
- [ ] 分层职责清晰，没有跨层越界依赖
- [ ] 对象模型已区分 `Command / DTO / Domain / DO / VO`
- [ ] 异常、日志、参数校验遵循统一规范
- [ ] 日志统一使用 `Slf4j + Logback`
- [ ] 指标统一通过 `Micrometer` 输出并满足 `Prometheus` 抓取要求
- [ ] 事务边界位于应用服务公开方法
- [ ] 关键业务逻辑已补充测试
- [ ] 已按对象类型正确选用 Lombok 注解
- [ ] 字符集相关代码显式使用 `StandardCharsets.UTF_8`
- [ ] 文件导入导出、模板读取、CLI 输出等高风险场景已完成字符集校验

## 关联文档

- [CODING_RULES.md](./CODING_RULES.md)
- [DDD_RULES.md](./DDD_RULES.md)
- [API_RULES.md](./API_RULES.md)
- [DB_RULES.md](./DB_RULES.md)
- [XINCHUANG_RULES.md](./XINCHUANG_RULES.md)
- [OBSERVABILITY_RULES.md](./OBSERVABILITY_RULES.md)
- [AGENTS.md](./AGENTS.md)
