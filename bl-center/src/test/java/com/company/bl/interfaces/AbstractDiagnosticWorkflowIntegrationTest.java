package com.company.bl.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class AbstractDiagnosticWorkflowIntegrationTest extends AbstractTechnicalWorkflowIntegrationTest {

    protected static final String USER_M4_ASSIGN = "USER_M4_ASSIGN";
    protected static final String USER_M4_DIAGNOSIS = "USER_M4_DIAGNOSIS";
    protected static final String USER_M4_REVIEW = "USER_M4_REVIEW";
    protected static final String USER_M4_SIGN = "USER_M4_SIGN";
    protected static final String USER_M4_TRACKING = "USER_M4_TRACKING";
    protected static final String USER_M4_ORDER_EXECUTE = "USER_M4_ORDER_EXECUTE";
    protected static final String USER_M4_NO_PERMISSION = "USER_M4_NO_PERMISSION";

    protected JsonNode listPendingDiagnosticTasks(String pathologyNo, String userId) throws Exception {
        ResultActions action = mockMvc.perform(authorized(get("/api/v1/diagnostic-tasks/pending"), userId)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", pathologyNo));
        return responseBody(action, 200);
    }

    protected JsonNode diagnosticWorkbench(String caseId, String userId) throws Exception {
        return responseBody(mockMvc.perform(authorized(get("/api/v1/pathology-cases/{id}/diagnostic-workbench", caseId), userId)), 200);
    }

    protected JsonNode reportTracking(String caseId, String userId) throws Exception {
        return responseBody(mockMvc.perform(authorized(get("/api/v1/pathology-cases/{id}/report-tracking", caseId), userId)), 200);
    }

    protected StartedDiagnosticContext prepareStartedDiagnosticCase(String applicationNo, String barcode) throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask(applicationNo, barcode);

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {"taskId":"%s","operatorName":"grossing-user","terminalCode":"M4-G-01"}
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk());
        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId":"%s",
              "caseId":"%s",
              "operatorName":"grossing-user",
              "terminalCode":"M4-G-02",
              "specimens":[{"specimenId":"%s","specimenType":"ROUTINE","grossDescription":"gd","blocks":[{"blockSite":"A","blockDescription":"B"}]}]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId()))
            .andExpect(status().isOk());

        String samplingBlockId = listPendingTasks("DEHYDRATION", context.pathologyNo(), USER_M3_DEHYDRATION)
            .path("items").get(0).path("objectId").asText();
        String batchId = responseBody(postJson("/api/v1/dehydration-batches", USER_M3_DEHYDRATION, """
            {
              "caseId":"%s",
              "basketNo":"B1",
              "deviceNo":"D1",
              "operatorName":"dehydration-user",
              "terminalCode":"M4-D-01",
              "samplingBlockIds":["%s"]
            }
            """.formatted(context.caseId(), samplingBlockId)), 201).path("batchId").asText();
        postJson("/api/v1/dehydration-batches/%s/start".formatted(batchId), USER_M3_DEHYDRATION, """
            {"operatorName":"dehydration-user","terminalCode":"M4-D-02"}
            """).andExpect(status().isOk());
        postJson("/api/v1/dehydration-batches/%s/complete".formatted(batchId), USER_M3_DEHYDRATION, """
            {"operatorName":"dehydration-user","terminalCode":"M4-D-03"}
            """).andExpect(status().isOk());

        String embeddingTaskId = listPendingTasks("EMBEDDING", context.pathologyNo(), USER_M3_EMBEDDING)
            .path("items").get(0).path("id").asText();
        postJson("/api/v1/embeddings/start", USER_M3_EMBEDDING, """
            {"taskId":"%s","operatorName":"embedding-user","terminalCode":"M4-E-01"}
            """.formatted(embeddingTaskId)).andExpect(status().isOk());
        String embeddingBoxId = responseBody(postJson("/api/v1/embeddings/complete", USER_M3_EMBEDDING, """
            {
              "taskId":"%s",
              "samplingBlockId":"%s",
              "blockCount":1,
              "sliceNotice":"n",
              "operatorName":"embedding-user",
              "terminalCode":"M4-E-02"
            }
            """.formatted(embeddingTaskId, samplingBlockId)), 200).path("embeddingBoxId").asText();

        String slicingTaskId = listPendingTasks("SLICING", context.pathologyNo(), USER_M3_SLICING)
            .path("items").get(0).path("id").asText();
        postJson("/api/v1/slicings/start", USER_M3_SLICING, """
            {"taskId":"%s","operatorName":"slicing-user","terminalCode":"M4-S-01"}
            """.formatted(slicingTaskId)).andExpect(status().isOk());
        String slideId = responseBody(postJson("/api/v1/slicings/complete", USER_M3_SLICING, """
            {
              "taskId":"%s",
              "embeddingBoxId":"%s",
              "slideCount":1,
              "operatorName":"slicing-user",
              "terminalCode":"M4-S-02"
            }
            """.formatted(slicingTaskId, embeddingBoxId)), 200).path("slideIds").get(0).asText();

        String stainingTaskId = listPendingTasks("STAINING", context.pathologyNo(), USER_M3_STAINING)
            .path("items").get(0).path("id").asText();
        postJson("/api/v1/slide-stainings/start", USER_M3_STAINING, """
            {"taskId":"%s","operatorName":"staining-user","terminalCode":"M4-T-01"}
            """.formatted(stainingTaskId)).andExpect(status().isOk());
        postJson("/api/v1/slide-stainings/complete", USER_M3_STAINING, """
            {"taskId":"%s","slideId":"%s","stainingType":"HE","operatorName":"staining-user","terminalCode":"M4-T-02"}
            """.formatted(stainingTaskId, slideId)).andExpect(status().isOk());

        JsonNode pendingDiagnosticTasks = listPendingDiagnosticTasks(context.pathologyNo(), USER_M4_ASSIGN);
        assertThat(pendingDiagnosticTasks.path("total").asInt()).isEqualTo(1);
        String diagnosticTaskId = pendingDiagnosticTasks.path("items").get(0).path("id").asText();

        postJson("/api/v1/diagnostic-tasks/%s/assign".formatted(diagnosticTaskId), USER_M4_ASSIGN, """
            {
              "diagnosisDoctorUserId":"USER_M4_DIAGNOSIS",
              "diagnosisDoctorName":"M4 Diagnosis",
              "primaryDoctorUserId":"USER_M4_DIAGNOSIS",
              "primaryDoctorName":"M4 Diagnosis",
              "reviewerUserId":"USER_M4_REVIEW",
              "reviewerName":"M4 Review",
              "operatorName":"assign-user",
              "terminalCode":"M4-A-01"
            }
            """).andExpect(status().isOk());
        postJson("/api/v1/diagnostic-tasks/%s/accept".formatted(diagnosticTaskId), USER_M4_DIAGNOSIS, """
            {"operatorName":"diag-user","terminalCode":"M4-A-02"}
            """).andExpect(status().isOk());
        postJson("/api/v1/diagnostic-tasks/%s/start".formatted(diagnosticTaskId), USER_M4_DIAGNOSIS, """
            {"operatorName":"diag-user","terminalCode":"M4-A-03"}
            """).andExpect(status().isOk());

        return new StartedDiagnosticContext(context.caseId(), context.pathologyNo(), diagnosticTaskId);
    }

    protected PublishedReportContext preparePublishedReportContext(String applicationNo, String barcode) throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase(applicationNo, barcode);
        JsonNode createdReport = responseBody(postJson("/api/v1/pathology-reports", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "taskId":"%s",
              "clinicalDiagnosis":"clinical a",
              "grossExam":"gross a",
              "microscopicExam":"micro a",
              "finalDiagnosis":"final a",
              "richTextContent":"<p>report a</p>",
              "operatorName":"diag-user",
              "terminalCode":"M4-R-01"
            }
            """.formatted(context.caseId(), context.diagnosticTaskId())), 200);
        String reportId = createdReport.path("reportId").asText();
        String reportNo = createdReport.path("reportNo").asText();

        postJson("/api/v1/pathology-reports/%s/submit".formatted(reportId), USER_M4_DIAGNOSIS, """
            {"operatorName":"diag-user","terminalCode":"M4-R-03"}
            """).andExpect(status().isOk());
        postJson("/api/v1/pathology-reports/%s/review".formatted(reportId), USER_M4_REVIEW, """
            {"operatorName":"review-user","terminalCode":"M4-R-04"}
            """).andExpect(status().isOk());
        postJson("/api/v1/pathology-reports/%s/sign".formatted(reportId), USER_M4_SIGN, """
            {"operatorName":"sign-user","terminalCode":"M4-R-05"}
            """).andExpect(status().isOk());
        postJson("/api/v1/pathology-reports/%s/publish".formatted(reportId), USER_M4_SIGN, """
            {"operatorName":"sign-user","terminalCode":"M4-R-06"}
            """).andExpect(status().isOk());

        return new PublishedReportContext(context.caseId(), context.pathologyNo(), context.diagnosticTaskId(), reportId, reportNo);
    }

    protected record StartedDiagnosticContext(
        String caseId,
        String pathologyNo,
        String diagnosticTaskId
    ) {
    }

    protected record PublishedReportContext(
        String caseId,
        String pathologyNo,
        String diagnosticTaskId,
        String reportId,
        String reportNo
    ) {
    }
}
