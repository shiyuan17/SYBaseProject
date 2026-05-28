package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class V60__move_m2_receipt_menu_into_m3_workflow extends BaseJavaMigration {

    private static final String RECEIPT_MENU_ID = "MENU_M2_RECEIPT";
    private static final String TECHNICAL_ROOT_MENU_ID = "MENU_M3_WORKFLOW";
    private static final String RECEIPT_ROLE_ID = "ROLE_M2_SPECIMEN_RECEIVE";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        moveReceiptMenu(connection, now);
        ensureRoleMenu(connection, "RM_M2_RECEIVE_TECH_ROOT", RECEIPT_ROLE_ID, TECHNICAL_ROOT_MENU_ID, now);
        ensureRoleMenu(connection, "RM_M2_RECEIVE_RECEIPT", RECEIPT_ROLE_ID, RECEIPT_MENU_ID, now);
    }

    private void moveReceiptMenu(Connection connection, Timestamp now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            update menus
            set parent_id = ?,
                menu_code = ?,
                menu_name = ?,
                menu_type = 'MENU',
                path = ?,
                component_name = ?,
                permission_prefix = ?,
                sort_order = ?,
                visible = 1,
                enabled = 1,
                updated_at = ?
            where id = ?
            """)) {
            statement.setString(1, TECHNICAL_ROOT_MENU_ID);
            statement.setString(2, "M2_RECEIPT");
            statement.setString(3, "病理接收");
            statement.setString(4, "/workflow/pathology-receipt");
            statement.setString(5, "PathologyReceipt");
            statement.setString(6, "m2:receipt");
            statement.setInt(7, 120);
            statement.setTimestamp(8, now);
            statement.setString(9, RECEIPT_MENU_ID);
            statement.executeUpdate();
        }
    }

    private void ensureRoleMenu(
        Connection connection,
        String id,
        String roleId,
        String menuId,
        Timestamp assignedAt
    ) throws SQLException {
        if (exists(connection, roleId, menuId)) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
            insert into role_menus
                (id, role_id, menu_id, assigned_at)
            values
                (?, ?, ?, ?)
            """)) {
            statement.setString(1, id);
            statement.setString(2, roleId);
            statement.setString(3, menuId);
            statement.setTimestamp(4, assignedAt);
            statement.executeUpdate();
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
