package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class M2CollectionAndLabelIntegrationTest extends AbstractSpecimenWorkflowIntegrationTest {

    @Test
    void shouldRegisterThroughCollectionEndpointAndRetryFailedLabels() throws Exception {
        String applicationId = createApplication("APP-M2-COLLECT-001");

        JsonNode registration = responseBody(postJson("/api/v1/specimen-collections", USER_REGISTER, """
            {
              "applicationId": "%s",
              "printerCode": "FAIL",
              "collectionScene": "WARD",
              
              "terminalCode": "WARD-01",
              "items": [
                {
                  "specimenNameStandardized": "Biopsy Tissue",
                  "specimenType": "ROUTINE",
                  "specimenSite": "Lung",
                  "collectionMode": "BIOPSY",
                  "containerName": "Specimen Bottle",
                  "containerCount": 1,
                  "specimenCount": 1,
                  "barcode": "BC-COLLECT-001"
                }
              ]
            }
            """.formatted(applicationId)), 201);

        String batchNo = registration.path("labelPrintBatchNo").asText();
        String barcode = registration.path("specimens").get(0).path("barcode").asText();

        mockMvc.perform(authorized(post("/api/v1/specimens/label-batches/{batchNo}/retry", batchNo), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      
                      "printerCode": "P-01",
                      "terminalCode": "WARD-02",
                      "remarks": "retry after printer failure"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.labelPrintBatchNo").value(batchNo))
            .andExpect(jsonPath("$.data.retriedCount").value(1))
            .andExpect(jsonPath("$.data.successCount").value(1))
            .andExpect(jsonPath("$.data.failedCount").value(0))
            .andExpect(jsonPath("$.data.allSuccessful").value(true));

        mockMvc.perform(authorized(post("/api/v1/specimens/label-batches/{batchNo}/retry", batchNo), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      
                      "printerCode": "P-01",
                      "terminalCode": "WARD-02"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.retriedCount").value(0));

        mockMvc.perform(authorized(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/specimens/barcodes/{barcode}/tracking", barcode), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.applicationNo").value("APP-M2-COLLECT-001"));
    }

    @Test
    void shouldReturnEmptyRetrySummaryWhenNoFailedLabelsExist() throws Exception {
        String applicationId = createApplication("APP-M2-COLLECT-002");
        JsonNode registration = registerSpecimens(
            applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-COLLECT-002");
        String batchNo = registration.path("labelPrintBatchNo").asText();

        mockMvc.perform(authorized(post("/api/v1/specimens/label-batches/{batchNo}/retry", batchNo), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      
                      "printerCode": "P-01",
                      "terminalCode": "WARD-03"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.labelPrintBatchNo").value(batchNo))
            .andExpect(jsonPath("$.data.retriedCount").value(0))
            .andExpect(jsonPath("$.data.successCount").value(0))
            .andExpect(jsonPath("$.data.failedCount").value(0))
            .andExpect(jsonPath("$.data.allSuccessful").value(true))
            .andExpect(jsonPath("$.data.message").value("No pending or failed labels found for retry"));
    }

    @Test
    void shouldKeepNewSpecimensUnboundUntilBarcodeBinding() throws Exception {
        String applicationIdOne = createApplication("APP-M2-UNBOUND-001", "DEPT-MGMT", "Specimen Department");
        String applicationIdTwo = createApplication("APP-M2-UNBOUND-002", "DEPT-MGMT", "Specimen Department");

        JsonNode registrationOne = responseBody(postJson("/api/v1/specimens/register", USER_REGISTER, """
            {
              "applicationId": "%s",
              "printerCode": "P-01",
              "terminalCode": "OR-UNBOUND-01",
              "items": [
                {
                  "specimenNameStandardized": "Biopsy Tissue",
                  "specimenType": "ROUTINE",
                  "specimenSite": "Lung",
                  "collectionMode": "BIOPSY",
                  "containerName": "Specimen Bottle",
                  "containerCount": 1,
                  "specimenCount": 1
                }
              ]
            }
            """.formatted(applicationIdOne)), 201);
        JsonNode registrationTwo = responseBody(postJson("/api/v1/specimens/register", USER_REGISTER, """
            {
              "applicationId": "%s",
              "printerCode": "P-01",
              "terminalCode": "OR-UNBOUND-02",
              "items": [
                {
                  "specimenNameStandardized": "Biopsy Tissue",
                  "specimenType": "ROUTINE",
                  "specimenSite": "Lung",
                  "collectionMode": "BIOPSY",
                  "containerName": "Specimen Bottle",
                  "containerCount": 1,
                  "specimenCount": 1
                }
              ]
            }
            """.formatted(applicationIdTwo)), 201);

        String specimenIdOne = registrationOne.path("specimens").get(0).path("id").asText();
        String specimenIdTwo = registrationTwo.path("specimens").get(0).path("id").asText();

        assertThat(registrationOne.path("labelPrintSuccess").asBoolean()).isFalse();
        assertThat(registrationOne.path("specimens").get(0).path("barcode").isNull()).isTrue();
        assertThat(registrationOne.path("specimens").get(0).path("barcodeBindingStatus").asText()).isEqualTo("UNBOUND");
        assertThat(registrationOne.path("specimens").get(0).path("labelPrintStatus").asText()).isEqualTo("PENDING");
        assertThat(querySingleString(
            """
                select barcode
                from specimens
                where id = :specimenId
                """,
            "specimenId",
            specimenIdOne)).isNull();
        assertThat(querySingleString(
            """
                select barcode
                from specimens
                where id = :specimenId
                """,
            "specimenId",
            specimenIdTwo)).isNull();

        JsonNode unboundList = responseBody(
            mockMvc.perform(authorized(get("/api/v1/specimens"), USER_REGISTER)
                .param("page", "1")
                .param("size", "20")
                .param("keyword", "APP-M2-UNBOUND")),
            200);
        assertThat(unboundList.path("total").asInt()).isEqualTo(2);
        assertThat(unboundList.path("summary").path("unboundCount").asInt()).isEqualTo(2);
        assertThat(unboundList.path("items").get(0).path("barcode").isNull()).isTrue();
        assertThat(unboundList.path("items").get(0).path("barcodeBindingStatus").asText()).isEqualTo("UNBOUND");

        responseBody(postJson("/api/v1/specimens/%s/barcode-binding".formatted(specimenIdOne), USER_REGISTER, """
            {
              "targetBarcode": "BC-M2-UNBOUND-001",
              "terminalCode": "OR-BIND-01"
            }
            """), 200);

        JsonNode boundList = responseBody(
            mockMvc.perform(authorized(get("/api/v1/specimens"), USER_REGISTER)
                .param("page", "1")
                .param("size", "20")
                .param("keyword", "APP-M2-UNBOUND")),
            200);
        assertThat(boundList.path("total").asInt()).isEqualTo(2);
        assertThat(boundList.path("summary").path("unboundCount").asInt()).isEqualTo(1);
        JsonNode boundItem = findSpecimenItem(boundList, specimenIdOne);
        assertThat(boundItem.path("barcode").asText()).isEqualTo("BC-M2-UNBOUND-001");
        assertThat(boundItem.path("barcodeBindingStatus").asText()).isEqualTo("BOUND");

        postJson("/api/v1/specimens/%s/barcode-binding".formatted(specimenIdTwo), USER_REGISTER, """
            {
              "targetBarcode": "BC-M2-UNBOUND-001",
              "terminalCode": "OR-BIND-02"
            }
            """)
            .andExpect(status().isConflict());
    }

    @Test
    void shouldRetryPendingLabelsAndListSpecimensForManagement() throws Exception {
        String applicationIdPrinted = createApplication("APP-M2-MGMT-001", "DEPT-MGMT", "Specimen Department");
        String applicationIdPending = createApplication("APP-M2-MGMT-002", "DEPT-MGMT", "Specimen Department");
        String applicationIdAbnormal = createApplication("APP-M2-MGMT-003", "DEPT-MGMT", "Specimen Department");

        registerSpecimens(
            applicationIdPrinted,
            USER_REGISTER,
            "P-01",
            "/api/v1/specimens/register",
            "BC-M2-MGMT-001");
        JsonNode pendingRegistration = registerSpecimens(
            applicationIdPending,
            USER_REGISTER,
            "P-01",
            "/api/v1/specimens/register",
            "BC-M2-MGMT-002");
        JsonNode abnormalRegistration = registerSpecimens(
            applicationIdAbnormal,
            USER_REGISTER,
            "FAIL",
            "/api/v1/specimens/register",
            "BC-M2-MGMT-003");

        String pendingBatchNo = pendingRegistration.path("labelPrintBatchNo").asText();
        String abnormalBatchNo = abnormalRegistration.path("labelPrintBatchNo").asText();

        jdbcTemplate.update(
            """
                update specimens
                set label_print_status = 'PENDING'
                where barcode = :barcode
                """,
            java.util.Map.of("barcode", "BC-M2-MGMT-002"));

        mockMvc.perform(authorized(post("/api/v1/specimens/label-batches/{batchNo}/retry", pendingBatchNo), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      
                      "printerCode": "P-02",
                      "terminalCode": "WARD-PENDING-01"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.labelPrintBatchNo").value(pendingBatchNo))
            .andExpect(jsonPath("$.data.retriedCount").value(1))
            .andExpect(jsonPath("$.data.successCount").value(1))
            .andExpect(jsonPath("$.data.failedCount").value(0))
            .andExpect(jsonPath("$.data.allSuccessful").value(true));

        jdbcTemplate.update(
            """
                update specimens
                set label_print_status = 'PENDING'
                where barcode = :barcode
                """,
            java.util.Map.of("barcode", "BC-M2-MGMT-002"));

        String today = LocalDate.now().toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(authorized(get("/api/v1/specimens"), USER_REGISTER)
                .param("page", "1")
                .param("size", "20")
                .param("keyword", "M2-MGMT")
                .param("departmentId", "DEPT-MGMT")
                .param("specimenStatus", "REGISTERED")
                .param("dateFrom", today)
                .param("dateTo", tomorrow))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(3))
            .andExpect(jsonPath("$.data.summary.totalCount").value(3))
            .andExpect(jsonPath("$.data.summary.labelPrintedCount").value(1))
            .andExpect(jsonPath("$.data.summary.pendingLabelCount").value(2))
            .andExpect(jsonPath("$.data.summary.abnormalCount").value(0))
            .andExpect(jsonPath("$.data.items[0].specimenId").isNotEmpty())
            .andExpect(jsonPath("$.data.items[0].applicationNo").value(org.hamcrest.Matchers.containsString("APP-M2-MGMT")))
            .andExpect(jsonPath("$.data.items[0].submittingDepartmentName").value("Specimen Department"));

        mockMvc.perform(authorized(get("/api/v1/specimens"), USER_REGISTER)
                .param("page", "1")
                .param("size", "20")
                .param("keyword", "M2-MGMT-003")
                .param("labelPrintStatus", "FAILED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].labelPrintBatchNo").value(abnormalBatchNo))
            .andExpect(jsonPath("$.data.items[0].labelPrintStatus").value("FAILED"));

        jdbcTemplate.update(
            """
                update specimens
                set specimen_status = 'REJECTED',
                    unqualified_reason = 'broken-container'
                where barcode = :barcode
                """,
            java.util.Map.of("barcode", "BC-M2-MGMT-003"));

        mockMvc.perform(authorized(get("/api/v1/specimens"), USER_REGISTER)
                .param("page", "1")
                .param("size", "20")
                .param("keyword", "M2-MGMT-003")
                .param("abnormalFlag", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.summary.totalCount").value(1))
            .andExpect(jsonPath("$.data.summary.labelPrintedCount").value(0))
            .andExpect(jsonPath("$.data.summary.pendingLabelCount").value(1))
            .andExpect(jsonPath("$.data.summary.abnormalCount").value(1))
            .andExpect(jsonPath("$.data.items[0].barcode").value("BC-M2-MGMT-003"))
            .andExpect(jsonPath("$.data.items[0].abnormalFlag").value(true))
            .andExpect(jsonPath("$.data.items[0].labelPrintBatchNo").value(abnormalBatchNo));
    }

    @Test
    void shouldLookupApplicationAndReloadLatestRegistrationResultFromApi() throws Exception {
        String applicationId = createApplication("APP-M2-COLLECT-LOOKUP-001");
        JsonNode failedRegistration = registerSpecimens(
            applicationId, USER_REGISTER, "FAIL", "/api/v1/specimens/register", "BC-COLLECT-LOOKUP-001");
        String batchNo = failedRegistration.path("labelPrintBatchNo").asText();

        mockMvc.perform(authorized(get("/api/v1/specimens/applications/lookup"), USER_REGISTER)
                .param("applicationNo", "APP-M2-COLLECT-LOOKUP-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(applicationId))
            .andExpect(jsonPath("$.data.applicationNo").value("APP-M2-COLLECT-LOOKUP-001"))
            .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.data.registeredSpecimenCount").value(1))
            .andExpect(jsonPath("$.data.latestLabelPrintStatus").value("FAILED"));

        JsonNode latestRegistration = responseBody(
            mockMvc.perform(authorized(get("/api/v1/specimens/applications/{applicationId}/latest-registration", applicationId), USER_REGISTER)),
            200);

        assertThat(latestRegistration.path("applicationId").asText()).isEqualTo(applicationId);
        assertThat(latestRegistration.path("labelPrintBatchNo").asText()).isEqualTo(batchNo);
        assertThat(latestRegistration.path("labelPrintSuccess").asBoolean()).isFalse();
        assertThat(latestRegistration.path("registrationSnapshot").path("collectionScene").asText()).isEqualTo("OPERATING_ROOM");
        assertThat(latestRegistration.path("registrationSnapshot").path("operatorUserId").asText()).isEqualTo(USER_REGISTER);
        assertThat(latestRegistration.path("registrationSnapshot").path("operatorName").asText()).isNotBlank();
        assertThat(latestRegistration.path("registrationSnapshot").path("printerCode").asText()).isEqualTo("FAIL");
        assertThat(latestRegistration.path("registrationSnapshot").path("terminalCode").asText()).isEqualTo("OR-01");
        assertThat(latestRegistration.path("specimens").get(0).path("barcode").asText()).isEqualTo("BC-COLLECT-LOOKUP-001");
        assertThat(latestRegistration.path("specimens").get(0).path("labelPrintStatus").asText()).isEqualTo("FAILED");

        jdbcTemplate.update(
            """
                update specimen_collection_records
                set printer_code = null
                where application_id = :applicationId
                  and label_print_batch_no = :batchNo
                """,
            java.util.Map.of("applicationId", applicationId, "batchNo", batchNo));

        JsonNode latestRegistrationWithoutPrinter = responseBody(
            mockMvc.perform(authorized(get("/api/v1/specimens/applications/{applicationId}/latest-registration", applicationId), USER_REGISTER)),
            200);
        assertThat(latestRegistrationWithoutPrinter.path("registrationSnapshot").path("printerCode").isNull()).isTrue();

        mockMvc.perform(authorized(post("/api/v1/specimens/label-batches/{batchNo}/retry", batchNo), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      
                      "printerCode": "P-01",
                      "terminalCode": "WARD-LOOKUP-02"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.allSuccessful").value(true));

        mockMvc.perform(authorized(get("/api/v1/specimens/applications/{applicationId}/latest-registration", applicationId), USER_REGISTER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.applicationId").value(applicationId))
            .andExpect(jsonPath("$.data.labelPrintBatchNo").value(batchNo))
            .andExpect(jsonPath("$.data.labelPrintSuccess").value(true))
            .andExpect(jsonPath("$.data.specimens[0].labelPrintStatus").value("SUCCESS"));
    }

    @Test
    void shouldPersistCurrentLoginAsOperatorIdentity() throws Exception {
        String applicationId = createApplication("APP-M2-COLLECT-OPERATOR-001");
        String expectedOperatorName = userDisplayName(USER_REGISTER);

        JsonNode registration = responseBody(postJson("/api/v1/specimens/register", USER_REGISTER, """
            {
              "applicationId": "%s",
              "printerCode": "FAIL",
              
              "terminalCode": "WARD-OP-01",
              "items": [
                {
                  "specimenNameStandardized": "Biopsy Tissue",
                  "specimenType": "ROUTINE",
                  "specimenSite": "Lung",
                  "collectionMode": "BIOPSY",
                  "containerName": "Specimen Bottle",
                  "containerCount": 1,
                  "specimenCount": 1,
                  "barcode": "BC-COLLECT-OPERATOR-001"
                }
              ]
            }
            """.formatted(applicationId)), 201);

        String specimenId = registration.path("specimens").get(0).path("id").asText();
        String batchNo = registration.path("labelPrintBatchNo").asText();

        assertThat(querySingleString(
            """
                select registered_by_user_id
                from specimens
                where id = :specimenId
                """,
            "specimenId",
            specimenId)).isEqualTo(USER_REGISTER);
        assertThat(querySingleString(
            """
                select registered_by_name
                from specimens
                where id = :specimenId
                """,
            "specimenId",
            specimenId)).isEqualTo(expectedOperatorName);

        mockMvc.perform(authorized(post("/api/v1/specimens/label-batches/{batchNo}/retry", batchNo), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      
                      "printerCode": "P-01",
                      "terminalCode": "WARD-OP-02"
                    }
                    """))
            .andExpect(status().isOk());

        assertThat(querySingleString(
            """
                select operator_user_id
                from workflow_events
                where specimen_id = :specimenId
                  and node_code = 'LABEL_PRINT'
                  and event_type = 'RETRY'
                order by event_time desc, created_at desc, id desc
                fetch next 1 rows only
                """,
            "specimenId",
            specimenId)).isEqualTo(USER_REGISTER);
        assertThat(querySingleString(
            """
                select operator_name
                from workflow_events
                where specimen_id = :specimenId
                  and node_code = 'LABEL_PRINT'
                  and event_type = 'RETRY'
                order by event_time desc, created_at desc, id desc
                fetch next 1 rows only
                """,
            "specimenId",
            specimenId)).isEqualTo(expectedOperatorName);
    }

    @Test
    void shouldRejectLegacyOperatorFieldsOnLabelRetryRequest() throws Exception {
        String applicationId = createApplication("APP-M2-COLLECT-LEGACY-001");
        JsonNode registration = responseBody(postJson("/api/v1/specimen-collections", USER_REGISTER, """
            {
              "applicationId": "%s",
              "printerCode": "FAIL",
              "collectionScene": "WARD",
              "terminalCode": "WARD-LEGACY-01",
              "items": [
                {
                  "specimenNameStandardized": "Biopsy Tissue",
                  "specimenType": "ROUTINE",
                  "specimenSite": "Lung",
                  "collectionMode": "BIOPSY",
                  "containerName": "Specimen Bottle",
                  "containerCount": 1,
                  "specimenCount": 1,
                  "barcode": "BC-COLLECT-LEGACY-001"
                }
              ]
            }
            """.formatted(applicationId)), 201);
        String batchNo = registration.path("labelPrintBatchNo").asText();

        mockMvc.perform(authorized(post("/api/v1/specimens/label-batches/{batchNo}/retry", batchNo), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "operatorName": "legacy-user",
                      "printerCode": "P-01",
                      "terminalCode": "WARD-LEGACY-02"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("operatorName")));
    }

    @Test
    void shouldRejectInvalidCollectionRequest() throws Exception {
        String applicationId = createApplication("APP-M2-COLLECT-003");

        mockMvc.perform(authorized(post("/api/v1/specimen-collections"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationId": "%s",
                      
                      "items": []
                    }
                    """.formatted(applicationId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private JsonNode findSpecimenItem(JsonNode page, String specimenId) {
        for (JsonNode item : page.path("items")) {
            if (specimenId.equals(item.path("specimenId").asText())) {
                return item;
            }
        }
        throw new AssertionError("Specimen item not found: " + specimenId);
    }
}
