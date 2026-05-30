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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class M5SingleApiIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    private static final String USER_M1_ARCHIVE = "USER_M1_ARCHIVE";
    private static final String USER_M1_REAGENT = "USER_M1_REAGENT";

    @Test
    void shouldQueryAndUpdateArchiveCabinetThroughSingleApiEndpoints() throws Exception {
        JsonNode cabinet = responseBody(postJson("/api/v1/archive-cabinets", USER_M1_ARCHIVE, """
            {
              "cabinetCode":"CAB-SINGLE-%d",
              "cabinetName":"Single Api Cabinet",
              "cabinetType":"STANDARD",
              "layerCount":1,
              "slotCountPerLayer":2,
              "terminalCode":"M5-SINGLE-01",
              "locationDescription":"Room-A"
            }
            """.formatted(System.nanoTime())), 200);
        String cabinetId = cabinet.path("id").asText();

        JsonNode listedCabinets = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-cabinets"), USER_M1_ARCHIVE)), 200);
        assertThat(listedCabinets.toString()).contains(cabinetId);

        mockMvc.perform(authorized(patch("/api/v1/archive-cabinets/{id}", cabinetId), USER_M1_ARCHIVE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cabinetName":"Single Api Cabinet Updated",
                      "cabinetStatus":"DISABLED",
                      "terminalCode":"M5-SINGLE-02",
                      "locationDescription":"Room-B",
                      "remarks":"updated by integration test"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(cabinetId))
            .andExpect(jsonPath("$.data.cabinetName").value("Single Api Cabinet Updated"))
            .andExpect(jsonPath("$.data.cabinetStatus").value("DISABLED"));

        JsonNode availablePositions = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-positions/available"), USER_M1_ARCHIVE)
            .param("cabinetId", cabinetId)), 200);
        assertThat(availablePositions).isEmpty();
    }

    @Test
    void shouldQueryAndUpdateReagentAndEquipmentThroughSingleApiEndpoints() throws Exception {
        JsonNode reagent = responseBody(postJson("/api/v1/reagents", USER_M1_REAGENT, """
            {
              "reagentCode":"RG-SINGLE-%d",
              "reagentName":"Single Api Reagent",
              "specification":"100ml",
              "unit":"bottle",
              "manufacturer":"Single Supplier",
              "defaultLowStockThreshold":8,
              "defaultNearExpiryDays":20,
              "enabled":true,
              "remarks":"created"
            }
            """.formatted(System.nanoTime())), 200);
        String reagentId = reagent.path("id").asText();
        String reagentCode = reagent.path("reagentCode").asText();

        JsonNode reagentList = responseBody(mockMvc.perform(authorized(get("/api/v1/reagents"), USER_M1_REAGENT)
            .param("keyword", reagentCode)), 200);
        assertThat(reagentList).hasSize(1);

        mockMvc.perform(authorized(patch("/api/v1/reagents/{id}", reagentId), USER_M1_REAGENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reagentName":"Single Api Reagent Updated",
                      "specification":"150ml",
                      "unit":"bottle",
                      "manufacturer":"Updated Supplier",
                      "defaultLowStockThreshold":6,
                      "defaultNearExpiryDays":15,
                      "enabled":false,
                      "remarks":"updated"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(reagentId))
            .andExpect(jsonPath("$.data.reagentName").value("Single Api Reagent Updated"))
            .andExpect(jsonPath("$.data.enabled").value(false));

        JsonNode equipment = responseBody(postJson("/api/v1/equipment-records", USER_M1_REAGENT, """
            {
              "equipmentCode":"EQ-SINGLE-%d",
              "equipmentName":"Single Api Equipment",
              "equipmentCategory":"MICROTOME",
              "modelNo":"EQ-100",
              "equipmentStatus":"ACTIVE",
              "locationDescription":"Lab-A",
              "enabledAt":"%s",
              "nextMaintenanceAt":"%s",
              "remarks":"created"
            }
            """.formatted(System.nanoTime(),
            LocalDateTime.now().minusDays(5).withNano(0),
            LocalDateTime.now().plusDays(10).withNano(0))), 200);
        String equipmentId = equipment.path("id").asText();
        String equipmentCode = equipment.path("equipmentCode").asText();

        JsonNode equipmentList = responseBody(mockMvc.perform(authorized(get("/api/v1/equipment-records"), USER_M1_REAGENT)
            .param("keyword", equipmentCode)), 200);
        assertThat(equipmentList).hasSize(1);

        mockMvc.perform(authorized(patch("/api/v1/equipment-records/{id}", equipmentId), USER_M1_REAGENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "equipmentName":"Single Api Equipment Updated",
                      "equipmentCategory":"MICROTOME",
                      "modelNo":"EQ-200",
                      "equipmentStatus":"MAINTENANCE",
                      "locationDescription":"Lab-B",
                      "enabledAt":"%s",
                      "nextMaintenanceAt":"%s",
                      "remarks":"updated"
                    }
                    """.formatted(LocalDateTime.now().minusDays(3).withNano(0),
                    LocalDateTime.now().plusDays(30).withNano(0))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(equipmentId))
            .andExpect(jsonPath("$.data.equipmentName").value("Single Api Equipment Updated"))
            .andExpect(jsonPath("$.data.equipmentStatus").value("MAINTENANCE"));

        responseBody(postJson("/api/v1/reagent-stocks", USER_M1_REAGENT, """
            {
              "reagentId":"%s",
              "batchNo":"BATCH-SINGLE-%d",
              "stockQuantity":3,
              "stockStatus":"ACTIVE",
              "expiryDate":"%s",
              "storageLocation":"Shelf-Z"
            }
            """.formatted(reagentId, System.nanoTime(), LocalDate.now().plusDays(2))), 200);

        JsonNode warnings = responseBody(mockMvc.perform(authorized(get("/api/v1/reagent-stocks/warnings"), USER_M1_REAGENT)), 200);
        assertThat(warnings.toString()).contains("LOW_STOCK");
    }
}
