package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class V85__add_m5_archive_cabinet_delete_permission extends BaseJavaMigration {

    private static final String PERMISSION_ID = "PERM_M5_ARCHIVE_CABINET_DELETE";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        ensurePermission(connection);
        ensureRolePermission(connection, "ROLE_PATHOLOGY_ADMIN", "RP_M5_ADMIN_ARCHIVE_CABINET_DELETE");
        ensureRolePermission(connection, "ROLE_ARCHIVE_MANAGER", "RP_M5_ARCHIVE_CABINET_DELETE");
    }

    private void ensurePermission(Connection connection) throws SQLException {
        if (exists(connection, "select 1 from permissions where id = ? or permission_code = ?", PERMISSION_ID, PERMISSION_ID)) {
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
                update.setString(2, "Archive cabinet delete");
                update.setString(3, "MENU_M5_ARCHIVE");
                update.setString(4, "ARCHIVE_CABINET_DELETE");
                update.setString(5, "DELETE");
                update.setString(6, "/api/v1/archive-cabinets/{id}");
                update.setString(7, "M5");
                update.setInt(8, 164);
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
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            insert.setString(1, PERMISSION_ID);
            insert.setString(2, PERMISSION_ID);
            insert.setString(3, "Archive cabinet delete");
            insert.setString(4, "MENU_M5_ARCHIVE");
            insert.setString(5, "ARCHIVE_CABINET_DELETE");
            insert.setString(6, "DELETE");
            insert.setString(7, "/api/v1/archive-cabinets/{id}");
            insert.setString(8, "M5");
            insert.setInt(9, 164);
            insert.setTimestamp(10, now);
            insert.setTimestamp(11, now);
            insert.executeUpdate();
        }
    }

    private void ensureRolePermission(Connection connection, String roleId, String rolePermissionId) throws SQLException {
        if (exists(connection, "select 1 from role_permissions where role_id = ? and permission_id = ?",
            roleId, PERMISSION_ID)) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into role_permissions (id, role_id, permission_id, assigned_at)
            values (?, ?, ?, ?)
            """)) {
            insert.setString(1, rolePermissionId);
            insert.setString(2, roleId);
            insert.setString(3, PERMISSION_ID);
            insert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private boolean exists(Connection connection, String sql, String... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) {
                statement.setString(index + 1, values[index]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
