ALTER TABLE users ADD password_algo VARCHAR(32);

ALTER TABLE users ADD password_salt VARCHAR(64);

CREATE TABLE auth_access_tokens (
    jti VARCHAR(128) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    client_ip VARCHAR(64),
    client_device VARCHAR(200),
    CONSTRAINT pk_auth_access_tokens PRIMARY KEY (jti),
    CONSTRAINT fk_auth_access_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_auth_access_tokens_user_id ON auth_access_tokens (user_id);
