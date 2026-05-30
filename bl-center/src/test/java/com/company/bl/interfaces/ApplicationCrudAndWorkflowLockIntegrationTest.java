package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class ApplicationCrudAndWorkflowLockIntegrationTest extends AbstractApplicationControllerIntegrationTest {

    @Test
    void shouldCreateApplicationWhenRequestIsValid() throws Exception {
        String applicationNo = "APP-1001-" + System.nanoTime();
        mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "%s",
                      "applicationType": "ROUTINE",
                      "patientId": "P-1001",
                      "applicationFormStatus": "PENDING",
                      "applicationDate": "2026-05-21",
                      "submissionDate": "2026-05-22",
                      "clinicalDiagnosis": "test diagnosis"
                    }
                    """.formatted(applicationNo)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("X-Trace-Id"))
            .andExpect(jsonPath("$.code", is("SUCCESS")))
            .andExpect(jsonPath("$.traceId", notNullValue()))
            .andExpect(jsonPath("$.data.id", notNullValue()));
    }

    @Test
    void shouldReturnValidationErrorWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
            .andExpect(jsonPath("$.message", anyOf(
                containsString("申请单号长度不能超过64个字符"),
                containsString("Application number must not exceed 64 characters")
            )))
            .andExpect(jsonPath("$.message", anyOf(
                containsString("申请类型不能为空"),
                containsString("Application type must not be blank")
            )))
            .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void shouldReturnValidationErrorWhenRequestBodyIsMissing() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
            .andExpect(jsonPath("$.message", anyOf(
                is("请求体不能为空，且必须是合法的 JSON"),
                is("Request body is required and must be valid JSON")
            )))
            .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void shouldUpdateApplicationBeforeDownstreamWorkflowStarts() throws Exception {
        JsonNode created = responseData(mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-UPDATE-001",
                      "applicationType": "ROUTINE",
                      "patientId": "P-UPDATE-001",
                      "patientName": "Patient Before Update",
                      "applicationDate": "2026-05-20",
                      "submissionDate": "2026-05-21",
                      "applicationFormStatus": "PENDING",
                      "clinicalDiagnosis": "before update"
                    }
                    """)), 201);
        String applicationId = created.path("id").asText();

        mockMvc.perform(authorized(patch("/api/v1/applications/{id}", applicationId), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-UPDATE-001-R",
                      "applicationType": "FROZEN",
                      "patientId": "P-UPDATE-001",
                      "patientName": "Patient After Update",
                      "applicationDate": "2026-05-22",
                      "submissionDate": "2026-05-23",
                      "applicationFormStatus": "UPLOADED",
                      "clinicalDiagnosis": "after update"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(applicationId));

        mockMvc.perform(authorized(get("/api/v1/applications/{id}", applicationId), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.applicationNo").value("APP-UPDATE-001-R"))
            .andExpect(jsonPath("$.data.patientName").value("Patient After Update"))
            .andExpect(jsonPath("$.data.applicationType").value("FROZEN"))
            .andExpect(jsonPath("$.data.applicationFormStatus").value("UPLOADED"))
            .andExpect(jsonPath("$.data.editable").value(true))
            .andExpect(jsonPath("$.data.deletable").value(true))
            .andExpect(jsonPath("$.data.voided").value(false));
    }

    @Test
    void shouldVoidApplicationAndHideItFromDefaultList() throws Exception {
        JsonNode created = responseData(mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-VOID-001",
                      "applicationType": "ROUTINE",
                      "patientId": "P-VOID-001",
                      "patientName": "Patient Void",
                      "submittingDepartmentId": "DEPT-VOID",
                      "submittingDepartmentName": "Void Department",
                      "submittingDoctorUserId": "DOC-VOID-001",
                      "submittingDoctorName": "Dr Void",
                      "clinicalDiagnosis": "void diagnosis",
                      "specimenSite": "Lung"
                    }
                    """)), 201);
        String applicationId = created.path("id").asText();

        mockMvc.perform(authorized(delete("/api/v1/applications/{id}", applicationId), USER_REGISTER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(applicationId));

        mockMvc.perform(authorized(get("/api/v1/applications"), USER_TRACKING)
                .param("page", "1")
                .param("size", "20")
                .param("applicationNo", "APP-VOID-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(authorized(get("/api/v1/applications"), USER_TRACKING)
                .param("page", "1")
                .param("size", "20")
                .param("applicationFormStatus", "VOIDED")
                .param("applicationNo", "APP-VOID-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].status").value("VOIDED"))
            .andExpect(jsonPath("$.data.items[0].currentNode").value("VOIDED"))
            .andExpect(jsonPath("$.data.items[0].editable").value(false))
            .andExpect(jsonPath("$.data.items[0].deletable").value(false))
            .andExpect(jsonPath("$.data.items[0].voided").value(true))
            .andExpect(jsonPath("$.data.items[0].operationDisabledReason").isNotEmpty());
    }

    @Test
    void shouldRejectUpdateAndVoidAfterDownstreamWorkflowStarts() throws Exception {
        JsonNode created = responseData(mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-DOWNSTREAM-LOCK-001",
                      "applicationType": "ROUTINE",
                      "patientId": "P-DOWNSTREAM-LOCK",
                      "patientName": "Patient Locked",
                      "clinicalDiagnosis": "locked diagnosis"
                    }
                    """)), 201);
        String applicationId = created.path("id").asText();

        responseData(mockMvc.perform(authorized(post("/api/v1/specimens/register"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationId": "%s",
                      "operatorName": "nurse-lock",
                      "items": [
                        {
                          "specimenNameStandardized": "Thyroid tissue",
                          "specimenType": "Tissue",
                          "specimenSite": "Thyroid",
                          "collectionMode": "SURGERY",
                          "containerName": "Specimen Bottle",
                          "containerCount": 1,
                          "specimenCount": 1,
                          "barcode": "BC-DOWNSTREAM-LOCK-001"
                        }
                      ]
                    }
                    """.formatted(applicationId))), 201);

        mockMvc.perform(authorized(post("/api/v1/specimen-verifications/start"), USER_FIXATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "specimenBarcode": "BC-DOWNSTREAM-LOCK-001",
                      "operatorName": "nurse-lock"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(post("/api/v1/specimen-verifications/complete"), USER_FIXATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "specimenBarcode": "BC-DOWNSTREAM-LOCK-001",
                      "operatorName": "nurse-lock"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(post("/api/v1/specimen-fixations/start"), USER_FIXATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "specimenBarcode": "BC-DOWNSTREAM-LOCK-001",
                      "fixationLiquidType": "FORMALIN",
                      "operatorName": "nurse-lock"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(patch("/api/v1/applications/{id}", applicationId), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-DOWNSTREAM-LOCK-001-R",
                      "applicationType": "ROUTINE",
                      "patientId": "P-DOWNSTREAM-LOCK",
                      "patientName": "Patient Locked",
                      "submittingDepartmentId": "DEPT-LOCK",
                      "submittingDepartmentName": "Lock Department",
                      "submittingDoctorUserId": "DOC-LOCK-001",
                      "submittingDoctorName": "Dr Lock",
                      "clinicalDiagnosis": "locked diagnosis",
                      "specimenSite": "Thyroid"
                    }
                    """))
            .andExpect(status().isConflict());

        mockMvc.perform(authorized(delete("/api/v1/applications/{id}", applicationId), USER_REGISTER))
            .andExpect(status().isConflict());
    }

    @Test
    void shouldGenerateApplicationNumberWhenMissing() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationType": "ROUTINE",
                      "patientId": "P-AUTO-001",
                      "clinicalDiagnosis": "auto no"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code", is("SUCCESS")))
            .andExpect(jsonPath("$.data.id", notNullValue()));
    }
}
