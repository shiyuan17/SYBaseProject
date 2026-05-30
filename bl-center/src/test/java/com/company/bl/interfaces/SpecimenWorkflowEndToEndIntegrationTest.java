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
class SpecimenWorkflowEndToEndIntegrationTest extends AbstractSpecimenWorkflowIntegrationTest {

    @Test
    void shouldCompleteSpecimenWorkflowEndToEnd() throws Exception {
        String applicationId = createApplication("APP-M2-001");
        JsonNode registration = registerSpecimens(applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-001", "BC-002");
        String barcode1 = registration.path("specimens").get(0).path("barcode").asText();
        String barcode2 = registration.path("specimens").get(1).path("barcode").asText();

        completeFixation(barcode1);
        completeFixation(barcode2);
        confirmSpecimen(barcode1);
        confirmSpecimen(barcode2);
        checkInSpecimen(barcode1);
        checkInSpecimen(barcode2);

        JsonNode order = createTransportOrder(applicationId, barcode1, barcode2);
        String transportOrderId = order.path("id").asText();

        postJson("/api/v1/transport-orders/%s/print".formatted(transportOrderId), USER_TRANSPORT, """
            {
              
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
}
