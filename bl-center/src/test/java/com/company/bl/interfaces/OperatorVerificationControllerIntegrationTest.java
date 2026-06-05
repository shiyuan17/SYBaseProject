package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class OperatorVerificationControllerIntegrationTest extends AbstractSpecimenWorkflowIntegrationTest {

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
    void shouldRequireValidOperatorTokenBeforeSpecimenStateMutation() throws Exception {
        String applicationId = createApplication("APP-M2-OPVERIFY-" + uniqueSuffix());
        JsonNode registration = registerSpecimens(
            applicationId,
            USER_REGISTER,
            "P-01",
            "/api/v1/specimens/register",
            "BC-OPVERIFY-" + uniqueSuffix());
        String barcode = registration.path("specimens").get(0).path("barcode").asText();

        completeFixation(barcode);

        postJson("/api/v1/specimens/barcodes/%s/confirm".formatted(barcode), USER_FIXATION, """
            {
              "terminalCode": "T-CONFIRM"
            }
            """)
            .andExpect(status().isBadRequest());

        postJson("/api/v1/specimens/barcodes/%s/confirm".formatted(barcode), USER_FIXATION, """
            {
              "operatorVerificationToken": "invalid-token",
              "terminalCode": "T-CONFIRM"
            }
            """)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message", containsString("核对人登录确认已失效")));

        String operatorVerificationToken = operatorVerificationToken(USER_FIXATION, USER_TRANSPORT);
        postJson("/api/v1/specimens/barcodes/%s/confirm".formatted(barcode), USER_FIXATION, """
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
            """, Map.of("barcode", barcode));
        assertThat(confirmEvent.get("operator_user_id")).isEqualTo(USER_TRANSPORT);
        assertThat(confirmEvent.get("operator_name")).isEqualTo(userDisplayName(USER_TRANSPORT));
    }
}
