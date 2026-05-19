ALTER TABLE technical_pending_tasks DROP CONSTRAINT uk_technical_pending_tasks_case_type;

ALTER TABLE technical_pending_tasks ADD COLUMN specimen_id VARCHAR(64);
ALTER TABLE technical_pending_tasks ADD COLUMN object_type VARCHAR(32);
ALTER TABLE technical_pending_tasks ADD COLUMN object_id VARCHAR(64);
ALTER TABLE technical_pending_tasks ADD COLUMN parent_task_id VARCHAR(64);
ALTER TABLE technical_pending_tasks ADD COLUMN started_at TIMESTAMP;
ALTER TABLE technical_pending_tasks ADD COLUMN completed_at TIMESTAMP;
ALTER TABLE technical_pending_tasks ADD COLUMN remarks VARCHAR(500);

UPDATE technical_pending_tasks
SET object_type = 'CASE',
    object_id = case_id
WHERE object_type IS NULL;

ALTER TABLE technical_pending_tasks ADD CONSTRAINT fk_technical_pending_tasks_specimen
FOREIGN KEY (specimen_id) REFERENCES specimens (id);

ALTER TABLE technical_pending_tasks ADD CONSTRAINT fk_technical_pending_tasks_parent
FOREIGN KEY (parent_task_id) REFERENCES technical_pending_tasks (id);

CREATE TABLE samplings (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    specimen_id VARCHAR(64) NOT NULL,
    sampling_status VARCHAR(32) NOT NULL,
    block_count INTEGER NOT NULL,
    gross_image_count INTEGER DEFAULT 0,
    sampling_template_id VARCHAR(64),
    gross_description CLOB,
    sampled_by_user_id VARCHAR(64),
    sampled_by_name VARCHAR(100),
    sampled_at TIMESTAMP,
    sampling_cancel_reason VARCHAR(500),
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_samplings PRIMARY KEY (id),
    CONSTRAINT fk_samplings_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_samplings_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_samplings_template FOREIGN KEY (sampling_template_id) REFERENCES sampling_templates (id)
);

CREATE TABLE sampling_blocks (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    specimen_id VARCHAR(64) NOT NULL,
    sampling_id VARCHAR(64) NOT NULL,
    sequence_no INTEGER NOT NULL,
    block_code VARCHAR(64) NOT NULL,
    block_site VARCHAR(200),
    block_description VARCHAR(1000),
    embedding_box_no VARCHAR(64),
    special_requirement VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sampling_blocks PRIMARY KEY (id),
    CONSTRAINT uk_sampling_blocks_block_code UNIQUE (block_code),
    CONSTRAINT fk_sampling_blocks_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_sampling_blocks_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_sampling_blocks_sampling FOREIGN KEY (sampling_id) REFERENCES samplings (id)
);

CREATE TABLE dehydration_batches (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    batch_status VARCHAR(32) NOT NULL,
    basket_no VARCHAR(64) NOT NULL,
    device_no VARCHAR(64),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    operator_user_id VARCHAR(64),
    operator_name VARCHAR(100),
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_dehydration_batches PRIMARY KEY (id),
    CONSTRAINT uk_dehydration_batches_batch_no UNIQUE (batch_no),
    CONSTRAINT fk_dehydration_batches_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
);

CREATE TABLE dehydration_batch_items (
    id VARCHAR(64) NOT NULL,
    batch_id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    specimen_id VARCHAR(64) NOT NULL,
    sampling_block_id VARCHAR(64) NOT NULL,
    item_status VARCHAR(32) NOT NULL,
    loaded_at TIMESTAMP,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_dehydration_batch_items PRIMARY KEY (id),
    CONSTRAINT uk_dehydration_batch_items_block UNIQUE (batch_id, sampling_block_id),
    CONSTRAINT fk_dehydration_batch_items_batch FOREIGN KEY (batch_id) REFERENCES dehydration_batches (id),
    CONSTRAINT fk_dehydration_batch_items_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_dehydration_batch_items_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_dehydration_batch_items_block FOREIGN KEY (sampling_block_id) REFERENCES sampling_blocks (id)
);

CREATE TABLE embeddings (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    specimen_id VARCHAR(64) NOT NULL,
    sampling_id VARCHAR(64) NOT NULL,
    sampling_block_id VARCHAR(64) NOT NULL,
    embedding_status VARCHAR(32) NOT NULL,
    evaluation_level VARCHAR(32),
    sampling_evaluation VARCHAR(500),
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    embedded_by_user_id VARCHAR(64),
    embedded_by_name VARCHAR(100),
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_embeddings PRIMARY KEY (id),
    CONSTRAINT fk_embeddings_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_embeddings_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_embeddings_sampling FOREIGN KEY (sampling_id) REFERENCES samplings (id),
    CONSTRAINT fk_embeddings_sampling_block FOREIGN KEY (sampling_block_id) REFERENCES sampling_blocks (id)
);

CREATE TABLE embedding_boxes (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    specimen_id VARCHAR(64) NOT NULL,
    sampling_block_id VARCHAR(64) NOT NULL,
    embedding_id VARCHAR(64) NOT NULL,
    embedding_box_no VARCHAR(64) NOT NULL,
    block_count INTEGER NOT NULL,
    re_embedding_flag INTEGER DEFAULT 0,
    slice_notice VARCHAR(500),
    storage_status VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_embedding_boxes PRIMARY KEY (id),
    CONSTRAINT ck_embedding_boxes_re_embedding_flag CHECK (re_embedding_flag IN (0, 1)),
    CONSTRAINT uk_embedding_boxes_box_no UNIQUE (embedding_box_no),
    CONSTRAINT fk_embedding_boxes_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_embedding_boxes_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_embedding_boxes_sampling_block FOREIGN KEY (sampling_block_id) REFERENCES sampling_blocks (id),
    CONSTRAINT fk_embedding_boxes_embedding FOREIGN KEY (embedding_id) REFERENCES embeddings (id)
);

