package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:blcenter_missing_specimen_confirmation_schema;MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
})
@DirtiesContext
class SpecimenManagementMissingConfirmationColumnsIntegrationTest extends AbstractSpecimenWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shouldListSpecimensWhenConfirmationAndCheckInColumnsAreMissing() throws Exception {
        String applicationId = createApplication("APP-M2-LEGACY-001");
        JsonNode registration = registerSpecimens(
            applicationId,
            USER_REGISTER,
            "P-01",
            "/api/v1/specimens/register",
            "BC-LEGACY-001");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();

        jdbcTemplate.getJdbcTemplate().execute("alter table specimens drop column specimen_confirmed_at");
        jdbcTemplate.getJdbcTemplate().execute("alter table specimens drop column check_in_status");
        jdbcTemplate.getJdbcTemplate().execute("alter table specimens drop column checked_in_at");
        jdbcTemplate.getJdbcTemplate().execute("alter table specimens drop column checked_in_by_user_id");
        jdbcTemplate.getJdbcTemplate().execute("alter table specimens drop column checked_in_by_name");

        mockMvc.perform(authorized(get("/api/v1/specimens"), USER_REGISTER)
                .param("page", "1")
                .param("size", "20")
                .param("applicationNo", "APP-M2-LEGACY-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total", is(1)))
            .andExpect(jsonPath("$.data.items[0].applicationId", is(applicationId)))
            .andExpect(jsonPath("$.data.items[0].barcode", is(barcode)))
            .andExpect(jsonPath("$.data.items[0].specimenConfirmedAt").isEmpty())
            .andExpect(jsonPath("$.data.items[0].checkInStatus", is("NOT_CHECKED_IN")))
            .andExpect(jsonPath("$.data.items[0].checkedInAt").isEmpty())
            .andExpect(jsonPath("$.data.items[0].checkedInByName").isEmpty());
    }
}
