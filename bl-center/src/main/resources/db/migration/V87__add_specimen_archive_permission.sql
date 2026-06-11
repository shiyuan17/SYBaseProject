INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order)
SELECT 'PERM_M5_SPECIMEN_ARCHIVE',
       'PERM_M5_SPECIMEN_ARCHIVE',
       '标本归档',
       'MENU_M5_ARCHIVE',
       'SPECIMEN_ARCHIVE',
       'POST',
       '/api/v1/archive/specimens',
       'M5',
       166
WHERE NOT EXISTS (
    SELECT 1
    FROM permissions
    WHERE id = 'PERM_M5_SPECIMEN_ARCHIVE'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M5_ADMIN_SPECIMEN_ARCHIVE',
       'ROLE_PATHOLOGY_ADMIN',
       'PERM_M5_SPECIMEN_ARCHIVE',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_PATHOLOGY_ADMIN'
      AND permission_id = 'PERM_M5_SPECIMEN_ARCHIVE'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M5_ARCHIVE_SPECIMEN_ARCHIVE',
       'ROLE_ARCHIVE_MANAGER',
       'PERM_M5_SPECIMEN_ARCHIVE',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_ARCHIVE_MANAGER'
      AND permission_id = 'PERM_M5_SPECIMEN_ARCHIVE'
);
