package com.company.bl.integration.application;

import com.company.bl.integration.infrastructure.M6JdbcRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
class PathologyScreenDashboardSupport extends StatisticsQualitySupportBase {

    private static final DateTimeFormatter YEAR_MONTH_NOTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String STATUS_UNKNOWN = "UNAVAILABLE";

    private final StatisticsQualitySupport qualitySupport;

    PathologyScreenDashboardSupport(M6JdbcRepository repository,
                                    NamedParameterJdbcTemplate jdbcTemplate,
                                    StatisticsQualitySupport qualitySupport) {
        super(repository, jdbcTemplate);
        this.qualitySupport = qualitySupport;
    }

    StatisticsService.PathologyScreenDashboardView queryDashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yearStart = today.withDayOfYear(1).atStartOfDay();
        LocalDateTime yearEnd = now;
        LocalDateTime lastMonthStart = today.minusMonths(1).withDayOfMonth(1).atStartOfDay();
        LocalDateTime lastMonthEnd = today.withDayOfMonth(1).atStartOfDay().minusSeconds(1);
        int currentYear = today.getYear();

        return new StatisticsService.PathologyScreenDashboardView(
            buildSummaryCards(yearStart, yearEnd, lastMonthStart, lastMonthEnd),
            buildReportRevisionRateTrend(currentYear),
            buildTechnicalQualificationRates(lastMonthStart, lastMonthEnd),
            buildDiagnosisWorkloadRows(currentYear),
            buildThreeYearTechnicalRates(currentYear, now),
            buildStructuredReportSummary(lastMonthStart, lastMonthEnd),
            buildLastMonthWorkload(lastMonthStart, lastMonthEnd),
            buildThreeYearReportQualityRates(currentYear, now),
            buildOverallComplianceRates(lastMonthStart, lastMonthEnd));
    }

    private StatisticsService.PathologyScreenSummaryCardsView buildSummaryCards(LocalDateTime yearStart,
                                                                                LocalDateTime yearEnd,
                                                                                LocalDateTime lastMonthStart,
                                                                                LocalDateTime lastMonthEnd) {
        return new StatisticsService.PathologyScreenSummaryCardsView(
            buildMetricCard(
                "全年病例总数（例）",
                String.valueOf(countApplications(yearStart, yearEnd)),
                STATUS_AVAILABLE,
                "按 applications.created_at 统计当前自然年申请单总数。"),
            buildMetricCard(
                "上月病例总数（例）",
                String.valueOf(countApplications(lastMonthStart, lastMonthEnd)),
                STATUS_AVAILABLE,
                "按 applications.created_at 统计上月申请单总数。"),
            buildMetricCard(
                "上月报告及时率",
                formatPercent(lastMonthPublishedTimeliness(lastMonthStart, lastMonthEnd)),
                STATUS_AVAILABLE,
                "按上月已发布报告是否在诊断任务 SLA 截止时间内发布统计。"));
    }

    private StatisticsService.PathologyScreenSectionView buildReportRevisionRateTrend(int year) {
        List<StatisticsService.PathologyScreenTrendItemView> items = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            LocalDateTime from = LocalDate.of(year, month, 1).atStartOfDay();
            LocalDateTime to = YearMonth.of(year, month).atEndOfMonth().atTime(LocalTime.of(23, 59, 59));
            RateSnapshot snapshot = signedReportRevisionRate(from, to);
            items.add(new StatisticsService.PathologyScreenTrendItemView(
                from.format(YEAR_MONTH_NOTE_FORMATTER),
                formatPercent(snapshot.value()),
                snapshot.status(),
                snapshot.sourceNote()));
        }
        return new StatisticsService.PathologyScreenSectionView(
            STATUS_AVAILABLE,
            "按已签发/已发布报告中发生版本变更或修订申请的病例数占比统计当前自然年每月趋势。",
            items);
    }

    private StatisticsService.PathologyScreenSectionView buildTechnicalQualificationRates(LocalDateTime from,
                                                                                          LocalDateTime to) {
        List<StatisticsService.PathologyScreenMetricItemView> items = List.of(
            metricItem("规范化固定率", qualityDatasetRate("QC_SPECIMEN_FIXATION_RATE", from, to)),
            metricItem("HE染色切片优良率", stainingRate("HE", from, to)),
            metricItem("免疫组化染色切片优良率", stainingRate("IHC", from, to)),
            metricItem("冰冻切片染色优良率", stainingRate("FROZEN", from, to)),
            metricItem("细胞学制片染色优良率", stainingRate("CYTOLOGY", from, to)),
            metricItem("特殊染色制片优良率", stainingRate("SPECIAL", from, to)));
        return buildMetricSection(items, "技术组指标基于标本固定记录、slide_stainings、slides.quality_status 与医嘱/任务类型代理聚合。");
    }

    private StatisticsService.PathologyScreenTableSectionView buildDiagnosisWorkloadRows(int year) {
        LocalDateTime januaryStart = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime januaryEnd = YearMonth.of(year, 1).atEndOfMonth().atTime(LocalTime.of(23, 59, 59));
        LocalDateTime februaryStart = LocalDate.of(year, 2, 1).atStartOfDay();
        LocalDateTime februaryEnd = YearMonth.of(year, 2).atEndOfMonth().atTime(LocalTime.of(23, 59, 59));

        List<StatisticsService.PathologyScreenWorkloadRowView> items = List.of(
            workloadRow("常规", workloadCount(januaryStart, januaryEnd, "ROUTINE"), workloadCount(februaryStart, februaryEnd, "ROUTINE")),
            workloadRow("冰冻", workloadCount(januaryStart, januaryEnd, "FROZEN"), workloadCount(februaryStart, februaryEnd, "FROZEN")),
            workloadRow("免疫组化", workloadCount(januaryStart, januaryEnd, "IHC"), workloadCount(februaryStart, februaryEnd, "IHC")),
            workloadRow("细胞学", workloadCount(januaryStart, januaryEnd, "CYTOLOGY"), workloadCount(februaryStart, februaryEnd, "CYTOLOGY")),
            workloadRow("特殊染色", workloadCount(januaryStart, januaryEnd, "SPECIAL"), workloadCount(februaryStart, februaryEnd, "SPECIAL")));
        return new StatisticsService.PathologyScreenTableSectionView(
            STATUS_AVAILABLE,
            "按 medical_orders.order_type 分类统计当前自然年 1 月、2 月医嘱量，并计算 (本期-上期)/上期。",
            items);
    }

    private StatisticsService.PathologyScreenThreeYearSectionView buildThreeYearTechnicalRates(int currentYear,
                                                                                                LocalDateTime currentMoment) {
        List<StatisticsService.PathologyScreenThreeYearItemView> items = new ArrayList<>();
        for (int year = currentYear - 2; year <= currentYear; year++) {
            LocalDateTime from = LocalDate.of(year, 1, 1).atStartOfDay();
            LocalDateTime to = year == currentYear
                ? currentMoment
                : LocalDate.of(year, 12, 31).atTime(LocalTime.of(23, 59, 59));
            items.add(new StatisticsService.PathologyScreenThreeYearItemView(
                year + "年",
                List.of(
                    metricItem("规范化固定率", qualityDatasetRate("QC_SPECIMEN_FIXATION_RATE", from, to)),
                    metricItem("HE染色切片优良率", stainingRate("HE", from, to)),
                    metricItem("免疫组化", stainingRate("IHC", from, to)))));
        }
        return new StatisticsService.PathologyScreenThreeYearSectionView(
            STATUS_AVAILABLE,
            "近三年按当前自然年及前两年累计返回；当前年截至当前时点。",
            items);
    }

    private StatisticsService.PathologyScreenStructuredReportSummaryView buildStructuredReportSummary(LocalDateTime from,
                                                                                                      LocalDateTime to) {
        List<TemplateCountRow> rows = structuredTemplateCounts(from, to);
        int templateTypeCount = rows.size();
        long reportCount = rows.stream().mapToLong(TemplateCountRow::reportCount).sum();
        List<StatisticsService.PathologyScreenMetricItemView> topTemplates = rows.stream()
            .limit(12)
            .map(row -> new StatisticsService.PathologyScreenMetricItemView(
                row.templateName(),
                String.valueOf(row.reportCount()),
                STATUS_AVAILABLE,
                "按 sampling_templates 与 pathology_reports.rich_text_content 的模板编码代理匹配统计。"))
            .toList();
        return new StatisticsService.PathologyScreenStructuredReportSummaryView(
            buildMetricCard(
                "结构化报告类型（种）",
                String.valueOf(templateTypeCount),
                templateTypeCount > 0 ? STATUS_AVAILABLE : STATUS_PARTIAL,
                "按 sampling_templates 可匹配到的模板数量统计。"),
            buildMetricCard(
                "结构化报告工作量（例）",
                String.valueOf(reportCount),
                templateTypeCount > 0 ? STATUS_AVAILABLE : STATUS_PARTIAL,
                "按模板编码命中已发布报告的数量统计。"),
            topTemplates,
            templateTypeCount > 0 ? STATUS_AVAILABLE : STATUS_PARTIAL,
            templateTypeCount > 0
                ? "当前使用 sampling_templates.template_code 对 rich_text_content 进行模板编码代理匹配。"
                : "未发现可稳定匹配的模板编码，当前仅返回空结构。");
    }

    private StatisticsService.PathologyScreenSectionView buildLastMonthWorkload(LocalDateTime from, LocalDateTime to) {
        List<StatisticsService.PathologyScreenMetricItemView> items = List.of(
            simpleMetricItem("切片", String.valueOf(countSlicings(from, to)), "按 slicings.sliced_at 统计上月完成切片数量。"),
            simpleMetricItem("脱水", String.valueOf(countDehydrations(from, to)), "按 dehydration_batches.completed_at 统计上月完成脱水数量。"),
            simpleMetricItem("包埋", String.valueOf(countEmbeddings(from, to)), "按 embeddings.ended_at/created_at 统计上月包埋数量。"),
            simpleMetricItem("报告", String.valueOf(countPublishedReports(from, to)), "按 pathology_reports.published_at 统计上月已发布报告数量。"));
        return buildMetricSection(items, "上月工作量固定展示切片、脱水、包埋、报告四项。");
    }

    private StatisticsService.PathologyScreenThreeYearSectionView buildThreeYearReportQualityRates(int currentYear,
                                                                                                    LocalDateTime currentMoment) {
        List<StatisticsService.PathologyScreenThreeYearItemView> items = new ArrayList<>();
        for (int year = currentYear - 2; year <= currentYear; year++) {
            LocalDateTime from = LocalDate.of(year, 1, 1).atStartOfDay();
            LocalDateTime to = year == currentYear
                ? currentMoment
                : LocalDate.of(year, 12, 31).atTime(LocalTime.of(23, 59, 59));
            items.add(new StatisticsService.PathologyScreenThreeYearItemView(
                year + "年",
                List.of(
                    metricItem("病理诊断及时率", qualityDatasetRate("QC_DIAGNOSIS_TIMELINESS_RATE", from, to)),
                    metricItem("组织病理诊断及时率", routineReportTimelinessRate(from, to)),
                    metricItem("冰冻快速病理诊断及时率", qualityDatasetRate("QC_FROZEN_DIAGNOSIS_TIMELINESS_RATE", from, to)))));
        }
        return new StatisticsService.PathologyScreenThreeYearSectionView(
            STATUS_AVAILABLE,
            "近三年病理/组织病理/冰冻快速病理诊断及时率。",
            items);
    }

    private StatisticsService.PathologyScreenSectionView buildOverallComplianceRates(LocalDateTime from, LocalDateTime to) {
        List<StatisticsService.PathologyScreenMetricItemView> items = List.of(
            metricItem("危急值上报及时率", criticalValueTenMinuteRate(from, to)),
            metricItem("细胞学病理诊断及时率", cytologyTimelinessRate(from, to)),
            metricItem("组织病理诊断及时率", routineReportTimelinessRate(from, to)),
            metricItem("术中快速病理诊断及时率", frozenTimelinessRate(from, to)),
            metricItem("术中快速诊断与石蜡诊断符合率", qualityDatasetRate("QC_FROZEN_PARAFFIN_MATCH_RATE", from, to)),
            metricItem("HE细胞学阳性结果与活检病理诊断符合率", qualityDatasetRate("QC_CYTOLOGY_MATCH_RATE", from, to)),
            metricItem("细胞学病理诊断质控符合率", cytologyQualityRate(from, to)));
        return buildMetricSection(items, "综合符合率区块混合复用 M6 质控口径与大屏专用代理计算。");
    }

    private StatisticsService.PathologyScreenSectionView buildMetricSection(List<StatisticsService.PathologyScreenMetricItemView> items,
                                                                            String sourceNote) {
        String status = items.stream().anyMatch(item -> STATUS_PARTIAL.equals(item.status())) ? STATUS_PARTIAL : STATUS_AVAILABLE;
        return new StatisticsService.PathologyScreenSectionView(status, sourceNote, items);
    }

    private StatisticsService.PathologyScreenMetricCardView buildMetricCard(String label,
                                                                            String value,
                                                                            String status,
                                                                            String sourceNote) {
        return new StatisticsService.PathologyScreenMetricCardView(label, value, status, sourceNote);
    }

    private StatisticsService.PathologyScreenMetricItemView simpleMetricItem(String label,
                                                                             String value,
                                                                             String sourceNote) {
        return new StatisticsService.PathologyScreenMetricItemView(label, value, STATUS_AVAILABLE, sourceNote);
    }

    private StatisticsService.PathologyScreenMetricItemView metricItem(String label, RateSnapshot snapshot) {
        return new StatisticsService.PathologyScreenMetricItemView(
            label,
            formatPercent(snapshot.value()),
            snapshot.status(),
            snapshot.sourceNote());
    }

    private StatisticsService.PathologyScreenWorkloadRowView workloadRow(String label, long januaryCount, long februaryCount) {
        return new StatisticsService.PathologyScreenWorkloadRowView(
            label,
            String.valueOf(januaryCount),
            String.valueOf(februaryCount),
            monthOverMonth(februaryCount, januaryCount),
            januaryCount == 0 ? STATUS_PARTIAL : STATUS_AVAILABLE,
            "按医嘱分类统计 1 月和 2 月总量。");
    }

    private long countApplications(LocalDateTime from, LocalDateTime to) {
        return queryForLong("""
            select count(*)
            from applications
            where (:fromTime is null or created_at >= :fromTime)
              and (:toTime is null or created_at <= :toTime)
            """, new MapSqlParameterSource()
            .addValue("fromTime", from)
            .addValue("toTime", to));
    }

    private long countPublishedReports(LocalDateTime from, LocalDateTime to) {
        return queryForLong("""
            select count(*)
            from pathology_reports
            where report_status = 'PUBLISHED'
              and (:fromTime is null or published_at >= :fromTime)
              and (:toTime is null or published_at <= :toTime)
            """, new MapSqlParameterSource()
            .addValue("fromTime", from)
            .addValue("toTime", to));
    }

    private long countDehydrations(LocalDateTime from, LocalDateTime to) {
        return queryForLong("""
            select count(*)
            from dehydration_batches
            where (:fromTime is null or completed_at >= :fromTime)
              and (:toTime is null or completed_at <= :toTime)
            """, new MapSqlParameterSource()
            .addValue("fromTime", from)
            .addValue("toTime", to));
    }

    private long countEmbeddings(LocalDateTime from, LocalDateTime to) {
        return queryForLong("""
            select count(*)
            from embeddings
            where (:fromTime is null or coalesce(ended_at, created_at) >= :fromTime)
              and (:toTime is null or coalesce(ended_at, created_at) <= :toTime)
            """, new MapSqlParameterSource()
            .addValue("fromTime", from)
            .addValue("toTime", to));
    }

    private long countSlicings(LocalDateTime from, LocalDateTime to) {
        return queryForLong("""
            select count(*)
            from slicings
            where (:fromTime is null or coalesce(sliced_at, created_at) >= :fromTime)
              and (:toTime is null or coalesce(sliced_at, created_at) <= :toTime)
            """, new MapSqlParameterSource()
            .addValue("fromTime", from)
            .addValue("toTime", to));
    }

    private BigDecimal lastMonthPublishedTimeliness(LocalDateTime from, LocalDateTime to) {
        String sql = """
            select count(*) as eligible_count,
                   sum(case when pr.published_at is not null and (sla_due_at is null or pr.published_at <= sla_due_at) then 1 else 0 end) as pass_count
            from pathology_reports pr
            left join (
                select case_id, max(sla_due_at) as sla_due_at
                from diagnostic_tasks
                group by case_id
            ) dt on dt.case_id = pr.case_id
            where pr.report_status = 'PUBLISHED'
              and pr.published_at >= :fromTime
              and pr.published_at <= :toTime
            """;
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
            .addValue("fromTime", from)
            .addValue("toTime", to), (rs, rowNum) -> percentValue(rs.getLong("pass_count"), rs.getLong("eligible_count")));
    }

    private RateSnapshot signedReportRevisionRate(LocalDateTime from, LocalDateTime to) {
        long total = queryForLong("""
            select count(distinct pr.case_id)
            from pathology_reports pr
            where pr.report_status in ('SIGNED', 'PUBLISHED')
              and (:fromTime is null or coalesce(pr.published_at, pr.signed_at, pr.created_at) >= :fromTime)
              and (:toTime is null or coalesce(pr.published_at, pr.signed_at, pr.created_at) <= :toTime)
            """, new MapSqlParameterSource()
            .addValue("fromTime", from)
            .addValue("toTime", to));
        long changed = queryForLong("""
            select count(distinct changed.case_id)
            from (
                select rv.case_id
                from report_versions rv
                where rv.version_no > 1
                  and (:fromTime is null or rv.created_at >= :fromTime)
                  and (:toTime is null or rv.created_at <= :toTime)
                union
                select rrr.case_id
                from report_revision_requests rrr
                where (:fromTime is null or coalesce(rrr.requested_at, rrr.created_at) >= :fromTime)
                  and (:toTime is null or coalesce(rrr.requested_at, rrr.created_at) <= :toTime)
            ) changed
            """, new MapSqlParameterSource()
            .addValue("fromTime", from)
            .addValue("toTime", to));
        return new RateSnapshot(
            total == 0 ? BigDecimal.ZERO : percentValue(changed, total),
            total == 0 ? STATUS_PARTIAL : STATUS_AVAILABLE,
            "分母为已签发/已发布报告对应病例数，分子为发生版本变更或修订申请的病例数。");
    }

    private RateSnapshot qualityDatasetRate(String indicatorCode, LocalDateTime from, LocalDateTime to) {
        QualityDetailDataset dataset = qualitySupport.buildQualityDetailDataset(
            indicatorCode,
            new StatFilter(from, to, null, null, null, "month"));
        return new RateSnapshot(
            percentValue(dataset.passCount(), dataset.eligibleCount()),
            dataset.availabilityStatus(),
            dataset.sourceNote());
    }

    private RateSnapshot stainingRate(String groupCode, LocalDateTime from, LocalDateTime to) {
        String sql = """
            select count(*) as eligible_count,
                   sum(case when upper(coalesce(sl.quality_status, '')) in ('GOOD', 'EXCELLENT', 'PASS', 'QUALIFIED') then 1 else 0 end) as pass_count
            from slide_stainings ss
            join slides sl on sl.id = ss.slide_id
            left join medical_orders mo on mo.case_id = ss.case_id
            left join diagnostic_tasks dt on dt.case_id = ss.case_id
            where (:fromTime is null or coalesce(ss.stained_at, ss.created_at) >= :fromTime)
              and (:toTime is null or coalesce(ss.stained_at, ss.created_at) <= :toTime)
              and (
                    (:groupCode = 'HE' and upper(coalesce(ss.staining_type, '')) = 'HE')
                 or (:groupCode = 'IHC' and (
                        upper(coalesce(ss.staining_type, '')) in ('IHC', 'IMMUNOHISTOCHEMISTRY')
                     or upper(coalesce(mo.order_type, '')) in ('IHC', 'IMMUNOHISTOCHEMISTRY')
                 ))
                 or (:groupCode = 'FROZEN' and (
                        upper(coalesce(ss.staining_type, '')) = 'FROZEN'
                     or upper(coalesce(dt.task_type, '')) = 'FROZEN'
                     or trim(coalesce(dt.frozen_diagnosis_result, '')) <> ''
                 ))
                 or (:groupCode = 'CYTOLOGY' and (
                        upper(coalesce(ss.staining_type, '')) like 'CYTOLOGY%'
                     or upper(coalesce(mo.order_type, '')) in ('CYTOLOGY', 'CYTOLOGY_CONSULTATION', 'CYTOLOGY_SMEAR', 'LIQUID_CYTOLOGY', 'GYNECOLOGY_LBC_CYTOLOGY', 'NON_GYNECOLOGY_LBC_CYTOLOGY')
                 ))
                 or (:groupCode = 'SPECIAL' and (
                        upper(coalesce(ss.staining_type, '')) like 'SPECIAL%'
                     or upper(coalesce(mo.order_type, '')) in ('SPECIAL_STAIN', 'SPECIAL_STAINING')
                 ))
              )
            """;
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
            .addValue("fromTime", from)
            .addValue("toTime", to)
            .addValue("groupCode", groupCode), (rs, rowNum) -> {
            long eligibleCount = rs.getLong("eligible_count");
            return new RateSnapshot(
                percentValue(rs.getLong("pass_count"), eligibleCount),
                eligibleCount == 0 ? STATUS_PARTIAL : STATUS_AVAILABLE,
                "按 slide_stainings + slides.quality_status 与染色/医嘱类型代理统计。");
        });
    }

    private long workloadCount(LocalDateTime from, LocalDateTime to, String groupCode) {
        return queryForLong("""
            select count(*)
            from medical_orders mo
            where mo.status = 'COMPLETED'
              and (:fromTime is null or coalesce(mo.completed_at, mo.created_at) >= :fromTime)
              and (:toTime is null or coalesce(mo.completed_at, mo.created_at) <= :toTime)
              and (
                    (:groupCode = 'ROUTINE' and upper(coalesce(mo.order_type, '')) in ('ROUTINE', 'RE_STAIN', 'RESTAIN', 'DEEP_CUT', 'RECUT', 'SLICE', 'SECTION'))
                 or (:groupCode = 'FROZEN' and upper(coalesce(mo.order_type, '')) = 'FROZEN')
                 or (:groupCode = 'IHC' and upper(coalesce(mo.order_type, '')) in ('IHC', 'IMMUNOHISTOCHEMISTRY'))
                 or (:groupCode = 'CYTOLOGY' and upper(coalesce(mo.order_type, '')) in ('CYTOLOGY', 'CYTOLOGY_CONSULTATION', 'CYTOLOGY_SMEAR', 'LIQUID_CYTOLOGY', 'GYNECOLOGY_LBC_CYTOLOGY', 'NON_GYNECOLOGY_LBC_CYTOLOGY'))
                 or (:groupCode = 'SPECIAL' and upper(coalesce(mo.order_type, '')) in ('SPECIAL_STAIN', 'SPECIAL_STAINING'))
              )
            """, new MapSqlParameterSource()
            .addValue("fromTime", from)
            .addValue("toTime", to)
            .addValue("groupCode", groupCode));
    }

    private List<TemplateCountRow> structuredTemplateCounts(LocalDateTime from, LocalDateTime to) {
        String sql = """
            select st.template_code,
                   st.template_name,
                   count(pr.id) as report_count
            from sampling_templates st
            join pathology_reports pr on pr.report_status = 'PUBLISHED'
             and upper(coalesce(pr.rich_text_content, '')) like concat('%', upper(st.template_code), '%')
            where (:fromTime is null or pr.published_at >= :fromTime)
              and (:toTime is null or pr.published_at <= :toTime)
              and st.enabled = 1
            group by st.template_code, st.template_name
            order by report_count desc, st.template_code
            """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource()
            .addValue("fromTime", from)
            .addValue("toTime", to), (rs, rowNum) -> new TemplateCountRow(
            normalizeEmpty(rs.getString("template_code")),
            normalizeEmpty(rs.getString("template_name")),
            rs.getLong("report_count")));
    }

    private RateSnapshot routineReportTimelinessRate(LocalDateTime from, LocalDateTime to) {
        String sql = """
            select count(*) as eligible_count,
                   sum(case when pr.published_at is not null and (dt.sla_due_at is null or pr.published_at <= dt.sla_due_at) then 1 else 0 end) as pass_count
            from pathology_reports pr
            join pathology_cases pc on pc.id = pr.case_id
            join applications a on a.id = pc.application_id
            left join (
                select case_id, max(sla_due_at) as sla_due_at
                from diagnostic_tasks
                where upper(coalesce(task_type, '')) <> 'FROZEN'
                   or task_type is null
                group by case_id
            ) dt on dt.case_id = pr.case_id
            where pr.report_status = 'PUBLISHED'
              and upper(coalesce(a.application_type, '')) <> 'CYTOLOGY'
              and (:fromTime is null or pr.published_at >= :fromTime)
              and (:toTime is null or pr.published_at <= :toTime)
            """;
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
            .addValue("fromTime", from)
            .addValue("toTime", to), (rs, rowNum) -> {
            long eligibleCount = rs.getLong("eligible_count");
            return new RateSnapshot(
                percentValue(rs.getLong("pass_count"), eligibleCount),
                eligibleCount == 0 ? STATUS_PARTIAL : STATUS_AVAILABLE,
                "按非细胞学已发布常规报告是否在 SLA 内发布统计。");
        });
    }

    private RateSnapshot criticalValueTenMinuteRate(LocalDateTime from, LocalDateTime to) {
        String sql = """
            select count(*) as eligible_count,
                   sum(case when read_at is not null and created_at is not null and read_at <= dateadd('MINUTE', 10, created_at) then 1 else 0 end) as pass_count
            from user_notifications
            where topic_code = 'CRITICAL_VALUE'
              and (:fromTime is null or created_at >= :fromTime)
              and (:toTime is null or created_at <= :toTime)
            """;
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
            .addValue("fromTime", from)
            .addValue("toTime", to), (rs, rowNum) -> {
            long eligibleCount = rs.getLong("eligible_count");
            return new RateSnapshot(
                percentValue(rs.getLong("pass_count"), eligibleCount),
                eligibleCount == 0 ? STATUS_PARTIAL : STATUS_AVAILABLE,
                "大屏专用代理：按危急值通知 10 分钟内已读确认统计及时率。");
        });
    }

    private RateSnapshot cytologyTimelinessRate(LocalDateTime from, LocalDateTime to) {
        String sql = """
            select count(*) as eligible_count,
                   sum(case when pr.published_at is not null and (dt.sla_due_at is null or pr.published_at <= dt.sla_due_at) then 1 else 0 end) as pass_count
            from pathology_reports pr
            join pathology_cases pc on pc.id = pr.case_id
            join applications a on a.id = pc.application_id
            left join (
                select case_id, max(sla_due_at) as sla_due_at
                from diagnostic_tasks
                group by case_id
            ) dt on dt.case_id = pr.case_id
            where pr.report_status = 'PUBLISHED'
              and upper(coalesce(a.application_type, '')) = 'CYTOLOGY'
              and (:fromTime is null or pr.published_at >= :fromTime)
              and (:toTime is null or pr.published_at <= :toTime)
            """;
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
            .addValue("fromTime", from)
            .addValue("toTime", to), (rs, rowNum) -> {
            long eligibleCount = rs.getLong("eligible_count");
            return new RateSnapshot(
                percentValue(rs.getLong("pass_count"), eligibleCount),
                eligibleCount == 0 ? STATUS_PARTIAL : STATUS_AVAILABLE,
                "按 application_type=CYTOLOGY 的已发布报告及时率统计。");
        });
    }

    private RateSnapshot frozenTimelinessRate(LocalDateTime from, LocalDateTime to) {
        return qualityDatasetRate("QC_FROZEN_DIAGNOSIS_TIMELINESS_RATE", from, to);
    }

    private RateSnapshot cytologyQualityRate(LocalDateTime from, LocalDateTime to) {
        String sql = """
            select count(*) as eligible_count,
                   sum(case when upper(coalesce(sl.quality_status, '')) in ('GOOD', 'EXCELLENT', 'PASS', 'QUALIFIED') then 1 else 0 end) as pass_count
            from slide_stainings ss
            join slides sl on sl.id = ss.slide_id
            join pathology_cases pc on pc.id = ss.case_id
            join applications a on a.id = pc.application_id
            where upper(coalesce(a.application_type, '')) = 'CYTOLOGY'
              and (:fromTime is null or coalesce(ss.stained_at, ss.created_at) >= :fromTime)
              and (:toTime is null or coalesce(ss.stained_at, ss.created_at) <= :toTime)
            """;
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
            .addValue("fromTime", from)
            .addValue("toTime", to), (rs, rowNum) -> {
            long eligibleCount = rs.getLong("eligible_count");
            return new RateSnapshot(
                percentValue(rs.getLong("pass_count"), eligibleCount),
                eligibleCount == 0 ? STATUS_PARTIAL : STATUS_AVAILABLE,
                "按细胞学玻片 quality_status 代理统计质控符合率。");
        });
    }

    private String monthOverMonth(long current, long previous) {
        if (previous == 0) {
            return "-";
        }
        BigDecimal ratio = BigDecimal.valueOf(current - previous)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(previous), 2, RoundingMode.HALF_UP);
        return ratio.toPlainString() + "%";
    }

    private String formatPercent(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    @Override
    protected QualityDetailDataset buildQualityDetailDataset(String indicatorCode, StatFilter filter) {
        return qualitySupport.buildQualityDetailDataset(indicatorCode, filter);
    }

    @Override
    protected MetricSnapshot summarizeQualityMetric(String indicatorCode, String aggregationType, QualityDetailDataset dataset) {
        return qualitySupport.summarizeQualityMetric(indicatorCode, aggregationType, dataset);
    }

    private record RateSnapshot(BigDecimal value, String status, String sourceNote) {
    }

    private record TemplateCountRow(String templateCode, String templateName, long reportCount) {
    }
}
