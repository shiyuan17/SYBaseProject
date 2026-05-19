-- 达梦数据库表结构整理（病理全流程中度优化版）
-- 来源: D:/SVN-Project/BlSysNew/pathology.db
-- 生成日期: 2026-05-19
-- 用途: 病理全流程管理系统数据库设计参考
-- 说明:
-- 1. 本版本按“病例主表 + 流程主链路 + 扩展流程 + 审计日志”进行重整。
-- 2. 字段命名统一采用英文下划线风格，优先使用 VARCHAR2、NUMBER、DATE、TIMESTAMP、CLOB。
-- 3. 所有关键人员、科室字段采用“ID + 名称快照”并存，便于统计与历史追溯。
-- 4. 原 reports、sections、stainings、specimen_archives 已被职责更清晰的新结构替代。
-- 5. 状态码和类型码先通过字段注释约定，不单独拆分字典表。

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
    name VARCHAR2(100) NOT NULL,
    password VARCHAR2(255),
    role VARCHAR2(50),
    department_id VARCHAR2(64),
    department_name VARCHAR2(100),
    avatar VARCHAR2(500),
    enabled NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT ck_users_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_users_user_code UNIQUE (user_code)
);

COMMENT ON TABLE users IS '系统用户表';
COMMENT ON COLUMN users.id IS '主键ID';
COMMENT ON COLUMN users.user_code IS '用户编码';
COMMENT ON COLUMN users.name IS '用户姓名';
COMMENT ON COLUMN users.password IS '登录密码';
COMMENT ON COLUMN users.role IS '角色编码';
COMMENT ON COLUMN users.department_id IS '所属科室ID';
COMMENT ON COLUMN users.department_name IS '所属科室名称快照';
COMMENT ON COLUMN users.avatar IS '头像地址';
COMMENT ON COLUMN users.enabled IS '是否启用，0否1是';
COMMENT ON COLUMN users.created_at IS '创建时间';

-- =========================================================
-- 申请与病例中心
-- =========================================================
CREATE TABLE applications (
    id VARCHAR2(64) NOT NULL,
    application_no VARCHAR2(64) NOT NULL,
    patient_id VARCHAR2(64),
    application_type VARCHAR2(50),
    status VARCHAR2(32),
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
    CONSTRAINT fk_applications_patient FOREIGN KEY (patient_id) REFERENCES patients (id)
);

COMMENT ON TABLE applications IS '病理申请单表';
COMMENT ON COLUMN applications.id IS '主键ID';
COMMENT ON COLUMN applications.application_no IS '申请单号';
COMMENT ON COLUMN applications.patient_id IS '患者ID';
COMMENT ON COLUMN applications.application_type IS '申请类型，示例：ROUTINE/FROZEN/MOLECULAR/CONSULTATION';
COMMENT ON COLUMN applications.status IS '申请状态，示例：DRAFT/SUBMITTED/RECEIVED/CLOSED/CANCELLED';
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
COMMENT ON COLUMN pathology_cases.current_status IS '当前主流程状态，示例：RECEIVED/REGISTERED/SAMPLING/EMBEDDING/SLICING/STAINING/DIAGNOSING/REVIEWING/PUBLISHED/ARCHIVED';
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
    case_id VARCHAR2(64) NOT NULL,
    application_id VARCHAR2(64),
    specimen_no VARCHAR2(64) NOT NULL,
    specimen_type VARCHAR2(100),
    specimen_site VARCHAR2(200),
    specimen_count NUMBER(10),
    fixation_status VARCHAR2(200),
    qualified_flag NUMBER(1) DEFAULT 1,
    unqualified_reason VARCHAR2(500),
    clinical_symptom VARCHAR2(500),
    applicant_department_id VARCHAR2(64),
    applicant_department_name VARCHAR2(100),
    applicant_doctor_user_id VARCHAR2(64),
    applicant_doctor_name VARCHAR2(100),
    submission_date DATE,
    registered_by_user_id VARCHAR2(64),
    registered_by_name VARCHAR2(100),
    registered_at TIMESTAMP,
    remarks VARCHAR2(500),
    CONSTRAINT pk_specimens PRIMARY KEY (id),
    CONSTRAINT ck_specimens_qualified_flag CHECK (qualified_flag IN (0, 1)),
    CONSTRAINT uk_specimens_case_specimen_no UNIQUE (case_id, specimen_no),
    CONSTRAINT fk_specimens_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_specimens_application FOREIGN KEY (application_id) REFERENCES applications (id)
);

