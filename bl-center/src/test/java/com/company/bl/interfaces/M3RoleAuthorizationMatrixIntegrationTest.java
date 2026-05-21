package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class M3RoleAuthorizationMatrixIntegrationTest extends AbstractTechnicalWorkflowIntegrationTest {

    @Test
    void shouldRejectCrossRoleOperationsAcrossTechnicalWorkstations() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-AUTH-001", "BC-M3-AUTH-001");

        postJson("/api/v1/grossings/start", USER_M3_TRACKING, """
            {
              "taskId": "%s",
              "operatorName": "tracking-user",
              "terminalCode": "M3-AUTH-01"
            }
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "operatorName": "grossing-user",
              "terminalCode": "M3-AUTH-02"
            }
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk());

        postJson("/api/v1/grossings/complete", USER_M3_TRACKING, """
            {
              "taskId": "%s",
              "caseId": "%s",
              "operatorName": "tracking-user",
              "terminalCode": "M3-AUTH-03",
              "specimens": [
                {
                  "specimenId": "%s",
                  "specimenType": "ROUTINE",
                  "grossDescription": "forbidden grossing",
                  "blocks": [
                    {
                      "blockSite": "A",
                      "blockDescription": "block-a"
                    }
                  ]
                }
              ]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "caseId": "%s",
              "operatorName": "grossing-user",
              "terminalCode": "M3-AUTH-04",
              "specimens": [
                {
                  "specimenId": "%s",
                  "specimenType": "ROUTINE",
                  "grossDescription": "authorized grossing",
                  "blocks": [
                    {
                      "blockSite": "A",
                      "blockDescription": "block-a"
                    }
                  ]
                }
              ]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.createdDehydrationTaskCount").value(1));

        String samplingBlockId = listPendingTasks("DEHYDRATION", context.pathologyNo(), USER_M3_DEHYDRATION)
            .path("items").get(0).path("objectId").asText();

        postJson("/api/v1/dehydration-batches", USER_M3_EMBEDDING, """
            {
              "caseId": "%s",
              "basketNo": "AUTH-BASKET-01",
              "deviceNo": "AUTH-DEV-01",
              "operatorName": "embedding-user",
              "terminalCode": "M3-AUTH-05",
              "samplingBlockIds": ["%s"]
            }
            """.formatted(context.caseId(), samplingBlockId))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        JsonNode batch = responseBody(postJson("/api/v1/dehydration-batches", USER_M3_DEHYDRATION, """
            {
              "caseId": "%s",
              "basketNo": "AUTH-BASKET-02",
              "deviceNo": "AUTH-DEV-02",
              "operatorName": "dehydration-user",
              "terminalCode": "M3-AUTH-06",
              "samplingBlockIds": ["%s"]
            }
            """.formatted(context.caseId(), samplingBlockId)), 201);

        postJson("/api/v1/dehydration-batches/%s/start".formatted(batch.path("batchId").asText()), USER_M3_SLICING, """
            {
              "operatorName": "slicing-user",
              "terminalCode": "M3-AUTH-07"
            }
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void shouldRestrictTaskAndTrackingQueriesToAuthorizedTechnicalRoles() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-AUTH-002", "BC-M3-AUTH-002");

        JsonNode pending = listPendingTasks("GROSSING", context.pathologyNo(), USER_M3_GROSSING);
        assertThat(pending.path("total").asInt()).isEqualTo(1);

        mockMvc.perform(authorized(get("/api/v1/technical-tasks/pending"), USER_REGISTER)
                .param("page", "1")
                .param("size", "20")
                .param("taskType", "GROSSING")
                .param("pathologyNo", context.pathologyNo()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        mockMvc.perform(authorized(get("/api/v1/pathology-cases/{id}/technical-tracking", context.caseId()), USER_RECEIVE))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        mockMvc.perform(post("/api/v1/grossings/start")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "taskId": "%s",
                      "operatorName": "anonymous-user"
                    }
                    """.formatted(context.grossingTaskId())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }
}
