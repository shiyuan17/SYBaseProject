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

public class V80__add_slicing_task_link extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "SLICINGS")) {
            return;
        }

        ensureColumn(connection, "SLICINGS", "TASK_ID",
            "ALTER TABLE slicings ADD COLUMN task_id VARCHAR(64)");

        if (tableExists(connection, "TECHNICAL_PENDING_TASKS")) {
            ensureForeignKey(connection, "SLICINGS", "FK_SLICINGS_TASK",
                "ALTER TABLE slicings ADD CONSTRAINT fk_slicings_task " +
                    "FOREIGN KEY (task_id) REFERENCES technical_pending_tasks (id)");
        }

        ensureIndex(connection, "SLICINGS", "IDX_SLICINGS_TASK_ID",
            "CREATE INDEX idx_slicings_task_id ON slicings (task_id)");
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String ddl) throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            execute(connection, ddl);
        }
    }

    private void ensureForeignKey(Connection connection, String tableName, String foreignKeyName, String ddl)
        throws SQLException {
        if (!foreignKeyExists(connection, tableName, foreignKeyName)) {
            execute(connection, ddl);
        }
    }

    private void ensureIndex(Connection connection, String tableName, String indexName, String ddl) throws SQLException {
        if (!indexExists(connection, tableName, indexName)) {
            execute(connection, ddl);
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getTables(null, null, null, new String[] {"TABLE"})) {
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

    private boolean foreignKeyExists(Connection connection, String tableName, String foreignKeyName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        for (String candidate : identifierCandidates(tableName)) {
            try (ResultSet resultSet = metaData.getImportedKeys(null, null, candidate)) {
                while (resultSet.next()) {
                    if (identifierEquals(resultSet.getString("FKTABLE_NAME"), tableName)
                        && identifierEquals(resultSet.getString("FK_NAME"), foreignKeyName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        for (String candidate : identifierCandidates(tableName)) {
            try (ResultSet resultSet = metaData.getIndexInfo(null, null, candidate, false, false)) {
                while (resultSet.next()) {
                    if (identifierEquals(resultSet.getString("TABLE_NAME"), tableName)
                        && identifierEquals(resultSet.getString("INDEX_NAME"), indexName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<String> identifierCandidates(String identifier) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(identifier);
        candidates.add(identifier.toUpperCase());
        candidates.add(identifier.toLowerCase());
        return new ArrayList<>(candidates);
    }

    private boolean identifierEquals(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
