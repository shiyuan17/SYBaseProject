# 项目健康治理规则

## 目标

本规则定义当前项目的健康治理基线，优先服务于“稳定化优先”的迭代目标。默认要求是：先守住模块边界、测试反馈速度和统一质量门禁，再继续放大业务能力。

## 当前事实

- `bl-center` 是当前复杂度最高的模块，后续拆分优先围绕业务闭环推进。
- 外部 HTTP API、URL、返回契约和既有数据库行为默认保持不变。
- 根工程负责统一测试分层与质量门禁，模块只补充本模块特有约束。

## 模块边界

- `bl-center` 的工作流 application service 按业务闭环拆分，不按技术层机械拆分。
- `SpecimenWorkflowAppService` 可作为 facade 保留现有入口；查询职责优先下沉到更小的 query service。
- repository port 默认区分命令与查询职责，避免单个 interface 同时承载状态迁移、列表查询和导出。
- controller 若出现过量 DTO/VO 映射，可增加 assembler/mapper，但不得借机改变接口语义。

## 公共能力归属

- 观测能力统一放在 `common/common-web`，模块只保留少量配置注入点。
- 测试基类与测试支撑能力统一放在 `common/common-test`。
- 只有跨模块、稳定复用的能力才能进入 `common`；任何 `bl-center` 领域概念不得伪装成平台能力上提。

## 测试分层

- 继承 `BaseWebIntegrationTest` 的测试默认标记为 `@Tag("slow")`。
- `BaseJdbcWebIntegrationTest` 用于需要数据库初始化的 Web 集成测试。
- `BaseMockMvcIntegrationTest` 用于不依赖数据库装配的 Web/MVC 测试。
- 新拆出的规则、状态转换和用例编排优先补 service/domain 级测试，不再只依赖重集成回归。

## 质量门禁

- 根 `pom.xml` 统一维护 Surefire 的 `groups` 与 `excludedGroups` 配置。
- 本地快速反馈默认使用 `./mvnw test "-Dsurefire.excludedGroups=slow"`。
- 全量回归默认使用 `./mvnw verify`。
- `common-test` 中的仓库文件健康测试属于基础静态门禁，用于守住 UTF-8、行数和文件健康规则。

## CI 约定

- `verify_fast` 负责快速反馈，默认排除 `slow` 测试。
- `verify_full` 负责全量 `verify`，保留覆盖率等完整构建行为。
- API regression 脚本属于增强校验，不替代统一质量门禁。
- 打包、镜像和部署阶段必须建立在统一门禁通过之后。

## 文档协作

- 涉及模块边界、公共能力归属、测试分层、质量门禁的规则更新，优先同步本文件。
- 文本文档默认使用 UTF-8 编码；Java、YAML、XML 等源代码与配置默认使用 LF。
- 如需新增治理例外，必须同时更新 `docs/file-health-exemptions.properties` 并说明原因。