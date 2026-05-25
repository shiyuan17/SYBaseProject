package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationCenterV55MigrationTest {

    @Test
    void shouldCreateNotificationCenterSchemaAndSeedDefaults() throws Exception {
        String url = "jdbc:h2:mem:notification_center_v55_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("55"))
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(1, queryInt(statement, """
                select count(*)
                from INFORMATION_SCHEMA.TABLES
                where lower(TABLE_NAME) = 'user_notifications'
                """));
            assertEquals(1, queryInt(statement, """
                select count(*)
                from INFORMATION_SCHEMA.TABLES
                where lower(TABLE_NAME) = 'user_notification_preferences'
                """));
            assertEquals(queryInt(statement, """
                    select count(*)
                    from users
                    """),
                queryInt(statement, """
                    select count(*)
                    from user_notification_preferences
                    """));
            assertEquals(4, queryInt(statement, """
                select count(*)
                from user_notifications
                """));
            assertEquals(1, queryInt(statement, """
                select count(*)
                from flyway_schema_history
                where version = '55' and success = 1
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
