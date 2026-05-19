INSERT INTO menus (id, parent_id, menu_code, menu_name, menu_type, path, component_name, permission_prefix, sort_order) VALUES
('MENU_M2_WORKFLOW', NULL, 'M2_WORKFLOW', 'M2 Workflow', 'DIRECTORY', '/workflow', 'WorkflowRoot', 'm2', 110),
('MENU_M2_CLINICAL', 'MENU_M2_WORKFLOW', 'M2_CLINICAL', 'Clinical Register', 'MENU', '/api/v1/specimens/register', 'ClinicalRegister', 'm2:clinical', 111),
('MENU_M2_FIXATION', 'MENU_M2_WORKFLOW', 'M2_FIXATION', 'Fixation Verify', 'MENU', '/api/v1/specimen-fixations', 'FixationVerify', 'm2:fixation', 112),
('MENU_M2_TRANSPORT', 'MENU_M2_WORKFLOW', 'M2_TRANSPORT', 'Transport Handover', 'MENU', '/api/v1/transport-orders', 'TransportHandover', 'm2:transport', 113),
('MENU_M2_RECEIPT', 'MENU_M2_WORKFLOW', 'M2_RECEIPT', 'Specimen Receipt', 'MENU', '/api/v1/specimen-receipts', 'SpecimenReceipt', 'm2:receipt', 114),
('MENU_M2_TRACKING', 'MENU_M2_WORKFLOW', 'M2_TRACKING', 'Tracking Query', 'MENU', '/api/v1/applications/{id}/tracking', 'TrackingQuery', 'm2:tracking', 115);

INSERT INTO permissions (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order) VALUES
('PERM_SPECIMEN_REGISTER', 'PERM_SPECIMEN_REGISTER', 'Specimen register', 'MENU_M2_CLINICAL', 'REGISTER', 'POST', '/api/v1/specimens/register', 'M2', 110),
('PERM_FIXATION_VERIFY', 'PERM_FIXATION_VERIFY', 'Fixation verify', 'MENU_M2_FIXATION', 'VERIFY', 'POST', '/api/v1/specimen-fixations', 'M2', 111),
('PERM_TRANSPORT_HANDOVER', 'PERM_TRANSPORT_HANDOVER', 'Transport handover', 'MENU_M2_TRANSPORT', 'HANDOVER', 'POST', '/api/v1/transport-orders', 'M2', 112),
('PERM_SPECIMEN_RECEIVE', 'PERM_SPECIMEN_RECEIVE', 'Specimen receive', 'MENU_M2_RECEIPT', 'RECEIVE', 'POST', '/api/v1/specimen-receipts', 'M2', 113),
('PERM_SPECIMEN_TRACKING_QUERY', 'PERM_SPECIMEN_TRACKING_QUERY', 'Tracking query', 'MENU_M2_TRACKING', 'QUERY', 'GET', '/api/v1/applications/{id}/tracking', 'M2', 114),
('PERM_CLINICAL_IMPORT', 'PERM_CLINICAL_IMPORT', 'Clinical import', 'MENU_M2_CLINICAL', 'IMPORT', 'POST', '/api/v1/clinical-applications/import', 'M2', 115);

INSERT INTO roles (id, role_code, role_name, role_type, data_scope, remarks) VALUES
('ROLE_M2_CLINICAL_REGISTER', 'M2_CLINICAL_REGISTER', 'M2 Clinical Register', 'BUSINESS', 'DEPARTMENT', 'M2 clinical register workstation'),
('ROLE_M2_FIXATION_VERIFY', 'M2_FIXATION_VERIFY', 'M2 Fixation Verify', 'BUSINESS', 'DEPARTMENT', 'M2 fixation workstation'),
('ROLE_M2_TRANSPORT_HANDOVER', 'M2_TRANSPORT_HANDOVER', 'M2 Transport Handover', 'BUSINESS', 'DEPARTMENT', 'M2 transport workstation'),
('ROLE_M2_SPECIMEN_RECEIVE', 'M2_SPECIMEN_RECEIVE', 'M2 Specimen Receive', 'BUSINESS', 'DEPARTMENT', 'M2 specimen receipt workstation'),
('ROLE_M2_TRACKING_QUERY', 'M2_TRACKING_QUERY', 'M2 Tracking Query', 'BUSINESS', 'DEPARTMENT', 'M2 tracking query workstation'),
('ROLE_M2_CLINICAL_IMPORT', 'M2_CLINICAL_IMPORT', 'M2 Clinical Import', 'BUSINESS', 'DEPARTMENT', 'M2 clinical import workstation');

