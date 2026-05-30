package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static db.migration.V44WorkflowReferenceSeedData.ACTION_KEY;
import static db.migration.V44WorkflowReferenceSeedData.LEGACY_MENU_ID;
import static db.migration.V44WorkflowReferenceSeedData.MENU_ID;
import static db.migration.V44WorkflowReferenceSeedData.PERMISSION_ID;

final class V44WorkflowReferenceMetadataSupport {

    private final Connection connection;

    V44WorkflowReferenceMetadataSupport(Connection connection) {
        this.connection = connection;
    }

    void ensurePermission() throws SQLException {
        String menuId = menuExists(MENU_ID) ? MENU_ID : LEGACY_MENU_ID;
        if (findPermissionId(PERMISSION_ID) != null) {
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
                bindPermissionUpdate(update, menuId);
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
            bindPermissionInsert(insert, menuId);
            insert.executeUpdate();
        }
    }

    String ensureCategory(
        String preferredId,
        String categoryCode,
        String parentId,
        String categoryName,
        String categoryType,
        int sortOrder,
        boolean enabled
    ) throws SQLException {
        String existingId = findCategoryIdByCode(categoryCode);
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

    void ensureItems(String categoryId, Iterable<ConfigItemSeed> items) throws SQLException {
        for (ConfigItemSeed item : items) {
            String existingId = findConfigItemId(item.configKey());
            if (existingId == null) {
                try (PreparedStatement insert = connection.prepareStatement("""
                    insert into system_config_items
                        (id, category_id, config_key, config_name, config_value, value_type, sort_order,
                         enabled, remarks, created_at, updated_at)
                    values
                        (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?)
                    """)) {
                    bindItemInsert(insert, categoryId, item);
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
                bindItemUpdate(update, categoryId, item, existingId);
                update.executeUpdate();
            }
        }
    }

    void ensureRolePermission(String roleId, String permissionId) throws SQLException {
        if (!exists("select 1 from roles where id = ?", roleId)) {
            return;
        }
        if (!exists("select 1 from permissions where id = ?", permissionId)) {
            return;
        }
        if (exists("select 1 from role_permissions where role_id = ? and permission_id = ?", roleId, permissionId)) {
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

    private void bindPermissionInsert(PreparedStatement statement, String menuId) throws SQLException {
        statement.setString(1, PERMISSION_ID);
        statement.setString(2, PERMISSION_ID);
        statement.setString(3, "Query workflow reference options");
        statement.setString(4, menuId);
        statement.setString(5, ACTION_KEY);
        statement.setString(6, "GET");
        statement.setString(7, "/api/v1/workflow-reference-options");
        statement.setString(8, "WORKFLOW");
        statement.setInt(9, 121);
        statement.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
        statement.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
    }

    private void bindPermissionUpdate(PreparedStatement statement, String menuId) throws SQLException {
        statement.setString(1, PERMISSION_ID);
        statement.setString(2, "Query workflow reference options");
        statement.setString(3, menuId);
        statement.setString(4, ACTION_KEY);
        statement.setString(5, "GET");
        statement.setString(6, "/api/v1/workflow-reference-options");
        statement.setString(7, "WORKFLOW");
        statement.setInt(8, 121);
        statement.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
        statement.setString(10, PERMISSION_ID);
        statement.setString(11, PERMISSION_ID);
    }

    private void bindItemInsert(PreparedStatement statement, String categoryId, ConfigItemSeed item) throws SQLException {
        statement.setString(1, item.id());
        statement.setString(2, categoryId);
        statement.setString(3, item.configKey());
        statement.setString(4, item.configName());
        statement.setString(5, item.configValue());
        statement.setString(6, "STRING");
        statement.setInt(7, item.sortOrder());
        statement.setString(8, "Workflow reference seed item");
        statement.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
        statement.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
    }

    private void bindItemUpdate(PreparedStatement statement, String categoryId, ConfigItemSeed item, String existingId)
        throws SQLException {
        statement.setString(1, categoryId);
        statement.setString(2, item.configName());
        statement.setString(3, item.configValue());
        statement.setString(4, "STRING");
        statement.setInt(5, item.sortOrder());
        statement.setString(6, "Workflow reference seed item");
        statement.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
        statement.setString(8, existingId);
    }

    private String findPermissionId(String permissionCode) throws SQLException {
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

    private String findCategoryIdByCode(String categoryCode) throws SQLException {
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

    private String findConfigItemId(String configKey) throws SQLException {
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

    private boolean menuExists(String menuId) throws SQLException {
        return exists("select 1 from menus where id = ?", menuId);
    }

    private boolean exists(String sql, String... values) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) {
                query.setString(index + 1, values[index]);
            }
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
