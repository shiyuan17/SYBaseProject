package com.company.user.interfaces;

import com.company.common.test.BaseWebIntegrationTest;
import com.company.user.UserCenterApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
            .andExpect(jsonPath("$.message", is("请求体不能为空或 JSON 格式不正确")))
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
}
