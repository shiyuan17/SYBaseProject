ALTER TABLE applications ADD COLUMN patient_name VARCHAR(100);
ALTER TABLE applications ADD COLUMN patient_gender VARCHAR(16);
ALTER TABLE applications ADD COLUMN patient_age VARCHAR(32);
ALTER TABLE applications ADD COLUMN source_hospital_id VARCHAR(64);
ALTER TABLE applications ADD COLUMN source_hospital_name VARCHAR(100);
ALTER TABLE applications ADD COLUMN submitting_department_id VARCHAR(64);
ALTER TABLE applications ADD COLUMN submitting_department_name VARCHAR(100);
ALTER TABLE applications ADD COLUMN submitting_doctor_user_id VARCHAR(64);
ALTER TABLE applications ADD COLUMN submitting_doctor_name VARCHAR(100);

CREATE TABLE pathology_cases (
    id VARCHAR(64) NOT NULL,
    application_id VARCHAR(64) NOT NULL,
    pathology_no VARCHAR(64) NOT NULL,
    case_status VARCHAR(32) NOT NULL,
    source_hospital_id VARCHAR(64),
    source_hospital_name VARCHAR(100),
    source_department_id VARCHAR(64),
    source_department_name VARCHAR(100),
    received_by_user_id VARCHAR(64),
    received_by_name VARCHAR(100),
    received_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_pathology_cases PRIMARY KEY (id),
    CONSTRAINT uk_pathology_cases_application UNIQUE (application_id),
    CONSTRAINT uk_pathology_cases_no UNIQUE (pathology_no),
    CONSTRAINT fk_pathology_cases_application FOREIGN KEY (application_id) REFERENCES applications (id)
);

CREATE TABLE specimens (
    id VARCHAR(64) NOT NULL,
    application_id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64),
    specimen_no VARCHAR(64) NOT NULL,
    barcode VARCHAR(128) NOT NULL,
    specimen_type VARCHAR(100),
    specimen_name_standardized VARCHAR(200),
    specimen_site VARCHAR(200),
    collection_mode VARCHAR(50),
    specimen_count INTEGER,
    specimen_status VARCHAR(32) NOT NULL,
    fixation_status VARCHAR(32),
    qualified_flag INTEGER DEFAULT 1,
    unqualified_reason VARCHAR(500),
    clinical_symptom VARCHAR(500),
    applicant_department_id VARCHAR(64),
    applicant_department_name VARCHAR(100),
    applicant_doctor_user_id VARCHAR(64),
    applicant_doctor_name VARCHAR(100),
    submission_date DATE,
    label_print_batch_no VARCHAR(64),
    label_print_status VARCHAR(32),
    registered_by_user_id VARCHAR(64),
    registered_by_name VARCHAR(100),
    registered_at TIMESTAMP,
    terminal_code VARCHAR(64),
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_specimens PRIMARY KEY (id),
    CONSTRAINT uk_specimens_application_specimen_no UNIQUE (application_id, specimen_no),
    CONSTRAINT uk_specimens_barcode UNIQUE (barcode),
    CONSTRAINT fk_specimens_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_specimens_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
);

CREATE TABLE specimen_collection_records (
    id VARCHAR(64) NOT NULL,
    application_id VARCHAR(64) NOT NULL,
    specimen_id VARCHAR(64) NOT NULL,
    collection_status VARCHAR(32) NOT NULL,
    collection_scene VARCHAR(100),
    collection_mode VARCHAR(50),
    label_print_batch_no VARCHAR(64),
    collector_user_id VARCHAR(64),
    collector_name VARCHAR(100),
    collected_at TIMESTAMP,
    terminal_code VARCHAR(64),
    remarks VARCHAR(500),
    CONSTRAINT pk_specimen_collection_records PRIMARY KEY (id),
    CONSTRAINT fk_specimen_collection_records_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_specimen_collection_records_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id)
);

CREATE TABLE specimen_fixation_records (
    id VARCHAR(64) NOT NULL,
    application_id VARCHAR(64) NOT NULL,
    specimen_id VARCHAR(64) NOT NULL,
    fixation_status VARCHAR(32) NOT NULL,
    fixation_liquid_type VARCHAR(100),
    fixation_start_at TIMESTAMP,
    fixation_completed_at TIMESTAMP,
    verified_by_user_id VARCHAR(64),
    verified_by_name VARCHAR(100),
    verified_at TIMESTAMP,
    terminal_code VARCHAR(64),
    remarks VARCHAR(500),
    CONSTRAINT pk_specimen_fixation_records PRIMARY KEY (id),
    CONSTRAINT uk_specimen_fixation_records_specimen UNIQUE (specimen_id),
    CONSTRAINT fk_specimen_fixation_records_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_specimen_fixation_records_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id)
);

