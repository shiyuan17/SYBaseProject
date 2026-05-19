CREATE TABLE users (
    id VARCHAR(64) NOT NULL,
    user_code VARCHAR(64),
    login_name VARCHAR(64),
    name VARCHAR(100) NOT NULL,
    password VARCHAR(255),
    role VARCHAR(50),
    job_no VARCHAR(64),
    title_name VARCHAR(100),
    department_id VARCHAR(64),
    department_name VARCHAR(100),
    phone VARCHAR(32),
    email VARCHAR(100),
    avatar VARCHAR(500),
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(64),
    last_login_device VARCHAR(200),
    login_tag_code VARCHAR(64),
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_user_code UNIQUE (user_code),
    CONSTRAINT uk_users_login_name UNIQUE (login_name),
    CONSTRAINT uk_users_job_no UNIQUE (job_no),
    CONSTRAINT uk_users_login_tag_code UNIQUE (login_tag_code)
);

CREATE TABLE roles (
    id VARCHAR(64) NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    role_type VARCHAR(50),
    data_scope VARCHAR(50),
    remarks VARCHAR(500),
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_code UNIQUE (role_code)
);

CREATE TABLE menus (
    id VARCHAR(64) NOT NULL,
    parent_id VARCHAR(64),
    menu_code VARCHAR(64) NOT NULL,
    menu_name VARCHAR(100) NOT NULL,
    menu_type VARCHAR(32) NOT NULL,
    path VARCHAR(200),
    component_name VARCHAR(200),
    icon VARCHAR(100),
    permission_prefix VARCHAR(100),
    sort_order INTEGER DEFAULT 0,
    visible INTEGER DEFAULT 1,
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_menus PRIMARY KEY (id),
    CONSTRAINT uk_menus_code UNIQUE (menu_code),
    CONSTRAINT fk_menus_parent FOREIGN KEY (parent_id) REFERENCES menus (id)
);

CREATE TABLE permissions (
    id VARCHAR(64) NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    menu_id VARCHAR(64) NOT NULL,
    action_key VARCHAR(64) NOT NULL,
    http_method VARCHAR(16),
    resource_path VARCHAR(200),
    permission_group VARCHAR(64),
    sort_order INTEGER DEFAULT 0,
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_permissions PRIMARY KEY (id),
    CONSTRAINT uk_permissions_code UNIQUE (permission_code),
    CONSTRAINT uk_permissions_menu_action UNIQUE (menu_id, action_key),
    CONSTRAINT fk_permissions_menu FOREIGN KEY (menu_id) REFERENCES menus (id)
);

CREATE TABLE message_topics (
    id VARCHAR(64) NOT NULL,
    topic_code VARCHAR(64) NOT NULL,
    topic_name VARCHAR(100) NOT NULL,
    topic_category VARCHAR(50),
    description VARCHAR(500),
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_message_topics PRIMARY KEY (id),
    CONSTRAINT uk_message_topics_code UNIQUE (topic_code)
);

CREATE TABLE stat_categories (
    id VARCHAR(64) NOT NULL,
    stat_code VARCHAR(64) NOT NULL,
    stat_name VARCHAR(100) NOT NULL,
    stat_scope VARCHAR(50),
    description VARCHAR(500),
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_stat_categories PRIMARY KEY (id),
    CONSTRAINT uk_stat_categories_code UNIQUE (stat_code)
);

CREATE TABLE body_part_dict (
    id VARCHAR(64) NOT NULL,
    parent_id VARCHAR(64),
    part_code VARCHAR(64) NOT NULL,
    part_name VARCHAR(100) NOT NULL,
    part_alias VARCHAR(100),
    part_level INTEGER DEFAULT 1,
    sort_order INTEGER DEFAULT 0,
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_body_part_dict PRIMARY KEY (id),
    CONSTRAINT uk_body_part_dict_code UNIQUE (part_code),
    CONSTRAINT fk_body_part_dict_parent FOREIGN KEY (parent_id) REFERENCES body_part_dict (id)
);

