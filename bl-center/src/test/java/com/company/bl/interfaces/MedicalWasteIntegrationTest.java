package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class MedicalWasteIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    private static final String USER_M1_REAGENT = "USER_M1_REAGENT";
    private static final String USER_M1_ARCHIVE = "USER_M1_ARCHIVE";
    private static final String USER_M1_NO_PERMISSION = "USER_M1_NO_PERMISSION";

    @Test
    void shouldPreviewPrintDestroyAndQuerySpecimenMedicalWaste() throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetGrossingTask("APP-MW-SPEC-" + uniqueSuffix(), "BC-MW-SPEC-" + uniqueSuffix());

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {"taskId":"%s","terminalCode":"MW-G-01"}
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk());
        namedParameterJdbcTemplate.update("""
            update technical_pending_tasks
            set station_name = :stationName
            where id = :taskId
            """, Map.of(
            "stationName", "取材台A",
            "taskId", context.grossingTaskId()));
        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId":"%s",
              "caseId":"%s",
              "terminalCode":"MW-G-02",
              "specimens":[{"specimenId":"%s","specimenType":"ROUTINE","grossDescription":"mw gross","blocks":[{"blockSite":"A","blockDescription":"B"}]}]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId()))
            .andExpect(status().isOk());

        String sampledByName = namedParameterJdbcTemplate.queryForObject("""
            select sampled_by_name
            from samplings
            where case_id = :caseId
            order by sampled_at desc
            limit 1
            """, Map.of("caseId", context.caseId()), String.class);
        LocalDate grossingDate = namedParameterJdbcTemplate.queryForObject("""
            select cast(sampled_at as date)
            from samplings
            where case_id = :caseId
            order by sampled_at desc
            limit 1
            """, Map.of("caseId", context.caseId()), LocalDate.class);
        String grossingStationName = "取材台A";

        JsonNode options = responseBody(mockMvc.perform(authorized(
            get("/api/v1/medical-waste/specimen-options"),
            USER_M1_REAGENT)), 200);
        assertThat(options.path("grossingOperators").toString()).contains(sampledByName);
        assertThat(options.path("grossingStations").toString()).contains(grossingStationName);
        assertThat(options.path("grossingPeriods").toString()).contains("AM", "PM");

        JsonNode preview = responseBody(postJson("/api/v1/medical-waste/specimen-batches/preview-labels", USER_M1_REAGENT, """
            {
              "bagName":"HB-%s",
              "grossingOperatorName":"%s",
              "grossingStationName":"%s",
              "grossingDate":"%s",
              "grossingPeriod":"AM"
            }
            """.formatted(uniqueSuffix(), sampledByName, grossingStationName, grossingDate)), 200);
        assertThat(preview).isNotEmpty();

        JsonNode printed = responseBody(postJson("/api/v1/medical-waste/specimen-batches/print", USER_M1_REAGENT, """
            {
              "bagName":"HB-%s",
              "grossingOperatorName":"%s",
              "grossingStationName":"%s",
              "grossingDate":"%s",
              "grossingPeriod":"AM",
              "weightKg":1.35
            }
            """.formatted(uniqueSuffix(), sampledByName, grossingStationName, grossingDate)), 200);
        String batchId = printed.path("batch").path("id").asText();
        assertThat(printed.path("labels")).isNotEmpty();
        assertThat(printed.path("batch").path("labelCount").asInt()).isGreaterThan(0);

        JsonNode listed = responseBody(mockMvc.perform(authorized(
            get("/api/v1/medical-waste/specimen-batches"),
            USER_M1_REAGENT).param("keyword", printed.path("batch").path("bagName").asText())), 200);
        assertThat(listed.toString()).contains(batchId);

        responseBody(postJson("/api/v1/medical-waste/specimen-batches/%s/destroy".formatted(batchId), USER_M1_REAGENT, "{}"), 200);

        mockMvc.perform(authorized(post("/api/v1/medical-waste/specimen-batches/{id}/destroy", batchId), USER_M1_REAGENT)
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
    }

    @Test
    void shouldRejectEmptySpecimenPrintAndEnforcePermissions() throws Exception {
        postJson("/api/v1/medical-waste/specimen-batches/print", USER_M1_REAGENT, """
            {
              "bagName":"HB-NONE",
              "grossingOperatorName":"不存在",
              "grossingStationName":"取材台X",
              "grossingDate":"2026-06-16",
              "grossingPeriod":"AM"
            }
            """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        mockMvc.perform(authorized(get("/api/v1/medical-waste/specimen-batches"), USER_M1_NO_PERMISSION))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void shouldSaveListAndHandoverReagentMedicalWaste() throws Exception {
        JsonNode created = responseBody(postJson("/api/v1/medical-waste/reagent-bags", USER_M1_REAGENT, """
            {
              "bagName":"DW-%s",
              "wasteType":"DRUG",
              "weightKg":1.50,
              "volumeMl":500,
              "source":"病理科",
              "remarks":"首次建袋"
            }
            """.formatted(uniqueSuffix())), 200);
        String bagId = created.path("id").asText();
        assertThat(created.path("wasteType").asText()).isEqualTo("DRUG");

        JsonNode listed = responseBody(mockMvc.perform(authorized(
            get("/api/v1/medical-waste/reagent-bags"),
            USER_M1_REAGENT).param("keyword", created.path("bagName").asText())), 200);
        assertThat(listed.toString()).contains(bagId);

        JsonNode handedOver = responseBody(postJson("/api/v1/medical-waste/reagent-bags/%s/handover".formatted(bagId), USER_M1_REAGENT, """
            {
              "handedOverByName":"交接员甲",
              "handedOverAt":"2026-06-16T10:00:00",
              "handoverRemarks":"完成交接"
            }
            """), 200);
        assertThat(handedOver.path("handedOverByName").asText()).isEqualTo("交接员甲");

        mockMvc.perform(authorized(post("/api/v1/medical-waste/reagent-bags/{id}/handover", bagId), USER_M1_REAGENT)
                .contentType("application/json")
                .content("""
                    {
                      "handedOverByName":"交接员乙",
                      "handedOverAt":"2026-06-16T11:00:00"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

        mockMvc.perform(authorized(get("/api/v1/medical-waste/reagent-bags"), USER_M1_ARCHIVE))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }
}
