package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class SpecimenManagementExportIntegrationTest extends AbstractSpecimenWorkflowIntegrationTest {

    @Test
    void shouldExportTrackingSpecimensAsXlsxForRegisterPermission() throws Exception {
        String suffix = uniqueSuffix();
        String applicationNo = "APP-M2-EXPORT-" + suffix;
        String applicationId = createApplication(applicationNo);
        JsonNode registration = registerSpecimens(
            applicationId,
            USER_REGISTER,
            "P-01",
            "/api/v1/specimens/register",
            "BC-EXPORT-" + suffix);
        assertThat(registration.path("specimens")).hasSize(1);

        byte[] content = mockMvc.perform(authorized(get("/api/v1/specimens/export"), USER_REGISTER)
                .param("page", "1")
                .param("size", "10000")
                .param("applicationNo", applicationNo))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("filename*=")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        assertThat(content).isNotEmpty();
        assertThat(new String(content, 0, 2, java.nio.charset.StandardCharsets.ISO_8859_1)).isEqualTo("PK");
    }

    @Test
    void shouldRejectTrackingSpecimenExportWithoutPermission() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/specimens/export"), USER_NO_PERMISSION)
                .param("page", "1")
                .param("size", "10000"))
            .andExpect(status().isForbidden());
    }
}
