package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class M6StatisticsIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    private static final String USER_M1_QUALITY = "USER_M1_QUALITY";

    @Test
    void shouldQueryAndExportStatReports() throws Exception {
        preparePublishedReportContext("APP-M6-STAT-001", "BC-M6-STAT-001");

        JsonNode indicators = responseBody(mockMvc.perform(authorized(get("/api/v1/stat-indicators"), USER_M1_QUALITY)
            .param("category", "QUALITY")), 200);
        assertThat(indicators.size()).isGreaterThanOrEqualTo(13);

        JsonNode templates = responseBody(mockMvc.perform(authorized(get("/api/v1/stat-report-templates"), USER_M1_QUALITY)
            .param("templateType", "OPERATION")), 200);
        assertThat(templates.toString()).contains("TPL_OPERATION_OVERVIEW");

        JsonNode report = responseBody(mockMvc.perform(authorized(post("/api/v1/stat-reports/query"), USER_M1_QUALITY)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "category":"OPERATION",
                      "from":"2026-05-01T00:00:00",
                      "to":"2026-05-31T23:59:59",
                      "operatorUserId":"USER_M1_QUALITY",
                      "operatorName":"quality-user"
                    }
                    """)), 200);
        assertThat(report.path("rows").size()).isGreaterThanOrEqualTo(4);
        assertThat(report.toString()).contains("OP_BILLING_AMOUNT");

        String csv = mockMvc.perform(authorized(post("/api/v1/stat-reports/export"), USER_M1_QUALITY)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "category":"QUALITY",
                      "from":"2026-05-01T00:00:00",
                      "to":"2026-05-31T23:59:59",
                      "operatorUserId":"USER_M1_QUALITY",
                      "operatorName":"quality-user"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".csv")))
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(csv).contains("indicatorCode,indicatorName,metricValue,metricUnit");
        assertThat(csv).contains("QC_SPECIMEN_FIXATION_RATE");
    }
}
