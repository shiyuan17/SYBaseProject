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

public class V84__add_system_log_management extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String menuId = ensureLogMenu(connection);
        ensurePermission(connection, "PERM_SYS_LOG_QUERY", "查询日志管理", menuId, "QUERY",
            "GET", "/api/v1/system/logs/login,/api/v1/system/logs/operations", "SYSTEM", 110);
        ensurePermission(connection, "PERM_SYS_LOG_DETAIL", "查看日志详情", menuId, "DETAIL",
            "GET", "/api/v1/system/logs/login/{id},/api/v1/system/logs/operations/{id}", "SYSTEM", 111);
        grantRoleMenu(connection, "ROLE_PATHOLOGY_ADMIN", menuId, "RM_ADMIN_SYS_LOG_MANAGEMENT");
        grantRolePermission(connection, "ROLE_PATHOLOGY_ADMIN", "PERM_SYS_LOG_QUERY", "RP_ADMIN_SYS_LOG_QUERY");
        grantRolePermission(connection, "ROLE_PATHOLOGY_ADMIN", "PERM_SYS_LOG_DETAIL", "RP_ADMIN_SYS_LOG_DETAIL");

        ensureIndex(connection, "USER_LOGIN_LOGS", "idx_user_login_logs_login_at_result_ip",
            "CREATE INDEX idx_user_login_logs_login_at_result_ip ON user_login_logs (login_at, login_result, client_ip)");
        ensureIndex(connection, "USER_LOGIN_LOGS", "idx_user_login_logs_login_name",
            "CREATE INDEX idx_user_login_logs_login_name ON user_login_logs (login_name)");
        ensureIndex(connection, "OPERATION_LOGS", "idx_operation_logs_at_module_result",
            "CREATE INDEX idx_operation_logs_at_module_result ON operation_logs (operation_at, module_code, operation_result)");
        ensureIndex(connection, "OPERATION_LOGS", "idx_operation_logs_business",
            "CREATE INDEX idx_operation_logs_business ON operation_logs (business_type, business_id)");
        ensureIndex(connection, "OPERATION_LOGS", "idx_operation_logs_operator",
            "CREATE INDEX idx_operation_logs_operator ON operation_logs (operator_user_id, operation_at)");
    }

    private String ensureLogMenu(Connection connection) throws SQLException {
        String parentId = menuExists(connection, "MENU_SYSTEM") ? "MENU_SYSTEM" : "MENU_SYS_ROOT";
        if (menuExists(connection, "MENU_SYS_LOG_MANAGEMENT")) {
            try (PreparedStatement update = connection.prepareStatement("""
                update menus
                set parent_id = ?, menu_code = ?, menu_name = ?, menu_type = 'MENU',
                    path = ?, component_name = ?, icon = ?, permission_prefix = ?,
                    sort_order = ?, visible = 1, enabled = 1, updated_at = ?
                where id = ?
                """)) {
                update.setString(1, parentId);
                update.setString(2, "SYS_LOG_MANAGEMENT");
                update.setString(3, "日志管理");
                update.setString(4, "/system/logs");
                update.setString(5, "LogManagement");
                update.setString(6, "carbon:document-audit");
                update.setString(7, "sys:log");
                update.setInt(8, 110);
                update.setTimestamp(9, now());
                update.setString(10, "MENU_SYS_LOG_MANAGEMENT");
                update.executeUpdate();
            }
            return "MENU_SYS_LOG_MANAGEMENT";
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into menus
                (id, parent_id, menu_code, menu_name, menu_type, path, component_name, icon,
                 permission_prefix, sort_order, visible, enabled, created_at, updated_at)
            values
                (?, ?, ?, ?, 'MENU', ?, ?, ?, ?, ?, 1, 1, ?, ?)
            """)) {
            insert.setString(1, "MENU_SYS_LOG_MANAGEMENT");
            insert.setString(2, parentId);
            insert.setString(3, "SYS_LOG_MANAGEMENT");
            insert.setString(4, "日志管理");
            insert.setString(5, "/system/logs");
            insert.setString(6, "LogManagement");
            insert.setString(7, "carbon:document-audit");
            insert.setString(8, "sys:log");
            insert.setInt(9, 110);
            insert.setTimestamp(10, now());
            insert.setTimestamp(11, now());
            insert.executeUpdate();
        }
        return "MENU_SYS_LOG_MANAGEMENT";
    }

    private void ensurePermission(Connection connection,
                                  String id,
                                  String name,
                                  String menuId,
                                  String actionKey,
                                  String httpMethod,
                                  String resourcePath,
                                  String permissionGroup,
                                  int sortOrder) throws SQLException {
        if (permissionExists(connection, id)) {
            try (PreparedStatement update = connection.prepareStatement("""
                update permissions
                set permission_code = ?, permission_name = ?, menu_id = ?, action_key = ?, http_method = ?,
                    resource_path = ?, permission_group = ?, sort_order = ?, enabled = 1, updated_at = ?
                where id = ?
                """)) {
                update.setString(1, id);
                update.setString(2, name);
                update.setString(3, menuId);
                update.setString(4, actionKey);
                update.setString(5, httpMethod);
                update.setString(6, resourcePath);
                update.setString(7, permissionGroup);
                update.setInt(8, sortOrder);
                update.setTimestamp(9, now());
                update.setString(10, id);
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
            insert.setString(1, id);
            insert.setString(2, id);
            insert.setString(3, name);
            insert.setString(4, menuId);
            insert.setString(5, actionKey);
            insert.setString(6, httpMethod);
            insert.setString(7, resourcePath);
            insert.setString(8, permissionGroup);
            insert.setInt(9, sortOrder);
            insert.setTimestamp(10, now());
            insert.setTimestamp(11, now());
            insert.executeUpdate();
        }
    }

    private void grantRoleMenu(Connection connection, String roleId, String menuId, String id) throws SQLException {
        if (roleMenuExists(connection, roleId, menuId)) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into role_menus (id, role_id, menu_id, assigned_at)
            values (?, ?, ?, ?)
            """)) {
            insert.setString(1, id);
            insert.setString(2, roleId);
            insert.setString(3, menuId);
            insert.setTimestamp(4, now());
            insert.executeUpdate();
        }
    }

    private void grantRolePermission(Connection connection, String roleId, String permissionId, String id) throws SQLException {
        if (rolePermissionExists(connection, roleId, permissionId)) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into role_permissions (id, role_id, permission_id, assigned_at)
            values (?, ?, ?, ?)
            """)) {
            insert.setString(1, id);
            insert.setString(2, roleId);
            insert.setString(3, permissionId);
            insert.setTimestamp(4, now());
            insert.executeUpdate();
        }
    }

    private boolean menuExists(Connection connection, String id) throws SQLException {
        return exists(connection, "select 1 from menus where id = ?", id);
    }

    private boolean permissionExists(Connection connection, String id) throws SQLException {
        return exists(connection, "select 1 from permissions where id = ?", id);
    }

    private boolean roleMenuExists(Connection connection, String roleId, String menuId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
            "select 1 from role_menus where role_id = ? and menu_id = ?")) {
            query.setString(1, roleId);
            query.setString(2, menuId);
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean rolePermissionExists(Connection connection, String roleId, String permissionId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
            "select 1 from role_permissions where role_id = ? and permission_id = ?")) {
            query.setString(1, roleId);
            query.setString(2, permissionId);
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean exists(Connection connection, String sql, String id) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setString(1, id);
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void ensureIndex(Connection connection, String tableName, String indexName, String ddl) throws SQLException {
        if (!tableExists(connection, tableName) || indexExists(connection, tableName, indexName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
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

    private boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        for (String candidate : identifierCandidates(tableName)) {
            try (ResultSet resultSet = metaData.getIndexInfo(null, null, candidate, false, false)) {
                while (resultSet.next()) {
                    if (identifierEquals(resultSet.getString("TABLE_NAME"), tableName)
                        && identifierEquals(resultSet.getString("INDEX_NAME"), indexName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<String> identifierCandidates(String identifier) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(identifier);
        candidates.add(identifier.toUpperCase());
        candidates.add(identifier.toLowerCase());
        return new ArrayList<>(candidates);
    }

    private boolean identifierEquals(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now());
    }
}
