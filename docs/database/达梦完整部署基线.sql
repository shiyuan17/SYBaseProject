-- 达梦数据库表结构整理（病理全流程中度优化版）
-- 来源: D:/SVN-Project/BlSysNew/pathology.db
-- 生成日期: 2026-05-19
-- 用途: 病理全流程管理系统数据库设计参考
-- 说明:
-- 1. 本版本按“病例主表 + 流程主链路 + 扩展流程 + 审计日志”进行重整。
-- 2. 字段命名统一采用英文下划线风格，优先使用 VARCHAR2、NUMBER、DATE、TIMESTAMP、CLOB。
-- 3. 所有关键人员、科室字段采用“ID + 名称快照”并存，便于统计与历史追溯。
-- 4. 原 reports、sections、stainings、specimen_archives 已被职责更清晰的新结构替代。
-- 5. 状态码和类型码原则上通过字段注释约定，系统管理模块涉及的通用字典和业务字典单独建表。

-- =========================================================
-- 基础主数据
-- =========================================================
CREATE TABLE patients (
    id VARCHAR2(64) NOT NULL,
    patient_no VARCHAR2(64),
    name VARCHAR2(100) NOT NULL,
    gender VARCHAR2(20),
    age NUMBER(3),
    birth_date DATE,
    inpatient_no VARCHAR2(64),
    outpatient_no VARCHAR2(64),
    phone VARCHAR2(32),
    id_card_no VARCHAR2(32),
    address VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_patients PRIMARY KEY (id),
    CONSTRAINT uk_patients_patient_no UNIQUE (patient_no)
);

COMMENT ON TABLE patients IS '患者主数据表';
COMMENT ON COLUMN patients.id IS '主键ID';
COMMENT ON COLUMN patients.patient_no IS '患者编号';
COMMENT ON COLUMN patients.name IS '患者姓名';
COMMENT ON COLUMN patients.gender IS '性别';
COMMENT ON COLUMN patients.age IS '年龄';
COMMENT ON COLUMN patients.birth_date IS '出生日期';
COMMENT ON COLUMN patients.inpatient_no IS '住院号';
COMMENT ON COLUMN patients.outpatient_no IS '门诊号';
COMMENT ON COLUMN patients.phone IS '联系电话';
COMMENT ON COLUMN patients.id_card_no IS '身份证号';
COMMENT ON COLUMN patients.address IS '联系地址';
COMMENT ON COLUMN patients.created_at IS '创建时间';
COMMENT ON COLUMN patients.updated_at IS '更新时间';

CREATE TABLE users (
    id VARCHAR2(64) NOT NULL,
    user_code VARCHAR2(64),
    login_name VARCHAR2(64),
    name VARCHAR2(100) NOT NULL,
    password VARCHAR2(255),
    password_algo VARCHAR2(32),
    password_salt VARCHAR2(64),
    role VARCHAR2(50),
    job_no VARCHAR2(64),
    title_name VARCHAR2(100),
    department_id VARCHAR2(64),
    department_name VARCHAR2(100),
    phone VARCHAR2(32),
    email VARCHAR2(100),
    avatar VARCHAR2(500),
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR2(64),
    last_login_device VARCHAR2(200),
    login_tag_code VARCHAR2(64),
    enabled NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT ck_users_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_users_user_code UNIQUE (user_code),
    CONSTRAINT uk_users_login_name UNIQUE (login_name),
    CONSTRAINT uk_users_job_no UNIQUE (job_no),
    CONSTRAINT uk_users_login_tag_code UNIQUE (login_tag_code)
);

COMMENT ON TABLE users IS '系统用户表';
COMMENT ON COLUMN users.id IS '主键ID';
COMMENT ON COLUMN users.user_code IS '用户编码';
COMMENT ON COLUMN users.login_name IS '登录账号';
COMMENT ON COLUMN users.name IS '用户姓名';
COMMENT ON COLUMN users.password IS '登录密码';
COMMENT ON COLUMN users.password_algo IS '密码算法';
COMMENT ON COLUMN users.password_salt IS '密码盐值';
COMMENT ON COLUMN users.role IS '角色编码';
COMMENT ON COLUMN users.job_no IS '工号';
COMMENT ON COLUMN users.title_name IS '职称';
COMMENT ON COLUMN users.department_id IS '所属科室ID';
COMMENT ON COLUMN users.department_name IS '所属科室名称快照';
COMMENT ON COLUMN users.phone IS '联系电话';
COMMENT ON COLUMN users.email IS '电子邮箱';
COMMENT ON COLUMN users.avatar IS '头像地址';
COMMENT ON COLUMN users.last_login_at IS '最近登录时间';
COMMENT ON COLUMN users.last_login_ip IS '最近登录IP';
COMMENT ON COLUMN users.last_login_device IS '最近登录设备';
COMMENT ON COLUMN users.login_tag_code IS '登录标签编码';
COMMENT ON COLUMN users.enabled IS '是否启用，0否1是';
COMMENT ON COLUMN users.created_at IS '创建时间';
COMMENT ON COLUMN users.updated_at IS '更新时间';

-- =========================================================
-- 系统管理与基础配置
-- =========================================================
CREATE TABLE departments (
    id VARCHAR2(64) NOT NULL,
    parent_id VARCHAR2(64),
    department_code VARCHAR2(64) NOT NULL,
    department_name VARCHAR2(100) NOT NULL,
    department_type VARCHAR2(50),
    sort_order NUMBER(10) DEFAULT 0,
    leader_user_id VARCHAR2(64),
    leader_name VARCHAR2(100),
    contact_phone VARCHAR2(32),
    enabled NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_departments PRIMARY KEY (id),
    CONSTRAINT ck_departments_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_departments_code UNIQUE (department_code),
    CONSTRAINT fk_departments_parent FOREIGN KEY (parent_id) REFERENCES departments (id)
);

COMMENT ON TABLE departments IS '科室/组织架构表';
COMMENT ON COLUMN departments.id IS '主键ID';
COMMENT ON COLUMN departments.parent_id IS '父级科室ID';
COMMENT ON COLUMN departments.department_code IS '科室编码';
COMMENT ON COLUMN departments.department_name IS '科室名称';
COMMENT ON COLUMN departments.department_type IS '科室类型，示例：HOSPITAL/DEPARTMENT/GROUP';
COMMENT ON COLUMN departments.sort_order IS '排序号';
COMMENT ON COLUMN departments.leader_user_id IS '负责人用户ID';
COMMENT ON COLUMN departments.leader_name IS '负责人姓名快照';
COMMENT ON COLUMN departments.contact_phone IS '联系电话';
COMMENT ON COLUMN departments.enabled IS '是否启用，0否1是';
COMMENT ON COLUMN departments.created_at IS '创建时间';
COMMENT ON COLUMN departments.updated_at IS '更新时间';

CREATE TABLE roles (
    id VARCHAR2(64) NOT NULL,
    role_code VARCHAR2(64) NOT NULL,
    role_name VARCHAR2(100) NOT NULL,
    role_type VARCHAR2(50),
    data_scope VARCHAR2(50),
    remarks VARCHAR2(500),
    enabled NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT ck_roles_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_roles_code UNIQUE (role_code)
);

COMMENT ON TABLE roles IS '系统角色表';
COMMENT ON COLUMN roles.id IS '主键ID';
COMMENT ON COLUMN roles.role_code IS '角色编码';
COMMENT ON COLUMN roles.role_name IS '角色名称';
COMMENT ON COLUMN roles.role_type IS '角色类型';
COMMENT ON COLUMN roles.data_scope IS '数据权限范围，示例：ALL/DEPARTMENT/SELF';
COMMENT ON COLUMN roles.remarks IS '备注';
COMMENT ON COLUMN roles.enabled IS '是否启用，0否1是';
COMMENT ON COLUMN roles.created_at IS '创建时间';
COMMENT ON COLUMN roles.updated_at IS '更新时间';

CREATE TABLE menus (
    id VARCHAR2(64) NOT NULL,
    parent_id VARCHAR2(64),
    menu_code VARCHAR2(64) NOT NULL,
    menu_name VARCHAR2(100) NOT NULL,
    menu_type VARCHAR2(32) NOT NULL,
    path VARCHAR2(200),
    component_name VARCHAR2(200),
    icon VARCHAR2(100),
    permission_prefix VARCHAR2(100),
    sort_order NUMBER(10) DEFAULT 0,
    visible NUMBER(1) DEFAULT 1,
    enabled NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_menus PRIMARY KEY (id),
    CONSTRAINT ck_menus_visible CHECK (visible IN (0, 1)),
    CONSTRAINT ck_menus_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_menus_code UNIQUE (menu_code),
    CONSTRAINT fk_menus_parent FOREIGN KEY (parent_id) REFERENCES menus (id)
);

COMMENT ON TABLE menus IS '系统菜单表';
COMMENT ON COLUMN menus.id IS '主键ID';
COMMENT ON COLUMN menus.parent_id IS '父级菜单ID';
COMMENT ON COLUMN menus.menu_code IS '菜单编码';
COMMENT ON COLUMN menus.menu_name IS '菜单名称';
COMMENT ON COLUMN menus.menu_type IS '菜单类型，示例：DIRECTORY/MENU';
COMMENT ON COLUMN menus.path IS '前端路由路径';
COMMENT ON COLUMN menus.component_name IS '前端组件标识';
COMMENT ON COLUMN menus.icon IS '菜单图标';
COMMENT ON COLUMN menus.permission_prefix IS '权限前缀';
COMMENT ON COLUMN menus.sort_order IS '排序号';
COMMENT ON COLUMN menus.visible IS '是否可见，0否1是';
COMMENT ON COLUMN menus.enabled IS '是否启用，0否1是';
COMMENT ON COLUMN menus.created_at IS '创建时间';
COMMENT ON COLUMN menus.updated_at IS '更新时间';

CREATE TABLE permissions (
    id VARCHAR2(64) NOT NULL,
    permission_code VARCHAR2(100) NOT NULL,
    permission_name VARCHAR2(100) NOT NULL,
    menu_id VARCHAR2(64) NOT NULL,
    action_key VARCHAR2(64) NOT NULL,
    http_method VARCHAR2(16),
    resource_path VARCHAR2(200),
    permission_group VARCHAR2(64),
    sort_order NUMBER(10) DEFAULT 0,
    enabled NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_permissions PRIMARY KEY (id),
    CONSTRAINT ck_permissions_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_permissions_code UNIQUE (permission_code),
    CONSTRAINT uk_permissions_menu_action UNIQUE (menu_id, action_key),
    CONSTRAINT fk_permissions_menu FOREIGN KEY (menu_id) REFERENCES menus (id)
);

COMMENT ON TABLE permissions IS '系统权限表';
COMMENT ON COLUMN permissions.id IS '主键ID';
COMMENT ON COLUMN permissions.permission_code IS '权限编码';
COMMENT ON COLUMN permissions.permission_name IS '权限名称';
COMMENT ON COLUMN permissions.menu_id IS '所属菜单ID';
COMMENT ON COLUMN permissions.action_key IS '动作标识';
COMMENT ON COLUMN permissions.http_method IS 'HTTP方法';
COMMENT ON COLUMN permissions.resource_path IS '资源路径';
COMMENT ON COLUMN permissions.permission_group IS '权限分组';
COMMENT ON COLUMN permissions.sort_order IS '排序号';
COMMENT ON COLUMN permissions.enabled IS '是否启用，0否1是';
COMMENT ON COLUMN permissions.created_at IS '创建时间';
COMMENT ON COLUMN permissions.updated_at IS '更新时间';

CREATE TABLE message_topics (
    id VARCHAR2(64) NOT NULL,
    topic_code VARCHAR2(64) NOT NULL,
    topic_name VARCHAR2(100) NOT NULL,
    topic_category VARCHAR2(50),
    description VARCHAR2(500),
    enabled NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_message_topics PRIMARY KEY (id),
    CONSTRAINT ck_message_topics_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_message_topics_code UNIQUE (topic_code)
);

COMMENT ON TABLE message_topics IS '消息订阅主题表';
COMMENT ON COLUMN message_topics.id IS '主键ID';
COMMENT ON COLUMN message_topics.topic_code IS '主题编码';
COMMENT ON COLUMN message_topics.topic_name IS '主题名称';
COMMENT ON COLUMN message_topics.topic_category IS '主题分类';
COMMENT ON COLUMN message_topics.description IS '主题说明';
COMMENT ON COLUMN message_topics.enabled IS '是否启用，0否1是';
COMMENT ON COLUMN message_topics.created_at IS '创建时间';
COMMENT ON COLUMN message_topics.updated_at IS '更新时间';

CREATE TABLE stat_categories (
    id VARCHAR2(64) NOT NULL,
    stat_code VARCHAR2(64) NOT NULL,
    stat_name VARCHAR2(100) NOT NULL,
    stat_scope VARCHAR2(50),
    description VARCHAR2(500),
    enabled NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_stat_categories PRIMARY KEY (id),
    CONSTRAINT ck_stat_categories_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_stat_categories_code UNIQUE (stat_code)
);

COMMENT ON TABLE stat_categories IS '统计类型授权表';
COMMENT ON COLUMN stat_categories.id IS '主键ID';
COMMENT ON COLUMN stat_categories.stat_code IS '统计类型编码';
COMMENT ON COLUMN stat_categories.stat_name IS '统计类型名称';
COMMENT ON COLUMN stat_categories.stat_scope IS '统计适用范围';
COMMENT ON COLUMN stat_categories.description IS '统计类型说明';
COMMENT ON COLUMN stat_categories.enabled IS '是否启用，0否1是';
COMMENT ON COLUMN stat_categories.created_at IS '创建时间';
COMMENT ON COLUMN stat_categories.updated_at IS '更新时间';

CREATE TABLE body_part_dict (
    id VARCHAR2(64) NOT NULL,
    parent_id VARCHAR2(64),
    part_code VARCHAR2(64) NOT NULL,
    part_name VARCHAR2(100) NOT NULL,
    part_alias VARCHAR2(100),
    part_level NUMBER(3) DEFAULT 1,
    sort_order NUMBER(10) DEFAULT 0,
    enabled NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_body_part_dict PRIMARY KEY (id),
    CONSTRAINT ck_body_part_dict_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_body_part_dict_code UNIQUE (part_code),
    CONSTRAINT fk_body_part_dict_parent FOREIGN KEY (parent_id) REFERENCES body_part_dict (id)
);

COMMENT ON TABLE body_part_dict IS '部位字典表';
COMMENT ON COLUMN body_part_dict.id IS '主键ID';
COMMENT ON COLUMN body_part_dict.parent_id IS '父级部位ID';
COMMENT ON COLUMN body_part_dict.part_code IS '部位编码';
COMMENT ON COLUMN body_part_dict.part_name IS '部位名称';
COMMENT ON COLUMN body_part_dict.part_alias IS '部位别名';
COMMENT ON COLUMN body_part_dict.part_level IS '层级';
COMMENT ON COLUMN body_part_dict.sort_order IS '排序号';
COMMENT ON COLUMN body_part_dict.enabled IS '是否启用，0否1是';
COMMENT ON COLUMN body_part_dict.created_at IS '创建时间';
COMMENT ON COLUMN body_part_dict.updated_at IS '更新时间';

CREATE TABLE sampling_template_categories (
    id VARCHAR2(64) NOT NULL,
    parent_id VARCHAR2(64),
    category_code VARCHAR2(64) NOT NULL,
    category_name VARCHAR2(100) NOT NULL,
    sort_order NUMBER(10) DEFAULT 0,
    enabled NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sampling_template_categories PRIMARY KEY (id),
    CONSTRAINT ck_sampling_template_categories_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_sampling_template_categories_code UNIQUE (category_code),
    CONSTRAINT fk_sampling_template_categories_parent FOREIGN KEY (parent_id) REFERENCES sampling_template_categories (id)
);

COMMENT ON TABLE sampling_template_categories IS '描写模板分类表';
COMMENT ON COLUMN sampling_template_categories.id IS '主键ID';
COMMENT ON COLUMN sampling_template_categories.parent_id IS '父级分类ID';
COMMENT ON COLUMN sampling_template_categories.category_code IS '分类编码';
COMMENT ON COLUMN sampling_template_categories.category_name IS '分类名称';
COMMENT ON COLUMN sampling_template_categories.sort_order IS '排序号';
COMMENT ON COLUMN sampling_template_categories.enabled IS '是否启用，0否1是';
COMMENT ON COLUMN sampling_template_categories.created_at IS '创建时间';
COMMENT ON COLUMN sampling_template_categories.updated_at IS '更新时间';

CREATE TABLE sampling_templates (
    id VARCHAR2(64) NOT NULL,
    category_id VARCHAR2(64) NOT NULL,
    template_code VARCHAR2(64) NOT NULL,
    template_name VARCHAR2(100) NOT NULL,
    template_content CLOB,
    split_part_count NUMBER(10) DEFAULT 1,
    applicable_specimen_type VARCHAR2(100),
    enabled NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sampling_templates PRIMARY KEY (id),
    CONSTRAINT ck_sampling_templates_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_sampling_templates_code UNIQUE (template_code),
    CONSTRAINT fk_sampling_templates_category FOREIGN KEY (category_id) REFERENCES sampling_template_categories (id)
);

COMMENT ON TABLE sampling_templates IS '描写模板表';
COMMENT ON COLUMN sampling_templates.id IS '主键ID';
COMMENT ON COLUMN sampling_templates.category_id IS '分类ID';
COMMENT ON COLUMN sampling_templates.template_code IS '模板编码';
COMMENT ON COLUMN sampling_templates.template_name IS '模板名称';
COMMENT ON COLUMN sampling_templates.template_content IS '模板内容';
COMMENT ON COLUMN sampling_templates.split_part_count IS '模板切分部位数量';
COMMENT ON COLUMN sampling_templates.applicable_specimen_type IS '适用标本类型';
COMMENT ON COLUMN sampling_templates.enabled IS '是否启用，0否1是';
COMMENT ON COLUMN sampling_templates.created_at IS '创建时间';
COMMENT ON COLUMN sampling_templates.updated_at IS '更新时间';

CREATE TABLE sampling_template_site_rel (
    id VARCHAR2(64) NOT NULL,
    template_id VARCHAR2(64) NOT NULL,
    body_part_id VARCHAR2(64) NOT NULL,
    sort_order NUMBER(10) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sampling_template_site_rel PRIMARY KEY (id),
    CONSTRAINT uk_sampling_template_site_rel UNIQUE (template_id, body_part_id),
    CONSTRAINT fk_sampling_template_site_rel_template FOREIGN KEY (template_id) REFERENCES sampling_templates (id),
    CONSTRAINT fk_sampling_template_site_rel_body_part FOREIGN KEY (body_part_id) REFERENCES body_part_dict (id)
);

COMMENT ON TABLE sampling_template_site_rel IS '描写模板适用部位关联表';
COMMENT ON COLUMN sampling_template_site_rel.id IS '主键ID';
COMMENT ON COLUMN sampling_template_site_rel.template_id IS '模板ID';
COMMENT ON COLUMN sampling_template_site_rel.body_part_id IS '部位ID';
COMMENT ON COLUMN sampling_template_site_rel.sort_order IS '排序号';
COMMENT ON COLUMN sampling_template_site_rel.created_at IS '创建时间';

CREATE TABLE sampling_guideline_categories (
    id VARCHAR2(64) NOT NULL,
    parent_id VARCHAR2(64),
    category_code VARCHAR2(64) NOT NULL,
    category_name VARCHAR2(100) NOT NULL,
    sort_order NUMBER(10) DEFAULT 0,
    enabled NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sampling_guideline_categories PRIMARY KEY (id),
    CONSTRAINT ck_sampling_guideline_categories_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_sampling_guideline_categories_code UNIQUE (category_code),
    CONSTRAINT fk_sampling_guideline_categories_parent FOREIGN KEY (parent_id) REFERENCES sampling_guideline_categories (id)
);

COMMENT ON TABLE sampling_guideline_categories IS '取材规范分类表';
COMMENT ON COLUMN sampling_guideline_categories.id IS '主键ID';
COMMENT ON COLUMN sampling_guideline_categories.parent_id IS '父级分类ID';
COMMENT ON COLUMN sampling_guideline_categories.category_code IS '分类编码';
COMMENT ON COLUMN sampling_guideline_categories.category_name IS '分类名称';
COMMENT ON COLUMN sampling_guideline_categories.sort_order IS '排序号';
COMMENT ON COLUMN sampling_guideline_categories.enabled IS '是否启用，0否1是';
COMMENT ON COLUMN sampling_guideline_categories.created_at IS '创建时间';
COMMENT ON COLUMN sampling_guideline_categories.updated_at IS '更新时间';

CREATE TABLE sampling_guidelines (
    id VARCHAR2(64) NOT NULL,
    category_id VARCHAR2(64) NOT NULL,
    guideline_code VARCHAR2(64) NOT NULL,
    guideline_name VARCHAR2(100) NOT NULL,
    guideline_content CLOB,
    version_no VARCHAR2(32),
    enabled NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sampling_guidelines PRIMARY KEY (id),
    CONSTRAINT ck_sampling_guidelines_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_sampling_guidelines_code UNIQUE (guideline_code),
    CONSTRAINT fk_sampling_guidelines_category FOREIGN KEY (category_id) REFERENCES sampling_guideline_categories (id)
);

COMMENT ON TABLE sampling_guidelines IS '取材规范库表';
COMMENT ON COLUMN sampling_guidelines.id IS '主键ID';
COMMENT ON COLUMN sampling_guidelines.category_id IS '分类ID';
COMMENT ON COLUMN sampling_guidelines.guideline_code IS '规范编码';
COMMENT ON COLUMN sampling_guidelines.guideline_name IS '规范名称';
COMMENT ON COLUMN sampling_guidelines.guideline_content IS '规范内容';
COMMENT ON COLUMN sampling_guidelines.version_no IS '版本号';
COMMENT ON COLUMN sampling_guidelines.enabled IS '是否启用，0否1是';
COMMENT ON COLUMN sampling_guidelines.created_at IS '创建时间';
COMMENT ON COLUMN sampling_guidelines.updated_at IS '更新时间';

CREATE TABLE medical_order_dict_categories (
    id VARCHAR2(64) NOT NULL,
    parent_id VARCHAR2(64),
    category_code VARCHAR2(64) NOT NULL,
    category_name VARCHAR2(100) NOT NULL,
    sort_order NUMBER(10) DEFAULT 0,
    enabled NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_medical_order_dict_categories PRIMARY KEY (id),
    CONSTRAINT ck_medical_order_dict_categories_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_medical_order_dict_categories_code UNIQUE (category_code),
    CONSTRAINT fk_medical_order_dict_categories_parent FOREIGN KEY (parent_id) REFERENCES medical_order_dict_categories (id)
);

COMMENT ON TABLE medical_order_dict_categories IS '医嘱字典分类表';
COMMENT ON COLUMN medical_order_dict_categories.id IS '主键ID';
COMMENT ON COLUMN medical_order_dict_categories.parent_id IS '父级分类ID';
COMMENT ON COLUMN medical_order_dict_categories.category_code IS '分类编码';
COMMENT ON COLUMN medical_order_dict_categories.category_name IS '分类名称';
COMMENT ON COLUMN medical_order_dict_categories.sort_order IS '排序号';
COMMENT ON COLUMN medical_order_dict_categories.enabled IS '是否启用，0否1是';
COMMENT ON COLUMN medical_order_dict_categories.created_at IS '创建时间';
COMMENT ON COLUMN medical_order_dict_categories.updated_at IS '更新时间';

CREATE TABLE medical_order_dict_items (
    id VARCHAR2(64) NOT NULL,
    category_id VARCHAR2(64) NOT NULL,
    order_item_code VARCHAR2(64) NOT NULL,
    order_item_name VARCHAR2(100) NOT NULL,
    order_type VARCHAR2(50),
    default_content VARCHAR2(1000),
    execution_scope VARCHAR2(50),
    sort_order NUMBER(10) DEFAULT 0,
    enabled NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_medical_order_dict_items PRIMARY KEY (id),
    CONSTRAINT ck_medical_order_dict_items_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_medical_order_dict_items_code UNIQUE (order_item_code),
    CONSTRAINT fk_medical_order_dict_items_category FOREIGN KEY (category_id) REFERENCES medical_order_dict_categories (id)
);

COMMENT ON TABLE medical_order_dict_items IS '医嘱字典项目表';
COMMENT ON COLUMN medical_order_dict_items.id IS '主键ID';
COMMENT ON COLUMN medical_order_dict_items.category_id IS '分类ID';
COMMENT ON COLUMN medical_order_dict_items.order_item_code IS '医嘱项目编码';
COMMENT ON COLUMN medical_order_dict_items.order_item_name IS '医嘱项目名称';
COMMENT ON COLUMN medical_order_dict_items.order_type IS '医嘱类型';
COMMENT ON COLUMN medical_order_dict_items.default_content IS '默认医嘱内容';
COMMENT ON COLUMN medical_order_dict_items.execution_scope IS '执行范围';
COMMENT ON COLUMN medical_order_dict_items.sort_order IS '排序号';
COMMENT ON COLUMN medical_order_dict_items.enabled IS '是否启用，0否1是';
COMMENT ON COLUMN medical_order_dict_items.created_at IS '创建时间';
COMMENT ON COLUMN medical_order_dict_items.updated_at IS '更新时间';

CREATE TABLE medical_order_charge_items (
    id VARCHAR2(64) NOT NULL,
    order_dict_item_id VARCHAR2(64) NOT NULL,
    charge_item_code VARCHAR2(64) NOT NULL,
    charge_item_name VARCHAR2(100) NOT NULL,
    specification VARCHAR2(100),
    unit VARCHAR2(32),
    price NUMBER(12, 2),
    sort_order NUMBER(10) DEFAULT 0,
    enabled NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_medical_order_charge_items PRIMARY KEY (id),
    CONSTRAINT ck_medical_order_charge_items_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_medical_order_charge_items_code UNIQUE (charge_item_code),
    CONSTRAINT fk_medical_order_charge_items_dict_item FOREIGN KEY (order_dict_item_id) REFERENCES medical_order_dict_items (id)
);

COMMENT ON TABLE medical_order_charge_items IS '医嘱收费字典表';
COMMENT ON COLUMN medical_order_charge_items.id IS '主键ID';
COMMENT ON COLUMN medical_order_charge_items.order_dict_item_id IS '医嘱字典项目ID';
COMMENT ON COLUMN medical_order_charge_items.charge_item_code IS '收费项目编码';
COMMENT ON COLUMN medical_order_charge_items.charge_item_name IS '收费项目名称';
COMMENT ON COLUMN medical_order_charge_items.specification IS '规格';
COMMENT ON COLUMN medical_order_charge_items.unit IS '单位';
COMMENT ON COLUMN medical_order_charge_items.price IS '标准单价';
COMMENT ON COLUMN medical_order_charge_items.sort_order IS '排序号';
COMMENT ON COLUMN medical_order_charge_items.enabled IS '是否启用，0否1是';
COMMENT ON COLUMN medical_order_charge_items.created_at IS '创建时间';
COMMENT ON COLUMN medical_order_charge_items.updated_at IS '更新时间';

CREATE TABLE system_config_categories (
    id VARCHAR2(64) NOT NULL,
    parent_id VARCHAR2(64),
    category_code VARCHAR2(64) NOT NULL,
    category_name VARCHAR2(100) NOT NULL,
    category_type VARCHAR2(50),
    sort_order NUMBER(10) DEFAULT 0,
    enabled NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_system_config_categories PRIMARY KEY (id),
    CONSTRAINT ck_system_config_categories_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_system_config_categories_code UNIQUE (category_code),
    CONSTRAINT fk_system_config_categories_parent FOREIGN KEY (parent_id) REFERENCES system_config_categories (id)
);

COMMENT ON TABLE system_config_categories IS '系统配置分类表';
COMMENT ON COLUMN system_config_categories.id IS '主键ID';
COMMENT ON COLUMN system_config_categories.parent_id IS '父级分类ID';
COMMENT ON COLUMN system_config_categories.category_code IS '分类编码';
COMMENT ON COLUMN system_config_categories.category_name IS '分类名称';
COMMENT ON COLUMN system_config_categories.category_type IS '分类类型，示例：CONFIG/DICT/ENUM';
COMMENT ON COLUMN system_config_categories.sort_order IS '排序号';
COMMENT ON COLUMN system_config_categories.enabled IS '是否启用，0否1是';
COMMENT ON COLUMN system_config_categories.created_at IS '创建时间';
COMMENT ON COLUMN system_config_categories.updated_at IS '更新时间';

CREATE TABLE system_config_items (
    id VARCHAR2(64) NOT NULL,
    category_id VARCHAR2(64) NOT NULL,
    config_key VARCHAR2(100) NOT NULL,
    config_name VARCHAR2(100) NOT NULL,
    config_value CLOB,
    value_type VARCHAR2(32),
    sort_order NUMBER(10) DEFAULT 0,
    enabled NUMBER(1) DEFAULT 1,
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_system_config_items PRIMARY KEY (id),
    CONSTRAINT ck_system_config_items_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_system_config_items_key UNIQUE (config_key),
    CONSTRAINT fk_system_config_items_category FOREIGN KEY (category_id) REFERENCES system_config_categories (id)
);

COMMENT ON TABLE system_config_items IS '系统配置项表';
COMMENT ON COLUMN system_config_items.id IS '主键ID';
COMMENT ON COLUMN system_config_items.category_id IS '分类ID';
COMMENT ON COLUMN system_config_items.config_key IS '配置键';
COMMENT ON COLUMN system_config_items.config_name IS '配置名称';
COMMENT ON COLUMN system_config_items.config_value IS '配置值';
COMMENT ON COLUMN system_config_items.value_type IS '值类型，示例：STRING/NUMBER/BOOLEAN/JSON';
COMMENT ON COLUMN system_config_items.sort_order IS '排序号';
COMMENT ON COLUMN system_config_items.enabled IS '是否启用，0否1是';
COMMENT ON COLUMN system_config_items.remarks IS '备注';
COMMENT ON COLUMN system_config_items.created_at IS '创建时间';
COMMENT ON COLUMN system_config_items.updated_at IS '更新时间';

