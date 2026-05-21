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
import java.util.UUID;

public class V37__seed_department_dict_and_permissions extends BaseJavaMigration {

    private static final String MENU_ID = "MENU_DEPARTMENTS";
    private static final String PERMISSION_QUERY = "PERM_SYS_DEPARTMENT_QUERY";
    private static final String PERMISSION_CREATE = "PERM_SYS_DEPARTMENT_CREATE";
    private static final List<String> USER_QUERY_ROLE_IDS = List.of(
        "ROLE_PATHOLOGY_ADMIN",
        "ROLE_M2_CLINICAL_REGISTER",
        "ROLE_M2_FIXATION_VERIFY",
        "ROLE_M2_TRANSPORT_HANDOVER",
        "ROLE_M2_SPECIMEN_RECEIVE",
        "ROLE_M2_TRACKING_QUERY",
        "ROLE_M2_CLINICAL_IMPORT",
        "ROLE_M3_GROSSING",
        "ROLE_M3_DEHYDRATION",
        "ROLE_M3_EMBEDDING",
        "ROLE_M3_SLICING",
        "ROLE_M3_STAINING",
        "ROLE_M3_REWORK",
        "ROLE_M3_TRACKING"
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        ensureDepartmentTable(connection);
        seedDepartments(connection);
        ensureDepartmentMenu(connection);
        ensurePermission(connection, PERMISSION_QUERY, "查询科室字典", "QUERY", "GET", "/api/v1/departments", 7);
        ensurePermission(connection, PERMISSION_CREATE, "维护科室字典", "CREATE", "POST", "/api/v1/departments", 8);
        ensureRoleMenu(connection, "ROLE_PATHOLOGY_ADMIN", MENU_ID);
        ensureRolePermission(connection, "ROLE_PATHOLOGY_ADMIN", PERMISSION_QUERY);
        ensureRolePermission(connection, "ROLE_PATHOLOGY_ADMIN", PERMISSION_CREATE);
        for (String roleId : USER_QUERY_ROLE_IDS) {
            ensureRolePermission(connection, roleId, PERMISSION_QUERY);
            ensureRolePermission(connection, roleId, "PERM_SYS_USER_QUERY");
        }
    }

