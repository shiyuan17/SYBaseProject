package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class V20__reconcile_m4_batch2_authorization extends BaseJavaMigration {

    private static final String DEFAULT_PASSWORD = "0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134";
    private static final String DEFAULT_PASSWORD_ALGO = "SM3";
    private static final String DEFAULT_PASSWORD_SALT = "9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        upsertMenu(connection, "MENU_M4_REVISION", "MENU_M4_WORKFLOW", "M4_REVISION", "Report Revision", "MENU",
            "/api/v1/report-revision-requests", "ReportRevision", "m4:revision", 135);
        upsertMenu(connection, "MENU_M4_MEDICAL_ORDER", "MENU_M4_WORKFLOW", "M4_MEDICAL_ORDER", "Medical Order", "MENU",
            "/api/v1/medical-orders/pending", "MedicalOrder", "m4:order", 136);
        upsertMenu(connection, "MENU_M4_CONSULTATION", "MENU_M4_WORKFLOW", "M4_CONSULTATION", "Consultation", "MENU",
            "/api/v1/consultations", "Consultation", "m4:consultation", 137);

        upsertPermission(connection, "PERM_M4_REVISION_REQUEST_CREATE", "PERM_M4_REVISION_REQUEST_CREATE", "Revision request create",
            "MENU_M4_REVISION", "CREATE", "POST", "/api/v1/report-revision-requests", "M4", 142);
        upsertPermission(connection, "PERM_M4_REVISION_APPROVE", "PERM_M4_REVISION_APPROVE", "Revision approve",
            "MENU_M4_REVISION", "APPROVE", "POST", "/api/v1/report-revision-requests/{id}/approve", "M4", 143);
        upsertPermission(connection, "PERM_M4_MEDICAL_ORDER_CREATE", "PERM_M4_MEDICAL_ORDER_CREATE", "Medical order create",
            "MENU_M4_MEDICAL_ORDER", "CREATE", "POST", "/api/v1/medical-orders", "M4", 144);
        upsertPermission(connection, "PERM_M4_MEDICAL_ORDER_CANCEL", "PERM_M4_MEDICAL_ORDER_CANCEL", "Medical order cancel",
            "MENU_M4_MEDICAL_ORDER", "CANCEL", "POST", "/api/v1/medical-orders/{id}/cancel", "M4", 145);
        upsertPermission(connection, "PERM_M4_MEDICAL_ORDER_QUERY", "PERM_M4_MEDICAL_ORDER_QUERY", "Medical order query",
            "MENU_M4_MEDICAL_ORDER", "QUERY", "GET", "/api/v1/medical-orders/pending", "M4", 146);
        upsertPermission(connection, "PERM_M4_MEDICAL_ORDER_ACCEPT", "PERM_M4_MEDICAL_ORDER_ACCEPT", "Medical order accept",
            "MENU_M4_MEDICAL_ORDER", "ACCEPT", "POST", "/api/v1/medical-orders/{id}/accept", "M4", 147);
        upsertPermission(connection, "PERM_M4_MEDICAL_ORDER_COMPLETE", "PERM_M4_MEDICAL_ORDER_COMPLETE", "Medical order complete",
            "MENU_M4_MEDICAL_ORDER", "COMPLETE", "POST", "/api/v1/medical-orders/{id}/complete", "M4", 148);
        upsertPermission(connection, "PERM_M4_CONSULTATION_CREATE", "PERM_M4_CONSULTATION_CREATE", "Consultation create",
            "MENU_M4_CONSULTATION", "CREATE", "POST", "/api/v1/consultations", "M4", 149);
        upsertPermission(connection, "PERM_M4_CONSULTATION_COMMENT", "PERM_M4_CONSULTATION_COMMENT", "Consultation comment",
            "MENU_M4_CONSULTATION", "COMMENT", "POST", "/api/v1/consultations/{id}/participants/{participantId}/comment", "M4", 150);
        upsertPermission(connection, "PERM_M4_CONSULTATION_COMPLETE", "PERM_M4_CONSULTATION_COMPLETE", "Consultation complete",
            "MENU_M4_CONSULTATION", "COMPLETE", "POST", "/api/v1/consultations/{id}/complete", "M4", 151);

        upsertRole(connection, "ROLE_M4_MEDICAL_ORDER_EXECUTE", "M4_MEDICAL_ORDER_EXECUTE", "医嘱执行员",
            "BUSINESS", "DEPARTMENT", "M4 medical order execution workstation");

        ensureRolePermissions(connection, "ROLE_PATHOLOGY_ADMIN", List.of(
            "PERM_M4_REVISION_REQUEST_CREATE", "PERM_M4_REVISION_APPROVE",
            "PERM_M4_MEDICAL_ORDER_CREATE", "PERM_M4_MEDICAL_ORDER_CANCEL", "PERM_M4_MEDICAL_ORDER_QUERY",
            "PERM_M4_MEDICAL_ORDER_ACCEPT", "PERM_M4_MEDICAL_ORDER_COMPLETE",
            "PERM_M4_CONSULTATION_CREATE", "PERM_M4_CONSULTATION_COMMENT", "PERM_M4_CONSULTATION_COMPLETE"),
            "RP_M4_BATCH2_ADMIN_");
        ensureRolePermissions(connection, "ROLE_M4_DIAGNOSIS", List.of(
            "PERM_M4_REVISION_REQUEST_CREATE", "PERM_M4_MEDICAL_ORDER_CREATE", "PERM_M4_MEDICAL_ORDER_CANCEL",
            "PERM_M4_CONSULTATION_CREATE", "PERM_M4_CONSULTATION_COMMENT", "PERM_M4_CONSULTATION_COMPLETE"),
            "RP_M4_BATCH2_DIAG_");
        ensureRolePermissions(connection, "ROLE_M4_REVIEW", List.of("PERM_M4_CONSULTATION_COMMENT"), "RP_M4_BATCH2_REVIEW_");
        ensureRolePermissions(connection, "ROLE_M4_SIGN", List.of("PERM_M4_REVISION_APPROVE", "PERM_M4_CONSULTATION_COMMENT"),
            "RP_M4_BATCH2_SIGN_");
        ensureRolePermissions(connection, "ROLE_M4_MEDICAL_ORDER_EXECUTE", List.of(
            "PERM_M4_MEDICAL_ORDER_QUERY", "PERM_M4_MEDICAL_ORDER_ACCEPT", "PERM_M4_MEDICAL_ORDER_COMPLETE"),
            "RP_M4_BATCH2_ORDER_EXEC_");

        upsertUser(connection, "USER_M4_ORDER_EXECUTE", "U-M4-ORDER-EXEC", "m4.order.execute", "M4 Order Execute", "M4_MEDICAL_ORDER_EXECUTE");
        ensureUserRoles(connection, "USER_M4_ORDER_EXECUTE", List.of("ROLE_M4_MEDICAL_ORDER_EXECUTE"), "UR_M4_ORDER_EXECUTE");
    }

    private void upsertMenu(Connection connection, String id, String parentId, String menuCode, String menuName, String menuType,
                            String path, String componentName, String permissionPrefix, int sortOrder) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
            update menus
            set parent_id = ?, menu_code = ?, menu_name = ?, menu_type = ?, path = ?, component_name = ?,
                permission_prefix = ?, sort_order = ?, updated_at = ?
            where id = ?
            """)) {
            update.setString(1, parentId);
            update.setString(2, menuCode);
            update.setString(3, menuName);
            update.setString(4, menuType);
            update.setString(5, path);
            update.setString(6, componentName);
            update.setString(7, permissionPrefix);
            update.setInt(8, sortOrder);
            update.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            update.setString(10, id);
            if (update.executeUpdate() > 0) {
                return;
            }
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into menus
                (id, parent_id, menu_code, menu_name, menu_type, path, component_name, permission_prefix, sort_order, created_at, updated_at)
            values
                (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
            insert.setString(1, id);
            insert.setString(2, parentId);
            insert.setString(3, menuCode);
            insert.setString(4, menuName);
            insert.setString(5, menuType);
            insert.setString(6, path);
            insert.setString(7, componentName);
            insert.setString(8, permissionPrefix);
            insert.setInt(9, sortOrder);
            insert.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            insert.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private void upsertPermission(Connection connection, String id, String code, String name, String menuId,
                                  String actionKey, String httpMethod, String resourcePath, String permissionGroup, int sortOrder) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
            update permissions
            set permission_code = ?, permission_name = ?, menu_id = ?, action_key = ?, http_method = ?,
                resource_path = ?, permission_group = ?, sort_order = ?, updated_at = ?
            where id = ?
            """)) {
            update.setString(1, code);
            update.setString(2, name);
            update.setString(3, menuId);
            update.setString(4, actionKey);
            update.setString(5, httpMethod);
            update.setString(6, resourcePath);
            update.setString(7, permissionGroup);
            update.setInt(8, sortOrder);
            update.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            update.setString(10, id);
            if (update.executeUpdate() > 0) {
                return;
            }
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into permissions
                (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path, permission_group, sort_order, created_at, updated_at)
            values
                (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
            insert.setString(1, id);
            insert.setString(2, code);
            insert.setString(3, name);
            insert.setString(4, menuId);
            insert.setString(5, actionKey);
            insert.setString(6, httpMethod);
            insert.setString(7, resourcePath);
            insert.setString(8, permissionGroup);
            insert.setInt(9, sortOrder);
            insert.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            insert.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private void upsertRole(Connection connection, String id, String roleCode, String roleName, String roleType,
                            String dataScope, String remarks) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
            update roles
            set role_code = ?, role_name = ?, role_type = ?, data_scope = ?, remarks = ?, updated_at = ?
            where id = ?
            """)) {
            update.setString(1, roleCode);
            update.setString(2, roleName);
            update.setString(3, roleType);
            update.setString(4, dataScope);
            update.setString(5, remarks);
            update.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            update.setString(7, id);
            if (update.executeUpdate() > 0) {
                return;
            }
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into roles
                (id, role_code, role_name, role_type, data_scope, remarks, enabled, created_at, updated_at)
            values
                (?, ?, ?, ?, ?, ?, 1, ?, ?)
            """)) {
            insert.setString(1, id);
            insert.setString(2, roleCode);
            insert.setString(3, roleName);
            insert.setString(4, roleType);
            insert.setString(5, dataScope);
            insert.setString(6, remarks);
            insert.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            insert.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private void ensureRolePermissions(Connection connection, String roleId, List<String> permissionIds, String idPrefix) throws SQLException {
        int index = 1;
        for (String permissionId : permissionIds) {
            if (exists(connection, "select 1 from role_permissions where role_id = '" + roleId + "' and permission_id = '" + permissionId + "'")) {
                index++;
                continue;
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                insert into role_permissions
                    (id, role_id, permission_id, assigned_at)
                values
                    (?, ?, ?, ?)
                """)) {
                insert.setString(1, idPrefix + index++);
                insert.setString(2, roleId);
                insert.setString(3, permissionId);
                insert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                insert.executeUpdate();
            }
        }
    }

    private void upsertUser(Connection connection, String id, String userCode, String loginName, String name, String roleCode) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
            update users
            set user_code = ?, login_name = ?, name = ?, password = ?, password_algo = ?, password_salt = ?, role = ?, enabled = 1, updated_at = ?
            where id = ?
            """)) {
            update.setString(1, userCode);
            update.setString(2, loginName);
            update.setString(3, name);
            update.setString(4, DEFAULT_PASSWORD);
            update.setString(5, DEFAULT_PASSWORD_ALGO);
            update.setString(6, DEFAULT_PASSWORD_SALT);
            update.setString(7, roleCode);
            update.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            update.setString(9, id);
            if (update.executeUpdate() > 0) {
                return;
            }
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into users
                (id, user_code, login_name, name, password, password_algo, password_salt, role, enabled, created_at, updated_at)
            values
                (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
            """)) {
            insert.setString(1, id);
            insert.setString(2, userCode);
            insert.setString(3, loginName);
            insert.setString(4, name);
            insert.setString(5, DEFAULT_PASSWORD);
            insert.setString(6, DEFAULT_PASSWORD_ALGO);
            insert.setString(7, DEFAULT_PASSWORD_SALT);
            insert.setString(8, roleCode);
            insert.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            insert.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private void ensureUserRoles(Connection connection, String userId, List<String> roleIds, String idPrefix) throws SQLException {
        int index = 1;
        for (String roleId : roleIds) {
            if (exists(connection, "select 1 from user_roles where user_id = '" + userId + "' and role_id = '" + roleId + "'")) {
                index++;
                continue;
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                insert into user_roles
                    (id, user_id, role_id, is_primary, assigned_at, assigned_by_name)
                values
                    (?, ?, ?, 1, ?, 'system')
                """)) {
                insert.setString(1, idPrefix + index++);
                insert.setString(2, userId);
                insert.setString(3, roleId);
                insert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                insert.executeUpdate();
            }
        }
    }

    private boolean exists(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next();
        }
    }
}
