package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.company.bl.system.application.SystemManagementService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class SystemManagementUserIntegrationTest extends AbstractSystemManagementIntegrationTest {

    @Test
    void shouldCreateUserAndAssignRole() throws Exception {
        String loginName = "user-" + System.nanoTime();
        MvcResult createResult = mockMvc.perform(asAdmin(post("/api/v1/system-users"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userCode": "UC-%s",
                      "loginName": "%s",
                      "name": "Test User",
                      "password": "123456",
                      "enabled": true
                    }
                    """.formatted(System.nanoTime(), loginName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("SUCCESS")))
            .andExpect(jsonPath("$.data.id", notNullValue()))
            .andReturn();

        JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String userId = createNode.path("data").path("id").asText();
        assertCreatedUserPasswordSecured(userId);

        mockMvc.perform(asAdmin(put("/api/v1/system-users/{id}/roles", userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "assignments": [
                        {
                          "roleId": "ROLE_PATHOLOGY_ADMIN",
                          "primary": true
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roles[0].roleId", is("ROLE_PATHOLOGY_ADMIN")))
            .andExpect(jsonPath("$.data.roles[0].primary", is(true)));
    }

    @Test
    void shouldSeedBuiltInUsersWithLoginReadySm3Passwords() {
        assertSeededPassword("USER_M1_ADMIN");
        assertSeededPassword("USER_M2_REGISTER");
        assertSeededPassword("USER_M3_GROSSING");
    }

    @Test
    void shouldRecordLoginLogsAndExposePagedQuery() throws Exception {
        String loginName = "login-" + System.nanoTime();
        String userId = createUser(loginName);
        LocalDateTime successAt = LocalDateTime.of(2026, 5, 19, 10, 0, 0);
        LocalDateTime failedAt = successAt.plusMinutes(5);

        systemManagementService.recordUserLogin(new SystemManagementService.RecordUserLoginCommand(
            userId, loginName, "SUCCESS", "10.0.0.1", "Chrome", null, "success login", successAt));
        systemManagementService.recordUserLogin(new SystemManagementService.RecordUserLoginCommand(
            null, loginName, "FAILED", "10.0.0.2", "Unknown", "bad password", "failed login", failedAt));

        mockMvc.perform(asAdmin(get("/api/v1/system-users/{id}/login-logs", userId))
                .param("page", "1")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total", is(1)))
            .andExpect(jsonPath("$.data.items[0].loginResult", is("SUCCESS")))
            .andExpect(jsonPath("$.data.items[0].clientIp", is("10.0.0.1")));

        Long totalLogs = jdbcTemplate.queryForObject("""
            select count(*)
            from user_login_logs
            where login_name = :loginName
            """, Map.of("loginName", loginName), Long.class);
        assertEquals(2L, totalLogs);

        SystemManagementService.UserView userView = systemManagementService.listUsers(1, 100).items().stream()
            .filter(item -> userId.equals(item.id()))
            .findFirst()
            .orElse(null);
        assertNotNull(userView);
        assertEquals(successAt.toString(), userView.lastLoginAt());
        assertEquals("10.0.0.1", userView.lastLoginIp());
        assertEquals("Chrome", userView.lastLoginDevice());
    }

    @Test
    void shouldQuerySystemUsersWithEnabledAndKeywordFilters() throws Exception {
        String loginName = "filter-" + System.nanoTime();
        createUser(loginName);

        mockMvc.perform(asAdmin(get("/api/v1/system-users"))
                .param("page", "1")
                .param("size", "20")
                .param("enabled", "true")
                .param("keyword", loginName))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("SUCCESS")))
            .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data.items[0].loginName", is(loginName)));
    }
}
