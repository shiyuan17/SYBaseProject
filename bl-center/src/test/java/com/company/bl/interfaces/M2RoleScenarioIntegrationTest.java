package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class M2RoleScenarioIntegrationTest extends AbstractSpecimenWorkflowIntegrationTest {

    @Test
    void shouldAllowRegisterRoleToRegisterRetryAndUseCollectionAlias() throws Exception {
        String applicationId = createApplication("APP-M2-ROLE-REG-001");
        JsonNode failedRegistration = registerSpecimens(
            applicationId, USER_REGISTER, "FAIL", "/api/v1/specimens/register", "BC-ROLE-REG-001");
        String batchNo = failedRegistration.path("labelPrintBatchNo").asText();

        postJson("/api/v1/specimens/label-batches/%s/retry".formatted(batchNo), USER_REGISTER, """
            {
              "operatorName": "register-user",
              "printerCode": "P-01",
              "terminalCode": "OR-REG-01"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.allSuccessful").value(true));

        String aliasApplicationId = createApplication("APP-M2-ROLE-REG-002");
        postJson("/api/v1/specimen-collections", USER_REGISTER, registerSpecimenPayload(
            aliasApplicationId, "P-01", "BC-ROLE-REG-002"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.specimens[0].barcode").value("BC-ROLE-REG-002"));
    }

    @Test
    void shouldAllowFixationRoleToViewPendingAndCompleteFixation() throws Exception {
        String applicationId = createApplication("APP-M2-ROLE-FIX-001");
        String barcode = registerSpecimens(
            applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-ROLE-FIX-001")
            .path("specimens").get(0).path("barcode").asText();

        mockMvc.perform(authorized(get("/api/v1/specimen-fixations/pending"), USER_FIXATION)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1));

        completeFixation(barcode);
    }

    @Test
    void shouldAllowTransportRoleToCreatePrintAndHandover() throws Exception {
        String applicationId = createApplication("APP-M2-ROLE-TRANS-001");
        String barcode = registerSpecimens(
            applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-ROLE-TRANS-001")
            .path("specimens").get(0).path("barcode").asText();
        completeFixation(barcode);

        String transportOrderId = createTransportOrder(applicationId, barcode).path("id").asText();
        postJson("/api/v1/transport-orders/%s/print".formatted(transportOrderId), USER_TRANSPORT, """
            {
              "operatorName": "transport-print",
              "terminalCode": "T-PRINT"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PRINTED"));

        postJson("/api/v1/transport-orders/%s/handover".formatted(transportOrderId), USER_TRANSPORT, """
            {
              "receiverUserName": "transport-receiver",
              "terminalCode": "T-HAND"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("HANDED_OVER"));
    }

    @Test
    void shouldAllowReceiveRoleToQueryPendingAndReceive() throws Exception {
        String applicationId = createApplication("APP-M2-ROLE-REC-001");
        String barcode = registerSpecimens(
            applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-ROLE-REC-001")
            .path("specimens").get(0).path("barcode").asText();
        completeFixation(barcode);
        String transportOrderId = createTransportOrder(applicationId, barcode).path("id").asText();
        postJson("/api/v1/transport-orders/%s/handover".formatted(transportOrderId), USER_TRANSPORT, """
            {
              "receiverUserName": "receive-ready",
              "terminalCode": "T-READY"
            }
            """)
            .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/v1/specimen-receipts/pending"), USER_RECEIVE)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1));

        postJson("/api/v1/specimen-receipts", USER_RECEIVE, """
            {
              "transportOrderId": "%s",
              "receivedByName": "receiver-role",
              "terminalCode": "T-REC",
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
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.receiptStatus").value("RECEIVED"));
    }

    @Test
    void shouldAllowTrackingAndImportRolesToAccessTheirOwnEndpoints() throws Exception {
        String applicationId = createApplication("APP-M2-ROLE-TRACK-001");
        String barcode = registerSpecimens(
            applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-ROLE-TRACK-001")
            .path("specimens").get(0).path("barcode").asText();

        mockMvc.perform(authorized(get("/api/v1/applications/{id}/tracking", applicationId), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(applicationId));

        mockMvc.perform(authorized(get("/api/v1/specimens/barcodes/{barcode}/tracking", barcode), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.applicationNo").value("APP-M2-ROLE-TRACK-001"));

        postJson("/api/v1/clinical-applications/import", USER_IMPORT, """
            {
              "thirdPartySource": "HIS",
              "externalOrderNo": "ROLE-IMPORT-001"
            }
            """)
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("EXTERNAL_INTEGRATION_UNAVAILABLE"));
    }
}
