package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
              "terminalCode":"M6-B-01"
            }
            """.formatted(context.caseId())), 200);
        String orderId = created.path("orderId").asText();

        postJson("/api/v1/medical-orders/%s/accept".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M6-B-02"}
            """).andExpect(status().isOk());
        postJson("/api/v1/medical-orders/%s/complete".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M6-B-03"}
            """).andExpect(status().isOk());

        JsonNode failedRecords = responseBody(mockMvc.perform(authorized(get("/api/v1/billing-records"), USER_M1_ADMIN)
            .param("billingStage", "SPECIAL_ORDER")
            .param("orderId", orderId)), 200);
        JsonNode failedRecord = findByField(failedRecords, "orderId", orderId);
        assertThat(failedRecord.path("billingStatus").asText()).isEqualTo("FAILED");
        assertThat(failedRecord.path("integrationTaskId").asText()).isNotBlank();
        assertThat(failedRecord.path("retryCount").asInt()).isEqualTo(1);
        assertThat(failedRecord.path("lastErrorCode").asText()).isEqualTo("BILLING_SUBMIT_FAILED");
        assertThat(failedRecord.path("compensationStatus").asText()).isEqualTo("RETRY_PENDING");

        JsonNode retryPendingTasks = responseBody(mockMvc.perform(authorized(get("/api/v1/integration-tasks"), USER_M1_ADMIN)
            .param("taskType", "BILLING_SUBMIT")
            .param("businessType", "BILLING_RECORD")
            .param("businessId", failedRecord.path("id").asText())
            .param("taskStatus", "RETRY_PENDING")
            .param("externalSystem", "MOCK_BILLING")
            .param("compensationStatus", "RETRY_PENDING")), 200);
        JsonNode retryPendingTask = retryPendingTasks.get(0);
        assertThat(retryPendingTask.path("requestPayload").asText()).contains("SPECIAL_ORDER");
        assertThat(retryPendingTask.path("lastErrorCode").asText()).isEqualTo("BILLING_SUBMIT_FAILED");
        assertThat(retryPendingTask.path("responsePayload").asText()).contains("one-time failure");

        JsonNode retried = responseBody(postJson("/api/v1/billing-records/%s/retry".formatted(failedRecord.path("id").asText()), USER_M1_ADMIN, """
            {}
            """), 200);
        assertThat(retried.path("billingStatus").asText()).isEqualTo("SUCCESS");
        assertThat(retried.path("compensationStatus").asText()).isEqualTo("RESOLVED");
        assertThat(retried.path("resolvedAt").asText()).isNotBlank();
        assertThat(retried.path("operatorUserId").asText()).isEqualTo(USER_M1_ADMIN);
        assertThat(retried.path("operatorName").asText()).isEqualTo(userDisplayName(USER_M1_ADMIN));

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
              "remarks":"receipt confirmed"
            }
            """), 200);
        assertThat(receipt.path("externalBillNo").asText()).isEqualTo("EXT-RECEIPT-001");
        assertThat(receipt.path("integrationTaskId").asText()).isNotBlank();
        assertThat(receipt.path("reconciliationStatus").asText()).isEqualTo("MATCHED");
        assertThat(receipt.path("operatorUserId").asText()).isEqualTo(USER_M1_ADMIN);
        assertThat(receipt.path("operatorName").asText()).isEqualTo(userDisplayName(USER_M1_ADMIN));
    }

    @Test
    void shouldExecuteDiagnosisWorkbenchBillingForSelectedMedicalOrders() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M6-BILL-004", "BC-M6-BILL-004");
        JsonNode selectedOrder = createMedicalOrder(context.caseId(), "manual selected billing");
        JsonNode untouchedOrder = createMedicalOrder(context.caseId(), "manual unselected billing");

        JsonNode result = responseBody(postJson("/api/v1/medical-orders/billing/execute", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "orderIds":["%s"],
              "terminalCode":"M6-B-21"
            }
            """.formatted(context.caseId(), selectedOrder.path("orderId").asText())), 200);

        assertThat(result.path("totalCount").asInt()).isEqualTo(1);
        assertThat(result.path("successCount").asInt()).isEqualTo(1);
        assertThat(result.path("items").get(0).path("billingStatus").asText()).isEqualTo("SUCCESS");
        assertThat(result.path("items").get(0).path("billingRecordId").asText()).isNotBlank();

        JsonNode selectedRecords = responseBody(mockMvc.perform(authorized(get("/api/v1/billing-records"), USER_M1_ADMIN)
            .param("billingStage", "SPECIAL_ORDER")
            .param("orderId", selectedOrder.path("orderId").asText())), 200);
        assertThat(selectedRecords).hasSize(1);

        JsonNode untouchedRecords = responseBody(mockMvc.perform(authorized(get("/api/v1/billing-records"), USER_M1_ADMIN)
            .param("billingStage", "SPECIAL_ORDER")
            .param("orderId", untouchedOrder.path("orderId").asText())), 200);
        assertThat(untouchedRecords).isEmpty();
    }

    @Test
    void shouldConfirmDiagnosisWorkbenchBillingForAllUnchargedOrders() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M6-BILL-005", "BC-M6-BILL-005");
        JsonNode firstOrder = createMedicalOrder(context.caseId(), "manual confirm billing one");
        JsonNode secondOrder = createMedicalOrder(context.caseId(), "manual confirm billing two");

        JsonNode result = responseBody(postJson("/api/v1/medical-orders/billing/confirm", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "remarks":"诊断工作站确认完成收费",
              "terminalCode":"M6-B-22"
            }
            """.formatted(context.caseId())), 200);

        assertThat(result.path("totalCount").asInt()).isEqualTo(2);
        assertThat(result.path("successCount").asInt()).isEqualTo(2);
        assertThat(result.path("failureCount").asInt()).isZero();

        JsonNode workbench = diagnosticWorkbench(context.caseId(), USER_M4_DIAGNOSIS);
        assertThat(findByField(workbench.path("medicalOrders"), "orderId", firstOrder.path("orderId").asText())
            .path("billingStatus").asText()).isEqualTo("SUCCESS");
        assertThat(findByField(workbench.path("medicalOrders"), "orderId", secondOrder.path("orderId").asText())
            .path("billingStatus").asText()).isEqualTo("SUCCESS");
    }

    @Test
    void shouldRejectLegacyOperatorFieldsOnBillingRetry() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M6-BILL-003", "BC-M6-BILL-003");

        JsonNode created = responseBody(postJson("/api/v1/medical-orders", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "orderType":"RE_STAIN",
              "orderContent":"FAIL_ONCE reject legacy operator",
              "terminalCode":"M6-B-11"
            }
            """.formatted(context.caseId())), 200);
        String orderId = created.path("orderId").asText();

        postJson("/api/v1/medical-orders/%s/accept".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M6-B-12"}
            """).andExpect(status().isOk());
        postJson("/api/v1/medical-orders/%s/complete".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M6-B-13"}
            """).andExpect(status().isOk());

        JsonNode failedRecords = responseBody(mockMvc.perform(authorized(get("/api/v1/billing-records"), USER_M1_ADMIN)
            .param("billingStage", "SPECIAL_ORDER")
            .param("orderId", orderId)), 200);
        String failedBillingId = findByField(failedRecords, "orderId", orderId).path("id").asText();

        postJson("/api/v1/billing-records/%s/retry".formatted(failedBillingId), USER_M1_ADMIN, """
            {"operatorUserId":"FORGED-USER","operatorName":"forged-user"}
            """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("operatorUserId")));
    }

    private JsonNode createMedicalOrder(String caseId, String content) throws Exception {
        return responseBody(postJson("/api/v1/medical-orders", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "orderType":"RE_STAIN",
              "orderContent":"%s",
              "terminalCode":"M6-B-20"
            }
            """.formatted(caseId, content)), 200);
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
