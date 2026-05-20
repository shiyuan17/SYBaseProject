package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class V17__reconcile_m1_permission_codes extends BaseJavaMigration {

    private static final List<PermissionSeed> M1_PERMISSIONS = List.of(
        new PermissionSeed("PERM_SYS_USER_QUERY", "PERM_SYS_USER_QUERY", "查询系统用户", "MENU_SYS_USERS", "QUERY", "GET", "/api/v1/system-users", "SYSTEM", 1,
            List.of("sys:user:query")),
        new PermissionSeed("PERM_SYS_USER_CREATE", "PERM_SYS_USER_CREATE", "维护系统用户", "MENU_SYS_USERS", "CREATE", "POST", "/api/v1/system-users", "SYSTEM", 2,
            List.of("sys:user:create")),
        new PermissionSeed("PERM_SYS_USER_UPDATE", "PERM_SYS_USER_UPDATE", "更新系统用户", "MENU_SYS_USERS", "UPDATE", "PATCH", "/api/v1/system-users", "SYSTEM", 3,
            List.of("sys:user:update")),
        new PermissionSeed("PERM_SYS_ROLE_QUERY", "PERM_SYS_ROLE_QUERY", "查询角色授权", "MENU_SYS_ROLES", "QUERY", "GET", "/api/v1/roles", "SYSTEM", 4,
            List.of("sys:role:query")),
        new PermissionSeed("PERM_SYS_ROLE_CREATE", "PERM_SYS_ROLE_CREATE", "维护角色", "MENU_SYS_ROLES", "CREATE", "POST", "/api/v1/roles", "SYSTEM", 5,
            List.of("sys:role:create")),
        new PermissionSeed("PERM_SYS_ROLE_ASSIGN", "PERM_SYS_ROLE_ASSIGN", "角色授权", "MENU_SYS_ROLES", "ASSIGN", "PUT", "/api/v1/roles/{id}/authorizations", "SYSTEM", 6,
            List.of("sys:role:auth-menu", "sys:role:auth-permission")),
        new PermissionSeed("PERM_SYS_BODY_PART_QUERY", "PERM_SYS_BODY_PART_QUERY", "查询部位字典", "MENU_BODY_PARTS", "QUERY", "GET", "/api/v1/body-parts", "MASTERDATA", 7,
            List.of("sys:body-part:query")),
        new PermissionSeed("PERM_SYS_BODY_PART_CREATE", "PERM_SYS_BODY_PART_CREATE", "维护部位字典", "MENU_BODY_PARTS", "CREATE", "POST", "/api/v1/body-parts", "MASTERDATA", 8,
            List.of("sys:body-part:create")),
        new PermissionSeed("PERM_SYS_ORDER_DICT_QUERY", "PERM_SYS_ORDER_DICT_QUERY", "查询医嘱字典", "MENU_ORDER_DICTS", "QUERY", "GET", "/api/v1/medical-order-dicts", "MASTERDATA", 9,
            List.of("sys:medical-order-dict:query")),
        new PermissionSeed("PERM_SYS_ORDER_DICT_CREATE", "PERM_SYS_ORDER_DICT_CREATE", "维护医嘱字典", "MENU_ORDER_DICTS", "CREATE", "POST", "/api/v1/medical-order-dicts", "MASTERDATA", 10,
            List.of("sys:medical-order-dict:create")),
        new PermissionSeed("PERM_SYS_ORDER_CHARGE_QUERY", "PERM_SYS_ORDER_CHARGE_QUERY", "查询医嘱收费", "MENU_ORDER_CHARGES", "QUERY", "GET", "/api/v1/medical-order-charge-items", "MASTERDATA", 11,
            List.of("sys:medical-order-charge:query")),
        new PermissionSeed("PERM_SYS_ORDER_CHARGE_CREATE", "PERM_SYS_ORDER_CHARGE_CREATE", "维护医嘱收费", "MENU_ORDER_CHARGES", "CREATE", "POST", "/api/v1/medical-order-charge-items", "MASTERDATA", 12,
            List.of("sys:medical-order-charge:create")),
        new PermissionSeed("PERM_SYS_PACKAGE_QUERY", "PERM_SYS_PACKAGE_QUERY", "查询医嘱套餐", "MENU_ORDER_PACKAGES", "QUERY", "GET", "/api/v1/medical-order-packages", "MASTERDATA", 13,
            List.of("sys:medical-order-package:query")),
        new PermissionSeed("PERM_SYS_PACKAGE_CREATE", "PERM_SYS_PACKAGE_CREATE", "维护医嘱套餐", "MENU_ORDER_PACKAGES", "CREATE", "POST", "/api/v1/medical-order-packages", "MASTERDATA", 14,
            List.of("sys:medical-order-package:create")),
        new PermissionSeed("PERM_SYS_TEMPLATE_QUERY", "PERM_SYS_TEMPLATE_QUERY", "查询描写模板", "MENU_TEMPLATES", "QUERY", "GET", "/api/v1/sampling-templates", "MASTERDATA", 15,
            List.of("sys:sampling-template:query")),
        new PermissionSeed("PERM_SYS_TEMPLATE_CREATE", "PERM_SYS_TEMPLATE_CREATE", "维护描写模板", "MENU_TEMPLATES", "CREATE", "POST", "/api/v1/sampling-templates", "MASTERDATA", 16,
            List.of("sys:sampling-template:create")),
        new PermissionSeed("PERM_SYS_GUIDELINE_QUERY", "PERM_SYS_GUIDELINE_QUERY", "查询取材规范", "MENU_GUIDELINES", "QUERY", "GET", "/api/v1/sampling-guidelines", "MASTERDATA", 17,
            List.of("sys:sampling-guideline:query")),
        new PermissionSeed("PERM_SYS_GUIDELINE_CREATE", "PERM_SYS_GUIDELINE_CREATE", "维护取材规范", "MENU_GUIDELINES", "CREATE", "POST", "/api/v1/sampling-guidelines", "MASTERDATA", 18,
            List.of("sys:sampling-guideline:create")),
        new PermissionSeed("PERM_SYS_CONFIG_QUERY", "PERM_SYS_CONFIG_QUERY", "查询系统配置", "MENU_CONFIGS", "QUERY", "GET", "/api/v1/system-configs", "SYSTEM", 19,
            List.of("sys:config:query")),
        new PermissionSeed("PERM_SYS_CONFIG_UPDATE", "PERM_SYS_CONFIG_UPDATE", "维护系统配置", "MENU_CONFIGS", "UPDATE", "PATCH", "/api/v1/system-configs", "SYSTEM", 20,
            List.of("sys:config:update")),
        new PermissionSeed("PERM_SYS_NUMBERING_QUERY", "PERM_SYS_NUMBERING_QUERY", "查询编号规则", "MENU_NUMBERING", "QUERY", "GET", "/api/v1/numbering-rules", "SYSTEM", 21,
            List.of("sys:numbering:query")),
        new PermissionSeed("PERM_SYS_NUMBERING_UPDATE", "PERM_SYS_NUMBERING_UPDATE", "维护编号规则", "MENU_NUMBERING", "UPDATE", "PATCH", "/api/v1/numbering-rules", "SYSTEM", 22,
            List.of("sys:numbering:update"))
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        List<String> resolvedPermissionIds = new ArrayList<>();
        for (PermissionSeed permission : M1_PERMISSIONS) {
            resolvedPermissionIds.add(reconcilePermission(connection, permission));
        }
        ensureRolePermissions(connection, "ROLE_PATHOLOGY_ADMIN", resolvedPermissionIds, "RP_ADMIN_M1_CANONICAL_");
    }

    private String reconcilePermission(Connection connection, PermissionSeed permission) throws SQLException {
        String canonicalId = findPermissionIdById(connection, permission.id());
        if (canonicalId != null) {
            copyLegacyRolePermissions(connection, permission.legacyCodes(), canonicalId);
            retireLegacyPermissionCodes(connection, permission.legacyCodes());
            updatePermissionById(connection, canonicalId, permission);
            return canonicalId;
        }

        String canonicalCodeId = findPermissionIdByCode(connection, permission.code());
        if (canonicalCodeId != null) {
            copyLegacyRolePermissions(connection, permission.legacyCodes(), canonicalCodeId);
            retireLegacyPermissionCodes(connection, permission.legacyCodes());
            updatePermissionById(connection, canonicalCodeId, permission);
            return canonicalCodeId;
        }

        String legacyId = findFirstPermissionIdByCodes(connection, permission.legacyCodes());
        if (legacyId != null) {
            updatePermissionById(connection, legacyId, permission);
            return legacyId;
        }

        insertPermission(connection, permission);
        return permission.id();
    }

    private void updatePermissionById(Connection connection, String id, PermissionSeed permission) throws SQLException {
        String menuId = resolveMenuId(connection, permission.menuId());
        try (PreparedStatement update = connection.prepareStatement("""
            update permissions
            set permission_code = ?, permission_name = ?, menu_id = ?, action_key = ?, http_method = ?,
                resource_path = ?, permission_group = ?, sort_order = ?, enabled = 1, updated_at = ?
            where id = ?
            """)) {
            update.setString(1, permission.code());
            update.setString(2, permission.name());
            update.setString(3, menuId);
            update.setString(4, permission.actionKey());
            update.setString(5, permission.httpMethod());
            update.setString(6, permission.resourcePath());
            update.setString(7, permission.permissionGroup());
            update.setInt(8, permission.sortOrder());
            update.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            update.setString(10, id);
            update.executeUpdate();
        }
    }

    private void insertPermission(Connection connection, PermissionSeed permission) throws SQLException {
        String menuId = resolveMenuId(connection, permission.menuId());
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into permissions
                (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path,
                 permission_group, sort_order, enabled, created_at, updated_at)
            values
                (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
            """)) {
            insert.setString(1, permission.id());
            insert.setString(2, permission.code());
            insert.setString(3, permission.name());
            insert.setString(4, menuId);
            insert.setString(5, permission.actionKey());
            insert.setString(6, permission.httpMethod());
            insert.setString(7, permission.resourcePath());
            insert.setString(8, permission.permissionGroup());
            insert.setInt(9, permission.sortOrder());
            insert.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            insert.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private String resolveMenuId(Connection connection, String preferredMenuId) throws SQLException {
        if (menuExists(connection, preferredMenuId)) {
            return preferredMenuId;
        }

        String legacyMenuId = legacyMenuId(preferredMenuId);
        if (legacyMenuId != null && menuExists(connection, legacyMenuId)) {
            return legacyMenuId;
        }

        if ("MENU_ORDER_PACKAGES".equals(preferredMenuId)) {
            return ensureMenu(connection, new MenuSeed(
                "MENU_ORDER_PACKAGES",
                findSystemRootMenuId(connection),
                "ORDER_PACKAGES",
                "医嘱套餐",
                "/system/medical-order-packages",
                "MedicalOrderPackages",
                "md:order-package",
                60
            ));
        }

        if ("MENU_NUMBERING".equals(preferredMenuId)) {
            return ensureMenu(connection, new MenuSeed(
                "MENU_NUMBERING",
                findSystemRootMenuId(connection),
                "NUMBERING_RULES",
                "编号规则",
                "/system/numbering-rules",
                "NumberingRules",
                "support:numbering",
                100
            ));
        }

        return preferredMenuId;
    }

    private String legacyMenuId(String preferredMenuId) {
        return switch (preferredMenuId) {
            case "MENU_BODY_PARTS" -> "MENU_SYS_BODY_PART";
            case "MENU_CONFIGS" -> "MENU_SYS_CONFIG";
            case "MENU_GUIDELINES" -> "MENU_SYS_GUIDELINE";
            case "MENU_ORDER_CHARGES" -> "MENU_SYS_ORDER_CHARGE";
            case "MENU_ORDER_DICTS" -> "MENU_SYS_ORDER_DICT";
            case "MENU_SYS_ROLES" -> "MENU_SYS_ROLE";
            case "MENU_SYS_USERS" -> "MENU_SYS_USER";
            case "MENU_TEMPLATES" -> "MENU_SYS_TEMPLATE";
            default -> null;
        };
    }

    private String findSystemRootMenuId(Connection connection) throws SQLException {
        if (menuExists(connection, "MENU_SYSTEM")) {
            return "MENU_SYSTEM";
        }
        if (menuExists(connection, "MENU_SYS_ROOT")) {
            return "MENU_SYS_ROOT";
        }
        return null;
    }

    private String ensureMenu(Connection connection, MenuSeed menu) throws SQLException {
        if (menuExists(connection, menu.id())) {
            return menu.id();
        }

        try (PreparedStatement insert = connection.prepareStatement("""
            insert into menus
                (id, parent_id, menu_code, menu_name, menu_type, path, component_name, icon,
                 sort_order, visible, enabled, created_at, updated_at)
            values
                (?, ?, ?, ?, 'MENU', ?, ?, ?, ?, 1, 1, ?, ?)
            """)) {
            insert.setString(1, menu.id());
            insert.setString(2, menu.parentId());
            insert.setString(3, menu.menuCode());
            insert.setString(4, menu.menuName());
            insert.setString(5, menu.path());
            insert.setString(6, menu.componentName());
            insert.setString(7, menu.icon());
            insert.setInt(8, menu.sortOrder());
            insert.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            insert.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }

        return menu.id();
    }

    private boolean menuExists(Connection connection, String menuId) throws SQLException {
        if (menuId == null) {
            return false;
        }

        try (PreparedStatement query = connection.prepareStatement("select 1 from menus where id = ?")) {
            query.setString(1, menuId);
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void copyLegacyRolePermissions(Connection connection, List<String> legacyCodes, String targetPermissionId) throws SQLException {
        for (String legacyCode : legacyCodes) {
            List<String> legacyRoleIds = new ArrayList<>();
            try (PreparedStatement query = connection.prepareStatement("""
                select distinct role_permissions.role_id
                from role_permissions
                join permissions on permissions.id = role_permissions.permission_id
                where permissions.permission_code = ?
                  and not exists (
                      select 1
                      from role_permissions existing
                      where existing.role_id = role_permissions.role_id
                        and existing.permission_id = ?
                  )
                """)) {
                query.setString(1, legacyCode);
                query.setString(2, targetPermissionId);
                try (ResultSet resultSet = query.executeQuery()) {
                    while (resultSet.next()) {
                        legacyRoleIds.add(resultSet.getString(1));
                    }
                }
            }

            for (String roleId : legacyRoleIds) {
                try (PreparedStatement insert = connection.prepareStatement("""
                    insert into role_permissions (id, role_id, permission_id, assigned_at)
                    values (?, ?, ?, ?)
                    """)) {
                    insert.setString(1, "RP_M1C_" + UUID.randomUUID().toString().replace("-", ""));
                    insert.setString(2, roleId);
                    insert.setString(3, targetPermissionId);
                    insert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                    insert.executeUpdate();
                }
            }
        }
    }

    private void retireLegacyPermissionCodes(Connection connection, List<String> legacyCodes) throws SQLException {
        for (String legacyCode : legacyCodes) {
            List<String> legacyIds = new ArrayList<>();
            try (PreparedStatement query = connection.prepareStatement("""
                select id from permissions where permission_code = ?
                """)) {
                query.setString(1, legacyCode);
                try (ResultSet resultSet = query.executeQuery()) {
                    while (resultSet.next()) {
                        legacyIds.add(resultSet.getString(1));
                    }
                }
            }

            int index = 1;
            for (String legacyId : legacyIds) {
                String retiredActionKey = "LEGACY_" + Math.abs(legacyId.hashCode()) + "_" + index++;
                if (retiredActionKey.length() > 64) {
                    retiredActionKey = retiredActionKey.substring(0, 64);
                }
                retireLegacyPermissionById(connection, legacyId, retiredActionKey);
            }
        }
    }

    private void retireLegacyPermissionById(Connection connection, String legacyId, String retiredActionKey) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
            update permissions
            set enabled = 0, action_key = ?, updated_at = ?
            where id = ?
            """)) {
            update.setString(1, retiredActionKey);
            update.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            update.setString(3, legacyId);
            update.executeUpdate();
        } catch (SQLException ignored) {
            try (PreparedStatement update = connection.prepareStatement("""
                update permissions
                set enabled = 0, updated_at = ?
                where id = ?
                """)) {
                update.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                update.setString(2, legacyId);
                update.executeUpdate();
            }
        }
    }

    private void ensureRolePermissions(Connection connection, String roleId, List<String> permissionIds, String idPrefix) throws SQLException {
        int index = 1;
        for (String permissionId : permissionIds) {
            if (rolePermissionExists(connection, roleId, permissionId)) {
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

    private boolean rolePermissionExists(Connection connection, String roleId, String permissionId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("""
            select 1 from role_permissions where role_id = ? and permission_id = ?
            """)) {
            query.setString(1, roleId);
            query.setString(2, permissionId);
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private String findPermissionIdById(Connection connection, String id) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("select id from permissions where id = ?")) {
            query.setString(1, id);
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    private String findPermissionIdByCode(Connection connection, String code) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("select id from permissions where permission_code = ?")) {
            query.setString(1, code);
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    private String findFirstPermissionIdByCodes(Connection connection, List<String> codes) throws SQLException {
        for (String code : codes) {
            String id = findPermissionIdByCode(connection, code);
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    private record PermissionSeed(
        String id,
        String code,
        String name,
        String menuId,
        String actionKey,
        String httpMethod,
        String resourcePath,
        String permissionGroup,
        int sortOrder,
        List<String> legacyCodes
    ) {
    }

    private record MenuSeed(
        String id,
        String parentId,
        String menuCode,
        String menuName,
        String path,
        String componentName,
        String icon,
        int sortOrder
    ) {
    }
}
