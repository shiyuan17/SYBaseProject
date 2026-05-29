-- Patch legacy DM full-baseline imports to the M2 schema expected by bl-center.
-- Safe to run multiple times in the current schema.

DECLARE
    v_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'APPLICATIONS' AND COLUMN_NAME = 'PATIENT_NAME';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE applications ADD COLUMN patient_name VARCHAR2(100)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'APPLICATIONS' AND COLUMN_NAME = 'PATIENT_GENDER';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE applications ADD COLUMN patient_gender VARCHAR2(16)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'APPLICATIONS' AND COLUMN_NAME = 'PATIENT_AGE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE applications ADD COLUMN patient_age VARCHAR2(32)';
    END IF;
END;
/

DECLARE
    v_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'PATHOLOGY_CASES' AND COLUMN_NAME = 'CASE_STATUS';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE pathology_cases ADD COLUMN case_status VARCHAR2(32)';
    END IF;

    EXECUTE IMMEDIATE 'UPDATE pathology_cases SET case_status = COALESCE(case_status, current_status) WHERE case_status IS NULL';
END;
/

DECLARE
    v_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'SPECIMENS' AND COLUMN_NAME = 'SPECIMEN_STATUS';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimens ADD COLUMN specimen_status VARCHAR2(32)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'SPECIMENS' AND COLUMN_NAME = 'LABEL_PRINT_BATCH_NO';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimens ADD COLUMN label_print_batch_no VARCHAR2(64)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'SPECIMENS' AND COLUMN_NAME = 'LABEL_PRINT_STATUS';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimens ADD COLUMN label_print_status VARCHAR2(32)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'SPECIMENS' AND COLUMN_NAME = 'TERMINAL_CODE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimens ADD COLUMN terminal_code VARCHAR2(64)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'SPECIMENS' AND COLUMN_NAME = 'CREATED_AT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimens ADD COLUMN created_at TIMESTAMP';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'SPECIMENS' AND COLUMN_NAME = 'UPDATED_AT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimens ADD COLUMN updated_at TIMESTAMP';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM USER_TAB_COLUMNS
    WHERE TABLE_NAME = 'SPECIMENS'
      AND COLUMN_NAME = 'CASE_ID'
      AND NULLABLE = 'N';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimens MODIFY case_id NULL';
    END IF;

    EXECUTE IMMEDIATE '
        UPDATE specimens
        SET application_id = (
            SELECT pc.application_id
            FROM pathology_cases pc
            WHERE pc.id = specimens.case_id
        )
        WHERE application_id IS NULL
          AND case_id IS NOT NULL';

    SELECT COUNT(*) INTO v_count
    FROM USER_CONSTRAINTS uc
    JOIN USER_CONS_COLUMNS ucc ON uc.CONSTRAINT_NAME = ucc.CONSTRAINT_NAME
    WHERE uc.TABLE_NAME = 'SPECIMENS'
      AND uc.CONSTRAINT_TYPE = 'U'
      AND uc.CONSTRAINT_NAME = 'UK_SPECIMENS_CASE_SPECIMEN_NO';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimens DROP CONSTRAINT uk_specimens_case_specimen_no';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM USER_CONSTRAINTS
    WHERE TABLE_NAME = 'SPECIMENS'
      AND CONSTRAINT_NAME = 'UK_SPECIMENS_SPECIMEN_NO';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimens ADD CONSTRAINT uk_specimens_specimen_no UNIQUE (specimen_no)';
    END IF;

    EXECUTE IMMEDIATE '
        UPDATE specimens
        SET specimen_status = COALESCE(specimen_status, ''REGISTERED''),
            created_at = COALESCE(created_at, registered_at, CURRENT_TIMESTAMP),
            updated_at = COALESCE(updated_at, registered_at, CURRENT_TIMESTAMP)
        WHERE specimen_status IS NULL
           OR created_at IS NULL
           OR updated_at IS NULL';
END;
/

DECLARE
    v_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'SPECIMEN_COLLECTION_RECORDS' AND COLUMN_NAME = 'TERMINAL_CODE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimen_collection_records ADD COLUMN terminal_code VARCHAR2(64)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'SPECIMEN_COLLECTION_RECORDS' AND COLUMN_NAME = 'CASE_ID' AND NULLABLE = 'N';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimen_collection_records MODIFY case_id NULL';
    END IF;
