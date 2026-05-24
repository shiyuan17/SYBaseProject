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

public class V48__repair_specimen_container_columns_and_workflow_reference_permission extends BaseJavaMigration {

    private static final String MENU_ID = "MENU_CONFIGS";
    private static final String LEGACY_MENU_ID = "MENU_SYS_CONFIG";
    private static final String PERMISSION_ID = "PERM_WORKFLOW_REFERENCE_QUERY";
    private static final String ACTION_KEY = "WORKFLOW_REFERENCE_QUERY";

    private static final List<String> ROLE_IDS = List.of(
        "ROLE_PATHOLOGY_ADMIN",
        "ROLE_M2_CLINICAL_REGISTER",
        "ROLE_M2_FIXATION_VERIFY",
        "ROLE_M3_GROSSING"
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        ensureSpecimenColumns(connection);
        ensureWorkflowReferencePermission(connection);
    }

    private void ensureSpecimenColumns(Connection connection) throws SQLException {
        if (!tableExists(connection, "SPECIMENS")) {
            return;
        }
        ensureColumn(connection, "SPECIMENS", "CONTAINER_NAME",
            "ALTER TABLE specimens ADD COLUMN container_name VARCHAR(200)");
        ensureColumn(connection, "SPECIMENS", "CONTAINER_COUNT",
            "ALTER TABLE specimens ADD COLUMN container_count INTEGER");
    }

    private void ensureWorkflowReferencePermission(Connection connection) throws SQLException {
        if (!tableExists(connection, "PERMISSIONS") || !tableExists(connection, "ROLE_PERMISSIONS")) {
            return;
        }
        String permissionId = ensurePermission(connection);
        for (String roleId : ROLE_IDS) {
            ensureRolePermission(connection, roleId, permissionId);
        }
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String ddl) throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            execute(connection, ddl);
        }
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

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getColumns(null, null, tableName, columnName)) {
            while (resultSet.next()) {
                if (tableName.equalsIgnoreCase(resultSet.getString("TABLE_NAME"))
                    && columnName.equalsIgnoreCase(resultSet.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        try (ResultSet resultSet = metaData.getColumns(null, null, tableName.toLowerCase(), columnName.toLowerCase())) {
            while (resultSet.next()) {
                if (tableName.equalsIgnoreCase(resultSet.getString("TABLE_NAME"))
                    && columnName.equalsIgnoreCase(resultSet.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String ensurePermission(Connection connection) throws SQLException {
        String menuId = menuExists(connection, MENU_ID) ? MENU_ID : LEGACY_MENU_ID;
        String permissionId = findPermissionId(connection, PERMISSION_ID);
        if (permissionId != null) {
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
                update.setString(10, permissionId);
                update.setString(11, PERMISSION_ID);
                update.executeUpdate();
            }
            return permissionId;
        }

        try (PreparedStatement insert = connection.prepareStatement("""
            insert into permissions
                (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path,
                 permission_group, sort_order, enabled, created_at, updated_at)
            values
                (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
            """)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
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

    private String findPermissionId(Connection connection, String permissionCode) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("""
            select id
            from permissions
            where id = ? or permission_code = ?
            """)) {
            query.setString(1, permissionCode);
            query.setString(2, permissionCode);
            try (ResultSet resultSet = query.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }
        return null;
    }

    private boolean menuExists(Connection connection, String menuId) throws SQLException {
        if (!tableExists(connection, "MENUS")) {
            return false;
        }
        try (PreparedStatement query = connection.prepareStatement("""
            select 1
            from menus
            where id = ?
            """)) {
            query.setString(1, menuId);
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void ensureRolePermission(Connection connection, String roleId, String permissionId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("""
            select 1
            from role_permissions
            where role_id = ?
              and permission_id = ?
            """)) {
            query.setString(1, roleId);
            query.setString(2, permissionId);
            try (ResultSet resultSet = query.executeQuery()) {
                if (resultSet.next()) {
                    return;
                }
            }
        }

        try (PreparedStatement insert = connection.prepareStatement("""
            insert into role_permissions (id, role_id, permission_id, created_at)
            values (?, ?, ?, ?)
            """)) {
            insert.setString(1, "RP_FIX_WORKFLOW_REFERENCE_" + roleId);
            insert.setString(2, roleId);
            insert.setString(3, permissionId);
            insert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
