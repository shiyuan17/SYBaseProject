package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpecimenReceiptQualityColumnsV50MigrationTest {

    @Test
    void shouldRepairReceiptQualityColumnsWhenLegacySchemaIsBaselinedPastV49() throws Exception {
        String url = "jdbc:h2:mem:legacy_m2_receipt_quality_v50_" + System.nanoTime() + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("44"))
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(0, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'specimen_receipts'
                  AND LOWER(COLUMN_NAME) = 'quality_check_result'
                """));
            assertEquals(0, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'specimen_receipts'
                  AND LOWER(COLUMN_NAME) = 'quality_issue_codes'
                """));
            statement.execute("DROP TABLE flyway_schema_history");
        }

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion(MigrationVersion.fromVersion("49"))
            .baselineDescription("legacy-m2-receipt-quality-v50")
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(1, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'specimen_receipts'
                  AND LOWER(COLUMN_NAME) = 'quality_check_result'
                """));
            assertEquals(1, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'specimen_receipts'
                  AND LOWER(COLUMN_NAME) = 'quality_issue_codes'
                """));
            connection.prepareStatement("""
                insert into specimen_receipts
                    (id, application_id, case_id, specimen_id, transport_order_id, receipt_status, container_count,
                     quality_check_result, quality_issue_codes, barcode, received_by_user_id, received_by_name,
                     received_at, terminal_code, reject_reason, return_reason, remarks)
                values
                    (?, ?, ?, ?, ?, ?, ?,
                     ?, ?, ?, ?, ?,
                     ?, ?, ?, ?, ?)
                """).close();
        }
    }

    private int queryInt(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
