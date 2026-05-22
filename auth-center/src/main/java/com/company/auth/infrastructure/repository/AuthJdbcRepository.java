package com.company.auth.infrastructure.repository;

import com.company.common.security.authorization.MenuEntryPermissionResolver;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class AuthJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AuthJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AuthUserRow findUserById(String userId) {
        List<AuthUserRow> rows = jdbcTemplate.query("""
            select id, login_name, name, password, password_algo, password_salt, avatar, enabled
            from users
            where id = :userId
            """, Map.of("userId", userId), this::mapUser);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public AuthUserRow findUserByLoginName(String loginName) {
        List<AuthUserRow> rows = jdbcTemplate.query("""
            select id, login_name, name, password, password_algo, password_salt, avatar, enabled
            from users
            where login_name = :loginName
            """, Map.of("loginName", loginName), this::mapUser);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<String> findRoleCodes(String userId) {
        return jdbcTemplate.query("""
            select distinct roles.role_code
            from user_roles
            join roles on roles.id = user_roles.role_id
            where user_roles.user_id = :userId
              and roles.enabled = 1
            order by roles.role_code
            """, Map.of("userId", userId), (rs, rowNum) -> rs.getString(1));
    }

    public List<String> findAccessCodes(String userId) {
        List<String> explicitPermissionCodes = jdbcTemplate.query("""
            select distinct permissions.permission_code
            from user_roles
            join roles on roles.id = user_roles.role_id
            join role_permissions on role_permissions.role_id = roles.id
            join permissions on permissions.id = role_permissions.permission_id
            where user_roles.user_id = :userId
              and roles.enabled = 1
              and permissions.enabled = 1
            order by permissions.permission_code
            """, Map.of("userId", userId), (rs, rowNum) -> rs.getString(1));
        Set<String> effectivePermissionCodes = MenuEntryPermissionResolver.resolveEffectivePermissionCodes(
            explicitPermissionCodes,
            findGrantedMenuPermissions(userId));
        return effectivePermissionCodes.stream().toList();
    }

    public void updatePassword(String userId, String password, String passwordAlgo, String passwordSalt) {
        jdbcTemplate.update("""
            update users
            set password = :password,
                password_algo = :passwordAlgo,
                password_salt = :passwordSalt,
                updated_at = :updatedAt
            where id = :userId
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("password", password)
            .addValue("passwordAlgo", passwordAlgo)
            .addValue("passwordSalt", passwordSalt)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void saveAccessToken(AccessTokenRow tokenRow) {
        jdbcTemplate.update("""
            insert into auth_access_tokens
                (jti, user_id, issued_at, expires_at, revoked_at, client_ip, client_device)
            values
                (:jti, :userId, :issuedAt, :expiresAt, :revokedAt, :clientIp, :clientDevice)
            """, new MapSqlParameterSource()
            .addValue("jti", tokenRow.jti())
            .addValue("userId", tokenRow.userId())
            .addValue("issuedAt", tokenRow.issuedAt())
            .addValue("expiresAt", tokenRow.expiresAt())
            .addValue("revokedAt", tokenRow.revokedAt())
            .addValue("clientIp", tokenRow.clientIp())
            .addValue("clientDevice", tokenRow.clientDevice()));
    }

    public void revokeAccessToken(String tokenId) {
        jdbcTemplate.update("""
            update auth_access_tokens
            set revoked_at = :revokedAt
            where jti = :tokenId
              and revoked_at is null
            """, new MapSqlParameterSource()
            .addValue("tokenId", tokenId)
            .addValue("revokedAt", LocalDateTime.now()));
    }

    public boolean isAccessTokenActive(String tokenId, String userId, Instant expiresAt) {
        Long count = jdbcTemplate.queryForObject("""
            select count(*)
            from auth_access_tokens
            where jti = :tokenId
              and user_id = :userId
              and revoked_at is null
              and expires_at >= :expiresAt
            """, new MapSqlParameterSource()
            .addValue("tokenId", tokenId)
            .addValue("userId", userId)
            .addValue("expiresAt", LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC)), Long.class);
        return count != null && count > 0;
    }

    public void updateLastLogin(String userId, String clientIp, String clientDevice, LocalDateTime loginAt) {
        jdbcTemplate.update("""
            update users
            set last_login_at = :loginAt,
                last_login_ip = :clientIp,
                last_login_device = :clientDevice,
                updated_at = :updatedAt
            where id = :userId
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("loginAt", loginAt)
            .addValue("clientIp", clientIp)
            .addValue("clientDevice", clientDevice)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void recordLogin(LoginLogRow logRow) {
        jdbcTemplate.update("""
            insert into user_login_logs
                (id, user_id, login_name, login_result, client_ip, client_device, login_at, failure_reason, remarks)
            values
                (:id, :userId, :loginName, :loginResult, :clientIp, :clientDevice, :loginAt, :failureReason, :remarks)
            """, new MapSqlParameterSource()
            .addValue("id", logRow.id())
            .addValue("userId", logRow.userId())
            .addValue("loginName", logRow.loginName())
            .addValue("loginResult", logRow.loginResult())
            .addValue("clientIp", logRow.clientIp())
            .addValue("clientDevice", logRow.clientDevice())
            .addValue("loginAt", logRow.loginAt())
            .addValue("failureReason", logRow.failureReason())
            .addValue("remarks", logRow.remarks()));
    }

    private AuthUserRow mapUser(ResultSet rs, int rowNum) throws SQLException {
        return new AuthUserRow(
            rs.getString("id"),
            rs.getString("login_name"),
            rs.getString("name"),
            rs.getString("password"),
            rs.getString("password_algo"),
            rs.getString("password_salt"),
            rs.getString("avatar"),
            rs.getInt("enabled") == 1);
    }

    private List<MenuEntryPermissionResolver.MenuPermissionBinding> findGrantedMenuPermissions(String userId) {
        return jdbcTemplate.query("""
            select distinct menus.id as menu_id,
                   menus.menu_type,
                   permissions.id as permission_id,
                   permissions.permission_code,
                   permissions.action_key,
                   permissions.sort_order,
                   menus.enabled as menu_enabled,
                   permissions.enabled as permission_enabled
            from user_roles
            join roles on roles.id = user_roles.role_id
            join role_menus on role_menus.role_id = roles.id
            join menus on menus.id = role_menus.menu_id
            join permissions on permissions.menu_id = menus.id
            where user_roles.user_id = :userId
              and roles.enabled = 1
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

    public record AuthUserRow(
        String id,
        String loginName,
        String name,
        String password,
        String passwordAlgo,
        String passwordSalt,
        String avatar,
        boolean enabled
    ) {
    }

    public record AccessTokenRow(
        String jti,
        String userId,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt,
        String clientIp,
        String clientDevice
    ) {
    }

    public record LoginLogRow(
        String id,
        String userId,
        String loginName,
        String loginResult,
        String clientIp,
        String clientDevice,
        LocalDateTime loginAt,
        String failureReason,
        String remarks
    ) {
    }
}
