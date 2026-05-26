package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class V57__seed_workflow_notification_topics extends BaseJavaMigration {

    private static final List<TopicSeed> TOPICS = List.of(
        new TopicSeed("TOPIC_TECH_TASK_ASSIGN", "TECH_TASK_ASSIGN", "技术任务分派通知", "WORKFLOW", "技术任务分派提醒"),
        new TopicSeed("TOPIC_TECH_TASK_RELEASE", "TECH_TASK_RELEASE", "技术任务释放通知", "WORKFLOW", "技术任务释放提醒"),
        new TopicSeed("TOPIC_TECH_TASK_PRIORITY", "TECH_TASK_PRIORITY", "技术任务优先级通知", "WORKFLOW", "技术任务优先级调整提醒"),
        new TopicSeed("TOPIC_DIAG_TASK_ASSIGN", "DIAG_TASK_ASSIGN", "诊断任务分派通知", "WORKFLOW", "诊断任务分派提醒"),
        new TopicSeed("TOPIC_CONSULTATION_INVITE", "CONSULTATION_INVITE", "会诊邀请通知", "WORKFLOW", "会诊发起邀请提醒"),
        new TopicSeed("TOPIC_CONSULTATION_COMMENT", "CONSULTATION_COMMENT", "会诊意见通知", "WORKFLOW", "会诊意见提交通知"),
        new TopicSeed("TOPIC_REWORK_CREATED", "REWORK_CREATED", "返工任务通知", "WORKFLOW", "返工任务生成提醒"),
        new TopicSeed("TOPIC_TASK_TIMEOUT", "TASK_TIMEOUT", "任务超时提醒", "WORKFLOW", "任务超时提醒")
    );

    private static final List<RoleTopicSeed> ROLE_TOPICS = List.of(
        new RoleTopicSeed("ROLE_PATHOLOGY_ADMIN", "TECH_TASK_ASSIGN", "INBOX"),
        new RoleTopicSeed("ROLE_PATHOLOGY_ADMIN", "TECH_TASK_RELEASE", "INBOX"),
        new RoleTopicSeed("ROLE_PATHOLOGY_ADMIN", "TECH_TASK_PRIORITY", "INBOX"),
        new RoleTopicSeed("ROLE_PATHOLOGY_ADMIN", "DIAG_TASK_ASSIGN", "INBOX"),
        new RoleTopicSeed("ROLE_PATHOLOGY_ADMIN", "CONSULTATION_INVITE", "INBOX"),
        new RoleTopicSeed("ROLE_PATHOLOGY_ADMIN", "CONSULTATION_COMMENT", "INBOX"),
        new RoleTopicSeed("ROLE_PATHOLOGY_ADMIN", "REWORK_CREATED", "INBOX"),
        new RoleTopicSeed("ROLE_PATHOLOGY_ADMIN", "TASK_TIMEOUT", "INBOX"),
        new RoleTopicSeed("ROLE_PATHOLOGY_ADMIN", "REPORT_REVISION", "INBOX"),

        new RoleTopicSeed("ROLE_PATHOLOGY_TECHNICIAN", "TECH_TASK_ASSIGN", "INBOX"),
        new RoleTopicSeed("ROLE_PATHOLOGY_TECHNICIAN", "TECH_TASK_RELEASE", "INBOX"),
        new RoleTopicSeed("ROLE_PATHOLOGY_TECHNICIAN", "TECH_TASK_PRIORITY", "INBOX"),
        new RoleTopicSeed("ROLE_PATHOLOGY_TECHNICIAN", "REWORK_CREATED", "INBOX"),
        new RoleTopicSeed("ROLE_PATHOLOGY_TECHNICIAN", "TASK_TIMEOUT", "INBOX"),

        new RoleTopicSeed("ROLE_M3_GROSSING", "TECH_TASK_ASSIGN", "INBOX"),
        new RoleTopicSeed("ROLE_M3_GROSSING", "TECH_TASK_RELEASE", "INBOX"),
        new RoleTopicSeed("ROLE_M3_GROSSING", "TECH_TASK_PRIORITY", "INBOX"),
        new RoleTopicSeed("ROLE_M3_GROSSING", "REWORK_CREATED", "INBOX"),
        new RoleTopicSeed("ROLE_M3_DEHYDRATION", "TECH_TASK_ASSIGN", "INBOX"),
        new RoleTopicSeed("ROLE_M3_DEHYDRATION", "TECH_TASK_RELEASE", "INBOX"),
        new RoleTopicSeed("ROLE_M3_DEHYDRATION", "TECH_TASK_PRIORITY", "INBOX"),
        new RoleTopicSeed("ROLE_M3_DEHYDRATION", "REWORK_CREATED", "INBOX"),
        new RoleTopicSeed("ROLE_M3_EMBEDDING", "TECH_TASK_ASSIGN", "INBOX"),
        new RoleTopicSeed("ROLE_M3_EMBEDDING", "TECH_TASK_RELEASE", "INBOX"),
        new RoleTopicSeed("ROLE_M3_EMBEDDING", "TECH_TASK_PRIORITY", "INBOX"),
        new RoleTopicSeed("ROLE_M3_EMBEDDING", "REWORK_CREATED", "INBOX"),
        new RoleTopicSeed("ROLE_M3_SLICING", "TECH_TASK_ASSIGN", "INBOX"),
        new RoleTopicSeed("ROLE_M3_SLICING", "TECH_TASK_RELEASE", "INBOX"),
        new RoleTopicSeed("ROLE_M3_SLICING", "TECH_TASK_PRIORITY", "INBOX"),
        new RoleTopicSeed("ROLE_M3_SLICING", "REWORK_CREATED", "INBOX"),
        new RoleTopicSeed("ROLE_M3_STAINING", "TECH_TASK_ASSIGN", "INBOX"),
        new RoleTopicSeed("ROLE_M3_STAINING", "TECH_TASK_RELEASE", "INBOX"),
        new RoleTopicSeed("ROLE_M3_STAINING", "TECH_TASK_PRIORITY", "INBOX"),
        new RoleTopicSeed("ROLE_M3_STAINING", "REWORK_CREATED", "INBOX"),
        new RoleTopicSeed("ROLE_M3_REWORK", "TECH_TASK_ASSIGN", "INBOX"),
        new RoleTopicSeed("ROLE_M3_REWORK", "TECH_TASK_RELEASE", "INBOX"),
        new RoleTopicSeed("ROLE_M3_REWORK", "TECH_TASK_PRIORITY", "INBOX"),
        new RoleTopicSeed("ROLE_M3_REWORK", "REWORK_CREATED", "INBOX"),

        new RoleTopicSeed("ROLE_PATHOLOGY_DOCTOR", "REPORT_REVISION", "INBOX"),
        new RoleTopicSeed("ROLE_M4_ASSIGN", "DIAG_TASK_ASSIGN", "INBOX"),
        new RoleTopicSeed("ROLE_M4_DIAGNOSIS", "DIAG_TASK_ASSIGN", "INBOX"),
        new RoleTopicSeed("ROLE_M4_DIAGNOSIS", "CONSULTATION_INVITE", "INBOX"),
        new RoleTopicSeed("ROLE_M4_DIAGNOSIS", "CONSULTATION_COMMENT", "INBOX"),
        new RoleTopicSeed("ROLE_M4_DIAGNOSIS", "REPORT_REVISION", "INBOX"),
        new RoleTopicSeed("ROLE_M4_REVIEW", "DIAG_TASK_ASSIGN", "INBOX"),
        new RoleTopicSeed("ROLE_M4_REVIEW", "CONSULTATION_INVITE", "INBOX"),
        new RoleTopicSeed("ROLE_M4_REVIEW", "CONSULTATION_COMMENT", "INBOX"),
        new RoleTopicSeed("ROLE_M4_SIGN", "DIAG_TASK_ASSIGN", "INBOX"),
        new RoleTopicSeed("ROLE_M4_SIGN", "CONSULTATION_INVITE", "INBOX"),
        new RoleTopicSeed("ROLE_M4_SIGN", "CONSULTATION_COMMENT", "INBOX"),
        new RoleTopicSeed("ROLE_M4_SIGN", "REPORT_REVISION", "INBOX")
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        for (TopicSeed topic : TOPICS) {
            upsertTopic(connection, topic);
        }
        for (RoleTopicSeed roleTopic : ROLE_TOPICS) {
            ensureRoleTopicSubscription(connection, roleTopic);
        }
    }

    private void upsertTopic(Connection connection, TopicSeed topic) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        try (PreparedStatement update = connection.prepareStatement("""
            update message_topics
            set topic_name = ?, topic_category = ?, description = ?, enabled = 1, updated_at = ?
            where topic_code = ?
            """)) {
            update.setString(1, topic.topicName());
            update.setString(2, topic.topicCategory());
            update.setString(3, topic.description());
            update.setTimestamp(4, Timestamp.valueOf(now));
            update.setString(5, topic.topicCode());
            if (update.executeUpdate() > 0) {
                return;
            }
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into message_topics
                (id, topic_code, topic_name, topic_category, description, enabled, created_at, updated_at)
            values
                (?, ?, ?, ?, ?, 1, ?, ?)
            """)) {
            insert.setString(1, topic.id());
            insert.setString(2, topic.topicCode());
            insert.setString(3, topic.topicName());
            insert.setString(4, topic.topicCategory());
            insert.setString(5, topic.description());
            insert.setTimestamp(6, Timestamp.valueOf(now));
            insert.setTimestamp(7, Timestamp.valueOf(now));
            insert.executeUpdate();
        }
    }

    private void ensureRoleTopicSubscription(Connection connection, RoleTopicSeed roleTopic) throws SQLException {
        String roleId = findExistingId(connection, "select id from roles where id = ?", roleTopic.roleId());
        String topicId = findExistingId(connection, "select id from message_topics where topic_code = ?", roleTopic.topicCode());
        if (roleId == null || topicId == null) {
            return;
        }
        if (exists(connection, """
            select 1
            from role_message_subscriptions
            where role_id = ?
              and topic_id = ?
            """, roleId, topicId)) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into role_message_subscriptions
                (id, role_id, topic_id, subscription_mode, assigned_at)
            values
                (?, ?, ?, ?, ?)
            """)) {
            insert.setString(1, "RMS-" + UUID.randomUUID());
            insert.setString(2, roleId);
            insert.setString(3, topicId);
            insert.setString(4, roleTopic.subscriptionMode());
            insert.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private boolean exists(Connection connection, String sql, String first, String second) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, first);
            statement.setString(2, second);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private String findExistingId(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    private record TopicSeed(
        String id,
        String topicCode,
        String topicName,
        String topicCategory,
        String description
    ) {
    }

    private record RoleTopicSeed(
        String roleId,
        String topicCode,
        String subscriptionMode
    ) {
    }
}
