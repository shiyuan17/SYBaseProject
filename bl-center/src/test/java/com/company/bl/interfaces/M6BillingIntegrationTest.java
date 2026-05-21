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
class M6BillingIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    private static final String USER_M1_ADMIN = "USER_M1_ADMIN";

    @Test
    void shouldTriggerSpecialOrderBillingAndAllowRetryAfterOneTimeFailure() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M6-BILL-001", "BC-M6-BILL-001");

        JsonNode created = responseBody(postJson("/api/v1/medical-orders", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "orderType":"RE_STAIN",
              "orderContent":"FAIL_ONCE special billing",
              "operatorName":"diag-user",
              "terminalCode":"M6-B-01"
            }
            """.formatted(context.caseId())), 200);
        String orderId = created.path("orderId").asText();

        postJson("/api/v1/medical-orders/%s/accept".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"operatorName":"order-exec","terminalCode":"M6-B-02"}
            """).andExpect(status().isOk());
        postJson("/api/v1/medical-orders/%s/complete".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"operatorName":"order-exec","terminalCode":"M6-B-03"}
            """).andExpect(status().isOk());

        JsonNode failedRecords = responseBody(mockMvc.perform(authorized(get("/api/v1/billing-records"), USER_M1_ADMIN)
            .param("billingStage", "SPECIAL_ORDER")), 200);
        JsonNode failedRecord = findByField(failedRecords, "orderId", orderId);
        assertThat(failedRecord.path("billingStatus").asText()).isEqualTo("FAILED");

        JsonNode retried = responseBody(postJson("/api/v1/billing-records/%s/retry".formatted(failedRecord.path("id").asText()), USER_M1_ADMIN, """
            {"operatorUserId":"USER_M1_ADMIN","operatorName":"admin-user"}
            """), 200);
        assertThat(retried.path("billingStatus").asText()).isEqualTo("SUCCESS");

        JsonNode tasks = responseBody(mockMvc.perform(authorized(get("/api/v1/integration-tasks"), USER_M1_ADMIN)
            .param("businessType", "BILLING_RECORD")), 200);
        assertThat(tasks.toString()).contains("SPECIAL_ORDER");
    }

    @Test
    void shouldTriggerReportPublishBillingAndAcceptReceiptCallback() throws Exception {
        PublishedReportContext context = preparePublishedReportContext("APP-M6-BILL-002", "BC-M6-BILL-002");

        JsonNode billingRecords = responseBody(mockMvc.perform(authorized(get("/api/v1/billing-records"), USER_M1_ADMIN)
            .param("billingStage", "REPORT_PUBLISH")), 200);
        JsonNode reportBilling = findByFieldContains(billingRecords, "itemType", context.reportNo());
        assertThat(reportBilling.path("billingStatus").asText()).isEqualTo("SUCCESS");

        JsonNode receipt = responseBody(postJson("/api/v1/billing-records/%s/receipt".formatted(reportBilling.path("id").asText()), USER_M1_ADMIN, """
            {
              "externalBillNo":"EXT-RECEIPT-001",
              "billingStatus":"SUCCESS",
              "operatorUserId":"USER_M1_ADMIN",
              "operatorName":"admin-user",
              "remarks":"receipt confirmed"
            }
            """), 200);
        assertThat(receipt.path("externalBillNo").asText()).isEqualTo("EXT-RECEIPT-001");
    }

    private JsonNode findByField(JsonNode items, String fieldName, String expectedValue) {
        for (JsonNode item : items) {
            if (expectedValue.equals(item.path(fieldName).asText())) {
                return item;
            }
        }
        throw new AssertionError("Unable to find item with " + fieldName + "=" + expectedValue + " in " + items);
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