COMMENT ON TABLE specimens IS '病例下标本表';
COMMENT ON COLUMN specimens.id IS '主键ID';
COMMENT ON COLUMN specimens.case_id IS '病例ID';
COMMENT ON COLUMN specimens.application_id IS '申请单ID';
COMMENT ON COLUMN specimens.specimen_no IS '标本序号或条码，病例内唯一';
COMMENT ON COLUMN specimens.specimen_type IS '标本类型';
COMMENT ON COLUMN specimens.specimen_site IS '标本部位';
COMMENT ON COLUMN specimens.specimen_count IS '标本数量';
COMMENT ON COLUMN specimens.fixation_status IS '固定情况';
COMMENT ON COLUMN specimens.qualified_flag IS '是否合格，0否1是';
COMMENT ON COLUMN specimens.unqualified_reason IS '不合格原因';
COMMENT ON COLUMN specimens.clinical_symptom IS '临床症状';
COMMENT ON COLUMN specimens.applicant_department_id IS '申请科室ID';
COMMENT ON COLUMN specimens.applicant_department_name IS '申请科室名称快照';
COMMENT ON COLUMN specimens.applicant_doctor_user_id IS '申请医生用户ID';
COMMENT ON COLUMN specimens.applicant_doctor_name IS '申请医生姓名快照';
COMMENT ON COLUMN specimens.submission_date IS '送检日期';
COMMENT ON COLUMN specimens.registered_by_user_id IS '登记人用户ID';
COMMENT ON COLUMN specimens.registered_by_name IS '登记人姓名快照';
COMMENT ON COLUMN specimens.registered_at IS '登记时间';
COMMENT ON COLUMN specimens.remarks IS '备注';

CREATE TABLE specimen_receipts (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64),
    receipt_status VARCHAR2(32) NOT NULL,
    container_count NUMBER(10),
    barcode VARCHAR2(128),
    received_by_user_id VARCHAR2(64),
    received_by_name VARCHAR2(100),
    received_at TIMESTAMP,
    reject_reason VARCHAR2(500),
    return_reason VARCHAR2(500),
    remarks VARCHAR2(500),
    CONSTRAINT pk_specimen_receipts PRIMARY KEY (id),
    CONSTRAINT uk_specimen_receipts_barcode UNIQUE (barcode),
    CONSTRAINT fk_specimen_receipts_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_specimen_receipts_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id)
);

COMMENT ON TABLE specimen_receipts IS '标本接收与拒收记录表';
COMMENT ON COLUMN specimen_receipts.id IS '主键ID';
COMMENT ON COLUMN specimen_receipts.case_id IS '病例ID';
COMMENT ON COLUMN specimen_receipts.specimen_id IS '标本ID';
COMMENT ON COLUMN specimen_receipts.receipt_status IS '接收状态，示例：RECEIVED/REJECTED/RETURNED';
COMMENT ON COLUMN specimen_receipts.container_count IS '容器数量';
COMMENT ON COLUMN specimen_receipts.barcode IS '接收条码';
COMMENT ON COLUMN specimen_receipts.received_by_user_id IS '接收人用户ID';
COMMENT ON COLUMN specimen_receipts.received_by_name IS '接收人姓名快照';
COMMENT ON COLUMN specimen_receipts.received_at IS '接收时间';
COMMENT ON COLUMN specimen_receipts.reject_reason IS '拒收原因';
COMMENT ON COLUMN specimen_receipts.return_reason IS '退回原因';
COMMENT ON COLUMN specimen_receipts.remarks IS '备注';

