package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.company.bl.system.application.SystemManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class M1RoleAuthorizationIntegrationTest extends AuthenticatedWebIntegrationTest {

    private static final String USER_M1_ADMIN = "USER_M1_ADMIN";
    private static final String USER_M1_DOCTOR = "USER_M1_DOCTOR";
    private static final String USER_M1_TECHNICIAN = "USER_M1_TECHNICIAN";
    private static final String USER_M1_ARCHIVE = "USER_M1_ARCHIVE";
    private static final String USER_M1_REAGENT = "USER_M1_REAGENT";
    private static final String USER_M1_QUALITY = "USER_M1_QUALITY";
    private static final String USER_M1_NO_PERMISSION = "USER_M1_NO_PERMISSION";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SystemManagementService systemManagementService;

    @Test
    void shouldRequireHeaderAndRejectUnauthorizedM1Access() throws Exception {
        mockMvc.perform(get("/api/v1/system-users"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(asUser(get("/api/v1/system-users"), USER_M1_NO_PERMISSION))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void shouldAllowAdminAcrossM1ProtectedFlows() throws Exception {
        String loginName = "m1-admin-flow-" + System.nanoTime();

        mockMvc.perform(asUser(post("/api/v1/system-users"), USER_M1_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userCode": "UC-%s",
                      "loginName": "%s",
                      "name": "Admin Flow User",
                      "enabled": true
                    }
                    """.formatted(System.nanoTime(), loginName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id", notNullValue()));

        mockMvc.perform(asUser(get("/api/v1/roles/ROLE_PATHOLOGY_ADMIN/authorizations"), USER_M1_ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roleId", is("ROLE_PATHOLOGY_ADMIN")))
            .andExpect(jsonPath("$.data.permissionIds.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(asUser(get("/api/v1/permissions"), USER_M1_ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(asUser(get("/api/v1/message-topics"), USER_M1_ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(asUser(get("/api/v1/stat-categories"), USER_M1_ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(asUser(get("/api/v1/body-parts"), USER_M1_ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id", is("BP_ROOT")));

        mockMvc.perform(asUser(get("/api/v1/sampling-templates/ST_HE_STOMACH"), USER_M1_ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id", is("ST_HE_STOMACH")));

        mockMvc.perform(asUser(get("/api/v1/check-item-rules"), USER_M1_ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(25)));

        mockMvc.perform(asUser(patch("/api/v1/system-configs/items/SCI_TEMPLATE_MATCH"), USER_M1_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "configValue": "true",
                      "enabled": true,
                      "remarks": "m1 admin auth test"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id", is("SCI_TEMPLATE_MATCH")));

        mockMvc.perform(asUser(patch("/api/v1/numbering-rules/NR_APPLICATION"), USER_M1_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "prefixPattern": "APY",
                      "datePattern": "yyyyMMdd",
                      "seqLength": 4,
                      "resetPolicy": "DAILY",
                      "scopeType": "GLOBAL",
                      "enabled": true,
                      "remarks": "m1 auth"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id", is("NR_APPLICATION")));
    }

    @Test
    void shouldRejectNonAdminRolesFromM1ProtectedEndpoints() throws Exception {
        mockMvc.perform(asUser(get("/api/v1/system-users"), USER_M1_DOCTOR))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        mockMvc.perform(asUser(get("/api/v1/body-parts"), USER_M1_TECHNICIAN))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        mockMvc.perform(asUser(post("/api/v1/medical-order-packages"), USER_M1_REAGENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "packageCode": "PK-FORBIDDEN",
                      "packageName": "Forbidden Package",
                      "packageType": "PRIVATE",
                      "enabled": true,
                      "itemIds": ["ODI_HE"]
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        mockMvc.perform(asUser(patch("/api/v1/system-configs/items/SCI_TEMPLATE_MATCH"), USER_M1_ARCHIVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "configValue": "false",
                      "enabled": true,
                      "remarks": "forbidden"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        mockMvc.perform(asUser(get("/api/v1/numbering-rules"), USER_M1_QUALITY))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        mockMvc.perform(asUser(get("/api/v1/check-item-rules"), USER_M1_QUALITY))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void shouldOnlyAllowAdminToQueryLoginLogs() throws Exception {
        String loginName = "m1-login-" + System.nanoTime();
        String userId = systemManagementService.createUser(new SystemManagementService.CreateUserCommand(
            "UC-" + System.nanoTime(),
            loginName,
            "Login Query User",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true)).id();

        systemManagementService.recordUserLogin(new SystemManagementService.RecordUserLoginCommand(
            userId, loginName, "SUCCESS", "10.10.0.1", "Chrome", null, "login success",
            LocalDateTime.of(2026, 5, 19, 9, 0, 0)));

        mockMvc.perform(asUser(get("/api/v1/system-users/{id}/login-logs", userId), USER_M1_ADMIN)
                .param("page", "1")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total", is(1)))
            .andExpect(jsonPath("$.data.items[0].clientIp", is("10.10.0.1")));

        mockMvc.perform(asUser(get("/api/v1/system-users/{id}/login-logs", userId), USER_M1_DOCTOR)
                .param("page", "1")
                .param("size", "20"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder requestBuilder, String userId) {
        return authorized(requestBuilder, userId);
    }
}
