INSERT INTO menus (id, parent_id, menu_code, menu_name, menu_type, path, component_name, permission_prefix, sort_order) VALUES
('MENU_M6_SUPPORT', NULL, 'M6_SUPPORT', '数据统计与分析', 'DIRECTORY', '/m6', 'M6Root', 'm6', 190),
('MENU_M6_INTEGRATION', 'MENU_M6_SUPPORT', 'M6_INTEGRATION', '集成任务管理', 'MENU', '/api/v1/integration-tasks', 'IntegrationManagement', 'm6:integration', 191),
('MENU_M6_BILLING', 'MENU_M6_SUPPORT', 'M6_BILLING', '收费管理', 'MENU', '/api/v1/billing-records', 'BillingManagement', 'm6:billing', 192),
('MENU_M6_HISTORY', 'MENU_M6_SUPPORT', 'M6_HISTORY', '历史报告', 'MENU', '/api/v1/historical-reports', 'HistoricalReports', 'm6:history', 193),
('MENU_M6_STAT', 'MENU_M6_SUPPORT', 'M6_STAT', '自定义统计分析', 'MENU', '/api/v1/stat-indicators', 'StatisticsAnalysis', 'm6:stat', 194);

INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order) VALUES
('PERM_M6_INTEGRATION_TASK_QUERY', 'PERM_M6_INTEGRATION_TASK_QUERY', '查询集成任务', 'MENU_M6_INTEGRATION', 'INTEGRATION_TASK_QUERY', 'GET', '/api/v1/integration-tasks', 'M6', 191),
('PERM_M6_BILLING_QUERY', 'PERM_M6_BILLING_QUERY', '查询收费记录', 'MENU_M6_BILLING', 'BILLING_QUERY', 'GET', '/api/v1/billing-records', 'M6', 192),
('PERM_M6_BILLING_RECEIPT', 'PERM_M6_BILLING_RECEIPT', '收费回执', 'MENU_M6_BILLING', 'BILLING_RECEIPT', 'POST', '/api/v1/billing-records/{id}/receipt', 'M6', 193),
('PERM_M6_BILLING_RETRY', 'PERM_M6_BILLING_RETRY', '收费重试', 'MENU_M6_BILLING', 'BILLING_RETRY', 'POST', '/api/v1/billing-records/{id}/retry', 'M6', 194),
('PERM_M6_BILLING_RECONCILE', 'PERM_M6_BILLING_RECONCILE', '收费对账', 'MENU_M6_BILLING', 'BILLING_RECONCILE', 'POST', '/api/v1/billing-records/reconcile', 'M6', 195),
('PERM_M6_HISTORY_IMPORT', 'PERM_M6_HISTORY_IMPORT', '导入历史报告', 'MENU_M6_HISTORY', 'HISTORY_IMPORT', 'POST', '/api/v1/historical-report-import-jobs', 'M6', 196),
('PERM_M6_HISTORY_QUERY', 'PERM_M6_HISTORY_QUERY', '查询历史报告', 'MENU_M6_HISTORY', 'HISTORY_QUERY', 'GET', '/api/v1/historical-reports', 'M6', 197),
('PERM_M6_STAT_INDICATOR_QUERY', 'PERM_M6_STAT_INDICATOR_QUERY', '查询统计指标', 'MENU_M6_STAT', 'STAT_INDICATOR_QUERY', 'GET', '/api/v1/stat-indicators', 'M6', 198),
('PERM_M6_STAT_TEMPLATE_QUERY', 'PERM_M6_STAT_TEMPLATE_QUERY', '查询统计模板', 'MENU_M6_STAT', 'STAT_TEMPLATE_QUERY', 'GET', '/api/v1/stat-report-templates', 'M6', 199),
('PERM_M6_STAT_REPORT_QUERY', 'PERM_M6_STAT_REPORT_QUERY', '查询统计报表', 'MENU_M6_STAT', 'STAT_REPORT_QUERY', 'POST', '/api/v1/stat-reports/query', 'M6', 200),
('PERM_M6_STAT_REPORT_EXPORT', 'PERM_M6_STAT_REPORT_EXPORT', '导出统计报表', 'MENU_M6_STAT', 'STAT_REPORT_EXPORT', 'POST', '/api/v1/stat-reports/export', 'M6', 201);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at) VALUES
('RP_M6_ADMIN_1', 'ROLE_PATHOLOGY_ADMIN', 'PERM_M6_INTEGRATION_TASK_QUERY', CURRENT_TIMESTAMP),
('RP_M6_ADMIN_2', 'ROLE_PATHOLOGY_ADMIN', 'PERM_M6_BILLING_QUERY', CURRENT_TIMESTAMP),
('RP_M6_ADMIN_3', 'ROLE_PATHOLOGY_ADMIN', 'PERM_M6_BILLING_RECEIPT', CURRENT_TIMESTAMP),
('RP_M6_ADMIN_4', 'ROLE_PATHOLOGY_ADMIN', 'PERM_M6_BILLING_RETRY', CURRENT_TIMESTAMP),
('RP_M6_ADMIN_5', 'ROLE_PATHOLOGY_ADMIN', 'PERM_M6_BILLING_RECONCILE', CURRENT_TIMESTAMP),
('RP_M6_ADMIN_6', 'ROLE_PATHOLOGY_ADMIN', 'PERM_M6_HISTORY_IMPORT', CURRENT_TIMESTAMP),
('RP_M6_ADMIN_7', 'ROLE_PATHOLOGY_ADMIN', 'PERM_M6_HISTORY_QUERY', CURRENT_TIMESTAMP),
('RP_M6_ADMIN_8', 'ROLE_PATHOLOGY_ADMIN', 'PERM_M6_STAT_INDICATOR_QUERY', CURRENT_TIMESTAMP),
('RP_M6_ADMIN_9', 'ROLE_PATHOLOGY_ADMIN', 'PERM_M6_STAT_TEMPLATE_QUERY', CURRENT_TIMESTAMP),
('RP_M6_ADMIN_10', 'ROLE_PATHOLOGY_ADMIN', 'PERM_M6_STAT_REPORT_QUERY', CURRENT_TIMESTAMP),
('RP_M6_ADMIN_11', 'ROLE_PATHOLOGY_ADMIN', 'PERM_M6_STAT_REPORT_EXPORT', CURRENT_TIMESTAMP),
('RP_M6_ARCHIVE_1', 'ROLE_ARCHIVE_MANAGER', 'PERM_M6_HISTORY_IMPORT', CURRENT_TIMESTAMP),
('RP_M6_ARCHIVE_2', 'ROLE_ARCHIVE_MANAGER', 'PERM_M6_HISTORY_QUERY', CURRENT_TIMESTAMP),
('RP_M6_QUALITY_1', 'ROLE_QUALITY_MANAGER', 'PERM_M6_STAT_INDICATOR_QUERY', CURRENT_TIMESTAMP),
('RP_M6_QUALITY_2', 'ROLE_QUALITY_MANAGER', 'PERM_M6_STAT_TEMPLATE_QUERY', CURRENT_TIMESTAMP),
('RP_M6_QUALITY_3', 'ROLE_QUALITY_MANAGER', 'PERM_M6_STAT_REPORT_QUERY', CURRENT_TIMESTAMP),
('RP_M6_QUALITY_4', 'ROLE_QUALITY_MANAGER', 'PERM_M6_STAT_REPORT_EXPORT', CURRENT_TIMESTAMP);

