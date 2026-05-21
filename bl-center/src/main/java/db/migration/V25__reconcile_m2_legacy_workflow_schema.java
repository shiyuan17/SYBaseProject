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

public class V25__reconcile_m2_legacy_workflow_schema extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        reconcileApplications(connection);
        reconcilePathologyCases(connection);
        reconcileSpecimens(connection);
        reconcileSpecimenCollectionRecords(connection);
        reconcileSpecimenFixationRecords(connection);
        reconcileTransportOrders(connection);
        reconcileTransportOrderItems(connection);
        reconcileSpecimenReceipts(connection);
        reconcileWorkflowEvents(connection);
    }

    private void reconcileApplications(Connection connection) throws SQLException {
        if (!tableExists(connection, "APPLICATIONS")) {
            return;
        }
        ensureColumn(connection, "APPLICATIONS", "PATIENT_NAME",
            "ALTER TABLE applications ADD COLUMN patient_name VARCHAR(100)");
        ensureColumn(connection, "APPLICATIONS", "PATIENT_GENDER",
            "ALTER TABLE applications ADD COLUMN patient_gender VARCHAR(16)");
        ensureColumn(connection, "APPLICATIONS", "PATIENT_AGE",
            "ALTER TABLE applications ADD COLUMN patient_age VARCHAR(32)");
    }

    private void reconcilePathologyCases(Connection connection) throws SQLException {
        if (!tableExists(connection, "PATHOLOGY_CASES")) {
            return;
        }
        ensureColumn(connection, "PATHOLOGY_CASES", "CASE_STATUS",
            "ALTER TABLE pathology_cases ADD COLUMN case_status VARCHAR(32)");
        if (columnExists(connection, "PATHOLOGY_CASES", "CURRENT_STATUS")) {
            execute(connection, """
                UPDATE pathology_cases
                SET case_status = COALESCE(case_status, current_status)
                WHERE case_status IS NULL
                """);
        }
    }

    private void reconcileSpecimens(Connection connection) throws SQLException {
        if (!tableExists(connection, "SPECIMENS")) {
            return;
        }
        ensureColumn(connection, "SPECIMENS", "SPECIMEN_STATUS",
            "ALTER TABLE specimens ADD COLUMN specimen_status VARCHAR(32)");
        ensureColumn(connection, "SPECIMENS", "LABEL_PRINT_BATCH_NO",
            "ALTER TABLE specimens ADD COLUMN label_print_batch_no VARCHAR(64)");
        ensureColumn(connection, "SPECIMENS", "LABEL_PRINT_STATUS",
            "ALTER TABLE specimens ADD COLUMN label_print_status VARCHAR(32)");
        ensureColumn(connection, "SPECIMENS", "TERMINAL_CODE",
            "ALTER TABLE specimens ADD COLUMN terminal_code VARCHAR(64)");
        ensureColumn(connection, "SPECIMENS", "CREATED_AT",
            "ALTER TABLE specimens ADD COLUMN created_at TIMESTAMP");
        ensureColumn(connection, "SPECIMENS", "UPDATED_AT",
            "ALTER TABLE specimens ADD COLUMN updated_at TIMESTAMP");
        execute(connection, """
            UPDATE specimens
            SET specimen_status = COALESCE(specimen_status, 'REGISTERED'),
                created_at = COALESCE(created_at, registered_at, CURRENT_TIMESTAMP),
                updated_at = COALESCE(updated_at, registered_at, CURRENT_TIMESTAMP)
            WHERE specimen_status IS NULL
               OR created_at IS NULL
               OR updated_at IS NULL
            """);
    }

    private void reconcileSpecimenCollectionRecords(Connection connection) throws SQLException {
        if (!tableExists(connection, "SPECIMEN_COLLECTION_RECORDS")) {
            return;
        }
        ensureColumn(connection, "SPECIMEN_COLLECTION_RECORDS", "TERMINAL_CODE",
            "ALTER TABLE specimen_collection_records ADD COLUMN terminal_code VARCHAR(64)");
    }

    private void reconcileSpecimenFixationRecords(Connection connection) throws SQLException {
        if (!tableExists(connection, "SPECIMEN_FIXATION_RECORDS")) {
            return;
        }
        ensureColumn(connection, "SPECIMEN_FIXATION_RECORDS", "APPLICATION_ID",
            "ALTER TABLE specimen_fixation_records ADD COLUMN application_id VARCHAR(64)");
        ensureColumn(connection, "SPECIMEN_FIXATION_RECORDS", "TERMINAL_CODE",
            "ALTER TABLE specimen_fixation_records ADD COLUMN terminal_code VARCHAR(64)");
        execute(connection, """
            UPDATE specimen_fixation_records
            SET application_id = (
                SELECT s.application_id
                FROM specimens s
                WHERE s.id = specimen_fixation_records.specimen_id
            )
            WHERE application_id IS NULL
              AND specimen_id IS NOT NULL
            """);
    }

    private void reconcileTransportOrders(Connection connection) throws SQLException {
        if (!tableExists(connection, "TRANSPORT_ORDERS")) {
            return;
        }
        ensureColumn(connection, "TRANSPORT_ORDERS", "TERMINAL_CODE",
            "ALTER TABLE transport_orders ADD COLUMN terminal_code VARCHAR(64)");
        ensureColumn(connection, "TRANSPORT_ORDERS", "CREATED_AT",
            "ALTER TABLE transport_orders ADD COLUMN created_at TIMESTAMP");
        ensureColumn(connection, "TRANSPORT_ORDERS", "UPDATED_AT",
            "ALTER TABLE transport_orders ADD COLUMN updated_at TIMESTAMP");
        execute(connection, """
            UPDATE transport_orders
            SET created_at = COALESCE(created_at, printed_at, to_be_transported_at, handed_over_at, CURRENT_TIMESTAMP),
                updated_at = COALESCE(updated_at, handed_over_at, to_be_transported_at, printed_at, CURRENT_TIMESTAMP)
            WHERE created_at IS NULL
               OR updated_at IS NULL
            """);
    }

    private void reconcileTransportOrderItems(Connection connection) throws SQLException {
        if (!tableExists(connection, "TRANSPORT_ORDER_ITEMS")) {
            return;
        }
        ensureColumn(connection, "TRANSPORT_ORDER_ITEMS", "APPLICATION_ID",
            "ALTER TABLE transport_order_items ADD COLUMN application_id VARCHAR(64)");
        execute(connection, """
            UPDATE transport_order_items
            SET application_id = (
                SELECT t.application_id
                FROM transport_orders t
                WHERE t.id = transport_order_items.transport_order_id
            )
            WHERE application_id IS NULL
              AND transport_order_id IS NOT NULL
            """);
    }

    private void reconcileSpecimenReceipts(Connection connection) throws SQLException {
        if (!tableExists(connection, "SPECIMEN_RECEIPTS")) {
            return;
        }
        ensureColumn(connection, "SPECIMEN_RECEIPTS", "APPLICATION_ID",
            "ALTER TABLE specimen_receipts ADD COLUMN application_id VARCHAR(64)");
        ensureColumn(connection, "SPECIMEN_RECEIPTS", "TRANSPORT_ORDER_ID",
            "ALTER TABLE specimen_receipts ADD COLUMN transport_order_id VARCHAR(64)");
        ensureColumn(connection, "SPECIMEN_RECEIPTS", "TERMINAL_CODE",
            "ALTER TABLE specimen_receipts ADD COLUMN terminal_code VARCHAR(64)");
        execute(connection, """
            UPDATE specimen_receipts
            SET application_id = (
                SELECT s.application_id
                FROM specimens s
                WHERE s.id = specimen_receipts.specimen_id
            )
            WHERE application_id IS NULL
              AND specimen_id IS NOT NULL
            """);
        execute(connection, """
            UPDATE specimen_receipts
            SET transport_order_id = (
                SELECT MIN(toi.transport_order_id)
                FROM transport_order_items toi
                WHERE toi.specimen_id = specimen_receipts.specimen_id
            )
            WHERE transport_order_id IS NULL
              AND specimen_id IS NOT NULL
            """);
    }

    private void reconcileWorkflowEvents(Connection connection) throws SQLException {
        if (!tableExists(connection, "WORKFLOW_EVENTS")) {
            return;
        }
        ensureColumn(connection, "WORKFLOW_EVENTS", "APPLICATION_ID",
            "ALTER TABLE workflow_events ADD COLUMN application_id VARCHAR(64)");
        ensureColumn(connection, "WORKFLOW_EVENTS", "TRANSPORT_ORDER_ID",
            "ALTER TABLE workflow_events ADD COLUMN transport_order_id VARCHAR(64)");
        ensureColumn(connection, "WORKFLOW_EVENTS", "EVENT_TYPE",
            "ALTER TABLE workflow_events ADD COLUMN event_type VARCHAR(64)");
        ensureColumn(connection, "WORKFLOW_EVENTS", "EVENT_STATUS",
            "ALTER TABLE workflow_events ADD COLUMN event_status VARCHAR(32)");
        ensureColumn(connection, "WORKFLOW_EVENTS", "EVENT_TIME",
            "ALTER TABLE workflow_events ADD COLUMN event_time TIMESTAMP");
        ensureColumn(connection, "WORKFLOW_EVENTS", "SOURCE_TERMINAL",
            "ALTER TABLE workflow_events ADD COLUMN source_terminal VARCHAR(64)");
        ensureColumn(connection, "WORKFLOW_EVENTS", "EVENT_CONTENT",
            "ALTER TABLE workflow_events ADD COLUMN event_content VARCHAR(1000)");
        ensureColumn(connection, "WORKFLOW_EVENTS", "CREATED_AT",
            "ALTER TABLE workflow_events ADD COLUMN created_at TIMESTAMP");
        execute(connection, """
            UPDATE workflow_events
            SET application_id = (
                SELECT s.application_id
                FROM specimens s
                WHERE s.id = workflow_events.specimen_id
            )
            WHERE application_id IS NULL
              AND specimen_id IS NOT NULL
            """);
        execute(connection, """
            UPDATE workflow_events
            SET application_id = (
                SELECT pc.application_id
                FROM pathology_cases pc
                WHERE pc.id = workflow_events.case_id
            )
            WHERE application_id IS NULL
              AND case_id IS NOT NULL
            """);
        if (columnExists(connection, "WORKFLOW_EVENTS", "ACTION_CODE")) {
            execute(connection, """
                UPDATE workflow_events
                SET event_type = COALESCE(event_type, action_code)
                WHERE event_type IS NULL
                """);
        }
        if (columnExists(connection, "WORKFLOW_EVENTS", "TO_STATUS")
            || columnExists(connection, "WORKFLOW_EVENTS", "FROM_STATUS")) {
            execute(connection, """
                UPDATE workflow_events
                SET event_status = COALESCE(event_status, to_status, from_status)
                WHERE event_status IS NULL
                """);
        }
        if (columnExists(connection, "WORKFLOW_EVENTS", "OCCURRED_AT")) {
            execute(connection, """
                UPDATE workflow_events
                SET event_time = COALESCE(event_time, occurred_at),
                    created_at = COALESCE(created_at, occurred_at)
                WHERE event_time IS NULL
                   OR created_at IS NULL
                """);
        }
        if (columnExists(connection, "WORKFLOW_EVENTS", "REMARKS")) {
            execute(connection, """
                UPDATE workflow_events
                SET event_content = COALESCE(event_content, remarks)
                WHERE event_content IS NULL
                """);
        }
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
