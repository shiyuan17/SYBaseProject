package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class V55__create_notification_center_schema extends BaseJavaMigration {

    private static final List<NotificationSeed> NOTIFICATION_SEEDS = List.of(
        new NotificationSeed(
            "NOTIFY_ADMIN_REPORT_REVISION",
            "USER_M1_ADMIN",
            "REPORT_REVISION",
            "SYSTEM_MESSAGE",
            "MEDIUM",
            "报告修订已提交",
            "你负责复核的病理报告已提交修订申请，请及时处理。",
            "病理报告 RPT-20260525-001 已进入修订流程。",
            "ROUTE",
            "/diagnostic/reports",
            "{\"query\":{\"reportId\":\"RPT-20260525-001\"}}",
            "查看报告",
            "UNREAD",
            null,
            null,
            "https://avatar.vercel.sh/pathology-admin.svg?text=PA",
            LocalDateTime.of(2026, 5, 25, 9, 30)
        ),
        new NotificationSeed(
            "NOTIFY_ADMIN_CRITICAL_VALUE",
            "USER_M1_ADMIN",
            "CRITICAL_VALUE",
            "TODO_TASK",
            "HIGH",
            "危急值待处理",
            "系统检测到一条危急值消息，需要你尽快确认并通知临床。",
            "病例 BL20260525001 的危急值提醒待处理。",
            "ROUTE",
            "/diagnostic/tasks",
            "{\"query\":{\"caseId\":\"CASE-CRITICAL-001\"}}",
            "立即处理",
            "UNREAD",
            null,
            null,
            "https://avatar.vercel.sh/critical.svg?text=CV",
            LocalDateTime.of(2026, 5, 25, 10, 0)
        ),
        new NotificationSeed(
            "NOTIFY_DOCTOR_REPORT_REVISION",
            "USER_M1_DOCTOR",
            "REPORT_REVISION",
            "SYSTEM_MESSAGE",
            "MEDIUM",
            "报告修订反馈",
            "你提交的报告修订申请已被接收，请根据反馈继续完善。",
            "报告修订流程已启动。",
            "ROUTE",
            "/diagnostic/reports",
            "{\"query\":{\"reportId\":\"RPT-20260525-002\"}}",
            "查看详情",
            "UNREAD",
            null,
            null,
            "https://avatar.vercel.sh/pathology-doctor.svg?text=PD",
            LocalDateTime.of(2026, 5, 25, 10, 15)
        ),
        new NotificationSeed(
            "NOTIFY_TECH_QC_WARNING",
            "USER_M1_TECHNICIAN",
            "QC_WARNING",
            "TODO_TASK",
            "HIGH",
            "质控预警待确认",
            "切片染色质控异常，请尽快确认并补充处理记录。",
            "今日染色批次存在一条质控预警。",
            "ROUTE",
            "/technical/tasks",
            "{\"query\":{\"taskId\":\"TASK-QC-001\"}}",
            "前往处理",
            "UNREAD",
            null,
            null,
            "https://avatar.vercel.sh/quality.svg?text=QC",
            LocalDateTime.of(2026, 5, 25, 10, 30)
        )
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        ensureNotificationTables(connection);
        ensureDefaultPreferenceRows(connection);
        ensureSampleNotifications(connection);
    }

    private void ensureNotificationTables(Connection connection) throws SQLException {
        if (!tableExists(connection, "user_notifications")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                    CREATE TABLE user_notifications (
                        id VARCHAR(64) NOT NULL,
                        user_id VARCHAR(64) NOT NULL,
                        topic_code VARCHAR(64) NOT NULL,
                        category VARCHAR(32) NOT NULL,
                        level VARCHAR(16) NOT NULL,
                        title VARCHAR(200) NOT NULL,
                        content CLOB,
                        summary VARCHAR(500),
                        avatar VARCHAR(500),
                        action_type VARCHAR(32),
                        action_target VARCHAR(500),
                        action_payload_json CLOB,
                        action_text VARCHAR(100),
                        status VARCHAR(16) NOT NULL,
                        read_at TIMESTAMP,
                        archived_at TIMESTAMP,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT pk_user_notifications PRIMARY KEY (id),
                        CONSTRAINT fk_user_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
                    )
                    """);
            }
        }
        if (!tableExists(connection, "user_notification_preferences")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                    CREATE TABLE user_notification_preferences (
                        user_id VARCHAR(64) NOT NULL,
                        account_password_enabled INTEGER DEFAULT 1,
                        system_message_enabled INTEGER DEFAULT 1,
                        todo_task_enabled INTEGER DEFAULT 1,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT pk_user_notification_preferences PRIMARY KEY (user_id),
                        CONSTRAINT fk_user_notification_preferences_user FOREIGN KEY (user_id) REFERENCES users (id)
                    )
                    """);
            }
        }
        ensureIndex(connection, "user_notifications", "idx_user_notifications_user_status", """
            CREATE INDEX idx_user_notifications_user_status
            ON user_notifications (user_id, status, created_at)
            """);
        ensureIndex(connection, "user_notifications", "idx_user_notifications_user_archived", """
            CREATE INDEX idx_user_notifications_user_archived
            ON user_notifications (user_id, archived_at, created_at)
            """);
    }

    private void ensureDefaultPreferenceRows(Connection connection) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("""
            select id
            from users
            """);
             ResultSet resultSet = query.executeQuery()) {
            while (resultSet.next()) {
                String userId = resultSet.getString(1);
                if (exists(
                    connection,
                    "select 1 from user_notification_preferences where user_id = ?",
                    userId
                )) {
                    continue;
                }
                try (PreparedStatement insert = connection.prepareStatement("""
                    insert into user_notification_preferences
                        (user_id, account_password_enabled, system_message_enabled, todo_task_enabled, updated_at)
                    values
                        (?, 1, 1, 1, ?)
                    """)) {
                    insert.setString(1, userId);
                    insert.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                    insert.executeUpdate();
                }
            }
        }
    }

    private void ensureSampleNotifications(Connection connection) throws SQLException {
        for (NotificationSeed seed : NOTIFICATION_SEEDS) {
            if (!exists(connection, "select 1 from users where id = ?", seed.userId())) {
                continue;
            }
            if (!exists(connection, "select 1 from message_topics where topic_code = ?", seed.topicCode())) {
                continue;
            }
            if (exists(connection, "select 1 from user_notifications where id = ?", seed.id())) {
                continue;
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                insert into user_notifications
                    (id, user_id, topic_code, category, level, title, content, summary,
                     action_type, action_target, action_payload_json, action_text,
                     status, read_at, archived_at, created_at, avatar)
                values
                    (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
                insert.setString(1, seed.id());
                insert.setString(2, seed.userId());
                insert.setString(3, seed.topicCode());
                insert.setString(4, seed.category());
                insert.setString(5, seed.level());
                insert.setString(6, seed.title());
                insert.setString(7, seed.content());
                insert.setString(8, seed.summary());
                insert.setString(9, seed.actionType());
                insert.setString(10, seed.actionTarget());
                insert.setString(11, seed.actionPayloadJson());
                insert.setString(12, seed.actionText());
                insert.setString(13, seed.status());
                insert.setTimestamp(14, toTimestamp(seed.readAt()));
                insert.setTimestamp(15, toTimestamp(seed.archivedAt()));
                insert.setTimestamp(16, Timestamp.valueOf(seed.createdAt()));
                insert.setString(17, seed.avatar());
                insert.executeUpdate();
            }
        }
    }

    private void ensureIndex(Connection connection, String tableName, String indexName, String ddl) throws SQLException {
        if (indexExists(connection, tableName, indexName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getTables(null, null, null, new String[]{"TABLE"})) {
            while (resultSet.next()) {
                String actualTableName = resultSet.getString("TABLE_NAME");
                if (tableName.equalsIgnoreCase(actualTableName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getIndexInfo(null, null, tableName, false, false)) {
            while (resultSet.next()) {
                String actualIndexName = resultSet.getString("INDEX_NAME");
                if (actualIndexName != null && indexName.equalsIgnoreCase(actualIndexName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean exists(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setString(1, value);
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private record NotificationSeed(
        String id,
        String userId,
        String topicCode,
        String category,
        String level,
        String title,
        String content,
        String summary,
        String actionType,
        String actionTarget,
        String actionPayloadJson,
        String actionText,
        String status,
        LocalDateTime readAt,
        LocalDateTime archivedAt,
        String avatar,
        LocalDateTime createdAt
    ) {
    }
}
