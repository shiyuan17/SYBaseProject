package com.company.bl.notification.infrastructure;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class NotificationCenterJdbcRepository {

    private static final Set<String> PREFERENCE_MANAGED_CATEGORIES = Set.of(
        "ACCOUNT_PASSWORD",
        "SYSTEM_MESSAGE",
        "TODO_TASK"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public NotificationCenterJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PagedNotifications findNotifications(
        String userId,
        int page,
        int size,
        String status,
        String category,
        String keyword,
        Set<String> topicCodes,
        PreferenceRow preference
    ) {
        if (topicCodes.isEmpty()) {
            return new PagedNotifications(List.of(), 0L);
        }

        int normalizedPage = Math.max(1, page);
        int normalizedSize = Math.max(1, size);
        int offset = (normalizedPage - 1) * normalizedSize;
        MapSqlParameterSource params = createVisibilityParams(userId, topicCodes, preference)
            .addValue("offset", offset)
            .addValue("size", normalizedSize);

        String conditions = buildListConditions(params, status, category, keyword);
        String baseSql = """
            from user_notifications n
            where n.user_id = :userId
              and n.archived_at is null
              and n.status <> 'ARCHIVED'
              and n.topic_code in (:topicCodes)
            """ + buildPreferenceCondition("n.");

        List<NotificationRow> items = jdbcTemplate.query("""
            select n.id, n.user_id, n.topic_code, n.category, n.level, n.title, n.content, n.summary,
                   n.avatar, n.action_type, n.action_target, n.action_payload_json, n.action_text,
                   n.status, n.read_at, n.archived_at, n.created_at
            """ + baseSql + conditions + """

            order by n.created_at desc, n.id desc
            offset :offset rows fetch next :size rows only
            """, params, this::mapNotification);

        Long total = jdbcTemplate.queryForObject(
            "select count(*) " + baseSql + conditions,
            params,
            Long.class
        );
        return new PagedNotifications(items, total == null ? 0L : total);
    }

    public long countUnreadNotifications(
        String userId,
        Set<String> topicCodes,
        PreferenceRow preference
    ) {
        if (topicCodes.isEmpty()) {
            return 0L;
        }
        MapSqlParameterSource params = createVisibilityParams(userId, topicCodes, preference);
        Long total = jdbcTemplate.queryForObject("""
            select count(*)
            from user_notifications n
            where n.user_id = :userId
              and n.archived_at is null
              and n.status = 'UNREAD'
              and n.topic_code in (:topicCodes)
            """ + buildPreferenceCondition("n."), params, Long.class);
        return total == null ? 0L : total;
    }

    public void markRead(String userId, String notificationId, LocalDateTime now) {
        jdbcTemplate.update("""
            update user_notifications
            set status = case when status = 'ARCHIVED' then status else 'READ' end,
                read_at = case when status = 'ARCHIVED' then read_at else coalesce(read_at, :readAt) end
            where user_id = :userId
              and id = :notificationId
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("notificationId", notificationId)
            .addValue("readAt", now));
    }

    public void markAllRead(
        String userId,
        Set<String> topicCodes,
        PreferenceRow preference,
        LocalDateTime now
    ) {
        if (topicCodes.isEmpty()) {
            return;
        }
        MapSqlParameterSource params = createVisibilityParams(userId, topicCodes, preference)
            .addValue("readAt", now);
        jdbcTemplate.update("""
            update user_notifications
            set status = 'READ',
                read_at = coalesce(read_at, :readAt)
            where user_id = :userId
              and archived_at is null
              and status = 'UNREAD'
              and topic_code in (:topicCodes)
            """ + buildPreferenceCondition(""), params);
    }

    public void archiveOne(String userId, String notificationId, LocalDateTime now) {
        jdbcTemplate.update("""
            update user_notifications
            set status = 'ARCHIVED',
                archived_at = coalesce(archived_at, :archivedAt)
            where user_id = :userId
              and id = :notificationId
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("notificationId", notificationId)
            .addValue("archivedAt", now));
    }

    public void archiveMany(String userId, List<String> notificationIds, LocalDateTime now) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }
        jdbcTemplate.update("""
            update user_notifications
            set status = 'ARCHIVED',
                archived_at = coalesce(archived_at, :archivedAt)
            where user_id = :userId
              and id in (:notificationIds)
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("notificationIds", notificationIds)
            .addValue("archivedAt", now));
    }

    public PreferenceRow findPreference(String userId) {
        List<PreferenceRow> rows = jdbcTemplate.query("""
            select user_id, account_password_enabled, system_message_enabled, todo_task_enabled, updated_at
            from user_notification_preferences
            where user_id = :userId
            """, Map.of("userId", userId), this::mapPreference);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void upsertPreference(String userId, PreferenceRow preference, LocalDateTime now) {
        int updated = jdbcTemplate.update("""
            update user_notification_preferences
            set account_password_enabled = :accountPasswordEnabled,
                system_message_enabled = :systemMessageEnabled,
                todo_task_enabled = :todoTaskEnabled,
                updated_at = :updatedAt
            where user_id = :userId
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("accountPasswordEnabled", preference.accountPasswordEnabled() ? 1 : 0)
            .addValue("systemMessageEnabled", preference.systemMessageEnabled() ? 1 : 0)
            .addValue("todoTaskEnabled", preference.todoTaskEnabled() ? 1 : 0)
            .addValue("updatedAt", now));
        if (updated > 0) {
            return;
        }
        jdbcTemplate.update("""
            insert into user_notification_preferences
                (user_id, account_password_enabled, system_message_enabled, todo_task_enabled, updated_at)
            values
                (:userId, :accountPasswordEnabled, :systemMessageEnabled, :todoTaskEnabled, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("accountPasswordEnabled", preference.accountPasswordEnabled() ? 1 : 0)
            .addValue("systemMessageEnabled", preference.systemMessageEnabled() ? 1 : 0)
            .addValue("todoTaskEnabled", preference.todoTaskEnabled() ? 1 : 0)
            .addValue("updatedAt", now));
    }

    public Set<String> findAuthorizedTopicCodes(String userId) {
        return new LinkedHashSet<>(jdbcTemplate.query("""
            select distinct mt.topic_code
            from user_roles ur
            join roles r on r.id = ur.role_id
            join role_message_subscriptions rms on rms.role_id = ur.role_id
            join message_topics mt on mt.id = rms.topic_id
            where ur.user_id = :userId
              and r.enabled = 1
              and mt.enabled = 1
            order by mt.topic_code
            """, Map.of("userId", userId), (rs, rowNum) -> rs.getString(1)));
    }

    private MapSqlParameterSource createVisibilityParams(
        String userId,
        Set<String> topicCodes,
        PreferenceRow preference
    ) {
        return new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("topicCodes", topicCodes)
            .addValue("accountPasswordEnabled", preference.accountPasswordEnabled() ? 1 : 0)
            .addValue("systemMessageEnabled", preference.systemMessageEnabled() ? 1 : 0)
            .addValue("todoTaskEnabled", preference.todoTaskEnabled() ? 1 : 0)
            .addValue("managedCategories", PREFERENCE_MANAGED_CATEGORIES);
    }

    private String buildListConditions(
        MapSqlParameterSource params,
        String status,
        String category,
        String keyword
    ) {
        StringBuilder builder = new StringBuilder();
        String normalizedStatus = status == null ? "ALL" : status.trim().toUpperCase();
        if ("UNREAD".equals(normalizedStatus) || "READ".equals(normalizedStatus)) {
            builder.append(" and n.status = :status");
            params.addValue("status", normalizedStatus);
        }
        if (category != null && !category.isBlank()) {
            builder.append(" and n.category = :category");
            params.addValue("category", category.trim().toUpperCase());
        }
        if (keyword != null && !keyword.isBlank()) {
            builder.append("""
                 and (
                    upper(n.title) like :keyword
                    or upper(coalesce(n.summary, '')) like :keyword
                    or upper(coalesce(n.content, '')) like :keyword
                 )
                """);
            params.addValue("keyword", "%" + keyword.trim().toUpperCase() + "%");
        }
        return builder.toString();
    }

    private String buildPreferenceCondition(String prefix) {
        return """
             and (
                (%scategory = 'ACCOUNT_PASSWORD' and :accountPasswordEnabled = 1)
                or (%scategory = 'SYSTEM_MESSAGE' and :systemMessageEnabled = 1)
                or (%scategory = 'TODO_TASK' and :todoTaskEnabled = 1)
                or %scategory not in (:managedCategories)
             )
            """.formatted(prefix, prefix, prefix, prefix);
    }

    private NotificationRow mapNotification(ResultSet rs, int rowNum) throws SQLException {
        return new NotificationRow(
            rs.getString("id"),
            rs.getString("user_id"),
            rs.getString("topic_code"),
            rs.getString("category"),
            rs.getString("level"),
            rs.getString("title"),
            rs.getString("content"),
            rs.getString("summary"),
            rs.getString("avatar"),
            rs.getString("action_type"),
            rs.getString("action_target"),
            rs.getString("action_payload_json"),
            rs.getString("action_text"),
            rs.getString("status"),
            rs.getTimestamp("read_at") == null ? null : rs.getTimestamp("read_at").toLocalDateTime(),
            rs.getTimestamp("archived_at") == null ? null : rs.getTimestamp("archived_at").toLocalDateTime(),
            rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private PreferenceRow mapPreference(ResultSet rs, int rowNum) throws SQLException {
        return new PreferenceRow(
            rs.getString("user_id"),
            rs.getInt("account_password_enabled") == 1,
            rs.getInt("system_message_enabled") == 1,
            rs.getInt("todo_task_enabled") == 1,
            rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    public record PagedNotifications(List<NotificationRow> items, long total) {
    }

    public record NotificationRow(
        String id,
        String userId,
        String topicCode,
        String category,
        String level,
        String title,
        String content,
        String summary,
        String avatar,
        String actionType,
        String actionTarget,
        String actionPayloadJson,
        String actionText,
        String status,
        LocalDateTime readAt,
        LocalDateTime archivedAt,
        LocalDateTime createdAt
    ) {
    }

    public record PreferenceRow(
        String userId,
        boolean accountPasswordEnabled,
        boolean systemMessageEnabled,
        boolean todoTaskEnabled,
        LocalDateTime updatedAt
    ) {
    }
}
