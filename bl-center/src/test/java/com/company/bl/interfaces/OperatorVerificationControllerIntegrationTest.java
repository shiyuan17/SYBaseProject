package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class OperatorVerificationControllerIntegrationTest extends AbstractSpecimenWorkflowIntegrationTest {

    @Test
    void shouldRequireAuthenticationBeforeValidatingOperatorVerificationPayload() throws Exception {
        mockMvc.perform(post("/api/v1/operator-verifications")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "operatorUserId": "",
                      "loginName": "",
                      "password": ""
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void shouldVerifyOperatorCredentialAndReturnShortLivedToken() throws Exception {
        JsonNode data = responseBody(postJson("/api/v1/operator-verifications", USER_FIXATION, """
            {
              "operatorUserId": "%s",
              "loginName": "%s",
              "password": "123456"
            }
            """.formatted(USER_TRANSPORT, userLoginName(USER_TRANSPORT))), 200);

        assertThat(data.path("operatorVerificationToken").asText()).isNotBlank();
        assertThat(data.path("operatorUserId").asText()).isEqualTo(USER_TRANSPORT);
        assertThat(data.path("loginName").asText()).isEqualTo(userLoginName(USER_TRANSPORT));
        assertThat(data.path("operatorName").asText()).isEqualTo(userDisplayName(USER_TRANSPORT));
        assertThat(data.path("expiresAt").asText()).isNotBlank();
    }

    @Test
    void shouldAuditOperatorVerificationSuccessAndFailureWithoutLeakingSecrets() throws Exception {
        long auditCountBefore = operationLogCount("issue_operator_verification_token");

        JsonNode success = responseBody(postJson("/api/v1/operator-verifications", USER_FIXATION, """
            {
              "operatorUserId": "%s",
              "loginName": "%s",
              "password": "123456"
            }
            """.formatted(USER_TRANSPORT, userLoginName(USER_TRANSPORT))), 200);

        assertThat(operationLogCount("issue_operator_verification_token")).isEqualTo(auditCountBefore + 1);
        Map<String, Object> successAudit = latestOperationLog("issue_operator_verification_token");
        assertThat(successAudit.get("operation_result")).isEqualTo("SUCCESS");
        assertThat(successAudit.get("operation_content").toString())
            .contains("operatorUserId=" + USER_TRANSPORT)
            .contains("loginName=" + userLoginName(USER_TRANSPORT))
            .doesNotContain("123456")
            .doesNotContain(success.path("operatorVerificationToken").asText())
            .doesNotContain("Authorization");

        postJson("/api/v1/operator-verifications", USER_FIXATION, """
            {
              "operatorUserId": "%s",
              "loginName": "%s",
              "password": "wrong-password"
            }
            """.formatted(USER_TRANSPORT, userLoginName(USER_TRANSPORT)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message", containsString("核对人账号或密码错误")));

        assertThat(operationLogCount("issue_operator_verification_token")).isEqualTo(auditCountBefore + 2);
        Map<String, Object> failedAudit = latestOperationLog("issue_operator_verification_token");
        assertThat(failedAudit.get("operation_result")).isEqualTo("FAILED");
        assertThat(failedAudit.get("failure_reason")).isEqualTo("核对人账号或密码错误");
        assertThat(failedAudit.get("operation_content").toString())
            .contains("operatorUserId=" + USER_TRANSPORT)
            .doesNotContain("wrong-password")
            .doesNotContain("Authorization");
    }

    @Test
    void shouldRejectWrongPasswordDisabledOperatorAndSameLoginUser() throws Exception {
        postJson("/api/v1/operator-verifications", USER_FIXATION, """
            {
              "operatorUserId": "%s",
              "loginName": "%s",
              "password": "wrong-password"
            }
            """.formatted(USER_TRANSPORT, userLoginName(USER_TRANSPORT)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message", containsString("核对人账号或密码错误")));

        try {
            jdbcTemplate.update("""
                update users
                set enabled = 0
                where id = :userId
                """, Map.of("userId", USER_TRANSPORT));

            postJson("/api/v1/operator-verifications", USER_FIXATION, """
                {
                  "operatorUserId": "%s",
                  "loginName": "%s",
                  "password": "123456"
                }
                """.formatted(USER_TRANSPORT, userLoginName(USER_TRANSPORT)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("核对人账号或密码错误")));
        } finally {
            jdbcTemplate.update("""
                update users
                set enabled = 1
                where id = :userId
                """, Map.of("userId", USER_TRANSPORT));
        }

        postJson("/api/v1/operator-verifications", USER_FIXATION, """
            {
              "operatorUserId": "%s",
              "loginName": "%s",
              "password": "123456"
            }
            """.formatted(USER_FIXATION, userLoginName(USER_FIXATION)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("登录人跟核对人不能是同一个人！"));
    }

    @Test
    void shouldAllowCurrentLoginUserWithoutTokenAndStillRejectInvalidOperatorToken() throws Exception {
        String fallbackBarcode = prepareFixedSpecimenBarcode("APP-M2-OPVERIFY-FALLBACK-" + uniqueSuffix());

        postJson("/api/v1/specimens/barcodes/%s/confirm".formatted(fallbackBarcode), USER_FIXATION, """
            {
              "terminalCode": "T-CONFIRM"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.specimenConfirmedAt").isNotEmpty());

        Map<String, Object> fallbackConfirmEvent = jdbcTemplate.queryForMap("""
            select operator_user_id, operator_name
            from workflow_events
            where specimen_id = (
                select id
                from specimens
                where barcode = :barcode
            )
              and node_code = 'CONFIRMATION'
            order by event_time desc, created_at desc
            limit 1
            """, Map.of("barcode", fallbackBarcode));
        assertThat(fallbackConfirmEvent.get("operator_user_id")).isEqualTo(USER_FIXATION);
        assertThat(fallbackConfirmEvent.get("operator_name")).isEqualTo(userDisplayName(USER_FIXATION));

        String verifiedBarcode = prepareFixedSpecimenBarcode("APP-M2-OPVERIFY-VERIFIED-" + uniqueSuffix());

        postJson("/api/v1/specimens/barcodes/%s/confirm".formatted(verifiedBarcode), USER_FIXATION, """
            {
              "operatorVerificationToken": "invalid-token",
              "terminalCode": "T-CONFIRM"
            }
            """)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message", containsString("核对人登录确认已失效")));

        String operatorVerificationToken = operatorVerificationToken(USER_FIXATION, USER_TRANSPORT);
        postJson("/api/v1/specimens/barcodes/%s/confirm".formatted(verifiedBarcode), USER_FIXATION, """
            {
              "operatorVerificationToken": "%s",
              "terminalCode": "T-CONFIRM"
            }
            """.formatted(operatorVerificationToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.specimenConfirmedAt").isNotEmpty());

        Map<String, Object> confirmEvent = jdbcTemplate.queryForMap("""
            select operator_user_id, operator_name
            from workflow_events
            where specimen_id = (
                select id
                from specimens
                where barcode = :barcode
            )
              and node_code = 'CONFIRMATION'
            order by event_time desc, created_at desc
            limit 1
            """, Map.of("barcode", verifiedBarcode));
        assertThat(confirmEvent.get("operator_user_id")).isEqualTo(USER_TRANSPORT);
        assertThat(confirmEvent.get("operator_name")).isEqualTo(userDisplayName(USER_TRANSPORT));
    }

    @Test
    void shouldRejectOperatorVerificationTokenReusedByAnotherCurrentUser() throws Exception {
        String applicationId = createApplication("APP-M2-OPVERIFY-MISMATCH-" + uniqueSuffix());
        String barcode = "BC-OPVERIFY-MISMATCH-" + uniqueSuffix();
        registerSpecimens(
            applicationId,
            USER_REGISTER,
            "P-01",
            "/api/v1/specimens/register",
            barcode
        );
        prepareTransportReadySpecimen(barcode);

        String mismatchedToken = operatorVerificationToken(USER_FIXATION, USER_TRANSPORT);

        postJson("/api/v1/transport-orders", USER_TRANSPORT, """
            {
              "applicationId": "%s",
              "specimenBarcodes": ["%s"],
              "operatorVerificationToken": "%s",
              "handoverUserName": "handover-a",
              "handoverDepartmentId": "DEPT-OR",
              "handoverDepartmentName": "OR",
              "receiverDepartmentId": "DEPT-PATH",
              "receiverDepartmentName": "Pathology",
              "terminalCode": "OR-02"
            }
            """.formatted(applicationId, barcode, mismatchedToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message", containsString("与当前登录人不匹配")));

        Long orderCount = jdbcTemplate.queryForObject("""
            select count(*)
            from transport_orders
            where application_id = :applicationId
            """, Map.of("applicationId", applicationId), Long.class);
        assertThat(orderCount).isZero();
    }

    private String prepareFixedSpecimenBarcode(String applicationNo) throws Exception {
        String barcode = "BC-OPVERIFY-" + uniqueSuffix();
        String applicationId = createApplication(applicationNo);
        JsonNode registration = registerSpecimens(
            applicationId,
            USER_REGISTER,
            "P-01",
            "/api/v1/specimens/register",
            barcode);
        String specimenBarcode = registration.path("specimens").get(0).path("barcode").asText();
        completeFixation(specimenBarcode);
        return specimenBarcode;
    }

    private long operationLogCount(String operationName) {
        Long count = jdbcTemplate.queryForObject("""
            select count(*)
            from operation_logs
            where operation_name = :operationName
            """, Map.of("operationName", operationName), Long.class);
        return count == null ? 0L : count;
    }

    private Map<String, Object> latestOperationLog(String operationName) {
        return jdbcTemplate.queryForMap("""
            select operation_result, operation_content, failure_reason
            from operation_logs
            where operation_name = :operationName
            order by operation_at desc
            limit 1
            """, Map.of("operationName", operationName));
    }
}
