package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class DiagnosticQueryIdentifierIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    @Test
    void shouldSupportPathologyNoForDiagnosticWorkbenchAndReportTracking() throws Exception {
        PublishedReportContext context = preparePublishedReportContext("APP-M4-QUERY-001", "BC-M4-QUERY-001");

        JsonNode workbench = diagnosticWorkbenchByIdentifier(context.pathologyNo(), USER_M4_DIAGNOSIS);
        JsonNode tracking = reportTrackingByIdentifier(context.pathologyNo(), USER_M4_TRACKING);

        assertThat(workbench.path("caseId").asText()).isEqualTo(context.caseId());
        assertThat(workbench.path("pathologyNo").asText()).isEqualTo(context.pathologyNo());
        assertThat(tracking.path("caseId").asText()).isEqualTo(context.caseId());
        assertThat(tracking.path("pathologyNo").asText()).isEqualTo(context.pathologyNo());
    }

    @Test
    void shouldReturn404WhenDiagnosticQueryIdentifierDoesNotExist() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/pathology-cases/{id}/diagnostic-workbench", "BL-NOT-FOUND"), USER_M4_DIAGNOSIS))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Pathology case not found"));

        mockMvc.perform(authorized(get("/api/v1/pathology-cases/{id}/report-tracking", "BL-NOT-FOUND"), USER_M4_TRACKING))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Pathology case not found"));
    }
}
