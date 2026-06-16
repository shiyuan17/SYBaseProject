package com.company.bl.integration.application;

import com.company.bl.integration.infrastructure.M6JdbcRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

final class StatisticsQualityAdvancedSupport extends StatisticsQualitySupportBase {

    StatisticsQualityAdvancedSupport(M6JdbcRepository repository, NamedParameterJdbcTemplate jdbcTemplate) {
        super(repository, jdbcTemplate);
    }

    QualityDetailDataset buildAdvancedQualityDetailDataset(String indicatorCode, StatFilter filter) {
        return switch (normalize(indicatorCode)) {
            case "QC_REPORT_CHANGE_COUNT" -> buildReportChangeDataset("QC_REPORT_CHANGE_COUNT", filter, ReportChangeMode.ALL);
            case "QC_REPORT_CHANGE_DOCTOR_COUNT" -> buildReportChangeDataset("QC_REPORT_CHANGE_DOCTOR_COUNT", filter, ReportChangeMode.DOCTOR);
            case "QC_REPORT_MODIFICATION_REASON_COUNT" -> buildReportChangeDataset("QC_REPORT_MODIFICATION_REASON_COUNT", filter, ReportChangeMode.MODIFICATION_REASON);
            case "QC_REPORT_REVISION_REASON_COUNT" -> buildReportChangeDataset("QC_REPORT_REVISION_REASON_COUNT", filter, ReportChangeMode.REVISION_REASON);
            case "QC_UNQUALIFIED_SPECIMEN_RATE" -> buildUnqualifiedSpecimenRateDataset(filter);
            case "QC_UNQUALIFIED_SPECIMEN_REASON_COUNT" -> buildUnqualifiedSpecimenReasonDataset(filter);
            case "QC_CRITICAL_VALUE_COUNT" -> buildCriticalValueNotificationDataset("QC_CRITICAL_VALUE_COUNT", filter, CriticalValueMode.COUNT);
            case "QC_CRITICAL_VALUE_REPORT_TIMELINESS_RATE" -> buildCriticalValueNotificationDataset("QC_CRITICAL_VALUE_REPORT_TIMELINESS_RATE", filter, CriticalValueMode.TIMELINESS);
            case "QC_CRITICAL_VALUE_REASON_ANALYSIS_COUNT" -> buildCriticalValueNotificationDataset("QC_CRITICAL_VALUE_REASON_ANALYSIS_COUNT", filter, CriticalValueMode.REASON_ANALYSIS);
            default -> new QualityDetailDataset(indicatorCode, STATUS_UNAVAILABLE, 0, 0, 0, "当前指标未配置可计算明细代理规则。", List.of());
        };
    }

    @Override
    protected QualityDetailDataset buildQualityDetailDataset(String indicatorCode, StatFilter filter) {
        return buildAdvancedQualityDetailDataset(indicatorCode, filter);
    }

    @Override
    protected MetricSnapshot summarizeQualityMetric(String indicatorCode, String aggregationType, QualityDetailDataset dataset) {
        throw new UnsupportedOperationException("Advanced quality support does not summarize direct metrics");
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
}
