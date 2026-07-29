package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
            listPendingTechnicalSpecimenRegistrations(context.applicationNo(), USER_RECEIVE);

        assertThat(pendingRegistrations.path("total").asInt()).isEqualTo(1);
        assertThat(pendingRegistrations.path("items").get(0).path("caseId").asText())
            .isEqualTo(context.caseId());
        assertThat(pendingRegistrations.path("items").get(0).path("pathologyNo").isNull()).isTrue();

        String pathologyNo = namedParameterJdbcTemplate.queryForObject("""
            select pathology_no
            from pathology_cases
            where id = :caseId
            """, Map.of("caseId", context.caseId()), String.class);
        assertThat(pathologyNo).isNull();

        Long pendingGrossingTaskCount = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from technical_pending_tasks
            where task_type = 'GROSSING'
              and object_type = 'CASE'
              and object_id = :caseId
            """, Map.of("caseId", context.caseId()), Long.class);
        assertThat(pendingGrossingTaskCount).isZero();
    }

    @Test
    void shouldHidePathologyNoForPendingRegistrationEvenWhenCaseAlreadyHasNumber() throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-LEGACY-001", "BC-M3-REG-LEGACY-001");
        String legacyPathologyNo = "BL-LEGACY-" + context.caseId();
        namedParameterJdbcTemplate.update("""
            update pathology_cases
            set pathology_no = :pathologyNo
            where id = :caseId
            """, Map.of("pathologyNo", legacyPathologyNo, "caseId", context.caseId()));

        JsonNode pendingRegistrations =
            listPendingTechnicalSpecimenRegistrations(context.applicationNo(), USER_RECEIVE);
        JsonNode workspace =
            technicalSpecimenRegistrationWorkspace(context.caseId(), USER_RECEIVE);

        assertThat(pendingRegistrations.path("total").asInt()).isEqualTo(1);
        assertThat(pendingRegistrations.path("items").get(0).path("pathologyNo").isNull()).isTrue();
        assertThat(workspace.path("pendingSummary").path("pathologyNo").isNull()).isTrue();
        assertThat(workspace.path("basicInfo").path("pathologyNo").isNull()).isTrue();
    }

    @Test
    void shouldFilterPendingRegistrationsByApplicationType() throws Exception {
        TechnicalCaseContext routineContext =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-TYPE-001", "BC-M3-REG-TYPE-001");
        TechnicalCaseContext frozenContext =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-TYPE-002", "BC-M3-REG-TYPE-002");

        namedParameterJdbcTemplate.update("""
            update applications
            set application_type = 'FROZEN'
            where id = :applicationId
            """, Map.of("applicationId", frozenContext.applicationId()));

        JsonNode filteredRegistrations =
            listPendingTechnicalSpecimenRegistrations(frozenContext.applicationNo(), "FROZEN", null, null, USER_RECEIVE);

        assertThat(filteredRegistrations.path("total").asInt()).isEqualTo(1);
        assertThat(filteredRegistrations.path("items").get(0).path("caseId").asText())
            .isEqualTo(frozenContext.caseId());
        assertThat(filteredRegistrations.path("items").get(0).path("applicationType").asText())
            .isEqualTo("FROZEN");
        assertThat(filteredRegistrations.path("items").get(0).path("caseId").asText())
            .isNotEqualTo(routineContext.caseId());
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
            .andExpect(jsonPath("$.data.pathologyNo").value(nullValue()))
            .andExpect(jsonPath("$.data.materials[0].specimenName").value("Thyroid Tissue"))
            .andExpect(jsonPath("$.data.checkItems").isArray())
            .andExpect(jsonPath("$.data.clinicalDiagnosis").value("Papillary thyroid carcinoma"));

        JsonNode firstComplete =
            completeTechnicalSpecimenRegistration(context.caseId(), "first completion");
        JsonNode secondComplete =
            completeTechnicalSpecimenRegistration(context.caseId(), "repeat completion");
        String pathologyNo = firstComplete.path("pathologyNo").asText();

        assertThat(firstComplete.path("registrationStatus").asText()).isEqualTo("COMPLETED");
        assertThat(firstComplete.path("grossingTaskCreated").asBoolean()).isTrue();
        assertThat(pathologyNo).isNotBlank();
        assertThat(secondComplete.path("registrationStatus").asText()).isEqualTo("COMPLETED");
        assertThat(secondComplete.path("grossingTaskCreated").asBoolean()).isFalse();
        assertThat(secondComplete.path("pathologyNo").asText()).isEqualTo(pathologyNo);

        JsonNode pendingRegistrations =
            listPendingTechnicalSpecimenRegistrations(context.applicationNo(), USER_RECEIVE);
        assertThat(pendingRegistrations.path("total").asInt()).isZero();

        JsonNode completedRegistrations =
            listTechnicalSpecimenRegistrations(context.applicationNo(), null, "COMPLETED", null, null, USER_RECEIVE);
        assertThat(completedRegistrations.path("total").asInt()).isEqualTo(1);
        assertThat(completedRegistrations.path("items").get(0).path("caseId").asText())
            .isEqualTo(context.caseId());
        assertThat(completedRegistrations.path("items").get(0).path("registrationStatus").asText())
            .isEqualTo("COMPLETED");
        assertThat(completedRegistrations.path("items").get(0).path("registeredAt").asText())
            .isNotBlank();

        JsonNode grossingTasks =
            listPendingTasks("GROSSING", pathologyNo, USER_M3_GROSSING);
        assertThat(grossingTasks.path("total").asInt()).isEqualTo(1);
        assertThat(grossingTasks.path("items").get(0).path("patientName").asText()).isEqualTo("Patient A");
        assertThat(grossingTasks.path("items").get(0).path("patientId").asText()).isEqualTo("P-001");

        String persistedPathologyNo = namedParameterJdbcTemplate.queryForObject("""
            select pathology_no
            from pathology_cases
            where id = :caseId
            """, Map.of("caseId", context.caseId()), String.class);
        assertThat(persistedPathologyNo).isEqualTo(pathologyNo);

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
    void shouldGenerateConsultationPathologyNoAndPersistApplicationType() throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-CONSULT-002A", "BC-M3-REG-CONSULT-002A");

        JsonNode completion =
            completeTechnicalSpecimenRegistration(context.caseId(), "consultation completion", "CONSULTATION");

        String pathologyNo = completion.path("pathologyNo").asText();
        assertThat(pathologyNo).matches("^HZ\\d{2}\\d{5}$");

        String persistedPathologyNo = namedParameterJdbcTemplate.queryForObject("""
            select pathology_no
            from pathology_cases
            where id = :caseId
            """, Map.of("caseId", context.caseId()), String.class);
        assertThat(persistedPathologyNo).isEqualTo(pathologyNo);

        String persistedApplicationType = namedParameterJdbcTemplate.queryForObject("""
            select application_type
            from applications
            where id = :applicationId
            """, Map.of("applicationId", context.applicationId()), String.class);
        assertThat(persistedApplicationType).isEqualTo("CONSULTATION");
    }

    @Test
    void shouldCompleteRegistrationWithManualPathologyNoWhenCandidateIsValidAndUnique() throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-MANUAL-002C", "BC-M3-REG-MANUAL-002C");

        JsonNode completion =
            completeTechnicalSpecimenRegistration(
                context.caseId(),
                "manual pathology no completion",
                "CONSULTATION",
                "HZ2601234"
            );

        assertThat(completion.path("pathologyNo").asText()).isEqualTo("HZ2601234");

        String persistedPathologyNo = namedParameterJdbcTemplate.queryForObject("""
            select pathology_no
            from pathology_cases
            where id = :caseId
            """, Map.of("caseId", context.caseId()), String.class);
        assertThat(persistedPathologyNo).isEqualTo("HZ2601234");
    }

    @Test
    void shouldRejectManualPathologyNoWhenCandidateDoesNotMatchSelectedApplicationType() throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-MISMATCH-002D", "BC-M3-REG-MISMATCH-002D");

        mockMvc.perform(authorized(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/api/v1/technical-specimen-registrations/{caseId}/complete",
                    context.caseId()),
                USER_RECEIVE)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "applicationType": "CONSULTATION",
                  "pathologyNo": "BL202606080001",
                  "terminalCode": "T-M3-REG",
                  "remarks": "mismatched candidate"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "Pathology number does not match selected application type")));

        String persistedPathologyNo = namedParameterJdbcTemplate.queryForObject("""
            select pathology_no
            from pathology_cases
            where id = :caseId
            """, Map.of("caseId", context.caseId()), String.class);
        assertThat(persistedPathologyNo).isNull();
    }

    @Test
    void shouldRejectManualPathologyNoWhenCandidateAlreadyBelongsToAnotherCase() throws Exception {
        TechnicalCaseContext existingContext =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-DUP-002E", "BC-M3-REG-DUP-002E");
        TechnicalCaseContext targetContext =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-DUP-002F", "BC-M3-REG-DUP-002F");
        namedParameterJdbcTemplate.update("""
            update pathology_cases
            set pathology_no = 'HZ2605678'
            where id = :caseId
            """, Map.of("caseId", existingContext.caseId()));

        mockMvc.perform(authorized(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/api/v1/technical-specimen-registrations/{caseId}/complete",
                    targetContext.caseId()),
                USER_RECEIVE)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "applicationType": "CONSULTATION",
                  "pathologyNo": "HZ2605678",
                  "terminalCode": "T-M3-REG",
                  "remarks": "duplicate candidate"
                }
                """))
            .andExpect(status().isConflict())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "Pathology number already exists")));

        String persistedPathologyNo = namedParameterJdbcTemplate.queryForObject("""
            select pathology_no
            from pathology_cases
            where id = :caseId
            """, Map.of("caseId", targetContext.caseId()), String.class);
        assertThat(persistedPathologyNo).isNull();
    }

    @Test
    void shouldAllowManualPathologyNoWhenCandidateAlreadyBelongsToCurrentCase() throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-SAME-002G", "BC-M3-REG-SAME-002G");
        namedParameterJdbcTemplate.update("""
            update pathology_cases
            set pathology_no = 'HZ2606789'
            where id = :caseId
            """, Map.of("caseId", context.caseId()));

        JsonNode completion =
            completeTechnicalSpecimenRegistration(
                context.caseId(),
                "same case manual pathology no",
                "CONSULTATION",
                "HZ2606789"
            );

        assertThat(completion.path("pathologyNo").asText()).isEqualTo("HZ2606789");
    }

    @Test
    void shouldSyncDiagnosticTaskPathologyNoWhenManualCandidateUpdatesCurrentCase() throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-DIAG-SYNC-001", "BC-M3-REG-DIAG-SYNC-001");
        String legacyPathologyNo = "BL-LEGACY-" + context.caseId();

        namedParameterJdbcTemplate.update("""
            insert into diagnostic_tasks
                (id, case_id, pathology_no, task_type, status, priority, created_at, updated_at)
            values
                (:id, :caseId, :pathologyNo, 'PRIMARY', 'PENDING', 'NORMAL', current_timestamp, current_timestamp)
            """, Map.of(
            "id", "DT-M3-REG-DIAG-SYNC-" + context.caseId(),
            "caseId", context.caseId(),
            "pathologyNo", legacyPathologyNo));

        namedParameterJdbcTemplate.update("""
            update diagnostic_tasks
            set pathology_no = :pathologyNo
            where case_id = :caseId
            """, Map.of("caseId", context.caseId(), "pathologyNo", legacyPathologyNo));

        namedParameterJdbcTemplate.update("""
            update pathology_cases
            set pathology_no = :pathologyNo
            where id = :caseId
            """, Map.of("caseId", context.caseId(), "pathologyNo", legacyPathologyNo));

        namedParameterJdbcTemplate.update("""
            update technical_specimen_registrations
            set registration_status = 'PENDING',
                registered_by_user_id = null,
                registered_by_name = null,
                registered_at = null,
                remarks = null
            where case_id = :caseId
            """, Map.of("caseId", context.caseId()));

        JsonNode completion =
            completeTechnicalSpecimenRegistration(
                context.caseId(),
                "sync diagnostic task pathology no",
                "CONSULTATION",
                "HZ2610001"
            );

        assertThat(completion.path("pathologyNo").asText()).isEqualTo("HZ2610001");

        List<String> taskPathologyNos = namedParameterJdbcTemplate.query("""
            select pathology_no
            from diagnostic_tasks
            where case_id = :caseId
            """, Map.of("caseId", context.caseId()), (rs, rowNum) -> rs.getString(1));
        assertThat(taskPathologyNos)
            .isNotEmpty()
            .allMatch("HZ2610001"::equals);
    }

    @Test
    void shouldRegeneratePathologyNoWhenSelectedTypeDoesNotMatchExistingRule() throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-SUP-002B", "BC-M3-REG-SUP-002B");

        JsonNode firstCompletion =
            completeTechnicalSpecimenRegistration(context.caseId(), "routine completion", "ROUTINE");
        String routinePathologyNo = firstCompletion.path("pathologyNo").asText();
        assertThat(routinePathologyNo).matches("^BL\\d{8}\\d{4}$");

        namedParameterJdbcTemplate.update("""
            update technical_specimen_registrations
            set registration_status = 'PENDING',
                registered_by_user_id = null,
                registered_by_name = null,
                registered_at = null,
                remarks = null
            where case_id = :caseId
            """, Map.of("caseId", context.caseId()));

        JsonNode secondCompletion =
            completeTechnicalSpecimenRegistration(
                context.caseId(),
                "supplemental completion",
                "SUPPLEMENTAL_REPORT"
            );
        String supplementalPathologyNo = secondCompletion.path("pathologyNo").asText();

        assertThat(supplementalPathologyNo).matches("^MS\\d{2}\\d{5}$");
        assertThat(supplementalPathologyNo).isNotEqualTo(routinePathologyNo);

        String persistedApplicationType = namedParameterJdbcTemplate.queryForObject("""
            select application_type
            from applications
            where id = :applicationId
            """, Map.of("applicationId", context.applicationId()), String.class);
        assertThat(persistedApplicationType).isEqualTo("SUPPLEMENTAL_REPORT");
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
            receiveCaseAndGetPendingRegistration("APP-M3-REG-FILTER-004", "BC-M3-REG-FILTER-004");

        JsonNode emptyResult = listPendingTechnicalSpecimenRegistrations(
            context.applicationNo(),
            LocalDate.now().minusDays(5).toString(),
            LocalDate.now().minusDays(3).toString(),
            USER_RECEIVE);
        assertThat(emptyResult.path("total").asInt()).isZero();

        JsonNode filteredResult = listPendingTechnicalSpecimenRegistrations(
            context.applicationNo(),
            LocalDate.now().minusDays(1).toString(),
            LocalDate.now().plusDays(1).toString(),
            USER_RECEIVE);
        assertThat(filteredResult.path("total").asInt()).isEqualTo(1);

        JsonNode workspace = technicalSpecimenRegistrationWorkspace(context.caseId(), USER_RECEIVE);
        assertThat(workspace.path("pendingSummary").path("caseId").asText()).isEqualTo(context.caseId());
        assertThat(workspace.path("pendingSummary").path("pathologyNo").isNull()).isTrue();
        assertThat(workspace.path("basicInfo").path("pathologyNo").isNull()).isTrue();
        assertThat(workspace.path("basicInfo").path("patientName").asText()).isEqualTo("Patient A");
        assertThat(workspace.path("materials").size()).isEqualTo(1);
        assertThat(workspace.path("materials").get(0).path("specimenId").asText()).isEqualTo(context.specimenId());
        assertThat(workspace.path("checkItems").size()).isZero();
        assertThat(workspace.path("mediaAssets").size()).isZero();
        assertThat(workspace.path("actionFlags").path("canSaveMaterials").asBoolean()).isTrue();
        assertThat(workspace.path("detailSections").path("historySummary").isNull()).isTrue();
    }

    @Test
    void shouldExposeAndSaveReceiveScopedApplicationWorkbench() throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-WORKBENCH-004A", "BC-M3-REG-WORKBENCH-004A");

        JsonNode workbench =
            technicalSpecimenRegistrationApplicationWorkbench(context.caseId(), USER_RECEIVE);
        assertThat(workbench.path("applicationId").asText()).isEqualTo(context.applicationId());
        assertThat(workbench.path("patientInfo").path("patientName").asText()).isEqualTo("Patient A");
        assertThat(workbench.path("patientInfo").path("applicationNo").asText()).isEqualTo("APP-M3-REG-WORKBENCH-004A");

        String savePayload = """
            {
              "contagiousSpecimen": {
                "hepatitis": true,
                "hiv": false,
                "isolation": true,
                "syphilis": false,
                "tuberculosis": false
              },
              "gynecologyInfo": {
                "additionalNotes": "补充备注",
                "hpvResult": "未查",
                "lastMenstrualPeriod": "2026-05-20",
                "menopause": false,
                "previousCytology": "无明显异常",
                "previousTreatment": "无",
                "specialConditions": {
                  "abnormalBleeding": false,
                  "birthControl": false,
                  "hormoneReplacement": false,
                  "hysterectomy": false,
                  "iud": false,
                  "lactation": false,
                  "menopause": false,
                  "other": "无",
                  "pregnancy": false,
                  "radiotherapy": false
                }
              },
              "patientInfo": {
                "age": "45",
                "applicationDate": "2026-06-01T08:00:00",
                "applicationNo": "APP-M3-REG-WORKBENCH-004A",
                "applyDept": "OR",
                "applyDoctor": "Dr Test",
                "bedNo": "18",
                "checkItem": "HE+免疫组化",
                "clinicalDiagnosis": "更新后的临床诊断",
                "clinicalHistory": "更新后的临床病史",
                "deliveryRequirement": "立即送检",
                "endoscopyDiagnosis": "无",
                "frozenReminder": false,
                "gender": "F",
                "idNo": "ID-UPDATED-001",
                "imagingResult": "更新后的影像结果",
                "inpatientNo": "IP-UPDATED-001",
                "patientName": "Patient A",
                "patientVerified": true,
                "phone": "13800138000",
                "registrationStatus": "RECEIVED",
                "remark": "技术登记补充备注",
                "specimenType": "ROUTINE",
                "wardName": "Ward-8"
              },
              "surgeryInfo": {
                "buildingId": "BLDG-1",
                "clinicalFindings": "更新后的临床及手术所见",
                "fixativeType": "FORMALIN",
                "fixationPerson": "Receiver A",
                "fixationTime": "2026-06-01T09:30:00",
                "roomId": "OR-02",
                "specimenRemovalTime": "2026-06-01T08:45:00",
                "surgeryName": "甲状腺切除术"
              }
            }
            """;

        JsonNode saved = saveTechnicalSpecimenRegistrationApplicationWorkbenchPatientInfo(
            context.caseId(),
            USER_RECEIVE,
            savePayload);

        assertThat(saved.path("patientInfo").path("clinicalDiagnosis").asText()).isEqualTo("更新后的临床诊断");
        assertThat(saved.path("patientInfo").path("idNo").asText()).isEqualTo("ID-UPDATED-001");
        assertThat(saved.path("surgeryInfo").path("fixationTime").asText()).isEqualTo("2026-06-01T09:30");
        assertThat(saved.path("contagiousSpecimen").path("hepatitis").asBoolean()).isTrue();

        JsonNode refreshed =
            technicalSpecimenRegistrationApplicationWorkbench(context.caseId(), USER_RECEIVE);
        assertThat(refreshed.path("patientInfo").path("clinicalDiagnosis").asText()).isEqualTo("更新后的临床诊断");
        assertThat(refreshed.path("patientInfo").path("inpatientNo").asText()).isEqualTo("IP-UPDATED-001");

        completeTechnicalSpecimenRegistration(context.caseId(), "lock application workbench after completion");

        mockMvc.perform(authorized(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                    "/api/v1/technical-specimen-registrations/{caseId}/application-workbench/patient-info",
                    context.caseId()),
                USER_RECEIVE)
            .contentType(MediaType.APPLICATION_JSON)
            .content(savePayload))
            .andExpect(status().isConflict())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Technical specimen registration is completed")));
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
                  "specimenType": "活体",
                  "specimenName": "Updated Tissue",
                  "sourcePart": "Left Thyroid",
                  "tissueCount": 3,
                  "specimenSize": "大标本",
                  "frozen": true,
                  "evaluationItems": ["密封不严", "自定义评价项"]
                },
                {
                  "specimenType": "细胞学",
                  "specimenName": "Added Tissue",
                  "sourcePart": "Right Thyroid",
                  "tissueCount": 2,
                  "specimenSize": "小标本",
                  "frozen": false,
                  "evaluationItems": ["切面质量低"]
                }
              ]
            }
            """.formatted(context.specimenId()));

        assertThat(saved.path("materials").size()).isEqualTo(2);
        assertThat(saved.path("materials").get(0).path("specimenType").asText()).isEqualTo("活体");
        assertThat(saved.path("materials").get(0).path("specimenName").asText()).isEqualTo("Updated Tissue");
        assertThat(saved.path("materials").get(0).path("tissueCount").asInt()).isEqualTo(3);
        assertThat(saved.path("materials").get(0).path("specimenSize").asText()).isEqualTo("大标本");
        assertThat(saved.path("materials").get(0).path("frozen").asBoolean()).isTrue();
        assertThat(saved.path("materials").get(0).path("evaluationItems").get(0).asText()).isEqualTo("密封不严");
        assertThat(saved.path("materials").get(1).path("specimenName").asText()).isEqualTo("Added Tissue");
        assertThat(saved.path("materials").get(1).path("evaluationItems").get(0).asText()).isEqualTo("切面质量低");

        String addedSpecimenId = saved.path("materials").get(1).path("specimenId").asText();
        JsonNode replaced = saveTechnicalSpecimenRegistrationMaterials(context.caseId(), USER_RECEIVE, """
            {
              "terminalCode": "T-M3-REG-02",
              "materials": [
                {
                  "specimenId": "%s",
                  "specimenType": "细胞学",
                  "specimenName": "Added Tissue",
                  "sourcePart": "Right Thyroid"
                }
              ]
            }
            """.formatted(addedSpecimenId));

        assertThat(replaced.path("materials").size()).isEqualTo(1);
        assertThat(replaced.path("materials").get(0).path("specimenId").asText()).isEqualTo(addedSpecimenId);
        assertThat(replaced.path("materials").get(0).path("tissueCount").asInt()).isEqualTo(1);
        assertThat(replaced.path("materials").get(0).path("specimenSize").asText()).isEqualTo("小标本");
        assertThat(replaced.path("materials").get(0).path("frozen").asBoolean()).isFalse();
        assertThat(replaced.path("materials").get(0).path("evaluationItems").isEmpty()).isTrue();

        String removedStatus = namedParameterJdbcTemplate.queryForObject("""
            select specimen_status
            from specimens
            where id = :specimenId
            """, Map.of("specimenId", context.specimenId()), String.class);
        assertThat(removedStatus).isEqualTo("RETURNED");
    }

    @Test
    void shouldVerifyAndCancelMaterialVerificationInWorkspace() throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-005A", "BC-M3-REG-005A");

        JsonNode workspace = technicalSpecimenRegistrationWorkspace(context.caseId(), USER_RECEIVE);
        assertThat(workspace.path("materials").get(0).path("verificationStatus").asText()).isEqualTo("VERIFIED");

        JsonNode canceled = cancelTechnicalSpecimenRegistrationMaterialVerification(
            context.caseId(),
            context.specimenId(),
            USER_RECEIVE,
            """
                {
                  "terminalCode": "T-M3-REG-CANCEL",
                  "remarks": "取消核对"
                }
                """);

        JsonNode canceledMaterial = canceled.path("materials").get(0);
        assertThat(canceledMaterial.path("verificationStatus").asText()).isEqualTo("UNVERIFIED");
        assertThat(canceledMaterial.path("verificationCompletedAt").isNull()).isTrue();
        assertThat(canceledMaterial.path("verifiedByName").isNull()).isTrue();

        JsonNode verified = verifyTechnicalSpecimenRegistrationMaterial(
            context.caseId(),
            context.specimenId(),
            USER_RECEIVE,
            """
                {
                  "terminalCode": "T-M3-REG-VERIFY",
                  "remarks": "标本核对"
                }
                """);

        JsonNode verifiedMaterial = verified.path("materials").get(0);
        assertThat(verifiedMaterial.path("verificationStatus").asText()).isEqualTo("VERIFIED");
        assertThat(verifiedMaterial.path("verificationCompletedAt").asText()).isNotBlank();
        assertThat(verifiedMaterial.path("verifiedByName").asText()).isEqualTo(userDisplayName(USER_RECEIVE));
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

    @Test
    void shouldSaveDetailSectionOverridesAndFallbackAfterClearing() throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-007", "BC-M3-REG-007");

        saveTechnicalSpecimenRegistrationApplicationWorkbenchPatientInfo(context.caseId(), USER_RECEIVE, """
            {
              "contagiousSpecimen": {
                "hepatitis": false,
                "hiv": false,
                "isolation": true,
                "syphilis": false,
                "tuberculosis": false
              },
              "gynecologyInfo": {
                "additionalNotes": "既往甲状腺手术史",
                "hpvResult": "",
                "lastMenstrualPeriod": "",
                "menopause": false,
                "previousCytology": "",
                "previousTreatment": "",
                "specialConditions": {
                  "abnormalBleeding": false,
                  "birthControl": false,
                  "hormoneReplacement": false,
                  "hysterectomy": false,
                  "iud": false,
                  "lactation": false,
                  "menopause": false,
                  "other": "",
                  "pregnancy": false,
                  "radiotherapy": false
                }
              },
              "patientInfo": {
                "age": "35岁",
                "applicationDate": "2026-05-27",
                "applicationNo": "APP-M3-REG-007",
                "applyDept": "OR",
                "applyDoctor": "Dr A",
                "bedNo": "16床",
                "checkItem": "术中病理；免疫组化复核",
                "clinicalDiagnosis": "Papillary thyroid carcinoma",
                "clinicalHistory": "甲状腺结节病史，近一个月增大",
                "deliveryRequirement": "立即送检",
                "endoscopyDiagnosis": "",
                "frozenReminder": false,
                "gender": "女",
                "idNo": "320101199001011234",
                "imagingResult": "超声提示甲状腺左叶低回声结节",
                "inpatientNo": "ZY-REG-007",
                "patientName": "Patient A",
                "patientVerified": true,
                "phone": "13800001111",
                "registrationStatus": "RECEIVED",
                "remark": "detail section fallback",
                "specimenType": "ROUTINE",
                "wardName": "外科病区"
              },
              "surgeryInfo": {
                "buildingId": "B001",
                "clinicalFindings": "术中见甲状腺左叶结节样病灶",
                "fixativeType": "福尔马林",
                "fixationPerson": "护士甲",
                "fixationTime": "2026-05-27T10:15:00",
                "roomId": "OR-101",
                "specimenRemovalTime": "2026-05-27T10:00:00",
                "surgeryName": "甲状腺左叶切除术"
              }
            }
            """);

        JsonNode saved = saveTechnicalSpecimenRegistrationDetailSections(context.caseId(), USER_RECEIVE, """
            {
              "terminalCode": "T-M3-REG-DETAIL-01",
              "detailSections": {
                "historySummary": "人工病史摘要",
                "clinicalExaminationAndSurgeryFindings": "人工临床检查及手术所见",
                "labAndImagingExaminations": "人工检验和影像检查",
                "clinicalSubmissionRequirements": "人工临床送检要求",
                "infectiousAndPastHistorySummary": "人工传染/既往信息摘要",
                "externalPathologyDiagnosis": "外院病理诊断结果"
              }
            }
            """);

        assertThat(saved.path("detailSections").path("historySummary").asText()).isEqualTo("人工病史摘要");
        assertThat(saved.path("detailSections").path("clinicalExaminationAndSurgeryFindings").asText())
            .isEqualTo("人工临床检查及手术所见");
        assertThat(saved.path("detailSections").path("labAndImagingExaminations").asText())
            .isEqualTo("人工检验和影像检查");
        assertThat(saved.path("detailSections").path("clinicalSubmissionRequirements").asText())
            .isEqualTo("人工临床送检要求");
        assertThat(saved.path("detailSections").path("infectiousAndPastHistorySummary").asText())
            .isEqualTo("人工传染/既往信息摘要");
        assertThat(saved.path("detailSections").path("externalPathologyDiagnosis").asText())
            .isEqualTo("外院病理诊断结果");

        JsonNode cleared = saveTechnicalSpecimenRegistrationDetailSections(context.caseId(), USER_RECEIVE, """
            {
              "terminalCode": "T-M3-REG-DETAIL-02",
              "detailSections": {
                "historySummary": " ",
                "clinicalExaminationAndSurgeryFindings": "",
                "labAndImagingExaminations": "",
                "clinicalSubmissionRequirements": "",
                "infectiousAndPastHistorySummary": "",
                "externalPathologyDiagnosis": ""
              }
            }
            """);

        assertThat(cleared.path("detailSections").path("historySummary").asText())
            .isEqualTo("甲状腺结节病史，近一个月增大");
        assertThat(cleared.path("detailSections").path("clinicalExaminationAndSurgeryFindings").asText())
            .contains("临床检查: 术中见甲状腺左叶结节样病灶");
        assertThat(cleared.path("detailSections").path("clinicalExaminationAndSurgeryFindings").asText())
            .contains("手术名称: 甲状腺左叶切除术");
        assertThat(cleared.path("detailSections").path("labAndImagingExaminations").asText())
            .contains("影像检查: 超声提示甲状腺左叶低回声结节");
        assertThat(cleared.path("detailSections").path("clinicalSubmissionRequirements").asText())
            .isEqualTo("立即送检");
        assertThat(cleared.path("detailSections").path("infectiousAndPastHistorySummary").asText())
            .contains("传染信息: 隔离");
        assertThat(cleared.path("detailSections").path("externalPathologyDiagnosis").isNull()).isTrue();
    }

    @Test
    void shouldExposeDetailSectionOverridesInGrossingContextAndRejectEditingAfterCompletion() throws Exception {
        TechnicalCaseContext context =
            receiveCaseAndGetPendingRegistration("APP-M3-REG-008", "BC-M3-REG-008");

        saveTechnicalSpecimenRegistrationDetailSections(context.caseId(), USER_RECEIVE, """
            {
              "terminalCode": "T-M3-REG-DETAIL-03",
              "detailSections": {
                "historySummary": "取材专用病史摘要",
                "clinicalExaminationAndSurgeryFindings": "取材专用临床检查及手术所见",
                "labAndImagingExaminations": "取材专用检验和影像检查",
                "clinicalSubmissionRequirements": "取材专用送检要求",
                "infectiousAndPastHistorySummary": "取材专用传染/既往摘要",
                "externalPathologyDiagnosis": "取材专用外院病理诊断"
              }
            }
            """);

        JsonNode completeResult = completeTechnicalSpecimenRegistration(context.caseId(), "complete with overrides");
        String pathologyNo = completeResult.path("pathologyNo").asText();
        JsonNode grossingTasks = listPendingTasks("GROSSING", pathologyNo, USER_M3_GROSSING);
        String grossingTaskId = grossingTasks.path("items").get(0).path("id").asText();

        mockMvc.perform(authorized(get("/api/v1/grossings/{taskId}/context", grossingTaskId), USER_M3_GROSSING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.clinicalHistory").value("取材专用病史摘要"))
            .andExpect(jsonPath("$.data.relatedExaminations").value("取材专用检验和影像检查"))
            .andExpect(jsonPath("$.data.clinicalSubmissionRequirements").value("取材专用送检要求"))
            .andExpect(jsonPath("$.data.infectiousAndPastHistorySummary").value("取材专用传染/既往摘要"))
            .andExpect(jsonPath("$.data.externalPathologyDiagnosis").value("取材专用外院病理诊断"))
            .andExpect(jsonPath("$.data.contextSummary").value(org.hamcrest.Matchers.containsString("取材专用临床检查及手术所见")))
            .andExpect(jsonPath("$.data.contextSummary").value(org.hamcrest.Matchers.containsString("取材专用送检要求")))
            .andExpect(jsonPath("$.data.contextSummary").value(org.hamcrest.Matchers.containsString("取材专用传染/既往摘要")));

        mockMvc.perform(authorized(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                    "/api/v1/technical-specimen-registrations/{caseId}/detail-sections",
                    context.caseId()),
                USER_RECEIVE)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "terminalCode": "T-M3-REG-DETAIL-04",
                  "detailSections": {
                    "historySummary": "完成后仍尝试修改",
                    "clinicalExaminationAndSurgeryFindings": null,
                    "labAndImagingExaminations": null,
                    "clinicalSubmissionRequirements": null,
                    "infectiousAndPastHistorySummary": null,
                    "externalPathologyDiagnosis": null
                  }
                }
                """))
            .andExpect(status().isConflict())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Technical specimen registration is completed")));
    }
}
