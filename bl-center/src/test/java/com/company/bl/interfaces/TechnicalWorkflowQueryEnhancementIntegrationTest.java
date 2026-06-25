package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void shouldFilterPendingTasksByKeywordForPatientName() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-KEYWORD-NAME-001", "BC-M3-KEYWORD-NAME-001");
        String patientName = "取材关键字患者-" + uniqueSuffix();
        namedParameterJdbcTemplate.update("""
            update applications
            set patient_name = :patientName
            where id = :applicationId
            """, Map.of("patientName", patientName, "applicationId", context.applicationId()));

        mockMvc.perform(authorized(get("/api/v1/technical-tasks/pending"), USER_M3_GROSSING)
                .param("page", "1")
                .param("size", "20")
                .param("taskType", "GROSSING")
                .param("keyword", patientName))
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

        mockMvc.perform(authorized(get("/api/v1/technical-tasks/pending"), USER_M3_GROSSING)
                .param("page", "1")
                .param("size", "20")
                .param("taskType", "GROSSING")
                .param("pathologyNo", context.pathologyNo())
                .param("includeAllStatuses", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(context.grossingTaskId()))
            .andExpect(jsonPath("$.data.items[0].taskStatus").value("COMPLETED"));
    }

    @Test
    void shouldLoadGrossingWorkbenchContextForCompletedTask() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-GROSSING-READ-001", "BC-M3-GROSSING-READ-001");
        String grossDescription = "completed grossing description";

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
                  "grossDescription": "%s",
                  "blocks": [
                    {"blockSite": "A", "blockDescription": "block-read-1"}
                  ],
                  "embeddingBoxes": [
                    {
                      "sequenceNo": 1,
                      "embeddingBoxNo": "A1",
                      "boxName": "box-read-1",
                      "status": "CONFIRMED",
                      "embeddingRemarks": "read-only remark"
                    }
                  ]
                }
              ]
            }
            """.formatted(
                context.grossingTaskId(),
                context.caseId(),
                context.specimenId(),
                grossDescription))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/v1/grossings/{taskId}/context", context.grossingTaskId()), USER_M3_GROSSING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.task.taskStatus").value("COMPLETED"))
            .andExpect(jsonPath("$.data.tracking.blocks[0].grossDescription").value(grossDescription))
            .andExpect(jsonPath("$.data.tracking.blocks[0].embeddingBoxNo").value("A1"))
            .andExpect(jsonPath("$.data.tracking.blocks[0].embeddingBoxName").value("box-read-1"))
            .andExpect(jsonPath("$.data.tracking.blocks[0].description").value("block-read-1"))
            .andExpect(jsonPath("$.data.tracking.blocks[0].embeddingRemarks").value("read-only remark"));
    }

    @Test
    void shouldExposeNullEmbeddingBoxNameWhenHistoricalGrossingDidNotSaveOne() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-GROSSING-READ-002", "BC-M3-GROSSING-READ-002");

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
                  "grossDescription": "completed grossing description 2",
                  "blocks": [
                    {"blockSite": "B", "blockDescription": "block-read-2"}
                  ],
                  "embeddingBoxes": [
                    {
                      "sequenceNo": 1,
                      "embeddingBoxNo": "A1",
                      "boxName": " ",
                      "status": "CONFIRMED",
                      "embeddingRemarks": "remark-2"
                    }
                  ]
                }
              ]
            }
            """.formatted(
                context.grossingTaskId(),
                context.caseId(),
                context.specimenId()))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/v1/grossings/{taskId}/context", context.grossingTaskId()), USER_M3_GROSSING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.tracking.blocks[0].embeddingBoxNo").value("A1"))
            .andExpect(jsonPath("$.data.tracking.blocks[0].embeddingBoxName").value(nullValue()));
    }

    @Test
    void shouldStartGrossingAndLoadContextWithoutRegistrationWorkbenchRecord() throws Exception {
        TechnicalCaseContext registrationContext =
            receiveCaseAndGetPendingRegistration("APP-M3-GROSSING-LEGACY-001", "BC-M3-GROSSING-LEGACY-001");
        JsonNode completionResult =
            completeTechnicalSpecimenRegistration(registrationContext.caseId(), "legacy workbench missing");
        String pathologyNo = completionResult.path("pathologyNo").asText();

        namedParameterJdbcTemplate.update("""
            delete from application_registration_workbench
            where application_id = :applicationId
            """, Map.of("applicationId", registrationContext.applicationId()));

        JsonNode pendingTasks = listPendingTasks("GROSSING", pathologyNo, USER_M3_GROSSING);
        assertThat(pendingTasks.path("items")).hasSize(1);
        JsonNode pendingTask = pendingTasks.path("items").get(0);
        assertThat(pendingTask.path("pathologyNo").asText()).isEqualTo(pathologyNo);
        assertThat(pendingTask.path("patientIdDisplay").isMissingNode()
            || pendingTask.path("patientIdDisplay").isNull()
            || pendingTask.path("patientIdDisplay").asText().isBlank()).isTrue();

        String grossingTaskId = pendingTask.path("id").asText();

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "terminalCode": "TG-LEGACY-01"
            }
            """.formatted(grossingTaskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.taskId").value(grossingTaskId))
            .andExpect(jsonPath("$.data.taskStatus").value("IN_PROGRESS"));

        mockMvc.perform(authorized(get("/api/v1/grossings/{taskId}/context", grossingTaskId), USER_M3_GROSSING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.task.taskId").value(grossingTaskId))
            .andExpect(jsonPath("$.data.caseSummary.caseId").value(registrationContext.caseId()))
            .andExpect(jsonPath("$.data.caseSummary.applicationId").value(registrationContext.applicationId()))
            .andExpect(jsonPath("$.data.caseSummary.pathologyNo").value(pathologyNo))
            .andExpect(jsonPath("$.data.tracking.caseId").value(registrationContext.caseId()));
    }

    @Test
    void shouldExposeQcEvaluationsInTechnicalTracking() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-QC-TRACK-001", "BC-M3-QC-TRACK-001");

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
        printSlides(slicingTaskId, embeddingBoxId);
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

        TechnicalCaseContext confirmedContext = receiveCaseAndGetGrossingTask("APP-M3-EMB-SUM-003", "BC-M3-EMB-SUM-003");
        advanceCaseToEmbeddingConfirmPending(confirmedContext, "confirm pending summary gross");

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

        assertThat(summary.path("pendingCount").asInt()).isGreaterThanOrEqualTo(2);
        assertThat(summary.path("completedCount").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(summary.path("pendingTasks").toString()).contains(pendingContext.pathologyNo());
        assertThat(summary.path("pendingTasks").toString()).contains(confirmedContext.pathologyNo());
        assertThat(summary.path("pendingTasks").toString()).contains("EMBEDDING_CONFIRM_PENDING");
        JsonNode pendingTask = findSummaryRecord(summary.path("pendingTasks"), pendingContext.pathologyNo());
        assertThat(pendingTask).isNotNull();
        assertThat(pendingTask.path("specimenName").asText()).isEqualTo("Thyroid Tissue");
        assertThat(pendingTask.path("grossDescription").asText()).isEqualTo("pending summary gross");
        JsonNode confirmedTask = findSummaryRecord(summary.path("pendingTasks"), confirmedContext.pathologyNo());
        assertThat(confirmedTask).isNotNull();
        assertThat(confirmedTask.path("samplingBlockDescription").asText()).isNotBlank();
        assertThat(confirmedTask.path("specimenName").asText()).isEqualTo("Thyroid Tissue");
        assertThat(confirmedTask.path("grossDescription").asText()).isEqualTo("confirm pending summary gross");
        JsonNode completedRecord = findSummaryRecord(summary.path("completedRecords"), completedContext.pathologyNo());
        assertThat(completedRecord).isNotNull();
        assertThat(completedRecord.path("samplingEvaluation").asText()).isEqualTo("取材评价-汇总");
        assertThat(completedRecord.path("embeddingRemarks").asText()).isEqualTo("包埋备注-汇总");
        assertThat(completedRecord.path("grossDescription").asText()).isEqualTo("summary gross description");
        assertThat(completedRecord.path("embeddingBoxId").asText()).isEqualTo(completedFixture.embeddingBoxId());
    }

    @Test
    void shouldConfirmEmbeddingWorkstationClearOncePerDayEvenWithPendingTasks() throws Exception {
        TechnicalCaseContext pendingContext = receiveCaseAndGetGrossingTask("APP-M3-EMB-CLEAR-001", "BC-M3-EMB-CLEAR-001");
        advanceCaseToPendingEmbedding(pendingContext, "clear pending gross");

        JsonNode summaryBefore = responseBody(mockMvc.perform(authorized(
            get("/api/v1/embeddings/workstation-summary"),
            USER_M3_EMBEDDING)), 200);
        assertThat(summaryBefore.path("dailyClear").path("cleared").asBoolean()).isFalse();
        assertThat(summaryBefore.path("pendingCount").asInt()).isGreaterThan(0);

        JsonNode clearResult = responseBody(postJson("/api/v1/embeddings/workstation-clear", USER_M3_EMBEDDING, "{}"), 200);
        assertThat(clearResult.path("cleared").asBoolean()).isTrue();
        assertThat(clearResult.path("operatorUserId").asText()).isEqualTo(USER_M3_EMBEDDING);
        assertThat(clearResult.path("clearStatus").asText()).isEqualTo("CLEARED");
        assertThat(clearResult.path("clearedAt").asText()).isNotBlank();

        JsonNode summaryAfter = responseBody(mockMvc.perform(authorized(
            get("/api/v1/embeddings/workstation-summary"),
            USER_M3_EMBEDDING)), 200);
        assertThat(summaryAfter.path("dailyClear").path("cleared").asBoolean()).isTrue();
        assertThat(summaryAfter.path("dailyClear").path("operatorUserId").asText()).isEqualTo(USER_M3_EMBEDDING);
        assertThat(summaryAfter.path("pendingCount").asInt()).isGreaterThan(0);

        postJson("/api/v1/embeddings/workstation-clear", USER_M3_EMBEDDING, "{}")
            .andExpect(status().isConflict());
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

    @Test
    void shouldFilterTechnicalTrackingDetailsByWorkDate() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-TRACK-DATE-001", "BC-M3-TRACK-DATE-001");
        advanceCaseToCompletedEmbedding(
            context,
            "tracking date gross",
            "tracking date remark",
            "tracking date evaluation",
            "tracking date notice");

        String slicingTaskId = listPendingTasks("SLICING", context.pathologyNo(), USER_M3_SLICING)
            .path("items").get(0).path("id").asText();
        String embeddingBoxId = namedParameterJdbcTemplate.queryForObject("""
            select id
            from embedding_boxes
            where case_id = :caseId
            order by created_at desc
            limit 1
            """, Map.of("caseId", context.caseId()), String.class);
        postJson("/api/v1/slicings/start", USER_M3_SLICING, """
            {
              "taskId": "%s"
            }
            """.formatted(slicingTaskId))
            .andExpect(status().isOk());
        printSlides(slicingTaskId, embeddingBoxId);
        String slideId = responseBody(postJson("/api/v1/slicings/complete", USER_M3_SLICING, """
            {
              "taskId": "%s",
              "embeddingBoxId": "%s",
              "slideCount": 1
            }
            """.formatted(slicingTaskId, embeddingBoxId)), 200).path("slideIds").get(0).asText();

        postJson("/api/v1/slide-qc-evaluations", USER_M3_SLICING, """
            {
              "caseId": "%s",
              "specimenId": "%s",
              "slideId": "%s",
              "qcType": "HE",
              "evaluationResult": "UNQUALIFIED",
              "issueDescription": "日期过滤",
              "improvementSuggestion": "返工",
              "remarks": "日期过滤"
            }
            """.formatted(context.caseId(), context.specimenId(), slideId))
            .andExpect(status().isOk());

        postJson("/api/v1/rework-orders", USER_M3_REWORK, """
            {
              "caseId": "%s",
              "specimenId": "%s",
              "slideId": "%s",
              "reworkType": "RESTAIN",
              "reason": "date-filter"
            }
            """.formatted(context.caseId(), context.specimenId(), slideId))
            .andExpect(status().isOk());

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime yesterdayTime = yesterday.atTime(11, 0);

        namedParameterJdbcTemplate.update("""
            update technical_pending_tasks
            set created_at = :value,
                started_at = :value,
                completed_at = :value
            where case_id = :caseId
            """, Map.of("value", yesterdayTime, "caseId", context.caseId()));
        namedParameterJdbcTemplate.update("""
            update embeddings
            set started_at = :value,
                ended_at = :value,
                created_at = :value
            where case_id = :caseId
            """, Map.of("value", yesterdayTime, "caseId", context.caseId()));
        namedParameterJdbcTemplate.update("""
            update samplings
            set sampled_at = :value
            where case_id = :caseId
            """, Map.of("value", yesterdayTime, "caseId", context.caseId()));
        namedParameterJdbcTemplate.update("""
            update slide_qc_evaluations
            set evaluated_at = :value,
                created_at = :value
            where case_id = :caseId
            """, Map.of("value", yesterdayTime, "caseId", context.caseId()));
        namedParameterJdbcTemplate.update("""
            update slides
            set created_at = :value,
                updated_at = :value
            where case_id = :caseId
            """, Map.of("value", yesterdayTime, "caseId", context.caseId()));
        namedParameterJdbcTemplate.update("""
            update rework_orders
            set requested_at = :value,
                created_at = :value,
                updated_at = :value
            where case_id = :caseId
            """, Map.of("value", yesterdayTime, "caseId", context.caseId()));
        namedParameterJdbcTemplate.update("""
            update workflow_events
            set event_time = :value,
                created_at = :value
            where case_id = :caseId
            """, Map.of("value", yesterdayTime, "caseId", context.caseId()));

        namedParameterJdbcTemplate.update("""
            update slides
            set created_at = :value,
                updated_at = :value
            where id = :slideId
            """, Map.of("value", today.atTime(9, 0), "slideId", slideId));

        JsonNode todayTracking = technicalTracking(context.caseId(), USER_M3_TRACKING, today.toString());
        assertThat(todayTracking.path("technicalTasks")).isEmpty();
        assertThat(todayTracking.path("embeddingRecords")).isEmpty();
        assertThat(todayTracking.path("embeddingEvaluationRecords")).isEmpty();
        assertThat(todayTracking.path("slides")).hasSize(1);
        assertThat(todayTracking.path("slides").get(0).path("slideId").asText()).isEqualTo(slideId);
        assertThat(todayTracking.path("qcEvaluations")).isEmpty();
        assertThat(todayTracking.path("reworks")).isEmpty();
        assertThat(todayTracking.path("events")).isEmpty();
        assertThat(todayTracking.path("specimens")).hasSize(1);
        assertThat(todayTracking.path("blocks")).isEmpty();
        assertThat(todayTracking.path("embeddingBoxes")).hasSize(1);

        JsonNode yesterdayTracking = technicalTracking(context.caseId(), USER_M3_TRACKING, yesterday.toString());
        assertThat(yesterdayTracking.path("embeddingRecords")).hasSize(1);
        assertThat(yesterdayTracking.path("slides")).isEmpty();
        assertThat(yesterdayTracking.path("qcEvaluations")).hasSize(2);
        assertThat(yesterdayTracking.path("reworks")).hasSize(1);
        assertThat(yesterdayTracking.path("events")).isNotEmpty();
    }

    @Test
    void shouldListTechnicalTrackingCasesByWorkDate() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-TRACK-LIST-001", "BC-M3-TRACK-LIST-001");

        JsonNode result = technicalTrackingCases(USER_M3_TRACKING, null, null, LocalDate.now().toString());

        assertThat(result.path("items")).isNotEmpty();
        JsonNode row = result.path("items").get(0);
        assertThat(result.path("items").toString()).contains(context.caseId());
        assertThat(row.path("matchedActivityTypes").isArray()).isTrue();
        assertThat(row.path("latestActivityAt").asText()).isNotBlank();
    }

    @Test
    void shouldIncludeCasesMatchedOnlyByQcReworkOrEventInTechnicalTrackingCaseList() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-TRACK-LIST-002", "BC-M3-TRACK-LIST-002");
        advanceCaseToCompletedEmbedding(
            context,
            "tracking list gross",
            "tracking list remark",
            "tracking list evaluation",
            "tracking list notice");

        String slicingTaskId = listPendingTasks("SLICING", context.pathologyNo(), USER_M3_SLICING)
            .path("items").get(0).path("id").asText();
        String embeddingBoxId = namedParameterJdbcTemplate.queryForObject("""
            select id
            from embedding_boxes
            where case_id = :caseId
            order by created_at desc
            limit 1
            """, Map.of("caseId", context.caseId()), String.class);
        postJson("/api/v1/slicings/start", USER_M3_SLICING, """
            {
              "taskId": "%s"
            }
            """.formatted(slicingTaskId))
            .andExpect(status().isOk());
        printSlides(slicingTaskId, embeddingBoxId);
        String slideId = responseBody(postJson("/api/v1/slicings/complete", USER_M3_SLICING, """
            {
              "taskId": "%s",
              "embeddingBoxId": "%s",
              "slideCount": 1
            }
            """.formatted(slicingTaskId, embeddingBoxId)), 200).path("slideIds").get(0).asText();

        postJson("/api/v1/slide-qc-evaluations", USER_M3_SLICING, """
            {
              "caseId": "%s",
              "specimenId": "%s",
              "slideId": "%s",
              "qcType": "HE",
              "evaluationResult": "UNQUALIFIED",
              "issueDescription": "case-list-only-qc",
              "improvementSuggestion": "返工"
            }
            """.formatted(context.caseId(), context.specimenId(), slideId))
            .andExpect(status().isOk());

        postJson("/api/v1/rework-orders", USER_M3_REWORK, """
            {
              "caseId": "%s",
              "specimenId": "%s",
              "slideId": "%s",
              "reworkType": "RESTAIN",
              "reason": "case-list-only-rework"
            }
            """.formatted(context.caseId(), context.specimenId(), slideId))
            .andExpect(status().isOk());

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime yesterdayTime = yesterday.atTime(11, 0);

        namedParameterJdbcTemplate.update("""
            update technical_pending_tasks
            set created_at = :value,
                started_at = :value,
                completed_at = :value
            where case_id = :caseId
            """, Map.of("value", yesterdayTime, "caseId", context.caseId()));
        namedParameterJdbcTemplate.update("""
            update embeddings
            set started_at = :value,
                ended_at = :value,
                created_at = :value
            where case_id = :caseId
            """, Map.of("value", yesterdayTime, "caseId", context.caseId()));
        namedParameterJdbcTemplate.update("""
            update workflow_events
            set event_time = :value,
                created_at = :value
            where case_id = :caseId
            """, Map.of("value", today.atTime(10, 0), "caseId", context.caseId()));

        JsonNode result = technicalTrackingCases(USER_M3_TRACKING, null, null, today.toString());

        JsonNode matched = null;
        for (JsonNode item : result.path("items")) {
            if (context.caseId().equals(item.path("caseId").asText())) {
                matched = item;
                break;
            }
        }
        assertThat(matched).isNotNull();
        assertThat(matched.path("matchedActivityTypes").toString()).contains("EVENT", "QC", "REWORK");
    }

    @Test
    void shouldSortTechnicalTrackingCaseListByLatestActivityAtDesc() throws Exception {
        TechnicalCaseContext older = receiveCaseAndGetGrossingTask("APP-M3-TRACK-LIST-003", "BC-M3-TRACK-LIST-003");
        TechnicalCaseContext newer = receiveCaseAndGetGrossingTask("APP-M3-TRACK-LIST-004", "BC-M3-TRACK-LIST-004");
        LocalDate today = LocalDate.now();

        namedParameterJdbcTemplate.update("""
            update technical_pending_tasks
            set created_at = :value
            where case_id = :caseId
            """, Map.of("value", today.atTime(9, 0), "caseId", older.caseId()));
        namedParameterJdbcTemplate.update("""
            update technical_pending_tasks
            set created_at = :value
            where case_id = :caseId
            """, Map.of("value", today.atTime(14, 0), "caseId", newer.caseId()));

        JsonNode result = technicalTrackingCases(USER_M3_TRACKING, null, null, today.toString());

        int olderIndex = Integer.MAX_VALUE;
        int newerIndex = Integer.MAX_VALUE;
        for (int index = 0; index < result.path("items").size(); index++) {
            JsonNode item = result.path("items").get(index);
            if (older.caseId().equals(item.path("caseId").asText())) {
                olderIndex = index;
            }
            if (newer.caseId().equals(item.path("caseId").asText())) {
                newerIndex = index;
            }
        }
        assertThat(newerIndex).isLessThan(olderIndex);
    }

    @Test
    void shouldFallbackToParentProductionRemarksForHistoricalStainingTasks() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-STAIN-REMARK-001", "BC-M3-STAIN-REMARK-001");
        EmbeddingFixture fixture = advanceCaseToCompletedEmbedding(
            context,
            "staining remark gross",
            "staining remark embedding",
            "staining remark evaluation",
            "staining remark notice");
        String slicingTaskId = listPendingTasks("SLICING", context.pathologyNo(), USER_M3_SLICING)
            .path("items").get(0).path("id").asText();
        String parentProductionRemarks = "历史切片主班备注";
        namedParameterJdbcTemplate.update("""
            update technical_pending_tasks
            set production_remarks = :productionRemarks
            where id = :taskId
            """, Map.of("productionRemarks", parentProductionRemarks, "taskId", slicingTaskId));

        postJson("/api/v1/slicings/start", USER_M3_SLICING, """
            {
              "taskId": "%s"
            }
            """.formatted(slicingTaskId))
            .andExpect(status().isOk());
        printSlides(slicingTaskId, fixture.embeddingBoxId());
        postJson("/api/v1/slicings/complete", USER_M3_SLICING, """
            {
              "taskId": "%s",
              "embeddingBoxId": "%s",
              "slideCount": 1
            }
            """.formatted(slicingTaskId, fixture.embeddingBoxId()))
            .andExpect(status().isOk());

        String stainingTaskId = listPendingTasks("STAINING", context.pathologyNo(), USER_M3_STAINING)
            .path("items").get(0).path("id").asText();
        namedParameterJdbcTemplate.update("""
            update technical_pending_tasks
            set production_remarks = null
            where id = :taskId
            """, Map.of("taskId", stainingTaskId));

        mockMvc.perform(authorized(get("/api/v1/technical-tasks/pending"), USER_M3_STAINING)
                .param("page", "1")
                .param("size", "20")
                .param("taskType", "STAINING")
                .param("pathologyNo", context.pathologyNo()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].productionRemarks").value(parentProductionRemarks));
    }

    @Test
    void shouldRejectTechnicalTrackingCaseListWithoutDateFilters() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/technical-tracking/cases"), USER_M3_TRACKING)
                .param("page", "1")
                .param("size", "20"))
            .andExpect(status().isBadRequest());
    }

    private void advanceCaseToPendingEmbedding(TechnicalCaseContext context, String grossDescription) throws Exception {
        advanceCaseToDehydrationCompleted(context, grossDescription);
    }

    private void advanceCaseToEmbeddingConfirmPending(TechnicalCaseContext context, String grossDescription) throws Exception {
        advanceCaseToDehydrationCompleted(context, grossDescription);
        String embeddingTaskId = listPendingTasks("EMBEDDING", context.pathologyNo(), USER_M3_EMBEDDING)
            .path("items").get(0).path("id").asText();
        postJson("/api/v1/embeddings/start", USER_M3_EMBEDDING, """
            {
              "taskId": "%s"
            }
            """.formatted(embeddingTaskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.taskStatus").value("EMBEDDING_CONFIRM_PENDING"));

        JsonNode slicingTasks = listPendingTasks("SLICING", context.pathologyNo(), USER_M3_SLICING);
        assertThat(slicingTasks.path("items")).isEmpty();
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
              "blockCount": 1,
              "sliceNotice": "%s",
              "evaluationLevel": "GOOD",
              "samplingEvaluation": "%s",
              "remarks": "%s"
            }
            """.formatted(embeddingTaskId, blockId, sliceNotice, samplingEvaluation, embeddingRemarks)), 200);
        return new EmbeddingFixture(blockId, embeddingTaskId, embedding.path("embeddingId").asText(), embedding.path("embeddingBoxId").asText());
    }

    private JsonNode findSummaryRecord(JsonNode records, String pathologyNo) {
        for (JsonNode record : records) {
            if (pathologyNo.equals(record.path("pathologyNo").asText())) {
                return record;
            }
        }
        return null;
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
