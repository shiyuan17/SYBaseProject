ALTER TABLE report_versions ADD COLUMN IF NOT EXISTS planned_issue_at TIMESTAMP;
ALTER TABLE report_versions ADD COLUMN IF NOT EXISTS delivery_schedule_status VARCHAR(32) DEFAULT 'NONE';

UPDATE report_versions
SET delivery_schedule_status = COALESCE(delivery_schedule_status, 'NONE');

ALTER TABLE report_versions ADD CONSTRAINT IF NOT EXISTS ck_report_versions_delivery_schedule_status
CHECK (delivery_schedule_status IN ('NONE', 'SCHEDULED', 'EXECUTED', 'CANCELLED'));

CREATE INDEX IF NOT EXISTS idx_report_versions_scheduled_issue
ON report_versions (delivery_schedule_status, planned_issue_at);
