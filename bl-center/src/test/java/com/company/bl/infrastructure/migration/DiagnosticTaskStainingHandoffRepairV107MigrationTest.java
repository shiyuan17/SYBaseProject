package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiagnosticTaskStainingHandoffRepairV107MigrationTest {

    @Test
    void shouldBackfillPendingDiagnosticTaskForFullyStainedCases() throws Exception {
        String url = "jdbc:h2:mem:diag_task_staining_handoff_v107_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("106"))
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                insert into applications
                    (id, application_no, patient_name, status, application_form_status, created_at, updated_at)
                values
                    ('APP_V107_READY', 'APP-V107-READY', 'Patient Ready', 'RECEIVED', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('APP_V107_ACTIVE', 'APP-V107-ACTIVE', 'Patient Active', 'RECEIVED', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('APP_V107_EXISTING', 'APP-V107-EXISTING', 'Patient Existing', 'RECEIVED', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
            statement.execute("""
                insert into pathology_cases
                    (id, application_id, pathology_no, case_status, created_at, updated_at)
                values
                    ('CASE_V107_READY', 'APP_V107_READY', 'BL-V107-READY', 'STAINING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('CASE_V107_ACTIVE', 'APP_V107_ACTIVE', 'BL-V107-ACTIVE', 'STAINING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('CASE_V107_EXISTING', 'APP_V107_EXISTING', 'BL-V107-EXISTING', 'STAINING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
            statement.execute("""
                insert into technical_pending_tasks
                    (id, application_id, case_id, task_type, task_status, object_type, object_id, created_at, updated_at)
                values
                    ('TT_V107_READY_1', 'APP_V107_READY', 'CASE_V107_READY', 'STAINING', 'COMPLETED', 'SLIDE', 'SLD_V107_READY_1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('TT_V107_READY_2', 'APP_V107_READY', 'CASE_V107_READY', 'STAINING', 'COMPLETED', 'SLIDE', 'SLD_V107_READY_2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('TT_V107_ACTIVE_1', 'APP_V107_ACTIVE', 'CASE_V107_ACTIVE', 'STAINING', 'COMPLETED', 'SLIDE', 'SLD_V107_ACTIVE_1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('TT_V107_ACTIVE_2', 'APP_V107_ACTIVE', 'CASE_V107_ACTIVE', 'STAINING', 'IN_PROGRESS', 'SLIDE', 'SLD_V107_ACTIVE_2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    ('TT_V107_EXISTING_1', 'APP_V107_EXISTING', 'CASE_V107_EXISTING', 'STAINING', 'COMPLETED', 'SLIDE', 'SLD_V107_EXISTING_1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
            statement.execute("""
                insert into diagnostic_tasks
                    (id, case_id, pathology_no, task_type, status, priority, created_at, updated_at)
                values
                    ('DT_V107_EXISTING', 'CASE_V107_EXISTING', 'BL-V107-EXISTING', 'PRIMARY', 'PENDING', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        }

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(1, queryInt(statement, """
                select count(*)
                from diagnostic_tasks
                where case_id = 'CASE_V107_READY'
                  and task_type = 'PRIMARY'
                  and status = 'PENDING'
                """));
            assertEquals("DIAGNOSIS_PENDING", queryString(
                statement,
                "select case_status from pathology_cases where id = 'CASE_V107_READY'"
            ));
            assertEquals(1, queryInt(statement, """
                select count(*)
                from workflow_events
                where case_id = 'CASE_V107_READY'
                  and node_code = 'DIAGNOSIS_ASSIGN'
                  and event_type = 'CREATE'
                  and event_status = 'SUCCESS'
                """));
            assertEquals(0, queryInt(statement, "select count(*) from diagnostic_tasks where case_id = 'CASE_V107_ACTIVE'"));
            assertEquals(1, queryInt(statement, "select count(*) from diagnostic_tasks where case_id = 'CASE_V107_EXISTING'"));
        }
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
}
