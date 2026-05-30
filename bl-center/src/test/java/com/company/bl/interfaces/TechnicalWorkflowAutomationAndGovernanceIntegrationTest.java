package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class TechnicalWorkflowAutomationAndGovernanceIntegrationTest extends AbstractTechnicalWorkflowIntegrationTest {

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
    void shouldCreateNotificationsForTechnicalTaskAssignReleaseAndPriority() throws Exception {
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
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.assignedToUserId").value(USER_M3_GROSSING));

        postJson("/api/v1/technical-tasks/%s/priority".formatted(context.grossingTaskId()), USER_M3_DEHYDRATION, """
            {
              "priority": "STAT",
              "productionRemarks": "expedite",
              
              "terminalCode": "M3-N-02"
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
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.assignedToUserId").isEmpty());

        assertThat(countNotifications(USER_M3_GROSSING, "TECH_TASK_ASSIGN", context.grossingTaskId())).isEqualTo(1L);
        assertThat(countNotifications(USER_M3_GROSSING, "TECH_TASK_PRIORITY", context.grossingTaskId())).isEqualTo(1L);
        assertThat(countNotifications(USER_M3_GROSSING, "TECH_TASK_RELEASE", context.grossingTaskId())).isEqualTo(1L);
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
}
