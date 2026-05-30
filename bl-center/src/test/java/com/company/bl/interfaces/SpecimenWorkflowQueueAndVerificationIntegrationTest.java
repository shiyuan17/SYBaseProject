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
class SpecimenWorkflowQueueAndVerificationIntegrationTest extends AbstractSpecimenWorkflowIntegrationTest {

    @Test
    void shouldExposeFixationProcessingAndReceiptLists() throws Exception {
        String applicationId = createApplication("APP-M2-PENDING-001");
        JsonNode registration = registerSpecimens(applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-PENDING-001");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();
        String specimenNo = registration.path("specimens").get(0).path("specimenNo").asText();

        mockMvc.perform(authorized(get("/api/v1/specimen-fixations/pending"), USER_FIXATION)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId)
                .param("fixationStatus", "PENDING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].barcode").value(barcode))
            .andExpect(jsonPath("$.data.items[0].verificationStatus").value("UNVERIFIED"))
            .andExpect(jsonPath("$.data.items[0].verificationStartedAt").isEmpty())
            .andExpect(jsonPath("$.data.items[0].verificationCompletedAt").isEmpty());

        mockMvc.perform(authorized(get("/api/v1/specimen-fixations/pending"), USER_FIXATION)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId)
                .param("specimenNo", specimenNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].specimenNo").value(specimenNo));

        mockMvc.perform(authorized(get("/api/v1/specimen-fixations/pending"), USER_FIXATION)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId)
                .param("verificationStatus", "UNVERIFIED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].barcode").value(barcode));

        startVerification(barcode);

