ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS quantity INTEGER;

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS purchase_date DATE;

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS purchaser_name VARCHAR(100);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS purchaser_code VARCHAR(64);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS management_unit VARCHAR(100);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS management_code VARCHAR(64);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS use_unit VARCHAR(100);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS principal_code VARCHAR(64);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS principal_name VARCHAR(100);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS user_name VARCHAR(100);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS production_date DATE;

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS warranty_end_date DATE;

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS factory_no VARCHAR(100);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS depreciation_method VARCHAR(100);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS service_life_years INTEGER;

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS price DECIMAL(18, 2);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS manufacturer VARCHAR(100);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS port_no VARCHAR(64);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(64);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS common_startup_time VARCHAR(16);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS common_shutdown_time VARCHAR(16);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS common_usage_content VARCHAR(500);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS commonly_used INTEGER DEFAULT 0;

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS set_temperature DECIMAL(8, 2);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS current_temperature DECIMAL(8, 2);

ALTER TABLE equipment_records
    ADD COLUMN IF NOT EXISTS rfid VARCHAR(100);
