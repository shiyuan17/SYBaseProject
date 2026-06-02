package com.company.bl.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

abstract class AbstractTechnicalWorkflowIntegrationTest extends AbstractSpecimenWorkflowIntegrationTest {

    protected static final String USER_M3_GROSSING = "USER_M3_GROSSING";
    protected static final String USER_M3_DEHYDRATION = "USER_M3_DEHYDRATION";
    protected static final String USER_M3_EMBEDDING = "USER_M3_EMBEDDING";
    protected static final String USER_M3_SLICING = "USER_M3_SLICING";
    protected static final String USER_M3_STAINING = "USER_M3_STAINING";
    protected static final String USER_M3_REWORK = "USER_M3_REWORK";
    protected static final String USER_M3_TRACKING = "USER_M3_TRACKING";
    protected static final String USER_M1_ADMIN = "USER_M1_ADMIN";

    @Autowired
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    protected TechnicalCaseContext receiveCaseAndGetGrossingTask(String applicationNo, String barcode) throws Exception {
        return receiveCaseAndGetGrossingTask(applicationNo, barcode, "DEPT-OR", "OR");
    }

    protected TechnicalCaseContext receiveCaseAndGetGrossingTask(String applicationNo,
                                                                 String barcode,
                                                                 String submittingDepartmentId,
                                                                 String submittingDepartmentName) throws Exception {
        TechnicalCaseContext registrationContext =
            receiveCaseAndGetPendingRegistration(applicationNo, barcode, submittingDepartmentId, submittingDepartmentName);
        completeTechnicalSpecimenRegistration(registrationContext.caseId(), "auto registration");

        JsonNode pendingTasks = listPendingTasks("GROSSING", registrationContext.pathologyNo(), USER_M3_GROSSING);
        String grossingTaskId = pendingTasks.path("items").get(0).path("id").asText();

        return new TechnicalCaseContext(
            registrationContext.applicationId(),
            registrationContext.caseId(),
            registrationContext.pathologyNo(),
            registrationContext.specimenId(),
            registrationContext.barcode(),
            grossingTaskId);
    }

    protected TechnicalCaseContext receiveCaseAndGetPendingRegistration(String applicationNo,
                                                                        String barcode) throws Exception {
        return receiveCaseAndGetPendingRegistration(applicationNo, barcode, "DEPT-OR", "OR");
    }

