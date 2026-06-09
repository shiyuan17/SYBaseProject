package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.ReworkOrder;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.Slide;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.SlideQcEvaluation;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.SlideStaining;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.Slicing;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.DehydrationBatch;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.DehydrationBatchItem;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.Embedding;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.EmbeddingBox;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.SamplingBlock;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.CaseMediaAsset;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.EmbeddingWorkstationRecord;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.TechnicalTask;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

final class JdbcTechnicalWorkflowRowMappers {

    PathologyCase mapPathologyCase(ResultSet rs, int rowNum) throws SQLException {
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
            toLocalDateTime(rs.getTimestamp("received_at")));
    }

    Specimen mapSpecimen(ResultSet rs, int rowNum) throws SQLException {
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
            rs.getObject("specimen_count") == null ? null : rs.getInt("specimen_count"),
            JdbcResultSetUtils.getNullableString(rs, "specimen_size"),
            rs.getInt("frozen_flag") != 0,
            JdbcResultSetUtils.getNullableString(rs, "registration_evaluation_items"),
            JdbcResultSetUtils.getNullableString(rs, "container_name"),
            JdbcResultSetUtils.getNullableInteger(rs, "container_count"),
            SpecimenStatus.from(rs.getString("specimen_status")),
            FixationStatus.from(rs.getString("fixation_status")),
            JdbcResultSetUtils.getNullableString(rs, "verification_status"),
            toLocalDateTime(rs.getTimestamp("verification_started_at")),
            toLocalDateTime(rs.getTimestamp("verification_completed_at")),
            JdbcResultSetUtils.getNullableString(rs, "verified_by_user_id"),
            JdbcResultSetUtils.getNullableString(rs, "verified_by_name"),
            null,
            null,
            null,
            null,
            JdbcResultSetUtils.getNullableString(rs, "resolved_check_in_status"),
            toLocalDateTime(rs.getTimestamp("checked_in_at")),
            JdbcResultSetUtils.getNullableString(rs, "checked_in_by_name"),
            rs.getInt("qualified_flag") != 0,
            rs.getString("unqualified_reason"),
            JdbcResultSetUtils.getNullableString(rs, "latest_receipt_status"),
            JdbcResultSetUtils.getNullableString(rs, "latest_quality_check_result"),
            JdbcResultSetUtils.getNullableString(rs, "latest_quality_issue_codes"),
            rs.getString("clinical_symptom"),
            rs.getString("applicant_department_id"),
            rs.getString("applicant_department_name"),
            rs.getString("applicant_doctor_user_id"),
            rs.getString("applicant_doctor_name"),
            toLocalDate(rs.getDate("submission_date")),
            rs.getString("label_print_batch_no"),
            rs.getString("label_print_status"),
            rs.getString("registered_by_user_id"),
            rs.getString("registered_by_name"),
            toLocalDateTime(rs.getTimestamp("registered_at")),
            rs.getString("terminal_code"),
            rs.getString("remarks"));
    }

    TechnicalTask mapTechnicalTask(ResultSet rs, int rowNum) throws SQLException {
        return new TechnicalTask(
            rs.getString("id"),
            rs.getString("application_id"),
            rs.getString("application_no"),
            JdbcResultSetUtils.getNullableString(rs, "patient_name"),
            JdbcResultSetUtils.getNullableString(rs, "patient_id"),
            rs.getString("case_id"),
            rs.getString("pathology_no"),
            rs.getString("specimen_id"),
            rs.getString("task_type"),
            rs.getString("task_status"),
            rs.getString("object_type"),
            rs.getString("object_id"),
            JdbcResultSetUtils.getNullableString(rs, "object_display_no"),
            rs.getString("sampling_block_code"),
            rs.getString("sampling_block_description"),
            rs.getString("sampled_by_name"),
            toLocalDateTime(rs.getTimestamp("sampled_at")),
            rs.getString("parent_task_id"),
            rs.getString("priority"),
            rs.getString("current_node"),
            rs.getString("station_code"),
            rs.getString("station_name"),
            rs.getString("assigned_to_user_id"),
            rs.getString("assigned_to_name"),
            toLocalDateTime(rs.getTimestamp("expected_completed_at")),
            rs.getString("production_remarks"),
            toLocalDateTime(rs.getTimestamp("received_at")),
            rs.getString("payload"),
            rs.getString("remarks"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("started_at")),
            toLocalDateTime(rs.getTimestamp("completed_at")));
    }

    SamplingBlock mapSamplingBlock(ResultSet rs, int rowNum) throws SQLException {
        return new SamplingBlock(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("sampling_id"),
            rs.getInt("sequence_no"),
            rs.getString("block_code"),
            rs.getString("block_site"),
            rs.getString("block_description"),
            rs.getString("embedding_box_no"),
            rs.getString("special_requirement"),
            rs.getString("specimen_name"),
            rs.getString("gross_description"));
    }

    DehydrationBatch mapDehydrationBatch(ResultSet rs, int rowNum) throws SQLException {
        return new DehydrationBatch(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("batch_no"),
            rs.getString("batch_status"),
            rs.getString("basket_no"),
            rs.getString("device_no"),
            rs.getString("operator_user_id"),
            rs.getString("operator_name"),
            toLocalDateTime(rs.getTimestamp("started_at")),
            toLocalDateTime(rs.getTimestamp("completed_at")),
            rs.getString("remarks"));
    }

    DehydrationBatchItem mapDehydrationBatchItem(ResultSet rs, int rowNum) throws SQLException {
        return new DehydrationBatchItem(
            rs.getString("id"),
            rs.getString("batch_id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("sampling_block_id"),
            rs.getString("item_status"),
            toLocalDateTime(rs.getTimestamp("loaded_at")),
            rs.getString("remarks"));
    }

    Embedding mapEmbedding(ResultSet rs, int rowNum) throws SQLException {
        return new Embedding(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("sampling_id"),
            rs.getString("sampling_block_id"),
            rs.getString("embedding_status"),
            rs.getString("evaluation_level"),
            rs.getString("sampling_evaluation"),
            toLocalDateTime(rs.getTimestamp("started_at")),
            toLocalDateTime(rs.getTimestamp("ended_at")),
            rs.getString("embedded_by_user_id"),
            rs.getString("embedded_by_name"),
            rs.getString("remarks"));
    }

    EmbeddingBox mapEmbeddingBox(ResultSet rs, int rowNum) throws SQLException {
        return new EmbeddingBox(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("sampling_block_id"),
            rs.getString("embedding_id"),
            rs.getString("embedding_box_no"),
            rs.getInt("block_count"),
            rs.getInt("re_embedding_flag") == 1,
            rs.getString("slice_notice"),
            rs.getString("storage_status"));
    }

    Slicing mapSlicing(ResultSet rs, int rowNum) throws SQLException {
        return new Slicing(
            rs.getString("id"),
            JdbcResultSetUtils.getNullableString(rs, "task_id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("embedding_id"),
            rs.getString("embedding_box_id"),
            rs.getString("slicing_batch_no"),
            rs.getString("slicing_status"),
            rs.getInt("slide_count"));
    }

    Slide mapSlide(ResultSet rs, int rowNum) throws SQLException {
        return new Slide(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("slicing_id"),
            rs.getString("embedding_box_id"),
            rs.getString("sampling_block_id"),
            rs.getString("slide_no"),
            rs.getInt("combined_slide_flag") == 1,
            rs.getString("quality_status"),
            rs.getString("slide_status"),
            rs.getObject("slice_count") == null ? null : rs.getInt("slice_count"));
    }

    SlideStaining mapSlideStaining(ResultSet rs, int rowNum) throws SQLException {
        return new SlideStaining(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("slide_id"),
            rs.getString("staining_type"),
            rs.getString("staining_status"),
            toLocalDateTime(rs.getTimestamp("stained_at")),
            rs.getString("quality_issue"),
            rs.getString("remarks"));
    }

    ReworkOrder mapReworkOrder(ResultSet rs, int rowNum) throws SQLException {
        return new ReworkOrder(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("sampling_block_id"),
            rs.getString("embedding_box_id"),
            rs.getString("slide_id"),
            rs.getString("rework_type"),
            rs.getString("status"),
            rs.getString("reason"));
    }

    SlideQcEvaluation mapSlideQcEvaluation(ResultSet rs, int rowNum) throws SQLException {
        return new SlideQcEvaluation(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("slide_id"),
            rs.getString("slide_no"),
            rs.getString("qc_type"),
            rs.getString("evaluation_result"),
            rs.getString("issue_description"),
            rs.getString("improvement_suggestion"),
            rs.getString("evaluator_user_id"),
            rs.getString("evaluator_name"),
            toLocalDateTime(rs.getTimestamp("evaluated_at")),
            rs.getString("remarks"));
    }

    CaseMediaAsset mapCaseMediaAsset(ResultSet rs, int rowNum) throws SQLException {
        return new CaseMediaAsset(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("object_type"),
            rs.getString("object_id"),
            rs.getString("media_type"),
            rs.getString("file_url"),
            rs.getString("file_name"),
            toLocalDateTime(rs.getTimestamp("captured_at")),
            rs.getString("captured_by_user_id"),
            rs.getString("captured_by_name"),
            rs.getString("remarks"));
    }

    EmbeddingWorkstationRecord mapEmbeddingWorkstationRecord(ResultSet rs, int rowNum) throws SQLException {
        return new EmbeddingWorkstationRecord(
            rs.getString("task_id"),
            rs.getString("case_id"),
            rs.getString("pathology_no"),
            rs.getString("specimen_id"),
            rs.getString("specimen_name"),
            rs.getString("sampling_block_id"),
            rs.getString("sampling_block_code"),
            rs.getString("sampling_block_description"),
            rs.getString("gross_description"),
            rs.getString("embedding_id"),
            rs.getString("embedding_box_id"),
            rs.getString("embedding_box_no"),
            rs.getString("slice_notice"),
            rs.getString("evaluation_level"),
            rs.getString("sampling_evaluation"),
            rs.getString("embedding_remarks"),
            rs.getString("sampled_by_name"),
            toLocalDateTime(rs.getTimestamp("sampled_at")),
            rs.getString("embedded_by_name"),
            toLocalDateTime(rs.getTimestamp("started_at")),
            toLocalDateTime(rs.getTimestamp("ended_at")),
            rs.getString("task_status"));
    }

    TrackingEvent mapTrackingEvent(ResultSet rs, int rowNum) throws SQLException {
        return new TrackingEvent(
            rs.getString("id"),
            rs.getString("application_id"),
            rs.getString("specimen_id"),
            rs.getString("case_id"),
            rs.getString("transport_order_id"),
            rs.getString("node_code"),
            rs.getString("event_type"),
            rs.getString("event_status"),
            toLocalDateTime(rs.getTimestamp("event_time")),
            rs.getString("operator_user_id"),
            rs.getString("operator_name"),
            rs.getString("source_terminal"),
            rs.getString("event_content"));
    }

    private LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
