package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class V66__reconcile_specimen_no_global_unique_and_deduplicate_dm extends BaseJavaMigration {

    private static final String TABLE_NAME = "SPECIMENS";
    private static final String TARGET_CONSTRAINT = "UK_SPECIMENS_SPECIMEN_NO";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isDmDatabase(connection)) {
            return;
        }
        renameLegacyConstraint(connection);
        deduplicateSpecimenNos(connection);
        ensureGlobalSpecimenNoConstraint(connection);
    }

    private void renameLegacyConstraint(Connection connection) throws SQLException {
        for (UniqueConstraint constraint : findUniqueConstraints(connection, TABLE_NAME)) {
            if (constraint.matches("APPLICATION_ID", "SPECIMEN_NO")) {
                execute(connection, "ALTER TABLE " + TABLE_NAME + " DROP CONSTRAINT " + constraint.name());
            }
        }
    }

    private void deduplicateSpecimenNos(Connection connection) throws SQLException {
        List<SpecimenRow> specimens = findSpecimens(connection);
        Map<String, List<SpecimenRow>> grouped = new LinkedHashMap<>();
        for (SpecimenRow specimen : specimens) {
            grouped.computeIfAbsent(normalize(specimen.specimenNo()), ignored -> new ArrayList<>())
                .add(specimen);
        }

        int duplicateGroupIndex = 1;
        for (Map.Entry<String, List<SpecimenRow>> entry : grouped.entrySet()) {
            List<SpecimenRow> rows = entry.getValue();
            if (rows.size() < 2) {
                continue;
            }
            rows.sort(Comparator
                .comparing(SpecimenRow::registeredAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(SpecimenRow::createdAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(SpecimenRow::id));
            for (int index = 1; index < rows.size(); index++) {
                SpecimenRow specimen = rows.get(index);
                String replacement = generateReplacementSpecimenNo(connection, specimen, duplicateGroupIndex, index);
                updateSpecimenNo(connection, specimen.id(), replacement);
            }
            duplicateGroupIndex++;
        }
    }

    private String generateReplacementSpecimenNo(
        Connection connection,
        SpecimenRow specimen,
        int duplicateGroupIndex,
        int rowIndex
    ) throws SQLException {
        String candidate = specimen.specimenNo();
        int attempt = 0;
        while (attempt < 100) {
            String prefix = derivePrefix(candidate);
            String numeric = String.format(Locale.ROOT, "%06d", duplicateGroupIndex * 100 + rowIndex + attempt);
            String replacement = prefix + numeric;
            if (!specimenNoExists(connection, replacement)) {
                return replacement;
            }
            attempt++;
        }
        throw new IllegalStateException("Failed to generate unique specimen number for " + specimen.id());
    }

    private String derivePrefix(String specimenNo) {
        if (specimenNo == null || specimenNo.isBlank()) {
            return "SP";
        }
        int index = specimenNo.length();
        while (index > 0 && Character.isDigit(specimenNo.charAt(index - 1))) {
            index--;
        }
        String prefix = specimenNo.substring(0, index).trim();
        return prefix.isBlank() ? "SP" : prefix;
    }

    private void updateSpecimenNo(Connection connection, String specimenId, String specimenNo) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            update specimens
            set specimen_no = ?
            where id = ?
            """)) {
            statement.setString(1, specimenNo);
            statement.setString(2, specimenId);
            statement.executeUpdate();
        }
    }

    private void ensureGlobalSpecimenNoConstraint(Connection connection) throws SQLException {
        for (UniqueConstraint constraint : findUniqueConstraints(connection, TABLE_NAME)) {
            if (constraint.matches("SPECIMEN_NO")) {
                return;
            }
        }
        execute(connection,
            "ALTER TABLE " + TABLE_NAME + " ADD CONSTRAINT " + TARGET_CONSTRAINT
                + " UNIQUE (SPECIMEN_NO)");
    }

    private List<SpecimenRow> findSpecimens(Connection connection) throws SQLException {
        List<SpecimenRow> specimens = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
            select id, specimen_no, registered_at, created_at
            from specimens
            order by registered_at asc, created_at asc, id asc
            """)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    specimens.add(new SpecimenRow(
                        resultSet.getString("id"),
                        resultSet.getString("specimen_no"),
                        resultSet.getTimestamp("registered_at") == null ? null : resultSet.getTimestamp("registered_at").toLocalDateTime(),
                        resultSet.getTimestamp("created_at") == null ? null : resultSet.getTimestamp("created_at").toLocalDateTime()));
                }
            }
        }
        return specimens;
    }

    private boolean specimenNoExists(Connection connection, String specimenNo) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            select 1
            from specimens
            where specimen_no = ?
            fetch first 1 rows only
            """)) {
            statement.setString(1, specimenNo);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
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
        Map<String, List<ConstraintColumn>> grouped = new LinkedHashMap<>();
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

    private boolean isDmDatabase(Connection connection) throws SQLException {
        String productName = connection.getMetaData().getDatabaseProductName();
        return productName != null && productName.toUpperCase(Locale.ROOT).contains("DM");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private record SpecimenRow(String id, String specimenNo, LocalDateTime registeredAt, LocalDateTime createdAt) {
    }

    private record ConstraintColumn(String columnName, int position) {
    }

    private record UniqueConstraint(String name, List<String> columns) {
        boolean matches(String... expectedColumns) {
            if (columns.size() != expectedColumns.length) {
                return false;
            }
            for (int index = 0; index < expectedColumns.length; index++) {
                if (!Objects.equals(columns.get(index), expectedColumns[index])) {
                    return false;
                }
            }
            return true;
        }
    }
}
