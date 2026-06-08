package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class V75__add_grossing_embedding_box_draft_fields extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "SAMPLING_BLOCKS")) {
            return;
        }
        ensureColumn(connection, "SAMPLING_BLOCKS", "EMBEDDING_BOX_NAME",
            "ALTER TABLE sampling_blocks ADD COLUMN embedding_box_name VARCHAR(100)");
        ensureColumn(connection, "SAMPLING_BLOCKS", "EMBEDDING_BOX_STATUS",
            "ALTER TABLE sampling_blocks ADD COLUMN embedding_box_status VARCHAR(32) DEFAULT 'PENDING'");
        ensureColumn(connection, "SAMPLING_BLOCKS", "EMBEDDING_REMARKS",
            "ALTER TABLE sampling_blocks ADD COLUMN embedding_remarks VARCHAR(500)");
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(null, null, tableName, null)) {
            if (tables.next()) {
                return true;
            }
        }
        try (ResultSet tables = metadata.getTables(null, null, tableName.toLowerCase(), null)) {
            return tables.next();
        }
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String ddl) throws SQLException {
        if (columnExists(connection, tableName, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(null, null, tableName, columnName)) {
            if (columns.next()) {
                return true;
            }
        }
        try (ResultSet columns = metadata.getColumns(null, null, tableName.toLowerCase(), columnName.toLowerCase())) {
            return columns.next();
        }
    }
}
