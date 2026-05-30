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
class TechnicalWorkflowExecutionIntegrationTest extends AbstractTechnicalWorkflowIntegrationTest {

    @Test
    void shouldCompleteTechnicalWorkflowEndToEndAndExposeTracking() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-001", "BC-M3-001");

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              
              "terminalCode": "TG-01"
            }
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.taskStatus").value("IN_PROGRESS"));

        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "caseId": "%s",
              
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
              
              "terminalCode": "TD-01",
              "samplingBlockIds": ["%s"]
            }
            """.formatted(context.caseId(), samplingBlockId)), 201);
        String batchId = dehydrationBatch.path("batchId").asText();

        postJson("/api/v1/dehydration-batches/%s/start".formatted(batchId), USER_M3_DEHYDRATION, """
            {
              
              "terminalCode": "TD-02"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.batchStatus").value("IN_PROGRESS"));

        postJson("/api/v1/dehydration-batches/%s/complete".formatted(batchId), USER_M3_DEHYDRATION, """
            {
              
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
              
              "terminalCode": "TE-02"
            }
            """.formatted(embeddingTaskId, samplingBlockId)), 200);
        String embeddingBoxId = embedding.path("embeddingBoxId").asText();

        JsonNode slicingTasks = listPendingTasks("SLICING", context.pathologyNo(), USER_M3_SLICING);
        String slicingTaskId = slicingTasks.path("items").get(0).path("id").asText();

        postJson("/api/v1/slicings/start", USER_M3_SLICING, """
            {
              "taskId": "%s",
              
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
              
              "terminalCode": "TS-02"
            }
            """.formatted(slicingTaskId, embeddingBoxId)), 200);
        String slideId = slicing.path("slideIds").get(0).asText();

        JsonNode stainingTasks = listPendingTasks("STAINING", context.pathologyNo(), USER_M3_STAINING);
        String stainingTaskId = stainingTasks.path("items").get(0).path("id").asText();

        postJson("/api/v1/slide-stainings/start", USER_M3_STAINING, """
            {
              "taskId": "%s",
              
              "terminalCode": "TT-01"
            }
            """.formatted(stainingTaskId))
            .andExpect(status().isOk());

        postJson("/api/v1/slide-stainings/complete", USER_M3_STAINING, """
            {
              "taskId": "%s",
              "slideId": "%s",
              "stainingType": "HE",
              
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
              
              "terminalCode": "TG-11"
            }
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk());

        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "caseId": "%s",
              
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
    void shouldCreateRestainReworkAndGenerateNewStainingTask() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-004", "BC-M3-004");

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {
              "taskId": "%s"}
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk());

        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "caseId": "%s",
              
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
              
              "samplingBlockIds": ["%s"]
            }
            """.formatted(context.caseId(), blockId)), 201).path("batchId").asText();
        postJson("/api/v1/dehydration-batches/%s/start".formatted(batchId), USER_M3_DEHYDRATION, """
            {
              }
            """)
            .andExpect(status().isOk());
        postJson("/api/v1/dehydration-batches/%s/complete".formatted(batchId), USER_M3_DEHYDRATION, """
            {
              }
            """)
            .andExpect(status().isOk());

        String embeddingTaskId = listPendingTasks("EMBEDDING", context.pathologyNo(), USER_M3_EMBEDDING).path("items").get(0).path("id").asText();
        postJson("/api/v1/embeddings/start", USER_M3_EMBEDDING, """
            {
              "taskId": "%s"}
            """.formatted(embeddingTaskId))
            .andExpect(status().isOk());
        String embeddingBoxId = responseBody(postJson("/api/v1/embeddings/complete", USER_M3_EMBEDDING, """
            {
              "taskId": "%s",
              "samplingBlockId": "%s",
              "blockCount": 1}
            """.formatted(embeddingTaskId, blockId)), 200).path("embeddingBoxId").asText();

        String slicingTaskId = listPendingTasks("SLICING", context.pathologyNo(), USER_M3_SLICING).path("items").get(0).path("id").asText();
        postJson("/api/v1/slicings/start", USER_M3_SLICING, """
            {
              "taskId": "%s"}
            """.formatted(slicingTaskId))
            .andExpect(status().isOk());
        String slideId = responseBody(postJson("/api/v1/slicings/complete", USER_M3_SLICING, """
            {
              "taskId": "%s",
              "embeddingBoxId": "%s",
              "slideCount": 1}
            """.formatted(slicingTaskId, embeddingBoxId)), 200).path("slideIds").get(0).asText();

        postJson("/api/v1/rework-orders", USER_M3_REWORK, """
            {
              "caseId": "%s",
              "specimenId": "%s",
              "slideId": "%s",
              "reworkType": "RESTAIN",
              "reason": "color-faded"}
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
}
