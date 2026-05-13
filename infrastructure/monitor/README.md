# infrastructure/monitor

监控与可观测性骨架目录，用于承载 `Prometheus + Grafana` 相关约定、示例资产与后续公共监控配置。

## 目录建议

```text
infrastructure/monitor/
├── prometheus/
├── grafana/
│   ├── dashboards/
│   └── datasources/
└── alerts/
```

## 目录职责

- `prometheus/`：抓取目标、抓取间隔、服务发现与规则示例
- `grafana/dashboards/`：基础看板模板，如服务概览、接口延迟、错误率、JVM/资源、依赖组件状态
- `grafana/datasources/`：Grafana 数据源配置示例
- `alerts/`：Prometheus 告警规则模板与命名约定

## 推荐抓取目标

- Spring Boot 服务的 `/actuator/prometheus`
- 必要的基础设施组件指标端点
- 关键中间件与依赖服务暴露的监控指标

## 命名约定

- Dashboard 名称建议使用 `模块-主题` 风格，例如 `user-center-overview`
- Alert 名称建议使用 `服务名_指标_语义` 风格，例如 `user_center_http_error_rate_high`

## 关联关系

- 监控技术栈与指标规则遵循 [OBSERVABILITY_RULES.md](../../docs/OBSERVABILITY_RULES.md)
- Java 侧集成方式遵循 [JAVA_RULES.md](../../docs/JAVA_RULES.md)
- 发布门禁遵循 [RELEASE.md](../../docs/RELEASE.md)
- 国产环境兼容要求遵循 [XINCHUANG_RULES.md](../../docs/XINCHUANG_RULES.md)

## 当前示例资产

- `prometheus/prometheus-example.yml`
- `alerts/user-center-alerts-example.yml`
- `grafana/datasources/prometheus-example.yml`
- `grafana/dashboards/user-center-overview.json`

以上文件为示例资产，可按环境复制调整，不直接假设生产可用。
