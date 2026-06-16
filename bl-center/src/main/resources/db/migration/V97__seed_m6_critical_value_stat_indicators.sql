INSERT INTO stat_indicator_definitions
    (id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type, description, sort_order, enabled)
SELECT
    'SID_QC_14',
    'QC_CRITICAL_VALUE_COUNT',
    'Critical Value Count',
    'QUALITY',
    'DEPARTMENT',
    'COUNT',
    'M6 critical value read-only proxy indicator',
    14,
    1
WHERE NOT EXISTS (
    SELECT 1
    FROM stat_indicator_definitions
    WHERE indicator_code = 'QC_CRITICAL_VALUE_COUNT'
);

INSERT INTO stat_indicator_definitions
    (id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type, description, sort_order, enabled)
SELECT
    'SID_QC_15',
    'QC_CRITICAL_VALUE_REPORT_TIMELINESS_RATE',
    'Critical Value Report Timeliness Rate',
    'QUALITY',
    'DEPARTMENT',
    'RATE',
    'M6 critical value read-only proxy indicator',
    15,
    1
WHERE NOT EXISTS (
    SELECT 1
    FROM stat_indicator_definitions
    WHERE indicator_code = 'QC_CRITICAL_VALUE_REPORT_TIMELINESS_RATE'
);

INSERT INTO stat_indicator_definitions
    (id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type, description, sort_order, enabled)
SELECT
    'SID_QC_16',
    'QC_CRITICAL_VALUE_REASON_ANALYSIS_COUNT',
    'Critical Value Reason Analysis Count',
    'QUALITY',
    'DEPARTMENT',
    'COUNT',
    'M6 critical value read-only proxy indicator',
    16,
    1
WHERE NOT EXISTS (
    SELECT 1
    FROM stat_indicator_definitions
    WHERE indicator_code = 'QC_CRITICAL_VALUE_REASON_ANALYSIS_COUNT'
);