        mockMvc.perform(authorized(get("/api/v1/specimen-fixations/pending"), USER_FIXATION)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId)
                .param("verificationStatus", "VERIFYING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].barcode").value(barcode))
            .andExpect(jsonPath("$.data.items[0].verificationStatus").value("VERIFYING"))
            .andExpect(jsonPath("$.data.items[0].verificationStartedAt").isNotEmpty())
            .andExpect(jsonPath("$.data.items[0].verificationCompletedAt").isEmpty());

        completeVerification(barcode);

        mockMvc.perform(authorized(get("/api/v1/specimen-fixations/pending"), USER_FIXATION)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].barcode").value(barcode))
            .andExpect(jsonPath("$.data.items[0].fixationStatus").value("PENDING"))
            .andExpect(jsonPath("$.data.items[0].specimenStatus").value("REGISTERED"))
            .andExpect(jsonPath("$.data.items[0].verificationStatus").value("VERIFIED"))
            .andExpect(jsonPath("$.data.items[0].verificationStartedAt").isNotEmpty())
            .andExpect(jsonPath("$.data.items[0].verificationCompletedAt").isNotEmpty());

        mockMvc.perform(authorized(get("/api/v1/specimen-fixations/pending"), USER_FIXATION)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId)
                .param("fixationStatus", "COMPLETED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(authorized(get("/api/v1/specimen-fixations/pending"), USER_FIXATION)
                .param("page", "1")
                .param("size", "20")
                .param("applicationId", applicationId)
                .param("verificationStatus", "VERIFIED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].barcode").value(barcode));

        postJson("/api/v1/specimen-fixations/start", USER_FIXATION, """
            {
              "specimenBarcode": "%s",
              "fixationLiquidType": "FORMALIN"}
            """.formatted(barcode))
            .andExpect(status().isOk());

        postJson("/api/v1/specimen-fixations/complete", USER_FIXATION, """
            {
              "specimenBarcode": "%s",
              "fixationLiquidType": "FORMALIN"}
            """.formatted(barcode))
            .andExpect(status().isOk());

        postJson("/api/v1/specimens/barcodes/%s/confirm".formatted(barcode), USER_FIXATION, """
            {
              
              "terminalCode": "T-CONFIRM"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.specimenConfirmedAt").isNotEmpty())
            .andExpect(jsonPath("$.data.checkInStatus").value("NOT_CHECKED_IN"));

        postJson("/api/v1/specimens/barcodes/%s/check-in".formatted(barcode), USER_FIXATION, """
            {
              
              "specimenBarcode": "%s",
              "terminalCode": "T-CHECK-IN"
            }
            """.formatted(barcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.checkInStatus").value("CHECKED_IN"))
            .andExpect(jsonPath("$.data.checkedInAt").isNotEmpty());

        mockMvc.perform(authorized(get("/api/v1/specimens/barcodes/{barcode}/verification-records", barcode), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(4))
            .andExpect(jsonPath("$.data[0].verificationType").isNotEmpty())
            .andExpect(jsonPath("$.data[1].verificationType").isNotEmpty());

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

        JsonNode pendingTransportOrdersBySpecimenNo = responseBody(
            mockMvc.perform(authorized(get("/api/v1/transport-orders/pending"), USER_TRANSPORT)
                .param("page", "1")
                .param("size", "20")
                .param("specimenNo", specimenNo)),
            200);
        assertThat(
                java.util.stream.StreamSupport.stream(
                    pendingTransportOrdersBySpecimenNo.path("items").spliterator(),
                    false)
                    .anyMatch(item -> applicationId.equals(item.path("applicationId").asText())
                        && java.util.stream.StreamSupport.stream(
                            item.path("specimenBarcodes").spliterator(),
                            false)
                            .map(JsonNode::asText)
                            .anyMatch(barcode::equals)))
            .isTrue();

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
    void shouldRequireStartedVerificationBeforeCompletionAndRejectDuplicateTransitions() throws Exception {
        String applicationId = createApplication("APP-M2-VERIFY-001");
        JsonNode registration = registerSpecimens(applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-VERIFY-001");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();

        postJson("/api/v1/specimen-verifications/complete", USER_FIXATION, """
            {
              "specimenBarcode": "%s"}
            """.formatted(barcode))
            .andExpect(status().isConflict());

        postJson("/api/v1/specimen-verifications/start", USER_FIXATION, """
            {
              "specimenBarcode": "%s"}
            """.formatted(barcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.verificationStatus").value("VERIFYING"))
            .andExpect(jsonPath("$.data.verificationStartedAt").isNotEmpty())
            .andExpect(jsonPath("$.data.verificationCompletedAt").isEmpty());

        postJson("/api/v1/specimen-verifications/start", USER_FIXATION, """
            {
              "specimenBarcode": "%s"}
            """.formatted(barcode))
            .andExpect(status().isConflict());

        postJson("/api/v1/specimen-verifications/complete", USER_FIXATION, """
            {
              "specimenBarcode": "%s"}
            """.formatted(barcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"))
            .andExpect(jsonPath("$.data.verificationStartedAt").isNotEmpty())
            .andExpect(jsonPath("$.data.verificationCompletedAt").isNotEmpty());

        postJson("/api/v1/specimen-verifications/complete", USER_FIXATION, """
            {
              "specimenBarcode": "%s"}
            """.formatted(barcode))
            .andExpect(status().isConflict());

        postJson("/api/v1/specimen-fixations/start", USER_FIXATION, """
            {
              "specimenBarcode": "%s",
              "fixationLiquidType": "FORMALIN"}
            """.formatted(barcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fixationStatus").value("FIXING"));
    }

    @Test
    void shouldSupportVerificationApisAndRecords() throws Exception {
        String applicationId = createApplication("APP-M2-VERIFY-002");
        JsonNode registration = registerSpecimens(applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-VERIFY-002");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();

        postJson("/api/v1/specimen-verifications/complete", USER_FIXATION, """
            {
              "specimenBarcode": "%s"}
            """.formatted(barcode))
            .andExpect(status().isConflict());

        postJson("/api/v1/specimen-verifications/start", USER_FIXATION, """
            {
              "specimenBarcode": "%s"}
            """.formatted(barcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.verificationStatus").value("VERIFYING"))
            .andExpect(jsonPath("$.data.verificationStartedAt").isNotEmpty())
            .andExpect(jsonPath("$.data.verificationCompletedAt").isEmpty());

        postJson("/api/v1/specimen-verifications/start", USER_FIXATION, """
            {
              "specimenBarcode": "%s"}
            """.formatted(barcode))
            .andExpect(status().isConflict());

        postJson("/api/v1/specimen-verifications/complete", USER_FIXATION, """
            {
              "specimenBarcode": "%s"}
            """.formatted(barcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"))
            .andExpect(jsonPath("$.data.verificationCompletedAt").isNotEmpty());

        postJson("/api/v1/specimen-verifications/complete", USER_FIXATION, """
            {
              "specimenBarcode": "%s"}
            """.formatted(barcode))
            .andExpect(status().isConflict());

        mockMvc.perform(authorized(get("/api/v1/specimens/barcodes/{barcode}/verification-records", barcode), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].barcode").value(barcode))
            .andExpect(jsonPath("$.data[0].verificationType").isNotEmpty());

        postJson("/api/v1/specimen-fixations/start", USER_FIXATION, """
            {
              "specimenBarcode": "%s",
              "fixationLiquidType": "FORMALIN"}
            """.formatted(barcode))
            .andExpect(status().isOk());

        postJson("/api/v1/specimen-fixations/complete", USER_FIXATION, """
            {
              "specimenBarcode": "%s",
              "fixationLiquidType": "FORMALIN"}
            """.formatted(barcode))
            .andExpect(status().isOk());

        postJson("/api/v1/specimens/barcodes/%s/confirm".formatted(barcode), USER_FIXATION, """
            {
              }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.specimenConfirmedAt").isNotEmpty())
            .andExpect(jsonPath("$.data.checkInStatus").value("NOT_CHECKED_IN"));

        postJson("/api/v1/specimens/barcodes/%s/check-in".formatted(barcode), USER_FIXATION, """
            {
              
              "specimenBarcode": "%s"
            }
            """.formatted(barcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.checkInStatus").value("CHECKED_IN"))
            .andExpect(jsonPath("$.data.checkedInAt").isNotEmpty());
    }
}
