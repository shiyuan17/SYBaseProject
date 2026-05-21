package com.company.bl.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

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
                  "containerCount": 1
                }
              ]
            }
            """.formatted(actualBarcode)), 200);

        String caseId = receipt.path("caseId").asText();
        String pathologyNo = receipt.path("pathologyNo").asText();
        JsonNode pendingTasks = listPendingTasks("GROSSING", pathologyNo, USER_M3_GROSSING);
        String grossingTaskId = pendingTasks.path("items").get(0).path("id").asText();

        return new TechnicalCaseContext(applicationId, caseId, pathologyNo, specimenId, actualBarcode, grossingTaskId);
    }

    protected JsonNode listPendingTasks(String taskType, String pathologyNo, String userId) throws Exception {
        ResultActions action = mockMvc.perform(authorized(get("/api/v1/technical-tasks/pending"), userId)
            .param("page", "1")
            .param("size", "20")
            .param("taskType", taskType)
            .param("pathologyNo", pathologyNo));
        return responseBody(action, 200);
    }

    protected JsonNode technicalTracking(String caseId, String userId) throws Exception {
        return responseBody(mockMvc.perform(authorized(get("/api/v1/pathology-cases/{id}/technical-tracking", caseId), userId)), 200);
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
