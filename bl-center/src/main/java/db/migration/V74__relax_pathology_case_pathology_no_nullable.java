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

public class V74__relax_pathology_case_pathology_no_nullable extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "PATHOLOGY_CASES")
            || !columnExists(connection, "PATHOLOGY_CASES", "PATHOLOGY_NO")
            || columnNullable(connection, "PATHOLOGY_CASES", "PATHOLOGY_NO")) {
            return;
        }

        executeCandidates(connection, ddlCandidates(connection));
    }

    private List<String> ddlCandidates(Connection connection) throws SQLException {
        String productName = connection.getMetaData().getDatabaseProductName();
        List<String> statements = new ArrayList<>();
        if (productName != null && productName.equalsIgnoreCase("H2")) {
            statements.add("ALTER TABLE pathology_cases ALTER COLUMN pathology_no DROP NOT NULL");
            return statements;
        }

        statements.add("ALTER TABLE pathology_cases MODIFY pathology_no VARCHAR2(64) NULL");
        statements.add("ALTER TABLE pathology_cases MODIFY(pathology_no VARCHAR2(64) NULL)");
        statements.add("ALTER TABLE pathology_cases MODIFY pathology_no VARCHAR2(64)");
        statements.add("ALTER TABLE pathology_cases MODIFY(pathology_no VARCHAR2(64))");
        statements.add("ALTER TABLE pathology_cases ALTER COLUMN pathology_no DROP NOT NULL");
        return statements;
    }

    private void executeCandidates(Connection connection, List<String> statements) throws SQLException {
        SQLException lastException = null;
        for (String statement : statements) {
            try {
                execute(connection, statement);
                return;
            } catch (SQLException exception) {
                lastException = exception;
            }
        }
        if (lastException != null) {
            throw lastException;
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

    private boolean columnNullable(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        for (String candidate : identifierCandidates(tableName)) {
            try (ResultSet resultSet = metaData.getColumns(null, null, candidate, null)) {
                while (resultSet.next()) {
                    if (identifierEquals(resultSet.getString("TABLE_NAME"), tableName)
                        && identifierEquals(resultSet.getString("COLUMN_NAME"), columnName)) {
                        String nullable = resultSet.getString("IS_NULLABLE");
                        return nullable != null && nullable.equalsIgnoreCase("YES");
                    }
                }
            }
        }
        return true;
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
