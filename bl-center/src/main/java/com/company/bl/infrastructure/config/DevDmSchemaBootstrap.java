package com.company.bl.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "bl-center.dev-dm-schema-bootstrap", name = "enabled", havingValue = "true")
public class DevDmSchemaBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDmSchemaBootstrap.class);

    private final DataSource dataSource;

    public DevDmSchemaBootstrap(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!isDmDatabase(connection)) {
                return;
            }
            ensureColumn(connection, "USERS", "PASSWORD_ALGO",
                "ALTER TABLE users ADD password_algo VARCHAR(32)");
            ensureColumn(connection, "USERS", "PASSWORD_SALT",
                "ALTER TABLE users ADD password_salt VARCHAR(64)");
            ensureTable(connection, "AUTH_ACCESS_TOKENS", """
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
            ensureIndex(connection, "IDX_AUTH_ACCESS_TOKENS_USER_ID", "AUTH_ACCESS_TOKENS",
                "CREATE INDEX idx_auth_access_tokens_user_id ON auth_access_tokens (user_id)");
        }
    }

    private boolean isDmDatabase(Connection connection) throws SQLException {
        String databaseProductName = connection.getMetaData().getDatabaseProductName();
        return databaseProductName != null && databaseProductName.toUpperCase().contains("DM");
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String ddl) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getColumns(null, null, tableName, columnName)) {
            if (resultSet.next()) {
                return;
            }
        }
        executeDdl(connection, ddl, "column " + tableName + "." + columnName);
    }

    private void ensureTable(Connection connection, String tableName, String ddl) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getTables(null, null, tableName, null)) {
            if (resultSet.next()) {
                return;
            }
        }
        executeDdl(connection, ddl, "table " + tableName);
    }

    private void ensureIndex(Connection connection, String indexName, String tableName, String ddl) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getIndexInfo(null, null, tableName, false, false)) {
            while (resultSet.next()) {
                String existingIndexName = resultSet.getString("INDEX_NAME");
                if (indexName.equalsIgnoreCase(existingIndexName)) {
                    return;
                }
            }
        }
        executeDdl(connection, ddl, "index " + indexName);
    }

    private void executeDdl(Connection connection, String ddl, String target) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
            log.info("Initialized missing dev DM schema object: {}", target);
        }
    }
}
