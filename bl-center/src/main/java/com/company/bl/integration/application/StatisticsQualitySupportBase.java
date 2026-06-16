package com.company.bl.integration.application;

import com.company.bl.integration.infrastructure.M6JdbcRepository;
import com.company.bl.integration.infrastructure.M6StatisticsRows;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract class StatisticsQualitySupportBase {

    protected static final String STATUS_AVAILABLE = "AVAILABLE";
    protected static final String STATUS_PARTIAL = "PARTIAL";
    protected static final String STATUS_UNAVAILABLE = "UNAVAILABLE";

    protected static final String DETAIL_STATUS_FAIL = "FAIL";
    protected static final String DETAIL_STATUS_INFO = "INFO";
    protected static final String DETAIL_STATUS_PASS = "PASS";

    protected static final int DEFAULT_FROZEN_GROSSING_SLA_MINUTES = 30;
    protected static final int DEFAULT_FROZEN_SLICING_SLA_MINUTES = 30;
    protected static final int DEFAULT_FROZEN_DIAGNOSIS_SLA_MINUTES = 30;

    protected final M6JdbcRepository repository;
    protected final NamedParameterJdbcTemplate jdbcTemplate;

    protected StatisticsQualitySupportBase(M6JdbcRepository repository, NamedParameterJdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    protected abstract QualityDetailDataset buildQualityDetailDataset(String indicatorCode, StatFilter filter);

    protected abstract MetricSnapshot summarizeQualityMetric(String indicatorCode, String aggregationType, QualityDetailDataset dataset);

    protected List<StatisticsService.TrendPointView> buildQualityTrendPoints(String indicatorCode,
                                                                             String aggregationType,
                                                                             StatFilter filter) {
        if (filter.from() == null || filter.to() == null) {
            return List.of();
        }
        return buildTrendBuckets(filter).stream()
            .map(bucket -> {
                QualityDetailDataset dataset = buildQualityDetailDataset(indicatorCode, filter.withRange(bucket.from(), bucket.to()));
                MetricSnapshot metric = summarizeQualityMetric(indicatorCode, aggregationType, dataset);
                return new StatisticsService.TrendPointView(bucket.label(), metric.metricValue());
            })
            .toList();
    }

    protected List<StatisticsService.BreakdownView> buildQualityBreakdowns(QualityDetailDataset dataset) {
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
                return List.of(new StatisticsService.BreakdownView("通过", String.valueOf(dataset.passCount())));
            }
            if (dataset.eligibleCount() == 0 && dataset.items().isEmpty()) {
                return List.of();
            }
        }
        return reasonCounts.entrySet().stream()
            .map(item -> new StatisticsService.BreakdownView(item.getKey(), String.valueOf(item.getValue())))
            .toList();
    }

    protected List<TrendBucket> buildTrendBuckets(StatFilter filter) {
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

    protected TrendBucket trimBucket(String label,
                                     LocalDateTime bucketStart,
                                     LocalDateTime bucketEnd,
                                     LocalDateTime from,
                                     LocalDateTime to) {
        LocalDateTime trimmedStart = bucketStart.isBefore(from) ? from : bucketStart;
        LocalDateTime trimmedEnd = bucketEnd.isAfter(to) ? to : bucketEnd;
        return new TrendBucket(label, trimmedStart, trimmedEnd);
    }

    protected int quarterOf(LocalDateTime value) {
        return ((value.getMonthValue() - 1) / 3) + 1;
    }

    protected String normalizePeriodMode(String value) {
        String normalized = normalize(value);
        if ("year".equalsIgnoreCase(normalized)) {
            return "year";
        }
        if ("quarter".equalsIgnoreCase(normalized)) {
            return "quarter";
        }
        return "month";
    }

    protected List<M6StatisticsRows.StatIndicatorDefinitionRow> selectIndicators(StatisticsService.QueryStatReportCommand command, String category) {
        if (normalize(command.indicatorCode()) != null) {
            M6StatisticsRows.StatIndicatorDefinitionRow row = repository.findStatIndicatorDefinitionByCode(command.indicatorCode());
            return row == null ? List.of() : List.of(row);
        }
        return repository.findStatIndicatorDefinitions(category);
    }

    protected String resolveCategory(StatisticsService.QueryStatReportCommand command) {
        if (normalize(command.category()) != null) {
            return command.category();
        }
        if (normalize(command.templateCode()) != null) {
            M6StatisticsRows.StatReportTemplateRow template = repository.findStatReportTemplateByCode(command.templateCode());
            return template == null ? "QUALITY" : template.templateType();
        }
        return "QUALITY";
    }

    protected String resolveTemplateId(String templateCode) {
        if (normalize(templateCode) == null) {
            return null;
        }
        M6StatisticsRows.StatReportTemplateRow template = repository.findStatReportTemplateByCode(templateCode);
        return template == null ? null : template.id();
    }

    protected long countPathologyCases(StatFilter filter) {
        return queryForLong("""
            select count(*)
            from pathology_cases pc
            join applications a on a.id = pc.application_id
            where (:fromTime is null or coalesce(pc.received_at, pc.created_at) >= :fromTime)
              and (:toTime is null or coalesce(pc.received_at, pc.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """, params(filter));
    }

    protected long countEligibleSpecimens(StatFilter filter) {
        return queryForLong("""
            select count(*)
            from specimens s
            join applications a on a.id = s.application_id
            where (:fromTime is null or coalesce(s.registered_at, s.created_at) >= :fromTime)
              and (:toTime is null or coalesce(s.registered_at, s.created_at) <= :toTime)
              and (:departmentId is null or a.submitting_department_id = :departmentId)
            """, params(filter));
    }

    protected BigDecimal sumBillingAmount(StatFilter filter) {
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

    protected long countGlobalReagentWarnings() {
        return queryForLong("""
            select count(*)
            from reagent_stocks
            where low_stock_threshold is not null
              and stock_quantity <= low_stock_threshold
            """, new MapSqlParameterSource());
    }

    protected long countWorkloadDiagnosticTasks(StatFilter filter) {
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

    protected long countWorkloadMedicalOrders(StatFilter filter) {
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

    protected long queryForLong(String sql, MapSqlParameterSource params) {
        Long value = jdbcTemplate.queryForObject(sql, params, Long.class);
        return value == null ? 0L : value;
    }

    protected BigDecimal queryForDecimal(String sql, MapSqlParameterSource params) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    protected MapSqlParameterSource params(StatFilter filter) {
        return new MapSqlParameterSource()
            .addValue("fromTime", filter.from())
            .addValue("toTime", filter.to())
            .addValue("departmentId", filter.departmentId())
            .addValue("roleId", filter.roleId())
            .addValue("workloadUserId", filter.workloadUserId());
    }

    protected MapSqlParameterSource detailParams(StatFilter filter) {
        return new MapSqlParameterSource()
            .addValue("fromTime", filter.from())
            .addValue("toTime", filter.to())
            .addValue("departmentId", filter.departmentId());
    }

    protected MetricValue countMetric(long count) {
        return new MetricValue(String.valueOf(count), "COUNT");
    }

    protected MetricValue decimalMetric(BigDecimal value, String unit) {
        return new MetricValue(formatDecimal(value), unit);
    }

    protected BigDecimal percentValue(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    protected BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            total = total.add(value);
        }
        return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    protected BigDecimal daysBetween(LocalDateTime from, LocalDateTime to) {
        return BigDecimal.valueOf(Duration.between(from, to).toMinutes())
            .divide(BigDecimal.valueOf(60 * 24), 2, RoundingMode.HALF_UP);
    }

    protected BigDecimal hoursBetween(LocalDateTime from, LocalDateTime to) {
        return BigDecimal.valueOf(Duration.between(from, to).toMinutes())
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    protected BigDecimal minutesBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return null;
        }
        return BigDecimal.valueOf(Duration.between(from, to).toMinutes())
            .setScale(2, RoundingMode.HALF_UP);
    }

    protected String frozenTimeoutReason(String stage, BigDecimal minutes, int slaMinutes) {
        if (minutes == null) {
            return stage + "缺少起止时间，按超时处理；默认 SLA " + slaMinutes + " 分钟";
        }
        return stage + "耗时 " + formatDecimal(minutes) + " 分钟，默认 SLA " + slaMinutes + " 分钟";
    }

    protected long countFrozenDiagnosticTasks(StatFilter filter) {
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

    protected String sourceNoteFor(String indicatorCode) {
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

    protected String sourceNoteForNonQualityMetric(String indicatorCode, StatFilter filter) {
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

    protected void appendCsvRow(StringBuilder builder, List<String> values) {
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

    protected LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return null;
    }

    protected String stringify(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    protected String formatDecimal(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    protected String joinInfo(String left, String right) {
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

    protected String extractJsonValue(String payload, String key) {
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

    protected String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    protected String normalizeEmpty(String value) {
        String normalized = normalize(value);
        return normalized == null ? "" : normalized;
    }
    protected QualityDetailDataset datasetFromRateItems(String indicatorCode,
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
}
