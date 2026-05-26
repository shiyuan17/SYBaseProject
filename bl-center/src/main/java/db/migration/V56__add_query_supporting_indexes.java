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

public class V56__add_query_supporting_indexes extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        ensureIndex(connection, "USER_LOGIN_LOGS", "idx_user_login_logs_user_login_at_id",
            "CREATE INDEX idx_user_login_logs_user_login_at_id ON user_login_logs (user_id, login_at, id)");

        ensureIndex(connection, "USER_ROLES", "idx_user_roles_role_id",
            "CREATE INDEX idx_user_roles_role_id ON user_roles (role_id)");
        ensureIndex(connection, "ROLE_PERMISSIONS", "idx_role_permissions_role_id",
            "CREATE INDEX idx_role_permissions_role_id ON role_permissions (role_id)");
        ensureIndex(connection, "ROLE_MENUS", "idx_role_menus_role_id",
            "CREATE INDEX idx_role_menus_role_id ON role_menus (role_id)");
        ensureIndex(connection, "ROLE_MESSAGE_SUBSCRIPTIONS", "idx_role_message_subscriptions_role_id",
            "CREATE INDEX idx_role_message_subscriptions_role_id ON role_message_subscriptions (role_id)");
        ensureIndex(connection, "ROLE_STAT_AUTHORIZATIONS", "idx_role_stat_authorizations_role_id",
            "CREATE INDEX idx_role_stat_authorizations_role_id ON role_stat_authorizations (role_id)");

        ensureIndex(connection, "SPECIMEN_COLLECTION_RECORDS", "idx_specimen_collection_records_app_batch_collected",
            "CREATE INDEX idx_specimen_collection_records_app_batch_collected ON specimen_collection_records (application_id, label_print_batch_no, collected_at, id)");
        ensureIndex(connection, "SPECIMEN_RECEIPTS", "idx_specimen_receipts_specimen_status",
            "CREATE INDEX idx_specimen_receipts_specimen_status ON specimen_receipts (specimen_id, receipt_status)");

        ensureIndex(connection, "WORKFLOW_EVENTS", "idx_workflow_events_application_event_time",
            "CREATE INDEX idx_workflow_events_application_event_time ON workflow_events (application_id, event_time, created_at)");
        ensureIndex(connection, "WORKFLOW_EVENTS", "idx_workflow_events_case_event_time",
            "CREATE INDEX idx_workflow_events_case_event_time ON workflow_events (case_id, event_time, created_at)");
        ensureIndex(connection, "WORKFLOW_EVENTS", "idx_workflow_events_specimen_event_time",
            "CREATE INDEX idx_workflow_events_specimen_event_time ON workflow_events (specimen_id, event_time, created_at)");
        ensureIndex(connection, "WORKFLOW_EVENTS", "idx_workflow_events_transport_order_event_time",
            "CREATE INDEX idx_workflow_events_transport_order_event_time ON workflow_events (transport_order_id, event_time, created_at)");

        ensureIndex(connection, "TECHNICAL_PENDING_TASKS", "idx_technical_pending_tasks_case_status_created",
            "CREATE INDEX idx_technical_pending_tasks_case_status_created ON technical_pending_tasks (case_id, task_status, created_at)");
        ensureIndex(connection, "TECHNICAL_PENDING_TASKS", "idx_technical_pending_tasks_object_status_created",
            "CREATE INDEX idx_technical_pending_tasks_object_status_created ON technical_pending_tasks (task_type, object_type, object_id, task_status, created_at)");

        ensureIndex(connection, "SAMPLINGS", "idx_samplings_case_id",
            "CREATE INDEX idx_samplings_case_id ON samplings (case_id)");
        ensureIndex(connection, "SAMPLINGS", "idx_samplings_specimen_id",
            "CREATE INDEX idx_samplings_specimen_id ON samplings (specimen_id)");

        ensureIndex(connection, "SAMPLING_BLOCKS", "idx_sampling_blocks_case_sequence_id",
            "CREATE INDEX idx_sampling_blocks_case_sequence_id ON sampling_blocks (case_id, sequence_no, id)");
        ensureIndex(connection, "SAMPLING_BLOCKS", "idx_sampling_blocks_sampling_id",
            "CREATE INDEX idx_sampling_blocks_sampling_id ON sampling_blocks (sampling_id)");
        ensureIndex(connection, "SAMPLING_BLOCKS", "idx_sampling_blocks_specimen_id",
            "CREATE INDEX idx_sampling_blocks_specimen_id ON sampling_blocks (specimen_id)");

        ensureIndex(connection, "DEHYDRATION_BATCH_ITEMS", "idx_dehydration_batch_items_batch_loaded_id",
            "CREATE INDEX idx_dehydration_batch_items_batch_loaded_id ON dehydration_batch_items (batch_id, loaded_at, id)");
        ensureIndex(connection, "DEHYDRATION_BATCH_ITEMS", "idx_dehydration_batch_items_specimen_id",
            "CREATE INDEX idx_dehydration_batch_items_specimen_id ON dehydration_batch_items (specimen_id)");

        ensureIndex(connection, "EMBEDDINGS", "idx_embeddings_sampling_block_created_id",
            "CREATE INDEX idx_embeddings_sampling_block_created_id ON embeddings (sampling_block_id, created_at, id)");

        ensureIndex(connection, "EMBEDDING_BOXES", "idx_embedding_boxes_case_created_id",
            "CREATE INDEX idx_embedding_boxes_case_created_id ON embedding_boxes (case_id, created_at, id)");
        ensureIndex(connection, "EMBEDDING_BOXES", "idx_embedding_boxes_sampling_block_id",
            "CREATE INDEX idx_embedding_boxes_sampling_block_id ON embedding_boxes (sampling_block_id)");

        ensureIndex(connection, "SLICINGS", "idx_slicings_embedding_box_id",
            "CREATE INDEX idx_slicings_embedding_box_id ON slicings (embedding_box_id)");

        ensureIndex(connection, "SLIDES", "idx_slides_case_created_id",
            "CREATE INDEX idx_slides_case_created_id ON slides (case_id, created_at, id)");
        ensureIndex(connection, "SLIDES", "idx_slides_slicing_created_id",
            "CREATE INDEX idx_slides_slicing_created_id ON slides (slicing_id, created_at, id)");
        ensureIndex(connection, "SLIDES", "idx_slides_sampling_block_id",
            "CREATE INDEX idx_slides_sampling_block_id ON slides (sampling_block_id)");

        ensureIndex(connection, "SLIDE_STAININGS", "idx_slide_stainings_case_created_id",
            "CREATE INDEX idx_slide_stainings_case_created_id ON slide_stainings (case_id, created_at, id)");

        ensureIndex(connection, "REWORK_ORDERS", "idx_rework_orders_case_created_id",
            "CREATE INDEX idx_rework_orders_case_created_id ON rework_orders (case_id, created_at, id)");

        ensureIndex(connection, "SLIDE_QC_EVALUATIONS", "idx_slide_qc_evaluations_case_evaluated_created",
            "CREATE INDEX idx_slide_qc_evaluations_case_evaluated_created ON slide_qc_evaluations (case_id, evaluated_at, created_at, id)");

        ensureIndex(connection, "CASE_MEDIA_ASSETS", "idx_case_media_assets_object_captured_created",
            "CREATE INDEX idx_case_media_assets_object_captured_created ON case_media_assets (object_type, object_id, captured_at, created_at)");
    }

    private void ensureIndex(Connection connection, String tableName, String indexName, String ddl) throws SQLException {
        if (!tableExists(connection, tableName) || indexExists(connection, tableName, indexName)) {
            return;
        }
        execute(connection, ddl);
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

    private boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
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

    private List<String> identifierCandidates(String identifier) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(identifier);
        candidates.add(identifier.toUpperCase());
        candidates.add(identifier.toLowerCase());
        return new ArrayList<>(candidates);
    }

    private boolean identifierEquals(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
