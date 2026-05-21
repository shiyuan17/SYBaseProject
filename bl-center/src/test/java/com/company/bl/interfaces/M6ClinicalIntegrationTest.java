package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class M6ClinicalIntegrationTest extends AbstractSpecimenWorkflowIntegrationTest {

    private static final String USER_M1_ADMIN = "USER_M1_ADMIN";

    @Test
    void shouldImportClinicalApplicationWithSourceFieldsAndTaskTrace() throws Exception {
        JsonNode imported = responseBody(postJson("/api/v1/clinical-applications/import", USER_IMPORT, """
            {
              "thirdPartySource":"MOCK_HIS",
              "externalOrderNo":"MOCK-M6-CLINICAL-001"
            }
            """), 201);
        String applicationId = imported.path("id").asText();

        JsonNode detail = responseBody(mockMvc.perform(
            authorized(get("/api/v1/applications/{id}", applicationId), USER_TRACKING)), 200);
        assertThat(detail.path("externalOrderNo").asText()).isEqualTo("MOCK-M6-CLINICAL-001");
        assertThat(detail.path("thirdPartySource").asText()).isEqualTo("MOCK_HIS");

        mockMvc.perform(authorized(get("/api/v1/integration-tasks"), USER_M1_ADMIN)
                .param("taskType", "CLINICAL_IMPORT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].taskStatus").value("SUCCESS"));

        postJson("/api/v1/clinical-applications/import", USER_IMPORT, """
            {
              "thirdPartySource":"MOCK_HIS",
              "externalOrderNo":"MOCK-M6-CLINICAL-001"
            }
            """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
    }
}
