package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class FrozenWorkflowIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

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
        assertThat(sessionByState.path(requested.caseId()).path("frozenPathologyNo").isNull()).isFalse();
        assertThat(sessionByState.path(requested.caseId()).path("frozenPathologyNo").asText()).isNotBlank();
        assertThat(sessionByState.path(requested.caseId()).path("requestedAt").asText()).isNotBlank();
        assertThat(sessionByState.path(grossing.caseId()).path("currentTaskType").asText()).isEqualTo("GROSSING");
        assertThat(sessionByState.path(grossing.caseId()).path("sessionStatus").asText()).isEqualTo("RECEIVED");
        assertThat(sessionByState.path(slicing.caseId()).path("currentTaskType").asText()).isEqualTo("SLICING");
        assertThat(sessionByState.path(slicing.caseId()).path("sessionStatus").asText()).isEqualTo("GROSSING");
        JsonNode requestedDetail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", requested.caseId()),
            USER_RECEIVE)), 200);
        assertThat(requestedDetail.path("id").asText()).isEqualTo(requested.caseId());
        assertThat(requestedDetail.path("frozenPathologyNo").isNull()).isFalse();
        assertThat(requestedDetail.path("frozenPathologyNo").asText()).isNotBlank();
        assertThat(requestedDetail.path("requestedAt").asText()).isNotBlank();
        assertThat(requestedDetail.path("tasks").isArray()).isTrue();
        assertThat(requestedDetail.path("timeline").isArray()).isTrue();
        assertThat(requestedDetail.path("tasks").toString()).contains("APPOINTMENT");
        assertThat(requestedDetail.path("timeline").toString()).contains("FROZEN_REQUESTED");
        assertThat(requestedDetail.path("timeline").toString()).doesNotContain("DIRECT_RECEIVE");
        assertThat(requestedDetail.path("tasks").toString()).contains("RECEIVE");

        JsonNode grossingDetail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", grossing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(grossingDetail.path("id").asText()).isEqualTo(grossing.caseId());
        assertThat(grossingDetail.path("receivedAt").asText()).isNotBlank();
        assertThat(grossingDetail.path("timeline").toString()).contains("FROZEN_RECEIVE_COMPLETED");

        JsonNode slicingDetail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", slicing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(slicingDetail.path("id").asText()).isEqualTo(slicing.caseId());
        assertThat(slicingDetail.path("grossingCompletedAt").asText()).isNotBlank();
        assertThat(slicingDetail.path("timeline").toString()).contains("FROZEN_GROSSING_COMPLETED");

        JsonNode diagnosingDetail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(diagnosingDetail.path("id").asText()).isEqualTo(diagnosing.caseId());
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
        assertThat(detail.path("id").asText()).isEqualTo(requested.caseId());
        assertThat(detail.path("currentTaskType").asText()).isEqualTo("RECEIVE");
    }

    @Test
    void shouldAllowFrozenSessionDetailReadForAssignedDiagnosticAndReviewUsers() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-WB-006", "BC-FR-WB-006");

        JsonNode diagnosisDetail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_M4_DIAGNOSIS)), 200);
        assertThat(diagnosisDetail.path("id").asText()).isEqualTo(diagnosing.caseId());
        assertThat(diagnosisDetail.path("currentTaskType").asText()).isEqualTo("REPORT");

        JsonNode reviewDetail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_M4_REVIEW)), 200);
        assertThat(reviewDetail.path("id").asText()).isEqualTo(diagnosing.caseId());
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

        assertThat(reminders.path("items").isArray()).isTrue();
        assertThat(reminders.path("items").size()).isGreaterThanOrEqualTo(4);
        assertThat(reminders.path("total").asInt()).isEqualTo(reminders.path("items").size());
        assertThat(reminders.path("items").toString()).contains("\"sessionNo\"");
        assertThat(reminders.path("items").toString()).contains(diagnosing.caseId());
        assertThat(reminders.path("orangeCount").asInt() + reminders.path("redCount").asInt())
            .isEqualTo(reminders.path("total").asInt());

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
        assertThat(detail.path("timeoutLevel").asText()).isEqualTo("ORANGE");

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
        assertThat(detail.path("currentTaskType").asText()).isEqualTo("REPORT");
        assertThat(detail.path("sessionStatus").asText()).isEqualTo("REPORTED");
        assertThat(detail.path("timeoutLevel").asText()).isEqualTo("ORANGE");

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

        assertThat(page.path("page").asInt()).isEqualTo(1);
        assertThat(page.path("size").asInt()).isEqualTo(5);
        assertThat(page.path("total").asInt()).isEqualTo(1);
        assertThat(page.path("items").isArray()).isTrue();
        assertThat(page.path("items").size()).isEqualTo(1);
        assertThat(page.path("items").get(0).path("caseId").asText()).isEqualTo(received.caseId());
        assertThat(page.path("items").get(0).path("sessionStatus").asText()).isEqualTo("RECEIVED");
        assertThat(page.path("items").get(0).path("applicationNo").asText()).isEqualTo("APP-FR-LIST-002");

        JsonNode requestedOnly = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions")
                .param("page", "1")
                .param("size", "5")
                .param("keyword", "APP-FR-LIST-001"),
            USER_RECEIVE)), 200);
        assertThat(requestedOnly.path("total").asInt()).isEqualTo(1);
        assertThat(requestedOnly.path("items").get(0).path("caseId").asText()).isEqualTo(requested.caseId());
        assertThat(requestedOnly.path("items").get(0).path("currentTaskType").asText()).isEqualTo("RECEIVE");
        assertThat(requestedOnly.path("items").get(0).path("frozenPathologyNo").isNull()).isFalse();
        assertThat(requestedOnly.path("items").get(0).path("frozenPathologyNo").asText()).isNotBlank();
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
        assertThat(page.path("total").asInt()).isEqualTo(1);
        assertThat(page.path("items").get(0).path("caseId").asText()).isEqualTo(received.caseId());

        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-sessions/{sessionId}", received.caseId()),
            USER_RECEIVE)), 200);
        assertThat(detail.path("id").asText()).isEqualTo(received.caseId());
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
        assertThat(detail.path("id").asText()).isEqualTo(requested.caseId());
        assertThat(detail.path("currentTaskType").asText()).isEqualTo("RECEIVE");
    }

    @Test
    void shouldCompleteFrozenTechnicalActionsOnFrozenSessionsRoutes() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-ALIAS-003", "BC-FR-ALIAS-003");

        JsonNode receiveResult = responseBody(postJson(
            "/api/v1/frozen-sessions/%s/receive".formatted(requested.caseId()),
            USER_RECEIVE,
            """
                {
                  "terminalCode": "T-FR-ALIAS-003-01",
                  "remarks": "receive through frozen-sessions route"
                }
                """), 200);
        assertThat(receiveResult.path("sessionStatus").asText()).isEqualTo("RECEIVED");
        assertThat(receiveResult.path("nextTaskType").asText()).isEqualTo("GROSSING");

        FrozenCaseContext received = prepareFrozenReceivedCase("APP-FR-ALIAS-004", "BC-FR-ALIAS-004");
        JsonNode grossingResult = responseBody(postJson(
            "/api/v1/frozen-sessions/%s/grossing/complete".formatted(received.caseId()),
            USER_M3_GROSSING,
            """
                {
                  "terminalCode": "T-FR-ALIAS-004-01",
                  "remarks": "grossing through frozen-sessions route"
                }
                """), 200);
        assertThat(grossingResult.path("sessionStatus").asText()).isEqualTo("GROSSING");
        assertThat(grossingResult.path("nextTaskType").asText()).isEqualTo("SLICING");

        FrozenCaseContext grossingCompleted =
            prepareFrozenGrossingCompletedCase("APP-FR-ALIAS-005", "BC-FR-ALIAS-005");
        JsonNode slicingResult = responseBody(postJson(
            "/api/v1/frozen-sessions/%s/slicing/complete".formatted(grossingCompleted.caseId()),
            USER_M3_SLICING,
            """
                {
                  "terminalCode": "T-FR-ALIAS-005-01",
                  "remarks": "slicing through frozen-sessions route"
                }
                """), 200);
        assertThat(slicingResult.path("sessionStatus").asText()).isEqualTo("DIAGNOSING");
        assertThat(slicingResult.path("nextTaskType").asText()).isEqualTo("REPORT");
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
        assertThat(page.path("size").asInt()).isEqualTo(10);
        assertThat(page.path("total").asInt()).isEqualTo(205);
        assertThat(page.path("items").isArray()).isTrue();
        assertThat(page.path("items").size()).isEqualTo(5);
    }

    @Test
    void shouldPersistFrozenRequestedWorkflowEventWhenFrozenSessionIsCreated() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-REQ-001", "BC-FR-REQ-001");

        List<String> eventTypes = namedParameterJdbcTemplate.queryForList("""
            select event_type
            from workflow_events
            where case_id = :caseId
            order by event_time asc, created_at asc
            """, Map.of("caseId", requested.caseId()), String.class);

        assertThat(eventTypes).contains("FROZEN_REQUESTED");
    }

    @Test
    void shouldKeepRequestedCaseStatusUntilFrozenReceiveIsCompleted() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-REQ-002", "BC-FR-REQ-002");

        String requestedCaseStatus = namedParameterJdbcTemplate.queryForObject("""
            select case_status
            from pathology_cases
            where id = :caseId
            """, Map.of("caseId", requested.caseId()), String.class);
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
            """, Map.of("caseId", requested.caseId()), String.class);
        assertThat(receivedCaseStatus).isEqualTo("RECEIVED");
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

        assertThat(page.path("total").asInt()).isEqualTo(1);
        assertThat(page.path("items").get(0).path("caseId").asText()).isEqualTo(confirmed.caseId());
        assertThat(page.path("items").get(0).path("sessionStatus").asText()).isEqualTo("CONFIRMED");

        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", confirmed.caseId()),
            USER_REGISTER)), 200);
        assertThat(detail.path("id").asText()).isEqualTo(confirmed.caseId());
        assertThat(detail.path("sessionStatus").asText()).isEqualTo("CONFIRMED");
        assertThat(detail.path("reportConfirmedAt").asText()).isNotBlank();
    }

    @Test
    void shouldAllowFrozenReminderSummaryReadForApplicationRegistrationRole() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-RMD-007", "BC-FR-RMD-007");
        FrozenCaseContext confirmed = prepareFrozenConfirmedCase("APP-FR-RMD-008", "BC-FR-RMD-008");

        JsonNode reminders = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/reminders"),
            USER_REGISTER)), 200);

        assertThat(reminders.path("items").isArray()).isTrue();
        assertThat(reminders.path("items").toString()).contains(requested.caseId(), confirmed.caseId());
        assertThat(reminders.path("total").asInt()).isGreaterThanOrEqualTo(2);
        assertThat(reminders.path("orangeCount").asInt() + reminders.path("redCount").asInt())
            .isEqualTo(reminders.path("total").asInt());
    }

    @Test
    void shouldCompleteFrozenReceiveAndCreateGrossingTask() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-RCV-001", "BC-FR-RCV-001");

        JsonNode result = responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/receive/complete".formatted(requested.caseId()),
            USER_RECEIVE,
            """
                {
                  "terminalCode": "T-FR-REC-01",
                  "remarks": "frozen receive completed"
                }
                """), 200);

        assertThat(result.path("taskStatus").asText()).isEqualTo("COMPLETED");
        assertThat(result.path("sessionStatus").asText()).isEqualTo("RECEIVED");
        assertThat(result.path("nextTaskType").asText()).isEqualTo("GROSSING");
        assertThat(result.path("frozenPathologyNo").asText()).isNotBlank();

        JsonNode grossingTasks = listPendingTasks("GROSSING", result.path("frozenPathologyNo").asText(), USER_M3_GROSSING);
        assertThat(grossingTasks.path("total").asInt()).isEqualTo(1);
        assertThat(grossingTasks.path("items").get(0).path("caseId").asText()).isEqualTo(requested.caseId());

        List<String> eventTypes = namedParameterJdbcTemplate.queryForList("""
            select event_type
            from workflow_events
            where case_id = :caseId
            order by event_time asc, created_at asc
            """, Map.of("caseId", requested.caseId()), String.class);
        assertThat(eventTypes).contains("FROZEN_RECEIVE_COMPLETED");
    }

    @Test
    void shouldCompleteFrozenReceiveWithoutTerminalCode() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-RCV-003", "BC-FR-RCV-003");

        JsonNode result = responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/receive/complete".formatted(requested.caseId()),
            USER_RECEIVE,
            """
                {
                  "remarks": "frozen receive without terminal"
                }
                """), 200);

        assertThat(result.path("taskStatus").asText()).isEqualTo("COMPLETED");
        assertThat(result.path("sessionStatus").asText()).isEqualTo("RECEIVED");

        String terminalCode = namedParameterJdbcTemplate.queryForObject("""
            select source_terminal
            from workflow_events
            where case_id = :caseId
              and event_type = 'FROZEN_RECEIVE_COMPLETED'
            order by event_time desc, created_at desc
            limit 1
            """, Map.of("caseId", requested.caseId()), String.class);
        assertThat(terminalCode).isNull();
    }

    @Test
    void shouldRejectFrozenReceiveWhenSessionAlreadyReceived() throws Exception {
        FrozenCaseContext received = prepareFrozenReceivedCase("APP-FR-RCV-002", "BC-FR-RCV-002");

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/receive/complete".formatted(received.caseId()),
            USER_RECEIVE,
            """
                {
                  "terminalCode": "T-FR-REC-02",
                  "remarks": "repeat frozen receive"
                }
                """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("Frozen receive can only be completed from requested status")));
    }

    @Test
    void shouldCompleteFrozenGrossingAndCreateSlicingTask() throws Exception {
        FrozenCaseContext received = prepareFrozenReceivedCase("APP-FR-GRS-001", "BC-FR-GRS-001");

        JsonNode result = responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/grossing/complete".formatted(received.caseId()),
            USER_M3_GROSSING,
            """
                {
                  "terminalCode": "T-FR-GRS-01",
                  "remarks": "frozen grossing completed"
                }
                """), 200);

        assertThat(result.path("taskStatus").asText()).isEqualTo("COMPLETED");
        assertThat(result.path("sessionStatus").asText()).isEqualTo("GROSSING");
        assertThat(result.path("nextTaskType").asText()).isEqualTo("SLICING");

        JsonNode slicingTasks = listPendingTasks("SLICING", received.pathologyNo(), USER_M3_SLICING);
        assertThat(slicingTasks.path("total").asInt()).isEqualTo(1);
        assertThat(slicingTasks.path("items").get(0).path("caseId").asText()).isEqualTo(received.caseId());

        List<String> eventTypes = namedParameterJdbcTemplate.queryForList("""
            select event_type
            from workflow_events
            where case_id = :caseId
            order by event_time asc, created_at asc
            """, Map.of("caseId", received.caseId()), String.class);
        assertThat(eventTypes).contains("FROZEN_GROSSING_COMPLETED");
    }

    @Test
    void shouldCompleteFrozenSlicingAndMoveSessionToDiagnosing() throws Exception {
        FrozenCaseContext grossing = prepareFrozenGrossingCompletedCase("APP-FR-SLC-001", "BC-FR-SLC-001");

        JsonNode result = responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/slicing/complete".formatted(grossing.caseId()),
            USER_M3_SLICING,
            """
                {
                  "terminalCode": "T-FR-SLC-01",
                  "remarks": "frozen slicing completed"
                }
                """), 200);

        assertThat(result.path("taskStatus").asText()).isEqualTo("COMPLETED");
        assertThat(result.path("sessionStatus").asText()).isEqualTo("DIAGNOSING");
        assertThat(result.path("nextTaskType").asText()).isEqualTo("REPORT");

        String caseStatus = namedParameterJdbcTemplate.queryForObject("""
            select case_status
            from pathology_cases
            where id = :caseId
            """, Map.of("caseId", grossing.caseId()), String.class);
        assertThat(caseStatus).isEqualTo("DIAGNOSIS_PENDING");

        List<String> eventTypes = namedParameterJdbcTemplate.queryForList("""
            select event_type
            from workflow_events
            where case_id = :caseId
            order by event_time asc, created_at asc
            """, Map.of("caseId", grossing.caseId()), String.class);
        assertThat(eventTypes).contains("FROZEN_SLICING_COMPLETED");
    }

    @Test
    void shouldCreateFrozenDiagnosticTaskTypeAfterFrozenSlicingCompleted() throws Exception {
        FrozenCaseContext grossing = prepareFrozenGrossingCompletedCase("APP-FR-SLC-101", "BC-FR-SLC-101");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/slicing/complete".formatted(grossing.caseId()),
            USER_M3_SLICING,
            """
                {
                  "terminalCode": "T-FR-SLC-101",
                  "remarks": "complete slicing for frozen diagnostic task type check"
                }
                """), 200);

        JsonNode pendingDiagnosticTasks = listPendingDiagnosticTasks(grossing.pathologyNo(), USER_M4_ASSIGN);
        assertThat(pendingDiagnosticTasks.path("total").asInt()).isEqualTo(1);
        assertThat(pendingDiagnosticTasks.path("items").get(0).path("taskType").asText()).isEqualTo("FROZEN");
    }

    @Test
    void shouldCreateFrozenScopeReportAndExposeItInDiagnosticWorkbench() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-057", "BC-FR-RPT-057");

        JsonNode createdReport = responseBody(postJson(
            "/api/v1/pathology-reports",
            USER_M4_DIAGNOSIS,
            """
                {
                  "caseId":"%s",
                  "taskId":"%s",
                  "clinicalDiagnosis":"术中快速临床诊断",
                  "grossExam":"冰冻大体所见",
                  "microscopicExam":"冰冻镜下所见",
                  "finalDiagnosis":"倾向良性病变",
                  "richTextContent":"<p>冰冻草稿报告</p>",
                  "terminalCode":"T-FR-RPT-057-01"
                }
                """.formatted(diagnosing.caseId(), diagnosing.diagnosticTaskId())), 200);

        String reportId = createdReport.path("reportId").asText();
        String reportScope = namedParameterJdbcTemplate.queryForObject("""
            select report_scope
            from pathology_reports
            where id = :reportId
            """, Map.of("reportId", reportId), String.class);
        assertThat(reportScope).isEqualTo("FROZEN");

        JsonNode workbench = diagnosticWorkbench(diagnosing.caseId(), USER_M4_DIAGNOSIS);
        assertThat(workbench.path("currentReport").path("reportId").asText()).isEqualTo(reportId);
        assertThat(workbench.path("currentReport").path("reportStatus").asText()).isEqualTo("DRAFT");
    }

    @Test
    void shouldExposeFrozenScopeReportInReportTrackingAndCaseReportVersions() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-058", "BC-FR-RPT-058");

        JsonNode createdReport = responseBody(postJson(
            "/api/v1/pathology-reports",
            USER_M4_DIAGNOSIS,
            """
                {
                  "caseId":"%s",
                  "taskId":"%s",
                  "clinicalDiagnosis":"术中快速临床诊断-追踪",
                  "grossExam":"冰冻大体所见-追踪",
                  "microscopicExam":"冰冻镜下所见-追踪",
                  "finalDiagnosis":"倾向良性病变-追踪",
                  "richTextContent":"<p>冰冻追踪草稿报告</p>",
                  "terminalCode":"T-FR-RPT-058-01"
                }
                """.formatted(diagnosing.caseId(), diagnosing.diagnosticTaskId())), 200);

        String reportId = createdReport.path("reportId").asText();

        JsonNode tracking = reportTrackingByIdentifier(diagnosing.pathologyNo(), USER_M4_TRACKING);
        assertThat(tracking.path("currentReport").path("reportId").asText()).isEqualTo(reportId);
        assertThat(tracking.path("currentReport").path("reportStatus").asText()).isEqualTo("DRAFT");

        JsonNode versions = caseReportVersions(diagnosing.pathologyNo(), USER_M4_REVIEW);
        assertThat(versions.isArray()).isTrue();
        assertThat(versions.toString()).contains(reportId);
    }

    @Test
    void shouldExposeFrozenRequestedStageInCaseLifecycleTracking() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-LIFE-001", "BC-FR-LIFE-001");

        JsonNode lifecycle = lifecycleTracking(requested.caseId(), USER_M4_TRACKING);
        assertThat(lifecycle.path("caseSummary").path("applicationType").asText()).isEqualTo("FROZEN");
        assertThat(lifecycle.path("overallTimeline").isArray()).isTrue();
        assertThat(lifecycle.path("overallTimeline").toString()).contains("FROZEN_REQUESTED");
    }

    @Test
    void shouldExposeFrozenAppointmentAsCurrentNodeWhileFrozenSessionIsRequested() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-TRACK-001", "BC-FR-TRACK-001");

        JsonNode tracking = responseBody(mockMvc.perform(authorized(
            get("/api/v1/applications/{id}/tracking", requested.applicationId()),
            USER_TRACKING)), 200);

        assertThat(tracking.path("applicationType").asText()).isEqualTo("FROZEN");
        assertThat(tracking.path("currentNode").asText()).isEqualTo("APPOINTMENT");
        assertThat(tracking.path("recentEvents").toString()).contains("FROZEN_REQUESTED");
    }

    @Test
    void shouldExposeFrozenAppointmentAsCurrentNodeInApplicationListWhileFrozenSessionIsRequested() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-TRACK-002", "BC-FR-TRACK-002");

        JsonNode page = responseBody(mockMvc.perform(authorized(
            get("/api/v1/applications")
                .param("page", "1")
                .param("size", "20")
                .param("applicationNo", "APP-FR-TRACK-002"),
            USER_ADMIN)), 200);

        assertThat(page.path("total").asInt()).isEqualTo(1);
        assertThat(page.path("items").get(0).path("id").asText()).isEqualTo(requested.applicationId());
        assertThat(page.path("items").get(0).path("applicationType").asText()).isEqualTo("FROZEN");
        assertThat(page.path("items").get(0).path("currentNode").asText()).isEqualTo("APPOINTMENT");
    }

    @Test
    void shouldSavePreliminaryResultCompletePhoneBackConfirmReportCompareParaffinAndCloseFrozenSession() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-001", "BC-FR-RPT-001");

        JsonNode preliminary = responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-01",
                  "remarks": "save frozen preliminary",
                  "preliminaryResult": "考虑低级别肿瘤"
                }
                """), 200);
        assertThat(preliminary.path("taskType").asText()).isEqualTo("REPORT");
        assertThat(preliminary.path("taskStatus").asText()).isEqualTo("IN_PROGRESS");
        assertThat(preliminary.path("sessionStatus").asText()).isEqualTo("DIAGNOSING");
        assertThat(preliminary.path("nextTaskType").asText()).isEqualTo("PHONE_BACK");

        JsonNode afterPreliminary = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(afterPreliminary.path("preliminaryResult").asText()).isEqualTo("考虑低级别肿瘤");
        assertThat(afterPreliminary.path("finalDiagnosis").asText()).isEqualTo("考虑低级别肿瘤");
        assertThat(afterPreliminary.path("sessionStatus").asText()).isEqualTo("DIAGNOSING");
        assertThat(afterPreliminary.path("timeline").toString()).contains("FROZEN_PRELIMINARY_SAVED");

        JsonNode phoneBack = responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-02",
                  "remarks": "phone back completed",
                  "preliminaryResult": "考虑低级别肿瘤"
                }
                """), 200);
        assertThat(phoneBack.path("taskType").asText()).isEqualTo("PHONE_BACK");
        assertThat(phoneBack.path("taskStatus").asText()).isEqualTo("COMPLETED");
        assertThat(phoneBack.path("sessionStatus").asText()).isEqualTo("REPORTED");
        assertThat(phoneBack.path("nextTaskType").asText()).isEqualTo("REPORT");

        JsonNode afterPhoneBack = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(afterPhoneBack.path("intraoperativePhoneBack").asBoolean()).isTrue();
        assertThat(afterPhoneBack.path("phoneBackAt").asText()).isNotBlank();
        assertThat(afterPhoneBack.path("currentTaskType").asText()).isEqualTo("REPORT");
        assertThat(afterPhoneBack.path("timeline").toString()).contains("FROZEN_PHONE_BACK_COMPLETED");

        JsonNode confirmed = responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-03",
                  "remarks": "confirm frozen report"
                }
                """), 200);
        assertThat(confirmed.path("taskType").asText()).isEqualTo("REPORT");
        assertThat(confirmed.path("taskStatus").asText()).isEqualTo("COMPLETED");
        assertThat(confirmed.path("sessionStatus").asText()).isEqualTo("CONFIRMED");
        assertThat(confirmed.path("nextTaskType").asText()).isEqualTo("COMPARE");

        JsonNode afterConfirm = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(afterConfirm.path("finalConfirmedAt").asText()).isNotBlank();
        assertThat(afterConfirm.path("reportConfirmedAt").asText()).isNotBlank();
        assertThat(afterConfirm.path("timeline").toString()).contains("FROZEN_REPORT_CONFIRMED");

        JsonNode compare = responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(diagnosing.caseId()),
            USER_M4_REVIEW,
            """
                {
                  "terminalCode": "T-FR-RPT-04",
                  "remarks": "complete paraffin compare",
                  "compareStatus": "SIGNED_OFF",
                  "compareSummary": "冰石一致"
                }
                """), 200);
        assertThat(compare.path("taskType").asText()).isEqualTo("COMPARE");
        assertThat(compare.path("taskStatus").asText()).isEqualTo("COMPLETED");
        assertThat(compare.path("sessionStatus").asText()).isEqualTo("PARAFFIN_REVIEWED");
        assertThat(compare.path("nextTaskType").asText()).isEqualTo("REMAINING_TISSUE");

        JsonNode afterCompare = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(afterCompare.path("compareStatus").asText()).isEqualTo("SIGNED_OFF");
        assertThat(afterCompare.path("compareSummary").asText()).isEqualTo("冰石一致");
        assertThat(afterCompare.path("currentTaskType").asText()).isEqualTo("REMAINING_TISSUE");
        assertThat(afterCompare.path("timeline").toString()).contains("FROZEN_COMPARE_COMPLETED");

        JsonNode closed = responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/remaining-tissue/complete".formatted(diagnosing.caseId()),
            USER_M3_GROSSING,
            """
                {
                  "terminalCode": "T-FR-RPT-05",
                  "remarks": "close frozen workflow",
                  "remainingTissueStatus": "DISPOSED"
                }
                """), 200);
        assertThat(closed.path("taskType").asText()).isEqualTo("REMAINING_TISSUE");
        assertThat(closed.path("taskStatus").asText()).isEqualTo("COMPLETED");
        assertThat(closed.path("sessionStatus").asText()).isEqualTo("CLOSED");
        assertThat(closed.path("nextTaskType").isNull()).isTrue();

        JsonNode afterClose = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(afterClose.path("currentTaskType").asText()).isEqualTo("REMAINING_TISSUE");
        assertThat(afterClose.path("remainingTissueStatus").asText()).isEqualTo("DISPOSED");
        assertThat(afterClose.path("sessionStatus").asText()).isEqualTo("CLOSED");
        assertThat(afterClose.path("timeline").toString()).contains("FROZEN_CLOSED");
        assertThat(afterClose.path("timeline").toString()).doesNotContain("\"nodeCode\":\"HANDOVER\"");
        assertThat(afterClose.path("timeline").toString()).contains("\"nodeCode\":\"REMAINING_TISSUE\"");
        assertThat(afterClose.path("tasks").toString()).doesNotContain("\"taskType\":\"HANDOVER\"");

        Map<String, Object> diagnosticTask = namedParameterJdbcTemplate.queryForMap("""
            select status, frozen_diagnosis_result
            from diagnostic_tasks
            where id = :taskId
            """, Map.of("taskId", diagnosing.diagnosticTaskId()));
        assertThat(diagnosticTask.get("STATUS")).isEqualTo("COMPLETED");
        assertThat(diagnosticTask.get("FROZEN_DIAGNOSIS_RESULT")).isEqualTo("考虑低级别肿瘤");
    }

    @Test
    void shouldSyncPathologyCaseStatusAfterFrozenReportConfirmationAndClosure() throws Exception {
        FrozenCaseContext confirmed = prepareFrozenConfirmedCase("APP-FR-RPT-123", "BC-FR-RPT-123");

        String confirmedCaseStatus = namedParameterJdbcTemplate.queryForObject("""
            select case_status
            from pathology_cases
            where id = :caseId
            """, Map.of("caseId", confirmed.caseId()), String.class);
        assertThat(confirmedCaseStatus).isEqualTo("REPORT_PUBLISHED");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(confirmed.caseId()),
            USER_M4_REVIEW,
            """
                {
                  "terminalCode": "T-FR-RPT-57",
                  "remarks": "compare before case status sync check",
                  "compareStatus": "SIGNED_OFF",
                  "compareSummary": "冰石一致"
                }
                """), 200);

        String reviewedCaseStatus = namedParameterJdbcTemplate.queryForObject("""
            select case_status
            from pathology_cases
            where id = :caseId
            """, Map.of("caseId", confirmed.caseId()), String.class);
        assertThat(reviewedCaseStatus).isEqualTo("REPORT_PUBLISHED");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/remaining-tissue/complete".formatted(confirmed.caseId()),
            USER_M3_GROSSING,
            """
                {
                  "terminalCode": "T-FR-RPT-58",
                  "remarks": "close after case status sync check",
                  "remainingTissueStatus": "DISPOSED"
                }
                """), 200);

        String closedCaseStatus = namedParameterJdbcTemplate.queryForObject("""
            select case_status
            from pathology_cases
            where id = :caseId
            """, Map.of("caseId", confirmed.caseId()), String.class);
        assertThat(closedCaseStatus).isEqualTo("CLOSED");
    }

    @Test
    void shouldKeepCompletedTechnicalTasksCompletedAfterFrozenSessionClosed() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-014", "BC-FR-RPT-014");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-36",
                  "remarks": "save preliminary before completed task retention check",
                  "preliminaryResult": "疑似低级别病变"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-37",
                  "remarks": "phone back before completed task retention check",
                  "preliminaryResult": "疑似低级别病变"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-38",
                  "remarks": "confirm before completed task retention check"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(diagnosing.caseId()),
            USER_M4_REVIEW,
            """
                {
                  "terminalCode": "T-FR-RPT-39",
                  "remarks": "compare before completed task retention check",
                  "compareStatus": "SIGNED_OFF",
                  "compareSummary": "冰石一致"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/remaining-tissue/complete".formatted(diagnosing.caseId()),
            USER_M3_GROSSING,
            """
                {
                  "terminalCode": "T-FR-RPT-40",
                  "remarks": "close before completed task retention check",
                  "remainingTissueStatus": "DISPOSED"
                }
                """), 200);

        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(detail.path("sessionStatus").asText()).isEqualTo("CLOSED");

        JsonNode tasksByType = indexTasksByType(detail.path("tasks"));
        assertThat(tasksByType.path("RECEIVE").path("status").asText()).isEqualTo("COMPLETED");
        assertThat(tasksByType.path("GROSSING").path("status").asText()).isEqualTo("COMPLETED");
        assertThat(tasksByType.path("SLICING").path("status").asText()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldRejectFrozenPhoneBackBeforeSavingPreliminaryResult() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-002", "BC-FR-RPT-002");

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-06",
                  "remarks": "invalid frozen phone back",
                  "preliminaryResult": "直接电话回报"
                }
                """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("Frozen preliminary result must be saved before phone back")));
    }

    @Test
    void shouldRejectFrozenPhoneBackAfterAlreadyCompleted() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-016", "BC-FR-RPT-016");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-42",
                  "remarks": "save preliminary before repeat phone back test",
                  "preliminaryResult": "疑似低级别乳头状病变"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-43",
                  "remarks": "first phone back",
                  "preliminaryResult": "疑似低级别乳头状病变"
                }
                """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-44",
                  "remarks": "repeat phone back",
                  "preliminaryResult": "重复电话回报"
                }
                """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("Frozen phone back is already completed")));
    }

    @Test
    void shouldRejectFrozenPreliminarySaveAfterPhoneBackCompleted() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-017", "BC-FR-RPT-017");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-45",
                  "remarks": "save preliminary before immutable-after-phone-back test",
                  "preliminaryResult": "疑似导管上皮增生"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-46",
                  "remarks": "complete phone back before immutable-after-phone-back test",
                  "preliminaryResult": "疑似导管上皮增生"
                }
                """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-47",
                  "remarks": "repeat save after phone back",
                  "preliminaryResult": "术后补改结果"
                }
                """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("Frozen preliminary report cannot be saved after phone back is completed")));
    }

    @Test
    void shouldRejectFrozenPhoneBackAfterCompletionByUnassignedDiagnosisDoctor() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-018", "BC-FR-RPT-018");
        String otherDiagnosisUserId = createDiagnosisUser("FRPHONEBACK");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-48",
                  "remarks": "save preliminary before outsider repeated phone back test",
                  "preliminaryResult": "疑似滤泡性病变"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-49",
                  "remarks": "complete phone back before outsider repeated phone back test",
                  "preliminaryResult": "疑似滤泡性病变"
                }
                """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            otherDiagnosisUserId,
            """
                {
                  "terminalCode": "T-FR-RPT-50",
                  "remarks": "outsider repeated phone back",
                  "preliminaryResult": "越权电话回报"
                }
                """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message", containsString("用户未被分配到诊断任务")));
    }

    @Test
    void shouldRejectFrozenPreliminarySaveAfterPhoneBackCompletedByUnassignedDiagnosisDoctor() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-019", "BC-FR-RPT-019");
        String otherDiagnosisUserId = createDiagnosisUser("FRPRESAVE");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-51",
                  "remarks": "save preliminary before outsider repeated preliminary save test",
                  "preliminaryResult": "疑似乳头状增生"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-52",
                  "remarks": "complete phone back before outsider repeated preliminary save test",
                  "preliminaryResult": "疑似乳头状增生"
                }
                """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            otherDiagnosisUserId,
            """
                {
                  "terminalCode": "T-FR-RPT-53",
                  "remarks": "outsider repeated preliminary save",
                  "preliminaryResult": "越权修改结果"
                }
                """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message", containsString("用户未被分配到诊断任务")));
    }

    @Test
    void shouldKeepFrozenSessionAtReportStageUntilReportConfirmed() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-009", "BC-FR-RPT-009");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-27",
                  "remarks": "save preliminary before stage check",
                  "preliminaryResult": "疑似乳头状肿瘤"
                }
                """), 200);

        JsonNode phoneBack = responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-28",
                  "remarks": "phone back before stage check",
                  "preliminaryResult": "疑似乳头状肿瘤"
                }
                """), 200);
        assertThat(phoneBack.path("taskType").asText()).isEqualTo("PHONE_BACK");
        assertThat(phoneBack.path("taskStatus").asText()).isEqualTo("COMPLETED");
        assertThat(phoneBack.path("sessionStatus").asText()).isEqualTo("REPORTED");
        assertThat(phoneBack.path("nextTaskType").asText()).isEqualTo("REPORT");

        JsonNode afterPhoneBack = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(afterPhoneBack.path("currentTaskType").asText()).isEqualTo("REPORT");

        JsonNode tasksByType = indexTasksByType(afterPhoneBack.path("tasks"));
        assertThat(tasksByType.path("REPORT").path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(tasksByType.path("PHONE_BACK").path("status").asText()).isEqualTo("COMPLETED");
        assertThat(tasksByType.path("COMPARE").path("status").asText()).isEqualTo("PENDING");
    }

    @Test
    void shouldMarkFrozenReportTaskInProgressAfterDiagnosticTaskStarted() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-010", "BC-FR-RPT-010");

        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(detail.path("currentTaskType").asText()).isEqualTo("REPORT");
        assertThat(detail.path("sessionStatus").asText()).isEqualTo("DIAGNOSING");

        JsonNode tasksByType = indexTasksByType(detail.path("tasks"));
        assertThat(tasksByType.path("REPORT").path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(tasksByType.path("REPORT").path("startedAt").isNull()).isFalse();
        assertThat(tasksByType.path("REPORT").path("startedAt").asText()).isNotBlank();
        assertThat(tasksByType.path("PHONE_BACK").path("status").asText()).isEqualTo("PENDING");
        assertThat(tasksByType.path("COMPARE").path("status").asText()).isEqualTo("PENDING");
    }

    @Test
    void shouldMarkFrozenPhoneBackTaskInProgressAfterPreliminarySaved() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-015", "BC-FR-RPT-015");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-41",
                  "remarks": "save preliminary before phone back task check",
                  "preliminaryResult": "疑似轻度病变"
                }
                """), 200);

        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(detail.path("currentTaskType").asText()).isEqualTo("REPORT");
        assertThat(detail.path("sessionStatus").asText()).isEqualTo("DIAGNOSING");

        JsonNode tasksByType = indexTasksByType(detail.path("tasks"));
        assertThat(tasksByType.path("REPORT").path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(tasksByType.path("PHONE_BACK").path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(tasksByType.path("COMPARE").path("status").asText()).isEqualTo("PENDING");
    }

    @Test
    void shouldMarkFrozenCompareTaskInProgressAfterReportConfirmed() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-012", "BC-FR-RPT-012");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-29",
                  "remarks": "save preliminary before compare task check",
                  "preliminaryResult": "疑似导管内病变"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-30",
                  "remarks": "phone back before compare task check",
                  "preliminaryResult": "疑似导管内病变"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-31",
                  "remarks": "confirm before compare task check"
                }
                """), 200);

        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(detail.path("currentTaskType").asText()).isEqualTo("COMPARE");
        assertThat(detail.path("sessionStatus").asText()).isEqualTo("CONFIRMED");
        assertThat(detail.path("compareStatus").asText()).isEqualTo("PENDING");

        JsonNode tasksByType = indexTasksByType(detail.path("tasks"));
        assertThat(tasksByType.path("COMPARE").path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(tasksByType.path("REMAINING_TISSUE").path("status").asText()).isEqualTo("PENDING");
    }

    @Test
    void shouldMarkFrozenRemainingTissueTaskInProgressAfterCompareCompleted() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-013", "BC-FR-RPT-013");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-32",
                  "remarks": "save preliminary before remaining tissue task check",
                  "preliminaryResult": "疑似乳头状病变"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-33",
                  "remarks": "phone back before remaining tissue task check",
                  "preliminaryResult": "疑似乳头状病变"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-34",
                  "remarks": "confirm before remaining tissue task check"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(diagnosing.caseId()),
            USER_M4_REVIEW,
            """
                {
                  "terminalCode": "T-FR-RPT-35",
                  "remarks": "compare before remaining tissue task check",
                  "compareStatus": "SIGNED_OFF",
                  "compareSummary": "冰石一致"
                }
                """), 200);

        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(detail.path("currentTaskType").asText()).isEqualTo("REMAINING_TISSUE");
        assertThat(detail.path("sessionStatus").asText()).isEqualTo("PARAFFIN_REVIEWED");

        JsonNode tasksByType = indexTasksByType(detail.path("tasks"));
        assertThat(tasksByType.path("REMAINING_TISSUE").path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(detail.path("tasks").toString()).doesNotContain("\"taskType\":\"HANDOVER\"");
    }

    @Test
    void shouldPopulateRequestedAtForStartedFrozenDiagnosticSession() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-011", "BC-FR-RPT-011");

        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);

        assertThat(detail.path("requestedAt").isNull()).isFalse();
        assertThat(detail.path("requestedAt").asText()).isNotBlank();
    }

    @Test
    void shouldMarkFrozenSessionRegularCaseLinkedWhenCheckItemIncludesRoutinePathology() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-RPT-024", "BC-FR-RPT-024");
        updateFrozenRegistrationCheckItem(requested.caseId(), "术中冰冻+常规病理");

        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", requested.caseId()),
            USER_RECEIVE)), 200);

        assertThat(detail.path("hasRegularCaseLinked").asBoolean()).isTrue();
    }

    @Test
    void shouldKeepFrozenTechnicalStartedAtAfterSessionProgressedToDiagnosing() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenDiagnosingCase("APP-FR-RPT-023", "BC-FR-RPT-023");

        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);

        assertThat(detail.path("grossingStartedAt").isNull()).isFalse();
        assertThat(detail.path("grossingStartedAt").asText()).isNotBlank();
        assertThat(detail.path("slicingStartedAt").isNull()).isFalse();
        assertThat(detail.path("slicingStartedAt").asText()).isNotBlank();

        JsonNode tasksByType = indexTasksByType(detail.path("tasks"));
        assertThat(tasksByType.path("GROSSING").path("startedAt").isNull()).isFalse();
        assertThat(tasksByType.path("GROSSING").path("startedAt").asText()).isNotBlank();
        assertThat(tasksByType.path("SLICING").path("startedAt").isNull()).isFalse();
        assertThat(tasksByType.path("SLICING").path("startedAt").asText()).isNotBlank();
    }

    @Test
    void shouldRejectFrozenPreliminarySaveBeforeDiagnosticTaskAssigned() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenDiagnosingCase("APP-FR-RPT-006", "BC-FR-RPT-006");

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-22",
                  "remarks": "save preliminary before assignment",
                  "preliminaryResult": "疑似纤维瘤"
                }
                """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message", containsString("用户未被分配到诊断任务")));
    }

    @Test
    void shouldRejectFrozenPreliminarySaveBeforeDiagnosticTaskStarted() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenDiagnosingCase("APP-FR-RPT-008", "BC-FR-RPT-008");

        JsonNode pendingDiagnosticTasks = listPendingDiagnosticTasks(diagnosing.pathologyNo(), USER_M4_ASSIGN);
        assertThat(pendingDiagnosticTasks.path("total").asInt()).isEqualTo(1);
        String diagnosticTaskId = pendingDiagnosticTasks.path("items").get(0).path("id").asText();

        postJson("/api/v1/diagnostic-tasks/%s/assign".formatted(diagnosticTaskId), USER_M4_ASSIGN, """
            {
              "diagnosisDoctorUserId":"USER_M4_DIAGNOSIS",
              "diagnosisDoctorName":"M4 Diagnosis",
              "primaryDoctorUserId":"USER_M4_DIAGNOSIS",
              "primaryDoctorName":"M4 Diagnosis",
              "reviewerUserId":"USER_M4_REVIEW",
              "reviewerName":"M4 Review",
              "terminalCode":"T-FR-DIAG-04"
            }
            """).andExpect(status().isOk());

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-26",
                  "remarks": "save preliminary before diagnostic task started",
                  "preliminaryResult": "疑似黏液性病变"
                }
                """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("Frozen diagnostic task must be started before report actions")));
    }

    @Test
    void shouldRejectFrozenReportConfirmationAfterAlreadyConfirmed() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-003", "BC-FR-RPT-003");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-07",
                  "remarks": "save preliminary before repeat confirm test",
                  "preliminaryResult": "疑似腺瘤"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-08",
                  "remarks": "phone back before repeat confirm test",
                  "preliminaryResult": "疑似腺瘤"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-09",
                  "remarks": "first confirm"
                }
                """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-10",
                  "remarks": "repeat confirm"
                }
                """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("Frozen report is already confirmed")));
    }

    @Test
    void shouldRejectFrozenReportConfirmationByUnassignedDiagnosisDoctor() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-007", "BC-FR-RPT-007");
        String otherDiagnosisUserId = createDiagnosisUser("FRCONFIRM");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-23",
                  "remarks": "save preliminary before outsider confirm test",
                  "preliminaryResult": "疑似腺性病变"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-24",
                  "remarks": "phone back before outsider confirm test",
                  "preliminaryResult": "疑似腺性病变"
                }
                """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            otherDiagnosisUserId,
            """
                {
                  "terminalCode": "T-FR-RPT-25",
                  "remarks": "outsider confirm"
                }
                """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message", containsString("用户未被分配到诊断任务")));
    }

    @Test
    void shouldRejectFrozenReportConfirmationAfterAlreadyConfirmedByUnassignedDiagnosisDoctor() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-020", "BC-FR-RPT-020");
        String otherDiagnosisUserId = createDiagnosisUser("FRCONFIRMREPEAT");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-27",
                  "remarks": "save preliminary before outsider repeat confirm test",
                  "preliminaryResult": "疑似乳头状瘤"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-28",
                  "remarks": "phone back before outsider repeat confirm test",
                  "preliminaryResult": "疑似乳头状瘤"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-29",
                  "remarks": "first confirm before outsider repeat confirm test"
                }
                """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            otherDiagnosisUserId,
            """
                {
                  "terminalCode": "T-FR-RPT-30",
                  "remarks": "outsider repeat confirm"
                }
                """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message", containsString("用户未被分配到诊断任务")));
    }

    @Test
    void shouldRejectFrozenRemainingTissueHandlingAfterSessionClosed() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-004", "BC-FR-RPT-004");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-11",
                  "remarks": "save preliminary before close repeat test",
                  "preliminaryResult": "疑似乳头状病变"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-12",
                  "remarks": "phone back before close repeat test",
                  "preliminaryResult": "疑似乳头状病变"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-13",
                  "remarks": "confirm before close repeat test"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(diagnosing.caseId()),
            USER_M4_REVIEW,
            """
                {
                  "terminalCode": "T-FR-RPT-14",
                  "remarks": "compare before close repeat test",
                  "compareStatus": "SIGNED_OFF",
                  "compareSummary": "冰石一致"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/remaining-tissue/complete".formatted(diagnosing.caseId()),
            USER_M3_GROSSING,
            """
                {
                  "terminalCode": "T-FR-RPT-15",
                  "remarks": "first close",
                  "remainingTissueStatus": "DISPOSED"
                }
                """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/remaining-tissue/complete".formatted(diagnosing.caseId()),
            USER_M3_GROSSING,
            """
                {
                  "terminalCode": "T-FR-RPT-16",
                  "remarks": "repeat close",
                  "remainingTissueStatus": "DISPOSED"
                }
                """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("Frozen session is already closed")));
    }

    @Test
    void shouldRejectFrozenRemainingTissueHandlingByUserWithoutGrossingPermission() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-022", "BC-FR-RPT-022");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-52",
                  "remarks": "save preliminary before forbidden remaining tissue test",
                  "preliminaryResult": "疑似导管内乳头状病变"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-53",
                  "remarks": "phone back before forbidden remaining tissue test",
                  "preliminaryResult": "疑似导管内乳头状病变"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-54",
                  "remarks": "confirm before forbidden remaining tissue test"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(diagnosing.caseId()),
            USER_M4_REVIEW,
            """
                {
                  "terminalCode": "T-FR-RPT-55",
                  "remarks": "compare before forbidden remaining tissue test",
                  "compareStatus": "SIGNED_OFF",
                  "compareSummary": "冰石一致"
                }
                """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/remaining-tissue/complete".formatted(diagnosing.caseId()),
            USER_M4_NO_PERMISSION,
            """
                {
                  "terminalCode": "T-FR-RPT-56",
                  "remarks": "forbidden close",
                  "remainingTissueStatus": "DISPOSED"
                }
                """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void shouldRejectFrozenParaffinCompareAfterAlreadyCompleted() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-005", "BC-FR-RPT-005");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-17",
                  "remarks": "save preliminary before repeat compare test",
                  "preliminaryResult": "疑似间质瘤"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-18",
                  "remarks": "phone back before repeat compare test",
                  "preliminaryResult": "疑似间质瘤"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-19",
                  "remarks": "confirm before repeat compare test"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(diagnosing.caseId()),
            USER_M4_REVIEW,
            """
                {
                  "terminalCode": "T-FR-RPT-20",
                  "remarks": "first compare",
                  "compareStatus": "SIGNED_OFF",
                  "compareSummary": "冰石一致"
                }
                """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(diagnosing.caseId()),
            USER_M4_REVIEW,
            """
                {
                  "terminalCode": "T-FR-RPT-21",
                  "remarks": "repeat compare",
                  "compareStatus": "MISMATCH",
                  "compareSummary": "重复对比"
                }
                """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("Frozen paraffin compare is already completed")));
    }

    @Test
    void shouldRejectFrozenParaffinCompareAfterCompletionByUnassignedReviewer() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-021", "BC-FR-RPT-021");
        String otherReviewerUserId = createReviewUser("FRCOMPARE");

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-31",
                  "remarks": "save preliminary before outsider compare test",
                  "preliminaryResult": "疑似纤维腺瘤"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-32",
                  "remarks": "phone back before outsider compare test",
                  "preliminaryResult": "疑似纤维腺瘤"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-33",
                  "remarks": "confirm before outsider compare test"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(diagnosing.caseId()),
            USER_M4_REVIEW,
            """
                {
                  "terminalCode": "T-FR-RPT-34",
                  "remarks": "first compare before outsider compare test",
                  "compareStatus": "SIGNED_OFF",
                  "compareSummary": "冰石一致"
                }
                """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(diagnosing.caseId()),
            otherReviewerUserId,
            """
                {
                  "terminalCode": "T-FR-RPT-35",
                  "remarks": "outsider repeat compare",
                  "compareStatus": "MISMATCH",
                  "compareSummary": "复核人重复提交"
                }
                """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message", containsString("用户未被分配审核报告")));
    }

    @Test
    void shouldRejectFrozenGrossingWithoutPendingGrossingTask() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-GRS-002", "BC-FR-GRS-002");

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/grossing/complete".formatted(requested.caseId()),
            USER_M3_GROSSING,
            """
                {
                  "terminalCode": "T-FR-GRS-02",
                  "remarks": "invalid frozen grossing"
                }
                """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("Frozen grossing task is not pending")));
    }

    private FrozenCaseContext prepareFrozenRequestedCase(String applicationNo, String barcode) throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration(applicationNo, barcode, "DEPT-OR", "OR", "FROZEN");
        return new FrozenCaseContext(context.applicationId(), context.caseId(), context.pathologyNo(), context.specimenId(), null, null, null);
    }

    private void clearFrozenSpecimenFlags(String caseId) {
        namedParameterJdbcTemplate.update("""
            update specimens
            set frozen_flag = 0
            where case_id = :caseId
            """, Map.of("caseId", caseId));
    }

    private String createReviewUser(String suffix) {
        String userId = "USER_M4_REVIEW_" + suffix;
        LocalDateTime now = LocalDateTime.now();
        namedParameterJdbcTemplate.update("""
            insert into users (id, user_code, login_name, name, role, enabled, created_at, updated_at)
            values (:id, :userCode, :loginName, :name, :role, 1, :createdAt, :updatedAt)
            """, Map.of(
            "id", userId,
            "userCode", "U-M4-REV-" + suffix,
            "loginName", "m4.review." + suffix.toLowerCase(),
            "name", "M4 Review " + suffix,
            "role", "M4_REVIEW",
            "createdAt", now,
            "updatedAt", now));
        namedParameterJdbcTemplate.update("""
            insert into user_roles (id, user_id, role_id, is_primary, assigned_at, assigned_by_name)
            values (:id, :userId, 'ROLE_M4_REVIEW', 1, :assignedAt, 'test')
            """, Map.of(
            "id", "UR-M4-REV-" + suffix,
            "userId", userId,
            "assignedAt", now));
        return userId;
    }

    private FrozenCaseContext prepareFrozenReceivedCase(String applicationNo, String barcode) throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration(applicationNo, barcode, "DEPT-OR", "OR", "FROZEN");
        JsonNode completion = responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/receive/complete".formatted(context.caseId()),
            USER_RECEIVE,
            """
                {
                  "terminalCode": "T-FR-REC-SETUP",
                  "remarks": "setup frozen receive"
                }
                """), 200);
        String pathologyNo = completion.path("pathologyNo").asText();
        String grossingTaskId = listPendingTasks("GROSSING", pathologyNo, USER_M3_GROSSING)
            .path("items")
            .get(0)
            .path("id")
            .asText();
        return new FrozenCaseContext(context.applicationId(), context.caseId(), pathologyNo, context.specimenId(), grossingTaskId, null, null);
    }

    private FrozenCaseContext prepareFrozenGrossingCompletedCase(String applicationNo, String barcode) throws Exception {
        FrozenCaseContext received = prepareFrozenReceivedCase(applicationNo, barcode);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/grossing/complete".formatted(received.caseId()),
            USER_M3_GROSSING,
            """
            {
              "terminalCode": "T-FR-GRS-COMPLETE",
              "remarks": "setup frozen grossing"
            }
            """), 200);

        String slicingTaskId = listPendingTasks("SLICING", received.pathologyNo(), USER_M3_SLICING)
            .path("items")
            .get(0)
            .path("id")
            .asText();

        return new FrozenCaseContext(
            received.applicationId(),
            received.caseId(),
            received.pathologyNo(),
            received.specimenId(),
            received.grossingTaskId(),
            slicingTaskId,
            null);
    }

    private FrozenCaseContext prepareFrozenDiagnosingCase(String applicationNo, String barcode) throws Exception {
        FrozenCaseContext grossingCompleted = prepareFrozenGrossingCompletedCase(applicationNo, barcode);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/slicing/complete".formatted(grossingCompleted.caseId()),
            USER_M3_SLICING,
            """
            {
              "terminalCode": "T-FR-SLC-DIAGNOSING",
              "remarks": "setup frozen diagnosing"
            }
            """), 200);
        return grossingCompleted;
    }

    private FrozenCaseContext prepareFrozenStartedDiagnosticCase(String applicationNo, String barcode) throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenDiagnosingCase(applicationNo, barcode);

        JsonNode pendingDiagnosticTasks = listPendingDiagnosticTasks(diagnosing.pathologyNo(), USER_M4_ASSIGN);
        assertThat(pendingDiagnosticTasks.path("total").asInt()).isEqualTo(1);
        String diagnosticTaskId = pendingDiagnosticTasks.path("items").get(0).path("id").asText();

        postJson("/api/v1/diagnostic-tasks/%s/assign".formatted(diagnosticTaskId), USER_M4_ASSIGN, """
            {
              "diagnosisDoctorUserId":"USER_M4_DIAGNOSIS",
              "diagnosisDoctorName":"M4 Diagnosis",
              "primaryDoctorUserId":"USER_M4_DIAGNOSIS",
              "primaryDoctorName":"M4 Diagnosis",
              "reviewerUserId":"USER_M4_REVIEW",
              "reviewerName":"M4 Review",
              "terminalCode":"T-FR-DIAG-01"
            }
            """).andExpect(status().isOk());
        postJson("/api/v1/diagnostic-tasks/%s/accept".formatted(diagnosticTaskId), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-DIAG-02"}
            """).andExpect(status().isOk());
        postJson("/api/v1/diagnostic-tasks/%s/start".formatted(diagnosticTaskId), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-DIAG-03"}
            """).andExpect(status().isOk());

        return new FrozenCaseContext(
            diagnosing.applicationId(),
            diagnosing.caseId(),
            diagnosing.pathologyNo(),
            diagnosing.specimenId(),
            diagnosing.grossingTaskId(),
            diagnosing.slicingTaskId(),
            diagnosticTaskId);
    }

    private FrozenCaseContext prepareFrozenConfirmedCase(String applicationNo, String barcode) throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase(applicationNo, barcode);

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-CONFIRM-01",
                  "remarks": "save preliminary before confirmed reminder test",
                  "preliminaryResult": "考虑良性病变"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-CONFIRM-02",
                  "remarks": "phone back before confirmed reminder test",
                  "preliminaryResult": "考虑良性病变"
                }
                """), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-CONFIRM-03",
                  "remarks": "confirm before confirmed reminder test"
                }
                """), 200);

        return diagnosing;
    }

    private void updateFrozenRegistrationCheckItem(String caseId, String checkItem) {
        String applicationId = namedParameterJdbcTemplate.queryForObject("""
            select application_id
            from pathology_cases
            where id = :caseId
            """, Map.of("caseId", caseId), String.class);
        LocalDateTime now = LocalDateTime.now();
        Long count = namedParameterJdbcTemplate.queryForObject("""
            select count(1)
            from application_registration_workbench
            where application_id = :applicationId
            """, Map.of("applicationId", applicationId), Long.class);
        if (count != null && count > 0) {
            namedParameterJdbcTemplate.update("""
                update application_registration_workbench
                set check_item = :checkItem,
                    updated_at = :updatedAt
                where application_id = :applicationId
                """, Map.of(
                "applicationId", applicationId,
                "checkItem", checkItem,
                "updatedAt", now));
            return;
        }
        namedParameterJdbcTemplate.update("""
            insert into application_registration_workbench
                (application_id, check_item, created_at, updated_at)
            values
                (:applicationId, :checkItem, :createdAt, :updatedAt)
            """, Map.of(
            "applicationId", applicationId,
            "checkItem", checkItem,
            "createdAt", now,
            "updatedAt", now));
    }

    private JsonNode indexSessionsByCaseId(JsonNode sessions) throws Exception {
        StringBuilder builder = new StringBuilder("{");
        for (int index = 0; index < sessions.size(); index++) {
            JsonNode session = sessions.get(index);
            if (index > 0) {
                builder.append(',');
            }
            builder.append('"')
                .append(session.path("caseId").asText())
                .append('"')
                .append(':')
                .append(session.toString());
        }
        builder.append('}');
        return objectMapper.readTree(builder.toString());
    }

    private JsonNode indexTasksByType(JsonNode tasks) throws Exception {
        StringBuilder builder = new StringBuilder("{");
        for (int index = 0; index < tasks.size(); index++) {
            JsonNode task = tasks.get(index);
            if (index > 0) {
                builder.append(',');
            }
            builder.append('"')
                .append(task.path("taskType").asText())
                .append('"')
                .append(':')
                .append(task.toString());
        }
        builder.append('}');
        return objectMapper.readTree(builder.toString());
    }

    private JsonNode indexRemindersByCaseId(JsonNode reminders) throws Exception {
        StringBuilder builder = new StringBuilder("{");
        for (int index = 0; index < reminders.size(); index++) {
            JsonNode reminder = reminders.get(index);
            if (index > 0) {
                builder.append(',');
            }
            builder.append('"')
                .append(reminder.path("caseId").asText())
                .append('"')
                .append(':')
                .append(reminder.toString());
        }
        builder.append('}');
        return objectMapper.readTree(builder.toString());
    }

    private record FrozenCaseContext(
        String applicationId,
        String caseId,
        String pathologyNo,
        String specimenId,
        String grossingTaskId,
        String slicingTaskId,
        String diagnosticTaskId
    ) {
    }
}
