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
                  "containerCount": 1
                },
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1
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
                  "containerCount": 1
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
    void shouldExposePendingFixationAndReceiptLists() throws Exception {
        String applicationId = createApplication("APP-M2-PENDING-001");
        JsonNode registration = registerSpecimens(applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-PENDING-001");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();

        mockMvc.perform(authorized(get("/api/v1/specimen-fixations/pending"), USER_FIXATION)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].barcode").value(barcode));

        completeFixation(barcode);

        mockMvc.perform(authorized(get("/api/v1/specimen-fixations/pending"), USER_FIXATION)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));

        String transportOrderId = createTransportOrder(applicationId, barcode).path("id").asText();
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
                  "containerCount": 1
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
                  "containerCount": 1
                },
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "REJECTED",
                  "containerCount": 1,
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
    }
}
