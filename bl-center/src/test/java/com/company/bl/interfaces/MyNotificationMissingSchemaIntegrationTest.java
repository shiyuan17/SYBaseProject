package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:blcenter_missing_notification_schema;MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
})
@DirtiesContext
class MyNotificationMissingSchemaIntegrationTest extends AuthenticatedWebIntegrationTest {

    private static final String USER_M1_ADMIN = "USER_M1_ADMIN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shouldFallbackWhenNotificationCenterTablesAreMissing() throws Exception {
        jdbcTemplate.getJdbcTemplate().execute("drop table user_notification_preferences");
        jdbcTemplate.getJdbcTemplate().execute("drop table user_notifications");

        mockMvc.perform(authorized(get("/api/v1/my/notifications"), USER_M1_ADMIN)
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total", is(0)))
            .andExpect(jsonPath("$.data.items", hasSize(0)));

        mockMvc.perform(authorized(get("/api/v1/my/notifications/unread-count"), USER_M1_ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.unreadCount", is(0)));

        mockMvc.perform(authorized(get("/api/v1/my/notification-preferences"), USER_M1_ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accountPassword", is(true)))
            .andExpect(jsonPath("$.data.systemMessage", is(true)))
            .andExpect(jsonPath("$.data.todoTask", is(true)));
    }
}
