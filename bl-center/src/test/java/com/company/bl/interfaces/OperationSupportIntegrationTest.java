package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class OperationSupportIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    private static final String USER_M1_REAGENT = "USER_M1_REAGENT";
    private static final String USER_M1_ARCHIVE = "USER_M1_ARCHIVE";

    @Test
    void shouldMaintainReagentLedgerStocksAndWarnings() throws Exception {
        JsonNode reagent = responseBody(postJson("/api/v1/reagents", USER_M1_REAGENT, """
            {
              "reagentCode":"RG-M5-OPS-001",
              "reagentName":"Neutral Gum",
              "specification":"250ml",
              "unit":"bottle",
              "manufacturer":"Path Supplier",
              "defaultLowStockThreshold":10,
              "defaultNearExpiryDays":30,
              "enabled":true,
              "remarks":"initial create"
            }
            """), 200);
        String reagentId = reagent.path("id").asText();

        responseBody(postJson("/api/v1/reagent-stocks", USER_M1_REAGENT, """
            {
              "reagentId":"%s",
              "batchNo":"BATCH-LOW-1",
              "stockQuantity":5,
              "stockStatus":"ACTIVE",
              "expiryDate":"%s",
              "storageLocation":"Shelf-A"
            }
            """.formatted(reagentId, LocalDate.now().plusDays(180))), 200);

        JsonNode nearExpiryStock = responseBody(postJson("/api/v1/reagent-stocks", USER_M1_REAGENT, """
            {
              "reagentId":"%s",
              "batchNo":"BATCH-EXP-1",
              "stockQuantity":50,
              "stockStatus":"ACTIVE",
              "expiryDate":"%s",
              "storageLocation":"Shelf-B"
            }
            """.formatted(reagentId, LocalDate.now().plusDays(5))), 200);

        JsonNode stocks = responseBody(mockMvc.perform(authorized(get("/api/v1/reagent-stocks"), USER_M1_REAGENT)
            .param("keyword", "RG-M5-OPS-001")), 200);
        assertThat(stocks).hasSize(2);

        JsonNode warnings = responseBody(mockMvc.perform(authorized(get("/api/v1/reagent-stocks/warnings"), USER_M1_REAGENT)), 200);
        assertThat(warnings.toString()).contains("LOW_STOCK");
        assertThat(warnings.toString()).contains("NEAR_EXPIRY");

        responseBody(mockMvc.perform(authorized(patch("/api/v1/reagent-stocks/{id}", nearExpiryStock.path("id").asText()), USER_M1_REAGENT)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "stockQuantity":60,
                  "stockStatus":"ACTIVE",
                  "expiryDate":"%s",
                  "storageLocation":"Shelf-C",
                  "nearExpiryDays":3
                }
                """.formatted(LocalDate.now().plusDays(10)))), 200);

        JsonNode warningsAfterUpdate = responseBody(mockMvc.perform(authorized(get("/api/v1/reagent-stocks/warnings"), USER_M1_REAGENT)), 200);
        assertThat(warningsAfterUpdate.toString()).contains("LOW_STOCK");
        assertThat(warningsAfterUpdate.toString()).doesNotContain("BATCH-EXP-1");
    }

    @Test
    void shouldMaintainReagentTemplateInventoryActionsAndCsvExchange() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        JsonNode template = responseBody(postJson("/api/v1/reagents", USER_M1_REAGENT, """
            {
              "reagentCode":"RG-LEGACY-%s",
              "reagentName":"CK Working Solution",
              "specification":"6ml",
              "unit":"bottle",
              "manufacturer":"Path Supplier",
              "reagentType":"IMMUNO_WORKING_SOLUTION",
              "reagentUsage":"IHC",
              "orderDictItemId":"ODI_IHC_CK",
              "cloneNo":"AE1/AE3",
              "recommendedDilution":"1:100",
              "applicationDilution":"1:80",
              "templateStatus":"ENABLED",
              "validityDays":365,
              "defaultStockThreshold":2,
              "stainCapacity":120,
              "stainThreshold":12,
              "remarks":"legacy template"
            }
            """.formatted(suffix)), 200);
        assertThat(template.path("reagentType").asText()).isEqualTo("IMMUNO_WORKING_SOLUTION");
        assertThat(template.path("orderDictItemId").asText()).isEqualTo("ODI_IHC_CK");
        assertThat(template.path("orderItemName").asText()).isEqualTo("CK");
        assertThat(template.path("templateStatus").asText()).isEqualTo("ENABLED");
        String reagentId = template.path("id").asText();

        JsonNode listedTemplates = responseBody(mockMvc.perform(authorized(get("/api/v1/reagents"), USER_M1_REAGENT)
            .param("keyword", "CK")
            .param("reagentType", "IMMUNO_WORKING_SOLUTION")
            .param("templateStatus", "ENABLED")), 200);
        assertThat(listedTemplates).anySatisfy(item -> assertThat(item.path("reagentCode").asText()).isEqualTo("RG-LEGACY-" + suffix));

        JsonNode stock = responseBody(postJson("/api/v1/reagent-stocks", USER_M1_REAGENT, """
            {
              "reagentId":"%s",
              "batchNo":"BATCH-LEGACY-%s",
              "initialQuantity":10,
              "stockQuantity":10,
              "remainingQuantity":10,
              "stockStatus":"IN_STOCK",
              "productionDate":"%s",
              "inboundAt":"%s",
              "expiryDate":"%s",
              "storageLocation":"Cold-01",
              "testReminderThreshold":3,
              "expiryReminderThreshold":20,
              "recommendedDilution":"1:100",
              "applicationDilution":"1:80",
              "stainCapacity":120,
              "stainThreshold":12,
              "validityDays":365,
              "remarks":"legacy inbound"
            }
            """.formatted(reagentId, suffix, LocalDate.now().minusDays(1),
            LocalDateTime.now().minusHours(2).withNano(0), LocalDate.now().plusDays(180))), 200);
        String stockId = stock.path("id").asText();
        assertThat(stock.path("remainingQuantity").decimalValue()).isEqualByComparingTo("10");
        assertThat(stock.path("stockStatus").asText()).isEqualTo("IN_STOCK");

        JsonNode tested = responseBody(postJson("/api/v1/reagent-stocks/%s/test".formatted(stockId), USER_M1_REAGENT, """
            {"quantity":1,"remarks":"QC passed"}
            """), 200);
        assertThat(tested.path("remainingQuantity").decimalValue()).isEqualByComparingTo("9");
        assertThat(tested.path("stockStatus").asText()).isEqualTo("TESTED");

        JsonNode inUse = responseBody(postJson("/api/v1/reagent-stocks/%s/start-use".formatted(stockId), USER_M1_REAGENT, """
            {"remarks":"start staining"}
            """), 200);
        assertThat(inUse.path("stockStatus").asText()).isEqualTo("IN_USE");

        JsonNode consumed = responseBody(postJson("/api/v1/reagent-stocks/%s/consume".formatted(stockId), USER_M1_REAGENT, """
            {"quantity":4,"remarks":"4 slides"}
            """), 200);
        assertThat(consumed.path("remainingQuantity").decimalValue()).isEqualByComparingTo("5");

        postJson("/api/v1/reagent-stocks/%s/consume".formatted(stockId), USER_M1_REAGENT, """
            {"quantity":6,"remarks":"over consume"}
            """).andExpect(status().isBadRequest());

        JsonNode finished = responseBody(postJson("/api/v1/reagent-stocks/%s/finish-use".formatted(stockId), USER_M1_REAGENT, """
            {"remarks":"empty bottle"}
            """), 200);
        assertThat(finished.path("stockStatus").asText()).isEqualTo("FINISHED");

        postJson("/api/v1/reagent-stocks/%s/consume".formatted(stockId), USER_M1_REAGENT, """
            {"quantity":1,"remarks":"cannot consume finished"}
            """).andExpect(status().isBadRequest());

        JsonNode events = responseBody(mockMvc.perform(authorized(get("/api/v1/reagent-stocks/{id}/events", stockId), USER_M1_REAGENT)), 200);
        assertThat(events).hasSizeGreaterThanOrEqualTo(5);
        assertThat(events.toString()).contains("INBOUND", "TEST", "START_USE", "CONSUME", "FINISH_USE");

        JsonNode filteredStocks = responseBody(mockMvc.perform(authorized(get("/api/v1/reagent-stocks"), USER_M1_REAGENT)
            .param("keyword", "LEGACY")
            .param("reagentType", "IMMUNO_WORKING_SOLUTION")
            .param("stockStatus", "FINISHED")
            .param("dateFrom", LocalDate.now().minusDays(2).toString())
            .param("dateTo", LocalDate.now().plusDays(1).toString())), 200);
        assertThat(filteredStocks).anySatisfy(item -> assertThat(item.path("id").asText()).isEqualTo(stockId));

        mockMvc.perform(authorized(get("/api/v1/reagent-stocks/export"), USER_M1_REAGENT)
            .param("keyword", "LEGACY"))
            .andExpect(status().isOk())
            .andExpect(result -> {
                byte[] content = result.getResponse().getContentAsByteArray();
                assertThat(content).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
                assertThat(new String(content, java.nio.charset.StandardCharsets.UTF_8))
                    .contains("试剂编码", "批号", "RG-LEGACY-" + suffix, "BATCH-LEGACY-" + suffix);
            });

        MockMultipartFile importFile = new MockMultipartFile(
            "file",
            "reagent-stocks.csv",
            "text/csv",
            ("\uFEFF试剂编码,试剂名称,批号,初始数量,当前剩余量,库存状态,生产日期,有效期,库位\n"
                + "RG-LEGACY-%s,CK Working Solution,BATCH-CSV-%s,3,3,IN_STOCK,%s,%s,Cold-CSV\n")
                .formatted(suffix, suffix, LocalDate.now(), LocalDate.now().plusDays(120))
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        JsonNode importResult = responseBody(mockMvc.perform(authorized(multipart("/api/v1/reagent-stocks/import").file(importFile), USER_M1_REAGENT)), 200);
        assertThat(importResult.path("successCount").asInt()).isEqualTo(1);
        assertThat(importResult.path("failureCount").asInt()).isZero();

        MockMultipartFile invalidImportFile = new MockMultipartFile(
            "file",
            "reagent-stocks-invalid.csv",
            "text/csv",
            ("\uFEFF试剂编码,试剂名称,批号,初始数量,当前剩余量,库存状态\n"
                + "MISSING-CODE,,BATCH-MISSING-%s,1,1,IN_STOCK\n")
                .formatted(suffix)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        JsonNode invalidImportResult = responseBody(mockMvc.perform(authorized(multipart("/api/v1/reagent-stocks/import").file(invalidImportFile), USER_M1_REAGENT)), 200);
        assertThat(invalidImportResult.path("successCount").asInt()).isZero();
        assertThat(invalidImportResult.path("failureCount").asInt()).isEqualTo(1);
        assertThat(invalidImportResult.path("errors").get(0).path("message").asText()).contains("Reagent template not found");
    }

    @Test
    void shouldReuseReagentPermissionsForInventoryImportExportAndActions() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/reagent-stocks/export"), USER_M1_REAGENT))
            .andExpect(status().isOk());

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "reagent-stocks.csv",
            "text/csv",
            "\uFEFF试剂编码,批号\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mockMvc.perform(authorized(multipart("/api/v1/reagent-stocks/import").file(file), USER_M1_ARCHIVE))
            .andExpect(status().isForbidden());

        mockMvc.perform(authorized(get("/api/v1/reagent-stocks/export"), USER_M1_ARCHIVE))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldMaintainEquipmentLedgerMaintenanceLogsAndWarnings() throws Exception {
        LocalDateTime dueSoon = LocalDateTime.now().plusDays(2).withNano(0);
        LocalDateTime overdue = LocalDateTime.now().minusDays(1).withNano(0);
        LocalDateTime nextMaintenance = LocalDateTime.now().plusDays(30).withNano(0);

        JsonNode dueSoonEquipment = responseBody(postJson("/api/v1/equipment-records", USER_M1_REAGENT, """
            {
              "equipmentCode":"EQ-M5-OPS-001",
              "equipmentName":"Microtome A",
              "equipmentCategory":"MICROTOME",
              "modelNo":"MT-100",
              "equipmentStatus":"ACTIVE",
              "locationDescription":"Lab-1",
              "enabledAt":"%s",
              "nextMaintenanceAt":"%s",
              "commonlyUsed":true
            }
            """.formatted(LocalDateTime.now().minusDays(30).withNano(0), dueSoon)), 200);
        String equipmentId = dueSoonEquipment.path("id").asText();

        responseBody(postJson("/api/v1/equipment-records", USER_M1_REAGENT, """
            {
              "equipmentCode":"EQ-M5-OPS-002",
              "equipmentName":"Stainer B",
              "equipmentCategory":"STAINER",
              "modelNo":"ST-200",
              "equipmentStatus":"ACTIVE",
              "locationDescription":"Lab-2",
              "enabledAt":"%s",
              "nextMaintenanceAt":"%s",
              "commonlyUsed":false
            }
            """.formatted(LocalDateTime.now().minusDays(60).withNano(0), overdue)), 200);

        JsonNode warnings = responseBody(mockMvc.perform(authorized(get("/api/v1/equipment-records/warnings"), USER_M1_REAGENT)), 200);
        assertThat(warnings.toString()).contains("DUE_SOON");
        assertThat(warnings.toString()).contains("OVERDUE");

        responseBody(postJson("/api/v1/equipment-records/%s/maintenance-logs".formatted(equipmentId), USER_M1_REAGENT, """
            {
              "maintenanceType":"MAINTENANCE",
              "maintenanceStatus":"COMPLETED",
              "performedAt":"%s",
              "nextMaintenanceAt":"%s",
              "description":"routine maintenance"
            }
            """.formatted(LocalDateTime.now().withNano(0), nextMaintenance)), 200);

        JsonNode maintenanceLogs = responseBody(mockMvc.perform(authorized(
            get("/api/v1/equipment-records/{id}/maintenance-logs", equipmentId), USER_M1_REAGENT)), 200);
        assertThat(maintenanceLogs).hasSize(1);
        assertThat(maintenanceLogs.get(0).path("maintenanceStatus").asText()).isEqualTo("COMPLETED");

        JsonNode equipmentRecords = responseBody(mockMvc.perform(authorized(get("/api/v1/equipment-records"), USER_M1_REAGENT)
            .param("keyword", "EQ-M5-OPS-001")), 200);
        assertThat(equipmentRecords).hasSize(1);
        assertThat(equipmentRecords.get(0).path("nextMaintenanceAt").asText()).isEqualTo(nextMaintenance.toString());
    }

    @Test
    void shouldSupportExpandedEquipmentFieldsAndBatchStatusUpdate() throws Exception {
        JsonNode equipment = responseBody(postJson("/api/v1/equipment-records", USER_M1_REAGENT, """
            {
              "equipmentCode":"EQ-M5-LEGACY-%d",
              "equipmentName":"Legacy Equipment",
              "equipmentCategory":"MICROTOME",
              "modelNo":"LEGACY-01",
              "equipmentStatus":"ACTIVE",
              "locationDescription":"Room-Legacy",
              "enabledAt":"%s",
              "quantity":2,
              "purchaseDate":"2026-06-01",
              "purchaserName":"采购员甲",
              "purchaserCode":"BUY-01",
              "managementUnit":"归口单位甲",
              "managementCode":"GL-001",
              "useUnit":"使用单位甲",
              "principalCode":"FZR-001",
              "principalName":"负责人甲",
              "userName":"使用人甲",
              "productionDate":"2026-05-01",
              "warrantyEndDate":"2027-05-01",
              "factoryNo":"FC-001",
              "depreciationMethod":"直线法",
              "serviceLifeYears":5,
              "price":12345.67,
              "manufacturer":"厂家甲",
              "portNo":"COM1",
              "ipAddress":"192.168.1.10",
              "commonStartupTime":"08:00:00",
              "commonShutdownTime":"18:00:00",
              "commonUsageContent":"常规切片",
              "commonlyUsed":true,
              "setTemperature":24.5,
              "currentTemperature":23.5,
              "rfid":"RFID-001"
            }
            """.formatted(System.nanoTime(), LocalDateTime.now().minusDays(3).withNano(0))), 200);
        String equipmentId = equipment.path("id").asText();

        assertThat(equipment.path("managementCode").asText()).isEqualTo("GL-001");
        assertThat(equipment.path("commonlyUsed").asBoolean()).isTrue();
        assertThat(equipment.path("price").asText()).isEqualTo("12345.67");

        JsonNode queried = responseBody(mockMvc.perform(authorized(get("/api/v1/equipment-records"), USER_M1_REAGENT)
            .param("keyword", "RFID-001")), 200);
        assertThat(queried).hasSize(1);
        assertThat(queried.get(0).path("manufacturer").asText()).isEqualTo("厂家甲");

        mockMvc.perform(authorized(post("/api/v1/equipment-records/batch-status"), USER_M1_REAGENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "equipmentIds":["%s"],
                      "equipmentStatus":"DISABLED"
                    }
            """.formatted(equipmentId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(equipmentId))
            .andExpect(jsonPath("$.data[0].equipmentStatus").value("DISABLED"));

        mockMvc.perform(authorized(post("/api/v1/equipment-records/batch-status"), USER_M1_REAGENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "equipmentIds":[],
                      "equipmentStatus":"ACTIVE"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        mockMvc.perform(authorized(post("/api/v1/equipment-records/batch-status"), USER_M1_REAGENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "equipmentIds":["%s"],
                      "equipmentStatus":"MAINTENANCE"
                    }
                    """.formatted(equipmentId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        mockMvc.perform(authorized(post("/api/v1/equipment-records/batch-status"), USER_M1_ARCHIVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "equipmentIds":["%s"],
                      "equipmentStatus":"ACTIVE"
                    }
                    """.formatted(equipmentId)))
            .andExpect(status().isForbidden());
    }
}
