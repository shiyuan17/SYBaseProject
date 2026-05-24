package com.company.bl.infrastructure.migration;

import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.infrastructure.persistence.JdbcSpecimenWorkflowRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpecimenCollectionPrinterCodeV51MigrationTest {

    @Test
    void shouldRepairCollectionPrinterCodeColumnWhenLegacySchemaIsBaselinedPastV50() throws Exception {
        String url = "jdbc:h2:mem:legacy_m2_collection_printer_v51_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("50"))
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(0, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'specimen_collection_records'
                  AND LOWER(COLUMN_NAME) = 'printer_code'
                """));
            statement.execute("DROP TABLE flyway_schema_history");
        }

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion(MigrationVersion.fromVersion("50"))
            .baselineDescription("legacy-m2-collection-printer-v51")
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(1, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'specimen_collection_records'
                  AND LOWER(COLUMN_NAME) = 'printer_code'
                """));
            connection.prepareStatement("""
                insert into specimen_collection_records
                    (id, application_id, specimen_id, collection_status, collection_scene, collection_mode,
                     label_print_batch_no, printer_code, collector_user_id, collector_name, collected_at, terminal_code, remarks)
                values
                    (?, ?, ?, ?, ?, ?,
                     ?, ?, ?, ?, ?, ?, ?)
                """).close();
        }
    }

    @Test
    void shouldRemainBackwardCompatibleWhenLegacySchemaHasNoPrinterCodeColumn() throws Exception {
        String url = "jdbc:h2:mem:legacy_m2_collection_printer_compat_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("50"))
            .load()
            .migrate();

        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, "sa", "");
        JdbcSpecimenWorkflowRepository repository = new JdbcSpecimenWorkflowRepository(new NamedParameterJdbcTemplate(dataSource));

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                insert into applications
                    (id, application_no, patient_id, application_type, status, application_form_status,
                     clinical_diagnosis, specimen_site, application_date, submission_date)
                values
                    ('APP-001', 'APP-001', 'PAT-001', 'ROUTINE', 'SUBMITTED', 'PENDING',
                     'diag', 'site', DATE '2026-05-24', DATE '2026-05-24')
                """);
            statement.execute("""
                insert into specimens
                    (id, application_id, specimen_no, barcode, specimen_status, fixation_status, label_print_batch_no, label_print_status)
                values
                    ('SPEC-001', 'APP-001', 'SP-001', 'BC-001', '%s', 'PENDING', 'LP-001', 'SUCCESS')
                """.formatted(SpecimenStatus.REGISTERED.name()));
        }

        repository.insertCollectionRecord(
            "APP-001",
            "SPEC-001",
            "REGISTERED",
            "WARD",
            "SURGERY",
            "LP-001",
            "P-01",
            "USER-001",
            "collector-a",
            LocalDateTime.of(2026, 5, 24, 11, 10, 0),
            "TERM-001",
            "legacy-schema");

        var snapshot = repository.findRegistrationSnapshotByApplicationIdAndBatchNo("APP-001", "LP-001").orElse(null);
        assertNotNull(snapshot);
        assertEquals("WARD", snapshot.collectionScene());
        assertEquals("USER-001", snapshot.operatorUserId());
        assertEquals("collector-a", snapshot.operatorName());
        assertNull(snapshot.printerCode());
        assertEquals("TERM-001", snapshot.terminalCode());
        assertEquals("legacy-schema", snapshot.remarks());
    }

    private int queryInt(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
