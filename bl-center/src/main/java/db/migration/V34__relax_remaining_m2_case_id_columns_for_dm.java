package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class V34__relax_remaining_m2_case_id_columns_for_dm extends BaseJavaMigration {

    private static final List<String> TABLES = List.of(
        "SPECIMEN_FIXATION_RECORDS",
        "TRANSPORT_ORDERS",
        "TRANSPORT_ORDER_ITEMS"
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isDmDatabase(connection)) {
            return;
        }
        for (String tableName : TABLES) {
            relaxNullableCaseId(connection, tableName);
        }
    }

    private void relaxNullableCaseId(Connection connection, String tableName) throws SQLException {
        if (!columnExists(connection, tableName, "CASE_ID") || isNullable(connection, tableName, "CASE_ID")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + tableName + " MODIFY CASE_ID NULL");
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
