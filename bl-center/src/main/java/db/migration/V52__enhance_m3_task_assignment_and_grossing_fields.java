package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class V52__enhance_m3_task_assignment_and_grossing_fields extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (tableExists(connection, "TECHNICAL_PENDING_TASKS")) {
            ensureColumn(connection, "TECHNICAL_PENDING_TASKS", "PRIORITY",
                "ALTER TABLE technical_pending_tasks ADD COLUMN priority VARCHAR(32)");
            ensureColumn(connection, "TECHNICAL_PENDING_TASKS", "CURRENT_NODE",
                "ALTER TABLE technical_pending_tasks ADD COLUMN current_node VARCHAR(64)");
            ensureColumn(connection, "TECHNICAL_PENDING_TASKS", "STATION_CODE",
                "ALTER TABLE technical_pending_tasks ADD COLUMN station_code VARCHAR(64)");
            ensureColumn(connection, "TECHNICAL_PENDING_TASKS", "STATION_NAME",
                "ALTER TABLE technical_pending_tasks ADD COLUMN station_name VARCHAR(100)");
            ensureColumn(connection, "TECHNICAL_PENDING_TASKS", "ASSIGNED_TO_USER_ID",
                "ALTER TABLE technical_pending_tasks ADD COLUMN assigned_to_user_id VARCHAR(64)");
            ensureColumn(connection, "TECHNICAL_PENDING_TASKS", "ASSIGNED_TO_NAME",
                "ALTER TABLE technical_pending_tasks ADD COLUMN assigned_to_name VARCHAR(100)");
            ensureColumn(connection, "TECHNICAL_PENDING_TASKS", "EXPECTED_COMPLETED_AT",
                "ALTER TABLE technical_pending_tasks ADD COLUMN expected_completed_at TIMESTAMP");
            ensureColumn(connection, "TECHNICAL_PENDING_TASKS", "PRODUCTION_REMARKS",
                "ALTER TABLE technical_pending_tasks ADD COLUMN production_remarks VARCHAR(500)");
            ensureColumn(connection, "TECHNICAL_PENDING_TASKS", "RECEIVED_AT",
                "ALTER TABLE technical_pending_tasks ADD COLUMN received_at TIMESTAMP");
            execute(connection, """
                update technical_pending_tasks
                set priority = coalesce(priority, 'NORMAL'),
                    current_node = coalesce(current_node, task_type),
                    received_at = coalesce(received_at, created_at)
                """);
        }
        if (tableExists(connection, "SAMPLINGS")) {
            ensureColumn(connection, "SAMPLINGS", "SIZE_TEXT",
                "ALTER TABLE samplings ADD COLUMN size_text VARCHAR(100)");
            ensureColumn(connection, "SAMPLINGS", "CUT_SURFACE_FEATURE",
                "ALTER TABLE samplings ADD COLUMN cut_surface_feature VARCHAR(500)");
            ensureColumn(connection, "SAMPLINGS", "MARGIN_MARKING",
                "ALTER TABLE samplings ADD COLUMN margin_marking VARCHAR(500)");
        }
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String ddl) throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            execute(connection, ddl);
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getTables(null, null, null, new String[]{"TABLE"})) {
            while (resultSet.next()) {
                if (identifierEquals(resultSet.getString("TABLE_NAME"), tableName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        for (String candidate : identifierCandidates(tableName)) {
            try (ResultSet resultSet = metaData.getColumns(null, null, candidate, null)) {
                while (resultSet.next()) {
                    if (identifierEquals(resultSet.getString("TABLE_NAME"), tableName)
                        && identifierEquals(resultSet.getString("COLUMN_NAME"), columnName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean identifierEquals(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private List<String> identifierCandidates(String identifier) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(identifier);
        candidates.add(identifier.toUpperCase());
        candidates.add(identifier.toLowerCase());
        return new ArrayList<>(candidates);
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
