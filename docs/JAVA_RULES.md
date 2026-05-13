# JAVA_RULES.md — Java 17 与 Spring Boot 3 实现规范

## 目标与适用范围

本文件定义本项目在 Java 17、Spring Boot 3、DDD 四层架构下的实现规则。

- 适用对象：Java 开发者、AI 编码助手、代码审查人员
- 适用范围：`controller`、`application`、`domain`、`infrastructure`、测试代码
- 规范定位：聚焦 Java 与 Spring 实现细则；通用工程规则以 `CODING_RULES.md` 为准

## 强制规则

### 1. 技术基线

- Java 版本统一为 `Java 17+`
- Spring Boot 版本统一为 `3.x`
- 代码组织默认遵循 `interfaces -> application -> domain -> infrastructure`
- 通用编码基线、命名与复杂度规则优先继承 `CODING_RULES.md`
- JDK、JVM 参数、字符集、时区、文件 IO、线程池配置、容器镜像必须满足国产环境兼容要求
- 涉及本地库、JNI、Agent、驱动或系统命令时，必须同步满足 `XINCHUANG_RULES.md` 的硬性门禁

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

### 4. Spring Boot 组件使用

- 依赖注入统一优先构造器注入
- 事务注解优先放在应用服务层公开方法
- 查询接口优先使用 `@Transactional(readOnly = true)`
- 参数校验统一使用 `jakarta.validation`
- 全局异常处理统一通过 `@RestControllerAdvice`
- 数据库驱动、连接池、任务调度、对象存储、MQ 客户端不得默认假设特定 OS、CPU 架构或 glibc 环境

示例：

```java
@Service
@RequiredArgsConstructor
public class CreateUserAppService {

    private final UserRepository userRepository;
    private final UserDomainService userDomainService;

    @Transactional
    public UserId create(CreateUserCommand command) {
        User user = userDomainService.create(command);
        userRepository.save(user);
        return user.getId();
    }
}
```

### 5. 对象设计与映射

- 请求对象、响应对象、领域对象、持久化对象必须分层定义
- DTO / Command / Query / VO 不得直接复用持久化对象
- 对象转换统一放在 assembler、converter、mapper 中，不得散落在 controller 和 domain 中
- 领域对象必须优先通过行为方法维护状态，不得在外层随意 set 破坏不变量

### 6. 异常、日志与校验

- 业务异常必须使用项目统一异常体系，不得直接抛裸 `RuntimeException`
- 参数非法、资源不存在、权限不足、并发冲突必须区分异常类型
- 日志必须使用占位符，不得字符串拼接
- 日志中不得输出密码、密钥、身份证号、手机号完整值、Token
- Controller 入参必须显式校验；跨字段复杂校验放到应用层或自定义校验器

### 7. 测试要求

- 新增业务逻辑必须补充测试
- 领域规则优先写单元测试
- 接口契约、参数校验、序列化与鉴权优先写集成测试
- 测试命名必须体现场景、条件和预期结果

## 推荐实践

- `controller` 方法尽量保持瘦，只做协议层拼装
- 应用服务名称使用 `*AppService`、命令使用 `*Command`、查询使用 `*Query`
- 领域服务只承载无法自然归属到单个聚合的方法
- 使用 `enum`、值对象和工厂方法表达业务语义，减少字符串与魔法值
- 通过 `MapStruct` 或统一 assembler 维护跨层映射规则
- 对并发写场景优先考虑乐观锁、幂等键、状态机校验
- 为依赖驱动、线程模型、文件处理逻辑保留国产环境验证说明

## 反例/禁用项

- 在 `controller` 中编排复杂业务流程
- 在 `service` 中混写参数校验、事务、领域规则、SQL 组装和返回对象映射
- 直接复用持久化实体作为 API 返回值
- 在领域对象外部直接修改关键状态而不经过业务行为
- 在私有方法或同类自调用上依赖 `@Transactional` 生效
- 通过复制粘贴新增多个近似实现而不抽象公共逻辑

## 检查清单

- [ ] 代码符合 Java 17 与 Spring Boot 3 基线
- [ ] 分层职责清晰，没有跨层越界依赖
- [ ] 对象模型已区分 Command / DTO / Domain / DO / VO
- [ ] 异常、日志、参数校验遵循统一规范
- [ ] 事务边界位于应用服务公开方法
- [ ] 关键业务逻辑已补充测试

## 关联文档

- [CODING_RULES.md](./CODING_RULES.md)
- [DDD_RULES.md](./DDD_RULES.md)
- [API_RULES.md](./API_RULES.md)
- [DB_RULES.md](./DB_RULES.md)
- [XINCHUANG_RULES.md](./XINCHUANG_RULES.md)
- [AGENTS.md](./AGENTS.md)
