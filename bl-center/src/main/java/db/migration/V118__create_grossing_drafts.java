package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class V118__create_grossing_drafts extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (tableExists(connection, "GROSSING_DRAFTS")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE grossing_drafts (
                    task_id VARCHAR(64) NOT NULL,
                    case_id VARCHAR(64) NOT NULL,
                    draft_payload CLOB NOT NULL,
                    saved_by_user_id VARCHAR(64) NOT NULL,
                    saved_by_name VARCHAR(100) NOT NULL,
                    saved_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL,
                    CONSTRAINT pk_grossing_drafts PRIMARY KEY (task_id),
                    CONSTRAINT fk_grossing_drafts_task FOREIGN KEY (task_id) REFERENCES technical_pending_tasks (id),
                    CONSTRAINT fk_grossing_drafts_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id)
                )
                """);
            statement.execute("CREATE INDEX idx_grossing_drafts_case ON grossing_drafts (case_id)");
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getTables(null, null, null, new String[]{"TABLE"})) {
            while (resultSet.next()) {
                String actualName = resultSet.getString("TABLE_NAME");
                if (actualName != null && actualName.equalsIgnoreCase(tableName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
