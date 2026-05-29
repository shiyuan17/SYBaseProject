package com.company.bl.interfaces;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
            .andExpect(jsonPath("$.data.fixationLiquidTypes.length()", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data.containerNames.length()", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data.specimenImageSizes.length()", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data.cutSurfaceFeatures.length()", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data.marginMarkings.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(authorized(get("/api/v1/workflow-reference-options"), USER_FIXATION))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fixationLiquidTypes[0].value", is("FORMALIN")));

        mockMvc.perform(authorized(get("/api/v1/workflow-reference-options"), USER_M3_GROSSING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.specimenTypes[*].value", hasItem("ROUTINE")))
            .andExpect(jsonPath("$.data.cutSurfaceFeatures[*].value", hasItem("灰白")))
            .andExpect(jsonPath("$.data.marginMarkings[*].value", hasItem("上缘墨染")));
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
                'FIXATION_LIQUID_TYPE',
                'CONTAINER_NAME',
                'SPECIMEN_IMAGE_SIZE',
                'CUT_SURFACE_FEATURE',
                'MARGIN_MARKING'
            )
            """, java.util.Map.of(), Integer.class);
        Integer itemCount = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from system_config_items
            where config_key like 'WORKFLOW_REFERENCE.%'
            """, java.util.Map.of(), Integer.class);

        org.junit.jupiter.api.Assertions.assertEquals(9, categoryCount);
        org.junit.jupiter.api.Assertions.assertNotNull(itemCount);
        org.junit.jupiter.api.Assertions.assertTrue(itemCount >= 16);

        mockMvc.perform(authorized(get("/api/v1/workflow-reference-options"), USER_TRACKING))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void shouldHideDisabledWorkflowReferenceItemsAndCategories() throws Exception {
        mockMvc.perform(authorized(patch("/api/v1/system-configs/items/SCI_WORKFLOW_CUT_SURFACE_FEATURE_GRAY_WHITE"), USER_M1_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "configValue": "灰白",
                      "enabled": false,
                      "remarks": "workflow reference hide item test"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.enabled", is(false)));

        mockMvc.perform(authorized(get("/api/v1/workflow-reference-options"), USER_M3_GROSSING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.cutSurfaceFeatures[*].value", not(hasItem("灰白"))));

        mockMvc.perform(authorized(patch("/api/v1/system-configs/items/SCI_WORKFLOW_CUT_SURFACE_FEATURE_GRAY_WHITE"), USER_M1_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "configValue": "灰白",
                      "enabled": true,
                      "remarks": "workflow reference restore item test"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.enabled", is(true)));

        mockMvc.perform(authorized(patch("/api/v1/system-configs/categories/SCC_WORKFLOW_REFERENCE_CUT_SURFACE_FEATURE"), USER_M1_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "parentId": "SCC_WORKFLOW_REFERENCE",
                      "categoryCode": null,
                      "categoryName": "切面特征",
                      "categoryType": "WORKFLOW_REFERENCE",
                      "sortOrder": 170,
                      "enabled": false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.enabled", is(false)));

        mockMvc.perform(authorized(get("/api/v1/workflow-reference-options"), USER_M3_GROSSING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.cutSurfaceFeatures.length()", is(0)));

        mockMvc.perform(authorized(patch("/api/v1/system-configs/categories/SCC_WORKFLOW_REFERENCE_CUT_SURFACE_FEATURE"), USER_M1_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "parentId": "SCC_WORKFLOW_REFERENCE",
                      "categoryCode": null,
                      "categoryName": "切面特征",
                      "categoryType": "WORKFLOW_REFERENCE",
                      "sortOrder": 170,
                      "enabled": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.enabled", is(true)));
    }

    @Test
    void shouldReflectWorkflowReferenceMaintenanceThroughSystemConfigApi() throws Exception {
        mockMvc.perform(authorized(patch("/api/v1/system-configs/items/SCI_WORKFLOW_FIXATION_FORMALIN"), USER_M1_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "configValue": "FORMALIN_BUFFERED",
                      "enabled": true,
                      "remarks": "workflow reference integration test"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id", is("SCI_WORKFLOW_FIXATION_FORMALIN")))
            .andExpect(jsonPath("$.data.configValue", is("FORMALIN_BUFFERED")));

        mockMvc.perform(authorized(get("/api/v1/workflow-reference-options"), USER_FIXATION))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fixationLiquidTypes[*].value", hasItem("FORMALIN_BUFFERED")));

        mockMvc.perform(authorized(patch("/api/v1/system-configs/items/SCI_WORKFLOW_FIXATION_FORMALIN"), USER_M1_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "configValue": "FORMALIN",
                      "enabled": false,
                      "remarks": "workflow reference integration test disabled"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.enabled", is(false)));

        mockMvc.perform(authorized(get("/api/v1/workflow-reference-options"), USER_FIXATION))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fixationLiquidTypes[*].value", not(hasItem("FORMALIN"))))
            .andExpect(jsonPath("$.data.fixationLiquidTypes[*].value", not(hasItem("FORMALIN_BUFFERED"))));

        mockMvc.perform(authorized(patch("/api/v1/system-configs/items/SCI_WORKFLOW_FIXATION_FORMALIN"), USER_M1_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "configValue": "FORMALIN",
                      "enabled": true,
                      "remarks": "workflow reference restored"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.configValue", is("FORMALIN")))
            .andExpect(jsonPath("$.data.enabled", is(true)));
    }

    @Test
    void shouldProtectWorkflowReferenceCategoriesFromDeletionWhenStillPopulated() throws Exception {
        mockMvc.perform(authorized(delete("/api/v1/system-configs/categories/SCC_WORKFLOW_REFERENCE_SPECIMEN_TYPE"), USER_M1_ADMIN))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", anyOf(
                is("Config category still has children or items"),
                is("配置分类下仍有子分类或配置项")
            )));
    }
}
