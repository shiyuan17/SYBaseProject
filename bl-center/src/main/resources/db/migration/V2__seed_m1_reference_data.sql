INSERT INTO roles (id, role_code, role_name, role_type, data_scope, remarks) VALUES
('ROLE_PATHOLOGY_ADMIN', 'PATHOLOGY_ADMIN', '病理科管理员', 'BUSINESS', 'ALL', '病理系统日常管理角色'),
('ROLE_PATHOLOGY_DOCTOR', 'PATHOLOGY_DOCTOR', '病理医生', 'BUSINESS', 'DEPARTMENT', '病理诊断医生角色'),
('ROLE_PATHOLOGY_TECHNICIAN', 'PATHOLOGY_TECHNICIAN', '病理技师', 'BUSINESS', 'DEPARTMENT', '病理技术人员角色'),
('ROLE_ARCHIVE_MANAGER', 'ARCHIVE_MANAGER', '归档管理员', 'BUSINESS', 'DEPARTMENT', '病理归档与借阅管理角色'),
('ROLE_REAGENT_DEVICE_MANAGER', 'REAGENT_DEVICE_MANAGER', '试剂设备管理员', 'BUSINESS', 'DEPARTMENT', '病理试剂与设备管理角色'),
('ROLE_QUALITY_MANAGER', 'QUALITY_MANAGER', '质控管理员', 'BUSINESS', 'ALL', '病理质控管理角色');

INSERT INTO menus (id, parent_id, menu_code, menu_name, menu_type, path, component_name, permission_prefix, sort_order) VALUES
('MENU_SYSTEM', NULL, 'SYSTEM', '系统管理', 'DIRECTORY', '/system', 'SystemRoot', 'sys', 1),
('MENU_SYS_USERS', 'MENU_SYSTEM', 'SYS_USERS', '系统用户', 'MENU', '/system/users', 'SystemUsers', 'sys:user', 10),
('MENU_SYS_ROLES', 'MENU_SYSTEM', 'SYS_ROLES', '角色授权', 'MENU', '/system/roles', 'Roles', 'sys:role', 20),
('MENU_BODY_PARTS', 'MENU_SYSTEM', 'BODY_PARTS', '部位字典', 'MENU', '/system/body-parts', 'BodyParts', 'md:body-part', 30),
('MENU_ORDER_DICTS', 'MENU_SYSTEM', 'ORDER_DICTS', '医嘱字典', 'MENU', '/system/medical-order-dicts', 'MedicalOrderDicts', 'md:order-dict', 40),
('MENU_ORDER_CHARGES', 'MENU_SYSTEM', 'ORDER_CHARGES', '医嘱收费', 'MENU', '/system/medical-order-charges', 'MedicalOrderCharges', 'md:order-charge', 50),
('MENU_ORDER_PACKAGES', 'MENU_SYSTEM', 'ORDER_PACKAGES', '医嘱套餐', 'MENU', '/system/medical-order-packages', 'MedicalOrderPackages', 'md:order-package', 60),
('MENU_TEMPLATES', 'MENU_SYSTEM', 'SAMPLING_TEMPLATES', '描写模板', 'MENU', '/system/sampling-templates', 'SamplingTemplates', 'md:template', 70),
('MENU_GUIDELINES', 'MENU_SYSTEM', 'SAMPLING_GUIDELINES', '取材规范', 'MENU', '/system/sampling-guidelines', 'SamplingGuidelines', 'md:guideline', 80),
('MENU_CONFIGS', 'MENU_SYSTEM', 'SYSTEM_CONFIGS', '系统配置', 'MENU', '/system/configs', 'SystemConfigs', 'md:config', 90),
('MENU_NUMBERING', 'MENU_SYSTEM', 'NUMBERING_RULES', '编号规则', 'MENU', '/system/numbering-rules', 'NumberingRules', 'support:numbering', 100);

INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order) VALUES
('PERM_SYS_USER_QUERY', 'PERM_SYS_USER_QUERY', '查询用户', 'MENU_SYS_USERS', 'QUERY', 'GET', '/api/v1/system-users', 'SYSTEM', 1),
('PERM_SYS_USER_CREATE', 'PERM_SYS_USER_CREATE', '创建用户', 'MENU_SYS_USERS', 'CREATE', 'POST', '/api/v1/system-users', 'SYSTEM', 2),
('PERM_SYS_USER_UPDATE', 'PERM_SYS_USER_UPDATE', '更新用户', 'MENU_SYS_USERS', 'UPDATE', 'PATCH', '/api/v1/system-users/{id}', 'SYSTEM', 3),
('PERM_SYS_ROLE_QUERY', 'PERM_SYS_ROLE_QUERY', '查询角色', 'MENU_SYS_ROLES', 'QUERY', 'GET', '/api/v1/roles', 'SYSTEM', 4),
('PERM_SYS_ROLE_CREATE', 'PERM_SYS_ROLE_CREATE', '创建角色', 'MENU_SYS_ROLES', 'CREATE', 'POST', '/api/v1/roles', 'SYSTEM', 5),
('PERM_SYS_ROLE_ASSIGN', 'PERM_SYS_ROLE_ASSIGN', '角色授权', 'MENU_SYS_ROLES', 'ASSIGN', 'PUT', '/api/v1/roles/{id}/authorizations', 'SYSTEM', 6),
('PERM_SYS_BODY_PART_QUERY', 'PERM_SYS_BODY_PART_QUERY', '查询部位字典', 'MENU_BODY_PARTS', 'QUERY', 'GET', '/api/v1/body-parts', 'MASTERDATA', 7),
('PERM_SYS_BODY_PART_CREATE', 'PERM_SYS_BODY_PART_CREATE', '维护部位字典', 'MENU_BODY_PARTS', 'CREATE', 'POST', '/api/v1/body-parts', 'MASTERDATA', 8),
('PERM_SYS_ORDER_DICT_QUERY', 'PERM_SYS_ORDER_DICT_QUERY', '查询医嘱字典', 'MENU_ORDER_DICTS', 'QUERY', 'GET', '/api/v1/medical-order-dicts', 'MASTERDATA', 9),
('PERM_SYS_ORDER_DICT_CREATE', 'PERM_SYS_ORDER_DICT_CREATE', '维护医嘱字典', 'MENU_ORDER_DICTS', 'CREATE', 'POST', '/api/v1/medical-order-dicts', 'MASTERDATA', 10),
('PERM_SYS_ORDER_CHARGE_QUERY', 'PERM_SYS_ORDER_CHARGE_QUERY', '查询医嘱收费', 'MENU_ORDER_CHARGES', 'QUERY', 'GET', '/api/v1/medical-order-charge-items', 'MASTERDATA', 11),
('PERM_SYS_ORDER_CHARGE_CREATE', 'PERM_SYS_ORDER_CHARGE_CREATE', '维护医嘱收费', 'MENU_ORDER_CHARGES', 'CREATE', 'POST', '/api/v1/medical-order-charge-items', 'MASTERDATA', 12),
('PERM_SYS_PACKAGE_QUERY', 'PERM_SYS_PACKAGE_QUERY', '查询医嘱套餐', 'MENU_ORDER_PACKAGES', 'QUERY', 'GET', '/api/v1/medical-order-packages', 'MASTERDATA', 13),
('PERM_SYS_PACKAGE_CREATE', 'PERM_SYS_PACKAGE_CREATE', '维护医嘱套餐', 'MENU_ORDER_PACKAGES', 'CREATE', 'POST', '/api/v1/medical-order-packages', 'MASTERDATA', 14),
('PERM_SYS_TEMPLATE_QUERY', 'PERM_SYS_TEMPLATE_QUERY', '查询描写模板', 'MENU_TEMPLATES', 'QUERY', 'GET', '/api/v1/sampling-templates', 'MASTERDATA', 15),
('PERM_SYS_TEMPLATE_CREATE', 'PERM_SYS_TEMPLATE_CREATE', '维护描写模板', 'MENU_TEMPLATES', 'CREATE', 'POST', '/api/v1/sampling-templates', 'MASTERDATA', 16),
('PERM_SYS_GUIDELINE_QUERY', 'PERM_SYS_GUIDELINE_QUERY', '查询取材规范', 'MENU_GUIDELINES', 'QUERY', 'GET', '/api/v1/sampling-guidelines', 'MASTERDATA', 17),
('PERM_SYS_GUIDELINE_CREATE', 'PERM_SYS_GUIDELINE_CREATE', '维护取材规范', 'MENU_GUIDELINES', 'CREATE', 'POST', '/api/v1/sampling-guidelines', 'MASTERDATA', 18),
('PERM_SYS_CONFIG_QUERY', 'PERM_SYS_CONFIG_QUERY', '查询系统配置', 'MENU_CONFIGS', 'QUERY', 'GET', '/api/v1/system-configs', 'MASTERDATA', 19),
('PERM_SYS_CONFIG_UPDATE', 'PERM_SYS_CONFIG_UPDATE', '维护系统配置', 'MENU_CONFIGS', 'UPDATE', 'PATCH', '/api/v1/system-configs/{id}', 'MASTERDATA', 20),
('PERM_SYS_NUMBERING_QUERY', 'PERM_SYS_NUMBERING_QUERY', '查询编号规则', 'MENU_NUMBERING', 'QUERY', 'GET', '/api/v1/numbering-rules', 'SUPPORT', 21),
('PERM_SYS_NUMBERING_UPDATE', 'PERM_SYS_NUMBERING_UPDATE', '维护编号规则', 'MENU_NUMBERING', 'UPDATE', 'PATCH', '/api/v1/numbering-rules/{id}', 'SUPPORT', 22);

