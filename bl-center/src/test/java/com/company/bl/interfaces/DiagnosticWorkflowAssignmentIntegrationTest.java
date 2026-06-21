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
        PendingDiagnosticContext context = preparePendingDiagnosticCase("APP-M4-LIST-001", "BC-M4-LIST-001");
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
}