CREATE TABLE samplings (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64) NOT NULL,
    sampling_status VARCHAR2(32),
    block_count NUMBER(10),
    gross_description CLOB,
    sampled_by_user_id VARCHAR2(64),
    sampled_by_name VARCHAR2(100),
    sampled_at TIMESTAMP,
    remarks VARCHAR2(500),
    CONSTRAINT pk_samplings PRIMARY KEY (id),
    CONSTRAINT fk_samplings_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_samplings_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id)
);

COMMENT ON TABLE samplings IS '取材记录表';
COMMENT ON COLUMN samplings.id IS '主键ID';
COMMENT ON COLUMN samplings.case_id IS '病例ID';
COMMENT ON COLUMN samplings.specimen_id IS '标本ID';
COMMENT ON COLUMN samplings.sampling_status IS '取材状态，示例：PENDING/IN_PROGRESS/COMPLETED';
COMMENT ON COLUMN samplings.block_count IS '材块数量';
COMMENT ON COLUMN samplings.gross_description IS '大体描述';
COMMENT ON COLUMN samplings.sampled_by_user_id IS '取材人用户ID';
COMMENT ON COLUMN samplings.sampled_by_name IS '取材人姓名快照';
COMMENT ON COLUMN samplings.sampled_at IS '取材时间';
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
    storage_status VARCHAR2(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_embedding_boxes PRIMARY KEY (id),
    CONSTRAINT uk_embedding_boxes_box_no UNIQUE (embedding_box_no),
    CONSTRAINT fk_embedding_boxes_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_embedding_boxes_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_embedding_boxes_embedding FOREIGN KEY (embedding_id) REFERENCES embeddings (id)
);

COMMENT ON TABLE embedding_boxes IS '包埋盒表';
COMMENT ON COLUMN embedding_boxes.id IS '主键ID';
COMMENT ON COLUMN embedding_boxes.case_id IS '病例ID';
COMMENT ON COLUMN embedding_boxes.specimen_id IS '标本ID';
COMMENT ON COLUMN embedding_boxes.embedding_id IS '包埋记录ID';
COMMENT ON COLUMN embedding_boxes.embedding_box_no IS '包埋盒号，全局唯一';
COMMENT ON COLUMN embedding_boxes.block_count IS '盒内材块数量';
COMMENT ON COLUMN embedding_boxes.storage_status IS '存储状态，示例：ACTIVE/STORED/DISCARDED';
COMMENT ON COLUMN embedding_boxes.created_at IS '创建时间';

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
    quality_status VARCHAR2(32),
    slide_status VARCHAR2(32),
    slice_count NUMBER(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_slides PRIMARY KEY (id),
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
    report_type VARCHAR2(50) DEFAULT 'ROUTINE',
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
    CONSTRAINT pk_pathology_reports PRIMARY KEY (id),
    CONSTRAINT ck_pathology_reports_is_amended CHECK (is_amended IN (0, 1)),
    CONSTRAINT uk_pathology_reports_case UNIQUE (case_id),
    CONSTRAINT uk_pathology_reports_report_no UNIQUE (report_no),
    CONSTRAINT fk_pathology_reports_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_pathology_reports_task FOREIGN KEY (task_id) REFERENCES diagnostic_tasks (id)
);

COMMENT ON TABLE pathology_reports IS '当前正式病理报告表';
COMMENT ON COLUMN pathology_reports.id IS '主键ID';
COMMENT ON COLUMN pathology_reports.case_id IS '病例ID，每例仅保留当前有效报告';
COMMENT ON COLUMN pathology_reports.task_id IS '诊断任务ID';
COMMENT ON COLUMN pathology_reports.report_no IS '报告编号';
COMMENT ON COLUMN pathology_reports.pathology_no IS '病理号快照';
COMMENT ON COLUMN pathology_reports.report_type IS '报告类型，示例：ROUTINE/FROZEN/ADDENDUM';
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

