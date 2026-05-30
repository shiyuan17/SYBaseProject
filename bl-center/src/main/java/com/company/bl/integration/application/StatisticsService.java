package com.company.bl.integration.application;

import com.company.bl.integration.infrastructure.M6JdbcRepository;
import com.company.common.web.observability.ObservedOperation;
import com.company.bl.support.application.OperationAuditService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StatisticsService {

    private final M6JdbcRepository repository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final OperationAuditService operationAuditService;

    public StatisticsService(M6JdbcRepository repository,
                             NamedParameterJdbcTemplate jdbcTemplate,
                             OperationAuditService operationAuditService) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.operationAuditService = operationAuditService;
    }

    @Transactional(readOnly = true)
    public List<IndicatorDefinitionView> listIndicators(String category) {
        return repository.findStatIndicatorDefinitions(category).stream()
            .map(item -> new IndicatorDefinitionView(
                item.id(), item.indicatorCode(), item.indicatorName(), item.indicatorCategory(), item.metricScope(),
                item.aggregationType(), item.description(), item.sortOrder(), item.enabled()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportTemplateView> listTemplates(String templateType) {
        return repository.findStatReportTemplates(templateType).stream()
            .map(item -> new ReportTemplateView(
                item.id(), item.templateCode(), item.templateName(), item.templateType(), item.indicatorCode(),
                item.defaultColumns(), item.parameterSchema(), item.sortOrder(), item.enabled()))
            .toList();
    }

    @ObservedOperation(
        operation = "stat_report_query",
        successCounter = "stat_report_query_total",
        failureCounter = "stat_report_query_failed_total",
        durationMetric = "stat_report_query_duration")
    @Transactional(readOnly = true)
    public StatReportResult queryReport(QueryStatReportCommand command) {
        String category = resolveCategory(command);
        StatFilter filter = StatFilter.from(command);
        List<M6JdbcRepository.StatIndicatorDefinitionRow> indicators = selectIndicators(command, category);
        List<StatRowView> rows = new ArrayList<>();
        for (M6JdbcRepository.StatIndicatorDefinitionRow indicator : indicators) {
            MetricValue metric = computeMetric(indicator.indicatorCode(), filter);
            rows.add(new StatRowView(indicator.indicatorCode(), indicator.indicatorName(), metric.value(), metric.unit()));
        }
        return new StatReportResult(
            command.templateCode() == null ? category : command.templateCode(),
            List.of("indicatorCode", "indicatorName", "metricValue", "metricUnit"),
            rows);
    }

    @ObservedOperation(
        operation = "stat_report_export",
        successCounter = "stat_report_export_total",
        failureCounter = "stat_report_export_failed_total",
        durationMetric = "stat_report_export_duration")
    @Transactional
    public byte[] exportReport(QueryStatReportCommand command) {
        return operationAuditService.audit("M6", "STAT", "export_stat_report", () -> {
            StatReportResult report = queryReport(command);
            LocalDateTime now = LocalDateTime.now();
            String exportId = "SEJ-" + UUID.randomUUID();
            String exportNo = "STAT-" + UUID.randomUUID();
            String fileName = (command.templateCode() == null ? "stat-report" : command.templateCode()) + ".csv";
            repository.insertStatExportJob(new M6JdbcRepository.CreateStatExportJobRow(
                exportId, exportNo, resolveTemplateId(command.templateCode()), command.indicatorCode(),
                "RUNNING", command.toString(), fileName, "text/csv;charset=UTF-8", command.operatorUserId(), command.operatorName(),
                null, now, null));
            try {
                StringBuilder builder = new StringBuilder();
                builder.append('\uFEFF');
                builder.append("indicatorCode,indicatorName,metricValue,metricUnit\r\n");
                for (StatRowView row : report.rows()) {
                    appendCsvRow(builder, List.of(row.indicatorCode(), row.indicatorName(), row.metricValue(), row.metricUnit()));
                }
                repository.completeStatExportJob(exportId, "COMPLETED", null, LocalDateTime.now());
                return builder.toString().getBytes(StandardCharsets.UTF_8);
            } catch (RuntimeException exception) {
                repository.completeStatExportJob(exportId, "FAILED", exception.getMessage(), LocalDateTime.now());
                throw exception;
            }
        }, bytes -> "stat-export", () -> "stat-export", command.operatorUserId(), command.operatorName(), () -> "stat export");
    }

    private List<M6JdbcRepository.StatIndicatorDefinitionRow> selectIndicators(QueryStatReportCommand command, String category) {
        if (command.indicatorCode() != null && !command.indicatorCode().isBlank()) {
            M6JdbcRepository.StatIndicatorDefinitionRow row = repository.findStatIndicatorDefinitionByCode(command.indicatorCode());
            return row == null ? List.of() : List.of(row);
        }
        return repository.findStatIndicatorDefinitions(category);
    }

    private String resolveCategory(QueryStatReportCommand command) {
        if (command.category() != null && !command.category().isBlank()) {
            return command.category();
        }
        if (command.templateCode() != null && !command.templateCode().isBlank()) {
            M6JdbcRepository.StatReportTemplateRow template = repository.findStatReportTemplateByCode(command.templateCode());
            return template == null ? "QUALITY" : template.templateType();
        }
        return "QUALITY";
    }

    private String resolveTemplateId(String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            return null;
        }
        M6JdbcRepository.StatReportTemplateRow template = repository.findStatReportTemplateByCode(templateCode);
        return template == null ? null : template.id();
    }

    private MetricValue computeMetric(String indicatorCode, StatFilter filter) {
        StatFilter caseFilter = filter.forCaseScopedMetrics();
        StatFilter workloadFilter = filter.forWorkloadMetrics();
        return switch (indicatorCode) {
            case "QC_SPECIMEN_FIXATION_RATE" -> percent(countPublishedReports(caseFilter), countPathologyCases(caseFilter));
            case "QC_UNQUALIFIED_SPECIMEN_COUNT" -> countMetric(countRevisionRequests(caseFilter));
            case "QC_CLINICAL_MATCH_RATE" -> percent(countPublishedReports(caseFilter), countDiagnosticTasks(caseFilter));
            case "QC_FIRST_LINE_MATCH_RATE" -> percent(countCompletedMedicalOrders(caseFilter), Math.max(countPathologyCases(caseFilter), 1));
            case "QC_FROZEN_PARAFFIN_MATCH_RATE" -> percent(countCompletedConsultations(caseFilter), Math.max(countPublishedReports(caseFilter), 1));
            case "QC_CYTOLOGY_MATCH_RATE" -> percent(countPublishedReports(caseFilter), Math.max(countCompletedConsultations(caseFilter) + 1, 1));
            case "QC_CONSULTATION_MATCH_RATE" -> percent(countCompletedConsultations(caseFilter), Math.max(countDiagnosticTasks(caseFilter), 1));
            case "QC_CANCELLED_REVIEW_COUNT" -> countMetric(countRevisionRequests(caseFilter));
            case "QC_TECHNICAL_QUALITY_COUNT" -> countMetric(countCompletedMedicalOrders(caseFilter));
            case "QC_GROSSING_QUALITY_COUNT" -> countMetric(countPathologyCases(caseFilter));
            case "QC_REPORT_RELEASE_DAYS" -> decimalMetric(BigDecimal.valueOf(countPublishedReports(caseFilter)), "case");
            case "QC_SPECIMEN_PROCESS_HOURS" -> decimalMetric(BigDecimal.valueOf(countCompletedMedicalOrders(caseFilter)), "case");
            case "QC_DIAGNOSIS_TIMELINESS_RATE" -> percent(countPublishedReports(caseFilter), Math.max(countPathologyCases(caseFilter), 1));
            case "OP_CASE_VOLUME" -> countMetric(countPathologyCases(caseFilter));
            case "OP_BILLING_AMOUNT" -> decimalMetric(sumBillingAmount(caseFilter), "CNY");
            case "OP_REAGENT_STOCK_ALERT" -> countMetric(countGlobalReagentWarnings());
            case "OP_PERFORMANCE_WORKLOAD" -> countMetric(countWorkloadDiagnosticTasks(workloadFilter) + countWorkloadMedicalOrders(workloadFilter));
            case "WL_DIAGNOSTIC_TASK_COUNT" -> countMetric(countWorkloadDiagnosticTasks(workloadFilter));
            case "WL_MEDICAL_ORDER_COUNT" -> countMetric(countWorkloadMedicalOrders(workloadFilter));
            default -> countMetric(0);
        };
    }

    private long countPathologyCases(StatFilter filter) {
        return queryForLong("""
            select count(*)
            from pathology_cases pc
            join applications a on a.id = pc.application_id
            where (:fromTime is null or coalesce(pc.received_at, pc.created_at) >= :fromTime)
              and (:toTime is null or coalesce(pc.received_at, pc.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """, toParams(filter));
    }

    private long countPublishedReports(StatFilter filter) {
        return queryForLong("""
            select count(*)
            from pathology_reports pr
            join pathology_cases pc on pc.id = pr.case_id
            join applications a on a.id = pc.application_id
            where pr.report_status = 'PUBLISHED'
              and (:fromTime is null or pr.published_at >= :fromTime)
              and (:toTime is null or pr.published_at <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """, toParams(filter));
    }

    private long countCompletedMedicalOrders(StatFilter filter) {
        return queryForLong("""
            select count(*)
            from medical_orders mo
            join pathology_cases pc on pc.id = mo.case_id
            join applications a on a.id = pc.application_id
            where mo.status = 'COMPLETED'
              and (:fromTime is null or mo.completed_at >= :fromTime)
              and (:toTime is null or mo.completed_at <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """, toParams(filter));
    }

    private long countCompletedConsultations(StatFilter filter) {
        return queryForLong("""
            select count(*)
            from consultation_cases cc
            join pathology_cases pc on pc.id = cc.case_id
            join applications a on a.id = pc.application_id
            where cc.status = 'COMPLETED'
              and (:fromTime is null or cc.completed_at >= :fromTime)
              and (:toTime is null or cc.completed_at <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """, toParams(filter));
    }

    private long countRevisionRequests(StatFilter filter) {
        return queryForLong("""
            select count(*)
            from report_revision_requests rrr
            join pathology_cases pc on pc.id = rrr.case_id
            join applications a on a.id = pc.application_id
            where (:fromTime is null or rrr.requested_at >= :fromTime)
              and (:toTime is null or rrr.requested_at <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """, toParams(filter));
    }

    private long countDiagnosticTasks(StatFilter filter) {
        return queryForLong("""
            select count(*)
            from diagnostic_tasks dt
            join pathology_cases pc on pc.id = dt.case_id
            join applications a on a.id = pc.application_id
            where (:fromTime is null or dt.created_at >= :fromTime)
              and (:toTime is null or dt.created_at <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """, toParams(filter));
    }

    private BigDecimal sumBillingAmount(StatFilter filter) {
        return queryForDecimal("""
            select sum(br.amount)
            from billing_records br
            join pathology_cases pc on pc.id = br.case_id
            join applications a on a.id = pc.application_id
            where br.billing_status = 'SUCCESS'
              and (:fromTime is null or coalesce(br.billed_at, br.created_at) >= :fromTime)
              and (:toTime is null or coalesce(br.billed_at, br.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """, toParams(filter));
    }

    private long countGlobalReagentWarnings() {
        return queryForLong("""
            select count(*)
            from reagent_stocks
            where low_stock_threshold is not null and stock_quantity <= low_stock_threshold
            """, new MapSqlParameterSource());
    }

    private long countWorkloadDiagnosticTasks(StatFilter filter) {
        return queryForLong("""
            select count(*)
            from diagnostic_tasks dt
            join pathology_cases pc on pc.id = dt.case_id
            join applications a on a.id = pc.application_id
            where (:fromTime is null or dt.created_at >= :fromTime)
              and (:toTime is null or dt.created_at <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
              and (:operatorUserId is null or dt.primary_doctor_user_id = :operatorUserId)
              and (:roleId is null or exists (
                    select 1
                    from user_roles ur
                    where ur.user_id = dt.primary_doctor_user_id
                      and ur.role_id = :roleId
                ))
            """, toParams(filter));
    }

    private long countWorkloadMedicalOrders(StatFilter filter) {
        return queryForLong("""
            select count(*)
            from medical_orders mo
            join pathology_cases pc on pc.id = mo.case_id
            join applications a on a.id = pc.application_id
            where (:fromTime is null or coalesce(mo.completed_at, mo.created_at) >= :fromTime)
              and (:toTime is null or coalesce(mo.completed_at, mo.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
              and (:operatorUserId is null or coalesce(mo.executor_user_id, mo.doctor_user_id) = :operatorUserId)
              and (:roleId is null or exists (
                    select 1
                    from user_roles ur
                    where ur.user_id = coalesce(mo.executor_user_id, mo.doctor_user_id)
                      and ur.role_id = :roleId
                ))
            """, toParams(filter));
    }

    private long queryForLong(String sql, MapSqlParameterSource params) {
        Long value = jdbcTemplate.queryForObject(sql, params, Long.class);
        return value == null ? 0L : value;
    }

    private BigDecimal queryForDecimal(String sql, MapSqlParameterSource params) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    private MapSqlParameterSource toParams(StatFilter filter) {
        return new MapSqlParameterSource()
            .addValue("fromTime", filter.from())
            .addValue("toTime", filter.to())
            .addValue("departmentId", filter.departmentId())
            .addValue("roleId", filter.roleId())
            .addValue("operatorUserId", filter.operatorUserId());
    }

    private MetricValue countMetric(long count) {
        return new MetricValue(String.valueOf(count), "COUNT");
    }

    private MetricValue decimalMetric(BigDecimal value, String unit) {
        return new MetricValue(value.setScale(2, RoundingMode.HALF_UP).toPlainString(), unit);
    }

    private MetricValue percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return new MetricValue("0.00", "PERCENT");
        }
        BigDecimal percent = BigDecimal.valueOf(numerator)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
        return new MetricValue(percent.toPlainString(), "PERCENT");
    }

    private void appendCsvRow(StringBuilder builder, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            String value = values.get(i);
            if (value == null) {
                continue;
            }
            String escaped = value.replace("\"", "\"\"");
            builder.append('"').append(escaped).append('"');
        }
        builder.append("\r\n");
    }

    private record MetricValue(String value, String unit) {
    }

    public record IndicatorDefinitionView(
        String id,
        String indicatorCode,
        String indicatorName,
        String indicatorCategory,
        String metricScope,
        String aggregationType,
        String description,
        int sortOrder,
        boolean enabled
    ) {
    }

    public record ReportTemplateView(
        String id,
        String templateCode,
        String templateName,
        String templateType,
        String indicatorCode,
        String defaultColumns,
        String parameterSchema,
        int sortOrder,
        boolean enabled
    ) {
    }

    public record QueryStatReportCommand(
        String templateCode,
        String indicatorCode,
        String category,
        LocalDateTime from,
        LocalDateTime to,
        String departmentId,
        String roleId,
        String operatorUserId,
        String operatorName
    ) {
    }

    public record StatReportResult(
        String reportCode,
        List<String> columns,
        List<StatRowView> rows
    ) {
    }

    public record StatRowView(
        String indicatorCode,
        String indicatorName,
        String metricValue,
        String metricUnit
    ) {
    }

    private record StatFilter(
        LocalDateTime from,
        LocalDateTime to,
        String departmentId,
        String roleId,
        String operatorUserId
    ) {

        private static StatFilter from(QueryStatReportCommand command) {
            return new StatFilter(
                command.from(),
                command.to(),
                normalize(command.departmentId()),
                normalize(command.roleId()),
                normalize(command.operatorUserId()));
        }

        private StatFilter forCaseScopedMetrics() {
            return new StatFilter(from, to, departmentId, null, null);
        }

        private StatFilter forWorkloadMetrics() {
            return this;
        }

        private static String normalize(String value) {
            return value == null || value.isBlank() ? null : value;
        }
    }
}
