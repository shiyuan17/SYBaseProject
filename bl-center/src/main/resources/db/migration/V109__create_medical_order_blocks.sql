CREATE TABLE IF NOT EXISTS medical_order_blocks (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    block_no VARCHAR(64) NOT NULL,
    created_by_user_id VARCHAR(64),
    created_by_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_medical_order_blocks PRIMARY KEY (id),
    CONSTRAINT fk_medical_order_blocks_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT uk_medical_order_blocks_case_block UNIQUE (case_id, block_no)
);

CREATE INDEX IF NOT EXISTS idx_medical_order_blocks_case_created
    ON medical_order_blocks (case_id, created_at);
