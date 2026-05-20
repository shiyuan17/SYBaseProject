# 文档导航

`docs/` 目录按“协作规范、工程说明、规划文档、专题资料”四类整理，便于快速定位和后续扩展。

## 快速入口

- [AI 与开发协作规范](./AGENTS.md)
- [工程说明与初始化指南](./guides/PROJECT_DIRECTORY.md)
- [模板定制说明](./guides/TEMPLATE_CUSTOMIZATION.md)
- [本地 GitLab 与测试环境流程](./guides/GITLAB_LOCAL_TEST_FLOW.md)

## 目录说明

- `rules/`：工程规范与发布规则，包括编码、DDD、API、数据库、Git、CI/CD、可观测性、信创等文档。
- `guides/`：工程结构、模板初始化、本地环境与操作手册。
- `plans/`：里程碑、阶段规划、验收与收口文档。
- `detailed_list/`：病理全流程功能分解清单。
- `database/`：数据库设计草案与 SQL 资料。
- `file-health-exemptions.properties`：仓库文件健康度校验豁免清单。

## 维护约定

- 新增规范类文档优先放入 `rules/`。
- 新增操作说明、初始化说明与环境手册优先放入 `guides/`。
- 新增阶段规划、验收记录与路线图优先放入 `plans/`。
- 专题型资料优先放入独立子目录，避免再次堆积到 `docs/` 根目录。
