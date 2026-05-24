package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class V46__consolidate_m2_clinical_submission_menus extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        updateVisibleMenu(
            connection,
            "MENU_M2_APPLICATION_LIST",
            "M2_APPLICATION_LIST",
            "申请与登记",
            "/workflow/submission-registration",
            "SubmissionRegistration",
            "m2:application-list",
            111,
            now
        );
        updateVisibleMenu(
            connection,
            "MENU_M2_FIXATION",
            "M2_FIXATION",
            "固定与转运",
            "/workflow/fixation-transport",
            "FixationTransport",
            "m2:fixation",
            112,
            now
        );
        updateVisibleMenu(
            connection,
            "MENU_M2_RECEIPT",
            "M2_RECEIPT",
            "病理接收",
            "/workflow/pathology-receipt",
            "PathologyReceipt",
            "m2:receipt",
            113,
            now
        );
        updateVisibleMenu(
            connection,
            "MENU_M2_TRACKING",
            "M2_TRACKING",
            "追踪与异常",
            "/workflow/tracking-exception",
            "TrackingException",
            "m2:tracking",
            114,
            now
        );

        hideLegacyMenu(connection, "MENU_M2_CLINICAL", "/workflow/clinical-register", "ClinicalRegister", now);
        hideLegacyMenu(connection, "MENU_M2_TRANSPORT", "/workflow/transport-handover", "TransportHandover", now);

        ensureRoleMenu(connection, "RM_M2_SUBMISSION_REGISTER", "ROLE_M2_CLINICAL_REGISTER", "MENU_M2_APPLICATION_LIST");
        ensureRoleMenu(connection, "RM_M2_SUBMISSION_IMPORT", "ROLE_M2_CLINICAL_IMPORT", "MENU_M2_APPLICATION_LIST");
        ensureRoleMenu(connection, "RM_M2_FIXATION_TRANSPORT", "ROLE_M2_TRANSPORT_HANDOVER", "MENU_M2_FIXATION");
    }

    private void updateVisibleMenu(
        Connection connection,
        String menuId,
        String menuCode,
        String menuName,
        String path,
        String componentName,
        String permissionPrefix,
        int sortOrder,
        Timestamp updatedAt
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            update menus
            set parent_id = 'MENU_M2_WORKFLOW',
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
            statement.setString(1, menuCode);
            statement.setString(2, menuName);
            statement.setString(3, path);
            statement.setString(4, componentName);
            statement.setString(5, permissionPrefix);
            statement.setInt(6, sortOrder);
            statement.setTimestamp(7, updatedAt);
            statement.setString(8, menuId);
            statement.executeUpdate();
        }
    }

    private void hideLegacyMenu(
        Connection connection,
        String menuId,
        String path,
        String componentName,
        Timestamp updatedAt
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            update menus
            set path = ?,
                component_name = ?,
                visible = 0,
                enabled = 1,
                updated_at = ?
            where id = ?
            """)) {
            statement.setString(1, path);
            statement.setString(2, componentName);
            statement.setTimestamp(3, updatedAt);
            statement.setString(4, menuId);
            statement.executeUpdate();
        }
    }

    private void ensureRoleMenu(Connection connection, String id, String roleId, String menuId) throws SQLException {
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
            statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
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
