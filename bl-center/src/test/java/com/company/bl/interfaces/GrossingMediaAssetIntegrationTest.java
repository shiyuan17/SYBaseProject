package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(
    classes = BlCenterApplication.class,
    properties = "bl.file-storage.grossing-media.root-dir=${java.io.tmpdir}/sybase/bl-center/grossing-media-test"
)
class GrossingMediaAssetIntegrationTest extends AbstractTechnicalWorkflowIntegrationTest {

    @Test
    void shouldUploadAndReadGrossingImage() throws Exception {
        byte[] imageContent = new byte[] {1, 2, 3, 4, 5};
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "specimen-photo.jpg",
            "image/jpeg",
            imageContent);

        JsonNode uploaded = responseBody(mockMvc.perform(
            authorized(multipart("/api/v1/grossing-media-assets").file(file), USER_M3_GROSSING)), 200);

        String fileUrl = uploaded.path("fileUrl").asText();
        assertThat(fileUrl).startsWith("/api/v1/grossing-media-assets/files/");
        assertThat(uploaded.path("fileName").asText()).isEqualTo("specimen-photo.jpg");
        assertThat(uploaded.path("contentType").asText()).isEqualTo("image/jpeg");
        assertThat(uploaded.path("size").asLong()).isEqualTo(imageContent.length);

        mockMvc.perform(authorized(get(fileUrl), USER_M3_GROSSING))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/jpeg"))
            .andExpect(content().bytes(imageContent));
    }

    @Test
    void shouldRejectNonImageUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "specimen.txt",
            "text/plain",
            "not-an-image".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(authorized(multipart("/api/v1/grossing-media-assets").file(file), USER_M3_GROSSING))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void shouldRejectInvalidImageReadPath() throws Exception {
        mockMvc.perform(authorized(
                get("/api/v1/grossing-media-assets/files/not-a-date/specimen.jpg"),
                USER_M3_GROSSING))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }
}
