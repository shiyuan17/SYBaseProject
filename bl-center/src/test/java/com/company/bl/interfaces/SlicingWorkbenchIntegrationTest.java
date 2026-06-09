package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class SlicingWorkbenchIntegrationTest extends AbstractTechnicalWorkflowIntegrationTest {

    @Test
    void shouldExposeSlicingWorkbenchAggregationAndFilters() throws Exception {
        SlicingReadyContext freshContext = prepareSlicingReadyContext("APP-M3-SLICE-001", "BC-M3-SLICE-001");
        SlicingReadyContext overdueContext = prepareSlicingReadyContext("APP-M3-SLICE-002", "BC-M3-SLICE-002");
        SlicingReadyContext completedContext = prepareSlicingReadyContext("APP-M3-SLICE-003", "BC-M3-SLICE-003");

        markSlicingTaskOverdue(overdueContext.slicingTaskId());
        String slideId = completeSlicingCase(completedContext);

        mockMvc.perform(authorized(get("/api/v1/slicings/workbench"), USER_M3_SLICING)
                .param("keyword", completedContext.baseContext().pathologyNo())
                .param("pendingPage", "1")
                .param("pendingSize", "20")
                .param("completedPage", "1")
                .param("completedSize", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.stats.pendingTodayCount").value(0))
            .andExpect(jsonPath("$.data.stats.pendingTomorrowCount").value(0))
            .andExpect(jsonPath("$.data.stats.completedMineTodayCount").value(1))
            .andExpect(jsonPath("$.data.stats.completedDeptTodayCount").value(1))
            .andExpect(jsonPath("$.data.stats.overdueCount").value(0))
            .andExpect(jsonPath("$.data.pendingTotal").value(0))
            .andExpect(jsonPath("$.data.pendingPrintTotal").value(0))
            .andExpect(jsonPath("$.data.pendingSliceTotal").value(0))
            .andExpect(jsonPath("$.data.completedTotal").value(1))
            .andExpect(jsonPath("$.data.completedTodayList[0].slideId").value(slideId));

        mockMvc.perform(authorized(get("/api/v1/slicings/workbench"), USER_M3_SLICING)
                .param("keyword", freshContext.baseContext().pathologyNo())
                .param("pendingPage", "1")
                .param("pendingSize", "20")
                .param("completedPage", "1")
                .param("completedSize", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pendingTotal").value(1))
            .andExpect(jsonPath("$.data.pendingPrintTotal").value(1))
            .andExpect(jsonPath("$.data.pendingSliceTotal").value(0))
            .andExpect(jsonPath("$.data.completedTotal").value(0))
            .andExpect(jsonPath("$.data.pendingList[0].pathologyNo").value(freshContext.baseContext().pathologyNo()))
            .andExpect(jsonPath("$.data.pendingPrintList[0].slidePrintStatus").value("PENDING"));

        mockMvc.perform(authorized(get("/api/v1/slicings/workbench"), USER_M3_SLICING)
                .param("keyword", overdueContext.baseContext().pathologyNo())
                .param("overdueOnly", "true")
                .param("pendingPage", "1")
                .param("pendingSize", "20")
                .param("completedPage", "1")
                .param("completedSize", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pendingTotal").value(1))
            .andExpect(jsonPath("$.data.pendingList[0].taskId").value(overdueContext.slicingTaskId()))
            .andExpect(jsonPath("$.data.pendingList[0].timedOut").value(true));

        mockMvc.perform(authorized(get("/api/v1/slicings/workbench"), USER_M3_SLICING)
                .param("keyword", freshContext.baseContext().pathologyNo())
                .param("pendingTodayOnly", "true")
                .param("pendingPage", "1")
                .param("pendingSize", "20")
                .param("completedPage", "1")
                .param("completedSize", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pendingTotal").value(1))
            .andExpect(jsonPath("$.data.pendingList[0].taskId").value(freshContext.slicingTaskId()));
    }

    @Test
    void shouldCreateSlideQcEvaluationAndExposeTracking() throws Exception {
        SlicingReadyContext context = prepareSlicingReadyContext("APP-M3-QC-001", "BC-M3-QC-001");
        String slideId = completeSlicingCase(context);

        postJson("/api/v1/slide-qc-evaluations", USER_M3_SLICING, """
            {
              "caseId": "%s",
              "specimenId": "%s",
              "slideId": "%s",
              "qcType": "HE",
              "evaluationResult": "UNQUALIFIED",
              "issueDescription": "切片有折痕",
              "improvementSuggestion": "重新切片",
              "remarks": "现场评价"
            }
            """.formatted(context.baseContext().caseId(), context.baseContext().specimenId(), slideId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.slideId").value(slideId))
            .andExpect(jsonPath("$.data.evaluationResult").value("UNQUALIFIED"))
            .andExpect(jsonPath("$.data.qualityStatus").value("UNQUALIFIED"));

        JsonNode tracking = technicalTracking(context.baseContext().caseId(), USER_M3_TRACKING);
        assertThat(tracking.path("qcEvaluations")).hasSize(1);
        assertThat(tracking.path("qcEvaluations").get(0).path("issueDescription").asText()).isEqualTo("切片有折痕");
        assertThat(tracking.path("slides").get(0).path("qualityStatus").asText()).isEqualTo("UNQUALIFIED");

        Long qualityCount = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from slide_qc_evaluations
            where case_id = :caseId
            """, new MapSqlParameterSource().addValue("caseId", context.baseContext().caseId()), Long.class);
        assertThat(qualityCount).isEqualTo(1L);
    }

    @Test
    void shouldPrintMergedSlidesBeforeSlicingAndRejectDuplicatePrint() throws Exception {
        SlicingReadyContext context = prepareSlicingReadyContext("APP-M3-SLICE-004", "BC-M3-SLICE-004");
        postJson("/api/v1/slicings/start", USER_M3_SLICING, """
            {
              "taskId": "%s",
              "terminalCode": "TS-MERGE"
            }
            """.formatted(context.slicingTaskId()))
            .andExpect(status().isOk());

        JsonNode printResult = responseBody(postJson("/api/v1/slicings/slide-print", USER_M3_SLICING, """
            {
              "taskId": "%s",
              "embeddingBoxId": "%s",
              "sourceSlideCount": 5,
              "mergeAdjacent": true,
              "printerCode": "PRN-1",
              "terminalCode": "TS-MERGE"
            }
            """.formatted(context.slicingTaskId(), context.embeddingBoxId())), 200);

        assertThat(printResult.path("printedSlideCount").asInt()).isEqualTo(3);
        assertThat(printResult.path("slideNos").get(0).asText()).contains("-");
        assertThat(printResult.path("slideNos").get(2).asText()).doesNotContain("-");

        postJson("/api/v1/slicings/slide-print", USER_M3_SLICING, """
            {
              "taskId": "%s",
              "embeddingBoxId": "%s",
              "sourceSlideCount": 2,
              "mergeAdjacent": true
            }
            """.formatted(context.slicingTaskId(), context.embeddingBoxId()))
            .andExpect(status().isConflict());

        mockMvc.perform(authorized(get("/api/v1/slicings/workbench"), USER_M3_SLICING)
                .param("keyword", context.baseContext().pathologyNo())
                .param("pendingPage", "1")
                .param("pendingSize", "20")
                .param("completedPage", "1")
                .param("completedSize", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pendingPrintTotal").value(0))
            .andExpect(jsonPath("$.data.pendingSliceTotal").value(1))
            .andExpect(jsonPath("$.data.pendingSliceList[0].printedSlideCount").value(3))
            .andExpect(jsonPath("$.data.pendingSliceList[0].combinedSlide").value(true));
    }

    private SlicingReadyContext prepareSlicingReadyContext(String applicationNo, String barcode) throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask(applicationNo, barcode);

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "terminalCode": "TG-%s"
            }
            """.formatted(context.grossingTaskId(), applicationNo.substring(applicationNo.length() - 3)))
            .andExpect(status().isOk());

        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "caseId": "%s",
              "terminalCode": "TG-%s",
              "specimens": [
                {
                  "specimenId": "%s",
                  "specimenType": "ROUTINE",
                  "grossDescription": "gross-%s",
                  "blocks": [
                    {
                      "blockSite": "A",
                      "blockDescription": "block-%s"
                    }
                  ]
                }
              ]
            }
            """.formatted(
                context.grossingTaskId(),
                context.caseId(),
                applicationNo.substring(applicationNo.length() - 3),
                context.specimenId(),
                applicationNo,
                applicationNo))
            .andExpect(status().isOk());

        String samplingBlockId = listPendingTasks("DEHYDRATION", context.pathologyNo(), USER_M3_DEHYDRATION)
            .path("items").get(0).path("objectId").asText();

        JsonNode dehydrationBatch = responseBody(postJson("/api/v1/dehydration-batches", USER_M3_DEHYDRATION, """
            {
              "caseId": "%s",
              "basketNo": "BASKET-%s",
              "samplingBlockIds": ["%s"]
            }
            """.formatted(context.caseId(), applicationNo, samplingBlockId)), 201);
        String batchId = dehydrationBatch.path("batchId").asText();

        postJson("/api/v1/dehydration-batches/%s/start".formatted(batchId), USER_M3_DEHYDRATION, """
            {
              "terminalCode": "TD-%s"
            }
            """.formatted(applicationNo.substring(applicationNo.length() - 3)))
            .andExpect(status().isOk());
        postJson("/api/v1/dehydration-batches/%s/complete".formatted(batchId), USER_M3_DEHYDRATION, """
            {
              "terminalCode": "TD-%s"
            }
            """.formatted(applicationNo.substring(applicationNo.length() - 3)))
            .andExpect(status().isOk());

        String embeddingTaskId = listPendingTasks("EMBEDDING", context.pathologyNo(), USER_M3_EMBEDDING)
            .path("items").get(0).path("id").asText();
        postJson("/api/v1/embeddings/start", USER_M3_EMBEDDING, """
            {
              "taskId": "%s",
              "terminalCode": "TE-%s"
            }
            """.formatted(embeddingTaskId, applicationNo.substring(applicationNo.length() - 3)))
            .andExpect(status().isOk());

        JsonNode embedding = responseBody(postJson("/api/v1/embeddings/complete", USER_M3_EMBEDDING, """
            {
              "taskId": "%s",
              "samplingBlockId": "%s",
              "blockCount": 1,
              "sliceNotice": "slice-%s",
              "terminalCode": "TE-%s"
            }
            """.formatted(
                embeddingTaskId,
                samplingBlockId,
                applicationNo,
                applicationNo.substring(applicationNo.length() - 3))), 200);
        String embeddingBoxId = embedding.path("embeddingBoxId").asText();

        String slicingTaskId = listPendingTasks("SLICING", context.pathologyNo(), USER_M3_SLICING)
            .path("items").get(0).path("id").asText();
        return new SlicingReadyContext(context, embeddingBoxId, slicingTaskId);
    }

    private String completeSlicingCase(SlicingReadyContext context) throws Exception {
        postJson("/api/v1/slicings/start", USER_M3_SLICING, """
            {
              "taskId": "%s",
              "terminalCode": "TS-COMPLETE"
            }
            """.formatted(context.slicingTaskId()))
            .andExpect(status().isOk());

        postJson("/api/v1/slicings/slide-print", USER_M3_SLICING, """
            {
              "taskId": "%s",
              "embeddingBoxId": "%s",
              "sourceSlideCount": 1,
              "terminalCode": "TS-COMPLETE"
            }
            """.formatted(context.slicingTaskId(), context.embeddingBoxId()))
            .andExpect(status().isOk());

        JsonNode slicing = responseBody(postJson("/api/v1/slicings/complete", USER_M3_SLICING, """
            {
              "taskId": "%s",
              "embeddingBoxId": "%s",
              "terminalCode": "TS-COMPLETE"
            }
            """.formatted(context.slicingTaskId(), context.embeddingBoxId())), 200);
        return slicing.path("slideIds").get(0).asText();
    }

    private void markSlicingTaskOverdue(String taskId) {
        namedParameterJdbcTemplate.update("""
            update technical_pending_tasks
            set created_at = :createdAt,
                updated_at = :updatedAt
            where id = :taskId
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("createdAt", java.time.LocalDateTime.now().minusDays(2))
            .addValue("updatedAt", java.time.LocalDateTime.now().minusDays(2)));
    }

    private record SlicingReadyContext(
        TechnicalCaseContext baseContext,
        String embeddingBoxId,
        String slicingTaskId
    ) {
    }
}
