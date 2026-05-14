# user-center

`user-center` 是脚手架中的示例业务模块，用于演示以下规范在代码中的落地方式：

- [docs/JAVA_RULES.md](../docs/JAVA_RULES.md)
- [docs/DDD_RULES.md](../docs/DDD_RULES.md)
- [docs/API_RULES.md](../docs/API_RULES.md)

## 结构映射

- `interfaces`：Controller、DTO、VO、Assembler
- `application`：用例编排、事务边界、命令与查询
- `domain`：聚合、值对象、领域服务、仓储抽象
- `infrastructure`：仓储实现、技术配置、对象转换

## 示例能力

- `POST /api/v1/users`：创建用户
- `GET /api/v1/users/{id}`：按主键查询用户

## 当前实现说明

- 仓储采用内存实现，便于在未确定数据库选型前先验证分层与接口契约
- 全局错误响应统一返回 `code`、`message`、`traceId`
- `/api/**` 业务接口默认自动包装为统一返回体；特殊接口可用 `@IgnoreApiResponseWrap` 显式跳过
- DTO / Command / Domain / DO / VO 分层建模，不直接复用
