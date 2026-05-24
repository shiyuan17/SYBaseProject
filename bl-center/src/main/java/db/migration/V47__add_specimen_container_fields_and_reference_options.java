package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class V47__add_specimen_container_fields_and_reference_options extends BaseJavaMigration {

    private static final String ROOT_CATEGORY_ID = "SCC_WORKFLOW_REFERENCE";
    private static final String CONTAINER_NAME_CATEGORY_ID = "SCC_WORKFLOW_REFERENCE_CONTAINER_NAME";

    private static final List<ConfigItemSeed> CONTAINER_NAME_ITEMS = List.of(
        new ConfigItemSeed(
            "SCI_WORKFLOW_CONTAINER_NAME_SPECIMEN_BOTTLE",
            "WORKFLOW_REFERENCE.CONTAINER_NAME.SPECIMEN_BOTTLE",
            "标本瓶",
            "标本瓶",
            10
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_CONTAINER_NAME_WIDE_MOUTH_BOTTLE",
            "WORKFLOW_REFERENCE.CONTAINER_NAME.WIDE_MOUTH_BOTTLE",
            "广口标本瓶",
            "广口标本瓶",
            20
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_CONTAINER_NAME_CYTOLOGY_BOTTLE",
            "WORKFLOW_REFERENCE.CONTAINER_NAME.CYTOLOGY_BOTTLE",
            "细胞保存液瓶",
            "细胞保存液瓶",
            30
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_CONTAINER_NAME_CENTRIFUGE_TUBE",
            "WORKFLOW_REFERENCE.CONTAINER_NAME.CENTRIFUGE_TUBE",
            "离心管",
            "离心管",
            40
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_CONTAINER_NAME_SAMPLE_CUP",
            "WORKFLOW_REFERENCE.CONTAINER_NAME.SAMPLE_CUP",
            "无菌采样杯",
            "无菌采样杯",
            50
        )
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        reconcileSpecimens(connection);
        reconcileWorkflowReferenceOptions(connection);
    }

    private void reconcileSpecimens(Connection connection) throws SQLException {
        if (!tableExists(connection, "SPECIMENS")) {
            return;
        }
        ensureColumn(connection, "SPECIMENS", "CONTAINER_NAME",
            "ALTER TABLE specimens ADD COLUMN container_name VARCHAR(200)");
        ensureColumn(connection, "SPECIMENS", "CONTAINER_COUNT",
            "ALTER TABLE specimens ADD COLUMN container_count INTEGER");
    }

    private void reconcileWorkflowReferenceOptions(Connection connection) throws SQLException {
        if (!tableExists(connection, "SYSTEM_CONFIG_CATEGORIES")
            || !tableExists(connection, "SYSTEM_CONFIG_ITEMS")) {
            return;
        }

        String rootCategoryId = findCategoryIdByCode(connection, "WORKFLOW_REFERENCE");
        if (rootCategoryId == null) {
            rootCategoryId = ROOT_CATEGORY_ID;
            ensureCategory(
                connection,
                rootCategoryId,
                "WORKFLOW_REFERENCE",
                null,
                "工作流参考字典",
                "WORKFLOW_REFERENCE",
                100,
                true
            );
        }

        String containerCategoryId = ensureCategory(
            connection,
            CONTAINER_NAME_CATEGORY_ID,
            "CONTAINER_NAME",
            rootCategoryId,
            "病理标本容器名称",
            "WORKFLOW_REFERENCE",
            150,
            true
        );
        ensureItems(connection, containerCategoryId, CONTAINER_NAME_ITEMS);
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String ddl) throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            execute(connection, ddl);
        }
    }

    private String ensureCategory(
        Connection connection,
        String preferredId,
        String categoryCode,
        String parentId,
        String categoryName,
        String categoryType,
        int sortOrder,
        boolean enabled
    ) throws SQLException {
        String existingId = findCategoryIdByCode(connection, categoryCode);
        if (existingId == null) {
            try (PreparedStatement insert = connection.prepareStatement("""
                insert into system_config_categories
                    (id, parent_id, category_code, category_name, category_type, sort_order,
                     enabled, created_at, updated_at)
                values
                    (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
                Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                insert.setString(1, preferredId);
                insert.setString(2, parentId);
                insert.setString(3, categoryCode);
                insert.setString(4, categoryName);
                insert.setString(5, categoryType);
                insert.setInt(6, sortOrder);
                insert.setInt(7, enabled ? 1 : 0);
                insert.setTimestamp(8, now);
                insert.setTimestamp(9, now);
                insert.executeUpdate();
            }
            return preferredId;
        }

        try (PreparedStatement update = connection.prepareStatement("""
            update system_config_categories
            set parent_id = ?,
                category_name = ?,
                category_type = ?,
                sort_order = ?,
                enabled = ?,
                updated_at = ?
            where id = ?
            """)) {
            update.setString(1, parentId);
            update.setString(2, categoryName);
            update.setString(3, categoryType);
            update.setInt(4, sortOrder);
            update.setInt(5, enabled ? 1 : 0);
            update.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            update.setString(7, existingId);
            update.executeUpdate();
        }
        return existingId;
    }

    private void ensureItems(
        Connection connection,
        String categoryId,
        List<ConfigItemSeed> items
    ) throws SQLException {
        for (ConfigItemSeed item : items) {
            String existingId = findConfigItemId(connection, item.configKey());
            if (existingId == null) {
                try (PreparedStatement insert = connection.prepareStatement("""
                    insert into system_config_items
                        (id, category_id, config_key, config_name, config_value, value_type, sort_order,
                         enabled, remarks, created_at, updated_at)
                    values
                        (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?)
                    """)) {
                    Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                    insert.setString(1, item.id());
                    insert.setString(2, categoryId);
                    insert.setString(3, item.configKey());
                    insert.setString(4, item.configName());
                    insert.setString(5, item.configValue());
                    insert.setString(6, "STRING");
                    insert.setInt(7, item.sortOrder());
                    insert.setString(8, "病理标本容器名称预置项");
                    insert.setTimestamp(9, now);
                    insert.setTimestamp(10, now);
                    insert.executeUpdate();
                }
                continue;
            }

            try (PreparedStatement update = connection.prepareStatement("""
                update system_config_items
                set category_id = ?,
                    config_name = ?,
                    config_value = ?,
                    value_type = ?,
                    sort_order = ?,
                    enabled = 1,
                    remarks = ?,
                    updated_at = ?
                where id = ?
                """)) {
                update.setString(1, categoryId);
                update.setString(2, item.configName());
                update.setString(3, item.configValue());
                update.setString(4, "STRING");
                update.setInt(5, item.sortOrder());
                update.setString(6, "病理标本容器名称预置项");
                update.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                update.setString(8, existingId);
                update.executeUpdate();
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

    private String findCategoryIdByCode(Connection connection, String categoryCode) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("""
            select id
            from system_config_categories
            where category_code = ?
            """)) {
            query.setString(1, categoryCode);
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    private String findConfigItemId(Connection connection, String configKey) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("""
            select id
            from system_config_items
            where config_key = ?
            """)) {
            query.setString(1, configKey);
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
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

    private record ConfigItemSeed(
        String id,
        String configKey,
        String configName,
        String configValue,
        int sortOrder
    ) {
    }
}
