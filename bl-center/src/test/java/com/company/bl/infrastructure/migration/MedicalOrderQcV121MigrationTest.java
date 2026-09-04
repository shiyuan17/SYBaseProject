package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicalOrderQcV121MigrationTest {

    @Test
    void shouldPreserveHistoryAndBackfillOnlyLatestUnambiguousEvaluations() throws Exception {
        String url = "jdbc:h2:mem:medical_order_qc_v121_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
        migrateTo(url, "120");

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
            statement.executeUpdate("""
                insert into pathology_cases (id, application_id, pathology_no, case_status)
                values ('CASE-V121', 'APP-V121', 'P-V121', 'IN_PROGRESS')
                """);
            statement.executeUpdate("""
                insert into slides
                    (id, case_id, specimen_id, slicing_id, embedding_box_id, sampling_block_id,
                     slide_no, slide_status)
                values ('SLIDE-V121', 'CASE-V121', 'SPEC-V121', 'SLICING-V121', 'BOX-V121',
                        'BLOCK-V121', 'S-V121', 'COMPLETED')
                """);
            statement.executeUpdate("""
                insert into medical_orders
                    (id, case_id, order_number, order_content, order_type, execution_scope,
                     billing_status, status, target_type, target_slide_id, target_slide_no)
                values ('ORDER-SLIDE-V121', 'CASE-V121', 'MO-SLIDE-V121', 'slide target', 'ROUTINE',
                        'CASE', 'UNBILLED', 'COMPLETED', 'SLIDE', 'SLIDE-V121', 'S-V121'),
                       ('ORDER-BLOCK-V121', 'CASE-V121', 'MO-BLOCK-V121', 'block target', 'ROUTINE',
                        'CASE', 'UNBILLED', 'COMPLETED', 'BLOCK', null, null)
                """);
            statement.executeUpdate("""
                insert into medical_order_qc_evaluations
                    (id, order_id, case_id, qc_aspect, total_score, grade, evaluated_at, created_at)
                values ('QC-SLIDE-OLD', 'ORDER-SLIDE-V121', 'CASE-V121', 'SLIDE', 80, '乙',
                        timestamp '2026-01-01 10:00:00', timestamp '2026-01-01 10:00:00'),
                       ('QC-SLIDE-LATEST', 'ORDER-SLIDE-V121', 'CASE-V121', 'SLIDE', 90, '乙',
                        timestamp '2026-01-02 10:00:00', timestamp '2026-01-02 10:00:00'),
                       ('QC-GROSSING-LATEST', 'ORDER-SLIDE-V121', 'CASE-V121', 'GROSSING', 95, '甲',
                        timestamp '2026-01-03 10:00:00', timestamp '2026-01-03 10:00:00'),
                       ('QC-BLOCK-LATEST', 'ORDER-BLOCK-V121', 'CASE-V121', 'SLIDE', 88, '乙',
                        timestamp '2026-01-04 10:00:00', timestamp '2026-01-04 10:00:00')
                """);
            statement.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
            assertEquals(4, queryInt(statement, "select count(*) from medical_order_qc_evaluations"));
        }

        migrateTo(url, "121");

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(4, queryInt(statement, "select count(*) from medical_order_qc_evaluations"));
            assertEquals("SLIDE-V121", queryString(statement,
                "select target_slide_id from medical_order_qc_evaluations where id = 'QC-SLIDE-LATEST'"));
            assertEquals("S-V121", queryString(statement,
                "select target_slide_no from medical_order_qc_evaluations where id = 'QC-SLIDE-LATEST'"));
            assertEquals("SLIDE-V121", queryString(statement,
                "select target_slide_id from medical_order_qc_evaluations where id = 'QC-GROSSING-LATEST'"));
            assertNull(queryString(statement,
                "select target_slide_id from medical_order_qc_evaluations where id = 'QC-SLIDE-OLD'"));
            assertNull(queryString(statement,
                "select target_slide_id from medical_order_qc_evaluations where id = 'QC-BLOCK-LATEST'"));
            assertEquals(0, queryInt(statement,
                "select max(version) from medical_order_qc_evaluations"));
            assertTrue(indexExists(connection, "IDX_MO_QC_ORDER_ASPECT_SLIDE"));
        }
    }

    private void migrateTo(String url, String version) {
        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion(version))
            .load()
            .migrate();
    }

    private int queryInt(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private String queryString(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private boolean indexExists(Connection connection, String expectedName) throws Exception {
        try (ResultSet indexes = connection.getMetaData()
            .getIndexInfo(null, null, "medical_order_qc_evaluations", false, false)) {
            while (indexes.next()) {
                if (expectedName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
