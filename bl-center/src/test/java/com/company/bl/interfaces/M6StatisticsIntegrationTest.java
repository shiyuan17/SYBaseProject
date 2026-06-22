package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class M6StatisticsIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    private static final String USER_M1_QUALITY = "USER_M1_QUALITY";
    private static final String ROLE_M4_DIAGNOSIS = "ROLE_M4_DIAGNOSIS";
    private static final String ROLE_M4_MEDICAL_ORDER_EXECUTE = "ROLE_M4_MEDICAL_ORDER_EXECUTE";
    private static final String DEPARTMENT_STAT_OR = "DEPT-M6-STAT-OR";
    private static final String DEPARTMENT_STAT_ICU = "DEPT-M6-STAT-ICU";

    @Test
    void shouldQueryAndExportStatReports() throws Exception {
        preparePublishedReportContext("APP-M6-STAT-001", "BC-M6-STAT-001");

        JsonNode indicators = responseBody(mockMvc.perform(authorized(get("/api/v1/stat-indicators"), USER_M1_QUALITY)
            .param("category", "QUALITY")), 200);
        assertThat(indicators.size()).isGreaterThanOrEqualTo(16);
        assertThat(indicators.toString()).contains("QC_CRITICAL_VALUE_COUNT");

        JsonNode templates = responseBody(mockMvc.perform(authorized(get("/api/v1/stat-report-templates"), USER_M1_QUALITY)
            .param("templateType", "OPERATION")), 200);
        assertThat(templates.toString()).contains("TPL_OPERATION_OVERVIEW");

        JsonNode report = responseBody(mockMvc.perform(authorized(post("/api/v1/stat-reports/query"), USER_M1_QUALITY)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "category":"OPERATION",
                      "from":"2026-01-01T00:00:00",
                      "to":"2026-12-31T23:59:59"
                    }
                    """)), 200);
        assertThat(report.path("rows").size()).isGreaterThanOrEqualTo(4);
        assertThat(report.toString()).contains("OP_BILLING_AMOUNT");

        JsonNode qualityTrendReport = queryStatReport("""
            {
              "indicatorCode":"QC_SPECIMEN_FIXATION_RATE",
              "category":"QUALITY",
              "from":"2026-01-01T00:00:00",
              "to":"2026-12-31T23:59:59",
              "periodMode":"month"
            }
            """);
        JsonNode qualityTrendRow = findRow(qualityTrendReport, "QC_SPECIMEN_FIXATION_RATE");
        assertThat(qualityTrendRow.path("trendPoints").size()).isGreaterThan(0);
        assertThat(qualityTrendRow.path("breakdowns").size()).isGreaterThan(0);
        assertThat(qualityTrendRow.path("sourceNote").asText()).contains("代理口径");

        String csv = mockMvc.perform(authorized(post("/api/v1/stat-reports/export"), USER_M1_QUALITY)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "category":"QUALITY",
                      "from":"2026-01-01T00:00:00",
                      "to":"2026-12-31T23:59:59"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".csv")))
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(csv).contains("indicatorCode,indicatorName,metricValue,metricUnit");
        assertThat(csv).contains("QC_SPECIMEN_FIXATION_RATE");
    }

    @Test
    void shouldQueryCriticalValueQualityIndicatorsAndDetails() throws Exception {
        JsonNode criticalValueCountReport = queryStatReport("""
            {
              "indicatorCode":"QC_CRITICAL_VALUE_COUNT",
              "category":"QUALITY",
              "from":"2026-01-01T00:00:00",
              "to":"2026-12-31T23:59:59",
              "periodMode":"month"
            }
            """);
        JsonNode criticalValueCountRow = findRow(criticalValueCountReport, "QC_CRITICAL_VALUE_COUNT");
        assertThat(criticalValueCountRow.path("metricValue").asText()).isEqualTo("1");
        assertThat(criticalValueCountRow.path("metricStatus").asText()).isEqualTo("PARTIAL");
        assertThat(criticalValueCountRow.path("sourceNote").asText()).contains("topic_code=CRITICAL_VALUE");
        assertThat(criticalValueCountRow.path("trendPoints").size()).isGreaterThan(0);
        assertThat(criticalValueCountRow.path("breakdowns").toString()).contains("危急值待处理");

        JsonNode criticalValueTimelinessReport = queryStatReport("""
            {
              "indicatorCode":"QC_CRITICAL_VALUE_REPORT_TIMELINESS_RATE",
              "category":"QUALITY",
              "from":"2026-01-01T00:00:00",
              "to":"2026-12-31T23:59:59",
              "periodMode":"month"
            }
            """);
        JsonNode timelinessRow = findRow(criticalValueTimelinessReport, "QC_CRITICAL_VALUE_REPORT_TIMELINESS_RATE");
        assertThat(timelinessRow.path("metricUnit").asText()).isEqualTo("PERCENT");
        assertThat(timelinessRow.path("denominator").asText()).isEqualTo("1");

        JsonNode criticalValueReasonDetails = responseBody(mockMvc.perform(authorized(post("/api/v1/stat-reports/details/query"), USER_M1_QUALITY)
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "indicatorCode":"QC_CRITICAL_VALUE_REASON_ANALYSIS_COUNT",
                  "from":"2026-01-01T00:00:00",
                  "to":"2026-12-31T23:59:59",
                  "page":1,
                  "size":10
                }
                """)), 200);
        assertThat(criticalValueReasonDetails.path("availabilityStatus").asText()).isEqualTo("PARTIAL");
        assertThat(criticalValueReasonDetails.path("total").asInt()).isEqualTo(1);
        assertThat(criticalValueReasonDetails.path("items").toString()).contains("NOTIFY_ADMIN_CRITICAL_VALUE");
        assertThat(criticalValueReasonDetails.path("sourceNote").asText()).contains("原因分布");

        String detailCsv = mockMvc.perform(authorized(post("/api/v1/stat-reports/details/export"), USER_M1_QUALITY)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "indicatorCode":"QC_CRITICAL_VALUE_REASON_ANALYSIS_COUNT",
                      "from":"2026-01-01T00:00:00",
                      "to":"2026-12-31T23:59:59"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".csv")))
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(detailCsv).contains("QC_CRITICAL_VALUE_REASON_ANALYSIS_COUNT");
        assertThat(detailCsv).contains("NOTIFY_ADMIN_CRITICAL_VALUE");
    }

    @Test
    void shouldApplyDepartmentAndRoleFiltersToStatReports() throws Exception {
        ensureRoleAssignment(USER_M4_DIAGNOSIS, ROLE_M4_MEDICAL_ORDER_EXECUTE);

        StartedDiagnosticContext deptOrContext =
            prepareStartedDiagnosticCase("APP-M6-STAT-FILTER-001", "BC-M6-STAT-FILTER-001", DEPARTMENT_STAT_OR, "M6 Stat OR");
        StartedDiagnosticContext deptIcuContext =
            prepareStartedDiagnosticCase("APP-M6-STAT-FILTER-002", "BC-M6-STAT-FILTER-002", DEPARTMENT_STAT_ICU, "M6 Stat ICU");

        completeMedicalOrder(deptOrContext.caseId(), "OR workload order", USER_M4_DIAGNOSIS);
        completeMedicalOrder(deptIcuContext.caseId(), "ICU workload order", USER_M4_ORDER_EXECUTE);

        JsonNode caseVolumeReport = queryStatReport("""
            {
              "indicatorCode":"OP_CASE_VOLUME",
              "category":"OPERATION",
              "from":"2026-01-01T00:00:00",
              "to":"2026-12-31T23:59:59",
              "departmentId":"DEPT-M6-STAT-OR"
            }
            """);
        assertThat(findMetricValue(caseVolumeReport, "OP_CASE_VOLUME")).isEqualTo("1");

        JsonNode qualityReport = queryStatReport("""
            {
              "indicatorCode":"QC_GROSSING_QUALITY_COUNT",
              "category":"QUALITY",
              "from":"2026-01-01T00:00:00",
              "to":"2026-12-31T23:59:59",
              "departmentId":"DEPT-M6-STAT-OR",
              "roleId":"ROLE_PATHOLOGY_ADMIN"
            }
            """);
        assertThat(findMetricValue(qualityReport, "QC_GROSSING_QUALITY_COUNT")).isEqualTo("1");

        JsonNode diagnosticWorkloadReport = queryStatReport("""
            {
              "indicatorCode":"WL_DIAGNOSTIC_TASK_COUNT",
              "category":"WORKLOAD",
              "from":"2026-01-01T00:00:00",
              "to":"2026-12-31T23:59:59",
              "periodMode":"quarter",
              "departmentId":"DEPT-M6-STAT-OR",
              "roleId":"ROLE_M4_DIAGNOSIS",
              "workloadUserId":"USER_M4_DIAGNOSIS"
            }
            """);
        assertThat(findMetricValue(diagnosticWorkloadReport, "WL_DIAGNOSTIC_TASK_COUNT")).isEqualTo("1");
        JsonNode diagnosticWorkloadRow = findRow(diagnosticWorkloadReport, "WL_DIAGNOSTIC_TASK_COUNT");
        assertThat(diagnosticWorkloadRow.path("trendPoints").size()).isEqualTo(4);
        assertThat(diagnosticWorkloadRow.path("trendPoints").toString()).contains("2026-Q1");
        assertThat(diagnosticWorkloadRow.path("sourceNote").asText()).contains("quarter");

        JsonNode medicalOrderWorkloadReport = queryStatReport("""
            {
              "indicatorCode":"WL_MEDICAL_ORDER_COUNT",
              "category":"WORKLOAD",
              "from":"2026-01-01T00:00:00",
              "to":"2026-12-31T23:59:59",
              "departmentId":"DEPT-M6-STAT-OR",
              "roleId":"ROLE_M4_DIAGNOSIS",
              "workloadUserId":"USER_M4_DIAGNOSIS"
            }
            """);
        assertThat(findMetricValue(medicalOrderWorkloadReport, "WL_MEDICAL_ORDER_COUNT")).isEqualTo("1");

        JsonNode performanceWorkloadReport = queryStatReport("""
            {
              "indicatorCode":"OP_PERFORMANCE_WORKLOAD",
              "category":"OPERATION",
              "from":"2026-01-01T00:00:00",
              "to":"2026-12-31T23:59:59",
              "departmentId":"DEPT-M6-STAT-OR",
              "roleId":"ROLE_M4_DIAGNOSIS",
              "workloadUserId":"USER_M4_DIAGNOSIS"
            }
            """);
        assertThat(findMetricValue(performanceWorkloadReport, "OP_PERFORMANCE_WORKLOAD")).isEqualTo("2");

        String workloadCsv = exportStatReport("""
            {
              "indicatorCode":"OP_PERFORMANCE_WORKLOAD",
              "category":"OPERATION",
              "from":"2026-01-01T00:00:00",
              "to":"2026-12-31T23:59:59",
              "departmentId":"DEPT-M6-STAT-OR",
              "roleId":"ROLE_M4_DIAGNOSIS",
              "workloadUserId":"USER_M4_DIAGNOSIS"
            }
            """);
        assertThat(workloadCsv).contains("OP_PERFORMANCE_WORKLOAD");
        assertThat(workloadCsv).contains("\"2\"");

        JsonNode reagentAlertUnfiltered = queryStatReport("""
            {
              "indicatorCode":"OP_REAGENT_STOCK_ALERT",
              "category":"OPERATION",
              "from":"2026-01-01T00:00:00",
              "to":"2026-12-31T23:59:59"
            }
            """);
        JsonNode reagentAlertFiltered = queryStatReport("""
            {
              "indicatorCode":"OP_REAGENT_STOCK_ALERT",
              "category":"OPERATION",
              "from":"2026-01-01T00:00:00",
              "to":"2026-12-31T23:59:59",
              "departmentId":"DEPT-M6-STAT-OR",
              "roleId":"ROLE_M4_DIAGNOSIS",
              "workloadUserId":"USER_M4_DIAGNOSIS"
            }
            """);
        assertThat(findMetricValue(reagentAlertFiltered, "OP_REAGENT_STOCK_ALERT"))
            .isEqualTo(findMetricValue(reagentAlertUnfiltered, "OP_REAGENT_STOCK_ALERT"));
    }

    @Test
    void shouldQueryAndExportQualityIndicatorDetails() throws Exception {
        PublishedReportContext publishedContext =
            preparePublishedReportContext("APP-M6-DETAIL-001", "BC-M6-DETAIL-001", DEPARTMENT_STAT_OR, "M6 Stat OR");
        StartedDiagnosticContext pendingContext =
            prepareStartedDiagnosticCase("APP-M6-DETAIL-002", "BC-M6-DETAIL-002", DEPARTMENT_STAT_ICU, "M6 Stat ICU");

        JsonNode consultationCreated = responseBody(postJson("/api/v1/consultations", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "participants":[
                {"participantUserId":"USER_M4_SIGN","participantName":"M4 Sign","participantRole":"EXPERT"}
              ],
              "terminalCode":"M6-CONS-01"
            }
            """.formatted(publishedContext.caseId())), 200);
        String consultationId = consultationCreated.path("consultationId").asText();
        String participantId = namedParameterJdbcTemplate.queryForObject("""
            select id
            from consultation_participants
            where consultation_id = :consultationId
            fetch first 1 row only
            """, new MapSqlParameterSource()
            .addValue("consultationId", consultationId), String.class);
        postJson("/api/v1/consultations/%s/participants/%s/comment".formatted(consultationId, participantId), USER_M4_SIGN, """
            {
              "opinion":"detail opinion",
              "terminalCode":"M6-CONS-02"
            }
            """).andExpect(status().isOk());
        postJson("/api/v1/consultations/%s/complete".formatted(consultationId), USER_M4_DIAGNOSIS, """
            {
              "opinion":"detail completed",
              "terminalCode":"M6-CONS-03"
            }
            """).andExpect(status().isOk());

        JsonNode revision = responseBody(postJson("/api/v1/report-revision-requests", USER_M4_DIAGNOSIS, """
            {
              "reportId":"%s",
              "requestReason":"detail revision request",
              "terminalCode":"M6-REV-01"
            }
            """.formatted(publishedContext.reportId())), 200);
        String revisionRequestId = revision.path("requestId").asText();
        assertThat(revisionRequestId).isNotBlank();

        JsonNode rateDetails = responseBody(mockMvc.perform(authorized(post("/api/v1/stat-reports/details/query"), USER_M1_QUALITY)
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "indicatorCode":"QC_SPECIMEN_FIXATION_RATE",
                  "from":"2026-01-01T00:00:00",
                  "to":"2026-12-31T23:59:59",
                  "departmentId":"DEPT-M6-STAT-OR",
                  "page":1,
                  "size":10
                }
                """)), 200);
        assertThat(rateDetails.path("indicatorCode").asText()).isEqualTo("QC_SPECIMEN_FIXATION_RATE");
        assertThat(rateDetails.path("availabilityStatus").asText()).isEqualTo("AVAILABLE");
        assertThat(rateDetails.path("eligibleCount").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(rateDetails.path("passCount").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(rateDetails.path("items").toString()).contains("APP-M6-DETAIL-001");

        JsonNode countDetails = responseBody(mockMvc.perform(authorized(post("/api/v1/stat-reports/details/query"), USER_M1_QUALITY)
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "indicatorCode":"QC_CANCELLED_REVIEW_COUNT",
                  "from":"2026-01-01T00:00:00",
                  "to":"2026-12-31T23:59:59",
                  "departmentId":"DEPT-M6-STAT-OR",
                  "page":1,
                  "size":10
                }
                """)), 200);
        assertThat(countDetails.path("indicatorCode").asText()).isEqualTo("QC_CANCELLED_REVIEW_COUNT");
        assertThat(countDetails.path("items").size()).isGreaterThanOrEqualTo(1);
        assertThat(countDetails.path("items").get(0).path("detailStatus").asText()).isEqualTo("INFO");

        JsonNode avgDetails = responseBody(mockMvc.perform(authorized(post("/api/v1/stat-reports/details/query"), USER_M1_QUALITY)
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "indicatorCode":"QC_REPORT_RELEASE_DAYS",
                  "from":"2026-01-01T00:00:00",
                  "to":"2026-12-31T23:59:59",
                  "departmentId":"DEPT-M6-STAT-OR",
                  "page":1,
                  "size":10
                }
                """)), 200);
        assertThat(avgDetails.path("indicatorCode").asText()).isEqualTo("QC_REPORT_RELEASE_DAYS");
        assertThat(avgDetails.path("items").size()).isGreaterThanOrEqualTo(1);
        assertThat(avgDetails.path("items").get(0).path("reason").asText()).contains("天");

        JsonNode paginationDetails = responseBody(mockMvc.perform(authorized(post("/api/v1/stat-reports/details/query"), USER_M1_QUALITY)
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "indicatorCode":"QC_SPECIMEN_FIXATION_RATE",
                  "from":"2026-01-01T00:00:00",
                  "to":"2026-12-31T23:59:59",
                  "page":1,
                  "size":1
                }
                """)), 200);
        assertThat(paginationDetails.path("items").size()).isEqualTo(1);
        assertThat(paginationDetails.path("total").asInt()).isGreaterThanOrEqualTo(2);

        JsonNode emptyDepartmentDetails = responseBody(mockMvc.perform(authorized(post("/api/v1/stat-reports/details/query"), USER_M1_QUALITY)
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "indicatorCode":"QC_SPECIMEN_FIXATION_RATE",
                  "from":"2026-01-01T00:00:00",
                  "to":"2026-12-31T23:59:59",
                  "departmentId":"DEPT-M6-NOT-EXIST",
                  "page":1,
                  "size":10
                }
                """)), 200);
        assertThat(emptyDepartmentDetails.path("items").size()).isEqualTo(0);
        assertThat(emptyDepartmentDetails.path("total").asInt()).isEqualTo(0);

        String detailCsv = mockMvc.perform(authorized(post("/api/v1/stat-reports/details/export"), USER_M1_QUALITY)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "indicatorCode":"QC_SPECIMEN_FIXATION_RATE",
                      "from":"2026-01-01T00:00:00",
                      "to":"2026-12-31T23:59:59",
                      "page":1,
                      "size":10
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".csv")))
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(detailCsv).contains("indicatorCode,availabilityStatus,eligibleCount,passCount,failCount");
        assertThat(detailCsv).contains("QC_SPECIMEN_FIXATION_RATE");

        JsonNode pendingRateDetails = responseBody(mockMvc.perform(authorized(post("/api/v1/stat-reports/details/query"), USER_M1_QUALITY)
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "indicatorCode":"QC_DIAGNOSIS_TIMELINESS_RATE",
                  "from":"2026-01-01T00:00:00",
                  "to":"2026-12-31T23:59:59",
                  "departmentId":"DEPT-M6-STAT-ICU",
                  "page":1,
                  "size":10
                }
                """)), 200);
        assertThat(pendingRateDetails.path("failCount").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(pendingRateDetails.path("items").toString()).contains("APP-M6-DETAIL-002");
    }

    @Test
    void shouldQueryGoal6FrozenTimeoutReportsAndDetails() throws Exception {
        PublishedReportContext context =
            preparePublishedReportContext("APP-M6-FROZEN-001", "BC-M6-FROZEN-001", DEPARTMENT_STAT_OR, "M6 Stat OR");
        markCaseAsFrozenWithTimeouts(context);

        JsonNode timelinessReport = queryStatReport("""
            {
              "indicatorCode":"QC_FROZEN_DIAGNOSIS_TIMELINESS_RATE",
              "category":"QUALITY",
              "from":"2026-01-01T00:00:00",
              "to":"2026-12-31T23:59:59",
              "periodMode":"month"
            }
            """);
        JsonNode timelinessRow = findRow(timelinessReport, "QC_FROZEN_DIAGNOSIS_TIMELINESS_RATE");
        assertThat(timelinessRow.path("metricUnit").asText()).isEqualTo("PERCENT");
        assertThat(timelinessRow.path("sourceNote").asText()).contains("默认 SLA 30 分钟");
        assertThat(timelinessRow.path("trendPoints").size()).isGreaterThan(0);

        JsonNode timeoutReport = queryStatReport("""
            {
              "indicatorCode":"QC_FROZEN_TIMEOUT_COUNT",
              "category":"QUALITY",
              "from":"2026-01-01T00:00:00",
              "to":"2026-12-31T23:59:59",
              "periodMode":"month"
            }
            """);
        JsonNode timeoutRow = findRow(timeoutReport, "QC_FROZEN_TIMEOUT_COUNT");
        assertThat(timeoutRow.path("metricValue").asInt()).isGreaterThanOrEqualTo(3);
        assertThat(timeoutRow.path("breakdowns").toString()).contains("取材耗时");
        assertThat(timeoutRow.path("breakdowns").toString()).contains("切片耗时");
        assertThat(timeoutRow.path("breakdowns").toString()).contains("诊断耗时");

        JsonNode grossingDetails = queryDetails("QC_FROZEN_GROSSING_TIMEOUT_COUNT");
        assertThat(grossingDetails.path("items").toString()).contains("取材耗时");
        assertThat(grossingDetails.path("items").toString()).contains(context.pathologyNo());

        JsonNode slicingDetails = queryDetails("QC_FROZEN_SLICING_TIMEOUT_COUNT");
        assertThat(slicingDetails.path("items").toString()).contains("切片耗时");

        JsonNode diagnosisDetails = queryDetails("QC_FROZEN_DIAGNOSIS_TIMEOUT_COUNT");
        assertThat(diagnosisDetails.path("items").toString()).contains("诊断耗时");

        String timeoutCsv = exportDetails("QC_FROZEN_DIAGNOSIS_TIMEOUT_COUNT");
        assertThat(timeoutCsv).contains("QC_FROZEN_DIAGNOSIS_TIMEOUT_COUNT");
        assertThat(timeoutCsv).contains("诊断耗时");
    }

    @Test
    void shouldQueryGoal7ReportChangeReportsAndDetails() throws Exception {
        PublishedReportContext context =
            preparePublishedReportContext("APP-M6-CHANGE-001", "BC-M6-CHANGE-001", DEPARTMENT_STAT_OR, "M6 Stat OR");
        addReportChangeFixtures(context);

        JsonNode changeReport = queryStatReport("""
            {
              "indicatorCode":"QC_REPORT_CHANGE_COUNT",
              "category":"QUALITY",
              "from":"2026-01-01T00:00:00",
              "to":"2026-12-31T23:59:59",
              "periodMode":"month"
            }
            """);
        JsonNode changeRow = findRow(changeReport, "QC_REPORT_CHANGE_COUNT");
        assertThat(changeRow.path("metricValue").asInt()).isGreaterThanOrEqualTo(2);
        assertThat(changeRow.path("trendPoints").size()).isGreaterThan(0);
        assertThat(changeRow.path("sourceNote").asText()).contains("report_versions");

        JsonNode doctorReport = queryStatReport("""
            {
              "indicatorCode":"QC_REPORT_CHANGE_DOCTOR_COUNT",
              "category":"QUALITY",
              "from":"2026-01-01T00:00:00",
              "to":"2026-12-31T23:59:59"
            }
            """);
        assertThat(findRow(doctorReport, "QC_REPORT_CHANGE_DOCTOR_COUNT").path("metricValue").asInt())
            .isGreaterThanOrEqualTo(1);

        JsonNode modificationDetails = queryDetails("QC_REPORT_MODIFICATION_REASON_COUNT");
        assertThat(modificationDetails.path("items").toString()).contains("版本状态");
        assertThat(modificationDetails.path("items").toString()).contains(context.reportId());

        JsonNode revisionDetails = queryDetails("QC_REPORT_REVISION_REASON_COUNT");
        assertThat(revisionDetails.path("items").toString()).contains("Goal7 revision reason");
        assertThat(revisionDetails.path("items").toString()).contains(context.pathologyNo());
    }

    @Test
    void shouldQueryGoal8UnqualifiedSpecimenReportsAndDetails() throws Exception {
        PublishedReportContext context =
            preparePublishedReportContext("APP-M6-UQ-001", "BC-M6-UQ-001", DEPARTMENT_STAT_OR, "M6 Stat OR");
        markSpecimenUnqualified(context, "固定不足");

        JsonNode countReport = queryStatReport("""
            {
              "indicatorCode":"QC_UNQUALIFIED_SPECIMEN_COUNT",
              "category":"QUALITY",
              "from":"2026-01-01T00:00:00",
              "to":"2026-12-31T23:59:59",
              "periodMode":"month"
            }
            """);
        JsonNode countRow = findRow(countReport, "QC_UNQUALIFIED_SPECIMEN_COUNT");
        assertThat(countRow.path("metricValue").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(countRow.path("breakdowns").toString()).contains("固定不足");

        JsonNode rateReport = queryStatReport("""
            {
              "indicatorCode":"QC_UNQUALIFIED_SPECIMEN_RATE",
              "category":"QUALITY",
              "from":"2026-01-01T00:00:00",
              "to":"2026-12-31T23:59:59",
              "periodMode":"month"
            }
            """);
        JsonNode rateRow = findRow(rateReport, "QC_UNQUALIFIED_SPECIMEN_RATE");
        assertThat(rateRow.path("metricUnit").asText()).isEqualTo("PERCENT");
        assertThat(rateRow.path("sourceNote").asText()).contains("真实字段");

        JsonNode reasonDetails = queryDetails("QC_UNQUALIFIED_SPECIMEN_REASON_COUNT");
        assertThat(reasonDetails.path("items").toString()).contains("固定不足");
        assertThat(reasonDetails.path("items").toString()).contains(context.pathologyNo());

        String reasonCsv = exportDetails("QC_UNQUALIFIED_SPECIMEN_REASON_COUNT");
        assertThat(reasonCsv).contains("QC_UNQUALIFIED_SPECIMEN_REASON_COUNT");
        assertThat(reasonCsv).contains("固定不足");
    }

    private JsonNode queryStatReport(String payload) throws Exception {
        return responseBody(mockMvc.perform(authorized(post("/api/v1/stat-reports/query"), USER_M1_QUALITY)
            .contentType(APPLICATION_JSON)
            .content(payload)), 200);
    }

    private JsonNode queryDetails(String indicatorCode) throws Exception {
        return responseBody(mockMvc.perform(authorized(post("/api/v1/stat-reports/details/query"), USER_M1_QUALITY)
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "indicatorCode":"%s",
                  "from":"2026-01-01T00:00:00",
                  "to":"2026-12-31T23:59:59",
                  "page":1,
                  "size":20
                }
                """.formatted(indicatorCode))), 200);
    }

    private String exportDetails(String indicatorCode) throws Exception {
        return mockMvc.perform(authorized(post("/api/v1/stat-reports/details/export"), USER_M1_QUALITY)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "indicatorCode":"%s",
                      "from":"2026-01-01T00:00:00",
                      "to":"2026-12-31T23:59:59",
                      "page":1,
                      "size":20
                    }
                    """.formatted(indicatorCode)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".csv")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    }

    private String exportStatReport(String payload) throws Exception {
        return mockMvc.perform(authorized(post("/api/v1/stat-reports/export"), USER_M1_QUALITY)
                .contentType(APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".csv")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    }

    private String findMetricValue(JsonNode report, String indicatorCode) {
        return findRow(report, indicatorCode).path("metricValue").asText();
    }

    private JsonNode findRow(JsonNode report, String indicatorCode) {
        for (JsonNode row : report.path("rows")) {
            if (indicatorCode.equals(row.path("indicatorCode").asText())) {
                return row;
            }
        }
        throw new AssertionError("Metric not found: " + indicatorCode + " in " + report);
    }

    private void completeMedicalOrder(String caseId, String orderContent, String executorUserId) throws Exception {
        JsonNode created = responseBody(postJson("/api/v1/medical-orders", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "orderType":"RE_STAIN",
              "orderContent":"%s",
              "terminalCode":"M6-S-01"
            }
            """.formatted(caseId, orderContent)), 200);
        String orderId = created.path("orderId").asText();

        postJson("/api/v1/medical-orders/%s/accept".formatted(orderId), executorUserId, """
            {"terminalCode":"M6-S-02"}
            """).andExpect(status().isOk());
        postJson("/api/v1/medical-orders/%s/print-slide".formatted(orderId), executorUserId, """
            {"terminalCode":"M6-S-02A"}
            """).andExpect(status().isOk());
        postJson("/api/v1/medical-orders/%s/complete".formatted(orderId), executorUserId, """
            {"terminalCode":"M6-S-03"}
            """).andExpect(status().isOk());
    }

    private void markCaseAsFrozenWithTimeouts(PublishedReportContext context) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("caseId", context.caseId())
            .addValue("taskId", context.diagnosticTaskId());

        namedParameterJdbcTemplate.update("""
            update pathology_cases
            set received_at = timestamp '2026-06-01 08:00:00',
                created_at = timestamp '2026-06-01 08:00:00',
                updated_at = current_timestamp
            where id = :caseId
            """, params);
        namedParameterJdbcTemplate.update("""
            update samplings
            set sampled_at = timestamp '2026-06-01 08:45:00',
                created_at = timestamp '2026-06-01 08:45:00',
                sampling_status = 'COMPLETED',
                updated_at = current_timestamp
            where case_id = :caseId
            """, params);
        namedParameterJdbcTemplate.update("""
            update slicings
            set sliced_at = timestamp '2026-06-01 09:30:00',
                created_at = timestamp '2026-06-01 09:30:00',
                slicing_status = 'COMPLETED',
                updated_at = current_timestamp
            where case_id = :caseId
            """, params);
        namedParameterJdbcTemplate.update("""
            update diagnostic_tasks
            set task_type = 'FROZEN',
                frozen_diagnosis_result = 'frozen result',
                primary_diagnosed_at = timestamp '2026-06-01 10:15:00',
                completed_at = timestamp '2026-06-01 10:15:00',
                created_at = timestamp '2026-06-01 10:15:00',
                updated_at = current_timestamp
            where id = :taskId
            """, params);
    }

    private void addReportChangeFixtures(PublishedReportContext context) {
        namedParameterJdbcTemplate.update("""
            insert into report_versions
                (id, report_id, case_id, report_scope, report_seq, version_no, version_status,
                 final_diagnosis_snapshot, content_snapshot, signed_by_user_id, signed_by_name, signed_at, created_at)
            values
                (:versionId, :reportId, :caseId, 'ROUTINE', 1, 2, 'SIGNED',
                 'Goal7 changed diagnosis', 'Goal7 changed content', :doctorUserId, :doctorName,
                 timestamp '2026-06-01 10:00:00', timestamp '2026-06-01 10:00:00')
            """, new MapSqlParameterSource()
            .addValue("versionId", "RV-GOAL7-" + UUID.randomUUID())
            .addValue("reportId", context.reportId())
            .addValue("caseId", context.caseId())
            .addValue("doctorUserId", USER_M4_DIAGNOSIS)
            .addValue("doctorName", "Goal7 Doctor"));
        namedParameterJdbcTemplate.update("""
            insert into report_revision_requests
                (id, case_id, report_id, current_version_no, request_status, request_reason,
                 requested_by_user_id, requested_by_name, requested_at, created_at, updated_at)
            values
                (:requestId, :caseId, :reportId, 2, 'PENDING', 'Goal7 revision reason',
                 :doctorUserId, :doctorName, timestamp '2026-06-01 11:00:00',
                 timestamp '2026-06-01 11:00:00', current_timestamp)
            """, new MapSqlParameterSource()
            .addValue("requestId", "RR-GOAL7-" + UUID.randomUUID())
            .addValue("caseId", context.caseId())
            .addValue("reportId", context.reportId())
            .addValue("doctorUserId", USER_M4_DIAGNOSIS)
            .addValue("doctorName", "Goal7 Doctor"));
    }

    private void markSpecimenUnqualified(PublishedReportContext context, String reason) {
        namedParameterJdbcTemplate.update("""
            update specimens
            set qualified_flag = 0,
                unqualified_reason = :reason,
                fixation_status = 'ABNORMAL',
                registered_at = timestamp '2026-06-01 12:00:00',
                created_at = timestamp '2026-06-01 12:00:00',
                updated_at = current_timestamp
            where case_id = :caseId
            """, new MapSqlParameterSource()
            .addValue("caseId", context.caseId())
            .addValue("reason", reason));
        namedParameterJdbcTemplate.update("""
            update specimen_fixation_records
            set fixation_status = 'ABNORMAL',
                verified_at = timestamp '2026-06-01 12:10:00',
                remarks = :reason
            where specimen_id in (
                select id
                from specimens
                where case_id = :caseId
            )
            """, new MapSqlParameterSource()
            .addValue("caseId", context.caseId())
            .addValue("reason", reason));
    }

    private void ensureRoleAssignment(String userId, String roleId) {
        Integer existing = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from user_roles
            where user_id = :userId and role_id = :roleId
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("roleId", roleId), Integer.class);
        if (existing != null && existing > 0) {
            return;
        }
        namedParameterJdbcTemplate.update("""
            insert into user_roles
                (id, user_id, role_id, is_primary, assigned_at, assigned_by_name)
            values
                (:id, :userId, :roleId, 0, current_timestamp, 'test')
            """, new MapSqlParameterSource()
            .addValue("id", "UR-TEST-" + UUID.randomUUID())
            .addValue("userId", userId)
            .addValue("roleId", roleId));
    }
}
