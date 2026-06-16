CREATE TABLE IF NOT EXISTS archive_cabinet_nodes (
    id VARCHAR(64) NOT NULL,
    parent_id VARCHAR(64),
    node_code VARCHAR(64) NOT NULL,
    node_type VARCHAR(32) NOT NULL,
    cabinet_type VARCHAR(32),
    cabinet_id VARCHAR(64),
    layer_no INTEGER,
    capacity INTEGER NOT NULL DEFAULT 0,
    path_location VARCHAR(200),
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_archive_cabinet_nodes PRIMARY KEY (id),
    CONSTRAINT fk_archive_cabinet_nodes_parent FOREIGN KEY (parent_id) REFERENCES archive_cabinet_nodes (id),
    CONSTRAINT fk_archive_cabinet_nodes_cabinet FOREIGN KEY (cabinet_id) REFERENCES archive_cabinets (id)
);

CREATE INDEX IF NOT EXISTS idx_archive_cabinet_nodes_parent ON archive_cabinet_nodes (parent_id);
CREATE INDEX IF NOT EXISTS idx_archive_cabinet_nodes_cabinet ON archive_cabinet_nodes (cabinet_id);

INSERT INTO archive_cabinet_nodes
    (id, parent_id, node_code, node_type, cabinet_type, cabinet_id, layer_no, capacity, path_location, remarks, created_at, updated_at)
SELECT
    'ACN-TYPE-' || cabinet_type,
    NULL,
    cabinet_type,
    'AREA',
    cabinet_type,
    NULL,
    NULL,
    SUM(capacity),
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM archive_cabinets
WHERE NOT EXISTS (
    SELECT 1
    FROM archive_cabinet_nodes existing
    WHERE existing.id = 'ACN-TYPE-' || archive_cabinets.cabinet_type
)
GROUP BY cabinet_type;

INSERT INTO archive_cabinet_nodes
    (id, parent_id, node_code, node_type, cabinet_type, cabinet_id, layer_no, capacity, path_location, remarks, created_at, updated_at)
SELECT
    'ACN-CAB-' || id,
    'ACN-TYPE-' || cabinet_type,
    cabinet_code,
    'CABINET',
    cabinet_type,
    id,
    NULL,
    capacity,
    location_description,
    remarks,
    created_at,
    updated_at
FROM archive_cabinets
WHERE NOT EXISTS (
    SELECT 1
    FROM archive_cabinet_nodes existing
    WHERE existing.id = 'ACN-CAB-' || archive_cabinets.id
);

INSERT INTO archive_cabinet_nodes
    (id, parent_id, node_code, node_type, cabinet_type, cabinet_id, layer_no, capacity, path_location, remarks, created_at, updated_at)
SELECT
    'ACN-DRW-' || id || '-' || layer_no,
    'ACN-CAB-' || id,
    CAST(((layer_no - 1) * slot_count_per_layer + 1) AS VARCHAR) || '-' ||
        CAST((layer_no * slot_count_per_layer) AS VARCHAR),
    'DRAWER',
    cabinet_type,
    id,
    layer_no,
    slot_count_per_layer,
    location_description,
    NULL,
    created_at,
    updated_at
FROM archive_cabinets
CROSS JOIN (
    SELECT 1 AS layer_no UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) layers
WHERE layer_no <= archive_cabinets.layer_count
  AND NOT EXISTS (
      SELECT 1
      FROM archive_cabinet_nodes existing
      WHERE existing.id = 'ACN-DRW-' || archive_cabinets.id || '-' || layer_no
  );
