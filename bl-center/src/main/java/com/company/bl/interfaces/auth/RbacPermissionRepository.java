package com.company.bl.interfaces.auth;

import com.company.common.security.authorization.MenuEntryPermissionResolver;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class RbacPermissionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RbacPermissionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean hasPermission(String userId, String permissionCode) {
        Set<String> effectivePermissionCodes = MenuEntryPermissionResolver.resolveEffectivePermissionCodes(
            findExplicitPermissionCodes(userId),
            findGrantedMenuPermissions(userId));
        return effectivePermissionCodes.contains(permissionCode);
    }

    public boolean isWorkbenchOverrideAllowed(String userId) {
        if (!hasPermission(userId, M4PermissionCodes.WORKBENCH_QUERY)) {
            return false;
        }
        Integer matchingRole = jdbcTemplate.queryForObject("""
            select case when exists (
                select 1
                from users u
                join user_roles ur on ur.user_id = u.id
                join roles r on r.id = ur.role_id
                where u.id = :userId
                  and u.enabled = 1
                  and r.enabled = 1
                  and (r.role_code = 'PATHOLOGY_ADMIN' or r.role_code like 'M4_WORKBENCH%')
            ) then 1 else 0 end
            """, Map.of("userId", userId), Integer.class);
        return matchingRole != null && matchingRole == 1;
    }

    public boolean hasAnyPermission(String userId, String[] permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return true;
        }
        Set<String> effectivePermissionCodes = MenuEntryPermissionResolver.resolveEffectivePermissionCodes(
            findExplicitPermissionCodes(userId),
            findGrantedMenuPermissions(userId));
        for (String permissionCode : permissionCodes) {
            if (effectivePermissionCodes.contains(permissionCode)) {
                return true;
            }
        }
        return false;
    }

    public String findPrimaryRoleCode(String userId) {
        return jdbcTemplate.query("""
            select role
            from users
            where id = :userId
              and enabled = 1
            """, Map.of("userId", userId), rs -> rs.next() ? rs.getString(1) : null);
    }

    private List<String> findExplicitPermissionCodes(String userId) {
        return jdbcTemplate.query("""
            select distinct p.permission_code
            from users u
            join user_roles ur on ur.user_id = u.id
            join roles r on r.id = ur.role_id
            join role_permissions rp on rp.role_id = r.id
            join permissions p on p.id = rp.permission_id
            where u.id = :userId
              and u.enabled = 1
              and r.enabled = 1
              and p.enabled = 1
            order by p.permission_code
            """, Map.of("userId", userId), (rs, rowNum) -> rs.getString(1));
    }

    private List<MenuEntryPermissionResolver.MenuPermissionBinding> findGrantedMenuPermissions(String userId) {
        return jdbcTemplate.query("""
            select distinct m.id as menu_id,
                   m.menu_type,
                   p.id as permission_id,
                   p.permission_code,
                   p.action_key,
                   p.sort_order,
                   m.enabled as menu_enabled,
                   p.enabled as permission_enabled
            from users u
            join user_roles ur on ur.user_id = u.id
            join roles r on r.id = ur.role_id
            join role_menus rm on rm.role_id = r.id
            join menus m on m.id = rm.menu_id
            join permissions p on p.menu_id = m.id
            where u.id = :userId
              and u.enabled = 1
              and r.enabled = 1
            """, Map.of("userId", userId), (rs, rowNum) ->
            new MenuEntryPermissionResolver.MenuPermissionBinding(
                rs.getString("menu_id"),
                rs.getString("menu_type"),
                rs.getString("permission_id"),
                rs.getString("permission_code"),
                rs.getString("action_key"),
                rs.getInt("sort_order"),
                rs.getInt("menu_enabled") == 1,
                rs.getInt("permission_enabled") == 1));
    }
}
