package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
public class V42__add_m2_application_list_menu extends BaseJavaMigration {

    private static final String MENU_ID = "MENU_M2_APPLICATION_LIST";
    private static final String ADMIN_ROLE_ID = "ROLE_PATHOLOGY_ADMIN";
    private static final String TRACKING_ROLE_ID = "ROLE_M2_TRACKING_QUERY";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        upsertMenu(connection);
        ensureRoleMenu(connection, "RM_M2_APP_LIST_ADMIN", ADMIN_ROLE_ID, MENU_ID);
        ensureRoleMenu(connection, "RM_M2_APP_LIST_TRACKING", TRACKING_ROLE_ID, MENU_ID);
    }

    private void upsertMenu(Connection connection) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
            update menus
            set parent_id = ?, menu_code = ?, menu_name = ?, menu_type = ?, path = ?, component_name = ?,
                permission_prefix = ?, sort_order = ?, visible = 1, enabled = 1, updated_at = ?
            where id = ?
            """)) {
            update.setString(1, "MENU_M2_WORKFLOW");
            update.setString(2, "M2_APPLICATION_LIST");
            update.setString(3, "申请表列表");
            update.setString(4, "MENU");
            update.setString(5, "/api/v1/applications");
            update.setString(6, "ApplicationList");
            update.setString(7, "m2:application-list");
            update.setInt(8, 110);
            update.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            update.setString(10, MENU_ID);
            if (update.executeUpdate() > 0) {
                return;
            }
        }

        try (PreparedStatement insert = connection.prepareStatement("""
            insert into menus
                (id, parent_id, menu_code, menu_name, menu_type, path, component_name, permission_prefix,
                 sort_order, visible, enabled, created_at, updated_at)
            values
                (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, ?, ?)
            """)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            insert.setString(1, MENU_ID);
            insert.setString(2, "MENU_M2_WORKFLOW");
            insert.setString(3, "M2_APPLICATION_LIST");
            insert.setString(4, "申请表列表");
            insert.setString(5, "MENU");
            insert.setString(6, "/api/v1/applications");
            insert.setString(7, "ApplicationList");
            insert.setString(8, "m2:application-list");
            insert.setInt(9, 110);
            insert.setTimestamp(10, now);
            insert.setTimestamp(11, now);
            insert.executeUpdate();
        }
    }

    private void ensureRoleMenu(Connection connection, String id, String roleId, String menuId) throws SQLException {
        if (exists(connection, roleId, menuId)) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into role_menus
                (id, role_id, menu_id, assigned_at)
            values
                (?, ?, ?, ?)
            """)) {
            insert.setString(1, id);
            insert.setString(2, roleId);
            insert.setString(3, menuId);
            insert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private boolean exists(Connection connection, String roleId, String menuId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            select 1
            from role_menus
            where role_id = ?
              and menu_id = ?
            """)) {
            statement.setString(1, roleId);
            statement.setString(2, menuId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
