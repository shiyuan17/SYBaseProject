package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class SpecimenBarcodeBindingIntegrationTest extends AbstractSpecimenWorkflowIntegrationTest {

    @Test
    void shouldBindRebindUnbindAndFilterSpecimenManagementList() throws Exception {
        String applicationId = createApplication("APP-BIND-001");
        String expectedRegistrationOperatorName = userLoginName(USER_REGISTER);
        JsonNode saved = responseBody(
            postJson(
                "/api/v1/application-registration-workbench/%s/save".formatted(applicationId),
                USER_REGISTER,
                workbenchSavePayload("ZY-BIND-001", "甲状腺病灶", "甲状腺")),
            200);
        String specimenId = saved.path("specimenItems").get(0).path("id").asText();

        jdbcTemplate.update("""
            update specimens
            set barcode = null
            where id = :specimenId
            """, Map.of("specimenId", specimenId));

        mockMvc.perform(authorized(get("/api/v1/specimens"), USER_REGISTER)
                .param("page", "1")
                .param("size", "20")
                .param("applicationNo", "APP-BIND-001")
                .param("buildingId", "B001")
                .param("roomId", "OR-101")
                .param("barcodeBindingStatus", "UNBOUND"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.summary.unboundCount").value(1))
            .andExpect(jsonPath("$.data.items[0].specimenId").value(specimenId))
            .andExpect(jsonPath("$.data.items[0].patientId").value("P-001"))
            .andExpect(jsonPath("$.data.items[0].buildingId").value("B001"))
            .andExpect(jsonPath("$.data.items[0].roomId").value("OR-101"))
            .andExpect(jsonPath("$.data.items[0].surgeryName").value("OR-101"))
            .andExpect(jsonPath("$.data.items[0].registrationOperatorName").value(expectedRegistrationOperatorName));

        mockMvc.perform(authorized(post("/api/v1/specimens/{specimenId}/barcode-binding", specimenId), USER_REGISTER)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "targetBarcode": "BC-BIND-001",
                      "terminalCode": "TERM-BIND-01",
                      "remarks": "首次绑定"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.barcode").value("BC-BIND-001"))
            .andExpect(jsonPath("$.data.barcodeBindingStatus").value("BOUND"));

        mockMvc.perform(authorized(put("/api/v1/specimens/{specimenId}/barcode-binding", specimenId), USER_REGISTER)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "targetBarcode": "BC-BIND-002",
                      "terminalCode": "TERM-BIND-02",
                      "remarks": "重绑"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.barcode").value("BC-BIND-002"))
            .andExpect(jsonPath("$.data.barcodeBindingStatus").value("BOUND"));

        mockMvc.perform(authorized(delete("/api/v1/specimens/{specimenId}/barcode-binding", specimenId), USER_REGISTER)
                .param("terminalCode", "TERM-BIND-03")
                .param("remarks", "取消绑定"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.barcodeBindingStatus").value("UNBOUND"));
    }

    @Test
    void shouldAllowFirstBindingAfterWorkflowProgressButKeepRebindAndUnbindLocked() throws Exception {
        String applicationId = createApplication("APP-BIND-409");
        JsonNode registration = registerSpecimens(
            applicationId,
            USER_REGISTER,
            "P-01",
            "/api/v1/specimens/register",
            "BC-BIND-409");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();
        String specimenId = registration.path("specimens").get(0).path("id").asText();

        prepareTransportReadySpecimen(barcode);
        jdbcTemplate.update("""
            update specimens
            set barcode = null
            where id = :specimenId
            """, Map.of("specimenId", specimenId));

        mockMvc.perform(authorized(post("/api/v1/specimens/{specimenId}/barcode-binding", specimenId), USER_REGISTER)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "targetBarcode": "BC-BIND-409-NEW",
                      "terminalCode": "TERM-BIND-409",
                      "remarks": "已入库后尝试绑定"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.barcode").value("BC-BIND-409-NEW"))
            .andExpect(jsonPath("$.data.barcodeBindingStatus").value("BOUND"));

        assertThat(querySingleString(
            """
                select barcode
                from specimens
                where id = :specimenId
                """,
            "specimenId",
            specimenId)).isEqualTo("BC-BIND-409-NEW");

        mockMvc.perform(authorized(put("/api/v1/specimens/{specimenId}/barcode-binding", specimenId), USER_REGISTER)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "targetBarcode": "BC-BIND-409-REBIND",
                      "terminalCode": "TERM-BIND-409-REBIND",
                      "remarks": "已入库后尝试重绑"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"))
            .andExpect(jsonPath("$.message").value("重绑条码前标本不能已入库"));

        mockMvc.perform(authorized(delete("/api/v1/specimens/{specimenId}/barcode-binding", specimenId), USER_REGISTER)
                .param("terminalCode", "TERM-BIND-410")
                .param("remarks", "已入库后尝试取消"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"))
            .andExpect(jsonPath("$.message").value("取消绑定条码前标本不能已入库"));
    }

    @Test
    void shouldAllowFirstBindingAfterReceiptTerminalStatus() throws Exception {
        String applicationId = createApplication("APP-BIND-RECEIVED-001");
        JsonNode registration = registerSpecimens(
            applicationId,
            USER_REGISTER,
            "P-01",
            "/api/v1/specimens/register",
            "BC-BIND-RECEIVED-001");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();
        String specimenId = registration.path("specimens").get(0).path("id").asText();

        prepareTransportReadySpecimen(barcode);
        String orderId = createTransportOrder(applicationId, barcode).path("id").asText();
        postJson("/api/v1/transport-orders/%s/handover".formatted(orderId), USER_TRANSPORT, """
            {
              "receiverUserName": "receiver-complete",
              "terminalCode": "T-BIND-RECEIVED"
            }
            """)
            .andExpect(status().isOk());
        postJson("/api/v1/specimen-receipts", USER_RECEIVE, """
            {
              "transportOrderId": "%s",
              "receivedByName": "receiver-complete",
              "logisticsStaffName": "物流员绑定终态",
              "terminalCode": "T-BIND-RECEIVED",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1,
                  "qualityCheckResult": "PASSED"
                }
              ]
            }
            """.formatted(orderId, barcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.receiptStatus").value("RECEIVED"));

        jdbcTemplate.update("""
            update specimens
            set barcode = null
            where id = :specimenId
            """, Map.of("specimenId", specimenId));

        mockMvc.perform(authorized(post("/api/v1/specimens/{specimenId}/barcode-binding", specimenId), USER_REGISTER)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "targetBarcode": "BC-BIND-RECEIVED-NEW",
                      "terminalCode": "TERM-BIND-RECEIVED",
                      "remarks": "接收后首次绑定"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.barcode").value("BC-BIND-RECEIVED-NEW"))
            .andExpect(jsonPath("$.data.barcodeBindingStatus").value("BOUND"));

        assertThat(querySingleString(
            """
                select barcode
                from specimens
                where id = :specimenId
                """,
            "specimenId",
            specimenId)).isEqualTo("BC-BIND-RECEIVED-NEW");
    }

    private String workbenchSavePayload(String inpatientNo, String specimenName, String specimenSite) {
        return """
            {
              "contagiousSpecimen": {
                "hepatitis": false,
                "hiv": false,
                "isolation": false,
                "syphilis": false,
                "tuberculosis": false
              },
              "gynecologyInfo": {
                "additionalNotes": "",
                "hpvResult": "",
                "lastMenstrualPeriod": "",
                "menopause": false,
                "previousCytology": "",
                "previousTreatment": "",
                "specialConditions": {
                  "abnormalBleeding": false,
                  "birthControl": false,
                  "hormoneReplacement": false,
                  "hysterectomy": false,
                  "iud": false,
                  "lactation": false,
                  "menopause": false,
                  "other": "",
                  "pregnancy": false,
                  "radiotherapy": false
                }
              },
              "patientInfo": {
                "age": "35岁",
                "applicationDate": "2026-05-27",
                "applicationNo": "",
                "applyDept": "OR",
                "applyDoctor": "Dr A",
                "bedNo": "16床",
                "checkItem": "术中病理",
                "clinicalDiagnosis": "甲状腺结节",
                "clinicalHistory": "甲状腺结节病史",
                "deliveryRequirement": "立即送检",
                "endoscopyDiagnosis": "",
                "frozenReminder": false,
                "gender": "女",
                "idNo": "320101199001011234",
                "imagingResult": "超声提示甲状腺结节",
                "inpatientNo": "%s",
                "patientName": "Patient A",
                "patientVerified": true,
                "phone": "13800001111",
                "registrationStatus": "登记",
                "remark": "工作台保存",
                "specimenType": "ROUTINE",
                "wardName": "外科病区"
              },
              "specimenItems": [
                {
                  "quantity": 1,
                  "specimenName": "%s",
                  "specimenSite": "%s",
                  "status": "新增"
                }
              ],
              "surgeryInfo": {
                "buildingId": "B001",
                "clinicalFindings": "术中见结节样病灶",
                "fixativeType": "福尔马林",
                "fixationPerson": "护士甲",
                "fixationTime": "2026-05-27T10:15:00",
                "roomId": "OR-101",
                "surgeryName": "甲状腺病灶切除术"
              }
            }
            """.formatted(inpatientNo, specimenName, specimenSite);
    }
}
