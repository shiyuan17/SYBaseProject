package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class TechnicalWorkflowAutomationAndGovernanceIntegrationTest extends AbstractTechnicalWorkflowIntegrationTest {

    private static final String USER_M3_TASK_QUERY_ONLY = "USER_M3_TASK_QUERY_ONLY";

    @Test
    void shouldMatchSingleSamplingTemplateAutomatically() throws Exception {
        JsonNode bodyPart = responseBody(postJson("/api/v1/body-parts", USER_M1_ADMIN, """
            {
              "partCode": "BP-M3-AUTO-%d",
              "partName": "M3 Auto Body Part",
              "partAlias": "M3 Auto",
              "partLevel": 1,
              "sortOrder": 1,
              "enabled": true
            }
            """.formatted(System.nanoTime())), 200);
        String bodyPartId = bodyPart.path("id").asText();

        JsonNode category = responseBody(postJson("/api/v1/sampling-templates/categories", USER_M1_ADMIN, """
            {
              "categoryCode": "STC-M3-%d",
              "categoryName": "M3 Category",
              "sortOrder": 1,
              "enabled": true
            }
            """.formatted(System.nanoTime())), 200);
        String categoryId = category.path("id").asText();

        String templateCode = "TPL-M3-" + System.nanoTime();
        JsonNode template = responseBody(postJson("/api/v1/sampling-templates", USER_M1_ADMIN, """
            {
              "categoryId": "%s",
              "templateCode": "%s",
              "templateName": "M3 Template",
              "templateContent": "auto match template",
              "splitPartCount": 1,
              "applicableSpecimenType": "ROUTINE",
              "enabled": true,
              "bodyPartIds": ["%s"]
            }
            """.formatted(categoryId, templateCode, bodyPartId)), 200);
        String templateId = template.path("id").asText();

        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-003", "BC-M3-003");

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              
              "terminalCode": "TG-21"
            }
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk());

        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "caseId": "%s",
              
              "terminalCode": "TG-22",
              "specimens": [
                {
                  "specimenId": "%s",
                  "specimenType": "ROUTINE",
                  "bodyPartId": "%s",
                  "grossDescription": "auto template",
                  "blocks": [
                    {"blockSite": "A", "blockDescription": "block-1"}
                  ]
                }
              ]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId(), bodyPartId))
            .andExpect(status().isOk());

        assertThat(querySamplingTemplateId(context.caseId(), context.specimenId())).isEqualTo(templateId);
    }

    @Test
    void shouldRequireActionPermissionsAndServiceOwnershipGuardsForTechnicalTaskMutations() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-NOTIFY-001", "BC-M3-NOTIFY-001");

        postJson("/api/v1/technical-tasks/%s/assign".formatted(context.grossingTaskId()), USER_M3_DEHYDRATION, """
            {
              "priority": "PRIORITY",
              "stationCode": "G-01",
              "stationName": "Grossing Station",
              "assignedToUserId": "%s",
              "assignedToName": "M3 Grossing",
              
              "terminalCode": "M3-N-01"
            }
            """.formatted(USER_M3_GROSSING))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        postJson("/api/v1/technical-tasks/%s/assign".formatted(context.grossingTaskId()), USER_M1_ADMIN, """
            {
              "priority": "PRIORITY",
              "stationCode": "G-01",
              "stationName": "Grossing Station",
              "assignedToUserId": "%s",
              "assignedToName": "M3 Grossing",
              
              "terminalCode": "M3-N-01A"
            }
            """.formatted(USER_M3_GROSSING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.assignedToUserId").value(USER_M3_GROSSING));

        postJson("/api/v1/technical-tasks/%s/priority".formatted(context.grossingTaskId()), USER_M3_DEHYDRATION, """
            {
              "priority": "STAT",
              "productionRemarks": "expedite",
              
              "terminalCode": "M3-N-02"
            }
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        postJson("/api/v1/technical-tasks/%s/priority".formatted(context.grossingTaskId()), USER_M1_ADMIN, """
            {
              "priority": "STAT",
              "productionRemarks": "expedite",
              
              "terminalCode": "M3-N-02A"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.priority").value("STAT"));

        postJson("/api/v1/technical-tasks/%s/release".formatted(context.grossingTaskId()), USER_M3_DEHYDRATION, """
            {
              
              "terminalCode": "M3-N-03",
              "remarks": "re-balance"
            }
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        postJson("/api/v1/technical-tasks/%s/release".formatted(context.grossingTaskId()), USER_M1_ADMIN, """
            {
              
              "terminalCode": "M3-N-03A",
              "remarks": "re-balance"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.assignedToUserId").isEmpty());

        assertThat(countNotifications(USER_M3_GROSSING, "TECH_TASK_ASSIGN", context.grossingTaskId())).isEqualTo(1L);
        assertThat(countNotifications(USER_M3_GROSSING, "TECH_TASK_PRIORITY", context.grossingTaskId())).isEqualTo(1L);
        assertThat(countNotifications(USER_M3_GROSSING, "TECH_TASK_RELEASE", context.grossingTaskId())).isEqualTo(1L);
    }

    @Test
    void shouldAllowQueryOnlyTaskUserToListButDenyMutations() throws Exception {
        seedQueryOnlyTaskUser();
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-QUERY-ONLY-001", "BC-M3-QUERY-ONLY-001");

        mockMvc.perform(authorized(get("/api/v1/technical-tasks/pending"), USER_M3_TASK_QUERY_ONLY)
                .param("page", "1")
                .param("size", "20")
                .param("taskType", "GROSSING")
                .param("pathologyNo", context.pathologyNo()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].id").value(context.grossingTaskId()));

        postJson("/api/v1/technical-tasks/%s/assign".formatted(context.grossingTaskId()), USER_M3_TASK_QUERY_ONLY, """
            {
              "priority": "PRIORITY",
              "stationCode": "Q-01",
              "stationName": "Query Only Station",
              "assignedToUserId": "%s",
              "assignedToName": "M3 Query Only",

              "terminalCode": "M3-Q-00"
            }
            """.formatted(USER_M3_TASK_QUERY_ONLY))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        postJson("/api/v1/technical-tasks/%s/claim".formatted(context.grossingTaskId()), USER_M3_TASK_QUERY_ONLY, """
            {
              "assignedToUserId": "%s",
              "assignedToName": "M3 Query Only",
              "stationCode": "Q-01",
              "stationName": "Query Only Station",
              
              "terminalCode": "M3-Q-01",
              "remarks": "claim"
            }
            """.formatted(USER_M3_TASK_QUERY_ONLY))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        postJson("/api/v1/technical-tasks/%s/release".formatted(context.grossingTaskId()), USER_M3_TASK_QUERY_ONLY, """
            {

              "terminalCode": "M3-Q-02",
              "remarks": "release"
            }
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        postJson("/api/v1/technical-tasks/%s/priority".formatted(context.grossingTaskId()), USER_M3_TASK_QUERY_ONLY, """
            {
              "priority": "STAT",
              "productionRemarks": "query only should fail",

              "terminalCode": "M3-Q-03"
            }
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        mockMvc.perform(authorized(patch("/api/v1/technical-tasks/%s/remarks".formatted(context.grossingTaskId())), USER_M3_TASK_QUERY_ONLY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "remarks": "query only should fail",
                      "productionRemarks": "query only should fail",

                      "terminalCode": "M3-Q-04"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void shouldRequireM3PermissionForPendingTaskQuery() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-005", "BC-M3-005");

        mockMvc.perform(authorized(get("/api/v1/technical-tasks/pending"), USER_NO_PERMISSION)
                .param("page", "1")
                .param("size", "20")
                .param("taskType", "GROSSING")
                .param("pathologyNo", context.pathologyNo()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    private void seedQueryOnlyTaskUser() {
        LocalDateTime now = LocalDateTime.now();
        namedParameterJdbcTemplate.update("""
            merge into users (id, user_code, login_name, name, role, enabled, created_at, updated_at)
            key (id)
            values (:id, :userCode, :loginName, :name, :role, 1, :createdAt, :updatedAt)
            """, Map.of(
            "id", USER_M3_TASK_QUERY_ONLY,
            "userCode", "U-M3-TASK-QUERY-ONLY",
            "loginName", "m3.task.query.only",
            "name", "M3 Query Only",
            "role", "M3_TASK_QUERY_ONLY",
            "createdAt", now,
            "updatedAt", now));
        namedParameterJdbcTemplate.update("""
            merge into roles (id, role_code, role_name, role_type, data_scope, remarks, enabled, created_at, updated_at)
            key (id)
            values (
              :id,
              :roleCode,
              :roleName,
              :roleType,
              :dataScope,
              :remarks,
              1,
              :createdAt,
              :updatedAt
            )
            """, Map.of(
            "id", "ROLE_M3_TASK_QUERY_ONLY",
            "roleCode", "M3_TASK_QUERY_ONLY",
            "roleName", "M3 任务池只读用户",
            "roleType", "BUSINESS",
            "dataScope", "DEPARTMENT",
            "remarks", "M3 technical task query-only role",
            "createdAt", now,
            "updatedAt", now));
        namedParameterJdbcTemplate.update("""
            merge into user_roles (id, user_id, role_id, is_primary, assigned_at, assigned_by_name)
            key (id)
            values (
              'UR_M3_TASK_QUERY_ONLY',
              :userId,
              'ROLE_M3_TASK_QUERY_ONLY',
              1,
              :assignedAt,
              'test'
            )
            """, Map.of(
            "userId", USER_M3_TASK_QUERY_ONLY,
            "assignedAt", now));
        namedParameterJdbcTemplate.update("""
            merge into role_permissions (id, role_id, permission_id, assigned_at)
            key (id)
            values (
              :id,
              :roleId,
              :permissionId,
              :assignedAt
            )
            """, Map.of(
            "id", "RP_M3_TASK_QUERY_ONLY_QUERY",
            "roleId", "ROLE_M3_TASK_QUERY_ONLY",
            "permissionId", "PERM_M3_TECH_TASK_QUERY",
            "assignedAt", now));
    }
}
