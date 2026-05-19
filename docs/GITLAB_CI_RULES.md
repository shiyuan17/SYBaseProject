# GITLAB_CI_RULES.md — GitLab CI/CD 与流水线规范

## 目标与适用范围

本文件定义项目在 GitLab 中的流水线阶段、环境映射、变量管理、部署门禁与回滚入口。

- 适用对象：研发、测试、运维、发布负责人、AI 助手
- 适用范围：`.gitlab-ci.yml`、`scripts/ci/`、GitLab Variables、Docker 主机部署任务
- 规范定位：聚焦“如何在 GitLab 上持续集成与持续部署”；分支模型与发布流程分别以 `GIT_RULES.md`、`RELEASE.md` 为准

## 强制规则

### 1. 流水线阶段

- 流水线阶段统一拆分为：
  - `verify`
  - `package`
  - `image`
  - `deploy`
- `verify` 用于执行测试与基础质量校验
- `package` 用于构建可发布产物
- `image` 用于构建并推送镜像
- `deploy` 用于执行环境级部署，不得混入源码构建

### 2. Verify 门禁

- `verify` 阶段必须执行仓库级自动测试与基础质量门禁
- `verify` 阶段除业务测试外，必须包含仓库文件编码与文件健康度校验
- 仓库文件编码与文件健康度校验至少覆盖以下内容：
  - 文本文件可按 `UTF-8` 严格解码
  - 文本文件不包含 `UTF-8 BOM`
  - 换行符符合仓库规则，默认 `LF`，仅 `cmd/bat` 例外
  - 行数与文件体积符合默认阈值，历史超大文件仅可通过显式豁免清单放宽
- 未通过文件编码与文件健康度校验的分支，不得进入后续 `package`、`image`、`deploy`

### 3. 分支与环境映射

- `develop` 分支：
  - 自动执行 `verify`、`package`、`image`
  - 自动部署到 `local`
  - 本地联调通过后，手动推进到 `test`
- `release/*` 分支：
  - 自动执行 `verify`、`package`、`image`
  - 仅允许手动部署到 `staging`
- `v*` 正式版本 Tag：
  - 自动执行 `verify`、`package`、`image`
  - 仅允许手动部署到 `prod`
- `prod` 部署不得直接从普通分支触发

### 4. 变量命名与保护

- GitLab Variables 必须区分“通用变量”和“环境变量”
- 需要出现在流水线中的密钥必须设置为 `Masked`
- 仅供 `staging`、`prod` 使用的变量必须设置为 `Protected`
- 环境相关变量统一使用以下命名：
  - `DEPLOY_LOCAL_HOST`
  - `DEPLOY_LOCAL_USER`
  - `DEPLOY_LOCAL_SSH_KEY`
  - `DEPLOY_LOCAL_APP_DIR`
  - `DEPLOY_LOCAL_PORT`
  - `DEPLOY_LOCAL_SPRING_PROFILE`
  - `DEPLOY_TEST_HOST`
  - `DEPLOY_TEST_USER`
  - `DEPLOY_TEST_SSH_KEY`
  - `DEPLOY_TEST_APP_DIR`
  - `DEPLOY_TEST_PORT`
  - `DEPLOY_TEST_SPRING_PROFILE`
  - `DEPLOY_STAGING_*`
  - `DEPLOY_PROD_*`

### 5. 镜像与部署约束

- 镜像仓库默认使用 `CI_REGISTRY_IMAGE/<service>`
- 镜像至少推送以下 tag：
  - `CI_COMMIT_SHA`
  - `CI_COMMIT_REF_SLUG`
  - `CI_COMMIT_TAG`（仅 tag pipeline）
- Docker 主机部署必须通过 `docker login`、`docker compose pull`、`docker compose up -d` 执行
- 目标主机部署目录必须固定，默认使用 `/opt/sybase/<service>`
- 回滚必须通过修改镜像 tag 并重新执行部署实现，不得在目标主机重新构建历史版本

### 6. 失败处理与审批

- `local` 环境允许自动部署，失败必须阻断后续手动推进 `test`
- `test`、`staging`、`prod` 必须保留人工确认点或明确的人工作业触发动作
- `prod` job 必须绑定 GitLab Protected Environment，并限制可执行角色
- 任何失败部署都必须保留日志、失败命令和镜像 tag，便于追溯
- 需要人工回滚时，必须能明确定位“上一稳定 tag”

### 7. 与现有发布规范的关系

- GitLab 流水线不能绕过 `GIT_RULES.md` 的分支治理要求
- GitLab 流水线不能替代 `RELEASE.md` 中的 UAT、信创验证、回滚确认职责
- 涉及镜像、脚本、系统包的新增或修改，必须同步满足 `XINCHUANG_RULES.md`

## 推荐实践

- 把重复的构建与部署命令沉淀到 `scripts/ci/`，避免把复杂逻辑直接塞进 `.gitlab-ci.yml`
- 为每个 `image` job 使用独立镜像仓库路径，避免不同服务共用同一 tag 空间
- 为 `deploy` job 明确设置 `environment` 名称，便于 GitLab 环境面板追踪
- 将测试产物、Jar 包、镜像 tag 和部署日志统一留档，提升问题定位效率
- 对 `prod` 部署启用双人复核或受保护环境审批
- 对文件健康度豁免清单的调整，与常规代码变更同样进入 MR 审查，避免例外失控

## 反例/禁用项

- 在 `deploy` job 中直接执行源码构建或修改源文件
- 使用一个共享 SSH 私钥同时控制所有环境且不做保护
- 让 `main` 或普通功能分支自动发布到生产环境
- 只记录“部署成功”，却不记录镜像 tag、主机目录和验证结果
- 在 GitLab Variables 中保存明文示例密码并长期复用
- 为绕过 `verify` 门禁临时删除文件健康校验，事后不恢复

## 检查清单

- [ ] 已按 `verify`、`package`、`image`、`deploy` 划分流水线
- [ ] `develop`、`release/*`、`v*` 与环境映射已明确
- [ ] `verify` 阶段已纳入仓库文件编码与文件健康度校验
- [ ] SSH 私钥、主机地址、端口、部署目录、Spring Profile 已按环境拆分变量
- [ ] `staging`、`prod` 变量已设置 `Protected`，密钥已设置 `Masked`
- [ ] `staging`、`prod` 部署保留人工审批点
- [ ] 回滚入口与上一稳定 tag 的定位方式已明确
- [ ] 镜像、脚本、系统包变更已同步纳入信创兼容记录

## 关联文档

- [CONTAINER_RULES.md](./CONTAINER_RULES.md)
- [GIT_RULES.md](./GIT_RULES.md)
- [GITLAB_LOCAL_TEST_FLOW.md](./GITLAB_LOCAL_TEST_FLOW.md)
- [RELEASE.md](./RELEASE.md)
- [XINCHUANG_RULES.md](./XINCHUANG_RULES.md)
