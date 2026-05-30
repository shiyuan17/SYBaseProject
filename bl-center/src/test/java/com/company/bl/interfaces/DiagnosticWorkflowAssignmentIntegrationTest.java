package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class DiagnosticWorkflowAssignmentIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    @Test
    void shouldOnlyListAssignedTasksForDiagnosisDoctor() throws Exception {
        String otherDiagnosisUserId = createDiagnosisUser("ALT");
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M4-LIST-001", "BC-M4-LIST-001");

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {"taskId":"%s"}
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk());
        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId":"%s",
              "caseId":"%s",
              
              "specimens":[{"specimenId":"%s","specimenType":"ROUTINE","grossDescription":"gd","blocks":[{"blockSite":"A","blockDescription":"B"}]}]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId()))
            .andExpect(status().isOk());

        String samplingBlockId = listPendingTasks("DEHYDRATION", context.pathologyNo(), USER_M3_DEHYDRATION)
            .path("items").get(0).path("objectId").asText();
        String batchId = responseBody(postJson("/api/v1/dehydration-batches", USER_M3_DEHYDRATION, """
            {"caseId":"%s","basketNo":"B1","deviceNo":"D1","samplingBlockIds":["%s"]}
            """.formatted(context.caseId(), samplingBlockId)), 201).path("batchId").asText();
        postJson("/api/v1/dehydration-batches/%s/start".formatted(batchId), USER_M3_DEHYDRATION, """
            {}
            """).andExpect(status().isOk());
        postJson("/api/v1/dehydration-batches/%s/complete".formatted(batchId), USER_M3_DEHYDRATION, """
            {}
            """).andExpect(status().isOk());

        String embeddingTaskId = listPendingTasks("EMBEDDING", context.pathologyNo(), USER_M3_EMBEDDING)
            .path("items").get(0).path("id").asText();
        postJson("/api/v1/embeddings/start", USER_M3_EMBEDDING, """
            {"taskId":"%s"}
            """.formatted(embeddingTaskId)).andExpect(status().isOk());
        String embeddingBoxId = responseBody(postJson("/api/v1/embeddings/complete", USER_M3_EMBEDDING, """
            {"taskId":"%s","samplingBlockId":"%s","blockCount":1,"sliceNotice":"n"}
            """.formatted(embeddingTaskId, samplingBlockId)), 200).path("embeddingBoxId").asText();

        String slicingTaskId = listPendingTasks("SLICING", context.pathologyNo(), USER_M3_SLICING)
            .path("items").get(0).path("id").asText();
        postJson("/api/v1/slicings/start", USER_M3_SLICING, """
            {"taskId":"%s"}
            """.formatted(slicingTaskId)).andExpect(status().isOk());
        String slideId = responseBody(postJson("/api/v1/slicings/complete", USER_M3_SLICING, """
            {"taskId":"%s","embeddingBoxId":"%s","slideCount":1}
            """.formatted(slicingTaskId, embeddingBoxId)), 200).path("slideIds").get(0).asText();

        String stainingTaskId = listPendingTasks("STAINING", context.pathologyNo(), USER_M3_STAINING)
            .path("items").get(0).path("id").asText();
        postJson("/api/v1/slide-stainings/start", USER_M3_STAINING, """
            {"taskId":"%s"}
            """.formatted(stainingTaskId)).andExpect(status().isOk());
        postJson("/api/v1/slide-stainings/complete", USER_M3_STAINING, """
            {"taskId":"%s","slideId":"%s","stainingType":"HE"}
            """.formatted(stainingTaskId, slideId)).andExpect(status().isOk());

        String diagnosticTaskId = listPendingDiagnosticTasks(context.pathologyNo(), USER_M4_ASSIGN)
            .path("items").get(0).path("id").asText();
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
