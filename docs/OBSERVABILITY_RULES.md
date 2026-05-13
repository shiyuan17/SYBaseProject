# OBSERVABILITY_RULES.md — 可观测性与监控规范

## 目标与适用范围

本文件定义项目在 Spring Boot 3 体系下的可观测性技术栈、指标暴露方式、Prometheus 抓取约定、Grafana 看板要求、告警规则与发布门禁。

- 适用对象：后端研发、运维、SRE、测试、AI 助手
- 适用范围：Spring Boot 3 服务、Micrometer 指标、Prometheus 抓取、Grafana 看板、告警规则、发布验证
- 规范定位：作为监控与指标专项规范，统一技术栈、交付物和发布检查要求

## 强制规则

### 1. 技术栈基线

- 监控技术栈统一为 `Spring Boot Actuator + Micrometer + Prometheus + Grafana`
- 默认暴露的监控端点为 `/actuator/health`、`/actuator/info`、`/actuator/prometheus`
- 禁止在没有访问控制和必要说明的情况下暴露高风险管理端点

### 2. 指标输出与命名

- 指标统一通过 `Micrometer` 注册和输出，由 `Prometheus` 抓取
- 指标至少覆盖 JVM、进程、HTTP、数据库连接池、消息队列、缓存和关键业务链路
- 自定义指标必须遵循统一命名，优先使用 `业务域_动作_含义` 风格，例如 `user_create_total`
- 统一控制 label 基数，保留 `service`、`module`、`env` 等低基数字段
- 禁止将 `userId`、`orderId`、`traceId`、手机号等高基数字段直接作为 Prometheus label

### 3. 业务指标要求

- 关键链路必须具备成功量、失败量、耗时指标
- 涉及重试、降级、积压、限流、熔断的链路，必须补充相应指标
- 指标设计必须支持告警和趋势分析，而不是仅用于临时排障

### 4. 看板与告警要求

- Grafana 至少提供以下基础看板：
  - 服务概览
  - 接口延迟
  - 错误率
  - JVM / 资源占用
  - 依赖组件状态
- Prometheus 告警至少覆盖：
  - 可用性
  - 错误率
  - 延迟
  - 资源使用
  - 消息或任务积压
  - 抓取失败
- 只有看板没有告警，或只有告警没有基础看板，都不满足交付要求

### 5. 配置与端点约定

- 推荐依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

- 推荐配置：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: when_authorized
```

- 业务指标示例：
  - `user_create_total`
  - `user_create_failed_total`
  - `user_create_duration`

### 6. 发布门禁

- 未接入基础指标、未准备基础看板或未配置核心告警的服务，不得进入候选发布
- 发布前必须完成 `/actuator/health`、`/actuator/prometheus` 验证
- 发布前必须确认 Prometheus 抓取成功、Grafana 基础看板可用、核心告警规则可触达

## 推荐实践

- 用 `MeterRegistry`、`Counter`、`Timer`、`Gauge` 构建业务指标
- 通过公共组件统一注册指标，避免模块各自命名和各自打点
- 通过 dashboard 模板和 alert 模板复用监控资产
- 在设计阶段就定义“需要被监控的业务结果”，不要等故障后再补指标

## 反例/禁用项

- 把 `traceId`、`userId`、`orderId` 等高基数字段直接做 label
- 只接 JVM 默认指标，不补业务指标
- 只有 Grafana 看板，没有 Prometheus 告警规则
- 通过业务代码分散控制日志、指标、端点暴露策略

## 检查清单

- [ ] 已接入 `Actuator + Micrometer + Prometheus + Grafana`
- [ ] `/actuator/health`、`/actuator/info`、`/actuator/prometheus` 已按规范暴露
- [ ] JVM、HTTP、连接池、依赖组件和关键业务指标已覆盖
- [ ] 自定义指标命名统一且 label 基数受控
- [ ] Grafana 基础看板已准备
- [ ] Prometheus 核心告警规则已配置并验证
- [ ] 发布前抓取、端点、看板、告警验证已完成

## 关联文档

- [JAVA_RULES.md](./JAVA_RULES.md)
- [RELEASE.md](./RELEASE.md)
- [XINCHUANG_RULES.md](./XINCHUANG_RULES.md)
- [AGENTS.md](./AGENTS.md)
