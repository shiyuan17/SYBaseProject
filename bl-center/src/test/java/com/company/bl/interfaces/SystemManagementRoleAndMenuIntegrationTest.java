package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import db.migration.V39__normalize_role_authorization_labels;
import org.flywaydb.core.api.configuration.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class SystemManagementRoleAndMenuIntegrationTest extends AbstractSystemManagementIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private DataSource dataSource;

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

        JsonNode menusNode = objectMapper.readTree(
            menusResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
        Map<String, JsonNode> menusById = new HashMap<>();
        Iterator<JsonNode> iterator = menusNode.elements();
        while (iterator.hasNext()) {
            JsonNode menuNode = iterator.next();
            menusById.put(menuNode.path("id").asText(), menuNode);
        }

        org.junit.jupiter.api.Assertions.assertEquals("/system", menusById.get("MENU_SYSTEM").path("path").asText());
        org.junit.jupiter.api.Assertions.assertEquals("/system/users", menusById.get("MENU_SYS_USERS").path("path").asText());
        org.junit.jupiter.api.Assertions.assertEquals("/system/departments", menusById.get("MENU_DEPARTMENTS").path("path").asText());
        org.junit.jupiter.api.Assertions.assertEquals("科室字典", menusById.get("MENU_DEPARTMENTS").path("menuName").asText());
        org.junit.jupiter.api.Assertions.assertEquals("/system/medical-order-dicts", menusById.get("MENU_ORDER_DICTS").path("path").asText());
        org.junit.jupiter.api.Assertions.assertEquals("/system/medical-order-charges", menusById.get("MENU_ORDER_CHARGES").path("path").asText());
        org.junit.jupiter.api.Assertions.assertEquals("SystemUsers", menusById.get("MENU_SYS_USERS").path("componentName").asText());
        org.junit.jupiter.api.Assertions.assertEquals("Departments", menusById.get("MENU_DEPARTMENTS").path("componentName").asText());
        org.junit.jupiter.api.Assertions.assertEquals("MedicalOrderCharges", menusById.get("MENU_ORDER_CHARGES").path("componentName").asText());

        MvcResult permissionsResult = mockMvc.perform(asAdmin(get("/api/v1/permissions")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)))
            .andReturn();

        JsonNode permissionsNode = objectMapper.readTree(
            permissionsResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
        Map<String, JsonNode> permissionsByCode = new HashMap<>();
        Iterator<JsonNode> permissionIterator = permissionsNode.elements();
        while (permissionIterator.hasNext()) {
            JsonNode permissionNode = permissionIterator.next();
            permissionsByCode.put(permissionNode.path("permissionCode").asText(), permissionNode);
        }

        org.junit.jupiter.api.Assertions.assertEquals(
            "查询科室字典",
            permissionsByCode.get("PERM_SYS_DEPARTMENT_QUERY").path("permissionName").asText());
        org.junit.jupiter.api.Assertions.assertEquals(
            "维护科室字典",
            permissionsByCode.get("PERM_SYS_DEPARTMENT_CREATE").path("permissionName").asText());

        mockMvc.perform(asAdmin(get("/api/v1/message-topics")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data[0].topicCode", notNullValue()));

        mockMvc.perform(asAdmin(get("/api/v1/stat-categories")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data[0].statCode", notNullValue()));
    }

    @Test
    void shouldNormalizeLegacyEnglishRoleAuthorizationLabelsWithoutOverwritingCustomNames() throws Exception {
        jdbcTemplate.update("""
            update menus
            set menu_name = :menuName
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", "MENU_DEPARTMENTS")
            .addValue("menuName", "Department Dictionary"));
        jdbcTemplate.update("""
            update permissions
            set permission_name = :permissionName
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", "PERM_SYS_DEPARTMENT_QUERY")
            .addValue("permissionName", "Query Department Dictionary"));
        jdbcTemplate.update("""
            update permissions
            set permission_name = :permissionName
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", "PERM_SYS_DEPARTMENT_CREATE")
            .addValue("permissionName", "自定义维护科室字典"));

        try (Connection connection = dataSource.getConnection()) {
            new V39__normalize_role_authorization_labels().migrate(new org.flywaydb.core.api.migration.Context() {
                @Override
                public Configuration getConfiguration() {
                    return null;
                }

                @Override
                public Connection getConnection() {
                    return connection;
                }
            });
        }

        org.junit.jupiter.api.Assertions.assertEquals(
            "科室字典",
            jdbcTemplate.queryForObject(
                "select menu_name from menus where id = :id",
                Map.of("id", "MENU_DEPARTMENTS"),
                String.class));
        org.junit.jupiter.api.Assertions.assertEquals(
            "查询科室字典",
            jdbcTemplate.queryForObject(
                "select permission_name from permissions where id = :id",
                Map.of("id", "PERM_SYS_DEPARTMENT_QUERY"),
                String.class));
        org.junit.jupiter.api.Assertions.assertEquals(
            "自定义维护科室字典",
            jdbcTemplate.queryForObject(
                "select permission_name from permissions where id = :id",
                Map.of("id", "PERM_SYS_DEPARTMENT_CREATE"),
                String.class));
    }

    @Test
    void shouldSeedRoleMenusForBuiltInAdminAndWorkflowRoles() throws Exception {
        mockMvc.perform(asAdmin(get("/api/v1/roles/ROLE_PATHOLOGY_ADMIN/authorizations")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.menuIds", hasItems(
                "MENU_SYSTEM",
                "MENU_SYS_USERS",
                "MENU_M2_WORKFLOW",
                "MENU_M2_APPLICATION_LIST",
                "MENU_M3_WORKFLOW",
                "MENU_M3_GROSSING",
                "MENU_M3_TASKS")));

        mockMvc.perform(asAdmin(get("/api/v1/roles/ROLE_M2_CLINICAL_REGISTER/authorizations")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.menuIds", hasItems(
                "MENU_M2_WORKFLOW",
                "MENU_M2_APPLICATION_LIST")));

        mockMvc.perform(asAdmin(get("/api/v1/roles/ROLE_M3_GROSSING/authorizations")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.menuIds", containsInAnyOrder(
                "MENU_M3_WORKFLOW",
                "MENU_M3_GROSSING",
                "MENU_M3_TASKS")));
    }

    @Test
    void shouldExposeEntryPermissionMetadataAndCleanManualAuthorizationPayloads() throws Exception {
        mockMvc.perform(asAdmin(get("/api/v1/permissions")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.permissionCode == 'PERM_SYS_ROLE_QUERY')].entryPermission").value(hasItems(true)))
            .andExpect(jsonPath("$.data[?(@.permissionCode == 'PERM_SYS_ROLE_ASSIGN')].entryPermission").value(hasItems(false)))
            .andExpect(jsonPath("$.data[?(@.permissionCode == 'PERM_M4_REPORT_CREATE')].entryPermission").value(hasItems(true)))
            .andExpect(jsonPath("$.data[?(@.permissionCode == 'PERM_M4_REPORT_REVIEW')].entryPermission").value(hasItems(false)));

        String roleId = createRole("Authorization Cleanup Role");

        mockMvc.perform(asAdmin(put("/api/v1/roles/{id}/authorizations", roleId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "menuIds": ["MENU_SYSTEM", "MENU_SYS_ROLES"],
                      "permissionIds": ["PERM_SYS_ROLE_QUERY", "PERM_SYS_ROLE_ASSIGN"],
                      "topicIds": [],
                      "statScopes": {}
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.menuIds", containsInAnyOrder("MENU_SYSTEM", "MENU_SYS_ROLES")))
            .andExpect(jsonPath("$.data.permissionIds", containsInAnyOrder("PERM_SYS_ROLE_ASSIGN")))
            .andExpect(jsonPath("$.data.permissionIds.length()", is(1)));
    }

    @Test
    void shouldGrantSystemRolePageEntryFromMenusButKeepAssignActionProtected() throws Exception {
        String roleId = createRole("System Menu Only Role");
        saveRoleAuthorization(roleId, """
            {
              "menuIds": ["MENU_SYSTEM", "MENU_SYS_ROLES"],
              "permissionIds": [],
              "topicIds": [],
              "statScopes": {}
            }
            """);
        String userId = createUser("menu-system-" + System.nanoTime());
        assignPrimaryRole(userId, roleId);

        mockMvc.perform(authorized(get("/api/v1/roles"), userId))
            .andExpect(status().isOk());
        mockMvc.perform(authorized(get("/api/v1/roles/{id}/authorizations", roleId), userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.permissionIds.length()", is(0)));
        mockMvc.perform(authorized(put("/api/v1/roles/{id}/authorizations", roleId), userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "menuIds": ["MENU_SYSTEM", "MENU_SYS_ROLES"],
                      "permissionIds": [],
                      "topicIds": [],
                      "statScopes": {}
                    }
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldGrantTechnicalTaskPageEntryWithoutOtherM3Actions() throws Exception {
        String roleId = createRole("Technical Menu Only Role");
        saveRoleAuthorization(roleId, """
            {
              "menuIds": ["MENU_M3_WORKFLOW", "MENU_M3_TASKS"],
              "permissionIds": [],
              "topicIds": [],
              "statScopes": {}
            }
            """);
        String userId = createUser("menu-m3-" + System.nanoTime());
        assignPrimaryRole(userId, roleId);

        mockMvc.perform(authorized(get("/api/v1/technical-tasks/pending"), userId)
                .param("page", "1")
                .param("size", "20"))
            .andExpect(status().isOk());
        mockMvc.perform(authorized(post("/api/v1/grossings/start"), userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldGrantReportEntryWithoutReviewOrSignActions() throws Exception {
        String roleId = createRole("Doctor Report Menu Only Role");
        saveRoleAuthorization(roleId, """
            {
              "menuIds": ["MENU_M4_WORKFLOW", "MENU_M4_REPORT"],
              "permissionIds": [],
              "topicIds": [],
              "statScopes": {}
            }
            """);
        String userId = createUser("menu-m4-" + System.nanoTime());
        assignPrimaryRole(userId, roleId);

        mockMvc.perform(authorized(post("/api/v1/pathology-reports"), userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(authorized(post("/api/v1/pathology-reports/RPT-X/review"), userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldFilterUpdateExportImportPrintAndDeleteRole() throws Exception {
        String loginName = "manage-" + System.nanoTime();
        String userId = createUser(loginName);

        mockMvc.perform(asAdmin(patch("/api/v1/system-users/{id}", userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userCode": null,
                      "name": "Updated User",
                      "jobNo": "JOB-%s",
                      "titleName": "Chief",
                      "departmentId": "DEP-01",
                      "departmentName": "Pathology",
                      "phone": "13800138000",
                      "email": "updated@example.com",
                      "loginTagCode": null,
                      "enabled": true
                    }
                    """.formatted(System.nanoTime())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name", is("Updated User")))
            .andExpect(jsonPath("$.data.loginTagCode", startsWith("LT-")));

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

        MvcResult roleResult = mockMvc.perform(asAdmin(post("/api/v1/roles"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "roleName": "Role For Delete",
                      "enabled": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roleCode", startsWith("ROLE-")))
            .andReturn();
        JsonNode roleNode = objectMapper.readTree(roleResult.getResponse().getContentAsString()).path("data");
        String roleId = roleNode.path("id").asText();
        String roleCode = roleNode.path("roleCode").asText();

        mockMvc.perform(asAdmin(patch("/api/v1/roles/{id}", roleId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "roleCode": null,
                      "roleName": "Role Updated",
                      "roleType": "BIZ",
                      "dataScope": "ALL",
                      "remarks": "updated",
                      "enabled": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roleName", is("Role Updated")))
            .andExpect(jsonPath("$.data.roleCode", is(roleCode)));

        mockMvc.perform(asAdmin(patch("/api/v1/roles/{id}", roleId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "roleCode": "ROLE-MUTATED",
                      "roleName": "Role Updated",
                      "roleType": "BIZ",
                      "dataScope": "ALL",
                      "remarks": "updated",
                      "enabled": true
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("cannot be changed once created")));

        mockMvc.perform(asAdmin(delete("/api/v1/roles/{id}", roleId)))
            .andExpect(status().isOk());
    }

    private String createRole(String roleName) throws Exception {
        MvcResult result = mockMvc.perform(asAdmin(post("/api/v1/roles"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "roleName": "%s",
                      "enabled": true
                    }
                    """.formatted(roleName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id", notNullValue()))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asText();
    }

    private void saveRoleAuthorization(String roleId, String payload) throws Exception {
        mockMvc.perform(asAdmin(put("/api/v1/roles/{id}/authorizations", roleId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());
    }

    private void assignPrimaryRole(String userId, String roleId) throws Exception {
        mockMvc.perform(asAdmin(put("/api/v1/system-users/{id}/roles", userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "assignments": [
                        {
                          "roleId": "%s",
                          "primary": true
                        }
                      ]
                    }
                    """.formatted(roleId)))
            .andExpect(status().isOk());
    }
}
