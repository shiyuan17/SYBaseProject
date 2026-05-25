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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class V54__reconcile_workflow_reference_options extends BaseJavaMigration {

    private static final String MENU_ID = "MENU_CONFIGS";
    private static final String LEGACY_MENU_ID = "MENU_SYS_CONFIG";
    private static final String PERMISSION_ID = "PERM_WORKFLOW_REFERENCE_QUERY";
    private static final String ACTION_KEY = "WORKFLOW_REFERENCE_QUERY";
    private static final String ROOT_CATEGORY_ID = "SCC_WORKFLOW_REFERENCE";
    private static final String ROOT_CATEGORY_CODE = "WORKFLOW_REFERENCE";
    private static final String CATEGORY_TYPE = "WORKFLOW_REFERENCE";
    private static final String ITEM_VALUE_TYPE = "STRING";
    private static final String ROOT_REMARKS = "工作流参考字典预置项";

    private static final List<String> ROLE_IDS = List.of(
        "ROLE_PATHOLOGY_ADMIN",
        "ROLE_M2_CLINICAL_REGISTER",
        "ROLE_M2_FIXATION_VERIFY",
        "ROLE_M3_GROSSING"
    );

    private static final List<CategorySeed> CATEGORY_SEEDS = List.of(
        new CategorySeed("SCC_WORKFLOW_REFERENCE_SPECIMEN_TYPE", "SPECIMEN_TYPE", "标本类型", 110),
        new CategorySeed("SCC_WORKFLOW_REFERENCE_COLLECTION_MODE", "COLLECTION_MODE", "采集方式", 120),
        new CategorySeed("SCC_WORKFLOW_REFERENCE_CLINICAL_SYMPTOM", "CLINICAL_SYMPTOM", "临床症状", 130),
        new CategorySeed("SCC_WORKFLOW_REFERENCE_FIXATION", "FIXATION_LIQUID_TYPE", "固定液类型", 140),
        new CategorySeed("SCC_WORKFLOW_REFERENCE_CONTAINER_NAME", "CONTAINER_NAME", "病理标本容器名称", 150),
        new CategorySeed("SCC_WORKFLOW_REFERENCE_SPECIMEN_IMAGE_SIZE", "SPECIMEN_IMAGE_SIZE", "标本影像大小", 160),
        new CategorySeed("SCC_WORKFLOW_REFERENCE_CUT_SURFACE_FEATURE", "CUT_SURFACE_FEATURE", "切面特征", 170),
        new CategorySeed("SCC_WORKFLOW_REFERENCE_MARGIN_MARKING", "MARGIN_MARKING", "切缘标记", 180)
    );

    private static final List<ItemSeed> ITEM_SEEDS = List.of(
        new ItemSeed("SCI_WORKFLOW_SPECIMEN_TYPE_ROUTINE", "SPECIMEN_TYPE", "WORKFLOW_REFERENCE.SPECIMEN_TYPE.ROUTINE", "常规", "ROUTINE", 10, ROOT_REMARKS),
        new ItemSeed("SCI_WORKFLOW_SPECIMEN_TYPE_FROZEN", "SPECIMEN_TYPE", "WORKFLOW_REFERENCE.SPECIMEN_TYPE.FROZEN", "冰冻", "FROZEN", 20, ROOT_REMARKS),
        new ItemSeed("SCI_WORKFLOW_SPECIMEN_TYPE_BIOPSY", "SPECIMEN_TYPE", "WORKFLOW_REFERENCE.SPECIMEN_TYPE.BIOPSY", "活检", "BIOPSY", 30, ROOT_REMARKS),
        new ItemSeed("SCI_WORKFLOW_SPECIMEN_TYPE_CYTOLOGY", "SPECIMEN_TYPE", "WORKFLOW_REFERENCE.SPECIMEN_TYPE.CYTOLOGY", "细胞学", "CYTOLOGY", 40, ROOT_REMARKS),
        new ItemSeed("SCI_WORKFLOW_COLLECTION_MODE_SURGERY", "COLLECTION_MODE", "WORKFLOW_REFERENCE.COLLECTION_MODE.SURGERY", "手术", "SURGERY", 10, ROOT_REMARKS),
        new ItemSeed("SCI_WORKFLOW_COLLECTION_MODE_BIOPSY", "COLLECTION_MODE", "WORKFLOW_REFERENCE.COLLECTION_MODE.BIOPSY", "活检", "BIOPSY", 20, ROOT_REMARKS),
        new ItemSeed("SCI_WORKFLOW_COLLECTION_MODE_PUNCTURE", "COLLECTION_MODE", "WORKFLOW_REFERENCE.COLLECTION_MODE.PUNCTURE", "穿刺", "PUNCTURE", 30, ROOT_REMARKS),
        new ItemSeed("SCI_WORKFLOW_COLLECTION_MODE_CYTOLOGY", "COLLECTION_MODE", "WORKFLOW_REFERENCE.COLLECTION_MODE.CYTOLOGY", "细胞学", "CYTOLOGY", 40, ROOT_REMARKS),
        new ItemSeed("SCI_WORKFLOW_CLINICAL_SYMPTOM_MASS", "CLINICAL_SYMPTOM", "WORKFLOW_REFERENCE.CLINICAL_SYMPTOM.MASS", "肿物", null, 10, ROOT_REMARKS),
        new ItemSeed("SCI_WORKFLOW_CLINICAL_SYMPTOM_PAIN", "CLINICAL_SYMPTOM", "WORKFLOW_REFERENCE.CLINICAL_SYMPTOM.PAIN", "疼痛", null, 20, ROOT_REMARKS),
        new ItemSeed("SCI_WORKFLOW_CLINICAL_SYMPTOM_BLEEDING", "CLINICAL_SYMPTOM", "WORKFLOW_REFERENCE.CLINICAL_SYMPTOM.BLEEDING", "出血", null, 30, ROOT_REMARKS),
        new ItemSeed("SCI_WORKFLOW_CLINICAL_SYMPTOM_FEVER", "CLINICAL_SYMPTOM", "WORKFLOW_REFERENCE.CLINICAL_SYMPTOM.FEVER", "发热", null, 40, ROOT_REMARKS),
        new ItemSeed("SCI_WORKFLOW_FIXATION_FORMALIN", "FIXATION_LIQUID_TYPE", "WORKFLOW_REFERENCE.FIXATION_LIQUID_TYPE.FORMALIN", "10% 中性福尔马林", "FORMALIN", 10, ROOT_REMARKS),
        new ItemSeed("SCI_WORKFLOW_FIXATION_ETHANOL", "FIXATION_LIQUID_TYPE", "WORKFLOW_REFERENCE.FIXATION_LIQUID_TYPE.ETHANOL", "酒精", "ETHANOL", 20, ROOT_REMARKS),
        new ItemSeed("SCI_WORKFLOW_FIXATION_SALINE", "FIXATION_LIQUID_TYPE", "WORKFLOW_REFERENCE.FIXATION_LIQUID_TYPE.SALINE", "生理盐水", "SALINE", 30, ROOT_REMARKS),
        new ItemSeed("SCI_WORKFLOW_CONTAINER_NAME_SPECIMEN_BOTTLE", "CONTAINER_NAME", "WORKFLOW_REFERENCE.CONTAINER_NAME.SPECIMEN_BOTTLE", "标本瓶", "标本瓶", 10, "病理标本容器名称预置项"),
        new ItemSeed("SCI_WORKFLOW_CONTAINER_NAME_WIDE_MOUTH_BOTTLE", "CONTAINER_NAME", "WORKFLOW_REFERENCE.CONTAINER_NAME.WIDE_MOUTH_BOTTLE", "广口标本瓶", "广口标本瓶", 20, "病理标本容器名称预置项"),
        new ItemSeed("SCI_WORKFLOW_CONTAINER_NAME_CYTOLOGY_BOTTLE", "CONTAINER_NAME", "WORKFLOW_REFERENCE.CONTAINER_NAME.CYTOLOGY_BOTTLE", "细胞保存液瓶", "细胞保存液瓶", 30, "病理标本容器名称预置项"),
        new ItemSeed("SCI_WORKFLOW_CONTAINER_NAME_CENTRIFUGE_TUBE", "CONTAINER_NAME", "WORKFLOW_REFERENCE.CONTAINER_NAME.CENTRIFUGE_TUBE", "离心管", "离心管", 40, "病理标本容器名称预置项"),
        new ItemSeed("SCI_WORKFLOW_CONTAINER_NAME_SAMPLE_CUP", "CONTAINER_NAME", "WORKFLOW_REFERENCE.CONTAINER_NAME.SAMPLE_CUP", "无菌采样杯", "无菌采样杯", 50, "病理标本容器名称预置项"),
        new ItemSeed("SCI_WORKFLOW_SPECIMEN_IMAGE_SIZE_3_2_2_1_1_0", "SPECIMEN_IMAGE_SIZE", "WORKFLOW_REFERENCE.SPECIMEN_IMAGE_SIZE.3_2X2_1X1_0CM", "3.2x2.1x1.0cm", "3.2x2.1x1.0cm", 10, "取材标本影像大小预置项"),
        new ItemSeed("SCI_WORKFLOW_SPECIMEN_IMAGE_SIZE_1_5_1_0_0_3", "SPECIMEN_IMAGE_SIZE", "WORKFLOW_REFERENCE.SPECIMEN_IMAGE_SIZE.1_5X1_0X0_3CM", "1.5x1.0x0.3cm", "1.5x1.0x0.3cm", 20, "取材标本影像大小预置项"),
        new ItemSeed("SCI_WORKFLOW_CUT_SURFACE_FEATURE_GRAY_WHITE", "CUT_SURFACE_FEATURE", "WORKFLOW_REFERENCE.CUT_SURFACE_FEATURE.GRAY_WHITE", "灰白", "灰白", 10, "取材切面特征预置项"),
        new ItemSeed("SCI_WORKFLOW_CUT_SURFACE_FEATURE_FIRM", "CUT_SURFACE_FEATURE", "WORKFLOW_REFERENCE.CUT_SURFACE_FEATURE.FIRM", "质硬", "质硬", 20, "取材切面特征预置项"),
        new ItemSeed("SCI_WORKFLOW_CUT_SURFACE_FEATURE_NECROSIS", "CUT_SURFACE_FEATURE", "WORKFLOW_REFERENCE.CUT_SURFACE_FEATURE.NECROSIS", "坏死", "坏死", 30, "取材切面特征预置项"),
        new ItemSeed("SCI_WORKFLOW_MARGIN_MARKING_UPPER", "MARGIN_MARKING", "WORKFLOW_REFERENCE.MARGIN_MARKING.UPPER", "上缘墨染", "上缘墨染", 10, "取材切缘标记预置项"),
        new ItemSeed("SCI_WORKFLOW_MARGIN_MARKING_LOWER", "MARGIN_MARKING", "WORKFLOW_REFERENCE.MARGIN_MARKING.LOWER", "下缘墨染", "下缘墨染", 20, "取材切缘标记预置项"),
        new ItemSeed("SCI_WORKFLOW_MARGIN_MARKING_BASE", "MARGIN_MARKING", "WORKFLOW_REFERENCE.MARGIN_MARKING.BASE", "基底部墨染", "基底部墨染", 30, "取材切缘标记预置项")
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "SYSTEM_CONFIG_CATEGORIES")
            || !tableExists(connection, "SYSTEM_CONFIG_ITEMS")) {
            return;
        }

        String rootCategoryId = ensureRootCategory(connection);
        for (CategorySeed categorySeed : CATEGORY_SEEDS) {
            ensureCategory(connection, categorySeed, rootCategoryId);
        }
        for (ItemSeed itemSeed : ITEM_SEEDS) {
            String categoryId = findCategoryIdByCode(connection, itemSeed.categoryCode());
            if (categoryId != null) {
                ensureItem(connection, itemSeed, categoryId);
            }
        }

        if (tableExists(connection, "PERMISSIONS")) {
            String permissionId = ensurePermission(connection);
            if (permissionId != null && tableExists(connection, "ROLE_PERMISSIONS")) {
                for (String roleId : ROLE_IDS) {
                    ensureRolePermission(connection, roleId, permissionId);
                }
            }
        }
    }

    private String ensureRootCategory(Connection connection) throws SQLException {
        String parentId = findCategoryIdByCode(connection, "ROOT");
        String existingId = findCategoryIdByCode(connection, ROOT_CATEGORY_CODE);
        if (existingId == null) {
            try (PreparedStatement insert = connection.prepareStatement("""
                insert into system_config_categories
                    (id, parent_id, category_code, category_name, category_type, sort_order,
                     enabled, created_at, updated_at)
                values
                    (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
                Timestamp now = now();
                insert.setString(1, ROOT_CATEGORY_ID);
                insert.setString(2, parentId);
                insert.setString(3, ROOT_CATEGORY_CODE);
                insert.setString(4, "工作流参考字典");
                insert.setString(5, CATEGORY_TYPE);
                insert.setInt(6, 100);
                insert.setInt(7, 1);
                insert.setTimestamp(8, now);
                insert.setTimestamp(9, now);
                insert.executeUpdate();
            }
            return ROOT_CATEGORY_ID;
        }

        try (PreparedStatement update = connection.prepareStatement("""
            update system_config_categories
            set parent_id = ?,
                category_name = ?,
                category_type = ?,
                sort_order = ?,
                enabled = 1,
                updated_at = ?
            where id = ?
            """)) {
            update.setString(1, parentId);
            update.setString(2, "工作流参考字典");
            update.setString(3, CATEGORY_TYPE);
            update.setInt(4, 100);
            update.setTimestamp(5, now());
            update.setString(6, existingId);
            update.executeUpdate();
        }
        return existingId;
    }

    private void ensureCategory(Connection connection, CategorySeed categorySeed, String rootCategoryId) throws SQLException {
        String existingId = findCategoryIdByCode(connection, categorySeed.code());
        if (existingId == null) {
            try (PreparedStatement insert = connection.prepareStatement("""
                insert into system_config_categories
                    (id, parent_id, category_code, category_name, category_type, sort_order,
                     enabled, created_at, updated_at)
                values
                    (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
                Timestamp now = now();
                insert.setString(1, categorySeed.id());
                insert.setString(2, rootCategoryId);
                insert.setString(3, categorySeed.code());
                insert.setString(4, categorySeed.name());
                insert.setString(5, CATEGORY_TYPE);
                insert.setInt(6, categorySeed.sortOrder());
                insert.setInt(7, 1);
                insert.setTimestamp(8, now);
                insert.setTimestamp(9, now);
                insert.executeUpdate();
            }
            return;
        }

        try (PreparedStatement update = connection.prepareStatement("""
            update system_config_categories
            set parent_id = ?,
                category_name = ?,
                category_type = ?,
                sort_order = ?,
                enabled = 1,
                updated_at = ?
            where id = ?
            """)) {
            update.setString(1, rootCategoryId);
            update.setString(2, categorySeed.name());
            update.setString(3, CATEGORY_TYPE);
            update.setInt(4, categorySeed.sortOrder());
            update.setTimestamp(5, now());
            update.setString(6, existingId);
            update.executeUpdate();
        }
    }

    private void ensureItem(Connection connection, ItemSeed itemSeed, String categoryId) throws SQLException {
        String existingId = findConfigItemId(connection, itemSeed.configKey());
        if (existingId == null) {
            try (PreparedStatement insert = connection.prepareStatement("""
                insert into system_config_items
                    (id, category_id, config_key, config_name, config_value, value_type, sort_order,
                     enabled, remarks, created_at, updated_at)
                values
                    (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?)
                """)) {
                Timestamp now = now();
                insert.setString(1, itemSeed.id());
                insert.setString(2, categoryId);
                insert.setString(3, itemSeed.configKey());
                insert.setString(4, itemSeed.configName());
                insert.setString(5, itemSeed.configValue());
                insert.setString(6, ITEM_VALUE_TYPE);
                insert.setInt(7, itemSeed.sortOrder());
                insert.setString(8, itemSeed.remarks());
                insert.setTimestamp(9, now);
                insert.setTimestamp(10, now);
                insert.executeUpdate();
            }
            return;
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
            update.setString(2, itemSeed.configName());
            update.setString(3, itemSeed.configValue());
            update.setString(4, ITEM_VALUE_TYPE);
            update.setInt(5, itemSeed.sortOrder());
            update.setString(6, itemSeed.remarks());
            update.setTimestamp(7, now());
            update.setString(8, existingId);
            update.executeUpdate();
        }
    }

    private String ensurePermission(Connection connection) throws SQLException {
        String menuId = menuExists(connection, MENU_ID) ? MENU_ID : LEGACY_MENU_ID;
        if (menuId == null || !menuExists(connection, menuId)) {
            return null;
        }

        String permissionId = findPermissionId(connection, PERMISSION_ID);
        if (permissionId == null) {
            try (PreparedStatement insert = connection.prepareStatement("""
                insert into permissions
                    (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path,
                     permission_group, sort_order, enabled, created_at, updated_at)
                values
                    (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
                """)) {
                Timestamp now = now();
                insert.setString(1, PERMISSION_ID);
                insert.setString(2, PERMISSION_ID);
                insert.setString(3, "查询工作流参考字典");
                insert.setString(4, menuId);
                insert.setString(5, ACTION_KEY);
                insert.setString(6, "GET");
                insert.setString(7, "/api/v1/workflow-reference-options");
                insert.setString(8, "WORKFLOW");
                insert.setInt(9, 121);
                insert.setTimestamp(10, now);
                insert.setTimestamp(11, now);
                insert.executeUpdate();
            }
            return PERMISSION_ID;
        }

        try (PreparedStatement update = connection.prepareStatement("""
            update permissions
            set permission_code = ?,
                permission_name = ?,
                menu_id = ?,
                action_key = ?,
                http_method = ?,
                resource_path = ?,
                permission_group = ?,
                sort_order = ?,
                enabled = 1,
                updated_at = ?
            where id = ? or permission_code = ?
            """)) {
            update.setString(1, PERMISSION_ID);
            update.setString(2, "查询工作流参考字典");
            update.setString(3, menuId);
            update.setString(4, ACTION_KEY);
            update.setString(5, "GET");
            update.setString(6, "/api/v1/workflow-reference-options");
            update.setString(7, "WORKFLOW");
            update.setInt(8, 121);
            update.setTimestamp(9, now());
            update.setString(10, permissionId);
            update.setString(11, PERMISSION_ID);
            update.executeUpdate();
        }
        return permissionId;
    }

    private void ensureRolePermission(Connection connection, String roleId, String permissionId) throws SQLException {
        if (!exists(connection, "select 1 from roles where id = ?", roleId)) {
            return;
        }
        if (exists(connection, "select 1 from role_permissions where role_id = ? and permission_id = ?", roleId, permissionId)) {
            return;
        }

        try (PreparedStatement insert = connection.prepareStatement("""
            insert into role_permissions (id, role_id, permission_id, assigned_at)
            values (?, ?, ?, ?)
            """)) {
            insert.setString(1, buildRolePermissionId(roleId));
            insert.setString(2, roleId);
            insert.setString(3, permissionId);
            insert.setTimestamp(4, now());
            insert.executeUpdate();
        }
    }

    private String buildRolePermissionId(String roleId) {
        String sanitizedRoleId = roleId.replaceAll("[^A-Z0-9_]", "_");
        String base = "RP_WF_REF_RECON_" + sanitizedRoleId;
        return base.length() <= 64 ? base : "RP_WF_REF_" + UUID.randomUUID().toString().replace("-", "");
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

    private String findPermissionId(Connection connection, String permissionCode) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("""
            select id
            from permissions
            where id = ? or permission_code = ?
            """)) {
            query.setString(1, permissionCode);
            query.setString(2, permissionCode);
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    private boolean menuExists(Connection connection, String menuId) throws SQLException {
        if (menuId == null || !tableExists(connection, "MENUS")) {
            return false;
        }
        return exists(connection, "select 1 from menus where id = ?", menuId);
    }

    private boolean exists(Connection connection, String sql, String... values) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) {
                query.setString(index + 1, values[index]);
            }
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(tableName);
        candidates.add(tableName.toUpperCase());
        candidates.add(tableName.toLowerCase());
        for (String candidate : candidates) {
            try (ResultSet resultSet = metaData.getTables(null, null, candidate, new String[]{"TABLE"})) {
                while (resultSet.next()) {
                    if (tableName.equalsIgnoreCase(resultSet.getString("TABLE_NAME"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now());
    }

    private record CategorySeed(String id, String code, String name, int sortOrder) {
    }

    private record ItemSeed(
        String id,
        String categoryCode,
        String configKey,
        String configName,
        String configValue,
        int sortOrder,
        String remarks
    ) {
    }
}
