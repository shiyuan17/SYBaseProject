package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class TechnicalWorkflowQueryEnhancementIntegrationTest extends AbstractTechnicalWorkflowIntegrationTest {

    @Test
    void shouldFilterTimedOutTasksAndExposeTimeoutMetadata() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-TIMEOUT-001", "BC-M3-TIMEOUT-001");
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(300);
        namedParameterJdbcTemplate.update("""
            update technical_pending_tasks
            set created_at = :createdAt
            where id = :taskId
            """, Map.of("createdAt", createdAt, "taskId", context.grossingTaskId()));

        mockMvc.perform(authorized(get("/api/v1/technical-tasks/pending"), USER_M3_GROSSING)
                .param("page", "1")
                .param("size", "20")
                .param("taskType", "GROSSING")
                .param("pathologyNo", context.pathologyNo())
                .param("timedOutOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].timedOut").value(true))
            .andExpect(jsonPath("$.data.items[0].timeoutRuleCode").value("technical.timeout.grossingMinutes"));

        JsonNode page = responseBody(mockMvc.perform(authorized(get("/api/v1/technical-tasks/pending"), USER_M3_GROSSING)
            .param("page", "1")
            .param("size", "20")
            .param("taskType", "GROSSING")
            .param("pathologyNo", context.pathologyNo())
            .param("createdFrom", createdAt.minusMinutes(1).toString())
            .param("createdTo", createdAt.plusMinutes(1).toString())), 200);
        JsonNode item = page.path("items").get(0);
        Duration timeoutWindow = Duration.between(
            LocalDateTime.parse(item.path("createdAt").asText()),
            LocalDateTime.parse(item.path("deadlineAt").asText()));
        assertThat(timeoutWindow.toMinutes()).isEqualTo(240);

        mockMvc.perform(authorized(get("/api/v1/technical-tasks/pending"), USER_M3_GROSSING)
                .param("page", "1")
                .param("size", "20")
                .param("taskType", "GROSSING")
                .param("pathologyNo", context.pathologyNo())
                .param("createdFrom", createdAt.plusMinutes(1).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void shouldExposeQcEvaluationsInTechnicalTracking() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-QC-001", "BC-M3-QC-001");

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
                  "grossDescription": "qc tracking",
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
              "basketNo": "BASKET-QC",
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
              "qcType": "HE",
              "operatorName": "rework-user",
              "remarks": "please restain"
            }
            """.formatted(context.caseId(), context.specimenId(), slideId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"));

        JsonNode tracking = technicalTracking(context.caseId(), USER_M3_TRACKING);
        assertThat(tracking.path("qcEvaluations")).hasSize(1);
        assertThat(tracking.path("qcEvaluations").get(0).path("slideId").asText()).isEqualTo(slideId);
        assertThat(tracking.path("qcEvaluations").get(0).path("evaluationResult").asText()).isEqualTo("REWORK_REQUIRED");
        assertThat(tracking.path("qcEvaluations").get(0).path("improvementSuggestion").asText()).isEqualTo("please restain");
        assertThat(tracking.path("qcEvaluations").get(0).path("slideNo").asText()).isNotBlank();
    }
}
