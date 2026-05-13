# DDD_RULES.md — 领域驱动设计规范

## 目标与适用范围

本文件定义本项目基于 DDD 的分层边界、建模方式与调用方向。

- 适用对象：后端开发者、架构设计者、AI 助手、代码审查人员
- 适用范围：`interfaces`、`application`、`domain`、`infrastructure` 四层及跨层协作
- 规范定位：统一架构语言，确保代码实现围绕领域模型组织

## 强制规则

### 1. 四层职责

#### `interfaces`

- 接收 HTTP / RPC / MQ 输入
- 做协议转换、参数校验、权限入口控制
- 调用应用服务并返回 VO / Response

#### `application`

- 编排用例、事务、权限上下文、领域对象协作
- 承载 `command`、`query`、`event handler`、`task`
- 不实现核心业务规则本身

#### `domain`

- 放置聚合、实体、值对象、领域服务、仓储接口、领域事件
- 维护业务不变量与状态变化规则
- 不依赖数据库、缓存、MQ、RPC、Web 框架

#### `infrastructure`

- 实现仓储、Mapper、DO、外部网关、缓存、消息、配置与技术适配
- 为应用层和领域层提供技术支撑

### 2. 允许的调用方向

允许：

- `interfaces -> application`
- `application -> domain`
- `application -> infrastructure`
- `infrastructure -> domain`（实现仓储接口、对象转换时）

禁止：

- `domain -> infrastructure`
- `domain -> interfaces`
- `interfaces -> domain` 直接绕过应用层
- `infrastructure -> interfaces`

### 3. 领域对象边界

- 聚合根负责维护聚合内部一致性
- 值对象必须不可变并通过值语义比较
- 仓储接口定义在 `domain`，实现放在 `infrastructure`
- 领域服务只处理跨实体、跨聚合且无法归属单对象的规则
- 领域事件只表达业务事实，不承载技术细节

### 4. 对象转换规则

- Request / DTO / Command / Domain / DO / VO 必须分层建模
- 转换逻辑统一放在 assembler、converter、mapper 中
- 不得把持久化对象直接返回给接口层
- 不得把接口入参对象直接作为领域对象长期流转

### 5. 事务与一致性

- 事务边界优先在应用层定义
- 单聚合内一致性优先通过聚合行为保证
- 跨聚合协作优先使用领域事件、补偿机制、异步协同
- 不得为了省事把所有业务塞进一个大事务

## 推荐实践

- 先识别限界上下文，再定义聚合与应用用例
- 用业务语言命名对象和方法，避免技术名词主导模型
- 复杂状态流转通过领域方法或状态机显式表达
- 通过防腐层隔离外部系统协议与内部领域模型
- 在 `application` 中做 orchestration，在 `domain` 中做 decision

## 反例/禁用项

- 在 `domain` 中直接注入 `Mapper`、`RedisTemplate`、`FeignClient`
- 在 `interfaces` 中写完整业务编排或事务控制
- 把数据库表结构直接当作领域模型
- 用一个“万能 service”横跨接口、应用、领域、持久化职责
- 所有逻辑都写在 DTO / Entity 的 setter 中，导致规则不可追踪

## 检查清单

- [ ] 四层职责划分清晰
- [ ] 调用方向符合 DDD 依赖规则
- [ ] 聚合、实体、值对象、领域服务职责明确
- [ ] 仓储接口与实现分层正确
- [ ] 对象转换没有越层泄漏
- [ ] 事务与一致性设计合理

## 关联文档

- [JAVA_RULES.md](./JAVA_RULES.md)
- [DB_RULES.md](./DB_RULES.md)
- [API_RULES.md](./API_RULES.md)
- [AGENTS.md](./AGENTS.md)
