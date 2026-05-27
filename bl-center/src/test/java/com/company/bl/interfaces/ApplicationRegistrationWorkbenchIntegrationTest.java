package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class ApplicationRegistrationWorkbenchIntegrationTest extends AbstractSpecimenWorkflowIntegrationTest {

    @Test
    void shouldLookupByApplicationNoAndSaveWorkbenchForDownstreamFlow() throws Exception {
        String applicationId = createApplication("APP-WORKBENCH-001");

        mockMvc.perform(authorized(get("/api/v1/application-registration-workbench/lookup"), USER_REGISTER)
                .param("keyword", "APP-WORKBENCH-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.applicationId").value(applicationId))
            .andExpect(jsonPath("$.data.patientInfo.applicationNo").value("APP-WORKBENCH-001"));

        mockMvc.perform(authorized(get("/api/v1/application-registration-workbench/lookup"), USER_REGISTER)
                .param("keyword", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.applicationId").value(applicationId))
            .andExpect(jsonPath("$.data.patientInfo.applicationNo").value("APP-WORKBENCH-001"));

        JsonNode saved = responseBody(
            postJson(
                "/api/v1/application-registration-workbench/%s/save".formatted(applicationId),
                USER_REGISTER,
                workbenchSavePayload("ZY-WORKBENCH-001", "甲状腺病灶", "甲状腺")),
            200);

        assertThat(saved.path("applicationId").asText()).isEqualTo(applicationId);
        assertThat(saved.path("patientInfo").path("inpatientNo").asText()).isEqualTo("ZY-WORKBENCH-001");
        assertThat(saved.path("specimenItems")).hasSize(1);

        mockMvc.perform(authorized(get("/api/v1/application-registration-workbench/lookup"), USER_REGISTER)
                .param("keyword", "ZY-WORKBENCH-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.applicationId").value(applicationId))
            .andExpect(jsonPath("$.data.patientInfo.inpatientNo").value("ZY-WORKBENCH-001"));

        JsonNode latestRegistration = responseBody(
            mockMvc.perform(authorized(get("/api/v1/specimens/applications/{applicationId}/latest-registration", applicationId), USER_REGISTER)),
            200);
        assertThat(latestRegistration.path("specimens")).hasSize(1);
        String barcode = latestRegistration.path("specimens").get(0).path("barcode").asText();

        mockMvc.perform(authorized(get("/api/v1/specimens"), USER_REGISTER)
                .param("page", "1")
                .param("size", "20")
                .param("applicationNo", "APP-WORKBENCH-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].applicationNo").value("APP-WORKBENCH-001"));

        mockMvc.perform(authorized(get("/api/v1/applications/{id}/tracking", applicationId), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.specimens[0].barcode").value(barcode));

        completeFixation(barcode);
        String transportOrderId = createTransportOrder(applicationId, barcode).path("id").asText();
        postJson("/api/v1/transport-orders/%s/handover".formatted(transportOrderId), USER_TRANSPORT, """
            {
              "receiverUserName": "receiver-workbench",
              "terminalCode": "WB-T-01"
            }
            """)
            .andExpect(status().isOk());
        postJson("/api/v1/specimen-receipts", USER_RECEIVE, """
            {
              "transportOrderId": "%s",
              "receivedByName": "receiver-workbench",
              "terminalCode": "WB-R-01",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1,
                  "qualityCheckResult": "PASSED"
                }
              ]
            }
            """.formatted(transportOrderId, barcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.receiptStatus").value("RECEIVED"));
    }

    @Test
    void shouldReturnNotFoundWhenWorkbenchLookupKeywordDoesNotMatchAnySupportedIdentifier() throws Exception {
        createApplication("APP-WORKBENCH-NOT-FOUND");

        mockMvc.perform(authorized(get("/api/v1/application-registration-workbench/lookup"), USER_REGISTER)
                .param("keyword", "MISSING-WORKBENCH-KEYWORD"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("申请登记工作台记录不存在"));
    }

    @Test
    void shouldRejectWorkbenchSaveAfterDownstreamWorkflowStarted() throws Exception {
        String applicationId = createApplication("APP-WORKBENCH-409");
        JsonNode latestRegistration = responseBody(
            postJson(
                "/api/v1/application-registration-workbench/%s/save".formatted(applicationId),
                USER_REGISTER,
                workbenchSavePayload("ZY-WORKBENCH-409", "乳腺病灶", "乳腺")),
            200);
        String specimenId = latestRegistration.path("specimenItems").get(0).path("id").asText();

        String actualBarcode = querySingleString(
            """
                select barcode
                from specimens
                where id = :specimenId
                """,
            "specimenId",
            specimenId);
        completeFixation(actualBarcode);

        postJson(
            "/api/v1/application-registration-workbench/%s/save".formatted(applicationId),
            USER_REGISTER,
            workbenchSavePayload("ZY-WORKBENCH-409", "乳腺复存病灶", "乳腺"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"))
            .andExpect(jsonPath("$.message").value("申请单已进入下游流程，无法在登记工作台重新填写"));
    }

    @Test
    void shouldReplaceRegisteredSpecimensWhenWorkbenchSavedAgainBeforeDownstream() throws Exception {
        String applicationId = createApplication("APP-WORKBENCH-REPLACE");

        JsonNode firstSave = responseBody(
            postJson(
                "/api/v1/application-registration-workbench/%s/save".formatted(applicationId),
                USER_REGISTER,
                workbenchSavePayloadWithTwoItems(
                    "ZY-WORKBENCH-REPLACE",
                    "第一次标本A",
                    "甲状腺",
                    "第一次标本B",
                    "淋巴结")),
            200);

        JsonNode secondSave = responseBody(
            postJson(
                "/api/v1/application-registration-workbench/%s/save".formatted(applicationId),
                USER_REGISTER,
                workbenchSavePayloadWithTwoItems(
                    "ZY-WORKBENCH-REPLACE",
                    "第二次标本A",
                    "胃",
                    "第二次标本B",
                    "肠")),
            200);

        mockMvc.perform(authorized(get("/api/v1/specimens"), USER_REGISTER)
                .param("page", "1")
                .param("size", "20")
                .param("applicationNo", "APP-WORKBENCH-REPLACE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2));

        assertThat(querySingleString(
            """
                select count(1)
                from specimens
                where application_id = :applicationId
                """,
            "applicationId",
            applicationId)).isEqualTo("2");

        JsonNode latestRegistration = responseBody(
            mockMvc.perform(authorized(get("/api/v1/specimens/applications/{applicationId}/latest-registration", applicationId), USER_REGISTER)),
            200);
        assertThat(latestRegistration.path("specimens")).hasSize(2);

        String firstSpecimenId = firstSave.path("specimenItems").get(0).path("id").asText();
        String secondSpecimenId = secondSave.path("specimenItems").get(0).path("id").asText();

        assertThat(latestRegistration.path("specimens"))
            .extracting(node -> node.path("id").asText())
            .contains(secondSpecimenId)
            .doesNotContain(firstSpecimenId);
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
                  "specimenNo": "22501",
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

    private String workbenchSavePayloadWithTwoItems(
        String inpatientNo,
        String specimenName1,
        String specimenSite1,
        String specimenName2,
        String specimenSite2
    ) {
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
                "age": "42岁",
                "applicationDate": "2026-05-27",
                "applicationNo": "",
                "applyDept": "OR",
                "applyDoctor": "Dr A",
                "bedNo": "18床",
                "checkItem": "常规病理",
                "clinicalDiagnosis": "送检病灶",
                "clinicalHistory": "工作台整单替换测试",
                "deliveryRequirement": "常规送检",
                "endoscopyDiagnosis": "",
                "frozenReminder": false,
                "gender": "男",
                "idNo": "320101199001011235",
                "imagingResult": "影像待补充",
                "inpatientNo": "%s",
                "patientName": "Patient A",
                "patientVerified": true,
                "phone": "13800002222",
                "registrationStatus": "登记",
                "remark": "整单替换",
                "specimenType": "ROUTINE",
                "wardName": "普外病区"
              },
              "specimenItems": [
                {
                  "quantity": 1,
                  "specimenName": "%s",
                  "specimenNo": "22501",
                  "specimenSite": "%s",
                  "status": "新增"
                },
                {
                  "quantity": 1,
                  "specimenName": "%s",
                  "specimenNo": "22502",
                  "specimenSite": "%s",
                  "status": "新增"
                }
              ],
              "surgeryInfo": {
                "buildingId": "B001",
                "clinicalFindings": "术中见占位病灶",
                "fixativeType": "福尔马林",
                "fixationPerson": "护士乙",
                "fixationTime": "2026-05-27T11:00:00",
                "roomId": "OR-102",
                "surgeryName": "病灶切除术"
              }
            }
            """.formatted(inpatientNo, specimenName1, specimenSite1, specimenName2, specimenSite2);
    }
}
