package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.company.bl.system.application.SystemManagementService;
import com.company.common.security.crypto.Sm3PasswordEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class SystemManagementControllerIntegrationTest extends AuthenticatedWebIntegrationTest {
    private static final String USER_M1_ADMIN = "USER_M1_ADMIN";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SystemManagementService systemManagementService;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

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

        Map<String, Object> passwordRow = jdbcTemplate.queryForMap("""
            select password, password_algo, password_salt
            from users
            where id = :userId
            """, Map.of("userId", userId));
        assertEquals("SM3", passwordRow.get("password_algo"));
        assertNotNull(passwordRow.get("password_salt"));
        assertNotEquals("123456", passwordRow.get("password"));

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
    void shouldExposeRoleAuthorizationAndMenus() throws Exception {
        mockMvc.perform(asAdmin(get("/api/v1/roles/ROLE_PATHOLOGY_ADMIN/authorizations")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roleId", is("ROLE_PATHOLOGY_ADMIN")))
            .andExpect(jsonPath("$.data.permissionIds.length()", greaterThanOrEqualTo(1)));

        MvcResult menusResult = mockMvc.perform(asAdmin(get("/api/v1/menus")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)))
            .andReturn();

        JsonNode menusNode = objectMapper.readTree(menusResult.getResponse().getContentAsString()).path("data");
        Map<String, JsonNode> menusById = new HashMap<>();
        Iterator<JsonNode> iterator = menusNode.elements();
        while (iterator.hasNext()) {
            JsonNode menuNode = iterator.next();
            menusById.put(menuNode.path("id").asText(), menuNode);
        }

        assertEquals("/system", menusById.get("MENU_SYSTEM").path("path").asText());
        assertEquals("/system/users", menusById.get("MENU_SYS_USERS").path("path").asText());
        assertEquals("/system/medical-order-dicts", menusById.get("MENU_ORDER_DICTS").path("path").asText());
        assertEquals("/system/medical-order-charges", menusById.get("MENU_ORDER_CHARGES").path("path").asText());
        assertEquals("SystemUsers", menusById.get("MENU_SYS_USERS").path("componentName").asText());
        assertEquals("MedicalOrderCharges", menusById.get("MENU_ORDER_CHARGES").path("componentName").asText());
    }

    @Test
    void shouldSeedBuiltInUsersWithLoginReadySm3Passwords() {
        assertSeededPassword("USER_M1_ADMIN");
        assertSeededPassword("USER_M2_REGISTER");
        assertSeededPassword("USER_M3_GROSSING");
    }
    @Test
    void shouldSeedRoleMenusForBuiltInAdminAndWorkflowRoles() throws Exception {
        mockMvc.perform(asAdmin(get("/api/v1/roles/ROLE_PATHOLOGY_ADMIN/authorizations")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.menuIds", hasItems(
                "MENU_SYSTEM",
                "MENU_SYS_USERS",
                "MENU_M2_WORKFLOW",
                "MENU_M2_CLINICAL",
                "MENU_M3_WORKFLOW",
                "MENU_M3_GROSSING",
                "MENU_M3_TASKS")));

        mockMvc.perform(asAdmin(get("/api/v1/roles/ROLE_M2_CLINICAL_REGISTER/authorizations")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.menuIds", containsInAnyOrder(
                "MENU_M2_WORKFLOW",
                "MENU_M2_CLINICAL")));

        mockMvc.perform(asAdmin(get("/api/v1/roles/ROLE_M3_GROSSING/authorizations")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.menuIds", containsInAnyOrder(
                "MENU_M3_WORKFLOW",
                "MENU_M3_GROSSING",
                "MENU_M3_TASKS")));
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
    void shouldFilterUpdateExportImportPrintAndDeleteRole() throws Exception {
        String loginName = "manage-" + System.nanoTime();
        String userId = createUser(loginName);

        mockMvc.perform(asAdmin(patch("/api/v1/system-users/{id}", userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userCode": "UPDATED-%s",
                      "name": "Updated User",
                      "jobNo": "JOB-%s",
                      "titleName": "Chief",
                      "departmentId": "DEP-01",
                      "departmentName": "Pathology",
                      "phone": "13800138000",
                      "email": "updated@example.com",
                      "loginTagCode": "TAG-%s",
                      "enabled": true
                    }
                    """.formatted(System.nanoTime(), System.nanoTime(), System.nanoTime())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name", is("Updated User")))
            .andExpect(jsonPath("$.data.loginTagCode", containsString("TAG-")));

        mockMvc.perform(asAdmin(get("/api/v1/system-users"))
                .param("page", "1")
                .param("size", "20")
                .param("keyword", loginName)
                .param("enabled", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data.items[0].loginName", is(loginName)));

        mockMvc.perform(asAdmin(get("/api/v1/system-users/export"))
                .param("keyword", loginName))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(loginName)));

        mockMvc.perform(asAdmin(post("/api/v1/system-users/{id}/print-login-tag", userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title", containsString("登录标签")))
            .andExpect(jsonPath("$.data.content", containsString(loginName)));

        MockMultipartFile userFile = new MockMultipartFile(
            "file",
            "system-users.csv",
            "text/csv",
            ("""
                userCode,loginName,name,enabled
                IMP-%s,import-%s,Imported User,true
                """.formatted(System.nanoTime(), System.nanoTime())).getBytes());
        mockMvc.perform(asAdmin(multipart("/api/v1/system-users/import").file(userFile)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount", is(1)));

        String roleCode = "ROLE-" + System.nanoTime();
        MvcResult roleResult = mockMvc.perform(asAdmin(post("/api/v1/roles"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "roleCode": "%s",
                      "roleName": "Role For Delete",
                      "enabled": true
                    }
                    """.formatted(roleCode)))
            .andExpect(status().isOk())
            .andReturn();
        String roleId = objectMapper.readTree(roleResult.getResponse().getContentAsString()).path("data").path("id").asText();

        mockMvc.perform(asAdmin(patch("/api/v1/roles/{id}", roleId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "roleCode": "%s",
                      "roleName": "Role Updated",
                      "roleType": "BIZ",
                      "dataScope": "ALL",
                      "remarks": "updated",
                      "enabled": true
                    }
                    """.formatted(roleCode)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roleName", is("Role Updated")));
        mockMvc.perform(asAdmin(delete("/api/v1/roles/{id}", roleId)))
            .andExpect(status().isOk());
    }
    private String createUser(String loginName) throws Exception {
        MvcResult createResult = mockMvc.perform(asAdmin(post("/api/v1/system-users"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userCode": "UC-%s",
                      "loginName": "%s",
                      "name": "Login Test User",
                      "enabled": true
                    }
                    """.formatted(System.nanoTime(), loginName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("SUCCESS")))
            .andExpect(jsonPath("$.data.id", notNullValue()))
            .andReturn();
        JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        return createNode.path("data").path("id").asText();
    }
    private void assertSeededPassword(String userId) {
        Map<String, Object> passwordRow = jdbcTemplate.queryForMap("""
            select password, password_algo, password_salt
            from users
            where id = :userId
            """, Map.of("userId", userId));
        Object passwordValue = passwordRow.get("password");
        Object passwordAlgoValue = passwordRow.get("password_algo");
        Object passwordSaltValue = passwordRow.get("password_salt");

        assertNotNull(passwordValue);
        assertNotNull(passwordAlgoValue);
        assertNotNull(passwordSaltValue);
        assertEquals("SM3", passwordAlgoValue);
        assertTrue(new Sm3PasswordEncoder().matchesSm3(
            "123456",
            passwordSaltValue.toString(),
            passwordValue.toString()));
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder requestBuilder) {
        return authorized(requestBuilder, USER_M1_ADMIN);
    }
}
