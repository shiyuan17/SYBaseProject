package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class M6AuthorizationIntegrationTest extends AuthenticatedWebIntegrationTest {

    private static final String USER_M1_ADMIN = "USER_M1_ADMIN";
    private static final String USER_M1_ARCHIVE = "USER_M1_ARCHIVE";
    private static final String USER_M1_QUALITY = "USER_M1_QUALITY";
    private static final String USER_M1_NO_PERMISSION = "USER_M1_NO_PERMISSION";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldEnforceM6PermissionsByCapability() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/integration-tasks"), USER_M1_ADMIN))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/v1/billing-records"), USER_M1_NO_PERMISSION))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        mockMvc.perform(authorized(post("/api/v1/historical-report-import-jobs"), USER_M1_ARCHIVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sourceSystem":"MOCK_HIS"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(post("/api/v1/stat-reports/query"), USER_M1_QUALITY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "category":"QUALITY"
                    }
                    """))
            .andExpect(status().isOk());
    }

    @Test
    void shouldRejectLegacyOperatorFieldsOnM6Requests() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/historical-report-import-jobs"), USER_M1_ARCHIVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sourceSystem":"MOCK_HIS",
                      "operatorUserId":"FORGED-USER",
                      "operatorName":"forged-user"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("operatorUserId")));

        mockMvc.perform(authorized(post("/api/v1/stat-reports/query"), USER_M1_QUALITY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "category":"QUALITY",
                      "operatorUserId":"FORGED-USER",
                      "operatorName":"forged-user"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("operatorUserId")));
    }
}
