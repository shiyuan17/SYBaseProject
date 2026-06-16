package com.company.bl.integration.application;

import com.company.bl.integration.infrastructure.M6JdbcRepository;
import com.company.bl.integration.infrastructure.M6StatisticsRows;
import com.company.bl.support.application.OperationAuditService;
import com.company.common.web.observability.ObservedOperation;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
class StatisticsComputationSupport extends StatisticsQualitySupportBase {

    private final OperationAuditService operationAuditService;
    private final StatisticsQualitySupport qualitySupport;

    StatisticsComputationSupport(M6JdbcRepository repository,
                                 NamedParameterJdbcTemplate jdbcTemplate,
                                 OperationAuditService operationAuditService,
                                 StatisticsQualitySupport qualitySupport) {
        super(repository, jdbcTemplate);
        this.operationAuditService = operationAuditService;
        this.qualitySupport = qualitySupport;
    }

    public StatisticsService.StatReportResult queryReport(StatisticsService.QueryStatReportCommand command) {
        String category = resolveCategory(command);
        StatFilter filter = StatFilter.from(command);
        List<M6StatisticsRows.StatIndicatorDefinitionRow> indicators = selectIndicators(command, category);
        List<StatisticsService.StatRowView> rows = new ArrayList<>();
        for (M6StatisticsRows.StatIndicatorDefinitionRow indicator : indicators) {
            if ("QUALITY".equals(category)) {
                rows.add(qualitySupport.buildQualityStatRow(indicator, filter));
            } else {
                MetricValue metric = computeNonQualityMetric(indicator.indicatorCode(), filter);
                rows.add(new StatisticsService.StatRowView(
                    indicator.indicatorCode(),
                    indicator.indicatorName(),
                    metric.value(),
                    metric.unit(),
                    STATUS_AVAILABLE,
                    null,
                    null,
                    sourceNoteForNonQualityMetric(indicator.indicatorCode(), filter),
                    buildTrendPoints(indicator.indicatorCode(), filter),
                    List.of()));
            }
        }
        return new StatisticsService.StatReportResult(
            command.templateCode() == null ? category : command.templateCode(),
            List.of(
                "indicatorCode",
                "indicatorName",
                "metricValue",
                "metricUnit",
                "metricStatus",
                "numerator",
                "denominator",
                "sourceNote"),
            rows);
    }

    @ObservedOperation(
        operation = "stat_report_export",
        successCounter = "stat_report_export_total",
        failureCounter = "stat_report_export_failed_total",
        durationMetric = "stat_report_export_duration")
    @Transactional
    public byte[] exportReport(StatisticsService.QueryStatReportCommand command) {
        return operationAuditService.audit("M6", "STAT", "export_stat_report", () -> {
            StatisticsService.StatReportResult report = queryReport(command);
            String exportId = "SEJ-" + UUID.randomUUID();
            String exportNo = "STAT-" + UUID.randomUUID();
            String fileName = (command.templateCode() == null ? "stat-report" : command.templateCode()) + ".csv";
            LocalDateTime now = LocalDateTime.now();
            repository.insertStatExportJob(new M6StatisticsRows.CreateStatExportJobRow(
                exportId,
                exportNo,
                resolveTemplateId(command.templateCode()),
                command.indicatorCode(),
                "RUNNING",
                command.toString(),
                fileName,
                "text/csv;charset=UTF-8",
                command.requestedByUserId(),
                command.requestedByName(),
                null,
                now,
                null));
            try {
                StringBuilder builder = new StringBuilder();
                builder.append('\uFEFF');
                builder.append("indicatorCode,indicatorName,metricValue,metricUnit,metricStatus,numerator,denominator,sourceNote\r\n");
                for (StatisticsService.StatRowView row : report.rows()) {
                    appendCsvRow(builder, Arrays.asList(
                        row.indicatorCode(),
                        row.indicatorName(),
                        row.metricValue(),
                        row.metricUnit(),
                        row.metricStatus(),
                        row.numerator(),
                        row.denominator(),
                        row.sourceNote()));
                }
                repository.completeStatExportJob(exportId, "COMPLETED", null, LocalDateTime.now());
                return builder.toString().getBytes(StandardCharsets.UTF_8);
            } catch (RuntimeException exception) {
                repository.completeStatExportJob(exportId, "FAILED", exception.getMessage(), LocalDateTime.now());
                throw exception;
            }
        }, bytes -> "stat-export", () -> "stat-export", () -> "stat export");
    }

