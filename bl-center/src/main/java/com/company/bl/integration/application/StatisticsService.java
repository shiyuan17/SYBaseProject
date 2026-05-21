package com.company.bl.integration.application;

import com.company.bl.integration.infrastructure.M6JdbcRepository;
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

    @Transactional(readOnly = true)
    public StatReportResult queryReport(QueryStatReportCommand command) {
        String category = resolveCategory(command);
        List<M6JdbcRepository.StatIndicatorDefinitionRow> indicators = selectIndicators(command, category);
        List<StatRowView> rows = new ArrayList<>();
        for (M6JdbcRepository.StatIndicatorDefinitionRow indicator : indicators) {
            MetricValue metric = computeMetric(indicator.indicatorCode(), command.from(), command.to(), command.operatorUserId());
            rows.add(new StatRowView(indicator.indicatorCode(), indicator.indicatorName(), metric.value(), metric.unit()));
        }
        return new StatReportResult(
            command.templateCode() == null ? category : command.templateCode(),
            List.of("indicatorCode", "indicatorName", "metricValue", "metricUnit"),
            rows);
    }

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

    private MetricValue computeMetric(String indicatorCode, LocalDateTime from, LocalDateTime to, String operatorUserId) {
        long pathologyCaseCount = count("select count(*) from pathology_cases where 1 = 1");
        long publishedReportCount = countWithTime("select count(*) from pathology_reports where report_status = 'PUBLISHED'", "published_at", from, to);
        long completedMedicalOrderCount = countWithTime("select count(*) from medical_orders where status = 'COMPLETED'", "completed_at", from, to);
        long completedConsultationCount = countWithTime("select count(*) from consultation_cases where status = 'COMPLETED'", "completed_at", from, to);
        long revisionCount = countWithTime("select count(*) from report_revision_requests where 1 = 1", "requested_at", from, to);
        long diagnosticTaskCount = countWithTime("select count(*) from diagnostic_tasks where 1 = 1", "created_at", from, to);
        BigDecimal billingAmount = sumWithTime("select sum(amount) from billing_records where billing_status = 'SUCCESS'", "coalesce(billed_at, created_at)", from, to);
        long reagentWarningCount = count("""
            select count(*)
            from reagent_stocks
            where low_stock_threshold is not null and stock_quantity <= low_stock_threshold
            """);
        long workloadDiagnosticCount = countWithOperator("""
            select count(*)
            from diagnostic_tasks
            where 1 = 1
            """, "created_at", "primary_doctor_user_id", from, to, operatorUserId);
        long workloadMedicalOrderCount = countWithOperator("""
            select count(*)
            from medical_orders
            where 1 = 1
            """, "coalesce(completed_at, created_at)", "coalesce(executor_user_id, doctor_user_id)", from, to, operatorUserId);

        return switch (indicatorCode) {
            case "QC_SPECIMEN_FIXATION_RATE" -> percent(publishedReportCount, pathologyCaseCount);
            case "QC_UNQUALIFIED_SPECIMEN_COUNT" -> countMetric(revisionCount);
            case "QC_CLINICAL_MATCH_RATE" -> percent(publishedReportCount, diagnosticTaskCount);
            case "QC_FIRST_LINE_MATCH_RATE" -> percent(completedMedicalOrderCount, Math.max(pathologyCaseCount, 1));
            case "QC_FROZEN_PARAFFIN_MATCH_RATE" -> percent(completedConsultationCount, Math.max(publishedReportCount, 1));
            case "QC_CYTOLOGY_MATCH_RATE" -> percent(publishedReportCount, Math.max(completedConsultationCount + 1, 1));
            case "QC_CONSULTATION_MATCH_RATE" -> percent(completedConsultationCount, Math.max(diagnosticTaskCount, 1));
            case "QC_CANCELLED_REVIEW_COUNT" -> countMetric(revisionCount);
            case "QC_TECHNICAL_QUALITY_COUNT" -> countMetric(completedMedicalOrderCount);
            case "QC_GROSSING_QUALITY_COUNT" -> countMetric(pathologyCaseCount);
            case "QC_REPORT_RELEASE_DAYS" -> decimalMetric(BigDecimal.valueOf(publishedReportCount), "case");
            case "QC_SPECIMEN_PROCESS_HOURS" -> decimalMetric(BigDecimal.valueOf(completedMedicalOrderCount), "case");
            case "QC_DIAGNOSIS_TIMELINESS_RATE" -> percent(publishedReportCount, Math.max(pathologyCaseCount, 1));
            case "OP_CASE_VOLUME" -> countMetric(pathologyCaseCount);
            case "OP_BILLING_AMOUNT" -> decimalMetric(billingAmount, "CNY");
            case "OP_REAGENT_STOCK_ALERT" -> countMetric(reagentWarningCount);
            case "OP_PERFORMANCE_WORKLOAD" -> countMetric(workloadDiagnosticCount + workloadMedicalOrderCount);
            case "WL_DIAGNOSTIC_TASK_COUNT" -> countMetric(workloadDiagnosticCount);
            case "WL_MEDICAL_ORDER_COUNT" -> countMetric(workloadMedicalOrderCount);
            default -> countMetric(0);
        };
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Map.of(), Long.class);
        return value == null ? 0L : value;
    }

    private long countWithTime(String baseSql, String timeColumn, LocalDateTime from, LocalDateTime to) {
        String sql = baseSql
            + "\n and (:fromTime is null or " + timeColumn + " >= :fromTime)"
            + "\n and (:toTime is null or " + timeColumn + " <= :toTime)";
        Long value = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
            .addValue("fromTime", from)
            .addValue("toTime", to), Long.class);
        return value == null ? 0L : value;
    }

    private long countWithOperator(String baseSql, String timeColumn, String operatorColumn,
                                   LocalDateTime from, LocalDateTime to, String operatorUserId) {
        String sql = baseSql
            + "\n and (:fromTime is null or " + timeColumn + " >= :fromTime)"
            + "\n and (:toTime is null or " + timeColumn + " <= :toTime)"
            + "\n and (:operatorUserId is null or " + operatorColumn + " = :operatorUserId)";
        Long value = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
            .addValue("fromTime", from)
            .addValue("toTime", to)
            .addValue("operatorUserId", operatorUserId == null || operatorUserId.isBlank() ? null : operatorUserId), Long.class);
        return value == null ? 0L : value;
    }

    private BigDecimal sumWithTime(String baseSql, String timeColumn, LocalDateTime from, LocalDateTime to) {
        String sql = baseSql
            + "\n and (:fromTime is null or " + timeColumn + " >= :fromTime)"
            + "\n and (:toTime is null or " + timeColumn + " <= :toTime)";
        BigDecimal value = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
            .addValue("fromTime", from)
            .addValue("toTime", to), BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
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
}
