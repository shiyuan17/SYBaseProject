package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class V33__relax_m2_workflow_event_legacy_columns_for_dm extends BaseJavaMigration {

    private static final String TABLE_NAME = "WORKFLOW_EVENTS";
    private static final List<String> LEGACY_COLUMNS = List.of("ACTION_CODE", "OCCURRED_AT");

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isDmDatabase(connection)) {
            return;
        }
        for (String columnName : LEGACY_COLUMNS) {
            relaxNullable(connection, TABLE_NAME, columnName);
        }
    }

    private void relaxNullable(Connection connection, String tableName, String columnName) throws SQLException {
        if (!columnExists(connection, tableName, columnName) || isNullable(connection, tableName, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + tableName + " MODIFY " + columnName + " NULL");
        }
    }

    private boolean isDmDatabase(Connection connection) throws SQLException {
        String productName = connection.getMetaData().getDatabaseProductName();
        return productName != null && productName.toUpperCase().contains("DM");
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getColumns(null, null, tableName, columnName)) {
            return resultSet.next();
        }
    }

    private boolean isNullable(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getColumns(null, null, tableName, columnName)) {
            if (!resultSet.next()) {
                return true;
            }
            return resultSet.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
        }
    }
}