CREATE TABLE slicings (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    specimen_id VARCHAR(64) NOT NULL,
    embedding_id VARCHAR(64) NOT NULL,
    embedding_box_id VARCHAR(64) NOT NULL,
    slicing_batch_no VARCHAR(64) NOT NULL,
    slicing_status VARCHAR(32) NOT NULL,
    slide_count INTEGER NOT NULL,
    slice_count_per_slide INTEGER,
    slice_thickness VARCHAR(64),
    sliced_by_user_id VARCHAR(64),
    sliced_by_name VARCHAR(100),
    sliced_at TIMESTAMP,
    quality_issue VARCHAR(500),
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_slicings PRIMARY KEY (id),
    CONSTRAINT uk_slicings_batch_no UNIQUE (slicing_batch_no),
    CONSTRAINT fk_slicings_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_slicings_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_slicings_embedding FOREIGN KEY (embedding_id) REFERENCES embeddings (id),
    CONSTRAINT fk_slicings_embedding_box FOREIGN KEY (embedding_box_id) REFERENCES embedding_boxes (id)
);

CREATE TABLE slides (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    specimen_id VARCHAR(64) NOT NULL,
    slicing_id VARCHAR(64) NOT NULL,
    embedding_box_id VARCHAR(64) NOT NULL,
    sampling_block_id VARCHAR(64) NOT NULL,
    slide_no VARCHAR(64) NOT NULL,
    slide_label VARCHAR(200),
    combined_slide_flag INTEGER DEFAULT 0,
    quality_status VARCHAR(32),
    slide_status VARCHAR(32) NOT NULL,
    slice_count INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_slides PRIMARY KEY (id),
    CONSTRAINT ck_slides_combined_slide_flag CHECK (combined_slide_flag IN (0, 1)),
    CONSTRAINT uk_slides_slide_no UNIQUE (slide_no),
    CONSTRAINT fk_slides_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_slides_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_slides_slicing FOREIGN KEY (slicing_id) REFERENCES slicings (id),
    CONSTRAINT fk_slides_embedding_box FOREIGN KEY (embedding_box_id) REFERENCES embedding_boxes (id),
    CONSTRAINT fk_slides_sampling_block FOREIGN KEY (sampling_block_id) REFERENCES sampling_blocks (id)
);

CREATE TABLE slide_stainings (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    specimen_id VARCHAR(64) NOT NULL,
    slide_id VARCHAR(64) NOT NULL,
    staining_type VARCHAR(100) NOT NULL,
    staining_status VARCHAR(32) NOT NULL,
    stained_by_user_id VARCHAR(64),
    stained_by_name VARCHAR(100),
    stained_at TIMESTAMP,
    quality_issue VARCHAR(500),
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_slide_stainings PRIMARY KEY (id),
    CONSTRAINT fk_slide_stainings_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_slide_stainings_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_slide_stainings_slide FOREIGN KEY (slide_id) REFERENCES slides (id)
);

CREATE TABLE rework_orders (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    specimen_id VARCHAR(64),
    sampling_block_id VARCHAR(64),
    embedding_box_id VARCHAR(64),
    slide_id VARCHAR(64),
    rework_type VARCHAR(50) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    requested_by_user_id VARCHAR(64),
    requested_by_name VARCHAR(100),
    requested_at TIMESTAMP,
    executed_by_user_id VARCHAR(64),
    executed_by_name VARCHAR(100),
    executed_at TIMESTAMP,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_rework_orders PRIMARY KEY (id),
    CONSTRAINT fk_rework_orders_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_rework_orders_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_rework_orders_sampling_block FOREIGN KEY (sampling_block_id) REFERENCES sampling_blocks (id),
    CONSTRAINT fk_rework_orders_embedding_box FOREIGN KEY (embedding_box_id) REFERENCES embedding_boxes (id),
    CONSTRAINT fk_rework_orders_slide FOREIGN KEY (slide_id) REFERENCES slides (id)
);

CREATE TABLE slide_qc_evaluations (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    specimen_id VARCHAR(64),
    slide_id VARCHAR(64) NOT NULL,
    qc_type VARCHAR(50) NOT NULL,
    evaluation_result VARCHAR(32) NOT NULL,
    issue_description VARCHAR(1000),
    improvement_suggestion VARCHAR(1000),
    evaluator_user_id VARCHAR(64),
    evaluator_name VARCHAR(100),
    evaluated_at TIMESTAMP,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_slide_qc_evaluations PRIMARY KEY (id),
    CONSTRAINT fk_slide_qc_evaluations_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_slide_qc_evaluations_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_slide_qc_evaluations_slide FOREIGN KEY (slide_id) REFERENCES slides (id)
);

CREATE TABLE case_media_assets (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    specimen_id VARCHAR(64),
    object_type VARCHAR(32) NOT NULL,
    object_id VARCHAR(64) NOT NULL,
    media_type VARCHAR(32) NOT NULL,
    file_url VARCHAR(1000) NOT NULL,
    file_name VARCHAR(255),
    captured_at TIMESTAMP,
    captured_by_user_id VARCHAR(64),
    captured_by_name VARCHAR(100),
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_case_media_assets PRIMARY KEY (id),
    CONSTRAINT fk_case_media_assets_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_case_media_assets_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id)
);
