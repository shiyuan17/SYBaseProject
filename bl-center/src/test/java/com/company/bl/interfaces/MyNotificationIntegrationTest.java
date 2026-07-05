package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class MyNotificationIntegrationTest extends AuthenticatedWebIntegrationTest {

    private static final String USER_M1_ADMIN = "USER_M1_ADMIN";
    private static final String USER_M1_DOCTOR = "USER_M1_DOCTOR";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRequireAuthenticationBeforeValidatingPreferenceUpdatePayload() throws Exception {
        mockMvc.perform(put("/api/v1/my/notification-preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("AUTHENTICATION_REQUIRED")));
    }

    @Test
    void shouldListOnlyCurrentUserNotifications() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/my/notifications"), USER_M1_ADMIN)
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.data.items[*].id", not(hasSize(0))))
            .andExpect(jsonPath("$.data.items[*].topicCode").isArray());

        mockMvc.perform(authorized(get("/api/v1/my/notifications"), USER_M1_DOCTOR)
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].id", is("NOTIFY_DOCTOR_REPORT_REVISION")))
            .andExpect(jsonPath("$.data.total", is(1)));
    }

    @Test
    void shouldReturnUnreadCountAndMarkReadIdempotently() throws Exception {
        resetAdminPreferences(true, true, true);
        insertNotification(
            "NOTIFY_ADMIN_UNREAD_COUNT_1",
            USER_M1_ADMIN,
            "REPORT_REVISION",
            "SYSTEM_MESSAGE",
            "UNREAD"
        );
        insertNotification(
            "NOTIFY_ADMIN_UNREAD_COUNT_2",
            USER_M1_ADMIN,
            "CRITICAL_VALUE",
            "TODO_TASK",
            "UNREAD"
        );

        mockMvc.perform(authorized(get("/api/v1/my/notifications/unread-count"), USER_M1_ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.unreadCount", greaterThanOrEqualTo(2)));

        mockMvc.perform(authorized(patch("/api/v1/my/notifications/NOTIFY_ADMIN_REPORT_REVISION/read"), USER_M1_ADMIN))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(patch("/api/v1/my/notifications/NOTIFY_ADMIN_REPORT_REVISION/read"), USER_M1_ADMIN))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/v1/my/notifications"), USER_M1_ADMIN)
                .param("page", "1")
                .param("size", "10")
                .param("status", "READ"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[?(@.id=='NOTIFY_ADMIN_REPORT_REVISION')].readAt[0]", notNullValue()));
    }

    @Test
    void shouldMarkAllReadArchiveSingleAndArchiveBatch() throws Exception {
        resetAdminPreferences(true, true, true);
        insertNotification(
            "NOTIFY_ADMIN_BATCH_ARCHIVE_1",
            USER_M1_ADMIN,
            "REPORT_REVISION",
            "SYSTEM_MESSAGE",
            "UNREAD"
        );
        insertNotification(
            "NOTIFY_ADMIN_BATCH_ARCHIVE_2",
            USER_M1_ADMIN,
            "CRITICAL_VALUE",
            "TODO_TASK",
            "UNREAD"
        );

        mockMvc.perform(authorized(patch("/api/v1/my/notifications/read-all"), USER_M1_ADMIN))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/v1/my/notifications"), USER_M1_ADMIN)
                .param("page", "1")
                .param("size", "20")
                .param("status", "UNREAD"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total", is(0)));

        mockMvc.perform(authorized(patch("/api/v1/my/notifications/NOTIFY_ADMIN_BATCH_ARCHIVE_1/archive"), USER_M1_ADMIN))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(patch("/api/v1/my/notifications/archive"), USER_M1_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "notificationIds": ["NOTIFY_ADMIN_BATCH_ARCHIVE_2"]
                    }
                    """))
            .andExpect(status().isOk());

        Long archivedCount = jdbcTemplate.queryForObject("""
            select count(*)
            from user_notifications
            where id in (:ids)
              and status = 'ARCHIVED'
              and archived_at is not null
            """, Map.of("ids", List.of("NOTIFY_ADMIN_BATCH_ARCHIVE_1", "NOTIFY_ADMIN_BATCH_ARCHIVE_2")), Long.class);
        org.junit.jupiter.api.Assertions.assertEquals(2L, archivedCount == null ? 0L : archivedCount);
    }

    @Test
    void shouldGetAndUpdatePreferences() throws Exception {
        resetAdminPreferences(true, true, true);
        mockMvc.perform(authorized(get("/api/v1/my/notification-preferences"), USER_M1_ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accountPassword", is(true)))
            .andExpect(jsonPath("$.data.systemMessage", is(true)))
            .andExpect(jsonPath("$.data.todoTask", is(true)));

        mockMvc.perform(authorized(put("/api/v1/my/notification-preferences"), USER_M1_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "accountPassword": true,
                      "systemMessage": false,
                      "todoTask": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.systemMessage", is(false)));

        mockMvc.perform(authorized(get("/api/v1/my/notifications"), USER_M1_ADMIN)
                .param("page", "1")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[*].category", not(org.hamcrest.Matchers.hasItem("SYSTEM_MESSAGE"))));
    }

    @Test
    void shouldFilterNotificationsByAuthorizedTopicCodes() throws Exception {
        insertNotification(
            "NOTIFY_DOCTOR_UNAUTHORIZED_TOPIC",
            USER_M1_DOCTOR,
            "CRITICAL_VALUE",
            "SYSTEM_MESSAGE",
            "UNREAD"
        );

        mockMvc.perform(authorized(get("/api/v1/my/notifications"), USER_M1_DOCTOR)
                .param("page", "1")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[?(@.id=='NOTIFY_DOCTOR_UNAUTHORIZED_TOPIC')]").isEmpty());
    }

    @Test
    void shouldAuditSensitiveNotificationQueryWithoutLeakingNotificationBody() throws Exception {
        resetAdminPreferences(true, true, true);
        insertNotification(
            "NOTIFY_ADMIN_AUDIT_QUERY",
            USER_M1_ADMIN,
            "REPORT_REVISION",
            "SYSTEM_MESSAGE",
            "UNREAD"
        );

        long auditCountBefore = operationLogCount("query_my_notifications");

        mockMvc.perform(authorized(get("/api/v1/my/notifications"), USER_M1_ADMIN)
                .param("page", "1")
                .param("size", "10")
                .param("category", "SYSTEM_MESSAGE")
                .param("keyword", "集成测试通知正文"))
            .andExpect(status().isOk());

        assertThat(operationLogCount("query_my_notifications")).isEqualTo(auditCountBefore + 1);
        Map<String, Object> latestAudit = latestOperationLog("query_my_notifications");
        assertThat(latestAudit.get("operation_result")).isEqualTo("SUCCESS");
        assertThat(latestAudit.get("operation_content").toString())
            .contains("category=SYSTEM_MESSAGE")
            .contains("keywordPresent=true")
            .doesNotContain("集成测试通知正文")
            .doesNotContain("NOTIFY_ADMIN_AUDIT_QUERY");
    }

    @Test
    void shouldAuditNotificationMutationsWithSummaryOnly() throws Exception {
        resetAdminPreferences(true, true, true);
        insertNotification(
            "NOTIFY_ADMIN_AUDIT_READ",
            USER_M1_ADMIN,
            "REPORT_REVISION",
            "SYSTEM_MESSAGE",
            "UNREAD"
        );
        insertNotification(
            "NOTIFY_ADMIN_AUDIT_ARCHIVE",
            USER_M1_ADMIN,
            "REPORT_REVISION",
            "SYSTEM_MESSAGE",
            "UNREAD"
        );

        long readAuditBefore = operationLogCount("mark_my_notification_read");
        mockMvc.perform(authorized(patch("/api/v1/my/notifications/NOTIFY_ADMIN_AUDIT_READ/read"), USER_M1_ADMIN))
            .andExpect(status().isOk());
        assertThat(operationLogCount("mark_my_notification_read")).isEqualTo(readAuditBefore + 1);
        assertThat(latestOperationLog("mark_my_notification_read").get("operation_content").toString())
            .contains("notificationId=NOTIFY_ADMIN_AUDIT_READ")
            .doesNotContain("集成测试通知正文");

        long readAllAuditBefore = operationLogCount("mark_all_my_notifications_read");
        mockMvc.perform(authorized(patch("/api/v1/my/notifications/read-all"), USER_M1_ADMIN))
            .andExpect(status().isOk());
        assertThat(operationLogCount("mark_all_my_notifications_read")).isEqualTo(readAllAuditBefore + 1);
        assertThat(latestOperationLog("mark_all_my_notifications_read").get("operation_content").toString())
            .contains("scope=visible-unread")
            .doesNotContain("集成测试通知正文");

        long archiveOneAuditBefore = operationLogCount("archive_my_notification");
        mockMvc.perform(authorized(patch("/api/v1/my/notifications/NOTIFY_ADMIN_AUDIT_READ/archive"), USER_M1_ADMIN))
            .andExpect(status().isOk());
        assertThat(operationLogCount("archive_my_notification")).isEqualTo(archiveOneAuditBefore + 1);
        assertThat(latestOperationLog("archive_my_notification").get("operation_content").toString())
            .contains("notificationId=NOTIFY_ADMIN_AUDIT_READ")
            .doesNotContain("集成测试通知正文");

        long archiveManyAuditBefore = operationLogCount("archive_my_notifications");
        mockMvc.perform(authorized(patch("/api/v1/my/notifications/archive"), USER_M1_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "notificationIds": ["NOTIFY_ADMIN_AUDIT_ARCHIVE"]
                    }
                    """))
            .andExpect(status().isOk());
        assertThat(operationLogCount("archive_my_notifications")).isEqualTo(archiveManyAuditBefore + 1);
        assertThat(latestOperationLog("archive_my_notifications").get("operation_content").toString())
            .contains("notificationCount=1")
            .doesNotContain("NOTIFY_ADMIN_AUDIT_ARCHIVE")
            .doesNotContain("集成测试通知正文");

        long preferenceAuditBefore = operationLogCount("update_my_notification_preferences");
        mockMvc.perform(authorized(put("/api/v1/my/notification-preferences"), USER_M1_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "accountPassword": false,
                      "systemMessage": true,
                      "todoTask": false
                    }
                    """))
            .andExpect(status().isOk());
        assertThat(operationLogCount("update_my_notification_preferences")).isEqualTo(preferenceAuditBefore + 1);
        assertThat(latestOperationLog("update_my_notification_preferences").get("operation_content").toString())
            .contains("accountPassword=false")
            .contains("systemMessage=true")
            .contains("todoTask=false");
    }

    @Test
    void shouldKeepOtherUsersNotificationsUnchangedWhenIdsAreForged() throws Exception {
        insertNotification(
            "NOTIFY_DOCTOR_FORGED_READ",
            USER_M1_DOCTOR,
            "CRITICAL_VALUE",
            "SYSTEM_MESSAGE",
            "UNREAD"
        );
        insertNotification(
            "NOTIFY_DOCTOR_FORGED_ARCHIVE_1",
            USER_M1_DOCTOR,
            "CRITICAL_VALUE",
            "SYSTEM_MESSAGE",
            "UNREAD"
        );
        insertNotification(
            "NOTIFY_DOCTOR_FORGED_ARCHIVE_2",
            USER_M1_DOCTOR,
            "CRITICAL_VALUE",
            "SYSTEM_MESSAGE",
            "UNREAD"
        );

        long readAuditBefore = operationLogCount("mark_my_notification_read");
        mockMvc.perform(authorized(patch("/api/v1/my/notifications/NOTIFY_DOCTOR_FORGED_READ/read"), USER_M1_ADMIN))
            .andExpect(status().isOk());
        assertThat(operationLogCount("mark_my_notification_read")).isEqualTo(readAuditBefore + 1);

        Map<String, Object> readTarget = jdbcTemplate.queryForMap("""
            select user_id, status, read_at
            from user_notifications
            where id = :notificationId
            """, Map.of("notificationId", "NOTIFY_DOCTOR_FORGED_READ"));
        assertThat(readTarget.get("user_id")).isEqualTo(USER_M1_DOCTOR);
        assertThat(readTarget.get("status")).isEqualTo("UNREAD");
        assertThat(readTarget.get("read_at")).isNull();

        long archiveAuditBefore = operationLogCount("archive_my_notifications");
        mockMvc.perform(authorized(patch("/api/v1/my/notifications/archive"), USER_M1_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "notificationIds": [
                        "NOTIFY_DOCTOR_FORGED_ARCHIVE_1",
                        "NOTIFY_DOCTOR_FORGED_ARCHIVE_2"
                      ]
                    }
                    """))
            .andExpect(status().isOk());
        assertThat(operationLogCount("archive_my_notifications")).isEqualTo(archiveAuditBefore + 1);
        assertThat(latestOperationLog("archive_my_notifications").get("operation_content").toString())
            .contains("notificationCount=2")
            .doesNotContain("NOTIFY_DOCTOR_FORGED_ARCHIVE_1")
            .doesNotContain("NOTIFY_DOCTOR_FORGED_ARCHIVE_2");

        Long archivedCount = jdbcTemplate.queryForObject("""
            select count(*)
            from user_notifications
            where id in (:ids)
              and user_id = :userId
              and status = 'ARCHIVED'
            """, Map.of(
            "ids", List.of("NOTIFY_DOCTOR_FORGED_ARCHIVE_1", "NOTIFY_DOCTOR_FORGED_ARCHIVE_2"),
            "userId", USER_M1_DOCTOR
        ), Long.class);
        assertThat(archivedCount).isZero();
    }

    private void insertNotification(
        String id,
        String userId,
        String topicCode,
        String category,
        String status
    ) {
        jdbcTemplate.update("""
            insert into user_notifications
                (id, user_id, topic_code, category, level, title, content, summary,
                 avatar, action_type, action_target, action_payload_json, action_text,
                 status, read_at, archived_at, created_at)
            values
                (:id, :userId, :topicCode, :category, 'MEDIUM', :title, :content, :summary,
                 :avatar, 'ROUTE', '/notifications', '{"query":{"id":"seed"}}', '查看',
                 :status, null, null, :createdAt)
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("userId", userId)
            .addValue("topicCode", topicCode)
            .addValue("category", category)
            .addValue("title", "集成测试通知")
            .addValue("content", "集成测试通知正文")
            .addValue("summary", "集成测试通知摘要")
            .addValue("avatar", "https://avatar.vercel.sh/test.svg?text=IT")
            .addValue("status", status)
            .addValue("createdAt", LocalDateTime.now()));
    }

    private void resetAdminPreferences(
        boolean accountPasswordEnabled,
        boolean systemMessageEnabled,
        boolean todoTaskEnabled
    ) {
        jdbcTemplate.update("""
            update user_notification_preferences
            set account_password_enabled = :accountPasswordEnabled,
                system_message_enabled = :systemMessageEnabled,
                todo_task_enabled = :todoTaskEnabled,
                updated_at = :updatedAt
            where user_id = :userId
            """, new MapSqlParameterSource()
            .addValue("userId", USER_M1_ADMIN)
            .addValue("accountPasswordEnabled", accountPasswordEnabled ? 1 : 0)
            .addValue("systemMessageEnabled", systemMessageEnabled ? 1 : 0)
            .addValue("todoTaskEnabled", todoTaskEnabled ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    private long operationLogCount(String operationName) {
        Long count = jdbcTemplate.queryForObject("""
            select count(*)
            from operation_logs
            where operation_name = :operationName
            """, Map.of("operationName", operationName), Long.class);
        return count == null ? 0L : count;
    }

    private Map<String, Object> latestOperationLog(String operationName) {
        return jdbcTemplate.queryForMap("""
            select operation_result, operation_content, failure_reason
            from operation_logs
            where operation_name = :operationName
            order by operation_at desc
            limit 1
            """, Map.of("operationName", operationName));
    }
}
