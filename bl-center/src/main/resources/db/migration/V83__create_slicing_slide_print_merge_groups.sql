CREATE TABLE IF NOT EXISTS slicing_slide_print_merge_groups (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    pathology_no VARCHAR(64),
    patient_id VARCHAR(64),
    embedding_box_no VARCHAR(200) NOT NULL,
    group_status VARCHAR(32) NOT NULL,
    printed_slicing_id VARCHAR(64),
    created_by_user_id VARCHAR(64),
    created_by_name VARCHAR(100),
    printed_by_user_id VARCHAR(64),
    printed_by_name VARCHAR(100),
    printed_at TIMESTAMP,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_slicing_slide_print_merge_groups PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS slicing_slide_print_merge_group_items (
    id VARCHAR(64) NOT NULL,
    group_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64) NOT NULL,
    embedding_box_id VARCHAR(64) NOT NULL,
    embedding_box_no VARCHAR(64) NOT NULL,
    sequence_no INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_slicing_slide_print_merge_group_items PRIMARY KEY (id)
);
