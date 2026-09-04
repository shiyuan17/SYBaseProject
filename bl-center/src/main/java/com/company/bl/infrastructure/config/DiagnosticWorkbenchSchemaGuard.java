package com.company.bl.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
@Profile("!test")
@ConditionalOnProperty(
    prefix = "bl-center.diagnostic-workbench.schema-guard",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class DiagnosticWorkbenchSchemaGuard implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticWorkbenchSchemaGuard.class);

    private final DataSource dataSource;

    public DiagnosticWorkbenchSchemaGuard(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            List<String> missing = missingObjects(connection);
            if (!missing.isEmpty()) {
                log.error("Diagnostic workbench schema is missing V122 objects: {}", String.join(",", missing));
                throw new IllegalStateException(
                    "Diagnostic workbench schema is missing V122 objects; run Flyway sync before starting bl-center");
            }
            log.info("Diagnostic workbench schema guard passed for V122");
        }
    }

    private List<String> missingObjects(Connection connection) throws SQLException {
        List<String> missing = new java.util.ArrayList<>();
        addMissingColumn(connection, missing, "PATHOLOGY_REPORTS", "RENDER_SNAPSHOT");
        addMissingColumn(connection, missing, "REPORT_VERSIONS", "RENDER_SNAPSHOT");
        addMissingColumn(connection, missing, "REPORT_VERSIONS", "ARTIFACT_ID");
        addMissingTable(connection, missing, "REPORT_RENDER_ASSETS");
        addMissingTable(connection, missing, "REPORT_VERSION_ARTIFACTS");
        addMissingIndex(connection.getMetaData(), missing, "REPORT_RENDER_ASSETS",
            "IDX_REPORT_RENDER_ASSETS_CASE_CREATED");
        addMissingIndex(connection.getMetaData(), missing, "REPORT_VERSIONS",
            "IDX_REPORT_VERSIONS_ARTIFACT_ID");
        addMissingForeignKey(connection.getMetaData(), missing, "REPORT_RENDER_ASSETS",
            "FK_REPORT_RENDER_ASSETS_CASE");
        addMissingForeignKey(connection.getMetaData(), missing, "REPORT_VERSION_ARTIFACTS",
            "FK_REPORT_VERSION_ARTIFACT_REPORT");
        addMissingForeignKey(connection.getMetaData(), missing, "REPORT_VERSIONS", "FK_REPORT_VERSIONS_ARTIFACT");
        return List.copyOf(missing);
    }

    private void addMissingColumn(Connection connection, List<String> missing, String table, String column)
        throws SQLException {
        String actualTable = actualTableName(connection.getMetaData(), table);
        if (actualTable == null) {
            missing.add(table + "." + column);
            return;
        }
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, actualTable, null)) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return;
                }
            }
        }
        missing.add(table + "." + column);
    }

    private void addMissingTable(Connection connection, List<String> missing, String table) throws SQLException {
        if (actualTableName(connection.getMetaData(), table) == null) {
            missing.add(table);
        }
    }

    private void addMissingForeignKey(DatabaseMetaData metadata, List<String> missing, String table, String key)
        throws SQLException {
        String actualTable = actualTableName(metadata, table);
        if (actualTable == null) {
            missing.add(table + "." + key);
            return;
        }
        try (ResultSet keys = metadata.getImportedKeys(null, null, actualTable)) {
            while (keys.next()) {
                if (key.equalsIgnoreCase(keys.getString("FK_NAME"))) {
                    return;
                }
            }
        }
        missing.add(table + "." + key);
    }

    private void addMissingIndex(DatabaseMetaData metadata, List<String> missing, String table, String index)
        throws SQLException {
        String actualTable = actualTableName(metadata, table);
        if (actualTable == null) {
            missing.add(table + "." + index);
            return;
        }
        try (ResultSet indexes = metadata.getIndexInfo(null, null, actualTable, false, false)) {
            while (indexes.next()) {
                if (index.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return;
                }
            }
        }
        missing.add(table + "." + index);
    }

    private String actualTableName(DatabaseMetaData metadata, String table) throws SQLException {
        try (ResultSet tables = metadata.getTables(null, null, null, new String[]{"TABLE"})) {
            while (tables.next()) {
                if (table.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return tables.getString("TABLE_NAME");
                }
            }
        }
        return null;
    }
}
