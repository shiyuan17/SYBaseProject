CREATE TABLE diagnostic_tasks (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    specimen_id VARCHAR(64),
    pathology_no VARCHAR(64),
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(32) DEFAULT 'NORMAL',
    urgent_reason VARCHAR(500),
    assignment_mode VARCHAR(32),
    assigned_by_user_id VARCHAR(64),
    assigned_by_name VARCHAR(100),
    diagnosis_doctor_user_id VARCHAR(64),
    diagnosis_doctor_name VARCHAR(100),
    primary_doctor_user_id VARCHAR(64),
    primary_doctor_name VARCHAR(100),
    primary_diagnosed_at TIMESTAMP,
    review_doctor_user_id VARCHAR(64),
    review_doctor_name VARCHAR(100),
    review_completed_at TIMESTAMP,
    review_level VARCHAR(32),
    reviewer_user_id VARCHAR(64),
    reviewer_name VARCHAR(100),
    reviewed_at TIMESTAMP,
    assigned_at TIMESTAMP,
    accepted_at TIMESTAMP,
    completed_at TIMESTAMP,
    sla_due_at TIMESTAMP,
    frozen_diagnosis_result VARCHAR(1000),
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_diagnostic_tasks PRIMARY KEY (id),
    CONSTRAINT fk_diagnostic_tasks_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_diagnostic_tasks_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id)
);

CREATE INDEX idx_diagnostic_tasks_case_status ON diagnostic_tasks (case_id, status);
CREATE INDEX idx_diagnostic_tasks_pathology_status ON diagnostic_tasks (pathology_no, status);

CREATE TABLE pathology_reports (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64),
    report_no VARCHAR(64) NOT NULL,
    pathology_no VARCHAR(64),
    report_scope VARCHAR(50) DEFAULT 'ROUTINE',
    report_seq INT DEFAULT 1 NOT NULL,
    report_status VARCHAR(32) NOT NULL,
    version_no INT DEFAULT 1 NOT NULL,
    specimen_type VARCHAR(100),
    patient_name VARCHAR(100),
    submitting_department_id VARCHAR(64),
    submitting_department_name VARCHAR(100),
    report_date TIMESTAMP,
    gross_exam TEXT,
    microscopic_exam TEXT,
    clinical_diagnosis VARCHAR(500),
    final_diagnosis VARCHAR(2000),
    submitted_at TIMESTAMP,
    reviewer_user_id VARCHAR(64),
    reviewer_name VARCHAR(100),
    reviewed_at TIMESTAMP,
    signed_by_user_id VARCHAR(64),
    signed_by_name VARCHAR(100),
    signed_at TIMESTAMP,
    published_at TIMESTAMP,
    rich_text_content TEXT,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_pathology_reports PRIMARY KEY (id),
    CONSTRAINT uk_pathology_reports_case_scope_seq UNIQUE (case_id, report_scope, report_seq),
    CONSTRAINT uk_pathology_reports_report_no UNIQUE (report_no),
    CONSTRAINT fk_pathology_reports_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_pathology_reports_task FOREIGN KEY (task_id) REFERENCES diagnostic_tasks (id)
);

CREATE INDEX idx_pathology_reports_case_status ON pathology_reports (case_id, report_status);

CREATE TABLE report_versions (
    id VARCHAR(64) NOT NULL,
    report_id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    report_scope VARCHAR(50),
    report_seq INT,
    version_no INT NOT NULL,
    version_status VARCHAR(32) NOT NULL,
    final_diagnosis_snapshot VARCHAR(2000),
    content_snapshot TEXT,
    signed_by_user_id VARCHAR(64),
    signed_by_name VARCHAR(100),
    signed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_report_versions PRIMARY KEY (id),
    CONSTRAINT uk_report_versions_report_version UNIQUE (report_id, version_no, version_status),
    CONSTRAINT fk_report_versions_report FOREIGN KEY (report_id) REFERENCES pathology_reports (id),
    CONSTRAINT fk_report_versions_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
);

CREATE INDEX idx_report_versions_report_id ON report_versions (report_id, version_no);

INSERT INTO numbering_rules (id, rule_code, biz_type, prefix_pattern, date_pattern, seq_length, reset_policy, scope_type, remarks)
VALUES ('NR_REPORT', 'RULE_REPORT_NO', 'REPORT_NO', 'RP', 'yyyyMMdd', 4, 'DAILY', 'GLOBAL', '报告编号');
