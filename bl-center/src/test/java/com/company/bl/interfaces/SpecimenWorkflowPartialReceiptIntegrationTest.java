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
class SpecimenWorkflowPartialReceiptIntegrationTest extends AbstractSpecimenWorkflowIntegrationTest {

    @Test
    void shouldSupportPartialReceiptAndMarkTrackingAbnormal() throws Exception {
        String applicationId = createApplication("APP-M2-003");
        JsonNode registration = registerSpecimens(applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-P1", "BC-P2");
        String barcode1 = registration.path("specimens").get(0).path("barcode").asText();
        String barcode2 = registration.path("specimens").get(1).path("barcode").asText();

        completeFixation(barcode1);
        completeFixation(barcode2);
        confirmSpecimen(barcode1);
        confirmSpecimen(barcode2);
        checkInSpecimen(barcode1);
        checkInSpecimen(barcode2);

        String transportOrderId = createTransportOrder(applicationId, barcode1, barcode2).path("id").asText();
        postJson("/api/v1/transport-orders/%s/handover".formatted(transportOrderId), USER_TRANSPORT, """
            {
              "receiverUserName": "receiver-b"
            }
            """)
            .andExpect(status().isOk());

        postJson("/api/v1/specimen-receipts", USER_RECEIVE, """
            {
              "transportOrderId": "%s",
              "receivedByName": "receiver-b",
              "logisticsStaffName": "物流员部分签收",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1,
                  "qualityCheckResult": "PASSED"
                },
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "REJECTED",
                  "containerCount": 1,
                  "qualityCheckResult": "FAILED",
                  "qualityIssueCodes": ["CONTAINER_DAMAGE"],
                  "reason": "broken-container"
                }
              ]
            }
            """.formatted(transportOrderId, barcode1, barcode2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.receiptStatus").value("PARTIALLY_RECEIVED"))
            .andExpect(jsonPath("$.data.unreceivedCount").value(1));

        mockMvc.perform(authorized(get("/api/v1/applications/{id}/tracking", applicationId), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PARTIALLY_RECEIVED"))
            .andExpect(jsonPath("$.data.abnormalFlag").value(true));

        JsonNode latestRegistration = responseBody(
            mockMvc.perform(authorized(get("/api/v1/specimens/applications/{applicationId}/latest-registration", applicationId), USER_REGISTER)),
            200);
        JsonNode returnedSpecimen = latestRegistration.path("specimens").findParents("barcode").stream()
            .filter(node -> barcode2.equals(node.path("barcode").asText()))
            .findFirst()
            .orElse(null);
        assertThat(returnedSpecimen).isNotNull();
        assertThat(returnedSpecimen.path("specimenStatus").asText()).isEqualTo("REJECTED");
        assertThat(returnedSpecimen.path("receiptStatus").asText()).isEqualTo("REJECTED");
        assertThat(returnedSpecimen.path("qualityCheckResult").asText()).isEqualTo("FAILED");
        assertThat(returnedSpecimen.path("qualityIssueCodes").get(0).asText()).isEqualTo("CONTAINER_DAMAGE");
        assertThat(returnedSpecimen.path("abnormalReason").asText()).isEqualTo("broken-container");
    }

    @Test
    void shouldAllowContinuingReceiptWhenTransportOrderIsPartiallyReceived() throws Exception {
        String applicationId = createApplication("APP-M2-PARTIAL-ORDER-001");
        JsonNode registration = registerSpecimens(
            applicationId,
            USER_REGISTER,
            "P-01",
            "/api/v1/specimens/register",
            "BC-PARTIAL-ORDER-001",
            "BC-PARTIAL-ORDER-002");
        String barcode1 = registration.path("specimens").get(0).path("barcode").asText();
        String barcode2 = registration.path("specimens").get(1).path("barcode").asText();

        completeFixation(barcode1);
        completeFixation(barcode2);
        confirmSpecimen(barcode1);
        confirmSpecimen(barcode2);
        checkInSpecimen(barcode1);
        checkInSpecimen(barcode2);

        String transportOrderId = createTransportOrder(applicationId, barcode1, barcode2).path("id").asText();
        postJson("/api/v1/transport-orders/%s/handover".formatted(transportOrderId), USER_TRANSPORT, """
            {
              "receiverUserName": "receiver-partial-order",
              "terminalCode": "T-PARTIAL-ORDER"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("HANDED_OVER"));

        postJson("/api/v1/specimen-receipts", USER_RECEIVE, """
            {
              "transportOrderId": "%s",
              "receivedByName": "receiver-partial-order",
              "logisticsStaffName": "物流员部分签收",
              "terminalCode": "T-PARTIAL-ORDER",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1,
                  "qualityCheckResult": "PASSED"
                }
              ]
            }
            """.formatted(transportOrderId, barcode1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.receiptStatus").value("PARTIALLY_RECEIVED"))
            .andExpect(jsonPath("$.data.unreceivedCount").value(1));

        postJson("/api/v1/specimen-receipts", USER_RECEIVE, """
            {
              "transportOrderId": "%s",
              "receivedByName": "receiver-partial-order",
              "logisticsStaffName": "物流员部分签收",
              "terminalCode": "T-PARTIAL-ORDER",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RETURNED",
                  "containerCount": 1,
                  "qualityCheckResult": "FAILED",
                  "qualityIssueCodes": ["PARTIAL_REJECT"],
                  "reason": "return-after-partial"
                }
              ]
            }
            """.formatted(transportOrderId, barcode2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.receiptStatus").value("PARTIALLY_RECEIVED"))
            .andExpect(jsonPath("$.data.unreceivedCount").value(1));

        mockMvc.perform(authorized(get("/api/v1/applications/{id}/tracking", applicationId), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PARTIALLY_RECEIVED"))
            .andExpect(jsonPath("$.data.abnormalFlag").value(true));

        JsonNode latestRegistration = responseBody(
            mockMvc.perform(authorized(get("/api/v1/specimens/applications/{applicationId}/latest-registration", applicationId), USER_REGISTER)),
            200);
        JsonNode returnedSpecimen = latestRegistration.path("specimens").findParents("barcode").stream()
            .filter(node -> barcode2.equals(node.path("barcode").asText()))
            .findFirst()
            .orElseThrow();
        assertThat(returnedSpecimen.path("specimenStatus").asText()).isEqualTo("RETURNED");
        assertThat(returnedSpecimen.path("receiptStatus").asText()).isEqualTo("RETURNED");
        assertThat(returnedSpecimen.path("qualityCheckResult").asText()).isEqualTo("FAILED");
        assertThat(returnedSpecimen.path("qualityIssueCodes").get(0).asText()).isEqualTo("PARTIAL_REJECT");
        assertThat(returnedSpecimen.path("abnormalReason").asText()).isEqualTo("return-after-partial");

        mockMvc.perform(authorized(get("/api/v1/transport-orders/pending"), USER_TRANSPORT)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));
    }
}
