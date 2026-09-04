package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class FrozenWorkflowActionIntegrationTest extends AbstractFrozenWorkflowIntegrationTest {

    @Test
    void shouldCompleteFrozenReportActionsAsWorkbenchOnlyOperator() throws Exception {
        String suffix = uniqueSuffix();
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase(
            "APP-FR-WB-" + suffix,
            "BC-FR-WB-" + suffix);
        String workbenchUserId = createWorkbenchOnlyUser("FR" + suffix);

        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            workbenchUserId,
            "{\"preliminaryResult\":\"工作台冰冻初步\"}"), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            workbenchUserId,
            "{\"preliminaryResult\":\"工作台冰冻初步\"}"), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            workbenchUserId,
            "{}"), 200);
        responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(diagnosing.caseId()),
            workbenchUserId,
            "{\"compareStatus\":\"SIGNED_OFF\",\"compareSummary\":\"工作台冰石一致\"}"), 200);
        JsonNode closed = responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/remaining-tissue/complete".formatted(diagnosing.caseId()),
            workbenchUserId,
            "{\"remainingTissueStatus\":\"DISPOSED\"}"), 200);

        assertThat(closed.path("sessionStatus").asText()).isEqualTo("CLOSED");
        List<String> operators = namedParameterJdbcTemplate.queryForList("""
            select distinct operator_user_id
            from workflow_events
            where case_id = :caseId
              and event_type in ('FROZEN_PRELIMINARY_REPORT_SAVED', 'FROZEN_PHONE_BACK_COMPLETED',
                                 'FROZEN_REPORT_CONFIRMED', 'FROZEN_PARAFFIN_COMPARE_COMPLETED',
                                 'FROZEN_REMAINING_TISSUE_COMPLETED')
            """, Map.of("caseId", diagnosing.caseId()), String.class);
        assertThat(operators).containsOnly(workbenchUserId);
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

        FrozenCaseContext grossingCompleted = prepareFrozenGrossingCompletedCase("APP-FR-ALIAS-005", "BC-FR-ALIAS-005");
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
        assertThat(namedParameterJdbcTemplate.queryForList("""
            select event_type
            from workflow_events
            where case_id = :caseId
            order by event_time asc, created_at asc
            """, Map.of("caseId", requested.caseId()), String.class)).contains("FROZEN_RECEIVE_COMPLETED");
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

        assertThat(result.path("sessionStatus").asText()).isEqualTo("GROSSING");
        JsonNode slicingTasks = listPendingTasks("SLICING", received.pathologyNo(), USER_M3_SLICING);
        assertThat(slicingTasks.path("total").asInt()).isEqualTo(1);
        assertThat(namedParameterJdbcTemplate.queryForList("""
            select event_type
            from workflow_events
            where case_id = :caseId
            order by event_time asc, created_at asc
            """, Map.of("caseId", received.caseId()), String.class)).contains("FROZEN_GROSSING_COMPLETED");
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

        assertThat(result.path("sessionStatus").asText()).isEqualTo("DIAGNOSING");
        String caseStatus = namedParameterJdbcTemplate.queryForObject("""
            select case_status
            from pathology_cases
            where id = :caseId
            """, Map.of("caseId", grossing.caseId()), String.class);
        assertThat(caseStatus).isEqualTo("DIAGNOSIS_PENDING");
        assertThat(namedParameterJdbcTemplate.queryForList("""
            select event_type
            from workflow_events
            where case_id = :caseId
            order by event_time asc, created_at asc
            """, Map.of("caseId", grossing.caseId()), String.class)).contains("FROZEN_SLICING_COMPLETED");
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

        JsonNode versions = caseReportVersions(diagnosing.pathologyNo(), USER_M4_REVIEW);
        assertThat(versions.toString()).contains(reportId);
    }

    @Test
    void shouldExposeFrozenRequestedStageInCaseLifecycleTracking() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-LIFE-001", "BC-FR-LIFE-001");

        JsonNode lifecycle = lifecycleTracking(requested.caseId(), USER_M4_TRACKING);
        assertThat(lifecycle.path("caseSummary").path("applicationType").asText()).isEqualTo("FROZEN");
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

        assertThat(page.path("items").get(0).path("id").asText()).isEqualTo(requested.applicationId());
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
        assertThat(preliminary.path("taskStatus").asText()).isEqualTo("IN_PROGRESS");

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
        assertThat(phoneBack.path("sessionStatus").asText()).isEqualTo("REPORTED");

        JsonNode confirmed = responseBody(postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-03",
                  "remarks": "confirm frozen report"
                }
                """), 200);
        assertThat(confirmed.path("sessionStatus").asText()).isEqualTo("CONFIRMED");

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
        assertThat(compare.path("sessionStatus").asText()).isEqualTo("PARAFFIN_REVIEWED");

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
        assertThat(closed.path("sessionStatus").asText()).isEqualTo("CLOSED");

        JsonNode afterClose = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(afterClose.path("remainingTissueStatus").asText()).isEqualTo("DISPOSED");
        assertThat(afterClose.path("timeline").toString()).contains("FROZEN_CLOSED");

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

        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-36","remarks":"save preliminary before completed task retention check","preliminaryResult":"疑似低级别病变"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-37","remarks":"phone back before completed task retention check","preliminaryResult":"疑似低级别病变"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-38","remarks":"confirm before completed task retention check"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(diagnosing.caseId()), USER_M4_REVIEW, """
            {"terminalCode":"T-FR-RPT-39","remarks":"compare before completed task retention check","compareStatus":"SIGNED_OFF","compareSummary":"冰石一致"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/remaining-tissue/complete".formatted(diagnosing.caseId()), USER_M3_GROSSING, """
            {"terminalCode":"T-FR-RPT-40","remarks":"close before completed task retention check","remainingTissueStatus":"DISPOSED"}
            """), 200);

        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        JsonNode tasksByType = indexTasksByType(detail.path("tasks"));
        assertThat(tasksByType.path("RECEIVE").path("status").asText()).isEqualTo("COMPLETED");
        assertThat(tasksByType.path("GROSSING").path("status").asText()).isEqualTo("COMPLETED");
        assertThat(tasksByType.path("SLICING").path("status").asText()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldKeepFrozenSessionAtReportStageUntilReportConfirmed() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-009", "BC-FR-RPT-009");

        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-27","remarks":"save preliminary before stage check","preliminaryResult":"疑似乳头状肿瘤"}
            """), 200);
        JsonNode phoneBack = responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-28","remarks":"phone back before stage check","preliminaryResult":"疑似乳头状肿瘤"}
            """), 200);
        assertThat(phoneBack.path("sessionStatus").asText()).isEqualTo("REPORTED");

        JsonNode afterPhoneBack = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        JsonNode tasksByType = indexTasksByType(afterPhoneBack.path("tasks"));
        assertThat(afterPhoneBack.path("currentTaskType").asText()).isEqualTo("REPORT");
        assertThat(tasksByType.path("REPORT").path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(tasksByType.path("PHONE_BACK").path("status").asText()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldMarkFrozenReportTaskInProgressAfterDiagnosticTaskStarted() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-010", "BC-FR-RPT-010");
        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        JsonNode tasksByType = indexTasksByType(detail.path("tasks"));
        assertThat(tasksByType.path("REPORT").path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(tasksByType.path("REPORT").path("startedAt").asText()).isNotBlank();
    }

    @Test
    void shouldMarkFrozenPhoneBackTaskInProgressAfterPreliminarySaved() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-015", "BC-FR-RPT-015");
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-41","remarks":"save preliminary before phone back task check","preliminaryResult":"疑似轻度病变"}
            """), 200);
        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        JsonNode tasksByType = indexTasksByType(detail.path("tasks"));
        assertThat(tasksByType.path("PHONE_BACK").path("status").asText()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void shouldMarkFrozenCompareTaskInProgressAfterReportConfirmed() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-012", "BC-FR-RPT-012");
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-29","remarks":"save preliminary before compare task check","preliminaryResult":"疑似导管内病变"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-30","remarks":"phone back before compare task check","preliminaryResult":"疑似导管内病变"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-31","remarks":"confirm before compare task check"}
            """), 200);
        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        JsonNode tasksByType = indexTasksByType(detail.path("tasks"));
        assertThat(tasksByType.path("COMPARE").path("status").asText()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void shouldMarkFrozenRemainingTissueTaskInProgressAfterCompareCompleted() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-013", "BC-FR-RPT-013");
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-32","remarks":"save preliminary before remaining tissue task check","preliminaryResult":"疑似乳头状病变"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-33","remarks":"phone back before remaining tissue task check","preliminaryResult":"疑似乳头状病变"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-34","remarks":"confirm before remaining tissue task check"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(diagnosing.caseId()), USER_M4_REVIEW, """
            {"terminalCode":"T-FR-RPT-35","remarks":"compare before remaining tissue task check","compareStatus":"SIGNED_OFF","compareSummary":"冰石一致"}
            """), 200);
        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        JsonNode tasksByType = indexTasksByType(detail.path("tasks"));
        assertThat(tasksByType.path("REMAINING_TISSUE").path("status").asText()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void shouldPopulateRequestedAtForStartedFrozenDiagnosticSession() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-011", "BC-FR-RPT-011");
        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
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
        JsonNode tasksByType = indexTasksByType(detail.path("tasks"));
        assertThat(detail.path("grossingStartedAt").asText()).isNotBlank();
        assertThat(detail.path("slicingStartedAt").asText()).isNotBlank();
        assertThat(tasksByType.path("GROSSING").path("startedAt").asText()).isNotBlank();
        assertThat(tasksByType.path("SLICING").path("startedAt").asText()).isNotBlank();
    }
}
