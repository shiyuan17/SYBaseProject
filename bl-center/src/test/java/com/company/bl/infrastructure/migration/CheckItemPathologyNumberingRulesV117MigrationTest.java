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

class CheckItemPathologyNumberingRulesV117MigrationTest {

    @Test
    void shouldSeedRulesBackfillCurrentPeriodAndReplayIdempotently() throws Exception {
        String url = "jdbc:h2:mem:check_item_rules_v117_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        migrateTo(url, "115");
        String routinePathologyNo = "BL" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "0321";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                insert into applications (id, application_no, application_type, status)
                values ('APP-V116', 'APP-V116', 'ROUTINE', 'RECEIVED')
                """);
            statement.executeUpdate("""
                insert into pathology_cases (id, application_id, pathology_no, case_status)
                values ('CASE-V116', 'APP-V116', '%s', 'RECEIVED')
                """.formatted(routinePathologyNo));
        }

        migrateTo(url, "117");
        assertMigrated(url);

        replayFromBaseline116(url);
        assertMigrated(url);
    }

    private void migrateTo(String url, String version) {
        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion(version))
            .load()
            .migrate();
    }

    private void replayFromBaseline116(String url) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE flyway_schema_history");
        }
        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion(MigrationVersion.fromVersion("116"))
            .baselineDescription("check-item-rules-v117-replay")
            .load()
            .migrate();
    }

    private void assertMigrated(String url) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(25, queryInt(statement, """
                select count(*) from numbering_rules
                where biz_type like 'CHECK_ITEM_PATHOLOGY_NO:%'
                """));
            assertEquals(1, queryInt(statement, """
                select count(*) from numbering_rules
                where biz_type = 'CHECK_ITEM_PATHOLOGY_NO:ROUTINE'
                  and format_template = 'BL{YYYY}{MM}{DD}{0:D4}'
                  and "AUTO_INCREMENT" = 1
                """));
            assertEquals(321, queryInt(statement, """
                select current_value from numbering_counters
                where rule_code = 'RULE_CHECK_ITEM_ROUTINE'
                  and period_key = '%s' and scope_key = 'GLOBAL'
                """.formatted(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE))));
            assertEquals(1, queryInt(statement, """
                select count(*) from menus
                where id = 'MENU_CHECK_ITEM_RULES'
                  and menu_code = 'CHECK_ITEM_RULES'
                  and path = '/system/check-item-rules'
                  and component_name = 'CheckItemRules'
                """));
            assertEquals(queryInt(statement, """
                    select count(*) from role_menus where menu_id = 'MENU_NUMBERING'
                    """),
                queryInt(statement, """
                    select count(*) from role_menus where menu_id = 'MENU_CHECK_ITEM_RULES'
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
