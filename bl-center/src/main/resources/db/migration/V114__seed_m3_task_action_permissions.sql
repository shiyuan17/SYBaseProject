INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order)
SELECT 'PERM_M3_TECH_TASK_ASSIGN',
       'PERM_M3_TECH_TASK_ASSIGN',
       '分派技术任务',
       'MENU_M3_TASKS',
       'ASSIGN',
       'POST',
       '/api/v1/technical-tasks/{id}/assign',
       'M3',
       1281
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE id = 'PERM_M3_TECH_TASK_ASSIGN'
);

INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order)
SELECT 'PERM_M3_TECH_TASK_CLAIM',
       'PERM_M3_TECH_TASK_CLAIM',
       '领取技术任务',
       'MENU_M3_TASKS',
       'CLAIM',
       'POST',
       '/api/v1/technical-tasks/{id}/claim',
       'M3',
       1282
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE id = 'PERM_M3_TECH_TASK_CLAIM'
);

INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order)
SELECT 'PERM_M3_TECH_TASK_RELEASE',
       'PERM_M3_TECH_TASK_RELEASE',
       '释放技术任务',
       'MENU_M3_TASKS',
       'RELEASE',
       'POST',
       '/api/v1/technical-tasks/{id}/release',
       'M3',
       1283
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE id = 'PERM_M3_TECH_TASK_RELEASE'
);

INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order)
SELECT 'PERM_M3_TECH_TASK_PRIORITY',
       'PERM_M3_TECH_TASK_PRIORITY',
       '调整任务优先级',
       'MENU_M3_TASKS',
       'PRIORITY',
       'POST',
       '/api/v1/technical-tasks/{id}/priority',
       'M3',
       1284
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE id = 'PERM_M3_TECH_TASK_PRIORITY'
);

INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order)
SELECT 'PERM_M3_TECH_TASK_REMARKS',
       'PERM_M3_TECH_TASK_REMARKS',
       '编辑任务备注',
       'MENU_M3_TASKS',
       'REMARKS',
       'PATCH',
       '/api/v1/technical-tasks/{id}/remarks',
       'M3',
       1285
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE id = 'PERM_M3_TECH_TASK_REMARKS'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M3_ADMIN_TASK_ASSIGN',
       'ROLE_PATHOLOGY_ADMIN',
       'PERM_M3_TECH_TASK_ASSIGN',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_PATHOLOGY_ADMIN'
      AND permission_id = 'PERM_M3_TECH_TASK_ASSIGN'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M3_ADMIN_TASK_CLAIM',
       'ROLE_PATHOLOGY_ADMIN',
       'PERM_M3_TECH_TASK_CLAIM',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_PATHOLOGY_ADMIN'
      AND permission_id = 'PERM_M3_TECH_TASK_CLAIM'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M3_ADMIN_TASK_RELEASE',
       'ROLE_PATHOLOGY_ADMIN',
       'PERM_M3_TECH_TASK_RELEASE',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_PATHOLOGY_ADMIN'
      AND permission_id = 'PERM_M3_TECH_TASK_RELEASE'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M3_ADMIN_TASK_PRIORITY',
       'ROLE_PATHOLOGY_ADMIN',
       'PERM_M3_TECH_TASK_PRIORITY',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_PATHOLOGY_ADMIN'
      AND permission_id = 'PERM_M3_TECH_TASK_PRIORITY'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT 'RP_M3_ADMIN_TASK_REMARKS',
       'ROLE_PATHOLOGY_ADMIN',
       'PERM_M3_TECH_TASK_REMARKS',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = 'ROLE_PATHOLOGY_ADMIN'
      AND permission_id = 'PERM_M3_TECH_TASK_REMARKS'
);

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at)
SELECT role_permission_id,
       role_id,
       permission_id,
       CURRENT_TIMESTAMP
FROM (
    SELECT 'RP_M3_GROSSING_TASK_CLAIM' AS role_permission_id, 'ROLE_M3_GROSSING' AS role_id, 'PERM_M3_TECH_TASK_CLAIM' AS permission_id
    UNION ALL
    SELECT 'RP_M3_DEHYDRATION_TASK_CLAIM', 'ROLE_M3_DEHYDRATION', 'PERM_M3_TECH_TASK_CLAIM'
    UNION ALL
    SELECT 'RP_M3_EMBEDDING_TASK_CLAIM', 'ROLE_M3_EMBEDDING', 'PERM_M3_TECH_TASK_CLAIM'
    UNION ALL
    SELECT 'RP_M3_SLICING_TASK_CLAIM', 'ROLE_M3_SLICING', 'PERM_M3_TECH_TASK_CLAIM'
    UNION ALL
    SELECT 'RP_M3_STAINING_TASK_CLAIM', 'ROLE_M3_STAINING', 'PERM_M3_TECH_TASK_CLAIM'
    UNION ALL
    SELECT 'RP_M3_REWORK_TASK_CLAIM', 'ROLE_M3_REWORK', 'PERM_M3_TECH_TASK_CLAIM'
    UNION ALL
    SELECT 'RP_M3_GROSSING_TASK_RELEASE', 'ROLE_M3_GROSSING', 'PERM_M3_TECH_TASK_RELEASE'
    UNION ALL
    SELECT 'RP_M3_DEHYDRATION_TASK_RELEASE', 'ROLE_M3_DEHYDRATION', 'PERM_M3_TECH_TASK_RELEASE'
    UNION ALL
    SELECT 'RP_M3_EMBEDDING_TASK_RELEASE', 'ROLE_M3_EMBEDDING', 'PERM_M3_TECH_TASK_RELEASE'
    UNION ALL
    SELECT 'RP_M3_SLICING_TASK_RELEASE', 'ROLE_M3_SLICING', 'PERM_M3_TECH_TASK_RELEASE'
    UNION ALL
    SELECT 'RP_M3_STAINING_TASK_RELEASE', 'ROLE_M3_STAINING', 'PERM_M3_TECH_TASK_RELEASE'
    UNION ALL
    SELECT 'RP_M3_REWORK_TASK_RELEASE', 'ROLE_M3_REWORK', 'PERM_M3_TECH_TASK_RELEASE'
    UNION ALL
    SELECT 'RP_M3_GROSSING_TASK_REMARKS', 'ROLE_M3_GROSSING', 'PERM_M3_TECH_TASK_REMARKS'
    UNION ALL
    SELECT 'RP_M3_DEHYDRATION_TASK_REMARKS', 'ROLE_M3_DEHYDRATION', 'PERM_M3_TECH_TASK_REMARKS'
    UNION ALL
    SELECT 'RP_M3_EMBEDDING_TASK_REMARKS', 'ROLE_M3_EMBEDDING', 'PERM_M3_TECH_TASK_REMARKS'
    UNION ALL
    SELECT 'RP_M3_SLICING_TASK_REMARKS', 'ROLE_M3_SLICING', 'PERM_M3_TECH_TASK_REMARKS'
    UNION ALL
    SELECT 'RP_M3_STAINING_TASK_REMARKS', 'ROLE_M3_STAINING', 'PERM_M3_TECH_TASK_REMARKS'
    UNION ALL
    SELECT 'RP_M3_REWORK_TASK_REMARKS', 'ROLE_M3_REWORK', 'PERM_M3_TECH_TASK_REMARKS'
) grants
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_id = grants.role_id
      AND permission_id = grants.permission_id
);
