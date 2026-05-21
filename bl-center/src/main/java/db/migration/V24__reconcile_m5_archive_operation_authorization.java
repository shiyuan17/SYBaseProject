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

public class V24__reconcile_m5_archive_operation_authorization extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        upsertMenu(connection, "MENU_M5_SUPPORT", null, "M5_SUPPORT", "M5 Support", "DIRECTORY",
            "/operation-support", "OperationSupportRoot", "m5", 160);
        upsertMenu(connection, "MENU_M5_ARCHIVE", "MENU_M5_SUPPORT", "M5_ARCHIVE", "Archive Management", "MENU",
            "/api/v1/archive-records/search", "ArchiveManagement", "m5:archive", 161);
        upsertMenu(connection, "MENU_M5_REAGENT", "MENU_M5_SUPPORT", "M5_REAGENT", "Reagent Ledger", "MENU",
            "/api/v1/reagents", "ReagentLedger", "m5:reagent", 162);
        upsertMenu(connection, "MENU_M5_EQUIPMENT", "MENU_M5_SUPPORT", "M5_EQUIPMENT", "Equipment Ledger", "MENU",
            "/api/v1/equipment-records", "EquipmentLedger", "m5:equipment", 163);

        upsertPermission(connection, "PERM_M5_ARCHIVE_CABINET_QUERY", "PERM_M5_ARCHIVE_CABINET_QUERY", "Archive cabinet query",
            "MENU_M5_ARCHIVE", "ARCHIVE_CABINET_QUERY", "GET", "/api/v1/archive-cabinets", "M5", 161);
        upsertPermission(connection, "PERM_M5_ARCHIVE_CABINET_CREATE", "PERM_M5_ARCHIVE_CABINET_CREATE", "Archive cabinet create",
            "MENU_M5_ARCHIVE", "ARCHIVE_CABINET_CREATE", "POST", "/api/v1/archive-cabinets", "M5", 162);
        upsertPermission(connection, "PERM_M5_ARCHIVE_CABINET_UPDATE", "PERM_M5_ARCHIVE_CABINET_UPDATE", "Archive cabinet update",
            "MENU_M5_ARCHIVE", "ARCHIVE_CABINET_UPDATE", "PATCH", "/api/v1/archive-cabinets/{id}", "M5", 163);
        upsertPermission(connection, "PERM_M5_APPLICATION_FORM_ARCHIVE", "PERM_M5_APPLICATION_FORM_ARCHIVE", "Application form archive",
            "MENU_M5_ARCHIVE", "APPLICATION_FORM_ARCHIVE", "POST", "/api/v1/archive/application-forms", "M5", 164);
        upsertPermission(connection, "PERM_M5_EMBEDDING_BOX_ARCHIVE", "PERM_M5_EMBEDDING_BOX_ARCHIVE", "Embedding box archive",
            "MENU_M5_ARCHIVE", "EMBEDDING_BOX_ARCHIVE", "POST", "/api/v1/archive/embedding-boxes", "M5", 165);
        upsertPermission(connection, "PERM_M5_SLIDE_ARCHIVE", "PERM_M5_SLIDE_ARCHIVE", "Slide archive",
            "MENU_M5_ARCHIVE", "SLIDE_ARCHIVE", "POST", "/api/v1/archive/slides", "M5", 166);
        upsertPermission(connection, "PERM_M5_ARCHIVE_QUERY", "PERM_M5_ARCHIVE_QUERY", "Archive record query",
            "MENU_M5_ARCHIVE", "ARCHIVE_RECORD_QUERY", "GET", "/api/v1/archive-records/search", "M5", 167);
        upsertPermission(connection, "PERM_M5_LOAN_CREATE", "PERM_M5_LOAN_CREATE", "Material loan create",
            "MENU_M5_ARCHIVE", "LOAN_CREATE", "POST", "/api/v1/material-loans", "M5", 168);
        upsertPermission(connection, "PERM_M5_LOAN_RETURN", "PERM_M5_LOAN_RETURN", "Material loan return",
            "MENU_M5_ARCHIVE", "LOAN_RETURN", "POST", "/api/v1/material-loans/{id}/return", "M5", 169);
        upsertPermission(connection, "PERM_M5_LOAN_QUERY", "PERM_M5_LOAN_QUERY", "Material loan query",
            "MENU_M5_ARCHIVE", "LOAN_QUERY", "GET", "/api/v1/material-loans/pending", "M5", 170);
        upsertPermission(connection, "PERM_M5_REAGENT_QUERY", "PERM_M5_REAGENT_QUERY", "Reagent query",
            "MENU_M5_REAGENT", "REAGENT_QUERY", "GET", "/api/v1/reagents", "M5", 171);
        upsertPermission(connection, "PERM_M5_REAGENT_CREATE", "PERM_M5_REAGENT_CREATE", "Reagent create",
            "MENU_M5_REAGENT", "REAGENT_CREATE", "POST", "/api/v1/reagents", "M5", 172);
        upsertPermission(connection, "PERM_M5_REAGENT_UPDATE", "PERM_M5_REAGENT_UPDATE", "Reagent update",
            "MENU_M5_REAGENT", "REAGENT_UPDATE", "PATCH", "/api/v1/reagents/{id}", "M5", 173);
        upsertPermission(connection, "PERM_M5_REAGENT_STOCK_QUERY", "PERM_M5_REAGENT_STOCK_QUERY", "Reagent stock query",
            "MENU_M5_REAGENT", "REAGENT_STOCK_QUERY", "GET", "/api/v1/reagent-stocks", "M5", 174);
        upsertPermission(connection, "PERM_M5_REAGENT_STOCK_UPDATE", "PERM_M5_REAGENT_STOCK_UPDATE", "Reagent stock update",
            "MENU_M5_REAGENT", "REAGENT_STOCK_UPDATE", "PATCH", "/api/v1/reagent-stocks/{id}", "M5", 175);
        upsertPermission(connection, "PERM_M5_REAGENT_WARNING_QUERY", "PERM_M5_REAGENT_WARNING_QUERY", "Reagent warning query",
            "MENU_M5_REAGENT", "REAGENT_WARNING_QUERY", "GET", "/api/v1/reagent-stocks/warnings", "M5", 176);
        upsertPermission(connection, "PERM_M5_EQUIPMENT_QUERY", "PERM_M5_EQUIPMENT_QUERY", "Equipment query",
            "MENU_M5_EQUIPMENT", "EQUIPMENT_QUERY", "GET", "/api/v1/equipment-records", "M5", 177);
        upsertPermission(connection, "PERM_M5_EQUIPMENT_CREATE", "PERM_M5_EQUIPMENT_CREATE", "Equipment create",
            "MENU_M5_EQUIPMENT", "EQUIPMENT_CREATE", "POST", "/api/v1/equipment-records", "M5", 178);
        upsertPermission(connection, "PERM_M5_EQUIPMENT_UPDATE", "PERM_M5_EQUIPMENT_UPDATE", "Equipment update",
            "MENU_M5_EQUIPMENT", "EQUIPMENT_UPDATE", "PATCH", "/api/v1/equipment-records/{id}", "M5", 179);
        upsertPermission(connection, "PERM_M5_EQUIPMENT_MAINTENANCE_CREATE", "PERM_M5_EQUIPMENT_MAINTENANCE_CREATE", "Equipment maintenance create",
            "MENU_M5_EQUIPMENT", "EQUIPMENT_MAINTENANCE_CREATE", "POST", "/api/v1/equipment-records/{id}/maintenance-logs", "M5", 180);
        upsertPermission(connection, "PERM_M5_EQUIPMENT_WARNING_QUERY", "PERM_M5_EQUIPMENT_WARNING_QUERY", "Equipment warning query",
            "MENU_M5_EQUIPMENT", "EQUIPMENT_WARNING_QUERY", "GET", "/api/v1/equipment-records/warnings", "M5", 181);

        ensureRolePermissions(connection, "ROLE_PATHOLOGY_ADMIN", List.of(
            "PERM_M5_ARCHIVE_CABINET_QUERY", "PERM_M5_ARCHIVE_CABINET_CREATE", "PERM_M5_ARCHIVE_CABINET_UPDATE",
            "PERM_M5_APPLICATION_FORM_ARCHIVE", "PERM_M5_EMBEDDING_BOX_ARCHIVE", "PERM_M5_SLIDE_ARCHIVE",
            "PERM_M5_ARCHIVE_QUERY", "PERM_M5_LOAN_CREATE", "PERM_M5_LOAN_RETURN", "PERM_M5_LOAN_QUERY",
            "PERM_M5_REAGENT_QUERY", "PERM_M5_REAGENT_CREATE", "PERM_M5_REAGENT_UPDATE",
            "PERM_M5_REAGENT_STOCK_QUERY", "PERM_M5_REAGENT_STOCK_UPDATE", "PERM_M5_REAGENT_WARNING_QUERY",
            "PERM_M5_EQUIPMENT_QUERY", "PERM_M5_EQUIPMENT_CREATE", "PERM_M5_EQUIPMENT_UPDATE",
            "PERM_M5_EQUIPMENT_MAINTENANCE_CREATE", "PERM_M5_EQUIPMENT_WARNING_QUERY"),
            "RP_M5_ADMIN_REC_");
        ensureRolePermissions(connection, "ROLE_ARCHIVE_MANAGER", List.of(
            "PERM_M5_ARCHIVE_CABINET_QUERY", "PERM_M5_ARCHIVE_CABINET_CREATE", "PERM_M5_ARCHIVE_CABINET_UPDATE",
            "PERM_M5_APPLICATION_FORM_ARCHIVE", "PERM_M5_EMBEDDING_BOX_ARCHIVE", "PERM_M5_SLIDE_ARCHIVE",
            "PERM_M5_ARCHIVE_QUERY", "PERM_M5_LOAN_CREATE", "PERM_M5_LOAN_RETURN", "PERM_M5_LOAN_QUERY"),
            "RP_M5_ARCHIVE_REC_");
        ensureRolePermissions(connection, "ROLE_REAGENT_DEVICE_MANAGER", List.of(
            "PERM_M5_REAGENT_QUERY", "PERM_M5_REAGENT_CREATE", "PERM_M5_REAGENT_UPDATE",
            "PERM_M5_REAGENT_STOCK_QUERY", "PERM_M5_REAGENT_STOCK_UPDATE", "PERM_M5_REAGENT_WARNING_QUERY",
            "PERM_M5_EQUIPMENT_QUERY", "PERM_M5_EQUIPMENT_CREATE", "PERM_M5_EQUIPMENT_UPDATE",
            "PERM_M5_EQUIPMENT_MAINTENANCE_CREATE", "PERM_M5_EQUIPMENT_WARNING_QUERY"),
            "RP_M5_REAGENT_REC_");
    }

    private void upsertMenu(Connection connection, String id, String parentId, String menuCode, String menuName, String menuType,
                            String path, String componentName, String permissionPrefix, int sortOrder) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
            update menus
            set parent_id = ?, menu_code = ?, menu_name = ?, menu_type = ?, path = ?, component_name = ?,
                permission_prefix = ?, sort_order = ?, updated_at = ?
            where id = ?
            """)) {
            update.setString(1, parentId);
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
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            insert.setString(1, id);
            insert.setString(2, parentId);
            insert.setString(3, menuCode);
            insert.setString(4, menuName);
            insert.setString(5, menuType);
            insert.setString(6, path);
            insert.setString(7, componentName);
            insert.setString(8, permissionPrefix);
            insert.setInt(9, sortOrder);
            insert.setTimestamp(10, now);
            insert.setTimestamp(11, now);
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
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            insert.setString(1, id);
            insert.setString(2, code);
            insert.setString(3, name);
            insert.setString(4, menuId);
            insert.setString(5, actionKey);
            insert.setString(6, httpMethod);
            insert.setString(7, resourcePath);
            insert.setString(8, permissionGroup);
            insert.setInt(9, sortOrder);
            insert.setTimestamp(10, now);
            insert.setTimestamp(11, now);
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

    private boolean exists(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next();
        }
    }
}
