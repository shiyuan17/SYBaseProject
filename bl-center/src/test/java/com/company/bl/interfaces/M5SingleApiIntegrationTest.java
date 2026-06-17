package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void shouldBatchCreateArchiveCabinetsAndRollbackOnDuplicateCodes() throws Exception {
        String uniquePrefix = "CAB-BATCH-" + System.nanoTime() + "-";

        JsonNode cabinets = responseBody(postJson("/api/v1/archive-cabinets/batch", USER_M1_ARCHIVE, """
            {
              "cabinetType":"EMBEDDING_BOX",
              "cabinetCodePrefix":"%s",
              "startNo":1,
              "count":3,
              "numberWidth":3,
              "cabinetNamePrefix":"批量蜡块柜",
              "layerCount":2,
              "slotCountPerLayer":4,
              "terminalCode":"M5-BATCH-01",
              "locationDescription":"Room-Batch",
              "remarks":"batch create"
            }
            """.formatted(uniquePrefix)), 200);

        assertThat(cabinets).hasSize(3);
        assertThat(cabinets.get(0).path("cabinetCode").asText()).isEqualTo(uniquePrefix + "001");
        assertThat(cabinets.get(2).path("cabinetCode").asText()).isEqualTo(uniquePrefix + "003");
        assertThat(cabinets.get(0).path("capacity").asInt()).isEqualTo(8);

        Integer positionCount = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from archive_positions ap
            join archive_cabinets ac on ac.id = ap.cabinet_id
            where ac.cabinet_code like :codePrefix
            """, Map.of("codePrefix", uniquePrefix + "%"), Integer.class);
        assertThat(positionCount).isEqualTo(24);

        String duplicatePrefix = "CAB-BATCH-DUP-" + System.nanoTime() + "-";
        responseBody(postJson("/api/v1/archive-cabinets", USER_M1_ARCHIVE, """
            {
              "cabinetCode":"%s002",
              "cabinetName":"Duplicate Cabinet",
              "cabinetType":"STANDARD",
              "layerCount":1,
              "slotCountPerLayer":1
            }
            """.formatted(duplicatePrefix)), 200);

        postJson("/api/v1/archive-cabinets/batch", USER_M1_ARCHIVE, """
            {
              "cabinetType":"STANDARD",
              "cabinetCodePrefix":"%s",
              "startNo":1,
              "count":3,
              "numberWidth":3,
              "cabinetNamePrefix":"重复柜",
              "layerCount":1,
              "slotCountPerLayer":1
            }
            """.formatted(duplicatePrefix))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

        Integer createdAfterConflict = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from archive_cabinets
            where cabinet_code like :codePrefix
            """, Map.of("codePrefix", duplicatePrefix + "%"), Integer.class);
        assertThat(createdAfterConflict).isEqualTo(1);
    }

    @Test
    void shouldCreateAndQueryArchiveCabinetNodes() throws Exception {
        JsonNode area = responseBody(postJson("/api/v1/archive-cabinet-nodes", USER_M1_ARCHIVE, """
            {
              "nodeCode":"AREA-NODE-%d",
              "nodeType":"AREA",
              "pathLocation":"M5 Area"
            }
            """.formatted(System.nanoTime())), 200);
        String areaId = area.path("id").asText();
        assertThat(area.path("nodeType").asText()).isEqualTo("AREA");
        assertThat(area.path("remainingCapacity").asInt()).isZero();

        JsonNode cabinetNode = responseBody(postJson("/api/v1/archive-cabinet-nodes", USER_M1_ARCHIVE, """
            {
              "parentId":"%s",
              "nodeCode":"CAB-NODE-%d",
              "nodeType":"CABINET",
              "cabinetType":"APPLICATION_FORM",
              "capacity":3,
              "pathLocation":"M5 Area Cabinet",
              "remarks":"node cabinet"
            }
            """.formatted(areaId, System.nanoTime())), 200);
        String cabinetId = cabinetNode.path("cabinetId").asText();
        assertThat(cabinetNode.path("parentId").asText()).isEqualTo(areaId);
        assertThat(cabinetNode.path("capacity").asInt()).isEqualTo(3);
        assertThat(cabinetNode.path("remainingCapacity").asInt()).isEqualTo(3);

        JsonNode drawerNode = responseBody(postJson("/api/v1/archive-cabinet-nodes", USER_M1_ARCHIVE, """
            {
              "parentId":"%s",
              "nodeCode":"2",
              "nodeType":"DRAWER",
              "capacity":3,
              "pathLocation":"M5 Area Drawer"
            }
            """.formatted(cabinetNode.path("id").asText())), 200);
        assertThat(drawerNode.path("nodeType").asText()).isEqualTo("DRAWER");
        assertThat(drawerNode.path("layerNo").asInt()).isEqualTo(2);
        assertThat(drawerNode.path("remainingCapacity").asInt()).isEqualTo(3);

        JsonNode nodes = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-cabinet-nodes"), USER_M1_ARCHIVE)), 200);
        assertThat(nodes.toString()).contains(areaId, cabinetNode.path("id").asText(), drawerNode.path("id").asText());

        JsonNode positions = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-positions/available"), USER_M1_ARCHIVE)
            .param("cabinetId", cabinetId)), 200);
        assertThat(positions).hasSize(6);

        postJson("/api/v1/archive-cabinet-nodes", USER_M1_ARCHIVE, """
            {
              "parentId":"%s",
              "nodeCode":"BAD-DRAWER",
              "nodeType":"DRAWER",
              "capacity":2
            }
            """.formatted(cabinetNode.path("id").asText()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        postJson("/api/v1/archive-cabinet-nodes", USER_M1_ARCHIVE, """
            {
              "parentId":"%s",
              "nodeCode":"BAD-AREA",
              "nodeType":"AREA"
            }
            """.formatted(cabinetNode.path("id").asText()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void shouldDeleteOnlyEmptyArchiveCabinet() throws Exception {
        JsonNode emptyCabinet = responseBody(postJson("/api/v1/archive-cabinets", USER_M1_ARCHIVE, """
            {
              "cabinetCode":"CAB-DELETE-EMPTY-%d",
              "cabinetName":"Empty Delete Cabinet",
              "cabinetType":"STANDARD",
              "layerCount":1,
              "slotCountPerLayer":1
            }
            """.formatted(System.nanoTime())), 200);
        String emptyCabinetId = emptyCabinet.path("id").asText();

        mockMvc.perform(authorized(delete("/api/v1/archive-cabinets/{id}", emptyCabinetId), USER_M1_ARCHIVE))
            .andExpect(status().isNoContent());

        Integer deletedCabinetCount = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from archive_cabinets
            where id = :cabinetId
            """, Map.of("cabinetId", emptyCabinetId), Integer.class);
        assertThat(deletedCabinetCount).isZero();

        JsonNode occupiedCabinet = responseBody(postJson("/api/v1/archive-cabinets", USER_M1_ARCHIVE, """
            {
              "cabinetCode":"CAB-DELETE-OCC-%d",
              "cabinetName":"Occupied Delete Cabinet",
              "cabinetType":"SLIDE",
              "layerCount":1,
              "slotCountPerLayer":1
            }
            """.formatted(System.nanoTime())), 200);
        String occupiedCabinetId = occupiedCabinet.path("id").asText();
        JsonNode positions = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-positions/available"), USER_M1_ARCHIVE)
            .param("cabinetId", occupiedCabinetId)), 200);
        namedParameterJdbcTemplate.update("""
            update archive_positions
            set position_status = 'OCCUPIED',
                current_object_type = 'SLIDE',
                current_object_id = 'SLIDE-M5-DELETE-LOCK'
            where id = :positionId
            """, Map.of("positionId", positions.get(0).path("id").asText()));

        mockMvc.perform(authorized(delete("/api/v1/archive-cabinets/{id}", occupiedCabinetId), USER_M1_ARCHIVE))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

        String referencedApplicationId = "APPID-M5-DELETE-REF-" + System.nanoTime();
        String referencedCaseId = "CASE-M5-DELETE-REF-" + System.nanoTime();
        namedParameterJdbcTemplate.update("""
            insert into applications (id, application_no, status, created_at, updated_at)
            values (:id, :applicationNo, 'RECEIVED', :createdAt, :updatedAt)
            """, Map.of(
            "id", referencedApplicationId,
            "applicationNo", "APP-M5-DELETE-REF-" + System.nanoTime(),
            "createdAt", LocalDateTime.now(),
            "updatedAt", LocalDateTime.now()));
        namedParameterJdbcTemplate.update("""
            insert into pathology_cases (id, application_id, pathology_no, case_status, created_at, updated_at)
            values (:id, :applicationId, :pathologyNo, 'ARCHIVE', :createdAt, :updatedAt)
            """, Map.of(
            "id", referencedCaseId,
            "applicationId", referencedApplicationId,
            "pathologyNo", "BL-M5-DELETE-REF-" + System.nanoTime(),
            "createdAt", LocalDateTime.now(),
            "updatedAt", LocalDateTime.now()));
        JsonNode referencedCabinet = responseBody(postJson("/api/v1/archive-cabinets", USER_M1_ARCHIVE, """
            {
              "cabinetCode":"CAB-DELETE-REF-%d",
              "cabinetName":"Referenced Delete Cabinet",
              "cabinetType":"APPLICATION_FORM",
              "layerCount":1,
              "slotCountPerLayer":1
            }
            """.formatted(System.nanoTime())), 200);
        String referencedCabinetId = referencedCabinet.path("id").asText();
        JsonNode referencedPositions = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-positions/available"), USER_M1_ARCHIVE)
            .param("cabinetId", referencedCabinetId)), 200);
        namedParameterJdbcTemplate.update("""
            insert into specimen_storage_records
                (id, case_id, object_type, object_id, storage_status, archive_position_id, created_at, updated_at)
            values
                (:id, :caseId, 'APPLICATION_FORM', :objectId, 'IN_STORAGE', :positionId, :createdAt, :updatedAt)
            """, Map.of(
            "id", "SSR-M5-DELETE-REF-" + System.nanoTime(),
            "caseId", referencedCaseId,
            "objectId", "APP-FORM-M5-DELETE-REF-" + System.nanoTime(),
            "positionId", referencedPositions.get(0).path("id").asText(),
            "createdAt", LocalDateTime.now(),
            "updatedAt", LocalDateTime.now()));

        mockMvc.perform(authorized(delete("/api/v1/archive-cabinets/{id}", referencedCabinetId), USER_M1_ARCHIVE))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
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
              "commonlyUsed":true,
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
                      "commonlyUsed":false,
                      "remarks":"updated"
                    }
                    """.formatted(LocalDateTime.now().minusDays(3).withNano(0),
                    LocalDateTime.now().plusDays(30).withNano(0))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(equipmentId))
            .andExpect(jsonPath("$.data.equipmentName").value("Single Api Equipment Updated"))
            .andExpect(jsonPath("$.data.equipmentStatus").value("MAINTENANCE"));

        mockMvc.perform(authorized(post("/api/v1/equipment-usage-records"), USER_M1_REAGENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "equipmentId":"%s",
                      "equipmentCategory":"MICROTOME",
                      "equipmentName":"Single Api Equipment Updated",
                      "commonlyUsed":false,
                      "startedAt":"2026-06-16T08:00:00",
                      "endedAt":"2026-06-16T17:00:00",
                      "runtimeHours":9,
                      "diagnosisCount":4,
                      "equipmentCondition":"正常",
                      "usageOperatorName":"设备员甲",
                      "usageContent":"常规切片"
                    }
                    """.formatted(equipmentId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.equipmentId").value(equipmentId))
            .andExpect(jsonPath("$.data.equipmentName").value("Single Api Equipment Updated"));

        mockMvc.perform(authorized(get("/api/v1/equipment-usage-records/common-devices"), USER_M1_REAGENT))
            .andExpect(status().isOk());

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
