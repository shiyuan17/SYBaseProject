package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

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
class ApplicationObservabilityAndWrappingIntegrationTest extends AbstractApplicationControllerIntegrationTest {

    @Test
    void shouldExposeHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")));
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
                      "clinicalDiagnosis": "metric diagnosis"
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
                      "clinicalDiagnosis": "forbidden diagnosis"
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
}
