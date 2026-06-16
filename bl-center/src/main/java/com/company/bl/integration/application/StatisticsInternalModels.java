package com.company.bl.integration.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

record MetricValue(String value, String unit) {
}

record MetricSnapshot(
    String metricValue,
    String metricUnit,
    String numerator,
    String denominator
) {
}

record QualityDetailDataset(
    String indicatorCode,
    String availabilityStatus,
    long eligibleCount,
    long passCount,
    long failCount,
    String sourceNote,
    List<QualityDetailRecord> items
) {
}

record QualityDetailRecord(
    String pathologyNo,
    String applicationNo,
    String specimenNo,
    LocalDateTime occurredAt,
    String detailStatus,
    String reason,
    BigDecimal numericValue
) {
}

enum CriticalValueMode {
    COUNT,
    REASON_ANALYSIS,
    TIMELINESS
}

enum FrozenTimeoutMode {
    ALL,
    DIAGNOSIS,
    GROSSING,
    SLICING
}

enum ReportChangeMode {
    ALL,
    DOCTOR,
    MODIFICATION_REASON,
    REVISION_REASON
}

record TrendBucket(
    String label,
    LocalDateTime from,
    LocalDateTime to
) {
}

record StatFilter(
    LocalDateTime from,
    LocalDateTime to,
    String departmentId,
    String roleId,
    String workloadUserId,
    String periodMode
) {

    static StatFilter from(StatisticsService.QueryStatReportCommand command) {
        return new StatFilter(
            command.from(),
            command.to(),
            normalizeValue(command.departmentId()),
            normalizeValue(command.roleId()),
            normalizeValue(command.workloadUserId()),
            normalizeValue(command.periodMode()));
    }

    static StatFilter from(StatisticsService.QueryStatReportDetailCommand command) {
        return new StatFilter(
            command.from(),
            command.to(),
            normalizeValue(command.departmentId()),
            null,
            null,
            null);
    }

    StatFilter forCaseScopedMetrics() {
        return new StatFilter(from, to, departmentId, null, null, periodMode);
    }

    StatFilter forWorkloadMetrics() {
        return this;
    }

    StatFilter withRange(LocalDateTime newFrom, LocalDateTime newTo) {
        return new StatFilter(newFrom, newTo, departmentId, roleId, workloadUserId, periodMode);
    }

    private static String normalizeValue(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
