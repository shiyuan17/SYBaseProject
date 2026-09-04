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

class BlockNumberingTemplateV120MigrationTest {

    @Test
    void shouldRestoreLegacyBlockTemplateAndAdvanceCurrentCounter() throws Exception {
        String url = "jdbc:h2:mem:block_numbering_template_v120_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
        migrateTo(url, "119");

        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                update numbering_rules
                set prefix_pattern = null, date_pattern = null, seq_length = 0
                where biz_type = 'BLOCK_NO'
                """);
            statement.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
            statement.executeUpdate("""
                insert into sampling_blocks
                    (id, case_id, specimen_id, sampling_id, sequence_no, block_code)
                values ('SBK-V120', 'CASE-V120', 'SP-V120', 'SMP-V120', 1, 'BK%s012')
                """.formatted(datePart));
            statement.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
        }

        migrateTo(url, "120");

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(1, queryInt(statement, """
                select count(*) from numbering_rules
                where biz_type = 'BLOCK_NO'
                  and prefix_pattern = 'BK'
                  and date_pattern = 'yyyyMMdd'
                  and seq_length = 3
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
