package com.company.bl.integration.application;

import com.company.bl.integration.infrastructure.M6JdbcRepository;
import com.company.bl.integration.infrastructure.M6StatisticsRows;
import com.company.bl.support.application.OperationAuditService;
import com.company.common.web.observability.ObservedOperation;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StatisticsService {

    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNAVAILABLE = "UNAVAILABLE";

    private static final String DETAIL_STATUS_FAIL = "FAIL";
    private static final String DETAIL_STATUS_INFO = "INFO";
    private static final String DETAIL_STATUS_PASS = "PASS";

    private static final int DEFAULT_FROZEN_GROSSING_SLA_MINUTES = 30;
    private static final int DEFAULT_FROZEN_SLICING_SLA_MINUTES = 30;
    private static final int DEFAULT_FROZEN_DIAGNOSIS_SLA_MINUTES = 30;

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
        List<M6StatisticsRows.StatIndicatorDefinitionRow> indicators = selectIndicators(command, category);
        List<StatRowView> rows = new ArrayList<>();
        for (M6StatisticsRows.StatIndicatorDefinitionRow indicator : indicators) {
            if ("QUALITY".equals(category)) {
                rows.add(buildQualityStatRow(indicator, filter));
            } else {
                MetricValue metric = computeNonQualityMetric(indicator.indicatorCode(), filter);
                rows.add(new StatRowView(
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
        return new StatReportResult(
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
    public byte[] exportReport(QueryStatReportCommand command) {
        return operationAuditService.audit("M6", "STAT", "export_stat_report", () -> {
            StatReportResult report = queryReport(command);
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
                for (StatRowView row : report.rows()) {
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
    public StatReportDetailResult queryReportDetails(QueryStatReportDetailCommand command) {
        StatFilter filter = StatFilter.from(command);
        QualityDetailDataset dataset = buildQualityDetailDataset(command.indicatorCode(), filter);
        int page = normalizePage(command.page());
        int size = normalizeSize(command.size());
        List<QualityDetailRecord> sortedItems = sortDetailRecords(dataset.items());
        long total = sortedItems.size();
        int fromIndex = Math.min((page - 1) * size, sortedItems.size());
        int toIndex = Math.min(fromIndex + size, sortedItems.size());
        List<StatReportDetailItemView> pageItems = sortedItems.subList(fromIndex, toIndex).stream()
            .map(item -> new StatReportDetailItemView(
                item.pathologyNo(),
                item.applicationNo(),
                item.specimenNo(),
                item.occurredAt(),
                item.detailStatus(),
                item.reason()))
            .toList();
        return new StatReportDetailResult(
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
    public byte[] exportReportDetails(QueryStatReportDetailCommand command) {
        return operationAuditService.audit("M6", "STAT", "export_stat_report_details", () -> {
            StatReportDetailResult detailResult = queryReportDetails(new QueryStatReportDetailCommand(
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
                for (StatReportDetailItemView item : detailResult.items()) {
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

    private StatRowView buildQualityStatRow(M6StatisticsRows.StatIndicatorDefinitionRow indicator, StatFilter filter) {
        QualityDetailDataset dataset = buildQualityDetailDataset(indicator.indicatorCode(), filter);
        MetricSnapshot metric = summarizeQualityMetric(indicator.indicatorCode(), indicator.aggregationType(), dataset);
        return new StatRowView(
            indicator.indicatorCode(),
            indicator.indicatorName(),
            metric.metricValue(),
            metric.metricUnit(),
            dataset.availabilityStatus(),
            metric.numerator(),
            metric.denominator(),
            dataset.sourceNote(),
            buildQualityTrendPoints(indicator.indicatorCode(), indicator.aggregationType(), filter),
            buildQualityBreakdowns(dataset));
    }

    private MetricSnapshot summarizeQualityMetric(String indicatorCode,
                                                 String aggregationType,
                                                 QualityDetailDataset dataset) {
        if (STATUS_UNAVAILABLE.equals(dataset.availabilityStatus())) {
            return new MetricSnapshot("0", "COUNT", null, null);
        }
        if ("RATE".equalsIgnoreCase(aggregationType)) {
            return new MetricSnapshot(
                formatDecimal(percentValue(dataset.passCount(), dataset.eligibleCount())),
                "PERCENT",
                String.valueOf(dataset.passCount()),
                String.valueOf(dataset.eligibleCount()));
        }
        if ("AVG".equalsIgnoreCase(aggregationType)) {
            List<BigDecimal> values = dataset.items().stream()
                .map(QualityDetailRecord::numericValue)
                .filter(value -> value != null)
                .toList();
            BigDecimal average = average(values);
            String unit = switch (indicatorCode) {
            case "QC_REPORT_RELEASE_DAYS" -> "天";
            case "QC_SPECIMEN_PROCESS_HOURS" -> "小时";
            default -> "AVG";
            };
            return new MetricSnapshot(formatDecimal(average), unit, null, null);
        }
        long countValue = switch (indicatorCode) {
            case "QC_UNQUALIFIED_SPECIMEN_COUNT" -> dataset.failCount();
            case "QC_FROZEN_TIMEOUT_COUNT",
                 "QC_FROZEN_GROSSING_TIMEOUT_COUNT",
                 "QC_FROZEN_SLICING_TIMEOUT_COUNT",
                 "QC_FROZEN_DIAGNOSIS_TIMEOUT_COUNT" -> dataset.failCount();
            case "QC_REPORT_CHANGE_DOCTOR_COUNT" -> dataset.items().stream()
                .map(QualityDetailRecord::reason)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .count();
            default -> dataset.items().size();
        };
        return new MetricSnapshot(String.valueOf(countValue), "COUNT", null, null);
    }

    private QualityDetailDataset buildQualityDetailDataset(String indicatorCode, StatFilter filter) {
        return switch (normalize(indicatorCode)) {
            case "QC_SPECIMEN_FIXATION_RATE" -> buildSpecimenFixationDataset(filter);
            case "QC_UNQUALIFIED_SPECIMEN_COUNT" -> buildUnqualifiedSpecimenDataset(filter);
            case "QC_CLINICAL_MATCH_RATE" -> buildClinicalMatchDataset(filter);
            case "QC_FIRST_LINE_MATCH_RATE" -> buildFirstLineMatchDataset(filter);
            case "QC_FROZEN_PARAFFIN_MATCH_RATE" -> buildFrozenParaffinMatchDataset(filter);
            case "QC_CYTOLOGY_MATCH_RATE" -> buildCytologyMatchDataset(filter);
            case "QC_CONSULTATION_MATCH_RATE" -> buildConsultationMatchDataset(filter);
            case "QC_CANCELLED_REVIEW_COUNT" -> buildCancelledReviewDataset(filter);
            case "QC_TECHNICAL_QUALITY_COUNT" -> buildTechnicalQualityDataset(filter);
            case "QC_GROSSING_QUALITY_COUNT" -> buildGrossingQualityDataset(filter);
            case "QC_REPORT_RELEASE_DAYS" -> buildReportReleaseDaysDataset(filter);
            case "QC_SPECIMEN_PROCESS_HOURS" -> buildSpecimenProcessHoursDataset(filter);
            case "QC_DIAGNOSIS_TIMELINESS_RATE" -> buildDiagnosisTimelinessDataset(filter);
            case "QC_FROZEN_DIAGNOSIS_TIMELINESS_RATE" -> buildFrozenDiagnosisTimelinessDataset(filter);
            case "QC_FROZEN_TIMEOUT_COUNT" -> buildFrozenTimeoutDataset("QC_FROZEN_TIMEOUT_COUNT", filter, FrozenTimeoutMode.ALL);
            case "QC_FROZEN_GROSSING_TIMEOUT_COUNT" -> buildFrozenTimeoutDataset("QC_FROZEN_GROSSING_TIMEOUT_COUNT", filter, FrozenTimeoutMode.GROSSING);
            case "QC_FROZEN_SLICING_TIMEOUT_COUNT" -> buildFrozenTimeoutDataset("QC_FROZEN_SLICING_TIMEOUT_COUNT", filter, FrozenTimeoutMode.SLICING);
            case "QC_FROZEN_DIAGNOSIS_TIMEOUT_COUNT" -> buildFrozenTimeoutDataset("QC_FROZEN_DIAGNOSIS_TIMEOUT_COUNT", filter, FrozenTimeoutMode.DIAGNOSIS);
            case "QC_REPORT_CHANGE_COUNT" -> buildReportChangeDataset("QC_REPORT_CHANGE_COUNT", filter, ReportChangeMode.ALL);
            case "QC_REPORT_CHANGE_DOCTOR_COUNT" -> buildReportChangeDataset("QC_REPORT_CHANGE_DOCTOR_COUNT", filter, ReportChangeMode.DOCTOR);
            case "QC_REPORT_MODIFICATION_REASON_COUNT" -> buildReportChangeDataset("QC_REPORT_MODIFICATION_REASON_COUNT", filter, ReportChangeMode.MODIFICATION_REASON);
            case "QC_REPORT_REVISION_REASON_COUNT" -> buildReportChangeDataset("QC_REPORT_REVISION_REASON_COUNT", filter, ReportChangeMode.REVISION_REASON);
            case "QC_UNQUALIFIED_SPECIMEN_RATE" -> buildUnqualifiedSpecimenRateDataset(filter);
            case "QC_UNQUALIFIED_SPECIMEN_REASON_COUNT" -> buildUnqualifiedSpecimenReasonDataset(filter);
            case "QC_CRITICAL_VALUE_COUNT" -> buildCriticalValueNotificationDataset(
                "QC_CRITICAL_VALUE_COUNT",
                filter,
                CriticalValueMode.COUNT);
            case "QC_CRITICAL_VALUE_REPORT_TIMELINESS_RATE" -> buildCriticalValueNotificationDataset(
                "QC_CRITICAL_VALUE_REPORT_TIMELINESS_RATE",
                filter,
                CriticalValueMode.TIMELINESS);
            case "QC_CRITICAL_VALUE_REASON_ANALYSIS_COUNT" -> buildCriticalValueNotificationDataset(
                "QC_CRITICAL_VALUE_REASON_ANALYSIS_COUNT",
                filter,
                CriticalValueMode.REASON_ANALYSIS);
            default -> new QualityDetailDataset(
                indicatorCode,
                STATUS_UNAVAILABLE,
                0,
                0,
                0,
                "当前指标未配置可计算明细代理规则。",
                List.of());
        };
    }

    private QualityDetailDataset buildSpecimenFixationDataset(StatFilter filter) {
        String sql = """
            select coalesce(pc.pathology_no, '') as pathology_no,
                   a.application_no,
                   s.specimen_no,
                   coalesce(sfr.fixation_completed_at, sfr.verified_at, s.registered_at, s.created_at) as occurred_at,
                   coalesce(sfr.fixation_status, s.fixation_status) as fixation_status,
                   s.unqualified_reason
            from specimens s
            join applications a on a.id = s.application_id
            left join pathology_cases pc on pc.id = s.case_id
            left join specimen_fixation_records sfr on sfr.specimen_id = s.id
            where (:fromTime is null or coalesce(s.registered_at, s.created_at) >= :fromTime)
              and (:toTime is null or coalesce(s.registered_at, s.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """;
        List<QualityDetailRecord> items = jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> {
            String fixationStatus = normalize(rs.getString("fixation_status"));
            String unqualifiedReason = normalize(rs.getString("unqualified_reason"));
            boolean passed = "COMPLETED".equals(fixationStatus) && unqualifiedReason == null;
            String reason = passed
                ? "固定完成且无不合格原因"
                : unqualifiedReason != null
                    ? "存在不合格原因：" + unqualifiedReason
                    : "ABNORMAL".equals(fixationStatus)
                        ? "固定状态异常"
                        : "固定未完成";
            return new QualityDetailRecord(
                normalizeEmpty(rs.getString("pathology_no")),
                normalizeEmpty(rs.getString("application_no")),
                normalizeEmpty(rs.getString("specimen_no")),
                toLocalDateTime(rs.getObject("occurred_at")),
                passed ? DETAIL_STATUS_PASS : DETAIL_STATUS_FAIL,
                reason,
                null);
        });
        return datasetFromRateItems("QC_SPECIMEN_FIXATION_RATE", sourceNoteFor("QC_SPECIMEN_FIXATION_RATE"), items);
    }

    private QualityDetailDataset buildUnqualifiedSpecimenDataset(StatFilter filter) {
        String sql = """
            select coalesce(pc.pathology_no, '') as pathology_no,
                   a.application_no,
                   s.specimen_no,
                   coalesce(sfr.verified_at, s.registered_at, s.created_at) as occurred_at,
                   s.qualified_flag,
                   coalesce(sfr.fixation_status, s.fixation_status) as fixation_status,
                   s.unqualified_reason
            from specimens s
            join applications a on a.id = s.application_id
            left join pathology_cases pc on pc.id = s.case_id
            left join specimen_fixation_records sfr on sfr.specimen_id = s.id
            where (:fromTime is null or coalesce(s.registered_at, s.created_at) >= :fromTime)
              and (:toTime is null or coalesce(s.registered_at, s.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
              and (
                    coalesce(s.qualified_flag, 1) = 0
                 or trim(coalesce(s.unqualified_reason, '')) <> ''
                 or coalesce(sfr.fixation_status, s.fixation_status) = 'ABNORMAL'
              )
            """;
        List<QualityDetailRecord> failItems = jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> {
            String reason = normalize(rs.getString("unqualified_reason"));
            if (reason == null && "ABNORMAL".equals(normalize(rs.getString("fixation_status")))) {
                reason = "固定状态异常";
            }
            if (reason == null && rs.getInt("qualified_flag") == 0) {
                reason = "qualified_flag=0";
            }
            return new QualityDetailRecord(
                normalizeEmpty(rs.getString("pathology_no")),
                normalizeEmpty(rs.getString("application_no")),
                normalizeEmpty(rs.getString("specimen_no")),
                toLocalDateTime(rs.getObject("occurred_at")),
                DETAIL_STATUS_FAIL,
                reason == null ? "标本质控不合格" : reason,
                null);
        });
        long eligibleCount = countEligibleSpecimens(filter);
        long failCount = failItems.size();
        long passCount = Math.max(eligibleCount - failCount, 0);
        return new QualityDetailDataset(
            "QC_UNQUALIFIED_SPECIMEN_COUNT",
            STATUS_AVAILABLE,
            eligibleCount,
            passCount,
            failCount,
            sourceNoteFor("QC_UNQUALIFIED_SPECIMEN_COUNT"),
            failItems);
    }

    private QualityDetailDataset buildClinicalMatchDataset(StatFilter filter) {
        String sql = """
            select pc.pathology_no,
                   a.application_no,
                   coalesce(
                       (
                           select coalesce(pr.published_at, pr.reviewed_at, pr.submitted_at, pr.created_at)
                           from pathology_reports pr
                           where pr.case_id = pc.id
                           order by coalesce(pr.published_at, pr.reviewed_at, pr.submitted_at, pr.created_at) desc
                           fetch first 1 row only
                       ),
                       pc.received_at,
                       pc.created_at
                   ) as occurred_at,
                   a.clinical_diagnosis,
                   (
                       select pr.final_diagnosis
                       from pathology_reports pr
                       where pr.case_id = pc.id
                       order by coalesce(pr.published_at, pr.reviewed_at, pr.submitted_at, pr.created_at) desc
                       fetch first 1 row only
                   ) as final_diagnosis
            from pathology_cases pc
            join applications a on a.id = pc.application_id
            where (:fromTime is null or coalesce(pc.received_at, pc.created_at) >= :fromTime)
              and (:toTime is null or coalesce(pc.received_at, pc.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """;
        List<QualityDetailRecord> items = jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> {
            String clinicalDiagnosis = normalize(rs.getString("clinical_diagnosis"));
            String finalDiagnosis = normalize(rs.getString("final_diagnosis"));
            boolean passed = clinicalDiagnosis != null && finalDiagnosis != null;
            String reason;
            if (passed) {
                reason = "临床诊断与病理最终诊断信息齐备";
            } else if (clinicalDiagnosis == null && finalDiagnosis == null) {
                reason = "缺少临床诊断和最终诊断";
            } else if (clinicalDiagnosis == null) {
                reason = "缺少临床诊断";
            } else {
                reason = "缺少最终诊断";
            }
            return new QualityDetailRecord(
                normalizeEmpty(rs.getString("pathology_no")),
                normalizeEmpty(rs.getString("application_no")),
                null,
                toLocalDateTime(rs.getObject("occurred_at")),
                passed ? DETAIL_STATUS_PASS : DETAIL_STATUS_FAIL,
                reason,
                null);
        });
        return datasetFromRateItems("QC_CLINICAL_MATCH_RATE", sourceNoteFor("QC_CLINICAL_MATCH_RATE"), items);
    }

    private QualityDetailDataset buildFirstLineMatchDataset(StatFilter filter) {
        String sql = """
            select pc.pathology_no,
                   a.application_no,
                   s.specimen_no,
                   coalesce(dt.review_completed_at, dt.reviewed_at, dt.primary_diagnosed_at, dt.created_at) as occurred_at,
                   dt.primary_diagnosed_at,
                   dt.review_completed_at,
                   dt.reviewed_at
            from diagnostic_tasks dt
            join pathology_cases pc on pc.id = dt.case_id
            join applications a on a.id = pc.application_id
            left join specimens s on s.id = dt.specimen_id
            where (:fromTime is null or dt.created_at >= :fromTime)
              and (:toTime is null or dt.created_at <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """;
        List<QualityDetailRecord> items = jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> {
            LocalDateTime primaryDiagnosedAt = toLocalDateTime(rs.getObject("primary_diagnosed_at"));
            LocalDateTime reviewCompletedAt = toLocalDateTime(rs.getObject("review_completed_at"));
            LocalDateTime reviewedAt = toLocalDateTime(rs.getObject("reviewed_at"));
            boolean passed = primaryDiagnosedAt != null && (reviewCompletedAt != null || reviewedAt != null);
            String reason;
            if (passed) {
                reason = "首诊与审核完成记录齐备";
            } else if (primaryDiagnosedAt == null && reviewCompletedAt == null && reviewedAt == null) {
                reason = "缺少首诊记录和审核完成记录";
            } else if (primaryDiagnosedAt == null) {
                reason = "缺少首诊记录";
            } else {
                reason = "缺少审核完成记录";
            }
            return new QualityDetailRecord(
                normalizeEmpty(rs.getString("pathology_no")),
                normalizeEmpty(rs.getString("application_no")),
                normalizeEmpty(rs.getString("specimen_no")),
                toLocalDateTime(rs.getObject("occurred_at")),
                passed ? DETAIL_STATUS_PASS : DETAIL_STATUS_FAIL,
                reason,
                null);
        });
        return datasetFromRateItems("QC_FIRST_LINE_MATCH_RATE", sourceNoteFor("QC_FIRST_LINE_MATCH_RATE"), items);
    }

    private QualityDetailDataset buildFrozenParaffinMatchDataset(StatFilter filter) {
        String sql = """
            select pc.pathology_no,
                   a.application_no,
                   s.specimen_no,
                   coalesce(dt.review_completed_at, dt.reviewed_at, dt.primary_diagnosed_at, dt.created_at) as occurred_at,
                   dt.task_type,
                   dt.frozen_diagnosis_result,
                   (
                       select pr.final_diagnosis
                       from pathology_reports pr
                       where pr.case_id = pc.id
                       order by coalesce(pr.published_at, pr.reviewed_at, pr.submitted_at, pr.created_at) desc
                       fetch first 1 row only
                   ) as final_diagnosis
            from diagnostic_tasks dt
            join pathology_cases pc on pc.id = dt.case_id
            join applications a on a.id = pc.application_id
            left join specimens s on s.id = dt.specimen_id
            where (:fromTime is null or dt.created_at >= :fromTime)
              and (:toTime is null or dt.created_at <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
              and (
                    trim(coalesce(dt.frozen_diagnosis_result, '')) <> ''
                 or upper(coalesce(dt.task_type, '')) = 'FROZEN'
              )
            """;
        List<QualityDetailRecord> items = jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> {
            String frozenDiagnosisResult = normalize(rs.getString("frozen_diagnosis_result"));
            String finalDiagnosis = normalize(rs.getString("final_diagnosis"));
            boolean passed = frozenDiagnosisResult != null && finalDiagnosis != null;
            String reason;
            if (passed) {
                reason = "冰冻诊断结果与最终病理诊断信息齐备";
            } else if (frozenDiagnosisResult == null && finalDiagnosis == null) {
                reason = "缺少冰冻结果和最终诊断";
            } else if (frozenDiagnosisResult == null) {
                reason = "缺少冰冻结果";
            } else {
                reason = "缺少最终诊断";
            }
            return new QualityDetailRecord(
                normalizeEmpty(rs.getString("pathology_no")),
                normalizeEmpty(rs.getString("application_no")),
                normalizeEmpty(rs.getString("specimen_no")),
                toLocalDateTime(rs.getObject("occurred_at")),
                passed ? DETAIL_STATUS_PASS : DETAIL_STATUS_FAIL,
                reason,
                null);
        });
        return datasetFromRateItems("QC_FROZEN_PARAFFIN_MATCH_RATE", sourceNoteFor("QC_FROZEN_PARAFFIN_MATCH_RATE"), items);
    }

    private QualityDetailDataset buildCytologyMatchDataset(StatFilter filter) {
        String sql = """
            select coalesce(pc.pathology_no, '') as pathology_no,
                   a.application_no,
                   s.specimen_no,
                   coalesce(
                       (
                           select coalesce(pr.published_at, pr.reviewed_at, pr.submitted_at, pr.created_at)
                           from pathology_reports pr
                           where pr.case_id = pc.id
                           order by coalesce(pr.published_at, pr.reviewed_at, pr.submitted_at, pr.created_at) desc
                           fetch first 1 row only
                       ),
                       s.registered_at,
                       s.created_at
                   ) as occurred_at,
                   (
                       select pr.report_status
                       from pathology_reports pr
                       where pr.case_id = pc.id
                       order by coalesce(pr.published_at, pr.reviewed_at, pr.submitted_at, pr.created_at) desc
                       fetch first 1 row only
                   ) as report_status,
                   (
                       select pr.final_diagnosis
                       from pathology_reports pr
                       where pr.case_id = pc.id
                       order by coalesce(pr.published_at, pr.reviewed_at, pr.submitted_at, pr.created_at) desc
                       fetch first 1 row only
                   ) as final_diagnosis
            from specimens s
            join applications a on a.id = s.application_id
            left join pathology_cases pc on pc.id = s.case_id
            where (:fromTime is null or coalesce(s.registered_at, s.created_at) >= :fromTime)
              and (:toTime is null or coalesce(s.registered_at, s.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
              and (
                    upper(coalesce(s.specimen_type, '')) = 'CYTOLOGY'
                 or upper(coalesce(s.collection_mode, '')) = 'CYTOLOGY'
              )
            """;
        List<QualityDetailRecord> items = jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> {
            String reportStatus = normalize(rs.getString("report_status"));
            String finalDiagnosis = normalize(rs.getString("final_diagnosis"));
            boolean passed = reportStatus != null && finalDiagnosis != null;
            String reason;
            if (passed) {
                reason = "细胞学病例报告与最终诊断齐备";
            } else if (reportStatus == null) {
                reason = "无病理报告";
            } else {
                reason = "缺少最终诊断";
            }
            return new QualityDetailRecord(
                normalizeEmpty(rs.getString("pathology_no")),
                normalizeEmpty(rs.getString("application_no")),
                normalizeEmpty(rs.getString("specimen_no")),
                toLocalDateTime(rs.getObject("occurred_at")),
                passed ? DETAIL_STATUS_PASS : DETAIL_STATUS_FAIL,
                reason,
                null);
        });
        return datasetFromRateItems("QC_CYTOLOGY_MATCH_RATE", sourceNoteFor("QC_CYTOLOGY_MATCH_RATE"), items);
    }

    private QualityDetailDataset buildConsultationMatchDataset(StatFilter filter) {
        String sql = """
            select pc.pathology_no,
                   a.application_no,
                   coalesce(cc.completed_at, cc.requested_at, cc.created_at) as occurred_at,
                   cc.status,
                   cc.opinion
            from consultation_cases cc
            join pathology_cases pc on pc.id = cc.case_id
            join applications a on a.id = pc.application_id
            where (:fromTime is null or coalesce(cc.requested_at, cc.created_at) >= :fromTime)
              and (:toTime is null or coalesce(cc.requested_at, cc.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """;
        List<QualityDetailRecord> items = jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> {
            String status = normalize(rs.getString("status"));
            String opinion = normalize(rs.getString("opinion"));
            boolean passed = "COMPLETED".equals(status) && opinion != null;
            String reason;
            if (passed) {
                reason = "会诊闭环已完成且存在会诊意见";
            } else if (!"COMPLETED".equals(status) && opinion == null) {
                reason = "会诊未完成且无会诊意见";
            } else if (!"COMPLETED".equals(status)) {
                reason = "会诊未完成";
            } else {
                reason = "缺少会诊意见";
            }
            return new QualityDetailRecord(
                normalizeEmpty(rs.getString("pathology_no")),
                normalizeEmpty(rs.getString("application_no")),
                null,
                toLocalDateTime(rs.getObject("occurred_at")),
                passed ? DETAIL_STATUS_PASS : DETAIL_STATUS_FAIL,
                reason,
                null);
        });
        return datasetFromRateItems("QC_CONSULTATION_MATCH_RATE", sourceNoteFor("QC_CONSULTATION_MATCH_RATE"), items);
    }

    private QualityDetailDataset buildCancelledReviewDataset(StatFilter filter) {
        String sql = """
            select pc.pathology_no,
                   a.application_no,
                   rrr.requested_at as occurred_at,
                   rrr.request_status,
                   rrr.request_reason
            from report_revision_requests rrr
            join pathology_cases pc on pc.id = rrr.case_id
            join applications a on a.id = pc.application_id
            where (:fromTime is null or coalesce(rrr.requested_at, rrr.created_at) >= :fromTime)
              and (:toTime is null or coalesce(rrr.requested_at, rrr.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """;
        List<QualityDetailRecord> items = jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> new QualityDetailRecord(
            normalizeEmpty(rs.getString("pathology_no")),
            normalizeEmpty(rs.getString("application_no")),
            null,
            toLocalDateTime(rs.getObject("occurred_at")),
            DETAIL_STATUS_INFO,
            joinInfo("修订申请状态：" + normalizeEmpty(rs.getString("request_status")), normalize(rs.getString("request_reason"))),
            null));
        return new QualityDetailDataset(
            "QC_CANCELLED_REVIEW_COUNT",
            STATUS_AVAILABLE,
            items.size(),
            0,
            0,
            sourceNoteFor("QC_CANCELLED_REVIEW_COUNT"),
            items);
    }

    private QualityDetailDataset buildTechnicalQualityDataset(StatFilter filter) {
        String sql = """
            select pc.pathology_no,
                   a.application_no,
                   coalesce(mo.completed_at, mo.created_at) as occurred_at,
                   mo.order_type,
                   mo.order_content
            from medical_orders mo
            join pathology_cases pc on pc.id = mo.case_id
            join applications a on a.id = pc.application_id
            where mo.status = 'COMPLETED'
              and (:fromTime is null or coalesce(mo.completed_at, mo.created_at) >= :fromTime)
              and (:toTime is null or coalesce(mo.completed_at, mo.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """;
        List<QualityDetailRecord> items = jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> new QualityDetailRecord(
            normalizeEmpty(rs.getString("pathology_no")),
            normalizeEmpty(rs.getString("application_no")),
            null,
            toLocalDateTime(rs.getObject("occurred_at")),
            DETAIL_STATUS_INFO,
            joinInfo("技术相关医嘱已完成：" + normalizeEmpty(rs.getString("order_type")), normalize(rs.getString("order_content"))),
            null));
        return new QualityDetailDataset(
            "QC_TECHNICAL_QUALITY_COUNT",
            STATUS_AVAILABLE,
            items.size(),
            0,
            0,
            sourceNoteFor("QC_TECHNICAL_QUALITY_COUNT"),
            items);
    }

    private QualityDetailDataset buildGrossingQualityDataset(StatFilter filter) {
        String sql = """
            select pc.pathology_no,
                   a.application_no,
                   s.specimen_no,
                   coalesce(sm.sampled_at, sm.created_at) as occurred_at,
                   sm.sampling_status,
                   sm.gross_description
            from samplings sm
            join pathology_cases pc on pc.id = sm.case_id
            join applications a on a.id = pc.application_id
            left join specimens s on s.id = sm.specimen_id
            where (:fromTime is null or coalesce(sm.sampled_at, sm.created_at) >= :fromTime)
              and (:toTime is null or coalesce(sm.sampled_at, sm.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
              and (
                    sm.sampling_status = 'COMPLETED'
                 or sm.sampled_at is not null
              )
            """;
        List<QualityDetailRecord> items = jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> new QualityDetailRecord(
            normalizeEmpty(rs.getString("pathology_no")),
            normalizeEmpty(rs.getString("application_no")),
            normalizeEmpty(rs.getString("specimen_no")),
            toLocalDateTime(rs.getObject("occurred_at")),
            DETAIL_STATUS_INFO,
            joinInfo("取材任务已完成", normalize(rs.getString("gross_description"))),
            null));
        return new QualityDetailDataset(
            "QC_GROSSING_QUALITY_COUNT",
            STATUS_AVAILABLE,
            items.size(),
            0,
            0,
            sourceNoteFor("QC_GROSSING_QUALITY_COUNT"),
            items);
    }

    private QualityDetailDataset buildReportReleaseDaysDataset(StatFilter filter) {
        String sql = """
            select pc.pathology_no,
                   a.application_no,
                   pr.published_at as occurred_at,
                   pr.submitted_at,
                   pr.created_at as report_created_at
            from pathology_reports pr
            join pathology_cases pc on pc.id = pr.case_id
            join applications a on a.id = pc.application_id
            where pr.report_status = 'PUBLISHED'
              and (:fromTime is null or pr.published_at >= :fromTime)
              and (:toTime is null or pr.published_at <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """;
        List<QualityDetailRecord> items = jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> {
            LocalDateTime occurredAt = toLocalDateTime(rs.getObject("occurred_at"));
            LocalDateTime submittedAt = toLocalDateTime(rs.getObject("submitted_at"));
            LocalDateTime createdAt = toLocalDateTime(rs.getObject("report_created_at"));
            LocalDateTime baseline = submittedAt != null ? submittedAt : createdAt;
            BigDecimal days = baseline == null || occurredAt == null ? null : daysBetween(baseline, occurredAt);
            String reason = days == null ? "缺少提交或发布时间，无法计算" : "报告发布耗时 " + formatDecimal(days) + " 天";
            return new QualityDetailRecord(
                normalizeEmpty(rs.getString("pathology_no")),
                normalizeEmpty(rs.getString("application_no")),
                null,
                occurredAt,
                DETAIL_STATUS_INFO,
                reason,
                days);
        });
        return new QualityDetailDataset(
            "QC_REPORT_RELEASE_DAYS",
            STATUS_AVAILABLE,
            items.size(),
            items.size(),
            0,
            sourceNoteFor("QC_REPORT_RELEASE_DAYS"),
            items);
    }

    private QualityDetailDataset buildSpecimenProcessHoursDataset(StatFilter filter) {
        String sql = """
            select pc.pathology_no,
                   a.application_no,
                   (
                       select s.specimen_no
                       from specimens s
                       where s.case_id = pc.id
                       order by coalesce(s.registered_at, s.created_at) desc
                       fetch first 1 row only
                   ) as specimen_no,
                   coalesce(
                       (
                           select pr.published_at
                           from pathology_reports pr
                           where pr.case_id = pc.id
                             and pr.report_status = 'PUBLISHED'
                           order by pr.published_at desc
                           fetch first 1 row only
                       ),
                       pc.received_at,
                       pc.created_at
                   ) as occurred_at,
                   pc.received_at,
                   (
                       select pr.published_at
                       from pathology_reports pr
                       where pr.case_id = pc.id
                         and pr.report_status = 'PUBLISHED'
                       order by pr.published_at desc
                       fetch first 1 row only
                   ) as published_at
            from pathology_cases pc
            join applications a on a.id = pc.application_id
            where (:fromTime is null or coalesce(pc.received_at, pc.created_at) >= :fromTime)
              and (:toTime is null or coalesce(pc.received_at, pc.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """;
        List<QualityDetailRecord> items = jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> {
            LocalDateTime receivedAt = toLocalDateTime(rs.getObject("received_at"));
            LocalDateTime publishedAt = toLocalDateTime(rs.getObject("published_at"));
            boolean passed = receivedAt != null && publishedAt != null;
            BigDecimal hours = passed ? hoursBetween(receivedAt, publishedAt) : null;
            String reason;
            if (passed) {
                reason = "标本到报告处理耗时 " + formatDecimal(hours) + " 小时";
            } else if (receivedAt == null && publishedAt == null) {
                reason = "缺少接收时间和发布时间";
            } else if (receivedAt == null) {
                reason = "缺少接收时间";
            } else {
                reason = "缺少发布时间";
            }
            return new QualityDetailRecord(
                normalizeEmpty(rs.getString("pathology_no")),
                normalizeEmpty(rs.getString("application_no")),
                normalizeEmpty(rs.getString("specimen_no")),
                toLocalDateTime(rs.getObject("occurred_at")),
                passed ? DETAIL_STATUS_PASS : DETAIL_STATUS_FAIL,
                reason,
                hours);
        });
        long passCount = items.stream().filter(item -> DETAIL_STATUS_PASS.equals(item.detailStatus())).count();
        long failCount = items.size() - passCount;
        return new QualityDetailDataset(
            "QC_SPECIMEN_PROCESS_HOURS",
            STATUS_AVAILABLE,
            items.size(),
            passCount,
            failCount,
            sourceNoteFor("QC_SPECIMEN_PROCESS_HOURS"),
            items);
    }

    private QualityDetailDataset buildDiagnosisTimelinessDataset(StatFilter filter) {
        String sql = """
            select pc.pathology_no,
                   a.application_no,
                   coalesce(
                       (
                           select pr.published_at
                           from pathology_reports pr
                           where pr.case_id = pc.id
                             and pr.report_status = 'PUBLISHED'
                           order by pr.published_at desc
                           fetch first 1 row only
                       ),
                       pc.received_at,
                       pc.created_at
                   ) as occurred_at,
                   (
                       select pr.published_at
                       from pathology_reports pr
                       where pr.case_id = pc.id
                         and pr.report_status = 'PUBLISHED'
                       order by pr.published_at desc
                       fetch first 1 row only
                   ) as published_at,
                   (
                       select max(dt.sla_due_at)
                       from diagnostic_tasks dt
                       where dt.case_id = pc.id
                   ) as sla_due_at
            from pathology_cases pc
            join applications a on a.id = pc.application_id
            where (:fromTime is null or coalesce(pc.received_at, pc.created_at) >= :fromTime)
              and (:toTime is null or coalesce(pc.received_at, pc.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """;
        List<QualityDetailRecord> items = jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> {
            LocalDateTime publishedAt = toLocalDateTime(rs.getObject("published_at"));
            LocalDateTime slaDueAt = toLocalDateTime(rs.getObject("sla_due_at"));
            boolean passed = publishedAt != null && (slaDueAt == null || !publishedAt.isAfter(slaDueAt));
            String reason;
            if (passed && slaDueAt == null) {
                reason = "存在已发布报告，且当前无 SLA 限制";
            } else if (passed) {
                reason = "已在 SLA 时限内完成发布";
            } else if (publishedAt == null) {
                reason = "无已发布报告";
            } else {
                reason = "报告发布时间晚于 SLA 截止时间";
            }
            return new QualityDetailRecord(
                normalizeEmpty(rs.getString("pathology_no")),
                normalizeEmpty(rs.getString("application_no")),
                null,
                toLocalDateTime(rs.getObject("occurred_at")),
                passed ? DETAIL_STATUS_PASS : DETAIL_STATUS_FAIL,
                reason,
                null);
        });
        return datasetFromRateItems("QC_DIAGNOSIS_TIMELINESS_RATE", sourceNoteFor("QC_DIAGNOSIS_TIMELINESS_RATE"), items);
    }

    private QualityDetailDataset buildFrozenDiagnosisTimelinessDataset(StatFilter filter) {
        return datasetFromRateItems(
            "QC_FROZEN_DIAGNOSIS_TIMELINESS_RATE",
            sourceNoteFor("QC_FROZEN_DIAGNOSIS_TIMELINESS_RATE"),
            buildFrozenTimeoutRecords(filter, FrozenTimeoutMode.DIAGNOSIS));
    }

    private QualityDetailDataset buildFrozenTimeoutDataset(String indicatorCode,
                                                           StatFilter filter,
                                                           FrozenTimeoutMode mode) {
        List<QualityDetailRecord> allItems = buildFrozenTimeoutRecords(filter, mode);
        List<QualityDetailRecord> items = allItems.stream()
            .filter(item -> DETAIL_STATUS_FAIL.equals(item.detailStatus()))
            .toList();
        long eligibleCount = mode == FrozenTimeoutMode.ALL
            ? allItems.size()
            : countFrozenDiagnosticTasks(filter);
        return new QualityDetailDataset(
            indicatorCode,
            STATUS_AVAILABLE,
            eligibleCount,
            Math.max(eligibleCount - items.size(), 0),
            items.size(),
            sourceNoteFor(indicatorCode),
            items);
    }

    private List<QualityDetailRecord> buildFrozenTimeoutRecords(StatFilter filter, FrozenTimeoutMode mode) {
        List<QualityDetailRecord> items = new ArrayList<>();
        if (mode == FrozenTimeoutMode.ALL || mode == FrozenTimeoutMode.GROSSING) {
            items.addAll(queryFrozenGrossingTimeoutRecords(filter));
        }
        if (mode == FrozenTimeoutMode.ALL || mode == FrozenTimeoutMode.SLICING) {
            items.addAll(queryFrozenSlicingTimeoutRecords(filter));
        }
        if (mode == FrozenTimeoutMode.ALL || mode == FrozenTimeoutMode.DIAGNOSIS) {
            items.addAll(queryFrozenDiagnosisTimeoutRecords(filter));
        }
        return items;
    }

    private List<QualityDetailRecord> queryFrozenGrossingTimeoutRecords(StatFilter filter) {
        String sql = """
            select pc.pathology_no,
                   a.application_no,
                   s.specimen_no,
                   coalesce(sm.sampled_at, sm.created_at) as occurred_at,
                   pc.received_at,
                   sm.sampled_at
            from samplings sm
            join pathology_cases pc on pc.id = sm.case_id
            join applications a on a.id = pc.application_id
            left join specimens s on s.id = sm.specimen_id
            where (:fromTime is null or coalesce(sm.sampled_at, sm.created_at) >= :fromTime)
              and (:toTime is null or coalesce(sm.sampled_at, sm.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
              and exists (
                    select 1
                    from diagnostic_tasks dt
                    where dt.case_id = pc.id
                      and (
                            upper(coalesce(dt.task_type, '')) = 'FROZEN'
                         or trim(coalesce(dt.frozen_diagnosis_result, '')) <> ''
                      )
              )
            """;
        return jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> {
            LocalDateTime receivedAt = toLocalDateTime(rs.getObject("received_at"));
            LocalDateTime sampledAt = toLocalDateTime(rs.getObject("sampled_at"));
            BigDecimal minutes = minutesBetween(receivedAt, sampledAt);
            boolean passed = minutes != null
                && minutes.compareTo(BigDecimal.valueOf(DEFAULT_FROZEN_GROSSING_SLA_MINUTES)) <= 0;
            return new QualityDetailRecord(
                normalizeEmpty(rs.getString("pathology_no")),
                normalizeEmpty(rs.getString("application_no")),
                normalizeEmpty(rs.getString("specimen_no")),
                toLocalDateTime(rs.getObject("occurred_at")),
                passed ? DETAIL_STATUS_PASS : DETAIL_STATUS_FAIL,
                frozenTimeoutReason("取材", minutes, DEFAULT_FROZEN_GROSSING_SLA_MINUTES),
                minutes);
        });
    }

    private List<QualityDetailRecord> queryFrozenSlicingTimeoutRecords(StatFilter filter) {
        String sql = """
            select pc.pathology_no,
                   a.application_no,
                   s.specimen_no,
                   coalesce(sl.sliced_at, sl.created_at) as occurred_at,
                   sm.sampled_at,
                   sl.sliced_at
            from slicings sl
            join pathology_cases pc on pc.id = sl.case_id
            join applications a on a.id = pc.application_id
            left join specimens s on s.id = sl.specimen_id
            left join embeddings e on e.id = sl.embedding_id
            left join samplings sm on sm.id = e.sampling_id
            where (:fromTime is null or coalesce(sl.sliced_at, sl.created_at) >= :fromTime)
              and (:toTime is null or coalesce(sl.sliced_at, sl.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
              and exists (
                    select 1
                    from diagnostic_tasks dt
                    where dt.case_id = pc.id
                      and (
                            upper(coalesce(dt.task_type, '')) = 'FROZEN'
                         or trim(coalesce(dt.frozen_diagnosis_result, '')) <> ''
                      )
              )
            """;
        return jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> {
            LocalDateTime sampledAt = toLocalDateTime(rs.getObject("sampled_at"));
            LocalDateTime slicedAt = toLocalDateTime(rs.getObject("sliced_at"));
            BigDecimal minutes = minutesBetween(sampledAt, slicedAt);
            boolean passed = minutes != null
                && minutes.compareTo(BigDecimal.valueOf(DEFAULT_FROZEN_SLICING_SLA_MINUTES)) <= 0;
            return new QualityDetailRecord(
                normalizeEmpty(rs.getString("pathology_no")),
                normalizeEmpty(rs.getString("application_no")),
                normalizeEmpty(rs.getString("specimen_no")),
                toLocalDateTime(rs.getObject("occurred_at")),
                passed ? DETAIL_STATUS_PASS : DETAIL_STATUS_FAIL,
                frozenTimeoutReason("切片", minutes, DEFAULT_FROZEN_SLICING_SLA_MINUTES),
                minutes);
        });
    }

    private List<QualityDetailRecord> queryFrozenDiagnosisTimeoutRecords(StatFilter filter) {
        String sql = """
            select pc.pathology_no,
                   a.application_no,
                   s.specimen_no,
                   coalesce(dt.completed_at, dt.review_completed_at, dt.reviewed_at, dt.primary_diagnosed_at, dt.created_at) as occurred_at,
                   sl.sliced_at,
                   coalesce(dt.completed_at, dt.review_completed_at, dt.reviewed_at, dt.primary_diagnosed_at) as diagnosis_completed_at
            from diagnostic_tasks dt
            join pathology_cases pc on pc.id = dt.case_id
            join applications a on a.id = pc.application_id
            left join specimens s on s.id = dt.specimen_id
            left join (
                select case_id, max(sliced_at) as sliced_at
                from slicings
                group by case_id
            ) sl on sl.case_id = pc.id
            where (:fromTime is null or coalesce(dt.completed_at, dt.review_completed_at, dt.reviewed_at, dt.primary_diagnosed_at, dt.created_at) >= :fromTime)
              and (:toTime is null or coalesce(dt.completed_at, dt.review_completed_at, dt.reviewed_at, dt.primary_diagnosed_at, dt.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
              and (
                    upper(coalesce(dt.task_type, '')) = 'FROZEN'
                 or trim(coalesce(dt.frozen_diagnosis_result, '')) <> ''
              )
            """;
        return jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> {
            LocalDateTime slicedAt = toLocalDateTime(rs.getObject("sliced_at"));
            LocalDateTime diagnosisCompletedAt = toLocalDateTime(rs.getObject("diagnosis_completed_at"));
            BigDecimal minutes = minutesBetween(slicedAt, diagnosisCompletedAt);
            boolean passed = minutes != null
                && minutes.compareTo(BigDecimal.valueOf(DEFAULT_FROZEN_DIAGNOSIS_SLA_MINUTES)) <= 0;
            return new QualityDetailRecord(
                normalizeEmpty(rs.getString("pathology_no")),
                normalizeEmpty(rs.getString("application_no")),
                normalizeEmpty(rs.getString("specimen_no")),
                toLocalDateTime(rs.getObject("occurred_at")),
                passed ? DETAIL_STATUS_PASS : DETAIL_STATUS_FAIL,
                frozenTimeoutReason("诊断", minutes, DEFAULT_FROZEN_DIAGNOSIS_SLA_MINUTES),
                minutes);
        });
    }

    private QualityDetailDataset buildReportChangeDataset(String indicatorCode,
                                                          StatFilter filter,
                                                          ReportChangeMode mode) {
        List<QualityDetailRecord> items = new ArrayList<>();
        if (mode == ReportChangeMode.ALL || mode == ReportChangeMode.DOCTOR || mode == ReportChangeMode.MODIFICATION_REASON) {
            items.addAll(queryReportVersionChangeRecords(filter, mode));
        }
        if (mode == ReportChangeMode.ALL || mode == ReportChangeMode.DOCTOR || mode == ReportChangeMode.REVISION_REASON) {
            items.addAll(queryReportRevisionChangeRecords(filter, mode));
        }
        return new QualityDetailDataset(
            indicatorCode,
            STATUS_AVAILABLE,
            items.size(),
            items.size(),
            0,
            sourceNoteFor(indicatorCode),
            items);
    }

    private List<QualityDetailRecord> queryReportVersionChangeRecords(StatFilter filter, ReportChangeMode mode) {
        String sql = """
            select pc.pathology_no,
                   a.application_no,
                   rv.report_id,
                   rv.version_no,
                   rv.version_status,
                   rv.signed_by_user_id,
                   rv.signed_by_name,
                   rv.created_at
            from report_versions rv
            join pathology_cases pc on pc.id = rv.case_id
            join applications a on a.id = pc.application_id
            where rv.version_no > 1
              and (:fromTime is null or rv.created_at >= :fromTime)
              and (:toTime is null or rv.created_at <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """;
        return jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> {
            String doctor = normalizeEmpty(rs.getString("signed_by_name"));
            String versionStatus = normalizeEmpty(rs.getString("version_status"));
            String reason = switch (mode) {
                case DOCTOR -> doctor.isBlank() ? "未知医生" : doctor;
                case MODIFICATION_REASON -> "版本状态：" + versionStatus;
                default -> joinInfo("报告版本变更 v" + rs.getInt("version_no"), "状态：" + versionStatus);
            };
            return new QualityDetailRecord(
                normalizeEmpty(rs.getString("pathology_no")),
                normalizeEmpty(rs.getString("application_no")),
                normalizeEmpty(rs.getString("report_id")),
                toLocalDateTime(rs.getObject("created_at")),
                DETAIL_STATUS_INFO,
                reason,
                null);
        });
    }

    private List<QualityDetailRecord> queryReportRevisionChangeRecords(StatFilter filter, ReportChangeMode mode) {
        String sql = """
            select pc.pathology_no,
                   a.application_no,
                   rrr.report_id,
                   rrr.current_version_no,
                   rrr.request_status,
                   rrr.request_reason,
                   rrr.requested_by_user_id,
                   rrr.requested_by_name,
                   coalesce(rrr.requested_at, rrr.created_at) as occurred_at
            from report_revision_requests rrr
            join pathology_cases pc on pc.id = rrr.case_id
            join applications a on a.id = pc.application_id
            where (:fromTime is null or coalesce(rrr.requested_at, rrr.created_at) >= :fromTime)
              and (:toTime is null or coalesce(rrr.requested_at, rrr.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """;
        return jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> {
            String doctor = normalizeEmpty(rs.getString("requested_by_name"));
            String requestStatus = normalizeEmpty(rs.getString("request_status"));
            String requestReason = normalize(rs.getString("request_reason"));
            String reason = switch (mode) {
                case DOCTOR -> doctor.isBlank() ? "未知医生" : doctor;
                case REVISION_REASON -> requestReason == null ? "未填写修订原因" : requestReason;
                default -> joinInfo("报告修订申请 v" + rs.getInt("current_version_no"), joinInfo(requestStatus, requestReason));
            };
            return new QualityDetailRecord(
                normalizeEmpty(rs.getString("pathology_no")),
                normalizeEmpty(rs.getString("application_no")),
                normalizeEmpty(rs.getString("report_id")),
                toLocalDateTime(rs.getObject("occurred_at")),
                DETAIL_STATUS_INFO,
                reason,
                null);
        });
    }

    private QualityDetailDataset buildUnqualifiedSpecimenRateDataset(StatFilter filter) {
        QualityDetailDataset countDataset = buildUnqualifiedSpecimenDataset(filter);
        return new QualityDetailDataset(
            "QC_UNQUALIFIED_SPECIMEN_RATE",
            STATUS_AVAILABLE,
            countDataset.eligibleCount(),
            countDataset.passCount(),
            countDataset.failCount(),
            sourceNoteFor("QC_UNQUALIFIED_SPECIMEN_RATE"),
            countDataset.items());
    }

    private QualityDetailDataset buildUnqualifiedSpecimenReasonDataset(StatFilter filter) {
        QualityDetailDataset countDataset = buildUnqualifiedSpecimenDataset(filter);
        return new QualityDetailDataset(
            "QC_UNQUALIFIED_SPECIMEN_REASON_COUNT",
            STATUS_AVAILABLE,
            countDataset.eligibleCount(),
            countDataset.passCount(),
            countDataset.failCount(),
            sourceNoteFor("QC_UNQUALIFIED_SPECIMEN_REASON_COUNT"),
            countDataset.items());
    }

    private QualityDetailDataset buildCriticalValueNotificationDataset(String indicatorCode,
                                                                       StatFilter filter,
                                                                       CriticalValueMode mode) {
        String sql = """
            select id,
                   coalesce(action_payload_json, '') as action_payload_json,
                   title,
                   summary,
                   status,
                   level,
                   created_at,
                   read_at
            from user_notifications
            where topic_code = 'CRITICAL_VALUE'
              and (:fromTime is null or created_at >= :fromTime)
              and (:toTime is null or created_at <= :toTime)
            """;
        List<QualityDetailRecord> items = jdbcTemplate.query(sql, detailParams(filter), (rs, rowNum) -> {
            LocalDateTime createdAt = toLocalDateTime(rs.getObject("created_at"));
            LocalDateTime readAt = toLocalDateTime(rs.getObject("read_at"));
            String payload = rs.getString("action_payload_json");
            String caseId = extractJsonValue(payload, "caseId");
            String notificationId = normalizeEmpty(rs.getString("id"));
            String status = normalizeEmpty(rs.getString("status"));
            String level = normalizeEmpty(rs.getString("level"));
            String summary = normalize(rs.getString("summary"));
            String title = normalize(rs.getString("title"));
            boolean timely = readAt != null
                && createdAt != null
                && !readAt.isAfter(createdAt.plusMinutes(30));
            String reason = switch (mode) {
                case COUNT -> joinInfo("危急值通知", title);
                case REASON_ANALYSIS -> joinInfo(level.isBlank() ? "未分级" : level, summary == null ? title : summary);
                case TIMELINESS -> timely
                    ? "30 分钟内已读确认"
                    : readAt == null ? "尚未读取确认" : "读取确认超过 30 分钟";
            };
            String detailStatus = mode == CriticalValueMode.TIMELINESS
                ? timely ? DETAIL_STATUS_PASS : DETAIL_STATUS_FAIL
                : DETAIL_STATUS_INFO;
            return new QualityDetailRecord(
                caseId == null ? notificationId : caseId,
                notificationId,
                null,
                createdAt,
                detailStatus,
                reason,
                null);
        });
        if (mode == CriticalValueMode.TIMELINESS) {
            return datasetFromRateItems(indicatorCode, sourceNoteFor(indicatorCode), items);
        }
        return new QualityDetailDataset(
            indicatorCode,
            STATUS_PARTIAL,
            items.size(),
            0,
            items.size(),
            sourceNoteFor(indicatorCode),
            items);
    }

    private QualityDetailDataset datasetFromRateItems(String indicatorCode,
                                                      String sourceNote,
                                                      List<QualityDetailRecord> items) {
        long passCount = items.stream().filter(item -> DETAIL_STATUS_PASS.equals(item.detailStatus())).count();
        long failCount = items.stream().filter(item -> DETAIL_STATUS_FAIL.equals(item.detailStatus())).count();
        return new QualityDetailDataset(
            indicatorCode,
            STATUS_AVAILABLE,
            items.size(),
            passCount,
            failCount,
            sourceNote,
            items);
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

    private List<TrendPointView> buildTrendPoints(String indicatorCode, StatFilter filter) {
        if (!isTrendSupportedMetric(indicatorCode) || filter.from() == null || filter.to() == null) {
            return List.of();
        }
        List<TrendBucket> buckets = buildTrendBuckets(filter);
        return buckets.stream()
            .map(bucket -> new TrendPointView(
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

    private List<TrendPointView> buildQualityTrendPoints(String indicatorCode,
                                                         String aggregationType,
                                                         StatFilter filter) {
        if (filter.from() == null || filter.to() == null) {
            return List.of();
        }
        return buildTrendBuckets(filter).stream()
            .map(bucket -> {
                QualityDetailDataset dataset = buildQualityDetailDataset(indicatorCode, filter.withRange(bucket.from(), bucket.to()));
                MetricSnapshot metric = summarizeQualityMetric(indicatorCode, aggregationType, dataset);
                return new TrendPointView(bucket.label(), metric.metricValue());
            })
            .toList();
    }

    private List<BreakdownView> buildQualityBreakdowns(QualityDetailDataset dataset) {
        Map<String, Long> reasonCounts = new LinkedHashMap<>();
        for (QualityDetailRecord item : dataset.items()) {
            if (DETAIL_STATUS_PASS.equals(item.detailStatus())) {
                continue;
            }
            String label = normalize(item.reason());
            if (label == null) {
                label = DETAIL_STATUS_INFO.equals(item.detailStatus()) ? "记录" : "未通过";
            }
            reasonCounts.merge(label, 1L, Long::sum);
        }
        if (reasonCounts.isEmpty()) {
            if (dataset.passCount() > 0) {
                return List.of(new BreakdownView("通过", String.valueOf(dataset.passCount())));
            }
            if (dataset.eligibleCount() == 0 && dataset.items().isEmpty()) {
                return List.of();
            }
        }
        return reasonCounts.entrySet().stream()
            .map(item -> new BreakdownView(item.getKey(), String.valueOf(item.getValue())))
            .toList();
    }

    private List<TrendBucket> buildTrendBuckets(StatFilter filter) {
        String mode = normalizePeriodMode(filter.periodMode());
        LocalDateTime from = filter.from();
        LocalDateTime to = filter.to();
        List<TrendBucket> buckets = new ArrayList<>();
        if ("year".equals(mode)) {
            int startYear = from.getYear();
            int endYear = to.getYear();
            for (int year = startYear; year <= endYear; year++) {
                LocalDateTime bucketStart = LocalDateTime.of(year, 1, 1, 0, 0, 0);
                LocalDateTime bucketEnd = LocalDateTime.of(year, 12, 31, 23, 59, 59);
                buckets.add(trimBucket(String.valueOf(year), bucketStart, bucketEnd, from, to));
            }
            return buckets;
        }
        if ("quarter".equals(mode)) {
            int year = from.getYear();
            int quarter = quarterOf(from);
            while (year < to.getYear() || (year == to.getYear() && quarter <= quarterOf(to))) {
                int startMonth = (quarter - 1) * 3 + 1;
                LocalDateTime bucketStart = LocalDateTime.of(year, startMonth, 1, 0, 0, 0);
                YearMonth endMonth = YearMonth.of(year, startMonth + 2);
                LocalDateTime bucketEnd = endMonth.atEndOfMonth().atTime(LocalTime.of(23, 59, 59));
                buckets.add(trimBucket(year + "-Q" + quarter, bucketStart, bucketEnd, from, to));
                quarter++;
                if (quarter > 4) {
                    quarter = 1;
                    year++;
                }
            }
            return buckets;
        }
        YearMonth cursor = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        while (!cursor.isAfter(end)) {
            LocalDateTime bucketStart = cursor.atDay(1).atStartOfDay();
            LocalDateTime bucketEnd = cursor.atEndOfMonth().atTime(LocalTime.of(23, 59, 59));
            buckets.add(trimBucket(cursor.toString(), bucketStart, bucketEnd, from, to));
            cursor = cursor.plusMonths(1);
        }
        return buckets;
    }

    private TrendBucket trimBucket(String label,
                                   LocalDateTime bucketStart,
                                   LocalDateTime bucketEnd,
                                   LocalDateTime from,
                                   LocalDateTime to) {
        LocalDateTime trimmedStart = bucketStart.isBefore(from) ? from : bucketStart;
        LocalDateTime trimmedEnd = bucketEnd.isAfter(to) ? to : bucketEnd;
        return new TrendBucket(label, trimmedStart, trimmedEnd);
    }

    private int quarterOf(LocalDateTime value) {
        return ((value.getMonthValue() - 1) / 3) + 1;
    }

    private String normalizePeriodMode(String value) {
        String normalized = normalize(value);
        if ("year".equalsIgnoreCase(normalized)) {
            return "year";
        }
        if ("quarter".equalsIgnoreCase(normalized)) {
            return "quarter";
        }
        return "month";
    }

    private List<M6StatisticsRows.StatIndicatorDefinitionRow> selectIndicators(QueryStatReportCommand command, String category) {
        if (normalize(command.indicatorCode()) != null) {
            M6StatisticsRows.StatIndicatorDefinitionRow row = repository.findStatIndicatorDefinitionByCode(command.indicatorCode());
            return row == null ? List.of() : List.of(row);
        }
        return repository.findStatIndicatorDefinitions(category);
    }

    private String resolveCategory(QueryStatReportCommand command) {
        if (normalize(command.category()) != null) {
            return command.category();
        }
        if (normalize(command.templateCode()) != null) {
            M6StatisticsRows.StatReportTemplateRow template = repository.findStatReportTemplateByCode(command.templateCode());
            return template == null ? "QUALITY" : template.templateType();
        }
        return "QUALITY";
    }

    private String resolveTemplateId(String templateCode) {
        if (normalize(templateCode) == null) {
            return null;
        }
        M6StatisticsRows.StatReportTemplateRow template = repository.findStatReportTemplateByCode(templateCode);
        return template == null ? null : template.id();
    }

    private long countPathologyCases(StatFilter filter) {
        return queryForLong("""
            select count(*)
            from pathology_cases pc
            join applications a on a.id = pc.application_id
            where (:fromTime is null or coalesce(pc.received_at, pc.created_at) >= :fromTime)
              and (:toTime is null or coalesce(pc.received_at, pc.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """, params(filter));
    }

    private long countEligibleSpecimens(StatFilter filter) {
        return queryForLong("""
            select count(*)
            from specimens s
            join applications a on a.id = s.application_id
            where (:fromTime is null or coalesce(s.registered_at, s.created_at) >= :fromTime)
              and (:toTime is null or coalesce(s.registered_at, s.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """, params(filter));
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
            """, params(filter));
    }

    private long countGlobalReagentWarnings() {
        return queryForLong("""
            select count(*)
            from reagent_stocks
            where low_stock_threshold is not null
              and stock_quantity <= low_stock_threshold
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
              and (:workloadUserId is null or dt.primary_doctor_user_id = :workloadUserId)
              and (:roleId is null or exists (
                    select 1
                    from user_roles ur
                    where ur.user_id = dt.primary_doctor_user_id
                      and ur.role_id = :roleId
                ))
            """, params(filter));
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
              and (:workloadUserId is null or coalesce(mo.executor_user_id, mo.doctor_user_id) = :workloadUserId)
              and (:roleId is null or exists (
                    select 1
                    from user_roles ur
                    where ur.user_id = coalesce(mo.executor_user_id, mo.doctor_user_id)
                      and ur.role_id = :roleId
                ))
            """, params(filter));
    }

    private long queryForLong(String sql, MapSqlParameterSource params) {
        Long value = jdbcTemplate.queryForObject(sql, params, Long.class);
        return value == null ? 0L : value;
    }

    private BigDecimal queryForDecimal(String sql, MapSqlParameterSource params) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    private MapSqlParameterSource params(StatFilter filter) {
        return new MapSqlParameterSource()
            .addValue("fromTime", filter.from())
            .addValue("toTime", filter.to())
            .addValue("departmentId", filter.departmentId())
            .addValue("roleId", filter.roleId())
            .addValue("workloadUserId", filter.workloadUserId());
    }

    private MapSqlParameterSource detailParams(StatFilter filter) {
        return new MapSqlParameterSource()
            .addValue("fromTime", filter.from())
            .addValue("toTime", filter.to())
            .addValue("departmentId", filter.departmentId());
    }

    private MetricValue countMetric(long count) {
        return new MetricValue(String.valueOf(count), "COUNT");
    }

    private MetricValue decimalMetric(BigDecimal value, String unit) {
        return new MetricValue(formatDecimal(value), unit);
    }

    private BigDecimal percentValue(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            total = total.add(value);
        }
        return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal daysBetween(LocalDateTime from, LocalDateTime to) {
        return BigDecimal.valueOf(Duration.between(from, to).toMinutes())
            .divide(BigDecimal.valueOf(60 * 24), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal hoursBetween(LocalDateTime from, LocalDateTime to) {
        return BigDecimal.valueOf(Duration.between(from, to).toMinutes())
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal minutesBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return null;
        }
        return BigDecimal.valueOf(Duration.between(from, to).toMinutes())
            .setScale(2, RoundingMode.HALF_UP);
    }

    private String frozenTimeoutReason(String stage, BigDecimal minutes, int slaMinutes) {
        if (minutes == null) {
            return stage + "缺少起止时间，按超时处理；默认 SLA " + slaMinutes + " 分钟";
        }
        return stage + "耗时 " + formatDecimal(minutes) + " 分钟，默认 SLA " + slaMinutes + " 分钟";
    }

    private long countFrozenDiagnosticTasks(StatFilter filter) {
        return queryForLong("""
            select count(*)
            from diagnostic_tasks dt
            join pathology_cases pc on pc.id = dt.case_id
            join applications a on a.id = pc.application_id
            where (:fromTime is null or coalesce(dt.completed_at, dt.review_completed_at, dt.reviewed_at, dt.primary_diagnosed_at, dt.created_at) >= :fromTime)
              and (:toTime is null or coalesce(dt.completed_at, dt.review_completed_at, dt.reviewed_at, dt.primary_diagnosed_at, dt.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
              and (
                    upper(coalesce(dt.task_type, '')) = 'FROZEN'
                 or trim(coalesce(dt.frozen_diagnosis_result, '')) <> ''
              )
            """, detailParams(filter));
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

    private String sourceNoteFor(String indicatorCode) {
        return switch (indicatorCode) {
            case "QC_SPECIMEN_FIXATION_RATE" -> "代理口径：按 specimens + specimen_fixation_records 的固定完成状态与不合格原因统计。";
            case "QC_UNQUALIFIED_SPECIMEN_COUNT" -> "代理口径：统计 qualified_flag=0、存在不合格原因或固定状态异常的标本。";
            case "QC_CLINICAL_MATCH_RATE" -> "代理口径：按申请单临床诊断与最新病理报告最终诊断是否齐备统计。";
            case "QC_FIRST_LINE_MATCH_RATE" -> "代理口径：按诊断任务是否存在首诊时间与审核完成记录统计。";
            case "QC_FROZEN_PARAFFIN_MATCH_RATE" -> "代理口径：按冰冻相关诊断任务的冰冻结果与最终病理诊断是否齐备统计。";
            case "QC_CYTOLOGY_MATCH_RATE" -> "代理口径：按细胞学标本病例是否形成病理报告且最终诊断非空统计。";
            case "QC_CONSULTATION_MATCH_RATE" -> "代理口径：按 consultation_cases 的完成状态与会诊意见闭环统计。";
            case "QC_CANCELLED_REVIEW_COUNT" -> "代理口径：沿用 report_revision_requests 作为最接近的修订申请实体记录数。";
            case "QC_TECHNICAL_QUALITY_COUNT" -> "代理口径：按 medical_orders 中已完成的技术相关医嘱闭环记录统计。";
            case "QC_GROSSING_QUALITY_COUNT" -> "代理口径：按 samplings 中已完成的取材任务记录统计。";
            case "QC_REPORT_RELEASE_DAYS" -> "代理口径：按已发布 pathology_reports 计算 submitted_at/created_at 到 published_at 的平均天数。";
            case "QC_SPECIMEN_PROCESS_HOURS" -> "代理口径：按病例接收时间到报告发布时间的平均小时数统计。";
            case "QC_DIAGNOSIS_TIMELINESS_RATE" -> "代理口径：按已发布报告是否在诊断任务 SLA 截止时间内完成统计。";
            case "QC_FROZEN_DIAGNOSIS_TIMELINESS_RATE" -> "只读代理口径：术中快速冰冻诊断以 diagnostic_tasks.task_type=FROZEN 或存在 frozen_diagnosis_result 识别，诊断阶段默认 SLA "
                + DEFAULT_FROZEN_DIAGNOSIS_SLA_MINUTES + " 分钟，按切片完成到诊断完成时间计算及时率。";
            case "QC_FROZEN_TIMEOUT_COUNT" -> "只读代理口径：冰冻超时汇总取取材、切片、诊断三阶段 FAIL 明细数；默认 SLA 取材 "
                + DEFAULT_FROZEN_GROSSING_SLA_MINUTES + " 分钟、切片 "
                + DEFAULT_FROZEN_SLICING_SLA_MINUTES + " 分钟、诊断 "
                + DEFAULT_FROZEN_DIAGNOSIS_SLA_MINUTES + " 分钟。";
            case "QC_FROZEN_GROSSING_TIMEOUT_COUNT" -> "只读代理口径：冰冻取材超时按病例接收到取材完成 sampled_at 计算，默认 SLA "
                + DEFAULT_FROZEN_GROSSING_SLA_MINUTES + " 分钟。";
            case "QC_FROZEN_SLICING_TIMEOUT_COUNT" -> "只读代理口径：冰冻切片超时按取材完成 sampled_at 到切片完成 sliced_at 计算，默认 SLA "
                + DEFAULT_FROZEN_SLICING_SLA_MINUTES + " 分钟。";
            case "QC_FROZEN_DIAGNOSIS_TIMEOUT_COUNT" -> "只读代理口径：冰冻诊断超时按切片完成 sliced_at 到诊断完成时间计算，默认 SLA "
                + DEFAULT_FROZEN_DIAGNOSIS_SLA_MINUTES + " 分钟。";
            case "QC_REPORT_CHANGE_COUNT" -> "只读代理口径：更改报告统计合并 report_versions 中 version_no>1 的版本变更与 report_revision_requests 修订申请，可追溯 report_id 与病例标识。";
            case "QC_REPORT_CHANGE_DOCTOR_COUNT" -> "只读代理口径：按 report_versions.signed_by_name 与 report_revision_requests.requested_by_name 统计参与报告修改/修订的医生数。";
            case "QC_REPORT_MODIFICATION_REASON_COUNT" -> "只读代理口径：按 report_versions.version_status 汇总修改报告原因代理分布，可追溯 report_id 与病例标识。";
            case "QC_REPORT_REVISION_REASON_COUNT" -> "只读代理口径：按 report_revision_requests.request_reason 汇总修订原因分布，可追溯 report_id 与病例标识。";
            case "QC_UNQUALIFIED_SPECIMEN_RATE" -> "代理口径：不合格占比以 specimens 总数为分母，以 qualified_flag=0、unqualified_reason 非空或固定状态 ABNORMAL 为分子；真实字段来自 specimens/specimen_fixation_records。";
            case "QC_UNQUALIFIED_SPECIMEN_REASON_COUNT" -> "代理口径：不合格原因分布优先使用 specimens.unqualified_reason，缺失时用 specimen_fixation_records/specimens.fixation_status=ABNORMAL 或 qualified_flag=0 代理。";
            case "QC_CRITICAL_VALUE_COUNT" -> "只读代理口径：按 user_notifications 中 topic_code=CRITICAL_VALUE 的危急值通知数量统计；真实危急值原因填写后续跨模块实现。";
            case "QC_CRITICAL_VALUE_REPORT_TIMELINESS_RATE" -> "只读代理口径：按危急值通知 created_at 后 30 分钟内 read_at 已确认统计及时率；真实上报闭环后续跨模块实现。";
            case "QC_CRITICAL_VALUE_REASON_ANALYSIS_COUNT" -> "只读代理口径：按危急值通知 level + summary/title 汇总原因分布；切片人填写真实原因后续跨模块实现。";
            default -> "当前指标未配置代理说明。";
        };
    }

    private String sourceNoteForNonQualityMetric(String indicatorCode, StatFilter filter) {
        String periodMode = normalizePeriodMode(filter.periodMode());
        return switch (indicatorCode) {
            case "OP_CASE_VOLUME" -> "按 pathology_cases 接收/创建时间统计病例量；趋势粒度：" + periodMode + "。";
            case "OP_BILLING_AMOUNT" -> "按 billing_records 成功收费金额统计；趋势粒度：" + periodMode + "。";
            case "OP_PERFORMANCE_WORKLOAD" -> "按诊断任务数 + 已完成医嘱数作为绩效工作量代理口径；趋势粒度：" + periodMode + "。";
            case "WL_DIAGNOSTIC_TASK_COUNT" -> "按 diagnostic_tasks 创建时间统计诊断任务量，支持角色和人员过滤；趋势粒度：" + periodMode + "。";
            case "WL_MEDICAL_ORDER_COUNT" -> "按 medical_orders 完成/创建时间统计医嘱量，支持角色和人员过滤；趋势粒度：" + periodMode + "。";
            default -> "当前指标未配置工作量趋势口径。";
        };
    }

    private void appendCsvRow(StringBuilder builder, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            String value = values.get(index);
            if (value == null) {
                continue;
            }
            builder.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        builder.append("\r\n");
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return null;
    }

    private String stringify(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private String formatDecimal(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String joinInfo(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        if (normalizedLeft == null) {
            return normalizedRight == null ? "" : normalizedRight;
        }
        if (normalizedRight == null) {
            return normalizedLeft;
        }
        return normalizedLeft + "；" + normalizedRight;
    }

    private String extractJsonValue(String payload, String key) {
        String normalizedPayload = normalize(payload);
        if (normalizedPayload == null) {
            return null;
        }
        String marker = "\"" + key + "\"";
        int keyIndex = normalizedPayload.indexOf(marker);
        if (keyIndex < 0) {
            return null;
        }
        int colonIndex = normalizedPayload.indexOf(':', keyIndex + marker.length());
        if (colonIndex < 0) {
            return null;
        }
        int valueStart = normalizedPayload.indexOf('"', colonIndex + 1);
        if (valueStart < 0) {
            return null;
        }
        int valueEnd = normalizedPayload.indexOf('"', valueStart + 1);
        if (valueEnd <= valueStart) {
            return null;
        }
        return normalize(normalizedPayload.substring(valueStart + 1, valueEnd));
    }

    private String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String normalizeEmpty(String value) {
        String normalized = normalize(value);
        return normalized == null ? "" : normalized;
    }

    private record MetricValue(String value, String unit) {
    }

    private record MetricSnapshot(
        String metricValue,
        String metricUnit,
        String numerator,
        String denominator
    ) {
    }

    private record QualityDetailDataset(
        String indicatorCode,
        String availabilityStatus,
        long eligibleCount,
        long passCount,
        long failCount,
        String sourceNote,
        List<QualityDetailRecord> items
    ) {
    }

    private record QualityDetailRecord(
        String pathologyNo,
        String applicationNo,
        String specimenNo,
        LocalDateTime occurredAt,
        String detailStatus,
        String reason,
        BigDecimal numericValue
    ) {
    }

    private enum CriticalValueMode {
        COUNT,
        REASON_ANALYSIS,
        TIMELINESS
    }

    private enum FrozenTimeoutMode {
        ALL,
        DIAGNOSIS,
        GROSSING,
        SLICING
    }

    private enum ReportChangeMode {
        ALL,
        DOCTOR,
        MODIFICATION_REASON,
        REVISION_REASON
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

    private record TrendBucket(
        String label,
        LocalDateTime from,
        LocalDateTime to
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

    private record StatFilter(
        LocalDateTime from,
        LocalDateTime to,
        String departmentId,
        String roleId,
        String workloadUserId,
        String periodMode
    ) {

        private static StatFilter from(QueryStatReportCommand command) {
            return new StatFilter(
                command.from(),
                command.to(),
                normalizeValue(command.departmentId()),
                normalizeValue(command.roleId()),
                normalizeValue(command.workloadUserId()),
                normalizeValue(command.periodMode()));
        }

        private static StatFilter from(QueryStatReportDetailCommand command) {
            return new StatFilter(
                command.from(),
                command.to(),
                normalizeValue(command.departmentId()),
                null,
                null,
                null);
        }

        private StatFilter forCaseScopedMetrics() {
            return new StatFilter(from, to, departmentId, null, null, periodMode);
        }

        private StatFilter forWorkloadMetrics() {
            return this;
        }

        private StatFilter withRange(LocalDateTime newFrom, LocalDateTime newTo) {
            return new StatFilter(newFrom, newTo, departmentId, roleId, workloadUserId, periodMode);
        }

        private static String normalizeValue(String value) {
            return value == null || value.isBlank() ? null : value;
        }
    }
}