INSERT INTO role_permissions (id, role_id, permission_id, assigned_at) VALUES
('RP_M2_ADMIN_REGISTER', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SPECIMEN_REGISTER', CURRENT_TIMESTAMP),
('RP_M2_ADMIN_FIXATION', 'ROLE_PATHOLOGY_ADMIN', 'PERM_FIXATION_VERIFY', CURRENT_TIMESTAMP),
('RP_M2_ADMIN_TRANSPORT', 'ROLE_PATHOLOGY_ADMIN', 'PERM_TRANSPORT_HANDOVER', CURRENT_TIMESTAMP),
('RP_M2_ADMIN_RECEIVE', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SPECIMEN_RECEIVE', CURRENT_TIMESTAMP),
('RP_M2_ADMIN_TRACKING', 'ROLE_PATHOLOGY_ADMIN', 'PERM_SPECIMEN_TRACKING_QUERY', CURRENT_TIMESTAMP),
('RP_M2_ADMIN_IMPORT', 'ROLE_PATHOLOGY_ADMIN', 'PERM_CLINICAL_IMPORT', CURRENT_TIMESTAMP),
('RP_M2_REGISTER_ROLE', 'ROLE_M2_CLINICAL_REGISTER', 'PERM_SPECIMEN_REGISTER', CURRENT_TIMESTAMP),
('RP_M2_FIXATION_ROLE', 'ROLE_M2_FIXATION_VERIFY', 'PERM_FIXATION_VERIFY', CURRENT_TIMESTAMP),
('RP_M2_TRANSPORT_ROLE', 'ROLE_M2_TRANSPORT_HANDOVER', 'PERM_TRANSPORT_HANDOVER', CURRENT_TIMESTAMP),
('RP_M2_RECEIVE_ROLE', 'ROLE_M2_SPECIMEN_RECEIVE', 'PERM_SPECIMEN_RECEIVE', CURRENT_TIMESTAMP),
('RP_M2_TRACKING_ROLE', 'ROLE_M2_TRACKING_QUERY', 'PERM_SPECIMEN_TRACKING_QUERY', CURRENT_TIMESTAMP),
('RP_M2_IMPORT_ROLE', 'ROLE_M2_CLINICAL_IMPORT', 'PERM_CLINICAL_IMPORT', CURRENT_TIMESTAMP);

INSERT INTO users (id, user_code, login_name, name, role, enabled, created_at, updated_at) VALUES
('USER_M2_ADMIN', 'U-M2-ADMIN', 'm2.admin', 'M2 Admin', 'PATHOLOGY_ADMIN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M2_REGISTER', 'U-M2-REGISTER', 'm2.register', 'M2 Register', 'M2_CLINICAL_REGISTER', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M2_FIXATION', 'U-M2-FIXATION', 'm2.fixation', 'M2 Fixation', 'M2_FIXATION_VERIFY', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M2_TRANSPORT', 'U-M2-TRANSPORT', 'm2.transport', 'M2 Transport', 'M2_TRANSPORT_HANDOVER', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M2_RECEIVE', 'U-M2-RECEIVE', 'm2.receive', 'M2 Receive', 'M2_SPECIMEN_RECEIVE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M2_TRACKING', 'U-M2-TRACKING', 'm2.tracking', 'M2 Tracking', 'M2_TRACKING_QUERY', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M2_IMPORT', 'U-M2-IMPORT', 'm2.import', 'M2 Import', 'M2_CLINICAL_IMPORT', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER_M2_NO_PERMISSION', 'U-M2-NOAUTH', 'm2.noauth', 'M2 No Permission', NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO user_roles (id, user_id, role_id, is_primary, assigned_at, assigned_by_name) VALUES
('UR_M2_ADMIN', 'USER_M2_ADMIN', 'ROLE_PATHOLOGY_ADMIN', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M2_REGISTER', 'USER_M2_REGISTER', 'ROLE_M2_CLINICAL_REGISTER', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M2_FIXATION', 'USER_M2_FIXATION', 'ROLE_M2_FIXATION_VERIFY', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M2_TRANSPORT', 'USER_M2_TRANSPORT', 'ROLE_M2_TRANSPORT_HANDOVER', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M2_RECEIVE', 'USER_M2_RECEIVE', 'ROLE_M2_SPECIMEN_RECEIVE', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M2_TRACKING', 'USER_M2_TRACKING', 'ROLE_M2_TRACKING_QUERY', 1, CURRENT_TIMESTAMP, 'system'),
('UR_M2_IMPORT', 'USER_M2_IMPORT', 'ROLE_M2_CLINICAL_IMPORT', 1, CURRENT_TIMESTAMP, 'system');

UPDATE specimens
SET label_print_status = 'PENDING'
WHERE label_print_status IS NULL;

ALTER TABLE specimens ADD CONSTRAINT ck_specimens_label_print_status
CHECK (label_print_status IS NULL OR label_print_status IN ('PENDING', 'SUCCESS', 'FAILED'));
