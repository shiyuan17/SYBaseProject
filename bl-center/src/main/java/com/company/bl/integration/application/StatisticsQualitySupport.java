package com.company.bl.integration.application;

import com.company.bl.integration.infrastructure.M6JdbcRepository;
import com.company.bl.integration.infrastructure.M6StatisticsRows;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
final class StatisticsQualitySupport extends StatisticsQualitySupportBase {

    private final StatisticsQualityAdvancedSupport advancedSupport;

    StatisticsQualitySupport(M6JdbcRepository repository, NamedParameterJdbcTemplate jdbcTemplate) {
        super(repository, jdbcTemplate);
        this.advancedSupport = new StatisticsQualityAdvancedSupport(repository, jdbcTemplate);
    }

    StatisticsService.StatRowView buildQualityStatRow(M6StatisticsRows.StatIndicatorDefinitionRow indicator, StatFilter filter) {
        QualityDetailDataset dataset = buildQualityDetailDataset(indicator.indicatorCode(), filter);
        MetricSnapshot metric = summarizeQualityMetric(indicator.indicatorCode(), indicator.aggregationType(), dataset);
        return new StatisticsService.StatRowView(
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

    @Override
    protected MetricSnapshot summarizeQualityMetric(String indicatorCode,
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

    @Override
    protected QualityDetailDataset buildQualityDetailDataset(String indicatorCode, StatFilter filter) {
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
            default -> advancedSupport.buildAdvancedQualityDetailDataset(indicatorCode, filter);
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
}
