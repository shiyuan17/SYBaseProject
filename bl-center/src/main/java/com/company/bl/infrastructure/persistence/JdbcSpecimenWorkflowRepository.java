package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.ReceiptStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.enums.TransportItemStatus;
import com.company.bl.domain.enums.TransportOrderStatus;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.domain.model.TransportOrderItem;
import com.company.bl.domain.repository.SpecimenWorkflowCommandRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Repository
public class JdbcSpecimenWorkflowRepository extends AbstractJdbcSpecimenWorkflowReadSupport implements SpecimenWorkflowCommandRepository {

    public JdbcSpecimenWorkflowRepository(NamedParameterJdbcTemplate jdbcTemplate) {
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

    @Override
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

    @Override
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

    @Override

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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public TransportOrder insertTransportOrder(TransportOrder order) {
        jdbcTemplate.update("""
            insert into transport_orders
                (id, transport_order_no, application_id, order_status, handover_user_id, handover_user_name,
                 handover_department_id, handover_department_name, receiver_department_id, receiver_department_name,
                 receiver_user_id, receiver_user_name, printed_at, to_be_transported_at, handed_over_at,
                 terminal_code, remarks, created_at, updated_at)
            values
                (:id, :transportOrderNo, :applicationId, :status, :handoverUserId, :handoverUserName,
                 :handoverDepartmentId, :handoverDepartmentName, :receiverDepartmentId, :receiverDepartmentName,
                 :receiverUserId, :receiverUserName, :printedAt, :toBeTransportedAt, :handedOverAt,
                 :terminalCode, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", order.id())
            .addValue("transportOrderNo", order.transportOrderNo())
            .addValue("applicationId", order.applicationId())
            .addValue("status", order.status().name())
            .addValue("handoverUserId", order.handoverUserId())
            .addValue("handoverUserName", order.handoverUserName())
            .addValue("handoverDepartmentId", order.handoverDepartmentId())
            .addValue("handoverDepartmentName", order.handoverDepartmentName())
            .addValue("receiverDepartmentId", order.receiverDepartmentId())
            .addValue("receiverDepartmentName", order.receiverDepartmentName())
            .addValue("receiverUserId", order.receiverUserId())
            .addValue("receiverUserName", order.receiverUserName())
            .addValue("printedAt", order.printedAt())
            .addValue("toBeTransportedAt", order.toBeTransportedAt())
            .addValue("handedOverAt", order.handedOverAt())
            .addValue("terminalCode", order.terminalCode())
            .addValue("remarks", order.remarks())
            .addValue("createdAt", LocalDateTime.now())
            .addValue("updatedAt", LocalDateTime.now()));
        return order;
    }

    @Override
    public TransportOrder updateTransportOrderStatus(String transportOrderId,
                                                     TransportOrderStatus status,
                                                     String receiverUserId,
                                                     String receiverUserName,
                                                     LocalDateTime printedAt,
                                                     LocalDateTime handedOverAt) {
        jdbcTemplate.update("""
            update transport_orders
            set order_status = :status,
                receiver_user_id = COALESCE(:receiverUserId, receiver_user_id),
                receiver_user_name = COALESCE(:receiverUserName, receiver_user_name),
                printed_at = COALESCE(:printedAt, printed_at),
                handed_over_at = COALESCE(:handedOverAt, handed_over_at),
                updated_at = :updatedAt
            where id = :transportOrderId
            """, new MapSqlParameterSource()
            .addValue("transportOrderId", transportOrderId)
            .addValue("status", status.name())
            .addValue("receiverUserId", receiverUserId)
            .addValue("receiverUserName", receiverUserName)
            .addValue("printedAt", printedAt)
            .addValue("handedOverAt", handedOverAt)
            .addValue("updatedAt", LocalDateTime.now()));
        return findTransportOrderById(transportOrderId).orElseThrow();
    }

    @Override
    public void insertTransportOrderItem(TransportOrderItem item) {
        jdbcTemplate.update("""
            insert into transport_order_items
                (id, transport_order_id, application_id, specimen_id, item_status, verification_result,
                 verified_by_user_id, verified_by_name, verified_at, remarks)
            values
                (:id, :transportOrderId, :applicationId, :specimenId, :status, :verificationResult,
                 :verifiedByUserId, :verifiedByName, :verifiedAt, :remarks)
            """, new MapSqlParameterSource()
            .addValue("id", item.id())
            .addValue("transportOrderId", item.transportOrderId())
            .addValue("applicationId", item.applicationId())
            .addValue("specimenId", item.specimenId())
            .addValue("status", item.status().name())
            .addValue("verificationResult", item.verificationResult())
            .addValue("verifiedByUserId", item.verifiedByUserId())
            .addValue("verifiedByName", item.verifiedByName())
            .addValue("verifiedAt", item.verifiedAt())
            .addValue("remarks", item.remarks()));
    }

    @Override
    public void updateTransportOrderItemStatus(String transportOrderId,
                                               String specimenId,
                                               TransportItemStatus status,
                                               String verificationResult,
                                               String verifiedByUserId,
                                               String verifiedByName,
                                               LocalDateTime verifiedAt,
                                               String remarks) {
        jdbcTemplate.update("""
            update transport_order_items
            set item_status = :status,
                verification_result = :verificationResult,
                verified_by_user_id = :verifiedByUserId,
                verified_by_name = :verifiedByName,
                verified_at = :verifiedAt,
                remarks = :remarks
            where transport_order_id = :transportOrderId
              and specimen_id = :specimenId
            """, new MapSqlParameterSource()
            .addValue("transportOrderId", transportOrderId)
            .addValue("specimenId", specimenId)
            .addValue("status", status.name())
            .addValue("verificationResult", verificationResult)
            .addValue("verifiedByUserId", verifiedByUserId)
            .addValue("verifiedByName", verifiedByName)
            .addValue("verifiedAt", verifiedAt)
            .addValue("remarks", remarks));
    }

    @Override
    public PathologyCase insertPathologyCase(PathologyCase pathologyCase) {
        jdbcTemplate.update("""
            insert into pathology_cases
                (id, application_id, pathology_no, case_status, source_hospital_id, source_hospital_name,
                 source_department_id, source_department_name, received_by_user_id, received_by_name,
                 received_at, created_at, updated_at)
            values
                (:id, :applicationId, :pathologyNo, :caseStatus, :sourceHospitalId, :sourceHospitalName,
                 :sourceDepartmentId, :sourceDepartmentName, :receivedByUserId, :receivedByName,
                 :receivedAt, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", pathologyCase.id())
            .addValue("applicationId", pathologyCase.applicationId())
            .addValue("pathologyNo", pathologyCase.pathologyNo())
            .addValue("caseStatus", pathologyCase.caseStatus())
            .addValue("sourceHospitalId", pathologyCase.sourceHospitalId())
            .addValue("sourceHospitalName", pathologyCase.sourceHospitalName())
            .addValue("sourceDepartmentId", pathologyCase.sourceDepartmentId())
            .addValue("sourceDepartmentName", pathologyCase.sourceDepartmentName())
            .addValue("receivedByUserId", pathologyCase.receivedByUserId())
            .addValue("receivedByName", pathologyCase.receivedByName())
            .addValue("receivedAt", pathologyCase.receivedAt())
            .addValue("createdAt", LocalDateTime.now())
            .addValue("updatedAt", LocalDateTime.now()));
        return pathologyCase;
    }

    @Override
    public void insertSpecimenReceipt(String applicationId,
                                      String caseId,
                                      String specimenId,
                                      String transportOrderId,
                                      ReceiptStatus receiptStatus,
                                      Integer containerCount,
                                      String qualityCheckResult,
                                      String qualityIssueCodes,
                                      String barcode,
                                      String receivedByUserId,
                                      String receivedByName,
                                      LocalDateTime receivedAt,
                                      String terminalCode,
                                      String rejectReason,
                                      String returnReason,
                                      String remarks) {
        jdbcTemplate.update("""
            insert into specimen_receipts
                (id, application_id, case_id, specimen_id, transport_order_id, receipt_status, container_count,
                 quality_check_result, quality_issue_codes, barcode, received_by_user_id, received_by_name,
                 received_at, terminal_code, reject_reason, return_reason, remarks)
            values
                (:id, :applicationId, :caseId, :specimenId, :transportOrderId, :receiptStatus, :containerCount,
                 :qualityCheckResult, :qualityIssueCodes, :barcode, :receivedByUserId, :receivedByName,
                 :receivedAt, :terminalCode, :rejectReason, :returnReason, :remarks)
            """, new MapSqlParameterSource()
            .addValue("id", nextId("SR"))
            .addValue("applicationId", applicationId)
            .addValue("caseId", caseId)
            .addValue("specimenId", specimenId)
            .addValue("transportOrderId", transportOrderId)
            .addValue("receiptStatus", receiptStatus.name())
            .addValue("containerCount", containerCount)
            .addValue("qualityCheckResult", qualityCheckResult)
            .addValue("qualityIssueCodes", qualityIssueCodes)
            .addValue("barcode", barcode)
            .addValue("receivedByUserId", receivedByUserId)
            .addValue("receivedByName", receivedByName)
            .addValue("receivedAt", receivedAt)
            .addValue("terminalCode", terminalCode)
            .addValue("rejectReason", rejectReason)
            .addValue("returnReason", returnReason)
            .addValue("remarks", remarks));
    }

    @Override
    public void insertWorkflowEvent(TrackingEvent event) {
        jdbcTemplate.update("""
            insert into workflow_events
                (id, application_id, specimen_id, case_id, transport_order_id, node_code, event_type,
                 event_status, event_time, operator_user_id, operator_name, source_terminal, event_content, created_at)
            values
                (:id, :applicationId, :specimenId, :caseId, :transportOrderId, :nodeCode, :eventType,
                 :eventStatus, :eventTime, :operatorUserId, :operatorName, :sourceTerminal, :eventContent, :createdAt)
            """, new MapSqlParameterSource()
            .addValue("id", event.id())
            .addValue("applicationId", event.applicationId())
            .addValue("specimenId", event.specimenId())
            .addValue("caseId", event.caseId())
            .addValue("transportOrderId", event.transportOrderId())
            .addValue("nodeCode", event.nodeCode())
            .addValue("eventType", event.eventType())
            .addValue("eventStatus", event.eventStatus())
            .addValue("eventTime", event.eventTime())
            .addValue("operatorUserId", event.operatorUserId())
            .addValue("operatorName", event.operatorName())
            .addValue("sourceTerminal", event.sourceTerminal())
            .addValue("eventContent", event.eventContent())
            .addValue("createdAt", LocalDateTime.now()));
    }

    @Override
    public void upsertTechnicalPendingTask(String applicationId, String caseId, String payload) {
        Long count = jdbcTemplate.queryForObject("""
            select count(1)
            from technical_pending_tasks
            where case_id = :caseId
              and task_type = 'GROSSING'
              and object_type = 'CASE'
              and object_id = :caseId
              and task_status in ('PENDING', 'IN_PROGRESS')
            """, Map.of("caseId", caseId), Long.class);
        if (count != null && count > 0) {
            jdbcTemplate.update("""
                update technical_pending_tasks
                set payload = :payload,
                    updated_at = :updatedAt
                where case_id = :caseId
                  and task_type = 'GROSSING'
                  and object_type = 'CASE'
                  and object_id = :caseId
                  and task_status in ('PENDING', 'IN_PROGRESS')
                """, new MapSqlParameterSource()
                .addValue("caseId", caseId)
                .addValue("payload", payload)
                .addValue("updatedAt", LocalDateTime.now()));
            return;
        }
        jdbcTemplate.update("""
            insert into technical_pending_tasks
                (id, application_id, case_id, specimen_id, task_type, task_status, object_type, object_id,
                 parent_task_id, payload, created_at, updated_at, remarks)
            values
                (:id, :applicationId, :caseId, null, 'GROSSING', 'PENDING', 'CASE', :caseId,
                 null, :payload, :createdAt, :updatedAt, null)
            """, new MapSqlParameterSource()
            .addValue("id", nextId("TT"))
            .addValue("applicationId", applicationId)
            .addValue("caseId", caseId)
            .addValue("payload", payload)
            .addValue("createdAt", LocalDateTime.now())
            .addValue("updatedAt", LocalDateTime.now()));
    }


    private String nextId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

}
