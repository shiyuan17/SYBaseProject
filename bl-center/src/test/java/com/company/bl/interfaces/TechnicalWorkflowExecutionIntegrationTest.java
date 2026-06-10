package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class TechnicalWorkflowExecutionIntegrationTest extends AbstractTechnicalWorkflowIntegrationTest {

    @Test
    void shouldExposeGrossingWorkbenchContextFromAggregatedSources() throws Exception {
        TechnicalCaseContext registrationContext =
            receiveCaseAndGetPendingRegistration("APP-M3-CTX-001", "BC-M3-CTX-001");

        saveTechnicalSpecimenRegistrationApplicationWorkbenchPatientInfo(registrationContext.caseId(), USER_RECEIVE, """
            {
              "contagiousSpecimen": {
                "hepatitis": false,
                "hiv": false,
                "isolation": true,
                "syphilis": false,
                "tuberculosis": false
              },
              "gynecologyInfo": {
                "additionalNotes": "既往曾行穿刺",
                "hpvResult": "",
                "lastMenstrualPeriod": "",
                "menopause": false,
                "previousCytology": "",
                "previousTreatment": "",
                "specialConditions": {
                  "abnormalBleeding": false,
                  "birthControl": false,
                  "hormoneReplacement": false,
                  "hysterectomy": false,
                  "iud": false,
                  "lactation": false,
                  "menopause": false,
                  "other": "",
                  "pregnancy": false,
                  "radiotherapy": false
                }
              },
              "patientInfo": {
                "age": "35岁",
                "applicationDate": "2026-05-27",
                "applicationNo": "",
                "applyDept": "OR",
                "applyDoctor": "Dr A",
                "bedNo": "16床",
                "checkItem": "术中病理；免疫组化复核",
                "clinicalDiagnosis": "Papillary thyroid carcinoma",
                "clinicalHistory": "甲状腺结节病史，近一个月增大",
                "deliveryRequirement": "立即送检",
                "endoscopyDiagnosis": "",
                "frozenReminder": false,
                "gender": "女",
                "idNo": "320101199001011234",
                "imagingResult": "超声提示甲状腺左叶低回声结节",
                "inpatientNo": "ZY-GROSSING-CTX-001",
                "patientName": "Patient A",
                "patientVerified": true,
                "phone": "13800001111",
                "registrationStatus": "登记",
                "remark": "取材工作台上下文测试",
                "specimenType": "ROUTINE",
                "wardName": "外科病区"
              },
              "surgeryInfo": {
                "buildingId": "B001",
                "clinicalFindings": "术中见甲状腺左叶结节样病灶",
                "fixativeType": "福尔马林",
                "fixationPerson": "护士甲",
                "fixationTime": "2026-05-27T10:15:00",
                "roomId": "OR-101",
                "specimenRemovalTime": "2026-05-27T10:00:00",
                "surgeryName": "甲状腺左叶切除术"
              }
            }
            """);

        JsonNode completionResult =
            completeTechnicalSpecimenRegistration(registrationContext.caseId(), "context ready");
        String pathologyNo = completionResult.path("pathologyNo").asText();
        String grossingTaskId = listPendingTasks("GROSSING", pathologyNo, USER_M3_GROSSING)
            .path("items")
            .get(0)
            .path("id")
            .asText();

        TechnicalCaseContext context = new TechnicalCaseContext(
            registrationContext.applicationId(),
            registrationContext.applicationNo(),
            registrationContext.caseId(),
            pathologyNo,
            registrationContext.specimenId(),
            registrationContext.barcode(),
            grossingTaskId);

        namedParameterJdbcTemplate.update("""
            insert into case_media_assets
                (id, case_id, specimen_id, object_type, object_id, media_type, file_url, file_name,
                 captured_at, captured_by_user_id, captured_by_name, remarks, created_at, updated_at)
            values
                (:id, :caseId, :specimenId, :objectType, :objectId, :mediaType, :fileUrl, :fileName,
                 :capturedAt, :capturedByUserId, :capturedByName, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", "MED-GROSSING-CTX-001")
            .addValue("caseId", context.caseId())
            .addValue("specimenId", context.specimenId())
            .addValue("objectType", "SAMPLING")
            .addValue("objectId", "SMP-GROSSING-CTX-001")
            .addValue("mediaType", "GROSS_IMAGE")
            .addValue("fileUrl", "http://example.com/grossing-context-1.jpg")
            .addValue("fileName", "grossing-context-1.jpg")
            .addValue("capturedAt", java.time.LocalDateTime.parse("2026-05-27T10:20:00"))
            .addValue("capturedByUserId", USER_M3_GROSSING)
            .addValue("capturedByName", userDisplayName(USER_M3_GROSSING))
            .addValue("remarks", "历史取材影像")
            .addValue("createdAt", java.time.LocalDateTime.parse("2026-05-27T10:20:00"))
            .addValue("updatedAt", java.time.LocalDateTime.parse("2026-05-27T10:20:00")));

        mockMvc.perform(authorized(get("/api/v1/grossings/{taskId}/context", context.grossingTaskId()), USER_M3_GROSSING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.task.taskId").value(context.grossingTaskId()))
            .andExpect(jsonPath("$.data.task.taskStatus").value("PENDING"))
            .andExpect(jsonPath("$.data.caseSummary.caseId").value(context.caseId()))
            .andExpect(jsonPath("$.data.caseSummary.applicationId").value(context.applicationId()))
            .andExpect(jsonPath("$.data.caseSummary.pathologyNo").value(context.pathologyNo()))
            .andExpect(jsonPath("$.data.clinicalDiagnosis").value("Papillary thyroid carcinoma"))
            .andExpect(jsonPath("$.data.clinicalHistory").value("甲状腺结节病史，近一个月增大"))
            .andExpect(jsonPath("$.data.relatedExaminations").value("影像检查: 超声提示甲状腺左叶低回声结节"))
            .andExpect(jsonPath("$.data.contextSummary").value(containsString("术中见甲状腺左叶结节样病灶")))
            .andExpect(jsonPath("$.data.contextSummary").value(containsString("立即送检")))
            .andExpect(jsonPath("$.data.contextSummary").value(containsString("传染信息: 隔离")))
            .andExpect(jsonPath("$.data.checkItems[0].name").value("术中病理"))
            .andExpect(jsonPath("$.data.checkItems[1].name").value("免疫组化复核"))
            .andExpect(jsonPath("$.data.tracking.caseId").value(context.caseId()))
            .andExpect(jsonPath("$.data.tracking.specimens[0].specimenId").value(context.specimenId()))
            .andExpect(jsonPath("$.data.mediaAssets[0].fileName").value("grossing-context-1.jpg"))
            .andExpect(jsonPath("$.data.mediaAssets[0].specimenId").value(context.specimenId()));
    }

    @Test
    void shouldFallbackGrossingWorkbenchContextWhenRegistrationTableIsUnavailable() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-CTX-002", "BC-M3-CTX-002");

        dropTechnicalSpecimenRegistrationsTable();
        try {
            mockMvc.perform(authorized(get("/api/v1/grossings/{taskId}/context", context.grossingTaskId()), USER_M3_GROSSING))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.task.taskId").value(context.grossingTaskId()))
                .andExpect(jsonPath("$.data.caseSummary.caseId").value(context.caseId()))
                .andExpect(jsonPath("$.data.caseSummary.applicationId").value(context.applicationId()))
                .andExpect(jsonPath("$.data.caseSummary.pathologyNo").value(context.pathologyNo()))
                .andExpect(jsonPath("$.data.tracking.caseId").value(context.caseId()))
                .andExpect(jsonPath("$.data.tracking.specimens[0].specimenId").value(context.specimenId()));
        } finally {
            recreateTechnicalSpecimenRegistrationsTable();
        }
    }

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
            .andExpect(jsonPath("$.data.taskStatus").value("EMBEDDING_CONFIRM_PENDING"));

        postJson("/api/v1/embeddings/cancel", USER_M3_EMBEDDING, """
            {
              "taskId": "%s",

              "terminalCode": "TE-01-CANCEL"
            }
            """.formatted(embeddingTaskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.taskStatus").value("PENDING"));

        postJson("/api/v1/embeddings/start", USER_M3_EMBEDDING, """
            {
              "taskId": "%s",

              "terminalCode": "TE-01-RETRY"
            }
            """.formatted(embeddingTaskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.taskStatus").value("EMBEDDING_CONFIRM_PENDING"));

        JsonNode slicingTasksBeforeEmbeddingCompletion =
            listPendingTasks("SLICING", context.pathologyNo(), USER_M3_SLICING);
        assertThat(slicingTasksBeforeEmbeddingCompletion.path("items")).isEmpty();

        JsonNode embedding = responseBody(postJson("/api/v1/embeddings/complete", USER_M3_EMBEDDING, """
            {
              "taskId": "%s",
              "samplingBlockId": "%s",
              "blockCount": 1,
              "sliceNotice": "careful",
              "deviceCode": "FAIL",
              
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

        printSlides(slicingTaskId, embeddingBoxId);

        JsonNode slicing = responseBody(postJson("/api/v1/slicings/complete", USER_M3_SLICING, """
            {
              "taskId": "%s",
              "embeddingBoxId": "%s",
              "slideCount": 1,
              
              "terminalCode": "TS-02"
            }
            """.formatted(slicingTaskId, embeddingBoxId)), 200);
        String slideId = slicing.path("slideIds").get(0).asText();

        JsonNode stainingTasks = listPendingTasks("STAINING", context.pathologyNo(), USER_M3_STAINING);
        JsonNode stainingTask = stainingTasks.path("items").get(0);
        String stainingTaskId = stainingTask.path("id").asText();
        assertThat(stainingTask.path("objectId").asText()).isEqualTo(slideId);
        assertThat(stainingTask.path("objectDisplayNo").asText()).startsWith("BX-");

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

        java.util.List<java.util.Map<String, Object>> samplingBlocks = namedParameterJdbcTemplate.queryForList("""
            select block_code, embedding_box_no, embedding_box_status
            from sampling_blocks
            where case_id = :caseId
            order by sequence_no
            """, java.util.Map.of("caseId", context.caseId()));
        assertThat(samplingBlocks).hasSize(2);
        samplingBlocks.forEach(block -> {
            assertThat(block.get("embedding_box_no")).isEqualTo("BX-" + block.get("block_code"));
            assertThat(block.get("embedding_box_status")).isEqualTo("PENDING");
        });

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
    void shouldPersistGrossingEmbeddingBoxDrafts() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-BOX-001", "BC-M3-BOX-001");

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "terminalCode": "TG-BOX-01"
            }
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk());

        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "caseId": "%s",
              "terminalCode": "TG-BOX-02",
              "specimens": [
                {
                  "specimenId": "%s",
                  "specimenType": "ROUTINE",
                  "grossDescription": "grossing with cassette plan",
                  "blocks": [
                    {
                      "blockSite": "skin",
                      "blockDescription": "box-a",
                      "specialRequirement": "none"
                    }
                  ],
                  "embeddingBoxes": [
                    {
                      "sequenceNo": 1,
                      "boxName": "包埋盒 1",
                      "embeddingBoxNo": "A1",
                      "status": "CONFIRMED",
                      "embeddingRemarks": "皮肤组织"
                    }
                  ]
                }
              ]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.createdDehydrationTaskCount").value(1));

        java.util.Map<String, Object> samplingBlock = namedParameterJdbcTemplate.queryForMap("""
            select embedding_box_no, embedding_box_name, embedding_box_status, embedding_remarks
            from sampling_blocks
            where case_id = :caseId
            order by sequence_no
            limit 1
            """, java.util.Map.of("caseId", context.caseId()));
        assertThat(samplingBlock.get("embedding_box_no")).isEqualTo("A1");
        assertThat(samplingBlock.get("embedding_box_name")).isEqualTo("包埋盒 1");
        assertThat(samplingBlock.get("embedding_box_status")).isEqualTo("CONFIRMED");
        assertThat(samplingBlock.get("embedding_remarks")).isEqualTo("皮肤组织");
    }

    @Test
    void shouldRejectDuplicateGrossingEmbeddingBoxNumbers() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-BOX-002", "BC-M3-BOX-002");

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "terminalCode": "TG-BOX-03"
            }
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk());

        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "caseId": "%s",
              "terminalCode": "TG-BOX-04",
              "specimens": [
                {
                  "specimenId": "%s",
                  "specimenType": "ROUTINE",
                  "grossDescription": "duplicate cassette",
                  "blocks": [
                    { "blockSite": "A", "blockDescription": "block-a" },
                    { "blockSite": "B", "blockDescription": "block-b" }
                  ],
                  "embeddingBoxes": [
                    {
                      "sequenceNo": 1,
                      "boxName": "包埋盒 1",
                      "embeddingBoxNo": "A1",
                      "status": "CONFIRMED"
                    },
                    {
                      "sequenceNo": 2,
                      "boxName": "包埋盒 2",
                      "embeddingBoxNo": "A1",
                      "status": "CONFIRMED"
                    }
                  ]
                }
              ]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowSameEmbeddingBoxNoAcrossDifferentPathologyCases() throws Exception {
        String embeddingBoxNo = "A1";
        TechnicalCaseContext firstContext =
            completeGrossingWithEmbeddingBoxNo("APP-M3-BOX-LEGACY-001", "BC-M3-BOX-LEGACY-001", embeddingBoxNo);
        String firstSamplingBlockId = completeFirstDehydrationTask(firstContext);
        completeFirstEmbeddingTask(firstContext, firstSamplingBlockId);

        TechnicalCaseContext secondContext =
            completeGrossingWithEmbeddingBoxNo("APP-M3-BOX-LEGACY-002", "BC-M3-BOX-LEGACY-002", embeddingBoxNo);
        String secondSamplingBlockId = completeFirstDehydrationTask(secondContext);
        completeFirstEmbeddingTask(secondContext, secondSamplingBlockId);

        java.util.List<java.util.Map<String, Object>> embeddingBoxRows = namedParameterJdbcTemplate.queryForList("""
            select case_id, embedding_box_no
            from embedding_boxes
            where case_id in (:caseIds)
            order by case_id
            """, java.util.Map.of("caseIds", java.util.List.of(firstContext.caseId(), secondContext.caseId())));
        assertThat(embeddingBoxRows).hasSize(2);
        assertThat(embeddingBoxRows)
            .extracting(row -> row.get("embedding_box_no"))
            .containsExactly(embeddingBoxNo, embeddingBoxNo);
    }

    @Test
    void shouldStartAndCompleteDehydrationTaskWithoutBatch() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M3-TASK-DEHY-001", "BC-M3-TASK-DEHY-001");

        postJson("/api/v1/dehydrations/start", USER_M3_DEHYDRATION, """
            {
              "taskId": "%s"
            }
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isBadRequest());

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "terminalCode": "TG-DHY-01"
            }
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk());

        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "caseId": "%s",
              "terminalCode": "TG-DHY-02",
              "specimens": [
                {
                  "specimenId": "%s",
                  "specimenType": "ROUTINE",
                  "grossDescription": "task-level dehydration",
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
            .andExpect(status().isOk());

        JsonNode dehydrationTask = listPendingTasks("DEHYDRATION", context.pathologyNo(), USER_M3_DEHYDRATION)
            .path("items")
            .get(0);
        String dehydrationTaskId = dehydrationTask.path("id").asText();
        String samplingBlockId = dehydrationTask.path("objectId").asText();

        postJson("/api/v1/dehydrations/complete", USER_M3_DEHYDRATION, """
            {
              "taskId": "%s"
            }
            """.formatted(dehydrationTaskId))
            .andExpect(status().isConflict());

        postJson("/api/v1/dehydrations/start", USER_M3_DEHYDRATION, """
            {
              "taskId": "%s",
              "terminalCode": "TD-DIRECT-01"
            }
            """.formatted(dehydrationTaskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.taskId").value(dehydrationTaskId))
            .andExpect(jsonPath("$.data.caseId").value(context.caseId()))
            .andExpect(jsonPath("$.data.caseStatus").value("DEHYDRATION"))
            .andExpect(jsonPath("$.data.taskStatus").value("IN_PROGRESS"));

        JsonNode startedTask = listPendingTasks("DEHYDRATION", context.pathologyNo(), USER_M3_DEHYDRATION)
            .path("items")
            .get(0);
        assertThat(startedTask.path("startedAt").asText()).isNotBlank();
        assertThat(startedTask.path("assignedToUserId").asText()).isEqualTo(USER_M3_DEHYDRATION);
        assertThat(startedTask.path("assignedToName").asText()).isNotBlank();

        postJson("/api/v1/dehydrations/start", USER_M3_DEHYDRATION, """
            {
              "taskId": "%s"
            }
            """.formatted(dehydrationTaskId))
            .andExpect(status().isConflict());

        postJson("/api/v1/dehydrations/complete", USER_M3_DEHYDRATION, """
            {
              "taskId": "%s",
              "terminalCode": "TD-DIRECT-02"
            }
            """.formatted(dehydrationTaskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.taskId").value(dehydrationTaskId))
            .andExpect(jsonPath("$.data.caseStatus").value("DEHYDRATION"))
            .andExpect(jsonPath("$.data.taskStatus").value("COMPLETED"));

        java.util.Map<String, Object> completedTask = namedParameterJdbcTemplate.queryForMap("""
            select task_status, assigned_to_user_id, assigned_to_name, started_at, completed_at
            from technical_pending_tasks
            where id = :taskId
            """, java.util.Map.of("taskId", dehydrationTaskId));
        assertThat(completedTask.get("task_status")).isEqualTo("COMPLETED");
        assertThat(completedTask.get("assigned_to_user_id")).isEqualTo(USER_M3_DEHYDRATION);
        assertThat(completedTask.get("assigned_to_name")).isNotNull();
        assertThat(completedTask.get("started_at")).isNotNull();
        assertThat(completedTask.get("completed_at")).isNotNull();

        postJson("/api/v1/dehydrations/start", USER_M3_DEHYDRATION, """
            {
              "taskId": "%s"
            }
            """.formatted(dehydrationTaskId))
            .andExpect(status().isConflict());

        JsonNode embeddingTasks = listPendingTasks("EMBEDDING", context.pathologyNo(), USER_M3_EMBEDDING);
        assertThat(embeddingTasks.path("items")).hasSize(1);
        assertThat(embeddingTasks.path("items").get(0).path("objectId").asText()).isEqualTo(samplingBlockId);

        JsonNode tracking = technicalTracking(context.caseId(), USER_M3_TRACKING);
        assertThat(tracking.path("events").toString()).contains("Dehydration started");
        assertThat(tracking.path("events").toString()).contains("Dehydration completed");
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

    private void dropTechnicalSpecimenRegistrationsTable() {
        namedParameterJdbcTemplate.getJdbcOperations().execute("drop table technical_specimen_registrations");
    }

    private TechnicalCaseContext completeGrossingWithEmbeddingBoxNo(String applicationNo,
                                                                    String barcode,
                                                                    String embeddingBoxNo) throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask(applicationNo, barcode);
        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "terminalCode": "TG-BOX-LEGACY-01"
            }
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk());

        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "caseId": "%s",
              "terminalCode": "TG-BOX-LEGACY-02",
              "specimens": [
                {
                  "specimenId": "%s",
                  "specimenType": "ROUTINE",
                  "grossDescription": "legacy box no",
                  "blocks": [
                    {
                      "blockSite": "A",
                      "blockDescription": "legacy block"
                    }
                  ],
                  "embeddingBoxes": [
                    {
                      "sequenceNo": 1,
                      "boxName": "包埋盒 1",
                      "embeddingBoxNo": "%s",
                      "status": "CONFIRMED"
                    }
                  ]
                }
              ]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId(), embeddingBoxNo))
            .andExpect(status().isOk());
        return context;
    }

    private TechnicalCaseContext completeGrossingWithDefaultEmbeddingBoxNo(String applicationNo,
                                                                           String barcode) throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask(applicationNo, barcode);
        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "terminalCode": "TG-BOX-DEFAULT-01"
            }
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk());

        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "caseId": "%s",
              "terminalCode": "TG-BOX-DEFAULT-02",
              "specimens": [
                {
                  "specimenId": "%s",
                  "specimenType": "ROUTINE",
                  "grossDescription": "default box no",
                  "blocks": [
                    {
                      "blockSite": "A",
                      "blockDescription": "default block"
                    }
                  ]
                }
              ]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId()))
            .andExpect(status().isOk());
        return context;
    }

    private String completeFirstDehydrationTask(TechnicalCaseContext context) throws Exception {
        JsonNode dehydrationTask = listPendingTasks("DEHYDRATION", context.pathologyNo(), USER_M3_DEHYDRATION)
            .path("items")
            .get(0);
        String dehydrationTaskId = dehydrationTask.path("id").asText();
        String samplingBlockId = dehydrationTask.path("objectId").asText();

        postJson("/api/v1/dehydrations/start", USER_M3_DEHYDRATION, """
            {
              "taskId": "%s"
            }
            """.formatted(dehydrationTaskId))
            .andExpect(status().isOk());
        postJson("/api/v1/dehydrations/complete", USER_M3_DEHYDRATION, """
            {
              "taskId": "%s"
            }
            """.formatted(dehydrationTaskId))
            .andExpect(status().isOk());
        return samplingBlockId;
    }

    private void completeFirstEmbeddingTask(TechnicalCaseContext context,
                                            String samplingBlockId) throws Exception {
        String embeddingTaskId = listPendingTasks("EMBEDDING", context.pathologyNo(), USER_M3_EMBEDDING)
            .path("items")
            .get(0)
            .path("id")
            .asText();

        postJson("/api/v1/embeddings/start", USER_M3_EMBEDDING, """
            {
              "taskId": "%s"
            }
            """.formatted(embeddingTaskId))
            .andExpect(status().isOk());
        postJson("/api/v1/embeddings/complete", USER_M3_EMBEDDING, """
            {
              "taskId": "%s",
              "samplingBlockId": "%s",
              "blockCount": 1
            }
            """.formatted(embeddingTaskId, samplingBlockId))
            .andExpect(status().isOk());
    }

    private void printSlides(String slicingTaskId, String embeddingBoxId) throws Exception {
        postJson("/api/v1/slicings/slide-print", USER_M3_SLICING, """
            {
              "taskId": "%s",
              "embeddingBoxId": "%s",
              "sourceSlideCount": 1,
              "requestedSlideCount": 1
            }
            """.formatted(slicingTaskId, embeddingBoxId))
            .andExpect(status().isOk());
    }

    private void recreateTechnicalSpecimenRegistrationsTable() {
        namedParameterJdbcTemplate.getJdbcOperations().execute("""
            create table technical_specimen_registrations (
                case_id varchar(64) not null,
                application_id varchar(64) not null,
                registration_status varchar(32) not null,
                registered_by_user_id varchar(64),
                registered_by_name varchar(128),
                registered_at timestamp,
                remarks varchar(500),
                created_at timestamp not null,
                updated_at timestamp not null,
                constraint pk_technical_specimen_registrations primary key (case_id),
                constraint fk_technical_specimen_reg_case foreign key (case_id) references pathology_cases (id)
            )
            """);
        namedParameterJdbcTemplate.getJdbcOperations().execute("""
            create index idx_tech_spec_reg_status_created
                on technical_specimen_registrations (registration_status, created_at)
            """);
        namedParameterJdbcTemplate.getJdbcOperations().execute("""
            create index idx_tech_spec_reg_application
                on technical_specimen_registrations (application_id)
            """);
    }
}