INSERT INTO message_topics (id, topic_code, topic_name, topic_category, description) VALUES
('TOPIC_CRITICAL_VALUE', 'CRITICAL_VALUE', '危急值通知', 'QUALITY', '危急值相关消息'),
('TOPIC_REPORT_REVISION', 'REPORT_REVISION', '报告修订通知', 'REPORT', '报告修订相关消息'),
('TOPIC_CONSULTATION', 'CONSULTATION', '会诊通知', 'REPORT', '会诊相关消息'),
('TOPIC_QC_WARNING', 'QC_WARNING', '质控预警', 'QUALITY', '质控预警消息');

INSERT INTO stat_categories (id, stat_code, stat_name, stat_scope, description) VALUES
('STAT_OPERATION', 'OPERATION_STAT', '运营统计', 'ALL', '病理运营统计'),
('STAT_WORKLOAD', 'WORKLOAD_STAT', '工作量统计', 'DEPARTMENT', '工作量统计'),
('STAT_QUALITY', 'QUALITY_STAT', '病理质控指标', 'ALL', '三甲评审及病理质控核心指标');

INSERT INTO body_part_dict (id, parent_id, part_code, part_name, part_alias, part_level, sort_order) VALUES
('BP_ROOT', NULL, 'ROOT', '全部部位', NULL, 0, 0),
('BP_DIGESTIVE', 'BP_ROOT', 'DIGESTIVE', '消化系统', NULL, 1, 1),
('BP_STOMACH', 'BP_DIGESTIVE', 'STOMACH', '胃', NULL, 2, 10),
('BP_COLON', 'BP_DIGESTIVE', 'COLON', '结肠', NULL, 2, 20);

INSERT INTO medical_order_dict_categories (id, parent_id, category_code, category_name, sort_order) VALUES
('ODC_ROOT', NULL, 'ROOT', '全部医嘱', 0),
('ODC_ROUTINE', 'ODC_ROOT', 'ROUTINE', '常规医嘱', 10),
('ODC_SPECIAL', 'ODC_ROOT', 'SPECIAL', '特检医嘱', 20);

INSERT INTO medical_order_dict_items (id, category_id, order_item_code, order_item_name, order_type, default_content, execution_scope, sort_order) VALUES
('ODI_HE', 'ODC_ROUTINE', 'HE', 'HE染色', 'ROUTINE', 'HE 染色', 'TECHNICIAN', 10),
('ODI_IHC', 'ODC_SPECIAL', 'IHC', '免疫组化', 'SPECIAL', '免疫组化', 'TECHNICIAN', 20);

INSERT INTO medical_order_charge_items (id, order_dict_item_id, charge_item_code, charge_item_name, specification, unit, price, sort_order) VALUES
('OCI_HE', 'ODI_HE', 'CHG_HE', 'HE染色收费', '次', '次', 20.00, 10),
('OCI_IHC', 'ODI_IHC', 'CHG_IHC', '免疫组化收费', '次', '次', 120.00, 20);

