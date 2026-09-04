package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class V121__extend_medical_order_qc_by_slide extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "medical_order_qc_evaluations")) {
            return;
        }
        addColumnIfMissing(connection, "medical_order_qc_evaluations", "target_slide_id", "VARCHAR(64)");
        addColumnIfMissing(connection, "medical_order_qc_evaluations", "target_slide_no", "VARCHAR(64)");
        addColumnIfMissing(connection, "medical_order_qc_evaluations", "version", "INTEGER DEFAULT 0 NOT NULL");
        createIndexIfMissing(connection,
            "IDX_MO_QC_ORDER_ASPECT_SLIDE",
            "CREATE INDEX idx_mo_qc_order_aspect_slide "
                + "ON medical_order_qc_evaluations (order_id, qc_aspect, target_slide_id, evaluated_at)");
        backfillLatestUnambiguousEvaluations(connection);
    }

    private void backfillLatestUnambiguousEvaluations(Connection connection) throws SQLException {
        if (!tableExists(connection, "medical_orders") || !tableExists(connection, "slides")) {
            return;
        }
        Set<String> seenKeys = new HashSet<>();
        try (PreparedStatement select = connection.prepareStatement("""
            select evaluation.id, evaluation.order_id, evaluation.qc_aspect,
                   orders.target_slide_id, orders.target_slide_no
            from medical_order_qc_evaluations evaluation
            join medical_orders orders on orders.id = evaluation.order_id
            join slides slide on slide.id = orders.target_slide_id and slide.case_id = orders.case_id
            where evaluation.target_slide_id is null
              and orders.target_slide_id is not null
              and (upper(orders.target_type) = 'SLIDE'
                   or (orders.target_type is null
                       and orders.target_block_id is null
                       and orders.target_specimen_id is null))
            order by evaluation.order_id, evaluation.qc_aspect,
                     evaluation.evaluated_at desc, evaluation.created_at desc, evaluation.id desc
            """);
             PreparedStatement update = connection.prepareStatement("""
                 update medical_order_qc_evaluations
                 set target_slide_id = ?, target_slide_no = ?
                 where id = ? and target_slide_id is null
                 """);
             ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                String key = rows.getString("order_id") + "\u0000" + rows.getString("qc_aspect");
                if (!seenKeys.add(key)) {
                    continue;
                }
                update.setString(1, rows.getString("target_slide_id"));
                update.setString(2, rows.getString("target_slide_no"));
                update.setString(3, rows.getString("id"));
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    private void addColumnIfMissing(Connection connection, String table, String column, String definition)
        throws SQLException {
        if (columnExists(connection, table, column)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD " + column + " " + definition);
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(null, null, null, null)) {
            while (columns.next()) {
                if (table.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                    && column.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tableExists(Connection connection, String table) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(null, null, null, new String[] {"TABLE"})) {
            while (tables.next()) {
                if (table.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void createIndexIfMissing(Connection connection, String indexName, String sql) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet indexes = metadata.getIndexInfo(null, null, "medical_order_qc_evaluations", false, false)) {
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
