package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class V53__seed_grossing_reference_options extends BaseJavaMigration {

    private static final String ROOT_CATEGORY_ID = "SCC_WORKFLOW_REFERENCE";
    private static final String SPECIMEN_IMAGE_SIZE_CATEGORY_ID = "SCC_WORKFLOW_REFERENCE_SPECIMEN_IMAGE_SIZE";
    private static final String CUT_SURFACE_FEATURE_CATEGORY_ID = "SCC_WORKFLOW_REFERENCE_CUT_SURFACE_FEATURE";
    private static final String MARGIN_MARKING_CATEGORY_ID = "SCC_WORKFLOW_REFERENCE_MARGIN_MARKING";

    private static final List<ConfigItemSeed> SPECIMEN_IMAGE_SIZE_ITEMS = List.of(
        new ConfigItemSeed(
            "SCI_WORKFLOW_SPECIMEN_IMAGE_SIZE_3_2_2_1_1_0",
            "WORKFLOW_REFERENCE.SPECIMEN_IMAGE_SIZE.3_2X2_1X1_0CM",
            "3.2x2.1x1.0cm",
            "3.2x2.1x1.0cm",
            10
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_SPECIMEN_IMAGE_SIZE_1_5_1_0_0_3",
            "WORKFLOW_REFERENCE.SPECIMEN_IMAGE_SIZE.1_5X1_0X0_3CM",
            "1.5x1.0x0.3cm",
            "1.5x1.0x0.3cm",
            20
        )
    );

    private static final List<ConfigItemSeed> CUT_SURFACE_FEATURE_ITEMS = List.of(
        new ConfigItemSeed(
            "SCI_WORKFLOW_CUT_SURFACE_FEATURE_GRAY_WHITE",
            "WORKFLOW_REFERENCE.CUT_SURFACE_FEATURE.GRAY_WHITE",
            "灰白",
            "灰白",
            10
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_CUT_SURFACE_FEATURE_FIRM",
            "WORKFLOW_REFERENCE.CUT_SURFACE_FEATURE.FIRM",
            "质硬",
            "质硬",
            20
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_CUT_SURFACE_FEATURE_NECROSIS",
            "WORKFLOW_REFERENCE.CUT_SURFACE_FEATURE.NECROSIS",
            "坏死",
            "坏死",
            30
        )
    );

    private static final List<ConfigItemSeed> MARGIN_MARKING_ITEMS = List.of(
        new ConfigItemSeed(
            "SCI_WORKFLOW_MARGIN_MARKING_UPPER",
            "WORKFLOW_REFERENCE.MARGIN_MARKING.UPPER",
            "上缘墨染",
            "上缘墨染",
            10
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_MARGIN_MARKING_LOWER",
            "WORKFLOW_REFERENCE.MARGIN_MARKING.LOWER",
            "下缘墨染",
            "下缘墨染",
            20
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_MARGIN_MARKING_BASE",
            "WORKFLOW_REFERENCE.MARGIN_MARKING.BASE",
            "基底部墨染",
            "基底部墨染",
            30
        )
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
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

        String specimenImageSizeCategoryId = ensureCategory(
            connection,
            SPECIMEN_IMAGE_SIZE_CATEGORY_ID,
            "SPECIMEN_IMAGE_SIZE",
            rootCategoryId,
            "标本影像大小",
            "WORKFLOW_REFERENCE",
            160,
            true
        );
        String cutSurfaceFeatureCategoryId = ensureCategory(
            connection,
            CUT_SURFACE_FEATURE_CATEGORY_ID,
            "CUT_SURFACE_FEATURE",
            rootCategoryId,
            "切面特征",
            "WORKFLOW_REFERENCE",
            170,
            true
        );
        String marginMarkingCategoryId = ensureCategory(
            connection,
            MARGIN_MARKING_CATEGORY_ID,
            "MARGIN_MARKING",
            rootCategoryId,
            "切缘标记",
            "WORKFLOW_REFERENCE",
            180,
            true
        );

        ensureItems(connection, specimenImageSizeCategoryId, SPECIMEN_IMAGE_SIZE_ITEMS, "取材标本影像大小预置项");
        ensureItems(connection, cutSurfaceFeatureCategoryId, CUT_SURFACE_FEATURE_ITEMS, "取材切面特征预置项");
        ensureItems(connection, marginMarkingCategoryId, MARGIN_MARKING_ITEMS, "取材切缘标记预置项");
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
        List<ConfigItemSeed> items,
        String remarks
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
                    insert.setString(8, remarks);
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
                update.setString(6, remarks);
                update.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                update.setString(8, existingId);
                update.executeUpdate();
            }
        }
    }

    private String findCategoryIdByCode(Connection connection, String categoryCode) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("""
            select id
            from system_config_categories
            where category_code = ?
            """)) {
            query.setString(1, categoryCode);
            try (ResultSet resultSet = query.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }
        return null;
    }

    private String findConfigItemId(Connection connection, String configKey) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("""
            select id
            from system_config_items
            where config_key = ?
            """)) {
            query.setString(1, configKey);
            try (ResultSet resultSet = query.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }
        return null;
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getTables(null, null, null, new String[]{"TABLE"})) {
            while (resultSet.next()) {
                String actualTableName = resultSet.getString("TABLE_NAME");
                if (tableName.equalsIgnoreCase(actualTableName)) {
                    return true;
                }
            }
        }
        return false;
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
