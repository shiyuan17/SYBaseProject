package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;

import static db.migration.V11LegacyDmSchemaSeedData.*;

final class V11LegacyDmSchemaSupport {

    private final V11LegacyDmSchemaMetadataSupport metadataSupport;

    V11LegacyDmSchemaSupport(Connection connection) {
        this.metadataSupport = new V11LegacyDmSchemaMetadataSupport(connection);
    }

    void migrate() throws Exception {
        metadataSupport.ensureUserLoginSchema();
        metadataSupport.ensureTechnicalPendingTasks();
        reconcileWorkflowAuthorization();
        reconcileUsers();
        reconcileRoleMenus();
    }

    private void reconcileWorkflowAuthorization() throws SQLException {
        for (MenuSeed menu : WORKFLOW_MENUS) {
            upsertMenu(menu);
        }
        for (PermissionSeed permission : WORKFLOW_PERMISSIONS) {
            upsertPermission(permission);
        }
        for (RoleSeed role : WORKFLOW_ROLES) {
            upsertRole(role);
        }

        deleteRolePermissions();
        for (RolePermissionSeed seed : WORKFLOW_ROLE_PERMISSIONS) {
            insertRolePermission(seed);
        }
    }

    private void reconcileUsers() throws SQLException {
        for (UserSeed user : STANDARD_USERS) {
            upsertUser(user);
        }

        try (PreparedStatement statement = metadataSupportConnection().prepareStatement(
            "DELETE FROM user_roles WHERE user_id = ?")) {
            for (UserSeed user : STANDARD_USERS) {
                statement.setString(1, user.id());
                statement.addBatch();
            }
            statement.executeBatch();
        }
        for (UserRoleSeed seed : STANDARD_USER_ROLES) {
            insertUserRole(seed);
        }
    }

    private void reconcileRoleMenus() throws SQLException {
        LinkedHashSet<String> reconciledRoleIds = new LinkedHashSet<>();
        reconciledRoleIds.add("ROLE_PATHOLOGY_ADMIN");
        for (RoleSeed role : WORKFLOW_ROLES) {
            reconciledRoleIds.add(role.id());
        }

        try (PreparedStatement statement = metadataSupportConnection().prepareStatement(
            "DELETE FROM role_menus WHERE role_id = ?")) {
            for (String roleId : reconciledRoleIds) {
                statement.setString(1, roleId);
                statement.addBatch();
            }
            statement.executeBatch();
        }

        for (MenuIdentity menu : loadAdminMenus()) {
            insertRoleMenu("RM_ADMIN_" + menu.menuCode(), "ROLE_PATHOLOGY_ADMIN", menu.id());
        }

        for (RoleSeed role : WORKFLOW_ROLES) {
            String rootMenuId = role.roleCode().startsWith("M2_") ? "MENU_M2_WORKFLOW" : "MENU_M3_WORKFLOW";
            LinkedHashSet<MenuIdentity> menus = new LinkedHashSet<>(loadMenusForRole(role.id()));
            menus.add(loadMenuIdentity(rootMenuId));
            for (MenuIdentity menu : menus) {
                insertRoleMenu("RM_" + role.roleCode() + "_" + menu.menuCode(), role.id(), menu.id());
            }
        }
    }

    private List<MenuIdentity> loadAdminMenus() throws SQLException {
        return queryMenus("""
            select id, menu_code
            from menus
            where id in ('MENU_SYSTEM', 'MENU_M2_WORKFLOW', 'MENU_M3_WORKFLOW')
               or parent_id in ('MENU_SYSTEM', 'MENU_M2_WORKFLOW', 'MENU_M3_WORKFLOW')
            order by sort_order, menu_code
            """);
    }

    private List<MenuIdentity> loadMenusForRole(String roleId) throws SQLException {
        try (PreparedStatement statement = metadataSupportConnection().prepareStatement("""
            select distinct menus.id, menus.menu_code
            from role_permissions
            join permissions on permissions.id = role_permissions.permission_id
            join menus on menus.id = permissions.menu_id
            where role_permissions.role_id = ?
            order by menus.menu_code
            """)) {
            statement.setString(1, roleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return readMenus(resultSet);
            }
        }
    }

