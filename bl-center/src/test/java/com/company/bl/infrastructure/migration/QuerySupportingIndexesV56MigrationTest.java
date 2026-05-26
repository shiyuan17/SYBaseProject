package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QuerySupportingIndexesV56MigrationTest {

    @Test
    void shouldCreateQuerySupportingIndexesAndRemainIdempotent() throws Exception {
        String url = "jdbc:h2:mem:query_supporting_indexes_v56_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway flyway = Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("56"))
            .load();
        flyway.migrate();
        flyway.migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            assertHasIndex(connection, "user_login_logs", "idx_user_login_logs_user_login_at_id");
            assertHasIndex(connection, "user_roles", "idx_user_roles_role_id");
            assertHasIndex(connection, "role_permissions", "idx_role_permissions_role_id");
            assertHasIndex(connection, "role_menus", "idx_role_menus_role_id");
            assertHasIndex(connection, "role_message_subscriptions", "idx_role_message_subscriptions_role_id");
            assertHasIndex(connection, "role_stat_authorizations", "idx_role_stat_authorizations_role_id");
            assertHasIndex(connection, "specimen_collection_records", "idx_specimen_collection_records_app_batch_collected");
            assertHasIndex(connection, "specimen_receipts", "idx_specimen_receipts_specimen_status");
            assertHasIndex(connection, "workflow_events", "idx_workflow_events_application_event_time");
            assertHasIndex(connection, "workflow_events", "idx_workflow_events_case_event_time");
            assertHasIndex(connection, "workflow_events", "idx_workflow_events_specimen_event_time");
            assertHasIndex(connection, "workflow_events", "idx_workflow_events_transport_order_event_time");
            assertHasIndex(connection, "technical_pending_tasks", "idx_technical_pending_tasks_case_status_created");
            assertHasIndex(connection, "technical_pending_tasks", "idx_technical_pending_tasks_object_status_created");
            assertHasIndex(connection, "sampling_blocks", "idx_sampling_blocks_case_sequence_id");
            assertHasIndex(connection, "dehydration_batch_items", "idx_dehydration_batch_items_batch_loaded_id");
            assertHasIndex(connection, "embeddings", "idx_embeddings_sampling_block_created_id");
            assertHasIndex(connection, "slides", "idx_slides_case_created_id");
            assertHasIndex(connection, "slide_qc_evaluations", "idx_slide_qc_evaluations_case_evaluated_created");
            assertHasIndex(connection, "case_media_assets", "idx_case_media_assets_object_captured_created");
        }
    }

    private void assertHasIndex(Connection connection, String tableName, String indexName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        Set<String> foundNames = new LinkedHashSet<>();
        try (ResultSet resultSet = metaData.getIndexInfo(null, null, tableName, false, false)) {
            while (resultSet.next()) {
                String actualName = resultSet.getString("INDEX_NAME");
                if (actualName != null) {
                    foundNames.add(actualName.toLowerCase());
                }
            }
        }
        assertTrue(foundNames.contains(indexName.toLowerCase()),
            () -> "missing index " + indexName + " on " + tableName + ", found=" + foundNames);
    }
}
