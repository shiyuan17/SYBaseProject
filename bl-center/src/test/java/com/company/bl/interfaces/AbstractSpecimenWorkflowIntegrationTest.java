package com.company.bl.interfaces;

import com.company.bl.interfaces.auth.ApiPermissionContext;
import com.company.common.test.BaseWebIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class AbstractSpecimenWorkflowIntegrationTest extends BaseWebIntegrationTest {

    protected static final String USER_ID_HEADER = ApiPermissionContext.USER_ID_HEADER;

    protected static final String USER_ADMIN = "USER_M2_ADMIN";
    protected static final String USER_REGISTER = "USER_M2_REGISTER";
    protected static final String USER_FIXATION = "USER_M2_FIXATION";
    protected static final String USER_TRANSPORT = "USER_M2_TRANSPORT";
    protected static final String USER_RECEIVE = "USER_M2_RECEIVE";
    protected static final String USER_TRACKING = "USER_M2_TRACKING";
    protected static final String USER_IMPORT = "USER_M2_IMPORT";
    protected static final String USER_NO_PERMISSION = "USER_M2_NO_PERMISSION";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String createApplication(String applicationNo) throws Exception {
        JsonNode data = createdBody("/api/v1/applications", """
            {
              "applicationNo": "%s",
              "patientId": "P-001",
              "patientName": "Patient A",
              "applicationType": "ROUTINE",
              "status": "DRAFT",
              "submittingDepartmentId": "DEPT-OR",
              "submittingDepartmentName": "OR",
              "submittingDoctorUserId": "DOC-001",
              "submittingDoctorName": "Dr A"
            }
            """.formatted(applicationNo));
        return data.path("id").asText();
    }

    protected JsonNode registerSpecimens(String applicationId,
                                         String userId,
                                         String printerCode,
                                         String path,
                                         String... barcodes) throws Exception {
        return responseBody(postJson(path, userId, registerSpecimenPayload(applicationId, printerCode, barcodes)), 201);
    }

    protected void completeFixation(String barcode) throws Exception {
        postJson("/api/v1/specimen-fixations/start", USER_FIXATION, """
            {
              "specimenBarcode": "%s",
              "fixationLiquidType": "FORMALIN",
              "operatorName": "nurse-b"
            }
            """.formatted(barcode))
            .andExpect(status().isOk());

        postJson("/api/v1/specimen-fixations/complete", USER_FIXATION, """
            {
              "specimenBarcode": "%s",
              "fixationLiquidType": "FORMALIN",
              "operatorName": "nurse-b"
            }
            """.formatted(barcode))
            .andExpect(status().isOk());
    }

    protected JsonNode createTransportOrder(String applicationId, String... barcodes) throws Exception {
        String joined = java.util.Arrays.stream(barcodes)
            .map(code -> "\"" + code + "\"")
            .reduce((left, right) -> left + "," + right)
            .orElseThrow();
        return responseBody(postJson("/api/v1/transport-orders", USER_TRANSPORT, """
            {
              "applicationId": "%s",
              "specimenBarcodes": [%s],
              "handoverUserName": "handover-a",
              "handoverDepartmentId": "DEPT-OR",
              "handoverDepartmentName": "OR",
              "receiverDepartmentId": "DEPT-PATH",
              "receiverDepartmentName": "Pathology",
              "terminalCode": "OR-02"
            }
            """.formatted(applicationId, joined)), 201);
    }

    protected String registerSpecimenPayload(String applicationId, String printerCode, String... barcodes) {
        String items = java.util.Arrays.stream(barcodes)
            .map(barcode -> """
                {
                  "specimenNameStandardized": "Thyroid Tissue",
                  "specimenType": "ROUTINE",
                  "specimenSite": "Thyroid",
                  "collectionMode": "SURGERY",
                  "specimenCount": 1,
                  "barcode": "%s"
                }
                """.formatted(barcode))
            .reduce((left, right) -> left + "," + right)
            .orElseThrow();
        return """
            {
              "applicationId": "%s",
              "printerCode": "%s",
              "operatorName": "nurse-a",
              "terminalCode": "OR-01",
              "items": [%s]
            }
            """.formatted(applicationId, printerCode, items);
    }

    protected ResultActions postJson(String path, String userId, String content) throws Exception {
        return mockMvc.perform(post(path)
            .header(USER_ID_HEADER, userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content));
    }

    protected JsonNode createdBody(String path, String content) throws Exception {
        return responseBody(mockMvc.perform(post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content)), 201);
    }

    protected JsonNode responseBody(ResultActions resultActions, int expectedStatus) throws Exception {
        String response = resultActions
            .andExpect(status().is(expectedStatus))
            .andReturn()
            .getResponse()
            .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        assertThat(root.path("code").asText()).isEqualTo("SUCCESS");
        return root.path("data");
    }
}
