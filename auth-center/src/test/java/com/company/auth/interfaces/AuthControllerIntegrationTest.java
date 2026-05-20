package com.company.auth.interfaces;

import com.company.auth.AuthCenterApplication;
import com.company.common.test.BaseWebIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = AuthCenterApplication.class)
class AuthControllerIntegrationTest extends BaseWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shouldLoginUpgradePlainPasswordAndPersistTokenSession() throws Exception {
        JsonNode loginResult = responseData(mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "loginName": "auth.plain",
                  "password": "123456"
                }
                """)));

        assertThat(loginResult.path("accessToken").asText()).isNotBlank();
        assertThat(loginResult.path("expiresAt").asText()).isNotBlank();

        Map<String, Object> user = jdbcTemplate.queryForMap("""
            select password, password_algo, password_salt
            from users
            where id = 'AUTH_USER_PLAIN'
            """);
        assertThat(user.get("password_algo")).isEqualTo("SM3");
        assertThat(user.get("password_salt")).isNotNull();
        assertThat(user.get("password")).isNotEqualTo("123456");

        Long tokenCount = jdbcTemplate.queryForObject("""
            select count(*)
            from auth_access_tokens
            where user_id = 'AUTH_USER_PLAIN'
            """, Map.of(), Long.class);
        assertThat(tokenCount).isEqualTo(1L);

        Long successLogCount = jdbcTemplate.queryForObject("""
            select count(*)
            from user_login_logs
            where user_id = 'AUTH_USER_PLAIN'
              and login_result = 'SUCCESS'
            """, Map.of(), Long.class);
        assertThat(successLogCount).isEqualTo(1L);
    }

    @Test
    void shouldRejectBadPasswordAndRecordFailureLog() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "loginName": "auth.fail",
                      "password": "wrong-password"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        Map<String, Object> failedLog = jdbcTemplate.queryForMap("""
            select login_name, login_result, failure_reason
            from user_login_logs
            where login_name = 'auth.fail'
            order by login_at desc
            fetch first 1 row only
            """);
        assertThat(failedLog.get("login_name")).isEqualTo("auth.fail");
        assertThat(failedLog.get("login_result")).isEqualTo("FAILED");
        assertThat(failedLog.get("failure_reason")).isEqualTo("Bad credentials");
    }

    @Test
    void shouldExposeCurrentUserAndAccessCodesAndRejectRevokedToken() throws Exception {
        JsonNode loginResult = responseData(mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "loginName": "auth.api",
                  "password": "123456"
                }
                """)));
        String accessToken = loginResult.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", bearerToken(accessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userId").value("AUTH_USER_API"))
            .andExpect(jsonPath("$.data.loginName").value("auth.api"))
            .andExpect(jsonPath("$.data.realName").value("Auth Api User"))
            .andExpect(jsonPath("$.data.roles[0]").value("AUTH_ADMIN"));

        mockMvc.perform(get("/api/v1/auth/access-codes")
                .header("Authorization", bearerToken(accessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0]").value("PERM_SYSTEM_USER_QUERY"));

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", bearerToken(accessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", bearerToken(accessToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("ACCESS_TOKEN_REVOKED"));
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }

    private JsonNode responseData(org.springframework.test.web.servlet.ResultActions resultActions) throws Exception {
        MvcResult mvcResult = resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andReturn();
        return objectMapper.readTree(mvcResult.getResponse().getContentAsString()).path("data");
    }
}
