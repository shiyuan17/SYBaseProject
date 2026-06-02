package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class TechnicalWorkflowQueryEnhancementIntegrationTest extends AbstractTechnicalWorkflowIntegrationTest {

    @Test
    void shouldFilterPendingTasksByKeywordForPatientIdAndPathologyNo() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-KEYWORD-001", "BC-M3-KEYWORD-001");
        String patientId = "P-KW-" + uniqueSuffix();
        namedParameterJdbcTemplate.update("""
            update applications
            set patient_id = :patientId
            where id = :applicationId
            """, Map.of("patientId", patientId, "applicationId", context.applicationId()));

        mockMvc.perform(authorized(get("/api/v1/technical-tasks/pending"), USER_M3_GROSSING)
                .param("page", "1")
                .param("size", "20")
                .param("taskType", "GROSSING")
                .param("keyword", patientId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].pathologyNo").value(context.pathologyNo()));

        mockMvc.perform(authorized(get("/api/v1/technical-tasks/pending"), USER_M3_GROSSING)
                .param("page", "1")
                .param("size", "20")
                .param("taskType", "GROSSING")
                .param("keyword", context.pathologyNo()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].pathologyNo").value(context.pathologyNo()));

        mockMvc.perform(authorized(get("/api/v1/technical-tasks/pending"), USER_M3_GROSSING)
                .param("page", "1")
                .param("size", "20")
                .param("taskType", "GROSSING")
                .param("pathologyNo", context.pathologyNo()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].pathologyNo").value(context.pathologyNo()));
    }

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
    void shouldHideCompletedTechnicalTasksFromPendingListByDefault() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-PENDING-ONLY-001", "BC-M3-PENDING-ONLY-001");

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
                  "grossDescription": "complete for pending query",
                  "blocks": [
                    {"blockSite": "A", "blockDescription": "block-1"}
                  ]
                }
              ]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId()))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/v1/technical-tasks/pending"), USER_M3_GROSSING)
                .param("page", "1")
                .param("size", "20")
                .param("taskType", "GROSSING")
                .param("pathologyNo", context.pathologyNo()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void shouldExposeQcEvaluationsInTechnicalTracking() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-QC-001", "BC-M3-QC-001");

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
              "reason": "color-faded",
              "qcType": "HE",
              
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

        JsonNode trackingBySlide = technicalTracking(slideId, USER_M3_TRACKING);
        assertThat(trackingBySlide.path("caseId").asText()).isEqualTo(context.caseId());
        assertThat(trackingBySlide.path("slides").get(0).path("slideId").asText()).isEqualTo(slideId);
    }

    @Test
    void shouldQueryTechnicalTrackingByPathologyNo() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-TRACK-PNO-001", "BC-M3-TRACK-PNO-001");

        JsonNode tracking = technicalTracking(context.pathologyNo(), USER_M3_TRACKING);

        assertThat(tracking.path("caseId").asText()).isEqualTo(context.caseId());
        assertThat(tracking.path("pathologyNo").asText()).isEqualTo(context.pathologyNo());
        assertThat(tracking.path("technicalTasks")).hasSize(1);
        assertThat(tracking.path("technicalTasks").get(0).path("caseId").asText()).isEqualTo(context.caseId());
    }

    @Test
    void shouldExposeEmbeddingWorkstationSummaryForCurrentDay() throws Exception {
        TechnicalCaseContext pendingContext = receiveCaseAndGetGrossingTask("APP-M3-EMB-SUM-001", "BC-M3-EMB-SUM-001");
        advanceCaseToPendingEmbedding(pendingContext, "pending summary gross");

        TechnicalCaseContext completedContext = receiveCaseAndGetGrossingTask("APP-M3-EMB-SUM-002", "BC-M3-EMB-SUM-002");
        EmbeddingFixture completedFixture = advanceCaseToCompletedEmbedding(
            completedContext,
            "summary gross description",
            "包埋备注-汇总",
            "取材评价-汇总",
            "注意切片");

        JsonNode summary = responseBody(mockMvc.perform(authorized(
            get("/api/v1/embeddings/workstation-summary"),
            USER_M3_EMBEDDING)), 200);

        assertThat(summary.path("pendingCount").asInt()).isEqualTo(1);
        assertThat(summary.path("completedCount").asInt()).isEqualTo(1);
        assertThat(summary.path("pendingTasks")).hasSize(1);
        assertThat(summary.path("pendingTasks").get(0).path("pathologyNo").asText()).isEqualTo(pendingContext.pathologyNo());
        assertThat(summary.path("completedRecords")).hasSize(1);
        assertThat(summary.path("completedRecords").get(0).path("pathologyNo").asText()).isEqualTo(completedContext.pathologyNo());
        assertThat(summary.path("completedRecords").get(0).path("samplingEvaluation").asText()).isEqualTo("取材评价-汇总");
        assertThat(summary.path("completedRecords").get(0).path("embeddingRemarks").asText()).isEqualTo("包埋备注-汇总");
        assertThat(summary.path("completedRecords").get(0).path("grossDescription").asText()).isEqualTo("summary gross description");
        assertThat(summary.path("completedRecords").get(0).path("embeddingBoxId").asText()).isEqualTo(completedFixture.embeddingBoxId());
    }

    @Test
    void shouldExposeEmbeddingRecordsAndGrossDescriptionInTechnicalTracking() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-TRACK-EMB-001", "BC-M3-TRACK-EMB-001");
        advanceCaseToCompletedEmbedding(
            context,
            "tracking gross description",
            "包埋备注-追踪",
            "取材评价-追踪",
            "追踪切片提示");

        JsonNode tracking = technicalTracking(context.caseId(), USER_M3_TRACKING);

        assertThat(tracking.path("blocks")).hasSize(1);
        assertThat(tracking.path("blocks").get(0).path("specimenName").asText()).isEqualTo("Thyroid Tissue");
        assertThat(tracking.path("blocks").get(0).path("grossDescription").asText()).isEqualTo("tracking gross description");
        assertThat(tracking.path("embeddingRecords")).hasSize(1);
        assertThat(tracking.path("embeddingRecords").get(0).path("samplingEvaluation").asText()).isEqualTo("取材评价-追踪");
        assertThat(tracking.path("embeddingRecords").get(0).path("embeddingRemarks").asText()).isEqualTo("包埋备注-追踪");
        assertThat(tracking.path("embeddingRecords").get(0).path("grossDescription").asText()).isEqualTo("tracking gross description");
        assertThat(tracking.path("embeddingRecords").get(0).path("specimenName").asText()).isEqualTo("Thyroid Tissue");
        assertThat(tracking.path("embeddingEvaluationRecords")).hasSize(1);
        assertThat(tracking.path("embeddingEvaluationRecords").get(0).path("samplingEvaluation").asText()).isEqualTo("取材评价-追踪");
        assertThat(tracking.path("embeddingEvaluationRecords").get(0).path("embeddingRemarks").asText()).isEqualTo("包埋备注-追踪");
        assertThat(tracking.path("embeddingEvaluationRecords").get(0).path("endedAt").asText()).isNotBlank();
    }

    @Test
    void shouldUpdateEmbeddingQualityReviewAndCreateRegrossingTask() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-EMB-REVIEW-001", "BC-M3-EMB-REVIEW-001");
        EmbeddingFixture fixture = advanceCaseToCompletedEmbedding(
            context,
            "quality review gross description",
            "包埋备注-评价",
            "原取材评价",
            "原切片备注");

        JsonNode response = responseBody(mockMvc.perform(authorized(
                patch("/api/v1/embeddings/{embeddingId}/quality-review", fixture.embeddingId()),
                USER_M3_EMBEDDING)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "sliceNotice": "皮肤",
                  "evaluationLevel": "UNQUALIFIED",
                  "samplingEvaluation": "取材评价调整",
                  "unqualifiedReasons": ["组织过厚", "有线头"],
                  "treatmentAction": "REGROSSING",
                  "treatmentRemark": "请重新取材",
                  "notifiedGrossingOperator": true,
                  "terminalCode": "T-EMB-REVIEW"
                }
                """)), 200);

        assertThat(response.path("record").path("sliceNotice").asText()).isEqualTo("皮肤");
        assertThat(response.path("record").path("evaluationLevel").asText()).isEqualTo("UNQUALIFIED");
        assertThat(response.path("record").path("samplingEvaluation").asText()).contains("组织过厚", "重新取材", "已通知取材人");
        assertThat(response.path("reworkType").asText()).isEqualTo("REGROSSING");
        assertThat(response.path("reworkStatus").asText()).isEqualTo("COMPLETED");

        JsonNode tracking = technicalTracking(context.caseId(), USER_M3_TRACKING);
        assertThat(tracking.path("embeddingRecords").get(0).path("sliceNotice").asText()).isEqualTo("皮肤");
        assertThat(tracking.path("embeddingRecords").get(0).path("samplingEvaluation").asText()).contains("有线头");

        JsonNode grossingTasks = listPendingTasks("GROSSING", context.pathologyNo(), USER_M3_GROSSING);
        assertThat(grossingTasks.path("items")).hasSize(1);
        assertThat(grossingTasks.path("items").get(0).path("taskStatus").asText()).isEqualTo("PENDING");
    }

    private void advanceCaseToPendingEmbedding(TechnicalCaseContext context, String grossDescription) throws Exception {
        advanceCaseToDehydrationCompleted(context, grossDescription);
    }

    private EmbeddingFixture advanceCaseToCompletedEmbedding(TechnicalCaseContext context,
                                                             String grossDescription,
                                                             String embeddingRemarks,
                                                             String samplingEvaluation,
                                                             String sliceNotice) throws Exception {
        String blockId = advanceCaseToDehydrationCompleted(context, grossDescription);
        String embeddingTaskId = listPendingTasks("EMBEDDING", context.pathologyNo(), USER_M3_EMBEDDING)
            .path("items").get(0).path("id").asText();
        postJson("/api/v1/embeddings/start", USER_M3_EMBEDDING, """
            {
              "taskId": "%s"
            }
            """.formatted(embeddingTaskId))
            .andExpect(status().isOk());
        JsonNode embedding = responseBody(postJson("/api/v1/embeddings/complete", USER_M3_EMBEDDING, """
            {
              "taskId": "%s",
              "samplingBlockId": "%s",
              "embeddingBoxNo": "EMB-BOX-001",
              "blockCount": 1,
              "sliceNotice": "%s",
              "evaluationLevel": "GOOD",
              "samplingEvaluation": "%s",
              "remarks": "%s"
            }
            """.formatted(embeddingTaskId, blockId, sliceNotice, samplingEvaluation, embeddingRemarks)), 200);
        return new EmbeddingFixture(blockId, embeddingTaskId, embedding.path("embeddingId").asText(), embedding.path("embeddingBoxId").asText());
    }

    private String advanceCaseToDehydrationCompleted(TechnicalCaseContext context, String grossDescription) throws Exception {
        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {
              "taskId": "%s"
            }
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
                  "grossDescription": "%s",
                  "blocks": [
                    {"blockSite": "A", "blockDescription": "block-1"}
                  ]
                }
              ]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId(), grossDescription))
            .andExpect(status().isOk());

        String blockId = listPendingTasks("DEHYDRATION", context.pathologyNo(), USER_M3_DEHYDRATION)
            .path("items").get(0).path("objectId").asText();
        String batchId = responseBody(postJson("/api/v1/dehydration-batches", USER_M3_DEHYDRATION, """
            {
              "caseId": "%s",
              "basketNo": "BASKET-%s",
              "samplingBlockIds": ["%s"]
            }
            """.formatted(context.caseId(), uniqueSuffix(), blockId)), 201).path("batchId").asText();
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
        return blockId;
    }

    private record EmbeddingFixture(String blockId, String embeddingTaskId, String embeddingId, String embeddingBoxId) {
    }
}
