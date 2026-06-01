package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                .param("keyword", "ZY-WORKBENCH-001")
                .param("queryType", "INPATIENT_NO"))
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

        prepareTransportReadySpecimen(barcode);
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
    void shouldSupportExplicitLookupTypes() throws Exception {
        createApplication("1122");

        mockMvc.perform(authorized(get("/api/v1/application-registration-workbench/lookup"), USER_REGISTER)
                .param("keyword", "1122")
                .param("queryType", "APPLICATION_NO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.patientInfo.applicationNo").value("1122"));

        mockMvc.perform(authorized(get("/api/v1/application-registration-workbench/lookup"), USER_REGISTER)
                .param("keyword", "Patient")
                .param("queryType", "PATIENT_NAME"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.patientInfo.patientName").value("Patient A"));
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
    void shouldSavePatientInfoWithoutReplacingRegisteredSpecimensOrChangingStatus() throws Exception {
        String applicationId = createApplication("APP-WORKBENCH-PATIENT-001");

        JsonNode registered = responseBody(
            postJson(
                "/api/v1/application-registration-workbench/%s/save".formatted(applicationId),
                USER_REGISTER,
                workbenchSavePayload("ZY-WORKBENCH-PATIENT-001", "甲状腺病灶", "甲状腺")),
            200);
        String specimenId = registered.path("specimenItems").get(0).path("id").asText();
        String registrationStatus = registered.path("patientInfo").path("registrationStatus").asText();

        JsonNode saved = responseBody(
            mockMvc.perform(authorized(patch("/api/v1/application-registration-workbench/{applicationId}/patient-info", applicationId), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(patientInfoSavePayload(
                    "ZY-WORKBENCH-PATIENT-001",
                    "患者信息轻保存后的临床诊断",
                    "患者信息轻保存",
                    "13900001111"))),
            200);

        assertThat(saved.path("patientInfo").path("clinicalDiagnosis").asText())
            .isEqualTo("患者信息轻保存后的临床诊断");
        assertThat(saved.path("patientInfo").path("remark").asText())
            .isEqualTo("患者信息轻保存");
        assertThat(saved.path("patientInfo").path("phone").asText())
            .isEqualTo("13900001111");
        assertThat(saved.path("patientInfo").path("registrationStatus").asText())
            .isEqualTo(registrationStatus);
        assertThat(saved.path("specimenItems")).hasSize(1);
        assertThat(saved.path("specimenItems").get(0).path("id").asText())
            .isEqualTo(specimenId);
        assertThat(querySingleString(
            """
                select count(1)
                from specimens
                where application_id = :applicationId
                """,
            "applicationId",
            applicationId)).isEqualTo("1");

        mockMvc.perform(authorized(get("/api/v1/application-registration-workbench/lookup"), USER_REGISTER)
                .param("keyword", "ZY-WORKBENCH-PATIENT-001")
                .param("queryType", "INPATIENT_NO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.patientInfo.clinicalDiagnosis").value("患者信息轻保存后的临床诊断"))
            .andExpect(jsonPath("$.data.patientInfo.remark").value("患者信息轻保存"))
            .andExpect(jsonPath("$.data.patientInfo.phone").value("13900001111"));
    }

    @Test
    void shouldRejectPatientInfoSaveAfterDownstreamWorkflowStarted() throws Exception {
        String applicationId = createApplication("APP-WORKBENCH-PATIENT-409");
        JsonNode latestRegistration = responseBody(
            postJson(
                "/api/v1/application-registration-workbench/%s/save".formatted(applicationId),
                USER_REGISTER,
                workbenchSavePayload("ZY-WORKBENCH-PATIENT-409", "乳腺病灶", "乳腺")),
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

        mockMvc.perform(authorized(patch("/api/v1/application-registration-workbench/{applicationId}/patient-info", applicationId), USER_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(patientInfoSavePayload(
                    "ZY-WORKBENCH-PATIENT-409",
                    "乳腺复存病灶",
                    "下游后禁止轻保存",
                    "13900002222")))
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
                    "肝",
                    "第二次标本B",
                    "肺")),
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

    @Test
    void shouldGenerateGloballyUniqueSpecimenNosAcrossApplicationsWhenWorkbenchSaved() throws Exception {
        String firstApplicationId = createApplication("APP-WORKBENCH-GLOBAL-001");
        String secondApplicationId = createApplication("APP-WORKBENCH-GLOBAL-002");

        responseBody(
            postJson(
                "/api/v1/application-registration-workbench/%s/save".formatted(firstApplicationId),
                USER_REGISTER,
                workbenchSavePayload("ZY-WORKBENCH-GLOBAL-001", "甲状腺病灶", "甲状腺")),
            200);

        responseBody(
            postJson(
                "/api/v1/application-registration-workbench/%s/save".formatted(secondApplicationId),
                USER_REGISTER,
                workbenchSavePayload("ZY-WORKBENCH-GLOBAL-002", "淋巴结病灶", "淋巴结")),
            200);

        var specimenNos = jdbcTemplate.queryForList("""
                select specimen_no
                from specimens
                where application_id in (:firstApplicationId, :secondApplicationId)
                order by specimen_no asc
                """, java.util.Map.of(
                "firstApplicationId", firstApplicationId,
                "secondApplicationId", secondApplicationId));

        assertThat(specimenNos).hasSize(2);
        assertThat(specimenNos)
            .extracting(row -> String.valueOf(row.get("specimen_no")))
            .doesNotHaveDuplicates()
            .doesNotContain("");
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

    private String patientInfoSavePayload(
        String inpatientNo,
        String clinicalDiagnosis,
        String remark,
        String phone
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
                "age": "35岁",
                "applicationDate": "2026-05-27",
                "applicationNo": "",
                "applyDept": "OR",
                "applyDoctor": "Dr A",
                "bedNo": "16床",
                "checkItem": "术中病理",
                "clinicalDiagnosis": "%s",
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
                "phone": "%s",
                "registrationStatus": "登记",
                "remark": "%s",
                "specimenType": "ROUTINE",
                "wardName": "外科病区"
              },
              "surgeryInfo": {
                "buildingId": "B001",
                "clinicalFindings": "术中见结节样病灶",
                "fixativeType": "福尔马林",
                "fixationPerson": "护士甲",
                "fixationTime": "2026-05-27T10:15:00",
                "roomId": "OR-101",
                "specimenRemovalTime": "2026-05-27T10:00:00",
                "surgeryName": "甲状腺病灶切除术"
              }
            }
            """.formatted(clinicalDiagnosis, inpatientNo, phone, remark);
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
                  "specimenSite": "%s",
                  "status": "新增"
                },
                {
                  "quantity": 1,
                  "specimenName": "%s",
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
