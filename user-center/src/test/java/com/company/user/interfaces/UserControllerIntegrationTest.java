package com.company.user.interfaces;

import com.company.common.test.BaseWebIntegrationTest;
import com.company.user.UserCenterApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

@SpringBootTest(classes = UserCenterApplication.class)
class UserControllerIntegrationTest extends BaseWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateUserWhenRequestIsValid() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Alice",
                      "email": "alice@example.com"
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
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "",
                      "email": "bad-email"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
            .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void shouldReturnValidationErrorWhenRequestBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
            .andExpect(jsonPath("$.message", is("Request body is required and must be valid JSON")))
            .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/users/not-found-id"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code", is("USER_NOT_FOUND")))
            .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void shouldExposeHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    void shouldExposePrometheusMetricsAfterCreateUser() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Bob",
                      "email": "bob@example.com"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("jvm_")))
            .andExpect(content().string(containsString("http_server_requests")))
            .andExpect(content().string(containsString("user_create_total")))
            .andExpect(content().string(containsString("user_create_duration_seconds")));
    }

    @Test
    void shouldExposeNotFoundMetricAfterQueryingMissingUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/missing-user"))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("user_query_not_found_total")));
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
