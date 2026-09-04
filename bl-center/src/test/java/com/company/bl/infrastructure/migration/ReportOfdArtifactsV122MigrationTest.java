package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportOfdArtifactsV122MigrationTest {

    @Test
    void shouldCreateSnapshotAssetAndSingleOfdPerLogicalVersionSchema() throws Exception {
        String url = "jdbc:h2:mem:report_ofd_v122_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("122"))
            .load()
            .migrate();
        replayFromBaseline121(url);

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertTrue(tableExists(connection, "report_render_assets"));
            assertTrue(tableExists(connection, "report_version_artifacts"));
            assertTrue(columnExists(connection, "pathology_reports", "render_snapshot"));
            assertTrue(columnExists(connection, "report_versions", "render_snapshot"));
            assertTrue(columnExists(connection, "report_versions", "artifact_id"));

            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");
            statement.executeUpdate("""
                insert into pathology_cases (id, application_id, pathology_no, case_status)
                values ('CASE-V122', 'APP-V122', 'P-V122', 'DIAGNOSING')
                """);
            statement.executeUpdate("""
                insert into pathology_reports
                    (id, case_id, report_no, report_scope, report_seq, report_status, version_no, render_snapshot)
                values ('REPORT-V122', 'CASE-V122', 'RP-V122', 'ROUTINE', 1, 'SIGNED', 1, '{"schemaVersion":1}')
                """);
            statement.executeUpdate("""
                insert into report_version_artifacts
                    (id, report_id, version_no, artifact_format, file_name, storage_key, content_type,
                     byte_size, sha256, generated_at)
                values ('RVA-V122-1', 'REPORT-V122', 1, 'OFD', 'report.ofd',
                        'formal/2026/08/REPORT-V122/RVA-V122-1.ofd', 'application/ofd', 10,
                        'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', current_timestamp)
                """);

            assertThrows(SQLException.class, () -> statement.executeUpdate("""
                insert into report_version_artifacts
                    (id, report_id, version_no, artifact_format, file_name, storage_key, content_type,
                     byte_size, sha256, generated_at)
                values ('RVA-V122-2', 'REPORT-V122', 1, 'OFD', 'report-duplicate.ofd',
                        'formal/2026/08/REPORT-V122/RVA-V122-2.ofd', 'application/ofd', 10,
                        'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', current_timestamp)
                """));
            assertEquals(1, queryInt(statement, "select count(*) from report_version_artifacts"));
        }
    }

    private void replayFromBaseline121(String url) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE flyway_schema_history");
        }
        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion(MigrationVersion.fromVersion("121"))
            .baselineDescription("report-ofd-v122-replay")
            .load()
            .migrate();
    }

    private int queryInt(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, tableName, columnName)) {
            return columns.next();
        }
    }
}