CREATE TABLE user_roles (
    id VARCHAR2(64) NOT NULL,
    user_id VARCHAR2(64) NOT NULL,
    role_id VARCHAR2(64) NOT NULL,
    is_primary NUMBER(1) DEFAULT 0,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by_user_id VARCHAR2(64),
    assigned_by_name VARCHAR2(100),
    CONSTRAINT pk_user_roles PRIMARY KEY (id),
    CONSTRAINT ck_user_roles_is_primary CHECK (is_primary IN (0, 1)),
    CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

COMMENT ON TABLE user_roles IS '用户角色关联表';
COMMENT ON COLUMN user_roles.id IS '主键ID';
COMMENT ON COLUMN user_roles.user_id IS '用户ID';
COMMENT ON COLUMN user_roles.role_id IS '角色ID';
COMMENT ON COLUMN user_roles.is_primary IS '是否主角色，0否1是';
COMMENT ON COLUMN user_roles.assigned_at IS '授权时间';
COMMENT ON COLUMN user_roles.assigned_by_user_id IS '授权人用户ID';
COMMENT ON COLUMN user_roles.assigned_by_name IS '授权人姓名快照';

CREATE TABLE role_menus (
    id VARCHAR2(64) NOT NULL,
    role_id VARCHAR2(64) NOT NULL,
    menu_id VARCHAR2(64) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_role_menus PRIMARY KEY (id),
    CONSTRAINT uk_role_menus_role_menu UNIQUE (role_id, menu_id),
    CONSTRAINT fk_role_menus_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_menus_menu FOREIGN KEY (menu_id) REFERENCES menus (id)
);

COMMENT ON TABLE role_menus IS '角色菜单授权表';
COMMENT ON COLUMN role_menus.id IS '主键ID';
COMMENT ON COLUMN role_menus.role_id IS '角色ID';
COMMENT ON COLUMN role_menus.menu_id IS '菜单ID';
COMMENT ON COLUMN role_menus.assigned_at IS '授权时间';

CREATE TABLE role_permissions (
    id VARCHAR2(64) NOT NULL,
    role_id VARCHAR2(64) NOT NULL,
    permission_id VARCHAR2(64) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_role_permissions PRIMARY KEY (id),
    CONSTRAINT uk_role_permissions_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

COMMENT ON TABLE role_permissions IS '角色权限授权表';
COMMENT ON COLUMN role_permissions.id IS '主键ID';
COMMENT ON COLUMN role_permissions.role_id IS '角色ID';
COMMENT ON COLUMN role_permissions.permission_id IS '权限ID';
COMMENT ON COLUMN role_permissions.assigned_at IS '授权时间';

CREATE TABLE role_message_subscriptions (
    id VARCHAR2(64) NOT NULL,
    role_id VARCHAR2(64) NOT NULL,
    topic_id VARCHAR2(64) NOT NULL,
    subscription_mode VARCHAR2(32) DEFAULT 'INBOX',
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_role_message_subscriptions PRIMARY KEY (id),
    CONSTRAINT uk_role_message_subscriptions UNIQUE (role_id, topic_id),
    CONSTRAINT fk_role_message_subscriptions_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_message_subscriptions_topic FOREIGN KEY (topic_id) REFERENCES message_topics (id)
);

COMMENT ON TABLE role_message_subscriptions IS '角色消息订阅授权表';
COMMENT ON COLUMN role_message_subscriptions.id IS '主键ID';
COMMENT ON COLUMN role_message_subscriptions.role_id IS '角色ID';
COMMENT ON COLUMN role_message_subscriptions.topic_id IS '主题ID';
COMMENT ON COLUMN role_message_subscriptions.subscription_mode IS '订阅方式，示例：INBOX/SMS/POPUP';
COMMENT ON COLUMN role_message_subscriptions.assigned_at IS '授权时间';

CREATE TABLE role_stat_authorizations (
    id VARCHAR2(64) NOT NULL,
    role_id VARCHAR2(64) NOT NULL,
    stat_category_id VARCHAR2(64) NOT NULL,
    auth_scope VARCHAR2(32) DEFAULT 'VIEW',
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_role_stat_authorizations PRIMARY KEY (id),
    CONSTRAINT uk_role_stat_authorizations UNIQUE (role_id, stat_category_id),
    CONSTRAINT fk_role_stat_authorizations_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_stat_authorizations_stat FOREIGN KEY (stat_category_id) REFERENCES stat_categories (id)
);

COMMENT ON TABLE role_stat_authorizations IS '角色统计授权表';
COMMENT ON COLUMN role_stat_authorizations.id IS '主键ID';
COMMENT ON COLUMN role_stat_authorizations.role_id IS '角色ID';
COMMENT ON COLUMN role_stat_authorizations.stat_category_id IS '统计类型ID';
COMMENT ON COLUMN role_stat_authorizations.auth_scope IS '授权范围，示例：VIEW/EXPORT/MANAGE';
COMMENT ON COLUMN role_stat_authorizations.assigned_at IS '授权时间';

CREATE TABLE user_login_logs (
    id VARCHAR2(64) NOT NULL,
    user_id VARCHAR2(64),
    login_name VARCHAR2(64),
    login_result VARCHAR2(32) NOT NULL,
    client_ip VARCHAR2(64),
    client_device VARCHAR2(200),
    login_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logout_at TIMESTAMP,
    failure_reason VARCHAR2(500),
    remarks VARCHAR2(500),
    CONSTRAINT pk_user_login_logs PRIMARY KEY (id),
    CONSTRAINT fk_user_login_logs_user FOREIGN KEY (user_id) REFERENCES users (id)
);

COMMENT ON TABLE user_login_logs IS '用户登录日志表';
COMMENT ON COLUMN user_login_logs.id IS '主键ID';
COMMENT ON COLUMN user_login_logs.user_id IS '用户ID';
COMMENT ON COLUMN user_login_logs.login_name IS '登录账号';
COMMENT ON COLUMN user_login_logs.login_result IS '登录结果，示例：SUCCESS/FAILED/DENIED';
COMMENT ON COLUMN user_login_logs.client_ip IS '客户端IP';
COMMENT ON COLUMN user_login_logs.client_device IS '客户端设备';
COMMENT ON COLUMN user_login_logs.login_at IS '登录时间';
COMMENT ON COLUMN user_login_logs.logout_at IS '退出时间';
COMMENT ON COLUMN user_login_logs.failure_reason IS '失败原因';
COMMENT ON COLUMN user_login_logs.remarks IS '备注';

CREATE TABLE operation_logs (
    id VARCHAR2(64) NOT NULL,
    module_code VARCHAR2(64) NOT NULL,
    business_type VARCHAR2(64),
    business_id VARCHAR2(64),
    operation_name VARCHAR2(100) NOT NULL,
    operation_result VARCHAR2(32) NOT NULL,
    operator_user_id VARCHAR2(64),
    operator_name VARCHAR2(100),
    operator_ip VARCHAR2(64),
    operation_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    operation_content CLOB,
    failure_reason VARCHAR2(500),
    CONSTRAINT pk_operation_logs PRIMARY KEY (id),
    CONSTRAINT fk_operation_logs_user FOREIGN KEY (operator_user_id) REFERENCES users (id)
);

COMMENT ON TABLE operation_logs IS '系统操作日志表';
COMMENT ON COLUMN operation_logs.id IS '主键ID';
COMMENT ON COLUMN operation_logs.module_code IS '模块编码';
COMMENT ON COLUMN operation_logs.business_type IS '业务类型';
COMMENT ON COLUMN operation_logs.business_id IS '业务主键ID';
COMMENT ON COLUMN operation_logs.operation_name IS '操作名称';
COMMENT ON COLUMN operation_logs.operation_result IS '操作结果，示例：SUCCESS/FAILED/DENIED';
COMMENT ON COLUMN operation_logs.operator_user_id IS '操作人用户ID';
COMMENT ON COLUMN operation_logs.operator_name IS '操作人姓名快照';
COMMENT ON COLUMN operation_logs.operator_ip IS '操作人IP';
COMMENT ON COLUMN operation_logs.operation_at IS '操作时间';
COMMENT ON COLUMN operation_logs.operation_content IS '操作内容';
COMMENT ON COLUMN operation_logs.failure_reason IS '失败原因';

-- =========================================================
-- 申请与病例中心
-- =========================================================
CREATE TABLE applications (
    id VARCHAR2(64) NOT NULL,
    application_no VARCHAR2(64) NOT NULL,
    patient_id VARCHAR2(64),
    patient_name VARCHAR2(100),
    patient_gender VARCHAR2(16),
    patient_age VARCHAR2(32),
    application_type VARCHAR2(50),
    status VARCHAR2(32),
    external_order_no VARCHAR2(64),
    third_party_source VARCHAR2(64),
    application_form_status VARCHAR2(32),
    clinical_diagnosis VARCHAR2(500),
    clinical_symptom VARCHAR2(500),
    source_hospital_id VARCHAR2(64),
    source_hospital_name VARCHAR2(200),
    applicant_department_id VARCHAR2(64),
    applicant_department_name VARCHAR2(100),
    submitting_department_id VARCHAR2(64),
    submitting_department_name VARCHAR2(100),
    applicant_doctor_user_id VARCHAR2(64),
    applicant_doctor_name VARCHAR2(100),
    submitting_doctor_user_id VARCHAR2(64),
    submitting_doctor_name VARCHAR2(100),
    specimen_site VARCHAR2(200),
    application_date DATE,
    submission_date DATE,
    specimen_removal_time TIMESTAMP,
    received_at TIMESTAMP,
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_applications PRIMARY KEY (id),
    CONSTRAINT uk_applications_application_no UNIQUE (application_no),
    CONSTRAINT uk_applications_source_order UNIQUE (third_party_source, external_order_no),
    CONSTRAINT fk_applications_patient FOREIGN KEY (patient_id) REFERENCES patients (id)
);

COMMENT ON TABLE applications IS '病理申请单表';
COMMENT ON COLUMN applications.id IS '主键ID';
COMMENT ON COLUMN applications.application_no IS '申请单号';
COMMENT ON COLUMN applications.patient_id IS '患者ID';
COMMENT ON COLUMN applications.patient_name IS '患者姓名快照';
COMMENT ON COLUMN applications.patient_gender IS '患者性别快照';
COMMENT ON COLUMN applications.patient_age IS '患者年龄快照';
COMMENT ON COLUMN applications.application_type IS '申请类型，示例：ROUTINE/FROZEN/MOLECULAR/CONSULTATION';
COMMENT ON COLUMN applications.status IS '申请状态，示例：DRAFT/SUBMITTED/RECEIVED/CLOSED/CANCELLED';
COMMENT ON COLUMN applications.external_order_no IS '外部系统医嘱号或申请号';
COMMENT ON COLUMN applications.third_party_source IS '第三方来源标识，示例：HIS/EMR/LIS';
COMMENT ON COLUMN applications.application_form_status IS '申请单附件状态，示例：NOT_UPLOADED/UPLOADED/ARCHIVED';
COMMENT ON COLUMN applications.clinical_diagnosis IS '临床诊断';
COMMENT ON COLUMN applications.clinical_symptom IS '临床症状';
COMMENT ON COLUMN applications.source_hospital_id IS '送检医院ID';
COMMENT ON COLUMN applications.source_hospital_name IS '送检医院名称快照';
COMMENT ON COLUMN applications.applicant_department_id IS '申请科室ID';
COMMENT ON COLUMN applications.applicant_department_name IS '申请科室名称快照';
COMMENT ON COLUMN applications.submitting_department_id IS '送检科室ID';
COMMENT ON COLUMN applications.submitting_department_name IS '送检科室名称快照';
COMMENT ON COLUMN applications.applicant_doctor_user_id IS '申请医生用户ID';
COMMENT ON COLUMN applications.applicant_doctor_name IS '申请医生姓名快照';
COMMENT ON COLUMN applications.submitting_doctor_user_id IS '送检医生用户ID';
COMMENT ON COLUMN applications.submitting_doctor_name IS '送检医生姓名快照';
COMMENT ON COLUMN applications.specimen_site IS '送检部位';
COMMENT ON COLUMN applications.application_date IS '申请日期';
COMMENT ON COLUMN applications.submission_date IS '送检日期';
COMMENT ON COLUMN applications.specimen_removal_time IS '标本离体时间';
COMMENT ON COLUMN applications.received_at IS '申请单接收时间';
COMMENT ON COLUMN applications.remarks IS '备注';
COMMENT ON COLUMN applications.created_at IS '创建时间';
COMMENT ON COLUMN applications.updated_at IS '更新时间';

CREATE TABLE pathology_cases (
    id VARCHAR2(64) NOT NULL,
    case_no VARCHAR2(64),
    pathology_no VARCHAR2(64) NOT NULL,
    patient_id VARCHAR2(64),
    application_id VARCHAR2(64),
    case_type VARCHAR2(50),
    current_status VARCHAR2(32),
    case_status VARCHAR2(32),
    priority VARCHAR2(32) DEFAULT 'NORMAL',
    source_hospital_id VARCHAR2(64),
    source_hospital_name VARCHAR2(200),
    source_department_id VARCHAR2(64),
    source_department_name VARCHAR2(100),
    received_by_user_id VARCHAR2(64),
    received_by_name VARCHAR2(100),
    registered_by_user_id VARCHAR2(64),
    registered_by_name VARCHAR2(100),
    received_at TIMESTAMP,
    registered_at TIMESTAMP,
    due_at TIMESTAMP,
    signed_out_at TIMESTAMP,
    closed_at TIMESTAMP,
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_pathology_cases PRIMARY KEY (id),
    CONSTRAINT uk_pathology_cases_pathology_no UNIQUE (pathology_no),
    CONSTRAINT uk_pathology_cases_case_no UNIQUE (case_no),
    CONSTRAINT fk_pathology_cases_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_pathology_cases_application FOREIGN KEY (application_id) REFERENCES applications (id)
);

COMMENT ON TABLE pathology_cases IS '病理病例主表';
COMMENT ON COLUMN pathology_cases.id IS '主键ID';
COMMENT ON COLUMN pathology_cases.case_no IS '病例编号';
COMMENT ON COLUMN pathology_cases.pathology_no IS '病理号，唯一';
COMMENT ON COLUMN pathology_cases.patient_id IS '患者ID';
COMMENT ON COLUMN pathology_cases.application_id IS '申请单ID';
COMMENT ON COLUMN pathology_cases.case_type IS '病例类型，示例：ROUTINE/FROZEN/CONSULTATION/MOLECULAR';
COMMENT ON COLUMN pathology_cases.current_status IS '当前主流程状态，示例：COLLECTION/FIXATION/TRANSPORT/RECEIVED/REGISTERED/SAMPLING/DEHYDRATION/EMBEDDING/SLICING/STAINING/DIAGNOSING/REVIEWING/PUBLISHED/ARCHIVED';
COMMENT ON COLUMN pathology_cases.case_status IS '当前病例状态';
COMMENT ON COLUMN pathology_cases.priority IS '优先级，示例：NORMAL/URGENT/STAT';
COMMENT ON COLUMN pathology_cases.source_hospital_id IS '送检医院ID';
COMMENT ON COLUMN pathology_cases.source_hospital_name IS '送检医院名称快照';
COMMENT ON COLUMN pathology_cases.source_department_id IS '送检科室ID';
COMMENT ON COLUMN pathology_cases.source_department_name IS '送检科室名称快照';
COMMENT ON COLUMN pathology_cases.received_by_user_id IS '接收人用户ID';
COMMENT ON COLUMN pathology_cases.received_by_name IS '接收人姓名快照';
COMMENT ON COLUMN pathology_cases.registered_by_user_id IS '登记人用户ID';
COMMENT ON COLUMN pathology_cases.registered_by_name IS '登记人姓名快照';
COMMENT ON COLUMN pathology_cases.received_at IS '病例接收时间';
COMMENT ON COLUMN pathology_cases.registered_at IS '病例登记时间';
COMMENT ON COLUMN pathology_cases.due_at IS '报告时限截止时间';
COMMENT ON COLUMN pathology_cases.signed_out_at IS '签发完成时间';
COMMENT ON COLUMN pathology_cases.closed_at IS '病例关闭时间';
COMMENT ON COLUMN pathology_cases.remarks IS '备注';
COMMENT ON COLUMN pathology_cases.created_at IS '创建时间';
COMMENT ON COLUMN pathology_cases.updated_at IS '更新时间';

-- =========================================================
-- 标本接收与主链路
-- =========================================================
CREATE TABLE specimens (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64),
    application_id VARCHAR2(64) NOT NULL,
    specimen_no VARCHAR2(64) NOT NULL,
    barcode VARCHAR2(128),
    specimen_type VARCHAR2(100),
    specimen_name_standardized VARCHAR2(200),
    specimen_site VARCHAR2(200),
    collection_mode VARCHAR2(50),
    specimen_count NUMBER(10),
    specimen_status VARCHAR2(32) DEFAULT 'REGISTERED',
    fixation_status VARCHAR2(200),
    qualified_flag NUMBER(1) DEFAULT 1,
    unqualified_reason VARCHAR2(500),
    clinical_symptom VARCHAR2(500),
    applicant_department_id VARCHAR2(64),
    applicant_department_name VARCHAR2(100),
    applicant_doctor_user_id VARCHAR2(64),
    applicant_doctor_name VARCHAR2(100),
    submission_date DATE,
    label_print_batch_no VARCHAR2(64),
    label_print_status VARCHAR2(32),
    registered_by_user_id VARCHAR2(64),
    registered_by_name VARCHAR2(100),
    registered_at TIMESTAMP,
    terminal_code VARCHAR2(64),
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_specimens PRIMARY KEY (id),
    CONSTRAINT ck_specimens_qualified_flag CHECK (qualified_flag IN (0, 1)),
    CONSTRAINT uk_specimens_specimen_no UNIQUE (specimen_no),
    CONSTRAINT uk_specimens_barcode UNIQUE (barcode),
    CONSTRAINT fk_specimens_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_specimens_application FOREIGN KEY (application_id) REFERENCES applications (id)
);

COMMENT ON TABLE specimens IS '病例下标本表';
COMMENT ON COLUMN specimens.id IS '主键ID';
COMMENT ON COLUMN specimens.case_id IS '病例ID';
COMMENT ON COLUMN specimens.application_id IS '申请单ID';
COMMENT ON COLUMN specimens.specimen_no IS '标本序号或条码，全局唯一';
COMMENT ON COLUMN specimens.barcode IS '标本条码，全局唯一';
COMMENT ON COLUMN specimens.specimen_type IS '标本类型';
COMMENT ON COLUMN specimens.specimen_name_standardized IS '标准化标本名称';
COMMENT ON COLUMN specimens.specimen_site IS '标本部位';
COMMENT ON COLUMN specimens.collection_mode IS '采集方式，示例：SURGERY/BIOPSY/PUNCTURE/CYTOLOGY';
COMMENT ON COLUMN specimens.specimen_count IS '标本数量';
COMMENT ON COLUMN specimens.specimen_status IS '标本状态';
COMMENT ON COLUMN specimens.fixation_status IS '固定情况';
COMMENT ON COLUMN specimens.qualified_flag IS '是否合格，0否1是';
COMMENT ON COLUMN specimens.unqualified_reason IS '不合格原因';
COMMENT ON COLUMN specimens.clinical_symptom IS '临床症状';
COMMENT ON COLUMN specimens.applicant_department_id IS '申请科室ID';
COMMENT ON COLUMN specimens.applicant_department_name IS '申请科室名称快照';
COMMENT ON COLUMN specimens.applicant_doctor_user_id IS '申请医生用户ID';
COMMENT ON COLUMN specimens.applicant_doctor_name IS '申请医生姓名快照';
COMMENT ON COLUMN specimens.submission_date IS '送检日期';
COMMENT ON COLUMN specimens.label_print_batch_no IS '标签打印批次号';
COMMENT ON COLUMN specimens.label_print_status IS '标签打印状态';
COMMENT ON COLUMN specimens.registered_by_user_id IS '登记人用户ID';
COMMENT ON COLUMN specimens.registered_by_name IS '登记人姓名快照';
COMMENT ON COLUMN specimens.registered_at IS '登记时间';
COMMENT ON COLUMN specimens.terminal_code IS '终端编码';
COMMENT ON COLUMN specimens.remarks IS '备注';
COMMENT ON COLUMN specimens.created_at IS '创建时间';
COMMENT ON COLUMN specimens.updated_at IS '更新时间';

CREATE TABLE specimen_collection_records (
    id VARCHAR2(64) NOT NULL,
    application_id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64),
    specimen_id VARCHAR2(64) NOT NULL,
    collection_status VARCHAR2(32) DEFAULT 'COLLECTED',
    collection_scene VARCHAR2(100),
    collection_mode VARCHAR2(50),
    label_print_batch_no VARCHAR2(64),
    collector_user_id VARCHAR2(64),
    collector_name VARCHAR2(100),
    collected_at TIMESTAMP,
    terminal_code VARCHAR2(64),
    remarks VARCHAR2(500),
    CONSTRAINT pk_specimen_collection_records PRIMARY KEY (id),
    CONSTRAINT fk_specimen_collection_records_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_specimen_collection_records_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_specimen_collection_records_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id)
);

COMMENT ON TABLE specimen_collection_records IS '标本采集登记记录表';
COMMENT ON COLUMN specimen_collection_records.id IS '主键ID';
COMMENT ON COLUMN specimen_collection_records.application_id IS '申请单ID';
COMMENT ON COLUMN specimen_collection_records.case_id IS '病例ID';
COMMENT ON COLUMN specimen_collection_records.specimen_id IS '标本ID';
COMMENT ON COLUMN specimen_collection_records.collection_status IS '采集状态，示例：PENDING/COLLECTED/CANCELLED';
COMMENT ON COLUMN specimen_collection_records.collection_scene IS '采集场景，示例：OUTPATIENT/INPATIENT/OPERATING_ROOM';
COMMENT ON COLUMN specimen_collection_records.collection_mode IS '采集方式，示例：MANUAL/AUTOMATIC/BED_SIDE';
COMMENT ON COLUMN specimen_collection_records.label_print_batch_no IS '标签打印批次号';
COMMENT ON COLUMN specimen_collection_records.collector_user_id IS '采集人用户ID';
COMMENT ON COLUMN specimen_collection_records.collector_name IS '采集人姓名快照';
COMMENT ON COLUMN specimen_collection_records.collected_at IS '采集时间';
COMMENT ON COLUMN specimen_collection_records.terminal_code IS '终端编码';
COMMENT ON COLUMN specimen_collection_records.remarks IS '备注';

CREATE TABLE specimen_fixation_records (
    id VARCHAR2(64) NOT NULL,
    application_id VARCHAR2(64),
    case_id VARCHAR2(64),
    specimen_id VARCHAR2(64) NOT NULL,
    fixation_status VARCHAR2(32) DEFAULT 'PENDING',
    fixation_liquid_type VARCHAR2(100),
    fixation_start_at TIMESTAMP,
    fixation_completed_at TIMESTAMP,
    verified_by_user_id VARCHAR2(64),
    verified_by_name VARCHAR2(100),
    verified_at TIMESTAMP,
    terminal_code VARCHAR2(64),
    remarks VARCHAR2(500),
    CONSTRAINT pk_specimen_fixation_records PRIMARY KEY (id),
    CONSTRAINT uk_specimen_fixation_records_specimen UNIQUE (specimen_id),
    CONSTRAINT fk_specimen_fixation_records_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_specimen_fixation_records_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_specimen_fixation_records_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id)
);

COMMENT ON TABLE specimen_fixation_records IS '标本固定核对记录表';
COMMENT ON COLUMN specimen_fixation_records.id IS '主键ID';
COMMENT ON COLUMN specimen_fixation_records.application_id IS '申请单ID';
COMMENT ON COLUMN specimen_fixation_records.case_id IS '病例ID';
COMMENT ON COLUMN specimen_fixation_records.specimen_id IS '标本ID';
COMMENT ON COLUMN specimen_fixation_records.fixation_status IS '固定状态，示例：PENDING/FIXING/COMPLETED/ABNORMAL';
COMMENT ON COLUMN specimen_fixation_records.fixation_liquid_type IS '固定液类型';
COMMENT ON COLUMN specimen_fixation_records.fixation_start_at IS '固定开始时间';
COMMENT ON COLUMN specimen_fixation_records.fixation_completed_at IS '固定完成时间';
COMMENT ON COLUMN specimen_fixation_records.verified_by_user_id IS '固定核对人用户ID';
COMMENT ON COLUMN specimen_fixation_records.verified_by_name IS '固定核对人姓名快照';
COMMENT ON COLUMN specimen_fixation_records.verified_at IS '固定核对时间';
COMMENT ON COLUMN specimen_fixation_records.terminal_code IS '终端编码';
COMMENT ON COLUMN specimen_fixation_records.remarks IS '备注';

CREATE TABLE transport_orders (
    id VARCHAR2(64) NOT NULL,
    transport_order_no VARCHAR2(64) NOT NULL,
    application_id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64),
    order_status VARCHAR2(32) DEFAULT 'PENDING',
    handover_user_id VARCHAR2(64),
    handover_user_name VARCHAR2(100),
    handover_department_id VARCHAR2(64),
    handover_department_name VARCHAR2(100),
    receiver_user_id VARCHAR2(64),
    receiver_user_name VARCHAR2(100),
    receiver_department_id VARCHAR2(64),
    receiver_department_name VARCHAR2(100),
    printed_at TIMESTAMP,
    to_be_transported_at TIMESTAMP,
    handed_over_at TIMESTAMP,
    terminal_code VARCHAR2(64),
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_transport_orders PRIMARY KEY (id),
    CONSTRAINT uk_transport_orders_order_no UNIQUE (transport_order_no),
    CONSTRAINT fk_transport_orders_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_transport_orders_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
);

COMMENT ON TABLE transport_orders IS '标本转运单主表';
COMMENT ON COLUMN transport_orders.id IS '主键ID';
COMMENT ON COLUMN transport_orders.transport_order_no IS '转运单号';
COMMENT ON COLUMN transport_orders.application_id IS '申请单ID';
COMMENT ON COLUMN transport_orders.case_id IS '病例ID';
COMMENT ON COLUMN transport_orders.order_status IS '转运状态，示例：PENDING/PRINTED/HANDED_OVER/PARTIALLY_RECEIVED/COMPLETED/CANCELLED';
COMMENT ON COLUMN transport_orders.handover_user_id IS '交出人用户ID';
COMMENT ON COLUMN transport_orders.handover_user_name IS '交出人姓名快照';
COMMENT ON COLUMN transport_orders.handover_department_id IS '交出科室ID';
COMMENT ON COLUMN transport_orders.handover_department_name IS '交出科室名称快照';
COMMENT ON COLUMN transport_orders.receiver_user_id IS '接收人用户ID';
COMMENT ON COLUMN transport_orders.receiver_user_name IS '接收人姓名快照';
COMMENT ON COLUMN transport_orders.receiver_department_id IS '接收科室ID';
COMMENT ON COLUMN transport_orders.receiver_department_name IS '接收科室名称快照';
COMMENT ON COLUMN transport_orders.printed_at IS '转运单打印时间';
COMMENT ON COLUMN transport_orders.to_be_transported_at IS '待运时间';
COMMENT ON COLUMN transport_orders.handed_over_at IS '交接完成时间';
COMMENT ON COLUMN transport_orders.terminal_code IS '终端编码';
COMMENT ON COLUMN transport_orders.remarks IS '备注';
COMMENT ON COLUMN transport_orders.created_at IS '创建时间';
COMMENT ON COLUMN transport_orders.updated_at IS '更新时间';

CREATE TABLE transport_order_items (
    id VARCHAR2(64) NOT NULL,
    transport_order_id VARCHAR2(64) NOT NULL,
    application_id VARCHAR2(64),
    case_id VARCHAR2(64),
    specimen_id VARCHAR2(64) NOT NULL,
    item_status VARCHAR2(32) DEFAULT 'PENDING',
    verification_result VARCHAR2(32),
    verified_by_user_id VARCHAR2(64),
    verified_by_name VARCHAR2(100),
    verified_at TIMESTAMP,
    remarks VARCHAR2(500),
    CONSTRAINT pk_transport_order_items PRIMARY KEY (id),
    CONSTRAINT uk_transport_order_items_order_specimen UNIQUE (transport_order_id, specimen_id),
    CONSTRAINT fk_transport_order_items_order FOREIGN KEY (transport_order_id) REFERENCES transport_orders (id),
    CONSTRAINT fk_transport_order_items_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_transport_order_items_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_transport_order_items_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id)
);

COMMENT ON TABLE transport_order_items IS '标本转运单明细表';
COMMENT ON COLUMN transport_order_items.id IS '主键ID';
COMMENT ON COLUMN transport_order_items.transport_order_id IS '转运单ID';
COMMENT ON COLUMN transport_order_items.application_id IS '申请单ID';
COMMENT ON COLUMN transport_order_items.case_id IS '病例ID';
COMMENT ON COLUMN transport_order_items.specimen_id IS '标本ID';
COMMENT ON COLUMN transport_order_items.item_status IS '明细状态，示例：PENDING/HANDED_OVER/PARTIALLY_RECEIVED/COMPLETED/RETURNED';
COMMENT ON COLUMN transport_order_items.verification_result IS '核对结果，示例：MATCHED/MISMATCHED/PARTIAL';
COMMENT ON COLUMN transport_order_items.verified_by_user_id IS '核对人用户ID';
COMMENT ON COLUMN transport_order_items.verified_by_name IS '核对人姓名快照';
COMMENT ON COLUMN transport_order_items.verified_at IS '核对时间';
COMMENT ON COLUMN transport_order_items.remarks IS '备注';

CREATE TABLE specimen_receipts (
    id VARCHAR2(64) NOT NULL,
    application_id VARCHAR2(64),
    case_id VARCHAR2(64),
    specimen_id VARCHAR2(64),
    transport_order_id VARCHAR2(64),
    receipt_status VARCHAR2(32) NOT NULL,
    container_count NUMBER(10),
    quality_check_result VARCHAR2(32),
    quality_issue_codes VARCHAR2(500),
    barcode VARCHAR2(128),
    received_by_user_id VARCHAR2(64),
    received_by_name VARCHAR2(100),
    received_at TIMESTAMP,
    terminal_code VARCHAR2(64),
    reject_reason VARCHAR2(500),
    return_reason VARCHAR2(500),
    remarks VARCHAR2(500),
    CONSTRAINT pk_specimen_receipts PRIMARY KEY (id),
    CONSTRAINT uk_specimen_receipts_barcode UNIQUE (barcode),
    CONSTRAINT fk_specimen_receipts_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_specimen_receipts_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_specimen_receipts_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_specimen_receipts_transport_order FOREIGN KEY (transport_order_id) REFERENCES transport_orders (id)
);

