package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

final class V11LegacyDmSchemaMetadataSupport {

    private final Connection connection;

    V11LegacyDmSchemaMetadataSupport(Connection connection) {
        this.connection = connection;
    }

    Connection connection() {
        return connection;
    }

    void ensureUserLoginSchema() throws SQLException {
        ensureColumn("USERS", "PASSWORD_ALGO", "ALTER TABLE users ADD password_algo VARCHAR(32)");
        ensureColumn("USERS", "PASSWORD_SALT", "ALTER TABLE users ADD password_salt VARCHAR(64)");
        ensureTable("AUTH_ACCESS_TOKENS", """
            CREATE TABLE auth_access_tokens (
                jti VARCHAR(128) NOT NULL,
                user_id VARCHAR(64) NOT NULL,
                issued_at TIMESTAMP NOT NULL,
                expires_at TIMESTAMP NOT NULL,
                revoked_at TIMESTAMP,
                client_ip VARCHAR(64),
                client_device VARCHAR(200),
                CONSTRAINT pk_auth_access_tokens PRIMARY KEY (jti),
                CONSTRAINT fk_auth_access_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
            )
            """);
        ensureIndex("AUTH_ACCESS_TOKENS", "IDX_AUTH_ACCESS_TOKENS_USER_ID",
            "CREATE INDEX idx_auth_access_tokens_user_id ON auth_access_tokens (user_id)");
    }

    void ensureTechnicalPendingTasks() throws SQLException {
        if (!tableExists("TECHNICAL_PENDING_TASKS")) {
            execute("""
                CREATE TABLE technical_pending_tasks (
                    id VARCHAR(64) NOT NULL,
                    application_id VARCHAR(64) NOT NULL,
                    case_id VARCHAR(64) NOT NULL,
                    specimen_id VARCHAR(64),
                    task_type VARCHAR(64) NOT NULL,
                    task_status VARCHAR(32) NOT NULL,
                    object_type VARCHAR(32),
                    object_id VARCHAR(64),
                    parent_task_id VARCHAR(64),
                    payload VARCHAR(2000),
                    started_at TIMESTAMP,
                    completed_at TIMESTAMP,
                    remarks VARCHAR(500),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT pk_technical_pending_tasks PRIMARY KEY (id),
                    CONSTRAINT fk_technical_pending_tasks_application FOREIGN KEY (application_id) REFERENCES applications (id),
                    CONSTRAINT fk_technical_pending_tasks_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
                    CONSTRAINT fk_technical_pending_tasks_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
                    CONSTRAINT fk_technical_pending_tasks_parent FOREIGN KEY (parent_task_id) REFERENCES technical_pending_tasks (id)
                )
                """);
            return;
        }

        if (indexExists("TECHNICAL_PENDING_TASKS", "UK_TECHNICAL_PENDING_TASKS_CASE_TYPE")) {
            execute("ALTER TABLE technical_pending_tasks DROP CONSTRAINT uk_technical_pending_tasks_case_type");
        }
        ensureColumn("TECHNICAL_PENDING_TASKS", "SPECIMEN_ID",
            "ALTER TABLE technical_pending_tasks ADD COLUMN specimen_id VARCHAR(64)");
        ensureColumn("TECHNICAL_PENDING_TASKS", "OBJECT_TYPE",
            "ALTER TABLE technical_pending_tasks ADD COLUMN object_type VARCHAR(32)");
        ensureColumn("TECHNICAL_PENDING_TASKS", "OBJECT_ID",
            "ALTER TABLE technical_pending_tasks ADD COLUMN object_id VARCHAR(64)");
        ensureColumn("TECHNICAL_PENDING_TASKS", "PARENT_TASK_ID",
            "ALTER TABLE technical_pending_tasks ADD COLUMN parent_task_id VARCHAR(64)");
        ensureColumn("TECHNICAL_PENDING_TASKS", "STARTED_AT",
            "ALTER TABLE technical_pending_tasks ADD COLUMN started_at TIMESTAMP");
        ensureColumn("TECHNICAL_PENDING_TASKS", "COMPLETED_AT",
            "ALTER TABLE technical_pending_tasks ADD COLUMN completed_at TIMESTAMP");
        ensureColumn("TECHNICAL_PENDING_TASKS", "REMARKS",
            "ALTER TABLE technical_pending_tasks ADD COLUMN remarks VARCHAR(500)");
        execute("""
            UPDATE technical_pending_tasks
            SET object_type = 'CASE',
                object_id = case_id
            WHERE object_type IS NULL
            """);
        if (!foreignKeyExists("TECHNICAL_PENDING_TASKS", "FK_TECHNICAL_PENDING_TASKS_SPECIMEN")) {
            execute("""
                ALTER TABLE technical_pending_tasks ADD CONSTRAINT fk_technical_pending_tasks_specimen
                FOREIGN KEY (specimen_id) REFERENCES specimens (id)
                """);
        }
        if (!foreignKeyExists("TECHNICAL_PENDING_TASKS", "FK_TECHNICAL_PENDING_TASKS_PARENT")) {
            execute("""
                ALTER TABLE technical_pending_tasks ADD CONSTRAINT fk_technical_pending_tasks_parent
                FOREIGN KEY (parent_task_id) REFERENCES technical_pending_tasks (id)
                """);
        }
    }

    void ensureTable(String tableName, String ddl) throws SQLException {
        if (!tableExists(tableName)) {
            execute(ddl);
        }
    }

    void ensureColumn(String tableName, String columnName, String ddl) throws SQLException {
        if (!columnExists(tableName, columnName)) {
            execute(ddl);
        }
    }

    void ensureIndex(String tableName, String indexName, String ddl) throws SQLException {
        if (!indexExists(tableName, indexName)) {
            execute(ddl);
        }
    }

    boolean tableExists(String tableName) throws SQLException {
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

    boolean columnExists(String tableName, String columnName) throws SQLException {
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

    boolean indexExists(String tableName, String indexName) throws SQLException {
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

    boolean foreignKeyExists(String tableName, String fkName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        for (String candidate : identifierCandidates(tableName)) {
            try (ResultSet resultSet = metaData.getImportedKeys(null, null, candidate)) {
                while (resultSet.next()) {
                    if (identifierEquals(resultSet.getString("FKTABLE_NAME"), tableName)
                        && identifierEquals(resultSet.getString("FK_NAME"), fkName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    void execute(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
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
}
