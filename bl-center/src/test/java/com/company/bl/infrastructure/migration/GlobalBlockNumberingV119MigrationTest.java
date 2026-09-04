package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalBlockNumberingV119MigrationTest {

    @Test
    void shouldEnforceGlobalScopeAndReconcileCurrentPeriodCounter() throws Exception {
        String url = "jdbc:h2:mem:global_block_numbering_v119_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        migrateTo(url, "118");
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("update numbering_rules set scope_type = 'CASE' where biz_type = 'BLOCK_NO'");
            statement.executeUpdate("""
                insert into numbering_counters
                    (id, rule_code, period_key, scope_key, current_value, version, updated_at)
                values ('NC-BLOCK-CASE-1', 'RULE_BLOCK_NO', '%s', 'CASE-1', 7, 0, current_timestamp),
                       ('NC-BLOCK-CASE-2', 'RULE_BLOCK_NO', '%s', 'CASE-2', 12, 0, current_timestamp)
                """.formatted(datePart, datePart));
        }

        migrateTo(url, "119");

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(1, queryInt(statement, """
                select count(*) from numbering_rules
                where biz_type = 'BLOCK_NO' and scope_type = 'GLOBAL'
                """));
            assertEquals(12, queryInt(statement, """
                select current_value from numbering_counters
                where rule_code = 'RULE_BLOCK_NO' and period_key = '%s' and scope_key = 'GLOBAL'
                """.formatted(datePart)));
        }
    }

    private void migrateTo(String url, String version) {
        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion(version))
            .load()
            .migrate();
    }

    private int queryInt(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
