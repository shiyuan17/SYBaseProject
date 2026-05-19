package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.company.common.test.BaseWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
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
class ApplicationControllerIntegrationTest extends BaseWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateApplicationWhenRequestIsValid() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-1001",
                      "applicationType": "ROUTINE",
                      "clinicalDiagnosis": "test diagnosis"
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
        mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": ""
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
            .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void shouldReturnValidationErrorWhenRequestBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
            .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void shouldReturnNotFoundWhenApplicationDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/applications/not-found-id"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code", is("APPLICATION_NOT_FOUND")))
            .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void shouldExposeHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    void shouldExposePrometheusMetricsAfterCreateApplication() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationNo": "APP-1002",
                      "applicationType": "ROUTINE"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("jvm_")))
            .andExpect(content().string(containsString("http_server_requests")))
            .andExpect(content().string(containsString("application_create_total")))
            .andExpect(content().string(containsString("application_create_duration")));
    }

    @Test
    void shouldExposeNotFoundMetricAfterQueryingMissingApplication() throws Exception {
        mockMvc.perform(get("/api/v1/applications/missing-application"))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("application_query_not_found_total")));
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
