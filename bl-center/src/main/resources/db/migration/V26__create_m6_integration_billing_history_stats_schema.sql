CREATE TABLE integration_tasks (
    id VARCHAR(64) NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id VARCHAR(64) NOT NULL,
    stage_code VARCHAR(64) NOT NULL,
    external_system VARCHAR(64),
    request_payload CLOB,
    response_payload CLOB,
    task_status VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    max_retry_count INT NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMP,
    last_attempt_at TIMESTAMP,
    last_error_code VARCHAR(64),
    last_error_message VARCHAR(1000),
    compensation_status VARCHAR(32) NOT NULL DEFAULT 'NONE',
    reconciliation_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    resolved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_integration_tasks PRIMARY KEY (id)
);

CREATE INDEX idx_integration_tasks_business ON integration_tasks (business_type, business_id);
CREATE INDEX idx_integration_tasks_status ON integration_tasks (task_status, next_retry_at);

CREATE TABLE billing_records (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64),
    billing_no VARCHAR(64) NOT NULL,
    billing_stage VARCHAR(50) NOT NULL,
    item_type VARCHAR(50),
    item_name VARCHAR(200),
    quantity DECIMAL(10, 2) DEFAULT 1,
    amount DECIMAL(12, 2),
    billing_status VARCHAR(32) NOT NULL,
    billed_at TIMESTAMP,
    operator_user_id VARCHAR(64),
    operator_name VARCHAR(100),
    external_bill_no VARCHAR(64),
    external_system VARCHAR(64),
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_billing_records PRIMARY KEY (id),
    CONSTRAINT uk_billing_records_billing_no UNIQUE (billing_no),
    CONSTRAINT fk_billing_records_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_billing_records_order FOREIGN KEY (order_id) REFERENCES medical_orders (id)
);

CREATE INDEX idx_billing_records_case_stage ON billing_records (case_id, billing_stage);
CREATE INDEX idx_billing_records_status_time ON billing_records (billing_status, billed_at);

CREATE TABLE historical_import_jobs (
    id VARCHAR(64) NOT NULL,
    source_system VARCHAR(64) NOT NULL,
    patient_id VARCHAR(64),
    pathology_no VARCHAR(64),
    application_no VARCHAR(64),
    import_status VARCHAR(32) NOT NULL,
    requested_by_user_id VARCHAR(64),
    requested_by_name VARCHAR(100),
    total_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    failure_count INT NOT NULL DEFAULT 0,
    requested_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    last_error_message VARCHAR(1000),
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_historical_import_jobs PRIMARY KEY (id)
);

CREATE INDEX idx_historical_import_jobs_source_status ON historical_import_jobs (source_system, import_status);

CREATE TABLE historical_reports (
    id VARCHAR(64) NOT NULL,
    import_job_id VARCHAR(64),
    source_system VARCHAR(64) NOT NULL,
    external_report_no VARCHAR(64) NOT NULL,
    patient_id VARCHAR(64),
    patient_name VARCHAR(100),
    pathology_no VARCHAR(64),
    application_no VARCHAR(64),
    report_date TIMESTAMP,
    final_diagnosis VARCHAR(1000),
    report_summary CLOB,
    raw_payload CLOB,
    source_department_name VARCHAR(100),
    source_doctor_name VARCHAR(100),
    attachment_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_historical_reports PRIMARY KEY (id),
    CONSTRAINT uk_historical_reports_source_report UNIQUE (source_system, external_report_no),
    CONSTRAINT fk_historical_reports_job FOREIGN KEY (import_job_id) REFERENCES historical_import_jobs (id)
);

CREATE INDEX idx_historical_reports_patient ON historical_reports (patient_id, report_date);
CREATE INDEX idx_historical_reports_pathology ON historical_reports (pathology_no, report_date);

CREATE TABLE historical_report_versions (
    id VARCHAR(64) NOT NULL,
    historical_report_id VARCHAR(64) NOT NULL,
    version_no INT NOT NULL,
    final_diagnosis VARCHAR(1000),
    report_summary CLOB,
    raw_payload CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_historical_report_versions PRIMARY KEY (id),
    CONSTRAINT uk_historical_report_versions UNIQUE (historical_report_id, version_no),
    CONSTRAINT fk_historical_report_versions_report FOREIGN KEY (historical_report_id) REFERENCES historical_reports (id)
);

CREATE TABLE stat_indicator_definitions (
    id VARCHAR(64) NOT NULL,
    indicator_code VARCHAR(64) NOT NULL,
    indicator_name VARCHAR(100) NOT NULL,
    indicator_category VARCHAR(32) NOT NULL,
    metric_scope VARCHAR(32),
    aggregation_type VARCHAR(32),
    description VARCHAR(500),
    sort_order INT NOT NULL DEFAULT 0,
    enabled INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_stat_indicator_definitions PRIMARY KEY (id),
    CONSTRAINT uk_stat_indicator_definitions_code UNIQUE (indicator_code)
);

CREATE TABLE stat_report_templates (
    id VARCHAR(64) NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    template_type VARCHAR(32) NOT NULL,
    indicator_code VARCHAR(64),
    default_columns CLOB,
    parameter_schema CLOB,
    sort_order INT NOT NULL DEFAULT 0,
    enabled INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_stat_report_templates PRIMARY KEY (id),
    CONSTRAINT uk_stat_report_templates_code UNIQUE (template_code)
);

CREATE TABLE stat_export_jobs (
    id VARCHAR(64) NOT NULL,
    export_no VARCHAR(64) NOT NULL,
    template_id VARCHAR(64),
    indicator_code VARCHAR(64),
    export_status VARCHAR(32) NOT NULL,
    filter_payload CLOB,
    file_name VARCHAR(255),
    content_type VARCHAR(100),
    requested_by_user_id VARCHAR(64),
    requested_by_name VARCHAR(100),
    error_message VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT pk_stat_export_jobs PRIMARY KEY (id),
    CONSTRAINT uk_stat_export_jobs_export_no UNIQUE (export_no),
    CONSTRAINT fk_stat_export_jobs_template FOREIGN KEY (template_id) REFERENCES stat_report_templates (id)
);
