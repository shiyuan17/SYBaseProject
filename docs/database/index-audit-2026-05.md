# BL Center Index Audit 2026-05

本清单基于 `bl-center` 当前 `Flyway/DDL` 与仓储查询实现整理，目标库按达梦生产环境评估。

## 已保留的合理索引

- `users`: `pk_users`, `uk_users_login_name`, `uk_users_user_code`, `uk_users_job_no`, `uk_users_login_tag_code`
- `applications`: `pk_applications`, `uk_applications_application_no`
- `pathology_cases`: `pk_pathology_cases`, `uk_pathology_cases_application`, `uk_pathology_cases_no`
- `specimens`: `pk_specimens`, `uk_specimens_specimen_no`, `uk_specimens_barcode`
- `diagnostic_tasks`: `idx_diagnostic_tasks_case_status`, `idx_diagnostic_tasks_pathology_status`
- `pathology_reports`: `uk_pathology_reports_case_scope_seq`, `uk_pathology_reports_report_no`, `idx_pathology_reports_case_status`
- `report_versions`: `uk_report_versions_report_version`, `idx_report_versions_report_id`
- `report_revision_requests`: `idx_report_revision_requests_case_status`, `idx_report_revision_requests_report_status`
- `medical_orders`: `uk_medical_orders_order_number`, `idx_medical_orders_case_status`, `idx_medical_orders_status`
- `consultation_cases`: `idx_consultation_cases_case_status`
- `consultation_participants`: `idx_consultation_participants_consultation`, `idx_consultation_participants_user`
- `user_notifications`: `idx_user_notifications_user_status`, `idx_user_notifications_user_archived`

## 本次新增的查询支撑索引

### P1

- `user_login_logs(user_id, login_at, id)`:
  支撑用户登录日志分页与倒序读取。
- `user_roles(role_id)`:
  支撑角色维度统计、回填和授权查询。
- `role_permissions(role_id)`, `role_menus(role_id)`, `role_message_subscriptions(role_id)`, `role_stat_authorizations(role_id)`:
  支撑角色授权读取、删除和 RBAC 联表。
- `specimen_collection_records(application_id, label_print_batch_no, collected_at, id)`:
  支撑登记快照最近一条查询。
- `specimen_receipts(specimen_id, receipt_status)`:
  支撑收样状态存在性判断与最新回执窗口函数场景。
- `workflow_events(application_id, event_time, created_at)`, `workflow_events(case_id, event_time, created_at)`, `workflow_events(specimen_id, event_time, created_at)`, `workflow_events(transport_order_id, event_time, created_at)`:
  支撑按申请、病例、标本、运送单的时间线读取与最近节点查询。
- `technical_pending_tasks(case_id, task_status, created_at)`, `technical_pending_tasks(task_type, object_type, object_id, task_status, created_at)`:
  支撑按病例与业务对象读取活跃技术任务。

### P2

- `samplings(case_id)`, `samplings(specimen_id)`
- `sampling_blocks(case_id, sequence_no, id)`, `sampling_blocks(sampling_id)`, `sampling_blocks(specimen_id)`
- `dehydration_batch_items(batch_id, loaded_at, id)`, `dehydration_batch_items(specimen_id)`
- `embeddings(sampling_block_id, created_at, id)`
- `embedding_boxes(case_id, created_at, id)`, `embedding_boxes(sampling_block_id)`
- `slicings(embedding_box_id)`
- `slides(case_id, created_at, id)`, `slides(slicing_id, created_at, id)`, `slides(sampling_block_id)`
- `slide_stainings(case_id, created_at, id)`
- `rework_orders(case_id, created_at, id)`
- `slide_qc_evaluations(case_id, evaluated_at, created_at, id)`
- `case_media_assets(object_type, object_id, captured_at, created_at)`

## 明确不新增的索引

- 不为 `%keyword%` 类型模糊搜索单独新增 B-Tree 索引。
- 不为已被唯一约束左前缀充分覆盖的列重复建近似索引。
- 不在本轮删除既有索引，除非后续有执行计划和线上负载证据证明冗余。
