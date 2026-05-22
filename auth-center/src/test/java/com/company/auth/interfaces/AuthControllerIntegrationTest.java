package com.company.auth.interfaces;

import com.company.auth.AuthCenterApplication;
import com.company.common.security.jwt.JwtAccessTokenClaims;
import com.company.common.security.jwt.Sm2JwtTokenService;
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

import java.time.Instant;
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

    @Autowired
    private Sm2JwtTokenService tokenService;

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
            """, Map.of());
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
            """, Map.of());
        assertThat(failedLog.get("login_name")).isEqualTo("auth.fail");
        assertThat(failedLog.get("login_result")).isEqualTo("FAILED");
        assertThat(failedLog.get("failure_reason")).isEqualTo("Bad credentials");
    }

    @Test
    void shouldRejectDisabledUserAndRecordFailureLog() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "loginName": "auth.disabled",
                      "password": "123456"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_DISABLED"));

        Map<String, Object> failedLog = jdbcTemplate.queryForMap("""
            select login_name, login_result, failure_reason
            from user_login_logs
            where login_name = 'auth.disabled'
            order by login_at desc
            fetch first 1 row only
            """, Map.of());
        assertThat(failedLog.get("login_name")).isEqualTo("auth.disabled");
        assertThat(failedLog.get("login_result")).isEqualTo("FAILED");
        assertThat(failedLog.get("failure_reason")).isEqualTo("Account disabled");
    }

    @Test
    void shouldTemporarilyBlockLoginAfterRepeatedFailures() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "loginName": "auth.lock",
                          "password": "wrong-password"
                        }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "loginName": "auth.lock",
                      "password": "123456"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        Map<String, Object> failedLog = jdbcTemplate.queryForMap("""
            select login_name, login_result, failure_reason
            from user_login_logs
            where login_name = 'auth.lock'
            order by login_at desc
            fetch first 1 row only
            """, Map.of());
        assertThat(failedLog.get("login_name")).isEqualTo("auth.lock");
        assertThat(failedLog.get("login_result")).isEqualTo("FAILED");
        assertThat(failedLog.get("failure_reason")).isEqualTo("Login temporarily locked");

        Long tokenCount = jdbcTemplate.queryForObject("""
            select count(*)
            from auth_access_tokens
            where user_id = 'AUTH_USER_LOCK'
            """, Map.of(), Long.class);
        assertThat(tokenCount).isEqualTo(0L);
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
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[?(@ == 'PERM_SYSTEM_USER_QUERY')]").exists())
            .andExpect(jsonPath("$.data[?(@ == 'PERM_SYS_ROLE_QUERY')]").exists())
            .andExpect(jsonPath("$.data[?(@ == 'PERM_SYS_ORDER_DICT_QUERY')]").exists())
            .andExpect(jsonPath("$.data[?(@ == 'sys:medical-order-dict:query')]").doesNotExist());

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", bearerToken(accessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", bearerToken(accessToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("ACCESS_TOKEN_REVOKED"));
    }

    @Test
    void shouldInferEntryPermissionCodesFromGrantedMenus() throws Exception {
        JsonNode loginResult = responseData(mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "loginName": "auth.menu",
                  "password": "123456"
                }
                """)));
        String accessToken = loginResult.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/access-codes")
                .header("Authorization", bearerToken(accessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@ == 'PERM_SYS_ROLE_QUERY')]").exists())
            .andExpect(jsonPath("$.data[?(@ == 'PERM_SYS_ROLE_ASSIGN')]").doesNotExist())
            .andExpect(jsonPath("$.data[?(@ == 'PERM_SYSTEM_USER_QUERY')]").doesNotExist());
    }

    @Test
    void shouldRequireAuthenticationForProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/v1/auth/access-codes"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void shouldRejectInvalidBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", bearerToken("not-a-jwt")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    void shouldRejectExpiredBearerToken() throws Exception {
        Instant expiresAt = Instant.now().minusSeconds(60);
        String accessToken = tokenService.generateToken(new JwtAccessTokenClaims(
            "AT-EXPIRED",
            "AUTH_USER_API",
            "auth.api",
            expiresAt.minusSeconds(300),
            expiresAt));

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", bearerToken(accessToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("ACCESS_TOKEN_EXPIRED"));
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