END;
/

DECLARE
    v_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'SPECIMEN_FIXATION_RECORDS' AND COLUMN_NAME = 'APPLICATION_ID';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimen_fixation_records ADD COLUMN application_id VARCHAR2(64)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'SPECIMEN_FIXATION_RECORDS' AND COLUMN_NAME = 'TERMINAL_CODE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimen_fixation_records ADD COLUMN terminal_code VARCHAR2(64)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'SPECIMEN_FIXATION_RECORDS' AND COLUMN_NAME = 'CASE_ID' AND NULLABLE = 'N';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimen_fixation_records MODIFY case_id NULL';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_CONSTRAINTS WHERE TABLE_NAME = 'SPECIMEN_FIXATION_RECORDS' AND CONSTRAINT_NAME = 'UK_SPECIMEN_FIXATION_RECORDS_SPECIMEN';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimen_fixation_records ADD CONSTRAINT uk_specimen_fixation_records_specimen UNIQUE (specimen_id)';
    END IF;

    EXECUTE IMMEDIATE '
        UPDATE specimen_fixation_records
        SET application_id = (
            SELECT s.application_id
            FROM specimens s
            WHERE s.id = specimen_fixation_records.specimen_id
        )
        WHERE application_id IS NULL
          AND specimen_id IS NOT NULL';
END;
/

DECLARE
    v_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'TRANSPORT_ORDERS' AND COLUMN_NAME = 'TERMINAL_CODE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE transport_orders ADD COLUMN terminal_code VARCHAR2(64)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'TRANSPORT_ORDERS' AND COLUMN_NAME = 'CREATED_AT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE transport_orders ADD COLUMN created_at TIMESTAMP';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'TRANSPORT_ORDERS' AND COLUMN_NAME = 'UPDATED_AT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE transport_orders ADD COLUMN updated_at TIMESTAMP';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'TRANSPORT_ORDERS' AND COLUMN_NAME = 'CASE_ID' AND NULLABLE = 'N';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE transport_orders MODIFY case_id NULL';
    END IF;

    EXECUTE IMMEDIATE '
        UPDATE transport_orders
        SET created_at = COALESCE(created_at, printed_at, to_be_transported_at, handed_over_at, CURRENT_TIMESTAMP),
            updated_at = COALESCE(updated_at, handed_over_at, to_be_transported_at, printed_at, CURRENT_TIMESTAMP)
        WHERE created_at IS NULL
           OR updated_at IS NULL';
END;
/

DECLARE
    v_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'TRANSPORT_ORDER_ITEMS' AND COLUMN_NAME = 'APPLICATION_ID';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE transport_order_items ADD COLUMN application_id VARCHAR2(64)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'TRANSPORT_ORDER_ITEMS' AND COLUMN_NAME = 'CASE_ID' AND NULLABLE = 'N';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE transport_order_items MODIFY case_id NULL';
    END IF;

    EXECUTE IMMEDIATE '
        UPDATE transport_order_items
        SET application_id = (
            SELECT t.application_id
            FROM transport_orders t
            WHERE t.id = transport_order_items.transport_order_id
        )
        WHERE application_id IS NULL
          AND transport_order_id IS NOT NULL';
END;
/

