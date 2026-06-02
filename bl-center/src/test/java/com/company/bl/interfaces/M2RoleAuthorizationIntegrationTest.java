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
class M2RoleAuthorizationIntegrationTest extends AbstractSpecimenWorkflowIntegrationTest {

    @Test
    void shouldAllowAdminToRunM2EndToEndWorkflow() throws Exception {
        String suffix = uniqueSuffix();
        String applicationId = createApplication("APP-M2-ROLE-ADMIN-" + suffix);
        JsonNode registration = registerSpecimens(
            applicationId, USER_ADMIN, "P-01", "/api/v1/specimens/register", "BC-ROLE-ADMIN-" + suffix);
        String barcode = registration.path("specimens").get(0).path("barcode").asText();

        completeFixationAsAdmin(barcode);

        JsonNode order = responseBody(postJson("/api/v1/transport-orders", USER_ADMIN, """
            {
              "applicationId": "%s",
              "specimenBarcodes": ["%s"],
              "handoverUserName": "admin-handover",
              "handoverDepartmentId": "DEPT-OR",
              "handoverDepartmentName": "OR",
              "receiverDepartmentId": "DEPT-PATH",
              "receiverDepartmentName": "Pathology",
              "terminalCode": "ADMIN-01"
            }
            """.formatted(applicationId, barcode)), 201);
        String orderId = order.path("id").asText();

        postJson("/api/v1/transport-orders/%s/handover".formatted(orderId), USER_ADMIN, """
            {
              "receiverUserName": "admin-receiver",
              "terminalCode": "ADMIN-02"
            }
            """)
            .andExpect(status().isOk());

        postJson("/api/v1/specimen-receipts", USER_ADMIN, """
            {
              "transportOrderId": "%s",
              "receivedByName": "admin-receiver",
              "logisticsStaffName": "物流员管理员",
              "terminalCode": "ADMIN-03",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1,
                  "qualityCheckResult": "PASSED"
                }
              ]
            }
            """.formatted(orderId, barcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.receiptStatus").value("RECEIVED"));

        mockMvc.perform(authorized(get("/api/v1/applications/{id}/tracking", applicationId), USER_ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("RECEIVED"));
    }

    @Test
    void shouldRejectCrossRoleActionsAcrossWorkstations() throws Exception {
        String suffix = uniqueSuffix();
        String applicationId = createApplication("APP-M2-ROLE-DENY-" + suffix);

        postJson("/api/v1/specimen-fixations/start", USER_REGISTER, """
            {
              "specimenBarcode": "BC-DENY-001",
              "fixationLiquidType": "FORMALIN"}
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        postJson("/api/v1/transport-orders", USER_FIXATION, """
            {
              "applicationId": "%s",
              "specimenBarcodes": ["BC-DENY-001"],
              "handoverUserName": "bad-role",
              "handoverDepartmentName": "OR",
              "receiverDepartmentName": "Pathology"
            }
            """.formatted(applicationId))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        postJson("/api/v1/specimen-receipts", USER_TRANSPORT, """
            {
              "transportOrderId": "TO-DENY-001",
              "receivedByName": "bad-role",
              "logisticsStaffName": "物流员越权",
              "items": [
                {
                  "specimenBarcode": "BC-DENY-001",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1,
                  "qualityCheckResult": "PASSED"
                }
              ]
            }
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        postJson("/api/v1/specimens/register", USER_RECEIVE, """
            {
              "applicationId": "%s",
              "printerCode": "P-01",
              
              "items": [
                {
                  "specimenNameStandardized": "Thyroid Tissue",
                  "specimenType": "ROUTINE",
                  "specimenSite": "Thyroid",
                  "collectionMode": "SURGERY",
                  "containerName": "Specimen Bottle",
                  "containerCount": 1,
                  "specimenCount": 1,
                  "barcode": "BC-DENY-002"
                }
              ]
            }
            """.formatted(applicationId))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        postJson("/api/v1/clinical-applications/import", USER_TRACKING, """
            {
              "thirdPartySource": "HIS",
              "externalOrderNo": "ROLE-DENY-IMPORT"
            }
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        mockMvc.perform(authorized(get("/api/v1/applications/{id}/tracking", applicationId), USER_IMPORT))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void shouldKeepMissingHeaderAndNoPermissionBehaviorForM2ProtectedEndpoints() throws Exception {
        String applicationId = createApplication("APP-M2-ROLE-AUTH-" + uniqueSuffix());

        mockMvc.perform(post("/api/v1/specimens/register")
                .contentType(APPLICATION_JSON)
                .content(registerSpecimenPayload(applicationId, "P-01", "BC-AUTH-M2-001")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        postJson("/api/v1/specimens/register", USER_NO_PERMISSION,
            registerSpecimenPayload(applicationId, "P-01", "BC-AUTH-M2-002"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    private void completeFixationAsAdmin(String barcode) throws Exception {
        postJson("/api/v1/specimen-verifications/start", USER_ADMIN, """
            {
              "specimenBarcode": "%s"}
            """.formatted(barcode))
            .andExpect(status().isOk());

        postJson("/api/v1/specimen-verifications/complete", USER_ADMIN, """
            {
              "specimenBarcode": "%s"}
            """.formatted(barcode))
            .andExpect(status().isOk());

        postJson("/api/v1/specimen-fixations/start", USER_ADMIN, """
            {
              "specimenBarcode": "%s",
              "fixationLiquidType": "FORMALIN"}
            """.formatted(barcode))
            .andExpect(status().isOk());

        postJson("/api/v1/specimen-fixations/complete", USER_ADMIN, """
            {
              "specimenBarcode": "%s",
              "fixationLiquidType": "FORMALIN"}
            """.formatted(barcode))
            .andExpect(status().isOk());

        postJson("/api/v1/specimens/barcodes/%s/confirm".formatted(barcode), USER_ADMIN, """
            {
              
              "terminalCode": "ADMIN-04"
            }
            """)
            .andExpect(status().isOk());

        postJson("/api/v1/specimens/barcodes/%s/check-in".formatted(barcode), USER_ADMIN, """
            {
              
              "specimenBarcode": "%s",
              "terminalCode": "ADMIN-05"
            }
            """.formatted(barcode))
            .andExpect(status().isOk());
    }
}
