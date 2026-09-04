package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class V122__add_report_ofd_artifacts extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (isH2(connection) && (!metadataTableExists(connection, "PATHOLOGY_REPORTS")
            || !metadataTableExists(connection, "REPORT_VERSIONS"))) {
            return;
        }
        ensureColumn(connection, "PATHOLOGY_REPORTS", "RENDER_SNAPSHOT", "TEXT");
        ensureTable(connection, "REPORT_RENDER_ASSETS", """
            CREATE TABLE report_render_assets (
                id VARCHAR(64) NOT NULL,
                case_id VARCHAR(64) NOT NULL,
                file_name VARCHAR(255) NOT NULL,
                storage_key VARCHAR(500) NOT NULL,
                content_type VARCHAR(100) NOT NULL,
                byte_size BIGINT NOT NULL,
                sha256 VARCHAR(64) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                CONSTRAINT pk_report_render_assets PRIMARY KEY (id),
                CONSTRAINT uk_report_render_assets_storage_key UNIQUE (storage_key),
                CONSTRAINT fk_report_render_assets_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
            )
            """);
        ensureIndex(connection, "IDX_REPORT_RENDER_ASSETS_CASE_CREATED", "REPORT_RENDER_ASSETS",
            "CREATE INDEX idx_report_render_assets_case_created ON report_render_assets (case_id, created_at)");

        ensureTable(connection, "REPORT_VERSION_ARTIFACTS", """
            CREATE TABLE report_version_artifacts (
                id VARCHAR(64) NOT NULL,
                report_id VARCHAR(64) NOT NULL,
                version_no INT NOT NULL,
                artifact_format VARCHAR(16) NOT NULL,
                file_name VARCHAR(255) NOT NULL,
                storage_key VARCHAR(500) NOT NULL,
                content_type VARCHAR(100) NOT NULL,
                byte_size BIGINT NOT NULL,
                sha256 VARCHAR(64) NOT NULL,
                generated_at TIMESTAMP NOT NULL,
                CONSTRAINT pk_report_version_artifacts PRIMARY KEY (id),
                CONSTRAINT uk_report_version_artifact UNIQUE (report_id, version_no, artifact_format),
                CONSTRAINT uk_report_version_artifact_storage_key UNIQUE (storage_key),
                CONSTRAINT fk_report_version_artifact_report FOREIGN KEY (report_id) REFERENCES pathology_reports (id),
                CONSTRAINT ck_report_version_artifact_format CHECK (artifact_format IN ('OFD'))
            )
            """);
        ensureColumn(connection, "REPORT_VERSIONS", "ARTIFACT_ID", "VARCHAR(64)");
        ensureColumn(connection, "REPORT_VERSIONS", "RENDER_SNAPSHOT", "TEXT");
        ensureForeignKey(connection, "REPORT_VERSIONS", "FK_REPORT_VERSIONS_ARTIFACT",
            "ALTER TABLE report_versions ADD CONSTRAINT fk_report_versions_artifact "
                + "FOREIGN KEY (artifact_id) REFERENCES report_version_artifacts (id)");
        ensureIndex(connection, "IDX_REPORT_VERSIONS_ARTIFACT_ID", "REPORT_VERSIONS",
            "CREATE INDEX idx_report_versions_artifact_id ON report_versions (artifact_id)");
    }

    private void ensureColumn(Connection connection, String table, String column, String definition) throws SQLException {
        if (columnExists(connection, table, column)) {
            return;
        }
        execute(connection, "ALTER TABLE " + table.toLowerCase(Locale.ROOT) + " ADD "
            + column.toLowerCase(Locale.ROOT) + " " + definition);
    }

    private void ensureTable(Connection connection, String table, String ddl) throws SQLException {
        if (tableExists(connection, table)) {
            return;
        }
        execute(connection, ddl);
    }

    private void ensureIndex(Connection connection, String indexName, String table, String ddl) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String actualTable = actualTableName(connection, table);
        try (ResultSet indexes = metadata.getIndexInfo(null, null, actualTable, false, false)) {
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return;
                }
            }
        }
        execute(connection, ddl);
    }

    private void ensureForeignKey(Connection connection, String table, String constraintName, String ddl) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String actualTable = actualTableName(connection, table);
        try (ResultSet keys = metadata.getImportedKeys(null, null, actualTable)) {
            while (keys.next()) {
                if (constraintName.equalsIgnoreCase(keys.getString("FK_NAME"))) {
                    return;
                }
            }
        }
        execute(connection, ddl);
    }

    private boolean tableExists(Connection connection, String table) {
        try (Statement statement = connection.createStatement()) {
            statement.executeQuery("select 1 from " + table.toLowerCase(Locale.ROOT) + " where 1 = 0");
            return true;
        } catch (SQLException exception) {
            return false;
        }
    }

    private boolean isH2(Connection connection) throws SQLException {
        String productName = connection.getMetaData().getDatabaseProductName();
        return productName != null && productName.toUpperCase(Locale.ROOT).contains("H2");
    }

    private boolean metadataTableExists(Connection connection, String table) throws SQLException {
        return actualTableName(connection, table) != null;
    }

    private String actualTableName(Connection connection, String table) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"})) {
            while (tables.next()) {
                if (table.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return tables.getString("TABLE_NAME");
                }
            }
        }
        return null;
    }

    private boolean columnExists(Connection connection, String table, String column) {
        try (Statement statement = connection.createStatement()) {
            statement.executeQuery("select " + column.toLowerCase(Locale.ROOT) + " from "
                + table.toLowerCase(Locale.ROOT) + " where 1 = 0");
            return true;
        } catch (SQLException exception) {
            return false;
        }
    }

    private void execute(Connection connection, String ddl) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
        }
    }
}