COMMENT ON TABLE specimen_receipts IS '标本接收与拒收记录表';
COMMENT ON COLUMN specimen_receipts.id IS '主键ID';
COMMENT ON COLUMN specimen_receipts.application_id IS '申请单ID';
COMMENT ON COLUMN specimen_receipts.case_id IS '病例ID';
COMMENT ON COLUMN specimen_receipts.specimen_id IS '标本ID';
COMMENT ON COLUMN specimen_receipts.transport_order_id IS '转运单ID';
COMMENT ON COLUMN specimen_receipts.receipt_status IS '接收状态，示例：RECEIVED/REJECTED/RETURNED';
COMMENT ON COLUMN specimen_receipts.container_count IS '容器数量';
COMMENT ON COLUMN specimen_receipts.barcode IS '接收条码';
COMMENT ON COLUMN specimen_receipts.received_by_user_id IS '接收人用户ID';
COMMENT ON COLUMN specimen_receipts.received_by_name IS '接收人姓名快照';
COMMENT ON COLUMN specimen_receipts.received_at IS '接收时间';
COMMENT ON COLUMN specimen_receipts.terminal_code IS '终端编码';
COMMENT ON COLUMN specimen_receipts.reject_reason IS '拒收原因';
COMMENT ON COLUMN specimen_receipts.return_reason IS '退回原因';
COMMENT ON COLUMN specimen_receipts.remarks IS '备注';

CREATE TABLE samplings (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64) NOT NULL,
    sampling_status VARCHAR2(32),
    block_count NUMBER(10),
    gross_image_count NUMBER(10) DEFAULT 0,
    sampling_template_id VARCHAR2(64),
    gross_description CLOB,
    sampled_by_user_id VARCHAR2(64),
    sampled_by_name VARCHAR2(100),
    sampled_at TIMESTAMP,
    sampling_cancel_reason VARCHAR2(500),
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_samplings PRIMARY KEY (id),
    CONSTRAINT fk_samplings_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_samplings_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_samplings_template FOREIGN KEY (sampling_template_id) REFERENCES sampling_templates (id)
);

COMMENT ON TABLE samplings IS '取材记录表';
COMMENT ON COLUMN samplings.id IS '主键ID';
COMMENT ON COLUMN samplings.case_id IS '病例ID';
COMMENT ON COLUMN samplings.specimen_id IS '标本ID';
COMMENT ON COLUMN samplings.sampling_status IS '取材状态，示例：PENDING/IN_PROGRESS/COMPLETED';
COMMENT ON COLUMN samplings.block_count IS '材块数量';
COMMENT ON COLUMN samplings.gross_image_count IS '大体图片数量';
COMMENT ON COLUMN samplings.sampling_template_id IS '取材模板ID';
COMMENT ON COLUMN samplings.gross_description IS '大体描述';
COMMENT ON COLUMN samplings.sampled_by_user_id IS '取材人用户ID';
COMMENT ON COLUMN samplings.sampled_by_name IS '取材人姓名快照';
COMMENT ON COLUMN samplings.sampled_at IS '取材时间';
COMMENT ON COLUMN samplings.sampling_cancel_reason IS '取消取材原因';
COMMENT ON COLUMN samplings.remarks IS '备注';

CREATE TABLE sampling_blocks (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64) NOT NULL,
    sampling_id VARCHAR2(64) NOT NULL,
    sequence_no NUMBER(10),
    block_code VARCHAR2(64),
    block_site VARCHAR2(200),
    block_description VARCHAR2(1000),
    embedding_box_no VARCHAR2(64),
    special_requirement VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sampling_blocks PRIMARY KEY (id),
    CONSTRAINT uk_sampling_blocks_block_code UNIQUE (block_code),
    CONSTRAINT fk_sampling_blocks_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_sampling_blocks_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_sampling_blocks_sampling FOREIGN KEY (sampling_id) REFERENCES samplings (id)
);

COMMENT ON TABLE sampling_blocks IS '取材块明细表';
COMMENT ON COLUMN sampling_blocks.id IS '主键ID';
COMMENT ON COLUMN sampling_blocks.case_id IS '病例ID';
COMMENT ON COLUMN sampling_blocks.specimen_id IS '标本ID';
COMMENT ON COLUMN sampling_blocks.sampling_id IS '取材记录ID';
COMMENT ON COLUMN sampling_blocks.sequence_no IS '序号';
COMMENT ON COLUMN sampling_blocks.block_code IS '材块编号';
COMMENT ON COLUMN sampling_blocks.block_site IS '材块部位';
COMMENT ON COLUMN sampling_blocks.block_description IS '材块描述';
COMMENT ON COLUMN sampling_blocks.embedding_box_no IS '预关联包埋盒号';
COMMENT ON COLUMN sampling_blocks.special_requirement IS '特殊要求';

CREATE TABLE embeddings (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64) NOT NULL,
    sampling_id VARCHAR2(64) NOT NULL,
    embedding_status VARCHAR2(32),
    evaluation_level VARCHAR2(32),
    sampling_evaluation VARCHAR2(500),
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    embedded_by_user_id VARCHAR2(64),
    embedded_by_name VARCHAR2(100),
    remarks VARCHAR2(500),
    sampling_block_id VARCHAR2(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_embeddings PRIMARY KEY (id),
    CONSTRAINT fk_embeddings_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_embeddings_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_embeddings_sampling FOREIGN KEY (sampling_id) REFERENCES samplings (id)
);

COMMENT ON TABLE embeddings IS '包埋记录表';
COMMENT ON COLUMN embeddings.id IS '主键ID';
COMMENT ON COLUMN embeddings.case_id IS '病例ID';
COMMENT ON COLUMN embeddings.specimen_id IS '标本ID';
COMMENT ON COLUMN embeddings.sampling_id IS '取材记录ID';
COMMENT ON COLUMN embeddings.embedding_status IS '包埋状态，示例：PENDING/IN_PROGRESS/COMPLETED';
COMMENT ON COLUMN embeddings.evaluation_level IS '评价等级';
COMMENT ON COLUMN embeddings.sampling_evaluation IS '取材评价';
COMMENT ON COLUMN embeddings.started_at IS '包埋开始时间';
COMMENT ON COLUMN embeddings.ended_at IS '包埋结束时间';
COMMENT ON COLUMN embeddings.embedded_by_user_id IS '包埋人用户ID';
COMMENT ON COLUMN embeddings.embedded_by_name IS '包埋人姓名快照';
COMMENT ON COLUMN embeddings.remarks IS '备注';

CREATE TABLE embedding_boxes (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64) NOT NULL,
    embedding_id VARCHAR2(64) NOT NULL,
    embedding_box_no VARCHAR2(64) NOT NULL,
    block_count NUMBER(10),
    re_embedding_flag NUMBER(1) DEFAULT 0,
    slice_notice VARCHAR2(500),
    storage_status VARCHAR2(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sampling_block_id VARCHAR2(64),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_embedding_boxes PRIMARY KEY (id),
    CONSTRAINT ck_embedding_boxes_re_embedding_flag CHECK (re_embedding_flag IN (0, 1)),
    CONSTRAINT uk_embedding_boxes_case_box_no UNIQUE (case_id, embedding_box_no),
    CONSTRAINT fk_embedding_boxes_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_embedding_boxes_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_embedding_boxes_embedding FOREIGN KEY (embedding_id) REFERENCES embeddings (id)
);

COMMENT ON TABLE embedding_boxes IS '包埋盒表';
COMMENT ON COLUMN embedding_boxes.id IS '主键ID';
COMMENT ON COLUMN embedding_boxes.case_id IS '病例ID';
COMMENT ON COLUMN embedding_boxes.specimen_id IS '标本ID';
COMMENT ON COLUMN embedding_boxes.embedding_id IS '包埋记录ID';
COMMENT ON COLUMN embedding_boxes.embedding_box_no IS '包埋盒号，病例内唯一';
COMMENT ON COLUMN embedding_boxes.block_count IS '盒内材块数量';
COMMENT ON COLUMN embedding_boxes.re_embedding_flag IS '是否重包埋，0否1是';
COMMENT ON COLUMN embedding_boxes.slice_notice IS '切片注意事项';
COMMENT ON COLUMN embedding_boxes.storage_status IS '存储状态，示例：ACTIVE/STORED/DISCARDED';
COMMENT ON COLUMN embedding_boxes.created_at IS '创建时间';

CREATE TABLE dehydration_batches (
    id VARCHAR2(64) NOT NULL,
    batch_no VARCHAR2(64) NOT NULL,
    batch_status VARCHAR2(32) DEFAULT 'PENDING',
    basket_no VARCHAR2(64),
    device_no VARCHAR2(64),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    operator_user_id VARCHAR2(64),
    operator_name VARCHAR2(100),
    remarks VARCHAR2(500),
    case_id VARCHAR2(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_dehydration_batches PRIMARY KEY (id),
    CONSTRAINT uk_dehydration_batches_batch_no UNIQUE (batch_no)
);

COMMENT ON TABLE dehydration_batches IS '脱水批次表';
COMMENT ON COLUMN dehydration_batches.id IS '主键ID';
COMMENT ON COLUMN dehydration_batches.batch_no IS '脱水批次号';
COMMENT ON COLUMN dehydration_batches.batch_status IS '脱水状态，示例：PENDING/IN_PROGRESS/COMPLETED/CANCELLED';
COMMENT ON COLUMN dehydration_batches.basket_no IS '脱水篮编号';
COMMENT ON COLUMN dehydration_batches.device_no IS '设备编号';
COMMENT ON COLUMN dehydration_batches.started_at IS '上机时间';
COMMENT ON COLUMN dehydration_batches.completed_at IS '完成时间';
COMMENT ON COLUMN dehydration_batches.operator_user_id IS '操作员用户ID';
COMMENT ON COLUMN dehydration_batches.operator_name IS '操作员姓名快照';
COMMENT ON COLUMN dehydration_batches.remarks IS '备注';

CREATE TABLE dehydration_batch_items (
    id VARCHAR2(64) NOT NULL,
    batch_id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64),
    object_type VARCHAR2(32),
    object_id VARCHAR2(64),
    embedding_box_id VARCHAR2(64),
    sampling_block_id VARCHAR2(64),
    item_status VARCHAR2(32) DEFAULT 'LOADED',
    loaded_at TIMESTAMP,
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_dehydration_batch_items PRIMARY KEY (id),
    CONSTRAINT uk_dehydration_batch_items_object UNIQUE (batch_id, object_type, object_id),
    CONSTRAINT fk_dehydration_batch_items_batch FOREIGN KEY (batch_id) REFERENCES dehydration_batches (id),
    CONSTRAINT fk_dehydration_batch_items_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_dehydration_batch_items_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_dehydration_batch_items_box FOREIGN KEY (embedding_box_id) REFERENCES embedding_boxes (id),
    CONSTRAINT fk_dehydration_batch_items_block FOREIGN KEY (sampling_block_id) REFERENCES sampling_blocks (id)
);

COMMENT ON TABLE dehydration_batch_items IS '脱水批次明细表';
COMMENT ON COLUMN dehydration_batch_items.id IS '主键ID';
COMMENT ON COLUMN dehydration_batch_items.batch_id IS '脱水批次ID';
COMMENT ON COLUMN dehydration_batch_items.case_id IS '病例ID';
COMMENT ON COLUMN dehydration_batch_items.specimen_id IS '标本ID';
COMMENT ON COLUMN dehydration_batch_items.object_type IS '明细对象类型，示例：EMBEDDING_BOX/SAMPLING_BLOCK';
COMMENT ON COLUMN dehydration_batch_items.object_id IS '明细对象ID';
COMMENT ON COLUMN dehydration_batch_items.embedding_box_id IS '包埋盒ID';
COMMENT ON COLUMN dehydration_batch_items.sampling_block_id IS '材块ID';
COMMENT ON COLUMN dehydration_batch_items.item_status IS '明细状态，示例：LOADED/PROCESSING/COMPLETED/REMOVED';
COMMENT ON COLUMN dehydration_batch_items.loaded_at IS '装篮时间';
COMMENT ON COLUMN dehydration_batch_items.remarks IS '备注';

CREATE TABLE slicings (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64) NOT NULL,
    embedding_id VARCHAR2(64) NOT NULL,
    slicing_batch_no VARCHAR2(64),
    slicing_status VARCHAR2(32),
    slide_count NUMBER(10),
    slice_thickness NUMBER(6, 2),
    sliced_by_user_id VARCHAR2(64),
    sliced_by_name VARCHAR2(100),
    sliced_at TIMESTAMP,
    quality_issue VARCHAR2(500),
    remarks VARCHAR2(500),
    embedding_box_id VARCHAR2(64),
    slice_count_per_slide NUMBER(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_slicings PRIMARY KEY (id),
    CONSTRAINT uk_slicings_batch_no UNIQUE (slicing_batch_no),
    CONSTRAINT fk_slicings_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_slicings_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_slicings_embedding FOREIGN KEY (embedding_id) REFERENCES embeddings (id)
);

COMMENT ON TABLE slicings IS '切片批次表';
COMMENT ON COLUMN slicings.id IS '主键ID';
COMMENT ON COLUMN slicings.case_id IS '病例ID';
COMMENT ON COLUMN slicings.specimen_id IS '标本ID';
COMMENT ON COLUMN slicings.embedding_id IS '包埋记录ID';
COMMENT ON COLUMN slicings.slicing_batch_no IS '切片批次号';
COMMENT ON COLUMN slicings.slicing_status IS '切片状态，示例：PENDING/IN_PROGRESS/COMPLETED';
COMMENT ON COLUMN slicings.slide_count IS '玻片数量';
COMMENT ON COLUMN slicings.slice_thickness IS '切片厚度';
COMMENT ON COLUMN slicings.sliced_by_user_id IS '切片人用户ID';
COMMENT ON COLUMN slicings.sliced_by_name IS '切片人姓名快照';
COMMENT ON COLUMN slicings.sliced_at IS '切片完成时间';
COMMENT ON COLUMN slicings.quality_issue IS '质量问题';
COMMENT ON COLUMN slicings.remarks IS '备注';

CREATE TABLE slides (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64) NOT NULL,
    slicing_id VARCHAR2(64) NOT NULL,
    sampling_block_id VARCHAR2(64),
    slide_no VARCHAR2(64) NOT NULL,
    slide_label VARCHAR2(200),
    combined_slide_flag NUMBER(1) DEFAULT 0,
    quality_status VARCHAR2(32),
    slide_status VARCHAR2(32),
    slice_count NUMBER(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    embedding_box_id VARCHAR2(64),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_slides PRIMARY KEY (id),
    CONSTRAINT ck_slides_combined_slide_flag CHECK (combined_slide_flag IN (0, 1)),
    CONSTRAINT uk_slides_slide_no UNIQUE (slide_no),
    CONSTRAINT fk_slides_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_slides_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_slides_slicing FOREIGN KEY (slicing_id) REFERENCES slicings (id),
    CONSTRAINT fk_slides_sampling_block FOREIGN KEY (sampling_block_id) REFERENCES sampling_blocks (id)
);

COMMENT ON TABLE slides IS '物理玻片表';
COMMENT ON COLUMN slides.id IS '主键ID';
COMMENT ON COLUMN slides.case_id IS '病例ID';
COMMENT ON COLUMN slides.specimen_id IS '标本ID';
COMMENT ON COLUMN slides.slicing_id IS '切片批次ID';
COMMENT ON COLUMN slides.sampling_block_id IS '取材块ID';
COMMENT ON COLUMN slides.slide_no IS '玻片编号，全局唯一';
COMMENT ON COLUMN slides.slide_label IS '玻片标签';
COMMENT ON COLUMN slides.combined_slide_flag IS '是否合片玻片，0否1是';
COMMENT ON COLUMN slides.quality_status IS '玻片质量状态，示例：QUALIFIED/UNQUALIFIED';
COMMENT ON COLUMN slides.slide_status IS '玻片当前状态，示例：CREATED/STAINED/REWORKED/ARCHIVED';
COMMENT ON COLUMN slides.slice_count IS '玻片切片数量';
COMMENT ON COLUMN slides.created_at IS '创建时间';

CREATE TABLE slide_stainings (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64) NOT NULL,
    slide_id VARCHAR2(64) NOT NULL,
    staining_type VARCHAR2(100),
    staining_status VARCHAR2(32),
    stained_by_user_id VARCHAR2(64),
    stained_by_name VARCHAR2(100),
    stained_at TIMESTAMP,
    quality_issue VARCHAR2(500),
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_slide_stainings PRIMARY KEY (id),
    CONSTRAINT fk_slide_stainings_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_slide_stainings_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_slide_stainings_slide FOREIGN KEY (slide_id) REFERENCES slides (id)
);

COMMENT ON TABLE slide_stainings IS '玻片染色记录表';
COMMENT ON COLUMN slide_stainings.id IS '主键ID';
COMMENT ON COLUMN slide_stainings.case_id IS '病例ID';
COMMENT ON COLUMN slide_stainings.specimen_id IS '标本ID';
COMMENT ON COLUMN slide_stainings.slide_id IS '玻片ID';
COMMENT ON COLUMN slide_stainings.staining_type IS '染色类型';
COMMENT ON COLUMN slide_stainings.staining_status IS '染色状态，示例：PENDING/IN_PROGRESS/COMPLETED';
COMMENT ON COLUMN slide_stainings.stained_by_user_id IS '染色人用户ID';
COMMENT ON COLUMN slide_stainings.stained_by_name IS '染色人姓名快照';
COMMENT ON COLUMN slide_stainings.stained_at IS '染色完成时间';
COMMENT ON COLUMN slide_stainings.quality_issue IS '染色质量问题';
COMMENT ON COLUMN slide_stainings.remarks IS '备注';

CREATE TABLE case_media_assets (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64),
    object_type VARCHAR2(32),
    object_id VARCHAR2(64),
    media_type VARCHAR2(50) NOT NULL,
    capture_node VARCHAR2(50),
    file_url VARCHAR2(500) NOT NULL,
    file_name VARCHAR2(255),
    captured_at TIMESTAMP,
    captured_by_user_id VARCHAR2(64),
    captured_by_name VARCHAR2(100),
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_case_media_assets PRIMARY KEY (id),
    CONSTRAINT fk_case_media_assets_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_case_media_assets_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id)
);

COMMENT ON TABLE case_media_assets IS '病例统一附件与影像归档表';
COMMENT ON COLUMN case_media_assets.id IS '主键ID';
COMMENT ON COLUMN case_media_assets.case_id IS '病例ID';
COMMENT ON COLUMN case_media_assets.specimen_id IS '标本ID';
COMMENT ON COLUMN case_media_assets.object_type IS '关联对象类型，示例：APPLICATION/SPECIMEN/SAMPLING/DEHYDRATION/ARCHIVE';
COMMENT ON COLUMN case_media_assets.object_id IS '关联对象ID';
COMMENT ON COLUMN case_media_assets.media_type IS '附件类型，示例：APPLICATION_FORM/GROSS_IMAGE/DEHYDRATION_IMAGE/ARCHIVE_IMAGE/OTHER';
COMMENT ON COLUMN case_media_assets.capture_node IS '采集节点，示例：APPLICATION/COLLECTION/SAMPLING/DEHYDRATION/ARCHIVE';
COMMENT ON COLUMN case_media_assets.file_url IS '文件访问地址';
COMMENT ON COLUMN case_media_assets.file_name IS '文件名称';
COMMENT ON COLUMN case_media_assets.captured_at IS '采集时间';
COMMENT ON COLUMN case_media_assets.captured_by_user_id IS '采集人用户ID';
COMMENT ON COLUMN case_media_assets.captured_by_name IS '采集人姓名快照';
COMMENT ON COLUMN case_media_assets.remarks IS '备注';

CREATE TABLE cytology_cases (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    cytology_type VARCHAR2(50) NOT NULL,
    prep_method VARCHAR2(50),
    default_slide_count NUMBER(10) DEFAULT 0,
    default_block_count NUMBER(10) DEFAULT 0,
    screening_status VARCHAR2(32) DEFAULT 'PENDING',
    screened_by_user_id VARCHAR2(64),
    screened_by_name VARCHAR2(100),
    screened_at TIMESTAMP,
    remarks VARCHAR2(500),
    CONSTRAINT pk_cytology_cases PRIMARY KEY (id),
    CONSTRAINT uk_cytology_cases_case UNIQUE (case_id),
    CONSTRAINT fk_cytology_cases_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
);

COMMENT ON TABLE cytology_cases IS '细胞学与液基细胞学子流程表';
COMMENT ON COLUMN cytology_cases.id IS '主键ID';
COMMENT ON COLUMN cytology_cases.case_id IS '病例ID';
COMMENT ON COLUMN cytology_cases.cytology_type IS '细胞学类型，示例：CYTOLOGY/LBC';
COMMENT ON COLUMN cytology_cases.prep_method IS '制片方法，示例：SMEAR/THIN_PREP/CENTRIFUGE';
COMMENT ON COLUMN cytology_cases.default_slide_count IS '默认玻片数量';
COMMENT ON COLUMN cytology_cases.default_block_count IS '默认蜡块数量';
COMMENT ON COLUMN cytology_cases.screening_status IS '筛查状态，示例：PENDING/SCREENING/COMPLETED';
COMMENT ON COLUMN cytology_cases.screened_by_user_id IS '筛查人用户ID';
COMMENT ON COLUMN cytology_cases.screened_by_name IS '筛查人姓名快照';
COMMENT ON COLUMN cytology_cases.screened_at IS '筛查完成时间';
COMMENT ON COLUMN cytology_cases.remarks IS '备注';

CREATE TABLE special_orders (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64),
    slide_id VARCHAR2(64),
    special_test_type VARCHAR2(100),
    antibody_reagent VARCHAR2(200),
    order_description VARCHAR2(1000),
    status VARCHAR2(32),
    result_status VARCHAR2(32),
    ordered_by_user_id VARCHAR2(64),
    ordered_by_name VARCHAR2(100),
    ordered_at TIMESTAMP,
    executed_by_user_id VARCHAR2(64),
    executed_by_name VARCHAR2(100),
    executed_at TIMESTAMP,
    result_summary CLOB,
    remarks VARCHAR2(500),
    CONSTRAINT pk_special_orders PRIMARY KEY (id),
    CONSTRAINT fk_special_orders_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_special_orders_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_special_orders_slide FOREIGN KEY (slide_id) REFERENCES slides (id)
);

COMMENT ON TABLE special_orders IS '特检/免疫医嘱表';
COMMENT ON COLUMN special_orders.id IS '主键ID';
COMMENT ON COLUMN special_orders.case_id IS '病例ID';
COMMENT ON COLUMN special_orders.specimen_id IS '标本ID';
COMMENT ON COLUMN special_orders.slide_id IS '玻片ID';
COMMENT ON COLUMN special_orders.special_test_type IS '特检类型';
COMMENT ON COLUMN special_orders.antibody_reagent IS '抗体试剂';
COMMENT ON COLUMN special_orders.order_description IS '医嘱描述';
COMMENT ON COLUMN special_orders.status IS '医嘱状态，示例：PENDING/IN_PROGRESS/COMPLETED/CANCELLED';
COMMENT ON COLUMN special_orders.result_status IS '结果状态，示例：NOT_READY/READY/CONFIRMED';
COMMENT ON COLUMN special_orders.ordered_by_user_id IS '开立人用户ID';
COMMENT ON COLUMN special_orders.ordered_by_name IS '开立人姓名快照';
COMMENT ON COLUMN special_orders.ordered_at IS '开立时间';
COMMENT ON COLUMN special_orders.executed_by_user_id IS '执行人用户ID';
COMMENT ON COLUMN special_orders.executed_by_name IS '执行人姓名快照';
COMMENT ON COLUMN special_orders.executed_at IS '执行完成时间';
COMMENT ON COLUMN special_orders.result_summary IS '结果摘要';
COMMENT ON COLUMN special_orders.remarks IS '备注';

CREATE TABLE molecular_exams (
    id VARCHAR2(64) NOT NULL,
    exam_code VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64),
    patient_id VARCHAR2(64),
    patient_name VARCHAR2(100) NOT NULL,
    patient_gender VARCHAR2(20),
    patient_age NUMBER(3),
    hospital_number VARCHAR2(64),
    outpatient_number VARCHAR2(64),
    apply_department_id VARCHAR2(64),
    apply_department_name VARCHAR2(100),
    apply_doctor_user_id VARCHAR2(64),
    apply_doctor_name VARCHAR2(100),
    apply_date DATE,
    sample_date DATE,
    exam_method VARCHAR2(100),
    sample_type VARCHAR2(100),
    sample_count NUMBER(10),
    clinical_diagnosis VARCHAR2(500),
    pathology_diagnosis VARCHAR2(500),
    molecular_result CLOB,
    diagnosis_status VARCHAR2(32),
    diagnosis_doctor_user_id VARCHAR2(64),
    diagnosis_doctor_name VARCHAR2(100),
    pdf_status VARCHAR2(32) DEFAULT 'NOT_UPLOADED',
    pdf_file_url VARCHAR2(500),
    pdf_file_name VARCHAR2(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_molecular_exams PRIMARY KEY (id),
    CONSTRAINT uk_molecular_exams_exam_code UNIQUE (exam_code),
    CONSTRAINT fk_molecular_exams_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_molecular_exams_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_molecular_exams_patient FOREIGN KEY (patient_id) REFERENCES patients (id)
);

COMMENT ON TABLE molecular_exams IS '分子检测主表';
COMMENT ON COLUMN molecular_exams.id IS '主键ID';
COMMENT ON COLUMN molecular_exams.exam_code IS '检测编号，全局唯一';
COMMENT ON COLUMN molecular_exams.case_id IS '病例ID';
COMMENT ON COLUMN molecular_exams.specimen_id IS '标本ID';
COMMENT ON COLUMN molecular_exams.patient_id IS '患者ID';
COMMENT ON COLUMN molecular_exams.patient_name IS '患者姓名快照';
COMMENT ON COLUMN molecular_exams.patient_gender IS '患者性别快照';
COMMENT ON COLUMN molecular_exams.patient_age IS '患者年龄快照';
COMMENT ON COLUMN molecular_exams.hospital_number IS '住院号';
COMMENT ON COLUMN molecular_exams.outpatient_number IS '门诊号';
COMMENT ON COLUMN molecular_exams.apply_department_id IS '申请科室ID';
COMMENT ON COLUMN molecular_exams.apply_department_name IS '申请科室名称快照';
COMMENT ON COLUMN molecular_exams.apply_doctor_user_id IS '申请医生用户ID';
COMMENT ON COLUMN molecular_exams.apply_doctor_name IS '申请医生姓名快照';
COMMENT ON COLUMN molecular_exams.apply_date IS '申请日期';
COMMENT ON COLUMN molecular_exams.sample_date IS '送检日期';
COMMENT ON COLUMN molecular_exams.exam_method IS '检测方法';
COMMENT ON COLUMN molecular_exams.sample_type IS '标本类型';
COMMENT ON COLUMN molecular_exams.sample_count IS '标本数量';
COMMENT ON COLUMN molecular_exams.clinical_diagnosis IS '临床诊断';
COMMENT ON COLUMN molecular_exams.pathology_diagnosis IS '病理诊断';
COMMENT ON COLUMN molecular_exams.molecular_result IS '分子检测结果';
COMMENT ON COLUMN molecular_exams.diagnosis_status IS '诊断状态，示例：PENDING/DIAGNOSING/COMPLETED';
COMMENT ON COLUMN molecular_exams.diagnosis_doctor_user_id IS '诊断医生用户ID';
COMMENT ON COLUMN molecular_exams.diagnosis_doctor_name IS '诊断医生姓名快照';
COMMENT ON COLUMN molecular_exams.pdf_status IS 'PDF状态，示例：NOT_UPLOADED/UPLOADED/SIGNED';
COMMENT ON COLUMN molecular_exams.pdf_file_url IS 'PDF文件地址';
COMMENT ON COLUMN molecular_exams.pdf_file_name IS 'PDF文件名称';
COMMENT ON COLUMN molecular_exams.created_at IS '创建时间';
COMMENT ON COLUMN molecular_exams.updated_at IS '更新时间';

CREATE TABLE molecular_test_details (
    id BIGINT IDENTITY(1, 1) NOT NULL,
    exam_id VARCHAR2(64) NOT NULL,
    gene_name VARCHAR2(100) NOT NULL,
    mutation_type VARCHAR2(100),
    mutation_site VARCHAR2(100),
    test_result VARCHAR2(500),
    reference_range VARCHAR2(500),
    interpretation VARCHAR2(1000),
    CONSTRAINT pk_molecular_test_details PRIMARY KEY (id),
    CONSTRAINT fk_molecular_test_details_exam FOREIGN KEY (exam_id) REFERENCES molecular_exams (id)
);

COMMENT ON TABLE molecular_test_details IS '分子检测结果明细表';
COMMENT ON COLUMN molecular_test_details.id IS '自增主键';
COMMENT ON COLUMN molecular_test_details.exam_id IS '分子检测主表ID';
COMMENT ON COLUMN molecular_test_details.gene_name IS '基因名称';
COMMENT ON COLUMN molecular_test_details.mutation_type IS '突变类型';
COMMENT ON COLUMN molecular_test_details.mutation_site IS '突变位点';
COMMENT ON COLUMN molecular_test_details.test_result IS '检测结果';
COMMENT ON COLUMN molecular_test_details.reference_range IS '参考范围';
COMMENT ON COLUMN molecular_test_details.interpretation IS '结果解读';

