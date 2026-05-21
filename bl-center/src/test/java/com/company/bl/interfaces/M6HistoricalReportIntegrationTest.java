package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class M6HistoricalReportIntegrationTest extends AbstractSpecimenWorkflowIntegrationTest {

    private static final String USER_M1_ARCHIVE = "USER_M1_ARCHIVE";

    @Test
    void shouldImportHistoricalReportsAndKeepQueryDeduplicated() throws Exception {
        JsonNode firstJob = responseBody(postJson("/api/v1/historical-report-import-jobs", USER_M1_ARCHIVE, """
            {
              "sourceSystem":"MOCK_HIS",
              "operatorUserId":"USER_M1_ARCHIVE",
              "operatorName":"archive-user",
              "remarks":"first import"
            }
            """), 200);
        assertThat(firstJob.path("importStatus").asText()).isEqualTo("COMPLETED");
        assertThat(firstJob.path("totalCount").asInt()).isEqualTo(2);

        JsonNode historicalReports = responseBody(mockMvc.perform(authorized(get("/api/v1/historical-reports"), USER_M1_ARCHIVE)
            .param("sourceSystem", "MOCK_HIS")), 200);
        assertThat(historicalReports).hasSize(2);

        responseBody(postJson("/api/v1/historical-report-import-jobs", USER_M1_ARCHIVE, """
            {
              "sourceSystem":"MOCK_HIS",
              "operatorUserId":"USER_M1_ARCHIVE",
              "operatorName":"archive-user",
              "remarks":"second import"
            }
            """), 200);

        JsonNode historicalReportsAfterSecondImport = responseBody(mockMvc.perform(authorized(get("/api/v1/historical-reports"), USER_M1_ARCHIVE)
            .param("sourceSystem", "MOCK_HIS")), 200);
        assertThat(historicalReportsAfterSecondImport).hasSize(2);
        assertThat(historicalReportsAfterSecondImport.toString()).contains("HIS-RPT-001");

        JsonNode importJobs = responseBody(mockMvc.perform(authorized(get("/api/v1/historical-report-import-jobs"), USER_M1_ARCHIVE)
            .param("sourceSystem", "MOCK_HIS")), 200);
        assertThat(importJobs.size()).isGreaterThanOrEqualTo(2);
        assertThat(importJobs.get(0).path("integrationTaskId").asText()).isNotBlank();
        assertThat(importJobs.get(0).path("compensationStatus").asText()).isIn("NONE", "RESOLVED");
    }
}
