ALTER TABLE sampling_blocks ADD COLUMN embedding_box_name VARCHAR(100);
ALTER TABLE sampling_blocks ADD COLUMN embedding_box_status VARCHAR(32) DEFAULT 'PENDING';
ALTER TABLE sampling_blocks ADD COLUMN embedding_remarks VARCHAR(500);