CREATE TABLE diagnostic_tasks (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64),
    pathology_no VARCHAR2(64),
    task_type VARCHAR2(50),
    status VARCHAR2(32),
    priority VARCHAR2(32) DEFAULT 'NORMAL',
    urgent_reason VARCHAR2(500),
    assignment_mode VARCHAR2(32),
    assigned_by_user_id VARCHAR2(64),
    assigned_by_name VARCHAR2(100),
    diagnosis_doctor_user_id VARCHAR2(64),
    diagnosis_doctor_name VARCHAR2(100),
    primary_doctor_user_id VARCHAR2(64),
    primary_doctor_name VARCHAR2(100),
    primary_diagnosed_at TIMESTAMP,
    review_doctor_user_id VARCHAR2(64),
    review_doctor_name VARCHAR2(100),
    review_completed_at TIMESTAMP,
    review_level VARCHAR2(32),
    reviewer_user_id VARCHAR2(64),
    reviewer_name VARCHAR2(100),
    reviewed_at TIMESTAMP,
    assigned_at TIMESTAMP,
    accepted_at TIMESTAMP,
    completed_at TIMESTAMP,
    sla_due_at TIMESTAMP,
    frozen_diagnosis_result VARCHAR2(1000),
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_diagnostic_tasks PRIMARY KEY (id),
    CONSTRAINT fk_diagnostic_tasks_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_diagnostic_tasks_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id)
);

COMMENT ON TABLE diagnostic_tasks IS '诊断任务表';
COMMENT ON COLUMN diagnostic_tasks.id IS '主键ID';
COMMENT ON COLUMN diagnostic_tasks.case_id IS '病例ID';
COMMENT ON COLUMN diagnostic_tasks.specimen_id IS '标本ID';
COMMENT ON COLUMN diagnostic_tasks.pathology_no IS '病理号快照';
COMMENT ON COLUMN diagnostic_tasks.task_type IS '任务类型，示例：PRIMARY/REVIEW/FROZEN/CONSULTATION';
COMMENT ON COLUMN diagnostic_tasks.status IS '任务状态，示例：PENDING/ASSIGNED/ACCEPTED/IN_PROGRESS/COMPLETED/CANCELLED';
COMMENT ON COLUMN diagnostic_tasks.priority IS '优先级，示例：NORMAL/URGENT/STAT';
COMMENT ON COLUMN diagnostic_tasks.urgent_reason IS '紧急原因';
COMMENT ON COLUMN diagnostic_tasks.assignment_mode IS '分派方式，示例：MANUAL/AUTO/ROUND_ROBIN';
COMMENT ON COLUMN diagnostic_tasks.assigned_by_user_id IS '派单人用户ID';
COMMENT ON COLUMN diagnostic_tasks.assigned_by_name IS '派单人姓名快照';
COMMENT ON COLUMN diagnostic_tasks.diagnosis_doctor_user_id IS '责任诊断医生用户ID';
COMMENT ON COLUMN diagnostic_tasks.diagnosis_doctor_name IS '责任诊断医生姓名快照';
COMMENT ON COLUMN diagnostic_tasks.primary_doctor_user_id IS '初诊医生用户ID';
COMMENT ON COLUMN diagnostic_tasks.primary_doctor_name IS '初诊医生姓名快照';
COMMENT ON COLUMN diagnostic_tasks.primary_diagnosed_at IS '初诊完成时间';
COMMENT ON COLUMN diagnostic_tasks.review_doctor_user_id IS '复诊医生用户ID';
COMMENT ON COLUMN diagnostic_tasks.review_doctor_name IS '复诊医生姓名快照';
COMMENT ON COLUMN diagnostic_tasks.review_completed_at IS '复诊完成时间';
COMMENT ON COLUMN diagnostic_tasks.review_level IS '审核级别';
COMMENT ON COLUMN diagnostic_tasks.reviewer_user_id IS '审核医生用户ID';
COMMENT ON COLUMN diagnostic_tasks.reviewer_name IS '审核医生姓名快照';
COMMENT ON COLUMN diagnostic_tasks.reviewed_at IS '审核时间';
COMMENT ON COLUMN diagnostic_tasks.assigned_at IS '任务派发时间';
COMMENT ON COLUMN diagnostic_tasks.accepted_at IS '任务接单时间';
COMMENT ON COLUMN diagnostic_tasks.completed_at IS '任务完成时间';
COMMENT ON COLUMN diagnostic_tasks.sla_due_at IS 'SLA截止时间';
COMMENT ON COLUMN diagnostic_tasks.frozen_diagnosis_result IS '冰冻诊断结果';
COMMENT ON COLUMN diagnostic_tasks.remarks IS '备注';
COMMENT ON COLUMN diagnostic_tasks.created_at IS '创建时间';

CREATE TABLE pathology_reports (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    task_id VARCHAR2(64),
    report_no VARCHAR2(64),
    pathology_no VARCHAR2(64),
    report_scope VARCHAR2(50) DEFAULT 'ROUTINE',
    report_seq NUMBER(10) DEFAULT 1 NOT NULL,
    report_status VARCHAR2(32),
    version_no NUMBER(10) DEFAULT 1,
    specimen_type VARCHAR2(100),
    patient_name VARCHAR2(100),
    submitting_department_id VARCHAR2(64),
    submitting_department_name VARCHAR2(100),
    report_date TIMESTAMP,
    gross_exam CLOB,
    microscopic_exam CLOB,
    tumor_size VARCHAR2(100),
    differentiation_grade VARCHAR2(100),
    invasion_depth VARCHAR2(100),
    clinical_diagnosis VARCHAR2(500),
    final_diagnosis VARCHAR2(2000),
    submitted_at TIMESTAMP,
    reviewer_user_id VARCHAR2(64),
    reviewer_name VARCHAR2(100),
    reviewed_at TIMESTAMP,
    signed_by_user_id VARCHAR2(64),
    signed_by_name VARCHAR2(100),
    signed_at TIMESTAMP,
    published_at TIMESTAMP,
    expected_completion_time TIMESTAMP,
    print_count NUMBER(10) DEFAULT 0,
    last_printed_at TIMESTAMP,
    is_amended NUMBER(1) DEFAULT 0,
    amendment_reason VARCHAR2(500),
    timeout_reason VARCHAR2(500),
    rich_text_content CLOB,
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_pathology_reports PRIMARY KEY (id),
    CONSTRAINT ck_pathology_reports_is_amended CHECK (is_amended IN (0, 1)),
    CONSTRAINT uk_pathology_reports_case_scope_seq UNIQUE (case_id, report_scope, report_seq),
    CONSTRAINT uk_pathology_reports_report_no UNIQUE (report_no),
    CONSTRAINT fk_pathology_reports_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_pathology_reports_task FOREIGN KEY (task_id) REFERENCES diagnostic_tasks (id)
);

COMMENT ON TABLE pathology_reports IS '当前正式病理报告表';
COMMENT ON COLUMN pathology_reports.id IS '主键ID';
COMMENT ON COLUMN pathology_reports.case_id IS '病例ID，同一病例可按范围和序号保留多份当前有效报告';
COMMENT ON COLUMN pathology_reports.task_id IS '诊断任务ID';
COMMENT ON COLUMN pathology_reports.report_no IS '报告编号';
COMMENT ON COLUMN pathology_reports.pathology_no IS '病理号快照';
COMMENT ON COLUMN pathology_reports.report_scope IS '报告范围，示例：ROUTINE/FROZEN/ADDENDUM/CONSULTATION';
COMMENT ON COLUMN pathology_reports.report_seq IS '同一报告范围下的顺序号';
COMMENT ON COLUMN pathology_reports.report_status IS '报告状态，示例：DRAFT/SUBMITTED/REVIEWED/SIGNED/PUBLISHED/WITHDRAWN';
COMMENT ON COLUMN pathology_reports.version_no IS '当前版本号';
COMMENT ON COLUMN pathology_reports.specimen_type IS '标本类型快照';
COMMENT ON COLUMN pathology_reports.patient_name IS '患者姓名快照';
COMMENT ON COLUMN pathology_reports.submitting_department_id IS '送检科室ID';
COMMENT ON COLUMN pathology_reports.submitting_department_name IS '送检科室名称快照';
COMMENT ON COLUMN pathology_reports.report_date IS '报告日期';
COMMENT ON COLUMN pathology_reports.gross_exam IS '大体检查';
COMMENT ON COLUMN pathology_reports.microscopic_exam IS '镜下检查';
COMMENT ON COLUMN pathology_reports.tumor_size IS '肿瘤大小';
COMMENT ON COLUMN pathology_reports.differentiation_grade IS '分化程度';
COMMENT ON COLUMN pathology_reports.invasion_depth IS '浸润深度';
COMMENT ON COLUMN pathology_reports.clinical_diagnosis IS '临床诊断';
COMMENT ON COLUMN pathology_reports.final_diagnosis IS '最终诊断';
COMMENT ON COLUMN pathology_reports.submitted_at IS '提交时间';
COMMENT ON COLUMN pathology_reports.reviewer_user_id IS '审核医生用户ID';
COMMENT ON COLUMN pathology_reports.reviewer_name IS '审核医生姓名快照';
COMMENT ON COLUMN pathology_reports.reviewed_at IS '审核时间';
COMMENT ON COLUMN pathology_reports.signed_by_user_id IS '签发医生用户ID';
COMMENT ON COLUMN pathology_reports.signed_by_name IS '签发医生姓名快照';
COMMENT ON COLUMN pathology_reports.signed_at IS '签发时间';
COMMENT ON COLUMN pathology_reports.published_at IS '发布时间';
COMMENT ON COLUMN pathology_reports.expected_completion_time IS '预计完成时间';
COMMENT ON COLUMN pathology_reports.print_count IS '打印次数';
COMMENT ON COLUMN pathology_reports.last_printed_at IS '最后打印时间';
COMMENT ON COLUMN pathology_reports.is_amended IS '是否修订，0否1是';
COMMENT ON COLUMN pathology_reports.amendment_reason IS '修订原因';
COMMENT ON COLUMN pathology_reports.timeout_reason IS '超时原因';
COMMENT ON COLUMN pathology_reports.rich_text_content IS '富文本正文';

CREATE TABLE report_versions (
    id VARCHAR2(64) NOT NULL,
    report_id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    report_scope VARCHAR2(50),
    report_seq NUMBER(10),
    version_no NUMBER(10) NOT NULL,
    version_status VARCHAR2(32),
    final_diagnosis_snapshot VARCHAR2(2000),
    content_snapshot CLOB,
    signed_by_user_id VARCHAR2(64),
    signed_by_name VARCHAR2(100),
    signed_at TIMESTAMP,
    amended_by_user_id VARCHAR2(64),
    amended_by_name VARCHAR2(100),
    amendment_reason VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_report_versions PRIMARY KEY (id),
    CONSTRAINT uk_report_versions_report_version UNIQUE (report_id, version_no),
    CONSTRAINT fk_report_versions_report FOREIGN KEY (report_id) REFERENCES pathology_reports (id),
    CONSTRAINT fk_report_versions_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
);

COMMENT ON TABLE report_versions IS '病理报告历史版本表';
COMMENT ON COLUMN report_versions.id IS '主键ID';
COMMENT ON COLUMN report_versions.report_id IS '当前报告ID';
COMMENT ON COLUMN report_versions.case_id IS '病例ID';
COMMENT ON COLUMN report_versions.report_scope IS '报告范围快照';
COMMENT ON COLUMN report_versions.report_seq IS '报告顺序号快照';
COMMENT ON COLUMN report_versions.version_no IS '版本号';
COMMENT ON COLUMN report_versions.version_status IS '版本状态，示例：SIGNED/PUBLISHED/WITHDRAWN/AMENDED';
COMMENT ON COLUMN report_versions.final_diagnosis_snapshot IS '最终诊断快照';
COMMENT ON COLUMN report_versions.content_snapshot IS '报告正文快照';
COMMENT ON COLUMN report_versions.signed_by_user_id IS '签发医生用户ID';
COMMENT ON COLUMN report_versions.signed_by_name IS '签发医生姓名快照';
COMMENT ON COLUMN report_versions.signed_at IS '签发时间';
COMMENT ON COLUMN report_versions.amended_by_user_id IS '修订人用户ID';
COMMENT ON COLUMN report_versions.amended_by_name IS '修订人姓名快照';
COMMENT ON COLUMN report_versions.amendment_reason IS '修订原因';
COMMENT ON COLUMN report_versions.created_at IS '版本创建时间';

CREATE TABLE report_revision_requests (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    report_id VARCHAR2(64) NOT NULL,
    current_version_no NUMBER(10) NOT NULL,
    request_status VARCHAR2(32) DEFAULT 'PENDING',
    request_reason VARCHAR2(1000) NOT NULL,
    requested_by_user_id VARCHAR2(64),
    requested_by_name VARCHAR2(100),
    requested_at TIMESTAMP,
    reviewed_by_user_id VARCHAR2(64),
    reviewed_by_name VARCHAR2(100),
    reviewed_at TIMESTAMP,
    reject_reason VARCHAR2(500),
    approved_version_no NUMBER(10),
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_report_revision_requests PRIMARY KEY (id),
    CONSTRAINT fk_report_revision_requests_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_report_revision_requests_report FOREIGN KEY (report_id) REFERENCES pathology_reports (id)
);

COMMENT ON TABLE report_revision_requests IS '病理报告修订申请审批表';
COMMENT ON COLUMN report_revision_requests.id IS '主键ID';
COMMENT ON COLUMN report_revision_requests.case_id IS '病例ID';
COMMENT ON COLUMN report_revision_requests.report_id IS '报告ID';
COMMENT ON COLUMN report_revision_requests.current_version_no IS '当前版本号';
COMMENT ON COLUMN report_revision_requests.request_status IS '申请状态，示例：PENDING/APPROVED/REJECTED/CANCELLED';
COMMENT ON COLUMN report_revision_requests.request_reason IS '修订申请原因';
COMMENT ON COLUMN report_revision_requests.requested_by_user_id IS '申请人用户ID';
COMMENT ON COLUMN report_revision_requests.requested_by_name IS '申请人姓名快照';
COMMENT ON COLUMN report_revision_requests.requested_at IS '申请时间';
COMMENT ON COLUMN report_revision_requests.reviewed_by_user_id IS '审批人用户ID';
COMMENT ON COLUMN report_revision_requests.reviewed_by_name IS '审批人姓名快照';
COMMENT ON COLUMN report_revision_requests.reviewed_at IS '审批时间';
COMMENT ON COLUMN report_revision_requests.reject_reason IS '驳回原因';
COMMENT ON COLUMN report_revision_requests.approved_version_no IS '审批通过后对应的新版本号';
COMMENT ON COLUMN report_revision_requests.remarks IS '备注';

-- =========================================================
-- 归档、交接、流程轨迹
-- =========================================================
CREATE TABLE archives (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    report_id VARCHAR2(64) NOT NULL,
    archive_status VARCHAR2(32),
    archive_location VARCHAR2(200),
    archived_by_user_id VARCHAR2(64),
    archived_by_name VARCHAR2(100),
    archived_at TIMESTAMP,
    borrowed_by_user_id VARCHAR2(64),
    borrowed_by_name VARCHAR2(100),
    borrowed_at TIMESTAMP,
    returned_at TIMESTAMP,
    remarks VARCHAR2(500),
    CONSTRAINT pk_archives PRIMARY KEY (id),
    CONSTRAINT fk_archives_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_archives_report FOREIGN KEY (report_id) REFERENCES pathology_reports (id)
);

COMMENT ON TABLE archives IS '报告归档与借阅记录表';
COMMENT ON COLUMN archives.id IS '主键ID';
COMMENT ON COLUMN archives.case_id IS '病例ID';
COMMENT ON COLUMN archives.report_id IS '报告ID';
COMMENT ON COLUMN archives.archive_status IS '归档状态，示例：ARCHIVED/BORROWED/RETURNED';
COMMENT ON COLUMN archives.archive_location IS '报告归档位置';
COMMENT ON COLUMN archives.archived_by_user_id IS '归档人用户ID';
COMMENT ON COLUMN archives.archived_by_name IS '归档人姓名快照';
COMMENT ON COLUMN archives.archived_at IS '归档时间';
COMMENT ON COLUMN archives.borrowed_by_user_id IS '借阅人用户ID';
COMMENT ON COLUMN archives.borrowed_by_name IS '借阅人姓名快照';
COMMENT ON COLUMN archives.borrowed_at IS '借阅时间';
COMMENT ON COLUMN archives.returned_at IS '归还时间';
COMMENT ON COLUMN archives.remarks IS '备注';

CREATE TABLE archive_cabinets (
    id VARCHAR2(64) NOT NULL,
    cabinet_code VARCHAR2(64) NOT NULL,
    cabinet_name VARCHAR2(100) NOT NULL,
    cabinet_type VARCHAR2(32) NOT NULL,
    capacity NUMBER(10),
    cabinet_status VARCHAR2(32) DEFAULT 'ACTIVE',
    location_description VARCHAR2(200),
    remarks VARCHAR2(500),
    layer_count NUMBER(10) DEFAULT 1 NOT NULL,
    slot_count_per_layer NUMBER(10) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_archive_cabinets PRIMARY KEY (id),
    CONSTRAINT uk_archive_cabinets_code UNIQUE (cabinet_code)
);

COMMENT ON TABLE archive_cabinets IS '归档柜主表';
COMMENT ON COLUMN archive_cabinets.id IS '主键ID';
COMMENT ON COLUMN archive_cabinets.cabinet_code IS '归档柜编号';
COMMENT ON COLUMN archive_cabinets.cabinet_name IS '归档柜名称';
COMMENT ON COLUMN archive_cabinets.cabinet_type IS '归档柜类型，示例：SLIDE/BLOCK/APPLICATION_FORM/MIXED';
COMMENT ON COLUMN archive_cabinets.capacity IS '容量';
COMMENT ON COLUMN archive_cabinets.cabinet_status IS '归档柜状态，示例：ACTIVE/FULL/DISABLED';
COMMENT ON COLUMN archive_cabinets.location_description IS '摆放位置描述';
COMMENT ON COLUMN archive_cabinets.remarks IS '备注';

CREATE TABLE archive_positions (
    id VARCHAR2(64) NOT NULL,
    cabinet_id VARCHAR2(64) NOT NULL,
    position_code VARCHAR2(64) NOT NULL,
    layer_no VARCHAR2(64),
    slot_no VARCHAR2(64),
    position_status VARCHAR2(32) DEFAULT 'AVAILABLE',
    current_object_type VARCHAR2(32),
    current_object_id VARCHAR2(64),
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_archive_positions PRIMARY KEY (id),
    CONSTRAINT uk_archive_positions_code UNIQUE (position_code),
    CONSTRAINT uk_archive_positions_cabinet_slot UNIQUE (cabinet_id, layer_no, slot_no),
    CONSTRAINT fk_archive_positions_cabinet FOREIGN KEY (cabinet_id) REFERENCES archive_cabinets (id)
);

COMMENT ON TABLE archive_positions IS '归档柜位表';
COMMENT ON COLUMN archive_positions.id IS '主键ID';
COMMENT ON COLUMN archive_positions.cabinet_id IS '归档柜ID';
COMMENT ON COLUMN archive_positions.position_code IS '柜位编码';
COMMENT ON COLUMN archive_positions.layer_no IS '层号';
COMMENT ON COLUMN archive_positions.slot_no IS '格号';
COMMENT ON COLUMN archive_positions.position_status IS '柜位状态，示例：AVAILABLE/OCCUPIED/LOCKED';
COMMENT ON COLUMN archive_positions.current_object_type IS '当前占用对象类型';
COMMENT ON COLUMN archive_positions.current_object_id IS '当前占用对象ID';
COMMENT ON COLUMN archive_positions.remarks IS '备注';

CREATE TABLE specimen_storage_records (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64),
    object_type VARCHAR2(32) NOT NULL,
    object_id VARCHAR2(64) NOT NULL,
    storage_status VARCHAR2(32),
    storage_location VARCHAR2(200),
    archive_position_id VARCHAR2(64),
    cabinet_no VARCHAR2(64),
    layer_no VARCHAR2(64),
    slot_no VARCHAR2(64),
    stored_by_user_id VARCHAR2(64),
    stored_by_name VARCHAR2(100),
    stored_at TIMESTAMP,
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_specimen_storage_records PRIMARY KEY (id),
    CONSTRAINT fk_specimen_storage_records_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_specimen_storage_records_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_specimen_storage_records_position FOREIGN KEY (archive_position_id) REFERENCES archive_positions (id)
);

COMMENT ON TABLE specimen_storage_records IS '标本/蜡块/玻片实物流转与存储记录表';
COMMENT ON COLUMN specimen_storage_records.id IS '主键ID';
COMMENT ON COLUMN specimen_storage_records.case_id IS '病例ID';
COMMENT ON COLUMN specimen_storage_records.specimen_id IS '标本ID，可为空';
COMMENT ON COLUMN specimen_storage_records.object_type IS '实物类型，示例：SPECIMEN/BLOCK/SLIDE';
COMMENT ON COLUMN specimen_storage_records.object_id IS '实物对象ID';
COMMENT ON COLUMN specimen_storage_records.storage_status IS '存储状态，示例：IN_STORAGE/BORROWED/DISCARDED';
COMMENT ON COLUMN specimen_storage_records.storage_location IS '存储位置描述';
COMMENT ON COLUMN specimen_storage_records.archive_position_id IS '归档柜位ID';
COMMENT ON COLUMN specimen_storage_records.cabinet_no IS '柜号';
COMMENT ON COLUMN specimen_storage_records.layer_no IS '层号';
COMMENT ON COLUMN specimen_storage_records.slot_no IS '格号';
COMMENT ON COLUMN specimen_storage_records.stored_by_user_id IS '入库人用户ID';
COMMENT ON COLUMN specimen_storage_records.stored_by_name IS '入库人姓名快照';
COMMENT ON COLUMN specimen_storage_records.stored_at IS '入库时间';
COMMENT ON COLUMN specimen_storage_records.remarks IS '备注';

CREATE TABLE material_loans (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64),
    material_type VARCHAR2(32) NOT NULL,
    material_id VARCHAR2(64) NOT NULL,
    archive_position_id VARCHAR2(64),
    loan_status VARCHAR2(32) DEFAULT 'BORROWED',
    borrowed_by_user_id VARCHAR2(64),
    borrowed_by_name VARCHAR2(100),
    borrowed_at TIMESTAMP,
    borrow_purpose VARCHAR2(500),
    approved_by_user_id VARCHAR2(64),
    approved_by_name VARCHAR2(100),
    returned_by_user_id VARCHAR2(64),
    returned_by_name VARCHAR2(100),
    returned_at TIMESTAMP,
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_material_loans PRIMARY KEY (id),
    CONSTRAINT fk_material_loans_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_material_loans_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_material_loans_position FOREIGN KEY (archive_position_id) REFERENCES archive_positions (id)
);

COMMENT ON TABLE material_loans IS '蜡块/玻片/申请单借阅记录表';
COMMENT ON COLUMN material_loans.id IS '主键ID';
COMMENT ON COLUMN material_loans.case_id IS '病例ID';
COMMENT ON COLUMN material_loans.specimen_id IS '标本ID';
COMMENT ON COLUMN material_loans.material_type IS '借阅材料类型，示例：BLOCK/SLIDE/APPLICATION_FORM';
COMMENT ON COLUMN material_loans.material_id IS '借阅材料对象ID';
COMMENT ON COLUMN material_loans.archive_position_id IS '借出前所在柜位ID';
COMMENT ON COLUMN material_loans.loan_status IS '借阅状态，示例：BORROWED/RETURNED/LOST';
COMMENT ON COLUMN material_loans.borrowed_by_user_id IS '借出对象用户ID';
COMMENT ON COLUMN material_loans.borrowed_by_name IS '借出对象姓名快照';
COMMENT ON COLUMN material_loans.borrowed_at IS '借出时间';
COMMENT ON COLUMN material_loans.borrow_purpose IS '借阅用途';
COMMENT ON COLUMN material_loans.approved_by_user_id IS '批准人用户ID';
COMMENT ON COLUMN material_loans.approved_by_name IS '批准人姓名快照';
COMMENT ON COLUMN material_loans.returned_by_user_id IS '归还接收人用户ID';
COMMENT ON COLUMN material_loans.returned_by_name IS '归还接收人姓名快照';
COMMENT ON COLUMN material_loans.returned_at IS '归还时间';
COMMENT ON COLUMN material_loans.remarks IS '备注';

CREATE TABLE workflow_events (
    id VARCHAR2(64) NOT NULL,
    application_id VARCHAR2(64),
    case_id VARCHAR2(64),
    specimen_id VARCHAR2(64),
    transport_order_id VARCHAR2(64),
    node_code VARCHAR2(50) NOT NULL,
    action_code VARCHAR2(50),
    event_type VARCHAR2(64),
    from_status VARCHAR2(32),
    to_status VARCHAR2(32),
    event_status VARCHAR2(32),
    operator_user_id VARCHAR2(64),
    operator_name VARCHAR2(100),
    occurred_at TIMESTAMP,
    event_time TIMESTAMP,
    source_terminal VARCHAR2(64),
    event_content VARCHAR2(1000),
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_workflow_events PRIMARY KEY (id),
    CONSTRAINT fk_workflow_events_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_workflow_events_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_workflow_events_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_workflow_events_transport_order FOREIGN KEY (transport_order_id) REFERENCES transport_orders (id)
);

COMMENT ON TABLE workflow_events IS '病理流程轨迹事件表';
COMMENT ON COLUMN workflow_events.id IS '主键ID';
COMMENT ON COLUMN workflow_events.application_id IS '申请单ID';
COMMENT ON COLUMN workflow_events.case_id IS '病例ID';
COMMENT ON COLUMN workflow_events.specimen_id IS '标本ID，可为空';
COMMENT ON COLUMN workflow_events.transport_order_id IS '转运单ID';
COMMENT ON COLUMN workflow_events.node_code IS '流程节点编码，示例：COLLECTION/FIXATION/TRANSPORT/RECEIPT/REGISTRATION/SAMPLING/DEHYDRATION/EMBEDDING/SLICING/STAINING/DIAGNOSIS/REVIEW/PUBLISH/ARCHIVE/REWORK/QC';
COMMENT ON COLUMN workflow_events.action_code IS '动作编码，示例：CREATE/ASSIGN/ACCEPT/COMPLETE/REJECT/RETURN/AMEND/BORROW/BACK';
COMMENT ON COLUMN workflow_events.event_type IS '事件类型';
COMMENT ON COLUMN workflow_events.from_status IS '变更前状态';
COMMENT ON COLUMN workflow_events.to_status IS '变更后状态';
COMMENT ON COLUMN workflow_events.event_status IS '事件结果状态';
COMMENT ON COLUMN workflow_events.operator_user_id IS '操作人用户ID';
COMMENT ON COLUMN workflow_events.operator_name IS '操作人姓名快照';
COMMENT ON COLUMN workflow_events.occurred_at IS '发生时间';
COMMENT ON COLUMN workflow_events.event_time IS '事件发生时间';
COMMENT ON COLUMN workflow_events.source_terminal IS '来源终端';
COMMENT ON COLUMN workflow_events.event_content IS '事件内容';
COMMENT ON COLUMN workflow_events.remarks IS '备注';
COMMENT ON COLUMN workflow_events.created_at IS '创建时间';

CREATE TABLE handover_logs (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64),
    from_node VARCHAR2(50) NOT NULL,
    to_node VARCHAR2(50) NOT NULL,
    handover_user_id VARCHAR2(64),
    handover_user_name VARCHAR2(100),
    receiver_user_id VARCHAR2(64),
    receiver_user_name VARCHAR2(100),
    handover_at TIMESTAMP NOT NULL,
    remarks VARCHAR2(500),
    CONSTRAINT pk_handover_logs PRIMARY KEY (id),
    CONSTRAINT fk_handover_logs_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_handover_logs_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id)
);

COMMENT ON TABLE handover_logs IS '流程节点交接记录表';
COMMENT ON COLUMN handover_logs.id IS '主键ID';
COMMENT ON COLUMN handover_logs.case_id IS '病例ID';
COMMENT ON COLUMN handover_logs.specimen_id IS '标本ID，可为空';
COMMENT ON COLUMN handover_logs.from_node IS '交出节点';
COMMENT ON COLUMN handover_logs.to_node IS '接收节点';
COMMENT ON COLUMN handover_logs.handover_user_id IS '交出人用户ID';
COMMENT ON COLUMN handover_logs.handover_user_name IS '交出人姓名快照';
COMMENT ON COLUMN handover_logs.receiver_user_id IS '接收人用户ID';
COMMENT ON COLUMN handover_logs.receiver_user_name IS '接收人姓名快照';
COMMENT ON COLUMN handover_logs.handover_at IS '交接时间';
COMMENT ON COLUMN handover_logs.remarks IS '备注';

CREATE TABLE frozen_sessions (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    session_no NUMBER(10) NOT NULL,
    session_status VARCHAR2(32) DEFAULT 'REQUESTED',
    request_time TIMESTAMP,
    request_doctor_user_id VARCHAR2(64),
    request_doctor_name VARCHAR2(100),
    intraoperative_phone_back NUMBER(1) DEFAULT 0,
    preliminary_result CLOB,
    final_confirmed_flag NUMBER(1) DEFAULT 0,
    confirmed_by_user_id VARCHAR2(64),
    confirmed_by_name VARCHAR2(100),
    final_confirmed_at TIMESTAMP,
    remarks VARCHAR2(500),
    CONSTRAINT pk_frozen_sessions PRIMARY KEY (id),
    CONSTRAINT ck_frozen_sessions_phone_back CHECK (intraoperative_phone_back IN (0, 1)),
    CONSTRAINT ck_frozen_sessions_confirmed CHECK (final_confirmed_flag IN (0, 1)),
    CONSTRAINT uk_frozen_sessions_case_session UNIQUE (case_id, session_no),
    CONSTRAINT fk_frozen_sessions_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
);

COMMENT ON TABLE frozen_sessions IS '冰冻流程会话表';
COMMENT ON COLUMN frozen_sessions.id IS '主键ID';
COMMENT ON COLUMN frozen_sessions.case_id IS '病例ID';
COMMENT ON COLUMN frozen_sessions.session_no IS '同一病例下的冰冻会话序号';
COMMENT ON COLUMN frozen_sessions.session_status IS '会话状态，示例：REQUESTED/DIAGNOSING/REPORTED/CONFIRMED/CANCELLED';
COMMENT ON COLUMN frozen_sessions.request_time IS '冰冻申请时间';
COMMENT ON COLUMN frozen_sessions.request_doctor_user_id IS '申请医生用户ID';
COMMENT ON COLUMN frozen_sessions.request_doctor_name IS '申请医生姓名快照';
COMMENT ON COLUMN frozen_sessions.intraoperative_phone_back IS '是否术中电话回报，0否1是';
COMMENT ON COLUMN frozen_sessions.preliminary_result IS '术中初步结果';
COMMENT ON COLUMN frozen_sessions.final_confirmed_flag IS '是否最终确认，0否1是';
COMMENT ON COLUMN frozen_sessions.confirmed_by_user_id IS '最终确认人用户ID';
COMMENT ON COLUMN frozen_sessions.confirmed_by_name IS '最终确认人姓名快照';
COMMENT ON COLUMN frozen_sessions.final_confirmed_at IS '最终确认时间';
COMMENT ON COLUMN frozen_sessions.remarks IS '备注';

