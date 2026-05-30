package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class M6ObservabilityIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    private static final String USER_M1_ADMIN = "USER_M1_ADMIN";
    private static final String USER_M1_ARCHIVE = "USER_M1_ARCHIVE";
    private static final String USER_M1_QUALITY = "USER_M1_QUALITY";

    @Test
    void shouldExposeM6MetricsAndBacklogGauges() throws Exception {
        postJson("/api/v1/clinical-applications/import", USER_IMPORT, """
            {
              "thirdPartySource":"MOCK_HIS",
              "externalOrderNo":"MOCK-M6-OBS-001"
            }
            """).andExpect(status().isCreated());

        StartedDiagnosticContext startedContext = prepareStartedDiagnosticCase("APP-M6-OBS-001", "BC-M6-OBS-001");
        JsonNode order = responseBody(postJson("/api/v1/medical-orders", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "orderType":"RE_STAIN",
              "orderContent":"FAIL_ONCE observability billing",
              "terminalCode":"M6-O-01"
            }
            """.formatted(startedContext.caseId())), 200);
        String orderId = order.path("orderId").asText();
        postJson("/api/v1/medical-orders/%s/accept".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M6-O-02"}
            """).andExpect(status().isOk());
        postJson("/api/v1/medical-orders/%s/complete".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M6-O-03"}
            """).andExpect(status().isOk());

        JsonNode failedRecords = responseBody(mockMvc.perform(authorized(get("/api/v1/billing-records"), USER_M1_ADMIN)
            .param("billingStage", "SPECIAL_ORDER")
            .param("orderId", orderId)), 200);
        String failedBillingId = failedRecords.get(0).path("id").asText();
        postJson("/api/v1/billing-records/%s/retry".formatted(failedBillingId), USER_M1_ADMIN, """
            {"operatorUserId":"USER_M1_ADMIN","operatorName":"admin-user"}
            """).andExpect(status().isOk());

        PublishedReportContext publishedContext = preparePublishedReportContext("APP-M6-OBS-002", "BC-M6-OBS-002");
        JsonNode reportBillingRecords = responseBody(mockMvc.perform(authorized(get("/api/v1/billing-records"), USER_M1_ADMIN)
            .param("billingStage", "REPORT_PUBLISH")), 200);
        String reportBillingId = findByFieldContains(reportBillingRecords, "itemType", publishedContext.reportNo()).path("id").asText();
        postJson("/api/v1/billing-records/%s/receipt".formatted(reportBillingId), USER_M1_ADMIN, """
            {
              "externalBillNo":"EXT-OBS-001",
              "billingStatus":"SUCCESS",
              "operatorUserId":"USER_M1_ADMIN",
              "operatorName":"admin-user",
              "remarks":"receipt confirmed"
            }
            """).andExpect(status().isOk());

        postJson("/api/v1/historical-report-import-jobs", USER_M1_ARCHIVE, """
            {
              "sourceSystem":"MOCK_HIS",
              "operatorUserId":"USER_M1_ARCHIVE",
              "operatorName":"archive-user",
              "remarks":"metrics import"
            }
            """).andExpect(status().isOk());

        mockMvc.perform(authorized(post("/api/v1/stat-reports/query"), USER_M1_QUALITY)
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
            .andExpect(status().isOk());

        mockMvc.perform(authorized(post("/api/v1/stat-reports/export"), USER_M1_QUALITY)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "category":"OPERATION",
                      "from":"2026-05-01T00:00:00",
                      "to":"2026-05-31T23:59:59",
                      "operatorUserId":"USER_M1_QUALITY",
                      "operatorName":"quality-user"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/actuator/prometheus"), USER_M1_ADMIN))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("clinical_application_import_total")))
            .andExpect(content().string(containsString("billing_submit_total")))
            .andExpect(content().string(containsString("billing_submit_failed_total")))
            .andExpect(content().string(containsString("billing_retry_total")))
            .andExpect(content().string(containsString("billing_receipt_total")))
            .andExpect(content().string(containsString("historical_report_import_total")))
            .andExpect(content().string(containsString("stat_report_query_total")))
            .andExpect(content().string(containsString("stat_report_export_total")))
            .andExpect(content().string(containsString("integration_retry_pending_count")))
            .andExpect(content().string(containsString("integration_manual_required_count")))
            .andExpect(content().string(containsString("billing_reconciliation_discrepancy_count")))
            .andExpect(content().string(containsString("historical_import_running_count")));
    }

    private JsonNode findByFieldContains(JsonNode items, String fieldName, String expectedValue) {
        for (JsonNode item : items) {
            if (item.path(fieldName).asText().contains(expectedValue)) {
                return item;
            }
        }
        throw new AssertionError("Unable to find item with " + fieldName + " containing " + expectedValue + " in " + items);
    }
}
