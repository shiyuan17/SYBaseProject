package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class SystemManagementRoleAndMenuIntegrationTest extends AbstractSystemManagementIntegrationTest {

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

        org.junit.jupiter.api.Assertions.assertEquals("/system", menusById.get("MENU_SYSTEM").path("path").asText());
        org.junit.jupiter.api.Assertions.assertEquals("/system/users", menusById.get("MENU_SYS_USERS").path("path").asText());
        org.junit.jupiter.api.Assertions.assertEquals("/system/departments", menusById.get("MENU_DEPARTMENTS").path("path").asText());
        org.junit.jupiter.api.Assertions.assertEquals("/system/medical-order-dicts", menusById.get("MENU_ORDER_DICTS").path("path").asText());
        org.junit.jupiter.api.Assertions.assertEquals("/system/medical-order-charges", menusById.get("MENU_ORDER_CHARGES").path("path").asText());
        org.junit.jupiter.api.Assertions.assertEquals("SystemUsers", menusById.get("MENU_SYS_USERS").path("componentName").asText());
        org.junit.jupiter.api.Assertions.assertEquals("Departments", menusById.get("MENU_DEPARTMENTS").path("componentName").asText());
        org.junit.jupiter.api.Assertions.assertEquals("MedicalOrderCharges", menusById.get("MENU_ORDER_CHARGES").path("componentName").asText());
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

        MockMultipartFile userFile = buildImportFile("import-" + System.nanoTime());
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
}
