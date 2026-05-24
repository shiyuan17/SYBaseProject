package com.company.bl.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class AbstractSpecimenWorkflowIntegrationTest extends AuthenticatedWebIntegrationTest {

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

    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;

    protected String createApplication(String applicationNo) throws Exception {
        return createApplication(applicationNo, "DEPT-OR", "OR");
    }

    protected String createApplication(String applicationNo, String submittingDepartmentId, String submittingDepartmentName) throws Exception {
        JsonNode data = responseBody(postJson("/api/v1/applications", USER_REGISTER, """
            {
              "applicationNo": "%s",
              "patientId": "P-001",
              "patientName": "Patient A",
              "applicationType": "ROUTINE",
              "status": "DRAFT",
              "submittingDepartmentId": "%s",
              "submittingDepartmentName": "%s",
              "submittingDoctorUserId": "DOC-001",
              "submittingDoctorName": "Dr A",
              "clinicalDiagnosis": "Papillary thyroid carcinoma",
              "specimenSite": "Thyroid"
            }
            """.formatted(applicationNo, submittingDepartmentId, submittingDepartmentName)), 201);
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
                  "containerName": "Specimen Bottle",
                  "containerCount": 1,
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
        return mockMvc.perform(authorized(post(path), userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content));
    }

    protected String querySingleString(String sql, String parameterName, String parameterValue) {
        return jdbcTemplate.queryForObject(
            sql,
            Map.of(parameterName, parameterValue),
            String.class);
    }

    protected String userLoginName(String userId) {
        return querySingleString(
            """
                select login_name
                from users
                where id = :userId
                """,
            "userId",
            userId);
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
