package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrossingReferenceOptionsV53MigrationTest {

    @Test
    void shouldSeedGrossingReferenceOptionsWithoutDuplicatesForLegacyBaseline() throws Exception {
        String url = "jdbc:h2:mem:legacy_m3_grossing_reference_v53_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("52"))
            .load()
            .migrate();

        rerunFromBaseline52(url);
        assertGrossingReferenceOptions(url);

        rerunFromBaseline52(url);
        assertGrossingReferenceOptions(url);
    }

    private void rerunFromBaseline52(String url) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE flyway_schema_history");
        }

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion(MigrationVersion.fromVersion("52"))
            .baselineDescription("legacy-m3-grossing-reference-v53")
            .load()
            .migrate();
    }

    private void assertGrossingReferenceOptions(String url) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(3, queryInt(statement, """
                SELECT COUNT(*)
                FROM system_config_categories
                WHERE category_code IN ('SPECIMEN_IMAGE_SIZE', 'CUT_SURFACE_FEATURE', 'MARGIN_MARKING')
                """));
            assertEquals(8, queryInt(statement, """
                SELECT COUNT(*)
                FROM system_config_items
                WHERE config_key IN (
                    'WORKFLOW_REFERENCE.SPECIMEN_IMAGE_SIZE.3_2X2_1X1_0CM',
                    'WORKFLOW_REFERENCE.SPECIMEN_IMAGE_SIZE.1_5X1_0X0_3CM',
                    'WORKFLOW_REFERENCE.CUT_SURFACE_FEATURE.GRAY_WHITE',
                    'WORKFLOW_REFERENCE.CUT_SURFACE_FEATURE.FIRM',
                    'WORKFLOW_REFERENCE.CUT_SURFACE_FEATURE.NECROSIS',
                    'WORKFLOW_REFERENCE.MARGIN_MARKING.UPPER',
                    'WORKFLOW_REFERENCE.MARGIN_MARKING.LOWER',
                    'WORKFLOW_REFERENCE.MARGIN_MARKING.BASE'
                )
                """));
            assertEquals(1, queryInt(statement, """
                SELECT COUNT(*)
                FROM system_config_items
                WHERE config_key = 'WORKFLOW_REFERENCE.CUT_SURFACE_FEATURE.GRAY_WHITE'
                  AND config_value = '灰白'
                """));
            assertEquals(1, queryInt(statement, """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version = '53' AND success = 1
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