    private MenuIdentity loadMenuIdentity(String menuId) throws SQLException {
        try (PreparedStatement statement = metadataSupportConnection().prepareStatement(
            "SELECT id, menu_code FROM menus WHERE id = ?")) {
            statement.setString(1, menuId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Missing menu required for reconciliation: " + menuId);
                }
                return new MenuIdentity(resultSet.getString("id"), resultSet.getString("menu_code"));
            }
        }
    }

    private List<MenuIdentity> queryMenus(String sql) throws SQLException {
        try (PreparedStatement statement = metadataSupportConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return readMenus(resultSet);
        }
    }

    private List<MenuIdentity> readMenus(ResultSet resultSet) throws SQLException {
        List<MenuIdentity> result = new java.util.ArrayList<>();
        while (resultSet.next()) {
            result.add(new MenuIdentity(resultSet.getString("id"), resultSet.getString("menu_code")));
        }
        return result;
    }

    private void upsertMenu(MenuSeed seed) throws SQLException {
        try (PreparedStatement update = metadataSupportConnection().prepareStatement("""
            UPDATE menus
            SET parent_id = ?,
                menu_code = ?,
                menu_name = ?,
                menu_type = ?,
                path = ?,
                component_name = ?,
                permission_prefix = ?,
                sort_order = ?,
                visible = 1,
                enabled = 1,
                updated_at = ?
            WHERE id = ?
            """)) {
            bindMenu(update, seed, false);
            if (update.executeUpdate() > 0) {
                return;
            }
        }

        try (PreparedStatement insert = metadataSupportConnection().prepareStatement("""
            INSERT INTO menus
                (id, parent_id, menu_code, menu_name, menu_type, path, component_name, permission_prefix,
                 sort_order, visible, enabled, created_at, updated_at)
            VALUES
                (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, ?, ?)
            """)) {
            bindMenu(insert, seed, true);
            insert.executeUpdate();
        }
    }

    private void bindMenu(PreparedStatement statement, MenuSeed seed, boolean includeIdFirst) throws SQLException {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        int index = 1;
        if (includeIdFirst) {
            statement.setString(index++, seed.id());
        }
        if (seed.parentId() == null) {
            statement.setNull(index++, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index++, seed.parentId());
        }
        statement.setString(index++, seed.menuCode());
        statement.setString(index++, seed.menuName());
        statement.setString(index++, seed.menuType());
        statement.setString(index++, seed.path());
        statement.setString(index++, seed.componentName());
        statement.setString(index++, seed.permissionPrefix());
        statement.setInt(index++, seed.sortOrder());
        statement.setTimestamp(index++, java.sql.Timestamp.valueOf(now));
        if (includeIdFirst) {
            statement.setTimestamp(index++, java.sql.Timestamp.valueOf(now));
        } else {
            statement.setString(index, seed.id());
        }
    }

    private void upsertPermission(PermissionSeed seed) throws SQLException {
        try (PreparedStatement update = metadataSupportConnection().prepareStatement("""
            UPDATE permissions
            SET permission_code = ?,
                permission_name = ?,
                menu_id = ?,
                action_key = ?,
                http_method = ?,
                resource_path = ?,
                permission_group = ?,
                sort_order = ?,
                enabled = 1,
                updated_at = ?
            WHERE id = ?
            """)) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            update.setString(1, seed.permissionCode());
            update.setString(2, seed.permissionName());
            update.setString(3, seed.menuId());
            update.setString(4, seed.actionKey());
            update.setString(5, seed.httpMethod());
            update.setString(6, seed.resourcePath());
            update.setString(7, seed.permissionGroup());
            update.setInt(8, seed.sortOrder());
            update.setTimestamp(9, java.sql.Timestamp.valueOf(now));
            update.setString(10, seed.id());
            if (update.executeUpdate() > 0) {
                return;
            }
        }

        try (PreparedStatement insert = metadataSupportConnection().prepareStatement("""
            INSERT INTO permissions
                (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path,
                 permission_group, sort_order, enabled, created_at, updated_at)
            VALUES
                (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
            """)) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            insert.setString(1, seed.id());
            insert.setString(2, seed.permissionCode());
            insert.setString(3, seed.permissionName());
            insert.setString(4, seed.menuId());
            insert.setString(5, seed.actionKey());
            insert.setString(6, seed.httpMethod());
            insert.setString(7, seed.resourcePath());
            insert.setString(8, seed.permissionGroup());
            insert.setInt(9, seed.sortOrder());
            insert.setTimestamp(10, java.sql.Timestamp.valueOf(now));
            insert.setTimestamp(11, java.sql.Timestamp.valueOf(now));
            insert.executeUpdate();
        }
    }

    private void upsertRole(RoleSeed seed) throws SQLException {
        try (PreparedStatement update = metadataSupportConnection().prepareStatement("""
            UPDATE roles
            SET role_code = ?,
                role_name = ?,
                role_type = ?,
                data_scope = ?,
                remarks = ?,
                enabled = 1,
                updated_at = ?
            WHERE id = ?
            """)) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            update.setString(1, seed.roleCode());
            update.setString(2, seed.roleName());
            update.setString(3, seed.roleType());
            update.setString(4, seed.dataScope());
            update.setString(5, seed.remarks());
            update.setTimestamp(6, java.sql.Timestamp.valueOf(now));
            update.setString(7, seed.id());
            if (update.executeUpdate() > 0) {
                return;
            }
        }

        try (PreparedStatement insert = metadataSupportConnection().prepareStatement("""
            INSERT INTO roles
                (id, role_code, role_name, role_type, data_scope, remarks, enabled, created_at, updated_at)
            VALUES
                (?, ?, ?, ?, ?, ?, 1, ?, ?)
            """)) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            insert.setString(1, seed.id());
            insert.setString(2, seed.roleCode());
            insert.setString(3, seed.roleName());
            insert.setString(4, seed.roleType());
            insert.setString(5, seed.dataScope());
            insert.setString(6, seed.remarks());
            insert.setTimestamp(7, java.sql.Timestamp.valueOf(now));
            insert.setTimestamp(8, java.sql.Timestamp.valueOf(now));
            insert.executeUpdate();
        }
    }

    private void deleteRolePermissions() throws SQLException {
        try (PreparedStatement byPermission = metadataSupportConnection().prepareStatement(
            "DELETE FROM role_permissions WHERE permission_id = ?")) {
            for (PermissionSeed permission : WORKFLOW_PERMISSIONS) {
                byPermission.setString(1, permission.id());
                byPermission.addBatch();
            }
            byPermission.executeBatch();
        }
        try (PreparedStatement byId = metadataSupportConnection().prepareStatement(
            "DELETE FROM role_permissions WHERE id = ?")) {
            for (RolePermissionSeed seed : WORKFLOW_ROLE_PERMISSIONS) {
                byId.setString(1, seed.id());
                byId.addBatch();
            }
            byId.executeBatch();
        }
    }

    private void insertRolePermission(RolePermissionSeed seed) throws SQLException {
        try (PreparedStatement insert = metadataSupportConnection().prepareStatement("""
            INSERT INTO role_permissions
                (id, role_id, permission_id, assigned_at)
            VALUES
                (?, ?, ?, ?)
            """)) {
            insert.setString(1, seed.id());
            insert.setString(2, seed.roleId());
            insert.setString(3, seed.permissionId());
            insert.setTimestamp(4, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private void upsertUser(UserSeed seed) throws SQLException {
        try (PreparedStatement update = metadataSupportConnection().prepareStatement("""
            UPDATE users
            SET user_code = ?,
                login_name = ?,
                name = ?,
                password = ?,
                password_algo = ?,
                password_salt = ?,
                role = ?,
                enabled = ?,
                updated_at = ?
            WHERE id = ?
            """)) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            update.setString(1, seed.userCode());
            update.setString(2, seed.loginName());
            update.setString(3, seed.name());
            update.setString(4, DEFAULT_PASSWORD);
            update.setString(5, DEFAULT_PASSWORD_ALGO);
            update.setString(6, DEFAULT_PASSWORD_SALT);
            if (seed.roleCode() == null) {
                update.setNull(7, java.sql.Types.VARCHAR);
            } else {
                update.setString(7, seed.roleCode());
            }
            update.setInt(8, seed.enabled() ? 1 : 0);
            update.setTimestamp(9, java.sql.Timestamp.valueOf(now));
            update.setString(10, seed.id());
            if (update.executeUpdate() > 0) {
                return;
            }
        }

        try (PreparedStatement insert = metadataSupportConnection().prepareStatement("""
            INSERT INTO users
                (id, user_code, login_name, name, password, password_algo, password_salt, role, enabled, created_at, updated_at)
            VALUES
                (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            insert.setString(1, seed.id());
            insert.setString(2, seed.userCode());
            insert.setString(3, seed.loginName());
            insert.setString(4, seed.name());
            insert.setString(5, DEFAULT_PASSWORD);
            insert.setString(6, DEFAULT_PASSWORD_ALGO);
            insert.setString(7, DEFAULT_PASSWORD_SALT);
            if (seed.roleCode() == null) {
                insert.setNull(8, java.sql.Types.VARCHAR);
            } else {
                insert.setString(8, seed.roleCode());
            }
            insert.setInt(9, seed.enabled() ? 1 : 0);
            insert.setTimestamp(10, java.sql.Timestamp.valueOf(now));
            insert.setTimestamp(11, java.sql.Timestamp.valueOf(now));
            insert.executeUpdate();
        }
    }

    private void insertUserRole(UserRoleSeed seed) throws SQLException {
        try (PreparedStatement insert = metadataSupportConnection().prepareStatement("""
            INSERT INTO user_roles
                (id, user_id, role_id, is_primary, assigned_at, assigned_by_name)
            VALUES
                (?, ?, ?, 1, ?, 'system')
            """)) {
            insert.setString(1, seed.id());
            insert.setString(2, seed.userId());
            insert.setString(3, seed.roleId());
            insert.setTimestamp(4, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private void insertRoleMenu(String id, String roleId, String menuId) throws SQLException {
        try (PreparedStatement insert = metadataSupportConnection().prepareStatement("""
            INSERT INTO role_menus
                (id, role_id, menu_id, assigned_at)
            VALUES
                (?, ?, ?, ?)
            """)) {
            insert.setString(1, id);
            insert.setString(2, roleId);
            insert.setString(3, menuId);
            insert.setTimestamp(4, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private java.sql.Connection metadataSupportConnection() {
        return metadataSupport.connection();
    }
}
