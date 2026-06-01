package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class SpecimenWorkflowClosureIntegrationTest extends AbstractSpecimenWorkflowIntegrationTest {

    @Test
    void shouldRequireAuthorizationAndRoleAccess() throws Exception {
        String applicationId = createApplication("APP-M2-AUTH-001");

        mockMvc.perform(post("/api/v1/specimens/register")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(registerSpecimenPayload(applicationId, "P-01", "BC-AUTH-001")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        postJson("/api/v1/specimens/register", USER_NO_PERMISSION, registerSpecimenPayload(applicationId, "P-01", "BC-AUTH-002"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

        postJson("/api/v1/specimens/register", USER_REGISTER, registerSpecimenPayload(applicationId, "P-01", "BC-AUTH-003"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.specimens[0].barcode").value("BC-AUTH-003"));

        mockMvc.perform(authorized(get("/api/v1/applications/{id}/tracking", applicationId), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(applicationId));
    }

    @Test
    void shouldRejectTransportOrderWhenSpecimenNotFixed() throws Exception {
        String applicationId = createApplication("APP-M2-002");
        JsonNode registration = registerSpecimens(applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-NOT-FIXED");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();

        postJson("/api/v1/transport-orders", USER_TRANSPORT, """
            {
              "applicationId": "%s",
              "specimenBarcodes": ["%s"],
              "handoverUserName": "handover-a",
              "handoverDepartmentName": "OR",
              "receiverDepartmentName": "Pathology"
            }
            """.formatted(applicationId, barcode))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));
    }

    @Test
    void shouldRejectDuplicateBarcodeAndDuplicateReceipt() throws Exception {
        String applicationId = createApplication("APP-M2-DUP-001");
        JsonNode registration = registerSpecimens(applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-DUP-001");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();

        postJson("/api/v1/specimens/register", USER_REGISTER, registerSpecimenPayload(applicationId, "P-01", barcode))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

        completeFixation(barcode);

        postJson("/api/v1/specimen-receipts/by-barcodes", USER_RECEIVE, """
            {
              "receivedByName": "receiver-dup",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1,
                  "qualityCheckResult": "PASSED"
                }
              ]
            }
            """.formatted(barcode))
            .andExpect(status().isOk());

        postJson("/api/v1/specimen-receipts/by-barcodes", USER_RECEIVE, """
            {
              "receivedByName": "receiver-dup",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1,
                  "qualityCheckResult": "PASSED"
                }
              ]
            }
            """.formatted(barcode))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
    }

    @Test
    void shouldPersistSelectedConfirmationAndCheckInOperators() throws Exception {
        String applicationId = createApplication("APP-M2-OPERATOR-001");
        JsonNode registration = registerSpecimens(
            applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-OPERATOR-001");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();
        String confirmUserId = USER_TRANSPORT;
        String confirmUserName = userDisplayName(confirmUserId);
        String checkInUserId = USER_RECEIVE;
        String checkInUserName = userDisplayName(checkInUserId);

        completeFixation(barcode);

        postJson("/api/v1/specimens/barcodes/%s/confirm".formatted(barcode), USER_FIXATION, """
            {
              "operatorUserId": "%s",
              "operatorName": "%s",
              "terminalCode": "T-CONFIRM-SELECTED"
            }
            """.formatted(confirmUserId, confirmUserName))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.specimenConfirmedAt").isNotEmpty());

        postJson("/api/v1/specimens/barcodes/%s/check-in".formatted(barcode), USER_FIXATION, """
            {
              "operatorUserId": "%s",
              "operatorName": "%s",
              "specimenBarcode": "%s",
              "terminalCode": "T-CHECK-IN-SELECTED"
            }
            """.formatted(checkInUserId, checkInUserName, barcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.checkedInByName").value(checkInUserName));

        mockMvc.perform(authorized(get("/api/v1/specimens"), USER_REGISTER)
                .param("page", "1")
                .param("size", "20")
                .param("keyword", barcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].specimenConfirmedByUserId").value(confirmUserId))
            .andExpect(jsonPath("$.data.items[0].specimenConfirmedByName").value(confirmUserName))
            .andExpect(jsonPath("$.data.items[0].checkedInByName").value(checkInUserName));
    }

    @Test
    void shouldOutboundTransportOrderAndPersistOutboundOperator() throws Exception {
        String applicationId = createApplication("APP-M2-OUTBOUND-001");
        JsonNode registration = registerSpecimens(
            applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-OUTBOUND-001");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();
        String outboundUserId = USER_RECEIVE;
        String outboundUserName = userDisplayName(outboundUserId);

        prepareTransportReadySpecimen(barcode);
        String transportOrderId = createTransportOrder(applicationId, barcode).path("id").asText();

        postJson("/api/v1/transport-orders/%s/print".formatted(transportOrderId), USER_TRANSPORT, """
            {
              "terminalCode": "T-OUTBOUND-PRINT"
            }
            """)
            .andExpect(status().isOk());

        postJson("/api/v1/transport-orders/%s/outbound".formatted(transportOrderId), USER_TRANSPORT, """
            {
              "outboundUserId": "%s",
              "outboundUserName": "%s",
              "terminalCode": "T-OUTBOUND-01",
              "remarks": "扫码直接出库"
            }
            """.formatted(outboundUserId, outboundUserName))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("HANDED_OVER"))
            .andExpect(jsonPath("$.data.outboundUserId").value(outboundUserId))
            .andExpect(jsonPath("$.data.outboundUserName").value(outboundUserName))
            .andExpect(jsonPath("$.data.handedOverAt").isNotEmpty());

        assertThat(querySingleString(
            """
                select outbound_user_name
                from transport_orders
                where id = :transportOrderId
                """,
            "transportOrderId",
            transportOrderId)).isEqualTo(outboundUserName);
        assertThat(querySingleString(
            """
                select specimen_status
                from specimens
                where barcode = :barcode
                """,
            "barcode",
            barcode)).isEqualTo("IN_TRANSIT");
        assertThat(querySingleString(
            """
                select status
                from applications
                where id = :applicationId
                """,
            "applicationId",
            applicationId)).isEqualTo("IN_TRANSIT");
        assertThat(querySingleString(
            """
                select operator_name
                from workflow_events
                where transport_order_id = :transportOrderId
                  and node_code = 'TRANSPORT'
                  and event_type = 'HANDED_OVER'
                order by event_time desc
                limit 1
                """,
            "transportOrderId",
            transportOrderId)).isEqualTo(outboundUserName);
    }

    @Test
    void shouldQuickOutboundSpecimenBySpecimenNoAndPersistAutoCreatedOrder() throws Exception {
        String applicationId = createApplication("APP-M2-QUICK-OUTBOUND-001");
        JsonNode registration = registerSpecimens(
            applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-QUICK-OUTBOUND-001");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();
        String specimenId = registration.path("specimens").get(0).path("id").asText();
        String specimenNo = registration.path("specimens").get(0).path("specimenNo").asText();
        String outboundUserId = USER_RECEIVE;
        String outboundUserName = userDisplayName(outboundUserId);

        prepareTransportReadySpecimen(barcode);

        JsonNode quickOutbound = responseBody(postJson("/api/v1/specimen-outbounds/quick-outbound", USER_TRANSPORT, """
            {
              "identifierType": "SPECIMEN_NO",
              "identifier": "%s",
              "outboundUserId": "%s",
              "outboundUserName": "%s",
              "terminalCode": "T-QUICK-OUTBOUND-01",
              "remarks": "自动补建并出库"
            }
            """.formatted(specimenNo, outboundUserId, outboundUserName)), 200);

        String transportOrderId = quickOutbound.path("id").asText();
        assertThat(transportOrderId).isNotBlank();
        assertThat(quickOutbound.path("status").asText()).isEqualTo("HANDED_OVER");
        assertThat(quickOutbound.path("outboundUserName").asText()).isEqualTo(outboundUserName);
        assertThat(quickOutbound.path("handedOverAt").asText()).isNotBlank();

        assertThat(jdbcTemplate.queryForObject(
            """
                select count(1)
                from transport_orders
                where application_id = :applicationId
                """,
            java.util.Map.of("applicationId", applicationId),
            Long.class)).isEqualTo(1L);
        assertThat(querySingleString(
            """
                select outbound_user_name
                from transport_orders
                where id = :transportOrderId
                """,
            "transportOrderId",
            transportOrderId)).isEqualTo(outboundUserName);
        assertThat(querySingleString(
            """
                select specimen_status
                from specimens
                where id = :specimenId
                """,
            "specimenId",
            specimenId)).isEqualTo("IN_TRANSIT");
        assertThat(querySingleString(
            """
                select status
                from applications
                where id = :applicationId
                """,
            "applicationId",
            applicationId)).isEqualTo("IN_TRANSIT");
        assertThat(jdbcTemplate.queryForObject(
            """
                select count(1)
                from transport_order_items
                where transport_order_id = :transportOrderId
                  and specimen_id = :specimenId
                """,
            java.util.Map.of(
                "transportOrderId", transportOrderId,
                "specimenId", specimenId),
            Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
            """
                select count(1)
                from workflow_events
                where transport_order_id = :transportOrderId
                  and specimen_id = :specimenId
                  and node_code = 'TRANSPORT'
                  and event_type = 'ORDER_CREATED'
                """,
            java.util.Map.of(
                "transportOrderId", transportOrderId,
                "specimenId", specimenId),
            Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
            """
                select count(1)
                from workflow_events
                where transport_order_id = :transportOrderId
                  and specimen_id = :specimenId
                  and node_code = 'TRANSPORT'
                  and event_type = 'HANDED_OVER'
                """,
            java.util.Map.of(
                "transportOrderId", transportOrderId,
                "specimenId", specimenId),
            Long.class)).isEqualTo(1L);

        mockMvc.perform(authorized(get("/api/v1/specimen-outbounds"), USER_TRANSPORT)
                .param("page", "1")
                .param("size", "20")
                .param("specimenNo", specimenNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].transportOrderId").value(transportOrderId))
            .andExpect(jsonPath("$.data.items[0].specimenId").value(specimenId))
            .andExpect(jsonPath("$.data.items[0].specimenNo").value(specimenNo))
            .andExpect(jsonPath("$.data.items[0].outboundAt").isNotEmpty())
            .andExpect(jsonPath("$.data.items[0].outboundUserName").value(outboundUserName));
    }

    @Test
    void shouldListOperatingOptionsWhenWorkbenchContainsBuildingAndRoom() throws Exception {
        String applicationId = createApplication("APP-M2-OPERATING-OPTIONS-001");
        insertWorkbenchOperatingOption(applicationId, "OR-BUILDING-A", "OR-ROOM-01");

        mockMvc.perform(authorized(get("/api/v1/application-registration-workbench/operating-options"), USER_REGISTER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.buildings[0].buildingId").value("OR-BUILDING-A"))
            .andExpect(jsonPath("$.data.buildings[0].operatingRooms[0].buildingId").value("OR-BUILDING-A"))
            .andExpect(jsonPath("$.data.buildings[0].operatingRooms[0].roomId").value("OR-ROOM-01"));
    }

    @Test
    void shouldListSpecimenOutboundsWithMixedStatesAndReflectOutboundUpdates() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/application-registration-workbench/operating-options"), USER_REGISTER))
            .andExpect(status().isOk());

        String pendingApplicationId = createApplication("APP-M2-OUTBOUND-LIST-PENDING-001");
        JsonNode pendingRegistration = registerSpecimens(
            pendingApplicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-OUTBOUND-LIST-PENDING-001");
        String pendingBarcode = pendingRegistration.path("specimens").get(0).path("barcode").asText();
        String pendingSpecimenNo = pendingRegistration.path("specimens").get(0).path("specimenNo").asText();
        String pendingSpecimenId = pendingRegistration.path("specimens").get(0).path("id").asText();

        prepareTransportReadySpecimen(pendingBarcode);
        String pendingTransportOrderId = createTransportOrder(pendingApplicationId, pendingBarcode).path("id").asText();
        insertWorkbenchExtension(pendingApplicationId, "ZY-OUT-001", "手术间A");

        String completedApplicationId = createApplication("APP-M2-OUTBOUND-LIST-COMPLETE-001");
        JsonNode completedRegistration = registerSpecimens(
            completedApplicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-OUTBOUND-LIST-COMPLETE-001");
        String completedBarcode = completedRegistration.path("specimens").get(0).path("barcode").asText();
        String completedSpecimenNo = completedRegistration.path("specimens").get(0).path("specimenNo").asText();
        String outboundUserName = userDisplayName(USER_RECEIVE);

        prepareTransportReadySpecimen(completedBarcode);
        String completedTransportOrderId = createTransportOrder(completedApplicationId, completedBarcode).path("id").asText();
        insertWorkbenchExtension(completedApplicationId, "ZY-OUT-002", "手术间B");

        postJson("/api/v1/transport-orders/%s/outbound".formatted(completedTransportOrderId), USER_TRANSPORT, """
            {
              "outboundUserId": "%s",
              "outboundUserName": "%s",
              "terminalCode": "T-OUTBOUND-LIST"
            }
            """.formatted(USER_RECEIVE, outboundUserName))
            .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/v1/specimen-outbounds"), USER_TRANSPORT)
                .param("page", "1")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.items[0].transportOrderId").value(pendingTransportOrderId))
            .andExpect(jsonPath("$.data.items[0].specimenId").value(pendingSpecimenId))
            .andExpect(jsonPath("$.data.items[0].specimenNo").value(pendingSpecimenNo))
            .andExpect(jsonPath("$.data.items[0].patientId").value("P-001"))
            .andExpect(jsonPath("$.data.items[0].inpatientNo").value("ZY-OUT-001"))
            .andExpect(jsonPath("$.data.items[0].surgeryName").value("手术间A"))
            .andExpect(jsonPath("$.data.items[0].registeredByName").value(userDisplayName(USER_REGISTER)))
            .andExpect(jsonPath("$.data.items[0].outboundAt").isEmpty())
            .andExpect(jsonPath("$.data.items[0].outboundUserName").isEmpty())
            .andExpect(jsonPath("$.data.items[1].transportOrderId").value(completedTransportOrderId))
            .andExpect(jsonPath("$.data.items[1].specimenNo").value(completedSpecimenNo))
            .andExpect(jsonPath("$.data.items[1].inpatientNo").value("ZY-OUT-002"))
            .andExpect(jsonPath("$.data.items[1].surgeryName").value("手术间B"))
            .andExpect(jsonPath("$.data.items[1].outboundAt").isNotEmpty())
            .andExpect(jsonPath("$.data.items[1].outboundUserName").value(outboundUserName));

        mockMvc.perform(authorized(get("/api/v1/specimen-outbounds"), USER_TRANSPORT)
                .param("page", "1")
                .param("size", "20")
                .param("specimenNo", pendingSpecimenNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].transportOrderId").value(pendingTransportOrderId))
            .andExpect(jsonPath("$.data.items[0].specimenNo").value(pendingSpecimenNo));

        mockMvc.perform(authorized(get("/api/v1/specimen-outbounds"), USER_TRANSPORT)
                .param("page", "1")
                .param("size", "20")
                .param("specimenNo", completedSpecimenNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].transportOrderId").value(completedTransportOrderId))
            .andExpect(jsonPath("$.data.items[0].outboundAt").isNotEmpty())
            .andExpect(jsonPath("$.data.items[0].outboundUserName").value(outboundUserName));
    }

    @Test
    void shouldQuickConfirmRemovalByBarcodeAndPersistAudit() throws Exception {
        String applicationId = createApplication("APP-M2-REMOVAL-BARCODE-001");
        JsonNode registration = registerSpecimens(
            applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-REMOVAL-BARCODE-001");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();
        String specimenId = registration.path("specimens").get(0).path("id").asText();
        String operatorName = userDisplayName(USER_FIXATION);

        postJson("/api/v1/specimen-removals/confirm-by-identifier", USER_FIXATION, """
            {
              "identifierType": "BARCODE",
              "identifier": "%s",
              
              "terminalCode": "T-REMOVAL-BARCODE",
              "remarks": "离体确认"
            }
            """.formatted(barcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.specimenId").value(specimenId))
            .andExpect(jsonPath("$.data.barcode").value(barcode))
            .andExpect(jsonPath("$.data.operatorName").value(operatorName))
            .andExpect(jsonPath("$.data.specimenRemovalAt").isNotEmpty());

        assertThat(querySingleString(
            """
                select specimen_removal_operator_name
                from specimens
                where id = :specimenId
                """,
            "specimenId",
            specimenId)).isEqualTo(operatorName);
        assertThat(jdbcTemplate.queryForObject(
            """
                select count(1)
                from workflow_events
                where specimen_id = :specimenId
                  and node_code = 'REMOVAL'
                  and event_type = 'COMPLETED'
                  and event_status = 'SUCCESS'
                """,
            java.util.Map.of("specimenId", specimenId),
            Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
            """
                select count(1)
                from specimen_fixation_records
                where specimen_id = :specimenId
                  and verification_completed_at is not null
                  and verified_at is not null
                """,
            java.util.Map.of("specimenId", specimenId),
            Long.class)).isEqualTo(1L);

        mockMvc.perform(authorized(get("/api/v1/specimens"), USER_REGISTER)
                .param("page", "1")
                .param("size", "20")
                .param("keyword", barcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].barcode").value(barcode))
            .andExpect(jsonPath("$.data.items[0].specimenRemovalAt").isNotEmpty())
            .andExpect(jsonPath("$.data.items[0].verificationStatus").value("VERIFIED"));

        postJson("/api/v1/specimen-fixations/start", USER_FIXATION, """
            {
              "specimenBarcode": "%s",
              "fixationLiquidType": "FORMALIN",
              
              "terminalCode": "T-FIXATION"
            }
            """.formatted(barcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fixationStatus").value("FIXING"));
    }

    private void insertWorkbenchExtension(String applicationId, String inpatientNo, String surgeryName) {
        jdbcTemplate.update(
            """
                insert into application_registration_workbench (
                    application_id,
                    inpatient_no,
                    surgery_name
                ) values (
                    :applicationId,
                    :inpatientNo,
                    :surgeryName
                )
                """,
            java.util.Map.of(
                "applicationId", applicationId,
                "inpatientNo", inpatientNo,
                "surgeryName", surgeryName));
    }

    private void insertWorkbenchOperatingOption(String applicationId, String buildingId, String roomId) {
        jdbcTemplate.update(
            """
                insert into application_registration_workbench (
                    application_id,
                    building_id,
                    room_id
                ) values (
                    :applicationId,
                    :buildingId,
                    :roomId
                )
                """,
            java.util.Map.of(
                "applicationId", applicationId,
                "buildingId", buildingId,
                "roomId", roomId));
    }

    @Test
    void shouldQuickConfirmRemovalBySpecimenNo() throws Exception {
        String applicationId = createApplication("APP-M2-REMOVAL-NO-001");
        JsonNode registration = registerSpecimens(
            applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-REMOVAL-NO-001");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();
        String specimenNo = "SN-REMOVAL-NO-UNIQUE-001";

        jdbcTemplate.update(
            """
                update specimens
                set specimen_no = :specimenNo
                where barcode = :barcode
                """,
            java.util.Map.of("specimenNo", specimenNo, "barcode", barcode));

        mockMvc.perform(authorized(get("/api/v1/specimens"), USER_REGISTER)
                .param("page", "1")
                .param("size", "20")
                .param("keyword", specimenNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].barcode").value(barcode))
            .andExpect(jsonPath("$.data.items[0].specimenRemovalAt").isEmpty());

        postJson("/api/v1/specimen-removals/confirm-by-identifier", USER_FIXATION, """
            {
              "identifierType": "SPECIMEN_NO",
              "identifier": "%s",
              
              "terminalCode": "T-REMOVAL-NO",
              "remarks": "离体确认"
            }
            """.formatted(specimenNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.barcode").value(barcode))
            .andExpect(jsonPath("$.data.specimenRemovalAt").isNotEmpty());
    }

    @Test
    void shouldRejectQuickConfirmWhenSpecimenNoMatchesMultipleRecords() throws Exception {
        String applicationId1 = createApplication("APP-M2-REMOVAL-DUP-001");
        JsonNode registration1 = registerSpecimens(
            applicationId1, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-REMOVAL-DUP-001");
        String specimenNo = registration1.path("specimens").get(0).path("specimenNo").asText();

        String applicationId2 = createApplication("APP-M2-REMOVAL-DUP-002");
        JsonNode registration2 = registerSpecimens(
            applicationId2, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-REMOVAL-DUP-002");
        String secondBarcode = registration2.path("specimens").get(0).path("barcode").asText();
        String secondSpecimenNo = registration2.path("specimens").get(0).path("specimenNo").asText();

        jdbcTemplate.getJdbcTemplate().execute("alter table specimens drop constraint uk_specimens_specimen_no");
        try {
            jdbcTemplate.update(
                """
                    update specimens
                    set specimen_no = :specimenNo
                    where barcode = :barcode
                    """,
                java.util.Map.of("specimenNo", specimenNo, "barcode", secondBarcode));

            postJson("/api/v1/specimen-removals/confirm-by-identifier", USER_FIXATION, """
                {
                  "identifierType": "SPECIMEN_NO",
                  "identifier": "%s"}
                """.formatted(specimenNo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        } finally {
            jdbcTemplate.update(
                """
                    update specimens
                    set specimen_no = :specimenNo
                    where barcode = :barcode
                    """,
                java.util.Map.of("specimenNo", secondSpecimenNo, "barcode", secondBarcode));
            jdbcTemplate.getJdbcTemplate()
                .execute("alter table specimens add constraint uk_specimens_specimen_no unique (specimen_no)");
        }
    }

    @Test
    void shouldRejectQuickConfirmWhenSpecimenAlreadyConfirmed() throws Exception {
        String applicationId = createApplication("APP-M2-REMOVAL-CONFLICT-001");
        JsonNode registration = registerSpecimens(
            applicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-REMOVAL-CONFLICT-001");
        String barcode = registration.path("specimens").get(0).path("barcode").asText();

        postJson("/api/v1/specimen-removals/confirm-by-identifier", USER_FIXATION, """
            {
              "identifierType": "BARCODE",
              "identifier": "%s"}
            """.formatted(barcode))
            .andExpect(status().isOk());

        postJson("/api/v1/specimen-removals/confirm-by-identifier", USER_FIXATION, """
            {
              "identifierType": "BARCODE",
              "identifier": "%s"}
            """.formatted(barcode))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
    }

    @Test
    void shouldRejectQuickConfirmWhenIdentifierNotFound() throws Exception {
        postJson("/api/v1/specimen-removals/confirm-by-identifier", USER_FIXATION, """
            {
              "identifierType": "BARCODE",
              "identifier": "BC-NOT-FOUND"}
            """)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldRetryFailedLabelPrintAndPersistStatus() throws Exception {
        String applicationId = createApplication("APP-M2-PRINT-001");
        JsonNode registration = registerSpecimens(applicationId, USER_REGISTER, "FAIL", "/api/v1/specimens/register", "BC-PRINT-001");
        String batchNo = registration.path("labelPrintBatchNo").asText();

        assertThat(registration.path("labelPrintSuccess").asBoolean()).isFalse();
        assertThat(registration.path("specimens").get(0).path("labelPrintStatus").asText()).isEqualTo("FAILED");

        postJson("/api/v1/specimens/label-batches/%s/retry".formatted(batchNo), USER_REGISTER, """
            {
              
              "printerCode": "P-01",
              "terminalCode": "OR-RETRY"
            }
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.labelPrintBatchNo").value(batchNo))
            .andExpect(jsonPath("$.data.retriedCount").value(1))
            .andExpect(jsonPath("$.data.successCount").value(1))
            .andExpect(jsonPath("$.data.failedCount").value(0))
            .andExpect(jsonPath("$.data.allSuccessful").value(true));

        mockMvc.perform(authorized(get("/api/v1/applications/{id}", applicationId), USER_TRACKING))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.specimens[0].labelPrintStatus").value("SUCCESS"));
    }

    @Test
    void shouldRejectRegistrationWhenApplicationAlreadyInTransitOrClosedByReceipt() throws Exception {
        String inTransitApplicationId = createApplication("APP-M2-STATUS-INTRANSIT-001");
        String inTransitBarcode = registerSpecimens(inTransitApplicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-STATUS-INTRANSIT-001")
            .path("specimens").get(0).path("barcode").asText();
        prepareTransportReadySpecimen(inTransitBarcode);
        String inTransitOrderId = createTransportOrder(inTransitApplicationId, inTransitBarcode).path("id").asText();
        postJson("/api/v1/transport-orders/%s/handover".formatted(inTransitOrderId), USER_TRANSPORT, """
            {
              "receiverUserName": "receiver-in-transit",
              "terminalCode": "T-IN-TRANSIT"
            }
            """)
            .andExpect(status().isOk());

        postJson("/api/v1/specimens/register", USER_REGISTER, registerSpecimenPayload(
            inTransitApplicationId, "P-01", "BC-STATUS-INTRANSIT-002"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));

        String receivedApplicationId = createApplication("APP-M2-STATUS-RECEIVED-001");
        String receivedBarcode = registerSpecimens(receivedApplicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-STATUS-RECEIVED-001")
            .path("specimens").get(0).path("barcode").asText();
        prepareTransportReadySpecimen(receivedBarcode);
        String receivedOrderId = createTransportOrder(receivedApplicationId, receivedBarcode).path("id").asText();
        postJson("/api/v1/transport-orders/%s/handover".formatted(receivedOrderId), USER_TRANSPORT, """
            {
              "receiverUserName": "receiver-complete",
              "terminalCode": "T-RECEIVED"
            }
            """)
            .andExpect(status().isOk());
        postJson("/api/v1/specimen-receipts", USER_RECEIVE, """
            {
              "transportOrderId": "%s",
              "receivedByName": "receiver-complete",
              "terminalCode": "T-RECEIVED",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1,
                  "qualityCheckResult": "PASSED"
                }
              ]
            }
            """.formatted(receivedOrderId, receivedBarcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.receiptStatus").value("RECEIVED"));

        postJson("/api/v1/specimens/register", USER_REGISTER, registerSpecimenPayload(
            receivedApplicationId, "P-01", "BC-STATUS-RECEIVED-002"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));

        String partialApplicationId = createApplication("APP-M2-STATUS-PARTIAL-001");
        JsonNode partialRegistration = registerSpecimens(
            partialApplicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-STATUS-PARTIAL-001", "BC-STATUS-PARTIAL-002");
        String partialBarcode1 = partialRegistration.path("specimens").get(0).path("barcode").asText();
        String partialBarcode2 = partialRegistration.path("specimens").get(1).path("barcode").asText();
        prepareTransportReadySpecimen(partialBarcode1);
        prepareTransportReadySpecimen(partialBarcode2);
        String partialOrderId = createTransportOrder(partialApplicationId, partialBarcode1, partialBarcode2).path("id").asText();
        postJson("/api/v1/transport-orders/%s/handover".formatted(partialOrderId), USER_TRANSPORT, """
            {
              "receiverUserName": "receiver-partial",
              "terminalCode": "T-PARTIAL"
            }
            """)
            .andExpect(status().isOk());
        postJson("/api/v1/specimen-receipts", USER_RECEIVE, """
            {
              "transportOrderId": "%s",
              "receivedByName": "receiver-partial",
              "terminalCode": "T-PARTIAL",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "RECEIVED",
                  "containerCount": 1,
                  "qualityCheckResult": "PASSED"
                },
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "REJECTED",
                  "containerCount": 1,
                  "qualityCheckResult": "FAILED",
                  "qualityIssueCodes": ["PARTIAL_REJECT"],
                  "reason": "partial-reject"
                }
              ]
            }
            """.formatted(partialOrderId, partialBarcode1, partialBarcode2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.receiptStatus").value("PARTIALLY_RECEIVED"));

        postJson("/api/v1/specimens/register", USER_REGISTER, registerSpecimenPayload(
            partialApplicationId, "P-01", "BC-STATUS-PARTIAL-003"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));

        String rejectedApplicationId = createApplication("APP-M2-STATUS-REJECTED-001");
        String rejectedBarcode = registerSpecimens(rejectedApplicationId, USER_REGISTER, "P-01", "/api/v1/specimens/register", "BC-STATUS-REJECTED-001")
            .path("specimens").get(0).path("barcode").asText();
        prepareTransportReadySpecimen(rejectedBarcode);
        String rejectedOrderId = createTransportOrder(rejectedApplicationId, rejectedBarcode).path("id").asText();
        postJson("/api/v1/transport-orders/%s/handover".formatted(rejectedOrderId), USER_TRANSPORT, """
            {
              "receiverUserName": "receiver-rejected",
              "terminalCode": "T-REJECTED"
            }
            """)
            .andExpect(status().isOk());
        postJson("/api/v1/specimen-receipts", USER_RECEIVE, """
            {
              "transportOrderId": "%s",
              "receivedByName": "receiver-rejected",
              "terminalCode": "T-REJECTED",
              "items": [
                {
                  "specimenBarcode": "%s",
                  "receiptStatus": "REJECTED",
                  "containerCount": 1,
                  "qualityCheckResult": "FAILED",
                  "qualityIssueCodes": ["FULL_REJECT"],
                  "reason": "fully-rejected"
                }
              ]
            }
            """.formatted(rejectedOrderId, rejectedBarcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.receiptStatus").value("REJECTED"));

        postJson("/api/v1/specimens/register", USER_REGISTER, registerSpecimenPayload(
            rejectedApplicationId, "P-01", "BC-STATUS-REJECTED-002"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));
    }

    @Test
    void shouldReturnPlaceholderErrorWhenClinicalImportUnavailable() throws Exception {
        postJson("/api/v1/clinical-applications/import", USER_IMPORT, """
            {
              "thirdPartySource": "HIS",
              "externalOrderNo": "REAL-001"
            }
            """)
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("EXTERNAL_INTEGRATION_UNAVAILABLE"));
    }

    @Test
    void shouldSupportSpecimenCollectionAlias() throws Exception {
        String applicationId = createApplication("APP-M2-ALIAS-001");
        postJson("/api/v1/specimen-collections", USER_REGISTER, registerSpecimenPayload(applicationId, "P-01", "BC-ALIAS-001"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.specimens[0].barcode").value("BC-ALIAS-001"))
            .andExpect(jsonPath("$.data.specimens[0].labelPrintStatus").value("SUCCESS"));
    }
}
