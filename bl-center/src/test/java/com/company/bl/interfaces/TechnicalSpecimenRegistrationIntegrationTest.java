package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(
    classes = BlCenterApplication.class,
    properties = "bl.file-storage.grossing-media.root-dir=${java.io.tmpdir}/sybase/bl-center/technical-registration-media-test"
)
class TechnicalSpecimenRegistrationIntegrationTest extends AbstractTechnicalWorkflowIntegrationTest {

    @Test
    void shouldCreatePendingRegistrationAfterReceiptWithoutImmediateGrossingTask() throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-001", "BC-M3-REG-001");

        JsonNode pendingRegistrations =
            listPendingTechnicalSpecimenRegistrations(context.pathologyNo(), USER_RECEIVE);

        assertThat(pendingRegistrations.path("total").asInt()).isEqualTo(1);
        assertThat(pendingRegistrations.path("items").get(0).path("caseId").asText())
            .isEqualTo(context.caseId());
        assertThat(pendingRegistrations.path("items").get(0).path("pathologyNo").asText())
            .isEqualTo(context.pathologyNo());

        JsonNode pendingGrossingTasks =
            listPendingTasks("GROSSING", context.pathologyNo(), USER_M3_GROSSING);
        assertThat(pendingGrossingTasks.path("total").asInt()).isZero();
    }

    @Test
    void shouldCompleteRegistrationIdempotentlyAndCreateSingleGrossingTask() throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-002", "BC-M3-REG-002");

        mockMvc.perform(authorized(
                get("/api/v1/technical-specimen-registrations/{caseId}", context.caseId()),
                USER_RECEIVE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.caseId").value(context.caseId()))
            .andExpect(jsonPath("$.data.pathologyNo").value(context.pathologyNo()))
            .andExpect(jsonPath("$.data.materials[0].specimenName").value("Thyroid Tissue"))
            .andExpect(jsonPath("$.data.checkItems").isArray())
            .andExpect(jsonPath("$.data.clinicalDiagnosis").value("Papillary thyroid carcinoma"));

        JsonNode firstComplete =
            completeTechnicalSpecimenRegistration(context.caseId(), "first completion");
        JsonNode secondComplete =
            completeTechnicalSpecimenRegistration(context.caseId(), "repeat completion");

        assertThat(firstComplete.path("registrationStatus").asText()).isEqualTo("COMPLETED");
        assertThat(firstComplete.path("grossingTaskCreated").asBoolean()).isTrue();
        assertThat(secondComplete.path("registrationStatus").asText()).isEqualTo("COMPLETED");
        assertThat(secondComplete.path("grossingTaskCreated").asBoolean()).isFalse();

        JsonNode pendingRegistrations =
            listPendingTechnicalSpecimenRegistrations(context.pathologyNo(), USER_RECEIVE);
        assertThat(pendingRegistrations.path("total").asInt()).isZero();

        JsonNode grossingTasks =
            listPendingTasks("GROSSING", context.pathologyNo(), USER_M3_GROSSING);
        assertThat(grossingTasks.path("total").asInt()).isEqualTo(1);

        Long grossingTaskCount = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from technical_pending_tasks
            where task_type = 'GROSSING'
              and object_type = 'CASE'
              and object_id = :caseId
            """, Map.of("caseId", context.caseId()), Long.class);
        assertThat(grossingTaskCount).isEqualTo(1L);

        List<String> eventNodes = namedParameterJdbcTemplate.queryForList("""
            select node_code
            from workflow_events
            where case_id = :caseId
            order by event_time asc, created_at asc
            """, Map.of("caseId", context.caseId()), String.class);
        assertThat(eventNodes).containsSequence("RECEPTION", "SPECIMEN_REGISTRATION", "GROSSING");
    }

    @Test
    void shouldAllowStartingGrossingWithoutAssignmentAfterRegistration() throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetGrossingTask("APP-M3-REG-003", "BC-M3-REG-003");

        JsonNode grossingTasks =
            listPendingTasks("GROSSING", context.pathologyNo(), USER_M3_GROSSING);
        assertThat(grossingTasks.path("items").get(0).path("assignedToUserId").isNull()).isTrue();

        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "terminalCode": "TG-REG-01"
            }
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.taskId").value(context.grossingTaskId()))
            .andExpect(jsonPath("$.data.taskStatus").value("IN_PROGRESS"));
    }

    @Test
    void shouldReturnWorkspaceAggregateAndSupportReceivedDateFilter() throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-004", "BC-M3-REG-004");

        JsonNode emptyResult = listPendingTechnicalSpecimenRegistrations(
            context.pathologyNo(),
            LocalDate.now().minusDays(5).toString(),
            LocalDate.now().minusDays(3).toString(),
            USER_RECEIVE);
        assertThat(emptyResult.path("total").asInt()).isZero();

        JsonNode filteredResult = listPendingTechnicalSpecimenRegistrations(
            context.pathologyNo(),
            LocalDate.now().minusDays(1).toString(),
            LocalDate.now().plusDays(1).toString(),
            USER_RECEIVE);
        assertThat(filteredResult.path("total").asInt()).isEqualTo(1);

        JsonNode workspace = technicalSpecimenRegistrationWorkspace(context.caseId(), USER_RECEIVE);
        assertThat(workspace.path("pendingSummary").path("caseId").asText()).isEqualTo(context.caseId());
        assertThat(workspace.path("basicInfo").path("pathologyNo").asText()).isEqualTo(context.pathologyNo());
        assertThat(workspace.path("basicInfo").path("patientName").asText()).isEqualTo("Patient A");
        assertThat(workspace.path("materials").size()).isEqualTo(1);
        assertThat(workspace.path("materials").get(0).path("specimenId").asText()).isEqualTo(context.specimenId());
        assertThat(workspace.path("checkItems").size()).isZero();
        assertThat(workspace.path("mediaAssets").size()).isZero();
        assertThat(workspace.path("actionFlags").path("canSaveMaterials").asBoolean()).isTrue();
        assertThat(workspace.path("detailSections").path("historySummary").isNull()).isTrue();
    }

    @Test
    void shouldSaveMaterialsByAddingUpdatingAndRemovingSpecimens() throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-005", "BC-M3-REG-005");

        JsonNode saved = saveTechnicalSpecimenRegistrationMaterials(context.caseId(), USER_RECEIVE, """
            {
              "terminalCode": "T-M3-REG-01",
              "materials": [
                {
                  "specimenId": "%s",
                  "specimenType": "CELL_BLOCK",
                  "specimenName": "Updated Tissue",
                  "sourcePart": "Left Thyroid"
                },
                {
                  "specimenType": "BIOPSY",
                  "specimenName": "Added Tissue",
                  "sourcePart": "Right Thyroid"
                }
              ]
            }
            """.formatted(context.specimenId()));

        assertThat(saved.path("materials").size()).isEqualTo(2);
        assertThat(saved.path("materials").get(0).path("specimenType").asText()).isEqualTo("CELL_BLOCK");
        assertThat(saved.path("materials").get(0).path("specimenName").asText()).isEqualTo("Updated Tissue");
        assertThat(saved.path("materials").get(1).path("specimenName").asText()).isEqualTo("Added Tissue");

        String addedSpecimenId = saved.path("materials").get(1).path("specimenId").asText();
        JsonNode replaced = saveTechnicalSpecimenRegistrationMaterials(context.caseId(), USER_RECEIVE, """
            {
              "terminalCode": "T-M3-REG-02",
              "materials": [
                {
                  "specimenId": "%s",
                  "specimenType": "BIOPSY",
                  "specimenName": "Added Tissue",
                  "sourcePart": "Right Thyroid"
                }
              ]
            }
            """.formatted(addedSpecimenId));

        assertThat(replaced.path("materials").size()).isEqualTo(1);
        assertThat(replaced.path("materials").get(0).path("specimenId").asText()).isEqualTo(addedSpecimenId);

        String removedStatus = namedParameterJdbcTemplate.queryForObject("""
            select specimen_status
            from specimens
            where id = :specimenId
            """, Map.of("specimenId", context.specimenId()), String.class);
        assertThat(removedStatus).isEqualTo("RETURNED");
    }

    @Test
    void shouldUploadAndDeleteTechnicalRegistrationMediaAssets() throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-006", "BC-M3-REG-006");
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "registration-photo.jpg",
            "image/jpeg",
            new byte[] {9, 8, 7, 6});

        JsonNode uploaded = responseBody(mockMvc.perform(authorized(
            multipart("/api/v1/technical-specimen-registrations/{caseId}/media-assets", context.caseId()).file(file),
            USER_RECEIVE)), 200);
        assertThat(uploaded.path("assetId").asText()).isNotBlank();
        assertThat(uploaded.path("fileUrl").asText()).startsWith("/api/v1/grossing-media-assets/files/");

        JsonNode workspace = technicalSpecimenRegistrationWorkspace(context.caseId(), USER_RECEIVE);
        assertThat(workspace.path("mediaAssets").size()).isEqualTo(1);

        JsonNode deleted = deleteTechnicalSpecimenRegistrationMediaAsset(
            context.caseId(),
            uploaded.path("assetId").asText(),
            USER_RECEIVE);
        assertThat(deleted.path("deleted").asBoolean()).isTrue();

        JsonNode refreshedWorkspace = technicalSpecimenRegistrationWorkspace(context.caseId(), USER_RECEIVE);
        assertThat(refreshedWorkspace.path("mediaAssets").size()).isZero();
    }
}
