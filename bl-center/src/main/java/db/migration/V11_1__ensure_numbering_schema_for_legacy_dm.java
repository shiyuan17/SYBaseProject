package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Set;

public class V11_1__ensure_numbering_schema_for_legacy_dm extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        ensureTable(connection, "NUMBERING_RULES", """
            CREATE TABLE numbering_rules (
                id VARCHAR(64) NOT NULL,
                rule_code VARCHAR(64) NOT NULL,
                biz_type VARCHAR(64) NOT NULL,
                prefix_pattern VARCHAR(64),
                date_pattern VARCHAR(32),
                seq_length INTEGER NOT NULL,
                reset_policy VARCHAR(32) NOT NULL,
                scope_type VARCHAR(32) DEFAULT 'GLOBAL',
                enabled INTEGER DEFAULT 1,
                remarks VARCHAR(500),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT pk_numbering_rules PRIMARY KEY (id),
                CONSTRAINT uk_numbering_rules_rule_code UNIQUE (rule_code),
                CONSTRAINT uk_numbering_rules_biz_type UNIQUE (biz_type)
            )
            """);
        ensureTable(connection, "NUMBERING_COUNTERS", """
            CREATE TABLE numbering_counters (
                id VARCHAR(64) NOT NULL,
                rule_code VARCHAR(64) NOT NULL,
                period_key VARCHAR(32) NOT NULL,
                scope_key VARCHAR(64) NOT NULL,
                current_value BIGINT NOT NULL,
                version INTEGER DEFAULT 0,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT pk_numbering_counters PRIMARY KEY (id),
                CONSTRAINT uk_numbering_counters_scope UNIQUE (rule_code, period_key, scope_key),
                CONSTRAINT fk_numbering_counters_rule FOREIGN KEY (rule_code) REFERENCES numbering_rules (rule_code)
            )
            """);
    }

    private void ensureTable(Connection connection, String tableName, String ddl) throws SQLException {
        if (!tableExists(connection, tableName)) {
            execute(connection, ddl);
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        for (String candidate : identifierCandidates(tableName)) {
            try (ResultSet resultSet = metaData.getTables(null, null, candidate, new String[]{"TABLE"})) {
                while (resultSet.next()) {
                    if (tableName.equalsIgnoreCase(resultSet.getString("TABLE_NAME"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Set<String> identifierCandidates(String identifier) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(identifier);
        candidates.add(identifier.toUpperCase());
        candidates.add(identifier.toLowerCase());
        return candidates;
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
