package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class V43__rename_m2_workflow_menus extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        updateMenuName(connection, "MENU_M2_WORKFLOW", "临床送检", now);
        updateMenuName(connection, "MENU_M2_APPLICATION_LIST", "申请管理", now);
        updateMenuName(connection, "MENU_M2_CLINICAL", "标本管理", now);
    }

    private void updateMenuName(Connection connection, String menuId, String menuName, Timestamp updatedAt)
        throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            update menus
            set menu_name = ?, updated_at = ?
            where id = ?
            """)) {
            statement.setString(1, menuName);
            statement.setTimestamp(2, updatedAt);
            statement.setString(3, menuId);
            statement.executeUpdate();
        }
    }
}
