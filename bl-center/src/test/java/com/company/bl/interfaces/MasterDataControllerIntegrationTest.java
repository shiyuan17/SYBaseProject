package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.company.bl.support.infrastructure.SupportJdbcRepository;
import com.company.common.test.BaseWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class MasterDataControllerIntegrationTest extends BaseWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SupportJdbcRepository supportJdbcRepository;

    @Test
    void shouldQueryBodyPartsAndTemplateDetails() throws Exception {
        mockMvc.perform(get("/api/v1/body-parts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id", is("BP_ROOT")));

        mockMvc.perform(get("/api/v1/sampling-templates/ST_HE_STOMACH"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id", is("ST_HE_STOMACH")))
            .andExpect(jsonPath("$.data.bodyParts.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    void shouldCreatePackageUpdateConfigAndQueryPagedResources() throws Exception {
        String packageCode = "PK-" + System.nanoTime();
        mockMvc.perform(post("/api/v1/medical-order-packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "packageCode": "%s",
                      "packageName": "Common Package",
                      "packageType": "PRIVATE",
                      "enabled": true,
                      "itemIds": ["ODI_HE"]
                    }
                    """.formatted(packageCode)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id", notNullValue()))
            .andExpect(jsonPath("$.data.items[0].orderItemId", is("ODI_HE")));

        mockMvc.perform(get("/api/v1/medical-order-packages/page")
                .param("page", "1")
                .param("size", "20")
                .param("keyword", packageCode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data.items[0].packageCode", is(packageCode)));

        mockMvc.perform(get("/api/v1/medical-order-charge-items/page")
                .param("page", "1")
                .param("size", "20")
                .param("orderDictItemId", "ODI_HE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data.items[0].orderDictItemId", is("ODI_HE")));

        mockMvc.perform(patch("/api/v1/system-configs/items/SCI_TEMPLATE_MATCH")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "configValue": "false",
                      "enabled": true,
                      "remarks": "test update"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id", is("SCI_TEMPLATE_MATCH")))
            .andExpect(jsonPath("$.data.configValue", is("false")));
    }

    @Test
    void shouldListUpdateAndAuditNumberingRules() throws Exception {
        mockMvc.perform(get("/api/v1/numbering-rules"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(5)));

        mockMvc.perform(patch("/api/v1/numbering-rules/NR_APPLICATION")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "prefixPattern": "APX",
                      "datePattern": "yyyyMMdd",
                      "seqLength": 4,
                      "resetPolicy": "DAILY",
                      "scopeType": "GLOBAL",
                      "enabled": true,
                      "remarks": "integration test"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id", is("NR_APPLICATION")))
            .andExpect(jsonPath("$.data.prefixPattern", is("APX")));

        assertTrue(supportJdbcRepository.findOperationLogs("SUPPORT").stream().anyMatch(log ->
            "update_numbering_rule".equals(log.get("operation_name"))
                && "SUCCESS".equals(log.get("operation_result"))
                && "NR_APPLICATION".equals(log.get("business_id"))));
    }
}
