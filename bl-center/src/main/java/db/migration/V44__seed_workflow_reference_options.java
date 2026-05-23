package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class V44__seed_workflow_reference_options extends BaseJavaMigration {

    private static final String MENU_ID = "MENU_CONFIGS";
    private static final String LEGACY_MENU_ID = "MENU_SYS_CONFIG";
    private static final String PERMISSION_ID = "PERM_WORKFLOW_REFERENCE_QUERY";
    private static final String ACTION_KEY = "WORKFLOW_REFERENCE_QUERY";

    private static final String ROOT_CATEGORY_ID = "SCC_WORKFLOW_REFERENCE";
    private static final String ROOT_CATEGORY_CODE = "WORKFLOW_REFERENCE";

    private static final String SPECIMEN_TYPE_CATEGORY_ID = "SCC_WORKFLOW_REFERENCE_SPECIMEN_TYPE";
    private static final String COLLECTION_MODE_CATEGORY_ID = "SCC_WORKFLOW_REFERENCE_COLLECTION_MODE";
    private static final String CLINICAL_SYMPTOM_CATEGORY_ID = "SCC_WORKFLOW_REFERENCE_CLINICAL_SYMPTOM";
    private static final String FIXATION_LIQUID_TYPE_CATEGORY_ID = "SCC_WORKFLOW_REFERENCE_FIXATION";

    private static final List<String> ROLE_IDS = List.of(
        "ROLE_PATHOLOGY_ADMIN",
        "ROLE_M2_CLINICAL_REGISTER",
        "ROLE_M2_FIXATION_VERIFY",
        "ROLE_M3_GROSSING"
    );

    private static final List<ConfigItemSeed> SPECIMEN_TYPE_ITEMS = List.of(
        new ConfigItemSeed(
            "SCI_WORKFLOW_SPECIMEN_TYPE_ROUTINE",
            "WORKFLOW_REFERENCE.SPECIMEN_TYPE.ROUTINE",
            "常规",
            "ROUTINE",
            10
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_SPECIMEN_TYPE_FROZEN",
            "WORKFLOW_REFERENCE.SPECIMEN_TYPE.FROZEN",
            "冰冻",
            "FROZEN",
            20
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_SPECIMEN_TYPE_BIOPSY",
            "WORKFLOW_REFERENCE.SPECIMEN_TYPE.BIOPSY",
            "活检",
            "BIOPSY",
            30
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_SPECIMEN_TYPE_CYTOLOGY",
            "WORKFLOW_REFERENCE.SPECIMEN_TYPE.CYTOLOGY",
            "细胞学",
            "CYTOLOGY",
            40
        )
    );

    private static final List<ConfigItemSeed> COLLECTION_MODE_ITEMS = List.of(
        new ConfigItemSeed(
            "SCI_WORKFLOW_COLLECTION_MODE_SURGERY",
            "WORKFLOW_REFERENCE.COLLECTION_MODE.SURGERY",
            "手术",
            "SURGERY",
            10
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_COLLECTION_MODE_BIOPSY",
            "WORKFLOW_REFERENCE.COLLECTION_MODE.BIOPSY",
            "活检",
            "BIOPSY",
            20
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_COLLECTION_MODE_PUNCTURE",
            "WORKFLOW_REFERENCE.COLLECTION_MODE.PUNCTURE",
            "穿刺",
            "PUNCTURE",
            30
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_COLLECTION_MODE_CYTOLOGY",
            "WORKFLOW_REFERENCE.COLLECTION_MODE.CYTOLOGY",
            "细胞学",
            "CYTOLOGY",
            40
        )
    );

    private static final List<ConfigItemSeed> CLINICAL_SYMPTOM_ITEMS = List.of(
        new ConfigItemSeed(
            "SCI_WORKFLOW_CLINICAL_SYMPTOM_MASS",
            "WORKFLOW_REFERENCE.CLINICAL_SYMPTOM.MASS",
            "肿物",
            null,
            10
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_CLINICAL_SYMPTOM_PAIN",
            "WORKFLOW_REFERENCE.CLINICAL_SYMPTOM.PAIN",
            "疼痛",
            null,
            20
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_CLINICAL_SYMPTOM_BLEEDING",
            "WORKFLOW_REFERENCE.CLINICAL_SYMPTOM.BLEEDING",
            "出血",
            null,
            30
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_CLINICAL_SYMPTOM_FEVER",
            "WORKFLOW_REFERENCE.CLINICAL_SYMPTOM.FEVER",
            "发热",
            null,
            40
        )
    );

    private static final List<ConfigItemSeed> FIXATION_ITEMS = List.of(
        new ConfigItemSeed(
            "SCI_WORKFLOW_FIXATION_FORMALIN",
            "WORKFLOW_REFERENCE.FIXATION_LIQUID_TYPE.FORMALIN",
            "10% 中性福尔马林",
            "FORMALIN",
            10
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_FIXATION_ETHANOL",
            "WORKFLOW_REFERENCE.FIXATION_LIQUID_TYPE.ETHANOL",
            "酒精",
            "ETHANOL",
            20
        ),
        new ConfigItemSeed(
            "SCI_WORKFLOW_FIXATION_SALINE",
            "WORKFLOW_REFERENCE.FIXATION_LIQUID_TYPE.SALINE",
            "生理盐水",
            "SALINE",
            30
        )
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        ensurePermission(connection);
        for (String roleId : ROLE_IDS) {
            ensureRolePermission(connection, roleId, PERMISSION_ID);
        }

        String rootCategoryId = ensureCategory(
            connection,
            ROOT_CATEGORY_ID,
            ROOT_CATEGORY_CODE,
            null,
            "工作流参考字典",
            "WORKFLOW_REFERENCE",
            100,
            true
        );
        String specimenTypeCategoryId = ensureCategory(
            connection,
            SPECIMEN_TYPE_CATEGORY_ID,
            "SPECIMEN_TYPE",
            rootCategoryId,
            "标本类型",
            "WORKFLOW_REFERENCE",
            110,
            true
        );
        String collectionModeCategoryId = ensureCategory(
            connection,
            COLLECTION_MODE_CATEGORY_ID,
            "COLLECTION_MODE",
            rootCategoryId,
            "采集方式",
            "WORKFLOW_REFERENCE",
            120,
            true
        );
        String clinicalSymptomCategoryId = ensureCategory(
            connection,
            CLINICAL_SYMPTOM_CATEGORY_ID,
            "CLINICAL_SYMPTOM",
            rootCategoryId,
            "临床症状",
            "WORKFLOW_REFERENCE",
            130,
            true
        );
        String fixationCategoryId = ensureCategory(
            connection,
            FIXATION_LIQUID_TYPE_CATEGORY_ID,
            "FIXATION_LIQUID_TYPE",
            rootCategoryId,
            "固定液类型",
            "WORKFLOW_REFERENCE",
            140,
            true
        );

        ensureItems(connection, specimenTypeCategoryId, SPECIMEN_TYPE_ITEMS);
        ensureItems(connection, collectionModeCategoryId, COLLECTION_MODE_ITEMS);
        ensureItems(connection, clinicalSymptomCategoryId, CLINICAL_SYMPTOM_ITEMS);
        ensureItems(connection, fixationCategoryId, FIXATION_ITEMS);
    }

    private void ensurePermission(Connection connection) throws SQLException {
        String menuId = menuExists(connection, MENU_ID) ? MENU_ID : LEGACY_MENU_ID;
        if (findPermissionId(connection, PERMISSION_ID) != null) {
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
                update.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                update.setString(10, PERMISSION_ID);
                update.setString(11, PERMISSION_ID);
                update.executeUpdate();
            }
            return;
        }

        try (PreparedStatement insert = connection.prepareStatement("""
            insert into permissions
                (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path,
                 permission_group, sort_order, enabled, created_at, updated_at)
            values
                (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
            """)) {
            insert.setString(1, PERMISSION_ID);
            insert.setString(2, PERMISSION_ID);
            insert.setString(3, "查询工作流参考字典");
            insert.setString(4, menuId);
            insert.setString(5, ACTION_KEY);
            insert.setString(6, "GET");
            insert.setString(7, "/api/v1/workflow-reference-options");
            insert.setString(8, "WORKFLOW");
            insert.setInt(9, 121);
            insert.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            insert.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
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
                insert.setString(1, preferredId);
                insert.setString(2, parentId);
                insert.setString(3, categoryCode);
                insert.setString(4, categoryName);
                insert.setString(5, categoryType);
                insert.setInt(6, sortOrder);
                insert.setInt(7, enabled ? 1 : 0);
                insert.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                insert.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
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
                    insert.setString(1, item.id());
                    insert.setString(2, categoryId);
                    insert.setString(3, item.configKey());
                    insert.setString(4, item.configName());
                    insert.setString(5, item.configValue());
                    insert.setString(6, "STRING");
                    insert.setInt(7, item.sortOrder());
                    insert.setString(8, "工作流参考字典预置项");
                    insert.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                    insert.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
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
                update.setString(6, "工作流参考字典预置项");
                update.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                update.setString(8, existingId);
                update.executeUpdate();
            }
        }
    }

    private void ensureRolePermission(
        Connection connection,
        String roleId,
        String permissionId
    ) throws SQLException {
        if (!exists(connection, "select 1 from roles where id = ?", roleId)) {
            return;
        }
        if (!exists(connection, "select 1 from permissions where id = ?", permissionId)) {
            return;
        }
        if (exists(
            connection,
            "select 1 from role_permissions where role_id = ? and permission_id = ?",
            roleId,
            permissionId
        )) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into role_permissions (id, role_id, permission_id, assigned_at)
            values (?, ?, ?, ?)
            """)) {
            insert.setString(1, "RP_WF_REF_" + UUID.randomUUID().toString().replace("-", ""));
            insert.setString(2, roleId);
            insert.setString(3, permissionId);
            insert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private String findPermissionId(Connection connection, String permissionCode) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("""
            select id
            from permissions
            where id = ?
               or permission_code = ?
            """)) {
            query.setString(1, permissionCode);
            query.setString(2, permissionCode);
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
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

    private boolean menuExists(Connection connection, String menuId) throws SQLException {
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

    private record ConfigItemSeed(
        String id,
        String configKey,
        String configName,
        String configValue,
        int sortOrder
    ) {
    }
}
