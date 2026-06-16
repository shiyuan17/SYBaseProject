create table medical_waste_specimen_batch (
    id varchar(64) primary key,
    bag_name varchar(100) not null,
    grossing_station_id varchar(64),
    grossing_station_name varchar(100) not null,
    grossing_operator_id varchar(64),
    grossing_operator_name varchar(100) not null,
    grossing_date date not null,
    grossing_period varchar(16) not null,
    weight_kg decimal(10, 3),
    label_count integer not null,
    printed_at timestamp not null,
    printed_by_user_id varchar(64) not null,
    printed_by_name varchar(100) not null,
    destroyed_at timestamp,
    destroyed_by_user_id varchar(64),
    destroyed_by_name varchar(100)
);

create index idx_medical_waste_specimen_batch_date
    on medical_waste_specimen_batch (grossing_date, grossing_operator_name, grossing_station_name);

create table medical_waste_specimen_batch_label (
    id varchar(64) primary key,
    batch_id varchar(64) not null,
    source_label_id varchar(64) not null,
    patient_id varchar(64),
    patient_name varchar(100),
    pathology_no varchar(64),
    specimen_name varchar(200),
    constraint fk_medical_waste_specimen_batch_label_batch
        foreign key (batch_id) references medical_waste_specimen_batch (id)
);

create index idx_medical_waste_specimen_batch_label_batch
    on medical_waste_specimen_batch_label (batch_id);

create table medical_waste_reagent_bag (
    id varchar(64) primary key,
    bag_name varchar(100) not null,
    waste_type varchar(32) not null,
    weight_kg decimal(10, 3),
    volume_ml decimal(10, 3),
    source varchar(200),
    remarks varchar(500),
    created_at timestamp not null,
    created_by_user_id varchar(64) not null,
    created_by_name varchar(100) not null,
    printed_at timestamp not null,
    printed_by_user_id varchar(64) not null,
    printed_by_name varchar(100) not null,
    handed_over_at timestamp,
    handed_over_by_user_id varchar(64),
    handed_over_by_name varchar(100),
    handover_remarks varchar(500),
    updated_at timestamp not null
);

create index idx_medical_waste_reagent_bag_created
    on medical_waste_reagent_bag (created_at, bag_name);
