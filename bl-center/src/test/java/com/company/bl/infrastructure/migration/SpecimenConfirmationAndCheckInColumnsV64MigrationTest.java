package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpecimenConfirmationAndCheckInColumnsV64MigrationTest {

    @Test
    void shouldRepairConfirmationAndCheckInColumnsWhenLegacySchemaIsBaselinedPastV63() throws Exception {
        String url = "jdbc:h2:mem:legacy_m2_specimen_confirmation_v64_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("62"))
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(0, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'specimens'
                  AND LOWER(COLUMN_NAME) = 'specimen_confirmed_at'
                """));
            assertEquals(0, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'specimens'
                  AND LOWER(COLUMN_NAME) = 'check_in_status'
                """));
            assertEquals(0, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'specimens'
                  AND LOWER(COLUMN_NAME) = 'checked_in_at'
                """));
            assertEquals(0, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'specimens'
                  AND LOWER(COLUMN_NAME) = 'checked_in_by_name'
                """));
            statement.execute("DROP TABLE flyway_schema_history");
        }

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion(MigrationVersion.fromVersion("63"))
            .baselineDescription("legacy-m2-specimen-confirmation-v64")
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(1, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'specimens'
                  AND LOWER(COLUMN_NAME) = 'specimen_confirmed_at'
                """));
            assertEquals(1, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'specimens'
                  AND LOWER(COLUMN_NAME) = 'check_in_status'
                """));
            assertEquals(1, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'specimens'
                  AND LOWER(COLUMN_NAME) = 'checked_in_at'
                """));
            assertEquals(1, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'specimens'
                  AND LOWER(COLUMN_NAME) = 'checked_in_by_name'
                """));
        }
    }

    private int queryInt(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