    protected TechnicalCaseContext receiveCaseAndGetPendingRegistration(String applicationNo,
                                                                        String barcode,
                                                                        String submittingDepartmentId,
                                                                        String submittingDepartmentName) throws Exception {
        String applicationId = createApplication(applicationNo, submittingDepartmentId, submittingDepartmentName);
        JsonNode registration = registerSpecimens(applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", barcode);
        JsonNode specimen = registration.path("specimens").get(0);
        String specimenId = specimen.path("id").asText();
        String actualBarcode = specimen.path("barcode").asText();

        completeFixation(actualBarcode);

        JsonNode receipt = responseBody(postJson("/api/v1/specimen-receipts/by-barcodes", USER_RECEIVE, """
            {
              "receivedByName": "receiver-m3",
              "terminalCode": "T-M3-01",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1,
                  "qualityCheckResult": "PASSED"
                }
              ]
            }
            """.formatted(actualBarcode)), 200);

        String caseId = receipt.path("caseId").asText();
        String pathologyNo = receipt.path("pathologyNo").asText();
        return new TechnicalCaseContext(applicationId, caseId, pathologyNo, specimenId, actualBarcode, null);
    }

    protected JsonNode listPendingTechnicalSpecimenRegistrations(String keyword, String userId) throws Exception {
        return listPendingTechnicalSpecimenRegistrations(keyword, null, null, userId);
    }

    protected JsonNode listPendingTechnicalSpecimenRegistrations(String keyword,
                                                                 String receivedFrom,
                                                                 String receivedTo,
                                                                 String userId) throws Exception {
        ResultActions action = mockMvc.perform(authorized(get("/api/v1/technical-specimen-registrations/pending"), userId)
            .param("page", "1")
            .param("size", "20")
            .param("keyword", keyword == null ? "" : keyword)
            .param("receivedFrom", receivedFrom == null ? "" : receivedFrom)
            .param("receivedTo", receivedTo == null ? "" : receivedTo));
        return responseBody(action, 200);
    }

    protected JsonNode technicalSpecimenRegistrationWorkspace(String caseId, String userId) throws Exception {
        return responseBody(mockMvc.perform(authorized(
            get("/api/v1/technical-specimen-registrations/{caseId}/workspace", caseId),
            userId)), 200);
    }

    protected JsonNode technicalSpecimenRegistrationApplicationWorkbench(String caseId, String userId) throws Exception {
        return responseBody(mockMvc.perform(authorized(
            get("/api/v1/technical-specimen-registrations/{caseId}/application-workbench", caseId),
            userId)), 200);
    }

    protected JsonNode saveTechnicalSpecimenRegistrationApplicationWorkbenchPatientInfo(
        String caseId,
        String userId,
        String content
    ) throws Exception {
        return responseBody(mockMvc.perform(authorized(
                patch("/api/v1/technical-specimen-registrations/{caseId}/application-workbench/patient-info", caseId),
                userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content)), 200);
    }

    protected JsonNode completeTechnicalSpecimenRegistration(String caseId, String remarks) throws Exception {
        return responseBody(postJson("/api/v1/technical-specimen-registrations/%s/complete".formatted(caseId), USER_RECEIVE, """
            {
              "terminalCode": "T-M3-REG",
              "remarks": %s
            }
            """.formatted(remarks == null ? "null" : "\"" + remarks + "\"")), 200);
    }

    protected JsonNode saveTechnicalSpecimenRegistrationMaterials(String caseId, String userId, String content) throws Exception {
        return responseBody(mockMvc.perform(authorized(
                put("/api/v1/technical-specimen-registrations/{caseId}/materials", caseId),
                userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content)), 200);
    }

    protected JsonNode saveTechnicalSpecimenRegistrationDetailSections(String caseId, String userId, String content) throws Exception {
        return responseBody(mockMvc.perform(authorized(
                patch("/api/v1/technical-specimen-registrations/{caseId}/detail-sections", caseId),
                userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content)), 200);
    }

    protected JsonNode deleteTechnicalSpecimenRegistrationMediaAsset(String caseId, String assetId, String userId) throws Exception {
        return responseBody(mockMvc.perform(authorized(
            delete("/api/v1/technical-specimen-registrations/{caseId}/media-assets/{assetId}", caseId, assetId),
            userId)), 200);
    }

    protected JsonNode listPendingTasks(String taskType, String pathologyNo, String userId) throws Exception {
        ResultActions action = mockMvc.perform(authorized(get("/api/v1/technical-tasks/pending"), userId)
            .param("page", "1")
            .param("size", "20")
            .param("taskType", taskType)
            .param("pathologyNo", pathologyNo));
        return responseBody(action, 200);
    }

    protected JsonNode technicalTracking(String caseIdentifier, String userId) throws Exception {
        return responseBody(mockMvc.perform(authorized(get("/api/v1/pathology-cases/{id}/technical-tracking", caseIdentifier), userId)), 200);
    }

    protected String querySamplingTemplateId(String caseId, String specimenId) {
        return namedParameterJdbcTemplate.queryForObject("""
            select sampling_template_id
            from samplings
            where case_id = :caseId
              and specimen_id = :specimenId
            order by created_at desc
            limit 1
            """, Map.of("caseId", caseId, "specimenId", specimenId), String.class);
    }

    protected long countNotifications(String userId, String topicCode, String payloadFragment) {
        Long count = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from user_notifications
            where user_id = :userId
              and topic_code = :topicCode
              and action_payload_json like :payloadPattern
            """, Map.of(
            "userId", userId,
            "topicCode", topicCode,
            "payloadPattern", "%" + payloadFragment + "%"
        ), Long.class);
        return count == null ? 0L : count;
    }

    protected record TechnicalCaseContext(
        String applicationId,
        String caseId,
        String pathologyNo,
        String specimenId,
        String barcode,
        String grossingTaskId
    ) {
    }
}
