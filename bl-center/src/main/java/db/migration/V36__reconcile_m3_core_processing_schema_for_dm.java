package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class V36__reconcile_m3_core_processing_schema_for_dm extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isDmDatabase(connection)) {
            return;
        }
        reconcileSamplings(connection);
        reconcileSamplingBlocks(connection);
        reconcileDehydrationBatches(connection);
        reconcileDehydrationBatchItems(connection);
        reconcileEmbeddings(connection);
        reconcileEmbeddingBoxes(connection);
        reconcileSlicings(connection);
        reconcileSlides(connection);
        reconcileSlideStainings(connection);
        reconcileReworkOrders(connection);
        reconcileSlideQcEvaluations(connection);
        reconcileCaseMediaAssets(connection);
    }

    private void reconcileSamplings(Connection connection) throws SQLException {
        if (!tableExists(connection, "SAMPLINGS")) {
            return;
        }
        ensureColumn(connection, "SAMPLINGS", "CREATED_AT", "ALTER TABLE samplings ADD COLUMN created_at TIMESTAMP");
        ensureColumn(connection, "SAMPLINGS", "UPDATED_AT", "ALTER TABLE samplings ADD COLUMN updated_at TIMESTAMP");
    }

    private void reconcileSamplingBlocks(Connection connection) throws SQLException {
        if (!tableExists(connection, "SAMPLING_BLOCKS")) {
            return;
        }
        ensureColumn(connection, "SAMPLING_BLOCKS", "CREATED_AT", "ALTER TABLE sampling_blocks ADD COLUMN created_at TIMESTAMP");
        ensureColumn(connection, "SAMPLING_BLOCKS", "UPDATED_AT", "ALTER TABLE sampling_blocks ADD COLUMN updated_at TIMESTAMP");
    }

    private void reconcileDehydrationBatches(Connection connection) throws SQLException {
        if (!tableExists(connection, "DEHYDRATION_BATCHES")) {
            return;
        }
        ensureColumn(connection, "DEHYDRATION_BATCHES", "CASE_ID", "ALTER TABLE dehydration_batches ADD COLUMN case_id VARCHAR(64)");
        ensureColumn(connection, "DEHYDRATION_BATCHES", "CREATED_AT", "ALTER TABLE dehydration_batches ADD COLUMN created_at TIMESTAMP");
        ensureColumn(connection, "DEHYDRATION_BATCHES", "UPDATED_AT", "ALTER TABLE dehydration_batches ADD COLUMN updated_at TIMESTAMP");
    }

    private void reconcileDehydrationBatchItems(Connection connection) throws SQLException {
        if (!tableExists(connection, "DEHYDRATION_BATCH_ITEMS")) {
            return;
        }
        ensureColumn(connection, "DEHYDRATION_BATCH_ITEMS", "CREATED_AT", "ALTER TABLE dehydration_batch_items ADD COLUMN created_at TIMESTAMP");
        ensureColumn(connection, "DEHYDRATION_BATCH_ITEMS", "UPDATED_AT", "ALTER TABLE dehydration_batch_items ADD COLUMN updated_at TIMESTAMP");
        relaxNullable(connection, "DEHYDRATION_BATCH_ITEMS", "OBJECT_TYPE");
        relaxNullable(connection, "DEHYDRATION_BATCH_ITEMS", "OBJECT_ID");
    }

    private void reconcileEmbeddings(Connection connection) throws SQLException {
        if (!tableExists(connection, "EMBEDDINGS")) {
            return;
        }
        ensureColumn(connection, "EMBEDDINGS", "SAMPLING_BLOCK_ID", "ALTER TABLE embeddings ADD COLUMN sampling_block_id VARCHAR(64)");
        ensureColumn(connection, "EMBEDDINGS", "REMARKS", "ALTER TABLE embeddings ADD COLUMN remarks VARCHAR(500)");
        ensureColumn(connection, "EMBEDDINGS", "CREATED_AT", "ALTER TABLE embeddings ADD COLUMN created_at TIMESTAMP");
        ensureColumn(connection, "EMBEDDINGS", "UPDATED_AT", "ALTER TABLE embeddings ADD COLUMN updated_at TIMESTAMP");
    }

    private void reconcileEmbeddingBoxes(Connection connection) throws SQLException {
        if (!tableExists(connection, "EMBEDDING_BOXES")) {
            return;
        }
        ensureColumn(connection, "EMBEDDING_BOXES", "SAMPLING_BLOCK_ID", "ALTER TABLE embedding_boxes ADD COLUMN sampling_block_id VARCHAR(64)");
        ensureColumn(connection, "EMBEDDING_BOXES", "UPDATED_AT", "ALTER TABLE embedding_boxes ADD COLUMN updated_at TIMESTAMP");
    }

    private void reconcileSlicings(Connection connection) throws SQLException {
        if (!tableExists(connection, "SLICINGS")) {
            return;
        }
        ensureColumn(connection, "SLICINGS", "EMBEDDING_BOX_ID", "ALTER TABLE slicings ADD COLUMN embedding_box_id VARCHAR(64)");
        ensureColumn(connection, "SLICINGS", "SLICE_COUNT_PER_SLIDE", "ALTER TABLE slicings ADD COLUMN slice_count_per_slide INTEGER");
        ensureColumn(connection, "SLICINGS", "CREATED_AT", "ALTER TABLE slicings ADD COLUMN created_at TIMESTAMP");
        ensureColumn(connection, "SLICINGS", "UPDATED_AT", "ALTER TABLE slicings ADD COLUMN updated_at TIMESTAMP");
    }

    private void reconcileSlides(Connection connection) throws SQLException {
        if (!tableExists(connection, "SLIDES")) {
            return;
        }
        ensureColumn(connection, "SLIDES", "EMBEDDING_BOX_ID", "ALTER TABLE slides ADD COLUMN embedding_box_id VARCHAR(64)");
        ensureColumn(connection, "SLIDES", "SAMPLING_BLOCK_ID", "ALTER TABLE slides ADD COLUMN sampling_block_id VARCHAR(64)");
        ensureColumn(connection, "SLIDES", "UPDATED_AT", "ALTER TABLE slides ADD COLUMN updated_at TIMESTAMP");
    }

    private void reconcileSlideStainings(Connection connection) throws SQLException {
        if (!tableExists(connection, "SLIDE_STAININGS")) {
            return;
        }
        ensureColumn(connection, "SLIDE_STAININGS", "CREATED_AT", "ALTER TABLE slide_stainings ADD COLUMN created_at TIMESTAMP");
        ensureColumn(connection, "SLIDE_STAININGS", "UPDATED_AT", "ALTER TABLE slide_stainings ADD COLUMN updated_at TIMESTAMP");
    }

    private void reconcileReworkOrders(Connection connection) throws SQLException {
        if (!tableExists(connection, "REWORK_ORDERS")) {
            return;
        }
        ensureColumn(connection, "REWORK_ORDERS", "SAMPLING_BLOCK_ID", "ALTER TABLE rework_orders ADD COLUMN sampling_block_id VARCHAR(64)");
        ensureColumn(connection, "REWORK_ORDERS", "EMBEDDING_BOX_ID", "ALTER TABLE rework_orders ADD COLUMN embedding_box_id VARCHAR(64)");
        ensureColumn(connection, "REWORK_ORDERS", "CREATED_AT", "ALTER TABLE rework_orders ADD COLUMN created_at TIMESTAMP");
        ensureColumn(connection, "REWORK_ORDERS", "UPDATED_AT", "ALTER TABLE rework_orders ADD COLUMN updated_at TIMESTAMP");
    }

    private void reconcileSlideQcEvaluations(Connection connection) throws SQLException {
        if (!tableExists(connection, "SLIDE_QC_EVALUATIONS")) {
            return;
        }
        ensureColumn(connection, "SLIDE_QC_EVALUATIONS", "CREATED_AT", "ALTER TABLE slide_qc_evaluations ADD COLUMN created_at TIMESTAMP");
        ensureColumn(connection, "SLIDE_QC_EVALUATIONS", "UPDATED_AT", "ALTER TABLE slide_qc_evaluations ADD COLUMN updated_at TIMESTAMP");
    }

    private void reconcileCaseMediaAssets(Connection connection) throws SQLException {
        if (!tableExists(connection, "CASE_MEDIA_ASSETS")) {
            return;
        }
        ensureColumn(connection, "CASE_MEDIA_ASSETS", "CREATED_AT", "ALTER TABLE case_media_assets ADD COLUMN created_at TIMESTAMP");
        ensureColumn(connection, "CASE_MEDIA_ASSETS", "UPDATED_AT", "ALTER TABLE case_media_assets ADD COLUMN updated_at TIMESTAMP");
        relaxNullable(connection, "CASE_MEDIA_ASSETS", "CAPTURE_NODE");
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String ddl) throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            execute(connection, ddl);
        }
    }

    private void relaxNullable(Connection connection, String tableName, String columnName) throws SQLException {
        if (!columnExists(connection, tableName, columnName) || isNullable(connection, tableName, columnName)) {
            return;
        }
        execute(connection, "ALTER TABLE " + tableName + " MODIFY " + columnName + " NULL");
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getColumns(null, null, tableName, columnName)) {
            return resultSet.next();
        }
    }

    private boolean isNullable(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getColumns(null, null, tableName, columnName)) {
            if (!resultSet.next()) {
                return true;
            }
            return resultSet.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
        }
    }

    private boolean isDmDatabase(Connection connection) throws SQLException {
        String productName = connection.getMetaData().getDatabaseProductName();
        return productName != null && productName.toUpperCase(Locale.ROOT).contains("DM");
    }
}