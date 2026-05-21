package com.company.bl.interfaces;

import com.company.bl.system.application.SystemManagementService;
import com.company.common.security.crypto.Sm3PasswordEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class AbstractSystemManagementIntegrationTest extends AuthenticatedWebIntegrationTest {

    protected static final String USER_M1_ADMIN = "USER_M1_ADMIN";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected SystemManagementService systemManagementService;
    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;

    protected String createUser(String loginName) throws Exception {
        MvcResult createResult = mockMvc.perform(asAdmin(post("/api/v1/system-users"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userCode": "UC-%s",
                      "loginName": "%s",
                      "name": "Login Test User",
                      "password": "123456",
                      "enabled": true
                    }
                    """.formatted(System.nanoTime(), loginName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("SUCCESS")))
            .andExpect(jsonPath("$.data.id", notNullValue()))
            .andReturn();
        JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        return createNode.path("data").path("id").asText();
    }

    protected void assertSeededPassword(String userId) {
        Map<String, Object> passwordRow = jdbcTemplate.queryForMap("""
            select password, password_algo, password_salt
            from users
            where id = :userId
            """, Map.of("userId", userId));
        Object passwordValue = passwordRow.get("password");
        Object passwordAlgoValue = passwordRow.get("password_algo");
        Object passwordSaltValue = passwordRow.get("password_salt");

        assertNotNull(passwordValue);
        assertNotNull(passwordAlgoValue);
        assertNotNull(passwordSaltValue);
        assertEquals("SM3", passwordAlgoValue);
        assertTrue(new Sm3PasswordEncoder().matchesSm3(
            "123456",
            passwordSaltValue.toString(),
            passwordValue.toString()));
    }

    protected void assertCreatedUserPasswordSecured(String userId) {
        Map<String, Object> passwordRow = jdbcTemplate.queryForMap("""
            select password, password_algo, password_salt
            from users
            where id = :userId
            """, Map.of("userId", userId));
        assertEquals("SM3", passwordRow.get("password_algo"));
        assertNotNull(passwordRow.get("password_salt"));
        assertNotEquals("123456", passwordRow.get("password"));
    }

    protected MockMultipartFile buildImportFile(String loginName) {
        return new MockMultipartFile(
            "file",
            "system-users.csv",
            "text/csv",
            ("""
                userCode,loginName,name,enabled
                IMP-%s,%s,Imported User,true
                """.formatted(System.nanoTime(), loginName)).getBytes());
    }

    protected MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder requestBuilder) {
        return authorized(requestBuilder, USER_M1_ADMIN);
    }
}
