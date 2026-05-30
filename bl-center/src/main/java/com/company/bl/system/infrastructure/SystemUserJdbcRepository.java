package com.company.bl.system.infrastructure;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class SystemUserJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SystemUserJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SystemJdbcRepository.PagedUsers findUsers(int page, int size, Boolean enabled, String keyword) {
        int offset = Math.max(0, (page - 1) * size);
        String baseSql = """
            from users
            where 1 = 1
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("offset", offset)
            .addValue("size", size);
        StringBuilder conditions = new StringBuilder();
        appendUserFilters(conditions, params, enabled, keyword);
        List<SystemJdbcRepository.UserRow> users = jdbcTemplate.query("""
            select id, user_code, login_name, name, role, job_no, title_name, department_id, department_name,
                   phone, email, avatar, last_login_at, last_login_ip, last_login_device, login_tag_code,
                   enabled, created_at, updated_at
            """ + baseSql + conditions + """
            order by created_at desc
            offset :offset rows fetch next :size rows only
            """, params, this::mapUser);
        Long total = jdbcTemplate.queryForObject("select count(*) " + baseSql + conditions, params, Long.class);
        Map<String, List<SystemRoleJdbcRepository.RoleAssignmentRow>> assignments =
            findUserRoleAssignments(users.stream().map(SystemJdbcRepository.UserRow::id).toList());
        return new SystemJdbcRepository.PagedUsers(users, assignments, total == null ? 0L : total);
    }

    public List<SystemJdbcRepository.UserRow> findUsers(Boolean enabled, String keyword) {
        String baseSql = """
            select id, user_code, login_name, name, role, job_no, title_name, department_id, department_name,
                   phone, email, avatar, last_login_at, last_login_ip, last_login_device, login_tag_code,
                   enabled, created_at, updated_at
            from users
            where 1 = 1
            """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder conditions = new StringBuilder();
        appendUserFilters(conditions, params, enabled, keyword);
        return jdbcTemplate.query(baseSql + conditions + """
            order by created_at desc
            """, params, this::mapUser);
    }

    public SystemJdbcRepository.UserRow insertUser(SystemJdbcRepository.CreateUserRow row) {
        jdbcTemplate.update("""
            insert into users
                (id, user_code, login_name, name, password, password_algo, password_salt, role, job_no, title_name,
                 department_id, department_name, phone, email, avatar, login_tag_code, enabled, created_at, updated_at)
            values
                (:id, :userCode, :loginName, :name, :password, :passwordAlgo, :passwordSalt, :role, :jobNo, :titleName,
                 :departmentId, :departmentName, :phone, :email, :avatar, :loginTagCode, :enabled, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("userCode", row.userCode())
            .addValue("loginName", row.loginName())
            .addValue("name", row.name())
            .addValue("password", row.password())
            .addValue("passwordAlgo", row.passwordAlgo())
            .addValue("passwordSalt", row.passwordSalt())
            .addValue("role", row.role())
            .addValue("jobNo", row.jobNo())
            .addValue("titleName", row.titleName())
            .addValue("departmentId", row.departmentId())
            .addValue("departmentName", row.departmentName())
            .addValue("phone", row.phone())
            .addValue("email", row.email())
            .addValue("avatar", row.avatar())
            .addValue("loginTagCode", row.loginTagCode())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt()));
        return findUserById(row.id());
    }

    public SystemJdbcRepository.UserRow findUserById(String id) {
        List<SystemJdbcRepository.UserRow> rows = jdbcTemplate.query("""
            select id, user_code, login_name, name, role, job_no, title_name, department_id, department_name,
                   phone, email, avatar, last_login_at, last_login_ip, last_login_device, login_tag_code,
                   enabled, created_at, updated_at
            from users
            where id = :id
            """, Map.of("id", id), this::mapUser);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public SystemJdbcRepository.UserRow findUserByLoginName(String loginName) {
        List<SystemJdbcRepository.UserRow> rows = jdbcTemplate.query("""
            select id, user_code, login_name, name, role, job_no, title_name, department_id, department_name,
                   phone, email, avatar, last_login_at, last_login_ip, last_login_device, login_tag_code,
                   enabled, created_at, updated_at
            from users
            where login_name = :loginName
            """, Map.of("loginName", loginName), this::mapUser);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void updateUser(String id, SystemJdbcRepository.UpdateUserRow row) {
        jdbcTemplate.update("""
            update users
            set user_code = :userCode,
                name = :name,
                job_no = :jobNo,
                title_name = :titleName,
                department_id = :departmentId,
                department_name = :departmentName,
                phone = :phone,
                email = :email,
                avatar = :avatar,
                login_tag_code = :loginTagCode,
                enabled = :enabled,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("userCode", row.userCode())
            .addValue("name", row.name())
            .addValue("jobNo", row.jobNo())
            .addValue("titleName", row.titleName())
            .addValue("departmentId", row.departmentId())
            .addValue("departmentName", row.departmentName())
            .addValue("phone", row.phone())
            .addValue("email", row.email())
            .addValue("avatar", row.avatar())
            .addValue("loginTagCode", row.loginTagCode())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void updateUserEnabled(String id, boolean enabled) {
        jdbcTemplate.update("""
            update users
            set enabled = :enabled, updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("enabled", enabled ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void updateUserLastLogin(String id, LocalDateTime lastLoginAt, String lastLoginIp, String lastLoginDevice) {
        jdbcTemplate.update("""
            update users
            set last_login_at = :lastLoginAt,
                last_login_ip = :lastLoginIp,
                last_login_device = :lastLoginDevice,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("lastLoginAt", lastLoginAt)
            .addValue("lastLoginIp", lastLoginIp)
            .addValue("lastLoginDevice", lastLoginDevice)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void insertUserLoginLog(SystemJdbcRepository.CreateUserLoginLogRow row) {
        jdbcTemplate.update("""
            insert into user_login_logs
                (id, user_id, login_name, login_result, client_ip, client_device, login_at, logout_at, failure_reason, remarks)
            values
                (:id, :userId, :loginName, :loginResult, :clientIp, :clientDevice, :loginAt, :logoutAt, :failureReason, :remarks)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("userId", row.userId())
            .addValue("loginName", row.loginName())
            .addValue("loginResult", row.loginResult())
            .addValue("clientIp", row.clientIp())
            .addValue("clientDevice", row.clientDevice())
            .addValue("loginAt", row.loginAt())
            .addValue("logoutAt", row.logoutAt())
            .addValue("failureReason", row.failureReason())
            .addValue("remarks", row.remarks()));
    }

    public SystemJdbcRepository.PagedUserLoginLogs findUserLoginLogs(String userId, int page, int size) {
        int offset = Math.max(0, (page - 1) * size);
        List<SystemJdbcRepository.UserLoginLogRow> logs = jdbcTemplate.query("""
            select id, user_id, login_name, login_result, client_ip, client_device, login_at, logout_at, failure_reason, remarks
            from user_login_logs
            where user_id = :userId
            order by login_at desc, id desc
            offset :offset rows fetch next :size rows only
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("offset", offset)
            .addValue("size", size), this::mapUserLoginLog);
        Long total = jdbcTemplate.queryForObject("""
            select count(*)
            from user_login_logs
            where user_id = :userId
            """, Map.of("userId", userId), Long.class);
        return new SystemJdbcRepository.PagedUserLoginLogs(logs, total == null ? 0L : total);
    }

    public Map<String, List<SystemRoleJdbcRepository.RoleAssignmentRow>> findUserRoleAssignments(List<String> userIds) {
        Map<String, List<SystemRoleJdbcRepository.RoleAssignmentRow>> result = new HashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }
        jdbcTemplate.query("""
            select user_roles.id, user_roles.user_id, user_roles.role_id, user_roles.is_primary,
                   roles.role_code, roles.role_name
            from user_roles
            join roles on roles.id = user_roles.role_id
            where user_roles.user_id in (:userIds)
            order by roles.role_code
            """, Map.of("userIds", userIds), rs -> {
            String userId = rs.getString("user_id");
            result.computeIfAbsent(userId, key -> new ArrayList<>()).add(mapRoleAssignment(rs, 0));
        });
        return result;
    }

    public void replaceUserRoles(String userId, List<SystemJdbcRepository.UserRoleAssignmentCommand> assignments) {
        jdbcTemplate.update("delete from user_roles where user_id = :userId", Map.of("userId", userId));
        for (SystemJdbcRepository.UserRoleAssignmentCommand assignment : assignments) {
            jdbcTemplate.update("""
                insert into user_roles
                    (id, user_id, role_id, is_primary, assigned_at, assigned_by_name)
                values
                    (:id, :userId, :roleId, :isPrimary, :assignedAt, :assignedByName)
                """, new MapSqlParameterSource()
                .addValue("id", "UR-" + UUID.randomUUID())
                .addValue("userId", userId)
                .addValue("roleId", assignment.roleId())
                .addValue("isPrimary", assignment.primary() ? 1 : 0)
                .addValue("assignedAt", LocalDateTime.now())
                .addValue("assignedByName", "system"));
        }
        String primaryRoleCode = jdbcTemplate.query("""
            select roles.role_code
            from user_roles
            join roles on roles.id = user_roles.role_id
            where user_roles.user_id = :userId and user_roles.is_primary = 1
            """, Map.of("userId", userId), rs -> rs.next() ? rs.getString(1) : null);
        jdbcTemplate.update("""
            update users set role = :role, updated_at = :updatedAt where id = :userId
            """, new MapSqlParameterSource()
            .addValue("role", primaryRoleCode)
            .addValue("updatedAt", LocalDateTime.now())
            .addValue("userId", userId));
    }

    private void appendUserFilters(StringBuilder conditions,
                                   MapSqlParameterSource params,
                                   Boolean enabled,
                                   String keyword) {
        if (enabled != null) {
            conditions.append(" and enabled = :enabled\n");
            params.addValue("enabled", enabled ? 1 : 0);
        }
        if (keyword != null && !keyword.isBlank()) {
            conditions.append("""
                 and (
                    login_name like :keyword
                    or name like :keyword
                    or user_code like :keyword
                    or job_no like :keyword
                    or phone like :keyword
                 )
                """);
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }
    }

    private SystemJdbcRepository.UserRow mapUser(ResultSet rs, int rowNum) throws SQLException {
        return new SystemJdbcRepository.UserRow(
            rs.getString("id"),
            rs.getString("user_code"),
            rs.getString("login_name"),
            rs.getString("name"),
            rs.getString("role"),
            rs.getString("job_no"),
            rs.getString("title_name"),
            rs.getString("department_id"),
            rs.getString("department_name"),
            rs.getString("phone"),
            rs.getString("email"),
            rs.getString("avatar"),
            rs.getTimestamp("last_login_at") == null ? null : rs.getTimestamp("last_login_at").toLocalDateTime(),
            rs.getString("last_login_ip"),
            rs.getString("last_login_device"),
            rs.getString("login_tag_code"),
            rs.getInt("enabled") == 1,
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private SystemRoleJdbcRepository.RoleAssignmentRow mapRoleAssignment(ResultSet rs, int rowNum) throws SQLException {
        return new SystemRoleJdbcRepository.RoleAssignmentRow(
            rs.getString("id"),
            rs.getString("user_id"),
            rs.getString("role_id"),
            rs.getInt("is_primary") == 1,
            rs.getString("role_code"),
            rs.getString("role_name"));
    }

    private SystemJdbcRepository.UserLoginLogRow mapUserLoginLog(ResultSet rs, int rowNum) throws SQLException {
        return new SystemJdbcRepository.UserLoginLogRow(
            rs.getString("id"),
            rs.getString("user_id"),
            rs.getString("login_name"),
            rs.getString("login_result"),
            rs.getString("client_ip"),
            rs.getString("client_device"),
            rs.getTimestamp("login_at") == null ? null : rs.getTimestamp("login_at").toLocalDateTime(),
            rs.getTimestamp("logout_at") == null ? null : rs.getTimestamp("logout_at").toLocalDateTime(),
            rs.getString("failure_reason"),
            rs.getString("remarks"));
    }
}