CREATE TABLE sampling_template_categories (
    id VARCHAR(64) NOT NULL,
    parent_id VARCHAR(64),
    category_code VARCHAR(64) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sampling_template_categories PRIMARY KEY (id),
    CONSTRAINT uk_sampling_template_categories_code UNIQUE (category_code),
    CONSTRAINT fk_sampling_template_categories_parent FOREIGN KEY (parent_id) REFERENCES sampling_template_categories (id)
);

CREATE TABLE sampling_templates (
    id VARCHAR(64) NOT NULL,
    category_id VARCHAR(64) NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    template_content CLOB,
    split_part_count INTEGER DEFAULT 1,
    applicable_specimen_type VARCHAR(100),
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sampling_templates PRIMARY KEY (id),
    CONSTRAINT uk_sampling_templates_code UNIQUE (template_code),
    CONSTRAINT fk_sampling_templates_category FOREIGN KEY (category_id) REFERENCES sampling_template_categories (id)
);

CREATE TABLE sampling_template_site_rel (
    id VARCHAR(64) NOT NULL,
    template_id VARCHAR(64) NOT NULL,
    body_part_id VARCHAR(64) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sampling_template_site_rel PRIMARY KEY (id),
    CONSTRAINT uk_sampling_template_site_rel UNIQUE (template_id, body_part_id),
    CONSTRAINT fk_sampling_template_site_rel_template FOREIGN KEY (template_id) REFERENCES sampling_templates (id),
    CONSTRAINT fk_sampling_template_site_rel_body_part FOREIGN KEY (body_part_id) REFERENCES body_part_dict (id)
);

CREATE TABLE sampling_guideline_categories (
    id VARCHAR(64) NOT NULL,
    parent_id VARCHAR(64),
    category_code VARCHAR(64) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sampling_guideline_categories PRIMARY KEY (id),
    CONSTRAINT uk_sampling_guideline_categories_code UNIQUE (category_code),
    CONSTRAINT fk_sampling_guideline_categories_parent FOREIGN KEY (parent_id) REFERENCES sampling_guideline_categories (id)
);

CREATE TABLE sampling_guidelines (
    id VARCHAR(64) NOT NULL,
    category_id VARCHAR(64) NOT NULL,
    guideline_code VARCHAR(64) NOT NULL,
    guideline_name VARCHAR(100) NOT NULL,
    guideline_content CLOB,
    version_no VARCHAR(32),
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sampling_guidelines PRIMARY KEY (id),
    CONSTRAINT uk_sampling_guidelines_code UNIQUE (guideline_code),
    CONSTRAINT fk_sampling_guidelines_category FOREIGN KEY (category_id) REFERENCES sampling_guideline_categories (id)
);

CREATE TABLE medical_order_dict_categories (
    id VARCHAR(64) NOT NULL,
    parent_id VARCHAR(64),
    category_code VARCHAR(64) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_medical_order_dict_categories PRIMARY KEY (id),
    CONSTRAINT uk_medical_order_dict_categories_code UNIQUE (category_code),
    CONSTRAINT fk_medical_order_dict_categories_parent FOREIGN KEY (parent_id) REFERENCES medical_order_dict_categories (id)
);

CREATE TABLE medical_order_dict_items (
    id VARCHAR(64) NOT NULL,
    category_id VARCHAR(64) NOT NULL,
    order_item_code VARCHAR(64) NOT NULL,
    order_item_name VARCHAR(100) NOT NULL,
    order_type VARCHAR(50),
    default_content VARCHAR(1000),
    execution_scope VARCHAR(50),
    sort_order INTEGER DEFAULT 0,
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_medical_order_dict_items PRIMARY KEY (id),
    CONSTRAINT uk_medical_order_dict_items_code UNIQUE (order_item_code),
    CONSTRAINT fk_medical_order_dict_items_category FOREIGN KEY (category_id) REFERENCES medical_order_dict_categories (id)
);

