ALTER TABLE equipment_records
    ADD quantity INTEGER;

ALTER TABLE equipment_records
    ADD purchase_date DATE;

ALTER TABLE equipment_records
    ADD purchaser_name VARCHAR(100);

ALTER TABLE equipment_records
    ADD purchaser_code VARCHAR(64);

ALTER TABLE equipment_records
    ADD management_unit VARCHAR(100);

ALTER TABLE equipment_records
    ADD management_code VARCHAR(64);

ALTER TABLE equipment_records
    ADD use_unit VARCHAR(100);

ALTER TABLE equipment_records
    ADD principal_code VARCHAR(64);

ALTER TABLE equipment_records
    ADD principal_name VARCHAR(100);

ALTER TABLE equipment_records
    ADD user_name VARCHAR(100);

ALTER TABLE equipment_records
    ADD production_date DATE;

ALTER TABLE equipment_records
    ADD warranty_end_date DATE;

ALTER TABLE equipment_records
    ADD factory_no VARCHAR(100);

ALTER TABLE equipment_records
    ADD depreciation_method VARCHAR(100);

ALTER TABLE equipment_records
    ADD service_life_years INTEGER;

ALTER TABLE equipment_records
    ADD price DECIMAL(18, 2);

ALTER TABLE equipment_records
    ADD manufacturer VARCHAR(100);

ALTER TABLE equipment_records
    ADD port_no VARCHAR(64);

ALTER TABLE equipment_records
    ADD ip_address VARCHAR(64);

ALTER TABLE equipment_records
    ADD common_startup_time VARCHAR(16);

ALTER TABLE equipment_records
    ADD common_shutdown_time VARCHAR(16);

ALTER TABLE equipment_records
    ADD common_usage_content VARCHAR(500);

ALTER TABLE equipment_records
    ADD commonly_used INTEGER DEFAULT 0;

ALTER TABLE equipment_records
    ADD set_temperature DECIMAL(8, 2);

ALTER TABLE equipment_records
    ADD current_temperature DECIMAL(8, 2);

ALTER TABLE equipment_records
    ADD rfid VARCHAR(100);