    @ObservedOperation(
        operation = "stat_report_detail_query",
        successCounter = "stat_report_detail_query_total",
        failureCounter = "stat_report_detail_query_failed_total",
        durationMetric = "stat_report_detail_query_duration")
    @Transactional(readOnly = true)
    public StatisticsService.StatReportDetailResult queryReportDetails(StatisticsService.QueryStatReportDetailCommand command) {
        StatFilter filter = StatFilter.from(command);
        QualityDetailDataset dataset = buildQualityDetailDataset(command.indicatorCode(), filter);
        int page = normalizePage(command.page());
        int size = normalizeSize(command.size());
        List<QualityDetailRecord> sortedItems = sortDetailRecords(dataset.items());
        long total = sortedItems.size();
        int fromIndex = Math.min((page - 1) * size, sortedItems.size());
        int toIndex = Math.min(fromIndex + size, sortedItems.size());
        List<StatisticsService.StatReportDetailItemView> pageItems = sortedItems.subList(fromIndex, toIndex).stream()
            .map(item -> new StatisticsService.StatReportDetailItemView(
                item.pathologyNo(),
                item.applicationNo(),
                item.specimenNo(),
                item.occurredAt(),
                item.detailStatus(),
                item.reason()))
            .toList();
        return new StatisticsService.StatReportDetailResult(
            dataset.indicatorCode(),
            dataset.availabilityStatus(),
            dataset.eligibleCount(),
            dataset.passCount(),
            dataset.failCount(),
            dataset.sourceNote(),
            pageItems,
            page,
            size,
            total);
    }

    @ObservedOperation(
        operation = "stat_report_detail_export",
        successCounter = "stat_report_detail_export_total",
        failureCounter = "stat_report_detail_export_failed_total",
        durationMetric = "stat_report_detail_export_duration")
    @Transactional
    public byte[] exportReportDetails(StatisticsService.QueryStatReportDetailCommand command) {
        return operationAuditService.audit("M6", "STAT", "export_stat_report_details", () -> {
            StatisticsService.StatReportDetailResult detailResult = queryReportDetails(new StatisticsService.QueryStatReportDetailCommand(
                command.indicatorCode(),
                command.from(),
                command.to(),
                command.departmentId(),
                1,
                Integer.MAX_VALUE,
                command.requestedByUserId(),
                command.requestedByName()));
            String exportId = "SEJ-" + UUID.randomUUID();
            String exportNo = "STAT-" + UUID.randomUUID();
            String fileName = command.indicatorCode().toLowerCase() + "-details.csv";
            LocalDateTime now = LocalDateTime.now();
            repository.insertStatExportJob(new M6StatisticsRows.CreateStatExportJobRow(
                exportId,
                exportNo,
                null,
                command.indicatorCode(),
                "RUNNING",
                command.toString(),
                fileName,
                "text/csv;charset=UTF-8",
                command.requestedByUserId(),
                command.requestedByName(),
                null,
                now,
                null));
            try {
                StringBuilder builder = new StringBuilder();
                builder.append('\uFEFF');
                builder.append("indicatorCode,availabilityStatus,eligibleCount,passCount,failCount,pathologyNo,applicationNo,specimenNo,occurredAt,detailStatus,reason\r\n");
                for (StatisticsService.StatReportDetailItemView item : detailResult.items()) {
                    appendCsvRow(builder, Arrays.asList(
                        detailResult.indicatorCode(),
                        detailResult.availabilityStatus(),
                        String.valueOf(detailResult.eligibleCount()),
                        String.valueOf(detailResult.passCount()),
                        String.valueOf(detailResult.failCount()),
                        item.pathologyNo(),
                        item.applicationNo(),
                        item.specimenNo(),
                        stringify(item.occurredAt()),
                        item.detailStatus(),
                        item.reason()));
                }
                repository.completeStatExportJob(exportId, "COMPLETED", null, LocalDateTime.now());
                return builder.toString().getBytes(StandardCharsets.UTF_8);
            } catch (RuntimeException exception) {
                repository.completeStatExportJob(exportId, "FAILED", exception.getMessage(), LocalDateTime.now());
                throw exception;
            }
        }, bytes -> "stat-detail-export", () -> "stat-detail-export", () -> "stat detail export");
    }