INSERT INTO sampling_template_categories (id, parent_id, category_code, category_name, sort_order) VALUES
('STC_ROOT', NULL, 'ROOT', '模板根目录', 0),
('STC_ROUTINE', 'STC_ROOT', 'ROUTINE', '常规模板', 10);

INSERT INTO sampling_templates (id, category_id, template_code, template_name, template_content, split_part_count, applicable_specimen_type) VALUES
('ST_HE_STOMACH', 'STC_ROUTINE', 'TPL_STOMACH', '胃活检模板', '胃黏膜灰白组织若干，送检。', 1, 'ROUTINE');

INSERT INTO sampling_template_site_rel (id, template_id, body_part_id, sort_order) VALUES
('STSR_1', 'ST_HE_STOMACH', 'BP_STOMACH', 1);

INSERT INTO sampling_guideline_categories (id, parent_id, category_code, category_name, sort_order) VALUES
('SGC_ROOT', NULL, 'ROOT', '规范根目录', 0),
('SGC_ROUTINE', 'SGC_ROOT', 'ROUTINE', '常规规范', 10);

INSERT INTO sampling_guidelines (id, category_id, guideline_code, guideline_name, guideline_content, version_no) VALUES
('SG_STOMACH', 'SGC_ROUTINE', 'GL_STOMACH', '胃活检取材规范', '按送检块数逐一包埋，记录部位及数量。', '1.0');

INSERT INTO system_config_categories (id, parent_id, category_code, category_name, category_type, sort_order) VALUES
('SCC_ROOT', NULL, 'ROOT', '配置根目录', 'CONFIG', 0),
('SCC_GENERAL', 'SCC_ROOT', 'GENERAL', '通用配置', 'CONFIG', 10),
('SCC_ENUM', 'SCC_ROOT', 'ENUM', '系统枚举', 'ENUM', 20);

INSERT INTO system_config_items (id, category_id, config_key, config_name, config_value, value_type, sort_order, remarks) VALUES
('SCI_DEFAULT_SCOPE', 'SCC_GENERAL', 'system.defaultScope', '默认数据范围', 'DEPARTMENT', 'STRING', 10, '默认数据范围'),
('SCI_TEMPLATE_MATCH', 'SCC_GENERAL', 'sampling.templateMatchEnabled', '模板智能匹配开关', 'true', 'BOOLEAN', 20, '模板智能匹配开关');

