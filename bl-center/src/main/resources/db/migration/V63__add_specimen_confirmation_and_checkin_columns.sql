ALTER TABLE specimens ADD COLUMN IF NOT EXISTS specimen_confirmed_at TIMESTAMP;

ALTER TABLE specimens ADD COLUMN IF NOT EXISTS check_in_status VARCHAR(32);

ALTER TABLE specimens ADD COLUMN IF NOT EXISTS checked_in_at TIMESTAMP;

ALTER TABLE specimens ADD COLUMN IF NOT EXISTS checked_in_by_user_id VARCHAR(64);

ALTER TABLE specimens ADD COLUMN IF NOT EXISTS checked_in_by_name VARCHAR(100);