CREATE TABLE medical_order_charge_items (
    id VARCHAR(64) NOT NULL,
    order_dict_item_id VARCHAR(64) NOT NULL,
    charge_item_code VARCHAR(64) NOT NULL,
    charge_item_name VARCHAR(100) NOT NULL,
    specification VARCHAR(100),
    unit VARCHAR(32),
    price DECIMAL(12, 2),
    sort_order INTEGER DEFAULT 0,
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_medical_order_charge_items PRIMARY KEY (id),
    CONSTRAINT uk_medical_order_charge_items_code UNIQUE (charge_item_code),
    CONSTRAINT fk_medical_order_charge_items_item FOREIGN KEY (order_dict_item_id) REFERENCES medical_order_dict_items (id)
);

CREATE TABLE medical_order_packages (
    id VARCHAR(64) NOT NULL,
    package_code VARCHAR(64) NOT NULL,
    package_name VARCHAR(100) NOT NULL,
    package_type VARCHAR(50),
    owner_user_id VARCHAR(64),
    enabled INTEGER DEFAULT 1,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_medical_order_packages PRIMARY KEY (id),
    CONSTRAINT uk_medical_order_packages_code UNIQUE (package_code),
    CONSTRAINT fk_medical_order_packages_owner FOREIGN KEY (owner_user_id) REFERENCES users (id)
);

CREATE TABLE medical_order_package_items (
    id VARCHAR(64) NOT NULL,
    package_id VARCHAR(64) NOT NULL,
    order_item_id VARCHAR(64) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_medical_order_package_items PRIMARY KEY (id),
    CONSTRAINT uk_medical_order_package_items UNIQUE (package_id, order_item_id),
    CONSTRAINT fk_medical_order_package_items_package FOREIGN KEY (package_id) REFERENCES medical_order_packages (id),
    CONSTRAINT fk_medical_order_package_items_item FOREIGN KEY (order_item_id) REFERENCES medical_order_dict_items (id)
);

CREATE TABLE system_config_categories (
    id VARCHAR(64) NOT NULL,
    parent_id VARCHAR(64),
    category_code VARCHAR(64) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    category_type VARCHAR(50),
    sort_order INTEGER DEFAULT 0,
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_system_config_categories PRIMARY KEY (id),
    CONSTRAINT uk_system_config_categories_code UNIQUE (category_code),
    CONSTRAINT fk_system_config_categories_parent FOREIGN KEY (parent_id) REFERENCES system_config_categories (id)
);

CREATE TABLE system_config_items (
    id VARCHAR(64) NOT NULL,
    category_id VARCHAR(64) NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    config_name VARCHAR(100) NOT NULL,
    config_value CLOB,
    value_type VARCHAR(32),
    sort_order INTEGER DEFAULT 0,
    enabled INTEGER DEFAULT 1,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_system_config_items PRIMARY KEY (id),
    CONSTRAINT uk_system_config_items_key UNIQUE (config_key),
    CONSTRAINT fk_system_config_items_category FOREIGN KEY (category_id) REFERENCES system_config_categories (id)
);

