package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class ApplicationControllerIntegrationTest extends AuthenticatedWebIntegrationTest {

    private static final String USER_REGISTER = "USER_M2_REGISTER";
    private static final String USER_FIXATION = "USER_M2_FIXATION";
    private static final String USER_TRANSPORT = "USER_M2_TRANSPORT";
    private static final String USER_RECEIVE = "USER_M2_RECEIVE";
    private static final String USER_TRACKING = "USER_M2_TRACKING";
    private static final String USER_NO_PERMISSION = "USER_M2_NO_PERMISSION";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateApplicationWhenRequestIsValid() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-1001",
                      "applicationType": "ROUTINE",
                      "patientId": "P-1001",
                      "applicationFormStatus": "PENDING",
                      "applicationDate": "2026-05-21",
                      "submissionDate": "2026-05-22",
                      "specimenRemovalTime": "2026-05-21T09:30:00",
                      "submittingDepartmentId": "DEPT-OR",
                      "submittingDepartmentName": "OR",
                      "submittingDoctorUserId": "DOC-1001",
                      "submittingDoctorName": "Dr Test",
                      "clinicalDiagnosis": "test diagnosis",
                      "specimenSite": "Thyroid"
                    }
                    """))
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
            .andExpect(jsonPath("$.message", containsString("申请单号长度不能超过64个字符")))
            .andExpect(jsonPath("$.message", containsString("申请类型不能为空")))
            .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void shouldReturnValidationErrorWhenRequestBodyIsMissing() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
            .andExpect(jsonPath("$.message", is("请求体不能为空，且必须是合法的 JSON")))
            .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void shouldReturnNotFoundWhenApplicationDoesNotExist() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/applications/not-found-id"), USER_TRACKING))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code", is("APPLICATION_NOT_FOUND")))
            .andExpect(jsonPath("$.message", is("申请单不存在")))
            .andExpect(jsonPath("$.traceId", notNullValue()));
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
            .andExpect(jsonPath("$.data.items[0].registeredSpecimenCount").value(0))
            .andExpect(jsonPath("$.data.items[0].latestLabelPrintStatus").value(nullValue()))
            .andExpect(jsonPath("$.data.items[0].abnormalFlag").value(false));
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
        JsonNode application = responseData(mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-LIST-ABNORMAL",
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
                    """)), 201);
        String applicationId = application.path("id").asText();

        JsonNode registration = responseData(mockMvc.perform(authorized(post("/api/v1/specimens/register"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationId": "%s",
                      "printerCode": "P-01",
                      "operatorName": "nurse-a",
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
                          "barcode": "BC-LIST-ABNORMAL-001"
                        }
                      ]
                    }
                    """.formatted(applicationId))), 201);
        String barcode = registration.path("specimens").get(0).path("barcode").asText();

        mockMvc.perform(authorized(post("/api/v1/specimen-fixations/start"), USER_FIXATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "specimenBarcode": "%s",
                      "fixationLiquidType": "FORMALIN",
                      "operatorName": "nurse-b"
                    }
                    """.formatted(barcode)))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(post("/api/v1/specimen-fixations/complete"), USER_FIXATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "specimenBarcode": "%s",
                      "fixationLiquidType": "FORMALIN",
                      "operatorName": "nurse-b"
                    }
                    """.formatted(barcode)))
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
                    """.formatted(applicationId, barcode))), 201);
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
                    """.formatted(transportOrderId, barcode)))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/v1/applications"), USER_TRACKING)
                .param("page", "1")
                .param("size", "20")
                .param("applicationNo", "APP-LIST-ABNORMAL"))
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
                      "submittingDepartmentId": "DEPT-DETAIL",
                      "submittingDepartmentName": "Detail Department",
                      "submittingDoctorUserId": "DOC-DETAIL-001",
                      "submittingDoctorName": "Dr Detail",
                      "clinicalDiagnosis": "detail diagnosis",
                      "specimenSite": "Stomach"
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
                      "clinicalSymptom": "腹痛",
                      "specimenSite": "Stomach"
                    }
                    """)), 201);
        String applicationId = created.path("id").asText();

        mockMvc.perform(authorized(post("/api/v1/specimens/register"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationId": "%s",
                      "operatorName": "nurse-detail",
                      "items": [
                        {
                          "specimenNameStandardized": "胃组织",
                          "specimenType": "组织",
                          "specimenSite": "胃窦",
                          "collectionMode": "SURGERY",
                          "clinicalSymptom": "腹痛",
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
            .andExpect(jsonPath("$.data.clinicalSymptom").value("腹痛"))
            .andExpect(jsonPath("$.data.specimens[0].specimenName").value("胃组织"))
            .andExpect(jsonPath("$.data.specimens[0].specimenType").value("组织"))
            .andExpect(jsonPath("$.data.specimens[0].specimenSite").value("胃窦"))
            .andExpect(jsonPath("$.data.specimens[0].collectionMode").value("SURGERY"))
            .andExpect(jsonPath("$.data.specimens[0].clinicalSymptom").value("腹痛"));
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
    void shouldExposeHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    void shouldGenerateApplicationNumberWhenMissing() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationType": "ROUTINE",
                      "patientId": "P-AUTO-001",
                      "submittingDepartmentId": "DEPT-OR",
                      "submittingDepartmentName": "OR",
                      "submittingDoctorUserId": "DOC-AUTO-001",
                      "submittingDoctorName": "Dr Auto",
                      "clinicalDiagnosis": "auto no",
                      "specimenSite": "Lung"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code", is("SUCCESS")))
            .andExpect(jsonPath("$.data.id", notNullValue()));
    }

    @Test
    void shouldExposePrometheusMetricsAfterCreateApplication() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/applications"), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-1002",
                      "applicationType": "ROUTINE",
                      "patientId": "P-1002",
                      "submittingDepartmentId": "DEPT-OR",
                      "submittingDepartmentName": "OR",
                      "submittingDoctorUserId": "DOC-1002",
                      "submittingDoctorName": "Dr Metrics",
                      "clinicalDiagnosis": "metric diagnosis",
                      "specimenSite": "Liver"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(authorized(get("/actuator/prometheus"), USER_REGISTER))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("jvm_")))
            .andExpect(content().string(containsString("http_server_requests")))
            .andExpect(content().string(containsString("application_create_total")))
            .andExpect(content().string(containsString("application_create_duration")));
    }

    @Test
    void shouldExposeNotFoundMetricAfterQueryingMissingApplication() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/applications/missing-application"), USER_TRACKING))
            .andExpect(status().isNotFound());

        mockMvc.perform(authorized(get("/actuator/prometheus"), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("application_query_not_found_total")));
    }

    @Test
    void shouldRequireAuthenticationForProtectedApplicationAndPrometheusEndpoints() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationType": "ROUTINE",
                      "patientId": "P-ANON-001",
                      "submittingDepartmentId": "DEPT-OR",
                      "submittingDepartmentName": "OR",
                      "submittingDoctorUserId": "DOC-ANON-001",
                      "submittingDoctorName": "Dr Anon",
                      "clinicalDiagnosis": "anon diagnosis",
                      "specimenSite": "Kidney"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("AUTHENTICATION_REQUIRED")));

        mockMvc.perform(get("/api/v1/applications/not-found-id"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("AUTHENTICATION_REQUIRED")));

        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("AUTHENTICATION_REQUIRED")));
    }

    @Test
    void shouldRejectUsersWithoutPermission() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/applications"), USER_NO_PERMISSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationType": "ROUTINE",
                      "patientId": "P-FORBID-001",
                      "submittingDepartmentId": "DEPT-OR",
                      "submittingDepartmentName": "OR",
                      "submittingDoctorUserId": "DOC-FORBID-001",
                      "submittingDoctorName": "Dr Forbidden",
                      "clinicalDiagnosis": "forbidden diagnosis",
                      "specimenSite": "Lung"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code", is("PERMISSION_DENIED")));

        mockMvc.perform(authorized(get("/api/v1/applications/not-found-id"), USER_REGISTER))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code", is("PERMISSION_DENIED")));

        mockMvc.perform(authorized(get("/api/v1/applications"), USER_REGISTER))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code", is("PERMISSION_DENIED")));
    }

    @Test
    void shouldWrapPlainObjectResponseForApiEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/wrap-test/plain"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("SUCCESS")))
            .andExpect(jsonPath("$.traceId", notNullValue()))
            .andExpect(jsonPath("$.data.value", is("ok")));
    }

    @Test
    void shouldPreserveResponseEntityStatusAndHeadersWhenWrapping() throws Exception {
        mockMvc.perform(get("/api/v1/wrap-test/entity"))
            .andExpect(status().isCreated())
            .andExpect(header().string("X-Test-Header", "wrapped"))
            .andExpect(header().exists("X-Trace-Id"))
            .andExpect(jsonPath("$.code", is("SUCCESS")))
            .andExpect(jsonPath("$.data.value", is("created")));
    }

    @Test
    void shouldNotWrapApiResponseTwice() throws Exception {
        mockMvc.perform(get("/api/v1/wrap-test/already"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("SUCCESS")))
            .andExpect(jsonPath("$.data.value", is("already")))
            .andExpect(jsonPath("$.data.code").doesNotExist());
    }

    @Test
    void shouldWrapNullResponseBodyForApiEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/wrap-test/null"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("SUCCESS")))
            .andExpect(jsonPath("$.traceId", notNullValue()))
            .andExpect(jsonPath("$.data", nullValue()));
    }

    @Test
    void shouldKeepNoContentResponseUnwrapped() throws Exception {
        mockMvc.perform(get("/api/v1/wrap-test/no-content"))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));
    }

    @Test
    void shouldSkipWrappingWhenIgnoreAnnotationIsPresent() throws Exception {
        mockMvc.perform(get("/api/v1/wrap-test/ignored"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value", is("ignored")))
            .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void shouldSkipWrappingForPlainTextResponses() throws Exception {
        mockMvc.perform(get("/api/v1/wrap-test/string"))
            .andExpect(status().isOk())
            .andExpect(content().string("raw-text"));
    }

    @Test
    void shouldSkipWrappingForResourceResponses() throws Exception {
        mockMvc.perform(get("/api/v1/wrap-test/resource"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=test.txt"))
            .andExpect(content().bytes("download".getBytes()));
    }

    @Test
    void shouldSkipWrappingForStreamingResponses() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/wrap-test/stream"))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(content().string("stream"));
    }

    private JsonNode responseData(ResultActions resultActions, int expectedStatus) throws Exception {
        String response = resultActions
            .andExpect(status().is(expectedStatus))
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(response).path("data");
    }
}