    private MetricValue computeNonQualityMetric(String indicatorCode, StatFilter filter) {
        StatFilter caseFilter = filter.forCaseScopedMetrics();
        StatFilter workloadFilter = filter.forWorkloadMetrics();
        return switch (indicatorCode) {
            case "OP_CASE_VOLUME" -> countMetric(countPathologyCases(caseFilter));
            case "OP_BILLING_AMOUNT" -> decimalMetric(sumBillingAmount(caseFilter), "CNY");
            case "OP_REAGENT_STOCK_ALERT" -> countMetric(countGlobalReagentWarnings());
            case "OP_PERFORMANCE_WORKLOAD" -> countMetric(
                countWorkloadDiagnosticTasks(workloadFilter) + countWorkloadMedicalOrders(workloadFilter));
            case "WL_DIAGNOSTIC_TASK_COUNT" -> countMetric(countWorkloadDiagnosticTasks(workloadFilter));
            case "WL_MEDICAL_ORDER_COUNT" -> countMetric(countWorkloadMedicalOrders(workloadFilter));
            default -> countMetric(0);
        };
    }

    private List<StatisticsService.TrendPointView> buildTrendPoints(String indicatorCode, StatFilter filter) {
        if (!isTrendSupportedMetric(indicatorCode) || filter.from() == null || filter.to() == null) {
            return List.of();
        }
        List<TrendBucket> buckets = buildTrendBuckets(filter);
        return buckets.stream()
            .map(bucket -> new StatisticsService.TrendPointView(
                bucket.label(),
                computeNonQualityMetric(indicatorCode, filter.withRange(bucket.from(), bucket.to())).value()))
            .toList();
    }

    private boolean isTrendSupportedMetric(String indicatorCode) {
        return switch (indicatorCode) {
            case "OP_CASE_VOLUME",
                 "OP_BILLING_AMOUNT",
                 "OP_PERFORMANCE_WORKLOAD",
                 "WL_DIAGNOSTIC_TASK_COUNT",
                 "WL_MEDICAL_ORDER_COUNT" -> true;
            default -> false;
        };
    }

    @Override
    protected QualityDetailDataset buildQualityDetailDataset(String indicatorCode, StatFilter filter) {
        return qualitySupport.buildQualityDetailDataset(indicatorCode, filter);
    }

    @Override
    protected MetricSnapshot summarizeQualityMetric(String indicatorCode, String aggregationType, QualityDetailDataset dataset) {
        return qualitySupport.summarizeQualityMetric(indicatorCode, aggregationType, dataset);
    }


    private List<QualityDetailRecord> sortDetailRecords(List<QualityDetailRecord> items) {
        List<QualityDetailRecord> sorted = new ArrayList<>(items);
        sorted.sort(
            Comparator
                .comparing(QualityDetailRecord::occurredAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(QualityDetailRecord::pathologyNo, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(QualityDetailRecord::applicationNo, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(QualityDetailRecord::specimenNo, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(QualityDetailRecord::reason, Comparator.nullsLast(Comparator.naturalOrder())));
        return sorted;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        return size == null || size < 1 ? 20 : Math.min(size, Integer.MAX_VALUE);
    }
}
