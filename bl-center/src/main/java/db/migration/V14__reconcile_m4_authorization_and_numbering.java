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
import java.util.List;

public class V14__reconcile_m4_authorization_and_numbering extends BaseJavaMigration {

    private static final String DEFAULT_PASSWORD = "0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134";
    private static final String DEFAULT_PASSWORD_ALGO = "SM3";
    private static final String DEFAULT_PASSWORD_SALT = "9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        ensureReportNumberingRule(connection);
        upsertMenu(connection, "MENU_M4_WORKFLOW", null, "M4_WORKFLOW", "M4 Workflow", "DIRECTORY",
            "/doctor-workflow", "DoctorWorkflowRoot", "m4", 130);
        upsertMenu(connection, "MENU_M4_ASSIGN", "MENU_M4_WORKFLOW", "M4_ASSIGN", "Diagnosis Assignment", "MENU",
            "/api/v1/diagnostic-tasks/pending", "DiagnosisAssignment", "m4:assign", 131);
        upsertMenu(connection, "MENU_M4_WORKBENCH", "MENU_M4_WORKFLOW", "M4_WORKBENCH", "Diagnosis Workbench", "MENU",
            "/api/v1/pathology-cases/{id}/diagnostic-workbench", "DiagnosisWorkbench", "m4:workbench", 132);
        upsertMenu(connection, "MENU_M4_REPORT", "MENU_M4_WORKFLOW", "M4_REPORT", "Pathology Report", "MENU",
            "/api/v1/pathology-reports", "PathologyReport", "m4:report", 133);
        upsertMenu(connection, "MENU_M4_TRACKING", "MENU_M4_WORKFLOW", "M4_TRACKING", "Report Tracking", "MENU",
            "/api/v1/pathology-cases/{id}/report-tracking", "ReportTracking", "m4:tracking", 134);

        upsertPermission(connection, "PERM_M4_DIAG_TASK_QUERY", "PERM_M4_DIAG_TASK_QUERY", "Diagnosis task query",
            "MENU_M4_ASSIGN", "QUERY", "GET", "/api/v1/diagnostic-tasks/pending", "M4", 131);
        upsertPermission(connection, "PERM_M4_ASSIGN", "PERM_M4_ASSIGN", "Diagnosis assign",
            "MENU_M4_ASSIGN", "ASSIGN", "POST", "/api/v1/diagnostic-tasks/{id}/assign", "M4", 132);
        upsertPermission(connection, "PERM_M4_ACCEPT", "PERM_M4_ACCEPT", "Diagnosis accept",
            "MENU_M4_WORKBENCH", "ACCEPT", "POST", "/api/v1/diagnostic-tasks/{id}/accept", "M4", 133);
        upsertPermission(connection, "PERM_M4_START", "PERM_M4_START", "Diagnosis start",
            "MENU_M4_WORKBENCH", "START", "POST", "/api/v1/diagnostic-tasks/{id}/start", "M4", 134);
        upsertPermission(connection, "PERM_M4_WORKBENCH_QUERY", "PERM_M4_WORKBENCH_QUERY", "Diagnosis workbench query",
            "MENU_M4_WORKBENCH", "QUERY", "GET", "/api/v1/pathology-cases/{id}/diagnostic-workbench", "M4", 135);
        upsertPermission(connection, "PERM_M4_REPORT_CREATE", "PERM_M4_REPORT_CREATE", "Pathology report create",
            "MENU_M4_REPORT", "CREATE", "POST", "/api/v1/pathology-reports", "M4", 136);
        upsertPermission(connection, "PERM_M4_REPORT_SUBMIT", "PERM_M4_REPORT_SUBMIT", "Pathology report submit",
            "MENU_M4_REPORT", "SUBMIT", "POST", "/api/v1/pathology-reports/{id}/submit", "M4", 137);
        upsertPermission(connection, "PERM_M4_REPORT_REVIEW", "PERM_M4_REPORT_REVIEW", "Pathology report review",
            "MENU_M4_REPORT", "REVIEW", "POST", "/api/v1/pathology-reports/{id}/review", "M4", 138);
        upsertPermission(connection, "PERM_M4_REPORT_SIGN", "PERM_M4_REPORT_SIGN", "Pathology report sign",
            "MENU_M4_REPORT", "SIGN", "POST", "/api/v1/pathology-reports/{id}/sign", "M4", 139);
        upsertPermission(connection, "PERM_M4_REPORT_PUBLISH", "PERM_M4_REPORT_PUBLISH", "Pathology report publish",
            "MENU_M4_REPORT", "PUBLISH", "POST", "/api/v1/pathology-reports/{id}/publish", "M4", 140);
        upsertPermission(connection, "PERM_M4_REPORT_TRACKING_QUERY", "PERM_M4_REPORT_TRACKING_QUERY", "Pathology report tracking query",
            "MENU_M4_TRACKING", "QUERY", "GET", "/api/v1/pathology-cases/{id}/report-tracking", "M4", 141);

        upsertRole(connection, "ROLE_M4_ASSIGN", "M4_ASSIGN", "诊断分派员", "BUSINESS", "DEPARTMENT", "M4 diagnosis assignment workstation");
        upsertRole(connection, "ROLE_M4_DIAGNOSIS", "M4_DIAGNOSIS", "诊断医生", "BUSINESS", "DEPARTMENT", "M4 diagnosis workstation");
        upsertRole(connection, "ROLE_M4_REVIEW", "M4_REVIEW", "审核医生", "BUSINESS", "DEPARTMENT", "M4 review workstation");
        upsertRole(connection, "ROLE_M4_SIGN", "M4_SIGN", "签发医生", "BUSINESS", "DEPARTMENT", "M4 sign workstation");
        upsertRole(connection, "ROLE_M4_TRACKING", "M4_TRACKING", "报告追踪员", "BUSINESS", "DEPARTMENT", "M4 report tracking workstation");

