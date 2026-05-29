UPDATE specimen_fixation_records
SET verification_started_at = COALESCE(
        verification_started_at,
        (
            SELECT s.specimen_removal_at
            FROM specimens s
            WHERE s.id = specimen_fixation_records.specimen_id
        )
    ),
    verification_completed_at = COALESCE(
        verification_completed_at,
        (
            SELECT s.specimen_removal_at
            FROM specimens s
            WHERE s.id = specimen_fixation_records.specimen_id
        )
    ),
    verified_at = COALESCE(
        verified_at,
        (
            SELECT s.specimen_removal_at
            FROM specimens s
            WHERE s.id = specimen_fixation_records.specimen_id
        )
    ),
    verified_by_user_id = COALESCE(
        verified_by_user_id,
        (
            SELECT s.specimen_removal_operator_user_id
            FROM specimens s
            WHERE s.id = specimen_fixation_records.specimen_id
        )
    ),
    verified_by_name = COALESCE(
        verified_by_name,
        (
            SELECT s.specimen_removal_operator_name
            FROM specimens s
            WHERE s.id = specimen_fixation_records.specimen_id
        )
    )
WHERE specimen_id IN (
    SELECT id
    FROM specimens
    WHERE specimen_removal_at IS NOT NULL
)
AND (
    verification_completed_at IS NULL
    OR verified_at IS NULL
);

INSERT INTO specimen_fixation_records (
    id,
    application_id,
    specimen_id,
    fixation_status,
    verification_started_at,
    verification_completed_at,
    verified_at,
    verified_by_user_id,
    verified_by_name
)
SELECT
    'SFR-REMOVAL-' || s.id,
    s.application_id,
    s.id,
    'PENDING',
    s.specimen_removal_at,
    s.specimen_removal_at,
    s.specimen_removal_at,
    s.specimen_removal_operator_user_id,
    s.specimen_removal_operator_name
FROM specimens s
WHERE s.specimen_removal_at IS NOT NULL
AND NOT EXISTS (
    SELECT 1
    FROM specimen_fixation_records sfr
    WHERE sfr.specimen_id = s.id
);
