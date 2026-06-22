ALTER TABLE medical_orders ADD COLUMN IF NOT EXISTS printed_at TIMESTAMP;
ALTER TABLE medical_orders ADD COLUMN IF NOT EXISTS printed_by_user_id VARCHAR(64);
ALTER TABLE medical_orders ADD COLUMN IF NOT EXISTS printed_by_name VARCHAR(100);
ALTER TABLE medical_orders ADD COLUMN IF NOT EXISTS released_at TIMESTAMP;
ALTER TABLE medical_orders ADD COLUMN IF NOT EXISTS released_by_user_id VARCHAR(64);
ALTER TABLE medical_orders ADD COLUMN IF NOT EXISTS released_by_name VARCHAR(100);
ALTER TABLE medical_orders ADD COLUMN IF NOT EXISTS terminated_at TIMESTAMP;
ALTER TABLE medical_orders ADD COLUMN IF NOT EXISTS terminated_by_user_id VARCHAR(64);
ALTER TABLE medical_orders ADD COLUMN IF NOT EXISTS terminated_by_name VARCHAR(100);
ALTER TABLE medical_orders ADD COLUMN IF NOT EXISTS termination_reason_code VARCHAR(64);
ALTER TABLE medical_orders ADD COLUMN IF NOT EXISTS termination_reason_label VARCHAR(200);
ALTER TABLE medical_orders ADD COLUMN IF NOT EXISTS target_type VARCHAR(32);
ALTER TABLE medical_orders ADD COLUMN IF NOT EXISTS target_specimen_id VARCHAR(64);
ALTER TABLE medical_orders ADD COLUMN IF NOT EXISTS target_specimen_no VARCHAR(64);
ALTER TABLE medical_orders ADD COLUMN IF NOT EXISTS target_block_id VARCHAR(64);
ALTER TABLE medical_orders ADD COLUMN IF NOT EXISTS target_block_no VARCHAR(64);
ALTER TABLE medical_orders ADD COLUMN IF NOT EXISTS target_slide_id VARCHAR(64);
ALTER TABLE medical_orders ADD COLUMN IF NOT EXISTS target_slide_no VARCHAR(64);

CREATE TABLE IF NOT EXISTS medical_order_qc_evaluations (
    id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    qc_aspect VARCHAR(32) NOT NULL,
    total_score INTEGER NOT NULL,
    grade VARCHAR(32) NOT NULL,
    evaluation_reason VARCHAR(1000),
    processing_action VARCHAR(32),
    rework_type VARCHAR(50),
    rework_order_id VARCHAR(64),
    remarks VARCHAR(500),
    evaluator_user_id VARCHAR(64),
    evaluator_name VARCHAR(100),
    evaluated_at TIMESTAMP,
    detail_payload_json CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_medical_order_qc_evaluations PRIMARY KEY (id),
    CONSTRAINT fk_medical_order_qc_evaluations_order FOREIGN KEY (order_id) REFERENCES medical_orders (id),
    CONSTRAINT fk_medical_order_qc_evaluations_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
);

CREATE INDEX IF NOT EXISTS idx_medical_orders_status_printed ON medical_orders (status, printed_at);
CREATE INDEX IF NOT EXISTS idx_medical_order_qc_evaluations_order_time ON medical_order_qc_evaluations (order_id, evaluated_at);

INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order)
SELECT 'PERM_M4_MEDICAL_ORDER_PRINT',
       'PERM_M4_MEDICAL_ORDER_PRINT',
       '打印玻片标签',
       'MENU_M4_MEDICAL_ORDER',
       'PRINT',
       'POST',
       '/api/v1/medical-orders/{id}/print-slide',
       'M4',
       1481
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE id = 'PERM_M4_MEDICAL_ORDER_PRINT'
);

INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order)
SELECT 'PERM_M4_MEDICAL_ORDER_TERMINATE',
       'PERM_M4_MEDICAL_ORDER_TERMINATE',
       '终止病理医嘱',
       'MENU_M4_MEDICAL_ORDER',
       'TERMINATE',
       'POST',
       '/api/v1/medical-orders/{id}/terminate',
       'M4',
       1482
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE id = 'PERM_M4_MEDICAL_ORDER_TERMINATE'
);

INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order)
SELECT 'PERM_M4_MEDICAL_ORDER_QC',
       'PERM_M4_MEDICAL_ORDER_QC',
       '病理医嘱质控评价',
       'MENU_M4_MEDICAL_ORDER',
       'QC',
       'POST',
       '/api/v1/medical-orders/{id}/qc-evaluations',
       'M4',
       1483
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE id = 'PERM_M4_MEDICAL_ORDER_QC'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M4_ADMIN_ORDER_PRINT',
       'ROLE_PATHOLOGY_ADMIN',
       'PERM_M4_MEDICAL_ORDER_PRINT',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_PATHOLOGY_ADMIN'
      AND permission_id = 'PERM_M4_MEDICAL_ORDER_PRINT'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M4_ADMIN_ORDER_TERMINATE',
       'ROLE_PATHOLOGY_ADMIN',
       'PERM_M4_MEDICAL_ORDER_TERMINATE',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_PATHOLOGY_ADMIN'
      AND permission_id = 'PERM_M4_MEDICAL_ORDER_TERMINATE'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M4_ADMIN_ORDER_QC',
       'ROLE_PATHOLOGY_ADMIN',
       'PERM_M4_MEDICAL_ORDER_QC',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_PATHOLOGY_ADMIN'
      AND permission_id = 'PERM_M4_MEDICAL_ORDER_QC'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M4_ORDER_EXEC_PRINT',
       'ROLE_M4_MEDICAL_ORDER_EXECUTE',
       'PERM_M4_MEDICAL_ORDER_PRINT',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_M4_MEDICAL_ORDER_EXECUTE'
      AND permission_id = 'PERM_M4_MEDICAL_ORDER_PRINT'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M4_ORDER_EXEC_TERMINATE',
       'ROLE_M4_MEDICAL_ORDER_EXECUTE',
       'PERM_M4_MEDICAL_ORDER_TERMINATE',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_M4_MEDICAL_ORDER_EXECUTE'
      AND permission_id = 'PERM_M4_MEDICAL_ORDER_TERMINATE'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M4_ORDER_EXEC_QC',
       'ROLE_M4_MEDICAL_ORDER_EXECUTE',
       'PERM_M4_MEDICAL_ORDER_QC',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_M4_MEDICAL_ORDER_EXECUTE'
      AND permission_id = 'PERM_M4_MEDICAL_ORDER_QC'
);
