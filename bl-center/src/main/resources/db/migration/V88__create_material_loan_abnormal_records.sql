CREATE TABLE material_loan_abnormal_records (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    material_type VARCHAR(32) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    loan_id VARCHAR(64),
    abnormal_reason VARCHAR(1000) NOT NULL,
    contacted INTEGER DEFAULT 0,
    contact_result VARCHAR(1000),
    borrowed_slide_no VARCHAR(100),
    borrower_name VARCHAR(100),
    borrower_relationship VARCHAR(100),
    borrower_phone VARCHAR(100),
    borrower_unit VARCHAR(200),
    borrower_identity_no VARCHAR(100),
    borrowed_at TIMESTAMP,
    expected_return_at TIMESTAMP,
    slide_count INTEGER,
    deposit_amount DECIMAL(18, 2),
    borrowed_content VARCHAR(1000),
    return_abnormal_info VARCHAR(1000),
    registered_by_user_id VARCHAR(64),
    registered_by_name VARCHAR(100),
    registered_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_material_loan_abnormal_records PRIMARY KEY (id),
    CONSTRAINT fk_material_loan_abnormal_records_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_material_loan_abnormal_records_loan FOREIGN KEY (loan_id) REFERENCES material_loans (id)
);

CREATE INDEX idx_material_loan_abnormal_records_material
    ON material_loan_abnormal_records (material_type, material_id);

CREATE INDEX idx_material_loan_abnormal_records_case
    ON material_loan_abnormal_records (case_id);

INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order)
SELECT 'PERM_M5_LOAN_ABNORMAL_REGISTER',
       'PERM_M5_LOAN_ABNORMAL_REGISTER',
       '借阅异常登记',
       'MENU_M5_ARCHIVE',
       'LOAN_ABNORMAL_REGISTER',
       'POST',
       '/api/v1/material-loans/abnormal-records',
       'M5',
       171
WHERE NOT EXISTS (
    SELECT 1
    FROM permissions
    WHERE id = 'PERM_M5_LOAN_ABNORMAL_REGISTER'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M5_ADMIN_LOAN_ABNORMAL_REGISTER',
       'ROLE_PATHOLOGY_ADMIN',
       'PERM_M5_LOAN_ABNORMAL_REGISTER',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_PATHOLOGY_ADMIN'
      AND permission_id = 'PERM_M5_LOAN_ABNORMAL_REGISTER'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M5_ARCHIVE_LOAN_ABNORMAL_REGISTER',
       'ROLE_ARCHIVE_MANAGER',
       'PERM_M5_LOAN_ABNORMAL_REGISTER',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_ARCHIVE_MANAGER'
      AND permission_id = 'PERM_M5_LOAN_ABNORMAL_REGISTER'
);
