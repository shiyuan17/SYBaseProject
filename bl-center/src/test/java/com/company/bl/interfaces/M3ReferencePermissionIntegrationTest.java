package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class M3ReferencePermissionIntegrationTest extends AuthenticatedWebIntegrationTest {

    private static final String USER_M3_GROSSING = "USER_M3_GROSSING";
    private static final String USER_M3_TRACKING = "USER_M3_TRACKING";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowGrossingRoleToQueryM1ReferenceDataWithoutExpandingTrackingRole() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/body-parts"), USER_M3_GROSSING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(authorized(get("/api/v1/sampling-templates"), USER_M3_GROSSING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(authorized(get("/api/v1/body-parts"), USER_M3_TRACKING))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        mockMvc.perform(authorized(get("/api/v1/sampling-templates"), USER_M3_TRACKING))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }
}