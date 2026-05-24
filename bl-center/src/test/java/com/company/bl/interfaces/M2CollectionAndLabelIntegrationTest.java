package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
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
              "operatorName": "register-user",
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
                      "operatorName": "register-user",
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
                      "operatorName": "register-user",
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
                      "operatorName": "register-user",
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
                      "operatorName": "register-user",
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

        mockMvc.perform(authorized(get("/api/v1/specimens/applications/{applicationId}/latest-registration", applicationId), USER_REGISTER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.applicationId").value(applicationId))
            .andExpect(jsonPath("$.data.labelPrintBatchNo").value(batchNo))
            .andExpect(jsonPath("$.data.labelPrintSuccess").value(false))
            .andExpect(jsonPath("$.data.specimens[0].barcode").value("BC-COLLECT-LOOKUP-001"))
            .andExpect(jsonPath("$.data.specimens[0].labelPrintStatus").value("FAILED"));

        mockMvc.perform(authorized(post("/api/v1/specimens/label-batches/{batchNo}/retry", batchNo), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "operatorName": "register-user",
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
        String expectedLoginName = userLoginName(USER_REGISTER);

        JsonNode registration = responseBody(postJson("/api/v1/specimens/register", USER_REGISTER, """
            {
              "applicationId": "%s",
              "printerCode": "FAIL",
              "operatorName": "forged-register-name",
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
            specimenId)).isEqualTo(expectedLoginName);

        mockMvc.perform(authorized(post("/api/v1/specimens/label-batches/{batchNo}/retry", batchNo), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "operatorName": "forged-retry-name",
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
            specimenId)).isEqualTo(expectedLoginName);
    }

    @Test
    void shouldRejectInvalidCollectionRequest() throws Exception {
        String applicationId = createApplication("APP-M2-COLLECT-003");

        mockMvc.perform(authorized(post("/api/v1/specimen-collections"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationId": "%s",
                      "operatorName": "register-user",
                      "items": []
                    }
                    """.formatted(applicationId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
