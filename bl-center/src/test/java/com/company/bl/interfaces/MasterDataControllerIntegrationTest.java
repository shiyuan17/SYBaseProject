package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.company.bl.support.infrastructure.SupportJdbcRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import db.migration.V38__normalize_seed_department_names;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class MasterDataControllerIntegrationTest extends AuthenticatedWebIntegrationTest {
    private static final String USER_M1_ADMIN = "USER_M1_ADMIN";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SupportJdbcRepository supportJdbcRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private DataSource dataSource;

    @Test
    void shouldQueryBodyPartsAndTemplateDetails() throws Exception {
        mockMvc.perform(asAdmin(get("/api/v1/departments")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id", is("DEPT_ROOT")))
            .andExpect(jsonPath("$.data[0].departmentName", is("\u5168\u90e8\u79d1\u5ba4")));

        mockMvc.perform(asAdmin(get("/api/v1/body-parts")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id", is("BP_ROOT")));

        mockMvc.perform(asAdmin(get("/api/v1/sampling-templates/ST_HE_STOMACH")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id", is("ST_HE_STOMACH")))
            .andExpect(jsonPath("$.data.bodyParts.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    void shouldNormalizeLegacyEnglishSeedDepartmentNamesWithoutOverwritingCustomNames() throws Exception {
        jdbcTemplate.update("""
            update department_dict
            set department_name = :departmentName
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", "DEPT_ROOT")
            .addValue("departmentName", "All Departments"));
        jdbcTemplate.update("""
            update department_dict
            set department_name = :departmentName
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", "DEPT_CLINICAL")
            .addValue("departmentName", "Clinical Departments"));
        jdbcTemplate.update("""
            update department_dict
            set department_name = :departmentName
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", "DEPT_OR")
            .addValue("departmentName", "\u81ea\u5b9a\u4e49\u624b\u672f\u5ba4"));
        jdbcTemplate.update("""
            update department_dict
            set department_name = :departmentName
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", "DEPT_ICU")
            .addValue("departmentName", "Intensive Care Unit"));
        jdbcTemplate.update("""
            update department_dict
            set department_name = :departmentName
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", "DEPT_PATH")
            .addValue("departmentName", "Pathology Department"));

        try (Connection connection = dataSource.getConnection()) {
            new V38__normalize_seed_department_names().migrate(new Context() {
                @Override
                public Configuration getConfiguration() {
                    return null;
                }

                @Override
                public Connection getConnection() {
                    return connection;
                }
            });
        }

        assertEquals(
            "\u5168\u90e8\u79d1\u5ba4",
            jdbcTemplate.queryForObject(
                "select department_name from department_dict where id = :id",
                Map.of("id", "DEPT_ROOT"),
                String.class));
        assertEquals(
            "\u4e34\u5e8a\u79d1\u5ba4",
            jdbcTemplate.queryForObject(
                "select department_name from department_dict where id = :id",
                Map.of("id", "DEPT_CLINICAL"),
                String.class));
        assertEquals(
            "\u81ea\u5b9a\u4e49\u624b\u672f\u5ba4",
            jdbcTemplate.queryForObject(
                "select department_name from department_dict where id = :id",
                Map.of("id", "DEPT_OR"),
                String.class));
        assertEquals(
            "\u91cd\u75c7\u76d1\u62a4\u5ba4",
            jdbcTemplate.queryForObject(
                "select department_name from department_dict where id = :id",
                Map.of("id", "DEPT_ICU"),
                String.class));
        assertEquals(
            "\u75c5\u7406\u79d1",
            jdbcTemplate.queryForObject(
                "select department_name from department_dict where id = :id",
                Map.of("id", "DEPT_PATH"),
                String.class));
    }
    @Test
    void shouldCreatePackageUpdateConfigAndQueryPagedResources() throws Exception {
        String packageCode = "PK-" + System.nanoTime();
        mockMvc.perform(asAdmin(post("/api/v1/medical-order-packages"))
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

        mockMvc.perform(asAdmin(get("/api/v1/medical-order-packages/page"))
                .param("page", "1")
                .param("size", "20")
                .param("keyword", packageCode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data.items[0].packageCode", is(packageCode)));

        mockMvc.perform(asAdmin(get("/api/v1/medical-order-charge-items/page"))
                .param("page", "1")
                .param("size", "20")
                .param("orderDictItemId", "ODI_HE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data.items[0].orderDictItemId", is("ODI_HE")));

        mockMvc.perform(asAdmin(patch("/api/v1/system-configs/items/SCI_TEMPLATE_MATCH"))
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
        mockMvc.perform(asAdmin(get("/api/v1/numbering-rules")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(20)));

        mockMvc.perform(asAdmin(patch("/api/v1/numbering-rules/NR_APPLICATION"))
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
    @Test
    void shouldUpdateDeleteAndImportExportMasterDataResources() throws Exception {
        mockMvc.perform(asAdmin(post("/api/v1/body-parts"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "parentId": "BP_ROOT",
                      "partName": "临时部位",
                      "partLevel": 1,
                      "sortOrder": 99,
                      "enabled": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.partCode", startsWith("BP-")));

        mockMvc.perform(asAdmin(patch("/api/v1/body-parts/BP_STOMACH"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "parentId": "BP_DIGESTIVE",
                      "partCode": null,
                      "partName": "胃",
                      "partAlias": "胃窦",
                      "partLevel": 2,
                      "sortOrder": 10,
                      "enabled": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.partAlias", is("胃窦")));

        MvcResult departmentResult = mockMvc.perform(asAdmin(post("/api/v1/departments"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "parentId": "DEPT_ROOT",
                      "departmentName": "临时科室",
                      "sortOrder": 99,
                      "enabled": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.departmentCode", startsWith("DEPT-")))
            .andReturn();
        String departmentCode = objectMapper.readTree(departmentResult.getResponse().getContentAsString())
            .path("data")
            .path("departmentCode")
            .asText();
        String departmentId = objectMapper.readTree(departmentResult.getResponse().getContentAsString())
            .path("data")
            .path("id")
            .asText();

        mockMvc.perform(asAdmin(patch("/api/v1/departments/{id}", departmentId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "parentId": "DEPT_ROOT",
                      "departmentCode": null,
                      "departmentName": "临时科室-更新",
                      "sortOrder": 100,
                      "enabled": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.departmentName", is("临时科室-更新")));

        mockMvc.perform(asAdmin(patch("/api/v1/departments/{id}", departmentId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "parentId": "DEPT_ROOT",
                      "departmentCode": "DEPT-MUTATED",
                      "departmentName": "Updated Department",
                      "sortOrder": 100,
                      "enabled": true
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("cannot be changed once created")));

        mockMvc.perform(asAdmin(delete("/api/v1/departments/{id}", departmentId)))
            .andExpect(status().isOk());

        mockMvc.perform(asAdmin(post("/api/v1/medical-order-dicts/categories"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryName": "临时分类",
                      "sortOrder": 1,
                      "enabled": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.categoryCode", startsWith("ODC-")));

        mockMvc.perform(asAdmin(patch("/api/v1/medical-order-dicts/items/ODI_HE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId": "ODC_ROUTINE",
                      "orderItemCode": null,
                      "orderItemName": "HE 染色",
                      "orderType": "ROUTINE",
                      "defaultContent": "更新默认内容",
                      "executionScope": "GLOBAL",
                      "sortOrder": 10,
                      "enabled": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.defaultContent", is("更新默认内容")));

        mockMvc.perform(asAdmin(patch("/api/v1/medical-order-charge-items/OCI_HE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "orderDictItemId": "ODI_HE",
                      "chargeItemCode": null,
                      "chargeItemName": "HE 收费",
                      "specification": "张",
                      "unit": "次",
                      "price": 35.5,
                      "sortOrder": 1,
                      "enabled": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.price", is(35.5)));

        mockMvc.perform(asAdmin(get("/api/v1/medical-order-charge-items/export")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("chargeItemCode")));

        MockMultipartFile chargeFile = new MockMultipartFile(
            "file",
            "charges.csv",
            "text/csv",
            """
                orderDictItemId,chargeItemCode,chargeItemName,specification,unit,price,sortOrder,enabled
                ODI_HE,IMPORT_CHARGE,导入收费,张,次,12.5,1,true
                """.getBytes());
        mockMvc.perform(asAdmin(multipart("/api/v1/medical-order-charge-items/import").file(chargeFile)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount", is(1)));

        MvcResult packageResult = mockMvc.perform(asAdmin(post("/api/v1/medical-order-packages"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "packageName": "临时套餐",
                      "packageType": "PUBLIC",
                      "enabled": true,
                      "itemIds": ["ODI_HE"]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.packageCode", startsWith("PKG-")))
            .andReturn();
        String packageId = objectMapper.readTree(packageResult.getResponse().getContentAsString()).path("data").path("id").asText();
        String packageCode = objectMapper.readTree(packageResult.getResponse().getContentAsString()).path("data").path("packageCode").asText();

        mockMvc.perform(asAdmin(patch("/api/v1/medical-order-packages/{id}", packageId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "packageCode": null,
                      "packageName": "基础取材套餐",
                      "packageType": "PUBLIC",
                      "enabled": true,
                      "remarks": "updated",
                      "itemIds": ["ODI_HE"]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.remarks", is("updated")))
            .andExpect(jsonPath("$.data.packageCode", is(packageCode)));

        mockMvc.perform(asAdmin(patch("/api/v1/sampling-templates/ST_HE_STOMACH"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId": "STC_ROUTINE",
                      "templateCode": null,
                      "templateName": "胃组织模板",
                      "templateContent": "更新模板内容",
                      "splitPartCount": 2,
                      "applicableSpecimenType": "活检",
                      "enabled": true,
                      "bodyPartIds": ["BP_STOMACH"]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.templateContent", is("更新模板内容")));

        mockMvc.perform(asAdmin(patch("/api/v1/sampling-guidelines/SG_STOMACH"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId": "SGC_ROUTINE",
                      "guidelineCode": null,
                      "guidelineName": "胃取材规范",
                      "guidelineContent": "更新规范内容",
                      "versionNo": "v2",
                      "enabled": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.versionNo", is("v2")));

        mockMvc.perform(asAdmin(patch("/api/v1/system-configs/categories/SCC_GENERAL"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "parentId": "SCC_ROOT",
                      "categoryCode": null,
                      "categoryName": "通用配置",
                      "categoryType": "BIZ",
                      "sortOrder": 1,
                      "enabled": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.categoryCode", is("GENERAL")));
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder requestBuilder) {
        return authorized(requestBuilder, USER_M1_ADMIN);
    }
}
