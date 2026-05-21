package com.company.bl.integration.infrastructure;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class M6JdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public M6JdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertIntegrationTask(CreateIntegrationTaskRow row) {
        jdbcTemplate.update("""
            insert into integration_tasks
                (id, task_type, business_type, business_id, stage_code, external_system, request_payload,
                 response_payload, task_status, retry_count, max_retry_count, next_retry_at, last_attempt_at,
                 last_error_code, last_error_message, compensation_status, reconciliation_status, resolved_at, created_at, updated_at)
            values
                (:id, :taskType, :businessType, :businessId, :stageCode, :externalSystem, :requestPayload,
                 :responsePayload, :taskStatus, :retryCount, :maxRetryCount, :nextRetryAt, :lastAttemptAt,
                 :lastErrorCode, :lastErrorMessage, :compensationStatus, :reconciliationStatus, :resolvedAt, :createdAt, :updatedAt)
            """, toIntegrationParams(row));
    }

    public void updateIntegrationTask(IntegrationTaskRow row) {
        jdbcTemplate.update("""
            update integration_tasks
            set request_payload = :requestPayload,
                response_payload = :responsePayload,
                task_status = :taskStatus,
                retry_count = :retryCount,
                max_retry_count = :maxRetryCount,
                next_retry_at = :nextRetryAt,
                last_attempt_at = :lastAttemptAt,
                last_error_code = :lastErrorCode,
                last_error_message = :lastErrorMessage,
                compensation_status = :compensationStatus,
                reconciliation_status = :reconciliationStatus,
                resolved_at = :resolvedAt,
                updated_at = :updatedAt
            where id = :id
            """, toIntegrationParams(new CreateIntegrationTaskRow(
            row.id(), row.taskType(), row.businessType(), row.businessId(), row.stageCode(), row.externalSystem(), row.requestPayload(),
            row.responsePayload(), row.taskStatus(), row.retryCount(), row.maxRetryCount(), row.nextRetryAt(), row.lastAttemptAt(),
            row.lastErrorCode(), row.lastErrorMessage(), row.compensationStatus(), row.reconciliationStatus(), row.resolvedAt(),
            row.createdAt(), row.updatedAt())));
    }

    public IntegrationTaskRow findIntegrationTaskById(String id) {
        List<IntegrationTaskRow> rows = jdbcTemplate.query("""
            select id, task_type, business_type, business_id, stage_code, external_system, request_payload, response_payload,
                   task_status, retry_count, max_retry_count, next_retry_at, last_attempt_at, last_error_code, last_error_message,
                   compensation_status, reconciliation_status, resolved_at, created_at, updated_at
            from integration_tasks
            where id = :id
            """, Map.of("id", id), this::mapIntegrationTask);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public IntegrationTaskRow findLatestIntegrationTask(String businessType, String businessId, String stageCode) {
        List<IntegrationTaskRow> rows = jdbcTemplate.query("""
            select id, task_type, business_type, business_id, stage_code, external_system, request_payload, response_payload,
                   task_status, retry_count, max_retry_count, next_retry_at, last_attempt_at, last_error_code, last_error_message,
                   compensation_status, reconciliation_status, resolved_at, created_at, updated_at
            from integration_tasks
            where business_type = :businessType
              and business_id = :businessId
              and (:stageCode is null or stage_code = :stageCode)
            order by created_at desc, id desc
            """, new MapSqlParameterSource()
            .addValue("businessType", businessType)
            .addValue("businessId", businessId)
            .addValue("stageCode", stageCode), this::mapIntegrationTask);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<IntegrationTaskRow> findIntegrationTasks(String taskType, String businessType, String taskStatus) {
        return jdbcTemplate.query("""
            select id, task_type, business_type, business_id, stage_code, external_system, request_payload, response_payload,
                   task_status, retry_count, max_retry_count, next_retry_at, last_attempt_at, last_error_code, last_error_message,
                   compensation_status, reconciliation_status, resolved_at, created_at, updated_at
            from integration_tasks
            where (:taskType is null or task_type = :taskType)
              and (:businessType is null or business_type = :businessType)
              and (:taskStatus is null or task_status = :taskStatus)
            order by created_at desc, id desc
            """, new MapSqlParameterSource()
            .addValue("taskType", blankToNull(taskType))
            .addValue("businessType", blankToNull(businessType))
            .addValue("taskStatus", blankToNull(taskStatus)), this::mapIntegrationTask);
    }

    public void insertBillingRecord(CreateBillingRecordRow row) {
        jdbcTemplate.update("""
            insert into billing_records
                (id, case_id, order_id, billing_no, billing_stage, item_type, item_name, quantity, amount,
                 billing_status, billed_at, operator_user_id, operator_name, external_bill_no, external_system,
                 remarks, created_at, updated_at)
            values
                (:id, :caseId, :orderId, :billingNo, :billingStage, :itemType, :itemName, :quantity, :amount,
                 :billingStatus, :billedAt, :operatorUserId, :operatorName, :externalBillNo, :externalSystem,
                 :remarks, :createdAt, :updatedAt)
            """, toBillingParams(row));
    }

    public void updateBillingRecord(BillingRecordRow row) {
        jdbcTemplate.update("""
            update billing_records
            set billing_status = :billingStatus,
                billed_at = :billedAt,
                operator_user_id = :operatorUserId,
                operator_name = :operatorName,
                external_bill_no = :externalBillNo,
                external_system = :externalSystem,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :id
            """, toBillingParams(new CreateBillingRecordRow(
            row.id(), row.caseId(), row.orderId(), row.billingNo(), row.billingStage(), row.itemType(), row.itemName(),
            row.quantity(), row.amount(), row.billingStatus(), row.billedAt(), row.operatorUserId(), row.operatorName(),
            row.externalBillNo(), row.externalSystem(), row.remarks(), row.createdAt(), row.updatedAt())));
    }

    public BillingRecordRow findBillingRecordById(String id) {
        List<BillingRecordRow> rows = jdbcTemplate.query("""
            select id, case_id, order_id, billing_no, billing_stage, item_type, item_name, quantity, amount,
                   billing_status, billed_at, operator_user_id, operator_name, external_bill_no, external_system,
                   remarks, created_at, updated_at
            from billing_records
            where id = :id
            """, Map.of("id", id), this::mapBillingRecord);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<BillingRecordRow> findBillingRecords(String billingStatus, String billingStage,
                                                     LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query("""
            select id, case_id, order_id, billing_no, billing_stage, item_type, item_name, quantity, amount,
                   billing_status, billed_at, operator_user_id, operator_name, external_bill_no, external_system,
                   remarks, created_at, updated_at
            from billing_records
            where (:billingStatus is null or billing_status = :billingStatus)
              and (:billingStage is null or billing_stage = :billingStage)
              and (:fromTime is null or coalesce(billed_at, created_at) >= :fromTime)
              and (:toTime is null or coalesce(billed_at, created_at) <= :toTime)
            order by created_at desc, id desc
            """, new MapSqlParameterSource()
            .addValue("billingStatus", blankToNull(billingStatus))
            .addValue("billingStage", blankToNull(billingStage))
            .addValue("fromTime", from)
            .addValue("toTime", to), this::mapBillingRecord);
    }

    public void updateMedicalOrderBillingStatus(String orderId, String billingStatus) {
        jdbcTemplate.update("""
            update medical_orders
            set billing_status = :billingStatus,
                updated_at = :updatedAt
            where id = :orderId
            """, new MapSqlParameterSource()
            .addValue("orderId", orderId)
            .addValue("billingStatus", billingStatus)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void insertHistoricalImportJob(CreateHistoricalImportJobRow row) {
        jdbcTemplate.update("""
            insert into historical_import_jobs
                (id, source_system, patient_id, pathology_no, application_no, import_status, requested_by_user_id,
                 requested_by_name, total_count, success_count, failure_count, requested_at, completed_at,
                 last_error_message, remarks, created_at, updated_at)
            values
                (:id, :sourceSystem, :patientId, :pathologyNo, :applicationNo, :importStatus, :requestedByUserId,
                 :requestedByName, :totalCount, :successCount, :failureCount, :requestedAt, :completedAt,
                 :lastErrorMessage, :remarks, :createdAt, :updatedAt)
            """, toHistoricalJobParams(row));
    }

    public void updateHistoricalImportJob(HistoricalImportJobRow row) {
        jdbcTemplate.update("""
            update historical_import_jobs
            set import_status = :importStatus,
                total_count = :totalCount,
                success_count = :successCount,
                failure_count = :failureCount,
                completed_at = :completedAt,
                last_error_message = :lastErrorMessage,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :id
            """, toHistoricalJobParams(new CreateHistoricalImportJobRow(
            row.id(), row.sourceSystem(), row.patientId(), row.pathologyNo(), row.applicationNo(), row.importStatus(),
            row.requestedByUserId(), row.requestedByName(), row.totalCount(), row.successCount(), row.failureCount(),
            row.requestedAt(), row.completedAt(), row.lastErrorMessage(), row.remarks(), row.createdAt(), row.updatedAt())));
    }

    public HistoricalImportJobRow findHistoricalImportJobById(String id) {
        List<HistoricalImportJobRow> rows = jdbcTemplate.query("""
            select id, source_system, patient_id, pathology_no, application_no, import_status, requested_by_user_id,
                   requested_by_name, total_count, success_count, failure_count, requested_at, completed_at,
                   last_error_message, remarks, created_at, updated_at
            from historical_import_jobs
            where id = :id
            """, Map.of("id", id), this::mapHistoricalImportJob);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<HistoricalImportJobRow> findHistoricalImportJobs(String sourceSystem, String importStatus) {
        return jdbcTemplate.query("""
            select id, source_system, patient_id, pathology_no, application_no, import_status, requested_by_user_id,
                   requested_by_name, total_count, success_count, failure_count, requested_at, completed_at,
                   last_error_message, remarks, created_at, updated_at
            from historical_import_jobs
            where (:sourceSystem is null or source_system = :sourceSystem)
              and (:importStatus is null or import_status = :importStatus)
            order by requested_at desc, id desc
            """, new MapSqlParameterSource()
            .addValue("sourceSystem", blankToNull(sourceSystem))
            .addValue("importStatus", blankToNull(importStatus)), this::mapHistoricalImportJob);
    }

    public HistoricalReportRow findHistoricalReportBySourceAndExternalNo(String sourceSystem, String externalReportNo) {
        try {
            return jdbcTemplate.queryForObject("""
                select id, import_job_id, source_system, external_report_no, patient_id, patient_name, pathology_no,
                       application_no, report_date, final_diagnosis, report_summary, raw_payload, source_department_name,
                       source_doctor_name, attachment_url, created_at, updated_at
                from historical_reports
                where source_system = :sourceSystem and external_report_no = :externalReportNo
                """, new MapSqlParameterSource()
                .addValue("sourceSystem", sourceSystem)
                .addValue("externalReportNo", externalReportNo), this::mapHistoricalReport);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    public void insertHistoricalReport(CreateHistoricalReportRow row) {
        jdbcTemplate.update("""
            insert into historical_reports
                (id, import_job_id, source_system, external_report_no, patient_id, patient_name, pathology_no,
                 application_no, report_date, final_diagnosis, report_summary, raw_payload, source_department_name,
                 source_doctor_name, attachment_url, created_at, updated_at)
            values
                (:id, :importJobId, :sourceSystem, :externalReportNo, :patientId, :patientName, :pathologyNo,
                 :applicationNo, :reportDate, :finalDiagnosis, :reportSummary, :rawPayload, :sourceDepartmentName,
                 :sourceDoctorName, :attachmentUrl, :createdAt, :updatedAt)
            """, toHistoricalReportParams(row));
    }

    public void updateHistoricalReport(HistoricalReportRow row) {
        jdbcTemplate.update("""
            update historical_reports
            set import_job_id = :importJobId,
                patient_id = :patientId,
                patient_name = :patientName,
                pathology_no = :pathologyNo,
                application_no = :applicationNo,
                report_date = :reportDate,
                final_diagnosis = :finalDiagnosis,
                report_summary = :reportSummary,
                raw_payload = :rawPayload,
                source_department_name = :sourceDepartmentName,
                source_doctor_name = :sourceDoctorName,
                attachment_url = :attachmentUrl,
                updated_at = :updatedAt
            where id = :id
            """, toHistoricalReportParams(new CreateHistoricalReportRow(
            row.id(), row.importJobId(), row.sourceSystem(), row.externalReportNo(), row.patientId(), row.patientName(),
            row.pathologyNo(), row.applicationNo(), row.reportDate(), row.finalDiagnosis(), row.reportSummary(), row.rawPayload(),
            row.sourceDepartmentName(), row.sourceDoctorName(), row.attachmentUrl(), row.createdAt(), row.updatedAt())));
    }

    public void deleteHistoricalReportVersions(String historicalReportId) {
        jdbcTemplate.update("delete from historical_report_versions where historical_report_id = :historicalReportId",
            Map.of("historicalReportId", historicalReportId));
    }

    public void insertHistoricalReportVersion(CreateHistoricalReportVersionRow row) {
        jdbcTemplate.update("""
            insert into historical_report_versions
                (id, historical_report_id, version_no, final_diagnosis, report_summary, raw_payload, created_at, updated_at)
            values
                (:id, :historicalReportId, :versionNo, :finalDiagnosis, :reportSummary, :rawPayload, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("historicalReportId", row.historicalReportId())
            .addValue("versionNo", row.versionNo())
            .addValue("finalDiagnosis", row.finalDiagnosis())
            .addValue("reportSummary", row.reportSummary())
            .addValue("rawPayload", row.rawPayload())
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt()));
    }

    public List<HistoricalReportRow> findHistoricalReports(String sourceSystem, String patientId, String pathologyNo,
                                                           String applicationNo, LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query("""
            select id, import_job_id, source_system, external_report_no, patient_id, patient_name, pathology_no,
                   application_no, report_date, final_diagnosis, report_summary, raw_payload, source_department_name,
                   source_doctor_name, attachment_url, created_at, updated_at
            from historical_reports
            where (:sourceSystem is null or source_system = :sourceSystem)
              and (:patientId is null or patient_id = :patientId)
              and (:pathologyNo is null or pathology_no = :pathologyNo)
              and (:applicationNo is null or application_no = :applicationNo)
              and (:fromTime is null or report_date >= :fromTime)
              and (:toTime is null or report_date <= :toTime)
            order by report_date desc nulls last, id desc
            """, new MapSqlParameterSource()
            .addValue("sourceSystem", blankToNull(sourceSystem))
            .addValue("patientId", blankToNull(patientId))
            .addValue("pathologyNo", blankToNull(pathologyNo))
            .addValue("applicationNo", blankToNull(applicationNo))
            .addValue("fromTime", from)
            .addValue("toTime", to), this::mapHistoricalReport);
    }

    public List<HistoricalReportVersionRow> findHistoricalReportVersions(String historicalReportId) {
        return jdbcTemplate.query("""
            select id, historical_report_id, version_no, final_diagnosis, report_summary, raw_payload, created_at, updated_at
            from historical_report_versions
            where historical_report_id = :historicalReportId
            order by version_no
            """, Map.of("historicalReportId", historicalReportId), this::mapHistoricalReportVersion);
    }

    public List<StatIndicatorDefinitionRow> findStatIndicatorDefinitions(String category) {
        return jdbcTemplate.query("""
            select id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type,
                   description, sort_order, enabled, created_at, updated_at
            from stat_indicator_definitions
            where enabled = 1 and (:category is null or indicator_category = :category)
            order by sort_order, indicator_code
            """, new MapSqlParameterSource().addValue("category", blankToNull(category)), this::mapStatIndicatorDefinition);
    }

    public StatIndicatorDefinitionRow findStatIndicatorDefinitionByCode(String indicatorCode) {
        List<StatIndicatorDefinitionRow> rows = jdbcTemplate.query("""
            select id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type,
                   description, sort_order, enabled, created_at, updated_at
            from stat_indicator_definitions
            where indicator_code = :indicatorCode
            """, Map.of("indicatorCode", indicatorCode), this::mapStatIndicatorDefinition);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<StatReportTemplateRow> findStatReportTemplates(String templateType) {
        return jdbcTemplate.query("""
            select id, template_code, template_name, template_type, indicator_code, default_columns, parameter_schema,
                   sort_order, enabled, created_at, updated_at
            from stat_report_templates
            where enabled = 1 and (:templateType is null or template_type = :templateType)
            order by sort_order, template_code
            """, new MapSqlParameterSource().addValue("templateType", blankToNull(templateType)), this::mapStatReportTemplate);
    }

    public StatReportTemplateRow findStatReportTemplateByCode(String templateCode) {
        List<StatReportTemplateRow> rows = jdbcTemplate.query("""
            select id, template_code, template_name, template_type, indicator_code, default_columns, parameter_schema,
                   sort_order, enabled, created_at, updated_at
            from stat_report_templates
            where template_code = :templateCode
            """, Map.of("templateCode", templateCode), this::mapStatReportTemplate);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insertStatExportJob(CreateStatExportJobRow row) {
        jdbcTemplate.update("""
            insert into stat_export_jobs
                (id, export_no, template_id, indicator_code, export_status, filter_payload, file_name, content_type,
                 requested_by_user_id, requested_by_name, error_message, created_at, completed_at)
            values
                (:id, :exportNo, :templateId, :indicatorCode, :exportStatus, :filterPayload, :fileName, :contentType,
                 :requestedByUserId, :requestedByName, :errorMessage, :createdAt, :completedAt)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("exportNo", row.exportNo())
            .addValue("templateId", row.templateId())
            .addValue("indicatorCode", row.indicatorCode())
            .addValue("exportStatus", row.exportStatus())
            .addValue("filterPayload", row.filterPayload())
            .addValue("fileName", row.fileName())
            .addValue("contentType", row.contentType())
            .addValue("requestedByUserId", row.requestedByUserId())
            .addValue("requestedByName", row.requestedByName())
            .addValue("errorMessage", row.errorMessage())
            .addValue("createdAt", row.createdAt())
            .addValue("completedAt", row.completedAt()));
    }

    public void completeStatExportJob(String id, String exportStatus, String errorMessage, LocalDateTime completedAt) {
        jdbcTemplate.update("""
            update stat_export_jobs
            set export_status = :exportStatus,
                error_message = :errorMessage,
                completed_at = :completedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("exportStatus", exportStatus)
            .addValue("errorMessage", errorMessage)
            .addValue("completedAt", completedAt));
    }

    private MapSqlParameterSource toIntegrationParams(CreateIntegrationTaskRow row) {
        return new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("taskType", row.taskType())
            .addValue("businessType", row.businessType())
            .addValue("businessId", row.businessId())
            .addValue("stageCode", row.stageCode())
            .addValue("externalSystem", row.externalSystem())
            .addValue("requestPayload", row.requestPayload())
            .addValue("responsePayload", row.responsePayload())
            .addValue("taskStatus", row.taskStatus())
            .addValue("retryCount", row.retryCount())
            .addValue("maxRetryCount", row.maxRetryCount())
            .addValue("nextRetryAt", row.nextRetryAt())
            .addValue("lastAttemptAt", row.lastAttemptAt())
            .addValue("lastErrorCode", row.lastErrorCode())
            .addValue("lastErrorMessage", row.lastErrorMessage())
            .addValue("compensationStatus", row.compensationStatus())
            .addValue("reconciliationStatus", row.reconciliationStatus())
            .addValue("resolvedAt", row.resolvedAt())
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt());
    }

    private MapSqlParameterSource toBillingParams(CreateBillingRecordRow row) {
        return new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("caseId", row.caseId())
            .addValue("orderId", row.orderId())
            .addValue("billingNo", row.billingNo())
            .addValue("billingStage", row.billingStage())
            .addValue("itemType", row.itemType())
            .addValue("itemName", row.itemName())
            .addValue("quantity", row.quantity())
            .addValue("amount", row.amount())
            .addValue("billingStatus", row.billingStatus())
            .addValue("billedAt", row.billedAt())
            .addValue("operatorUserId", row.operatorUserId())
            .addValue("operatorName", row.operatorName())
            .addValue("externalBillNo", row.externalBillNo())
            .addValue("externalSystem", row.externalSystem())
            .addValue("remarks", row.remarks())
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt());
    }

    private MapSqlParameterSource toHistoricalJobParams(CreateHistoricalImportJobRow row) {
        return new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("sourceSystem", row.sourceSystem())
            .addValue("patientId", row.patientId())
            .addValue("pathologyNo", row.pathologyNo())
            .addValue("applicationNo", row.applicationNo())
            .addValue("importStatus", row.importStatus())
            .addValue("requestedByUserId", row.requestedByUserId())
            .addValue("requestedByName", row.requestedByName())
            .addValue("totalCount", row.totalCount())
            .addValue("successCount", row.successCount())
            .addValue("failureCount", row.failureCount())
            .addValue("requestedAt", row.requestedAt())
            .addValue("completedAt", row.completedAt())
            .addValue("lastErrorMessage", row.lastErrorMessage())
            .addValue("remarks", row.remarks())
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt());
    }

    private MapSqlParameterSource toHistoricalReportParams(CreateHistoricalReportRow row) {
        return new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("importJobId", row.importJobId())
            .addValue("sourceSystem", row.sourceSystem())
            .addValue("externalReportNo", row.externalReportNo())
            .addValue("patientId", row.patientId())
            .addValue("patientName", row.patientName())
            .addValue("pathologyNo", row.pathologyNo())
            .addValue("applicationNo", row.applicationNo())
            .addValue("reportDate", row.reportDate())
            .addValue("finalDiagnosis", row.finalDiagnosis())
            .addValue("reportSummary", row.reportSummary())
            .addValue("rawPayload", row.rawPayload())
            .addValue("sourceDepartmentName", row.sourceDepartmentName())
            .addValue("sourceDoctorName", row.sourceDoctorName())
            .addValue("attachmentUrl", row.attachmentUrl())
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt());
    }

    private IntegrationTaskRow mapIntegrationTask(ResultSet rs, int rowNum) throws SQLException {
        return new IntegrationTaskRow(
            rs.getString("id"),
            rs.getString("task_type"),
            rs.getString("business_type"),
            rs.getString("business_id"),
            rs.getString("stage_code"),
            rs.getString("external_system"),
            rs.getString("request_payload"),
            rs.getString("response_payload"),
            rs.getString("task_status"),
            rs.getInt("retry_count"),
            rs.getInt("max_retry_count"),
            toLocalDateTime(rs.getTimestamp("next_retry_at")),
            toLocalDateTime(rs.getTimestamp("last_attempt_at")),
            rs.getString("last_error_code"),
            rs.getString("last_error_message"),
            rs.getString("compensation_status"),
            rs.getString("reconciliation_status"),
            toLocalDateTime(rs.getTimestamp("resolved_at")),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private BillingRecordRow mapBillingRecord(ResultSet rs, int rowNum) throws SQLException {
        return new BillingRecordRow(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("order_id"),
            rs.getString("billing_no"),
            rs.getString("billing_stage"),
            rs.getString("item_type"),
            rs.getString("item_name"),
            rs.getBigDecimal("quantity"),
            rs.getBigDecimal("amount"),
            rs.getString("billing_status"),
            toLocalDateTime(rs.getTimestamp("billed_at")),
            rs.getString("operator_user_id"),
            rs.getString("operator_name"),
            rs.getString("external_bill_no"),
            rs.getString("external_system"),
            rs.getString("remarks"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private HistoricalImportJobRow mapHistoricalImportJob(ResultSet rs, int rowNum) throws SQLException {
        return new HistoricalImportJobRow(
            rs.getString("id"),
            rs.getString("source_system"),
            rs.getString("patient_id"),
            rs.getString("pathology_no"),
            rs.getString("application_no"),
            rs.getString("import_status"),
            rs.getString("requested_by_user_id"),
            rs.getString("requested_by_name"),
            rs.getInt("total_count"),
            rs.getInt("success_count"),
            rs.getInt("failure_count"),
            toLocalDateTime(rs.getTimestamp("requested_at")),
            toLocalDateTime(rs.getTimestamp("completed_at")),
            rs.getString("last_error_message"),
            rs.getString("remarks"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private HistoricalReportRow mapHistoricalReport(ResultSet rs, int rowNum) throws SQLException {
        return new HistoricalReportRow(
            rs.getString("id"),
            rs.getString("import_job_id"),
            rs.getString("source_system"),
            rs.getString("external_report_no"),
            rs.getString("patient_id"),
            rs.getString("patient_name"),
            rs.getString("pathology_no"),
            rs.getString("application_no"),
            toLocalDateTime(rs.getTimestamp("report_date")),
            rs.getString("final_diagnosis"),
            rs.getString("report_summary"),
            rs.getString("raw_payload"),
            rs.getString("source_department_name"),
            rs.getString("source_doctor_name"),
            rs.getString("attachment_url"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private HistoricalReportVersionRow mapHistoricalReportVersion(ResultSet rs, int rowNum) throws SQLException {
        return new HistoricalReportVersionRow(
            rs.getString("id"),
            rs.getString("historical_report_id"),
            rs.getInt("version_no"),
            rs.getString("final_diagnosis"),
            rs.getString("report_summary"),
            rs.getString("raw_payload"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private StatIndicatorDefinitionRow mapStatIndicatorDefinition(ResultSet rs, int rowNum) throws SQLException {
        return new StatIndicatorDefinitionRow(
            rs.getString("id"),
            rs.getString("indicator_code"),
            rs.getString("indicator_name"),
            rs.getString("indicator_category"),
            rs.getString("metric_scope"),
            rs.getString("aggregation_type"),
            rs.getString("description"),
            rs.getInt("sort_order"),
            rs.getInt("enabled") == 1,
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private StatReportTemplateRow mapStatReportTemplate(ResultSet rs, int rowNum) throws SQLException {
        return new StatReportTemplateRow(
            rs.getString("id"),
            rs.getString("template_code"),
            rs.getString("template_name"),
            rs.getString("template_type"),
            rs.getString("indicator_code"),
            rs.getString("default_columns"),
            rs.getString("parameter_schema"),
            rs.getInt("sort_order"),
            rs.getInt("enabled") == 1,
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record CreateIntegrationTaskRow(
        String id,
        String taskType,
        String businessType,
        String businessId,
        String stageCode,
        String externalSystem,
        String requestPayload,
        String responsePayload,
        String taskStatus,
        int retryCount,
        int maxRetryCount,
        LocalDateTime nextRetryAt,
        LocalDateTime lastAttemptAt,
        String lastErrorCode,
        String lastErrorMessage,
        String compensationStatus,
        String reconciliationStatus,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record IntegrationTaskRow(
        String id,
        String taskType,
        String businessType,
        String businessId,
        String stageCode,
        String externalSystem,
        String requestPayload,
        String responsePayload,
        String taskStatus,
        int retryCount,
        int maxRetryCount,
        LocalDateTime nextRetryAt,
        LocalDateTime lastAttemptAt,
        String lastErrorCode,
        String lastErrorMessage,
        String compensationStatus,
        String reconciliationStatus,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record CreateBillingRecordRow(
        String id,
        String caseId,
        String orderId,
        String billingNo,
        String billingStage,
        String itemType,
        String itemName,
        BigDecimal quantity,
        BigDecimal amount,
        String billingStatus,
        LocalDateTime billedAt,
        String operatorUserId,
        String operatorName,
        String externalBillNo,
        String externalSystem,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record BillingRecordRow(
        String id,
        String caseId,
        String orderId,
        String billingNo,
        String billingStage,
        String itemType,
        String itemName,
        BigDecimal quantity,
        BigDecimal amount,
        String billingStatus,
        LocalDateTime billedAt,
        String operatorUserId,
        String operatorName,
        String externalBillNo,
        String externalSystem,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record CreateHistoricalImportJobRow(
        String id,
        String sourceSystem,
        String patientId,
        String pathologyNo,
        String applicationNo,
        String importStatus,
        String requestedByUserId,
        String requestedByName,
        int totalCount,
        int successCount,
        int failureCount,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        String lastErrorMessage,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record HistoricalImportJobRow(
        String id,
        String sourceSystem,
        String patientId,
        String pathologyNo,
        String applicationNo,
        String importStatus,
        String requestedByUserId,
        String requestedByName,
        int totalCount,
        int successCount,
        int failureCount,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        String lastErrorMessage,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record CreateHistoricalReportRow(
        String id,
        String importJobId,
        String sourceSystem,
        String externalReportNo,
        String patientId,
        String patientName,
        String pathologyNo,
        String applicationNo,
        LocalDateTime reportDate,
        String finalDiagnosis,
        String reportSummary,
        String rawPayload,
        String sourceDepartmentName,
        String sourceDoctorName,
        String attachmentUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record HistoricalReportRow(
        String id,
        String importJobId,
        String sourceSystem,
        String externalReportNo,
        String patientId,
        String patientName,
        String pathologyNo,
        String applicationNo,
        LocalDateTime reportDate,
        String finalDiagnosis,
        String reportSummary,
        String rawPayload,
        String sourceDepartmentName,
        String sourceDoctorName,
        String attachmentUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record CreateHistoricalReportVersionRow(
        String id,
        String historicalReportId,
        int versionNo,
        String finalDiagnosis,
        String reportSummary,
        String rawPayload,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record HistoricalReportVersionRow(
        String id,
        String historicalReportId,
        int versionNo,
        String finalDiagnosis,
        String reportSummary,
        String rawPayload,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record StatIndicatorDefinitionRow(
        String id,
        String indicatorCode,
        String indicatorName,
        String indicatorCategory,
        String metricScope,
        String aggregationType,
        String description,
        int sortOrder,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record StatReportTemplateRow(
        String id,
        String templateCode,
        String templateName,
        String templateType,
        String indicatorCode,
        String defaultColumns,
        String parameterSchema,
        int sortOrder,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record CreateStatExportJobRow(
        String id,
        String exportNo,
        String templateId,
        String indicatorCode,
        String exportStatus,
        String filterPayload,
        String fileName,
        String contentType,
        String requestedByUserId,
        String requestedByName,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime completedAt
    ) {
    }
}
