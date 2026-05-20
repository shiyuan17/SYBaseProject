INSERT INTO roles (id, role_code, role_name, enabled) VALUES
('ROLE_AUTH_ADMIN', 'AUTH_ADMIN', 'Auth Admin', 1);

INSERT INTO permissions (id, permission_code, permission_name, enabled) VALUES
('PERM_AUTH_SYSTEM_USER_QUERY', 'PERM_SYSTEM_USER_QUERY', 'System user query', 1),
('PERM_SYS_ORDER_DICT_QUERY', 'PERM_SYS_ORDER_DICT_QUERY', 'Medical order dict query', 1),
('PERM_LEGACY_ORDER_DICT_QUERY', 'sys:medical-order-dict:query', 'Legacy medical order dict query', 0);

INSERT INTO role_permissions (id, role_id, permission_id) VALUES
('RP_AUTH_ADMIN_QUERY', 'ROLE_AUTH_ADMIN', 'PERM_AUTH_SYSTEM_USER_QUERY'),
('RP_AUTH_ADMIN_ORDER_DICT_QUERY', 'ROLE_AUTH_ADMIN', 'PERM_SYS_ORDER_DICT_QUERY'),
('RP_AUTH_ADMIN_LEGACY_ORDER_DICT_QUERY', 'ROLE_AUTH_ADMIN', 'PERM_LEGACY_ORDER_DICT_QUERY');

INSERT INTO users
    (id, login_name, name, password, password_algo, password_salt, avatar, enabled, created_at, updated_at)
VALUES
    ('AUTH_USER_PLAIN', 'auth.plain', 'Auth Plain User', '123456', 'PLAIN', NULL, NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('AUTH_USER_FAIL', 'auth.fail', 'Auth Fail User', '123456', 'PLAIN', NULL, NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('AUTH_USER_LOCK', 'auth.lock', 'Auth Lock User', '123456', 'PLAIN', NULL, NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('AUTH_USER_API', 'auth.api', 'Auth Api User', '123456', 'PLAIN', NULL, NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('AUTH_USER_DISABLED', 'auth.disabled', 'Auth Disabled User', '123456', 'PLAIN', NULL, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO user_roles (id, user_id, role_id) VALUES
('UR_AUTH_PLAIN', 'AUTH_USER_PLAIN', 'ROLE_AUTH_ADMIN'),
('UR_AUTH_FAIL', 'AUTH_USER_FAIL', 'ROLE_AUTH_ADMIN'),
('UR_AUTH_LOCK', 'AUTH_USER_LOCK', 'ROLE_AUTH_ADMIN'),
('UR_AUTH_API', 'AUTH_USER_API', 'ROLE_AUTH_ADMIN'),
('UR_AUTH_DISABLED', 'AUTH_USER_DISABLED', 'ROLE_AUTH_ADMIN');
