CREATE TABLE users (
    id VARCHAR(64) NOT NULL,
    login_name VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    password VARCHAR(255),
    password_algo VARCHAR(32),
    password_salt VARCHAR(64),
    avatar VARCHAR(500),
    enabled INTEGER DEFAULT 1,
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(64),
    last_login_device VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_login_name UNIQUE (login_name)
);

CREATE TABLE roles (
    id VARCHAR(64) NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    enabled INTEGER DEFAULT 1,
    CONSTRAINT pk_roles PRIMARY KEY (id)
);

CREATE TABLE permissions (
    id VARCHAR(64) NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    enabled INTEGER DEFAULT 1,
    CONSTRAINT pk_permissions PRIMARY KEY (id)
);

CREATE TABLE user_roles (
    id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    role_id VARCHAR(64) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (id)
);

CREATE TABLE role_permissions (
    id VARCHAR(64) NOT NULL,
    role_id VARCHAR(64) NOT NULL,
    permission_id VARCHAR(64) NOT NULL,
    CONSTRAINT pk_role_permissions PRIMARY KEY (id)
);

CREATE TABLE user_login_logs (
    id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64),
    login_name VARCHAR(64),
    login_result VARCHAR(32) NOT NULL,
    client_ip VARCHAR(64),
    client_device VARCHAR(200),
    login_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logout_at TIMESTAMP,
    failure_reason VARCHAR(500),
    remarks VARCHAR(500),
    CONSTRAINT pk_user_login_logs PRIMARY KEY (id)
);

CREATE TABLE auth_access_tokens (
    jti VARCHAR(128) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    client_ip VARCHAR(64),
    client_device VARCHAR(200),
    CONSTRAINT pk_auth_access_tokens PRIMARY KEY (jti)
);
