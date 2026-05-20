# infrastructure/monitor

监控与可观测性骨架目录，用于承载 `Prometheus + Loki + Alloy + Grafana` 相关约定、示例资产与后续公共监控配置。

## 目录建议

```text
infrastructure/monitor/
├── alloy/
├── loki/
├── prometheus/
├── grafana/
│   ├── dashboards/
│   └── datasources/
└── alerts/
```

## 目录职责

- `prometheus/`：抓取目标、抓取间隔、服务发现与规则示例
- `loki/`：Loki 本地单机运行配置
- `alloy/`：Grafana Alloy 日志采集配置
- `grafana/dashboards/`：基础看板模板，如服务概览、接口延迟、错误率、JVM/资源、依赖组件状态
- `grafana/datasources/`：Grafana 数据源配置示例
- `alerts/`：Prometheus 告警规则模板与命名约定

## 推荐抓取目标

- Spring Boot 服务的 `/actuator/prometheus`
- Spring Boot 容器 stdout JSON 日志
- 必要的基础设施组件指标端点
- 关键中间件与依赖服务暴露的监控指标

## 命名约定

- Dashboard 名称建议使用 `模块-主题` 风格，例如 `user-center-overview`
- Alert 名称建议使用 `服务名_指标_语义` 风格，例如 `user_center_http_error_rate_high`

## 关联关系

- 监控技术栈与指标规则遵循 [OBSERVABILITY_RULES.md](../../docs/rules/OBSERVABILITY_RULES.md)
- Java 侧集成方式遵循 [JAVA_RULES.md](../../docs/rules/JAVA_RULES.md)
- 发布门禁遵循 [RELEASE.md](../../docs/rules/RELEASE.md)
- 国产环境兼容要求遵循 [XINCHUANG_RULES.md](../../docs/rules/XINCHUANG_RULES.md)

## 当前示例资产

- `prometheus/prometheus-example.yml`
- `prometheus/prometheus.yml`
- `loki/loki.yml`
- `alloy/config.alloy`
- `alerts/user-center-alerts-example.yml`
- `alerts/user-center-alerts.yml`
- `grafana/datasources/prometheus-example.yml`
- `grafana/datasources/prometheus.yml`
- `grafana/datasources/loki.yml`
- `grafana/dashboards/dashboard-provider.yml`
- `grafana/dashboards/user-center-overview.json`

以上文件为示例资产，可按环境复制调整，不直接假设生产可用。

## 本地可见化运行

启动顺序：

1. 执行 `docker compose -f deploy/docker/docker-compose.observability.yml up --build`
2. 打开 `http://localhost:9090`
3. 打开 `http://localhost:3000`

验证步骤：

- `curl http://localhost:18080/actuator/health`
- `curl http://localhost:18080/actuator/prometheus`
- 在 Prometheus UI 查询：
  - `user_create_total`
  - `user_query_not_found_total`
- 在 Grafana 中打开 `user-center-overview`
- 在 Grafana Explore 中选择 `Loki`，查询 `{service="user-center"}`

手工触发指标变化：

1. 调用 `POST /api/v1/users`
2. 调用不存在用户的 `GET /api/v1/users/{id}`

说明：

- 当前本地观测链路默认通过 compose service name 互联，不再依赖 `host.docker.internal`
- `Alloy` 仅采集 `user-center` 容器 stdout JSON 日志，并写入 `Loki`
- 宿主机访问 `user-center` 的默认端口为 `18080`；如需改回其它端口，可在启动前设置 `USER_CENTER_HOST_PORT`
