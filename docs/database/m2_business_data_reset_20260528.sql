-- M2/M3/M4/M5/M6 业务流水清理脚本
-- 使用前提：
-- 1. 仅用于允许清空业务流水数据的环境。
-- 2. 必须先执行包含 V62__add_specimen_verification_timestamps.sql 的 Flyway 迁移。
-- 3. 请先完成数据库备份，再执行本脚本。

DELETE FROM billing_records;

DELETE FROM consultation_participants;
DELETE FROM consultation_cases;
DELETE FROM report_revision_requests;
DELETE FROM report_versions;
DELETE FROM pathology_reports;
DELETE FROM diagnostic_tasks;
DELETE FROM medical_orders;

DELETE FROM material_loans;
DELETE FROM specimen_storage_records;

DELETE FROM slide_qc_evaluations;
DELETE FROM slide_stainings;
DELETE FROM rework_orders;
DELETE FROM case_media_assets;
DELETE FROM slides;
DELETE FROM slicings;
DELETE FROM embedding_boxes;
DELETE FROM embeddings;
DELETE FROM dehydration_batch_items;
DELETE FROM dehydration_batches;
DELETE FROM sampling_blocks;
DELETE FROM samplings;

DELETE FROM technical_pending_tasks;
DELETE FROM workflow_events;
DELETE FROM specimen_receipts;
DELETE FROM transport_order_items;
DELETE FROM transport_orders;
DELETE FROM specimen_fixation_records;
DELETE FROM specimen_collection_records;
DELETE FROM application_registration_workbench;
DELETE FROM specimens;
DELETE FROM pathology_cases;
DELETE FROM applications;
