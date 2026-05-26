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
class DiagnosticRevisionIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    @Test
    void shouldApproveRevisionAndReenterDraftWorkflowWithNewVersion() throws Exception {
        PublishedReportContext context = preparePublishedReportContext("APP-M4-REV-001", "BC-M4-REV-001");

        JsonNode revision = responseBody(postJson("/api/v1/report-revision-requests", USER_M4_DIAGNOSIS, """
            {
              "reportId":"%s",
              "requestReason":"correct final diagnosis",
              "operatorName":"diag-user",
              "terminalCode":"M4-REV-01"
            }
            """.formatted(context.reportId())), 200);
        String requestId = revision.path("requestId").asText();
        assertThat(revision.path("requestStatus").asText()).isEqualTo("PENDING");
        assertThat(countNotifications(USER_M4_SIGN, "REPORT_REVISION", requestId)).isEqualTo(1L);

        JsonNode workbench = diagnosticWorkbench(context.caseId(), USER_M4_DIAGNOSIS);
        assertThat(workbench.path("hasPendingRevision").asBoolean()).isTrue();
        assertThat(workbench.path("revisions")).hasSize(1);

        postJson("/api/v1/report-revision-requests/%s/approve".formatted(requestId), USER_M4_SIGN, """
            {
              "operatorName":"sign-user",
              "terminalCode":"M4-REV-02",
              "remarks":"approved"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.requestStatus").value("APPROVED"))
            .andExpect(jsonPath("$.data.approvedVersionNo").value(2));
        assertThat(countNotifications(USER_M4_DIAGNOSIS, "REPORT_REVISION", requestId)).isEqualTo(1L);

        JsonNode trackingAfterApprove = reportTracking(context.caseId(), USER_M4_TRACKING);
        assertThat(trackingAfterApprove.path("currentReport").path("reportStatus").asText()).isEqualTo("DRAFT");
        assertThat(trackingAfterApprove.path("currentReport").path("versionNo").asInt()).isEqualTo(2);
        assertThat(trackingAfterApprove.path("latestEffectiveVersionNo").asInt()).isEqualTo(1);
        assertThat(trackingAfterApprove.path("currentDraftVersionNo").asInt()).isEqualTo(2);
        assertThat(trackingAfterApprove.path("hasPendingRevision").asBoolean()).isFalse();

        postJson("/api/v1/pathology-reports/%s/save-draft".formatted(context.reportId()), USER_M4_DIAGNOSIS, """
            {
              "clinicalDiagnosis":"clinical revised",
              "grossExam":"gross revised",
              "microscopicExam":"micro revised",
              "finalDiagnosis":"final revised",
              "richTextContent":"<p>report revised</p>",
              "operatorName":"diag-user",
              "terminalCode":"M4-REV-03"
            }
            """).andExpect(status().isOk());
        postJson("/api/v1/pathology-reports/%s/submit".formatted(context.reportId()), USER_M4_DIAGNOSIS, """
            {"operatorName":"diag-user","terminalCode":"M4-REV-04"}
            """).andExpect(status().isOk());
        postJson("/api/v1/pathology-reports/%s/review".formatted(context.reportId()), USER_M4_REVIEW, """
            {"operatorName":"review-user","terminalCode":"M4-REV-05"}
            """).andExpect(status().isOk());
        postJson("/api/v1/pathology-reports/%s/sign".formatted(context.reportId()), USER_M4_SIGN, """
            {"operatorName":"sign-user","terminalCode":"M4-REV-06"}
            """).andExpect(status().isOk());
        postJson("/api/v1/pathology-reports/%s/publish".formatted(context.reportId()), USER_M4_SIGN, """
            {"operatorName":"sign-user","terminalCode":"M4-REV-07"}
            """).andExpect(status().isOk());

        JsonNode trackingAfterRepublish = reportTracking(context.caseId(), USER_M4_TRACKING);
        assertThat(trackingAfterRepublish.path("currentReport").path("reportStatus").asText()).isEqualTo("PUBLISHED");
        assertThat(trackingAfterRepublish.path("currentReport").path("versionNo").asInt()).isEqualTo(2);
        assertThat(trackingAfterRepublish.path("latestEffectiveVersionNo").asInt()).isEqualTo(2);
        assertThat(trackingAfterRepublish.path("versions")).hasSize(4);
        assertThat(trackingAfterRepublish.toString()).contains("REPORT_REVISION_APPROVE");
    }

    @Test
    void shouldRejectRevisionWithoutChangingCurrentReport() throws Exception {
        PublishedReportContext context = preparePublishedReportContext("APP-M4-REV-002", "BC-M4-REV-002");

        JsonNode revision = responseBody(postJson("/api/v1/report-revision-requests", USER_M4_DIAGNOSIS, """
            {
              "reportId":"%s",
              "requestReason":"double-check wording",
              "operatorName":"diag-user",
              "terminalCode":"M4-REV-11"
            }
            """.formatted(context.reportId())), 200);
        String requestId = revision.path("requestId").asText();

        postJson("/api/v1/report-revision-requests/%s/reject".formatted(requestId), USER_M4_SIGN, """
            {
              "operatorName":"sign-user",
              "terminalCode":"M4-REV-12",
              "rejectReason":"no change needed"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.requestStatus").value("REJECTED"));
        assertThat(countNotifications(USER_M4_DIAGNOSIS, "REPORT_REVISION", requestId)).isEqualTo(1L);

        JsonNode tracking = reportTracking(context.caseId(), USER_M4_TRACKING);
        assertThat(tracking.path("currentReport").path("reportStatus").asText()).isEqualTo("PUBLISHED");
        assertThat(tracking.path("revisions").get(0).path("requestStatus").asText()).isEqualTo("REJECTED");
        assertThat(tracking.path("revisions").get(0).path("rejectReason").asText()).isEqualTo("no change needed");
    }
}
