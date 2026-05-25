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
}
