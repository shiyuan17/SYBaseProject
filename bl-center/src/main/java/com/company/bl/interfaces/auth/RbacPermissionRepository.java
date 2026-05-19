package com.company.bl.interfaces.auth;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class RbacPermissionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RbacPermissionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean hasPermission(String userId, String permissionCode) {
        Long count = jdbcTemplate.queryForObject("""
            select count(1)
            from users u
            join user_roles ur on ur.user_id = u.id
            join roles r on r.id = ur.role_id
            join role_permissions rp on rp.role_id = r.id
            join permissions p on p.id = rp.permission_id
            where u.id = :userId
              and u.enabled = 1
              and r.enabled = 1
              and p.enabled = 1
              and p.permission_code = :permissionCode
            """, Map.of(
            "userId", userId,
            "permissionCode", permissionCode
        ), Long.class);
        return count != null && count > 0;
    }
}
