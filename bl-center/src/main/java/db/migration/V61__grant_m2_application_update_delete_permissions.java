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

public class V61__grant_m2_application_update_delete_permissions extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        upsertPermission(connection, "PERM_APPLICATION_UPDATE", "PERM_APPLICATION_UPDATE", "Application update",
            "MENU_M2_CLINICAL", "UPDATE", "PATCH", "/api/v1/applications/{id}", "M2", 118);
        upsertPermission(connection, "PERM_APPLICATION_DELETE", "PERM_APPLICATION_DELETE", "Application delete",
            "MENU_M2_CLINICAL", "DELETE", "DELETE", "/api/v1/applications/{id}", "M2", 119);

        ensureRolePermissions(connection, "ROLE_PATHOLOGY_ADMIN",
            List.of("PERM_APPLICATION_UPDATE", "PERM_APPLICATION_DELETE"), "RP_M2_APP_ADMIN_EXT_");
        ensureRolePermissions(connection, "ROLE_M2_CLINICAL_REGISTER",
            List.of("PERM_APPLICATION_UPDATE", "PERM_APPLICATION_DELETE"), "RP_M2_APP_REGISTER_EXT_");
        ensureRolePermissions(connection, "ROLE_M2_CLINICAL_IMPORT",
            List.of("PERM_APPLICATION_UPDATE", "PERM_APPLICATION_DELETE"), "RP_M2_APP_IMPORT_EXT_");
    }

    private void upsertPermission(Connection connection, String id, String code, String name, String menuId,
                                  String actionKey, String httpMethod, String resourcePath,
                                  String permissionGroup, int sortOrder) throws SQLException {
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
                (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path,
                 permission_group, sort_order, created_at, updated_at)
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

    private void ensureRolePermissions(Connection connection, String roleId, List<String> permissionIds, String idPrefix) throws SQLException {
        int index = 1;
        for (String permissionId : permissionIds) {
            if (exists(connection, "select 1 from role_permissions where role_id = '" + roleId
                + "' and permission_id = '" + permissionId + "'")) {
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

    private boolean exists(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next();
        }
    }
}
