package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class SpecimenWorkflowHappyPathIntegrationTest extends AbstractSpecimenWorkflowIntegrationTest {

    @Test
    void shouldCompleteSpecimenWorkflowEndToEnd() throws Exception {
        String applicationId = createApplication("APP-M2-001");
        JsonNode registration = registerSpecimens(applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-001", "BC-002");
        String barcode1 = registration.path("specimens").get(0).path("barcode").asText();
        String barcode2 = registration.path("specimens").get(1).path("barcode").asText();

        completeFixation(barcode1);
        completeFixation(barcode2);

        JsonNode order = createTransportOrder(applicationId, barcode1, barcode2);
        String transportOrderId = order.path("id").asText();

        postJson("/api/v1/transport-orders/%s/print".formatted(transportOrderId), USER_TRANSPORT, """
            {
              "operatorName": "print-user",
              "terminalCode": "T-01"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PRINTED"));

        postJson("/api/v1/transport-orders/%s/handover".formatted(transportOrderId), USER_TRANSPORT, """
            {
              "receiverUserName": "receiver-a",
              "terminalCode": "T-01"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("HANDED_OVER"));

        postJson("/api/v1/specimen-receipts", USER_RECEIVE, """
            {
              "transportOrderId": "%s",
              "receivedByName": "receiver-a",
              "terminalCode": "T-02",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1,
                  "qualityCheckResult": "PASSED"
                },
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1,
                  "qualityCheckResult": "PASSED"
                }
              ]
            }
            """.formatted(transportOrderId, barcode1, barcode2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.caseId").isNotEmpty())
            .andExpect(jsonPath("$.data.pathologyNo").isNotEmpty())
            .andExpect(jsonPath("$.data.receiptStatus").value("RECEIVED"))
            .andExpect(jsonPath("$.data.unreceivedCount").value(0));

        mockMvc.perform(authorized(get("/api/v1/applications/{id}/tracking", applicationId), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("RECEIVED"))
            .andExpect(jsonPath("$.data.currentNode").value("RECEPTION"))
            .andExpect(jsonPath("$.data.abnormalFlag").value(false))
            .andExpect(jsonPath("$.data.recentEvents[0].specimenId").isNotEmpty())
            .andExpect(jsonPath("$.data.recentEvents[0].specimenNo").isNotEmpty())
            .andExpect(jsonPath("$.data.recentEvents[0].specimenBarcode").isNotEmpty())
            .andExpect(jsonPath("$.data.specimens[0].specimenStatus").value("RECEIVED"))
            .andExpect(jsonPath("$.data.specimens[1].specimenStatus").value("RECEIVED"));

        mockMvc.perform(authorized(get("/api/v1/specimens/barcodes/{barcode}/tracking", barcode1), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.applicationNo").value("APP-M2-001"));
    }

    @Test
    void shouldSupportDirectBarcodeReceiptAndGeneratePathologyCase() throws Exception {
        String applicationId = createApplication("APP-M2-DIRECT-001");
        JsonNode registration = registerSpecimens(applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-DIRECT-001");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();

        completeFixation(barcode);

        postJson("/api/v1/specimen-receipts/by-barcodes", USER_RECEIVE, """
            {
              "receivedByName": "receiver-direct",
              "terminalCode": "T-03",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1,
                  "qualityCheckResult": "PASSED"
                }
              ]
            }
            """.formatted(barcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.caseId").isNotEmpty())
            .andExpect(jsonPath("$.data.pathologyNo").isNotEmpty())
            .andExpect(jsonPath("$.data.receiptStatus").value("RECEIVED"))
            .andExpect(jsonPath("$.data.unreceivedCount").value(0));

        mockMvc.perform(authorized(get("/api/v1/applications/{id}/tracking", applicationId), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("RECEIVED"));
    }

    @Test
    void shouldExposeFixationProcessingAndReceiptLists() throws Exception {
        String applicationId = createApplication("APP-M2-PENDING-001");
        JsonNode registration = registerSpecimens(applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-PENDING-001");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();

        mockMvc.perform(authorized(get("/api/v1/specimen-fixations/pending"), USER_FIXATION)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId)
                .param("fixationStatus", "PENDING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].barcode").value(barcode));

        completeFixation(barcode);

        mockMvc.perform(authorized(get("/api/v1/specimen-fixations/pending"), USER_FIXATION)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].barcode").value(barcode))
            .andExpect(jsonPath("$.data.items[0].fixationStatus").value("COMPLETED"))
            .andExpect(jsonPath("$.data.items[0].specimenStatus").value("FIXED"));

        mockMvc.perform(authorized(get("/api/v1/specimen-fixations/pending"), USER_FIXATION)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId)
                .param("fixationStatus", "COMPLETED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].barcode").value(barcode));

        String transportOrderId = createTransportOrder(applicationId, barcode).path("id").asText();

        mockMvc.perform(authorized(get("/api/v1/specimen-fixations/pending"), USER_FIXATION)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(authorized(get("/api/v1/transport-orders/pending"), USER_TRANSPORT)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].transportOrderNo").isNotEmpty())
            .andExpect(jsonPath("$.data.items[0].specimenBarcodes[0]").value(barcode));

        mockMvc.perform(authorized(get("/api/v1/specimen-receipts/pending"), USER_RECEIVE)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));

        postJson("/api/v1/transport-orders/%s/handover".formatted(transportOrderId), USER_TRANSPORT, """
            {
              "receiverUserName": "receiver-pending",
              "terminalCode": "T-04"
            }
            """)
            .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/v1/specimen-receipts/pending"), USER_RECEIVE)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].barcode").value(barcode));

        postJson("/api/v1/specimen-receipts", USER_RECEIVE, """
            {
              "transportOrderId": "%s",
              "receivedByName": "receiver-pending",
              "terminalCode": "T-05",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1,
                  "qualityCheckResult": "PASSED"
                }
              ]
            }
            """.formatted(transportOrderId, barcode))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/v1/specimen-receipts/pending"), USER_RECEIVE)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void shouldSupportPartialReceiptAndMarkTrackingAbnormal() throws Exception {
        String applicationId = createApplication("APP-M2-003");
        JsonNode registration = registerSpecimens(applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-P1", "BC-P2");
        String barcode1 = registration.path("specimens").get(0).path("barcode").asText();
        String barcode2 = registration.path("specimens").get(1).path("barcode").asText();

        completeFixation(barcode1);
        completeFixation(barcode2);

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
        org.assertj.core.api.Assertions.assertThat(returnedSpecimen).isNotNull();
        org.assertj.core.api.Assertions.assertThat(returnedSpecimen.path("specimenStatus").asText()).isEqualTo("REJECTED");
        org.assertj.core.api.Assertions.assertThat(returnedSpecimen.path("receiptStatus").asText()).isEqualTo("REJECTED");
        org.assertj.core.api.Assertions.assertThat(returnedSpecimen.path("qualityCheckResult").asText()).isEqualTo("FAILED");
        org.assertj.core.api.Assertions.assertThat(returnedSpecimen.path("qualityIssueCodes").get(0).asText()).isEqualTo("CONTAINER_DAMAGE");
        org.assertj.core.api.Assertions.assertThat(returnedSpecimen.path("abnormalReason").asText()).isEqualTo("broken-container");
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
        org.assertj.core.api.Assertions.assertThat(returnedSpecimen.path("specimenStatus").asText()).isEqualTo("RETURNED");
        org.assertj.core.api.Assertions.assertThat(returnedSpecimen.path("receiptStatus").asText()).isEqualTo("RETURNED");
        org.assertj.core.api.Assertions.assertThat(returnedSpecimen.path("qualityCheckResult").asText()).isEqualTo("FAILED");
        org.assertj.core.api.Assertions.assertThat(returnedSpecimen.path("qualityIssueCodes").get(0).asText()).isEqualTo("PARTIAL_REJECT");
        org.assertj.core.api.Assertions.assertThat(returnedSpecimen.path("abnormalReason").asText()).isEqualTo("return-after-partial");

        mockMvc.perform(authorized(get("/api/v1/transport-orders/pending"), USER_TRANSPORT)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));
    }
}
