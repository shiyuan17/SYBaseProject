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
public class SystemJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SystemJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PagedUsers findUsers(int page, int size, Boolean enabled, String keyword) {
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
        List<UserRow> users = jdbcTemplate.query("""
            select id, user_code, login_name, name, role, job_no, title_name, department_id, department_name,
                   phone, email, avatar, last_login_at, last_login_ip, last_login_device, login_tag_code,
                   enabled, created_at, updated_at
            """ + baseSql + conditions + """
            order by created_at desc
            offset :offset rows fetch next :size rows only
            """, params, this::mapUser);
        Long total = jdbcTemplate.queryForObject("select count(*) " + baseSql + conditions, params, Long.class);
        Map<String, List<RoleAssignmentRow>> assignments = findUserRoleAssignments(users.stream().map(UserRow::id).toList());
        return new PagedUsers(users, assignments, total == null ? 0L : total);
    }

    public List<UserRow> findUsers(Boolean enabled, String keyword) {
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

    public UserRow insertUser(CreateUserRow row) {
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

    public UserRow findUserById(String id) {
        List<UserRow> rows = jdbcTemplate.query("""
            select id, user_code, login_name, name, role, job_no, title_name, department_id, department_name,
                   phone, email, avatar, last_login_at, last_login_ip, last_login_device, login_tag_code,
                   enabled, created_at, updated_at
            from users
            where id = :id
            """, Map.of("id", id), this::mapUser);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public UserRow findUserByLoginName(String loginName) {
        List<UserRow> rows = jdbcTemplate.query("""
            select id, user_code, login_name, name, role, job_no, title_name, department_id, department_name,
                   phone, email, avatar, last_login_at, last_login_ip, last_login_device, login_tag_code,
                   enabled, created_at, updated_at
            from users
            where login_name = :loginName
            """, Map.of("loginName", loginName), this::mapUser);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void updateUser(String id, UpdateUserRow row) {
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

    public void insertUserLoginLog(CreateUserLoginLogRow row) {
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

    public PagedUserLoginLogs findUserLoginLogs(String userId, int page, int size) {
        int offset = Math.max(0, (page - 1) * size);
        List<UserLoginLogRow> logs = jdbcTemplate.query("""
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
        return new PagedUserLoginLogs(logs, total == null ? 0L : total);
    }

    public List<RoleRow> findRoles() {
        return jdbcTemplate.query("""
            select id, role_code, role_name, role_type, data_scope, remarks, enabled, created_at, updated_at
            from roles
            order by role_code
            """, this::mapRole);
    }

    public RoleRow insertRole(CreateRoleRow row) {
        jdbcTemplate.update("""
            insert into roles
                (id, role_code, role_name, role_type, data_scope, remarks, enabled, created_at, updated_at)
            values
                (:id, :roleCode, :roleName, :roleType, :dataScope, :remarks, :enabled, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("roleCode", row.roleCode())
            .addValue("roleName", row.roleName())
            .addValue("roleType", row.roleType())
            .addValue("dataScope", row.dataScope())
            .addValue("remarks", row.remarks())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt()));
        return findRoleById(row.id());
    }

    public RoleRow findRoleById(String id) {
        List<RoleRow> rows = jdbcTemplate.query("""
            select id, role_code, role_name, role_type, data_scope, remarks, enabled, created_at, updated_at
            from roles
            where id = :id
            """, Map.of("id", id), this::mapRole);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void updateRole(String id, UpdateRoleRow row) {
        jdbcTemplate.update("""
            update roles
            set role_code = :roleCode,
                role_name = :roleName,
                role_type = :roleType,
                data_scope = :dataScope,
                remarks = :remarks,
                enabled = :enabled,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("roleCode", row.roleCode())
            .addValue("roleName", row.roleName())
            .addValue("roleType", row.roleType())
            .addValue("dataScope", row.dataScope())
            .addValue("remarks", row.remarks())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public long countRoleAssignments(String roleId) {
        Long total = jdbcTemplate.queryForObject("""
            select count(*)
            from user_roles
            where role_id = :roleId
            """, Map.of("roleId", roleId), Long.class);
        return total == null ? 0L : total;
    }

    public void deleteRole(String id) {
        jdbcTemplate.update("delete from role_menus where role_id = :roleId", Map.of("roleId", id));
        jdbcTemplate.update("delete from role_permissions where role_id = :roleId", Map.of("roleId", id));
        jdbcTemplate.update("delete from role_message_subscriptions where role_id = :roleId", Map.of("roleId", id));
        jdbcTemplate.update("delete from role_stat_authorizations where role_id = :roleId", Map.of("roleId", id));
        jdbcTemplate.update("delete from roles where id = :id", Map.of("id", id));
    }

    public List<RoleAssignmentRow> findRoleAssignments(String roleId) {
        return jdbcTemplate.query("""
            select user_roles.id, user_roles.user_id, user_roles.role_id, user_roles.is_primary,
                   roles.role_code, roles.role_name
            from user_roles
            join roles on roles.id = user_roles.role_id
            where user_roles.role_id = :roleId
            order by roles.role_code
            """, Map.of("roleId", roleId), this::mapRoleAssignment);
    }

    public Map<String, List<RoleAssignmentRow>> findUserRoleAssignments(List<String> userIds) {
        Map<String, List<RoleAssignmentRow>> result = new HashMap<>();
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

    public void replaceUserRoles(String userId, List<UserRoleAssignmentCommand> assignments) {
        jdbcTemplate.update("delete from user_roles where user_id = :userId", Map.of("userId", userId));
        for (UserRoleAssignmentCommand assignment : assignments) {
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

    public List<MenuRow> findMenus() {
        return jdbcTemplate.query("""
            select id, parent_id, menu_code, menu_name, menu_type, path, component_name, icon,
                   permission_prefix, sort_order, visible, enabled
            from menus
            order by sort_order, menu_code
            """, this::mapMenu);
    }

    public List<PermissionRow> findPermissions() {
        return jdbcTemplate.query("""
            select id, permission_code, permission_name, menu_id, action_key, http_method,
                   resource_path, permission_group, sort_order, enabled
            from permissions
            order by sort_order, permission_code
            """, this::mapPermission);
    }

    public List<MessageTopicRow> findMessageTopics() {
        return jdbcTemplate.query("""
            select id, topic_code, topic_name, topic_category, description, enabled
            from message_topics
            order by topic_code
            """, this::mapMessageTopic);
    }

    public List<StatCategoryRow> findStatCategories() {
        return jdbcTemplate.query("""
            select id, stat_code, stat_name, stat_scope, description, enabled
            from stat_categories
            order by stat_code
            """, this::mapStatCategory);
    }

    public RoleAuthorizationRow findRoleAuthorization(String roleId) {
        return new RoleAuthorizationRow(
            findIdList("select menu_id from role_menus where role_id = :roleId order by menu_id", roleId),
            findIdList("select permission_id from role_permissions where role_id = :roleId order by permission_id", roleId),
            findIdList("select topic_id from role_message_subscriptions where role_id = :roleId order by topic_id", roleId),
            findStatScopes(roleId));
    }

    public void replaceRoleAuthorizations(String roleId, AuthorizationCommand command) {
        jdbcTemplate.update("delete from role_menus where role_id = :roleId", Map.of("roleId", roleId));
        jdbcTemplate.update("delete from role_permissions where role_id = :roleId", Map.of("roleId", roleId));
        jdbcTemplate.update("delete from role_message_subscriptions where role_id = :roleId", Map.of("roleId", roleId));
        jdbcTemplate.update("delete from role_stat_authorizations where role_id = :roleId", Map.of("roleId", roleId));

        for (String menuId : command.menuIds()) {
            jdbcTemplate.update("""
                insert into role_menus (id, role_id, menu_id, assigned_at)
                values (:id, :roleId, :menuId, :assignedAt)
                """, new MapSqlParameterSource()
                .addValue("id", "RM-" + UUID.randomUUID())
                .addValue("roleId", roleId)
                .addValue("menuId", menuId)
                .addValue("assignedAt", LocalDateTime.now()));
        }
        for (String permissionId : command.permissionIds()) {
            jdbcTemplate.update("""
                insert into role_permissions (id, role_id, permission_id, assigned_at)
                values (:id, :roleId, :permissionId, :assignedAt)
                """, new MapSqlParameterSource()
                .addValue("id", "RP-" + UUID.randomUUID())
                .addValue("roleId", roleId)
                .addValue("permissionId", permissionId)
                .addValue("assignedAt", LocalDateTime.now()));
        }
        for (String topicId : command.topicIds()) {
            jdbcTemplate.update("""
                insert into role_message_subscriptions (id, role_id, topic_id, subscription_mode, assigned_at)
                values (:id, :roleId, :topicId, :subscriptionMode, :assignedAt)
                """, new MapSqlParameterSource()
                .addValue("id", "RMS-" + UUID.randomUUID())
                .addValue("roleId", roleId)
                .addValue("topicId", topicId)
                .addValue("subscriptionMode", "INBOX")
                .addValue("assignedAt", LocalDateTime.now()));
        }
        for (Map.Entry<String, String> entry : command.statScopes().entrySet()) {
            jdbcTemplate.update("""
                insert into role_stat_authorizations (id, role_id, stat_category_id, auth_scope, assigned_at)
                values (:id, :roleId, :statCategoryId, :authScope, :assignedAt)
                """, new MapSqlParameterSource()
                .addValue("id", "RSA-" + UUID.randomUUID())
                .addValue("roleId", roleId)
                .addValue("statCategoryId", entry.getKey())
                .addValue("authScope", entry.getValue())
                .addValue("assignedAt", LocalDateTime.now()));
        }
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

    private List<String> findIdList(String sql, String roleId) {
        return jdbcTemplate.query(sql, Map.of("roleId", roleId), (rs, rowNum) -> rs.getString(1));
    }

    private Map<String, String> findStatScopes(String roleId) {
        Map<String, String> result = new HashMap<>();
        jdbcTemplate.query("""
            select stat_category_id, auth_scope
            from role_stat_authorizations
            where role_id = :roleId
            """, Map.of("roleId", roleId), rs -> {
                result.put(rs.getString("stat_category_id"), rs.getString("auth_scope"));
            });
        return result;
    }

    private UserRow mapUser(ResultSet rs, int rowNum) throws SQLException {
        return new UserRow(
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

    private RoleRow mapRole(ResultSet rs, int rowNum) throws SQLException {
        return new RoleRow(
            rs.getString("id"),
            rs.getString("role_code"),
            rs.getString("role_name"),
            rs.getString("role_type"),
            rs.getString("data_scope"),
            rs.getString("remarks"),
            rs.getInt("enabled") == 1,
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private RoleAssignmentRow mapRoleAssignment(ResultSet rs, int rowNum) throws SQLException {
        return new RoleAssignmentRow(
            rs.getString("id"),
            rs.getString("user_id"),
            rs.getString("role_id"),
            rs.getInt("is_primary") == 1,
            rs.getString("role_code"),
            rs.getString("role_name"));
    }

    private MenuRow mapMenu(ResultSet rs, int rowNum) throws SQLException {
        return new MenuRow(
            rs.getString("id"),
            rs.getString("parent_id"),
            rs.getString("menu_code"),
            rs.getString("menu_name"),
            rs.getString("menu_type"),
            rs.getString("path"),
            rs.getString("component_name"),
            rs.getString("icon"),
            rs.getString("permission_prefix"),
            rs.getInt("sort_order"),
            rs.getInt("visible") == 1,
            rs.getInt("enabled") == 1);
    }

    private PermissionRow mapPermission(ResultSet rs, int rowNum) throws SQLException {
        return new PermissionRow(
            rs.getString("id"),
            rs.getString("permission_code"),
            rs.getString("permission_name"),
            rs.getString("menu_id"),
            rs.getString("action_key"),
            rs.getString("http_method"),
            rs.getString("resource_path"),
            rs.getString("permission_group"),
            rs.getInt("sort_order"),
            rs.getInt("enabled") == 1);
    }

    private MessageTopicRow mapMessageTopic(ResultSet rs, int rowNum) throws SQLException {
        return new MessageTopicRow(
            rs.getString("id"),
            rs.getString("topic_code"),
            rs.getString("topic_name"),
            rs.getString("topic_category"),
            rs.getString("description"),
            rs.getInt("enabled") == 1);
    }

    private StatCategoryRow mapStatCategory(ResultSet rs, int rowNum) throws SQLException {
        return new StatCategoryRow(
            rs.getString("id"),
            rs.getString("stat_code"),
            rs.getString("stat_name"),
            rs.getString("stat_scope"),
            rs.getString("description"),
            rs.getInt("enabled") == 1);
    }

    private UserLoginLogRow mapUserLoginLog(ResultSet rs, int rowNum) throws SQLException {
        return new UserLoginLogRow(
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

    public record PagedUsers(List<UserRow> users, Map<String, List<RoleAssignmentRow>> assignments, long total) {
    }

    public record UserRow(
        String id,
        String userCode,
        String loginName,
        String name,
        String role,
        String jobNo,
        String titleName,
        String departmentId,
        String departmentName,
        String phone,
        String email,
        String avatar,
        LocalDateTime lastLoginAt,
        String lastLoginIp,
        String lastLoginDevice,
        String loginTagCode,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record CreateUserRow(
        String id,
        String userCode,
        String loginName,
        String name,
        String password,
        String passwordAlgo,
        String passwordSalt,
        String role,
        String jobNo,
        String titleName,
        String departmentId,
        String departmentName,
        String phone,
        String email,
        String avatar,
        String loginTagCode,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record UpdateUserRow(
        String userCode,
        String name,
        String jobNo,
        String titleName,
        String departmentId,
        String departmentName,
        String phone,
        String email,
        String avatar,
        String loginTagCode,
        boolean enabled
    ) {
    }

    public record CreateUserLoginLogRow(
        String id,
        String userId,
        String loginName,
        String loginResult,
        String clientIp,
        String clientDevice,
        LocalDateTime loginAt,
        LocalDateTime logoutAt,
        String failureReason,
        String remarks
    ) {
    }

    public record RoleRow(
        String id,
        String roleCode,
        String roleName,
        String roleType,
        String dataScope,
        String remarks,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record CreateRoleRow(
        String id,
        String roleCode,
        String roleName,
        String roleType,
        String dataScope,
        String remarks,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record UpdateRoleRow(
        String roleCode,
        String roleName,
        String roleType,
        String dataScope,
        String remarks,
        boolean enabled
    ) {
    }

    public record RoleAssignmentRow(
        String id,
        String userId,
        String roleId,
        boolean primary,
        String roleCode,
        String roleName
    ) {
    }

    public record UserRoleAssignmentCommand(String roleId, boolean primary) {
    }

    public record MenuRow(
        String id,
        String parentId,
        String menuCode,
        String menuName,
        String menuType,
        String path,
        String componentName,
        String icon,
        String permissionPrefix,
        int sortOrder,
        boolean visible,
        boolean enabled
    ) {
    }

    public record PermissionRow(
        String id,
        String permissionCode,
        String permissionName,
        String menuId,
        String actionKey,
        String httpMethod,
        String resourcePath,
        String permissionGroup,
        int sortOrder,
        boolean enabled
    ) {
    }

    public record MessageTopicRow(
        String id,
        String topicCode,
        String topicName,
        String topicCategory,
        String description,
        boolean enabled
    ) {
    }

    public record StatCategoryRow(
        String id,
        String statCode,
        String statName,
        String statScope,
        String description,
        boolean enabled
    ) {
    }

    public record PagedUserLoginLogs(List<UserLoginLogRow> logs, long total) {
    }

    public record UserLoginLogRow(
        String id,
        String userId,
        String loginName,
        String loginResult,
        String clientIp,
        String clientDevice,
        LocalDateTime loginAt,
        LocalDateTime logoutAt,
        String failureReason,
        String remarks
    ) {
    }

    public record RoleAuthorizationRow(
        List<String> menuIds,
        List<String> permissionIds,
        List<String> topicIds,
        Map<String, String> statScopes
    ) {
    }

    public record AuthorizationCommand(
        List<String> menuIds,
        List<String> permissionIds,
        List<String> topicIds,
        Map<String, String> statScopes
    ) {
    }
}
