package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.company.bl.system.application.SystemManagementService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                      "loginName": "%s",
                      "name": "Test User",
                      "password": "123456",
                      "enabled": true
                    }
                    """.formatted(loginName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("SUCCESS")))
            .andExpect(jsonPath("$.data.id", notNullValue()))
            .andExpect(jsonPath("$.data.userCode", startsWith("USER-")))
            .andExpect(jsonPath("$.data.loginTagCode", startsWith("LT-")))
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

        mockMvc.perform(asAdmin(patch("/api/v1/system-users/{id}", userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userCode": "USER-MUTATED",
                      "loginTagCode": "LT-MUTATED",
                      "name": "Test User",
                      "enabled": true
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("cannot be changed once created")));
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

        mockMvc.perform(asAdmin(get("/api/v1/system/logs/login"))
                .param("page", "0")
                .param("size", "999")
                .param("loginName", loginName)
                .param("result", "FAILED")
                .param("ip", "10.0.0.2")
                .param("clientDevice", "Unknown"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.page", is(1)))
            .andExpect(jsonPath("$.data.size", is(100)))
            .andExpect(jsonPath("$.data.total", is(1)))
            .andExpect(jsonPath("$.data.items[0].loginName", is(loginName)))
            .andExpect(jsonPath("$.data.items[0].loginResult", is("FAILED")))
            .andExpect(jsonPath("$.data.items[0].clientIp", is("10.0.0.2")));

        String failedLogId = jdbcTemplate.queryForObject("""
            select id
            from user_login_logs
            where login_name = :loginName and login_result = 'FAILED'
            """, Map.of("loginName", loginName), String.class);
        mockMvc.perform(asAdmin(get("/api/v1/system/logs/login/{id}", failedLogId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id", is(failedLogId)))
            .andExpect(jsonPath("$.data.failureReason", is("bad password")));

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
    void shouldExposeOperationLogsWithMaskedDetailAndAuditSensitiveQueries() throws Exception {
        String operationId = "OP-LOG-MASK-" + System.nanoTime();
        jdbcTemplate.update("""
            insert into operation_logs
                (id, module_code, business_type, business_id, operation_name, operation_result,
                 operator_user_id, operator_name, operator_ip, operation_at, operation_content, failure_reason)
            values
                (:id, :moduleCode, :businessType, :businessId, :operationName, :operationResult,
                 :operatorUserId, :operatorName, :operatorIp, :operationAt, :operationContent, :failureReason)
            """, new MapSqlParameterSource()
            .addValue("id", operationId)
            .addValue("moduleCode", "M2")
            .addValue("businessType", "APPLICATION")
            .addValue("businessId", "APP-LOG-1")
            .addValue("operationName", "update_application")
            .addValue("operationResult", "FAILED")
            .addValue("operatorUserId", USER_M1_ADMIN)
            .addValue("operatorName", "病理科管理员")
            .addValue("operatorIp", "10.20.30.40")
            .addValue("operationAt", LocalDateTime.of(2026, 5, 20, 8, 30, 0))
            .addValue("operationContent", "password=secret&token=abc&field=visible")
            .addValue("failureReason", "token=abc failed"));

        mockMvc.perform(asAdmin(get("/api/v1/system/logs/operations"))
                .param("page", "1")
                .param("size", "20")
                .param("moduleCode", "M2")
                .param("businessType", "APPLICATION")
                .param("result", "FAILED")
                .param("operatorKeyword", "管理员")
                .param("contentKeyword", "password"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data.items[0].id", is(operationId)))
            .andExpect(jsonPath("$.data.items[0].operationContent").doesNotExist());

        MvcResult detailResult = mockMvc.perform(asAdmin(get("/api/v1/system/logs/operations/{id}", operationId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id", is(operationId)))
            .andExpect(jsonPath("$.data.failureReason", containsString("token=***")))
            .andReturn();
        String detailBody = detailResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(detailBody.contains("password=***"));
        assertTrue(detailBody.contains("token=***"));
        assertFalse(detailBody.contains("secret"));
        assertFalse(detailBody.contains("token=abc"));

        long queryAuditCountBefore = operationLogCount("query_operation_logs");
        mockMvc.perform(asAdmin(get("/api/v1/system/logs/operations"))
                .param("page", "1")
                .param("size", "1"))
            .andExpect(status().isOk());
        assertEquals(queryAuditCountBefore + 1, operationLogCount("query_operation_logs"));

        mockMvc.perform(authorized(get("/api/v1/system/logs/operations"), "USER_M1_DOCTOR")
                .param("page", "1")
                .param("size", "20"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void shouldAuditFailedAuthenticatedWriteRequests() throws Exception {
        long failedAuditCountBefore = operationLogCount("post /api/v1/system-users");

        mockMvc.perform(asAdmin(post("/api/v1/system-users"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "loginName": "",
                      "name": "",
                      "password": "secret",
                      "enabled": true
                    }
                    """))
            .andExpect(status().isBadRequest());

        Map<String, Object> latestAudit = jdbcTemplate.queryForMap("""
            select operation_result, operation_content, failure_reason
            from operation_logs
            where operation_name = 'post /api/v1/system-users'
            order by operation_at desc
            limit 1
            """, Map.of());
        assertEquals(failedAuditCountBefore + 1, operationLogCount("post /api/v1/system-users"));
        assertEquals("FAILED", latestAudit.get("operation_result"));
        String content = latestAudit.get("operation_content").toString();
        assertTrue(content.contains("POST"));
        assertFalse(content.contains("secret"));
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

    @Test
    void shouldReturnRowLevelErrorsForSystemUserImport() throws Exception {
        String successLoginName = "import-ok-" + System.nanoTime();
        MockMultipartFile importFile = new MockMultipartFile(
            "file",
            "system-users-invalid.csv",
            "text/csv",
            ("""
                userCode,loginName,name,enabled
                ,%s,Imported User,true
                ,,Missing Login,true
                """.formatted(successLoginName)).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(asAdmin(multipart("/api/v1/system-users/import").file(importFile)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount", is(1)))
            .andExpect(jsonPath("$.data.failureCount", is(1)))
            .andExpect(jsonPath("$.data.errors[0].rowNumber", is(3)))
            .andExpect(jsonPath("$.data.errors[0].field", is("loginName")))
            .andExpect(jsonPath("$.data.errors[0].message", containsString("must not be blank")));
    }

    @Test
    void shouldAuditAuthenticatedOperatorAndFallbackToSystemForNonWebCalls() throws Exception {
        String webLoginName = "audit-web-" + System.nanoTime();
        MvcResult webCreateResult = mockMvc.perform(asAdmin(post("/api/v1/system-users"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "loginName": "%s",
                      "name": "Audit Web User",
                      "password": "123456",
                      "enabled": true
                    }
                    """.formatted(webLoginName)))
            .andExpect(status().isOk())
            .andReturn();
        String webUserId = objectMapper.readTree(webCreateResult.getResponse().getContentAsString())
            .path("data").path("id").asText();

        Map<String, Object> webAudit = jdbcTemplate.queryForMap("""
            select operator_user_id, operator_name
            from operation_logs
            where module_code = 'SYSTEM'
              and operation_name = 'create_user'
              and business_id = :businessId
            order by operation_at desc
            limit 1
            """, Map.of("businessId", webUserId));
        String expectedAdminName = jdbcTemplate.queryForObject("""
            select name
            from users
            where id = :userId
            """, Map.of("userId", USER_M1_ADMIN), String.class);
        assertEquals(USER_M1_ADMIN, webAudit.get("operator_user_id"));
        assertEquals(expectedAdminName, webAudit.get("operator_name"));

        String systemLoginName = "audit-system-" + System.nanoTime();
        String systemUserId = systemManagementService.createUser(new SystemManagementService.CreateUserCommand(
            null,
            systemLoginName,
            "Audit System User",
            "123456",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true)).id();

        Map<String, Object> systemAudit = jdbcTemplate.queryForMap("""
            select operator_user_id, operator_name
            from operation_logs
            where module_code = 'SYSTEM'
              and operation_name = 'create_user'
              and business_id = :businessId
            order by operation_at desc
            limit 1
            """, Map.of("businessId", systemUserId));
        assertNull(systemAudit.get("operator_user_id"));
        assertEquals("system", systemAudit.get("operator_name"));
    }

    @Test
    void shouldExposeBuiltInUsersWithChineseDisplayNames() throws Exception {
        MvcResult result = mockMvc.perform(asAdmin(get("/api/v1/system-users"))
                .param("page", "1")
                .param("size", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("SUCCESS")))
            .andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
            .path("data")
            .path("items");
        Map<String, String> namesById = new HashMap<>();
        for (JsonNode item : items) {
            namesById.put(item.path("id").asText(), item.path("name").asText());
        }

        assertEquals("病理科管理员", namesById.get("USER_M1_ADMIN"));
        assertEquals("标本登记员", namesById.get("USER_M2_REGISTER"));
        assertEquals("取材员", namesById.get("USER_M3_GROSSING"));
        assertEquals("诊断医生", namesById.get("USER_M4_DIAGNOSIS"));
        assertEquals("医嘱执行员", namesById.get("USER_M4_ORDER_EXECUTE"));
    }

    private long operationLogCount(String operationName) {
        Long count = jdbcTemplate.queryForObject("""
            select count(*)
            from operation_logs
            where operation_name = :operationName
            """, Map.of("operationName", operationName), Long.class);
        return count == null ? 0L : count;
    }
}