COMMENT ON TABLE archives IS '报告归档及借阅记录表';
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

CREATE TABLE specimen_storage_records (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64),
    object_type VARCHAR2(32) NOT NULL,
    object_id VARCHAR2(64) NOT NULL,
    storage_status VARCHAR2(32),
    storage_location VARCHAR2(200),
    cabinet_no VARCHAR2(64),
    layer_no VARCHAR2(64),
    slot_no VARCHAR2(64),
    stored_by_user_id VARCHAR2(64),
    stored_by_name VARCHAR2(100),
    stored_at TIMESTAMP,
    remarks VARCHAR2(500),
    CONSTRAINT pk_specimen_storage_records PRIMARY KEY (id),
    CONSTRAINT fk_specimen_storage_records_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_specimen_storage_records_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id)
);

COMMENT ON TABLE specimen_storage_records IS '标本/蜡块/玻片实物存储记录表';
COMMENT ON COLUMN specimen_storage_records.id IS '主键ID';
COMMENT ON COLUMN specimen_storage_records.case_id IS '病例ID';
COMMENT ON COLUMN specimen_storage_records.specimen_id IS '标本ID，可为空';
COMMENT ON COLUMN specimen_storage_records.object_type IS '实物类型，示例：SPECIMEN/BLOCK/SLIDE';
COMMENT ON COLUMN specimen_storage_records.object_id IS '实物对象ID';
COMMENT ON COLUMN specimen_storage_records.storage_status IS '存储状态，示例：IN_STORAGE/BORROWED/DISCARDED';
COMMENT ON COLUMN specimen_storage_records.storage_location IS '存储位置描述';
COMMENT ON COLUMN specimen_storage_records.cabinet_no IS '柜号';
COMMENT ON COLUMN specimen_storage_records.layer_no IS '层号';
COMMENT ON COLUMN specimen_storage_records.slot_no IS '格号';
COMMENT ON COLUMN specimen_storage_records.stored_by_user_id IS '入库人用户ID';
COMMENT ON COLUMN specimen_storage_records.stored_by_name IS '入库人姓名快照';
COMMENT ON COLUMN specimen_storage_records.stored_at IS '入库时间';
COMMENT ON COLUMN specimen_storage_records.remarks IS '备注';

CREATE TABLE workflow_events (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
    specimen_id VARCHAR2(64),
    node_code VARCHAR2(50) NOT NULL,
    action_code VARCHAR2(50) NOT NULL,
    from_status VARCHAR2(32),
    to_status VARCHAR2(32),
    operator_user_id VARCHAR2(64),
    operator_name VARCHAR2(100),
    occurred_at TIMESTAMP NOT NULL,
    remarks VARCHAR2(500),
    CONSTRAINT pk_workflow_events PRIMARY KEY (id),
    CONSTRAINT fk_workflow_events_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_workflow_events_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id)
);

COMMENT ON TABLE workflow_events IS '病理流程轨迹事件表';
COMMENT ON COLUMN workflow_events.id IS '主键ID';
COMMENT ON COLUMN workflow_events.case_id IS '病例ID';
COMMENT ON COLUMN workflow_events.specimen_id IS '标本ID，可为空';
COMMENT ON COLUMN workflow_events.node_code IS '流程节点编码，示例：RECEIPT/REGISTRATION/SAMPLING/EMBEDDING/SLICING/STAINING/DIAGNOSIS/REVIEW/PUBLISH/ARCHIVE/REWORK';
COMMENT ON COLUMN workflow_events.action_code IS '动作编码，示例：CREATE/ASSIGN/ACCEPT/COMPLETE/REJECT/RETURN/AMEND/BORROW/BACK';
COMMENT ON COLUMN workflow_events.from_status IS '变更前状态';
COMMENT ON COLUMN workflow_events.to_status IS '变更后状态';
COMMENT ON COLUMN workflow_events.operator_user_id IS '操作人用户ID';
COMMENT ON COLUMN workflow_events.operator_name IS '操作人姓名快照';
COMMENT ON COLUMN workflow_events.occurred_at IS '发生时间';
COMMENT ON COLUMN workflow_events.remarks IS '备注';

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

