package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class DiagnosticWorkbenchMaterialSectionIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    @Test
    void shouldReturnMaterialSectionAggregatesInDiagnosticWorkbench() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase(
            "APP-M4-MATERIAL-001",
            "BC-M4-MATERIAL-001");
        String applicationId = namedParameterJdbcTemplate.queryForObject("""
            select application_id
            from pathology_cases
            where id = :caseId
            """, Map.of("caseId", context.caseId()), String.class);
        String patientId = "P-M4-MATERIAL-001";
        LocalDateTime now = LocalDateTime.now();
        namedParameterJdbcTemplate.update("""
            update applications
            set patient_id = :patientId,
                submission_date = :submissionDate,
                updated_at = :updatedAt
            where id = :applicationId
            """, Map.of(
            "applicationId", applicationId,
            "patientId", patientId,
            "submissionDate", LocalDate.of(2026, 5, 26),
            "updatedAt", now));

        Long registrationExtensionCount = namedParameterJdbcTemplate.queryForObject("""
            select count(1)
            from application_registration_workbench
            where application_id = :applicationId
            """, Map.of("applicationId", applicationId), Long.class);
        if (registrationExtensionCount != null && registrationExtensionCount > 0) {
            namedParameterJdbcTemplate.update("""
                update application_registration_workbench
                set id_no = :patientIdDisplay,
                    inpatient_no = :inpatientNo,
                    bed_no = :bedNo,
                    phone = :phone,
                    check_item = :checkItem,
                    ward_name = :wardName,
                    updated_at = :updatedAt
                where application_id = :applicationId
                """, Map.of(
                "applicationId", applicationId,
                "patientIdDisplay", "08305",
                "inpatientNo", "IP-001",
                "bedNo", "B-12",
                "phone", "18170000000",
                "checkItem", "切片检查与诊断",
                "wardName", "外科病区",
                "updatedAt", now));
        } else {
            namedParameterJdbcTemplate.update("""
                insert into application_registration_workbench
                    (application_id, id_no, inpatient_no, bed_no, phone, check_item, ward_name, created_at, updated_at)
                values
                    (:applicationId, :patientIdDisplay, :inpatientNo, :bedNo, :phone, :checkItem, :wardName, :createdAt, :updatedAt)
                """, Map.of(
                "applicationId", applicationId,
                "patientIdDisplay", "08305",
                "inpatientNo", "IP-001",
                "bedNo", "B-12",
                "phone", "18170000000",
                "checkItem", "切片检查与诊断",
                "wardName", "外科病区",
                "createdAt", now,
                "updatedAt", now));
        }
        String specimenId = namedParameterJdbcTemplate.queryForObject("""
            select id from specimens where case_id = :caseId fetch first 1 rows only
            """, Map.of("caseId", context.caseId()), String.class);
        namedParameterJdbcTemplate.update("""
            insert into samplings
                (id, case_id, specimen_id, sampling_status, block_count, sampled_by_name, sampled_at, created_at, updated_at)
            values
                (:id, :caseId, :specimenId, 'COMPLETED', 1, :sampledByName, :sampledAt, :createdAt, :updatedAt)
            """, Map.of(
            "id", "SAMPLING-M4-MATERIAL-001",
            "caseId", context.caseId(),
            "specimenId", specimenId,
            "sampledByName", "取材医生甲",
            "sampledAt", LocalDateTime.of(2026, 6, 1, 8, 0),
            "createdAt", now,
            "updatedAt", now));
        namedParameterJdbcTemplate.update("""
            insert into samplings
                (id, case_id, specimen_id, sampling_status, block_count, sampled_by_name, sampled_at, created_at, updated_at)
            values
                (:id, :caseId, :specimenId, 'COMPLETED', 1, :sampledByName, :sampledAt, :createdAt, :updatedAt)
            """, Map.of(
            "id", "SAMPLING-M4-MATERIAL-002",
            "caseId", context.caseId(),
            "specimenId", specimenId,
            "sampledByName", "取材医生乙",
            "sampledAt", LocalDateTime.of(2026, 6, 1, 8, 1),
            "createdAt", now,
            "updatedAt", now));
        namedParameterJdbcTemplate.update("""
            insert into samplings
                (id, case_id, specimen_id, sampling_status, block_count, sampled_by_name, sampled_at, created_at, updated_at)
            values
                (:id, :caseId, :specimenId, 'COMPLETED', 1, :sampledByName, :sampledAt, :createdAt, :updatedAt)
            """, Map.of(
            "id", "SAMPLING-M4-MATERIAL-003",
            "caseId", context.caseId(),
            "specimenId", specimenId,
            "sampledByName", "取材医生甲",
            "sampledAt", LocalDateTime.of(2026, 6, 1, 8, 2),
            "createdAt", now,
            "updatedAt", now));
        namedParameterJdbcTemplate.update("""
            insert into historical_reports
                (id, source_system, external_report_no, patient_id, patient_name, pathology_no,
                 report_date, final_diagnosis, created_at, updated_at)
            values
                (:id, 'HISTORY', :externalReportNo, :patientId, 'Patient A', :pathologyNo,
                 :reportDate, :finalDiagnosis, :createdAt, :updatedAt)
            """, Map.of(
            "id", "HIS-M4-MATERIAL-001",
            "externalReportNo", "F2600039",
            "patientId", patientId,
            "pathologyNo", "OLD-PATH-001",
            "reportDate", LocalDateTime.of(2026, 5, 14, 11, 40),
            "finalDiagnosis", "历史诊断",
            "createdAt", now,
            "updatedAt", now));
        namedParameterJdbcTemplate.update("""
            insert into billing_records
                (id, case_id, billing_no, billing_stage, item_name, billing_status,
                 billed_at, operator_name, created_at, updated_at)
            values
                (:id, :caseId, :billingNo, 'DIAGNOSIS', :itemName, 'SUCCESS',
                 :billedAt, :operatorName, :createdAt, :updatedAt)
            """, Map.of(
            "id", "BILL-M4-MATERIAL-001",
            "caseId", context.caseId(),
            "billingNo", "BN-M4-MATERIAL-001",
            "itemName", "免疫组化 CK",
            "billedAt", LocalDateTime.of(2026, 6, 1, 11, 0),
            "operatorName", "收费员甲",
            "createdAt", now,
            "updatedAt", now));

        JsonNode workbench = diagnosticWorkbench(context.caseId(), USER_M4_DIAGNOSIS);

        assertThat(workbench.path("patientId").asText()).isEqualTo(patientId);
        assertThat(workbench.path("patientIdDisplay").asText()).isEqualTo("08305");
        assertThat(workbench.path("inpatientNo").asText()).isEqualTo("IP-001");
        assertThat(workbench.path("bedNo").asText()).isEqualTo("B-12");
        assertThat(workbench.path("phone").asText()).isEqualTo("18170000000");
        assertThat(workbench.path("wardName").asText()).isEqualTo("外科病区");
        assertThat(workbench.path("samplingDoctorNames").toString())
            .isEqualTo("[\"取材医生甲\",\"取材医生乙\",\"取材员\"]");
        assertThat(workbench.path("checkItem").asText()).isEqualTo("切片检查与诊断");
        assertThat(workbench.path("submissionDate").asText()).isEqualTo("2026-05-26");
        assertThat(workbench.path("historicalPathologies")).hasSize(1);
        assertThat(workbench.path("historicalPathologies").get(0).path("examinationNo").asText())
            .isEqualTo("F2600039");
        assertThat(workbench.path("historicalPathologies").get(0).path("diagnosis").asText())
            .isEqualTo("历史诊断");
        assertThat(workbench.path("pacsExaminations")).isEmpty();
        assertThat(workbench.path("reportTraces")).isNotEmpty();
        assertThat(workbench.path("remarkSections")).isNotEmpty();
        assertThat(workbench.path("chargeItems")).hasSize(1);
        assertThat(workbench.path("chargeItems").get(0).path("itemName").asText())
            .isEqualTo("免疫组化 CK");
        assertThat(workbench.path("chargeItems").get(0).path("chargedByName").asText())
            .isEqualTo("收费员甲");
    }

    @Test
    void shouldReturnMedicalOrderOnlyBlocksInDiagnosticWorkbench() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase(
            "APP-M4-MATERIAL-BLOCK-001",
            "BC-M4-MATERIAL-BLOCK-001");
        LocalDateTime now = LocalDateTime.now();
        namedParameterJdbcTemplate.update("""
            insert into medical_order_blocks
                (id, case_id, block_no, created_by_user_id, created_by_name, created_at, updated_at)
            values
                (:id, :caseId, :blockNo, :createdByUserId, :createdByName, :createdAt, :updatedAt)
            """, Map.of(
            "id", "MOB-M4-MATERIAL-001",
            "caseId", context.caseId(),
            "blockNo", "A3",
            "createdByUserId", USER_M4_DIAGNOSIS,
            "createdByName", "M4 Diagnosis",
            "createdAt", now,
            "updatedAt", now));

        JsonNode workbench = diagnosticWorkbench(context.caseId(), USER_M4_DIAGNOSIS);

        assertThat(workbench.path("medicalOrderBlocks")).hasSize(1);
        assertThat(workbench.path("medicalOrderBlocks").get(0).path("medicalOrderBlockId").asText())
            .isEqualTo("MOB-M4-MATERIAL-001");
        assertThat(workbench.path("medicalOrderBlocks").get(0).path("blockNo").asText())
            .isEqualTo("A3");
    }

    @Test
    void shouldKeepDiagnosticWorkbenchAvailableWhenRegistrationExtensionIsMissing() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase(
            "APP-M4-MATERIAL-NO-REGISTRATION-001",
            "BC-M4-MATERIAL-NO-REGISTRATION-001");

        namedParameterJdbcTemplate.update("""
            delete from application_registration_workbench
            where application_id = (
                select application_id from pathology_cases where id = :caseId
            )
            """, Map.of("caseId", context.caseId()));

        JsonNode workbench = diagnosticWorkbench(context.caseId(), USER_M4_DIAGNOSIS);

        assertThat(workbench.path("caseId").asText()).isEqualTo(context.caseId());
        assertThat(workbench.path("inpatientNo").isMissingNode()
            || workbench.path("inpatientNo").isNull()
            || workbench.path("inpatientNo").asText().isEmpty()).isTrue();
        assertThat(workbench.path("phone").isMissingNode()
            || workbench.path("phone").isNull()
            || workbench.path("phone").asText().isEmpty()).isTrue();
        assertThat(workbench.path("patientIdDisplay").isMissingNode()
            || workbench.path("patientIdDisplay").isNull()
            || workbench.path("patientIdDisplay").asText().isEmpty()).isTrue();
        assertThat(workbench.path("checkItem").isMissingNode()
            || workbench.path("checkItem").isNull()
            || workbench.path("checkItem").asText().isEmpty()).isTrue();
    }

    @Test
    void shouldReturnWorkbenchForCaseWithRenderedReport() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase(
            "APP-M4-MATERIAL-REPORT-001",
            "BC-M4-MATERIAL-REPORT-001");
        namedParameterJdbcTemplate.update("""
            insert into pathology_reports
                (id, case_id, task_id, report_no, pathology_no, report_scope, report_seq,
                 report_status, version_no, final_diagnosis, rich_text_content, render_snapshot,
                 created_at, updated_at)
            values
                (:id, :caseId, :taskId, :reportNo, :pathologyNo, 'ROUTINE', 1,
                 'DRAFT', 1, '诊断工作台报告', '<p>诊断工作台报告</p>', :renderSnapshot,
                 :createdAt, :updatedAt)
            """, Map.of(
            "id", "REPORT-M4-MATERIAL-001",
            "caseId", context.caseId(),
            "taskId", context.diagnosticTaskId(),
            "reportNo", "RP-M4-MATERIAL-001",
            "pathologyNo", context.pathologyNo(),
            "renderSnapshot", "{\"schemaVersion\":1,\"blocks\":[]}",
            "createdAt", LocalDateTime.now(),
            "updatedAt", LocalDateTime.now()));

        JsonNode workbench = diagnosticWorkbench(context.caseId(), USER_M4_DIAGNOSIS);

        assertThat(workbench.path("currentReport").path("reportNo").asText())
            .isEqualTo("RP-M4-MATERIAL-001");
        assertThat(workbench.path("currentReport").path("renderSnapshot").path("schemaVersion").asInt())
            .isEqualTo(1);
    }
}
