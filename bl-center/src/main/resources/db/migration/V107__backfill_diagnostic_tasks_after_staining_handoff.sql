INSERT INTO diagnostic_tasks
    (id, case_id, pathology_no, task_type, status, priority, remarks, created_at, updated_at)
SELECT
    'DT_BACKFILL_' || pc.id,
    pc.id,
    pc.pathology_no,
    'PRIMARY',
    'PENDING',
    'NORMAL',
    'Backfilled after staining completed without diagnostic handoff',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM pathology_cases pc
WHERE pc.case_status = 'STAINING'
  AND EXISTS (
      SELECT 1
      FROM technical_pending_tasks t
      WHERE t.case_id = pc.id
        AND t.task_type = 'STAINING'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM technical_pending_tasks t
      WHERE t.case_id = pc.id
        AND t.task_type = 'STAINING'
        AND t.task_status IN ('PENDING', 'IN_PROGRESS', 'EMBEDDING_CONFIRM_PENDING')
  )
  AND NOT EXISTS (
      SELECT 1
      FROM diagnostic_tasks dt
      WHERE dt.case_id = pc.id
        AND dt.task_type = 'PRIMARY'
        AND dt.status IN ('PENDING', 'ASSIGNED', 'ACCEPTED', 'IN_PROGRESS')
  );

INSERT INTO workflow_events
    (id, application_id, specimen_id, case_id, transport_order_id, node_code, event_type,
     event_status, event_time, operator_user_id, operator_name, source_terminal, event_content, created_at)
SELECT
    'EVT_BACKFILL_' || pc.id,
    pc.application_id,
    NULL,
    pc.id,
    NULL,
    'DIAGNOSIS_ASSIGN',
    'CREATE',
    'SUCCESS',
    CURRENT_TIMESTAMP,
    NULL,
    'Migration V107',
    NULL,
    'Backfilled diagnostic task after staining completed',
    CURRENT_TIMESTAMP
FROM pathology_cases pc
WHERE pc.case_status = 'STAINING'
  AND EXISTS (
      SELECT 1
      FROM diagnostic_tasks dt
      WHERE dt.id = 'DT_BACKFILL_' || pc.id
  )
  AND NOT EXISTS (
      SELECT 1
      FROM workflow_events we
      WHERE we.id = 'EVT_BACKFILL_' || pc.id
  );

UPDATE pathology_cases pc
SET case_status = 'DIAGNOSIS_PENDING',
    updated_at = CURRENT_TIMESTAMP
WHERE pc.case_status = 'STAINING'
  AND EXISTS (
      SELECT 1
      FROM diagnostic_tasks dt
      WHERE dt.id = 'DT_BACKFILL_' || pc.id
  );
