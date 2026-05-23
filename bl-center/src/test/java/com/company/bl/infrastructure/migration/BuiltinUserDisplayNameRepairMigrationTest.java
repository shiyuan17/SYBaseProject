package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuiltinUserDisplayNameRepairMigrationTest {

    @Test
    void shouldRepairBuiltinUserNamesAndHistoricalDisplayColumns() throws Exception {
        String url = "jdbc:h2:mem:builtin_name_repair_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("40"))
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            executeAll(statement,
                """
                INSERT INTO applications
                    (id, application_no, status, submitting_doctor_user_id, submitting_doctor_name, created_at, updated_at)
                VALUES
                    ('APP_REPAIR', 'APP-REPAIR-001', 'REGISTERED', 'USER_M2_IMPORT', 'M2 Import', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                """
                INSERT INTO pathology_cases
                    (id, application_id, pathology_no, case_status, received_by_user_id, received_by_name, created_at, updated_at)
                VALUES
                    ('CASE_REPAIR', 'APP_REPAIR', 'PA-REPAIR-001', 'RECEIVED', 'USER_M2_RECEIVE', 'M2 Receive', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                """
                INSERT INTO specimens
                    (id, application_id, case_id, specimen_no, barcode, specimen_status,
                     applicant_doctor_user_id, applicant_doctor_name, registered_by_user_id, registered_by_name,
                     created_at, updated_at)
                VALUES
                    ('SPEC_REPAIR', 'APP_REPAIR', 'CASE_REPAIR', 'SP-001', 'BC-REPAIR-001', 'RECEIVED',
                     'USER_M2_IMPORT', 'M2 Import', 'USER_M2_REGISTER', 'M2 Register',
                     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                """
                INSERT INTO specimen_collection_records
                    (id, application_id, specimen_id, collection_status, collector_user_id, collector_name)
                VALUES
                    ('SCR_REPAIR', 'APP_REPAIR', 'SPEC_REPAIR', 'COLLECTED', 'USER_M2_REGISTER', 'M2 Register')
                """,
                """
                INSERT INTO specimen_fixation_records
                    (id, application_id, specimen_id, fixation_status, verified_by_user_id, verified_by_name)
                VALUES
                    ('SFR_REPAIR', 'APP_REPAIR', 'SPEC_REPAIR', 'VERIFIED', 'USER_M2_FIXATION', 'M2 Fixation')
                """,
                """
                INSERT INTO transport_orders
                    (id, transport_order_no, application_id, order_status, handover_user_id, handover_user_name,
                     receiver_user_id, receiver_user_name, created_at, updated_at)
                VALUES
                    ('TO_REPAIR', 'TR-REPAIR-001', 'APP_REPAIR', 'HANDED_OVER', 'USER_M2_TRANSPORT', 'M2 Transport',
                     'USER_M2_RECEIVE', 'M2 Receive', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                """
                INSERT INTO transport_order_items
                    (id, transport_order_id, application_id, specimen_id, item_status, verified_by_user_id, verified_by_name)
                VALUES
                    ('TOI_REPAIR', 'TO_REPAIR', 'APP_REPAIR', 'SPEC_REPAIR', 'VERIFIED', 'USER_M2_TRACKING', 'M2 Tracking')
                """,
                """
                INSERT INTO specimen_receipts
                    (id, application_id, case_id, specimen_id, transport_order_id, receipt_status, barcode,
                     received_by_user_id, received_by_name)
                VALUES
                    ('SR_REPAIR', 'APP_REPAIR', 'CASE_REPAIR', 'SPEC_REPAIR', 'TO_REPAIR', 'RECEIVED', 'BC-REPAIR-001',
                     'USER_M2_RECEIVE', 'M2 Receive')
                """,
                """
                INSERT INTO workflow_events
                    (id, application_id, specimen_id, case_id, transport_order_id, node_code, event_type, event_status,
                     event_time, operator_user_id, operator_name)
                VALUES
                    ('WE_REPAIR', 'APP_REPAIR', 'SPEC_REPAIR', 'CASE_REPAIR', 'TO_REPAIR', 'TRANSPORT',
                     'HANDOVER', 'DONE', CURRENT_TIMESTAMP, 'USER_M2_TRANSPORT', 'M2 Transport')
                """,
                """
                INSERT INTO samplings
                    (id, case_id, specimen_id, sampling_status, block_count, sampled_by_user_id, sampled_by_name,
                     created_at, updated_at)
                VALUES
                    ('SAMP_REPAIR', 'CASE_REPAIR', 'SPEC_REPAIR', 'COMPLETED', 1, 'USER_M3_GROSSING', 'M3 Grossing',
                     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                """
                INSERT INTO dehydration_batches
                    (id, case_id, batch_no, batch_status, basket_no, operator_user_id, operator_name, created_at, updated_at)
                VALUES
                    ('DB_REPAIR', 'CASE_REPAIR', 'DB-REPAIR-001', 'COMPLETED', 'BASKET-001',
                     'USER_M3_DEHYDRATION', 'M3 Dehydration', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                """
                INSERT INTO rework_orders
                    (id, case_id, specimen_id, rework_type, status, reason, requested_by_user_id, requested_by_name,
                     executed_by_user_id, executed_by_name, created_at, updated_at)
                VALUES
                    ('RO_REPAIR', 'CASE_REPAIR', 'SPEC_REPAIR', 'RESTAIN', 'COMPLETED', 'repair migration test',
                     'USER_M3_REWORK', 'M3 Rework', 'USER_M3_STAINING', 'M3 Staining',
                     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                """
                INSERT INTO case_media_assets
                    (id, case_id, specimen_id, object_type, object_id, media_type, file_url,
                     captured_by_user_id, captured_by_name, created_at, updated_at)
                VALUES
                    ('CMA_REPAIR', 'CASE_REPAIR', 'SPEC_REPAIR', 'CASE', 'CASE_REPAIR', 'IMAGE', '/repair/test.png',
                     'USER_M3_TRACKING', 'M3 Tracking', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                """
                INSERT INTO diagnostic_tasks
                    (id, case_id, specimen_id, pathology_no, task_type, status,
                     assigned_by_user_id, assigned_by_name,
                     diagnosis_doctor_user_id, diagnosis_doctor_name,
                     primary_doctor_user_id, primary_doctor_name,
                     review_doctor_user_id, review_doctor_name,
                     reviewer_user_id, reviewer_name,
                     created_at, updated_at)
                VALUES
                    ('DT_REPAIR', 'CASE_REPAIR', 'SPEC_REPAIR', 'PA-REPAIR-001', 'ROUTINE', 'ASSIGNED',
                     'USER_M4_ASSIGN', 'M4 Assign',
                     'USER_M4_DIAGNOSIS', 'M4 Diagnosis',
                     'USER_M4_DIAGNOSIS', 'M4 Diagnosis',
                     'USER_M4_REVIEW', 'M4 Review',
                     'USER_M4_SIGN', 'M4 Sign',
                     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                """
                INSERT INTO pathology_reports
                    (id, case_id, task_id, report_no, pathology_no, report_status,
                     reviewer_user_id, reviewer_name, signed_by_user_id, signed_by_name, created_at, updated_at)
                VALUES
                    ('PR_REPAIR', 'CASE_REPAIR', 'DT_REPAIR', 'RP-REPAIR-001', 'PA-REPAIR-001', 'SIGNED',
                     'USER_M4_REVIEW', 'M4 Review', 'USER_M4_SIGN', 'M4 Sign', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                """
                INSERT INTO report_versions
                    (id, report_id, case_id, version_no, version_status, signed_by_user_id, signed_by_name, created_at)
                VALUES
                    ('RV_REPAIR', 'PR_REPAIR', 'CASE_REPAIR', 1, 'SIGNED', 'USER_M4_SIGN', 'M4 Sign', CURRENT_TIMESTAMP)
                """,
                """
                INSERT INTO report_revision_requests
                    (id, case_id, report_id, current_version_no, request_status,
                     requested_by_user_id, requested_by_name, reviewed_by_user_id, reviewed_by_name,
                     created_at, updated_at)
                VALUES
                    ('RRR_REPAIR', 'CASE_REPAIR', 'PR_REPAIR', 1, 'APPROVED',
                     'USER_M4_DIAGNOSIS', 'M4 Diagnosis', 'USER_M4_REVIEW', 'M4 Review',
                     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                """
                INSERT INTO medical_orders
                    (id, case_id, order_number, order_content, order_type, execution_scope, billing_status, status,
                     doctor_user_id, doctor_name, executor_user_id, executor_name, created_at, updated_at)
                VALUES
                    ('MO_REPAIR', 'CASE_REPAIR', 'MO-REPAIR-001', 'repair migration test order', 'EXAM', 'INTERNAL',
                     'PENDING', 'CREATED', 'USER_M4_DIAGNOSIS', 'M4 Diagnosis',
                     'USER_M4_ORDER_EXECUTE', 'M4 Order Execute', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                """
                INSERT INTO consultation_cases
                    (id, case_id, consultation_type, status, requested_by_user_id, requested_by_name,
                     host_user_id, host_name, created_at, updated_at)
                VALUES
                    ('CC_REPAIR', 'CASE_REPAIR', 'INTERNAL', 'CREATED', 'USER_M4_TRACKING', 'M4 Tracking',
                     'USER_M4_DIAGNOSIS', 'M4 Diagnosis', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                """
                INSERT INTO consultation_participants
                    (id, consultation_id, case_id, participant_user_id, participant_name, participant_role,
                     drafted_by_user_id, drafted_by_name, created_at, updated_at)
                VALUES
                    ('CP_REPAIR', 'CC_REPAIR', 'CASE_REPAIR', 'USER_M4_REVIEW', 'M4 Review', 'REVIEWER',
                     'USER_M4_ASSIGN', 'M4 Assign', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                """
                INSERT INTO operation_logs
                    (id, module_code, business_type, business_id, operation_name, operation_result,
                     operator_user_id, operator_name, operation_content)
                VALUES
                    ('OL_REPAIR', 'SYSTEM', 'MIGRATION', 'CASE_REPAIR', 'repair display name', 'SUCCESS',
                     'USER_M1_ADMIN', 'M1 Admin', 'repair migration test')
                """
            );
        }

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals("病理科管理员", queryString(statement, "SELECT name FROM users WHERE id = 'USER_M1_ADMIN'"));
            assertEquals("标本登记员", queryString(statement, "SELECT name FROM users WHERE id = 'USER_M2_REGISTER'"));
            assertEquals("取材员", queryString(statement, "SELECT name FROM users WHERE id = 'USER_M3_GROSSING'"));
            assertEquals("医嘱执行员", queryString(statement, "SELECT name FROM users WHERE id = 'USER_M4_ORDER_EXECUTE'"));

            assertEquals("病理科管理员", queryString(statement, "SELECT operator_name FROM operation_logs WHERE id = 'OL_REPAIR'"));
            assertEquals("临床导入员", queryString(statement, "SELECT submitting_doctor_name FROM applications WHERE id = 'APP_REPAIR'"));
            assertEquals("标本接收员", queryString(statement, "SELECT received_by_name FROM pathology_cases WHERE id = 'CASE_REPAIR'"));
            assertEquals("标本登记员", queryString(statement, "SELECT registered_by_name FROM specimens WHERE id = 'SPEC_REPAIR'"));
            assertEquals("固定核验员", queryString(statement, "SELECT verified_by_name FROM specimen_fixation_records WHERE id = 'SFR_REPAIR'"));
            assertEquals("转运交接员", queryString(statement, "SELECT handover_user_name FROM transport_orders WHERE id = 'TO_REPAIR'"));
            assertEquals("标本接收员", queryString(statement, "SELECT receiver_user_name FROM transport_orders WHERE id = 'TO_REPAIR'"));
            assertEquals("标本追踪员", queryString(statement, "SELECT verified_by_name FROM transport_order_items WHERE id = 'TOI_REPAIR'"));
            assertEquals("标本接收员", queryString(statement, "SELECT received_by_name FROM specimen_receipts WHERE id = 'SR_REPAIR'"));
            assertEquals("转运交接员", queryString(statement, "SELECT operator_name FROM workflow_events WHERE id = 'WE_REPAIR'"));
            assertEquals("取材员", queryString(statement, "SELECT sampled_by_name FROM samplings WHERE id = 'SAMP_REPAIR'"));
            assertEquals("脱水员", queryString(statement, "SELECT operator_name FROM dehydration_batches WHERE id = 'DB_REPAIR'"));
            assertEquals("返工员", queryString(statement, "SELECT requested_by_name FROM rework_orders WHERE id = 'RO_REPAIR'"));
            assertEquals("染色员", queryString(statement, "SELECT executed_by_name FROM rework_orders WHERE id = 'RO_REPAIR'"));
            assertEquals("技术追踪员", queryString(statement, "SELECT captured_by_name FROM case_media_assets WHERE id = 'CMA_REPAIR'"));
            assertEquals("诊断分派员", queryString(statement, "SELECT assigned_by_name FROM diagnostic_tasks WHERE id = 'DT_REPAIR'"));
            assertEquals("诊断医生", queryString(statement, "SELECT diagnosis_doctor_name FROM diagnostic_tasks WHERE id = 'DT_REPAIR'"));
            assertEquals("审核医生", queryString(statement, "SELECT review_doctor_name FROM diagnostic_tasks WHERE id = 'DT_REPAIR'"));
            assertEquals("签发医生", queryString(statement, "SELECT reviewer_name FROM diagnostic_tasks WHERE id = 'DT_REPAIR'"));
            assertEquals("审核医生", queryString(statement, "SELECT reviewer_name FROM pathology_reports WHERE id = 'PR_REPAIR'"));
            assertEquals("签发医生", queryString(statement, "SELECT signed_by_name FROM pathology_reports WHERE id = 'PR_REPAIR'"));
            assertEquals("签发医生", queryString(statement, "SELECT signed_by_name FROM report_versions WHERE id = 'RV_REPAIR'"));
            assertEquals("诊断医生", queryString(statement, "SELECT requested_by_name FROM report_revision_requests WHERE id = 'RRR_REPAIR'"));
            assertEquals("审核医生", queryString(statement, "SELECT reviewed_by_name FROM report_revision_requests WHERE id = 'RRR_REPAIR'"));
            assertEquals("诊断医生", queryString(statement, "SELECT doctor_name FROM medical_orders WHERE id = 'MO_REPAIR'"));
            assertEquals("医嘱执行员", queryString(statement, "SELECT executor_name FROM medical_orders WHERE id = 'MO_REPAIR'"));
            assertEquals("报告追踪员", queryString(statement, "SELECT requested_by_name FROM consultation_cases WHERE id = 'CC_REPAIR'"));
            assertEquals("诊断医生", queryString(statement, "SELECT host_name FROM consultation_cases WHERE id = 'CC_REPAIR'"));
            assertEquals("审核医生", queryString(statement, "SELECT participant_name FROM consultation_participants WHERE id = 'CP_REPAIR'"));
            assertEquals("诊断分派员", queryString(statement, "SELECT drafted_by_name FROM consultation_participants WHERE id = 'CP_REPAIR'"));
        }
    }

    private void executeAll(Statement statement, String... sqlStatements) throws Exception {
        for (String sqlStatement : sqlStatements) {
            statement.execute(sqlStatement);
        }
    }

    private String queryString(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }
}
