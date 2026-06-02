package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TechnicalRegistrationOverrideColumnsV71MigrationTest {

    @Test
    void shouldRepairTechnicalRegistrationOverrideColumnsWhenLegacySchemaIsBaselinedPastV70() throws Exception {
        String url = "jdbc:h2:mem:legacy_technical_registration_override_v71_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("58"))
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(0, queryInt(statement, columnCountSql("technical_history_summary_override")));
            assertEquals(0, queryInt(statement, columnCountSql("technical_external_pathology_diagnosis_override")));
            statement.execute("DROP TABLE flyway_schema_history");
        }

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion(MigrationVersion.fromVersion("70"))
            .baselineDescription("legacy-technical-registration-override-v71")
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(1, queryInt(statement, columnCountSql("technical_history_summary_override")));
            assertEquals(1, queryInt(statement, columnCountSql("technical_clinical_exam_surgery_override")));
            assertEquals(1, queryInt(statement, columnCountSql("technical_lab_imaging_override")));
            assertEquals(1, queryInt(statement, columnCountSql("technical_submission_requirement_override")));
            assertEquals(1, queryInt(statement, columnCountSql("technical_infectious_past_history_override")));
            assertEquals(1, queryInt(statement, columnCountSql("technical_external_pathology_diagnosis_override")));
        }
    }

    private String columnCountSql(String columnName) {
        return """
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE LOWER(TABLE_NAME) = 'application_registration_workbench'
              AND LOWER(COLUMN_NAME) = '%s'
            """.formatted(columnName);
    }

    private int queryInt(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
