package com.company.bl.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = com.company.bl.BlCenterApplication.class)
class DiagnosticTaskPendingSummaryIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    @Test
    void shouldReturnPendingDiagnosticTaskCaseSummaryFields() throws Exception {
        PendingDiagnosticContext context = preparePendingDiagnosticCase("APP-M4-SUMMARY-001", "BC-M4-SUMMARY-001");
        updateRegistrationCheckItem(context.caseId(), "术中冰冻+常规病理");
        updateRegistrationIdNo(context.caseId(), "08305");

        JsonNode task = listPendingDiagnosticTasks(context.pathologyNo(), USER_M4_ASSIGN)
            .path("items")
            .get(0);

        assertThat(task.path("patientId").asText()).isNotBlank();
        assertThat(task.path("patientIdDisplay").asText()).isEqualTo("08305");
        assertThat(task.path("applicationType").asText()).isEqualTo("ROUTINE");
        assertThat(task.path("checkItem").asText()).isEqualTo("术中冰冻+常规病理");
        assertThat(task.path("submittingDepartmentName").asText()).isEqualTo("OR");
        assertThat(task.path("specimenName").asText()).isEqualTo("Thyroid Tissue");
        assertThat(task.path("blockCount").asInt()).isEqualTo(1);
    }

    @Test
    void shouldAggregateDistinctSpecimenNamesAndKeepBlockCountWhenCaseHasMultipleSpecimens() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask("APP-M4-SUMMARY-002", "BC-M4-SUMMARY-002");
        addSecondSpecimenForCase(context.applicationId(), context.caseId(), "SP-SECOND", "胃窦组织");
        addSecondSpecimenForCase(context.applicationId(), context.caseId(), "SP-THIRD", "胃体组织");

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {"taskId":"%s","terminalCode":"M4-G-01"}
            """.formatted(context.grossingTaskId()));
        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId":"%s",
              "caseId":"%s",
              "terminalCode":"M4-G-02",
              "specimens":[
                {"specimenId":"%s","specimenType":"ROUTINE","grossDescription":"gd-1","blocks":[{"blockSite":"A","blockDescription":"B"}]},
                {"specimenId":"SPEC-SP-SECOND","specimenType":"ROUTINE","grossDescription":"gd-2","blocks":[{"blockSite":"C","blockDescription":"D"}]},
                {"specimenId":"SPEC-SP-THIRD","specimenType":"ROUTINE","grossDescription":"gd-3","blocks":[{"blockSite":"E","blockDescription":"F"}]}
              ]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId()))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        String firstSamplingBlockId = listPendingTasks("DEHYDRATION", context.pathologyNo(), USER_M3_DEHYDRATION)
            .path("items").get(0).path("objectId").asText();
        String batchId = responseBody(postJson("/api/v1/dehydration-batches", USER_M3_DEHYDRATION, """
            {
              "caseId":"%s",
              "basketNo":"B1",
              "deviceNo":"D1",
              "terminalCode":"M4-D-01",
              "samplingBlockIds":["%s"]
            }
            """.formatted(context.caseId(), firstSamplingBlockId)), 201).path("batchId").asText();
        postJson("/api/v1/dehydration-batches/%s/start".formatted(batchId), USER_M3_DEHYDRATION, """
            {"terminalCode":"M4-D-02"}
            """).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        postJson("/api/v1/dehydration-batches/%s/complete".formatted(batchId), USER_M3_DEHYDRATION, """
            {"terminalCode":"M4-D-03"}
            """).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        String embeddingTaskId = listPendingTasks("EMBEDDING", context.pathologyNo(), USER_M3_EMBEDDING)
            .path("items").get(0).path("id").asText();
        postJson("/api/v1/embeddings/start", USER_M3_EMBEDDING, """
            {"taskId":"%s","terminalCode":"M4-E-01"}
            """.formatted(embeddingTaskId)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        String embeddingBoxId = responseBody(postJson("/api/v1/embeddings/complete", USER_M3_EMBEDDING, """
            {
              "taskId":"%s",
              "samplingBlockId":"%s",
              "blockCount":1,
              "sliceNotice":"n",
              "terminalCode":"M4-E-02"
            }
            """.formatted(embeddingTaskId, firstSamplingBlockId)), 200).path("embeddingBoxId").asText();

        String slicingTaskId = listPendingTasks("SLICING", context.pathologyNo(), USER_M3_SLICING)
            .path("items").get(0).path("id").asText();
        postJson("/api/v1/slicings/start", USER_M3_SLICING, """
            {"taskId":"%s","terminalCode":"M4-S-01"}
            """.formatted(slicingTaskId)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        postJson("/api/v1/slicings/slide-print", USER_M3_SLICING, """
            {
              "taskId":"%s",
              "embeddingBoxId":"%s",
              "sourceSlideCount":1,
              "terminalCode":"M4-S-PRINT"
            }
            """.formatted(slicingTaskId, embeddingBoxId)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        String slideId = responseBody(postJson("/api/v1/slicings/complete", USER_M3_SLICING, """
            {
              "taskId":"%s",
              "embeddingBoxId":"%s",
              "slideCount":1,
              "terminalCode":"M4-S-02"
            }
            """.formatted(slicingTaskId, embeddingBoxId)), 200).path("slideIds").get(0).asText();

        String stainingTaskId = listPendingTasks("STAINING", context.pathologyNo(), USER_M3_STAINING)
            .path("items").get(0).path("id").asText();
        postJson("/api/v1/slide-stainings/start", USER_M3_STAINING, """
            {"taskId":"%s","terminalCode":"M4-T-01"}
            """.formatted(stainingTaskId)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        postJson("/api/v1/slide-stainings/complete", USER_M3_STAINING, """
            {"taskId":"%s","slideId":"%s","stainingType":"HE","terminalCode":"M4-T-02"}
            """.formatted(stainingTaskId, slideId)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        JsonNode task = listPendingDiagnosticTasks(context.pathologyNo(), USER_M4_ASSIGN)
            .path("items")
            .get(0);

        assertThat(task.path("specimenName").asText()).isEqualTo("Thyroid Tissue、胃体组织、胃窦组织");
        assertThat(task.path("blockCount").asInt()).isEqualTo(3);
    }

    @Test
    void shouldKeepSpecimenSummaryWhenBlocksAreMissingAndAllowEmptyCheckItem() throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetPendingRegistration("APP-M4-SUMMARY-003", "BC-M4-SUMMARY-003");
        JsonNode registration = completeTechnicalSpecimenRegistration(context.caseId(), "no block task");
        String pathologyNo = registration.path("pathologyNo").asText();
        insertPendingDiagnosticTask(context.caseId(), pathologyNo);
        clearRegistrationCheckItem(context.caseId());

        JsonNode task = listPendingDiagnosticTasks(pathologyNo, USER_M4_ASSIGN)
            .path("items")
            .get(0);

        assertThat(task.path("specimenName").asText()).isEqualTo("Thyroid Tissue");
        assertThat(task.path("blockCount").asInt()).isEqualTo(0);
        assertThat(task.path("checkItem").isNull() || task.path("checkItem").asText().isBlank()).isTrue();
    }

    private void updateRegistrationCheckItem(String caseId, String checkItem) {
        String applicationId = namedParameterJdbcTemplate.queryForObject("""
            select application_id from pathology_cases where id = :caseId
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
                """, new MapSqlParameterSource()
                .addValue("applicationId", applicationId)
                .addValue("checkItem", checkItem)
                .addValue("updatedAt", now));
            return;
        }
        namedParameterJdbcTemplate.update("""
            insert into application_registration_workbench
                (application_id, check_item, created_at, updated_at)
            values
                (:applicationId, :checkItem, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("applicationId", applicationId)
            .addValue("checkItem", checkItem)
            .addValue("createdAt", now)
            .addValue("updatedAt", now));
    }

    private void clearRegistrationCheckItem(String caseId) {
        updateRegistrationCheckItem(caseId, null);
    }

    private void updateRegistrationIdNo(String caseId, String idNo) {
        String applicationId = namedParameterJdbcTemplate.queryForObject("""
            select application_id from pathology_cases where id = :caseId
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
                set id_no = :idNo,
                    updated_at = :updatedAt
                where application_id = :applicationId
                """, new MapSqlParameterSource()
                .addValue("applicationId", applicationId)
                .addValue("idNo", idNo)
                .addValue("updatedAt", now));
            return;
        }
        namedParameterJdbcTemplate.update("""
            insert into application_registration_workbench
                (application_id, id_no, created_at, updated_at)
            values
                (:applicationId, :idNo, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("applicationId", applicationId)
            .addValue("idNo", idNo)
            .addValue("createdAt", now)
            .addValue("updatedAt", now));
    }

    private void addSecondSpecimenForCase(String applicationId, String caseId, String specimenNo, String specimenName) {
        namedParameterJdbcTemplate.update("""
            insert into specimens
                (id, application_id, case_id, specimen_no, barcode, specimen_type, specimen_name_standardized,
                 specimen_status, fixation_status, registered_at, created_at, updated_at)
            values
                (:id, :applicationId, :caseId, :specimenNo, null, 'ROUTINE', :specimenName,
                 'REGISTERED', 'PENDING', :now, :now, :now)
            """, Map.of(
            "id", "SPEC-" + specimenNo,
            "applicationId", applicationId,
            "caseId", caseId,
            "specimenNo", specimenNo,
            "specimenName", specimenName,
            "now", LocalDateTime.now()));
    }

    private void insertPendingDiagnosticTask(String caseId, String pathologyNo) {
        namedParameterJdbcTemplate.update("""
            insert into diagnostic_tasks
                (id, case_id, specimen_id, pathology_no, task_type, status, priority, remarks, created_at, updated_at)
            values
                (:id, :caseId, null, :pathologyNo, 'PRIMARY', 'PENDING', 'NORMAL', 'manual pending task', :now, :now)
            """, Map.of(
            "id", "DT-M4-SUMMARY-" + System.nanoTime(),
            "caseId", caseId,
            "pathologyNo", pathologyNo,
            "now", LocalDateTime.now()));
    }
}
