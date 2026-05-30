package com.company.bl.system.infrastructure;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class SystemRoleJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SystemRoleJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

    private List<String> findIdList(String sql, String roleId) {
        return jdbcTemplate.query(sql, Map.of("roleId", roleId), (rs, rowNum) -> rs.getString(1));
    }

    private Map<String, String> findStatScopes(String roleId) {
        Map<String, String> result = new HashMap<>();
        jdbcTemplate.query("""
            select stat_category_id, auth_scope
            from role_stat_authorizations
            where role_id = :roleId
            """, Map.of("roleId", roleId), (org.springframework.jdbc.core.RowCallbackHandler) rs ->
            result.put(rs.getString("stat_category_id"), rs.getString("auth_scope")));
        return result;
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
