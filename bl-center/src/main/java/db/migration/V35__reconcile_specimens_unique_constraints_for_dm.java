package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class V35__reconcile_specimens_unique_constraints_for_dm extends BaseJavaMigration {

    private static final String TABLE_NAME = "SPECIMENS";
    private static final String TARGET_CONSTRAINT = "UK_SPECIMENS_SPECIMEN_NO";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isDmDatabase(connection)) {
            return;
        }
        dropLegacyCaseSpecimenConstraint(connection);
        ensureSpecimenNoConstraint(connection);
    }

    private void dropLegacyCaseSpecimenConstraint(Connection connection) throws SQLException {
        for (UniqueConstraint constraint : findUniqueConstraints(connection, TABLE_NAME)) {
            if (constraint.matches("CASE_ID", "SPECIMEN_NO")) {
                execute(connection, "ALTER TABLE " + TABLE_NAME + " DROP CONSTRAINT " + quoteIdentifier(constraint.name()));
            }
        }
    }

    private void ensureSpecimenNoConstraint(Connection connection) throws SQLException {
        for (UniqueConstraint constraint : findUniqueConstraints(connection, TABLE_NAME)) {
            if (constraint.matches("SPECIMEN_NO")) {
                return;
            }
        }
        execute(connection,
            "ALTER TABLE " + TABLE_NAME + " ADD CONSTRAINT " + TARGET_CONSTRAINT
                + " UNIQUE (SPECIMEN_NO)");
    }

    private List<UniqueConstraint> findUniqueConstraints(Connection connection, String tableName) throws SQLException {
        String sql = """
            SELECT uc.constraint_name, ucc.column_name, ucc.position
            FROM user_constraints uc
            JOIN user_cons_columns ucc ON uc.constraint_name = ucc.constraint_name
            WHERE uc.table_name = ?
              AND uc.constraint_type = 'U'
            ORDER BY uc.constraint_name, ucc.position
            """;
        Map<String, List<ConstraintColumn>> grouped = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName.toUpperCase(Locale.ROOT));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String constraintName = resultSet.getString("constraint_name");
                    grouped.computeIfAbsent(constraintName, ignored -> new ArrayList<>())
                        .add(new ConstraintColumn(
                            resultSet.getString("column_name"),
                            resultSet.getInt("position")));
                }
            }
        }
        List<UniqueConstraint> constraints = new ArrayList<>();
        for (Map.Entry<String, List<ConstraintColumn>> entry : grouped.entrySet()) {
            List<String> columns = entry.getValue().stream()
                .sorted(Comparator.comparingInt(ConstraintColumn::position))
                .map(ConstraintColumn::columnName)
                .toList();
            constraints.add(new UniqueConstraint(entry.getKey(), columns));
        }
        return constraints;
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private boolean isDmDatabase(Connection connection) throws SQLException {
        String productName = connection.getMetaData().getDatabaseProductName();
        return productName != null && productName.toUpperCase(Locale.ROOT).contains("DM");
    }

    private record ConstraintColumn(String columnName, int position) {
    }

    private record UniqueConstraint(String name, List<String> columns) {
        boolean matches(String... expectedColumns) {
            if (columns.size() != expectedColumns.length) {
                return false;
            }
            for (int index = 0; index < expectedColumns.length; index++) {
                if (columns.get(index) == null || !expectedColumns[index].equalsIgnoreCase(columns.get(index))) {
                    return false;
                }
            }
            return true;
        }
    }
}
