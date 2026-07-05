package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class FrozenWorkflowQueryIntegrationTest extends AbstractFrozenWorkflowIntegrationTest {

    @Test
    void shouldListFrozenWorkbenchAndSessionDetailAcrossFirstBatchStates() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-WB-001", "BC-FR-WB-001");
        FrozenCaseContext grossing = prepareFrozenReceivedCase("APP-FR-WB-002", "BC-FR-WB-002");
        FrozenCaseContext slicing = prepareFrozenGrossingCompletedCase("APP-FR-WB-003", "BC-FR-WB-003");
        FrozenCaseContext diagnosing = prepareFrozenDiagnosingCase("APP-FR-WB-004", "BC-FR-WB-004");

        JsonNode workbench = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/workbench"),
            USER_RECEIVE)), 200);

        assertThat(workbench.path("sessions").isArray()).isTrue();
        assertThat(workbench.path("sessions").size()).isGreaterThanOrEqualTo(3);
        assertThat(workbench.path("sessions").toString()).contains(
            requested.caseId(),
            grossing.caseId(),
            slicing.caseId());
        assertThat(workbench.path("sessions").toString()).doesNotContain(diagnosing.caseId());

        JsonNode reminders = workbench.path("reminders");
        assertThat(reminders.path("items").isArray()).isTrue();
        assertThat(reminders.path("total").asInt()).isEqualTo(reminders.path("items").size());
        assertThat(reminders.path("items").toString()).contains("\"sessionNo\"");

        JsonNode sessionByState = indexSessionsByCaseId(workbench.path("sessions"));
        assertThat(sessionByState.path(requested.caseId()).path("currentTaskType").asText()).isEqualTo("RECEIVE");
        assertThat(sessionByState.path(requested.caseId()).path("sessionStatus").asText()).isEqualTo("REQUESTED");
        assertThat(sessionByState.path(requested.caseId()).path("frozenPathologyNo").asText()).isNotBlank();
        assertThat(sessionByState.path(requested.caseId()).path("requestedAt").asText()).isNotBlank();
        assertThat(sessionByState.path(grossing.caseId()).path("currentTaskType").asText()).isEqualTo("GROSSING");
        assertThat(sessionByState.path(grossing.caseId()).path("sessionStatus").asText()).isEqualTo("RECEIVED");
        assertThat(sessionByState.path(slicing.caseId()).path("currentTaskType").asText()).isEqualTo("SLICING");
        assertThat(sessionByState.path(slicing.caseId()).path("sessionStatus").asText()).isEqualTo("GROSSING");

        JsonNode requestedDetail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", requested.caseId()),
            USER_RECEIVE)), 200);
        assertThat(requestedDetail.path("timeline").toString()).contains("FROZEN_REQUESTED");
        assertThat(requestedDetail.path("tasks").toString()).contains("APPOINTMENT", "RECEIVE");

        JsonNode grossingDetail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", grossing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(grossingDetail.path("timeline").toString()).contains("FROZEN_RECEIVE_COMPLETED");

        JsonNode slicingDetail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", slicing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(slicingDetail.path("timeline").toString()).contains("FROZEN_GROSSING_COMPLETED");

        JsonNode diagnosingDetail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(diagnosingDetail.path("currentTaskType").asText()).isEqualTo("REPORT");
        assertThat(diagnosingDetail.path("sessionStatus").asText()).isEqualTo("DIAGNOSING");
        assertThat(diagnosingDetail.path("timeoutLevel").asText()).isEqualTo("ORANGE");
    }

    @Test
    void shouldAllowFrozenWorkbenchReadForTechnicalTaskQueryRole() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-WB-005", "BC-FR-WB-005");

        JsonNode workbench = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/workbench"),
            USER_M3_GROSSING)), 200);
        assertThat(workbench.path("sessions").toString()).contains(requested.caseId());

        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", requested.caseId()),
            USER_M3_GROSSING)), 200);
        assertThat(detail.path("currentTaskType").asText()).isEqualTo("RECEIVE");
    }

    @Test
    void shouldAllowFrozenSessionDetailReadForAssignedDiagnosticAndReviewUsers() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-WB-006", "BC-FR-WB-006");

        JsonNode diagnosisDetail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_M4_DIAGNOSIS)), 200);
        assertThat(diagnosisDetail.path("currentTaskType").asText()).isEqualTo("REPORT");

        JsonNode reviewDetail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_M4_REVIEW)), 200);
        assertThat(reviewDetail.path("currentTaskType").asText()).isEqualTo("REPORT");
    }

    @Test
    void shouldListFrozenRemindersSummaryViaDedicatedEndpoint() throws Exception {
        prepareFrozenRequestedCase("APP-FR-RMD-001", "BC-FR-RMD-001");
        prepareFrozenReceivedCase("APP-FR-RMD-002", "BC-FR-RMD-002");
        prepareFrozenGrossingCompletedCase("APP-FR-RMD-003", "BC-FR-RMD-003");
        FrozenCaseContext diagnosing = prepareFrozenDiagnosingCase("APP-FR-RMD-004", "BC-FR-RMD-004");

        JsonNode reminders = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/reminders"),
            USER_RECEIVE)), 200);

        assertThat(reminders.path("items").size()).isGreaterThanOrEqualTo(4);
        JsonNode reminderByCaseId = indexRemindersByCaseId(reminders.path("items"));
        JsonNode diagnosingReminder = reminderByCaseId.path(diagnosing.caseId());
        assertThat(diagnosingReminder.path("title").asText())
            .isEqualTo(diagnosingReminder.path("patientName").asText()
                + " / "
                + diagnosingReminder.path("frozenPathologyNo").asText());
    }

    @Test
    void shouldKeepConfirmedCompareStageInFrozenReminders() throws Exception {
        FrozenCaseContext confirmed = prepareFrozenConfirmedCase("APP-FR-RMD-005", "BC-FR-RMD-005");

        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", confirmed.caseId()),
            USER_RECEIVE)), 200);
        assertThat(detail.path("currentTaskType").asText()).isEqualTo("COMPARE");
        assertThat(detail.path("sessionStatus").asText()).isEqualTo("CONFIRMED");

        JsonNode reminders = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/reminders"),
            USER_RECEIVE)), 200);
        assertThat(reminders.path("items").toString()).contains(confirmed.caseId());
    }

    @Test
    void shouldKeepReportedStageInFrozenRemindersBeforeFinalConfirm() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RMD-006", "BC-FR-RMD-006");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RMD-006-01",
                  "remarks": "save preliminary before reported reminder test",
                  "preliminaryResult": "考虑良性病变"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RMD-006-02",
                  "remarks": "phone back before reported reminder test",
                  "preliminaryResult": "考虑良性病变"
                }
                """), 200);

        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(detail.path("sessionStatus").asText()).isEqualTo("REPORTED");

        JsonNode reminders = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/reminders"),
            USER_RECEIVE)), 200);
        assertThat(reminders.path("items").toString()).contains(diagnosing.caseId());
    }

    @Test
    void shouldListFrozenSessionsPageForReminderWatcherQueries() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-LIST-001", "BC-FR-LIST-001");
        FrozenCaseContext received = prepareFrozenReceivedCase("APP-FR-LIST-002", "BC-FR-LIST-002");
        prepareFrozenGrossingCompletedCase("APP-FR-LIST-003", "BC-FR-LIST-003");

        JsonNode page = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions")
                .param("page", "1")
                .param("size", "5")
                .param("keyword", "APP-FR-LIST")
                .param("sessionStatus", "RECEIVED"),
            USER_RECEIVE)), 200);
        assertThat(page.path("total").asInt()).isEqualTo(1);
        assertThat(page.path("items").get(0).path("caseId").asText()).isEqualTo(received.caseId());

        JsonNode requestedOnly = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions")
                .param("page", "1")
                .param("size", "5")
                .param("keyword", "APP-FR-LIST-001"),
            USER_RECEIVE)), 200);
        assertThat(requestedOnly.path("items").get(0).path("caseId").asText()).isEqualTo(requested.caseId());
        assertThat(requestedOnly.path("items").get(0).path("requestedAt").asText()).isNotBlank();
    }

    @Test
    void shouldExposeFrozenQueryBaselineOnFrozenSessionsRoutes() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-ALIAS-001", "BC-FR-ALIAS-001");
        FrozenCaseContext received = prepareFrozenReceivedCase("APP-FR-ALIAS-002", "BC-FR-ALIAS-002");

        JsonNode workbench = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-sessions/workbench"),
            USER_RECEIVE)), 200);
        assertThat(workbench.path("sessions").toString()).contains(requested.caseId(), received.caseId());

        JsonNode page = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-sessions")
                .param("page", "1")
                .param("size", "10")
                .param("keyword", "APP-FR-ALIAS-002"),
            USER_RECEIVE)), 200);
        assertThat(page.path("items").get(0).path("caseId").asText()).isEqualTo(received.caseId());

        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-sessions/{sessionId}", received.caseId()),
            USER_RECEIVE)), 200);
        assertThat(detail.path("currentTaskType").asText()).isEqualTo("GROSSING");
    }

    @Test
    void shouldKeepFrozenQueryRoutesAvailableWhenLegacyFrozenSpecimenFlagIsMissing() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-LEGACY-001", "BC-FR-LEGACY-001");
        clearFrozenSpecimenFlags(requested.caseId());

        JsonNode workbench = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-sessions/workbench"),
            USER_RECEIVE)), 200);
        assertThat(workbench.path("sessions").toString()).contains(requested.caseId());

        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-sessions/{sessionId}", requested.caseId()),
            USER_RECEIVE)), 200);
        assertThat(detail.path("currentTaskType").asText()).isEqualTo("RECEIVE");
    }

    @Test
    void shouldListFrozenSessionsBeyondFirstTwoHundredPendingRegistrations() throws Exception {
        for (int index = 1; index <= 205; index++) {
            String suffix = "%03d".formatted(index);
            prepareFrozenRequestedCase("APP-FR-BULK-" + suffix, "BC-FR-BULK-" + suffix);
        }

        JsonNode page = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions")
                .param("page", "21")
                .param("size", "10")
                .param("keyword", "APP-FR-BULK-"),
            USER_RECEIVE)), 200);

        assertThat(page.path("page").asInt()).isEqualTo(21);
        assertThat(page.path("total").asInt()).isEqualTo(205);
        assertThat(page.path("items").size()).isEqualTo(5);
    }

    @Test
    void shouldAllowFrozenReminderWatcherReadForApplicationRegistrationRole() throws Exception {
        FrozenCaseContext confirmed = prepareFrozenConfirmedCase("APP-FR-LIST-004", "BC-FR-LIST-004");

        JsonNode page = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions")
                .param("page", "1")
                .param("size", "5")
                .param("keyword", "APP-FR-LIST-004"),
            USER_REGISTER)), 200);
        assertThat(page.path("items").get(0).path("caseId").asText()).isEqualTo(confirmed.caseId());

        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", confirmed.caseId()),
            USER_REGISTER)), 200);
        assertThat(detail.path("sessionStatus").asText()).isEqualTo("CONFIRMED");
    }

    @Test
    void shouldAllowFrozenReminderSummaryReadForApplicationRegistrationRole() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-RMD-007", "BC-FR-RMD-007");
        FrozenCaseContext confirmed = prepareFrozenConfirmedCase("APP-FR-RMD-008", "BC-FR-RMD-008");

        JsonNode reminders = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/reminders"),
            USER_REGISTER)), 200);

        assertThat(reminders.path("items").toString()).contains(requested.caseId(), confirmed.caseId());
    }

    @Test
    void shouldPersistFrozenRequestedWorkflowEventWhenFrozenSessionIsCreated() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-REQ-001", "BC-FR-REQ-001");

        assertThat(namedParameterJdbcTemplate.queryForList("""
            select event_type
            from workflow_events
            where case_id = :caseId
            order by event_time asc, created_at asc
            """, java.util.Map.of("caseId", requested.caseId()), String.class)).contains("FROZEN_REQUESTED");
    }

    @Test
    void shouldKeepRequestedCaseStatusUntilFrozenReceiveIsCompleted() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-REQ-002", "BC-FR-REQ-002");

        String requestedCaseStatus = namedParameterJdbcTemplate.queryForObject("""
            select case_status
            from pathology_cases
            where id = :caseId
            """, java.util.Map.of("caseId", requested.caseId()), String.class);
        assertThat(requestedCaseStatus).isEqualTo("REQUESTED");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/receive/complete".formatted(requested.caseId()),
            USER_RECEIVE,
            """
                {
                  "terminalCode": "T-FR-REQ-002-01",
                  "remarks": "complete receive after requested case status assertion"
                }
                """), 200);

        String receivedCaseStatus = namedParameterJdbcTemplate.queryForObject("""
            select case_status
            from pathology_cases
            where id = :caseId
            """, java.util.Map.of("caseId", requested.caseId()), String.class);
        assertThat(receivedCaseStatus).isEqualTo("RECEIVED");
    }
}
