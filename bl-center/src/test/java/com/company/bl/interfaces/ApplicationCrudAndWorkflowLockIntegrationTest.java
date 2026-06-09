package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.not;
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

    @BeforeEach
    void seedPatients() {
        insertPatient("P-1001", "P-1001", "P-1001", "P-1001", "Patient 1001");
        insertPatient("P-UPDATE-001", "P-UPDATE-001", "P-UPDATE-001", "P-UPDATE-001", "Patient Update");
        insertPatient("P-VOID-001", "P-VOID-001", "P-VOID-001", "P-VOID-001", "Patient Void");
        insertPatient(
            "P-DOWNSTREAM-LOCK",
            "P-DOWNSTREAM-LOCK",
            "P-DOWNSTREAM-LOCK",
            "P-DOWNSTREAM-LOCK",
            "Patient Locked");
        insertPatient("P-AUTO-001", "P-AUTO-001", "P-AUTO-001", "P-AUTO-001", "Patient Auto");
    }

    private void insertPatient(
        String id,
        String patientNo,
        String inpatientNo,
        String outpatientNo,
        String name
    ) {
        jdbcTemplate.getJdbcOperations().execute("""
                create table if not exists patients (
                    id varchar(64) primary key,
                    patient_no varchar(64),
                    name varchar(100),
                    gender varchar(16),
                    age varchar(32),
                    inpatient_no varchar(64),
                    outpatient_no varchar(64),
                    created_at timestamp,
                    updated_at timestamp
                )
                """);
        jdbcTemplate.update(
            "delete from patients where id = :id",
            new MapSqlParameterSource().addValue("id", id));
        jdbcTemplate.update("""
                insert into patients
                    (id, patient_no, name, gender, age, inpatient_no, outpatient_no, created_at, updated_at)
                values
                    (:id, :patientNo, :name, 'M', '45', :inpatientNo, :outpatientNo, current_timestamp, current_timestamp)
                """,
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("patientNo", patientNo)
                .addValue("name", name)
                .addValue("inpatientNo", inpatientNo)
                .addValue("outpatientNo", outpatientNo));
    }

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
    void shouldResolvePatientIdentifierToPatientPrimaryKeyWhenCreatingApplication() throws Exception {
        insertPatient(
            "PATIENT-LINK-001",
            "PATIENT-NO-001",
            "INPATIENT-001",
            "OUTPATIENT-001",
            "关联患者");

        JsonNode created = responseData(mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-LINK-%s",
                      "applicationType": "ROUTINE",
                      "patientId": "PATIENT-NO-001",
                      "patientName": "关联患者",
                      "applicationDate": "2026-05-21",
                      "submissionDate": "2026-05-22",
                      "applicationFormStatus": "PENDING",
                      "clinicalDiagnosis": "identifier link"
                    }
                    """.formatted(System.nanoTime()))), 201);
        String applicationId = created.path("id").asText();

        mockMvc.perform(authorized(get("/api/v1/applications/{id}", applicationId), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.patientId").value("PATIENT-LINK-001"))
            .andExpect(jsonPath("$.data.patientIdentifier").value("PATIENT-NO-001"))
            .andExpect(jsonPath("$.data.patientName").value("关联患者"));
    }

    @Test
    void shouldAutoCreatePatientWhenIdentifierDoesNotExist() throws Exception {
        JsonNode created = responseData(mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-AUTO-PATIENT-%s",
                      "applicationType": "ROUTINE",
                      "patientId": "PATIENT-NO-AUTO-001",
                      "patientName": "自动建档患者",
                      "patientGender": "F",
                      "patientAge": "30",
                      "applicationDate": "2026-05-21",
                      "submissionDate": "2026-05-22",
                      "applicationFormStatus": "PENDING",
                      "clinicalDiagnosis": "auto create patient"
                    }
                    """.formatted(System.nanoTime()))), 201);
        String applicationId = created.path("id").asText();

        mockMvc.perform(authorized(get("/api/v1/applications/{id}", applicationId), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.patientId", not("PATIENT-NO-AUTO-001")))
            .andExpect(jsonPath("$.data.patientIdentifier").value("PATIENT-NO-AUTO-001"))
            .andExpect(jsonPath("$.data.patientName").value("自动建档患者"))
            .andExpect(jsonPath("$.data.patientGender").value("F"))
            .andExpect(jsonPath("$.data.patientAge").value("30"));

        jdbcTemplate.queryForObject("""
                select id
                from patients
                where patient_no = :patientNo
                """,
            new MapSqlParameterSource().addValue("patientNo", "PATIENT-NO-AUTO-001"),
            String.class);
    }

    @Test
    void shouldNormalizeDisplayAgeWhenAutoCreatingPatient() throws Exception {
        String patientNo = "PATIENT-NO-AUTO-AGE-" + System.nanoTime();
        JsonNode created = responseData(mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-AUTO-PATIENT-AGE-%s",
                      "applicationType": "ROUTINE",
                      "patientId": "%s",
                      "patientName": "展示年龄建档患者",
                      "patientGender": "F",
                      "patientAge": "35岁0月0天",
                      "applicationDate": "2026-05-21",
                      "submissionDate": "2026-05-22",
                      "applicationFormStatus": "PENDING",
                      "clinicalDiagnosis": "auto create patient display age"
                    }
                    """.formatted(System.nanoTime(), patientNo))), 201);
        String applicationId = created.path("id").asText();

        mockMvc.perform(authorized(get("/api/v1/applications/{id}", applicationId), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.patientId", not(patientNo)))
            .andExpect(jsonPath("$.data.patientIdentifier").value(patientNo))
            .andExpect(jsonPath("$.data.patientAge").value("35岁0月0天"));

        String savedAge = jdbcTemplate.queryForObject("""
                select age
                from patients
                where patient_no = :patientNo
                """,
            new MapSqlParameterSource().addValue("patientNo", patientNo),
            String.class);
        org.assertj.core.api.Assertions.assertThat(savedAge).isEqualTo("35");
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
    void shouldAutoCreatePatientWhenUpdatingApplicationToNewIdentifier() throws Exception {
        JsonNode created = responseData(mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-UPDATE-AUTO-%s",
                      "applicationType": "ROUTINE",
                      "patientId": "P-UPDATE-001",
                      "patientName": "Patient Before Auto Update",
                      "applicationDate": "2026-05-20",
                      "submissionDate": "2026-05-21",
                      "applicationFormStatus": "PENDING",
                      "clinicalDiagnosis": "before auto update"
                    }
                    """.formatted(System.nanoTime()))), 201);
        String applicationId = created.path("id").asText();

        mockMvc.perform(authorized(patch("/api/v1/applications/{id}", applicationId), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-UPDATE-AUTO-FINAL",
                      "applicationType": "ROUTINE",
                      "patientId": "PATIENT-NO-UPDATE-AUTO-001",
                      "patientName": "Patient Auto Updated",
                      "patientGender": "M",
                      "patientAge": "41",
                      "applicationDate": "2026-05-22",
                      "submissionDate": "2026-05-23",
                      "applicationFormStatus": "UPLOADED",
                      "clinicalDiagnosis": "after auto update"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(applicationId));

        mockMvc.perform(authorized(get("/api/v1/applications/{id}", applicationId), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.patientId", not("PATIENT-NO-UPDATE-AUTO-001")))
            .andExpect(jsonPath("$.data.patientIdentifier").value("PATIENT-NO-UPDATE-AUTO-001"))
            .andExpect(jsonPath("$.data.patientName").value("Patient Auto Updated"))
            .andExpect(jsonPath("$.data.patientAge").value("41"));
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
                      "specimenBarcode": "BC-DOWNSTREAM-LOCK-001"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(post("/api/v1/specimen-verifications/complete"), USER_FIXATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "specimenBarcode": "BC-DOWNSTREAM-LOCK-001"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(post("/api/v1/specimen-fixations/start"), USER_FIXATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "specimenBarcode": "BC-DOWNSTREAM-LOCK-001",
                      "fixationLiquidType": "FORMALIN"}
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
