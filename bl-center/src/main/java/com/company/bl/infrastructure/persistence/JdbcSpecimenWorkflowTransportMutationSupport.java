package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.enums.ReceiptStatus;
import com.company.bl.domain.enums.TransportItemStatus;
import com.company.bl.domain.enums.TransportOrderStatus;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.domain.model.TransportOrderItem;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDateTime;
import java.util.Map;

abstract class JdbcSpecimenWorkflowTransportMutationSupport extends JdbcSpecimenWorkflowSpecimenMutationSupport {

    protected JdbcSpecimenWorkflowTransportMutationSupport(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    public TransportOrder insertTransportOrder(TransportOrder order) {
        jdbcTemplate.update("""
            insert into transport_orders
                (id, transport_order_no, application_id, order_status, handover_user_id, handover_user_name,
                 handover_department_id, handover_department_name, receiver_department_id, receiver_department_name,
                 receiver_user_id, receiver_user_name, outbound_user_id, outbound_user_name, printed_at,
                 to_be_transported_at, handed_over_at, terminal_code, remarks, created_at, updated_at)
            values
                (:id, :transportOrderNo, :applicationId, :status, :handoverUserId, :handoverUserName,
                 :handoverDepartmentId, :handoverDepartmentName, :receiverDepartmentId, :receiverDepartmentName,
                 :receiverUserId, :receiverUserName, :outboundUserId, :outboundUserName, :printedAt,
                 :toBeTransportedAt, :handedOverAt, :terminalCode, :remarks, :createdAt, :updatedAt)
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
            .addValue("outboundUserId", order.outboundUserId())
            .addValue("outboundUserName", order.outboundUserName())
            .addValue("printedAt", order.printedAt())
            .addValue("toBeTransportedAt", order.toBeTransportedAt())
            .addValue("handedOverAt", order.handedOverAt())
            .addValue("terminalCode", order.terminalCode())
            .addValue("remarks", order.remarks())
            .addValue("createdAt", LocalDateTime.now())
            .addValue("updatedAt", LocalDateTime.now()));
        return order;
    }

    public TransportOrder updateTransportOrderStatus(String transportOrderId,
                                                     TransportOrderStatus status,
                                                     String receiverUserId,
                                                     String receiverUserName,
                                                     String outboundUserId,
                                                     String outboundUserName,
                                                     LocalDateTime printedAt,
                                                     LocalDateTime handedOverAt) {
        jdbcTemplate.update("""
            update transport_orders
            set order_status = :status,
                receiver_user_id = COALESCE(:receiverUserId, receiver_user_id),
                receiver_user_name = COALESCE(:receiverUserName, receiver_user_name),
                outbound_user_id = COALESCE(:outboundUserId, outbound_user_id),
                outbound_user_name = COALESCE(:outboundUserName, outbound_user_name),
                printed_at = COALESCE(:printedAt, printed_at),
                handed_over_at = COALESCE(:handedOverAt, handed_over_at),
                updated_at = :updatedAt
            where id = :transportOrderId
            """, new MapSqlParameterSource()
            .addValue("transportOrderId", transportOrderId)
            .addValue("status", status.name())
            .addValue("receiverUserId", receiverUserId)
            .addValue("receiverUserName", receiverUserName)
            .addValue("outboundUserId", outboundUserId)
            .addValue("outboundUserName", outboundUserName)
            .addValue("printedAt", printedAt)
            .addValue("handedOverAt", handedOverAt)
            .addValue("updatedAt", LocalDateTime.now()));
        return findTransportOrderById(transportOrderId).orElseThrow();
    }

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

    public void ensureTechnicalSpecimenRegistrationPending(String applicationId, String caseId) {
        Long count = jdbcTemplate.queryForObject("""
            select count(1)
            from technical_specimen_registrations
            where case_id = :caseId
            """, Map.of("caseId", caseId), Long.class);
        if (count != null && count > 0) {
            jdbcTemplate.update("""
                update technical_specimen_registrations
                set application_id = :applicationId,
                    updated_at = :updatedAt
                where case_id = :caseId
                  and registration_status = 'PENDING'
                """, new MapSqlParameterSource()
                .addValue("caseId", caseId)
                .addValue("applicationId", applicationId)
                .addValue("updatedAt", LocalDateTime.now()));
            return;
        }
        jdbcTemplate.update("""
            insert into technical_specimen_registrations
                (case_id, application_id, registration_status, created_at, updated_at)
            values
                (:caseId, :applicationId, 'PENDING', :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("applicationId", applicationId)
            .addValue("caseId", caseId)
            .addValue("createdAt", LocalDateTime.now())
            .addValue("updatedAt", LocalDateTime.now()));
    }
}
