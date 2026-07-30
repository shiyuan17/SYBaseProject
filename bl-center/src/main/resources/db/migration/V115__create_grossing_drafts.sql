CREATE TABLE grossing_drafts (
    task_id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    draft_payload CLOB NOT NULL,
    saved_by_user_id VARCHAR(64) NOT NULL,
    saved_by_name VARCHAR(100) NOT NULL,
    saved_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_grossing_drafts PRIMARY KEY (task_id),
    CONSTRAINT fk_grossing_drafts_task FOREIGN KEY (task_id) REFERENCES technical_pending_tasks (id),
    CONSTRAINT fk_grossing_drafts_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
);

CREATE INDEX idx_grossing_drafts_case ON grossing_drafts (case_id);