CREATE TABLE consultation_cases (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    consultation_type VARCHAR2(50),
    status VARCHAR2(32),
    requested_by_user_id VARCHAR2(64),
    requested_by_name VARCHAR2(100),
    requested_at TIMESTAMP,
    expert_name VARCHAR2(100),
    expert_org VARCHAR2(200),
    opinion CLOB,
    completed_at TIMESTAMP,
    remarks VARCHAR2(500),
    host_user_id VARCHAR2(64),
    host_name VARCHAR2(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_consultation_cases PRIMARY KEY (id),
    CONSTRAINT fk_consultation_cases_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
);

COMMENT ON TABLE consultation_cases IS '病理会诊流程表';
COMMENT ON COLUMN consultation_cases.id IS '主键ID';
COMMENT ON COLUMN consultation_cases.case_id IS '病例ID';
COMMENT ON COLUMN consultation_cases.consultation_type IS '会诊类型，示例：INTERNAL/EXTERNAL/MDT';
COMMENT ON COLUMN consultation_cases.status IS '会诊状态，示例：PENDING/IN_PROGRESS/COMPLETED/CANCELLED';
COMMENT ON COLUMN consultation_cases.requested_by_user_id IS '申请人用户ID';
COMMENT ON COLUMN consultation_cases.requested_by_name IS '申请人姓名快照';
COMMENT ON COLUMN consultation_cases.requested_at IS '申请时间';
COMMENT ON COLUMN consultation_cases.expert_name IS '会诊专家姓名';
COMMENT ON COLUMN consultation_cases.expert_org IS '会诊专家机构';
COMMENT ON COLUMN consultation_cases.opinion IS '会诊意见';
COMMENT ON COLUMN consultation_cases.completed_at IS '完成时间';
COMMENT ON COLUMN consultation_cases.remarks IS '备注';

CREATE TABLE consultation_participants (
    id VARCHAR2(64) NOT NULL,
    consultation_id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    participant_user_id VARCHAR2(64),
    participant_name VARCHAR2(100) NOT NULL,
    participant_role VARCHAR2(50),
    expert_org VARCHAR2(200),
    opinion CLOB,
    drafted_by_user_id VARCHAR2(64),
    drafted_by_name VARCHAR2(100),
    read_flag NUMBER(1) DEFAULT 0,
    read_at TIMESTAMP,
    commented_at TIMESTAMP,
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_consultation_participants PRIMARY KEY (id),
    CONSTRAINT ck_consultation_participants_read_flag CHECK (read_flag IN (0, 1)),
    CONSTRAINT fk_consultation_participants_consultation FOREIGN KEY (consultation_id) REFERENCES consultation_cases (id),
    CONSTRAINT fk_consultation_participants_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
);

COMMENT ON TABLE consultation_participants IS '会诊参与人明细表';
COMMENT ON COLUMN consultation_participants.id IS '主键ID';
COMMENT ON COLUMN consultation_participants.consultation_id IS '会诊单ID';
COMMENT ON COLUMN consultation_participants.case_id IS '病例ID';
COMMENT ON COLUMN consultation_participants.participant_user_id IS '参与人用户ID';
COMMENT ON COLUMN consultation_participants.participant_name IS '参与人姓名快照';
COMMENT ON COLUMN consultation_participants.participant_role IS '参与角色，示例：HOST/EXPERT/RECORDER/OBSERVER';
COMMENT ON COLUMN consultation_participants.expert_org IS '外部专家机构';
COMMENT ON COLUMN consultation_participants.opinion IS '参与人意见';
COMMENT ON COLUMN consultation_participants.drafted_by_user_id IS '代写人用户ID';
COMMENT ON COLUMN consultation_participants.drafted_by_name IS '代写人姓名快照';
COMMENT ON COLUMN consultation_participants.read_flag IS '是否已读，0否1是';
COMMENT ON COLUMN consultation_participants.read_at IS '已读时间';
COMMENT ON COLUMN consultation_participants.commented_at IS '意见录入时间';
COMMENT ON COLUMN consultation_participants.remarks IS '备注';

CREATE TABLE rework_orders (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64),
    slide_id VARCHAR2(64),
    rework_type VARCHAR2(50),
    status VARCHAR2(32),
    reason VARCHAR2(500),
    requested_by_user_id VARCHAR2(64),
    requested_by_name VARCHAR2(100),
    requested_at TIMESTAMP,
    executed_by_user_id VARCHAR2(64),
    executed_by_name VARCHAR2(100),
    executed_at TIMESTAMP,
    remarks VARCHAR2(500),
    sampling_block_id VARCHAR2(64),
    embedding_box_id VARCHAR2(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_rework_orders PRIMARY KEY (id),
    CONSTRAINT fk_rework_orders_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_rework_orders_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_rework_orders_slide FOREIGN KEY (slide_id) REFERENCES slides (id)
);

COMMENT ON TABLE rework_orders IS '返工医嘱表';
COMMENT ON COLUMN rework_orders.id IS '主键ID';
COMMENT ON COLUMN rework_orders.case_id IS '病例ID';
COMMENT ON COLUMN rework_orders.specimen_id IS '标本ID';
COMMENT ON COLUMN rework_orders.slide_id IS '玻片ID';
COMMENT ON COLUMN rework_orders.rework_type IS '返工类型，示例：DEEPER_CUT/THIN_SECTION/RECUT/RESTAIN/REEMBED/RESAMPLE/ADD_SLIDE';
COMMENT ON COLUMN rework_orders.status IS '返工状态，示例：PENDING/IN_PROGRESS/COMPLETED/CANCELLED';
COMMENT ON COLUMN rework_orders.reason IS '返工原因';
COMMENT ON COLUMN rework_orders.requested_by_user_id IS '申请人用户ID';
COMMENT ON COLUMN rework_orders.requested_by_name IS '申请人姓名快照';
COMMENT ON COLUMN rework_orders.requested_at IS '申请时间';
COMMENT ON COLUMN rework_orders.executed_by_user_id IS '执行人用户ID';
COMMENT ON COLUMN rework_orders.executed_by_name IS '执行人姓名快照';
COMMENT ON COLUMN rework_orders.executed_at IS '执行完成时间';
COMMENT ON COLUMN rework_orders.remarks IS '备注';

-- =========================================================
-- 质量与辅助配置
-- =========================================================
CREATE TABLE sampling_evaluations (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64),
    embedding_id VARCHAR2(64),
    evaluation_level VARCHAR2(32) NOT NULL,
    evaluation_content VARCHAR2(1000) NOT NULL,
    improvement_suggestion VARCHAR2(1000),
    evaluator_user_id VARCHAR2(64),
    evaluator_name VARCHAR2(100),
    evaluated_at TIMESTAMP,
    CONSTRAINT pk_sampling_evaluations PRIMARY KEY (id),
    CONSTRAINT fk_sampling_evaluations_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_sampling_evaluations_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_sampling_evaluations_embedding FOREIGN KEY (embedding_id) REFERENCES embeddings (id)
);

COMMENT ON TABLE sampling_evaluations IS '取材/包埋质量评价表';
COMMENT ON COLUMN sampling_evaluations.id IS '主键ID';
COMMENT ON COLUMN sampling_evaluations.case_id IS '病例ID';
COMMENT ON COLUMN sampling_evaluations.specimen_id IS '标本ID';
COMMENT ON COLUMN sampling_evaluations.embedding_id IS '包埋记录ID';
COMMENT ON COLUMN sampling_evaluations.evaluation_level IS '评价等级';
COMMENT ON COLUMN sampling_evaluations.evaluation_content IS '评价内容';
COMMENT ON COLUMN sampling_evaluations.improvement_suggestion IS '改进建议';
COMMENT ON COLUMN sampling_evaluations.evaluator_user_id IS '评价人用户ID';
COMMENT ON COLUMN sampling_evaluations.evaluator_name IS '评价人姓名快照';
COMMENT ON COLUMN sampling_evaluations.evaluated_at IS '评价时间';

CREATE TABLE slide_qc_evaluations (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64),
    slide_id VARCHAR2(64) NOT NULL,
    qc_type VARCHAR2(50) NOT NULL,
    evaluation_result VARCHAR2(32) NOT NULL,
    issue_description VARCHAR2(1000),
    improvement_suggestion VARCHAR2(1000),
    evaluator_user_id VARCHAR2(64),
    evaluator_name VARCHAR2(100),
    evaluated_at TIMESTAMP,
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_slide_qc_evaluations PRIMARY KEY (id),
    CONSTRAINT fk_slide_qc_evaluations_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_slide_qc_evaluations_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_slide_qc_evaluations_slide FOREIGN KEY (slide_id) REFERENCES slides (id)
);

COMMENT ON TABLE slide_qc_evaluations IS '玻片质控评价表';
COMMENT ON COLUMN slide_qc_evaluations.id IS '主键ID';
COMMENT ON COLUMN slide_qc_evaluations.case_id IS '病例ID';
COMMENT ON COLUMN slide_qc_evaluations.specimen_id IS '标本ID';
COMMENT ON COLUMN slide_qc_evaluations.slide_id IS '玻片ID';
COMMENT ON COLUMN slide_qc_evaluations.qc_type IS '质控类型，示例：HE/IHC/SPECIAL/MOLECULAR';
COMMENT ON COLUMN slide_qc_evaluations.evaluation_result IS '评价结果，示例：PASS/FAIL/REWORK_REQUIRED';
COMMENT ON COLUMN slide_qc_evaluations.issue_description IS '问题描述';
COMMENT ON COLUMN slide_qc_evaluations.improvement_suggestion IS '改进建议';
COMMENT ON COLUMN slide_qc_evaluations.evaluator_user_id IS '评价人用户ID';
COMMENT ON COLUMN slide_qc_evaluations.evaluator_name IS '评价人姓名快照';
COMMENT ON COLUMN slide_qc_evaluations.evaluated_at IS '评价时间';
COMMENT ON COLUMN slide_qc_evaluations.remarks IS '备注';

CREATE TABLE cytology_review_records (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    cytology_case_id VARCHAR2(64) NOT NULL,
    slide_id VARCHAR2(64),
    review_type VARCHAR2(50) NOT NULL,
    review_result VARCHAR2(32) NOT NULL,
    review_opinion VARCHAR2(1000),
    reviewer_user_id VARCHAR2(64),
    reviewer_name VARCHAR2(100),
    reviewed_at TIMESTAMP,
    remarks VARCHAR2(500),
    CONSTRAINT pk_cytology_review_records PRIMARY KEY (id),
    CONSTRAINT fk_cytology_review_records_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_cytology_review_records_cytology FOREIGN KEY (cytology_case_id) REFERENCES cytology_cases (id),
    CONSTRAINT fk_cytology_review_records_slide FOREIGN KEY (slide_id) REFERENCES slides (id)
);

COMMENT ON TABLE cytology_review_records IS '细胞学抽查复核记录表';
COMMENT ON COLUMN cytology_review_records.id IS '主键ID';
COMMENT ON COLUMN cytology_review_records.case_id IS '病例ID';
COMMENT ON COLUMN cytology_review_records.cytology_case_id IS '细胞学子流程ID';
COMMENT ON COLUMN cytology_review_records.slide_id IS '玻片ID';
COMMENT ON COLUMN cytology_review_records.review_type IS '复核类型，示例：INTERNAL_SPOT_CHECK/TERTIARY_REVIEW/FINAL_REVIEW';
COMMENT ON COLUMN cytology_review_records.review_result IS '复核结果，示例：PASS/FAIL/RECHECK_REQUIRED';
COMMENT ON COLUMN cytology_review_records.review_opinion IS '复核意见';
COMMENT ON COLUMN cytology_review_records.reviewer_user_id IS '复核人用户ID';
COMMENT ON COLUMN cytology_review_records.reviewer_name IS '复核人姓名快照';
COMMENT ON COLUMN cytology_review_records.reviewed_at IS '复核时间';
COMMENT ON COLUMN cytology_review_records.remarks IS '备注';

CREATE TABLE medical_orders (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64),
    order_number VARCHAR2(64) NOT NULL,
    order_content VARCHAR2(1000) NOT NULL,
    order_type VARCHAR2(50) NOT NULL,
    execution_scope VARCHAR2(50),
    billing_status VARCHAR2(32) DEFAULT 'PENDING',
    status VARCHAR2(32) DEFAULT 'PENDING' NOT NULL,
    doctor_user_id VARCHAR2(64),
    doctor_name VARCHAR2(100) NOT NULL,
    doctor_department_id VARCHAR2(64),
    doctor_department_name VARCHAR2(100),
    order_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    executor_user_id VARCHAR2(64),
    executor_name VARCHAR2(100),
    accepted_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    remarks VARCHAR2(500),
    CONSTRAINT pk_medical_orders PRIMARY KEY (id),
    CONSTRAINT uk_medical_orders_order_number UNIQUE (order_number),
    CONSTRAINT fk_medical_orders_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
);

COMMENT ON TABLE medical_orders IS '通用病理医嘱表';
COMMENT ON COLUMN medical_orders.id IS '主键ID';
COMMENT ON COLUMN medical_orders.case_id IS '病例ID，可为空';
COMMENT ON COLUMN medical_orders.order_number IS '医嘱单号';
COMMENT ON COLUMN medical_orders.order_content IS '医嘱内容';
COMMENT ON COLUMN medical_orders.order_type IS '医嘱类型';
COMMENT ON COLUMN medical_orders.execution_scope IS '执行范围，示例：TECHNICAL/DOCTOR/SPECIAL/MOLECULAR';
COMMENT ON COLUMN medical_orders.billing_status IS '计费状态，示例：PENDING/BILLED/FAILED/WAIVED';
COMMENT ON COLUMN medical_orders.status IS '医嘱状态，示例：PENDING/IN_PROGRESS/COMPLETED/CANCELLED';
COMMENT ON COLUMN medical_orders.doctor_user_id IS '开立医生用户ID';
COMMENT ON COLUMN medical_orders.doctor_name IS '开立医生姓名快照';
COMMENT ON COLUMN medical_orders.doctor_department_id IS '开立科室ID';
COMMENT ON COLUMN medical_orders.doctor_department_name IS '开立科室名称快照';
COMMENT ON COLUMN medical_orders.order_date IS '开立日期';
COMMENT ON COLUMN medical_orders.created_at IS '创建时间';
COMMENT ON COLUMN medical_orders.updated_at IS '更新时间';

