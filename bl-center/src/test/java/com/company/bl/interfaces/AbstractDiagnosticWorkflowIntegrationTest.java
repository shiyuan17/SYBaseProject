package com.company.bl.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

abstract class AbstractDiagnosticWorkflowIntegrationTest extends AbstractTechnicalWorkflowIntegrationTest {

    protected static final String USER_M4_ASSIGN = "USER_M4_ASSIGN";
    protected static final String USER_M4_DIAGNOSIS = "USER_M4_DIAGNOSIS";
    protected static final String USER_M4_REVIEW = "USER_M4_REVIEW";
    protected static final String USER_M4_SIGN = "USER_M4_SIGN";
    protected static final String USER_M4_TRACKING = "USER_M4_TRACKING";
    protected static final String USER_M4_NO_PERMISSION = "USER_M4_NO_PERMISSION";

    protected JsonNode listPendingDiagnosticTasks(String pathologyNo, String userId) throws Exception {
        ResultActions action = mockMvc.perform(authorized(get("/api/v1/diagnostic-tasks/pending"), userId)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", pathologyNo));
        return responseBody(action, 200);
    }

    protected JsonNode diagnosticWorkbench(String caseId, String userId) throws Exception {
        return responseBody(mockMvc.perform(authorized(get("/api/v1/pathology-cases/{id}/diagnostic-workbench", caseId), userId)), 200);
    }

    protected JsonNode reportTracking(String caseId, String userId) throws Exception {
        return responseBody(mockMvc.perform(authorized(get("/api/v1/pathology-cases/{id}/report-tracking", caseId), userId)), 200);
    }
}
