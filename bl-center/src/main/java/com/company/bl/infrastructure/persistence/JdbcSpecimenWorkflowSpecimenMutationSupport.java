package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.model.Specimen;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

abstract class JdbcSpecimenWorkflowSpecimenMutationSupport extends AbstractJdbcSpecimenWorkflowReadSupport {

    protected JdbcSpecimenWorkflowSpecimenMutationSupport(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    public void updateApplicationStatus(String applicationId, String status) {
        jdbcTemplate.update("""
            update applications
            set status = :status,
                updated_at = :updatedAt
            where id = :applicationId
            """, new MapSqlParameterSource()
            .addValue("applicationId", applicationId)
            .addValue("status", status)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void updateSpecimenLabelPrintStatus(String specimenId, String labelPrintStatus) {
        jdbcTemplate.update("""
            update specimens
            set label_print_status = :labelPrintStatus,
                updated_at = :updatedAt
            where id = :specimenId
            """, new MapSqlParameterSource()
            .addValue("specimenId", specimenId)
            .addValue("labelPrintStatus", labelPrintStatus)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void updateSpecimenMaterial(String specimenId,
                                       String specimenType,
                                       String specimenNameStandardized,
                                       String specimenSite,
                                       String remarks) {
        jdbcTemplate.update("""
            update specimens
            set specimen_type = :specimenType,
                specimen_name_standardized = :specimenNameStandardized,
                specimen_site = :specimenSite,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :specimenId
            """, new MapSqlParameterSource()
            .addValue("specimenId", specimenId)
            .addValue("specimenType", specimenType)
            .addValue("specimenNameStandardized", specimenNameStandardized)
            .addValue("specimenSite", specimenSite)
            .addValue("remarks", remarks)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public Specimen insertSpecimen(Specimen specimen) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("id", specimen.id())
            .addValue("applicationId", specimen.applicationId())
            .addValue("caseId", specimen.caseId())
            .addValue("specimenNo", specimen.specimenNo())
            .addValue("barcode", specimen.barcode())
            .addValue("specimenType", specimen.specimenType())
            .addValue("specimenNameStandardized", specimen.specimenNameStandardized())
            .addValue("specimenSite", specimen.specimenSite())
            .addValue("collectionMode", specimen.collectionMode())
            .addValue("specimenCount", specimen.specimenCount())
            .addValue("containerName", specimen.containerName())
            .addValue("containerCount", specimen.containerCount())
            .addValue("specimenStatus", specimen.specimenStatus().name())
            .addValue("fixationStatus", specimen.fixationStatus().name())
            .addValue("qualifiedFlag", specimen.qualified() ? 1 : 0)
            .addValue("unqualifiedReason", specimen.unqualifiedReason())
            .addValue("clinicalSymptom", specimen.clinicalSymptom())
            .addValue("applicantDepartmentId", specimen.applicantDepartmentId())
            .addValue("applicantDepartmentName", specimen.applicantDepartmentName())
            .addValue("applicantDoctorUserId", specimen.applicantDoctorUserId())
            .addValue("applicantDoctorName", specimen.applicantDoctorName())
            .addValue("submissionDate", specimen.submissionDate())
            .addValue("labelPrintBatchNo", specimen.labelPrintBatchNo())
            .addValue("labelPrintStatus", specimen.labelPrintStatus())
            .addValue("registeredByUserId", specimen.registeredByUserId())
            .addValue("registeredByName", specimen.registeredByName())
            .addValue("registeredAt", specimen.registeredAt())
            .addValue("terminalCode", specimen.terminalCode())
            .addValue("remarks", specimen.remarks())
            .addValue("createdAt", specimen.registeredAt())
            .addValue("updatedAt", specimen.registeredAt());
        if (hasSpecimenContainerColumns()) {
            jdbcTemplate.update("""
                insert into specimens
                    (id, application_id, case_id, specimen_no, barcode, specimen_type, specimen_name_standardized,
                     specimen_site, collection_mode, specimen_count, container_name, container_count,
                     specimen_status, fixation_status, qualified_flag,
                     unqualified_reason, clinical_symptom, applicant_department_id, applicant_department_name,
                     applicant_doctor_user_id, applicant_doctor_name, submission_date, label_print_batch_no,
                     label_print_status, registered_by_user_id, registered_by_name, registered_at, terminal_code,
                     remarks, created_at, updated_at)
                values
                    (:id, :applicationId, :caseId, :specimenNo, :barcode, :specimenType, :specimenNameStandardized,
                     :specimenSite, :collectionMode, :specimenCount, :containerName, :containerCount,
                     :specimenStatus, :fixationStatus, :qualifiedFlag,
                     :unqualifiedReason, :clinicalSymptom, :applicantDepartmentId, :applicantDepartmentName,
                     :applicantDoctorUserId, :applicantDoctorName, :submissionDate, :labelPrintBatchNo,
                     :labelPrintStatus, :registeredByUserId, :registeredByName, :registeredAt, :terminalCode,
                     :remarks, :createdAt, :updatedAt)
                """, parameters);
        } else {
            jdbcTemplate.update("""
                insert into specimens
                    (id, application_id, case_id, specimen_no, barcode, specimen_type, specimen_name_standardized,
                     specimen_site, collection_mode, specimen_count,
                     specimen_status, fixation_status, qualified_flag,
                     unqualified_reason, clinical_symptom, applicant_department_id, applicant_department_name,
                     applicant_doctor_user_id, applicant_doctor_name, submission_date, label_print_batch_no,
                     label_print_status, registered_by_user_id, registered_by_name, registered_at, terminal_code,
                     remarks, created_at, updated_at)
                values
                    (:id, :applicationId, :caseId, :specimenNo, :barcode, :specimenType, :specimenNameStandardized,
                     :specimenSite, :collectionMode, :specimenCount,
                     :specimenStatus, :fixationStatus, :qualifiedFlag,
                     :unqualifiedReason, :clinicalSymptom, :applicantDepartmentId, :applicantDepartmentName,
                     :applicantDoctorUserId, :applicantDoctorName, :submissionDate, :labelPrintBatchNo,
                     :labelPrintStatus, :registeredByUserId, :registeredByName, :registeredAt, :terminalCode,
                     :remarks, :createdAt, :updatedAt)
                """, parameters);
        }
        return specimen;
    }

    public void insertCollectionRecord(String applicationId,
                                       String specimenId,
                                       String collectionStatus,
                                       String collectionScene,
                                       String collectionMode,
                                       String labelPrintBatchNo,
                                       String printerCode,
                                       String collectorUserId,
                                       String collectorName,
                                       LocalDateTime collectedAt,
                                       String terminalCode,
                                       String remarks) {
        boolean hasPrinterCodeColumn = hasCollectionPrinterCodeColumn();
        String sql = hasPrinterCodeColumn
            ? """
                insert into specimen_collection_records
                    (id, application_id, specimen_id, collection_status, collection_scene, collection_mode,
                     label_print_batch_no, printer_code, collector_user_id, collector_name, collected_at, terminal_code, remarks)
                values
                    (:id, :applicationId, :specimenId, :collectionStatus, :collectionScene, :collectionMode,
                     :labelPrintBatchNo, :printerCode, :collectorUserId, :collectorName, :collectedAt, :terminalCode, :remarks)
                """
            : """
                insert into specimen_collection_records
                    (id, application_id, specimen_id, collection_status, collection_scene, collection_mode,
                     label_print_batch_no, collector_user_id, collector_name, collected_at, terminal_code, remarks)
                values
                    (:id, :applicationId, :specimenId, :collectionStatus, :collectionScene, :collectionMode,
                     :labelPrintBatchNo, :collectorUserId, :collectorName, :collectedAt, :terminalCode, :remarks)
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("id", nextId("SCR"))
            .addValue("applicationId", applicationId)
            .addValue("specimenId", specimenId)
            .addValue("collectionStatus", collectionStatus)
            .addValue("collectionScene", collectionScene)
            .addValue("collectionMode", collectionMode)
            .addValue("labelPrintBatchNo", labelPrintBatchNo)
            .addValue("printerCode", printerCode)
            .addValue("collectorUserId", collectorUserId)
            .addValue("collectorName", collectorName)
            .addValue("collectedAt", collectedAt)
            .addValue("terminalCode", terminalCode)
            .addValue("remarks", remarks));
    }

    public void upsertFixationRecord(String applicationId,
                                     String specimenId,
                                     FixationStatus fixationStatus,
                                     String fixationLiquidType,
                                     LocalDateTime fixationStartAt,
                                     LocalDateTime fixationCompletedAt,
                                     String verifiedByUserId,
                                     String verifiedByName,
                                     LocalDateTime verifiedAt,
                                     String terminalCode,
                                     String remarks) {
        Long count = jdbcTemplate.queryForObject("""
            select count(1)
            from specimen_fixation_records
            where specimen_id = :specimenId
            """, Map.of("specimenId", specimenId), Long.class);
        if (count != null && count > 0) {
            jdbcTemplate.update("""
                update specimen_fixation_records
                set fixation_status = :fixationStatus,
                    fixation_liquid_type = COALESCE(:fixationLiquidType, fixation_liquid_type),
                    fixation_start_at = COALESCE(:fixationStartAt, fixation_start_at),
                    fixation_completed_at = :fixationCompletedAt,
                    verified_by_user_id = COALESCE(:verifiedByUserId, verified_by_user_id),
                    verified_by_name = COALESCE(:verifiedByName, verified_by_name),
                    verified_at = COALESCE(:verifiedAt, verified_at),
                    terminal_code = :terminalCode,
                    remarks = :remarks
                where specimen_id = :specimenId
                """, new MapSqlParameterSource()
                .addValue("specimenId", specimenId)
                .addValue("fixationStatus", fixationStatus.name())
                .addValue("fixationLiquidType", fixationLiquidType)
                .addValue("fixationStartAt", fixationStartAt)
                .addValue("fixationCompletedAt", fixationCompletedAt)
                .addValue("verifiedByUserId", verifiedByUserId)
                .addValue("verifiedByName", verifiedByName)
                .addValue("verifiedAt", verifiedAt)
                .addValue("terminalCode", terminalCode)
                .addValue("remarks", remarks));
            return;
        }
        jdbcTemplate.update("""
            insert into specimen_fixation_records
                (id, application_id, specimen_id, fixation_status, fixation_liquid_type, fixation_start_at,
                 fixation_completed_at, verified_by_user_id, verified_by_name, verified_at, terminal_code, remarks)
            values
                (:id, :applicationId, :specimenId, :fixationStatus, :fixationLiquidType, :fixationStartAt,
                 :fixationCompletedAt, :verifiedByUserId, :verifiedByName, :verifiedAt, :terminalCode, :remarks)
            """, new MapSqlParameterSource()
            .addValue("id", nextId("SFR"))
            .addValue("applicationId", applicationId)
            .addValue("specimenId", specimenId)
            .addValue("fixationStatus", fixationStatus.name())
            .addValue("fixationLiquidType", fixationLiquidType)
            .addValue("fixationStartAt", fixationStartAt)
            .addValue("fixationCompletedAt", fixationCompletedAt)
            .addValue("verifiedByUserId", verifiedByUserId)
            .addValue("verifiedByName", verifiedByName)
            .addValue("verifiedAt", verifiedAt)
            .addValue("terminalCode", terminalCode)
            .addValue("remarks", remarks));
    }

    public void startSpecimenVerification(String applicationId,
                                          String specimenId,
                                          String verifiedByUserId,
                                          String verifiedByName,
                                          LocalDateTime verificationStartedAt,
                                          String terminalCode,
                                          String remarks) {
        Long count = jdbcTemplate.queryForObject("""
            select count(1)
            from specimen_fixation_records
            where specimen_id = :specimenId
            """, Map.of("specimenId", specimenId), Long.class);
        if (count != null && count > 0) {
            jdbcTemplate.update("""
                update specimen_fixation_records
                set verification_started_at = :verificationStartedAt,
                    verified_by_user_id = :verifiedByUserId,
                    verified_by_name = :verifiedByName,
                    terminal_code = :terminalCode,
                    remarks = :remarks
                where specimen_id = :specimenId
                """, new MapSqlParameterSource()
                .addValue("specimenId", specimenId)
                .addValue("verificationStartedAt", verificationStartedAt)
                .addValue("verifiedByUserId", verifiedByUserId)
                .addValue("verifiedByName", verifiedByName)
                .addValue("terminalCode", terminalCode)
                .addValue("remarks", remarks));
            return;
        }
        jdbcTemplate.update("""
            insert into specimen_fixation_records
                (id, application_id, specimen_id, fixation_status, verification_started_at,
                 verified_by_user_id, verified_by_name, terminal_code, remarks)
            values
                (:id, :applicationId, :specimenId, :fixationStatus, :verificationStartedAt,
                 :verifiedByUserId, :verifiedByName, :terminalCode, :remarks)
            """, new MapSqlParameterSource()
            .addValue("id", nextId("SFR"))
            .addValue("applicationId", applicationId)
            .addValue("specimenId", specimenId)
            .addValue("fixationStatus", FixationStatus.PENDING.name())
            .addValue("verificationStartedAt", verificationStartedAt)
            .addValue("verifiedByUserId", verifiedByUserId)
            .addValue("verifiedByName", verifiedByName)
            .addValue("terminalCode", terminalCode)
            .addValue("remarks", remarks));
    }

    public void completeSpecimenVerification(String specimenId,
                                             String verifiedByUserId,
                                             String verifiedByName,
                                             LocalDateTime verificationCompletedAt,
                                             String terminalCode,
                                             String remarks) {
        jdbcTemplate.update("""
            update specimen_fixation_records
            set verification_completed_at = :verificationCompletedAt,
                verified_at = :verificationCompletedAt,
                verified_by_user_id = :verifiedByUserId,
                verified_by_name = :verifiedByName,
                terminal_code = :terminalCode,
                remarks = :remarks
            where specimen_id = :specimenId
            """, new MapSqlParameterSource()
            .addValue("specimenId", specimenId)
            .addValue("verificationCompletedAt", verificationCompletedAt)
            .addValue("verifiedByUserId", verifiedByUserId)
            .addValue("verifiedByName", verifiedByName)
            .addValue("terminalCode", terminalCode)
            .addValue("remarks", remarks));
    }

    public void confirmSpecimen(String specimenId, LocalDateTime specimenConfirmedAt) {
        jdbcTemplate.update("""
            update specimens
            set specimen_status = 'VERIFIED',
                specimen_confirmed_at = :specimenConfirmedAt,
                updated_at = :updatedAt
            where id = :specimenId
            """, new MapSqlParameterSource()
            .addValue("specimenId", specimenId)
            .addValue("specimenConfirmedAt", specimenConfirmedAt)
            .addValue("updatedAt", specimenConfirmedAt));
    }

    public void bindSpecimenBarcode(String specimenId, String barcode) {
        jdbcTemplate.update("""
            update specimens
            set barcode = :barcode,
                updated_at = :updatedAt
            where id = :specimenId
            """, new MapSqlParameterSource()
            .addValue("specimenId", specimenId)
            .addValue("barcode", barcode)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void unbindSpecimenBarcode(String specimenId) {
        jdbcTemplate.update("""
            update specimens
            set barcode = null,
                updated_at = :updatedAt
            where id = :specimenId
            """, new MapSqlParameterSource()
            .addValue("specimenId", specimenId)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void checkInSpecimen(String specimenId,
                                String checkInStatus,
                                LocalDateTime checkedInAt,
                                String checkedInByUserId,
                                String checkedInByName) {
        jdbcTemplate.update("""
            update specimens
            set specimen_status = 'CHECKED_IN',
                check_in_status = :checkInStatus,
                checked_in_at = :checkedInAt,
                checked_in_by_user_id = :checkedInByUserId,
                checked_in_by_name = :checkedInByName,
                updated_at = :updatedAt
            where id = :specimenId
            """, new MapSqlParameterSource()
            .addValue("specimenId", specimenId)
            .addValue("checkInStatus", checkInStatus)
            .addValue("checkedInAt", checkedInAt)
            .addValue("checkedInByUserId", checkedInByUserId)
            .addValue("checkedInByName", checkedInByName)
            .addValue("updatedAt", checkedInAt));
    }

    public void confirmSpecimenRemoval(String specimenId,
                                       LocalDateTime specimenRemovalAt,
                                       String removalOperatorUserId,
                                       String removalOperatorName) {
        jdbcTemplate.update("""
            update specimens
            set specimen_removal_at = :specimenRemovalAt,
                specimen_removal_operator_user_id = :removalOperatorUserId,
                specimen_removal_operator_name = :removalOperatorName,
                updated_at = :updatedAt
            where id = :specimenId
            """, new MapSqlParameterSource()
            .addValue("specimenId", specimenId)
            .addValue("specimenRemovalAt", specimenRemovalAt)
            .addValue("removalOperatorUserId", removalOperatorUserId)
            .addValue("removalOperatorName", removalOperatorName)
            .addValue("updatedAt", specimenRemovalAt));
    }

    public void completeSpecimenVerificationFromRemoval(String applicationId,
                                                       String specimenId,
                                                       LocalDateTime verificationCompletedAt,
                                                       String verifiedByUserId,
                                                       String verifiedByName,
                                                       String terminalCode,
                                                       String remarks) {
        Long count = jdbcTemplate.queryForObject("""
            select count(1)
            from specimen_fixation_records
            where specimen_id = :specimenId
            """, Map.of("specimenId", specimenId), Long.class);
        if (count != null && count > 0) {
            jdbcTemplate.update("""
                update specimen_fixation_records
                set verification_started_at = coalesce(verification_started_at, :verificationCompletedAt),
                    verification_completed_at = coalesce(verification_completed_at, :verificationCompletedAt),
                    verified_at = coalesce(verified_at, :verificationCompletedAt),
                    verified_by_user_id = coalesce(verified_by_user_id, :verifiedByUserId),
                    verified_by_name = coalesce(verified_by_name, :verifiedByName),
                    terminal_code = coalesce(terminal_code, :terminalCode),
                    remarks = coalesce(remarks, :remarks)
                where specimen_id = :specimenId
                """, new MapSqlParameterSource()
                .addValue("specimenId", specimenId)
                .addValue("verificationCompletedAt", verificationCompletedAt)
                .addValue("verifiedByUserId", verifiedByUserId)
                .addValue("verifiedByName", verifiedByName)
                .addValue("terminalCode", terminalCode)
                .addValue("remarks", remarks));
            return;
        }
        jdbcTemplate.update("""
            insert into specimen_fixation_records
                (id, application_id, specimen_id, fixation_status, verification_started_at,
                 verification_completed_at, verified_at, verified_by_user_id, verified_by_name,
                 terminal_code, remarks)
            values
                (:id, :applicationId, :specimenId, :fixationStatus, :verificationCompletedAt,
                 :verificationCompletedAt, :verificationCompletedAt, :verifiedByUserId, :verifiedByName,
                 :terminalCode, :remarks)
            """, new MapSqlParameterSource()
            .addValue("id", nextId("SFR"))
            .addValue("applicationId", applicationId)
            .addValue("specimenId", specimenId)
            .addValue("fixationStatus", FixationStatus.PENDING.name())
            .addValue("verificationCompletedAt", verificationCompletedAt)
            .addValue("verifiedByUserId", verifiedByUserId)
            .addValue("verifiedByName", verifiedByName)
            .addValue("terminalCode", terminalCode)
            .addValue("remarks", remarks));
    }

    public void updateSpecimenStatus(String specimenId,
                                     SpecimenStatus specimenStatus,
                                     FixationStatus fixationStatus,
                                     String unqualifiedReason,
                                     String remarks,
                                     String caseId) {
        jdbcTemplate.update("""
            update specimens
            set specimen_status = :specimenStatus,
                fixation_status = :fixationStatus,
                unqualified_reason = :unqualifiedReason,
                remarks = :remarks,
                case_id = COALESCE(:caseId, case_id),
                updated_at = :updatedAt
            where id = :specimenId
            """, new MapSqlParameterSource()
            .addValue("specimenId", specimenId)
            .addValue("specimenStatus", specimenStatus.name())
            .addValue("fixationStatus", fixationStatus.name())
            .addValue("unqualifiedReason", unqualifiedReason)
            .addValue("remarks", remarks)
            .addValue("caseId", caseId)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    protected String nextId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
