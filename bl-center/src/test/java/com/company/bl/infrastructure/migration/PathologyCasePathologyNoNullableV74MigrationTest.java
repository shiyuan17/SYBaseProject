package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PathologyCasePathologyNoNullableV74MigrationTest {

    @Test
    void shouldRelaxPathologyCasePathologyNoNullabilityWhenLegacySchemaIsBaselinedPastV73() throws Exception {
        String url =
            "jdbc:h2:mem:legacy_pathology_case_nullable_v74_" + System.nanoTime()
                + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("73"))
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals("NO", queryNullableFlag(statement));
            statement.execute("DROP TABLE flyway_schema_history");
        }

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion(MigrationVersion.fromVersion("73"))
            .baselineDescription("legacy-pathology-case-nullable-v74")
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals("YES", queryNullableFlag(statement));
        }
    }

    private String queryNullableFlag(Statement statement) throws Exception {
        try (ResultSet resultSet = statement.executeQuery("""
            SELECT IS_NULLABLE
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE LOWER(TABLE_NAME) = 'pathology_cases'
              AND LOWER(COLUMN_NAME) = 'pathology_no'
            """)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }
}