INSERT INTO numbering_rules (id, rule_code, biz_type, prefix_pattern, date_pattern, seq_length, reset_policy, scope_type, remarks) VALUES
('NR_APPLICATION', 'RULE_APPLICATION_NO', 'APPLICATION_NO', 'AP', 'yyyyMMdd', 4, 'DAILY', 'GLOBAL', '申请单号'),
('NR_PATHOLOGY', 'RULE_PATHOLOGY_NO', 'PATHOLOGY_NO', 'BL', 'yyyyMMdd', 4, 'DAILY', 'GLOBAL', '病理号'),
('NR_SPECIMEN', 'RULE_SPECIMEN_NO', 'SPECIMEN_NO', 'SP', 'yyyyMMdd', 3, 'DAILY', 'CASE', '标本号'),
('NR_BLOCK', 'RULE_BLOCK_NO', 'BLOCK_NO', 'BK', 'yyyyMMdd', 3, 'DAILY', 'CASE', '蜡块号'),
('NR_SLIDE', 'RULE_SLIDE_NO', 'SLIDE_NO', 'SL', 'yyyyMMdd', 3, 'DAILY', 'GLOBAL', '玻片号');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at) VALUES
('RP_ADMIN_USER_QUERY', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_USER_QUERY', CURRENT_TIMESTAMP),
('RP_ADMIN_USER_CREATE', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_USER_CREATE', CURRENT_TIMESTAMP),
('RP_ADMIN_USER_UPDATE', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_USER_UPDATE', CURRENT_TIMESTAMP),
('RP_ADMIN_ROLE_QUERY', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_ROLE_QUERY', CURRENT_TIMESTAMP),
('RP_ADMIN_ROLE_CREATE', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_ROLE_CREATE', CURRENT_TIMESTAMP),
('RP_ADMIN_ROLE_ASSIGN', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_ROLE_ASSIGN', CURRENT_TIMESTAMP),
('RP_ADMIN_BODY_PART_QUERY', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_BODY_PART_QUERY', CURRENT_TIMESTAMP),
('RP_ADMIN_BODY_PART_CREATE', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_BODY_PART_CREATE', CURRENT_TIMESTAMP),
('RP_ADMIN_ORDER_DICT_QUERY', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_ORDER_DICT_QUERY', CURRENT_TIMESTAMP),
('RP_ADMIN_ORDER_DICT_CREATE', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_ORDER_DICT_CREATE', CURRENT_TIMESTAMP),
('RP_ADMIN_ORDER_CHARGE_QUERY', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_ORDER_CHARGE_QUERY', CURRENT_TIMESTAMP),
('RP_ADMIN_ORDER_CHARGE_CREATE', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_ORDER_CHARGE_CREATE', CURRENT_TIMESTAMP),
('RP_ADMIN_PACKAGE_QUERY', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_PACKAGE_QUERY', CURRENT_TIMESTAMP),
('RP_ADMIN_PACKAGE_CREATE', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_PACKAGE_CREATE', CURRENT_TIMESTAMP),
('RP_ADMIN_TEMPLATE_QUERY', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_TEMPLATE_QUERY', CURRENT_TIMESTAMP),
('RP_ADMIN_TEMPLATE_CREATE', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_TEMPLATE_CREATE', CURRENT_TIMESTAMP),
('RP_ADMIN_GUIDELINE_QUERY', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_GUIDELINE_QUERY', CURRENT_TIMESTAMP),
('RP_ADMIN_GUIDELINE_CREATE', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_GUIDELINE_CREATE', CURRENT_TIMESTAMP),
('RP_ADMIN_CONFIG_QUERY', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_CONFIG_QUERY', CURRENT_TIMESTAMP),
('RP_ADMIN_CONFIG_UPDATE', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_CONFIG_UPDATE', CURRENT_TIMESTAMP),
('RP_ADMIN_NUMBERING_QUERY', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_NUMBERING_QUERY', CURRENT_TIMESTAMP),
('RP_ADMIN_NUMBERING_UPDATE', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SYS_NUMBERING_UPDATE', CURRENT_TIMESTAMP);

INSERT INTO role_message_subscriptions (id, role_id, topic_id, subscription_mode, assigned_at) VALUES
('RMS_ADMIN_CRITICAL', 'ROLE_PATHOLOGY_ADMIN', 'TOPIC_CRITICAL_VALUE', 'POPUP', CURRENT_TIMESTAMP),
('RMS_ADMIN_REVISION', 'ROLE_PATHOLOGY_ADMIN', 'TOPIC_REPORT_REVISION', 'INBOX', CURRENT_TIMESTAMP),
('RMS_DOCTOR_REVISION', 'ROLE_PATHOLOGY_DOCTOR', 'TOPIC_REPORT_REVISION', 'INBOX', CURRENT_TIMESTAMP),
('RMS_TECH_QC', 'ROLE_PATHOLOGY_TECHNICIAN', 'TOPIC_QC_WARNING', 'POPUP', CURRENT_TIMESTAMP);

INSERT INTO role_stat_authorizations (id, role_id, stat_category_id, auth_scope, assigned_at) VALUES
('RSA_ADMIN_OPERATION', 'ROLE_PATHOLOGY_ADMIN', 'STAT_OPERATION', 'MANAGE', CURRENT_TIMESTAMP),
('RSA_ADMIN_WORKLOAD', 'ROLE_PATHOLOGY_ADMIN', 'STAT_WORKLOAD', 'MANAGE', CURRENT_TIMESTAMP),
('RSA_ADMIN_QUALITY', 'ROLE_PATHOLOGY_ADMIN', 'STAT_QUALITY', 'MANAGE', CURRENT_TIMESTAMP),
('RSA_DOCTOR_WORKLOAD', 'ROLE_PATHOLOGY_DOCTOR', 'STAT_WORKLOAD', 'VIEW', CURRENT_TIMESTAMP),
('RSA_QM_QUALITY', 'ROLE_QUALITY_MANAGER', 'STAT_QUALITY', 'MANAGE', CURRENT_TIMESTAMP);
