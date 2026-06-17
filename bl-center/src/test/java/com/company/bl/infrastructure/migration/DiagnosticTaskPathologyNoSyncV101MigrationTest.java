package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiagnosticTaskPathologyNoSyncV101MigrationTest {

    @Test
    void shouldBackfillDiagnosticTaskPathologyNoFromCurrentCaseNumber() throws Exception {
        String url = "jdbc:h2:mem:diag_task_pathology_sync_v101_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target("99")
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                insert into applications
                    (id, application_no, patient_name, status, application_form_status, created_at, updated_at)
                values
                    ('APP_V101', 'APP-V101', 'Patient V101', 'RECEIVED', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
            statement.execute("""
                insert into pathology_cases
                    (id, application_id, pathology_no, case_status, created_at, updated_at)
                values
                    ('CASE_V101', 'APP_V101', 'HZ2610002', 'RECEIVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
            statement.execute("""
                insert into diagnostic_tasks
                    (id, case_id, pathology_no, task_type, status, priority, created_at, updated_at)
                values
                    ('DT_V101', 'CASE_V101', 'BL202606170003', 'PRIMARY', 'PENDING', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        }

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals("HZ2610002", queryString(
                statement,
                "select pathology_no from diagnostic_tasks where id = 'DT_V101'"
            ));
        }
    }

    private String queryString(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }
}
