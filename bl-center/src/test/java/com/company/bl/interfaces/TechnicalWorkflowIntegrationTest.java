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
class TechnicalWorkflowIntegrationTest extends AbstractTechnicalWorkflowIntegrationTest {

    @Test
    void shouldCompleteTechnicalWorkflowEndToEndAndExposeTracking() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-001", "BC-M3-001");

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "operatorName": "grossing-user",
              "terminalCode": "TG-01"
            }
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.taskStatus").value("IN_PROGRESS"));

        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "caseId": "%s",
              "operatorName": "grossing-user",
              "terminalCode": "TG-01",
              "specimens": [
                {
                  "specimenId": "%s",
                  "specimenType": "ROUTINE",
                  "grossDescription": "routine grossing",
                  "blocks": [
                    {
                      "blockSite": "stomach",
                      "blockDescription": "block-a",
                      "specialRequirement": "none"
                    }
                  ]
                }
              ]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.createdDehydrationTaskCount").value(1));

        JsonNode dehydrationTasks = listPendingTasks("DEHYDRATION", context.pathologyNo(), USER_M3_DEHYDRATION);
        String samplingBlockId = dehydrationTasks.path("items").get(0).path("objectId").asText();

        JsonNode dehydrationBatch = responseBody(postJson("/api/v1/dehydration-batches", USER_M3_DEHYDRATION, """
            {
              "caseId": "%s",
              "basketNo": "BASKET-001",
              "deviceNo": "DEV-001",
              "operatorName": "dehydration-user",
              "terminalCode": "TD-01",
              "samplingBlockIds": ["%s"]
            }
            """.formatted(context.caseId(), samplingBlockId)), 201);
        String batchId = dehydrationBatch.path("batchId").asText();

        postJson("/api/v1/dehydration-batches/%s/start".formatted(batchId), USER_M3_DEHYDRATION, """
            {
              "operatorName": "dehydration-user",
              "terminalCode": "TD-02"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.batchStatus").value("IN_PROGRESS"));

        postJson("/api/v1/dehydration-batches/%s/complete".formatted(batchId), USER_M3_DEHYDRATION, """
            {
              "operatorName": "dehydration-user",
              "terminalCode": "TD-03",
              "mediaAssets": [
                {
                  "fileUrl": "http://example.com/dehydration-1.jpg",
                  "fileName": "dehydration-1.jpg"
                }
              ]
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.batchStatus").value("COMPLETED"));

        JsonNode embeddingTasks = listPendingTasks("EMBEDDING", context.pathologyNo(), USER_M3_EMBEDDING);
        String embeddingTaskId = embeddingTasks.path("items").get(0).path("id").asText();

        postJson("/api/v1/embeddings/start", USER_M3_EMBEDDING, """
            {
              "taskId": "%s",
              "operatorName": "embedding-user",
              "terminalCode": "TE-01"
            }
            """.formatted(embeddingTaskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.taskStatus").value("IN_PROGRESS"));

        JsonNode embedding = responseBody(postJson("/api/v1/embeddings/complete", USER_M3_EMBEDDING, """
            {
              "taskId": "%s",
              "samplingBlockId": "%s",
              "blockCount": 1,
              "sliceNotice": "careful",
              "operatorName": "embedding-user",
              "terminalCode": "TE-02"
            }
            """.formatted(embeddingTaskId, samplingBlockId)), 200);
        String embeddingBoxId = embedding.path("embeddingBoxId").asText();

        JsonNode slicingTasks = listPendingTasks("SLICING", context.pathologyNo(), USER_M3_SLICING);
        String slicingTaskId = slicingTasks.path("items").get(0).path("id").asText();

        postJson("/api/v1/slicings/start", USER_M3_SLICING, """
            {
              "taskId": "%s",
              "operatorName": "slicing-user",
              "terminalCode": "TS-01"
            }
            """.formatted(slicingTaskId))
            .andExpect(status().isOk());

        JsonNode slicing = responseBody(postJson("/api/v1/slicings/complete", USER_M3_SLICING, """
            {
              "taskId": "%s",
              "embeddingBoxId": "%s",
              "slideCount": 1,
              "deviceCode": "FAIL",
              "operatorName": "slicing-user",
              "terminalCode": "TS-02"
            }
            """.formatted(slicingTaskId, embeddingBoxId)), 200);
        String slideId = slicing.path("slideIds").get(0).asText();

        JsonNode stainingTasks = listPendingTasks("STAINING", context.pathologyNo(), USER_M3_STAINING);
        String stainingTaskId = stainingTasks.path("items").get(0).path("id").asText();

        postJson("/api/v1/slide-stainings/start", USER_M3_STAINING, """
            {
              "taskId": "%s",
              "operatorName": "staining-user",
              "terminalCode": "TT-01"
            }
            """.formatted(stainingTaskId))
            .andExpect(status().isOk());

        postJson("/api/v1/slide-stainings/complete", USER_M3_STAINING, """
            {
              "taskId": "%s",
              "slideId": "%s",
              "stainingType": "HE",
              "operatorName": "staining-user",
              "terminalCode": "TT-02"
            }
            """.formatted(stainingTaskId, slideId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.caseStatus").value("DIAGNOSIS_PENDING"));

        JsonNode tracking = technicalTracking(context.caseId(), USER_M3_TRACKING);
        assertThat(tracking.path("caseStatus").asText()).isEqualTo("DIAGNOSIS_PENDING");
        assertThat(tracking.path("blocks")).hasSize(1);
        assertThat(tracking.path("embeddingBoxes")).hasSize(1);
        assertThat(tracking.path("slides")).hasSize(1);
        assertThat(tracking.path("events").toString()).contains("FAILED");
    }

    @Test
    void shouldFanOutObjectLevelTasksForMultipleBlocks() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-002", "BC-M3-002");

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "operatorName": "grossing-user",
              "terminalCode": "TG-11"
            }
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk());

        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "caseId": "%s",
              "operatorName": "grossing-user",
              "terminalCode": "TG-12",
              "specimens": [
                {
                  "specimenId": "%s",
                  "specimenType": "ROUTINE",
                  "grossDescription": "multi-block",
                  "blocks": [
                    {"blockSite": "A", "blockDescription": "block-1"},
                    {"blockSite": "B", "blockDescription": "block-2"}
                  ]
                }
              ]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.createdDehydrationTaskCount").value(2));

        mockMvc.perform(authorized(get("/api/v1/technical-tasks/pending"), USER_M3_DEHYDRATION)
                .param("page", "1")
                .param("size", "20")
                .param("taskType", "DEHYDRATION")
                .param("pathologyNo", context.pathologyNo()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.items[0].objectType").value("SAMPLING_BLOCK"));
    }

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
              "operatorName": "grossing-user",
              "terminalCode": "TG-21"
            }
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk());

        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "caseId": "%s",
              "operatorName": "grossing-user",
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
    void shouldCreateRestainReworkAndGenerateNewStainingTask() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-004", "BC-M3-004");

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "operatorName": "grossing-user"
            }
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk());

        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "caseId": "%s",
              "operatorName": "grossing-user",
              "specimens": [
                {
                  "specimenId": "%s",
                  "specimenType": "ROUTINE",
                  "grossDescription": "rework",
                  "blocks": [
                    {"blockSite": "A", "blockDescription": "block-1"}
                  ]
                }
              ]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId()))
            .andExpect(status().isOk());

        String blockId = listPendingTasks("DEHYDRATION", context.pathologyNo(), USER_M3_DEHYDRATION).path("items").get(0).path("objectId").asText();
        String batchId = responseBody(postJson("/api/v1/dehydration-batches", USER_M3_DEHYDRATION, """
            {
              "caseId": "%s",
              "basketNo": "BASKET-RW",
              "operatorName": "dehydration-user",
              "samplingBlockIds": ["%s"]
            }
            """.formatted(context.caseId(), blockId)), 201).path("batchId").asText();
        postJson("/api/v1/dehydration-batches/%s/start".formatted(batchId), USER_M3_DEHYDRATION, """
            {
              "operatorName": "dehydration-user"
            }
            """)
            .andExpect(status().isOk());
        postJson("/api/v1/dehydration-batches/%s/complete".formatted(batchId), USER_M3_DEHYDRATION, """
            {
              "operatorName": "dehydration-user"
            }
            """)
            .andExpect(status().isOk());

        String embeddingTaskId = listPendingTasks("EMBEDDING", context.pathologyNo(), USER_M3_EMBEDDING).path("items").get(0).path("id").asText();
        postJson("/api/v1/embeddings/start", USER_M3_EMBEDDING, """
            {
              "taskId": "%s",
              "operatorName": "embedding-user"
            }
            """.formatted(embeddingTaskId))
            .andExpect(status().isOk());
        String embeddingBoxId = responseBody(postJson("/api/v1/embeddings/complete", USER_M3_EMBEDDING, """
            {
              "taskId": "%s",
              "samplingBlockId": "%s",
              "blockCount": 1,
              "operatorName": "embedding-user"
            }
            """.formatted(embeddingTaskId, blockId)), 200).path("embeddingBoxId").asText();

        String slicingTaskId = listPendingTasks("SLICING", context.pathologyNo(), USER_M3_SLICING).path("items").get(0).path("id").asText();
        postJson("/api/v1/slicings/start", USER_M3_SLICING, """
            {
              "taskId": "%s",
              "operatorName": "slicing-user"
            }
            """.formatted(slicingTaskId))
            .andExpect(status().isOk());
        String slideId = responseBody(postJson("/api/v1/slicings/complete", USER_M3_SLICING, """
            {
              "taskId": "%s",
              "embeddingBoxId": "%s",
              "slideCount": 1,
              "operatorName": "slicing-user"
            }
            """.formatted(slicingTaskId, embeddingBoxId)), 200).path("slideIds").get(0).asText();

        postJson("/api/v1/rework-orders", USER_M3_REWORK, """
            {
              "caseId": "%s",
              "specimenId": "%s",
              "slideId": "%s",
              "reworkType": "RESTAIN",
              "reason": "color-faded",
              "operatorName": "rework-user"
            }
            """.formatted(context.caseId(), context.specimenId(), slideId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"));

        String reworkOrderId = namedParameterJdbcTemplate.queryForObject("""
            select id
            from rework_orders
            where case_id = :caseId
            order by created_at desc
            limit 1
            """, java.util.Map.of("caseId", context.caseId()), String.class);

        postJson("/api/v1/rework-orders/%s/execute".formatted(reworkOrderId), USER_M3_REWORK, """
            {
              "operatorName": "rework-user"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(authorized(get("/api/v1/technical-tasks/pending"), USER_M3_STAINING)
                .param("page", "1")
                .param("size", "20")
                .param("taskType", "STAINING")
                .param("pathologyNo", context.pathologyNo()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1));
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
              "operatorName": "dehydration-user",
              "terminalCode": "M3-N-01"
            }
            """.formatted(USER_M3_GROSSING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.assignedToUserId").value(USER_M3_GROSSING));

        postJson("/api/v1/technical-tasks/%s/priority".formatted(context.grossingTaskId()), USER_M3_DEHYDRATION, """
            {
              "priority": "STAT",
              "productionRemarks": "expedite",
              "operatorName": "dehydration-user",
              "terminalCode": "M3-N-02"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.priority").value("STAT"));

        postJson("/api/v1/technical-tasks/%s/release".formatted(context.grossingTaskId()), USER_M3_DEHYDRATION, """
            {
              "operatorName": "dehydration-user",
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
