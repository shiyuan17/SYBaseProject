INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order)
SELECT 'PERM_M5_LOAN_APPROVE',
       'PERM_M5_LOAN_APPROVE',
       '审批借阅申请',
       'MENU_M5_ARCHIVE',
       'LOAN_APPROVE',
       'POST',
       '/api/v1/material-loans/{id}/approve',
       'M5',
       182
WHERE NOT EXISTS (
    SELECT 1
    FROM permissions
    WHERE id = 'PERM_M5_LOAN_APPROVE'
);

INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order)
SELECT 'PERM_M5_LOAN_REJECT',
       'PERM_M5_LOAN_REJECT',
       '驳回借阅申请',
       'MENU_M5_ARCHIVE',
       'LOAN_REJECT',
       'POST',
       '/api/v1/material-loans/{id}/reject',
       'M5',
       183
WHERE NOT EXISTS (
    SELECT 1
    FROM permissions
    WHERE id = 'PERM_M5_LOAN_REJECT'
);

INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order)
SELECT 'PERM_M5_LOAN_BORROW',
       'PERM_M5_LOAN_BORROW',
       '执行借出',
       'MENU_M5_ARCHIVE',
       'LOAN_BORROW',
       'POST',
       '/api/v1/material-loans/{id}/borrow',
       'M5',
       184
WHERE NOT EXISTS (
    SELECT 1
    FROM permissions
    WHERE id = 'PERM_M5_LOAN_BORROW'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M5_ADMIN_LOAN_APPROVE',
       'ROLE_PATHOLOGY_ADMIN',
       'PERM_M5_LOAN_APPROVE',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_PATHOLOGY_ADMIN'
      AND permission_id = 'PERM_M5_LOAN_APPROVE'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M5_ADMIN_LOAN_REJECT',
       'ROLE_PATHOLOGY_ADMIN',
       'PERM_M5_LOAN_REJECT',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_PATHOLOGY_ADMIN'
      AND permission_id = 'PERM_M5_LOAN_REJECT'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M5_ADMIN_LOAN_BORROW',
       'ROLE_PATHOLOGY_ADMIN',
       'PERM_M5_LOAN_BORROW',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_PATHOLOGY_ADMIN'
      AND permission_id = 'PERM_M5_LOAN_BORROW'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M5_ARCHIVE_LOAN_APPROVE',
       'ROLE_ARCHIVE_MANAGER',
       'PERM_M5_LOAN_APPROVE',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_ARCHIVE_MANAGER'
      AND permission_id = 'PERM_M5_LOAN_APPROVE'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M5_ARCHIVE_LOAN_REJECT',
       'ROLE_ARCHIVE_MANAGER',
       'PERM_M5_LOAN_REJECT',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_ARCHIVE_MANAGER'
      AND permission_id = 'PERM_M5_LOAN_REJECT'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M5_ARCHIVE_LOAN_BORROW',
       'ROLE_ARCHIVE_MANAGER',
       'PERM_M5_LOAN_BORROW',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_ARCHIVE_MANAGER'
      AND permission_id = 'PERM_M5_LOAN_BORROW'
);
