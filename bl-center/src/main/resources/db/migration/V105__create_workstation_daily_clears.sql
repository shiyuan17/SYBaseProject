CREATE TABLE IF NOT EXISTS workstation_daily_clears (
    id VARCHAR(64) NOT NULL,
    workstation_type VARCHAR(32) NOT NULL,
    work_date DATE NOT NULL,
    operator_user_id VARCHAR(64) NOT NULL,
    operator_name VARCHAR(100) NOT NULL,
    cleared_at TIMESTAMP NOT NULL,
    clear_status VARCHAR(32) NOT NULL,
    operator_ip VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_workstation_daily_clears PRIMARY KEY (id),
    CONSTRAINT uk_workstation_daily_clears UNIQUE (workstation_type, work_date)
);
