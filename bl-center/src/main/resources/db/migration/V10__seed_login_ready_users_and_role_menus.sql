UPDATE users
SET password = '0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134',
    password_algo = 'SM3',
    password_salt = '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1',
    updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    'USER_M1_ADMIN',
    'USER_M1_DOCTOR',
    'USER_M1_TECHNICIAN',
    'USER_M1_ARCHIVE',
    'USER_M1_REAGENT',
    'USER_M1_QUALITY',
    'USER_M1_NO_PERMISSION',
    'USER_M2_ADMIN',
    'USER_M2_REGISTER',
    'USER_M2_FIXATION',
    'USER_M2_TRANSPORT',
    'USER_M2_RECEIVE',
    'USER_M2_TRACKING',
    'USER_M2_IMPORT',
    'USER_M2_NO_PERMISSION',
    'USER_M3_GROSSING',
    'USER_M3_DEHYDRATION',
    'USER_M3_EMBEDDING',
    'USER_M3_SLICING',
    'USER_M3_STAINING',
    'USER_M3_REWORK',
    'USER_M3_TRACKING'
);

DELETE FROM role_menus
WHERE role_id IN (
    'ROLE_PATHOLOGY_ADMIN',
    'ROLE_PATHOLOGY_DOCTOR',
    'ROLE_PATHOLOGY_TECHNICIAN',
    'ROLE_ARCHIVE_MANAGER',
    'ROLE_REAGENT_DEVICE_MANAGER',
    'ROLE_QUALITY_MANAGER'
)
   OR role_id LIKE 'ROLE_M2_%'
   OR role_id LIKE 'ROLE_M3_%';

INSERT INTO role_menus (id, role_id, menu_id, assigned_at)
SELECT 'RM_ADMIN_' || menu_code, 'ROLE_PATHOLOGY_ADMIN', id, CURRENT_TIMESTAMP
FROM menus
WHERE id IN ('MENU_SYSTEM', 'MENU_M2_WORKFLOW', 'MENU_M3_WORKFLOW')
   OR parent_id IN ('MENU_SYSTEM', 'MENU_M2_WORKFLOW', 'MENU_M3_WORKFLOW');

INSERT INTO role_menus (id, role_id, menu_id, assigned_at)
SELECT DISTINCT
    'RM_' || roles.role_code || '_' || menus.menu_code,
    roles.id,
    menus.id,
    CURRENT_TIMESTAMP
FROM roles
JOIN role_permissions ON role_permissions.role_id = roles.id
JOIN permissions ON permissions.id = role_permissions.permission_id
JOIN menus ON menus.id = permissions.menu_id
WHERE roles.id LIKE 'ROLE_M2_%';

INSERT INTO role_menus (id, role_id, menu_id, assigned_at)
SELECT DISTINCT
    'RM_' || roles.role_code || '_M2_WORKFLOW',
    roles.id,
    'MENU_M2_WORKFLOW',
    CURRENT_TIMESTAMP
FROM roles
WHERE roles.id LIKE 'ROLE_M2_%'
  AND EXISTS (
      SELECT 1
      FROM role_permissions
      WHERE role_permissions.role_id = roles.id
  );

INSERT INTO role_menus (id, role_id, menu_id, assigned_at)
SELECT DISTINCT
    'RM_' || roles.role_code || '_' || menus.menu_code,
    roles.id,
    menus.id,
    CURRENT_TIMESTAMP
FROM roles
JOIN role_permissions ON role_permissions.role_id = roles.id
JOIN permissions ON permissions.id = role_permissions.permission_id
JOIN menus ON menus.id = permissions.menu_id
WHERE roles.id LIKE 'ROLE_M3_%';

INSERT INTO role_menus (id, role_id, menu_id, assigned_at)
SELECT DISTINCT
    'RM_' || roles.role_code || '_M3_WORKFLOW',
    roles.id,
    'MENU_M3_WORKFLOW',
    CURRENT_TIMESTAMP
FROM roles
WHERE roles.id LIKE 'ROLE_M3_%'
  AND EXISTS (
      SELECT 1
      FROM role_permissions
      WHERE role_permissions.role_id = roles.id
  );
