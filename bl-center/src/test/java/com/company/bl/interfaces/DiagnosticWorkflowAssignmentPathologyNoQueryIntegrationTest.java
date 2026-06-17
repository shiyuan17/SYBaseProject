package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class DiagnosticWorkflowAssignmentPathologyNoQueryIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    @Test
    void shouldListPendingDiagnosticTaskByExactPathologyNo() throws Exception {
        PendingDiagnosticContext context = preparePendingDiagnosticCase("APP-M4-PATH-EXACT-001", "BC-M4-PATH-EXACT-001");

        JsonNode pendingTasks = listPendingDiagnosticTasks(context.pathologyNo(), USER_M4_ASSIGN);

        assertThat(pendingTasks.path("items"))
            .anyMatch(item -> context.diagnosticTaskId().equals(item.path("id").asText()));
    }

    @Test
    void shouldListPendingDiagnosticTaskByNormalizedPathologyNo() throws Exception {
        PendingDiagnosticContext context = preparePendingDiagnosticCase("APP-M4-PATH-NORM-001", "BC-M4-PATH-NORM-001");
        String standardPathologyNo = "BL202606030001";

        namedParameterJdbcTemplate.update("""
            update diagnostic_tasks
            set pathology_no = :pathologyNo
            where id = :taskId
            """, Map.of(
            "pathologyNo", " BL-202606030001 ",
            "taskId", context.diagnosticTaskId()));

        JsonNode pendingTasks = listPendingDiagnosticTasks(standardPathologyNo, USER_M4_ASSIGN);

        assertThat(pendingTasks.path("items"))
            .anyMatch(item ->
                context.diagnosticTaskId().equals(item.path("id").asText())
                    && " BL-202606030001 ".equals(item.path("pathologyNo").asText()));
    }

    @Test
    void shouldNotListDifferentPendingDiagnosticTaskForSimilarPathologyNo() throws Exception {
        PendingDiagnosticContext context = preparePendingDiagnosticCase("APP-M4-PATH-NORM-002", "BC-M4-PATH-NORM-002");

        namedParameterJdbcTemplate.update("""
            update diagnostic_tasks
            set pathology_no = :pathologyNo
            where id = :taskId
            """, Map.of(
            "pathologyNo", " BL-202606030001 ",
            "taskId", context.diagnosticTaskId()));

        JsonNode pendingTasks = listPendingDiagnosticTasks("BL202606030002", USER_M4_ASSIGN);

        assertThat(pendingTasks.path("items"))
            .noneMatch(item -> context.diagnosticTaskId().equals(item.path("id").asText()));
    }

    @Test
    void shouldListPendingDiagnosticTaskByUpdatedPathologyNoAfterCaseNumberChanges() throws Exception {
        PendingDiagnosticContext context = preparePendingDiagnosticCase("APP-M4-PATH-UPDATE-001", "BC-M4-PATH-UPDATE-001");
        String legacyPathologyNo = "BL-LEGACY-" + context.caseId();

        namedParameterJdbcTemplate.update("""
            update diagnostic_tasks
            set pathology_no = :legacyPathologyNo
            where id = :taskId
            """, Map.of(
            "legacyPathologyNo", "BL202606170001",
            "taskId", context.diagnosticTaskId()));

        namedParameterJdbcTemplate.update("""
            update pathology_cases
            set pathology_no = :legacyPathologyNo
            where id = :caseId
            """, Map.of(
            "legacyPathologyNo", legacyPathologyNo,
            "caseId", context.caseId()));

        namedParameterJdbcTemplate.update("""
            update technical_specimen_registrations
            set registration_status = 'PENDING',
                registered_by_user_id = null,
                registered_by_name = null,
                registered_at = null,
                remarks = null
            where case_id = :caseId
            """, Map.of("caseId", context.caseId()));

        String updatedPathologyNo = "HZ2609999";

        completeTechnicalSpecimenRegistration(
            context.caseId(),
            "update pathology number",
            "CONSULTATION",
            updatedPathologyNo
        );

        JsonNode updatedPendingTasks = listPendingDiagnosticTasks(updatedPathologyNo, USER_M4_ASSIGN);

        assertThat(updatedPendingTasks.path("items"))
            .anyMatch(item ->
                context.diagnosticTaskId().equals(item.path("id").asText())
                    && updatedPathologyNo.equals(item.path("pathologyNo").asText()));

        JsonNode legacyPendingTasks = listPendingDiagnosticTasks(legacyPathologyNo, USER_M4_ASSIGN);

        assertThat(legacyPendingTasks.path("items"))
            .noneMatch(item -> context.diagnosticTaskId().equals(item.path("id").asText()));

        String persistedTaskPathologyNo = namedParameterJdbcTemplate.queryForObject("""
            select pathology_no
            from diagnostic_tasks
            where id = :taskId
            """, Map.of("taskId", context.diagnosticTaskId()), String.class);
        assertThat(persistedTaskPathologyNo).isEqualTo(updatedPathologyNo);
    }
}