CREATE TABLE billing_records (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    order_id VARCHAR2(64),
    billing_no VARCHAR2(64),
    billing_stage VARCHAR2(50),
    item_type VARCHAR2(50),
    item_name VARCHAR2(200),
    quantity NUMBER(10, 2) DEFAULT 1,
    amount NUMBER(12, 2),
    billing_status VARCHAR2(32) DEFAULT 'PENDING',
    billed_at TIMESTAMP,
    operator_user_id VARCHAR2(64),
    operator_name VARCHAR2(100),
    external_bill_no VARCHAR2(64),
    remarks VARCHAR2(500),
    external_system VARCHAR2(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_billing_records PRIMARY KEY (id),
    CONSTRAINT uk_billing_records_billing_no UNIQUE (billing_no),
    CONSTRAINT fk_billing_records_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_billing_records_order FOREIGN KEY (order_id) REFERENCES medical_orders (id)
);

COMMENT ON TABLE billing_records IS '病理计费留痕表';
COMMENT ON COLUMN billing_records.id IS '主键ID';
COMMENT ON COLUMN billing_records.case_id IS '病例ID';
COMMENT ON COLUMN billing_records.order_id IS '医嘱ID';
COMMENT ON COLUMN billing_records.billing_no IS '计费流水号';
COMMENT ON COLUMN billing_records.billing_stage IS '计费阶段，示例：RECEIPT/SPECIAL_ORDER/REPORT_PUBLISH';
COMMENT ON COLUMN billing_records.item_type IS '计费项目类型';
COMMENT ON COLUMN billing_records.item_name IS '计费项目名称';
COMMENT ON COLUMN billing_records.quantity IS '数量';
COMMENT ON COLUMN billing_records.amount IS '金额';
COMMENT ON COLUMN billing_records.billing_status IS '计费状态，示例：PENDING/SUCCESS/FAILED/REVERSED';
COMMENT ON COLUMN billing_records.billed_at IS '计费时间';
COMMENT ON COLUMN billing_records.operator_user_id IS '计费操作人用户ID';
COMMENT ON COLUMN billing_records.operator_name IS '计费操作人姓名快照';
COMMENT ON COLUMN billing_records.external_bill_no IS 'HIS或第三方回写计费单号';
COMMENT ON COLUMN billing_records.remarks IS '备注';

CREATE TABLE medical_order_changes (
    id VARCHAR2(64) NOT NULL,
    order_id VARCHAR2(64) NOT NULL,
    change_type VARCHAR2(50) NOT NULL,
    previous_status VARCHAR2(32),
    new_status VARCHAR2(32),
    previous_content VARCHAR2(1000),
    new_content VARCHAR2(1000),
    change_reason VARCHAR2(500),
    changed_by_user_id VARCHAR2(64),
    changed_by_name VARCHAR2(100) NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_medical_order_changes PRIMARY KEY (id),
    CONSTRAINT fk_medical_order_changes_order FOREIGN KEY (order_id) REFERENCES medical_orders (id)
);

COMMENT ON TABLE medical_order_changes IS '医嘱变更记录表';
COMMENT ON COLUMN medical_order_changes.id IS '主键ID';
COMMENT ON COLUMN medical_order_changes.order_id IS '医嘱ID';
COMMENT ON COLUMN medical_order_changes.change_type IS '变更类型';
COMMENT ON COLUMN medical_order_changes.previous_status IS '变更前状态';
COMMENT ON COLUMN medical_order_changes.new_status IS '变更后状态';
COMMENT ON COLUMN medical_order_changes.previous_content IS '变更前内容';
COMMENT ON COLUMN medical_order_changes.new_content IS '变更后内容';
COMMENT ON COLUMN medical_order_changes.change_reason IS '变更原因';
COMMENT ON COLUMN medical_order_changes.changed_by_user_id IS '变更人用户ID';
COMMENT ON COLUMN medical_order_changes.changed_by_name IS '变更人姓名快照';
COMMENT ON COLUMN medical_order_changes.changed_at IS '变更时间';

CREATE TABLE reagents (
    id VARCHAR2(64) NOT NULL,
    reagent_code VARCHAR2(64) NOT NULL,
    reagent_name VARCHAR2(100) NOT NULL,
    specification VARCHAR2(100),
    unit VARCHAR2(32),
    manufacturer VARCHAR2(200),
    default_low_stock_threshold NUMBER(18, 2),
    default_near_expiry_days NUMBER(10),
    enabled NUMBER(1) DEFAULT 1,
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_reagents PRIMARY KEY (id),
    CONSTRAINT uk_reagents_code UNIQUE (reagent_code)
);

CREATE TABLE reagent_stocks (
    id VARCHAR2(64) NOT NULL,
    reagent_id VARCHAR2(64) NOT NULL,
    batch_no VARCHAR2(64) NOT NULL,
    stock_quantity NUMBER(18, 2) NOT NULL,
    stock_status VARCHAR2(32) NOT NULL,
    expiry_date DATE,
    storage_location VARCHAR2(200),
    low_stock_threshold NUMBER(18, 2),
    near_expiry_days NUMBER(10),
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_reagent_stocks PRIMARY KEY (id),
    CONSTRAINT uk_reagent_stocks_batch UNIQUE (reagent_id, batch_no),
    CONSTRAINT fk_reagent_stocks_reagent FOREIGN KEY (reagent_id) REFERENCES reagents (id)
);

CREATE TABLE equipment_records (
    id VARCHAR2(64) NOT NULL,
    equipment_code VARCHAR2(64) NOT NULL,
    equipment_name VARCHAR2(100) NOT NULL,
    equipment_category VARCHAR2(64),
    model_no VARCHAR2(100),
    equipment_status VARCHAR2(32) NOT NULL,
    location_description VARCHAR2(200),
    enabled_at TIMESTAMP,
    next_maintenance_at TIMESTAMP,
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_equipment_records PRIMARY KEY (id),
    CONSTRAINT uk_equipment_records_code UNIQUE (equipment_code)
);

CREATE TABLE equipment_maintenance_logs (
    id VARCHAR2(64) NOT NULL,
    equipment_id VARCHAR2(64) NOT NULL,
    maintenance_type VARCHAR2(32) NOT NULL,
    maintenance_status VARCHAR2(32) NOT NULL,
    performed_at TIMESTAMP NOT NULL,
    performed_by_user_id VARCHAR2(64),
    performed_by_name VARCHAR2(100),
    description VARCHAR2(1000),
    next_maintenance_at TIMESTAMP,
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_equipment_maintenance_logs PRIMARY KEY (id),
    CONSTRAINT fk_equipment_maintenance_logs_equipment FOREIGN KEY (equipment_id) REFERENCES equipment_records (id)
);

CREATE TABLE pdf_signature_configs (
    id BIGINT IDENTITY(1, 1) NOT NULL,
    doctor_id VARCHAR2(64) NOT NULL,
    doctor_name VARCHAR2(100) NOT NULL,
    signature_image_url VARCHAR2(500) NOT NULL,
    position_x NUMBER(10, 2) DEFAULT 100,
    position_y NUMBER(10, 2) DEFAULT 100,
    width NUMBER(10, 2) DEFAULT 150,
    height NUMBER(10, 2) DEFAULT 80,
    enabled NUMBER(1) DEFAULT 1,
    CONSTRAINT pk_pdf_signature_configs PRIMARY KEY (id),
    CONSTRAINT ck_pdf_signature_configs_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT fk_pdf_signature_configs_doctor FOREIGN KEY (doctor_id) REFERENCES users (id)
);

COMMENT ON TABLE pdf_signature_configs IS 'PDF签名配置表';
COMMENT ON COLUMN pdf_signature_configs.id IS '自增主键';
COMMENT ON COLUMN pdf_signature_configs.doctor_id IS '医生ID';
COMMENT ON COLUMN pdf_signature_configs.doctor_name IS '医生姓名快照';
COMMENT ON COLUMN pdf_signature_configs.signature_image_url IS '签名图片地址';
COMMENT ON COLUMN pdf_signature_configs.position_x IS '签名横坐标';
COMMENT ON COLUMN pdf_signature_configs.position_y IS '签名纵坐标';
COMMENT ON COLUMN pdf_signature_configs.width IS '签名宽度';
COMMENT ON COLUMN pdf_signature_configs.height IS '签名高度';
COMMENT ON COLUMN pdf_signature_configs.enabled IS '是否启用，0否1是';

-- =========================================================
-- 基线补齐结构（与当前程序最终迁移状态对齐）
-- =========================================================
CREATE TABLE medical_order_packages (
    id VARCHAR2(64) NOT NULL,
    package_code VARCHAR2(64) NOT NULL,
    package_name VARCHAR2(100) NOT NULL,
    package_type VARCHAR2(50),
    owner_user_id VARCHAR2(64),
    enabled NUMBER(1) DEFAULT 1,
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_medical_order_packages PRIMARY KEY (id),
    CONSTRAINT uk_medical_order_packages_code UNIQUE (package_code),
    CONSTRAINT fk_medical_order_packages_owner FOREIGN KEY (owner_user_id) REFERENCES users (id)
);

CREATE TABLE medical_order_package_items (
    id VARCHAR2(64) NOT NULL,
    package_id VARCHAR2(64) NOT NULL,
    order_item_id VARCHAR2(64) NOT NULL,
    sort_order NUMBER(10) DEFAULT 0,
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_medical_order_package_items PRIMARY KEY (id),
    CONSTRAINT uk_medical_order_package_items UNIQUE (package_id, order_item_id),
    CONSTRAINT fk_medical_order_package_items_package FOREIGN KEY (package_id) REFERENCES medical_order_packages (id),
    CONSTRAINT fk_medical_order_package_items_item FOREIGN KEY (order_item_id) REFERENCES medical_order_dict_items (id)
);

CREATE TABLE numbering_rules (
    id VARCHAR2(64) NOT NULL,
    rule_code VARCHAR2(64) NOT NULL,
    biz_type VARCHAR2(64) NOT NULL,
    prefix_pattern VARCHAR2(64),
    date_pattern VARCHAR2(32),
    seq_length NUMBER(10) NOT NULL,
    reset_policy VARCHAR2(32) NOT NULL,
    scope_type VARCHAR2(32) DEFAULT 'GLOBAL',
    enabled NUMBER(1) DEFAULT 1,
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_numbering_rules PRIMARY KEY (id),
    CONSTRAINT uk_numbering_rules_rule_code UNIQUE (rule_code),
    CONSTRAINT uk_numbering_rules_biz_type UNIQUE (biz_type)
);

CREATE TABLE numbering_counters (
    id VARCHAR2(64) NOT NULL,
    rule_code VARCHAR2(64) NOT NULL,
    period_key VARCHAR2(32) NOT NULL,
    scope_key VARCHAR2(64) NOT NULL,
    current_value NUMBER(19) NOT NULL,
    version NUMBER(10) DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_numbering_counters PRIMARY KEY (id),
    CONSTRAINT uk_numbering_counters_scope UNIQUE (rule_code, period_key, scope_key),
    CONSTRAINT fk_numbering_counters_rule FOREIGN KEY (rule_code) REFERENCES numbering_rules (rule_code)
);

CREATE TABLE auth_access_tokens (
    jti VARCHAR2(128) NOT NULL,
    user_id VARCHAR2(64) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    client_ip VARCHAR2(64),
    client_device VARCHAR2(200),
    CONSTRAINT pk_auth_access_tokens PRIMARY KEY (jti),
    CONSTRAINT fk_auth_access_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_auth_access_tokens_user_id ON auth_access_tokens (user_id);

CREATE TABLE technical_pending_tasks (
    id VARCHAR2(64) NOT NULL,
    application_id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64),
    specimen_id VARCHAR2(64),
    task_type VARCHAR2(64) NOT NULL,
    task_status VARCHAR2(32) NOT NULL,
    object_type VARCHAR2(32),
    object_id VARCHAR2(64),
    parent_task_id VARCHAR2(64),
    payload CLOB,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_technical_pending_tasks PRIMARY KEY (id),
    CONSTRAINT fk_technical_pending_tasks_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_technical_pending_tasks_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_technical_pending_tasks_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_technical_pending_tasks_parent FOREIGN KEY (parent_task_id) REFERENCES technical_pending_tasks (id)
);

CREATE TABLE integration_tasks (
    id VARCHAR2(64) NOT NULL,
    task_type VARCHAR2(64) NOT NULL,
    business_type VARCHAR2(64) NOT NULL,
    business_id VARCHAR2(64) NOT NULL,
    stage_code VARCHAR2(64) NOT NULL,
    external_system VARCHAR2(64),
    request_payload CLOB,
    response_payload CLOB,
    task_status VARCHAR2(32) NOT NULL,
    retry_count NUMBER(10) DEFAULT 0 NOT NULL,
    max_retry_count NUMBER(10) DEFAULT 3 NOT NULL,
    next_retry_at TIMESTAMP,
    last_attempt_at TIMESTAMP,
    last_error_code VARCHAR2(64),
    last_error_message VARCHAR2(1000),
    compensation_status VARCHAR2(32) DEFAULT 'NONE' NOT NULL,
    reconciliation_status VARCHAR2(32) DEFAULT 'PENDING' NOT NULL,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_integration_tasks PRIMARY KEY (id)
);

CREATE INDEX idx_integration_tasks_business ON integration_tasks (business_type, business_id);
CREATE INDEX idx_integration_tasks_status ON integration_tasks (task_status, next_retry_at);

CREATE TABLE historical_import_jobs (
    id VARCHAR2(64) NOT NULL,
    source_system VARCHAR2(64) NOT NULL,
    patient_id VARCHAR2(64),
    pathology_no VARCHAR2(64),
    application_no VARCHAR2(64),
    import_status VARCHAR2(32) NOT NULL,
    requested_by_user_id VARCHAR2(64),
    requested_by_name VARCHAR2(100),
    total_count NUMBER(10) DEFAULT 0 NOT NULL,
    success_count NUMBER(10) DEFAULT 0 NOT NULL,
    failure_count NUMBER(10) DEFAULT 0 NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    last_error_message VARCHAR2(1000),
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_historical_import_jobs PRIMARY KEY (id)
);

CREATE INDEX idx_historical_import_jobs_source_status ON historical_import_jobs (source_system, import_status);

CREATE TABLE historical_reports (
    id VARCHAR2(64) NOT NULL,
    import_job_id VARCHAR2(64),
    source_system VARCHAR2(64) NOT NULL,
    external_report_no VARCHAR2(64) NOT NULL,
    patient_id VARCHAR2(64),
    patient_name VARCHAR2(100),
    pathology_no VARCHAR2(64),
    application_no VARCHAR2(64),
    report_date TIMESTAMP,
    final_diagnosis VARCHAR2(1000),
    report_summary CLOB,
    raw_payload CLOB,
    source_department_name VARCHAR2(100),
    source_doctor_name VARCHAR2(100),
    attachment_url VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_historical_reports PRIMARY KEY (id),
    CONSTRAINT uk_historical_reports_source_report UNIQUE (source_system, external_report_no),
    CONSTRAINT fk_historical_reports_job FOREIGN KEY (import_job_id) REFERENCES historical_import_jobs (id)
);

CREATE INDEX idx_historical_reports_patient ON historical_reports (patient_id, report_date);
CREATE INDEX idx_historical_reports_pathology ON historical_reports (pathology_no, report_date);

CREATE TABLE historical_report_versions (
    id VARCHAR2(64) NOT NULL,
    historical_report_id VARCHAR2(64) NOT NULL,
    version_no NUMBER(10) NOT NULL,
    final_diagnosis VARCHAR2(1000),
    report_summary CLOB,
    raw_payload CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_historical_report_versions PRIMARY KEY (id),
    CONSTRAINT uk_historical_report_versions UNIQUE (historical_report_id, version_no),
    CONSTRAINT fk_historical_report_versions_report FOREIGN KEY (historical_report_id) REFERENCES historical_reports (id)
);

CREATE TABLE stat_indicator_definitions (
    id VARCHAR2(64) NOT NULL,
    indicator_code VARCHAR2(64) NOT NULL,
    indicator_name VARCHAR2(100) NOT NULL,
    indicator_category VARCHAR2(32) NOT NULL,
    metric_scope VARCHAR2(32),
    aggregation_type VARCHAR2(32),
    description VARCHAR2(500),
    sort_order NUMBER(10) DEFAULT 0 NOT NULL,
    enabled NUMBER(1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_stat_indicator_definitions PRIMARY KEY (id),
    CONSTRAINT uk_stat_indicator_definitions_code UNIQUE (indicator_code)
);

CREATE TABLE stat_report_templates (
    id VARCHAR2(64) NOT NULL,
    template_code VARCHAR2(64) NOT NULL,
    template_name VARCHAR2(100) NOT NULL,
    template_type VARCHAR2(32) NOT NULL,
    indicator_code VARCHAR2(64),
    default_columns CLOB,
    parameter_schema CLOB,
    sort_order NUMBER(10) DEFAULT 0 NOT NULL,
    enabled NUMBER(1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_stat_report_templates PRIMARY KEY (id),
    CONSTRAINT uk_stat_report_templates_code UNIQUE (template_code)
);

CREATE TABLE stat_export_jobs (
    id VARCHAR2(64) NOT NULL,
    export_no VARCHAR2(64) NOT NULL,
    template_id VARCHAR2(64),
    indicator_code VARCHAR2(64),
    export_status VARCHAR2(32) NOT NULL,
    filter_payload CLOB,
    file_name VARCHAR2(255),
    content_type VARCHAR2(100),
    requested_by_user_id VARCHAR2(64),
    requested_by_name VARCHAR2(100),
    error_message VARCHAR2(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT pk_stat_export_jobs PRIMARY KEY (id),
    CONSTRAINT uk_stat_export_jobs_export_no UNIQUE (export_no),
    CONSTRAINT fk_stat_export_jobs_template FOREIGN KEY (template_id) REFERENCES stat_report_templates (id)
);

-- =========================================================
-- 系统管理初始化权限数据
-- =========================================================
INSERT INTO departments (id, parent_id, department_code, department_name, department_type, sort_order, enabled, created_at, updated_at) VALUES
('DEPT_HOSPITAL', NULL, 'HOSPITAL', '示范医院', 'HOSPITAL', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('DEPT_PATHOLOGY', 'DEPT_HOSPITAL', 'PATHOLOGY', '病理科', 'DEPARTMENT', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('DEPT_PATHOLOGY_DIAG', 'DEPT_PATHOLOGY', 'PATHOLOGY_DIAG', '病理诊断组', 'GROUP', 11, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('DEPT_PATHOLOGY_TECH', 'DEPT_PATHOLOGY', 'PATHOLOGY_TECH', '病理技术组', 'GROUP', 12, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('DEPT_PATHOLOGY_ARCHIVE', 'DEPT_PATHOLOGY', 'PATHOLOGY_ARCHIVE', '病理档案组', 'GROUP', 13, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('DEPT_PATHOLOGY_SUPPORT', 'DEPT_PATHOLOGY', 'PATHOLOGY_SUPPORT', '病理运营支持组', 'GROUP', 14, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('DEPT_SURGERY', 'DEPT_HOSPITAL', 'SURGERY', '普通外科', 'DEPARTMENT', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO roles (id, role_code, role_name, role_type, data_scope, remarks, enabled, created_at, updated_at) VALUES
('ROLE_PATHOLOGY_ADMIN', 'PATHOLOGY_ADMIN', '病理管理员', 'BUSINESS', 'ALL', '病理系统全局管理角色', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_PATHOLOGY_DOCTOR', 'PATHOLOGY_DOCTOR', '病理医生', 'BUSINESS', 'DEPARTMENT', '病理诊断医生角色', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_PATHOLOGY_TECHNICIAN', 'PATHOLOGY_TECHNICIAN', '病理技师', 'BUSINESS', 'DEPARTMENT', '病理技术人员角色', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_ARCHIVE_MANAGER', 'ARCHIVE_MANAGER', '归档管理员', 'BUSINESS', 'DEPARTMENT', '病理归档与借阅管理角色', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_REAGENT_DEVICE_MANAGER', 'REAGENT_DEVICE_MANAGER', '试剂设备管理员', 'BUSINESS', 'DEPARTMENT', '病理试剂与设备管理角色', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_QUALITY_MANAGER', 'QUALITY_MANAGER', '质控管理员', 'BUSINESS', 'ALL', '病理质控管理角色', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M2_CLINICAL_REGISTER', 'M2_CLINICAL_REGISTER', '标本登记岗', 'BUSINESS', 'DEPARTMENT', 'M2 标本登记工作岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M2_FIXATION_VERIFY', 'M2_FIXATION_VERIFY', '固定核验岗', 'BUSINESS', 'DEPARTMENT', 'M2 固定核验工作岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M2_TRANSPORT_HANDOVER', 'M2_TRANSPORT_HANDOVER', '转运交接岗', 'BUSINESS', 'DEPARTMENT', 'M2 转运交接工作岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M2_SPECIMEN_RECEIVE', 'M2_SPECIMEN_RECEIVE', '标本签收岗', 'BUSINESS', 'DEPARTMENT', 'M2 标本签收工作岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M2_TRACKING_QUERY', 'M2_TRACKING_QUERY', '流程追踪岗', 'BUSINESS', 'DEPARTMENT', 'M2 流程追踪工作岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M2_CLINICAL_IMPORT', 'M2_CLINICAL_IMPORT', '申请导入岗', 'BUSINESS', 'DEPARTMENT', 'M2 临床申请导入岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M3_GROSSING', 'M3_GROSSING', '取材岗', 'BUSINESS', 'DEPARTMENT', 'M3 取材工作岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M3_DEHYDRATION', 'M3_DEHYDRATION', '脱水岗', 'BUSINESS', 'DEPARTMENT', 'M3 脱水工作岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M3_EMBEDDING', 'M3_EMBEDDING', '包埋岗', 'BUSINESS', 'DEPARTMENT', 'M3 包埋工作岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M3_SLICING', 'M3_SLICING', '切片岗', 'BUSINESS', 'DEPARTMENT', 'M3 切片工作岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M3_STAINING', 'M3_STAINING', '染色岗', 'BUSINESS', 'DEPARTMENT', 'M3 染色工作岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M3_REWORK', 'M3_REWORK', '返工岗', 'BUSINESS', 'DEPARTMENT', 'M3 返工工作岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M3_TRACKING', 'M3_TRACKING', '技术追踪岗', 'BUSINESS', 'DEPARTMENT', 'M3 技术追踪工作岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M4_ASSIGN', 'M4_ASSIGN', '诊断分配岗', 'BUSINESS', 'DEPARTMENT', 'M4 诊断任务分配岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M4_DIAGNOSIS', 'M4_DIAGNOSIS', '诊断岗', 'BUSINESS', 'DEPARTMENT', 'M4 诊断工作岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M4_REVIEW', 'M4_REVIEW', '复核岗', 'BUSINESS', 'DEPARTMENT', 'M4 复核工作岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M4_SIGN', 'M4_SIGN', '签发岗', 'BUSINESS', 'DEPARTMENT', 'M4 签发工作岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M4_TRACKING', 'M4_TRACKING', '报告追踪岗', 'BUSINESS', 'DEPARTMENT', 'M4 报告追踪工作岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ROLE_M4_MEDICAL_ORDER_EXECUTE', 'M4_MEDICAL_ORDER_EXECUTE', '医嘱执行岗', 'BUSINESS', 'DEPARTMENT', 'M4 病理医嘱执行岗', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO message_topics (id, topic_code, topic_name, topic_category, description, enabled, created_at, updated_at) VALUES
('TOPIC_CRITICAL_VALUE', 'CRITICAL_VALUE', '危急值通知', 'QUALITY', '危急值上报与处置通知', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TOPIC_REPORT_REVISION', 'REPORT_REVISION', '报告修订通知', 'REPORT', '报告修订申请与审批通知', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TOPIC_CONSULTATION', 'CONSULTATION', '会诊通知', 'REPORT', '病理会诊参与与完成通知', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TOPIC_QC_WARNING', 'QC_WARNING', '质控预警', 'QUALITY', '质控超时和异常预警', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO stat_categories (id, stat_code, stat_name, stat_scope, description, enabled, created_at, updated_at) VALUES
('STAT_OPERATION', 'OPERATION_STAT', '运营统计', 'ALL', '运营、收费、物资和绩效统计', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('STAT_WORKLOAD', 'WORKLOAD_STAT', '工作量统计', 'DEPARTMENT', '人员与岗位工作量统计', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('STAT_QUALITY', 'QUALITY_STAT', '质控统计', 'ALL', '病理质控与评审指标统计', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO menus (id, parent_id, menu_code, menu_name, menu_type, path, component_name, icon, permission_prefix, sort_order, visible, enabled, created_at, updated_at) VALUES
('MENU_SYSTEM', NULL, 'SYSTEM', '系统管理', 'DIRECTORY', '/system', 'SystemRoot', 'setting', 'sys', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_SYS_USERS', 'MENU_SYSTEM', 'SYS_USERS', '系统用户', 'MENU', '/system/users', 'SystemUsers', 'user', 'sys:user', 10, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_SYS_ROLES', 'MENU_SYSTEM', 'SYS_ROLES', '角色授权', 'MENU', '/system/roles', 'Roles', 'peoples', 'sys:role', 20, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_BODY_PARTS', 'MENU_SYSTEM', 'BODY_PARTS', '部位字典', 'MENU', '/system/body-parts', 'BodyParts', 'list', 'md:body-part', 30, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_ORDER_DICTS', 'MENU_SYSTEM', 'ORDER_DICTS', '医嘱字典', 'MENU', '/system/medical-order-dicts', 'MedicalOrderDicts', 'clipboard', 'md:order-dict', 40, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_ORDER_CHARGES', 'MENU_SYSTEM', 'ORDER_CHARGES', '医嘱收费', 'MENU', '/system/medical-order-charges', 'MedicalOrderCharges', 'money', 'md:order-charge', 50, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_ORDER_PACKAGES', 'MENU_SYSTEM', 'ORDER_PACKAGES', '医嘱套餐', 'MENU', '/system/medical-order-packages', 'MedicalOrderPackages', 'boxes', 'md:order-package', 60, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_TEMPLATES', 'MENU_SYSTEM', 'SAMPLING_TEMPLATES', '描写模板', 'MENU', '/system/sampling-templates', 'SamplingTemplates', 'edit', 'md:template', 70, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_GUIDELINES', 'MENU_SYSTEM', 'SAMPLING_GUIDELINES', '取材规范', 'MENU', '/system/sampling-guidelines', 'SamplingGuidelines', 'book-open', 'md:guideline', 80, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_CONFIGS', 'MENU_SYSTEM', 'SYSTEM_CONFIGS', '系统配置', 'MENU', '/system/configs', 'SystemConfigs', 'tool', 'sys:config', 90, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_NUMBERING', 'MENU_SYSTEM', 'NUMBERING_RULES', '编号规则', 'MENU', '/system/numbering-rules', 'NumberingRules', 'hash', 'support:numbering', 100, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M2_WORKFLOW', NULL, 'M2_WORKFLOW', '标本工作流', 'DIRECTORY', '/workflow', 'WorkflowRoot', NULL, 'm2', 110, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M2_CLINICAL', 'MENU_M2_WORKFLOW', 'M2_CLINICAL', '标本登记', 'MENU', '/api/v1/specimens/register', 'ClinicalRegister', NULL, 'm2:clinical', 111, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M2_FIXATION', 'MENU_M2_WORKFLOW', 'M2_FIXATION', '固定核验', 'MENU', '/api/v1/specimen-fixations', 'FixationVerify', NULL, 'm2:fixation', 112, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M2_TRANSPORT', 'MENU_M2_WORKFLOW', 'M2_TRANSPORT', '转运交接', 'MENU', '/api/v1/transport-orders', 'TransportHandover', NULL, 'm2:transport', 113, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M2_RECEIPT', 'MENU_M2_WORKFLOW', 'M2_RECEIPT', '标本签收', 'MENU', '/api/v1/specimen-receipts', 'SpecimenReceipt', NULL, 'm2:receipt', 114, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M2_TRACKING', 'MENU_M2_WORKFLOW', 'M2_TRACKING', '流程追踪', 'MENU', '/api/v1/applications/{id}/tracking', 'TrackingQuery', NULL, 'm2:tracking', 115, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M3_WORKFLOW', NULL, 'M3_WORKFLOW', '技术工作流', 'DIRECTORY', '/technical-workflow', 'TechnicalWorkflowRoot', NULL, 'm3', 120, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M3_GROSSING', 'MENU_M3_WORKFLOW', 'M3_GROSSING', '取材', 'MENU', '/api/v1/grossings', 'Grossing', NULL, 'm3:grossing', 121, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M3_DEHYDRATION', 'MENU_M3_WORKFLOW', 'M3_DEHYDRATION', '脱水', 'MENU', '/api/v1/dehydration-batches', 'Dehydration', NULL, 'm3:dehydration', 122, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M3_EMBEDDING', 'MENU_M3_WORKFLOW', 'M3_EMBEDDING', '包埋', 'MENU', '/api/v1/embeddings', 'Embedding', NULL, 'm3:embedding', 123, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M3_SLICING', 'MENU_M3_WORKFLOW', 'M3_SLICING', '切片', 'MENU', '/api/v1/slicings', 'Slicing', NULL, 'm3:slicing', 124, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M3_STAINING', 'MENU_M3_WORKFLOW', 'M3_STAINING', '染色', 'MENU', '/api/v1/slide-stainings', 'Staining', NULL, 'm3:staining', 125, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M3_REWORK', 'MENU_M3_WORKFLOW', 'M3_REWORK', '返工', 'MENU', '/api/v1/rework-orders', 'Rework', NULL, 'm3:rework', 126, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M3_TRACKING', 'MENU_M3_WORKFLOW', 'M3_TRACKING', '技术追踪', 'MENU', '/api/v1/pathology-cases/{id}/technical-tracking', 'TechnicalTracking', NULL, 'm3:tracking', 127, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M3_TASKS', 'MENU_M3_WORKFLOW', 'M3_TASKS', '技术任务', 'MENU', '/api/v1/technical-tasks/pending', 'TechnicalTasks', NULL, 'm3:tasks', 128, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M4_WORKFLOW', NULL, 'M4_WORKFLOW', '医生工作流', 'DIRECTORY', '/doctor-workflow', 'DoctorWorkflowRoot', NULL, 'm4', 130, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M4_ASSIGN', 'MENU_M4_WORKFLOW', 'M4_ASSIGN', '诊断分配', 'MENU', '/api/v1/diagnostic-tasks/pending', 'DiagnosisAssignment', NULL, 'm4:assign', 131, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M4_WORKBENCH', 'MENU_M4_WORKFLOW', 'M4_WORKBENCH', '诊断工作台', 'MENU', '/api/v1/pathology-cases/{id}/diagnostic-workbench', 'DiagnosisWorkbench', NULL, 'm4:workbench', 132, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M4_REPORT', 'MENU_M4_WORKFLOW', 'M4_REPORT', '病理报告', 'MENU', '/api/v1/pathology-reports', 'PathologyReport', NULL, 'm4:report', 133, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M4_TRACKING', 'MENU_M4_WORKFLOW', 'M4_TRACKING', '报告追踪', 'MENU', '/api/v1/pathology-cases/{id}/report-tracking', 'ReportTracking', NULL, 'm4:tracking', 134, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M4_REVISION', 'MENU_M4_WORKFLOW', 'M4_REVISION', '报告修订', 'MENU', '/api/v1/report-revision-requests', 'ReportRevision', NULL, 'm4:revision', 135, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M4_MEDICAL_ORDER', 'MENU_M4_WORKFLOW', 'M4_MEDICAL_ORDER', '病理医嘱', 'MENU', '/api/v1/medical-orders/pending', 'MedicalOrder', NULL, 'm4:order', 136, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M4_CONSULTATION', 'MENU_M4_WORKFLOW', 'M4_CONSULTATION', '会诊管理', 'MENU', '/api/v1/consultations', 'Consultation', NULL, 'm4:consultation', 137, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M5_SUPPORT', NULL, 'M5_SUPPORT', '运营支持', 'DIRECTORY', '/operation-support', 'OperationSupportRoot', NULL, 'm5', 160, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M5_ARCHIVE', 'MENU_M5_SUPPORT', 'M5_ARCHIVE', '归档管理', 'MENU', '/api/v1/archive-records/search', 'ArchiveManagement', NULL, 'm5:archive', 161, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M5_REAGENT', 'MENU_M5_SUPPORT', 'M5_REAGENT', '试剂台账', 'MENU', '/api/v1/reagents', 'ReagentLedger', NULL, 'm5:reagent', 162, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M5_EQUIPMENT', 'MENU_M5_SUPPORT', 'M5_EQUIPMENT', '设备台账', 'MENU', '/api/v1/equipment-records', 'EquipmentLedger', NULL, 'm5:equipment', 163, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M6_SUPPORT', NULL, 'M6_SUPPORT', '集成与统计', 'DIRECTORY', '/m6', 'M6Root', NULL, 'm6', 190, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M6_INTEGRATION', 'MENU_M6_SUPPORT', 'M6_INTEGRATION', '集成任务', 'MENU', '/api/v1/integration-tasks', 'IntegrationManagement', NULL, 'm6:integration', 191, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M6_BILLING', 'MENU_M6_SUPPORT', 'M6_BILLING', '收费管理', 'MENU', '/api/v1/billing-records', 'BillingManagement', NULL, 'm6:billing', 192, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M6_HISTORY', 'MENU_M6_SUPPORT', 'M6_HISTORY', '历史报告', 'MENU', '/api/v1/historical-reports', 'HistoricalReports', NULL, 'm6:history', 193, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MENU_M6_STAT', 'MENU_M6_SUPPORT', 'M6_STAT', '统计分析', 'MENU', '/api/v1/stat-indicators', 'StatisticsAnalysis', NULL, 'm6:stat', 194, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order, enabled, created_at, updated_at) VALUES
('PERM_SYS_USER_QUERY', 'PERM_SYS_USER_QUERY', '查询系统用户', 'MENU_SYS_USERS', 'QUERY', 'GET', '/api/v1/system-users', 'SYSTEM', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_USER_CREATE', 'PERM_SYS_USER_CREATE', '维护系统用户', 'MENU_SYS_USERS', 'CREATE', 'POST', '/api/v1/system-users', 'SYSTEM', 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_USER_UPDATE', 'PERM_SYS_USER_UPDATE', '更新系统用户', 'MENU_SYS_USERS', 'UPDATE', 'PATCH', '/api/v1/system-users', 'SYSTEM', 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_ROLE_QUERY', 'PERM_SYS_ROLE_QUERY', '查询角色授权', 'MENU_SYS_ROLES', 'QUERY', 'GET', '/api/v1/roles', 'SYSTEM', 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_ROLE_CREATE', 'PERM_SYS_ROLE_CREATE', '维护角色', 'MENU_SYS_ROLES', 'CREATE', 'POST', '/api/v1/roles', 'SYSTEM', 5, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_ROLE_ASSIGN', 'PERM_SYS_ROLE_ASSIGN', '角色授权', 'MENU_SYS_ROLES', 'ASSIGN', 'PUT', '/api/v1/roles/{id}/authorizations', 'SYSTEM', 6, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_BODY_PART_QUERY', 'PERM_SYS_BODY_PART_QUERY', '查询部位字典', 'MENU_BODY_PARTS', 'QUERY', 'GET', '/api/v1/body-parts', 'MASTERDATA', 7, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_BODY_PART_CREATE', 'PERM_SYS_BODY_PART_CREATE', '维护部位字典', 'MENU_BODY_PARTS', 'CREATE', 'POST', '/api/v1/body-parts', 'MASTERDATA', 8, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_ORDER_DICT_QUERY', 'PERM_SYS_ORDER_DICT_QUERY', '查询医嘱字典', 'MENU_ORDER_DICTS', 'QUERY', 'GET', '/api/v1/medical-order-dicts', 'MASTERDATA', 9, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_ORDER_DICT_CREATE', 'PERM_SYS_ORDER_DICT_CREATE', '维护医嘱字典', 'MENU_ORDER_DICTS', 'CREATE', 'POST', '/api/v1/medical-order-dicts', 'MASTERDATA', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_ORDER_CHARGE_QUERY', 'PERM_SYS_ORDER_CHARGE_QUERY', '查询医嘱收费', 'MENU_ORDER_CHARGES', 'QUERY', 'GET', '/api/v1/medical-order-charge-items', 'MASTERDATA', 11, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_ORDER_CHARGE_CREATE', 'PERM_SYS_ORDER_CHARGE_CREATE', '维护医嘱收费', 'MENU_ORDER_CHARGES', 'CREATE', 'POST', '/api/v1/medical-order-charge-items', 'MASTERDATA', 12, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_PACKAGE_QUERY', 'PERM_SYS_PACKAGE_QUERY', '查询医嘱套餐', 'MENU_ORDER_PACKAGES', 'QUERY', 'GET', '/api/v1/medical-order-packages', 'MASTERDATA', 13, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_PACKAGE_CREATE', 'PERM_SYS_PACKAGE_CREATE', '维护医嘱套餐', 'MENU_ORDER_PACKAGES', 'CREATE', 'POST', '/api/v1/medical-order-packages', 'MASTERDATA', 14, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_TEMPLATE_QUERY', 'PERM_SYS_TEMPLATE_QUERY', '查询描写模板', 'MENU_TEMPLATES', 'QUERY', 'GET', '/api/v1/sampling-templates', 'MASTERDATA', 15, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_TEMPLATE_CREATE', 'PERM_SYS_TEMPLATE_CREATE', '维护描写模板', 'MENU_TEMPLATES', 'CREATE', 'POST', '/api/v1/sampling-templates', 'MASTERDATA', 16, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_GUIDELINE_QUERY', 'PERM_SYS_GUIDELINE_QUERY', '查询取材规范', 'MENU_GUIDELINES', 'QUERY', 'GET', '/api/v1/sampling-guidelines', 'MASTERDATA', 17, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_GUIDELINE_CREATE', 'PERM_SYS_GUIDELINE_CREATE', '维护取材规范', 'MENU_GUIDELINES', 'CREATE', 'POST', '/api/v1/sampling-guidelines', 'MASTERDATA', 18, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_CONFIG_QUERY', 'PERM_SYS_CONFIG_QUERY', '查询系统配置', 'MENU_CONFIGS', 'QUERY', 'GET', '/api/v1/system-configs', 'SYSTEM', 19, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_CONFIG_UPDATE', 'PERM_SYS_CONFIG_UPDATE', '维护系统配置', 'MENU_CONFIGS', 'UPDATE', 'PATCH', '/api/v1/system-configs', 'SYSTEM', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_NUMBERING_QUERY', 'PERM_SYS_NUMBERING_QUERY', '查询编号规则', 'MENU_NUMBERING', 'QUERY', 'GET', '/api/v1/numbering-rules', 'SYSTEM', 21, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SYS_NUMBERING_UPDATE', 'PERM_SYS_NUMBERING_UPDATE', '维护编号规则', 'MENU_NUMBERING', 'UPDATE', 'PATCH', '/api/v1/numbering-rules', 'SYSTEM', 22, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SPECIMEN_REGISTER', 'PERM_SPECIMEN_REGISTER', '执行标本登记', 'MENU_M2_CLINICAL', 'REGISTER', 'POST', '/api/v1/specimens/register', 'M2', 110, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_FIXATION_VERIFY', 'PERM_FIXATION_VERIFY', '执行固定核验', 'MENU_M2_FIXATION', 'VERIFY', 'POST', '/api/v1/specimen-fixations', 'M2', 111, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_TRANSPORT_HANDOVER', 'PERM_TRANSPORT_HANDOVER', '执行转运交接', 'MENU_M2_TRANSPORT', 'HANDOVER', 'POST', '/api/v1/transport-orders', 'M2', 112, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SPECIMEN_RECEIVE', 'PERM_SPECIMEN_RECEIVE', '执行标本签收', 'MENU_M2_RECEIPT', 'RECEIVE', 'POST', '/api/v1/specimen-receipts', 'M2', 113, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_SPECIMEN_TRACKING_QUERY', 'PERM_SPECIMEN_TRACKING_QUERY', '查询流程追踪', 'MENU_M2_TRACKING', 'QUERY', 'GET', '/api/v1/applications/{id}/tracking', 'M2', 114, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_CLINICAL_IMPORT', 'PERM_CLINICAL_IMPORT', '导入临床申请', 'MENU_M2_CLINICAL', 'IMPORT', 'POST', '/api/v1/clinical-applications/import', 'M2', 115, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_APPLICATION_CREATE', 'PERM_APPLICATION_CREATE', '创建申请单', 'MENU_M2_CLINICAL', 'CREATE', 'POST', '/api/v1/applications', 'M2', 116, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_APPLICATION_DETAIL_QUERY', 'PERM_APPLICATION_DETAIL_QUERY', '查询申请单详情', 'MENU_M2_TRACKING', 'DETAIL_QUERY', 'GET', '/api/v1/applications/{id}', 'M2', 117, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M3_GROSSING', 'PERM_M3_GROSSING', '执行取材', 'MENU_M3_GROSSING', 'OPERATE', 'POST', '/api/v1/grossings', 'M3', 121, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M3_DEHYDRATION', 'PERM_M3_DEHYDRATION', '执行脱水', 'MENU_M3_DEHYDRATION', 'OPERATE', 'POST', '/api/v1/dehydration-batches', 'M3', 122, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M3_EMBEDDING', 'PERM_M3_EMBEDDING', '执行包埋', 'MENU_M3_EMBEDDING', 'OPERATE', 'POST', '/api/v1/embeddings', 'M3', 123, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M3_SLICING', 'PERM_M3_SLICING', '执行切片', 'MENU_M3_SLICING', 'OPERATE', 'POST', '/api/v1/slicings', 'M3', 124, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M3_STAINING', 'PERM_M3_STAINING', '执行染色', 'MENU_M3_STAINING', 'OPERATE', 'POST', '/api/v1/slide-stainings', 'M3', 125, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M3_REWORK', 'PERM_M3_REWORK', '执行返工', 'MENU_M3_REWORK', 'OPERATE', 'POST', '/api/v1/rework-orders', 'M3', 126, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M3_TECH_TRACKING_QUERY', 'PERM_M3_TECH_TRACKING_QUERY', '查询技术追踪', 'MENU_M3_TRACKING', 'QUERY', 'GET', '/api/v1/pathology-cases/{id}/technical-tracking', 'M3', 127, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M3_TECH_TASK_QUERY', 'PERM_M3_TECH_TASK_QUERY', '查询技术任务', 'MENU_M3_TASKS', 'QUERY', 'GET', '/api/v1/technical-tasks/pending', 'M3', 128, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_DIAG_TASK_QUERY', 'PERM_M4_DIAG_TASK_QUERY', '查询诊断任务', 'MENU_M4_ASSIGN', 'QUERY', 'GET', '/api/v1/diagnostic-tasks/pending', 'M4', 131, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_ASSIGN', 'PERM_M4_ASSIGN', '执行诊断分配', 'MENU_M4_ASSIGN', 'ASSIGN', 'POST', '/api/v1/diagnostic-tasks/{id}/assign', 'M4', 132, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_ACCEPT', 'PERM_M4_ACCEPT', '签收诊断任务', 'MENU_M4_WORKBENCH', 'ACCEPT', 'POST', '/api/v1/diagnostic-tasks/{id}/accept', 'M4', 133, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_START', 'PERM_M4_START', '开始诊断任务', 'MENU_M4_WORKBENCH', 'START', 'POST', '/api/v1/diagnostic-tasks/{id}/start', 'M4', 134, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_WORKBENCH_QUERY', 'PERM_M4_WORKBENCH_QUERY', '查询诊断工作台', 'MENU_M4_WORKBENCH', 'QUERY', 'GET', '/api/v1/pathology-cases/{id}/diagnostic-workbench', 'M4', 135, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_REPORT_CREATE', 'PERM_M4_REPORT_CREATE', '创建病理报告', 'MENU_M4_REPORT', 'CREATE', 'POST', '/api/v1/pathology-reports', 'M4', 136, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_REPORT_SUBMIT', 'PERM_M4_REPORT_SUBMIT', '提交病理报告', 'MENU_M4_REPORT', 'SUBMIT', 'POST', '/api/v1/pathology-reports/{id}/submit', 'M4', 137, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_REPORT_REVIEW', 'PERM_M4_REPORT_REVIEW', '复核病理报告', 'MENU_M4_REPORT', 'REVIEW', 'POST', '/api/v1/pathology-reports/{id}/review', 'M4', 138, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_REPORT_SIGN', 'PERM_M4_REPORT_SIGN', '签发病理报告', 'MENU_M4_REPORT', 'SIGN', 'POST', '/api/v1/pathology-reports/{id}/sign', 'M4', 139, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_REPORT_PUBLISH', 'PERM_M4_REPORT_PUBLISH', '发布病理报告', 'MENU_M4_REPORT', 'PUBLISH', 'POST', '/api/v1/pathology-reports/{id}/publish', 'M4', 140, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_REPORT_TRACKING_QUERY', 'PERM_M4_REPORT_TRACKING_QUERY', '查询报告追踪', 'MENU_M4_TRACKING', 'QUERY', 'GET', '/api/v1/pathology-cases/{id}/report-tracking', 'M4', 141, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_REVISION_REQUEST_CREATE', 'PERM_M4_REVISION_REQUEST_CREATE', '发起报告修订', 'MENU_M4_REVISION', 'CREATE', 'POST', '/api/v1/report-revision-requests', 'M4', 142, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_REVISION_APPROVE', 'PERM_M4_REVISION_APPROVE', '审批报告修订', 'MENU_M4_REVISION', 'APPROVE', 'POST', '/api/v1/report-revision-requests/{id}/approve', 'M4', 143, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_MEDICAL_ORDER_CREATE', 'PERM_M4_MEDICAL_ORDER_CREATE', '开立病理医嘱', 'MENU_M4_MEDICAL_ORDER', 'CREATE', 'POST', '/api/v1/medical-orders', 'M4', 144, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_MEDICAL_ORDER_CANCEL', 'PERM_M4_MEDICAL_ORDER_CANCEL', '取消病理医嘱', 'MENU_M4_MEDICAL_ORDER', 'CANCEL', 'POST', '/api/v1/medical-orders/{id}/cancel', 'M4', 145, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_MEDICAL_ORDER_QUERY', 'PERM_M4_MEDICAL_ORDER_QUERY', '查询病理医嘱', 'MENU_M4_MEDICAL_ORDER', 'QUERY', 'GET', '/api/v1/medical-orders/pending', 'M4', 146, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_MEDICAL_ORDER_ACCEPT', 'PERM_M4_MEDICAL_ORDER_ACCEPT', '签收病理医嘱', 'MENU_M4_MEDICAL_ORDER', 'ACCEPT', 'POST', '/api/v1/medical-orders/{id}/accept', 'M4', 147, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_MEDICAL_ORDER_COMPLETE', 'PERM_M4_MEDICAL_ORDER_COMPLETE', '完成病理医嘱', 'MENU_M4_MEDICAL_ORDER', 'COMPLETE', 'POST', '/api/v1/medical-orders/{id}/complete', 'M4', 148, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_CONSULTATION_CREATE', 'PERM_M4_CONSULTATION_CREATE', '发起会诊', 'MENU_M4_CONSULTATION', 'CREATE', 'POST', '/api/v1/consultations', 'M4', 149, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_CONSULTATION_COMMENT', 'PERM_M4_CONSULTATION_COMMENT', '填写会诊意见', 'MENU_M4_CONSULTATION', 'COMMENT', 'POST', '/api/v1/consultations/{id}/participants/{participantId}/comment', 'M4', 150, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M4_CONSULTATION_COMPLETE', 'PERM_M4_CONSULTATION_COMPLETE', '完成会诊', 'MENU_M4_CONSULTATION', 'COMPLETE', 'POST', '/api/v1/consultations/{id}/complete', 'M4', 151, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_ARCHIVE_CABINET_QUERY', 'PERM_M5_ARCHIVE_CABINET_QUERY', '查询档案柜', 'MENU_M5_ARCHIVE', 'ARCHIVE_CABINET_QUERY', 'GET', '/api/v1/archive-cabinets', 'M5', 161, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_ARCHIVE_CABINET_CREATE', 'PERM_M5_ARCHIVE_CABINET_CREATE', '维护档案柜', 'MENU_M5_ARCHIVE', 'ARCHIVE_CABINET_CREATE', 'POST', '/api/v1/archive-cabinets', 'M5', 162, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_ARCHIVE_CABINET_UPDATE', 'PERM_M5_ARCHIVE_CABINET_UPDATE', '更新档案柜', 'MENU_M5_ARCHIVE', 'ARCHIVE_CABINET_UPDATE', 'PATCH', '/api/v1/archive-cabinets/{id}', 'M5', 163, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_APPLICATION_FORM_ARCHIVE', 'PERM_M5_APPLICATION_FORM_ARCHIVE', '归档申请单', 'MENU_M5_ARCHIVE', 'APPLICATION_FORM_ARCHIVE', 'POST', '/api/v1/archive/application-forms', 'M5', 164, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_EMBEDDING_BOX_ARCHIVE', 'PERM_M5_EMBEDDING_BOX_ARCHIVE', '归档包埋盒', 'MENU_M5_ARCHIVE', 'EMBEDDING_BOX_ARCHIVE', 'POST', '/api/v1/archive/embedding-boxes', 'M5', 165, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_SLIDE_ARCHIVE', 'PERM_M5_SLIDE_ARCHIVE', '归档玻片', 'MENU_M5_ARCHIVE', 'SLIDE_ARCHIVE', 'POST', '/api/v1/archive/slides', 'M5', 166, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_ARCHIVE_QUERY', 'PERM_M5_ARCHIVE_QUERY', '查询归档记录', 'MENU_M5_ARCHIVE', 'ARCHIVE_RECORD_QUERY', 'GET', '/api/v1/archive-records/search', 'M5', 167, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_LOAN_CREATE', 'PERM_M5_LOAN_CREATE', '发起借阅', 'MENU_M5_ARCHIVE', 'LOAN_CREATE', 'POST', '/api/v1/material-loans', 'M5', 168, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_LOAN_RETURN', 'PERM_M5_LOAN_RETURN', '归还借阅物', 'MENU_M5_ARCHIVE', 'LOAN_RETURN', 'POST', '/api/v1/material-loans/{id}/return', 'M5', 169, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_LOAN_QUERY', 'PERM_M5_LOAN_QUERY', '查询借阅记录', 'MENU_M5_ARCHIVE', 'LOAN_QUERY', 'GET', '/api/v1/material-loans/pending', 'M5', 170, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_REAGENT_QUERY', 'PERM_M5_REAGENT_QUERY', '查询试剂台账', 'MENU_M5_REAGENT', 'REAGENT_QUERY', 'GET', '/api/v1/reagents', 'M5', 171, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_REAGENT_CREATE', 'PERM_M5_REAGENT_CREATE', '维护试剂台账', 'MENU_M5_REAGENT', 'REAGENT_CREATE', 'POST', '/api/v1/reagents', 'M5', 172, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_REAGENT_UPDATE', 'PERM_M5_REAGENT_UPDATE', '更新试剂台账', 'MENU_M5_REAGENT', 'REAGENT_UPDATE', 'PATCH', '/api/v1/reagents/{id}', 'M5', 173, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_REAGENT_STOCK_QUERY', 'PERM_M5_REAGENT_STOCK_QUERY', '查询试剂库存', 'MENU_M5_REAGENT', 'REAGENT_STOCK_QUERY', 'GET', '/api/v1/reagent-stocks', 'M5', 174, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_REAGENT_STOCK_UPDATE', 'PERM_M5_REAGENT_STOCK_UPDATE', '更新试剂库存', 'MENU_M5_REAGENT', 'REAGENT_STOCK_UPDATE', 'PATCH', '/api/v1/reagent-stocks/{id}', 'M5', 175, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_REAGENT_WARNING_QUERY', 'PERM_M5_REAGENT_WARNING_QUERY', '查询试剂预警', 'MENU_M5_REAGENT', 'REAGENT_WARNING_QUERY', 'GET', '/api/v1/reagent-stocks/warnings', 'M5', 176, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_EQUIPMENT_QUERY', 'PERM_M5_EQUIPMENT_QUERY', '查询设备台账', 'MENU_M5_EQUIPMENT', 'EQUIPMENT_QUERY', 'GET', '/api/v1/equipment-records', 'M5', 177, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_EQUIPMENT_CREATE', 'PERM_M5_EQUIPMENT_CREATE', '维护设备台账', 'MENU_M5_EQUIPMENT', 'EQUIPMENT_CREATE', 'POST', '/api/v1/equipment-records', 'M5', 178, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_EQUIPMENT_UPDATE', 'PERM_M5_EQUIPMENT_UPDATE', '更新设备台账', 'MENU_M5_EQUIPMENT', 'EQUIPMENT_UPDATE', 'PATCH', '/api/v1/equipment-records/{id}', 'M5', 179, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_EQUIPMENT_MAINTENANCE_CREATE', 'PERM_M5_EQUIPMENT_MAINTENANCE_CREATE', '登记设备维护', 'MENU_M5_EQUIPMENT', 'EQUIPMENT_MAINTENANCE_CREATE', 'POST', '/api/v1/equipment-records/{id}/maintenance-logs', 'M5', 180, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M5_EQUIPMENT_WARNING_QUERY', 'PERM_M5_EQUIPMENT_WARNING_QUERY', '查询设备预警', 'MENU_M5_EQUIPMENT', 'EQUIPMENT_WARNING_QUERY', 'GET', '/api/v1/equipment-records/warnings', 'M5', 181, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M6_INTEGRATION_TASK_QUERY', 'PERM_M6_INTEGRATION_TASK_QUERY', '查询集成任务', 'MENU_M6_INTEGRATION', 'INTEGRATION_TASK_QUERY', 'GET', '/api/v1/integration-tasks', 'M6', 191, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M6_BILLING_QUERY', 'PERM_M6_BILLING_QUERY', '查询收费记录', 'MENU_M6_BILLING', 'BILLING_QUERY', 'GET', '/api/v1/billing-records', 'M6', 192, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M6_BILLING_RECEIPT', 'PERM_M6_BILLING_RECEIPT', '回写收费回执', 'MENU_M6_BILLING', 'BILLING_RECEIPT', 'POST', '/api/v1/billing-records/{id}/receipt', 'M6', 193, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M6_BILLING_RETRY', 'PERM_M6_BILLING_RETRY', '重试收费任务', 'MENU_M6_BILLING', 'BILLING_RETRY', 'POST', '/api/v1/billing-records/{id}/retry', 'M6', 194, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M6_BILLING_RECONCILE', 'PERM_M6_BILLING_RECONCILE', '执行收费对账', 'MENU_M6_BILLING', 'BILLING_RECONCILE', 'POST', '/api/v1/billing-records/reconcile', 'M6', 195, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M6_HISTORY_IMPORT', 'PERM_M6_HISTORY_IMPORT', '导入历史报告', 'MENU_M6_HISTORY', 'HISTORY_IMPORT', 'POST', '/api/v1/historical-report-import-jobs', 'M6', 196, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M6_HISTORY_QUERY', 'PERM_M6_HISTORY_QUERY', '查询历史报告', 'MENU_M6_HISTORY', 'HISTORY_QUERY', 'GET', '/api/v1/historical-reports', 'M6', 197, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M6_STAT_INDICATOR_QUERY', 'PERM_M6_STAT_INDICATOR_QUERY', '查询统计指标', 'MENU_M6_STAT', 'STAT_INDICATOR_QUERY', 'GET', '/api/v1/stat-indicators', 'M6', 198, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M6_STAT_TEMPLATE_QUERY', 'PERM_M6_STAT_TEMPLATE_QUERY', '查询统计模板', 'MENU_M6_STAT', 'STAT_TEMPLATE_QUERY', 'GET', '/api/v1/stat-report-templates', 'M6', 199, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M6_STAT_REPORT_QUERY', 'PERM_M6_STAT_REPORT_QUERY', '查询统计报表', 'MENU_M6_STAT', 'STAT_REPORT_QUERY', 'POST', '/api/v1/stat-reports/query', 'M6', 200, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERM_M6_STAT_REPORT_EXPORT', 'PERM_M6_STAT_REPORT_EXPORT', '导出统计报表', 'MENU_M6_STAT', 'STAT_REPORT_EXPORT', 'POST', '/api/v1/stat-reports/export', 'M6', 201, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO body_part_dict (id, parent_id, part_code, part_name, part_alias, part_level, sort_order, enabled, created_at, updated_at) VALUES
('BP_ROOT', NULL, 'ROOT', '全部部位', NULL, 0, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BP_DIGESTIVE', 'BP_ROOT', 'DIGESTIVE', '消化系统', NULL, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BP_STOMACH', 'BP_DIGESTIVE', 'STOMACH', '胃', NULL, 2, 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BP_COLON', 'BP_DIGESTIVE', 'COLON', '结肠', NULL, 2, 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BP_RESPIRATORY', 'BP_ROOT', 'RESPIRATORY', '呼吸系统', NULL, 1, 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BP_LUNG', 'BP_RESPIRATORY', 'LUNG', '肺', NULL, 2, 30, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO medical_order_dict_categories (id, parent_id, category_code, category_name, sort_order, enabled, created_at, updated_at) VALUES
('ODC_ROOT', NULL, 'ROOT', '全部医嘱', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ODC_ROUTINE', 'ODC_ROOT', 'ROUTINE', '常规医嘱', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ODC_SPECIAL', 'ODC_ROOT', 'SPECIAL', '特检医嘱', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO medical_order_dict_items (id, category_id, order_item_code, order_item_name, order_type, default_content, execution_scope, sort_order, enabled, created_at, updated_at) VALUES
('ODI_HE', 'ODC_ROUTINE', 'HE', 'HE 染色', 'ROUTINE', 'HE 染色', 'TECHNICIAN', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ODI_IHC', 'ODC_SPECIAL', 'IHC', '免疫组化', 'SPECIAL', '免疫组化', 'TECHNICIAN', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ODI_FROZEN', 'ODC_SPECIAL', 'FROZEN', '术中冰冻', 'SPECIAL', '术中冰冻检查', 'DOCTOR', 30, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO medical_order_charge_items (id, order_dict_item_id, charge_item_code, charge_item_name, specification, unit, price, sort_order, enabled, created_at, updated_at) VALUES
('OCI_HE', 'ODI_HE', 'CHG_HE', 'HE 染色收费', '次', '次', 20.00, 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('OCI_IHC', 'ODI_IHC', 'CHG_IHC', '免疫组化收费', '次', '次', 120.00, 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('OCI_FROZEN', 'ODI_FROZEN', 'CHG_FROZEN', '术中冰冻收费', '次', '次', 260.00, 30, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO medical_order_packages (id, package_code, package_name, package_type, owner_user_id, enabled, remarks, created_at, updated_at) VALUES
('PKG_ROUTINE', 'PKG_ROUTINE', '常规病理基础套餐', 'PUBLIC', NULL, 1, '首次部署默认医嘱套餐', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO medical_order_package_items (id, package_id, order_item_id, sort_order, remarks, created_at) VALUES
('PKGI_ROUTINE_HE', 'PKG_ROUTINE', 'ODI_HE', 10, '常规染色', CURRENT_TIMESTAMP),
('PKGI_ROUTINE_IHC', 'PKG_ROUTINE', 'ODI_IHC', 20, '必要时追加免疫组化', CURRENT_TIMESTAMP);

INSERT INTO sampling_template_categories (id, parent_id, category_code, category_name, sort_order, enabled, created_at, updated_at) VALUES
('STC_ROOT', NULL, 'ROOT', '模板根目录', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('STC_ROUTINE', 'STC_ROOT', 'ROUTINE', '常规模板', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sampling_templates (id, category_id, template_code, template_name, template_content, split_part_count, applicable_specimen_type, enabled, created_at, updated_at) VALUES
('ST_HE_STOMACH', 'STC_ROUTINE', 'TPL_STOMACH', '胃活检模板', '胃黏膜灰白组织数枚，全部送检。', 1, 'ROUTINE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ST_HE_COLON', 'STC_ROUTINE', 'TPL_COLON', '结肠活检模板', '结肠黏膜组织数枚，全部送检。', 1, 'ROUTINE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sampling_template_site_rel (id, template_id, body_part_id, sort_order, created_at) VALUES
('STSR_STOMACH', 'ST_HE_STOMACH', 'BP_STOMACH', 1, CURRENT_TIMESTAMP),
('STSR_COLON', 'ST_HE_COLON', 'BP_COLON', 2, CURRENT_TIMESTAMP);

INSERT INTO sampling_guideline_categories (id, parent_id, category_code, category_name, sort_order, enabled, created_at, updated_at) VALUES
('SGC_ROOT', NULL, 'ROOT', '规范根目录', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SGC_ROUTINE', 'SGC_ROOT', 'ROUTINE', '常规规范', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sampling_guidelines (id, category_id, guideline_code, guideline_name, guideline_content, version_no, enabled, created_at, updated_at) VALUES
('SG_STOMACH', 'SGC_ROUTINE', 'GL_STOMACH', '胃活检取材规范', '按送检块数逐一包埋，记录部位、数量及病灶描述。', '1.0', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SG_COLON', 'SGC_ROUTINE', 'GL_COLON', '结肠活检取材规范', '按瓶逐一核对，保留送检方向信息并完整记录数量。', '1.0', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO system_config_categories (id, parent_id, category_code, category_name, category_type, sort_order, enabled, created_at, updated_at) VALUES
('SCC_ROOT', NULL, 'ROOT', '配置根目录', 'CONFIG', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SCC_GENERAL', 'SCC_ROOT', 'GENERAL', '通用配置', 'CONFIG', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SCC_ENUM', 'SCC_ROOT', 'ENUM', '系统枚举', 'ENUM', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO system_config_items (id, category_id, config_key, config_name, config_value, value_type, sort_order, enabled, remarks, created_at, updated_at) VALUES
('SCI_DEFAULT_SCOPE', 'SCC_GENERAL', 'system.defaultScope', '默认数据范围', 'DEPARTMENT', 'STRING', 10, 1, '默认按科室隔离数据权限', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SCI_TEMPLATE_MATCH', 'SCC_GENERAL', 'sampling.templateMatchEnabled', '模板智能匹配开关', 'true', 'BOOLEAN', 20, 1, '控制取材模板智能匹配', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SCI_M3_GROSSING_TIMEOUT', 'SCC_GENERAL', 'technical.timeout.grossingMinutes', '取材任务超时分钟数', '240', 'INTEGER', 30, 1, 'M3 取材任务超时阈值', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SCI_M3_DEHYDRATION_TIMEOUT', 'SCC_GENERAL', 'technical.timeout.dehydrationMinutes', '脱水任务超时分钟数', '720', 'INTEGER', 31, 1, 'M3 脱水任务超时阈值', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SCI_M3_STAINING_TIMEOUT', 'SCC_GENERAL', 'technical.timeout.stainingMinutes', '染色任务超时分钟数', '240', 'INTEGER', 32, 1, 'M3 染色任务超时阈值', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO numbering_rules (id, rule_code, biz_type, prefix_pattern, date_pattern, seq_length, reset_policy, scope_type, enabled, remarks, created_at, updated_at) VALUES
('NR_APPLICATION', 'RULE_APPLICATION_NO', 'APPLICATION_NO', 'AP', 'yyyyMMdd', 4, 'DAILY', 'GLOBAL', 1, '申请单号', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('NR_PATHOLOGY', 'RULE_PATHOLOGY_NO', 'PATHOLOGY_NO', 'BL', 'yyyyMMdd', 4, 'DAILY', 'GLOBAL', 1, '病理号', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('NR_SPECIMEN', 'RULE_SPECIMEN_NO', 'SPECIMEN_NO', 'SP', 'yyyyMMdd', 3, 'DAILY', 'CASE', 1, '标本号', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('NR_BLOCK', 'RULE_BLOCK_NO', 'BLOCK_NO', 'BK', 'yyyyMMdd', 3, 'DAILY', 'GLOBAL', 1, '蜡块号', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('NR_SLIDE', 'RULE_SLIDE_NO', 'SLIDE_NO', 'SL', 'yyyyMMdd', 3, 'DAILY', 'GLOBAL', 1, '玻片号', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('NR_TRANSPORT', 'RULE_TRANSPORT_ORDER_NO', 'TRANSPORT_ORDER_NO', 'TR', 'yyyyMMdd', 4, 'DAILY', 'GLOBAL', 1, '转运单号', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('NR_REPORT', 'RULE_REPORT_NO', 'REPORT_NO', 'RP', 'yyyyMMdd', 4, 'DAILY', 'GLOBAL', 1, '报告号', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO stat_indicator_definitions (id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type, description, sort_order, enabled, created_at, updated_at) VALUES
('SID_QC_01', 'QC_SPECIMEN_FIXATION_RATE', '标本固定合格率', 'QUALITY', 'DEPARTMENT', 'RATE', '病理质控核心指标', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SID_QC_02', 'QC_UNQUALIFIED_SPECIMEN_COUNT', '不合格标本数', 'QUALITY', 'DEPARTMENT', 'COUNT', '病理质控核心指标', 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SID_QC_03', 'QC_CLINICAL_MATCH_RATE', '临床病理符合率', 'QUALITY', 'DEPARTMENT', 'RATE', '病理质控核心指标', 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SID_QC_04', 'QC_FIRST_LINE_MATCH_RATE', '首诊符合率', 'QUALITY', 'DEPARTMENT', 'RATE', '病理质控核心指标', 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SID_QC_05', 'QC_FROZEN_PARAFFIN_MATCH_RATE', '冰冻石蜡符合率', 'QUALITY', 'DEPARTMENT', 'RATE', '病理质控核心指标', 5, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SID_QC_06', 'QC_CYTOLOGY_MATCH_RATE', '细胞学符合率', 'QUALITY', 'DEPARTMENT', 'RATE', '病理质控核心指标', 6, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SID_QC_07', 'QC_CONSULTATION_MATCH_RATE', '会诊符合率', 'QUALITY', 'DEPARTMENT', 'RATE', '病理质控核心指标', 7, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SID_QC_08', 'QC_CANCELLED_REVIEW_COUNT', '取消复检数', 'QUALITY', 'DEPARTMENT', 'COUNT', '病理质控核心指标', 8, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SID_QC_09', 'QC_TECHNICAL_QUALITY_COUNT', '技术质控异常数', 'QUALITY', 'DEPARTMENT', 'COUNT', '病理质控核心指标', 9, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SID_QC_10', 'QC_GROSSING_QUALITY_COUNT', '取材质控异常数', 'QUALITY', 'DEPARTMENT', 'COUNT', '病理质控核心指标', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SID_QC_11', 'QC_REPORT_RELEASE_DAYS', '报告发布天数', 'QUALITY', 'DEPARTMENT', 'AVG', '病理质控核心指标', 11, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SID_QC_12', 'QC_SPECIMEN_PROCESS_HOURS', '标本处理时长', 'QUALITY', 'DEPARTMENT', 'AVG', '病理质控核心指标', 12, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SID_QC_13', 'QC_DIAGNOSIS_TIMELINESS_RATE', '诊断及时率', 'QUALITY', 'DEPARTMENT', 'RATE', '病理质控核心指标', 13, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SID_OP_01', 'OP_CASE_VOLUME', '病例量', 'OPERATION', 'DEPARTMENT', 'COUNT', '运营类指标', 21, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SID_OP_02', 'OP_BILLING_AMOUNT', '收费金额', 'OPERATION', 'DEPARTMENT', 'SUM', '运营类指标', 22, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SID_OP_03', 'OP_REAGENT_STOCK_ALERT', '试剂预警数', 'OPERATION', 'DEPARTMENT', 'COUNT', '运营类指标', 23, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SID_OP_04', 'OP_PERFORMANCE_WORKLOAD', '绩效工作量', 'OPERATION', 'DEPARTMENT', 'COUNT', '运营类指标', 24, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SID_WL_01', 'WL_DIAGNOSTIC_TASK_COUNT', '诊断任务数', 'WORKLOAD', 'USER', 'COUNT', '工作量指标', 31, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SID_WL_02', 'WL_MEDICAL_ORDER_COUNT', '病理医嘱数', 'WORKLOAD', 'USER', 'COUNT', '工作量指标', 32, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO stat_report_templates (id, template_code, template_name, template_type, indicator_code, default_columns, parameter_schema, sort_order, enabled, created_at, updated_at) VALUES
('SRT_QC', 'TPL_QC_OVERVIEW', '质控总览模板', 'QUALITY', NULL, 'indicatorCode,indicatorName,metricValue,metricUnit', '{"from":"datetime","to":"datetime"}', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SRT_OP', 'TPL_OPERATION_OVERVIEW', '运营总览模板', 'OPERATION', NULL, 'indicatorCode,indicatorName,metricValue,metricUnit', '{"from":"datetime","to":"datetime"}', 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SRT_WL', 'TPL_WORKLOAD_OVERVIEW', '工作量总览模板', 'WORKLOAD', NULL, 'indicatorCode,indicatorName,metricValue,metricUnit', '{"from":"datetime","to":"datetime","operatorUserId":"string"}', 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SRT_CUSTOM', 'TPL_CUSTOM_BILLING', '收费专项模板', 'CUSTOM', 'OP_BILLING_AMOUNT', 'indicatorCode,indicatorName,metricValue,metricUnit', '{"from":"datetime","to":"datetime","templateCode":"string"}', 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_ADMIN_' || permission_code, 'ROLE_PATHOLOGY_ADMIN', id, CURRENT_TIMESTAMP
FROM permissions;

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_ARCHIVE_' || permission_code, 'ROLE_ARCHIVE_MANAGER', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN (
    'PERM_M5_ARCHIVE_CABINET_QUERY', 'PERM_M5_ARCHIVE_CABINET_CREATE', 'PERM_M5_ARCHIVE_CABINET_UPDATE',
    'PERM_M5_APPLICATION_FORM_ARCHIVE', 'PERM_M5_EMBEDDING_BOX_ARCHIVE', 'PERM_M5_SLIDE_ARCHIVE',
    'PERM_M5_ARCHIVE_QUERY', 'PERM_M5_LOAN_CREATE', 'PERM_M5_LOAN_RETURN', 'PERM_M5_LOAN_QUERY',
    'PERM_M6_HISTORY_IMPORT', 'PERM_M6_HISTORY_QUERY'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_REAGENT_' || permission_code, 'ROLE_REAGENT_DEVICE_MANAGER', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN (
    'PERM_M5_REAGENT_QUERY', 'PERM_M5_REAGENT_CREATE', 'PERM_M5_REAGENT_UPDATE',
    'PERM_M5_REAGENT_STOCK_QUERY', 'PERM_M5_REAGENT_STOCK_UPDATE', 'PERM_M5_REAGENT_WARNING_QUERY',
    'PERM_M5_EQUIPMENT_QUERY', 'PERM_M5_EQUIPMENT_CREATE', 'PERM_M5_EQUIPMENT_UPDATE',
    'PERM_M5_EQUIPMENT_MAINTENANCE_CREATE', 'PERM_M5_EQUIPMENT_WARNING_QUERY'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_QUALITY_' || permission_code, 'ROLE_QUALITY_MANAGER', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN (
    'PERM_M6_STAT_INDICATOR_QUERY', 'PERM_M6_STAT_TEMPLATE_QUERY',
    'PERM_M6_STAT_REPORT_QUERY', 'PERM_M6_STAT_REPORT_EXPORT'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M2_REGISTER_' || permission_code, 'ROLE_M2_CLINICAL_REGISTER', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN ('PERM_SPECIMEN_REGISTER', 'PERM_APPLICATION_CREATE');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M2_FIXATION_' || permission_code, 'ROLE_M2_FIXATION_VERIFY', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN ('PERM_FIXATION_VERIFY');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M2_TRANSPORT_' || permission_code, 'ROLE_M2_TRANSPORT_HANDOVER', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN ('PERM_TRANSPORT_HANDOVER');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M2_RECEIVE_' || permission_code, 'ROLE_M2_SPECIMEN_RECEIVE', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN ('PERM_SPECIMEN_RECEIVE');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M2_TRACKING_' || permission_code, 'ROLE_M2_TRACKING_QUERY', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN ('PERM_SPECIMEN_TRACKING_QUERY', 'PERM_APPLICATION_DETAIL_QUERY');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M2_IMPORT_' || permission_code, 'ROLE_M2_CLINICAL_IMPORT', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN ('PERM_CLINICAL_IMPORT');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M3_GROSSING_' || permission_code, 'ROLE_M3_GROSSING', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN ('PERM_M3_GROSSING', 'PERM_M3_TECH_TASK_QUERY', 'PERM_SYS_BODY_PART_QUERY', 'PERM_SYS_TEMPLATE_QUERY');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M3_DEHYDRATION_' || permission_code, 'ROLE_M3_DEHYDRATION', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN ('PERM_M3_DEHYDRATION', 'PERM_M3_TECH_TASK_QUERY');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M3_EMBEDDING_' || permission_code, 'ROLE_M3_EMBEDDING', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN ('PERM_M3_EMBEDDING', 'PERM_M3_TECH_TASK_QUERY');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M3_SLICING_' || permission_code, 'ROLE_M3_SLICING', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN ('PERM_M3_SLICING', 'PERM_M3_TECH_TASK_QUERY');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M3_STAINING_' || permission_code, 'ROLE_M3_STAINING', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN ('PERM_M3_STAINING', 'PERM_M3_TECH_TASK_QUERY');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M3_REWORK_' || permission_code, 'ROLE_M3_REWORK', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN ('PERM_M3_REWORK', 'PERM_M3_TECH_TASK_QUERY');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M3_TRACKING_' || permission_code, 'ROLE_M3_TRACKING', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN ('PERM_M3_TECH_TRACKING_QUERY');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M4_ASSIGN_' || permission_code, 'ROLE_M4_ASSIGN', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN ('PERM_M4_DIAG_TASK_QUERY', 'PERM_M4_ASSIGN');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M4_DIAG_' || permission_code, 'ROLE_M4_DIAGNOSIS', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN (
    'PERM_M4_DIAG_TASK_QUERY', 'PERM_M4_ACCEPT', 'PERM_M4_START', 'PERM_M4_WORKBENCH_QUERY',
    'PERM_M4_REPORT_CREATE', 'PERM_M4_REPORT_SUBMIT', 'PERM_M4_REVISION_REQUEST_CREATE',
    'PERM_M4_MEDICAL_ORDER_CREATE', 'PERM_M4_MEDICAL_ORDER_CANCEL',
    'PERM_M4_CONSULTATION_CREATE', 'PERM_M4_CONSULTATION_COMMENT', 'PERM_M4_CONSULTATION_COMPLETE'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M4_REVIEW_' || permission_code, 'ROLE_M4_REVIEW', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN ('PERM_M4_DIAG_TASK_QUERY', 'PERM_M4_WORKBENCH_QUERY', 'PERM_M4_REPORT_REVIEW', 'PERM_M4_CONSULTATION_COMMENT');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M4_SIGN_' || permission_code, 'ROLE_M4_SIGN', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN ('PERM_M4_DIAG_TASK_QUERY', 'PERM_M4_WORKBENCH_QUERY', 'PERM_M4_REPORT_SIGN', 'PERM_M4_REPORT_PUBLISH', 'PERM_M4_REVISION_APPROVE', 'PERM_M4_CONSULTATION_COMMENT');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M4_TRACKING_' || permission_code, 'ROLE_M4_TRACKING', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN ('PERM_M4_REPORT_TRACKING_QUERY');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M4_ORDER_EXEC_' || permission_code, 'ROLE_M4_MEDICAL_ORDER_EXECUTE', id, CURRENT_TIMESTAMP
FROM permissions
WHERE permission_code IN ('PERM_M4_MEDICAL_ORDER_QUERY', 'PERM_M4_MEDICAL_ORDER_ACCEPT', 'PERM_M4_MEDICAL_ORDER_COMPLETE');

INSERT INTO role_message_subscriptions (id, role_id, topic_id, subscription_mode, assigned_at) VALUES
('RMS_ADMIN_CRITICAL', 'ROLE_PATHOLOGY_ADMIN', 'TOPIC_CRITICAL_VALUE', 'POPUP', CURRENT_TIMESTAMP),
('RMS_ADMIN_REVISION', 'ROLE_PATHOLOGY_ADMIN', 'TOPIC_REPORT_REVISION', 'INBOX', CURRENT_TIMESTAMP),
('RMS_ADMIN_CONSULT', 'ROLE_PATHOLOGY_ADMIN', 'TOPIC_CONSULTATION', 'INBOX', CURRENT_TIMESTAMP),
('RMS_ADMIN_QC', 'ROLE_PATHOLOGY_ADMIN', 'TOPIC_QC_WARNING', 'POPUP', CURRENT_TIMESTAMP),
('RMS_QM_CRITICAL', 'ROLE_QUALITY_MANAGER', 'TOPIC_CRITICAL_VALUE', 'POPUP', CURRENT_TIMESTAMP),
('RMS_QM_QC', 'ROLE_QUALITY_MANAGER', 'TOPIC_QC_WARNING', 'POPUP', CURRENT_TIMESTAMP),
('RMS_DOCTOR_REVISION', 'ROLE_PATHOLOGY_DOCTOR', 'TOPIC_REPORT_REVISION', 'INBOX', CURRENT_TIMESTAMP),
('RMS_DOCTOR_CONSULT', 'ROLE_PATHOLOGY_DOCTOR', 'TOPIC_CONSULTATION', 'POPUP', CURRENT_TIMESTAMP),
('RMS_TECH_QC', 'ROLE_PATHOLOGY_TECHNICIAN', 'TOPIC_QC_WARNING', 'INBOX', CURRENT_TIMESTAMP);

INSERT INTO role_stat_authorizations (id, role_id, stat_category_id, auth_scope, assigned_at) VALUES
('RSA_ADMIN_OPERATION', 'ROLE_PATHOLOGY_ADMIN', 'STAT_OPERATION', 'MANAGE', CURRENT_TIMESTAMP),
('RSA_ADMIN_WORKLOAD', 'ROLE_PATHOLOGY_ADMIN', 'STAT_WORKLOAD', 'MANAGE', CURRENT_TIMESTAMP),
('RSA_ADMIN_QUALITY', 'ROLE_PATHOLOGY_ADMIN', 'STAT_QUALITY', 'MANAGE', CURRENT_TIMESTAMP),
('RSA_QM_QUALITY', 'ROLE_QUALITY_MANAGER', 'STAT_QUALITY', 'MANAGE', CURRENT_TIMESTAMP),
('RSA_QM_WORKLOAD', 'ROLE_QUALITY_MANAGER', 'STAT_WORKLOAD', 'VIEW', CURRENT_TIMESTAMP),
('RSA_DOCTOR_WORKLOAD', 'ROLE_PATHOLOGY_DOCTOR', 'STAT_WORKLOAD', 'VIEW', CURRENT_TIMESTAMP),
('RSA_TECH_WORKLOAD', 'ROLE_PATHOLOGY_TECHNICIAN', 'STAT_WORKLOAD', 'VIEW', CURRENT_TIMESTAMP);

INSERT INTO role_menus (id, role_id, menu_id, assigned_at) VALUES
('RM_ADMIN_SYSTEM', 'ROLE_PATHOLOGY_ADMIN', 'MENU_SYSTEM', CURRENT_TIMESTAMP),
('RM_ADMIN_USERS', 'ROLE_PATHOLOGY_ADMIN', 'MENU_SYS_USERS', CURRENT_TIMESTAMP),
('RM_ADMIN_ROLES', 'ROLE_PATHOLOGY_ADMIN', 'MENU_SYS_ROLES', CURRENT_TIMESTAMP),
('RM_ADMIN_BODY_PARTS', 'ROLE_PATHOLOGY_ADMIN', 'MENU_BODY_PARTS', CURRENT_TIMESTAMP),
('RM_ADMIN_ORDER_DICTS', 'ROLE_PATHOLOGY_ADMIN', 'MENU_ORDER_DICTS', CURRENT_TIMESTAMP),
('RM_ADMIN_ORDER_CHARGES', 'ROLE_PATHOLOGY_ADMIN', 'MENU_ORDER_CHARGES', CURRENT_TIMESTAMP),
('RM_ADMIN_ORDER_PACKAGES', 'ROLE_PATHOLOGY_ADMIN', 'MENU_ORDER_PACKAGES', CURRENT_TIMESTAMP),
('RM_ADMIN_TEMPLATES', 'ROLE_PATHOLOGY_ADMIN', 'MENU_TEMPLATES', CURRENT_TIMESTAMP),
('RM_ADMIN_GUIDELINES', 'ROLE_PATHOLOGY_ADMIN', 'MENU_GUIDELINES', CURRENT_TIMESTAMP),
('RM_ADMIN_CONFIGS', 'ROLE_PATHOLOGY_ADMIN', 'MENU_CONFIGS', CURRENT_TIMESTAMP),
('RM_ADMIN_NUMBERING', 'ROLE_PATHOLOGY_ADMIN', 'MENU_NUMBERING', CURRENT_TIMESTAMP),
('RM_ADMIN_M2_ROOT', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M2_WORKFLOW', CURRENT_TIMESTAMP),
('RM_ADMIN_M2_CLINICAL', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M2_CLINICAL', CURRENT_TIMESTAMP),
('RM_ADMIN_M2_FIXATION', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M2_FIXATION', CURRENT_TIMESTAMP),
('RM_ADMIN_M2_TRANSPORT', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M2_TRANSPORT', CURRENT_TIMESTAMP),
('RM_ADMIN_M2_RECEIPT', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M2_RECEIPT', CURRENT_TIMESTAMP),
('RM_ADMIN_M2_TRACKING', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M2_TRACKING', CURRENT_TIMESTAMP),
('RM_ADMIN_M3_ROOT', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M3_WORKFLOW', CURRENT_TIMESTAMP),
('RM_ADMIN_M3_GROSSING', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M3_GROSSING', CURRENT_TIMESTAMP),
('RM_ADMIN_M3_DEHYDRATION', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M3_DEHYDRATION', CURRENT_TIMESTAMP),
('RM_ADMIN_M3_EMBEDDING', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M3_EMBEDDING', CURRENT_TIMESTAMP),
('RM_ADMIN_M3_SLICING', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M3_SLICING', CURRENT_TIMESTAMP),
('RM_ADMIN_M3_STAINING', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M3_STAINING', CURRENT_TIMESTAMP),
('RM_ADMIN_M3_REWORK', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M3_REWORK', CURRENT_TIMESTAMP),
('RM_ADMIN_M3_TRACKING', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M3_TRACKING', CURRENT_TIMESTAMP),
('RM_ADMIN_M3_TASKS', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M3_TASKS', CURRENT_TIMESTAMP),
('RM_ADMIN_M4_ROOT', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M4_WORKFLOW', CURRENT_TIMESTAMP),
('RM_ADMIN_M4_ASSIGN', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M4_ASSIGN', CURRENT_TIMESTAMP),
('RM_ADMIN_M4_WORKBENCH', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M4_WORKBENCH', CURRENT_TIMESTAMP),
('RM_ADMIN_M4_REPORT', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M4_REPORT', CURRENT_TIMESTAMP),
('RM_ADMIN_M4_TRACKING', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M4_TRACKING', CURRENT_TIMESTAMP),
('RM_ADMIN_M4_REVISION', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M4_REVISION', CURRENT_TIMESTAMP),
('RM_ADMIN_M4_ORDER', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M4_MEDICAL_ORDER', CURRENT_TIMESTAMP),
('RM_ADMIN_M4_CONSULT', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M4_CONSULTATION', CURRENT_TIMESTAMP),
('RM_ADMIN_M5_ROOT', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M5_SUPPORT', CURRENT_TIMESTAMP),
('RM_ADMIN_M5_ARCHIVE', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M5_ARCHIVE', CURRENT_TIMESTAMP),
('RM_ADMIN_M5_REAGENT', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M5_REAGENT', CURRENT_TIMESTAMP),
('RM_ADMIN_M5_EQUIPMENT', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M5_EQUIPMENT', CURRENT_TIMESTAMP),
('RM_ADMIN_M6_ROOT', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M6_SUPPORT', CURRENT_TIMESTAMP),
('RM_ADMIN_M6_INTEGRATION', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M6_INTEGRATION', CURRENT_TIMESTAMP),
('RM_ADMIN_M6_BILLING', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M6_BILLING', CURRENT_TIMESTAMP),
('RM_ADMIN_M6_HISTORY', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M6_HISTORY', CURRENT_TIMESTAMP),
('RM_ADMIN_M6_STAT', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M6_STAT', CURRENT_TIMESTAMP),
('RM_ARCHIVE_ROOT', 'ROLE_ARCHIVE_MANAGER', 'MENU_M5_SUPPORT', CURRENT_TIMESTAMP),
('RM_ARCHIVE_MENU', 'ROLE_ARCHIVE_MANAGER', 'MENU_M5_ARCHIVE', CURRENT_TIMESTAMP),
('RM_ARCHIVE_M6_ROOT', 'ROLE_ARCHIVE_MANAGER', 'MENU_M6_SUPPORT', CURRENT_TIMESTAMP),
('RM_ARCHIVE_HISTORY', 'ROLE_ARCHIVE_MANAGER', 'MENU_M6_HISTORY', CURRENT_TIMESTAMP),
('RM_REAGENT_ROOT', 'ROLE_REAGENT_DEVICE_MANAGER', 'MENU_M5_SUPPORT', CURRENT_TIMESTAMP),
('RM_REAGENT_MENU', 'ROLE_REAGENT_DEVICE_MANAGER', 'MENU_M5_REAGENT', CURRENT_TIMESTAMP),
('RM_REAGENT_EQUIPMENT', 'ROLE_REAGENT_DEVICE_MANAGER', 'MENU_M5_EQUIPMENT', CURRENT_TIMESTAMP),
('RM_QUALITY_ROOT', 'ROLE_QUALITY_MANAGER', 'MENU_M6_SUPPORT', CURRENT_TIMESTAMP),
('RM_QUALITY_STAT', 'ROLE_QUALITY_MANAGER', 'MENU_M6_STAT', CURRENT_TIMESTAMP),
('RM_M2_REGISTER_ROOT', 'ROLE_M2_CLINICAL_REGISTER', 'MENU_M2_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M2_REGISTER_MENU', 'ROLE_M2_CLINICAL_REGISTER', 'MENU_M2_CLINICAL', CURRENT_TIMESTAMP),
('RM_M2_FIXATION_ROOT', 'ROLE_M2_FIXATION_VERIFY', 'MENU_M2_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M2_FIXATION_MENU', 'ROLE_M2_FIXATION_VERIFY', 'MENU_M2_FIXATION', CURRENT_TIMESTAMP),
('RM_M2_TRANSPORT_ROOT', 'ROLE_M2_TRANSPORT_HANDOVER', 'MENU_M2_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M2_TRANSPORT_MENU', 'ROLE_M2_TRANSPORT_HANDOVER', 'MENU_M2_TRANSPORT', CURRENT_TIMESTAMP),
('RM_M2_RECEIVE_ROOT', 'ROLE_M2_SPECIMEN_RECEIVE', 'MENU_M2_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M2_RECEIVE_MENU', 'ROLE_M2_SPECIMEN_RECEIVE', 'MENU_M2_RECEIPT', CURRENT_TIMESTAMP),
('RM_M2_TRACKING_ROOT', 'ROLE_M2_TRACKING_QUERY', 'MENU_M2_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M2_TRACKING_MENU', 'ROLE_M2_TRACKING_QUERY', 'MENU_M2_TRACKING', CURRENT_TIMESTAMP),
('RM_M2_IMPORT_ROOT', 'ROLE_M2_CLINICAL_IMPORT', 'MENU_M2_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M2_IMPORT_MENU', 'ROLE_M2_CLINICAL_IMPORT', 'MENU_M2_CLINICAL', CURRENT_TIMESTAMP),
('RM_M3_GROSSING_ROOT', 'ROLE_M3_GROSSING', 'MENU_M3_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M3_GROSSING_MENU', 'ROLE_M3_GROSSING', 'MENU_M3_GROSSING', CURRENT_TIMESTAMP),
('RM_M3_GROSSING_TASKS', 'ROLE_M3_GROSSING', 'MENU_M3_TASKS', CURRENT_TIMESTAMP),
('RM_M3_DEHYDRATION_ROOT', 'ROLE_M3_DEHYDRATION', 'MENU_M3_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M3_DEHYDRATION_MENU', 'ROLE_M3_DEHYDRATION', 'MENU_M3_DEHYDRATION', CURRENT_TIMESTAMP),
('RM_M3_DEHYDRATION_TASKS', 'ROLE_M3_DEHYDRATION', 'MENU_M3_TASKS', CURRENT_TIMESTAMP),
('RM_M3_EMBEDDING_ROOT', 'ROLE_M3_EMBEDDING', 'MENU_M3_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M3_EMBEDDING_MENU', 'ROLE_M3_EMBEDDING', 'MENU_M3_EMBEDDING', CURRENT_TIMESTAMP),
('RM_M3_EMBEDDING_TASKS', 'ROLE_M3_EMBEDDING', 'MENU_M3_TASKS', CURRENT_TIMESTAMP),
('RM_M3_SLICING_ROOT', 'ROLE_M3_SLICING', 'MENU_M3_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M3_SLICING_MENU', 'ROLE_M3_SLICING', 'MENU_M3_SLICING', CURRENT_TIMESTAMP),
('RM_M3_SLICING_TASKS', 'ROLE_M3_SLICING', 'MENU_M3_TASKS', CURRENT_TIMESTAMP),
('RM_M3_STAINING_ROOT', 'ROLE_M3_STAINING', 'MENU_M3_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M3_STAINING_MENU', 'ROLE_M3_STAINING', 'MENU_M3_STAINING', CURRENT_TIMESTAMP),
('RM_M3_STAINING_TASKS', 'ROLE_M3_STAINING', 'MENU_M3_TASKS', CURRENT_TIMESTAMP),
('RM_M3_REWORK_ROOT', 'ROLE_M3_REWORK', 'MENU_M3_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M3_REWORK_MENU', 'ROLE_M3_REWORK', 'MENU_M3_REWORK', CURRENT_TIMESTAMP),
('RM_M3_REWORK_TASKS', 'ROLE_M3_REWORK', 'MENU_M3_TASKS', CURRENT_TIMESTAMP),
('RM_M3_TRACKING_ROOT', 'ROLE_M3_TRACKING', 'MENU_M3_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M3_TRACKING_MENU', 'ROLE_M3_TRACKING', 'MENU_M3_TRACKING', CURRENT_TIMESTAMP),
('RM_M4_ASSIGN_ROOT', 'ROLE_M4_ASSIGN', 'MENU_M4_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M4_ASSIGN_MENU', 'ROLE_M4_ASSIGN', 'MENU_M4_ASSIGN', CURRENT_TIMESTAMP),
('RM_M4_DIAG_ROOT', 'ROLE_M4_DIAGNOSIS', 'MENU_M4_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M4_DIAG_ASSIGN', 'ROLE_M4_DIAGNOSIS', 'MENU_M4_ASSIGN', CURRENT_TIMESTAMP),
('RM_M4_DIAG_WORKBENCH', 'ROLE_M4_DIAGNOSIS', 'MENU_M4_WORKBENCH', CURRENT_TIMESTAMP),
('RM_M4_DIAG_REPORT', 'ROLE_M4_DIAGNOSIS', 'MENU_M4_REPORT', CURRENT_TIMESTAMP),
('RM_M4_DIAG_REVISION', 'ROLE_M4_DIAGNOSIS', 'MENU_M4_REVISION', CURRENT_TIMESTAMP),
('RM_M4_DIAG_ORDER', 'ROLE_M4_DIAGNOSIS', 'MENU_M4_MEDICAL_ORDER', CURRENT_TIMESTAMP),
('RM_M4_DIAG_CONSULT', 'ROLE_M4_DIAGNOSIS', 'MENU_M4_CONSULTATION', CURRENT_TIMESTAMP),
('RM_M4_REVIEW_ROOT', 'ROLE_M4_REVIEW', 'MENU_M4_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M4_REVIEW_ASSIGN', 'ROLE_M4_REVIEW', 'MENU_M4_ASSIGN', CURRENT_TIMESTAMP),
('RM_M4_REVIEW_WORKBENCH', 'ROLE_M4_REVIEW', 'MENU_M4_WORKBENCH', CURRENT_TIMESTAMP),
('RM_M4_REVIEW_REPORT', 'ROLE_M4_REVIEW', 'MENU_M4_REPORT', CURRENT_TIMESTAMP),
('RM_M4_REVIEW_CONSULT', 'ROLE_M4_REVIEW', 'MENU_M4_CONSULTATION', CURRENT_TIMESTAMP),
('RM_M4_SIGN_ROOT', 'ROLE_M4_SIGN', 'MENU_M4_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M4_SIGN_ASSIGN', 'ROLE_M4_SIGN', 'MENU_M4_ASSIGN', CURRENT_TIMESTAMP),
('RM_M4_SIGN_WORKBENCH', 'ROLE_M4_SIGN', 'MENU_M4_WORKBENCH', CURRENT_TIMESTAMP),
('RM_M4_SIGN_REPORT', 'ROLE_M4_SIGN', 'MENU_M4_REPORT', CURRENT_TIMESTAMP),
('RM_M4_SIGN_REVISION', 'ROLE_M4_SIGN', 'MENU_M4_REVISION', CURRENT_TIMESTAMP),
('RM_M4_SIGN_CONSULT', 'ROLE_M4_SIGN', 'MENU_M4_CONSULTATION', CURRENT_TIMESTAMP),
('RM_M4_TRACKING_ROOT', 'ROLE_M4_TRACKING', 'MENU_M4_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M4_TRACKING_MENU', 'ROLE_M4_TRACKING', 'MENU_M4_TRACKING', CURRENT_TIMESTAMP),
('RM_M4_ORDER_EXEC_ROOT', 'ROLE_M4_MEDICAL_ORDER_EXECUTE', 'MENU_M4_WORKFLOW', CURRENT_TIMESTAMP),
('RM_M4_ORDER_EXEC_MENU', 'ROLE_M4_MEDICAL_ORDER_EXECUTE', 'MENU_M4_MEDICAL_ORDER', CURRENT_TIMESTAMP);

INSERT INTO users (id, user_code, login_name, name, password, password_algo, password_salt, role, job_no, title_name, department_id, department_name, enabled, created_at, updated_at) VALUES
('USER_M1_ADMIN', 'U-M1-ADMIN', 'm1.admin', '病理管理员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'PATHOLOGY_ADMIN', 'BL0001', '主任医师', 'DEPT_PATHOLOGY_DIAG', '病理诊断组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M1_DOCTOR', 'U-M1-DOCTOR', 'm1.doctor', '病理医生', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'PATHOLOGY_DOCTOR', 'BL0002', '主治医师', 'DEPT_PATHOLOGY_DIAG', '病理诊断组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M1_TECHNICIAN', 'U-M1-TECH', 'm1.technician', '病理技师', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'PATHOLOGY_TECHNICIAN', 'BL0003', '主管技师', 'DEPT_PATHOLOGY_TECH', '病理技术组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M1_ARCHIVE', 'U-M1-ARCHIVE', 'm1.archive', '归档管理员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'ARCHIVE_MANAGER', 'BL0004', '档案管理员', 'DEPT_PATHOLOGY_ARCHIVE', '病理档案组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M1_REAGENT', 'U-M1-REAGENT', 'm1.reagent', '试剂设备管理员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'REAGENT_DEVICE_MANAGER', 'BL0005', '设备管理员', 'DEPT_PATHOLOGY_SUPPORT', '病理运营支持组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M1_QUALITY', 'U-M1-QUALITY', 'm1.quality', '质控管理员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'QUALITY_MANAGER', 'BL0006', '质控主管', 'DEPT_PATHOLOGY_SUPPORT', '病理运营支持组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M1_NO_PERMISSION', 'U-M1-NOAUTH', 'm1.noauth', '未授权测试用户', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', NULL, 'BL0007', '试用人员', 'DEPT_PATHOLOGY', '病理科', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M2_ADMIN', 'U-M2-ADMIN', 'm2.admin', '流程管理员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'PATHOLOGY_ADMIN', 'M20001', '流程管理员', 'DEPT_PATHOLOGY_TECH', '病理技术组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M2_REGISTER', 'U-M2-REGISTER', 'm2.register', '标本登记员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M2_CLINICAL_REGISTER', 'M20002', '登记员', 'DEPT_PATHOLOGY_TECH', '病理技术组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M2_FIXATION', 'U-M2-FIXATION', 'm2.fixation', '固定核验员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M2_FIXATION_VERIFY', 'M20003', '技师', 'DEPT_PATHOLOGY_TECH', '病理技术组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M2_TRANSPORT', 'U-M2-TRANSPORT', 'm2.transport', '转运交接员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M2_TRANSPORT_HANDOVER', 'M20004', '转运员', 'DEPT_PATHOLOGY_TECH', '病理技术组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M2_RECEIVE', 'U-M2-RECEIVE', 'm2.receive', '标本签收员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M2_SPECIMEN_RECEIVE', 'M20005', '签收员', 'DEPT_PATHOLOGY_TECH', '病理技术组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M2_TRACKING', 'U-M2-TRACKING', 'm2.tracking', '流程追踪员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M2_TRACKING_QUERY', 'M20006', '追踪员', 'DEPT_PATHOLOGY_TECH', '病理技术组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M2_IMPORT', 'U-M2-IMPORT', 'm2.import', '申请导入员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M2_CLINICAL_IMPORT', 'M20007', '导入员', 'DEPT_PATHOLOGY_TECH', '病理技术组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M2_NO_PERMISSION', 'U-M2-NOAUTH', 'm2.noauth', 'M2 未授权用户', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', NULL, 'M20008', '试用人员', 'DEPT_PATHOLOGY_TECH', '病理技术组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M3_GROSSING', 'U-M3-GROSSING', 'm3.grossing', '取材技师', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M3_GROSSING', 'M30001', '技师', 'DEPT_PATHOLOGY_TECH', '病理技术组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M3_DEHYDRATION', 'U-M3-DEHYDRATION', 'm3.dehydration', '脱水技师', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M3_DEHYDRATION', 'M30002', '技师', 'DEPT_PATHOLOGY_TECH', '病理技术组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M3_EMBEDDING', 'U-M3-EMBEDDING', 'm3.embedding', '包埋技师', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M3_EMBEDDING', 'M30003', '技师', 'DEPT_PATHOLOGY_TECH', '病理技术组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M3_SLICING', 'U-M3-SLICING', 'm3.slicing', '切片技师', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M3_SLICING', 'M30004', '技师', 'DEPT_PATHOLOGY_TECH', '病理技术组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M3_STAINING', 'U-M3-STAINING', 'm3.staining', '染色技师', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M3_STAINING', 'M30005', '技师', 'DEPT_PATHOLOGY_TECH', '病理技术组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M3_REWORK', 'U-M3-REWORK', 'm3.rework', '返工技师', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M3_REWORK', 'M30006', '技师', 'DEPT_PATHOLOGY_TECH', '病理技术组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M3_TRACKING', 'U-M3-TRACKING', 'm3.tracking', '技术追踪员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M3_TRACKING', 'M30007', '追踪员', 'DEPT_PATHOLOGY_TECH', '病理技术组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M4_ASSIGN', 'U-M4-ASSIGN', 'm4.assign', '诊断分配员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M4_ASSIGN', 'M40001', '分配员', 'DEPT_PATHOLOGY_DIAG', '病理诊断组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M4_DIAGNOSIS', 'U-M4-DIAG', 'm4.diagnosis', '病理诊断医师', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M4_DIAGNOSIS', 'M40002', '主治医师', 'DEPT_PATHOLOGY_DIAG', '病理诊断组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M4_REVIEW', 'U-M4-REVIEW', 'm4.review', '病理复核医师', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M4_REVIEW', 'M40003', '副主任医师', 'DEPT_PATHOLOGY_DIAG', '病理诊断组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M4_SIGN', 'U-M4-SIGN', 'm4.sign', '病理签发医师', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M4_SIGN', 'M40004', '主任医师', 'DEPT_PATHOLOGY_DIAG', '病理诊断组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M4_TRACKING', 'U-M4-TRACKING', 'm4.tracking', '报告追踪员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M4_TRACKING', 'M40005', '追踪员', 'DEPT_PATHOLOGY_DIAG', '病理诊断组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M4_ORDER_EXECUTE', 'U-M4-ORDER-EXEC', 'm4.order.execute', '病理医嘱执行员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'M4_MEDICAL_ORDER_EXECUTE', 'M40006', '执行员', 'DEPT_PATHOLOGY_SUPPORT', '病理运营支持组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M4_NO_PERMISSION', 'U-M4-NOAUTH', 'm4.noauth', 'M4 未授权用户', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', NULL, 'M40007', '试用人员', 'DEPT_PATHOLOGY_DIAG', '病理诊断组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M5_ARCHIVE', 'U-M5-ARCHIVE', 'm5.archive', '病理归档管理员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'ARCHIVE_MANAGER', 'M50001', '档案管理员', 'DEPT_PATHOLOGY_ARCHIVE', '病理档案组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M5_REAGENT', 'U-M5-REAGENT', 'm5.reagent', '试剂设备管理员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'REAGENT_DEVICE_MANAGER', 'M50002', '设备管理员', 'DEPT_PATHOLOGY_SUPPORT', '病理运营支持组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M6_ADMIN', 'U-M6-ADMIN', 'm6.admin', '集成运营管理员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'PATHOLOGY_ADMIN', 'M60001', '运营管理员', 'DEPT_PATHOLOGY_SUPPORT', '病理运营支持组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M6_ARCHIVE', 'U-M6-ARCHIVE', 'm6.archive', '历史报告管理员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'ARCHIVE_MANAGER', 'M60002', '档案管理员', 'DEPT_PATHOLOGY_ARCHIVE', '病理档案组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M6_QUALITY', 'U-M6-QUALITY', 'm6.quality', '统计质控管理员', '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134', 'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1', 'QUALITY_MANAGER', 'M60003', '质控主管', 'DEPT_PATHOLOGY_SUPPORT', '病理运营支持组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO user_roles (id, user_id, role_id, is_primary, assigned_at, assigned_by_name) VALUES
('UR_M1_ADMIN', 'USER_M1_ADMIN', 'ROLE_PATHOLOGY_ADMIN', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M1_DOCTOR', 'USER_M1_DOCTOR', 'ROLE_PATHOLOGY_DOCTOR', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M1_TECHNICIAN', 'USER_M1_TECHNICIAN', 'ROLE_PATHOLOGY_TECHNICIAN', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M1_ARCHIVE', 'USER_M1_ARCHIVE', 'ROLE_ARCHIVE_MANAGER', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M1_REAGENT', 'USER_M1_REAGENT', 'ROLE_REAGENT_DEVICE_MANAGER', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M1_QUALITY', 'USER_M1_QUALITY', 'ROLE_QUALITY_MANAGER', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M2_ADMIN', 'USER_M2_ADMIN', 'ROLE_PATHOLOGY_ADMIN', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M2_REGISTER', 'USER_M2_REGISTER', 'ROLE_M2_CLINICAL_REGISTER', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M2_FIXATION', 'USER_M2_FIXATION', 'ROLE_M2_FIXATION_VERIFY', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M2_TRANSPORT', 'USER_M2_TRANSPORT', 'ROLE_M2_TRANSPORT_HANDOVER', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M2_RECEIVE', 'USER_M2_RECEIVE', 'ROLE_M2_SPECIMEN_RECEIVE', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M2_TRACKING', 'USER_M2_TRACKING', 'ROLE_M2_TRACKING_QUERY', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M2_IMPORT', 'USER_M2_IMPORT', 'ROLE_M2_CLINICAL_IMPORT', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M3_GROSSING', 'USER_M3_GROSSING', 'ROLE_M3_GROSSING', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M3_DEHYDRATION', 'USER_M3_DEHYDRATION', 'ROLE_M3_DEHYDRATION', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M3_EMBEDDING', 'USER_M3_EMBEDDING', 'ROLE_M3_EMBEDDING', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M3_SLICING', 'USER_M3_SLICING', 'ROLE_M3_SLICING', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M3_STAINING', 'USER_M3_STAINING', 'ROLE_M3_STAINING', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M3_REWORK', 'USER_M3_REWORK', 'ROLE_M3_REWORK', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M3_TRACKING', 'USER_M3_TRACKING', 'ROLE_M3_TRACKING', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M4_ASSIGN', 'USER_M4_ASSIGN', 'ROLE_M4_ASSIGN', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M4_DIAGNOSIS', 'USER_M4_DIAGNOSIS', 'ROLE_M4_DIAGNOSIS', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M4_REVIEW', 'USER_M4_REVIEW', 'ROLE_M4_REVIEW', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M4_SIGN', 'USER_M4_SIGN', 'ROLE_M4_SIGN', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M4_TRACKING', 'USER_M4_TRACKING', 'ROLE_M4_TRACKING', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M4_ORDER_EXECUTE', 'USER_M4_ORDER_EXECUTE', 'ROLE_M4_MEDICAL_ORDER_EXECUTE', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M5_ARCHIVE', 'USER_M5_ARCHIVE', 'ROLE_ARCHIVE_MANAGER', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M5_REAGENT', 'USER_M5_REAGENT', 'ROLE_REAGENT_DEVICE_MANAGER', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M6_ADMIN', 'USER_M6_ADMIN', 'ROLE_PATHOLOGY_ADMIN', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M6_ARCHIVE', 'USER_M6_ARCHIVE', 'ROLE_ARCHIVE_MANAGER', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M6_QUALITY', 'USER_M6_QUALITY', 'ROLE_QUALITY_MANAGER', 1, CURRENT_TIMESTAMP, 'system');

COMMIT;
