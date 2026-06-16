CREATE TABLE IF NOT EXISTS white_slide_stocks (
    id VARCHAR(64) NOT NULL,
    stock_no VARCHAR(64) NOT NULL,
    stock_code VARCHAR(64) NOT NULL,
    specification VARCHAR(100),
    quantity_available INTEGER NOT NULL DEFAULT 0,
    quantity_borrowed INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_white_slide_stocks PRIMARY KEY (id),
    CONSTRAINT uk_white_slide_stocks_stock_no UNIQUE (stock_no),
    CONSTRAINT uk_white_slide_stocks_stock_code UNIQUE (stock_code)
);

CREATE TABLE IF NOT EXISTS white_slide_loans (
    id VARCHAR(64) NOT NULL,
    loan_no VARCHAR(64) NOT NULL,
    stock_id VARCHAR(64) NOT NULL,
    quantity INTEGER NOT NULL,
    case_id VARCHAR(64),
    pathology_no VARCHAR(64),
    patient_name VARCHAR(100),
    embedding_box_no VARCHAR(64),
    slice_purpose VARCHAR(500),
    slice_thickness VARCHAR(100),
    borrower_name VARCHAR(100) NOT NULL,
    borrower_identity_no VARCHAR(64),
    borrower_unit VARCHAR(200),
    borrower_phone VARCHAR(32),
    unit_price DECIMAL(18, 2),
    amount DECIMAL(18, 2),
    save_direct_print INTEGER DEFAULT 0,
    loan_status VARCHAR(32) NOT NULL DEFAULT 'BORROWED',
    wax_block_usage VARCHAR(500),
    operator_user_id VARCHAR(64),
    operator_name VARCHAR(100) NOT NULL,
    loaned_at TIMESTAMP NOT NULL,
    returned_at TIMESTAMP,
    returned_by_user_id VARCHAR(64),
    returned_by_name VARCHAR(100),
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_white_slide_loans PRIMARY KEY (id),
    CONSTRAINT uk_white_slide_loans_loan_no UNIQUE (loan_no),
    CONSTRAINT fk_white_slide_loans_stock FOREIGN KEY (stock_id) REFERENCES white_slide_stocks (id),
    CONSTRAINT fk_white_slide_loans_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
);

CREATE INDEX IF NOT EXISTS idx_white_slide_loans_status
    ON white_slide_loans (loan_status, loaned_at);

CREATE INDEX IF NOT EXISTS idx_white_slide_loans_case
    ON white_slide_loans (case_id, pathology_no);

INSERT INTO white_slide_stocks (id, stock_no, stock_code, specification, quantity_available, quantity_borrowed, status, remarks)
SELECT 'WS-STOCK-DEFAULT',
       'WS-DEFAULT',
       'WHITE-SLIDE-DEFAULT',
       '默认白片库存',
       1000,
       0,
       'ACTIVE',
       '系统默认白片库存池'
WHERE NOT EXISTS (
    SELECT 1 FROM white_slide_stocks WHERE id = 'WS-STOCK-DEFAULT'
);

INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order)
SELECT 'PERM_M5_WHITE_SLIDE_QUERY',
       'PERM_M5_WHITE_SLIDE_QUERY',
       '查询白片借记',
       'MENU_M5_ARCHIVE',
       'WHITE_SLIDE_QUERY',
       'GET',
       '/api/v1/white-slide-loans',
       'M5',
       172
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE id = 'PERM_M5_WHITE_SLIDE_QUERY'
);

INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order)
SELECT 'PERM_M5_WHITE_SLIDE_CREATE',
       'PERM_M5_WHITE_SLIDE_CREATE',
       '创建白片借记',
       'MENU_M5_ARCHIVE',
       'WHITE_SLIDE_CREATE',
       'POST',
       '/api/v1/white-slide-loans',
       'M5',
       173
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE id = 'PERM_M5_WHITE_SLIDE_CREATE'
);

INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order)
SELECT 'PERM_M5_WHITE_SLIDE_RETURN',
       'PERM_M5_WHITE_SLIDE_RETURN',
       '归还白片借记',
       'MENU_M5_ARCHIVE',
       'WHITE_SLIDE_RETURN',
       'POST',
       '/api/v1/white-slide-loans/{id}/return',
       'M5',
       174
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE id = 'PERM_M5_WHITE_SLIDE_RETURN'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M5_ADMIN_WHITE_SLIDE_QUERY',
       'ROLE_PATHOLOGY_ADMIN',
       'PERM_M5_WHITE_SLIDE_QUERY',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions
    WHERE role_id = 'ROLE_PATHOLOGY_ADMIN'
      AND permission_id = 'PERM_M5_WHITE_SLIDE_QUERY'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M5_ADMIN_WHITE_SLIDE_CREATE',
       'ROLE_PATHOLOGY_ADMIN',
       'PERM_M5_WHITE_SLIDE_CREATE',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions
    WHERE role_id = 'ROLE_PATHOLOGY_ADMIN'
      AND permission_id = 'PERM_M5_WHITE_SLIDE_CREATE'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M5_ADMIN_WHITE_SLIDE_RETURN',
       'ROLE_PATHOLOGY_ADMIN',
       'PERM_M5_WHITE_SLIDE_RETURN',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions
    WHERE role_id = 'ROLE_PATHOLOGY_ADMIN'
      AND permission_id = 'PERM_M5_WHITE_SLIDE_RETURN'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M5_ARCHIVE_WHITE_SLIDE_QUERY',
       'ROLE_ARCHIVE_MANAGER',
       'PERM_M5_WHITE_SLIDE_QUERY',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions
    WHERE role_id = 'ROLE_ARCHIVE_MANAGER'
      AND permission_id = 'PERM_M5_WHITE_SLIDE_QUERY'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M5_ARCHIVE_WHITE_SLIDE_CREATE',
       'ROLE_ARCHIVE_MANAGER',
       'PERM_M5_WHITE_SLIDE_CREATE',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions
    WHERE role_id = 'ROLE_ARCHIVE_MANAGER'
      AND permission_id = 'PERM_M5_WHITE_SLIDE_CREATE'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M5_ARCHIVE_WHITE_SLIDE_RETURN',
       'ROLE_ARCHIVE_MANAGER',
       'PERM_M5_WHITE_SLIDE_RETURN',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions
    WHERE role_id = 'ROLE_ARCHIVE_MANAGER'
      AND permission_id = 'PERM_M5_WHITE_SLIDE_RETURN'
);
