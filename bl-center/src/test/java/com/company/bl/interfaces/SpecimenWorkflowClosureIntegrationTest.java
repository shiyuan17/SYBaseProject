package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class SpecimenWorkflowClosureIntegrationTest extends AbstractSpecimenWorkflowIntegrationTest {

    @Test
    void shouldRequireAuthorizationAndRoleAccess() throws Exception {
        String applicationId = createApplication("APP-M2-AUTH-001");

        mockMvc.perform(post("/api/v1/specimens/register")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(registerSpecimenPayload(applicationId, "P-01", "BC-AUTH-001")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        postJson("/api/v1/specimens/register", USER_NO_PERMISSION, registerSpecimenPayload(applicationId, "P-01", "BC-AUTH-002"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        postJson("/api/v1/specimens/register", USER_REGISTER, registerSpecimenPayload(applicationId, "P-01", "BC-AUTH-003"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.specimens[0].barcode").value("BC-AUTH-003"));

        mockMvc.perform(authorized(get("/api/v1/applications/{id}/tracking", applicationId), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(applicationId));
    }

    @Test
    void shouldRejectTransportOrderWhenSpecimenNotFixed() throws Exception {
        String applicationId = createApplication("APP-M2-002");
        JsonNode registration = registerSpecimens(applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-NOT-FIXED");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();

        postJson("/api/v1/transport-orders", USER_TRANSPORT, """
            {
              "applicationId": "%s",
              "specimenBarcodes": ["%s"],
              "handoverUserName": "handover-a",
              "handoverDepartmentName": "OR",
              "receiverDepartmentName": "Pathology"
            }
            """.formatted(applicationId, barcode))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));
    }

    @Test
    void shouldRejectDuplicateBarcodeAndDuplicateReceipt() throws Exception {
        String applicationId = createApplication("APP-M2-DUP-001");
        JsonNode registration = registerSpecimens(applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-DUP-001");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();

        postJson("/api/v1/specimens/register", USER_REGISTER, registerSpecimenPayload(applicationId, "P-01", barcode))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

        completeFixation(barcode);

        postJson("/api/v1/specimen-receipts/by-barcodes", USER_RECEIVE, """
            {
              "receivedByName": "receiver-dup",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1
                }
              ]
            }
            """.formatted(barcode))
            .andExpect(status().isOk());

        postJson("/api/v1/specimen-receipts/by-barcodes", USER_RECEIVE, """
            {
              "receivedByName": "receiver-dup",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1
                }
              ]
            }
            """.formatted(barcode))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
    }

    @Test
    void shouldRetryFailedLabelPrintAndPersistStatus() throws Exception {
        String applicationId = createApplication("APP-M2-PRINT-001");
        JsonNode registration = registerSpecimens(applicationId, USER_REGISTER, "FAIL", "/api/v1/specimens/register", "BC-PRINT-001");
        String batchNo = registration.path("labelPrintBatchNo").asText();

        assertThat(registration.path("labelPrintSuccess").asBoolean()).isFalse();
        assertThat(registration.path("specimens").get(0).path("labelPrintStatus").asText()).isEqualTo("FAILED");

        postJson("/api/v1/specimens/label-batches/%s/retry".formatted(batchNo), USER_REGISTER, """
            {
              "operatorName": "retry-user",
              "printerCode": "P-01",
              "terminalCode": "OR-RETRY"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.labelPrintBatchNo").value(batchNo))
            .andExpect(jsonPath("$.data.retriedCount").value(1))
            .andExpect(jsonPath("$.data.successCount").value(1))
            .andExpect(jsonPath("$.data.failedCount").value(0))
            .andExpect(jsonPath("$.data.allSuccessful").value(true));

        mockMvc.perform(authorized(get("/api/v1/applications/{id}", applicationId), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.specimens[0].labelPrintStatus").value("SUCCESS"));
    }

    @Test
    void shouldRejectRegistrationWhenApplicationAlreadyInTransitOrClosedByReceipt() throws Exception {
        String inTransitApplicationId = createApplication("APP-M2-STATUS-INTRANSIT-001");
        String inTransitBarcode = registerSpecimens(inTransitApplicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-STATUS-INTRANSIT-001")
            .path("specimens").get(0).path("barcode").asText();
        completeFixation(inTransitBarcode);
        String inTransitOrderId = createTransportOrder(inTransitApplicationId, inTransitBarcode).path("id").asText();
        postJson("/api/v1/transport-orders/%s/handover".formatted(inTransitOrderId), USER_TRANSPORT, """
            {
              "receiverUserName": "receiver-in-transit",
              "terminalCode": "T-IN-TRANSIT"
            }
            """)
            .andExpect(status().isOk());

        postJson("/api/v1/specimens/register", USER_REGISTER, registerSpecimenPayload(
            inTransitApplicationId, "P-01", "BC-STATUS-INTRANSIT-002"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));

        String receivedApplicationId = createApplication("APP-M2-STATUS-RECEIVED-001");
        String receivedBarcode = registerSpecimens(receivedApplicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-STATUS-RECEIVED-001")
            .path("specimens").get(0).path("barcode").asText();
        completeFixation(receivedBarcode);
        String receivedOrderId = createTransportOrder(receivedApplicationId, receivedBarcode).path("id").asText();
        postJson("/api/v1/transport-orders/%s/handover".formatted(receivedOrderId), USER_TRANSPORT, """
            {
              "receiverUserName": "receiver-complete",
              "terminalCode": "T-RECEIVED"
            }
            """)
            .andExpect(status().isOk());
        postJson("/api/v1/specimen-receipts", USER_RECEIVE, """
            {
              "transportOrderId": "%s",
              "receivedByName": "receiver-complete",
              "terminalCode": "T-RECEIVED",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1
                }
              ]
            }
            """.formatted(receivedOrderId, receivedBarcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.receiptStatus").value("RECEIVED"));

        postJson("/api/v1/specimens/register", USER_REGISTER, registerSpecimenPayload(
            receivedApplicationId, "P-01", "BC-STATUS-RECEIVED-002"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));

        String partialApplicationId = createApplication("APP-M2-STATUS-PARTIAL-001");
        JsonNode partialRegistration = registerSpecimens(
            partialApplicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-STATUS-PARTIAL-001", "BC-STATUS-PARTIAL-002");
        String partialBarcode1 = partialRegistration.path("specimens").get(0).path("barcode").asText();
        String partialBarcode2 = partialRegistration.path("specimens").get(1).path("barcode").asText();
        completeFixation(partialBarcode1);
        completeFixation(partialBarcode2);
        String partialOrderId = createTransportOrder(partialApplicationId, partialBarcode1, partialBarcode2).path("id").asText();
        postJson("/api/v1/transport-orders/%s/handover".formatted(partialOrderId), USER_TRANSPORT, """
            {
              "receiverUserName": "receiver-partial",
              "terminalCode": "T-PARTIAL"
            }
            """)
            .andExpect(status().isOk());
        postJson("/api/v1/specimen-receipts", USER_RECEIVE, """
            {
              "transportOrderId": "%s",
              "receivedByName": "receiver-partial",
              "terminalCode": "T-PARTIAL",
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
                  "reason": "partial-reject"
                }
              ]
            }
            """.formatted(partialOrderId, partialBarcode1, partialBarcode2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.receiptStatus").value("PARTIALLY_RECEIVED"));

        postJson("/api/v1/specimens/register", USER_REGISTER, registerSpecimenPayload(
            partialApplicationId, "P-01", "BC-STATUS-PARTIAL-003"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));

        String rejectedApplicationId = createApplication("APP-M2-STATUS-REJECTED-001");
        String rejectedBarcode = registerSpecimens(rejectedApplicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-STATUS-REJECTED-001")
            .path("specimens").get(0).path("barcode").asText();
        completeFixation(rejectedBarcode);
        String rejectedOrderId = createTransportOrder(rejectedApplicationId, rejectedBarcode).path("id").asText();
        postJson("/api/v1/transport-orders/%s/handover".formatted(rejectedOrderId), USER_TRANSPORT, """
            {
              "receiverUserName": "receiver-rejected",
              "terminalCode": "T-REJECTED"
            }
            """)
            .andExpect(status().isOk());
        postJson("/api/v1/specimen-receipts", USER_RECEIVE, """
            {
              "transportOrderId": "%s",
              "receivedByName": "receiver-rejected",
              "terminalCode": "T-REJECTED",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "REJECTED",
                  "containerCount": 1,
                  "reason": "fully-rejected"
                }
              ]
            }
            """.formatted(rejectedOrderId, rejectedBarcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.receiptStatus").value("REJECTED"));

        postJson("/api/v1/specimens/register", USER_REGISTER, registerSpecimenPayload(
            rejectedApplicationId, "P-01", "BC-STATUS-REJECTED-002"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));
    }

    @Test
    void shouldReturnPlaceholderErrorWhenClinicalImportUnavailable() throws Exception {
        postJson("/api/v1/clinical-applications/import", USER_IMPORT, """
            {
              "thirdPartySource": "HIS",
              "externalOrderNo": "REAL-001"
            }
            """)
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("EXTERNAL_INTEGRATION_UNAVAILABLE"));
    }

    @Test
    void shouldSupportSpecimenCollectionAlias() throws Exception {
        String applicationId = createApplication("APP-M2-ALIAS-001");
        postJson("/api/v1/specimen-collections", USER_REGISTER, registerSpecimenPayload(applicationId, "P-01", "BC-ALIAS-001"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.specimens[0].barcode").value("BC-ALIAS-001"))
            .andExpect(jsonPath("$.data.specimens[0].labelPrintStatus").value("SUCCESS"));
    }
}
