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

public class V64__ensure_specimen_confirmation_and_checkin_columns extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "SPECIMENS")) {
            return;
        }
        ensureColumn(connection, "SPECIMENS", "SPECIMEN_CONFIRMED_AT",
            "ALTER TABLE specimens ADD COLUMN specimen_confirmed_at TIMESTAMP");
        ensureColumn(connection, "SPECIMENS", "CHECK_IN_STATUS",
            "ALTER TABLE specimens ADD COLUMN check_in_status VARCHAR(32)");
        ensureColumn(connection, "SPECIMENS", "CHECKED_IN_AT",
            "ALTER TABLE specimens ADD COLUMN checked_in_at TIMESTAMP");
        ensureColumn(connection, "SPECIMENS", "CHECKED_IN_BY_USER_ID",
            "ALTER TABLE specimens ADD COLUMN checked_in_by_user_id VARCHAR(64)");
        ensureColumn(connection, "SPECIMENS", "CHECKED_IN_BY_NAME",
            "ALTER TABLE specimens ADD COLUMN checked_in_by_name VARCHAR(100)");
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