DECLARE
    v_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'SPECIMEN_RECEIPTS' AND COLUMN_NAME = 'APPLICATION_ID';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimen_receipts ADD COLUMN application_id VARCHAR2(64)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'SPECIMEN_RECEIPTS' AND COLUMN_NAME = 'TRANSPORT_ORDER_ID';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimen_receipts ADD COLUMN transport_order_id VARCHAR2(64)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'SPECIMEN_RECEIPTS' AND COLUMN_NAME = 'TERMINAL_CODE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimen_receipts ADD COLUMN terminal_code VARCHAR2(64)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'SPECIMEN_RECEIPTS' AND COLUMN_NAME = 'QUALITY_CHECK_RESULT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimen_receipts ADD COLUMN quality_check_result VARCHAR2(32)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'SPECIMEN_RECEIPTS' AND COLUMN_NAME = 'QUALITY_ISSUE_CODES';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimen_receipts ADD COLUMN quality_issue_codes VARCHAR2(500)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'SPECIMEN_RECEIPTS' AND COLUMN_NAME = 'CASE_ID' AND NULLABLE = 'N';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE specimen_receipts MODIFY case_id NULL';
    END IF;

    EXECUTE IMMEDIATE '
        UPDATE specimen_receipts
        SET application_id = (
            SELECT s.application_id
            FROM specimens s
            WHERE s.id = specimen_receipts.specimen_id
        )
        WHERE application_id IS NULL
          AND specimen_id IS NOT NULL';

    EXECUTE IMMEDIATE '
        UPDATE specimen_receipts
        SET transport_order_id = (
            SELECT MIN(toi.transport_order_id)
            FROM transport_order_items toi
            WHERE toi.specimen_id = specimen_receipts.specimen_id
        )
        WHERE transport_order_id IS NULL
          AND specimen_id IS NOT NULL';
END;
/

DECLARE
    v_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'WORKFLOW_EVENTS' AND COLUMN_NAME = 'APPLICATION_ID';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE workflow_events ADD COLUMN application_id VARCHAR2(64)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'WORKFLOW_EVENTS' AND COLUMN_NAME = 'TRANSPORT_ORDER_ID';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE workflow_events ADD COLUMN transport_order_id VARCHAR2(64)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'WORKFLOW_EVENTS' AND COLUMN_NAME = 'EVENT_TYPE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE workflow_events ADD COLUMN event_type VARCHAR2(64)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'WORKFLOW_EVENTS' AND COLUMN_NAME = 'EVENT_STATUS';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE workflow_events ADD COLUMN event_status VARCHAR2(32)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'WORKFLOW_EVENTS' AND COLUMN_NAME = 'EVENT_TIME';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE workflow_events ADD COLUMN event_time TIMESTAMP';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'WORKFLOW_EVENTS' AND COLUMN_NAME = 'SOURCE_TERMINAL';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE workflow_events ADD COLUMN source_terminal VARCHAR2(64)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'WORKFLOW_EVENTS' AND COLUMN_NAME = 'EVENT_CONTENT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE workflow_events ADD COLUMN event_content VARCHAR2(1000)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'WORKFLOW_EVENTS' AND COLUMN_NAME = 'CREATED_AT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE workflow_events ADD COLUMN created_at TIMESTAMP';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'WORKFLOW_EVENTS' AND COLUMN_NAME = 'CASE_ID' AND NULLABLE = 'N';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE workflow_events MODIFY case_id NULL';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'WORKFLOW_EVENTS' AND COLUMN_NAME = 'ACTION_CODE' AND NULLABLE = 'N';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE workflow_events MODIFY action_code NULL';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'WORKFLOW_EVENTS' AND COLUMN_NAME = 'OCCURRED_AT' AND NULLABLE = 'N';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE workflow_events MODIFY occurred_at NULL';
    END IF;

    EXECUTE IMMEDIATE '
        UPDATE workflow_events
        SET application_id = (
            SELECT s.application_id
            FROM specimens s
            WHERE s.id = workflow_events.specimen_id
        )
        WHERE application_id IS NULL
          AND specimen_id IS NOT NULL';

    EXECUTE IMMEDIATE '
        UPDATE workflow_events
        SET application_id = (
            SELECT pc.application_id
            FROM pathology_cases pc
            WHERE pc.id = workflow_events.case_id
        )
        WHERE application_id IS NULL
          AND case_id IS NOT NULL';

    EXECUTE IMMEDIATE '
        UPDATE workflow_events
        SET event_type = COALESCE(event_type, action_code),
            event_status = COALESCE(event_status, to_status, from_status),
            event_time = COALESCE(event_time, occurred_at),
            created_at = COALESCE(created_at, occurred_at, CURRENT_TIMESTAMP),
            event_content = COALESCE(event_content, remarks)
        WHERE event_type IS NULL
           OR event_status IS NULL
           OR event_time IS NULL
           OR created_at IS NULL
           OR event_content IS NULL';
END;
/
