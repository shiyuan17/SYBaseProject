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
    void shouldRequirePermissionHeaderAndRoleAccess() throws Exception {
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

        mockMvc.perform(get("/api/v1/applications/{id}/tracking", applicationId).header("X-User-Id", USER_TRACKING))
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

        mockMvc.perform(get("/api/v1/applications/{id}", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.specimens[0].labelPrintStatus").value("SUCCESS"));
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
