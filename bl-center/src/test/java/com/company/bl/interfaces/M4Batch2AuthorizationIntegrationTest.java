package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class M4Batch2AuthorizationIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    @Test
    void shouldRejectCrossRoleActionsForBatch2ProtectedEndpoints() throws Exception {
        postJson("/api/v1/report-revision-requests/RR-X/approve", USER_M4_DIAGNOSIS, """
            {"operatorName":"diag-user","terminalCode":"M4-AUTH-01"}
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        postJson("/api/v1/medical-orders/MO-X/accept", USER_M4_SIGN, """
            {"operatorName":"sign-user","terminalCode":"M4-AUTH-02"}
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void shouldRejectCommentFromNonParticipantEvenWithCommentPermission() throws Exception {
        String suffix = uniqueSuffix();
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-AUTH-" + suffix, "BC-M4-AUTH-" + suffix);
        JsonNode created = responseBody(postJson("/api/v1/consultations", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "participants":[
                {"participantUserId":"USER_M4_SIGN","participantName":"M4 Sign","participantRole":"EXPERT"}
              ],
              "operatorName":"diag-user",
              "terminalCode":"M4-AUTH-03"
            }
            """.formatted(context.caseId())), 200);
        String consultationId = created.path("consultationId").asText();
        String signParticipantId = namedParameterJdbcTemplate.queryForObject("""
            select id
            from consultation_participants
            where consultation_id = :consultationId
              and participant_user_id = :participantUserId
            """, Map.of("consultationId", consultationId, "participantUserId", USER_M4_SIGN), String.class);

        postJson("/api/v1/consultations/%s/participants/%s/comment".formatted(consultationId, signParticipantId), USER_M4_REVIEW, """
            {
              "opinion":"should fail",
              "operatorName":"review-user",
              "terminalCode":"M4-AUTH-04"
            }
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void shouldKeepMissingHeaderAndNoPermissionBehaviorForBatch2Endpoints() throws Exception {
        mockMvc.perform(post("/api/v1/medical-orders")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "caseId":"CASE-X",
                      "orderType":"RECUT",
                      "orderContent":"need recut",
                      "operatorName":"diag-user"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        postJson("/api/v1/report-revision-requests", USER_M4_NO_PERMISSION, """
            {
              "reportId":"RPT-X",
              "requestReason":"revise",
              "operatorName":"diag-user"
            }
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }
}
