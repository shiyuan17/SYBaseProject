package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            .andExpect(jsonPath("$.data.pendingPrintList[0].embeddingBoxNo").exists())
            .andExpect(jsonPath("$.data.pendingPrintList[0].embeddingRemarks").value("embedding-APP-M3-SLICE-001"))
            .andExpect(jsonPath("$.data.pendingPrintList[0].submittingDepartmentName").value("OR"))
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

    @Test
    void shouldExposeSlicingWorkbenchFieldsForLegacySamplingBlockTasks() throws Exception {
        SlicingReadyContext context = prepareSlicingReadyContext("APP-M3-SLICE-005", "BC-M3-SLICE-005");
        String samplingBlockId = namedParameterJdbcTemplate.queryForObject("""
            select sampling_block_id
            from embedding_boxes
            where id = :embeddingBoxId
            """, new MapSqlParameterSource().addValue("embeddingBoxId", context.embeddingBoxId()), String.class);
        String expectedEmbeddingBoxNo = namedParameterJdbcTemplate.queryForObject("""
            select embedding_box_no
            from sampling_blocks
            where id = :samplingBlockId
            """, new MapSqlParameterSource().addValue("samplingBlockId", samplingBlockId), String.class);

        namedParameterJdbcTemplate.update("""
            update sampling_blocks
            set embedding_remarks = :embeddingRemarks
            where id = :samplingBlockId
            """, new MapSqlParameterSource()
            .addValue("samplingBlockId", samplingBlockId)
            .addValue("embeddingRemarks", "legacy-sampling-remark"));
        namedParameterJdbcTemplate.update("""
            update embeddings
            set remarks = null
            where id = (
                select embedding_id
                from embedding_boxes
                where id = :embeddingBoxId
            )
            """, new MapSqlParameterSource().addValue("embeddingBoxId", context.embeddingBoxId()));
        namedParameterJdbcTemplate.update("""
            update technical_pending_tasks
            set object_type = 'SAMPLING_BLOCK',
                object_id = :samplingBlockId
            where id = :taskId
            """, new MapSqlParameterSource()
            .addValue("taskId", context.slicingTaskId())
            .addValue("samplingBlockId", samplingBlockId));

        mockMvc.perform(authorized(get("/api/v1/slicings/workbench"), USER_M3_SLICING)
                .param("keyword", context.baseContext().pathologyNo())
                .param("pendingPage", "1")
                .param("pendingSize", "20")
                .param("completedPage", "1")
                .param("completedSize", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pendingPrintList[0].embeddingBoxId").value(context.embeddingBoxId()))
            .andExpect(jsonPath("$.data.pendingPrintList[0].embeddingBoxNo").value(expectedEmbeddingBoxNo))
            .andExpect(jsonPath("$.data.pendingPrintList[0].embeddingRemarks").value("legacy-sampling-remark"))
            .andExpect(jsonPath("$.data.pendingPrintList[0].selectable").value(true));
    }

    @Test
    void shouldCreateCancelAndPrintPendingSlideMergeGroups() throws Exception {
        MergeReadyContext context = prepareSlicingMergeReadyContext(
            "APP-M3-MERGE-001",
            "BC-M3-MERGE-001",
            List.of("A1", "A2", "A3", "A4", "B1", "B2", "B3", "B4", "B5"));

        JsonNode mergeResult = responseBody(postJson("/api/v1/slicings/slide-print-merge-groups", USER_M3_SLICING, """
            {
              "taskIds": [%s],
              "terminalCode": "TS-MERGE-GROUP"
            }
            """.formatted(quotedJsonArray(context.taskIdsByBoxNo().values().stream().toList()))), 200);
        assertThat(mergeResult.path("printGroupIds")).hasSize(4);

        JsonNode workbench = responseBody(mockMvc.perform(authorized(get("/api/v1/slicings/workbench"), USER_M3_SLICING)
                .param("keyword", context.baseContext().pathologyNo())
                .param("pendingPage", "1")
                .param("pendingSize", "20")
                .param("completedPage", "1")
                .param("completedSize", "20")), 200);
        assertThat(workbench.path("pendingPrintTotal").asInt()).isEqualTo(5);
        assertThat(workbench.path("pendingPrintList"))
            .extracting(item -> item.path("embeddingBoxNo").asText())
            .containsExactly("A1+A2", "A3+A4", "B1+B2", "B3+B4", "B5");
        assertThat(workbench.path("pendingPrintList").get(0).path("mergedPrintGroup").asBoolean()).isTrue();
        assertThat(workbench.path("pendingPrintList").get(0).path("taskIds")).hasSize(2);

        String firstGroupId = mergeResult.path("printGroupIds").get(0).asText();
        JsonNode printResult = responseBody(postJson("/api/v1/slicings/slide-print-merge-groups/print", USER_M3_SLICING, """
            {
              "printGroupId": "%s",
              "printerCode": "PRN-MERGE",
              "terminalCode": "TS-MERGE-GROUP"
            }
            """.formatted(firstGroupId)), 200);
        assertThat(printResult.path("merged").asBoolean()).isTrue();
        assertThat(printResult.path("printedSlideCount").asInt()).isEqualTo(2);

        String secondGroupId = mergeResult.path("printGroupIds").get(1).asText();
        responseBody(postJson("/api/v1/slicings/slide-print-merge-groups/cancel", USER_M3_SLICING, """
            {
              "printGroupIds": ["%s"],
              "terminalCode": "TS-MERGE-GROUP"
            }
            """.formatted(secondGroupId)), 200);

        JsonNode afterCancel = responseBody(mockMvc.perform(authorized(get("/api/v1/slicings/workbench"), USER_M3_SLICING)
                .param("keyword", context.baseContext().pathologyNo())
                .param("pendingPage", "1")
                .param("pendingSize", "20")
                .param("completedPage", "1")
                .param("completedSize", "20")), 200);
        assertThat(afterCancel.path("pendingPrintList"))
            .extracting(item -> item.path("embeddingBoxNo").asText())
            .contains("A3", "A4");
    }

    @Test
    void shouldExposePrintedMergeGroupAsMergedPendingSliceRow() throws Exception {
        MergeReadyContext context = prepareSlicingMergeReadyContext(
            "APP-M3-MERGE-PRINTED-001",
            "BC-M3-MERGE-PRINTED-001",
            List.of("A1", "A2", "B1"));

        JsonNode mergeResult = responseBody(postJson("/api/v1/slicings/slide-print-merge-groups", USER_M3_SLICING, """
            {
              "taskIds": [%s],
              "terminalCode": "TS-MERGED-PENDING-SLICE"
            }
            """.formatted(quotedJsonArray(context.taskIdsByBoxNo().values().stream().toList()))), 200);
        assertThat(mergeResult.path("printGroupIds")).hasSize(1);

        String printGroupId = mergeResult.path("printGroupIds").get(0).asText();
        JsonNode printResult = responseBody(postJson("/api/v1/slicings/slide-print-merge-groups/print", USER_M3_SLICING, """
            {
              "printGroupId": "%s",
              "printerCode": "PRN-MERGED-PENDING-SLICE",
              "terminalCode": "TS-MERGED-PENDING-SLICE"
            }
            """.formatted(printGroupId)), 200);
        assertThat(printResult.path("merged").asBoolean()).isTrue();
        assertThat(printResult.path("printedSlideCount").asInt()).isEqualTo(2);

        JsonNode workbench = responseBody(mockMvc.perform(authorized(get("/api/v1/slicings/workbench"), USER_M3_SLICING)
                .param("keyword", context.baseContext().pathologyNo())
                .param("pendingPage", "1")
                .param("pendingSize", "20")
                .param("completedPage", "1")
                .param("completedSize", "20")), 200);

        assertThat(workbench.path("pendingPrintTotal").asInt()).isEqualTo(1);
        assertThat(workbench.path("pendingPrintList")).hasSize(1);
        assertThat(workbench.path("pendingPrintList").get(0).path("embeddingBoxNo").asText()).isEqualTo("B1");

        assertThat(workbench.path("pendingSliceTotal").asInt()).isEqualTo(1);
        assertThat(workbench.path("pendingSliceList")).hasSize(1);
        JsonNode mergedPendingSlice = workbench.path("pendingSliceList").get(0);
        assertThat(mergedPendingSlice.path("printGroupId").asText()).isEqualTo(printGroupId);
        assertThat(mergedPendingSlice.path("mergedPrintGroup").asBoolean()).isTrue();
        assertThat(mergedPendingSlice.path("combinedSlide").asBoolean()).isFalse();
        assertThat(mergedPendingSlice.path("slidePrintStatus").asText()).isEqualTo("PRINTED");
        assertThat(mergedPendingSlice.path("printedSlideCount").asInt()).isEqualTo(2);
        assertThat(mergedPendingSlice.path("embeddingBoxNo").asText()).isEqualTo("A1+A2");
        assertThat(mergedPendingSlice.path("taskIds"))
            .extracting(JsonNode::asText)
            .containsExactly(
                context.taskIdsByBoxNo().get("A1"),
                context.taskIdsByBoxNo().get("A2"));
        assertThat(mergedPendingSlice.path("embeddingBoxIds"))
            .extracting(JsonNode::asText)
            .containsExactly(
                context.embeddingBoxIdsByBoxNo().get("A1"),
                context.embeddingBoxIdsByBoxNo().get("A2"));
        assertThat(workbench.path("pendingSliceList"))
            .extracting(item -> item.path("taskId").asText())
            .doesNotContain(context.taskIdsByBoxNo().get("A1"), context.taskIdsByBoxNo().get("A2"));
    }

    @Test
    void shouldRejectPendingSlideMergeWhenNoSamePrefixPairExists() throws Exception {
        MergeReadyContext context = prepareSlicingMergeReadyContext(
            "APP-M3-MERGE-002",
            "BC-M3-MERGE-002",
            List.of("A1", "B1"));

        postJson("/api/v1/slicings/slide-print-merge-groups", USER_M3_SLICING, """
            {
              "taskIds": [%s],
              "terminalCode": "TS-MERGE-GROUP"
            }
            """.formatted(quotedJsonArray(context.taskIdsByBoxNo().values().stream().toList())))
            .andExpect(status().isConflict());
    }

    @Test
    void shouldFilterSlicingWorkbenchBySelectedWorkDate() throws Exception {
        SlicingReadyContext pendingTodayContext = prepareSlicingReadyContext("APP-M3-SLICE-DATE-001", "BC-M3-SLICE-DATE-001");
        SlicingReadyContext pendingYesterdayContext = prepareSlicingReadyContext("APP-M3-SLICE-DATE-002", "BC-M3-SLICE-DATE-002");
        SlicingReadyContext completedYesterdayContext = prepareSlicingReadyContext("APP-M3-SLICE-DATE-003", "BC-M3-SLICE-DATE-003");
        String completedSlideId = completeSlicingCase(completedYesterdayContext);
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        LocalDateTime yesterdayTime = yesterday.atTime(10, 0);
        namedParameterJdbcTemplate.update("""
            update technical_pending_tasks
            set created_at = :createdAt
            where id = :taskId
            """, new MapSqlParameterSource()
            .addValue("createdAt", yesterdayTime)
            .addValue("taskId", pendingYesterdayContext.slicingTaskId()));
        namedParameterJdbcTemplate.update("""
            update technical_pending_tasks
            set completed_at = :completedAt
            where id = :taskId
            """, new MapSqlParameterSource()
            .addValue("completedAt", yesterdayTime.plusHours(2))
            .addValue("taskId", completedYesterdayContext.slicingTaskId()));

        mockMvc.perform(authorized(get("/api/v1/slicings/workbench"), USER_M3_SLICING)
                .param("keyword", pendingTodayContext.baseContext().pathologyNo())
                .param("workDate", today.toString())
                .param("pendingPage", "1")
                .param("pendingSize", "20")
                .param("completedPage", "1")
                .param("completedSize", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.stats.pendingTodayCount").value(1))
            .andExpect(jsonPath("$.data.pendingPrintTotal").value(1))
            .andExpect(jsonPath("$.data.pendingPrintList[0].taskId").value(pendingTodayContext.slicingTaskId()))
            .andExpect(jsonPath("$.data.completedTotal").value(0));

        mockMvc.perform(authorized(get("/api/v1/slicings/workbench"), USER_M3_SLICING)
                .param("keyword", pendingYesterdayContext.baseContext().pathologyNo())
                .param("workDate", yesterday.toString())
                .param("pendingPage", "1")
                .param("pendingSize", "20")
                .param("completedPage", "1")
                .param("completedSize", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.stats.pendingTodayCount").value(1))
            .andExpect(jsonPath("$.data.pendingPrintTotal").value(1))
            .andExpect(jsonPath("$.data.pendingPrintList[0].taskId").value(pendingYesterdayContext.slicingTaskId()))
            .andExpect(jsonPath("$.data.completedTotal").value(0));

        mockMvc.perform(authorized(get("/api/v1/slicings/workbench"), USER_M3_SLICING)
                .param("keyword", completedYesterdayContext.baseContext().pathologyNo())
                .param("workDate", yesterday.toString())
                .param("pendingPage", "1")
                .param("pendingSize", "20")
                .param("completedPage", "1")
                .param("completedSize", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pendingPrintTotal").value(0))
            .andExpect(jsonPath("$.data.completedTotal").value(1))
            .andExpect(jsonPath("$.data.completedTodayList[0].slideId").value(completedSlideId));
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
              "terminalCode": "TE-%s",
              "remarks": "embedding-%s"
            }
            """.formatted(
                embeddingTaskId,
                samplingBlockId,
                applicationNo,
                applicationNo.substring(applicationNo.length() - 3),
                applicationNo)), 200);
        String embeddingBoxId = embedding.path("embeddingBoxId").asText();

        String slicingTaskId = listPendingTasks("SLICING", context.pathologyNo(), USER_M3_SLICING)
            .path("items").get(0).path("id").asText();
        return new SlicingReadyContext(context, embeddingBoxId, slicingTaskId);
    }

    private MergeReadyContext prepareSlicingMergeReadyContext(
        String applicationNo,
        String barcode,
        List<String> embeddingBoxNos
    ) throws Exception {
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
                  "blocks": [%s]
                }
              ]
            }
            """.formatted(
                context.grossingTaskId(),
                context.caseId(),
                applicationNo.substring(applicationNo.length() - 3),
                context.specimenId(),
                applicationNo,
                buildGrossingBlocksJson(embeddingBoxNos)))
            .andExpect(status().isOk());

        JsonNode dehydrationTasks = listPendingTasks("DEHYDRATION", context.pathologyNo(), USER_M3_DEHYDRATION)
            .path("items");
        assertThat(dehydrationTasks).hasSize(embeddingBoxNos.size());
        Map<String, String> boxNoBySamplingBlockId = new LinkedHashMap<>();
        for (int index = 0; index < dehydrationTasks.size(); index++) {
            boxNoBySamplingBlockId.put(
                dehydrationTasks.get(index).path("objectId").asText(),
                embeddingBoxNos.get(index));
        }

        JsonNode dehydrationBatch = responseBody(postJson("/api/v1/dehydration-batches", USER_M3_DEHYDRATION, """
            {
              "caseId": "%s",
              "basketNo": "BASKET-%s",
              "samplingBlockIds": [%s]
            }
            """.formatted(
                context.caseId(),
                applicationNo,
                quotedJsonArray(boxNoBySamplingBlockId.keySet().stream().toList()))), 201);
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

        JsonNode embeddingTasks = listPendingTasks("EMBEDDING", context.pathologyNo(), USER_M3_EMBEDDING)
            .path("items");
        assertThat(embeddingTasks).hasSize(embeddingBoxNos.size());
        Map<String, String> desiredBoxNoByEmbeddingBoxId = new LinkedHashMap<>();
        for (int index = 0; index < embeddingTasks.size(); index++) {
            JsonNode task = embeddingTasks.get(index);
            String embeddingTaskId = task.path("id").asText();
            String samplingBlockId = task.path("objectId").asText();
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
                  "terminalCode": "TE-%s",
                  "remarks": "embedding-%s"
                }
                """.formatted(
                    embeddingTaskId,
                    samplingBlockId,
                    boxNoBySamplingBlockId.get(samplingBlockId),
                    applicationNo.substring(applicationNo.length() - 3),
                    applicationNo)), 200);
            desiredBoxNoByEmbeddingBoxId.put(
                embedding.path("embeddingBoxId").asText(),
                boxNoBySamplingBlockId.get(samplingBlockId));
        }

        int sequence = 0;
        for (String embeddingBoxId : desiredBoxNoByEmbeddingBoxId.keySet()) {
            namedParameterJdbcTemplate.update("""
                update embedding_boxes
                set embedding_box_no = :embeddingBoxNo,
                    updated_at = :updatedAt
                where id = :embeddingBoxId
                """, new MapSqlParameterSource()
                .addValue("embeddingBoxId", embeddingBoxId)
                .addValue("embeddingBoxNo", "TMP-" + applicationNo + "-" + sequence++)
                .addValue("updatedAt", LocalDateTime.now()));
        }
        for (Map.Entry<String, String> entry : desiredBoxNoByEmbeddingBoxId.entrySet()) {
            namedParameterJdbcTemplate.update("""
                update embedding_boxes
                set embedding_box_no = :embeddingBoxNo,
                    updated_at = :updatedAt
                where id = :embeddingBoxId
                """, new MapSqlParameterSource()
                .addValue("embeddingBoxId", entry.getKey())
                .addValue("embeddingBoxNo", entry.getValue())
                .addValue("updatedAt", LocalDateTime.now()));
        }

        Map<String, String> taskIdsByBoxNo = new LinkedHashMap<>();
        Map<String, String> embeddingBoxIdsByBoxNo = new LinkedHashMap<>();
        namedParameterJdbcTemplate.query("""
            select t.id as task_id,
                   eb.id as embedding_box_id,
                   eb.embedding_box_no
            from technical_pending_tasks t
            join embedding_boxes eb on eb.id = t.object_id
            where t.case_id = :caseId
              and t.task_type = 'SLICING'
            order by eb.embedding_box_no asc
            """, new MapSqlParameterSource().addValue("caseId", context.caseId()), rs -> {
            taskIdsByBoxNo.put(rs.getString("embedding_box_no"), rs.getString("task_id"));
            embeddingBoxIdsByBoxNo.put(rs.getString("embedding_box_no"), rs.getString("embedding_box_id"));
        });
        assertThat(taskIdsByBoxNo).containsOnlyKeys(embeddingBoxNos.toArray(String[]::new));
        return new MergeReadyContext(context, taskIdsByBoxNo, embeddingBoxIdsByBoxNo);
    }

    private String buildGrossingBlocksJson(List<String> embeddingBoxNos) {
        return embeddingBoxNos.stream()
            .map(boxNo -> """
                {
                  "blockSite": "%s",
                  "blockDescription": "block-%s"
                }
                """.formatted(embeddingBoxPrefix(boxNo), boxNo))
            .collect(Collectors.joining(","));
    }

    private String embeddingBoxPrefix(String embeddingBoxNo) {
        int index = 0;
        while (index < embeddingBoxNo.length() && Character.isLetter(embeddingBoxNo.charAt(index))) {
            index++;
        }
        return index == 0 ? embeddingBoxNo : embeddingBoxNo.substring(0, index);
    }

    private String quotedJsonArray(List<String> values) {
        return values.stream()
            .map(value -> "\"" + value + "\"")
            .collect(Collectors.joining(","));
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

    private record MergeReadyContext(
        TechnicalCaseContext baseContext,
        Map<String, String> taskIdsByBoxNo,
        Map<String, String> embeddingBoxIdsByBoxNo
    ) {
    }
}
