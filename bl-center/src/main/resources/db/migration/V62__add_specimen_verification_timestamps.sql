ALTER TABLE specimen_fixation_records ADD COLUMN IF NOT EXISTS verification_started_at TIMESTAMP;

ALTER TABLE specimen_fixation_records ADD COLUMN IF NOT EXISTS verification_completed_at TIMESTAMP;
