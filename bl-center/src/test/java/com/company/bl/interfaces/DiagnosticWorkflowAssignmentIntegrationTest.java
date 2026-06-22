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
class DiagnosticWorkflowAssignmentIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    @Test
    void shouldOnlyListAssignedTasksForDiagnosisDoctor() throws Exception {
        PendingDiagnosticContext context = preparePendingDiagnosticCase("APP-M4-ASSIGN-LIST-001", "BC-M4-ASSIGN-LIST-001");
        String otherDiagnosisUserId = createDiagnosisUser("ALT");
        String diagnosticTaskId = context.diagnosticTaskId();
        postJson("/api/v1/diagnostic-tasks/%s/assign".formatted(diagnosticTaskId), USER_M4_ASSIGN, """
            {
              "diagnosisDoctorUserId":"%s",
              "diagnosisDoctorName":"Alt Diagnosis",
              "primaryDoctorUserId":"%s",
              "primaryDoctorName":"Alt Diagnosis",
              "reviewerUserId":"USER_M4_REVIEW",
              "reviewerName":"M4 Review"}
            """.formatted(otherDiagnosisUserId, otherDiagnosisUserId))
            .andExpect(status().isOk());

        JsonNode assignView = listPendingDiagnosticTasks(context.pathologyNo(), USER_M4_ASSIGN);
        assertThat(assignView.path("total").asInt()).isEqualTo(1);

        JsonNode currentDiagnosisView = listPendingDiagnosticTasks(context.pathologyNo(), USER_M4_DIAGNOSIS);
        assertThat(currentDiagnosisView.path("total").asInt()).isEqualTo(0);

        JsonNode currentDiagnosisAssignmentView = listAssignableDiagnosticTasks(context.pathologyNo(), USER_M4_DIAGNOSIS);
        assertThat(currentDiagnosisAssignmentView.path("total").asInt()).isEqualTo(1);
        assertThat(currentDiagnosisAssignmentView.path("items").get(0).path("id").asText()).isEqualTo(diagnosticTaskId);

        JsonNode assignmentWideView = listAssignableDiagnosticTasks(context.pathologyNo(), USER_M4_ASSIGN);
        assertThat(assignmentWideView.path("total").asInt()).isEqualTo(1);
        assertThat(assignmentWideView.path("items").get(0).path("id").asText()).isEqualTo(diagnosticTaskId);

        mockMvc.perform(authorized(get("/api/v1/diagnostic-tasks/assignment"), USER_M4_NO_PERMISSION)
                .param("page", "1")
                .param("size", "20")
                .param("pathologyNo", context.pathologyNo()))
            .andExpect(status().isForbidden());

        JsonNode otherDiagnosisView = listPendingDiagnosticTasks(context.pathologyNo(), otherDiagnosisUserId);
        assertThat(otherDiagnosisView.path("total").asInt()).isEqualTo(1);
        assertThat(otherDiagnosisView.path("items").get(0).path("id").asText()).isEqualTo(diagnosticTaskId);
    }

    @Test
    void shouldAllowReassignAssignedDiagnosticTask() throws Exception {
        PendingDiagnosticContext context = preparePendingDiagnosticCase("APP-M4-REASSIGN-001", "BC-M4-REASSIGN-001");
        String reassignedDiagnosisUserId = createDiagnosisUser("REASSIGN");

        postJson("/api/v1/diagnostic-tasks/%s/assign".formatted(context.diagnosticTaskId()), USER_M4_ASSIGN, """
            {
              "diagnosisDoctorUserId":"USER_M4_DIAGNOSIS",
              "diagnosisDoctorName":"M4 Diagnosis",
              "primaryDoctorUserId":"USER_M4_DIAGNOSIS",
              "primaryDoctorName":"M4 Diagnosis",
              "reviewerUserId":"USER_M4_REVIEW",
              "reviewerName":"M4 Review",
              
              "terminalCode":"M4-A-01"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.taskStatus").value("ASSIGNED"));

        postJson("/api/v1/diagnostic-tasks/%s/assign".formatted(context.diagnosticTaskId()), USER_M4_ASSIGN, """
            {
              "diagnosisDoctorUserId":"%s",
              "diagnosisDoctorName":"M4 Diagnosis REASSIGN",
              "primaryDoctorUserId":"%s",
              "primaryDoctorName":"M4 Diagnosis REASSIGN",
              "reviewerUserId":"USER_M4_REVIEW",
              "reviewerName":"M4 Review",
              
              "terminalCode":"M4-A-02",
              "remarks":"reassign task"
            }
            """.formatted(reassignedDiagnosisUserId, reassignedDiagnosisUserId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.taskStatus").value("ASSIGNED"));

        JsonNode previousDiagnosisView = listPendingDiagnosticTasks(context.pathologyNo(), USER_M4_DIAGNOSIS);
        assertThat(previousDiagnosisView.path("total").asInt()).isEqualTo(0);

        JsonNode reassignedDiagnosisView = listPendingDiagnosticTasks(context.pathologyNo(), reassignedDiagnosisUserId);
        assertThat(reassignedDiagnosisView.path("total").asInt()).isEqualTo(1);
        assertThat(reassignedDiagnosisView.path("items").get(0).path("id").asText()).isEqualTo(context.diagnosticTaskId());

        Long reassignEventCount = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from workflow_events
            where case_id = :caseId
              and node_code = 'DIAGNOSIS_ASSIGN'
              and event_type = 'REASSIGN'
            """, java.util.Map.of("caseId", context.caseId()), Long.class);
        assertThat(reassignEventCount).isEqualTo(1L);

        assertThat(countNotifications(USER_M4_DIAGNOSIS, "DIAG_TASK_ASSIGN", context.diagnosticTaskId())).isEqualTo(1L);
        assertThat(countNotifications(reassignedDiagnosisUserId, "DIAG_TASK_ASSIGN", context.diagnosticTaskId())).isEqualTo(1L);
    }

    @Test
    void shouldFilterAssignmentTasksByCreatedDateRange() throws Exception {
        PendingDiagnosticContext olderContext = preparePendingDiagnosticCase("APP-M4-DATE-OLD-001", "BC-M4-DATE-OLD-001");
        PendingDiagnosticContext newerContext = preparePendingDiagnosticCase("APP-M4-DATE-NEW-001", "BC-M4-DATE-NEW-001");

        namedParameterJdbcTemplate.update("""
            update diagnostic_tasks
            set created_at = timestamp '2024-06-20 09:00:00',
                updated_at = timestamp '2024-06-20 09:00:00'
            where id = :taskId
            """, java.util.Map.of("taskId", olderContext.diagnosticTaskId()));
        namedParameterJdbcTemplate.update("""
            update diagnostic_tasks
            set created_at = timestamp '2024-06-21 11:00:00',
                updated_at = timestamp '2024-06-21 11:00:00'
            where id = :taskId
            """, java.util.Map.of("taskId", newerContext.diagnosticTaskId()));

        JsonNode onlyOlder = listAssignableDiagnosticTasks(olderContext.pathologyNo(), "2024-06-20", "2024-06-20", USER_M4_ASSIGN);
        assertThat(onlyOlder.path("total").asInt()).isEqualTo(1);
        assertThat(onlyOlder.path("items").get(0).path("id").asText()).isEqualTo(olderContext.diagnosticTaskId());

        JsonNode onlyNewer = listAssignableDiagnosticTasks(newerContext.pathologyNo(), "2024-06-21", "2024-06-21", USER_M4_ASSIGN);
        assertThat(onlyNewer.path("total").asInt()).isEqualTo(1);
        assertThat(onlyNewer.path("items").get(0).path("id").asText()).isEqualTo(newerContext.diagnosticTaskId());

        JsonNode fromOnly = listAssignableDiagnosticTasks(newerContext.pathologyNo(), "2024-06-21", null, USER_M4_ASSIGN);
        assertThat(fromOnly.path("total").asInt()).isEqualTo(1);
        assertThat(fromOnly.path("items").get(0).path("id").asText()).isEqualTo(newerContext.diagnosticTaskId());

        JsonNode toOnly = listAssignableDiagnosticTasks(olderContext.pathologyNo(), null, "2024-06-20", USER_M4_ASSIGN);
        assertThat(toOnly.path("total").asInt()).isEqualTo(1);
        assertThat(toOnly.path("items").get(0).path("id").asText()).isEqualTo(olderContext.diagnosticTaskId());

        mockMvc.perform(authorized(get("/api/v1/diagnostic-tasks/assignment"), USER_M4_ASSIGN)
                .param("page", "1")
                .param("size", "20")
                .param("dateFrom", "2024-06-20")
                .param("dateTo", "2024-06-21"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void shouldRejectReassignAfterDiagnosticTaskAccepted() throws Exception {
        PendingDiagnosticContext context = preparePendingDiagnosticCase("APP-M4-REASSIGN-ACCEPTED-001", "BC-M4-REASSIGN-ACCEPTED-001");
        String reassignedDiagnosisUserId = createDiagnosisUser("ACCEPTED");

        postJson("/api/v1/diagnostic-tasks/%s/assign".formatted(context.diagnosticTaskId()), USER_M4_ASSIGN, """
            {
              "diagnosisDoctorUserId":"USER_M4_DIAGNOSIS",
              "diagnosisDoctorName":"M4 Diagnosis",
              "primaryDoctorUserId":"USER_M4_DIAGNOSIS",
              "primaryDoctorName":"M4 Diagnosis",
              "reviewerUserId":"USER_M4_REVIEW",
              "reviewerName":"M4 Review",
              
              "terminalCode":"M4-A-01"
            }
            """).andExpect(status().isOk());

        postJson("/api/v1/diagnostic-tasks/%s/accept".formatted(context.diagnosticTaskId()), USER_M4_DIAGNOSIS, """
            {
              
              "terminalCode":"M4-A-02"
            }
            """).andExpect(status().isOk());

        postJson("/api/v1/diagnostic-tasks/%s/assign".formatted(context.diagnosticTaskId()), USER_M4_ASSIGN, """
            {
              "diagnosisDoctorUserId":"%s",
              "diagnosisDoctorName":"M4 Diagnosis ACCEPTED",
              "primaryDoctorUserId":"%s",
              "primaryDoctorName":"M4 Diagnosis ACCEPTED",
              "reviewerUserId":"USER_M4_REVIEW",
              "reviewerName":"M4 Review",
              
              "terminalCode":"M4-A-03"
            }
            """.formatted(reassignedDiagnosisUserId, reassignedDiagnosisUserId))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));
    }

    @Test
    void shouldRejectReassignAfterDiagnosticTaskStarted() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-REASSIGN-STARTED-001", "BC-M4-REASSIGN-STARTED-001");
        String reassignedDiagnosisUserId = createDiagnosisUser("STARTED");

        postJson("/api/v1/diagnostic-tasks/%s/assign".formatted(context.diagnosticTaskId()), USER_M4_ASSIGN, """
            {
              "diagnosisDoctorUserId":"%s",
              "diagnosisDoctorName":"M4 Diagnosis STARTED",
              "primaryDoctorUserId":"%s",
              "primaryDoctorName":"M4 Diagnosis STARTED",
              "reviewerUserId":"USER_M4_REVIEW",
              "reviewerName":"M4 Review",
              
              "terminalCode":"M4-A-04"
            }
            """.formatted(reassignedDiagnosisUserId, reassignedDiagnosisUserId))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));
    }

    @Test
    void shouldCreateDeduplicatedNotificationsForDiagnosticAssignment() throws Exception {
        PendingDiagnosticContext context = preparePendingDiagnosticCase("APP-M4-NOTIFY-001", "BC-M4-NOTIFY-001");
        String sameDoctorId = createDiagnosisUser("NOTIFY");

        postJson("/api/v1/diagnostic-tasks/%s/assign".formatted(context.diagnosticTaskId()), USER_M4_ASSIGN, """
            {
              "diagnosisDoctorUserId":"%s",
              "diagnosisDoctorName":"Notify Diagnosis",
              "primaryDoctorUserId":"%s",
              "primaryDoctorName":"Notify Diagnosis",
              "reviewerUserId":"USER_M4_REVIEW",
              "reviewerName":"M4 Review",
              
              "terminalCode":"M4-N-01"
            }
            """.formatted(sameDoctorId, sameDoctorId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.taskStatus").value("ASSIGNED"));

        assertThat(countNotifications(sameDoctorId, "DIAG_TASK_ASSIGN", context.diagnosticTaskId())).isEqualTo(1L);
        assertThat(countNotifications(USER_M4_REVIEW, "DIAG_TASK_ASSIGN", context.diagnosticTaskId())).isEqualTo(1L);
    }

    @Test
    void shouldKeepReviewerEmptyWhenPrimaryAssignmentOmitsReviewerFields() throws Exception {
        PendingDiagnosticContext context = preparePendingDiagnosticCase("APP-M4-PRIMARY-ONLY-001", "BC-M4-PRIMARY-ONLY-001");

        postJson("/api/v1/diagnostic-tasks/%s/assign".formatted(context.diagnosticTaskId()), USER_M4_ASSIGN, """
            {
              "diagnosisDoctorUserId":"USER_M4_DIAGNOSIS",
              "diagnosisDoctorName":"M4 Diagnosis",
              "primaryDoctorUserId":"USER_M4_DIAGNOSIS",
              "primaryDoctorName":"M4 Diagnosis"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.taskStatus").value("ASSIGNED"));

        JsonNode assignmentView = listAssignableDiagnosticTasks(context.pathologyNo(), USER_M4_ASSIGN);
        JsonNode task = assignmentView.path("items").get(0);
        assertThat(task.path("diagnosisDoctorUserId").asText()).isEqualTo(USER_M4_DIAGNOSIS);
        assertThat(task.path("primaryDoctorUserId").asText()).isEqualTo(USER_M4_DIAGNOSIS);
        assertThat(task.path("reviewerUserId").isNull()).isTrue();
        assertThat(task.path("reviewerName").isNull()).isTrue();
        assertThat(countNotifications(USER_M4_DIAGNOSIS, "DIAG_TASK_ASSIGN", context.diagnosticTaskId())).isEqualTo(1L);
        assertThat(countNotifications(USER_M4_REVIEW, "DIAG_TASK_ASSIGN", context.diagnosticTaskId())).isEqualTo(0L);
    }

    @Test
    void shouldKeepPrimaryAssignmentWhenReviewerAssignmentOnlyUpdatesReviewerFields() throws Exception {
        PendingDiagnosticContext context = preparePendingDiagnosticCase("APP-M4-REVIEW-ONLY-001", "BC-M4-REVIEW-ONLY-001");

        postJson("/api/v1/diagnostic-tasks/%s/assign".formatted(context.diagnosticTaskId()), USER_M4_ASSIGN, """
            {
              "diagnosisDoctorUserId":"USER_M4_DIAGNOSIS",
              "diagnosisDoctorName":"M4 Diagnosis",
              "primaryDoctorUserId":"USER_M4_DIAGNOSIS",
              "primaryDoctorName":"M4 Diagnosis"
            }
            """)
            .andExpect(status().isOk());

        postJson("/api/v1/diagnostic-tasks/%s/assign".formatted(context.diagnosticTaskId()), USER_M4_ASSIGN, """
            {
              "reviewerUserId":"USER_M4_REVIEW",
              "reviewerName":"M4 Review"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.taskStatus").value("ASSIGNED"));

        JsonNode assignmentView = listAssignableDiagnosticTasks(context.pathologyNo(), USER_M4_ASSIGN);
        JsonNode task = assignmentView.path("items").get(0);
        assertThat(task.path("diagnosisDoctorUserId").asText()).isEqualTo(USER_M4_DIAGNOSIS);
        assertThat(task.path("primaryDoctorUserId").asText()).isEqualTo(USER_M4_DIAGNOSIS);
        assertThat(task.path("reviewerUserId").asText()).isEqualTo(USER_M4_REVIEW);
        assertThat(task.path("reviewerName").asText()).isEqualTo("M4 Review");
    }

    @Test
    void shouldTreatBlankReviewerFieldsAsKeepCurrentReviewer() throws Exception {
        PendingDiagnosticContext context = preparePendingDiagnosticCase("APP-M4-KEEP-REVIEW-001", "BC-M4-KEEP-REVIEW-001");

        postJson("/api/v1/diagnostic-tasks/%s/assign".formatted(context.diagnosticTaskId()), USER_M4_ASSIGN, """
            {
              "diagnosisDoctorUserId":"USER_M4_DIAGNOSIS",
              "diagnosisDoctorName":"M4 Diagnosis",
              "primaryDoctorUserId":"USER_M4_DIAGNOSIS",
              "primaryDoctorName":"M4 Diagnosis",
              "reviewerUserId":"USER_M4_REVIEW",
              "reviewerName":"M4 Review"
            }
            """)
            .andExpect(status().isOk());

        postJson("/api/v1/diagnostic-tasks/%s/assign".formatted(context.diagnosticTaskId()), USER_M4_ASSIGN, """
            {
              "diagnosisDoctorUserId":"USER_M4_DIAGNOSIS",
              "diagnosisDoctorName":"M4 Diagnosis",
              "primaryDoctorUserId":"USER_M4_DIAGNOSIS",
              "primaryDoctorName":"M4 Diagnosis",
              "reviewerUserId":"   ",
              "reviewerName":"   "
            }
            """)
            .andExpect(status().isOk());

        JsonNode assignmentView = listAssignableDiagnosticTasks(context.pathologyNo(), USER_M4_ASSIGN);
        JsonNode task = assignmentView.path("items").get(0);
        assertThat(task.path("reviewerUserId").asText()).isEqualTo(USER_M4_REVIEW);
        assertThat(task.path("reviewerName").asText()).isEqualTo("M4 Review");
    }
}