    private void ensureDepartmentTable(Connection connection) throws SQLException {
        if (tableExists(connection, "DEPARTMENT_DICT")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE department_dict (
                    id VARCHAR2(64) PRIMARY KEY,
                    parent_id VARCHAR2(64),
                    department_code VARCHAR2(64) NOT NULL,
                    department_name VARCHAR2(100) NOT NULL,
                    sort_order NUMBER(10) DEFAULT 0 NOT NULL,
                    enabled NUMBER(1) DEFAULT 1 NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
                )
                """);
            statement.execute("CREATE UNIQUE INDEX uk_department_dict_code ON department_dict (department_code)");
            statement.execute("CREATE INDEX idx_department_dict_parent ON department_dict (parent_id)");
        }
    }

    private void seedDepartments(Connection connection) throws SQLException {
        insertDepartmentIfAbsent(connection, "DEPT_ROOT", null, "ROOT", "全部科室", 0);
        insertDepartmentIfAbsent(connection, "DEPT_CLINICAL", "DEPT_ROOT", "CLINICAL", "临床科室", 10);
        insertDepartmentIfAbsent(connection, "DEPT_OR", "DEPT_CLINICAL", "OR", "手术室", 20);
        insertDepartmentIfAbsent(connection, "DEPT_ICU", "DEPT_CLINICAL", "ICU", "重症监护室", 30);
        insertDepartmentIfAbsent(connection, "DEPT_PATH", "DEPT_ROOT", "PATHOLOGY", "病理科", 40);
    }

    private void insertDepartmentIfAbsent(
        Connection connection,
        String id,
        String parentId,
        String code,
        String name,
        int sortOrder
    ) throws SQLException {
        if (exists(connection, "select 1 from department_dict where id = '" + id + "'")) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into department_dict
                (id, parent_id, department_code, department_name, sort_order, enabled, created_at, updated_at)
            values
                (?, ?, ?, ?, ?, 1, ?, ?)
            """)) {
            insert.setString(1, id);
            insert.setString(2, parentId);
            insert.setString(3, code);
            insert.setString(4, name);
            insert.setInt(5, sortOrder);
            insert.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            insert.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private void ensureDepartmentMenu(Connection connection) throws SQLException {
        String existingMenuId = menuExists(connection, MENU_ID)
            ? MENU_ID
            : menuExists(connection, "MENU_SYS_DEPT") ? "MENU_SYS_DEPT" : null;
        if (existingMenuId == null) {
            try (PreparedStatement insert = connection.prepareStatement("""
                insert into menus
                    (id, parent_id, menu_code, menu_name, menu_type, path, component_name, icon,
                     sort_order, visible, enabled, created_at, updated_at)
                values
                    (?, 'MENU_SYSTEM', 'DEPARTMENTS', '科室字典', 'MENU', '/system/departments', 'Departments',
                     'carbon:building', 25, 1, 1, ?, ?)
                """)) {
                insert.setString(1, MENU_ID);
                insert.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                insert.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                insert.executeUpdate();
            }
            return;
        }
        try (PreparedStatement update = connection.prepareStatement("""
            update menus
            set parent_id = 'MENU_SYSTEM',
                menu_code = 'DEPARTMENTS',
                menu_name = '科室字典',
                menu_type = 'MENU',
                path = '/system/departments',
                component_name = 'Departments',
                icon = 'carbon:building',
                sort_order = 25,
                visible = 1,
                enabled = 1,
                updated_at = ?
            where id = ?
            """)) {
            update.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            update.setString(2, existingMenuId);
            update.executeUpdate();
        }
    }

    private void ensurePermission(
        Connection connection,
        String permissionId,
        String permissionName,
        String actionKey,
        String method,
        String resourcePath,
        int sortOrder
    ) throws SQLException {
        String menuId = menuExists(connection, MENU_ID) ? MENU_ID : "MENU_SYS_DEPT";
        if (exists(connection, "select 1 from permissions where id = '" + permissionId + "'")
            || exists(connection, "select 1 from permissions where permission_code = '" + permissionId + "'")) {
            try (PreparedStatement update = connection.prepareStatement("""
                update permissions
                set permission_code = ?,
                    permission_name = ?,
                    menu_id = ?,
                    action_key = ?,
                    http_method = ?,
                    resource_path = ?,
                    permission_group = 'MASTERDATA',
                    sort_order = ?,
                    enabled = 1,
                    updated_at = ?
                where id = ? or permission_code = ?
                """)) {
                update.setString(1, permissionId);
                update.setString(2, permissionName);
                update.setString(3, menuId);
                update.setString(4, actionKey);
                update.setString(5, method);
                update.setString(6, resourcePath);
                update.setInt(7, sortOrder);
                update.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                update.setString(9, permissionId);
                update.setString(10, permissionId);
                update.executeUpdate();
            }
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into permissions
                (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path,
                 permission_group, sort_order, enabled, created_at, updated_at)
            values
                (?, ?, ?, ?, ?, ?, ?, 'MASTERDATA', ?, 1, ?, ?)
            """)) {
            insert.setString(1, permissionId);
            insert.setString(2, permissionId);
            insert.setString(3, permissionName);
            insert.setString(4, menuId);
            insert.setString(5, actionKey);
            insert.setString(6, method);
            insert.setString(7, resourcePath);
            insert.setInt(8, sortOrder);
            insert.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            insert.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private void ensureRoleMenu(Connection connection, String roleId, String menuId) throws SQLException {
        if (menuExists(connection, menuId) && !exists(connection,
            "select 1 from role_menus where role_id = '" + roleId + "' and menu_id = '" + menuId + "'")) {
            try (PreparedStatement insert = connection.prepareStatement("""
                insert into role_menus (id, role_id, menu_id, assigned_at)
                values (?, ?, ?, ?)
                """)) {
                insert.setString(1, "RM_DEPT_" + UUID.randomUUID().toString().replace("-", ""));
                insert.setString(2, roleId);
                insert.setString(3, menuId);
                insert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                insert.executeUpdate();
            }
        }
    }

    private void ensureRolePermission(Connection connection, String roleId, String permissionId) throws SQLException {
        if (!exists(connection, "select 1 from roles where id = '" + roleId + "'")) {
            return;
        }
        if (!exists(connection, "select 1 from permissions where id = '" + permissionId + "'")) {
            return;
        }
        if (exists(connection,
            "select 1 from role_permissions where role_id = '" + roleId + "' and permission_id = '" + permissionId + "'")) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into role_permissions (id, role_id, permission_id, assigned_at)
            values (?, ?, ?, ?)
            """)) {
            insert.setString(1, "RP_DEPT_" + UUID.randomUUID().toString().replace("-", ""));
            insert.setString(2, roleId);
            insert.setString(3, permissionId);
            insert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    private boolean menuExists(Connection connection, String menuId) throws SQLException {
        return exists(connection, "select 1 from menus where id = '" + menuId + "'");
    }

    private boolean exists(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next();
        }
    }
}
