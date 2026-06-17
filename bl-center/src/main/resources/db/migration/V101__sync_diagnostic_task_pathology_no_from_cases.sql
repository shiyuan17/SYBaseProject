UPDATE diagnostic_tasks dt
SET pathology_no = (
        SELECT pc.pathology_no
        FROM pathology_cases pc
        WHERE pc.id = dt.case_id
    ),
    updated_at = CURRENT_TIMESTAMP
WHERE EXISTS (
        SELECT 1
        FROM pathology_cases pc
        WHERE pc.id = dt.case_id
          AND (
              dt.pathology_no IS NULL
              OR pc.pathology_no IS NULL
              OR dt.pathology_no <> pc.pathology_no
          )
    );