-- =========================================================
-- 扩展流程
-- =========================================================
CREATE TABLE frozen_cases (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64) NOT NULL,
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
    CONSTRAINT pk_frozen_cases PRIMARY KEY (id),
    CONSTRAINT ck_frozen_cases_phone_back CHECK (intraoperative_phone_back IN (0, 1)),
    CONSTRAINT ck_frozen_cases_confirmed CHECK (final_confirmed_flag IN (0, 1)),
    CONSTRAINT uk_frozen_cases_case UNIQUE (case_id),
    CONSTRAINT fk_frozen_cases_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
);

COMMENT ON TABLE frozen_cases IS '冰冻流程表';
COMMENT ON COLUMN frozen_cases.id IS '主键ID';
COMMENT ON COLUMN frozen_cases.case_id IS '病例ID，一例最多一条冰冻流程主记录';
COMMENT ON COLUMN frozen_cases.request_time IS '冰冻申请时间';
COMMENT ON COLUMN frozen_cases.request_doctor_user_id IS '申请医生用户ID';
COMMENT ON COLUMN frozen_cases.request_doctor_name IS '申请医生姓名快照';
COMMENT ON COLUMN frozen_cases.intraoperative_phone_back IS '是否术中电话回报，0否1是';
COMMENT ON COLUMN frozen_cases.preliminary_result IS '术中初步结果';
COMMENT ON COLUMN frozen_cases.final_confirmed_flag IS '是否最终确认，0否1是';
COMMENT ON COLUMN frozen_cases.confirmed_by_user_id IS '最终确认人用户ID';
COMMENT ON COLUMN frozen_cases.confirmed_by_name IS '最终确认人姓名快照';
COMMENT ON COLUMN frozen_cases.final_confirmed_at IS '最终确认时间';
COMMENT ON COLUMN frozen_cases.remarks IS '备注';

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
COMMENT ON COLUMN rework_orders.rework_type IS '返工类型，示例：RECUT/RESTAIN/REEMBED/RESAMPLE/ADD_SLIDE';
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

CREATE TABLE medical_orders (
    id VARCHAR2(64) NOT NULL,
    case_id VARCHAR2(64),
    order_number VARCHAR2(64) NOT NULL,
    order_content VARCHAR2(1000) NOT NULL,
    order_type VARCHAR2(50) NOT NULL,
    status VARCHAR2(32) DEFAULT 'PENDING' NOT NULL,
    doctor_user_id VARCHAR2(64),
    doctor_name VARCHAR2(100) NOT NULL,
    doctor_department_id VARCHAR2(64),
    doctor_department_name VARCHAR2(100),
    order_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
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
COMMENT ON COLUMN medical_orders.status IS '医嘱状态，示例：PENDING/IN_PROGRESS/COMPLETED/CANCELLED';
COMMENT ON COLUMN medical_orders.doctor_user_id IS '开立医生用户ID';
COMMENT ON COLUMN medical_orders.doctor_name IS '开立医生姓名快照';
COMMENT ON COLUMN medical_orders.doctor_department_id IS '开立科室ID';
COMMENT ON COLUMN medical_orders.doctor_department_name IS '开立科室名称快照';
COMMENT ON COLUMN medical_orders.order_date IS '开立日期';
COMMENT ON COLUMN medical_orders.created_at IS '创建时间';
COMMENT ON COLUMN medical_orders.updated_at IS '更新时间';

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
