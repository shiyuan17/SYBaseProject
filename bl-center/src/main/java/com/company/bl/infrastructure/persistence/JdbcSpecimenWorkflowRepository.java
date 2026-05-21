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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcSpecimenWorkflowRepository implements SpecimenWorkflowRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcSpecimenWorkflowRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Specimen> findSpecimenByBarcode(String barcode) {
        List<Specimen> rows = jdbcTemplate.query("""
            select *
            from specimens
            where barcode = :barcode
            """, Map.of("barcode", barcode), this::mapSpecimen);
        return rows.stream().findFirst();
    }

    @Override
    public List<Specimen> findSpecimensByApplicationId(String applicationId) {
        return jdbcTemplate.query("""
            select *
            from specimens
            where application_id = :applicationId
            order by registered_at asc, created_at asc
            """, Map.of("applicationId", applicationId), this::mapSpecimen);
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
        jdbcTemplate.update("""
            insert into specimens
                (id, application_id, case_id, specimen_no, barcode, specimen_type, specimen_name_standardized,
                 specimen_site, collection_mode, specimen_count, specimen_status, fixation_status, qualified_flag,
                 unqualified_reason, clinical_symptom, applicant_department_id, applicant_department_name,
                 applicant_doctor_user_id, applicant_doctor_name, submission_date, label_print_batch_no,
                 label_print_status, registered_by_user_id, registered_by_name, registered_at, terminal_code,
                 remarks, created_at, updated_at)
            values
                (:id, :applicationId, :caseId, :specimenNo, :barcode, :specimenType, :specimenNameStandardized,
                 :specimenSite, :collectionMode, :specimenCount, :specimenStatus, :fixationStatus, :qualifiedFlag,
                 :unqualifiedReason, :clinicalSymptom, :applicantDepartmentId, :applicantDepartmentName,
                 :applicantDoctorUserId, :applicantDoctorName, :submissionDate, :labelPrintBatchNo,
                 :labelPrintStatus, :registeredByUserId, :registeredByName, :registeredAt, :terminalCode,
                 :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
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
            .addValue("updatedAt", specimen.registeredAt()));
        return specimen;
    }

    @Override
    public List<Specimen> findSpecimensByLabelPrintBatchNoAndStatus(String labelPrintBatchNo, String labelPrintStatus) {
        return jdbcTemplate.query("""
            select *
            from specimens
            where label_print_batch_no = :labelPrintBatchNo
              and label_print_status = :labelPrintStatus
            order by registered_at asc, id asc
            """, new MapSqlParameterSource()
            .addValue("labelPrintBatchNo", labelPrintBatchNo)
            .addValue("labelPrintStatus", labelPrintStatus), this::mapSpecimen);
    }

    @Override
    public void insertCollectionRecord(String applicationId,
                                       String specimenId,
                                       String collectionStatus,
                                       String collectionScene,
                                       String collectionMode,
                                       String labelPrintBatchNo,
                                       String collectorUserId,
                                       String collectorName,
                                       LocalDateTime collectedAt,
                                       String terminalCode,
                                       String remarks) {
        jdbcTemplate.update("""
            insert into specimen_collection_records
                (id, application_id, specimen_id, collection_status, collection_scene, collection_mode,
                 label_print_batch_no, collector_user_id, collector_name, collected_at, terminal_code, remarks)
            values
                (:id, :applicationId, :specimenId, :collectionStatus, :collectionScene, :collectionMode,
                 :labelPrintBatchNo, :collectorUserId, :collectorName, :collectedAt, :terminalCode, :remarks)
            """, new MapSqlParameterSource()
            .addValue("id", nextId("SCR"))
            .addValue("applicationId", applicationId)
            .addValue("specimenId", specimenId)
            .addValue("collectionStatus", collectionStatus)
            .addValue("collectionScene", collectionScene)
            .addValue("collectionMode", collectionMode)
            .addValue("labelPrintBatchNo", labelPrintBatchNo)
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
                 barcode, received_by_user_id, received_by_name, received_at, terminal_code, reject_reason,
                 return_reason, remarks)
            values
                (:id, :applicationId, :caseId, :specimenId, :transportOrderId, :receiptStatus, :containerCount,
                 :barcode, :receivedByUserId, :receivedByName, :receivedAt, :terminalCode, :rejectReason,
                 :returnReason, :remarks)
            """, new MapSqlParameterSource()
            .addValue("id", nextId("SR"))
            .addValue("applicationId", applicationId)
            .addValue("caseId", caseId)
            .addValue("specimenId", specimenId)
            .addValue("transportOrderId", transportOrderId)
            .addValue("receiptStatus", receiptStatus.name())
            .addValue("containerCount", containerCount)
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
            where s.specimen_status in ('REGISTERED', 'FIXING')
              and coalesce(s.fixation_status, 'PENDING') <> 'COMPLETED'
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
                        where toi.specimen_id = s.id
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
                    where toi.specimen_id = s.id
                    order by toi.verified_at desc, toi.id desc
                    fetch next 1 rows only
                ) as transport_order_id,
                s.id as specimen_id,
                s.specimen_no,
                s.barcode,
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
            SpecimenStatus.from(rs.getString("specimen_status")),
            FixationStatus.from(rs.getString("fixation_status")),
            rs.getInt("qualified_flag") != 0,
            rs.getString("unqualified_reason"),
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

    private String buildPendingFilters(PendingSpecimenQuery query, String applicationAlias, String specimenAlias) {
        StringBuilder builder = new StringBuilder();
        if (query.applicationId() != null && !query.applicationId().isBlank()) {
            builder.append(" and ").append(applicationAlias).append(".id = :applicationId");
        }
        if (query.departmentId() != null && !query.departmentId().isBlank()) {
            builder.append(" and ").append(applicationAlias).append(".submitting_department_id = :departmentId");
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

    private MapSqlParameterSource pendingParams(PendingSpecimenQuery query, boolean paged) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (query.applicationId() != null && !query.applicationId().isBlank()) {
            parameters.addValue("applicationId", query.applicationId());
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
        return parameters;
    }

    private MapSqlParameterSource pendingTransportOrderParams(PendingTransportOrderQuery query) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (query.applicationId() != null && !query.applicationId().isBlank()) {
            parameters.addValue("applicationId", query.applicationId());
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
}
