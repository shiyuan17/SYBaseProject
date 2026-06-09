package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class ApplicationQueryAndDetailIntegrationTest extends AbstractApplicationControllerIntegrationTest {

    private void ensurePatientsTable() {
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
    }

    private void insertPatient(
        String id,
        String patientNo,
        String inpatientNo,
        String outpatientNo,
        String name
    ) {
        ensurePatientsTable();
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
    void shouldReturnNotFoundWhenApplicationDoesNotExist() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/applications/not-found-id"), USER_TRACKING))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code", is("APPLICATION_NOT_FOUND")))
            .andExpect(jsonPath("$.message", is("申请单不存在")))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void shouldListApplicationsWithFilters() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-LIST-001",
                      "applicationType": "ROUTINE",
                      "applicationDate": "2026-05-21",
                      "patientId": "P-LIST-001",
                      "patientName": "Patient List Alpha",
                      "submittingDepartmentId": "DEPT-LIST",
                      "submittingDepartmentName": "List Department",
                      "submittingDoctorUserId": "DOC-LIST-001",
                      "submittingDoctorName": "Dr List",
                      "clinicalDiagnosis": "list diagnosis",
                      "specimenSite": "Thyroid"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-LIST-002",
                      "applicationType": "FROZEN",
                      "applicationDate": "2026-05-25",
                      "patientId": "P-LIST-002",
                      "patientName": "Patient List Beta",
                      "submittingDepartmentId": "DEPT-OTHER",
                      "submittingDepartmentName": "Other Department",
                      "submittingDoctorUserId": "DOC-LIST-002",
                      "submittingDoctorName": "Dr Other",
                      "clinicalDiagnosis": "other diagnosis",
                      "specimenSite": "Liver"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(authorized(get("/api/v1/applications"), USER_TRACKING)
                .param("page", "1")
                .param("size", "20")
                .param("applicationNo", "LIST-001")
                .param("patientName", "Alpha")
                .param("submittingDepartmentId", "DEPT-LIST")
                .param("applicationType", "ROUTINE")
                .param("dateFrom", "2026-05-20")
                .param("dateTo", "2026-05-22"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].applicationNo").value("APP-LIST-001"))
            .andExpect(jsonPath("$.data.items[0].patientName").value("Patient List Alpha"))
            .andExpect(jsonPath("$.data.items[0].status").value("DRAFT"))
            .andExpect(jsonPath("$.data.items[0].currentNode").isNotEmpty())
            .andExpect(jsonPath("$.data.items[0].pathologyNo").value(nullValue()))
            .andExpect(jsonPath("$.data.items[0].registeredSpecimenCount").value(0))
            .andExpect(jsonPath("$.data.items[0].latestLabelPrintStatus").value(nullValue()))
            .andExpect(jsonPath("$.data.items[0].abnormalFlag").value(false));
    }

    @Test
    void shouldExposePathologyNoInApplicationListWhenCaseExists() throws Exception {
        String applicationNo = "APP-LIST-PATHOLOGY-" + System.nanoTime();
        JsonNode application = responseData(mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "%s",
                      "applicationType": "ROUTINE",
                      "applicationDate": "2026-06-08",
                      "patientId": "P-LIST-PATHOLOGY",
                      "patientName": "Patient Pathology",
                      "submittingDepartmentId": "DEPT-LIST",
                      "submittingDepartmentName": "List Department",
                      "submittingDoctorUserId": "DOC-LIST-PATHOLOGY",
                      "submittingDoctorName": "Dr Pathology",
                      "clinicalDiagnosis": "pathology no diagnosis",
                      "specimenSite": "Lung"
                    }
                    """.formatted(applicationNo))), 201);
        String applicationId = application.path("id").asText();

        jdbcTemplate.update("""
                insert into pathology_cases
                    (id, application_id, pathology_no, case_status, created_at, updated_at)
                values
                    (:id, :applicationId, :pathologyNo, 'RECEIVED', current_timestamp, current_timestamp)
                """,
            new MapSqlParameterSource()
                .addValue("id", "CASE-LIST-PATHOLOGY-" + System.nanoTime())
                .addValue("applicationId", applicationId)
                .addValue("pathologyNo", "BL202606080001"));

        mockMvc.perform(authorized(get("/api/v1/applications"), USER_TRACKING)
                .param("page", "1")
                .param("size", "20")
                .param("applicationNo", applicationNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].applicationNo").value(applicationNo))
            .andExpect(jsonPath("$.data.items[0].pathologyNo").value("BL202606080001"));
    }

    @Test
    void shouldReturnEmptyApplicationListWhenNoRecordsMatch() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/applications"), USER_TRACKING)
                .param("page", "1")
                .param("size", "20")
                .param("applicationNo", "APP-LIST-NO-MATCH"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0))
            .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void shouldExposeAbnormalFlagInApplicationList() throws Exception {
        String applicationNo = "APP-LIST-ABNORMAL-" + System.nanoTime();
        String barcode = "BC-LIST-ABNORMAL-" + System.nanoTime();
        JsonNode application = responseData(mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "%s",
                      "applicationType": "ROUTINE",
                      "patientId": "P-LIST-ABNORMAL",
                      "patientName": "Patient Abnormal",
                      "submittingDepartmentId": "DEPT-LIST",
                      "submittingDepartmentName": "List Department",
                      "submittingDoctorUserId": "DOC-LIST-ABNORMAL",
                      "submittingDoctorName": "Dr Abnormal",
                      "clinicalDiagnosis": "abnormal diagnosis",
                      "specimenSite": "Thyroid"
                    }
                    """.formatted(applicationNo))), 201);
        String applicationId = application.path("id").asText();

        JsonNode registration = responseData(mockMvc.perform(authorized(post("/api/v1/specimens/register"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationId": "%s",
                      "printerCode": "P-01",
                      
                      "terminalCode": "OR-01",
                      "items": [
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
                      ]
                    }
                    """.formatted(applicationId, barcode))), 201);
        String registeredBarcode = registration.path("specimens").get(0).path("barcode").asText();

        mockMvc.perform(authorized(post("/api/v1/specimen-verifications/start"), USER_FIXATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "specimenBarcode": "%s"}
                    """.formatted(registeredBarcode)))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(post("/api/v1/specimen-verifications/complete"), USER_FIXATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "specimenBarcode": "%s"}
                    """.formatted(registeredBarcode)))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(post("/api/v1/specimen-fixations/start"), USER_FIXATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "specimenBarcode": "%s",
                      "fixationLiquidType": "FORMALIN"}
                    """.formatted(registeredBarcode)))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(post("/api/v1/specimen-fixations/complete"), USER_FIXATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "specimenBarcode": "%s",
                      "fixationLiquidType": "FORMALIN"}
                    """.formatted(registeredBarcode)))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(post("/api/v1/specimens/barcodes/%s/confirm".formatted(registeredBarcode)), USER_FIXATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      
                      "terminalCode": "T-CONFIRM"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(post("/api/v1/specimens/barcodes/%s/check-in".formatted(registeredBarcode)), USER_FIXATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      
                      "specimenBarcode": "%s",
                      "terminalCode": "T-CHECK-IN"
                    }
                    """.formatted(registeredBarcode)))
            .andExpect(status().isOk());

        JsonNode transportOrder = responseData(mockMvc.perform(authorized(post("/api/v1/transport-orders"), USER_TRANSPORT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationId": "%s",
                      "specimenBarcodes": ["%s"],
                      "handoverUserName": "handover-a",
                      "handoverDepartmentId": "DEPT-OR",
                      "handoverDepartmentName": "OR",
                      "receiverDepartmentId": "DEPT-PATH",
                      "receiverDepartmentName": "Pathology",
                      "terminalCode": "OR-02"
                    }
                    """.formatted(applicationId, registeredBarcode))), 201);
        String transportOrderId = transportOrder.path("id").asText();

        mockMvc.perform(authorized(post("/api/v1/transport-orders/%s/handover".formatted(transportOrderId)), USER_TRANSPORT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "receiverUserName": "receiver-b",
                      "terminalCode": "T-01"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(post("/api/v1/specimen-receipts"), USER_RECEIVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "transportOrderId": "%s",
                      "receivedByName": "receiver-b",
                      "logisticsStaffName": "物流员乙",
                      "items": [
                        {
                          "specimenBarcode": "%s",
                          "receiptStatus": "REJECTED",
                          "containerCount": 1,
                          "qualityCheckResult": "FAILED",
                          "qualityIssueCodes": ["LABEL_MISMATCH"],
                          "reason": "broken-container"
                        }
                      ]
                    }
                    """.formatted(transportOrderId, registeredBarcode)))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/v1/applications"), USER_TRACKING)
                .param("page", "1")
                .param("size", "20")
                .param("applicationNo", applicationNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].abnormalFlag").value(true));
    }

    @Test
    void shouldExposeSpecimenRemovalTimeAndApplicationFormStatusInDetail() throws Exception {
        JsonNode created = responseData(mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-DETAIL-001",
                      "applicationType": "ROUTINE",
                      "patientId": "P-DETAIL-001",
                      "patientName": "Patient Detail",
                      "applicationDate": "2026-05-20",
                      "submissionDate": "2026-05-21",
                      "specimenRemovalTime": "2026-05-20T08:45:00",
                      "applicationFormStatus": "PENDING",
                      "clinicalDiagnosis": "detail diagnosis"
                    }
                    """)), 201);
        String applicationId = created.path("id").asText();

        mockMvc.perform(authorized(get("/api/v1/applications/{id}", applicationId), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.applicationFormStatus").value("PENDING"))
            .andExpect(jsonPath("$.data.applicationDate").value("2026-05-20"))
            .andExpect(jsonPath("$.data.submissionDate").value("2026-05-21"))
            .andExpect(jsonPath("$.data.specimenRemovalTime").value("2026-05-20T08:45"));
    }

    @Test
    void shouldExposeSpecimenCollectionModeAndClinicalSymptomInDetail() throws Exception {
        JsonNode created = responseData(mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-DETAIL-002",
                      "applicationType": "ROUTINE",
                      "patientId": "P-DETAIL-002",
                      "patientName": "Patient Detail Two",
                      "submittingDepartmentId": "DEPT-DETAIL",
                      "submittingDepartmentName": "Detail Department",
                      "submittingDoctorUserId": "DOC-DETAIL-002",
                      "submittingDoctorName": "Dr Detail Two",
                      "clinicalDiagnosis": "detail diagnosis",
                      "clinicalSymptom": "Abdominal pain",
                      "specimenSite": "Stomach"
                    }
                    """)), 201);
        String applicationId = created.path("id").asText();

        mockMvc.perform(authorized(post("/api/v1/specimens/register"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationId": "%s",
                      
                      "items": [
                        {
                          "specimenNameStandardized": "Gastric tissue",
                          "specimenType": "Tissue",
                          "specimenSite": "Stomach",
                          "collectionMode": "SURGERY",
                          "clinicalSymptom": "Abdominal pain",
                          "containerName": "Specimen Bottle",
                          "containerCount": 1,
                          "specimenCount": 1,
                          "barcode": "BC-DETAIL-002"
                        }
                      ]
                    }
                    """.formatted(applicationId)))
            .andExpect(status().isCreated());

        mockMvc.perform(authorized(get("/api/v1/applications/{id}", applicationId), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.clinicalSymptom").value("Abdominal pain"))
            .andExpect(jsonPath("$.data.specimens[0].specimenName").value("Gastric tissue"))
            .andExpect(jsonPath("$.data.specimens[0].specimenType").value("Tissue"))
            .andExpect(jsonPath("$.data.specimens[0].specimenSite").value("Stomach"))
            .andExpect(jsonPath("$.data.specimens[0].collectionMode").value("SURGERY"))
            .andExpect(jsonPath("$.data.specimens[0].clinicalSymptom").value("Abdominal pain"));
    }

    @Test
    void shouldLookupPatientByExternalIdentifier() throws Exception {
        insertPatient(
            "PATIENT-LOOKUP-ID",
            "PATIENT-LOOKUP-NO",
            "INPATIENT-LOOKUP-NO",
            "OUTPATIENT-LOOKUP-NO",
            "Lookup Patient");

        mockMvc.perform(authorized(get("/api/v1/applications/patient-lookup"), USER_REGISTER)
                .param("identifier", "INPATIENT-LOOKUP-NO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.patientId").value("PATIENT-LOOKUP-ID"))
            .andExpect(jsonPath("$.data.patientIdentifier").value("PATIENT-LOOKUP-NO"))
            .andExpect(jsonPath("$.data.patientName").value("Lookup Patient"))
            .andExpect(jsonPath("$.data.patientGender").value("M"))
            .andExpect(jsonPath("$.data.patientAge").value("45"));
    }

    @Test
    void shouldWarnDuplicateApplicationsByExternalOrderAndSameDaySite() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-DUP-CHECK-001",
                      "applicationType": "ROUTINE",
                      "patientId": "P-DUP-CHECK",
                      "patientName": "Patient Dup",
                      "externalOrderNo": "EXT-DUP-001",
                      "applicationDate": "2026-05-20",
                      "submittingDepartmentId": "DEPT-DUP",
                      "submittingDepartmentName": "Dup Department",
                      "submittingDoctorUserId": "DOC-DUP-001",
                      "submittingDoctorName": "Dr Dup",
                      "clinicalDiagnosis": "dup diagnosis",
                      "specimenSite": "Colon"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(authorized(get("/api/v1/applications/duplicate-check"), USER_REGISTER)
                .param("patientId", "P-DUP-CHECK")
                .param("externalOrderNo", "EXT-DUP-001")
                .param("applicationDate", "2026-05-20")
                .param("applicationType", "ROUTINE")
                .param("specimenSite", "Colon"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.suggestedAction").value("BLOCK"))
            .andExpect(jsonPath("$.data.items[0].applicationNo").value("APP-DUP-CHECK-001"))
            .andExpect(jsonPath("$.data.items[0].matchedBy[0]").value("EXTERNAL_ORDER_NO"))
            .andExpect(jsonPath("$.data.items[0].matchedBy[1]").value("SAME_DAY_SAME_SITE"));
    }

    @Test
    void shouldWarnDuplicateApplicationsWhenQueriedByPatientIdentifier() throws Exception {
        insertPatient(
            "PATIENT-DUP-LINK-ID",
            "PATIENT-DUP-LINK-NO",
            "INPATIENT-DUP-LINK",
            "OUTPATIENT-DUP-LINK",
            "Patient Dup Link");

        mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-DUP-LINK-001",
                      "applicationType": "ROUTINE",
                      "patientId": "PATIENT-DUP-LINK-NO",
                      "patientName": "Patient Dup Link",
                      "externalOrderNo": "EXT-DUP-LINK-001",
                      "applicationDate": "2026-05-20",
                      "submittingDepartmentId": "DEPT-DUP",
                      "submittingDepartmentName": "Dup Department",
                      "submittingDoctorUserId": "DOC-DUP-LINK-001",
                      "submittingDoctorName": "Dr Dup Link",
                      "clinicalDiagnosis": "dup link diagnosis",
                      "specimenSite": "Colon"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(authorized(get("/api/v1/applications/duplicate-check"), USER_REGISTER)
                .param("patientId", "PATIENT-DUP-LINK-NO")
                .param("externalOrderNo", "EXT-DUP-LINK-001")
                .param("applicationDate", "2026-05-20")
                .param("applicationType", "ROUTINE")
                .param("specimenSite", "Colon"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.suggestedAction").value("BLOCK"))
            .andExpect(jsonPath("$.data.items[0].applicationNo").value("APP-DUP-LINK-001"));
    }
}
