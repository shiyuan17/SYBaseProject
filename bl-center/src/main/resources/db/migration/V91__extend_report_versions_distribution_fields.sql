ALTER TABLE report_versions ADD COLUMN print_status VARCHAR(32) DEFAULT 'UNPRINTED';
ALTER TABLE report_versions ADD COLUMN printed_at TIMESTAMP;
ALTER TABLE report_versions ADD COLUMN delivery_status VARCHAR(32) DEFAULT 'PENDING';
ALTER TABLE report_versions ADD COLUMN issued_at TIMESTAMP;
ALTER TABLE report_versions ADD COLUMN recalled_at TIMESTAMP;

UPDATE report_versions
SET print_status = COALESCE(print_status, 'UNPRINTED'),
    delivery_status = COALESCE(delivery_status, 'PENDING');

ALTER TABLE report_versions ADD CONSTRAINT ck_report_versions_print_status
CHECK (print_status IN ('UNPRINTED', 'PRINTED'));

ALTER TABLE report_versions ADD CONSTRAINT ck_report_versions_delivery_status
CHECK (delivery_status IN ('PENDING', 'ISSUED', 'RECALLED'));

CREATE INDEX idx_report_versions_case_formal_created
ON report_versions (case_id, version_status, signed_at, created_at);
