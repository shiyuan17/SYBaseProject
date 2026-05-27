package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.ReceiptStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.enums.TransportItemStatus;
import com.company.bl.domain.enums.TransportOrderStatus;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.domain.model.TransportOrderItem;
import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcSpecimenWorkflowRepository implements SpecimenWorkflowRepository {

    private static final String SPECIMEN_SELECT_COLUMNS = """
        select
            s.*,
            latest_receipt.receipt_status as latest_receipt_status,
            latest_receipt.quality_check_result as latest_quality_check_result,
            latest_receipt.quality_issue_codes as latest_quality_issue_codes
        from specimens s
        left join (
            select
                ranked.specimen_id,
                ranked.receipt_status,
                ranked.quality_check_result,
                ranked.quality_issue_codes
            from (
                select
                    sr.*,
                    row_number() over (
                        partition by sr.specimen_id
                        order by sr.received_at desc, sr.id desc
                    ) as rn
                from specimen_receipts sr
            ) ranked
            where ranked.rn = 1
        ) latest_receipt on latest_receipt.specimen_id = s.id
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private volatile Boolean specimenContainerColumnsAvailable;
    private volatile Boolean collectionPrinterCodeColumnAvailable;

    public JdbcSpecimenWorkflowRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Specimen> findSpecimenByBarcode(String barcode) {
        List<Specimen> rows = jdbcTemplate.query(
            SPECIMEN_SELECT_COLUMNS + """
                where s.barcode = :barcode
                """,
            Map.of("barcode", barcode),
            this::mapSpecimen);
        return rows.stream().findFirst();
    }

    @Override
    public List<Specimen> findSpecimensByApplicationId(String applicationId) {
        return jdbcTemplate.query(
            SPECIMEN_SELECT_COLUMNS + """
                where s.application_id = :applicationId
                order by s.registered_at asc, s.created_at asc
                """,
            Map.of("applicationId", applicationId),
            this::mapSpecimen);
    }

    @Override
    public Optional<PathologyCase> findPathologyCaseByApplicationId(String applicationId) {
        List<PathologyCase> rows = jdbcTemplate.query("""
            select *
            from pathology_cases
            where application_id = :applicationId
            """, Map.of("applicationId", applicationId), this::mapPathologyCase);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<TransportOrder> findTransportOrderById(String transportOrderId) {
        List<TransportOrder> rows = jdbcTemplate.query("""
            select *
            from transport_orders
            where id = :id
            """, Map.of("id", transportOrderId), this::mapTransportOrder);
        return rows.stream().findFirst();
    }

    @Override
    public List<TransportOrderItem> findTransportOrderItems(String transportOrderId) {
        return jdbcTemplate.query("""
            select *
            from transport_order_items
            where transport_order_id = :transportOrderId
            order by verified_at asc, id asc
            """, Map.of("transportOrderId", transportOrderId), this::mapTransportOrderItem);
    }

    @Override
    public List<String> findTransportOrderSpecimenBarcodes(String transportOrderId) {
        return jdbcTemplate.query("""
            select s.barcode
            from transport_order_items toi
            join specimens s on s.id = toi.specimen_id
            where toi.transport_order_id = :transportOrderId
            order by s.registered_at asc, s.id asc
            """, Map.of("transportOrderId", transportOrderId), (rs, rowNum) -> rs.getString("barcode"));
    }

    @Override
    public List<TrackingEvent> findTrackingEventsByApplicationId(String applicationId) {
        return jdbcTemplate.query("""
            select *
            from workflow_events
            where application_id = :applicationId
            order by event_time asc, created_at asc
            """, Map.of("applicationId", applicationId), this::mapTrackingEvent);
    }

    @Override
    public Optional<String> findApplicationIdByBarcode(String barcode) {
        List<String> rows = jdbcTemplate.query("""
            select application_id
            from specimens
            where barcode = :barcode
            """, Map.of("barcode", barcode), (rs, rowNum) -> rs.getString("application_id"));
        return rows.stream().findFirst();
    }

    @Override
    public boolean existsApplicationByExternalSource(String externalOrderNo, String thirdPartySource) {
        Long count = jdbcTemplate.queryForObject("""
            select count(1)
            from applications
            where external_order_no = :externalOrderNo
              and third_party_source = :thirdPartySource
            """, new MapSqlParameterSource()
            .addValue("externalOrderNo", externalOrderNo)
            .addValue("thirdPartySource", thirdPartySource), Long.class);
        return count != null && count > 0;
    }

    @Override
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
    public List<Specimen> findSpecimensByLabelPrintBatchNoAndStatus(String labelPrintBatchNo, String labelPrintStatus) {
        return jdbcTemplate.query(
            SPECIMEN_SELECT_COLUMNS + """
                where s.label_print_batch_no = :labelPrintBatchNo
                  and s.label_print_status = :labelPrintStatus
                order by s.registered_at asc, s.id asc
                """,
            new MapSqlParameterSource()
            .addValue("labelPrintBatchNo", labelPrintBatchNo)
            .addValue("labelPrintStatus", labelPrintStatus),
            this::mapSpecimen);
    }

    @Override
    public List<Specimen> findSpecimensByLabelPrintBatchNoAndStatuses(String labelPrintBatchNo, List<String> labelPrintStatuses) {
        return jdbcTemplate.query(
            SPECIMEN_SELECT_COLUMNS + """
                where s.label_print_batch_no = :labelPrintBatchNo
                  and s.label_print_status in (:labelPrintStatuses)
                order by s.registered_at asc, s.id asc
                """,
            new MapSqlParameterSource()
            .addValue("labelPrintBatchNo", labelPrintBatchNo)
            .addValue("labelPrintStatuses", labelPrintStatuses),
            this::mapSpecimen);
    }

    @Override
    public Optional<RegistrationSnapshotData> findRegistrationSnapshotByApplicationIdAndBatchNo(
        String applicationId,
        String labelPrintBatchNo
    ) {
        String printerCodeSelect = hasCollectionPrinterCodeColumn()
            ? "printer_code"
            : "cast(null as varchar(64)) as printer_code";
        List<RegistrationSnapshotData> rows = jdbcTemplate.query("""
            select
                collection_scene,
                collector_user_id,
                collector_name,
                %s,
                terminal_code,
                remarks
            from specimen_collection_records
            where application_id = :applicationId
              and label_print_batch_no = :labelPrintBatchNo
            order by collected_at desc, id desc
            fetch next 1 rows only
            """.formatted(printerCodeSelect), new MapSqlParameterSource()
            .addValue("applicationId", applicationId)
            .addValue("labelPrintBatchNo", labelPrintBatchNo), this::mapRegistrationSnapshot);
        return rows.stream().findFirst();
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
                    fixation_liquid_type = :fixationLiquidType,
                    fixation_start_at = COALESCE(:fixationStartAt, fixation_start_at),
                    fixation_completed_at = :fixationCompletedAt,
                    verified_by_user_id = :verifiedByUserId,
                    verified_by_name = :verifiedByName,
                    verified_at = :verifiedAt,
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

    @Override
    public PagedPendingSpecimens findPendingFixations(PendingSpecimenQuery query) {
        String whereClause = """
            from specimens s
            join applications a on a.id = s.application_id
            left join (
                select specimen_id, max(event_time) as latest_event_time
                from workflow_events
                group by specimen_id
            ) evt on evt.specimen_id = s.id
            where (
                    (s.specimen_status = 'REGISTERED' and coalesce(s.fixation_status, 'PENDING') = 'PENDING')
                    or (s.specimen_status = 'FIXING' and s.fixation_status = 'FIXING')
                    or (s.specimen_status = 'FIXED' and s.fixation_status = 'COMPLETED')
                  )
              and not exists (
                    select 1
                    from transport_order_items toi
                    where toi.specimen_id = s.id
                )
            """ + buildPendingFilters(query, "a", "s");
        long total = countPending(whereClause, query);
        List<PendingSpecimenRow> items = queryPending("""
            select
                a.id as application_id,
                a.application_no,
                a.patient_name,
                a.submitting_department_id,
                a.submitting_department_name,
                cast(null as varchar(64)) as transport_order_id,
                s.id as specimen_id,
                s.specimen_no,
                s.barcode,
            """ + containerNameSelect("s")
                + containerCountSelect("s")
                + """
                s.specimen_status,
                s.fixation_status,
                s.registered_at,
                evt.latest_event_time,
                case
                    when s.label_print_status = 'FAILED'
                        or s.specimen_status in ('REJECTED', 'RETURNED')
                        or s.fixation_status = 'ABNORMAL'
                        or s.unqualified_reason is not null
                    then 1 else 0
                end as abnormal_flag
            """ + whereClause + " order by s.registered_at asc, s.id asc", query);
        return new PagedPendingSpecimens(items, total);
    }

    @Override
    public PagedPendingSpecimens findPendingReceipts(PendingSpecimenQuery query) {
        String whereClause = """
            from specimens s
            join applications a on a.id = s.application_id
            left join (
                select specimen_id, max(event_time) as latest_event_time
                from workflow_events
                group by specimen_id
            ) evt on evt.specimen_id = s.id
            where (
                    exists (
                        select 1
                        from transport_order_items toi
                        join transport_orders t on t.id = toi.transport_order_id
                        where toi.specimen_id = s.id
                          and t.order_status in ('PRINTED', 'HANDED_OVER', 'PARTIALLY_RECEIVED')
                    )
                    or s.specimen_status = 'IN_TRANSIT'
                  )
              and s.specimen_status not in ('RECEIVED', 'REJECTED', 'RETURNED')
              and not exists (
                    select 1
                    from specimen_receipts sr
                    where sr.specimen_id = s.id
                      and sr.receipt_status = 'RECEIVED'
                )
            """ + buildPendingFilters(query, "a", "s");
        long total = countPending(whereClause, query);
        List<PendingSpecimenRow> items = queryPending("""
            select
                a.id as application_id,
                a.application_no,
                a.patient_name,
                a.submitting_department_id,
                a.submitting_department_name,
                (
                    select toi.transport_order_id
                    from transport_order_items toi
                    join transport_orders t on t.id = toi.transport_order_id
                    where toi.specimen_id = s.id
                      and t.order_status in ('PRINTED', 'HANDED_OVER', 'PARTIALLY_RECEIVED')
                    order by toi.verified_at desc, toi.id desc
                    fetch next 1 rows only
                ) as transport_order_id,
                s.id as specimen_id,
                s.specimen_no,
                s.barcode,
            """ + containerNameSelect("s")
                + containerCountSelect("s")
                + """
                s.specimen_status,
                s.fixation_status,
                s.registered_at,
                evt.latest_event_time,
                case
                    when s.label_print_status = 'FAILED'
                        or s.specimen_status in ('REJECTED', 'RETURNED')
                        or s.fixation_status = 'ABNORMAL'
                        or s.unqualified_reason is not null
                    then 1 else 0
                end as abnormal_flag
            """ + whereClause + " order by coalesce(evt.latest_event_time, s.registered_at) asc, s.id asc", query);
        return new PagedPendingSpecimens(items, total);
    }

    @Override
    public PagedPendingTransportOrders findPendingTransportOrders(PendingTransportOrderQuery query) {
        String whereClause = """
            from transport_orders t
            join applications a on a.id = t.application_id
            where t.order_status <> 'COMPLETED'
            """ + buildTransportPendingFilters(query);
        long total = countPendingTransportOrders(whereClause, query);
        List<PendingTransportOrderRow> items = queryPendingTransportOrders("""
            select
                t.id,
                t.transport_order_no,
                t.application_id,
                a.application_no,
                a.patient_name,
                t.handover_department_name,
                t.receiver_department_name,
                t.order_status,
                t.to_be_transported_at,
                t.handed_over_at
            """ + whereClause + " order by t.to_be_transported_at asc, t.id asc", query);
        return new PagedPendingTransportOrders(items, total);
    }

    @Override
    public PagedApplications findApplications(ApplicationListQuery query) {
        String whereClause = """
            from applications a
            where 1 = 1
            """ + buildApplicationFilters(query);
        long total = countApplications(whereClause, query);
        List<ApplicationListRow> items = queryApplications("""
            select
                a.id,
                a.application_no,
                a.patient_name,
                a.patient_gender,
                a.patient_age,
                a.status,
                a.submitting_department_name,
                a.submitting_doctor_name,
                a.application_type,
                a.application_form_status,
                coalesce(
                    (
                        select we.node_code
                        from workflow_events we
                        where we.application_id = a.id
                        order by we.event_time desc, we.created_at desc, we.id desc
                        fetch next 1 rows only
                    ),
                    a.status
                ) as current_node,
                case
                    when exists (
                        select 1
                        from specimens s
                        where s.application_id = a.id
                          and (
                              s.specimen_status in ('REJECTED', 'RETURNED')
                              or s.fixation_status = 'ABNORMAL'
                          )
                    )
                    then 1 else 0
                end as abnormal_flag,
                (
                    select count(1)
                    from specimens s
                    where s.application_id = a.id
                ) as registered_specimen_count,
                (
                    select case
                        when sum(case when sb.label_print_status = 'FAILED' then 1 else 0 end) > 0 then 'FAILED'
                        when sum(case when sb.label_print_status = 'PENDING' then 1 else 0 end) > 0 then 'PENDING'
                        when sum(case when sb.label_print_status = 'SUCCESS' then 1 else 0 end) > 0 then 'SUCCESS'
                        else null
                    end
                    from specimens sb
                    where sb.application_id = a.id
                      and sb.label_print_batch_no = (
                          select latest.label_print_batch_no
                          from specimens latest
                          where latest.application_id = a.id
                            and latest.label_print_batch_no is not null
                          order by latest.registered_at desc, latest.created_at desc, latest.id desc
                          fetch next 1 rows only
                      )
                ) as latest_label_print_status,
                a.application_date,
                a.submission_date,
                a.created_at,
                a.updated_at
            """ + whereClause + " order by coalesce(a.updated_at, a.created_at) desc, a.id desc", query);
        return new PagedApplications(items, total);
    }

    @Override
    public List<DuplicateApplicationRow> findDuplicateApplications(DuplicateApplicationQuery query) {
        String sql = """
            select
                a.id,
                a.application_no,
                a.patient_name,
                a.specimen_site,
                a.status,
                a.application_date,
                case
                    when :externalOrderNo is not null and a.external_order_no = :externalOrderNo then 1
                    else 0
                end as external_order_matched,
                case
                    when :applicationDate is not null
                         and :applicationType is not null
                         and :specimenSite is not null
                         and a.application_date = :applicationDate
                         and a.application_type = :applicationType
                         and a.specimen_site = :specimenSite then 1
                    else 0
                end as same_day_site_matched,
                coalesce(
                    (
                        select we.node_code
                        from workflow_events we
                        where we.application_id = a.id
                        order by we.event_time desc, we.created_at desc, we.id desc
                        fetch next 1 rows only
                    ),
                    a.status
                ) as current_node
            from applications a
            where (
                (:patientId is not null and a.patient_id = :patientId)
                or (:patientName is not null and a.patient_name = :patientName)
            )
            and (
                (:externalOrderNo is not null and a.external_order_no = :externalOrderNo)
                or (
                    :applicationDate is not null
                    and :applicationType is not null
                    and :specimenSite is not null
                    and a.application_date = :applicationDate
                    and a.application_type = :applicationType
                    and a.specimen_site = :specimenSite
                )
            )
            order by coalesce(a.updated_at, a.created_at) desc, a.id desc
            """;
        return jdbcTemplate.query(sql, duplicateApplicationParams(query), this::mapDuplicateApplicationRow);
    }

    @Override
    public PagedSpecimenManagementItems findSpecimenManagementItems(SpecimenManagementListQuery query) {
        String abnormalExpression = specimenManagementAbnormalExpression("s");
        String whereClause = """
            from specimens s
            join applications a on a.id = s.application_id
            left join (
                select specimen_id, max(event_time) as latest_event_time
                from workflow_events
                group by specimen_id
            ) evt on evt.specimen_id = s.id
            where 1 = 1
            """ + buildSpecimenManagementFilters(query, abnormalExpression);
        long total = countSpecimenManagement(whereClause, query);
        List<SpecimenManagementListRow> items = querySpecimenManagement(
            """
            select
                s.id as specimen_id,
                s.specimen_no,
                s.barcode,
                a.id as application_id,
                a.application_no,
                a.patient_name,
                a.submitting_department_id,
                a.submitting_department_name,
                s.specimen_name_standardized as specimen_name,
                s.specimen_type,
                s.specimen_site,
                s.specimen_count,
            """ + containerNameSelect("s")
                + containerCountSelect("s")
                + """
                s.specimen_status,
                s.fixation_status,
                s.label_print_status,
                s.label_print_batch_no,
                s.registered_at,
                evt.latest_event_time,
            """
                + "    case when (" + abnormalExpression + ")\n"
                + """
                    then 1 else 0
                end as abnormal_flag
            """
                + whereClause
                + " order by coalesce(s.registered_at, evt.latest_event_time) desc, s.id desc",
            query);
        SpecimenManagementSummary summary = summarizeSpecimenManagement(whereClause, query);
        return new PagedSpecimenManagementItems(items, total, summary);
    }

    @Override
    public ApplicationTracking getApplicationTracking(String applicationId, Application application) {
        List<Specimen> specimens = findSpecimensByApplicationId(applicationId);
        List<TrackingEvent> events = findTrackingEventsByApplicationId(applicationId);
        boolean abnormal = specimens.stream().anyMatch(specimen ->
            specimen.specimenStatus() == SpecimenStatus.REJECTED
                || specimen.specimenStatus() == SpecimenStatus.RETURNED
                || specimen.fixationStatus() == FixationStatus.ABNORMAL);
        String currentNode = events.isEmpty()
            ? application.getStatus().name()
            : events.get(events.size() - 1).nodeCode();
        return new ApplicationTracking(application, currentNode, abnormal, specimens, events);
    }

    private Specimen mapSpecimen(ResultSet rs, int rowNum) throws SQLException {
        return new Specimen(
            rs.getString("id"),
            rs.getString("application_id"),
            rs.getString("case_id"),
            rs.getString("specimen_no"),
            rs.getString("barcode"),
            rs.getString("specimen_type"),
            rs.getString("specimen_name_standardized"),
            rs.getString("specimen_site"),
            rs.getString("collection_mode"),
            rs.getObject("specimen_count", Integer.class),
            JdbcResultSetUtils.getNullableString(rs, "container_name"),
            JdbcResultSetUtils.getNullableInteger(rs, "container_count"),
            SpecimenStatus.from(rs.getString("specimen_status")),
            FixationStatus.from(rs.getString("fixation_status")),
            rs.getInt("qualified_flag") != 0,
            rs.getString("unqualified_reason"),
            rs.getString("latest_receipt_status"),
            rs.getString("latest_quality_check_result"),
            rs.getString("latest_quality_issue_codes"),
            rs.getString("clinical_symptom"),
            rs.getString("applicant_department_id"),
            rs.getString("applicant_department_name"),
            rs.getString("applicant_doctor_user_id"),
            rs.getString("applicant_doctor_name"),
            rs.getDate("submission_date") == null ? null : rs.getDate("submission_date").toLocalDate(),
            rs.getString("label_print_batch_no"),
            rs.getString("label_print_status"),
            rs.getString("registered_by_user_id"),
            rs.getString("registered_by_name"),
            rs.getTimestamp("registered_at") == null ? null : rs.getTimestamp("registered_at").toLocalDateTime(),
            rs.getString("terminal_code"),
            rs.getString("remarks"));
    }

    private PathologyCase mapPathologyCase(ResultSet rs, int rowNum) throws SQLException {
        return new PathologyCase(
            rs.getString("id"),
            rs.getString("application_id"),
            rs.getString("pathology_no"),
            rs.getString("case_status"),
            rs.getString("source_hospital_id"),
            rs.getString("source_hospital_name"),
            rs.getString("source_department_id"),
            rs.getString("source_department_name"),
            rs.getString("received_by_user_id"),
            rs.getString("received_by_name"),
            rs.getTimestamp("received_at") == null ? null : rs.getTimestamp("received_at").toLocalDateTime());
    }

    private RegistrationSnapshotData mapRegistrationSnapshot(ResultSet rs, int rowNum) throws SQLException {
        return new RegistrationSnapshotData(
            rs.getString("collection_scene"),
            rs.getString("collector_user_id"),
            rs.getString("collector_name"),
            JdbcResultSetUtils.getNullableString(rs, "printer_code"),
            rs.getString("terminal_code"),
            rs.getString("remarks"));
    }

    private TransportOrder mapTransportOrder(ResultSet rs, int rowNum) throws SQLException {
        return new TransportOrder(
            rs.getString("id"),
            rs.getString("transport_order_no"),
            rs.getString("application_id"),
            TransportOrderStatus.from(rs.getString("order_status")),
            rs.getString("handover_user_id"),
            rs.getString("handover_user_name"),
            rs.getString("handover_department_id"),
            rs.getString("handover_department_name"),
            rs.getString("receiver_department_id"),
            rs.getString("receiver_department_name"),
            rs.getString("receiver_user_id"),
            rs.getString("receiver_user_name"),
            rs.getTimestamp("printed_at") == null ? null : rs.getTimestamp("printed_at").toLocalDateTime(),
            rs.getTimestamp("to_be_transported_at") == null ? null : rs.getTimestamp("to_be_transported_at").toLocalDateTime(),
            rs.getTimestamp("handed_over_at") == null ? null : rs.getTimestamp("handed_over_at").toLocalDateTime(),
            rs.getString("terminal_code"),
            rs.getString("remarks"));
    }

    private TransportOrderItem mapTransportOrderItem(ResultSet rs, int rowNum) throws SQLException {
        return new TransportOrderItem(
            rs.getString("id"),
            rs.getString("transport_order_id"),
            rs.getString("application_id"),
            rs.getString("specimen_id"),
            TransportItemStatus.from(rs.getString("item_status")),
            rs.getString("verification_result"),
            rs.getString("verified_by_user_id"),
            rs.getString("verified_by_name"),
            rs.getTimestamp("verified_at") == null ? null : rs.getTimestamp("verified_at").toLocalDateTime(),
            rs.getString("remarks"));
    }

    private TrackingEvent mapTrackingEvent(ResultSet rs, int rowNum) throws SQLException {
        return new TrackingEvent(
            rs.getString("id"),
            rs.getString("application_id"),
            rs.getString("specimen_id"),
            rs.getString("case_id"),
            rs.getString("transport_order_id"),
            rs.getString("node_code"),
            rs.getString("event_type"),
            rs.getString("event_status"),
            rs.getTimestamp("event_time").toLocalDateTime(),
            rs.getString("operator_user_id"),
            rs.getString("operator_name"),
            rs.getString("source_terminal"),
            rs.getString("event_content"));
    }

    private long countPending(String whereClause, PendingSpecimenQuery query) {
        Long total = jdbcTemplate.queryForObject(
            "select count(1) " + whereClause,
            pendingParams(query, false),
            Long.class);
        return total == null ? 0L : total;
    }

    private long countPendingTransportOrders(String whereClause, PendingTransportOrderQuery query) {
        Long total = jdbcTemplate.queryForObject(
            "select count(1) " + whereClause,
            pendingTransportOrderParams(query),
            Long.class);
        return total == null ? 0L : total;
    }

    private long countApplications(String whereClause, ApplicationListQuery query) {
        Long total = jdbcTemplate.queryForObject(
            "select count(1) " + whereClause,
            applicationParams(query),
            Long.class);
        return total == null ? 0L : total;
    }

    private List<PendingSpecimenRow> queryPending(String sql, PendingSpecimenQuery query) {
        MapSqlParameterSource parameters = pendingParams(query, true)
            .addValue("offset", Math.max(0, (query.page() - 1) * query.size()))
            .addValue("size", query.size());
        return jdbcTemplate.query(sql + " offset :offset rows fetch next :size rows only", parameters, this::mapPendingSpecimenRow);
    }

    private List<PendingTransportOrderRow> queryPendingTransportOrders(String sql, PendingTransportOrderQuery query) {
        MapSqlParameterSource parameters = pendingTransportOrderParams(query)
            .addValue("offset", Math.max(0, (query.page() - 1) * query.size()))
            .addValue("size", query.size());
        return jdbcTemplate.query(sql + " offset :offset rows fetch next :size rows only", parameters, this::mapPendingTransportOrderRow);
    }

    private List<ApplicationListRow> queryApplications(String sql, ApplicationListQuery query) {
        MapSqlParameterSource parameters = applicationParams(query)
            .addValue("offset", Math.max(0, (query.page() - 1) * query.size()))
            .addValue("size", query.size());
        return jdbcTemplate.query(sql + " offset :offset rows fetch next :size rows only", parameters, this::mapApplicationListRow);
    }

    private long countSpecimenManagement(String whereClause, SpecimenManagementListQuery query) {
        Long total = jdbcTemplate.queryForObject(
            "select count(1) " + whereClause,
            specimenManagementParams(query),
            Long.class);
        return total == null ? 0L : total;
    }

    private List<SpecimenManagementListRow> querySpecimenManagement(String sql, SpecimenManagementListQuery query) {
        MapSqlParameterSource parameters = specimenManagementParams(query)
            .addValue("offset", Math.max(0, (query.page() - 1) * query.size()))
            .addValue("size", query.size());
        return jdbcTemplate.query(
            sql + " offset :offset rows fetch next :size rows only",
            parameters,
            this::mapSpecimenManagementListRow);
    }

    private SpecimenManagementSummary summarizeSpecimenManagement(String whereClause, SpecimenManagementListQuery query) {
        return jdbcTemplate.queryForObject(
            """
            select
                count(1) as total_count,
                sum(case when s.label_print_status = 'SUCCESS' then 1 else 0 end) as label_printed_count,
                sum(case when s.label_print_status in ('PENDING', 'FAILED') then 1 else 0 end) as pending_label_count,
            """
                + "    sum(case when (" + specimenManagementAbnormalExpression("s") + ")\n"
                + """
                    then 1 else 0
                end) as abnormal_count
            """
                + whereClause,
            specimenManagementParams(query),
            this::mapSpecimenManagementSummary);
    }

    private String buildPendingFilters(PendingSpecimenQuery query, String applicationAlias, String specimenAlias) {
        StringBuilder builder = new StringBuilder();
        if (query.applicationId() != null && !query.applicationId().isBlank()) {
            builder.append(" and ").append(applicationAlias).append(".id = :applicationId");
        }
        if (query.specimenNo() != null && !query.specimenNo().isBlank()) {
            builder.append(" and ").append(specimenAlias).append(".specimen_no = :specimenNo");
        }
        if (query.departmentId() != null && !query.departmentId().isBlank()) {
            builder.append(" and ").append(applicationAlias).append(".submitting_department_id = :departmentId");
        }
        if (query.fixationStatus() != null && !query.fixationStatus().isBlank()) {
            builder.append(" and ").append(specimenAlias).append(".fixation_status = :fixationStatus");
        }
        if (query.dateFrom() != null) {
            builder.append(" and ").append(specimenAlias).append(".registered_at >= :dateFrom");
        }
        if (query.dateTo() != null) {
            builder.append(" and ").append(specimenAlias).append(".registered_at < :dateTo");
        }
        return builder.toString();
    }

    private String buildTransportPendingFilters(PendingTransportOrderQuery query) {
        StringBuilder builder = new StringBuilder();
        if (query.applicationId() != null && !query.applicationId().isBlank()) {
            builder.append(" and a.id = :applicationId");
        }
        if (query.specimenNo() != null && !query.specimenNo().isBlank()) {
            builder.append("""
                 and exists (
                    select 1
                    from transport_order_items toi
                    join specimens s on s.id = toi.specimen_id
                    where toi.transport_order_id = t.id
                      and s.specimen_no = :specimenNo
                )
                """);
        }
        if (query.departmentId() != null && !query.departmentId().isBlank()) {
            builder.append(" and a.submitting_department_id = :departmentId");
        }
        if (query.dateFrom() != null) {
            builder.append(" and t.to_be_transported_at >= :dateFrom");
        }
        if (query.dateTo() != null) {
            builder.append(" and t.to_be_transported_at < :dateTo");
        }
        if (query.status() != null && !query.status().isBlank()) {
            builder.append(" and t.order_status = :status");
        }
        return builder.toString();
    }

    private String buildApplicationFilters(ApplicationListQuery query) {
        StringBuilder builder = new StringBuilder();
        if (query.applicationNo() != null && !query.applicationNo().isBlank()) {
            builder.append(" and a.application_no like :applicationNo");
        }
        if (query.patientName() != null && !query.patientName().isBlank()) {
            builder.append(" and a.patient_name like :patientName");
        }
        if (query.submittingDepartmentId() != null && !query.submittingDepartmentId().isBlank()) {
            builder.append(" and a.submitting_department_id = :submittingDepartmentId");
        }
        if (query.applicationType() != null && !query.applicationType().isBlank()) {
            builder.append(" and a.application_type = :applicationType");
        }
        if (query.applicationFormStatus() != null && !query.applicationFormStatus().isBlank()) {
            builder.append(" and a.application_form_status = :applicationFormStatus");
        }
        if (query.dateFrom() != null) {
            builder.append(" and a.application_date >= :dateFrom");
        }
        if (query.dateTo() != null) {
            builder.append(" and a.application_date < :dateTo");
        }
        return builder.toString();
    }

    private String buildSpecimenManagementFilters(
        SpecimenManagementListQuery query,
        String abnormalExpression
    ) {
        StringBuilder builder = new StringBuilder();
        if (query.keyword() != null && !query.keyword().isBlank()) {
            builder.append("""
                 and (
                    s.specimen_no like :keyword
                    or s.barcode like :keyword
                    or a.application_no like :keyword
                    or a.patient_name like :keyword
                )
                """);
        }
        if (query.applicationNo() != null && !query.applicationNo().isBlank()) {
            builder.append(" and a.application_no like :applicationNo");
        }
        if (query.departmentId() != null && !query.departmentId().isBlank()) {
            builder.append(" and a.submitting_department_id = :departmentId");
        }
        if (query.specimenStatus() != null && !query.specimenStatus().isBlank()) {
            builder.append(" and s.specimen_status = :specimenStatus");
        }
        if (query.labelPrintStatus() != null && !query.labelPrintStatus().isBlank()) {
            builder.append(" and s.label_print_status = :labelPrintStatus");
        }
        if (query.abnormalFlag() != null) {
            builder.append(
                query.abnormalFlag()
                    ? " and (" + abnormalExpression + ")"
                    : " and not (" + abnormalExpression + ")");
        }
        if (query.dateFrom() != null) {
            builder.append(" and s.registered_at >= :dateFrom");
        }
        if (query.dateTo() != null) {
            builder.append(" and s.registered_at < :dateTo");
        }
        return builder.toString();
    }

    private String specimenManagementAbnormalExpression(String specimenAlias) {
        return specimenAlias + ".specimen_status in ('REJECTED', 'RETURNED')"
            + " or " + specimenAlias + ".fixation_status = 'ABNORMAL'"
            + " or " + specimenAlias + ".unqualified_reason is not null";
    }

    private MapSqlParameterSource pendingParams(PendingSpecimenQuery query, boolean paged) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (query.applicationId() != null && !query.applicationId().isBlank()) {
            parameters.addValue("applicationId", query.applicationId());
        }
        if (query.specimenNo() != null && !query.specimenNo().isBlank()) {
            parameters.addValue("specimenNo", query.specimenNo());
        }
        if (query.departmentId() != null && !query.departmentId().isBlank()) {
            parameters.addValue("departmentId", query.departmentId());
        }
        if (query.fixationStatus() != null && !query.fixationStatus().isBlank()) {
            parameters.addValue("fixationStatus", query.fixationStatus());
        }
        if (query.dateFrom() != null) {
            parameters.addValue("dateFrom", query.dateFrom());
        }
        if (query.dateTo() != null) {
            parameters.addValue("dateTo", query.dateTo());
        }
        return parameters;
    }

    private MapSqlParameterSource pendingTransportOrderParams(PendingTransportOrderQuery query) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (query.applicationId() != null && !query.applicationId().isBlank()) {
            parameters.addValue("applicationId", query.applicationId());
        }
        if (query.specimenNo() != null && !query.specimenNo().isBlank()) {
            parameters.addValue("specimenNo", query.specimenNo());
        }
        if (query.departmentId() != null && !query.departmentId().isBlank()) {
            parameters.addValue("departmentId", query.departmentId());
        }
        if (query.dateFrom() != null) {
            parameters.addValue("dateFrom", query.dateFrom());
        }
        if (query.dateTo() != null) {
            parameters.addValue("dateTo", query.dateTo());
        }
        if (query.status() != null && !query.status().isBlank()) {
            parameters.addValue("status", query.status());
        }
        return parameters;
    }

    private MapSqlParameterSource applicationParams(ApplicationListQuery query) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (query.applicationNo() != null && !query.applicationNo().isBlank()) {
            parameters.addValue("applicationNo", "%" + query.applicationNo() + "%");
        }
        if (query.patientName() != null && !query.patientName().isBlank()) {
            parameters.addValue("patientName", "%" + query.patientName() + "%");
        }
        if (query.submittingDepartmentId() != null && !query.submittingDepartmentId().isBlank()) {
            parameters.addValue("submittingDepartmentId", query.submittingDepartmentId());
        }
        if (query.applicationType() != null && !query.applicationType().isBlank()) {
            parameters.addValue("applicationType", query.applicationType());
        }
        if (query.applicationFormStatus() != null && !query.applicationFormStatus().isBlank()) {
            parameters.addValue("applicationFormStatus", query.applicationFormStatus());
        }
        if (query.dateFrom() != null) {
            parameters.addValue("dateFrom", query.dateFrom());
        }
        if (query.dateTo() != null) {
            parameters.addValue("dateTo", query.dateTo());
        }
        return parameters;
    }

    private MapSqlParameterSource duplicateApplicationParams(DuplicateApplicationQuery query) {
        return new MapSqlParameterSource()
            .addValue("patientId", query.patientId())
            .addValue("patientName", query.patientName())
            .addValue("externalOrderNo", query.externalOrderNo())
            .addValue("applicationDate", query.applicationDate())
            .addValue("applicationType", query.applicationType())
            .addValue("specimenSite", query.specimenSite());
    }

    private MapSqlParameterSource specimenManagementParams(SpecimenManagementListQuery query) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (query.keyword() != null && !query.keyword().isBlank()) {
            parameters.addValue("keyword", "%" + query.keyword() + "%");
        }
        if (query.applicationNo() != null && !query.applicationNo().isBlank()) {
            parameters.addValue("applicationNo", "%" + query.applicationNo() + "%");
        }
        if (query.departmentId() != null && !query.departmentId().isBlank()) {
            parameters.addValue("departmentId", query.departmentId());
        }
        if (query.specimenStatus() != null && !query.specimenStatus().isBlank()) {
            parameters.addValue("specimenStatus", query.specimenStatus());
        }
        if (query.labelPrintStatus() != null && !query.labelPrintStatus().isBlank()) {
            parameters.addValue("labelPrintStatus", query.labelPrintStatus());
        }
        if (query.dateFrom() != null) {
            parameters.addValue("dateFrom", query.dateFrom());
        }
        if (query.dateTo() != null) {
            parameters.addValue("dateTo", query.dateTo());
        }
        return parameters;
    }

    private boolean hasSpecimenContainerColumns() {
        Boolean cached = specimenContainerColumnsAvailable;
        if (cached != null) {
            return cached;
        }
        Boolean resolved = jdbcTemplate.getJdbcOperations().execute((ConnectionCallback<Boolean>) connection ->
            columnExists(connection.getMetaData(), "SPECIMENS", "CONTAINER_NAME")
                && columnExists(connection.getMetaData(), "SPECIMENS", "CONTAINER_COUNT"));
        specimenContainerColumnsAvailable = Boolean.TRUE.equals(resolved);
        return specimenContainerColumnsAvailable;
    }

    private boolean hasCollectionPrinterCodeColumn() {
        Boolean cached = collectionPrinterCodeColumnAvailable;
        if (cached != null) {
            return cached;
        }
        Boolean resolved = jdbcTemplate.getJdbcOperations().execute((ConnectionCallback<Boolean>) connection ->
            columnExists(connection.getMetaData(), "SPECIMEN_COLLECTION_RECORDS", "PRINTER_CODE"));
        collectionPrinterCodeColumnAvailable = Boolean.TRUE.equals(resolved);
        return collectionPrinterCodeColumnAvailable;
    }

    private boolean columnExists(DatabaseMetaData metadata, String tableName, String columnName) throws SQLException {
        try (ResultSet columns = metadata.getColumns(null, null, tableName, columnName)) {
            while (columns.next()) {
                if (tableName.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                    && columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        try (ResultSet columns = metadata.getColumns(null, null, tableName.toLowerCase(), columnName.toLowerCase())) {
            while (columns.next()) {
                if (tableName.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                    && columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String containerNameSelect(String alias) {
        if (hasSpecimenContainerColumns()) {
            return "                " + alias + ".container_name as container_name,\n";
        }
        return "                cast(null as varchar(200)) as container_name,\n";
    }

    private String containerCountSelect(String alias) {
        if (hasSpecimenContainerColumns()) {
            return "                " + alias + ".container_count as container_count,\n";
        }
        return "                cast(null as integer) as container_count,\n";
    }

    private String nextId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private PendingSpecimenRow mapPendingSpecimenRow(ResultSet rs, int rowNum) throws SQLException {
        return new PendingSpecimenRow(
            rs.getString("application_id"),
            rs.getString("application_no"),
            rs.getString("patient_name"),
            rs.getString("submitting_department_id"),
            rs.getString("submitting_department_name"),
            rs.getString("transport_order_id"),
            rs.getString("specimen_id"),
            rs.getString("specimen_no"),
            rs.getString("barcode"),
            JdbcResultSetUtils.getNullableString(rs, "container_name"),
            JdbcResultSetUtils.getNullableInteger(rs, "container_count"),
            rs.getString("specimen_status"),
            rs.getString("fixation_status"),
            rs.getTimestamp("registered_at") == null ? null : rs.getTimestamp("registered_at").toLocalDateTime(),
            rs.getTimestamp("latest_event_time") == null ? null : rs.getTimestamp("latest_event_time").toLocalDateTime(),
            rs.getInt("abnormal_flag") == 1);
    }

    private PendingTransportOrderRow mapPendingTransportOrderRow(ResultSet rs, int rowNum) throws SQLException {
        return new PendingTransportOrderRow(
            rs.getString("id"),
            rs.getString("transport_order_no"),
            rs.getString("application_id"),
            rs.getString("application_no"),
            rs.getString("patient_name"),
            rs.getString("handover_department_name"),
            rs.getString("receiver_department_name"),
            rs.getString("order_status"),
            rs.getTimestamp("to_be_transported_at") == null ? null : rs.getTimestamp("to_be_transported_at").toLocalDateTime(),
            rs.getTimestamp("handed_over_at") == null ? null : rs.getTimestamp("handed_over_at").toLocalDateTime());
    }

    private ApplicationListRow mapApplicationListRow(ResultSet rs, int rowNum) throws SQLException {
        return new ApplicationListRow(
            rs.getString("id"),
            rs.getString("application_no"),
            rs.getString("patient_name"),
            rs.getString("patient_gender"),
            rs.getString("patient_age"),
            rs.getString("status"),
            rs.getString("submitting_department_name"),
            rs.getString("submitting_doctor_name"),
            rs.getString("application_type"),
            rs.getString("application_form_status"),
            rs.getString("current_node"),
            rs.getInt("abnormal_flag") == 1,
            rs.getInt("registered_specimen_count"),
            rs.getString("latest_label_print_status"),
            rs.getDate("application_date") == null ? null : rs.getDate("application_date").toLocalDate(),
            rs.getDate("submission_date") == null ? null : rs.getDate("submission_date").toLocalDate(),
            rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private DuplicateApplicationRow mapDuplicateApplicationRow(ResultSet rs, int rowNum) throws SQLException {
        return new DuplicateApplicationRow(
            rs.getString("id"),
            rs.getString("application_no"),
            rs.getString("patient_name"),
            rs.getString("specimen_site"),
            rs.getString("status"),
            rs.getString("current_node"),
            rs.getDate("application_date") == null ? null : rs.getDate("application_date").toLocalDate(),
            rs.getInt("external_order_matched") == 1,
            rs.getInt("same_day_site_matched") == 1);
    }

    private SpecimenManagementListRow mapSpecimenManagementListRow(ResultSet rs, int rowNum) throws SQLException {
        return new SpecimenManagementListRow(
            rs.getString("specimen_id"),
            rs.getString("specimen_no"),
            rs.getString("barcode"),
            rs.getString("application_id"),
            rs.getString("application_no"),
            rs.getString("patient_name"),
            rs.getString("submitting_department_id"),
            rs.getString("submitting_department_name"),
            rs.getString("specimen_name"),
            rs.getString("specimen_type"),
            rs.getString("specimen_site"),
            rs.getObject("specimen_count", Integer.class),
            JdbcResultSetUtils.getNullableString(rs, "container_name"),
            JdbcResultSetUtils.getNullableInteger(rs, "container_count"),
            rs.getString("specimen_status"),
            rs.getString("fixation_status"),
            rs.getString("label_print_status"),
            rs.getString("label_print_batch_no"),
            rs.getTimestamp("registered_at") == null ? null : rs.getTimestamp("registered_at").toLocalDateTime(),
            rs.getTimestamp("latest_event_time") == null ? null : rs.getTimestamp("latest_event_time").toLocalDateTime(),
            rs.getInt("abnormal_flag") == 1);
    }

    private SpecimenManagementSummary mapSpecimenManagementSummary(ResultSet rs, int rowNum) throws SQLException {
        return new SpecimenManagementSummary(
            rs.getLong("total_count"),
            rs.getLong("label_printed_count"),
            rs.getLong("pending_label_count"),
            rs.getLong("abnormal_count"));
    }
}
