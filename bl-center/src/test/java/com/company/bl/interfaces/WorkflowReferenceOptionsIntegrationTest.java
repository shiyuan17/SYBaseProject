package com.company.bl.interfaces;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = com.company.bl.BlCenterApplication.class)
class WorkflowReferenceOptionsIntegrationTest extends AbstractTechnicalWorkflowIntegrationTest {

    @Test
    void shouldExposeWorkflowReferenceOptionsToSeededRoles() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/workflow-reference-options"), USER_REGISTER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.specimenTypes.length()", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data.collectionModes.length()", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data.fixationLiquidTypes.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(authorized(get("/api/v1/workflow-reference-options"), USER_FIXATION))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fixationLiquidTypes[0].value", is("FORMALIN")));

        mockMvc.perform(authorized(get("/api/v1/workflow-reference-options"), USER_M3_GROSSING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.specimenTypes[*].value", hasItem("ROUTINE")));
    }

    @Test
    void shouldFallbackToConfigNameWhenConfigValueIsBlank() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/workflow-reference-options"), USER_REGISTER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.clinicalSymptoms[0].label", is("肿物")))
            .andExpect(jsonPath("$.data.clinicalSymptoms[0].value", is("肿物")));
    }

    @Test
    void shouldSeedWorkflowReferenceCategoriesAndProtectEndpoint() throws Exception {
        Integer categoryCount = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from system_config_categories
            where category_code in (
                'WORKFLOW_REFERENCE',
                'SPECIMEN_TYPE',
                'COLLECTION_MODE',
                'CLINICAL_SYMPTOM',
                'FIXATION_LIQUID_TYPE'
            )
            """, java.util.Map.of(), Integer.class);
        Integer itemCount = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from system_config_items
            where config_key like 'WORKFLOW_REFERENCE.%'
            """, java.util.Map.of(), Integer.class);

        org.junit.jupiter.api.Assertions.assertEquals(5, categoryCount);
        org.junit.jupiter.api.Assertions.assertNotNull(itemCount);
        org.junit.jupiter.api.Assertions.assertTrue(itemCount >= 8);

        mockMvc.perform(authorized(get("/api/v1/workflow-reference-options"), USER_TRACKING))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }
}
