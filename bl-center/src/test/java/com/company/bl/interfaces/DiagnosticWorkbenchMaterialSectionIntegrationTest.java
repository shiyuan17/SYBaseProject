package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
                updated_at = :updatedAt
            where id = :applicationId
            """, Map.of(
            "applicationId", applicationId,
            "patientId", patientId,
            "updatedAt", now));

        Long registrationExtensionCount = namedParameterJdbcTemplate.queryForObject("""
            select count(1)
            from application_registration_workbench
            where application_id = :applicationId
            """, Map.of("applicationId", applicationId), Long.class);
        if (registrationExtensionCount != null && registrationExtensionCount > 0) {
            namedParameterJdbcTemplate.update("""
                update application_registration_workbench
                set inpatient_no = :inpatientNo,
                    bed_no = :bedNo,
                    phone = :phone,
                    updated_at = :updatedAt
                where application_id = :applicationId
                """, Map.of(
                "applicationId", applicationId,
                "inpatientNo", "IP-001",
                "bedNo", "B-12",
                "phone", "18170000000",
                "updatedAt", now));
        } else {
            namedParameterJdbcTemplate.update("""
                insert into application_registration_workbench
                    (application_id, inpatient_no, bed_no, phone, created_at, updated_at)
                values
                    (:applicationId, :inpatientNo, :bedNo, :phone, :createdAt, :updatedAt)
                """, Map.of(
                "applicationId", applicationId,
                "inpatientNo", "IP-001",
                "bedNo", "B-12",
                "phone", "18170000000",
                "createdAt", now,
                "updatedAt", now));
        }
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
        assertThat(workbench.path("inpatientNo").asText()).isEqualTo("IP-001");
        assertThat(workbench.path("bedNo").asText()).isEqualTo("B-12");
        assertThat(workbench.path("phone").asText()).isEqualTo("18170000000");
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
}