CREATE TABLE user_roles (
    id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    role_id VARCHAR(64) NOT NULL,
    is_primary INTEGER DEFAULT 0,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by_user_id VARCHAR(64),
    assigned_by_name VARCHAR(100),
    CONSTRAINT pk_user_roles PRIMARY KEY (id),
    CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE role_menus (
    id VARCHAR(64) NOT NULL,
    role_id VARCHAR(64) NOT NULL,
    menu_id VARCHAR(64) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_role_menus PRIMARY KEY (id),
    CONSTRAINT uk_role_menus_role_menu UNIQUE (role_id, menu_id),
    CONSTRAINT fk_role_menus_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_menus_menu FOREIGN KEY (menu_id) REFERENCES menus (id)
);

CREATE TABLE role_permissions (
    id VARCHAR(64) NOT NULL,
    role_id VARCHAR(64) NOT NULL,
    permission_id VARCHAR(64) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_role_permissions PRIMARY KEY (id),
    CONSTRAINT uk_role_permissions_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

CREATE TABLE role_message_subscriptions (
    id VARCHAR(64) NOT NULL,
    role_id VARCHAR(64) NOT NULL,
    topic_id VARCHAR(64) NOT NULL,
    subscription_mode VARCHAR(32) DEFAULT 'INBOX',
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_role_message_subscriptions PRIMARY KEY (id),
    CONSTRAINT uk_role_message_subscriptions UNIQUE (role_id, topic_id),
    CONSTRAINT fk_role_message_subscriptions_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_message_subscriptions_topic FOREIGN KEY (topic_id) REFERENCES message_topics (id)
);

CREATE TABLE role_stat_authorizations (
    id VARCHAR(64) NOT NULL,
    role_id VARCHAR(64) NOT NULL,
    stat_category_id VARCHAR(64) NOT NULL,
    auth_scope VARCHAR(32) DEFAULT 'VIEW',
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_role_stat_authorizations PRIMARY KEY (id),
    CONSTRAINT uk_role_stat_authorizations UNIQUE (role_id, stat_category_id),
    CONSTRAINT fk_role_stat_authorizations_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_stat_authorizations_stat FOREIGN KEY (stat_category_id) REFERENCES stat_categories (id)
);

CREATE TABLE user_login_logs (
    id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64),
    login_name VARCHAR(64),
    login_result VARCHAR(32) NOT NULL,
    client_ip VARCHAR(64),
    client_device VARCHAR(200),
    login_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logout_at TIMESTAMP,
    failure_reason VARCHAR(500),
    remarks VARCHAR(500),
    CONSTRAINT pk_user_login_logs PRIMARY KEY (id),
    CONSTRAINT fk_user_login_logs_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE operation_logs (
    id VARCHAR(64) NOT NULL,
    module_code VARCHAR(64) NOT NULL,
    business_type VARCHAR(64),
    business_id VARCHAR(64),
    operation_name VARCHAR(100) NOT NULL,
    operation_result VARCHAR(32) NOT NULL,
    operator_user_id VARCHAR(64),
    operator_name VARCHAR(100),
    operator_ip VARCHAR(64),
    operation_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    operation_content CLOB,
    failure_reason VARCHAR(500),
    CONSTRAINT pk_operation_logs PRIMARY KEY (id),
    CONSTRAINT fk_operation_logs_user FOREIGN KEY (operator_user_id) REFERENCES users (id)
);

CREATE TABLE numbering_rules (
    id VARCHAR(64) NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    biz_type VARCHAR(64) NOT NULL,
    prefix_pattern VARCHAR(64),
    date_pattern VARCHAR(32),
    seq_length INTEGER NOT NULL,
    reset_policy VARCHAR(32) NOT NULL,
    scope_type VARCHAR(32) DEFAULT 'GLOBAL',
    enabled INTEGER DEFAULT 1,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_numbering_rules PRIMARY KEY (id),
    CONSTRAINT uk_numbering_rules_rule_code UNIQUE (rule_code),
    CONSTRAINT uk_numbering_rules_biz_type UNIQUE (biz_type)
);

CREATE TABLE numbering_counters (
    id VARCHAR(64) NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    period_key VARCHAR(32) NOT NULL,
    scope_key VARCHAR(64) NOT NULL,
    current_value BIGINT NOT NULL,
    version INTEGER DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_numbering_counters PRIMARY KEY (id),
    CONSTRAINT uk_numbering_counters_scope UNIQUE (rule_code, period_key, scope_key),
    CONSTRAINT fk_numbering_counters_rule FOREIGN KEY (rule_code) REFERENCES numbering_rules (rule_code)
);

CREATE TABLE applications (
    id VARCHAR(64) NOT NULL,
    application_no VARCHAR(64) NOT NULL,
    patient_id VARCHAR(64),
    application_type VARCHAR(50),
    status VARCHAR(32),
    external_order_no VARCHAR(64),
    third_party_source VARCHAR(64),
    application_form_status VARCHAR(32),
    clinical_diagnosis VARCHAR(500),
    clinical_symptom VARCHAR(500),
    specimen_site VARCHAR(200),
    application_date DATE,
    submission_date DATE,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_applications PRIMARY KEY (id),
    CONSTRAINT uk_applications_application_no UNIQUE (application_no)
);
