package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class ArchiveRoleAuthorizationIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    private static final String USER_M1_ARCHIVE = "USER_M1_ARCHIVE";
    private static final String USER_M1_REAGENT = "USER_M1_REAGENT";

    @Test
    void shouldAllowArchiveRoleAndReagentRoleOnlyWithinOwnedM5Capabilities() throws Exception {
        responseBody(postJson("/api/v1/archive-cabinets", USER_M1_ARCHIVE, """
            {
              "cabinetCode":"CAB-M5-AUTH-1",
              "cabinetName":"Archive Auth Cabinet",
              "cabinetType":"STANDARD",
              "layerCount":1,
              "slotCountPerLayer":2,
              "operatorName":"archive-user"
            }
            """), 200);

        JsonNode reagent = responseBody(postJson("/api/v1/reagents", USER_M1_REAGENT, """
            {
              "reagentCode":"RG-M5-AUTH-1",
              "reagentName":"H&E Dye",
              "specification":"500ml",
              "unit":"bottle",
              "manufacturer":"Path Lab",
              "defaultLowStockThreshold":10,
              "defaultNearExpiryDays":30,
              "enabled":true,
              "operatorName":"reagent-user"
            }
            """), 200);
        assertThat(reagent.path("reagentCode").asText()).isEqualTo("RG-M5-AUTH-1");

        postJson("/api/v1/reagents", USER_M1_ARCHIVE, """
            {
              "reagentCode":"RG-M5-AUTH-2",
              "reagentName":"Unauthorized Reagent",
              "enabled":true,
              "operatorName":"archive-user"
            }
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        postJson("/api/v1/archive-cabinets", USER_M1_REAGENT, """
            {
              "cabinetCode":"CAB-M5-AUTH-2",
              "cabinetName":"Reagent Should Not Archive",
              "cabinetType":"STANDARD",
              "layerCount":1,
              "slotCountPerLayer":1,
              "operatorName":"reagent-user"
            }
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void shouldKeepM4UsersReadOnlyForBackfilledViewsAndRejectArchiveManagement() throws Exception {
        PublishedReportContext context = preparePublishedReportContext("APP-M5-AUTH-003", "BC-M5-AUTH-003");

        JsonNode workbench = diagnosticWorkbench(context.caseId(), USER_M4_DIAGNOSIS);
        assertThat(workbench.path("caseId").asText()).isEqualTo(context.caseId());

        JsonNode tracking = reportTracking(context.caseId(), USER_M4_TRACKING);
        assertThat(tracking.path("caseId").asText()).isEqualTo(context.caseId());

        postJson("/api/v1/archive-cabinets", USER_M4_DIAGNOSIS, """
            {
              "cabinetCode":"CAB-M5-AUTH-3",
              "cabinetName":"M4 Diagnosis Forbidden",
              "cabinetType":"STANDARD",
              "layerCount":1,
              "slotCountPerLayer":1,
              "operatorName":"diag-user"
            }
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        mockMvc.perform(authorized(get("/api/v1/archive-cabinets"), USER_M4_NO_PERMISSION))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }
}
