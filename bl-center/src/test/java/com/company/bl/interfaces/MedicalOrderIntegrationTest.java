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
class MedicalOrderIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    @Test
    void shouldCompleteTechnicalMedicalOrderAndExposeStatusInWorkbenchAndTracking() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-ORDER-001", "BC-M4-ORDER-001");

        JsonNode created = responseBody(postJson("/api/v1/medical-orders", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "orderType":"RE_STAIN",
              "orderContent":"restain with HE",
              "operatorName":"diag-user",
              "terminalCode":"M4-ORD-01"
            }
            """.formatted(context.caseId())), 200);
        String orderId = created.path("orderId").asText();
        assertThat(created.path("orderNumber").asText()).startsWith("MO-");
        assertThat(created.path("status").asText()).isEqualTo("PENDING");

        JsonNode pending = responseBody(mockMvc.perform(authorized(get("/api/v1/medical-orders/pending"), USER_M4_ORDER_EXECUTE)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", context.pathologyNo())), 200);
        assertThat(pending.path("total").asInt()).isEqualTo(1);

        postJson("/api/v1/medical-orders/%s/accept".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"operatorName":"order-exec","terminalCode":"M4-ORD-02"}
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        JsonNode workbenchAfterAccept = diagnosticWorkbench(context.caseId(), USER_M4_DIAGNOSIS);
        assertThat(workbenchAfterAccept.path("medicalOrders").get(0).path("status").asText()).isEqualTo("IN_PROGRESS");

        postJson("/api/v1/medical-orders/%s/complete".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"operatorName":"order-exec","terminalCode":"M4-ORD-03","remarks":"done"}
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        JsonNode tracking = reportTracking(context.caseId(), USER_M4_TRACKING);
        assertThat(tracking.path("medicalOrders").get(0).path("status").asText()).isEqualTo("COMPLETED");
        assertThat(tracking.toString()).contains("MEDICAL_ORDER_COMPLETE");
    }

    @Test
    void shouldCancelPendingMedicalOrderOnly() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-ORDER-002", "BC-M4-ORDER-002");

        JsonNode created = responseBody(postJson("/api/v1/medical-orders", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "orderType":"DEEP_CUT",
              "orderContent":"deep cut required",
              "operatorName":"diag-user",
              "terminalCode":"M4-ORD-11"
            }
            """.formatted(context.caseId())), 200);
        String orderId = created.path("orderId").asText();

        postJson("/api/v1/medical-orders/%s/cancel".formatted(orderId), USER_M4_DIAGNOSIS, """
            {"operatorName":"diag-user","terminalCode":"M4-ORD-12","remarks":"not needed"}
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        JsonNode tracking = reportTracking(context.caseId(), USER_M4_TRACKING);
        assertThat(tracking.path("medicalOrders").get(0).path("status").asText()).isEqualTo("CANCELLED");
    }
}
