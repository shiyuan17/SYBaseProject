ALTER TABLE report_versions ADD COLUMN IF NOT EXISTS print_status VARCHAR(32) DEFAULT 'UNPRINTED';
ALTER TABLE report_versions ADD COLUMN IF NOT EXISTS printed_at TIMESTAMP;
ALTER TABLE report_versions ADD COLUMN IF NOT EXISTS delivery_status VARCHAR(32) DEFAULT 'PENDING';
ALTER TABLE report_versions ADD COLUMN IF NOT EXISTS issued_at TIMESTAMP;
ALTER TABLE report_versions ADD COLUMN IF NOT EXISTS recalled_at TIMESTAMP;

UPDATE report_versions
SET print_status = COALESCE(print_status, 'UNPRINTED'),
    delivery_status = COALESCE(delivery_status, 'PENDING');

ALTER TABLE report_versions ADD CONSTRAINT IF NOT EXISTS ck_report_versions_print_status
CHECK (print_status IN ('UNPRINTED', 'PRINTED'));

ALTER TABLE report_versions ADD CONSTRAINT IF NOT EXISTS ck_report_versions_delivery_status
CHECK (delivery_status IN ('PENDING', 'ISSUED', 'RECALLED'));

CREATE INDEX IF NOT EXISTS idx_report_versions_case_formal_created
ON report_versions (case_id, version_status, signed_at, created_at);
