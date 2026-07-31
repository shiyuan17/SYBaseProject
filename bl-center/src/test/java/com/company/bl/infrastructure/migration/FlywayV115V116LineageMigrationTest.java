package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayV115V116LineageMigrationTest {

    @Test
    void shouldUpgradeDeployedV116LineageToV118WithoutRepair() throws Exception {
        String url = "jdbc:h2:mem:flyway_v115_v116_lineage_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        migrateTo(url, "116");
        assertInstalled(url, "115");
        assertInstalled(url, "116");

        Flyway upgrade = configure(url, "118");
        upgrade.migrate();

        assertTrue(configure(url, "118").validateWithResult().validationSuccessful);
        assertInstalled(url, "117");
        assertInstalled(url, "118");
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(1, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE LOWER(TABLE_NAME) = 'grossing_drafts'
                """));
            assertEquals(3, queryInt(statement, """
                SELECT COUNT(*)
                FROM permissions
                WHERE id IN (
                    'PERM_M5_LOAN_APPROVE',
                    'PERM_M5_LOAN_REJECT',
                    'PERM_M5_LOAN_BORROW'
                )
                """));
        }
    }

    @Test
    void shouldRecordV118WhenGrossingDraftsWereCreatedByLegacyV115() throws Exception {
        String url = "jdbc:h2:mem:flyway_v118_existing_grossing_drafts_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        migrateTo(url, "116");
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE grossing_drafts (task_id VARCHAR(64) PRIMARY KEY)");
        }

        migrateTo(url, "118");

        assertInstalled(url, "118");
    }

    private void migrateTo(String url, String version) {
        configure(url, version).migrate();
    }

    private Flyway configure(String url, String version) {
        return Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion(version))
            .load();
    }

    private void assertInstalled(String url, String version) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(1, queryInt(statement, """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version = '%s' AND success = 1
                """.formatted(version)));
        }
    }

    private int queryInt(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
