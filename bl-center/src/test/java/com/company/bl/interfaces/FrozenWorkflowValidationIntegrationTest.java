package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class FrozenWorkflowValidationIntegrationTest extends AbstractFrozenWorkflowIntegrationTest {

    @Test
    void shouldRejectFrozenReceiveWhenSessionAlreadyReceived() throws Exception {
        FrozenCaseContext received = prepareFrozenReceivedCase("APP-FR-RCV-002", "BC-FR-RCV-002");

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/receive/complete".formatted(received.caseId()),
            USER_RECEIVE,
            """
                {
                  "terminalCode": "T-FR-REC-02",
                  "remarks": "repeat frozen receive"
                }
                """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("Frozen receive can only be completed from requested status")));
    }

    @Test
    void shouldRejectFrozenPhoneBackBeforeSavingPreliminaryResult() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-002", "BC-FR-RPT-002");

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-06",
                  "remarks": "invalid frozen phone back",
                  "preliminaryResult": "直接电话回报"
                }
                """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("Frozen preliminary result must be saved before phone back")));
    }

    @Test
    void shouldRejectFrozenPhoneBackAfterAlreadyCompleted() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-016", "BC-FR-RPT-016");

        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-42","remarks":"save preliminary before repeat phone back test","preliminaryResult":"疑似低级别乳头状病变"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-43","remarks":"first phone back","preliminaryResult":"疑似低级别乳头状病变"}
            """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-44",
                  "remarks": "repeat phone back",
                  "preliminaryResult": "重复电话回报"
                }
                """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("Frozen phone back is already completed")));
    }

    @Test
    void shouldRejectFrozenPreliminarySaveAfterPhoneBackCompleted() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-017", "BC-FR-RPT-017");

        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-45","remarks":"save preliminary before immutable-after-phone-back test","preliminaryResult":"疑似导管上皮增生"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-46","remarks":"complete phone back before immutable-after-phone-back test","preliminaryResult":"疑似导管上皮增生"}
            """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-47",
                  "remarks": "repeat save after phone back",
                  "preliminaryResult": "术后补改结果"
                }
                """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("Frozen preliminary report cannot be saved after phone back is completed")));
    }

    @Test
    void shouldRejectFrozenPhoneBackAfterCompletionByUnassignedDiagnosisDoctor() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-018", "BC-FR-RPT-018");
        String otherDiagnosisUserId = createDiagnosisUser("FRPHONEBACK");

        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-48","remarks":"save preliminary before outsider repeated phone back test","preliminaryResult":"疑似滤泡性病变"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-49","remarks":"complete phone back before outsider repeated phone back test","preliminaryResult":"疑似滤泡性病变"}
            """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()),
            otherDiagnosisUserId,
            """
                {
                  "terminalCode": "T-FR-RPT-50",
                  "remarks": "outsider repeated phone back",
                  "preliminaryResult": "越权电话回报"
                }
                """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message", containsString("用户未被分配到诊断任务")));
    }

    @Test
    void shouldRejectFrozenPreliminarySaveAfterPhoneBackCompletedByUnassignedDiagnosisDoctor() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-019", "BC-FR-RPT-019");
        String otherDiagnosisUserId = createDiagnosisUser("FRPRESAVE");

        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-51","remarks":"save preliminary before outsider repeated preliminary save test","preliminaryResult":"疑似乳头状增生"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-52","remarks":"complete phone back before outsider repeated preliminary save test","preliminaryResult":"疑似乳头状增生"}
            """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            otherDiagnosisUserId,
            """
                {
                  "terminalCode": "T-FR-RPT-53",
                  "remarks": "outsider repeated preliminary save",
                  "preliminaryResult": "越权修改结果"
                }
                """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message", containsString("用户未被分配到诊断任务")));
    }

    @Test
    void shouldRejectFrozenPreliminarySaveBeforeDiagnosticTaskAssigned() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenDiagnosingCase("APP-FR-RPT-006", "BC-FR-RPT-006");

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-22",
                  "remarks": "save preliminary before assignment",
                  "preliminaryResult": "疑似纤维瘤"
                }
                """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message", containsString("用户未被分配到诊断任务")));
    }

    @Test
    void shouldRejectFrozenPreliminarySaveBeforeDiagnosticTaskStarted() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenDiagnosingCase("APP-FR-RPT-008", "BC-FR-RPT-008");

        JsonNode pendingDiagnosticTasks = listPendingDiagnosticTasks(diagnosing.pathologyNo(), USER_M4_ASSIGN);
        String diagnosticTaskId = pendingDiagnosticTasks.path("items").get(0).path("id").asText();

        postJson("/api/v1/diagnostic-tasks/%s/assign".formatted(diagnosticTaskId), USER_M4_ASSIGN, """
            {
              "diagnosisDoctorUserId":"USER_M4_DIAGNOSIS",
              "diagnosisDoctorName":"M4 Diagnosis",
              "primaryDoctorUserId":"USER_M4_DIAGNOSIS",
              "primaryDoctorName":"M4 Diagnosis",
              "reviewerUserId":"USER_M4_REVIEW",
              "reviewerName":"M4 Review",
              "terminalCode":"T-FR-DIAG-04"
            }
            """).andExpect(status().isOk());

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-26",
                  "remarks": "save preliminary before diagnostic task started",
                  "preliminaryResult": "疑似黏液性病变"
                }
                """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("Frozen diagnostic task must be started before report actions")));
    }

    @Test
    void shouldRejectFrozenReportConfirmationAfterAlreadyConfirmed() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-003", "BC-FR-RPT-003");

        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-07","remarks":"save preliminary before repeat confirm test","preliminaryResult":"疑似腺瘤"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-08","remarks":"phone back before repeat confirm test","preliminaryResult":"疑似腺瘤"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-09","remarks":"first confirm"}
            """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            USER_M4_DIAGNOSIS,
            """
                {
                  "terminalCode": "T-FR-RPT-10",
                  "remarks": "repeat confirm"
                }
                """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("Frozen report is already confirmed")));
    }

    @Test
    void shouldRejectFrozenReportConfirmationByUnassignedDiagnosisDoctor() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-007", "BC-FR-RPT-007");
        String otherDiagnosisUserId = createDiagnosisUser("FRCONFIRM");

        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-23","remarks":"save preliminary before outsider confirm test","preliminaryResult":"疑似腺性病变"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-24","remarks":"phone back before outsider confirm test","preliminaryResult":"疑似腺性病变"}
            """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            otherDiagnosisUserId,
            """
                {
                  "terminalCode": "T-FR-RPT-25",
                  "remarks": "outsider confirm"
                }
                """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message", containsString("用户未被分配到诊断任务")));
    }

    @Test
    void shouldRejectFrozenReportConfirmationAfterAlreadyConfirmedByUnassignedDiagnosisDoctor() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-020", "BC-FR-RPT-020");
        String otherDiagnosisUserId = createDiagnosisUser("FRCONFIRMREPEAT");

        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-27","remarks":"save preliminary before outsider repeat confirm test","preliminaryResult":"疑似乳头状瘤"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-28","remarks":"phone back before outsider repeat confirm test","preliminaryResult":"疑似乳头状瘤"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-29","remarks":"first confirm before outsider repeat confirm test"}
            """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()),
            otherDiagnosisUserId,
            """
                {
                  "terminalCode": "T-FR-RPT-30",
                  "remarks": "outsider repeat confirm"
                }
                """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message", containsString("用户未被分配到诊断任务")));
    }

    @Test
    void shouldRejectFrozenRemainingTissueHandlingAfterSessionClosed() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-004", "BC-FR-RPT-004");

        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-11","remarks":"save preliminary before close repeat test","preliminaryResult":"疑似乳头状病变"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-12","remarks":"phone back before close repeat test","preliminaryResult":"疑似乳头状病变"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-13","remarks":"confirm before close repeat test"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(diagnosing.caseId()), USER_M4_REVIEW, """
            {"terminalCode":"T-FR-RPT-14","remarks":"compare before close repeat test","compareStatus":"SIGNED_OFF","compareSummary":"冰石一致"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/remaining-tissue/complete".formatted(diagnosing.caseId()), USER_M3_GROSSING, """
            {"terminalCode":"T-FR-RPT-15","remarks":"first close","remainingTissueStatus":"DISPOSED"}
            """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/remaining-tissue/complete".formatted(diagnosing.caseId()),
            USER_M3_GROSSING,
            """
                {
                  "terminalCode": "T-FR-RPT-16",
                  "remarks": "repeat close",
                  "remainingTissueStatus": "DISPOSED"
                }
                """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("Frozen session is already closed")));
    }

    @Test
    void shouldRejectFrozenRemainingTissueHandlingByUserWithoutGrossingPermission() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-022", "BC-FR-RPT-022");

        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-52","remarks":"save preliminary before forbidden remaining tissue test","preliminaryResult":"疑似导管内乳头状病变"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-53","remarks":"phone back before forbidden remaining tissue test","preliminaryResult":"疑似导管内乳头状病变"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-54","remarks":"confirm before forbidden remaining tissue test"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(diagnosing.caseId()), USER_M4_REVIEW, """
            {"terminalCode":"T-FR-RPT-55","remarks":"compare before forbidden remaining tissue test","compareStatus":"SIGNED_OFF","compareSummary":"冰石一致"}
            """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/remaining-tissue/complete".formatted(diagnosing.caseId()),
            USER_M4_NO_PERMISSION,
            """
                {
                  "terminalCode": "T-FR-RPT-56",
                  "remarks": "forbidden close",
                  "remainingTissueStatus": "DISPOSED"
                }
                """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void shouldRejectFrozenParaffinCompareAfterAlreadyCompleted() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-005", "BC-FR-RPT-005");

        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-17","remarks":"save preliminary before repeat compare test","preliminaryResult":"疑似间质瘤"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-18","remarks":"phone back before repeat compare test","preliminaryResult":"疑似间质瘤"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-19","remarks":"confirm before repeat compare test"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(diagnosing.caseId()), USER_M4_REVIEW, """
            {"terminalCode":"T-FR-RPT-20","remarks":"first compare","compareStatus":"SIGNED_OFF","compareSummary":"冰石一致"}
            """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(diagnosing.caseId()),
            USER_M4_REVIEW,
            """
                {
                  "terminalCode": "T-FR-RPT-21",
                  "remarks": "repeat compare",
                  "compareStatus": "MISMATCH",
                  "compareSummary": "重复对比"
                }
                """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("Frozen paraffin compare is already completed")));
    }

    @Test
    void shouldRejectFrozenParaffinCompareAfterCompletionByUnassignedReviewer() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-021", "BC-FR-RPT-021");
        String otherReviewerUserId = createReviewUser("FRCOMPARE");

        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/preliminary-report/save".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-31","remarks":"save preliminary before outsider compare test","preliminaryResult":"疑似纤维腺瘤"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/phone-back/complete".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-32","remarks":"phone back before outsider compare test","preliminaryResult":"疑似纤维腺瘤"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/report/confirm".formatted(diagnosing.caseId()), USER_M4_DIAGNOSIS, """
            {"terminalCode":"T-FR-RPT-33","remarks":"confirm before outsider compare test"}
            """), 200);
        responseBody(postJson("/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(diagnosing.caseId()), USER_M4_REVIEW, """
            {"terminalCode":"T-FR-RPT-34","remarks":"first compare before outsider compare test","compareStatus":"SIGNED_OFF","compareSummary":"冰石一致"}
            """), 200);

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/paraffin-compare/complete".formatted(diagnosing.caseId()),
            otherReviewerUserId,
            """
                {
                  "terminalCode": "T-FR-RPT-35",
                  "remarks": "outsider repeat compare",
                  "compareStatus": "MISMATCH",
                  "compareSummary": "复核人重复提交"
                }
                """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message", containsString("用户未被分配审核报告")));
    }

    @Test
    void shouldRejectFrozenGrossingWithoutPendingGrossingTask() throws Exception {
        FrozenCaseContext requested = prepareFrozenRequestedCase("APP-FR-GRS-002", "BC-FR-GRS-002");

        postJson(
            "/api/v1/frozen-workflow/sessions/%s/grossing/complete".formatted(requested.caseId()),
            USER_M3_GROSSING,
            """
                {
                  "terminalCode": "T-FR-GRS-02",
                  "remarks": "invalid frozen grossing"
                }
                """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("Frozen grossing task is not pending")));
    }

    @Test
    void shouldKeepFrozenSessionAtReportStageUntilReportConfirmedByDetailView() throws Exception {
        FrozenCaseContext diagnosing = prepareFrozenStartedDiagnosticCase("APP-FR-RPT-009B", "BC-FR-RPT-009B");
        JsonNode detail = responseBody(mockMvc.perform(authorized(
            get("/api/v1/frozen-workflow/sessions/{sessionId}", diagnosing.caseId()),
            USER_RECEIVE)), 200);
        assertThat(detail.path("currentTaskType").asText()).isEqualTo("REPORT");
    }
}
