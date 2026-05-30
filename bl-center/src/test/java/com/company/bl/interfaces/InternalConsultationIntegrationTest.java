package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class InternalConsultationIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    @Test
    void shouldCompleteInternalConsultationAndTrackParticipantComments() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-CONS-001", "BC-M4-CONS-001");

        JsonNode created = responseBody(postJson("/api/v1/consultations", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "participants":[
                {"participantUserId":"USER_M4_SIGN","participantName":"M4 Sign","participantRole":"EXPERT"}
              ],
              "terminalCode":"M4-CONS-01"
            }
            """.formatted(context.caseId())), 200);
        String consultationId = created.path("consultationId").asText();
        assertThat(created.path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(countNotifications(USER_M4_SIGN, "CONSULTATION_INVITE", consultationId)).isEqualTo(1L);

        String signParticipantId = namedParameterJdbcTemplate.queryForObject("""
            select id
            from consultation_participants
            where consultation_id = :consultationId
              and participant_user_id = :participantUserId
            """, Map.of("consultationId", consultationId, "participantUserId", USER_M4_SIGN), String.class);

        postJson("/api/v1/consultations/%s/participants/%s/comment".formatted(consultationId, signParticipantId), USER_M4_SIGN, """
            {
              "opinion":"need more correlation",
              "terminalCode":"M4-CONS-02"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        assertThat(countNotifications(USER_M4_DIAGNOSIS, "CONSULTATION_COMMENT", consultationId)).isEqualTo(1L);

        postJson("/api/v1/consultations/%s/complete".formatted(consultationId), USER_M4_DIAGNOSIS, """
            {
              "opinion":"internal consultation completed",
              "terminalCode":"M4-CONS-03"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        JsonNode workbench = diagnosticWorkbench(context.caseId(), USER_M4_DIAGNOSIS);
        assertThat(workbench.path("consultations").get(0).path("status").asText()).isEqualTo("COMPLETED");

        JsonNode tracking = reportTracking(context.caseId(), USER_M4_TRACKING);
        assertThat(tracking.toString()).contains("CONSULTATION_COMMENT");
        assertThat(tracking.toString()).contains("CONSULTATION_COMPLETE");
    }

    @Test
    void shouldRejectNonParticipantComment() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-CONS-002", "BC-M4-CONS-002");

        JsonNode created = responseBody(postJson("/api/v1/consultations", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "participants":[
                {"participantUserId":"USER_M4_SIGN","participantName":"M4 Sign","participantRole":"EXPERT"}
              ],
              "terminalCode":"M4-CONS-11"
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
              "opinion":"unauthorized comment",
              "terminalCode":"M4-CONS-12"
            }
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }
}
