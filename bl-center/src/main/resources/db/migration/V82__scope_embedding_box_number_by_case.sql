ALTER TABLE IF EXISTS embedding_boxes DROP CONSTRAINT IF EXISTS uk_embedding_boxes_box_no;

ALTER TABLE IF EXISTS embedding_boxes ADD CONSTRAINT IF NOT EXISTS uk_embedding_boxes_case_box_no
    UNIQUE (case_id, embedding_box_no);
