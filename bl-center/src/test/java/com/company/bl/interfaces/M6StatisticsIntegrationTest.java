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
        assertThat(indicators.size()).isGreaterThanOrEqualTo(13);

        JsonNode templates = responseBody(mockMvc.perform(authorized(get("/api/v1/stat-report-templates"), USER_M1_QUALITY)
            .param("templateType", "OPERATION")), 200);
        assertThat(templates.toString()).contains("TPL_OPERATION_OVERVIEW");

        JsonNode report = responseBody(mockMvc.perform(authorized(post("/api/v1/stat-reports/query"), USER_M1_QUALITY)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "category":"OPERATION",
                      "from":"2026-05-01T00:00:00",
                      "to":"2026-05-31T23:59:59",
                      "operatorUserId":"USER_M1_QUALITY",
                      "operatorName":"quality-user"
                    }
                    """)), 200);
        assertThat(report.path("rows").size()).isGreaterThanOrEqualTo(4);
        assertThat(report.toString()).contains("OP_BILLING_AMOUNT");

        String csv = mockMvc.perform(authorized(post("/api/v1/stat-reports/export"), USER_M1_QUALITY)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "category":"QUALITY",
                      "from":"2026-05-01T00:00:00",
                      "to":"2026-05-31T23:59:59",
                      "operatorUserId":"USER_M1_QUALITY",
                      "operatorName":"quality-user"
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
              "from":"2026-05-01T00:00:00",
              "to":"2026-05-31T23:59:59",
              "departmentId":"DEPT-M6-STAT-OR",
              "operatorUserId":"USER_M1_QUALITY",
              "operatorName":"quality-user"
            }
            """);
        assertThat(findMetricValue(caseVolumeReport, "OP_CASE_VOLUME")).isEqualTo("1");

        JsonNode qualityReport = queryStatReport("""
            {
              "indicatorCode":"QC_GROSSING_QUALITY_COUNT",
              "category":"QUALITY",
              "from":"2026-05-01T00:00:00",
              "to":"2026-05-31T23:59:59",
              "departmentId":"DEPT-M6-STAT-OR",
              "roleId":"ROLE_PATHOLOGY_ADMIN",
              "operatorUserId":"USER_M1_ADMIN",
              "operatorName":"quality-user"
            }
            """);
        assertThat(findMetricValue(qualityReport, "QC_GROSSING_QUALITY_COUNT")).isEqualTo("1");

        JsonNode diagnosticWorkloadReport = queryStatReport("""
            {
              "indicatorCode":"WL_DIAGNOSTIC_TASK_COUNT",
              "category":"WORKLOAD",
              "from":"2026-05-01T00:00:00",
              "to":"2026-05-31T23:59:59",
              "departmentId":"DEPT-M6-STAT-OR",
              "roleId":"ROLE_M4_DIAGNOSIS",
              "operatorUserId":"USER_M4_DIAGNOSIS",
              "operatorName":"quality-user"
            }
            """);
        assertThat(findMetricValue(diagnosticWorkloadReport, "WL_DIAGNOSTIC_TASK_COUNT")).isEqualTo("1");

        JsonNode medicalOrderWorkloadReport = queryStatReport("""
            {
              "indicatorCode":"WL_MEDICAL_ORDER_COUNT",
              "category":"WORKLOAD",
              "from":"2026-05-01T00:00:00",
              "to":"2026-05-31T23:59:59",
              "departmentId":"DEPT-M6-STAT-OR",
              "roleId":"ROLE_M4_DIAGNOSIS",
              "operatorUserId":"USER_M4_DIAGNOSIS",
              "operatorName":"quality-user"
            }
            """);
        assertThat(findMetricValue(medicalOrderWorkloadReport, "WL_MEDICAL_ORDER_COUNT")).isEqualTo("1");

        JsonNode performanceWorkloadReport = queryStatReport("""
            {
              "indicatorCode":"OP_PERFORMANCE_WORKLOAD",
              "category":"OPERATION",
              "from":"2026-05-01T00:00:00",
              "to":"2026-05-31T23:59:59",
              "departmentId":"DEPT-M6-STAT-OR",
              "roleId":"ROLE_M4_DIAGNOSIS",
              "operatorUserId":"USER_M4_DIAGNOSIS",
              "operatorName":"quality-user"
            }
            """);
        assertThat(findMetricValue(performanceWorkloadReport, "OP_PERFORMANCE_WORKLOAD")).isEqualTo("2");

        String workloadCsv = exportStatReport("""
            {
              "indicatorCode":"OP_PERFORMANCE_WORKLOAD",
              "category":"OPERATION",
              "from":"2026-05-01T00:00:00",
              "to":"2026-05-31T23:59:59",
              "departmentId":"DEPT-M6-STAT-OR",
              "roleId":"ROLE_M4_DIAGNOSIS",
              "operatorUserId":"USER_M4_DIAGNOSIS",
              "operatorName":"quality-user"
            }
            """);
        assertThat(workloadCsv).contains("OP_PERFORMANCE_WORKLOAD");
        assertThat(workloadCsv).contains("\"2\"");

        JsonNode reagentAlertUnfiltered = queryStatReport("""
            {
              "indicatorCode":"OP_REAGENT_STOCK_ALERT",
              "category":"OPERATION",
              "from":"2026-05-01T00:00:00",
              "to":"2026-05-31T23:59:59",
              "operatorUserId":"USER_M1_QUALITY",
              "operatorName":"quality-user"
            }
            """);
        JsonNode reagentAlertFiltered = queryStatReport("""
            {
              "indicatorCode":"OP_REAGENT_STOCK_ALERT",
              "category":"OPERATION",
              "from":"2026-05-01T00:00:00",
              "to":"2026-05-31T23:59:59",
              "departmentId":"DEPT-M6-STAT-OR",
              "roleId":"ROLE_M4_DIAGNOSIS",
              "operatorUserId":"USER_M4_DIAGNOSIS",
              "operatorName":"quality-user"
            }
            """);
        assertThat(findMetricValue(reagentAlertFiltered, "OP_REAGENT_STOCK_ALERT"))
            .isEqualTo(findMetricValue(reagentAlertUnfiltered, "OP_REAGENT_STOCK_ALERT"));
    }

    private JsonNode queryStatReport(String payload) throws Exception {
        return responseBody(mockMvc.perform(authorized(post("/api/v1/stat-reports/query"), USER_M1_QUALITY)
            .contentType(APPLICATION_JSON)
            .content(payload)), 200);
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
        for (JsonNode row : report.path("rows")) {
            if (indicatorCode.equals(row.path("indicatorCode").asText())) {
                return row.path("metricValue").asText();
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
        postJson("/api/v1/medical-orders/%s/complete".formatted(orderId), executorUserId, """
            {"terminalCode":"M6-S-03"}
            """).andExpect(status().isOk());
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
