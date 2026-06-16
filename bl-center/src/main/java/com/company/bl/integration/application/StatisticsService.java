package com.company.bl.integration.application;

import com.company.bl.integration.infrastructure.M6JdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StatisticsService {

    private final M6JdbcRepository repository;
    private final StatisticsComputationSupport computationSupport;

    public StatisticsService(M6JdbcRepository repository,
                             StatisticsComputationSupport computationSupport) {
        this.repository = repository;
        this.computationSupport = computationSupport;
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

    public StatReportResult queryReport(QueryStatReportCommand command) {
        return computationSupport.queryReport(command);
    }

    public byte[] exportReport(QueryStatReportCommand command) {
        return computationSupport.exportReport(command);
    }

    public StatReportDetailResult queryReportDetails(QueryStatReportDetailCommand command) {
        return computationSupport.queryReportDetails(command);
    }

    public byte[] exportReportDetails(QueryStatReportDetailCommand command) {
        return computationSupport.exportReportDetails(command);
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
        String workloadUserId,
        String periodMode,
        String requestedByUserId,
        String requestedByName
    ) {
    }

    public record QueryStatReportDetailCommand(
        String indicatorCode,
        LocalDateTime from,
        LocalDateTime to,
        String departmentId,
        Integer page,
        Integer size,
        String requestedByUserId,
        String requestedByName
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
        String metricUnit,
        String metricStatus,
        String numerator,
        String denominator,
        String sourceNote,
        List<TrendPointView> trendPoints,
        List<BreakdownView> breakdowns
    ) {
    }

    public record TrendPointView(
        String label,
        String value
    ) {
    }

    public record BreakdownView(
        String label,
        String value
    ) {
    }

    public record StatReportDetailResult(
        String indicatorCode,
        String availabilityStatus,
        long eligibleCount,
        long passCount,
        long failCount,
        String sourceNote,
        List<StatReportDetailItemView> items,
        int page,
        int size,
        long total
    ) {
    }

    public record StatReportDetailItemView(
        String pathologyNo,
        String applicationNo,
        String specimenNo,
        LocalDateTime occurredAt,
        String detailStatus,
        String reason
    ) {
    }
}
