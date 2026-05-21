CREATE TABLE report_revision_requests (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    report_id VARCHAR(64) NOT NULL,
    current_version_no INT NOT NULL,
    request_status VARCHAR(32) NOT NULL,
    request_reason VARCHAR(1000),
    requested_by_user_id VARCHAR(64),
    requested_by_name VARCHAR(100),
    requested_at TIMESTAMP,
    reviewed_by_user_id VARCHAR(64),
    reviewed_by_name VARCHAR(100),
    reviewed_at TIMESTAMP,
    reject_reason VARCHAR(1000),
    approved_version_no INT,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_report_revision_requests PRIMARY KEY (id),
    CONSTRAINT fk_report_revision_requests_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_report_revision_requests_report FOREIGN KEY (report_id) REFERENCES pathology_reports (id)
);

CREATE INDEX idx_report_revision_requests_case_status ON report_revision_requests (case_id, request_status);
CREATE INDEX idx_report_revision_requests_report_status ON report_revision_requests (report_id, request_status);

CREATE TABLE medical_orders (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    order_number VARCHAR(64) NOT NULL,
    order_content VARCHAR(1000) NOT NULL,
    order_type VARCHAR(64) NOT NULL,
    execution_scope VARCHAR(64) NOT NULL,
    billing_status VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    doctor_user_id VARCHAR(64),
    doctor_name VARCHAR(100),
    executor_user_id VARCHAR(64),
    executor_name VARCHAR(100),
    order_date TIMESTAMP,
    accepted_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_medical_orders PRIMARY KEY (id),
    CONSTRAINT uk_medical_orders_order_number UNIQUE (order_number),
    CONSTRAINT fk_medical_orders_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
);

CREATE INDEX idx_medical_orders_case_status ON medical_orders (case_id, status);
CREATE INDEX idx_medical_orders_status ON medical_orders (status);

CREATE TABLE consultation_cases (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    consultation_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    requested_by_user_id VARCHAR(64),
    requested_by_name VARCHAR(100),
    requested_at TIMESTAMP,
    host_user_id VARCHAR(64),
    host_name VARCHAR(100),
    opinion VARCHAR(2000),
    completed_at TIMESTAMP,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_consultation_cases PRIMARY KEY (id),
    CONSTRAINT fk_consultation_cases_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
);

CREATE INDEX idx_consultation_cases_case_status ON consultation_cases (case_id, status);

CREATE TABLE consultation_participants (
    id VARCHAR(64) NOT NULL,
    consultation_id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    participant_user_id VARCHAR(64) NOT NULL,
    participant_name VARCHAR(100),
    participant_role VARCHAR(32) NOT NULL,
    opinion VARCHAR(2000),
    drafted_by_user_id VARCHAR(64),
    drafted_by_name VARCHAR(100),
    read_flag INT DEFAULT 0 NOT NULL,
    read_at TIMESTAMP,
    commented_at TIMESTAMP,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_consultation_participants PRIMARY KEY (id),
    CONSTRAINT ck_consultation_participants_read_flag CHECK (read_flag IN (0, 1)),
    CONSTRAINT fk_consultation_participants_consultation FOREIGN KEY (consultation_id) REFERENCES consultation_cases (id),
    CONSTRAINT fk_consultation_participants_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
);

CREATE INDEX idx_consultation_participants_consultation ON consultation_participants (consultation_id);
CREATE INDEX idx_consultation_participants_user ON consultation_participants (participant_user_id);
