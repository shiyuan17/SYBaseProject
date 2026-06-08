package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class V78__medical_order_dictionary_snapshot extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "MEDICAL_ORDERS")) {
            return;
        }

        ensureColumn(connection, "MEDICAL_ORDERS", "ORDER_ITEM_ID",
            "ALTER TABLE medical_orders ADD COLUMN order_item_id VARCHAR(64)");
        ensureColumn(connection, "MEDICAL_ORDERS", "ORDER_ITEM_CODE",
            "ALTER TABLE medical_orders ADD COLUMN order_item_code VARCHAR(64)");
        ensureColumn(connection, "MEDICAL_ORDERS", "ORDER_ITEM_NAME",
            "ALTER TABLE medical_orders ADD COLUMN order_item_name VARCHAR(200)");
        ensureColumn(connection, "MEDICAL_ORDERS", "ORDER_CATEGORY_ID",
            "ALTER TABLE medical_orders ADD COLUMN order_category_id VARCHAR(64)");
        ensureColumn(connection, "MEDICAL_ORDERS", "ORDER_CATEGORY_CODE",
            "ALTER TABLE medical_orders ADD COLUMN order_category_code VARCHAR(64)");
        ensureColumn(connection, "MEDICAL_ORDERS", "ORDER_CATEGORY_NAME",
            "ALTER TABLE medical_orders ADD COLUMN order_category_name VARCHAR(100)");
        ensureIndex(connection, "IDX_MEDICAL_ORDERS_CATEGORY_STATUS",
            "CREATE INDEX idx_medical_orders_category_status ON medical_orders (order_category_code, status)");

        upsertCategory(connection, "ODC_CYTOLOGY", "ODC_ROOT", "CYTOLOGY", "细胞学", 140);
        upsertCategory(connection, "ODC_LIQUID_CYTOLOGY", "ODC_ROOT", "LIQUID_CYTOLOGY", "液基细胞学", 150);
        upsertItem(connection, "ODI_CYTOLOGY_ROUTINE", "ODC_CYTOLOGY", "CYTOLOGY_ROUTINE",
            "细胞学", "CYTOLOGY", "细胞学", 10);
        upsertItem(connection, "ODI_CYTOLOGY_CONSULTATION", "ODC_CYTOLOGY", "CYTOLOGY_CONSULTATION",
            "细胞学会诊", "CYTOLOGY", "细胞学会诊", 20);
        upsertItem(connection, "ODI_LIQUID_CYTOLOGY_GYN", "ODC_LIQUID_CYTOLOGY", "LIQUID_CYTOLOGY_GYN",
            "妇科液基细胞学", "LIQUID_CYTOLOGY", "妇科液基细胞学", 10);
        upsertItem(connection, "ODI_LIQUID_CYTOLOGY_NONGYN", "ODC_LIQUID_CYTOLOGY", "LIQUID_CYTOLOGY_NONGYN",
            "非妇科液基细胞学", "LIQUID_CYTOLOGY", "非妇科液基细胞学", 20);
        backfillMedicalOrderSnapshots(connection);
    }

    private void upsertCategory(
        Connection connection,
        String id,
        String parentId,
        String categoryCode,
        String categoryName,
        int sortOrder
    ) throws SQLException {
        if (existsByColumn(connection, "medical_order_dict_categories", "category_code", categoryCode)) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
            """
                INSERT INTO medical_order_dict_categories
                    (id, parent_id, category_code, category_name, sort_order, enabled)
                VALUES (?, ?, ?, ?, ?, 1)
                """)) {
            statement.setString(1, id);
            statement.setString(2, parentId);
            statement.setString(3, categoryCode);
            statement.setString(4, categoryName);
            statement.setInt(5, sortOrder);
            statement.executeUpdate();
        }
    }

    private void upsertItem(
        Connection connection,
        String id,
        String categoryId,
        String orderItemCode,
        String orderItemName,
        String orderType,
        String defaultContent,
        int sortOrder
    ) throws SQLException {
        if (existsByColumn(connection, "medical_order_dict_items", "order_item_code", orderItemCode)) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
            """
                INSERT INTO medical_order_dict_items
                    (id, category_id, order_item_code, order_item_name, order_type,
                     default_content, execution_scope, sort_order, enabled)
                VALUES (?, ?, ?, ?, ?, ?, 'TECHNICIAN', ?, 1)
                """)) {
            statement.setString(1, id);
            statement.setString(2, categoryId);
            statement.setString(3, orderItemCode);
            statement.setString(4, orderItemName);
            statement.setString(5, orderType);
            statement.setString(6, defaultContent);
            statement.setInt(7, sortOrder);
            statement.executeUpdate();
        }
    }

    private void backfillMedicalOrderSnapshots(Connection connection) throws SQLException {
        execute(connection,
            """
                UPDATE medical_orders mo
                SET order_item_id = (
                        SELECT item.id
                        FROM medical_order_dict_items item
                        WHERE lower(mo.order_content) = lower(item.default_content)
                           OR lower(mo.order_content) = lower(item.order_item_name)
                           OR lower(mo.order_type) = lower(item.order_item_code)
                        FETCH FIRST 1 ROW ONLY
                    ),
                    order_item_code = (
                        SELECT item.order_item_code
                        FROM medical_order_dict_items item
                        WHERE lower(mo.order_content) = lower(item.default_content)
                           OR lower(mo.order_content) = lower(item.order_item_name)
                           OR lower(mo.order_type) = lower(item.order_item_code)
                        FETCH FIRST 1 ROW ONLY
                    ),
                    order_item_name = (
                        SELECT item.order_item_name
                        FROM medical_order_dict_items item
                        WHERE lower(mo.order_content) = lower(item.default_content)
                           OR lower(mo.order_content) = lower(item.order_item_name)
                           OR lower(mo.order_type) = lower(item.order_item_code)
                        FETCH FIRST 1 ROW ONLY
                    ),
                    order_category_id = (
                        SELECT category.id
                        FROM medical_order_dict_items item
                        JOIN medical_order_dict_categories category ON category.id = item.category_id
                        WHERE lower(mo.order_content) = lower(item.default_content)
                           OR lower(mo.order_content) = lower(item.order_item_name)
                           OR lower(mo.order_type) = lower(item.order_item_code)
                        FETCH FIRST 1 ROW ONLY
                    ),
                    order_category_code = (
                        SELECT category.category_code
                        FROM medical_order_dict_items item
                        JOIN medical_order_dict_categories category ON category.id = item.category_id
                        WHERE lower(mo.order_content) = lower(item.default_content)
                           OR lower(mo.order_content) = lower(item.order_item_name)
                           OR lower(mo.order_type) = lower(item.order_item_code)
                        FETCH FIRST 1 ROW ONLY
                    ),
                    order_category_name = (
                        SELECT category.category_name
                        FROM medical_order_dict_items item
                        JOIN medical_order_dict_categories category ON category.id = item.category_id
                        WHERE lower(mo.order_content) = lower(item.default_content)
                           OR lower(mo.order_content) = lower(item.order_item_name)
                           OR lower(mo.order_type) = lower(item.order_item_code)
                        FETCH FIRST 1 ROW ONLY
                    )
                WHERE mo.order_item_id IS NULL
                  AND EXISTS (
                      SELECT 1
                      FROM medical_order_dict_items item
                      WHERE lower(mo.order_content) = lower(item.default_content)
                         OR lower(mo.order_content) = lower(item.order_item_name)
                         OR lower(mo.order_type) = lower(item.order_item_code)
                  )
                """);
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String ddl) throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            execute(connection, ddl);
        }
    }

    private void ensureIndex(Connection connection, String indexName, String ddl) throws SQLException {
        if (!indexExists(connection, indexName)) {
            execute(connection, ddl);
        }
    }

    private boolean existsByColumn(Connection connection, String tableName, String columnName, String value) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + columnName + " = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
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

    private boolean indexExists(Connection connection, String indexName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet tables = metaData.getTables(null, null, null, new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                for (String candidate : identifierCandidates(tableName)) {
                    try (ResultSet indexes = metaData.getIndexInfo(null, null, candidate, false, false)) {
                        while (indexes.next()) {
                            if (identifierEquals(indexes.getString("INDEX_NAME"), indexName)) {
                                return true;
                            }
                        }
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
