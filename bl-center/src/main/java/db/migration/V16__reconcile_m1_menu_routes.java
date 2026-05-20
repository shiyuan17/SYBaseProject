package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class V16__reconcile_m1_menu_routes extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        updateMenuIfExists(connection, "MENU_SYSTEM", "/system", "SystemRoot", true, true);
        updateMenuIfExists(connection, "MENU_SYS_USERS", "/system/users", "SystemUsers", true, true);
        updateMenuIfExists(connection, "MENU_SYS_ROLES", "/system/roles", "Roles", true, true);
        updateMenuIfExists(connection, "MENU_BODY_PARTS", "/system/body-parts", "BodyParts", true, true);
        updateMenuIfExists(connection, "MENU_ORDER_DICTS", "/system/medical-order-dicts", "MedicalOrderDicts", true, true);
        updateMenuIfExists(connection, "MENU_ORDER_CHARGES", "/system/medical-order-charges", "MedicalOrderCharges", true, true);
        updateMenuIfExists(connection, "MENU_ORDER_PACKAGES", "/system/medical-order-packages", "MedicalOrderPackages", true, true);
        updateMenuIfExists(connection, "MENU_TEMPLATES", "/system/sampling-templates", "SamplingTemplates", true, true);
        updateMenuIfExists(connection, "MENU_GUIDELINES", "/system/sampling-guidelines", "SamplingGuidelines", true, true);
        updateMenuIfExists(connection, "MENU_CONFIGS", "/system/configs", "SystemConfigs", true, true);
        updateMenuIfExists(connection, "MENU_NUMBERING", "/system/numbering-rules", "NumberingRules", true, true);

        updateMenuIfExists(connection, "MENU_SYS_ROOT", "/system", "SystemRoot", true, true);
        updateMenuIfExists(connection, "MENU_SYS_USER", "/system/users", "SystemUsers", true, true);
        updateMenuIfExists(connection, "MENU_SYS_ROLE", "/system/roles", "Roles", true, true);
        updateMenuIfExists(connection, "MENU_SYS_TEMPLATE", "/system/sampling-templates", "SamplingTemplates", true, true);
        updateMenuIfExists(connection, "MENU_SYS_GUIDELINE", "/system/sampling-guidelines", "SamplingGuidelines", true, true);
        updateMenuIfExists(connection, "MENU_SYS_BODY_PART", "/system/body-parts", "BodyParts", true, true);
        updateMenuIfExists(connection, "MENU_SYS_ORDER_DICT", "/system/medical-order-dicts", "MedicalOrderDicts", true, true);
        updateMenuIfExists(connection, "MENU_SYS_ORDER_CHARGE", "/system/medical-order-charges", "MedicalOrderCharges", true, true);
        updateMenuIfExists(connection, "MENU_SYS_CONFIG", "/system/configs", "SystemConfigs", true, true);

        updateMenuIfExists(connection, "MENU_SYS_DEPT", "/system/departments", "system/department/index", false, false);
        updateMenuIfExists(connection, "MENU_SYS_LOGIN_LOG", "/system/login-logs", "system/login-log/index", false, false);
        updateMenuIfExists(connection, "MENU_SYS_OPERATION_LOG", "/system/operation-logs", "system/operation-log/index", false, false);
    }

    private void updateMenuIfExists(Connection connection,
                                    String id,
                                    String path,
                                    String componentName,
                                    boolean visible,
                                    boolean enabled) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
            update menus
            set path = ?,
                component_name = ?,
                visible = ?,
                enabled = ?,
                updated_at = ?
            where id = ?
            """)) {
            update.setString(1, path);
            update.setString(2, componentName);
            update.setInt(3, visible ? 1 : 0);
            update.setInt(4, enabled ? 1 : 0);
            update.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            update.setString(6, id);
            update.executeUpdate();
        }
    }
}
