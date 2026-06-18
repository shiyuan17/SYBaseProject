package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpecimenDictionarySystemConfigV102ReplayMigrationTest {

    @Test
    void shouldReplayFromBaseline101WithoutDuplicatingDepartmentRelationTable() throws Exception {
        String url = "jdbc:h2:mem:specimen_dictionary_v102_replay_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("101"))
            .load()
            .migrate();

        rerunFromBaseline101(url);
        assertSeeded(url);

        rerunFromBaseline101(url);
        assertSeeded(url);
    }

    private void rerunFromBaseline101(String url) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE flyway_schema_history");
        }

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion(MigrationVersion.fromVersion("101"))
            .baselineDescription("specimen-dictionary-v102-replay")
            .load()
            .migrate();
    }

    private void assertSeeded(String url) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(1, queryInt(statement, """
                select count(*)
                from INFORMATION_SCHEMA.TABLES
                where upper(TABLE_NAME) = 'SYSTEM_CONFIG_ITEM_DEPARTMENTS'
                """));
            assertEquals(34, queryInt(statement, """
                select count(*)
                from system_config_items
                where value_type = 'SPECIMEN_DICTIONARY_ITEM'
                """));
            assertEquals(1, queryInt(statement, """
                select count(*)
                from flyway_schema_history
                where version = '102' and success = 1
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
