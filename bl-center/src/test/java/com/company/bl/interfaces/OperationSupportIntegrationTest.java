package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class OperationSupportIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    private static final String USER_M1_REAGENT = "USER_M1_REAGENT";

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
              "operatorName":"reagent-user",
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
              "storageLocation":"Shelf-A",
              "operatorName":"reagent-user"
            }
            """.formatted(reagentId, LocalDate.now().plusDays(180))), 200);

        JsonNode nearExpiryStock = responseBody(postJson("/api/v1/reagent-stocks", USER_M1_REAGENT, """
            {
              "reagentId":"%s",
              "batchNo":"BATCH-EXP-1",
              "stockQuantity":50,
              "stockStatus":"ACTIVE",
              "expiryDate":"%s",
              "storageLocation":"Shelf-B",
              "operatorName":"reagent-user"
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
                  "nearExpiryDays":3,
                  "operatorName":"reagent-user"
                }
                """.formatted(LocalDate.now().plusDays(10)))), 200);

        JsonNode warningsAfterUpdate = responseBody(mockMvc.perform(authorized(get("/api/v1/reagent-stocks/warnings"), USER_M1_REAGENT)), 200);
        assertThat(warningsAfterUpdate.toString()).contains("LOW_STOCK");
        assertThat(warningsAfterUpdate.toString()).doesNotContain("BATCH-EXP-1");
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
              "operatorName":"reagent-user"
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
              "operatorName":"reagent-user"
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
              "operatorName":"reagent-user",
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
}
