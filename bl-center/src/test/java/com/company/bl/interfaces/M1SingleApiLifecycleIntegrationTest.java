package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class M1SingleApiLifecycleIntegrationTest extends AbstractSystemManagementIntegrationTest {

    @Test
    void shouldToggleSystemUserEnabledStateThroughDedicatedEndpoint() throws Exception {
        String loginName = "toggle-" + System.nanoTime();
        String userId = createUser(loginName);

        mockMvc.perform(asAdmin(patch("/api/v1/system-users/{id}/enabled", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "enabled": false
                    }
                    """)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(userId))
            .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(asAdmin(get("/api/v1/system-users"))
                .param("page", "1")
                .param("size", "20")
                .param("keyword", loginName)
                .param("enabled", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(userId))
            .andExpect(jsonPath("$.data.items[0].enabled").value(false));
    }

    @Test
    void shouldManageBodyPartSystemConfigAndSamplingLifecycles() throws Exception {
        String bodyPartCode = "BP-LIFE-" + System.nanoTime();
        JsonNode bodyPart = responseData(mockMvc.perform(asAdmin(post("/api/v1/body-parts"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "parentId": "BP_ROOT",
                      "partCode": "%s",
                      "partName": "Lifecycle Body Part",
                      "partAlias": "Lifecycle Alias",
                      "partLevel": 1,
                      "sortOrder": 9,
                      "enabled": true
                    }
                    """.formatted(bodyPartCode))), 200);
        String bodyPartId = bodyPart.path("id").asText();

        mockMvc.perform(asAdmin(patch("/api/v1/body-parts/{id}/enabled", bodyPartId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "enabled": false
                    }
                    """)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(bodyPartId))
            .andExpect(jsonPath("$.data.enabled").value(false));

        String configCategoryCode = "CFG-CAT-" + System.nanoTime();
        JsonNode configCategory = responseData(mockMvc.perform(asAdmin(post("/api/v1/system-configs/categories"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "parentId": "SCC_ROOT",
                      "categoryCode": "%s",
                      "categoryName": "Lifecycle Config Category",
                      "categoryType": "BIZ",
                      "sortOrder": 5,
                      "enabled": true
                    }
                    """.formatted(configCategoryCode))), 200);
        String configCategoryId = configCategory.path("id").asText();

        JsonNode configItem = responseData(mockMvc.perform(asAdmin(post("/api/v1/system-configs/items"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId": "%s",
                      "configKey": "lifecycle.config.%d",
                      "configName": "Lifecycle Config Item",
                      "configValue": "true",
                      "valueType": "BOOLEAN",
                      "sortOrder": 1,
                      "enabled": true,
                      "remarks": "created by integration test"
                    }
                    """.formatted(configCategoryId, System.nanoTime()))), 200);
        String configItemId = configItem.path("id").asText();

        responseData(mockMvc.perform(asAdmin(delete("/api/v1/system-configs/items/{id}", configItemId))), 200);
        responseData(mockMvc.perform(asAdmin(delete("/api/v1/system-configs/categories/{id}", configCategoryId))), 200);

        String templateCategoryCode = "TPL-CAT-" + System.nanoTime();
        JsonNode templateCategory = responseData(mockMvc.perform(asAdmin(post("/api/v1/sampling-templates/categories"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryCode": "%s",
                      "categoryName": "Lifecycle Template Category",
                      "sortOrder": 1,
                      "enabled": true
                    }
                    """.formatted(templateCategoryCode))), 200);
        String templateCategoryId = templateCategory.path("id").asText();

        JsonNode template = responseData(mockMvc.perform(asAdmin(post("/api/v1/sampling-templates"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId": "%s",
                      "templateCode": "TPL-LIFE-%d",
                      "templateName": "Lifecycle Template",
                      "templateContent": "template content",
                      "splitPartCount": 1,
                      "applicableSpecimenType": "ROUTINE",
                      "enabled": true,
                      "bodyPartIds": ["%s"]
                    }
                    """.formatted(templateCategoryId, System.nanoTime(), bodyPartId))), 200);
        String templateId = template.path("id").asText();

        mockMvc.perform(asAdmin(patch("/api/v1/sampling-templates/{id}/enabled", templateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "enabled": false
                    }
                    """)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(templateId))
            .andExpect(jsonPath("$.data.enabled").value(false));

        responseData(mockMvc.perform(asAdmin(delete("/api/v1/sampling-templates/{id}", templateId))), 200);
        responseData(mockMvc.perform(asAdmin(delete("/api/v1/sampling-templates/categories/{id}", templateCategoryId))), 200);

        String guidelineCategoryCode = "GL-CAT-" + System.nanoTime();
        JsonNode guidelineCategory = responseData(mockMvc.perform(asAdmin(post("/api/v1/sampling-guidelines/categories"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryCode": "%s",
                      "categoryName": "Lifecycle Guideline Category",
                      "sortOrder": 1,
                      "enabled": true
                    }
                    """.formatted(guidelineCategoryCode))), 200);
        String guidelineCategoryId = guidelineCategory.path("id").asText();

        JsonNode guideline = responseData(mockMvc.perform(asAdmin(post("/api/v1/sampling-guidelines"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId": "%s",
                      "guidelineCode": "GL-LIFE-%d",
                      "guidelineName": "Lifecycle Guideline",
                      "guidelineContent": "guideline content",
                      "versionNo": "v1",
                      "enabled": true
                    }
                    """.formatted(guidelineCategoryId, System.nanoTime()))), 200);
        String guidelineId = guideline.path("id").asText();

        mockMvc.perform(asAdmin(patch("/api/v1/sampling-guidelines/{id}/enabled", guidelineId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "enabled": false
                    }
                    """)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(guidelineId))
            .andExpect(jsonPath("$.data.enabled").value(false));

        responseData(mockMvc.perform(asAdmin(delete("/api/v1/sampling-guidelines/{id}", guidelineId))), 200);
        responseData(mockMvc.perform(asAdmin(delete("/api/v1/sampling-guidelines/categories/{id}", guidelineCategoryId))), 200);
        responseData(mockMvc.perform(asAdmin(delete("/api/v1/body-parts/{id}", bodyPartId))), 200);
    }

    @Test
    void shouldManageSpecimenDictionaryLifecycleThroughSystemConfigApis() throws Exception {
        String systemCode = "SPECIMEN_SYSTEM_IT_" + System.nanoTime();
        JsonNode systemCategory = responseData(mockMvc.perform(asAdmin(post("/api/v1/system-configs/specimen-dictionary/categories"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "parentId": "SCC_SPECIMEN_DICTIONARY",
                      "categoryCode": "%s",
                      "categoryName": "集成测试标本系统",
                      "sortOrder": 91,
                      "enabled": true
                    }
                    """.formatted(systemCode))), 200);
        String systemCategoryId = systemCategory.path("id").asText();

        String partCode = "SPECIMEN_PART_IT_" + System.nanoTime();
        JsonNode partCategory = responseData(mockMvc.perform(asAdmin(post("/api/v1/system-configs/specimen-dictionary/categories"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "parentId": "%s",
                      "categoryCode": "%s",
                      "categoryName": "集成测试标本部位",
                      "sortOrder": 11,
                      "enabled": true
                    }
                    """.formatted(systemCategoryId, partCode))), 200);
        String partCategoryId = partCategory.path("id").asText();

        String itemKey = "specimen.dictionary.item.it." + System.nanoTime();
        JsonNode item = responseData(mockMvc.perform(asAdmin(post("/api/v1/system-configs/specimen-dictionary/items"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "partCategoryId": "%s",
                      "configKey": "%s",
                      "specimenName": "集成测试标本项",
                      "sortOrder": 3,
                      "enabled": true,
                      "remarks": "integration test",
                      "departmentIds": ["DEPT-GYNE", "DEPT-GENERAL"]
                    }
                    """.formatted(partCategoryId, itemKey))), 200);
        String itemId = item.path("id").asText();

        assertThat(item.path("departmentIds"))
            .extracting(JsonNode::asText)
            .containsExactly("DEPT-GYNE", "DEPT-GENERAL");

        JsonNode tree = responseData(mockMvc.perform(asAdmin(get("/api/v1/system-configs/specimen-dictionary"))), 200);
        assertThat(tree.findValuesAsText("id")).contains(systemCategoryId, partCategoryId, itemId);
        assertThat(tree.findValuesAsText("configKey")).contains(itemKey);

        JsonNode updatedSystem = responseData(mockMvc.perform(asAdmin(patch("/api/v1/system-configs/specimen-dictionary/categories/{id}", systemCategoryId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "parentId": "SCC_SPECIMEN_DICTIONARY",
                      "categoryCode": null,
                      "categoryName": "集成测试标本系统-更新",
                      "sortOrder": 92,
                      "enabled": true
                    }
                    """)), 200);
        assertThat(updatedSystem.path("categoryName").asText()).isEqualTo("集成测试标本系统-更新");

        JsonNode updatedItem = responseData(mockMvc.perform(asAdmin(patch("/api/v1/system-configs/specimen-dictionary/items/{id}", itemId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "specimenName": "集成测试标本项-更新",
                      "sortOrder": 4,
                      "enabled": true,
                      "remarks": "integration test updated",
                      "departmentIds": ["DEPT-GYNE"]
                    }
                    """)), 200);
        assertThat(updatedItem.path("specimenName").asText()).isEqualTo("集成测试标本项-更新");
        assertThat(updatedItem.path("departmentIds"))
            .extracting(JsonNode::asText)
            .containsExactly("DEPT-GYNE");

        responseData(mockMvc.perform(asAdmin(delete("/api/v1/system-configs/specimen-dictionary/items/{id}", itemId))), 200);
        responseData(mockMvc.perform(asAdmin(delete("/api/v1/system-configs/specimen-dictionary/categories/{id}", partCategoryId))), 200);
        responseData(mockMvc.perform(asAdmin(delete("/api/v1/system-configs/specimen-dictionary/categories/{id}", systemCategoryId))), 200);

        JsonNode deletedTree = responseData(mockMvc.perform(asAdmin(get("/api/v1/system-configs/specimen-dictionary"))), 200);
        assertThat(deletedTree.findValuesAsText("id"))
            .doesNotContain(systemCategoryId, partCategoryId, itemId);
        assertThat(deletedTree.findValuesAsText("configKey"))
            .doesNotContain(itemKey);
    }

    @Test
    void shouldManageMedicalOrderDictionaryChargeAndPackageLifecycles() throws Exception {
        JsonNode category = responseData(mockMvc.perform(asAdmin(post("/api/v1/medical-order-dicts/categories"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryCode": "ODC-LIFE-%d",
                      "categoryName": "Lifecycle Order Category",
                      "sortOrder": 1,
                      "enabled": true
                    }
                    """.formatted(System.nanoTime()))), 200);
        String categoryId = category.path("id").asText();

        JsonNode item = responseData(mockMvc.perform(asAdmin(post("/api/v1/medical-order-dicts/items"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId": "%s",
                      "orderItemCode": "ODI-LIFE-%d",
                      "orderItemName": "Lifecycle Order Item",
                      "orderType": "ROUTINE",
                      "defaultContent": "default content",
                      "executionScope": "GLOBAL",
                      "sortOrder": 1,
                      "enabled": true
                    }
                    """.formatted(categoryId, System.nanoTime()))), 200);
        String itemId = item.path("id").asText();

        mockMvc.perform(asAdmin(get("/api/v1/medical-order-dicts")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").isNotEmpty());

        JsonNode charge = responseData(mockMvc.perform(asAdmin(post("/api/v1/medical-order-charge-items"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "orderDictItemId": "%s",
                      "chargeItemCode": "OCI-LIFE-%d",
                      "chargeItemName": "Lifecycle Charge Item",
                      "specification": "1 unit",
                      "unit": "each",
                      "price": 12.5,
                      "sortOrder": 1,
                      "enabled": true
                    }
                    """.formatted(itemId, System.nanoTime()))), 200);
        String chargeId = charge.path("id").asText();

        JsonNode medicalPackage = responseData(mockMvc.perform(asAdmin(post("/api/v1/medical-order-packages"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "packageCode": "PKG-LIFE-%d",
                      "packageName": "Lifecycle Package",
                      "packageType": "PUBLIC",
                      "enabled": true,
                      "remarks": "lifecycle",
                      "itemIds": ["%s"]
                    }
                    """.formatted(System.nanoTime(), itemId))), 200);
        String packageId = medicalPackage.path("id").asText();

        mockMvc.perform(asAdmin(get("/api/v1/medical-order-charge-items")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").isNotEmpty());

        mockMvc.perform(asAdmin(get("/api/v1/medical-order-packages")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").isNotEmpty());

        mockMvc.perform(asAdmin(patch("/api/v1/medical-order-charge-items/{id}/enabled", chargeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "enabled": false
                    }
                    """)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(chargeId))
            .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(asAdmin(patch("/api/v1/medical-order-packages/{id}/enabled", packageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "enabled": false
                    }
                    """)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(packageId))
            .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(asAdmin(patch("/api/v1/medical-order-dicts/items/{id}/enabled", itemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "enabled": false
                    }
                    """)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(itemId))
            .andExpect(jsonPath("$.data.enabled").value(false));

        responseData(mockMvc.perform(asAdmin(delete("/api/v1/medical-order-packages/{id}", packageId))), 200);
        responseData(mockMvc.perform(asAdmin(delete("/api/v1/medical-order-charge-items/{id}", chargeId))), 200);
        responseData(mockMvc.perform(asAdmin(delete("/api/v1/medical-order-dicts/items/{id}", itemId))), 200);
        responseData(mockMvc.perform(asAdmin(delete("/api/v1/medical-order-dicts/categories/{id}", categoryId))), 200);
    }

    private JsonNode responseData(ResultActions resultActions, int expectedStatus) throws Exception {
        String response = resultActions
            .andExpect(status().is(expectedStatus))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(response);
        assertThat(root.path("code").asText()).isEqualTo("SUCCESS");
        return root.path("data");
    }
}
