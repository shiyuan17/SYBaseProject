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

public class V71__ensure_technical_registration_override_columns extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "APPLICATION_REGISTRATION_WORKBENCH")) {
            return;
        }
        ensureColumn(connection, "APPLICATION_REGISTRATION_WORKBENCH", "TECHNICAL_HISTORY_SUMMARY_OVERRIDE",
            "ALTER TABLE application_registration_workbench ADD COLUMN technical_history_summary_override VARCHAR(1000)");
        ensureColumn(connection, "APPLICATION_REGISTRATION_WORKBENCH", "TECHNICAL_CLINICAL_EXAM_SURGERY_OVERRIDE",
            "ALTER TABLE application_registration_workbench ADD COLUMN technical_clinical_exam_surgery_override VARCHAR(1000)");
        ensureColumn(connection, "APPLICATION_REGISTRATION_WORKBENCH", "TECHNICAL_LAB_IMAGING_OVERRIDE",
            "ALTER TABLE application_registration_workbench ADD COLUMN technical_lab_imaging_override VARCHAR(1000)");
        ensureColumn(connection, "APPLICATION_REGISTRATION_WORKBENCH", "TECHNICAL_SUBMISSION_REQUIREMENT_OVERRIDE",
            "ALTER TABLE application_registration_workbench ADD COLUMN technical_submission_requirement_override VARCHAR(1000)");
        ensureColumn(connection, "APPLICATION_REGISTRATION_WORKBENCH", "TECHNICAL_INFECTIOUS_PAST_HISTORY_OVERRIDE",
            "ALTER TABLE application_registration_workbench ADD COLUMN technical_infectious_past_history_override VARCHAR(1000)");
        ensureColumn(connection, "APPLICATION_REGISTRATION_WORKBENCH", "TECHNICAL_EXTERNAL_PATHOLOGY_DIAGNOSIS_OVERRIDE",
            "ALTER TABLE application_registration_workbench ADD COLUMN technical_external_pathology_diagnosis_override VARCHAR(1000)");
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String ddl) throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            execute(connection, ddl);
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
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
