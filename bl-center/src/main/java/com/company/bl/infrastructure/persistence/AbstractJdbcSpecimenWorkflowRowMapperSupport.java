package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.enums.TransportItemStatus;
import com.company.bl.domain.enums.TransportOrderStatus;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.domain.model.TransportOrderItem;
import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;

abstract class AbstractJdbcSpecimenWorkflowRowMapperSupport extends AbstractJdbcSpecimenWorkflowSchemaSupport {

    protected AbstractJdbcSpecimenWorkflowRowMapperSupport(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    protected Specimen mapSpecimen(ResultSet rs, int rowNum) throws SQLException {
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
            JdbcResultSetUtils.getNullableString(rs, "verification_status"),
            rs.getTimestamp("verification_started_at") == null
                ? null
                : rs.getTimestamp("verification_started_at").toLocalDateTime(),
            rs.getTimestamp("verification_completed_at") == null
                ? null
                : rs.getTimestamp("verification_completed_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableTimestamp(rs, "specimen_removal_at") == null
                ? null
                : JdbcResultSetUtils.getNullableTimestamp(rs, "specimen_removal_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableString(rs, "specimen_removal_operator_user_id"),
            JdbcResultSetUtils.getNullableString(rs, "specimen_removal_operator_name"),
            JdbcResultSetUtils.getNullableTimestamp(rs, "specimen_confirmed_at") == null
                ? null
                : JdbcResultSetUtils.getNullableTimestamp(rs, "specimen_confirmed_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableString(rs, "resolved_check_in_status"),
            JdbcResultSetUtils.getNullableTimestamp(rs, "checked_in_at") == null
                ? null
                : JdbcResultSetUtils.getNullableTimestamp(rs, "checked_in_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableString(rs, "checked_in_by_name"),
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

    protected PathologyCase mapPathologyCase(ResultSet rs, int rowNum) throws SQLException {
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

    protected SpecimenWorkflowRepository.RegistrationSnapshotData mapRegistrationSnapshot(ResultSet rs, int rowNum) throws SQLException {
        return new SpecimenWorkflowRepository.RegistrationSnapshotData(
            rs.getString("collection_scene"),
            rs.getString("collector_user_id"),
            rs.getString("collector_name"),
            JdbcResultSetUtils.getNullableString(rs, "printer_code"),
            rs.getString("terminal_code"),
            rs.getString("remarks"));
    }

    protected TransportOrder mapTransportOrder(ResultSet rs, int rowNum) throws SQLException {
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

    protected TransportOrderItem mapTransportOrderItem(ResultSet rs, int rowNum) throws SQLException {
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

    protected TrackingEvent mapTrackingEvent(ResultSet rs, int rowNum) throws SQLException {
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
}
