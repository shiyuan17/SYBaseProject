package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class M4RoleAuthorizationIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    @Test
    void shouldRejectCrossRoleActionsAcrossM4Workstations() throws Exception {
        postJson("/api/v1/diagnostic-tasks/DT-X/assign", USER_M4_DIAGNOSIS, """
            {
              "diagnosisDoctorUserId":"USER_M4_DIAGNOSIS",
              "diagnosisDoctorName":"M4 Diagnosis",
              "primaryDoctorUserId":"USER_M4_DIAGNOSIS",
              "primaryDoctorName":"M4 Diagnosis",
              "reviewerUserId":"USER_M4_REVIEW",
              "reviewerName":"M4 Review",
              "operatorName":"diag-user"
            }
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        postJson("/api/v1/pathology-reports/RPT-X/review", USER_M4_DIAGNOSIS, """
            {"operatorName":"diag-user"}
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        postJson("/api/v1/pathology-reports/RPT-X/sign", USER_M4_REVIEW, """
            {"operatorName":"review-user"}
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        mockMvc.perform(authorized(get("/api/v1/pathology-cases/{id}/report-tracking", "CASE-X"), USER_M4_DIAGNOSIS))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void shouldKeepMissingHeaderAndNoPermissionBehaviorForM4ProtectedEndpoints() throws Exception {
        mockMvc.perform(post("/api/v1/pathology-reports")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "caseId":"CASE-X",
                      "taskId":"TASK-X",
                      "clinicalDiagnosis":"c",
                      "grossExam":"g",
                      "microscopicExam":"m",
                      "finalDiagnosis":"f",
                      "richTextContent":"<p>x</p>",
                      "operatorName":"diag-user"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        postJson("/api/v1/pathology-reports", USER_M4_NO_PERMISSION, """
            {
              "caseId":"CASE-X",
              "taskId":"TASK-X",
              "clinicalDiagnosis":"c",
              "grossExam":"g",
              "microscopicExam":"m",
              "finalDiagnosis":"f",
              "richTextContent":"<p>x</p>",
              "operatorName":"diag-user"
            }
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }
}
