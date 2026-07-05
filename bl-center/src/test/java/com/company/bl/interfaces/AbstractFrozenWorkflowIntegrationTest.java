package com.company.bl.interfaces;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class AbstractFrozenWorkflowIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    protected FrozenCaseContext prepareFrozenRequestedCase(String applicationNo, String barcode) throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration(applicationNo, barcode, "DEPT-OR", "OR", "FROZEN");
        return new FrozenCaseContext(context.applicationId(), context.caseId(), context.pathologyNo(), context.specimenId(), null, null, null);
    }

    protected void clearFrozenSpecimenFlags(String caseId) {
        namedParameterJdbcTemplate.update("""
            update specimens
            set frozen_flag = 0
            where case_id = :caseId
            """, Map.of("caseId", caseId));
    }

    protected String createReviewUser(String suffix) {
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

    protected FrozenCaseContext prepareFrozenReceivedCase(String applicationNo, String barcode) throws Exception {
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

    protected FrozenCaseContext prepareFrozenGrossingCompletedCase(String applicationNo, String barcode) throws Exception {
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

    protected FrozenCaseContext prepareFrozenDiagnosingCase(String applicationNo, String barcode) throws Exception {
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

    protected FrozenCaseContext prepareFrozenStartedDiagnosticCase(String applicationNo, String barcode) throws Exception {
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

    protected FrozenCaseContext prepareFrozenConfirmedCase(String applicationNo, String barcode) throws Exception {
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

    protected void updateFrozenRegistrationCheckItem(String caseId, String checkItem) {
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

    protected JsonNode indexSessionsByCaseId(JsonNode sessions) throws Exception {
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

    protected JsonNode indexTasksByType(JsonNode tasks) throws Exception {
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

    protected JsonNode indexRemindersByCaseId(JsonNode reminders) throws Exception {
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

    protected record FrozenCaseContext(
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
