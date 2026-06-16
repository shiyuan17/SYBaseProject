package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class DiagnosticWorkflowReportLifecycleIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

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

        postJson("/api/v1/pathology-reports/%s/sign".formatted(reportId), USER_M4_SIGN, """
            {
              
              "terminalCode": "M4-R-05"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reportStatus").value("SIGNED"))
            .andExpect(jsonPath("$.data.versionStatus").value("SIGNED"));

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

        JsonNode versions = caseReportVersions(context.caseId(), USER_M4_SIGN);
        String versionId = versions.get(versions.size() - 1).path("versionId").asText();

        postJson("/api/v1/pathology-reports/formal-versions/issue", USER_M4_SIGN, """
            {
              "versionIds":["%s"],
              "issueMode":"DELAY_2_HOURS"
            }
            """.formatted(versionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount").value(1));

        JsonNode refreshed = caseReportVersions(context.caseId(), USER_M4_SIGN);
        JsonNode scheduled = refreshed.get(refreshed.size() - 1);
        assertThat(scheduled.path("deliveryStatus").asText()).isEqualTo("PENDING");
        assertThat(scheduled.path("plannedIssueAt").asText()).isNotBlank();
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
            .andExpect(jsonPath("$.data.failureCount").value(1));

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
            .andExpect(jsonPath("$.data.successCount").value(1));

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
    }

    @Test
    void shouldListCaseReportVersionsAcrossLifecycleStates() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-LIST-001", "BC-M4-LIST-001");

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
}