CREATE TABLE transport_orders (
    id VARCHAR(64) NOT NULL,
    transport_order_no VARCHAR(64) NOT NULL,
    application_id VARCHAR(64) NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    handover_user_id VARCHAR(64),
    handover_user_name VARCHAR(100),
    handover_department_id VARCHAR(64),
    handover_department_name VARCHAR(100),
    receiver_department_id VARCHAR(64),
    receiver_department_name VARCHAR(100),
    receiver_user_id VARCHAR(64),
    receiver_user_name VARCHAR(100),
    printed_at TIMESTAMP,
    to_be_transported_at TIMESTAMP,
    handed_over_at TIMESTAMP,
    terminal_code VARCHAR(64),
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_transport_orders PRIMARY KEY (id),
    CONSTRAINT uk_transport_orders_no UNIQUE (transport_order_no),
    CONSTRAINT fk_transport_orders_application FOREIGN KEY (application_id) REFERENCES applications (id)
);

CREATE TABLE transport_order_items (
    id VARCHAR(64) NOT NULL,
    transport_order_id VARCHAR(64) NOT NULL,
    application_id VARCHAR(64) NOT NULL,
    specimen_id VARCHAR(64) NOT NULL,
    item_status VARCHAR(32) NOT NULL,
    verification_result VARCHAR(32),
    verified_by_user_id VARCHAR(64),
    verified_by_name VARCHAR(100),
    verified_at TIMESTAMP,
    remarks VARCHAR(500),
    CONSTRAINT pk_transport_order_items PRIMARY KEY (id),
    CONSTRAINT uk_transport_order_items_order_specimen UNIQUE (transport_order_id, specimen_id),
    CONSTRAINT fk_transport_order_items_order FOREIGN KEY (transport_order_id) REFERENCES transport_orders (id),
    CONSTRAINT fk_transport_order_items_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_transport_order_items_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id)
);

CREATE TABLE specimen_receipts (
    id VARCHAR(64) NOT NULL,
    application_id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64),
    specimen_id VARCHAR(64) NOT NULL,
    transport_order_id VARCHAR(64),
    receipt_status VARCHAR(32) NOT NULL,
    container_count INTEGER,
    barcode VARCHAR(128) NOT NULL,
    received_by_user_id VARCHAR(64),
    received_by_name VARCHAR(100),
    received_at TIMESTAMP,
    terminal_code VARCHAR(64),
    reject_reason VARCHAR(500),
    return_reason VARCHAR(500),
    remarks VARCHAR(500),
    CONSTRAINT pk_specimen_receipts PRIMARY KEY (id),
    CONSTRAINT fk_specimen_receipts_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_specimen_receipts_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_specimen_receipts_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_specimen_receipts_transport_order FOREIGN KEY (transport_order_id) REFERENCES transport_orders (id)
);

CREATE TABLE workflow_events (
    id VARCHAR(64) NOT NULL,
    application_id VARCHAR(64) NOT NULL,
    specimen_id VARCHAR(64),
    case_id VARCHAR(64),
    transport_order_id VARCHAR(64),
    node_code VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_status VARCHAR(32) NOT NULL,
    event_time TIMESTAMP NOT NULL,
    operator_user_id VARCHAR(64),
    operator_name VARCHAR(100),
    source_terminal VARCHAR(64),
    event_content VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_workflow_events PRIMARY KEY (id),
    CONSTRAINT fk_workflow_events_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_workflow_events_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_workflow_events_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_workflow_events_transport_order FOREIGN KEY (transport_order_id) REFERENCES transport_orders (id)
);

CREATE TABLE technical_pending_tasks (
    id VARCHAR(64) NOT NULL,
    application_id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    task_status VARCHAR(32) NOT NULL,
    payload VARCHAR(2000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_technical_pending_tasks PRIMARY KEY (id),
    CONSTRAINT uk_technical_pending_tasks_case_type UNIQUE (case_id, task_type),
    CONSTRAINT fk_technical_pending_tasks_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_technical_pending_tasks_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
);

INSERT INTO numbering_rules (id, rule_code, biz_type, prefix_pattern, date_pattern, seq_length, reset_policy, scope_type, remarks) VALUES
('NR_TRANSPORT', 'RULE_TRANSPORT_ORDER_NO', 'TRANSPORT_ORDER_NO', 'TR', 'yyyyMMdd', 4, 'DAILY', 'GLOBAL', '转运单号');
