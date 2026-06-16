INSERT INTO stat_indicator_definitions
    (id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type, description, sort_order, enabled)
SELECT 'SID_QC_17', 'QC_FROZEN_DIAGNOSIS_TIMELINESS_RATE', 'Frozen Diagnosis Timeliness Rate', 'QUALITY', 'DEPARTMENT', 'RATE', 'M6 frozen diagnosis timeliness indicator', 17, 1
WHERE NOT EXISTS (SELECT 1 FROM stat_indicator_definitions WHERE indicator_code = 'QC_FROZEN_DIAGNOSIS_TIMELINESS_RATE');

INSERT INTO stat_indicator_definitions
    (id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type, description, sort_order, enabled)
SELECT 'SID_QC_18', 'QC_FROZEN_TIMEOUT_COUNT', 'Frozen Timeout Count', 'QUALITY', 'DEPARTMENT', 'COUNT', 'M6 frozen timeout summary indicator', 18, 1
WHERE NOT EXISTS (SELECT 1 FROM stat_indicator_definitions WHERE indicator_code = 'QC_FROZEN_TIMEOUT_COUNT');

INSERT INTO stat_indicator_definitions
    (id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type, description, sort_order, enabled)
SELECT 'SID_QC_19', 'QC_FROZEN_GROSSING_TIMEOUT_COUNT', 'Frozen Grossing Timeout Count', 'QUALITY', 'DEPARTMENT', 'COUNT', 'M6 frozen grossing timeout indicator', 19, 1
WHERE NOT EXISTS (SELECT 1 FROM stat_indicator_definitions WHERE indicator_code = 'QC_FROZEN_GROSSING_TIMEOUT_COUNT');

INSERT INTO stat_indicator_definitions
    (id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type, description, sort_order, enabled)
SELECT 'SID_QC_20', 'QC_FROZEN_SLICING_TIMEOUT_COUNT', 'Frozen Slicing Timeout Count', 'QUALITY', 'DEPARTMENT', 'COUNT', 'M6 frozen slicing timeout indicator', 20, 1
WHERE NOT EXISTS (SELECT 1 FROM stat_indicator_definitions WHERE indicator_code = 'QC_FROZEN_SLICING_TIMEOUT_COUNT');

INSERT INTO stat_indicator_definitions
    (id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type, description, sort_order, enabled)
SELECT 'SID_QC_21', 'QC_FROZEN_DIAGNOSIS_TIMEOUT_COUNT', 'Frozen Diagnosis Timeout Count', 'QUALITY', 'DEPARTMENT', 'COUNT', 'M6 frozen diagnosis timeout indicator', 21, 1
WHERE NOT EXISTS (SELECT 1 FROM stat_indicator_definitions WHERE indicator_code = 'QC_FROZEN_DIAGNOSIS_TIMEOUT_COUNT');

INSERT INTO stat_indicator_definitions
    (id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type, description, sort_order, enabled)
SELECT 'SID_QC_22', 'QC_REPORT_CHANGE_COUNT', 'Report Change Count', 'QUALITY', 'DEPARTMENT', 'COUNT', 'M6 report change count indicator', 22, 1
WHERE NOT EXISTS (SELECT 1 FROM stat_indicator_definitions WHERE indicator_code = 'QC_REPORT_CHANGE_COUNT');

INSERT INTO stat_indicator_definitions
    (id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type, description, sort_order, enabled)
SELECT 'SID_QC_23', 'QC_REPORT_CHANGE_DOCTOR_COUNT', 'Report Change Doctor Count', 'QUALITY', 'DEPARTMENT', 'COUNT', 'M6 report change doctor count indicator', 23, 1
WHERE NOT EXISTS (SELECT 1 FROM stat_indicator_definitions WHERE indicator_code = 'QC_REPORT_CHANGE_DOCTOR_COUNT');

INSERT INTO stat_indicator_definitions
    (id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type, description, sort_order, enabled)
SELECT 'SID_QC_24', 'QC_REPORT_MODIFICATION_REASON_COUNT', 'Report Modification Reason Count', 'QUALITY', 'DEPARTMENT', 'COUNT', 'M6 report modification reason indicator', 24, 1
WHERE NOT EXISTS (SELECT 1 FROM stat_indicator_definitions WHERE indicator_code = 'QC_REPORT_MODIFICATION_REASON_COUNT');

INSERT INTO stat_indicator_definitions
    (id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type, description, sort_order, enabled)
SELECT 'SID_QC_25', 'QC_REPORT_REVISION_REASON_COUNT', 'Report Revision Reason Count', 'QUALITY', 'DEPARTMENT', 'COUNT', 'M6 report revision reason indicator', 25, 1
WHERE NOT EXISTS (SELECT 1 FROM stat_indicator_definitions WHERE indicator_code = 'QC_REPORT_REVISION_REASON_COUNT');

INSERT INTO stat_indicator_definitions
    (id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type, description, sort_order, enabled)
SELECT 'SID_QC_26', 'QC_UNQUALIFIED_SPECIMEN_RATE', 'Unqualified Specimen Rate', 'QUALITY', 'DEPARTMENT', 'RATE', 'M6 unqualified specimen rate indicator', 26, 1
WHERE NOT EXISTS (SELECT 1 FROM stat_indicator_definitions WHERE indicator_code = 'QC_UNQUALIFIED_SPECIMEN_RATE');

INSERT INTO stat_indicator_definitions
    (id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type, description, sort_order, enabled)
SELECT 'SID_QC_27', 'QC_UNQUALIFIED_SPECIMEN_REASON_COUNT', 'Unqualified Specimen Reason Count', 'QUALITY', 'DEPARTMENT', 'COUNT', 'M6 unqualified specimen reason indicator', 27, 1
WHERE NOT EXISTS (SELECT 1 FROM stat_indicator_definitions WHERE indicator_code = 'QC_UNQUALIFIED_SPECIMEN_REASON_COUNT');
