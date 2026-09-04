package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class DiagnosticWorkflowReportLifecycleIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    @Test
    void shouldCompleteReportLifecycleAndIssueAsWorkbenchOnlyOperator() throws Exception {
        String suffix = uniqueSuffix();
        StartedDiagnosticContext context = prepareStartedDiagnosticCase(
            "APP-M4-WB-" + suffix,
            "BC-M4-WB-" + suffix);
        String workbenchUserId = createWorkbenchOnlyUser(suffix);

        JsonNode pending = listPendingDiagnosticTasks(context.pathologyNo(), workbenchUserId);
        assertThat(pending.path("items")).isEmpty();

        String reportId = responseBody(postJson("/api/v1/pathology-reports", workbenchUserId, """
            {
              "caseId":"%s",
              "taskId":"%s",
              "clinicalDiagnosis":"workbench clinical",
              "grossExam":"workbench gross",
              "microscopicExam":"workbench microscopic",
              "finalDiagnosis":"workbench final",
              "richTextContent":"<p>workbench report</p>"
            }
            """.formatted(context.caseId(), context.diagnosticTaskId())), 200).path("reportId").asText();

        postJson("/api/v1/pathology-reports/%s/save-draft".formatted(reportId), workbenchUserId, """
            {
              "clinicalDiagnosis":"workbench clinical saved",
              "grossExam":"workbench gross saved",
              "microscopicExam":"workbench microscopic saved",
              "finalDiagnosis":"workbench final saved",
              "richTextContent":"<p>workbench report saved</p>"
            }
            """).andExpect(status().isOk());
        postJson("/api/v1/pathology-reports/%s/submit".formatted(reportId), workbenchUserId, "{}")
            .andExpect(status().isOk());
        postJson("/api/v1/pathology-reports/%s/review".formatted(reportId), workbenchUserId, "{}")
            .andExpect(status().isOk());
        postJson("/api/v1/pathology-reports/%s/sign".formatted(reportId), workbenchUserId, "{}")
            .andExpect(status().isOk());

        JsonNode versions = caseReportVersions(context.caseId(), workbenchUserId);
        String versionId = versions.get(0).path("versionId").asText();
        postJson("/api/v1/pathology-reports/formal-versions/issue", workbenchUserId, """
            {"versionIds":["%s"],"issueMode":"IMMEDIATE"}
            """.formatted(versionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount").value(1));

        JsonNode issuedVersions = caseReportVersions(context.caseId(), workbenchUserId);
        assertThat(issuedVersions.get(0).path("printStatus").asText()).isEqualTo("UNPRINTED");
        assertThat(issuedVersions.get(0).path("printedAt").isNull()).isTrue();
        assertThat(issuedVersions.get(0).path("deliveryStatus").asText()).isEqualTo("ISSUED");
        Long reportPrintEventCount = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from workflow_events
            where case_id = :caseId
              and node_code = 'REPORT_PRINT'
            """, Map.of("caseId", context.caseId()), Long.class);
        assertThat(reportPrintEventCount).isZero();
        List<String> operators = namedParameterJdbcTemplate.queryForList("""
            select distinct operator_user_id
            from workflow_events
            where case_id = :caseId
              and node_code in ('REPORT_DRAFT', 'REPORT_SUBMIT', 'REPORT_REVIEW',
                                'REPORT_SIGN', 'REPORT_PRINT', 'REPORT_ISSUE')
            """, Map.of("caseId", context.caseId()), String.class);
        assertThat(operators).containsOnly(workbenchUserId);
    }

    @Test
    void shouldCreateDiagnosticTaskAfterStainingAndCompleteMinimalReportClosure() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M4-001", "BC-M4-001");

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              
              "terminalCode": "M4-G-01"
            }
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk());

        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "caseId": "%s",
              
              "terminalCode": "M4-G-02",
              "specimens": [
                {
                  "specimenId": "%s",
                  "specimenType": "ROUTINE",
                  "grossDescription": "m4 grossing",
                  "blocks": [
                    {
                      "blockSite": "lung",
                      "blockDescription": "block-1"
                    }
                  ]
                }
              ]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId()))
            .andExpect(status().isOk());

        JsonNode dehydrationTasks = listPendingTasks("DEHYDRATION", context.pathologyNo(), USER_M3_DEHYDRATION);
        String samplingBlockId = dehydrationTasks.path("items").get(0).path("objectId").asText();

        JsonNode batch = responseBody(postJson("/api/v1/dehydration-batches", USER_M3_DEHYDRATION, """
            {
              "caseId": "%s",
              "basketNo": "M4-BASKET-1",
              "deviceNo": "M4-DEV-1",
              
              "terminalCode": "M4-D-01",
              "samplingBlockIds": ["%s"]
            }
            """.formatted(context.caseId(), samplingBlockId)), 201);
        String batchId = batch.path("batchId").asText();

        postJson("/api/v1/dehydration-batches/%s/start".formatted(batchId), USER_M3_DEHYDRATION, """
            {
              
              "terminalCode": "M4-D-02"
            }
            """)
            .andExpect(status().isOk());

        postJson("/api/v1/dehydration-batches/%s/complete".formatted(batchId), USER_M3_DEHYDRATION, """
            {
              
              "terminalCode": "M4-D-03"
            }
            """)
            .andExpect(status().isOk());

        JsonNode embeddingTasks = listPendingTasks("EMBEDDING", context.pathologyNo(), USER_M3_EMBEDDING);
        String embeddingTaskId = embeddingTasks.path("items").get(0).path("id").asText();

        postJson("/api/v1/embeddings/start", USER_M3_EMBEDDING, """
            {
              "taskId": "%s",
              
              "terminalCode": "M4-E-01"
            }
            """.formatted(embeddingTaskId))
            .andExpect(status().isOk());

        JsonNode embedding = responseBody(postJson("/api/v1/embeddings/complete", USER_M3_EMBEDDING, """
            {
              "taskId": "%s",
              "samplingBlockId": "%s",
              "blockCount": 1,
              "sliceNotice": "notice",
              
              "terminalCode": "M4-E-02"
            }
            """.formatted(embeddingTaskId, samplingBlockId)), 200);
        String embeddingBoxId = embedding.path("embeddingBoxId").asText();

        JsonNode slicingTasks = listPendingTasks("SLICING", context.pathologyNo(), USER_M3_SLICING);
        String slicingTaskId = slicingTasks.path("items").get(0).path("id").asText();

        postJson("/api/v1/slicings/start", USER_M3_SLICING, """
            {
              "taskId": "%s",
              
              "terminalCode": "M4-S-01"
            }
            """.formatted(slicingTaskId))
            .andExpect(status().isOk());

        postJson("/api/v1/slicings/slide-print", USER_M3_SLICING, """
            {
              "taskId": "%s",
              "embeddingBoxId": "%s",
              "sourceSlideCount": 1,
              "terminalCode": "M4-S-PRINT"
            }
            """.formatted(slicingTaskId, embeddingBoxId)).andExpect(status().isOk());

        JsonNode slicing = responseBody(postJson("/api/v1/slicings/complete", USER_M3_SLICING, """
            {
              "taskId": "%s",
              "embeddingBoxId": "%s",
              "slideCount": 1,
              
              "terminalCode": "M4-S-02"
            }
            """.formatted(slicingTaskId, embeddingBoxId)), 200);
        String slideId = slicing.path("slideIds").get(0).asText();

        JsonNode stainingTasks = listPendingTasks("STAINING", context.pathologyNo(), USER_M3_STAINING);
        String stainingTaskId = stainingTasks.path("items").get(0).path("id").asText();

        postJson("/api/v1/slide-stainings/start", USER_M3_STAINING, """
            {
              "taskId": "%s",
              
              "terminalCode": "M4-T-01"
            }
            """.formatted(stainingTaskId))
            .andExpect(status().isOk());

        postJson("/api/v1/slide-stainings/complete", USER_M3_STAINING, """
            {
              "taskId": "%s",
              "slideId": "%s",
              "stainingType": "HE",
              
              "terminalCode": "M4-T-02"
            }
            """.formatted(stainingTaskId, slideId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.caseStatus").value("DIAGNOSIS_PENDING"));

        JsonNode diagnosticTasks = listPendingDiagnosticTasks(context.pathologyNo(), USER_M4_ASSIGN);
        assertThat(diagnosticTasks.path("total").asInt()).isEqualTo(1);
        String diagnosticTaskId = diagnosticTasks.path("items").get(0).path("id").asText();

        postJson("/api/v1/diagnostic-tasks/%s/assign".formatted(diagnosticTaskId), USER_M4_ASSIGN, """
            {
              "diagnosisDoctorUserId": "USER_M4_DIAGNOSIS",
              "diagnosisDoctorName": "M4 Diagnosis",
              "primaryDoctorUserId": "USER_M4_DIAGNOSIS",
              "primaryDoctorName": "M4 Diagnosis",
              "reviewerUserId": "USER_M4_REVIEW",
              "reviewerName": "M4 Review",
              
              "terminalCode": "M4-A-01"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.taskStatus").value("ASSIGNED"));

        postJson("/api/v1/diagnostic-tasks/%s/accept".formatted(diagnosticTaskId), USER_M4_DIAGNOSIS, """
            {
              
              "terminalCode": "M4-A-02"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.taskStatus").value("ACCEPTED"));

        postJson("/api/v1/diagnostic-tasks/%s/start".formatted(diagnosticTaskId), USER_M4_DIAGNOSIS, """
            {
              
              "terminalCode": "M4-A-03"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.caseStatus").value("DIAGNOSING"));

        JsonNode createdReport = responseBody(postJson("/api/v1/pathology-reports", USER_M4_DIAGNOSIS, """
            {
              "caseId": "%s",
              "taskId": "%s",
              "clinicalDiagnosis": "clinical a",
              "grossExam": "gross a",
              "microscopicExam": "micro a",
              "finalDiagnosis": "final a",
              "richTextContent": "<p>report a</p>",
              
              "terminalCode": "M4-R-01"
            }
            """.formatted(context.caseId(), diagnosticTaskId)), 200);
        String reportId = createdReport.path("reportId").asText();
        String reportNo = createdReport.path("reportNo").asText();
        assertThat(reportNo).startsWith("RP");

        postJson("/api/v1/pathology-reports/%s/save-draft".formatted(reportId), USER_M4_DIAGNOSIS, """
            {
              "clinicalDiagnosis": "clinical b",
              "grossExam": "gross b",
              "microscopicExam": "micro b",
              "finalDiagnosis": "final b",
              "richTextContent": "<p>report b</p>",
              
              "terminalCode": "M4-R-02"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reportStatus").value("DRAFT"));

        postJson("/api/v1/pathology-reports/%s/submit".formatted(reportId), USER_M4_DIAGNOSIS, """
            {
              
              "terminalCode": "M4-R-03"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reportStatus").value("SUBMITTED"));

        postJson("/api/v1/pathology-reports/%s/review".formatted(reportId), USER_M4_REVIEW, """
            {
              
              "terminalCode": "M4-R-04"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reportStatus").value("REVIEWED"));

        postJson("/api/v1/pathology-reports/%s/save-draft".formatted(reportId), USER_M4_DIAGNOSIS, """
            {
              "clinicalDiagnosis": "unauthorized change",
              "finalDiagnosis": "unauthorized change"
            }
            """)
            .andExpect(status().isOk());

        postJson("/api/v1/pathology-reports/%s/save-draft".formatted(reportId), USER_M4_REVIEW, """
            {
              "clinicalDiagnosis": "reviewed clinical",
              "grossExam": "reviewed gross",
              "microscopicExam": "reviewed micro",
              "finalDiagnosis": "reviewed final",
              "richTextContent": "<p>reviewed report</p>",
              "remarks": "审核医生修改"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reportStatus").value("REVIEWED"));

        postJson("/api/v1/pathology-reports/%s/sign".formatted(reportId), USER_M4_SIGN, """
            {
              
              "terminalCode": "M4-R-05"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reportStatus").value("SIGNED"))
            .andExpect(jsonPath("$.data.versionStatus").value("SIGNED"));

        JsonNode signedTask = listPendingDiagnosticTasks(context.pathologyNo(), USER_M4_ASSIGN)
            .path("items").get(0);
        assertThat(signedTask.path("reportStatus").asText()).isEqualTo("SIGNED");

        postJson("/api/v1/pathology-reports/%s/publish".formatted(reportId), USER_M4_SIGN, """
            {
              
              "terminalCode": "M4-R-06"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reportStatus").value("PUBLISHED"))
            .andExpect(jsonPath("$.data.versionStatus").value("PUBLISHED"));

        JsonNode workbench = diagnosticWorkbench(context.caseId(), USER_M4_DIAGNOSIS);
        assertThat(workbench.path("currentReport").path("reportStatus").asText()).isEqualTo("PUBLISHED");
        assertThat(workbench.path("currentReport").path("finalDiagnosis").asText()).isEqualTo("reviewed final");
        assertThat(workbench.path("currentReport").path("remarks").asText()).isEqualTo("审核医生修改");
        assertThat(workbench.path("slides")).hasSize(1);

        JsonNode tracking = reportTracking(context.caseId(), USER_M4_TRACKING);
        assertThat(tracking.path("caseStatus").asText()).isEqualTo("REPORT_PUBLISHED");
        assertThat(tracking.path("currentReport").path("reportNo").asText()).isEqualTo(reportNo);
        assertThat(tracking.path("versions")).hasSize(2);
    }

    @Test
    void shouldRejectSubmittedReportAndAllowResubmit() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M4-REJECT-001", "BC-M4-REJECT-001");

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {"taskId":"%s"}
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk());
        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId":"%s",
              "caseId":"%s",
              
              "specimens":[{"specimenId":"%s","specimenType":"ROUTINE","grossDescription":"gd","blocks":[{"blockSite":"A","blockDescription":"B"}]}]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId()))
            .andExpect(status().isOk());

        JsonNode dehydrationTasks = listPendingTasks("DEHYDRATION", context.pathologyNo(), USER_M3_DEHYDRATION);
        String samplingBlockId = dehydrationTasks.path("items").get(0).path("objectId").asText();
        JsonNode batch = responseBody(postJson("/api/v1/dehydration-batches", USER_M3_DEHYDRATION, """
            {"caseId":"%s","basketNo":"B1","deviceNo":"D1","samplingBlockIds":["%s"]}
            """.formatted(context.caseId(), samplingBlockId)), 201);
        String batchId = batch.path("batchId").asText();
        postJson("/api/v1/dehydration-batches/%s/start".formatted(batchId), USER_M3_DEHYDRATION, """
            {}
            """).andExpect(status().isOk());
        postJson("/api/v1/dehydration-batches/%s/complete".formatted(batchId), USER_M3_DEHYDRATION, """
            {}
            """).andExpect(status().isOk());
        String embeddingTaskId = listPendingTasks("EMBEDDING", context.pathologyNo(), USER_M3_EMBEDDING).path("items").get(0).path("id").asText();
        postJson("/api/v1/embeddings/start", USER_M3_EMBEDDING, """
            {"taskId":"%s"}
            """.formatted(embeddingTaskId)).andExpect(status().isOk());
        String embeddingBoxId = responseBody(postJson("/api/v1/embeddings/complete", USER_M3_EMBEDDING, """
            {"taskId":"%s","samplingBlockId":"%s","blockCount":1,"sliceNotice":"n"}
            """.formatted(embeddingTaskId, samplingBlockId)), 200).path("embeddingBoxId").asText();
        String slicingTaskId = listPendingTasks("SLICING", context.pathologyNo(), USER_M3_SLICING).path("items").get(0).path("id").asText();
        postJson("/api/v1/slicings/start", USER_M3_SLICING, """
            {"taskId":"%s"}
            """.formatted(slicingTaskId)).andExpect(status().isOk());
        postJson("/api/v1/slicings/slide-print", USER_M3_SLICING, """
            {
              "taskId":"%s",
              "embeddingBoxId":"%s",
              "sourceSlideCount":1,
              "terminalCode":"M4-S-PRINT"
            }
            """.formatted(slicingTaskId, embeddingBoxId)).andExpect(status().isOk());
        String slideId = responseBody(postJson("/api/v1/slicings/complete", USER_M3_SLICING, """
            {"taskId":"%s","embeddingBoxId":"%s","slideCount":1}
            """.formatted(slicingTaskId, embeddingBoxId)), 200).path("slideIds").get(0).asText();
        String stainingTaskId = listPendingTasks("STAINING", context.pathologyNo(), USER_M3_STAINING).path("items").get(0).path("id").asText();
        postJson("/api/v1/slide-stainings/start", USER_M3_STAINING, """
            {"taskId":"%s"}
            """.formatted(stainingTaskId)).andExpect(status().isOk());
        postJson("/api/v1/slide-stainings/complete", USER_M3_STAINING, """
            {"taskId":"%s","slideId":"%s","stainingType":"HE"}
            """.formatted(stainingTaskId, slideId)).andExpect(status().isOk());

        String diagnosticTaskId = listPendingDiagnosticTasks(context.pathologyNo(), USER_M4_ASSIGN).path("items").get(0).path("id").asText();
        postJson("/api/v1/diagnostic-tasks/%s/assign".formatted(diagnosticTaskId), USER_M4_ASSIGN, """
            {
              "diagnosisDoctorUserId":"USER_M4_DIAGNOSIS",
              "diagnosisDoctorName":"M4 Diagnosis",
              "primaryDoctorUserId":"USER_M4_DIAGNOSIS",
              "primaryDoctorName":"M4 Diagnosis",
              "reviewerUserId":"USER_M4_REVIEW",
              "reviewerName":"M4 Review"}
            """).andExpect(status().isOk());
        postJson("/api/v1/diagnostic-tasks/%s/accept".formatted(diagnosticTaskId), USER_M4_DIAGNOSIS, """
            {}
            """).andExpect(status().isOk());
        postJson("/api/v1/diagnostic-tasks/%s/start".formatted(diagnosticTaskId), USER_M4_DIAGNOSIS, """
            {}
            """).andExpect(status().isOk());

        String reportId = responseBody(postJson("/api/v1/pathology-reports", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "taskId":"%s",
              "clinicalDiagnosis":"c1",
              "grossExam":"g1",
              "microscopicExam":"m1",
              "finalDiagnosis":"f1",
              "richTextContent":"<p>r1</p>"}
            """.formatted(context.caseId(), diagnosticTaskId)), 200).path("reportId").asText();
        postJson("/api/v1/pathology-reports/%s/submit".formatted(reportId), USER_M4_DIAGNOSIS, """
            {}
            """).andExpect(status().isOk());
        postJson("/api/v1/pathology-reports/%s/reject".formatted(reportId), USER_M4_REVIEW, """
            {"rejectReason":"need revise"}
            """).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reportStatus").value("DRAFT"));
        postJson("/api/v1/pathology-reports/%s/save-draft".formatted(reportId), USER_M4_DIAGNOSIS, """
            {
              "clinicalDiagnosis":"c2",
              "grossExam":"g2",
              "microscopicExam":"m2",
              "finalDiagnosis":"f2",
              "richTextContent":"<p>r2</p>"}
            """).andExpect(status().isOk());
        postJson("/api/v1/pathology-reports/%s/submit".formatted(reportId), USER_M4_DIAGNOSIS, """
            {}
            """).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reportStatus").value("SUBMITTED"));
    }

    @Test
    void shouldScheduleFormalReportIssueForTwoHoursLater() throws Exception {
        PublishedReportContext context = preparePublishedReportContext("APP-M4-ISSUE-SCHEDULE-001", "BC-M4-ISSUE-SCHEDULE-001");

        JsonNode versions = formalReportVersions(context.caseId(), USER_M4_SIGN);
        JsonNode targetVersion = null;
        for (JsonNode item : versions) {
            if ("PUBLISHED".equals(item.path("versionStatus").asText())) {
                targetVersion = item;
                break;
            }
        }
        assertThat(targetVersion).isNotNull();
        String versionId = targetVersion.path("versionId").asText();

        postJson("/api/v1/pathology-reports/formal-versions/issue", USER_M4_SIGN, """
            {
              "versionIds":["%s"],
              "issueMode":"DELAY_2_HOURS"
            }
            """.formatted(versionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount").value(1));

        JsonNode refreshed = caseReportVersions(context.caseId(), USER_M4_REVIEW);
        JsonNode scheduled = null;
        for (JsonNode item : refreshed) {
            if (versionId.equals(item.path("versionId").asText())) {
                scheduled = item;
                break;
            }
        }
        assertThat(scheduled).isNotNull();
        assertThat(scheduled.path("printStatus").asText()).isEqualTo("UNPRINTED");
        assertThat(scheduled.path("printedAt").isNull()).isTrue();
        assertThat(scheduled.path("deliveryStatus").asText()).isEqualTo("PENDING");
        assertThat(scheduled.path("plannedIssueAt").asText()).isNotBlank();
        String scheduledVersionLabel = "V" + scheduled.path("versionNo").asText();
        String plannedIssueAt = scheduled.path("plannedIssueAt").asText();
        String plannedIssueAtSecondPrecision = plannedIssueAt.length() >= 19
            ? plannedIssueAt.substring(0, 19)
            : plannedIssueAt;

        List<String> scheduledDistributionNodes = namedParameterJdbcTemplate.queryForList("""
            select node_code
            from workflow_events
            where case_id = :caseId
              and node_code in ('REPORT_PRINT', 'REPORT_SCHEDULE_ISSUE', 'REPORT_ISSUE')
            order by event_time asc, id asc
            """, Map.of("caseId", context.caseId()), String.class);
        assertThat(scheduledDistributionNodes)
            .contains("REPORT_SCHEDULE_ISSUE")
            .doesNotContain("REPORT_PRINT")
            .doesNotContain("REPORT_ISSUE");

        List<String> scheduledDistributionContents = namedParameterJdbcTemplate.queryForList("""
            select event_content
            from workflow_events
            where case_id = :caseId
              and node_code = 'REPORT_SCHEDULE_ISSUE'
            """, Map.of("caseId", context.caseId()), String.class);
        assertThat(scheduledDistributionContents)
            .singleElement()
            .satisfies(content -> assertThat(content)
                .contains(scheduledVersionLabel)
                .contains(plannedIssueAtSecondPrecision));

        JsonNode tracking = reportTracking(context.caseId(), USER_M4_TRACKING);
        assertThat(tracking.toString()).contains("REPORT_SCHEDULE_ISSUE");

        JsonNode lifecycle = lifecycleTracking(context.caseId(), USER_M4_TRACKING);
        assertThat(lifecycle.toString()).contains("REPORT_SCHEDULE_ISSUE");
    }

    @Test
    void shouldListFormalReportsAndSupportPrintIssueRecallFlow() throws Exception {
        PublishedReportContext context = preparePublishedReportContext("APP-M4-DIST-001", "BC-M4-DIST-001");

        JsonNode versions = formalReportVersions(context.caseId(), USER_M4_SIGN);
        assertThat(versions).hasSize(2);
        String signedVersionId = versions.findValuesAsText("versionStatus").contains("SIGNED")
            ? versions.get(1).path("versionId").asText()
            : versions.get(0).path("versionId").asText();

        postJson("/api/v1/pathology-reports/formal-versions/issue", USER_M4_SIGN, """
            {
              "versionIds":["%s"]
            }
            """.formatted(signedVersionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount").value(1));

        postJson("/api/v1/pathology-reports/formal-versions/print", USER_M4_SIGN, """
            {
              "versionIds":["%s"]
            }
            """.formatted(signedVersionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount").value(1));

        postJson("/api/v1/pathology-reports/formal-versions/issue", USER_M4_SIGN, """
            {
              "versionIds":["%s"]
            }
            """.formatted(signedVersionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.failureCount").value(1));

        postJson("/api/v1/pathology-reports/formal-versions/recall", USER_M4_SIGN, """
            {
              "versionIds":["%s"]
            }
            """.formatted(signedVersionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount").value(1));

        JsonNode updatedVersions = formalReportVersions(context.caseId(), USER_M4_SIGN);
        JsonNode target = null;
        for (JsonNode item : updatedVersions) {
            if (signedVersionId.equals(item.path("versionId").asText())) {
                target = item;
                break;
            }
        }
        assertThat(target).isNotNull();
        assertThat(updatedVersions.toString()).contains("PRINTED");
        assertThat(updatedVersions.toString()).contains("RECALLED");
        assertThat(target.path("printedAt").asText()).isNotBlank();
        assertThat(target.path("recalledAt").asText()).isNotBlank();
        String versionLabel = "V" + target.path("versionNo").asText();

        List<String> distributionNodes = namedParameterJdbcTemplate.queryForList("""
            select node_code
            from workflow_events
            where case_id = :caseId
              and node_code in ('REPORT_PRINT', 'REPORT_ISSUE', 'REPORT_RECALL')
            order by event_time asc, id asc
            """, Map.of("caseId", context.caseId()), String.class);
        assertThat(distributionNodes).containsSubsequence(
            "REPORT_ISSUE",
            "REPORT_PRINT",
            "REPORT_RECALL"
        );

        List<String> distributionContents = namedParameterJdbcTemplate.queryForList("""
            select event_content
            from workflow_events
            where case_id = :caseId
              and node_code in ('REPORT_PRINT', 'REPORT_ISSUE', 'REPORT_RECALL')
            order by event_time asc, id asc
            """, Map.of("caseId", context.caseId()), String.class);
        assertThat(distributionContents).allSatisfy(content -> assertThat(content).contains(versionLabel));

        JsonNode tracking = reportTracking(context.caseId(), USER_M4_TRACKING);
        assertThat(tracking.toString())
            .contains("REPORT_PRINT")
            .contains("REPORT_ISSUE")
            .contains("REPORT_RECALL");

        JsonNode lifecycle = lifecycleTracking(context.caseId(), USER_M4_TRACKING);
        assertThat(lifecycle.toString())
            .contains("REPORT_PRINT")
            .contains("REPORT_ISSUE")
            .contains("REPORT_RECALL");
    }

    @Test
    void shouldDeduplicateRepeatedVersionIdsBeforeWritingDistributionEvents() throws Exception {
        PublishedReportContext context = preparePublishedReportContext("APP-M4-DIST-DEDUPE-001", "BC-M4-DIST-DEDUPE-001");

        JsonNode versions = formalReportVersions(context.caseId(), USER_M4_SIGN);
        String versionId = versions.get(0).path("versionId").asText();

        postJson("/api/v1/pathology-reports/formal-versions/print", USER_M4_SIGN, """
            {
              "versionIds":["%s","%s"]
            }
            """.formatted(versionId, versionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalCount").value(1))
            .andExpect(jsonPath("$.data.successCount").value(1))
            .andExpect(jsonPath("$.data.failureCount").value(0));

        Long reportPrintEventCount = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from workflow_events
            where case_id = :caseId
              and node_code = 'REPORT_PRINT'
            """, Map.of("caseId", context.caseId()), Long.class);
        assertThat(reportPrintEventCount).isEqualTo(1);
    }

    @Test
    void shouldNotWriteDuplicateDistributionEventsWhenActionRetried() throws Exception {
        PublishedReportContext context = preparePublishedReportContext("APP-M4-DIST-RETRY-001", "BC-M4-DIST-RETRY-001");

        JsonNode versions = formalReportVersions(context.caseId(), USER_M4_SIGN);
        String versionId = null;
        for (JsonNode item : versions) {
            if ("SIGNED".equals(item.path("versionStatus").asText())) {
                versionId = item.path("versionId").asText();
                break;
            }
        }
        assertThat(versionId).isNotBlank();

        postJson("/api/v1/pathology-reports/formal-versions/print", USER_M4_SIGN, """
            {
              "versionIds":["%s"]
            }
            """.formatted(versionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount").value(1))
            .andExpect(jsonPath("$.data.failureCount").value(0));

        postJson("/api/v1/pathology-reports/formal-versions/print", USER_M4_SIGN, """
            {
              "versionIds":["%s"]
            }
            """.formatted(versionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount").value(0))
            .andExpect(jsonPath("$.data.failureCount").value(1));

        postJson("/api/v1/pathology-reports/formal-versions/issue", USER_M4_SIGN, """
            {
              "versionIds":["%s"]
            }
            """.formatted(versionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount").value(1))
            .andExpect(jsonPath("$.data.failureCount").value(0));

        postJson("/api/v1/pathology-reports/formal-versions/issue", USER_M4_SIGN, """
            {
              "versionIds":["%s"]
            }
            """.formatted(versionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount").value(0))
            .andExpect(jsonPath("$.data.failureCount").value(1));

        postJson("/api/v1/pathology-reports/formal-versions/recall", USER_M4_SIGN, """
            {
              "versionIds":["%s"]
            }
            """.formatted(versionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount").value(1))
            .andExpect(jsonPath("$.data.failureCount").value(0));

        postJson("/api/v1/pathology-reports/formal-versions/recall", USER_M4_SIGN, """
            {
              "versionIds":["%s"]
            }
            """.formatted(versionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount").value(0))
            .andExpect(jsonPath("$.data.failureCount").value(1));

        Map<String, Object> params = Map.of("caseId", context.caseId());
        Long reportPrintEventCount = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from workflow_events
            where case_id = :caseId
              and node_code = 'REPORT_PRINT'
            """, params, Long.class);
        Long reportIssueEventCount = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from workflow_events
            where case_id = :caseId
              and node_code = 'REPORT_ISSUE'
            """, params, Long.class);
        Long reportRecallEventCount = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from workflow_events
            where case_id = :caseId
              and node_code = 'REPORT_RECALL'
            """, params, Long.class);
        assertThat(reportPrintEventCount).isEqualTo(1);
        assertThat(reportIssueEventCount).isEqualTo(1);
        assertThat(reportRecallEventCount).isEqualTo(1);
    }

    @Test
    void shouldWriteDistributionEventsOnlyForSuccessfulItemsInPartialSuccessBatch() throws Exception {
        PublishedReportContext context = preparePublishedReportContext("APP-M4-DIST-PARTIAL-001", "BC-M4-DIST-PARTIAL-001");

        JsonNode versions = formalReportVersions(context.caseId(), USER_M4_SIGN);
        String publishedVersionId = null;
        String signedVersionId = null;
        for (JsonNode item : versions) {
            String versionStatus = item.path("versionStatus").asText();
            if ("PUBLISHED".equals(versionStatus)) {
                publishedVersionId = item.path("versionId").asText();
            }
            if ("SIGNED".equals(versionStatus)) {
                signedVersionId = item.path("versionId").asText();
            }
        }
        assertThat(publishedVersionId).isNotBlank();
        assertThat(signedVersionId).isNotBlank();

        postJson("/api/v1/pathology-reports/formal-versions/issue", USER_M4_SIGN, """
            {
              "versionIds":["%s"]
            }
            """.formatted(signedVersionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount").value(1));

        postJson("/api/v1/pathology-reports/formal-versions/print", USER_M4_SIGN, """
            {
              "versionIds":["%s"]
            }
            """.formatted(publishedVersionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount").value(1));

        postJson("/api/v1/pathology-reports/formal-versions/issue", USER_M4_SIGN, """
            {
              "versionIds":["%s","%s"]
            }
            """.formatted(publishedVersionId, signedVersionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalCount").value(2))
            .andExpect(jsonPath("$.data.successCount").value(1))
            .andExpect(jsonPath("$.data.failureCount").value(1));

        Long reportIssueEventCount = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from workflow_events
            where case_id = :caseId
              and node_code = 'REPORT_ISSUE'
            """, Map.of("caseId", context.caseId()), Long.class);
        assertThat(reportIssueEventCount).isEqualTo(2);
    }

    @Test
    void shouldAuditSuccessfulFormalReportDistributionWrites() throws Exception {
        PublishedReportContext context = preparePublishedReportContext("APP-M4-DIST-AUDIT-001", "BC-M4-DIST-AUDIT-001");

        JsonNode versions = formalReportVersions(context.caseId(), USER_M4_SIGN);
        String versionId = null;
        for (JsonNode item : versions) {
            if ("SIGNED".equals(item.path("versionStatus").asText())) {
                versionId = item.path("versionId").asText();
                break;
            }
        }
        assertThat(versionId).isNotBlank();

        String printOperation = "post /api/v1/pathology-reports/formal-versions/print";
        String issueOperation = "post /api/v1/pathology-reports/formal-versions/issue";
        String recallOperation = "post /api/v1/pathology-reports/formal-versions/recall";
        long printAuditCountBefore = operationLogCount(printOperation);
        long issueAuditCountBefore = operationLogCount(issueOperation);
        long recallAuditCountBefore = operationLogCount(recallOperation);

        postJson("/api/v1/pathology-reports/formal-versions/print", USER_M4_SIGN, """
            {
              "versionIds":["%s"]
            }
            """.formatted(versionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount").value(1));

        postJson("/api/v1/pathology-reports/formal-versions/issue", USER_M4_SIGN, """
            {
              "versionIds":["%s"]
            }
            """.formatted(versionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount").value(1));

        postJson("/api/v1/pathology-reports/formal-versions/recall", USER_M4_SIGN, """
            {
              "versionIds":["%s"]
            }
            """.formatted(versionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount").value(1));

        assertThat(operationLogCount(printOperation)).isEqualTo(printAuditCountBefore + 1);
        assertThat(operationLogCount(issueOperation)).isEqualTo(issueAuditCountBefore + 1);
        assertThat(operationLogCount(recallOperation)).isEqualTo(recallAuditCountBefore + 1);

        assertSuccessfulDistributionAudit(printOperation);
        assertSuccessfulDistributionAudit(issueOperation);
        assertSuccessfulDistributionAudit(recallOperation);
    }

    @Test
    void shouldListCaseReportVersionsAcrossLifecycleStates() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-REPORT-LIST-001", "BC-M4-REPORT-LIST-001");

        String reportId = responseBody(postJson("/api/v1/pathology-reports", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "taskId":"%s",
              "clinicalDiagnosis":"c1",
              "grossExam":"g1",
              "microscopicExam":"m1",
              "finalDiagnosis":"f1",
              "richTextContent":"<p>r1</p>"
            }
            """.formatted(context.caseId(), context.diagnosticTaskId())), 200).path("reportId").asText();

        postJson("/api/v1/pathology-reports/%s/submit".formatted(reportId), USER_M4_DIAGNOSIS, """
            {}
            """).andExpect(status().isOk());
        JsonNode submittedVersions = caseReportVersions(context.caseId(), USER_M4_REVIEW);
        assertThat(submittedVersions).hasSize(1);
        assertThat(submittedVersions.get(0).path("versionStatus").asText()).isEqualTo("SUBMITTED");
        assertThat(submittedVersions.get(0).path("submittedAt").asText()).isNotBlank();

        postJson("/api/v1/pathology-reports/%s/review".formatted(reportId), USER_M4_REVIEW, """
            {}
            """).andExpect(status().isOk());
        JsonNode reviewedVersions = caseReportVersions(context.caseId(), USER_M4_REVIEW);
        assertThat(reviewedVersions.get(0).path("versionStatus").asText()).isEqualTo("REVIEWED");
        assertThat(reviewedVersions.get(0).path("reviewedAt").asText()).isNotBlank();

        postJson("/api/v1/pathology-reports/%s/sign".formatted(reportId), USER_M4_SIGN, """
            {}
            """).andExpect(status().isOk());
        postJson("/api/v1/pathology-reports/%s/publish".formatted(reportId), USER_M4_SIGN, """
            {}
            """).andExpect(status().isOk());
        JsonNode publishedFormalVersions = formalReportVersions(context.caseId(), USER_M4_SIGN);
        String publishedVersionId = publishedFormalVersions.get(0).path("versionId").asText();

        postJson("/api/v1/pathology-reports/formal-versions/print", USER_M4_SIGN, """
            {
              "versionIds":["%s"]
            }
            """.formatted(publishedVersionId)).andExpect(status().isOk());

        JsonNode publishedVersions = caseReportVersions(context.caseId(), USER_M4_REVIEW);
        assertThat(publishedVersions.get(0).path("versionStatus").asText()).isEqualTo("PUBLISHED");
        assertThat(publishedVersions.get(0).path("printedAt").asText()).isNotBlank();
        assertThat(publishedVersions.get(0).path("printStatus").asText()).isEqualTo("PRINTED");
    }

    private long operationLogCount(String operationName) {
        Long count = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from operation_logs
            where operation_name = :operationName
            """, Map.of("operationName", operationName), Long.class);
        return count == null ? 0L : count;
    }

    private void assertSuccessfulDistributionAudit(String operationName) {
        Map<String, Object> latestAudit = namedParameterJdbcTemplate.queryForMap("""
            select business_type, operation_result, operation_content
            from operation_logs
            where operation_name = :operationName
            order by operation_at desc
            limit 1
            """, Map.of("operationName", operationName));
        assertThat(latestAudit.get("business_type")).isEqualTo("PATHOLOGY_REPORTS");
        assertThat(latestAudit.get("operation_result")).isEqualTo("SUCCESS");
        assertThat(latestAudit.get("operation_content").toString())
            .contains("POST")
            .contains("status=200")
            .contains(operationName.substring("post ".length()));
    }
}
