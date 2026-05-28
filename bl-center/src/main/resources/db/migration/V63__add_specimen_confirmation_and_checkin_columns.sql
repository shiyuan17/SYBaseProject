ALTER TABLE specimens ADD COLUMN specimen_confirmed_at TIMESTAMP;

ALTER TABLE specimens ADD COLUMN check_in_status VARCHAR(32);

ALTER TABLE specimens ADD COLUMN checked_in_at TIMESTAMP;

ALTER TABLE specimens ADD COLUMN checked_in_by_user_id VARCHAR(64);

ALTER TABLE specimens ADD COLUMN checked_in_by_name VARCHAR(100);
