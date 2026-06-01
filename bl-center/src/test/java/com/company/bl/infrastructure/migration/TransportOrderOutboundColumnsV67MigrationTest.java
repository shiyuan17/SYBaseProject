package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransportOrderOutboundColumnsV67MigrationTest {

    @Test
    void shouldRepairTransportOrderOutboundColumnsWhenLegacySchemaIsBaselinedPastV66() throws Exception {
        String url = "jdbc:h2:mem:legacy_m2_transport_outbound_v67_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("66"))
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(0, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'transport_orders'
                  AND LOWER(COLUMN_NAME) = 'outbound_user_id'
                """));
            assertEquals(0, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'transport_orders'
                  AND LOWER(COLUMN_NAME) = 'outbound_user_name'
                """));
            statement.execute("DROP TABLE flyway_schema_history");
        }

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion(MigrationVersion.fromVersion("66"))
            .baselineDescription("legacy-m2-transport-outbound-v67")
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(1, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'transport_orders'
                  AND LOWER(COLUMN_NAME) = 'outbound_user_id'
                """));
            assertEquals(1, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'transport_orders'
                  AND LOWER(COLUMN_NAME) = 'outbound_user_name'
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
