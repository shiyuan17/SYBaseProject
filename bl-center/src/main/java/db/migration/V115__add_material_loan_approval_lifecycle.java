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

public class V115__add_material_loan_approval_lifecycle extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "MATERIAL_LOANS")) {
            return;
        }
        ensureColumn(connection, "MATERIAL_LOANS", "REQUESTED_BY_USER_ID",
            "ALTER TABLE material_loans ADD COLUMN requested_by_user_id VARCHAR(64)");
        ensureColumn(connection, "MATERIAL_LOANS", "REQUESTED_BY_NAME",
            "ALTER TABLE material_loans ADD COLUMN requested_by_name VARCHAR(100)");
        ensureColumn(connection, "MATERIAL_LOANS", "REQUESTED_AT",
            "ALTER TABLE material_loans ADD COLUMN requested_at TIMESTAMP");
        ensureColumn(connection, "MATERIAL_LOANS", "APPROVED_AT",
            "ALTER TABLE material_loans ADD COLUMN approved_at TIMESTAMP");
        ensureColumn(connection, "MATERIAL_LOANS", "REJECTED_BY_USER_ID",
            "ALTER TABLE material_loans ADD COLUMN rejected_by_user_id VARCHAR(64)");
        ensureColumn(connection, "MATERIAL_LOANS", "REJECTED_BY_NAME",
            "ALTER TABLE material_loans ADD COLUMN rejected_by_name VARCHAR(100)");
        ensureColumn(connection, "MATERIAL_LOANS", "REJECTED_AT",
            "ALTER TABLE material_loans ADD COLUMN rejected_at TIMESTAMP");
        ensureColumn(connection, "MATERIAL_LOANS", "REJECT_REASON",
            "ALTER TABLE material_loans ADD COLUMN reject_reason VARCHAR(500)");
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
