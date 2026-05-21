CREATE TABLE archive_cabinets (
    id VARCHAR(64) NOT NULL,
    cabinet_code VARCHAR(64) NOT NULL,
    cabinet_name VARCHAR(100) NOT NULL,
    cabinet_type VARCHAR(32) NOT NULL,
    layer_count INTEGER NOT NULL,
    slot_count_per_layer INTEGER NOT NULL,
    capacity INTEGER NOT NULL,
    cabinet_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    location_description VARCHAR(200),
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_archive_cabinets PRIMARY KEY (id),
    CONSTRAINT uk_archive_cabinets_code UNIQUE (cabinet_code)
);

CREATE TABLE archive_positions (
    id VARCHAR(64) NOT NULL,
    cabinet_id VARCHAR(64) NOT NULL,
    position_code VARCHAR(64) NOT NULL,
    layer_no INTEGER NOT NULL,
    slot_no INTEGER NOT NULL,
    position_status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    current_object_type VARCHAR(32),
    current_object_id VARCHAR(64),
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_archive_positions PRIMARY KEY (id),
    CONSTRAINT uk_archive_positions_code UNIQUE (position_code),
    CONSTRAINT uk_archive_positions_slot UNIQUE (cabinet_id, layer_no, slot_no),
    CONSTRAINT fk_archive_positions_cabinet FOREIGN KEY (cabinet_id) REFERENCES archive_cabinets (id)
);

CREATE TABLE specimen_storage_records (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    specimen_id VARCHAR(64),
    object_type VARCHAR(32) NOT NULL,
    object_id VARCHAR(64) NOT NULL,
    storage_status VARCHAR(32) NOT NULL,
    storage_location VARCHAR(200),
    archive_position_id VARCHAR(64),
    cabinet_no VARCHAR(64),
    layer_no VARCHAR(64),
    slot_no VARCHAR(64),
    stored_by_user_id VARCHAR(64),
    stored_by_name VARCHAR(100),
    stored_at TIMESTAMP,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_specimen_storage_records PRIMARY KEY (id),
    CONSTRAINT uk_specimen_storage_records_object UNIQUE (object_type, object_id),
    CONSTRAINT fk_specimen_storage_records_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_specimen_storage_records_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_specimen_storage_records_position FOREIGN KEY (archive_position_id) REFERENCES archive_positions (id)
);

CREATE TABLE material_loans (
    id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    specimen_id VARCHAR(64),
    material_type VARCHAR(32) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    archive_position_id VARCHAR(64),
    loan_status VARCHAR(32) NOT NULL DEFAULT 'BORROWED',
    borrowed_by_user_id VARCHAR(64),
    borrowed_by_name VARCHAR(100),
    borrowed_at TIMESTAMP,
    borrow_purpose VARCHAR(500),
    approved_by_user_id VARCHAR(64),
    approved_by_name VARCHAR(100),
    returned_by_user_id VARCHAR(64),
    returned_by_name VARCHAR(100),
    returned_at TIMESTAMP,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_material_loans PRIMARY KEY (id),
    CONSTRAINT fk_material_loans_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
    CONSTRAINT fk_material_loans_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
    CONSTRAINT fk_material_loans_position FOREIGN KEY (archive_position_id) REFERENCES archive_positions (id)
);

CREATE TABLE reagents (
    id VARCHAR(64) NOT NULL,
    reagent_code VARCHAR(64) NOT NULL,
    reagent_name VARCHAR(100) NOT NULL,
    specification VARCHAR(100),
    unit VARCHAR(32),
    manufacturer VARCHAR(200),
    default_low_stock_threshold DECIMAL(18, 2),
    default_near_expiry_days INTEGER,
    enabled INTEGER DEFAULT 1,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_reagents PRIMARY KEY (id),
    CONSTRAINT uk_reagents_code UNIQUE (reagent_code)
);

CREATE TABLE reagent_stocks (
    id VARCHAR(64) NOT NULL,
    reagent_id VARCHAR(64) NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    stock_quantity DECIMAL(18, 2) NOT NULL,
    stock_status VARCHAR(32) NOT NULL,
    expiry_date DATE,
    storage_location VARCHAR(200),
    low_stock_threshold DECIMAL(18, 2),
    near_expiry_days INTEGER,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_reagent_stocks PRIMARY KEY (id),
    CONSTRAINT uk_reagent_stocks_batch UNIQUE (reagent_id, batch_no),
    CONSTRAINT fk_reagent_stocks_reagent FOREIGN KEY (reagent_id) REFERENCES reagents (id)
);

CREATE TABLE equipment_records (
    id VARCHAR(64) NOT NULL,
    equipment_code VARCHAR(64) NOT NULL,
    equipment_name VARCHAR(100) NOT NULL,
    equipment_category VARCHAR(64),
    model_no VARCHAR(100),
    equipment_status VARCHAR(32) NOT NULL,
    location_description VARCHAR(200),
    enabled_at TIMESTAMP,
    next_maintenance_at TIMESTAMP,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_equipment_records PRIMARY KEY (id),
    CONSTRAINT uk_equipment_records_code UNIQUE (equipment_code)
);

CREATE TABLE equipment_maintenance_logs (
    id VARCHAR(64) NOT NULL,
    equipment_id VARCHAR(64) NOT NULL,
    maintenance_type VARCHAR(32) NOT NULL,
    maintenance_status VARCHAR(32) NOT NULL,
    performed_at TIMESTAMP NOT NULL,
    performed_by_user_id VARCHAR(64),
    performed_by_name VARCHAR(100),
    description VARCHAR(1000),
    next_maintenance_at TIMESTAMP,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_equipment_maintenance_logs PRIMARY KEY (id),
    CONSTRAINT fk_equipment_maintenance_logs_equipment FOREIGN KEY (equipment_id) REFERENCES equipment_records (id)
);