        ensureRolePermissions(connection, "ROLE_PATHOLOGY_ADMIN", List.of(
            "PERM_M4_DIAG_TASK_QUERY", "PERM_M4_ASSIGN", "PERM_M4_ACCEPT", "PERM_M4_START", "PERM_M4_WORKBENCH_QUERY",
            "PERM_M4_REPORT_CREATE", "PERM_M4_REPORT_SUBMIT", "PERM_M4_REPORT_REVIEW", "PERM_M4_REPORT_SIGN",
            "PERM_M4_REPORT_PUBLISH", "PERM_M4_REPORT_TRACKING_QUERY"), "RP_M4_ADMIN_");
        ensureRolePermissions(connection, "ROLE_M4_ASSIGN", List.of("PERM_M4_DIAG_TASK_QUERY", "PERM_M4_ASSIGN"), "RP_M4_ASSIGN_");
        ensureRolePermissions(connection, "ROLE_M4_DIAGNOSIS", List.of(
            "PERM_M4_DIAG_TASK_QUERY", "PERM_M4_ACCEPT", "PERM_M4_START", "PERM_M4_WORKBENCH_QUERY",
            "PERM_M4_REPORT_CREATE", "PERM_M4_REPORT_SUBMIT"), "RP_M4_DIAG_");
        ensureRolePermissions(connection, "ROLE_M4_REVIEW", List.of(
            "PERM_M4_DIAG_TASK_QUERY", "PERM_M4_WORKBENCH_QUERY", "PERM_M4_REPORT_REVIEW"), "RP_M4_REVIEW_");
        ensureRolePermissions(connection, "ROLE_M4_SIGN", List.of(
            "PERM_M4_DIAG_TASK_QUERY", "PERM_M4_WORKBENCH_QUERY", "PERM_M4_REPORT_SIGN", "PERM_M4_REPORT_PUBLISH"), "RP_M4_SIGN_");
        ensureRolePermissions(connection, "ROLE_M4_TRACKING", List.of("PERM_M4_REPORT_TRACKING_QUERY"), "RP_M4_TRACKING_");

        upsertUser(connection, "USER_M4_ASSIGN", "U-M4-ASSIGN", "m4.assign", "M4 Assign", "M4_ASSIGN");
        upsertUser(connection, "USER_M4_DIAGNOSIS", "U-M4-DIAG", "m4.diagnosis", "M4 Diagnosis", "M4_DIAGNOSIS");
        upsertUser(connection, "USER_M4_REVIEW", "U-M4-REVIEW", "m4.review", "M4 Review", "M4_REVIEW");
        upsertUser(connection, "USER_M4_SIGN", "U-M4-SIGN", "m4.sign", "M4 Sign", "M4_SIGN");
        upsertUser(connection, "USER_M4_TRACKING", "U-M4-TRACKING", "m4.tracking", "M4 Tracking", "M4_TRACKING");
        upsertUser(connection, "USER_M4_NO_PERMISSION", "U-M4-NOAUTH", "m4.noauth", "M4 No Permission", null);

        ensureUserRoles(connection, "USER_M4_ASSIGN", List.of("ROLE_M4_ASSIGN"), "UR_M4_ASSIGN");
        ensureUserRoles(connection, "USER_M4_DIAGNOSIS", List.of("ROLE_M4_DIAGNOSIS"), "UR_M4_DIAGNOSIS");
        ensureUserRoles(connection, "USER_M4_REVIEW", List.of("ROLE_M4_REVIEW"), "UR_M4_REVIEW");
        ensureUserRoles(connection, "USER_M4_SIGN", List.of("ROLE_M4_SIGN"), "UR_M4_SIGN");
        ensureUserRoles(connection, "USER_M4_TRACKING", List.of("ROLE_M4_TRACKING"), "UR_M4_TRACKING");
    }

    private void ensureReportNumberingRule(Connection connection) throws SQLException {
        if (exists(connection, "select 1 from numbering_rules where biz_type = 'REPORT_NO'")) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
            insert into numbering_rules
                (id, rule_code, biz_type, prefix_pattern, date_pattern, seq_length, reset_policy, scope_type, remarks)
            values
                ('NR_REPORT', 'RULE_REPORT_NO', 'REPORT_NO', 'RP', 'yyyyMMdd', 4, 'DAILY', 'GLOBAL', '报告编号')
            """)) {
            statement.executeUpdate();
        }
    }

    private void upsertMenu(Connection connection, String id, String parentId, String menuCode, String menuName, String menuType,
                            String path, String componentName, String permissionPrefix, int sortOrder) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
            update menus
            set parent_id = ?, menu_code = ?, menu_name = ?, menu_type = ?, path = ?, component_name = ?,
                permission_prefix = ?, sort_order = ?, updated_at = ?
            where id = ?
            """)) {
            if (parentId == null) {
                update.setNull(1, java.sql.Types.VARCHAR);
            } else {
                update.setString(1, parentId);
            }
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
            if (parentId == null) {
                insert.setNull(2, java.sql.Types.VARCHAR);
            } else {
                insert.setString(2, parentId);
            }
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
            if (roleCode == null) {
                update.setNull(7, java.sql.Types.VARCHAR);
            } else {
                update.setString(7, roleCode);
            }
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
            if (roleCode == null) {
                insert.setNull(8, java.sql.Types.VARCHAR);
            } else {
                insert.setString(8, roleCode);
            }
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