INSERT INTO stat_indicator_definitions (id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type, description, sort_order, enabled) VALUES
('SID_QC_01', 'QC_SPECIMEN_FIXATION_RATE', 'Specimen Fixation Compliance Rate', 'QUALITY', 'DEPARTMENT', 'RATE', 'M6 quality indicator', 1, 1),
('SID_QC_02', 'QC_UNQUALIFIED_SPECIMEN_COUNT', 'Unqualified Specimen Count', 'QUALITY', 'DEPARTMENT', 'COUNT', 'M6 quality indicator', 2, 1),
('SID_QC_03', 'QC_CLINICAL_MATCH_RATE', 'Clinical Match Rate', 'QUALITY', 'DEPARTMENT', 'RATE', 'M6 quality indicator', 3, 1),
('SID_QC_04', 'QC_FIRST_LINE_MATCH_RATE', 'First Line Match Rate', 'QUALITY', 'DEPARTMENT', 'RATE', 'M6 quality indicator', 4, 1),
('SID_QC_05', 'QC_FROZEN_PARAFFIN_MATCH_RATE', 'Frozen Paraffin Match Rate', 'QUALITY', 'DEPARTMENT', 'RATE', 'M6 quality indicator', 5, 1),
('SID_QC_06', 'QC_CYTOLOGY_MATCH_RATE', 'Cytology Match Rate', 'QUALITY', 'DEPARTMENT', 'RATE', 'M6 quality indicator', 6, 1),
('SID_QC_07', 'QC_CONSULTATION_MATCH_RATE', 'Consultation Match Rate', 'QUALITY', 'DEPARTMENT', 'RATE', 'M6 quality indicator', 7, 1),
('SID_QC_08', 'QC_CANCELLED_REVIEW_COUNT', 'Cancelled Review Count', 'QUALITY', 'DEPARTMENT', 'COUNT', 'M6 quality indicator', 8, 1),
('SID_QC_09', 'QC_TECHNICAL_QUALITY_COUNT', 'Technical Quality Count', 'QUALITY', 'DEPARTMENT', 'COUNT', 'M6 quality indicator', 9, 1),
('SID_QC_10', 'QC_GROSSING_QUALITY_COUNT', 'Grossing Quality Count', 'QUALITY', 'DEPARTMENT', 'COUNT', 'M6 quality indicator', 10, 1),
('SID_QC_11', 'QC_REPORT_RELEASE_DAYS', 'Report Release Days', 'QUALITY', 'DEPARTMENT', 'AVG', 'M6 quality indicator', 11, 1),
('SID_QC_12', 'QC_SPECIMEN_PROCESS_HOURS', 'Specimen Processing Hours', 'QUALITY', 'DEPARTMENT', 'AVG', 'M6 quality indicator', 12, 1),
('SID_QC_13', 'QC_DIAGNOSIS_TIMELINESS_RATE', 'Diagnosis Timeliness Rate', 'QUALITY', 'DEPARTMENT', 'RATE', 'M6 quality indicator', 13, 1),
('SID_OP_01', 'OP_CASE_VOLUME', 'Case Volume', 'OPERATION', 'DEPARTMENT', 'COUNT', 'M6 operation indicator', 21, 1),
('SID_OP_02', 'OP_BILLING_AMOUNT', 'Billing Amount', 'OPERATION', 'DEPARTMENT', 'SUM', 'M6 operation indicator', 22, 1),
('SID_OP_03', 'OP_REAGENT_STOCK_ALERT', 'Reagent Stock Alert Count', 'OPERATION', 'DEPARTMENT', 'COUNT', 'M6 operation indicator', 23, 1),
('SID_OP_04', 'OP_PERFORMANCE_WORKLOAD', 'Performance Workload', 'OPERATION', 'DEPARTMENT', 'COUNT', 'M6 operation indicator', 24, 1),
('SID_WL_01', 'WL_DIAGNOSTIC_TASK_COUNT', 'Diagnostic Task Count', 'WORKLOAD', 'USER', 'COUNT', 'M6 workload indicator', 31, 1),
('SID_WL_02', 'WL_MEDICAL_ORDER_COUNT', 'Medical Order Count', 'WORKLOAD', 'USER', 'COUNT', 'M6 workload indicator', 32, 1);

INSERT INTO stat_report_templates (id, template_code, template_name, template_type, indicator_code, default_columns, parameter_schema, sort_order, enabled) VALUES
('SRT_QC', 'TPL_QC_OVERVIEW', 'Quality Overview', 'QUALITY', NULL, 'indicatorCode,indicatorName,metricValue,metricUnit', '{"from":"datetime","to":"datetime"}', 1, 1),
('SRT_OP', 'TPL_OPERATION_OVERVIEW', 'Operation Overview', 'OPERATION', NULL, 'indicatorCode,indicatorName,metricValue,metricUnit', '{"from":"datetime","to":"datetime"}', 2, 1),
('SRT_WL', 'TPL_WORKLOAD_OVERVIEW', 'Workload Overview', 'WORKLOAD', NULL, 'indicatorCode,indicatorName,metricValue,metricUnit', '{"from":"datetime","to":"datetime","operatorUserId":"string"}', 3, 1),
('SRT_CUSTOM', 'TPL_CUSTOM_BILLING', 'Custom Billing Overview', 'CUSTOM', 'OP_BILLING_AMOUNT', 'indicatorCode,indicatorName,metricValue,metricUnit', '{"from":"datetime","to":"datetime","templateCode":"string"}', 4, 1);
