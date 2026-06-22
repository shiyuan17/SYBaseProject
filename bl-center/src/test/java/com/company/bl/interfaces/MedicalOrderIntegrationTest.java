package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class MedicalOrderIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    @Test
    void shouldCompleteTechnicalMedicalOrderAndExposeStatusInWorkbenchAndTracking() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-ORDER-001", "BC-M4-ORDER-001");
        MedicalOrderTargetSnapshot targetSnapshot = queryMedicalOrderTargetSnapshot(context.caseId());

        JsonNode created = responseBody(postJson("/api/v1/medical-orders", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "orderType":"RE_STAIN",
              "orderContent":"restain with HE",
              "targetType":"SLIDE",
              "targetSpecimenId":"%s",
              "targetSpecimenNo":"%s",
              "targetBlockId":"%s",
              "targetBlockNo":"%s",
              "targetSlideId":"%s",
              "targetSlideNo":"%s",
              "terminalCode":"M4-ORD-01"
            }
            """.formatted(
            context.caseId(),
            targetSnapshot.specimenId(),
            targetSnapshot.specimenNo(),
            targetSnapshot.blockId(),
            targetSnapshot.blockNo(),
            targetSnapshot.slideId(),
            targetSnapshot.slideNo())), 200);
        String orderId = created.path("orderId").asText();
        assertThat(created.path("orderNumber").asText()).startsWith("MO-");
        assertThat(created.path("status").asText()).isEqualTo("PENDING");

        JsonNode pending = responseBody(mockMvc.perform(authorized(get("/api/v1/medical-orders/pending"), USER_M4_ORDER_EXECUTE)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", context.pathologyNo())), 200);
        assertThat(pending.path("total").asInt()).isEqualTo(1);
        JsonNode pendingItem = pending.path("items").get(0);
        assertThat(pendingItem.path("targetType").asText()).isEqualTo("SLIDE");
        assertThat(pendingItem.path("specimenNo").asText()).isEqualTo(targetSnapshot.specimenNo());
        assertThat(pendingItem.path("blockNo").asText()).isEqualTo(targetSnapshot.blockNo());
        assertThat(pendingItem.path("slideNo").asText()).isEqualTo(targetSnapshot.slideNo());
        assertThat(pendingItem.path("canConfirm").asBoolean()).isTrue();
        assertThat(pendingItem.path("canPrint").asBoolean()).isFalse();
        assertThat(pendingItem.path("canRelease").asBoolean()).isFalse();
        assertThat(pendingItem.path("canTerminate").asBoolean()).isFalse();
        assertThat(pendingItem.path("canQc").asBoolean()).isFalse();

        postJson("/api/v1/medical-orders/%s/accept".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-02"}
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        JsonNode printed = responseBody(postJson("/api/v1/medical-orders/%s/print-slide".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-PRINT-01","remarks":"print labels"}
            """), 200);
        assertThat(printed.path("orderId").asText()).isEqualTo(orderId);
        assertThat(printed.path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(printed.path("printedAt").asText()).isNotBlank();
        assertThat(printed.path("labels").isArray()).isTrue();
        assertThat(printed.path("labels").get(0).path("slideNo").asText()).isEqualTo(targetSnapshot.slideNo());

        JsonNode workbenchAfterAccept = diagnosticWorkbench(context.caseId(), USER_M4_DIAGNOSIS);
        assertThat(workbenchAfterAccept.path("medicalOrders").get(0).path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(workbenchAfterAccept.path("medicalOrders").get(0).path("printedAt").asText()).isNotBlank();

        JsonNode inProgressAfterPrint = responseBody(mockMvc.perform(authorized(get("/api/v1/medical-orders/pending"), USER_M4_ORDER_EXECUTE)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", context.pathologyNo())), 200);
        JsonNode printedItem = inProgressAfterPrint.path("items").get(0);
        assertThat(printedItem.path("canPrint").asBoolean()).isFalse();
        assertThat(printedItem.path("canRelease").asBoolean()).isTrue();
        assertThat(printedItem.path("canTerminate").asBoolean()).isTrue();
        assertThat(printedItem.path("canQc").asBoolean()).isTrue();
        assertThat(printedItem.path("printedByName").asText()).isEqualTo(userDisplayName(USER_M4_ORDER_EXECUTE));
        assertThat(printedItem.path("printedAt").asText()).isNotBlank();

        postJson("/api/v1/medical-orders/%s/complete".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-03","remarks":"done"}
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        JsonNode completed = responseBody(mockMvc.perform(authorized(get("/api/v1/medical-orders/pending"), USER_M4_ORDER_EXECUTE)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", context.pathologyNo())
            .param("status", "COMPLETED")), 200);
        JsonNode completedItem = completed.path("items").get(0);
        assertThat(completedItem.path("releasedByName").asText()).isEqualTo(userDisplayName(USER_M4_ORDER_EXECUTE));
        assertThat(completedItem.path("releasedAt").asText()).isNotBlank();
        assertThat(completedItem.path("canRelease").asBoolean()).isFalse();

        JsonNode tracking = reportTracking(context.caseId(), USER_M4_TRACKING);
        assertThat(tracking.path("medicalOrders").get(0).path("status").asText()).isEqualTo("COMPLETED");
        assertThat(tracking.path("medicalOrders").get(0).path("releasedAt").asText()).isNotBlank();
        assertThat(tracking.toString()).contains("MEDICAL_ORDER_COMPLETE");
    }

    @Test
    void shouldRejectCompletingMedicalOrderBeforeSlidePrinted() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-ORDER-NOPRINT", "BC-M4-ORDER-NOPRINT");
        JsonNode created = createMedicalOrderWithTarget(context.caseId());
        String orderId = created.path("orderId").asText();

        postJson("/api/v1/medical-orders/%s/accept".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-NOPRINT-A"}
            """).andExpect(status().isOk());

        postJson("/api/v1/medical-orders/%s/complete".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-NOPRINT-C","remarks":"should fail"}
            """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("printed")));
    }

    @Test
    void shouldPrintAcceptedMedicalOrderWithoutTargetSnapshot() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-ORDER-LEGACY-PRINT", "BC-M4-ORDER-LEGACY-PRINT");
        JsonNode created = createMedicalOrder(context.caseId(), "ODI_CGRS_HE_STAIN", "ROUTINE", "legacy print");
        String orderId = created.path("orderId").asText();

        postJson("/api/v1/medical-orders/%s/accept".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-LEGACY-PRINT-A"}
            """).andExpect(status().isOk());

        JsonNode pendingAfterAccept = responseBody(mockMvc.perform(authorized(get("/api/v1/medical-orders/pending"), USER_M4_ORDER_EXECUTE)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", context.pathologyNo())), 200);
        JsonNode acceptedItem = pendingAfterAccept.path("items").get(0);
        assertThat(acceptedItem.path("targetType").isNull()).isTrue();
        assertThat(acceptedItem.path("targetSlideId").isNull()).isTrue();
        assertThat(acceptedItem.path("canPrint").asBoolean()).isTrue();

        JsonNode printed = responseBody(postJson("/api/v1/medical-orders/%s/print-slide".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-LEGACY-PRINT-P","remarks":"legacy print"}
            """), 200);
        JsonNode printLabel = printed.path("labels").get(0);
        assertThat(printed.path("printedAt").asText()).isNotBlank();
        assertThat(printed.path("printedByName").asText()).isEqualTo(userDisplayName(USER_M4_ORDER_EXECUTE));
        assertThat(printLabel.path("slideNo").isNull()).isTrue();
        assertThat(printLabel.path("blockNo").isNull()).isTrue();
        assertThat(printLabel.path("specimenNo").isNull()).isTrue();

        JsonNode pendingAfterPrint = responseBody(mockMvc.perform(authorized(get("/api/v1/medical-orders/pending"), USER_M4_ORDER_EXECUTE)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", context.pathologyNo())), 200);
        JsonNode printedItem = pendingAfterPrint.path("items").get(0);
        assertThat(printedItem.path("printedAt").asText()).isNotBlank();
        assertThat(printedItem.path("printedByName").asText()).isEqualTo(userDisplayName(USER_M4_ORDER_EXECUTE));
        assertThat(printedItem.path("canPrint").asBoolean()).isFalse();
        assertThat(printedItem.path("canRelease").asBoolean()).isTrue();

        postJson("/api/v1/medical-orders/%s/print-slide".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-LEGACY-PRINT-RETRY"}
            """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("cannot be printed")));
    }

    @Test
    void shouldPrintRoutineMedicalOrderWithoutTargetSnapshotAndRejectDuplicatePrint() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-ORDER-NOTARGET", "BC-M4-ORDER-NOTARGET");
        JsonNode created = createMedicalOrder(context.caseId(), "ODI_CGRS_HE_STAIN", "ROUTINE", "HE补片");
        String orderId = created.path("orderId").asText();

        postJson("/api/v1/medical-orders/%s/accept".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-NOTARGET-A"}
            """).andExpect(status().isOk());

        JsonNode inProgressPending = responseBody(mockMvc.perform(authorized(get("/api/v1/medical-orders/pending"), USER_M4_ORDER_EXECUTE)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", context.pathologyNo())), 200);
        JsonNode inProgressItem = inProgressPending.path("items").get(0);
        assertThat(inProgressItem.path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(inProgressItem.path("canPrint").asBoolean()).isTrue();
        assertThat(inProgressItem.path("canQc").asBoolean()).isFalse();

        JsonNode printed = responseBody(postJson("/api/v1/medical-orders/%s/print-slide".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-NOTARGET-P","remarks":"print without target snapshot"}
            """), 200);
        assertThat(printed.path("orderId").asText()).isEqualTo(orderId);
        assertThat(printed.path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(printed.path("printedAt").asText()).isNotBlank();
        assertThat(printed.path("printedByName").asText()).isEqualTo(userDisplayName(USER_M4_ORDER_EXECUTE));
        assertThat(printed.path("labels")).hasSize(1);
        assertThat(printed.path("labels").get(0).path("slideId").isNull()).isTrue();
        assertThat(printed.path("labels").get(0).path("slideNo").isNull()).isTrue();
        assertThat(printed.path("labels").get(0).path("specimenNo").isNull()).isTrue();
        assertThat(printed.path("labels").get(0).path("blockNo").isNull()).isTrue();

        JsonNode printedPending = responseBody(mockMvc.perform(authorized(get("/api/v1/medical-orders/pending"), USER_M4_ORDER_EXECUTE)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", context.pathologyNo())), 200);
        JsonNode printedItem = printedPending.path("items").get(0);
        assertThat(printedItem.path("canPrint").asBoolean()).isFalse();
        assertThat(printedItem.path("canQc").asBoolean()).isFalse();
        assertThat(printedItem.path("printedAt").asText()).isNotBlank();
        assertThat(printedItem.path("printedByName").asText()).isEqualTo(userDisplayName(USER_M4_ORDER_EXECUTE));

        Map<String, Object> persisted = namedParameterJdbcTemplate.queryForMap("""
            select printed_at, printed_by_name
            from medical_orders
            where id = :orderId
            """, Map.of("orderId", orderId));
        assertThat(persisted.get("printed_at")).isNotNull();
        assertThat(persisted.get("printed_by_name")).isEqualTo(userDisplayName(USER_M4_ORDER_EXECUTE));

        postJson("/api/v1/medical-orders/%s/print-slide".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-NOTARGET-P2","remarks":"duplicate print"}
            """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("cannot be printed")));
    }

    @Test
    void shouldFallbackPrintPayloadFieldsWhenTargetSnapshotNumbersMissing() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-ORDER-FALLBACK", "BC-M4-ORDER-FALLBACK");
        MedicalOrderTargetSnapshot targetSnapshot = queryMedicalOrderTargetSnapshot(context.caseId());

        JsonNode created = responseBody(postJson("/api/v1/medical-orders", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "orderType":"RE_STAIN",
              "orderContent":"restain with fallback fields",
              "targetType":"SLIDE",
              "targetSpecimenId":"%s",
              "targetBlockId":"%s",
              "targetSlideId":"%s",
              "terminalCode":"M4-ORD-FALLBACK"
            }
            """.formatted(
            context.caseId(),
            targetSnapshot.specimenId(),
            targetSnapshot.blockId(),
            targetSnapshot.slideId())), 200);
        String orderId = created.path("orderId").asText();

        postJson("/api/v1/medical-orders/%s/accept".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-FALLBACK-A"}
            """).andExpect(status().isOk());

        JsonNode printed = responseBody(postJson("/api/v1/medical-orders/%s/print-slide".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-FALLBACK-P","remarks":"fallback label values"}
            """), 200);

        JsonNode label = printed.path("labels").get(0);
        assertThat(label.path("slideId").asText()).isEqualTo(targetSnapshot.slideId());
        assertThat(label.path("slideNo").asText()).isEqualTo(targetSnapshot.slideNo());
        assertThat(label.path("specimenNo").asText()).isEqualTo(targetSnapshot.specimenNo());
        assertThat(label.path("blockNo").asText()).isEqualTo(targetSnapshot.blockNo());
    }

    @Test
    void shouldTerminateInProgressMedicalOrderAndPersistTerminationFields() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-ORDER-TERM", "BC-M4-ORDER-TERM");
        JsonNode created = createMedicalOrderWithTarget(context.caseId());
        String orderId = created.path("orderId").asText();

        postJson("/api/v1/medical-orders/%s/accept".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-TERM-A"}
            """).andExpect(status().isOk());

        postJson("/api/v1/medical-orders/%s/terminate".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {
              "terminalCode":"M4-ORD-TERM-INVALID",
              "terminationReasonCode":"OTHER",
              "terminationReasonLabel":"其他"
            }
            """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("remarks")));

        responseBody(postJson("/api/v1/medical-orders/%s/terminate".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {
              "terminalCode":"M4-ORD-TERM-OK",
              "terminationReasonCode":"BLOCK_DAMAGED",
              "terminationReasonLabel":"蜡块已损坏无法使用",
              "remarks":"block broken during handling"
            }
            """), 200);

        JsonNode terminated = responseBody(mockMvc.perform(authorized(get("/api/v1/medical-orders/pending"), USER_M4_ORDER_EXECUTE)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", context.pathologyNo())
            .param("status", "TERMINATED")), 200);
        JsonNode terminatedItem = terminated.path("items").get(0);
        assertThat(terminatedItem.path("status").asText()).isEqualTo("TERMINATED");
        assertThat(terminatedItem.path("terminatedByName").asText()).isEqualTo(userDisplayName(USER_M4_ORDER_EXECUTE));
        assertThat(terminatedItem.path("terminationReasonCode").asText()).isEqualTo("BLOCK_DAMAGED");
        assertThat(terminatedItem.path("terminationReasonLabel").asText()).isEqualTo("蜡块已损坏无法使用");
        assertThat(terminatedItem.path("terminationRemarks").asText()).isEqualTo("block broken during handling");
        assertThat(terminatedItem.path("canPrint").asBoolean()).isFalse();
        assertThat(terminatedItem.path("canRelease").asBoolean()).isFalse();
        assertThat(terminatedItem.path("canQc").asBoolean()).isFalse();
    }

    @Test
    void shouldCreateQcEvaluationAndReworkOrderForMedicalOrder() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-ORDER-QC", "BC-M4-ORDER-QC");
        MedicalOrderTargetSnapshot targetSnapshot = queryMedicalOrderTargetSnapshot(context.caseId());
        JsonNode created = createMedicalOrderWithTarget(context.caseId());
        String orderId = created.path("orderId").asText();

        postJson("/api/v1/medical-orders/%s/accept".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-QC-A"}
            """).andExpect(status().isOk());
        postJson("/api/v1/medical-orders/%s/print-slide".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-QC-P"}
            """).andExpect(status().isOk());

        JsonNode evaluation = responseBody(postJson("/api/v1/medical-orders/%s/qc-evaluations".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {
              "qcAspect":"SLIDE",
              "totalScore":82,
              "grade":"乙",
              "evaluationReason":"染色不均;组织皱褶",
              "processingAction":"REWORK_URGENT",
              "remarks":"need remake",
              "detailPayload":[
                {
                  "code":"STAIN_UNEVEN",
                  "label":"染色不均",
                  "group":"染色质量",
                  "suggestedDeduction":10,
                  "deduction":10
                },
                {
                  "code":"TISSUE_FOLD",
                  "label":"组织皱褶",
                  "group":"制片质量",
                  "suggestedDeduction":8,
                  "deduction":8
                }
              ],
              "terminalCode":"M4-ORD-QC-01"
            }
            """), 200);
        assertThat(evaluation.path("orderId").asText()).isEqualTo(orderId);
        assertThat(evaluation.path("qcAspect").asText()).isEqualTo("SLIDE");
        assertThat(evaluation.path("reworkOrderId").asText()).isNotBlank();

        JsonNode latest = responseBody(mockMvc.perform(authorized(
            get("/api/v1/medical-orders/{id}/qc-evaluations/latest", orderId),
            USER_M4_ORDER_EXECUTE)), 200);
        assertThat(latest.path("qcAspect").asText()).isEqualTo("SLIDE");
        assertThat(latest.path("grade").asText()).isEqualTo("乙");
        assertThat(latest.path("totalScore").asInt()).isEqualTo(82);
        assertThat(latest.path("processingAction").asText()).isEqualTo("REWORK_URGENT");
        assertThat(latest.path("detailPayload").isArray()).isTrue();

        Integer evaluationCount = namedParameterJdbcTemplate.queryForObject("""
            select count(1)
            from medical_order_qc_evaluations
            where order_id = :orderId
            """, Map.of("orderId", orderId), Integer.class);
        assertThat(evaluationCount).isEqualTo(1);

        Integer reworkCount = namedParameterJdbcTemplate.queryForObject("""
            select count(1)
            from rework_orders
            where case_id = :caseId
              and slide_id = :slideId
              and rework_type = 'RESLICE'
              and remarks like '%URGENT%'
            """, Map.of(
            "caseId", context.caseId(),
            "slideId", targetSnapshot.slideId()), Integer.class);
        assertThat(reworkCount).isEqualTo(1);
    }

    @Test
    void shouldCancelPendingMedicalOrderOnly() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-ORDER-002", "BC-M4-ORDER-002");

        JsonNode created = responseBody(postJson("/api/v1/medical-orders", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "orderType":"DEEP_CUT",
              "orderContent":"deep cut required",
              "terminalCode":"M4-ORD-11"
            }
            """.formatted(context.caseId())), 200);
        String orderId = created.path("orderId").asText();

        postJson("/api/v1/medical-orders/%s/cancel".formatted(orderId), USER_M4_DIAGNOSIS, """
            {"terminalCode":"M4-ORD-12","remarks":"not needed"}
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        JsonNode tracking = reportTracking(context.caseId(), USER_M4_TRACKING);
        assertThat(tracking.path("medicalOrders").get(0).path("status").asText()).isEqualTo("CANCELLED");
    }

    @Test
    void shouldSnapshotOrderDictionaryItemAndFilterBySingleCategory() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-ORDER-IHC", "BC-M4-ORDER-IHC");

        responseBody(postJson("/api/v1/medical-orders", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "orderType":"SPECIAL",
              "orderContent":"CK",
              "orderItemId":"ODI_IHC_CK",
              "terminalCode":"M4-ORD-IHC"
            }
            """.formatted(context.caseId())), 200);

        JsonNode pending = responseBody(mockMvc.perform(authorized(get("/api/v1/medical-orders/pending"), USER_M4_ORDER_EXECUTE)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", context.pathologyNo())
            .param("orderCategoryCode", "IHC")), 200);

        assertThat(pending.path("total").asInt()).isEqualTo(1);
        JsonNode item = pending.path("items").get(0);
        assertThat(item.path("orderItemId").asText()).isEqualTo("ODI_IHC_CK");
        assertThat(item.path("orderItemCode").asText()).isEqualTo("IHC_CK");
        assertThat(item.path("orderItemName").asText()).isEqualTo("CK");
        assertThat(item.path("orderCategoryCode").asText()).isEqualTo("IHC");
        assertThat(item.path("orderCategoryName").asText()).isEqualTo("免疫组化");
    }

    @Test
    void shouldFilterPendingOrdersByMultipleCategoriesAndDefaultStatuses() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-ORDER-MULTI", "BC-M4-ORDER-MULTI");
        JsonNode examOrder = createMedicalOrderWithTarget(context.caseId(), "ODI_EXAM_DECALCIFICATION", "ROUTINE", "脱钙");
        createMedicalOrder(context.caseId(), "ODI_CGRS_HE_STAIN", "ROUTINE", "HE染色");
        createMedicalOrder(context.caseId(), "ODI_TSRS_PAS", "SPECIAL", "PAS染色");

        postJson("/api/v1/medical-orders/%s/accept".formatted(examOrder.path("orderId").asText()), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-MULTI-A"}
            """).andExpect(status().isOk());
        postJson("/api/v1/medical-orders/%s/print-slide".formatted(examOrder.path("orderId").asText()), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-MULTI-P"}
            """).andExpect(status().isOk());
        postJson("/api/v1/medical-orders/%s/complete".formatted(examOrder.path("orderId").asText()), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-MULTI-C","remarks":"done"}
            """).andExpect(status().isOk());

        JsonNode routinePending = responseBody(mockMvc.perform(authorized(get("/api/v1/medical-orders/pending"), USER_M4_ORDER_EXECUTE)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", context.pathologyNo())
            .param("orderCategoryCode", "EXAM,CGRS,BLOCK,QP")), 200);
        assertThat(routinePending.path("total").asInt()).isEqualTo(1);
        assertThat(routinePending.path("items").get(0).path("orderCategoryCode").asText()).isEqualTo("CGRS");

        JsonNode specialPending = responseBody(mockMvc.perform(authorized(get("/api/v1/medical-orders/pending"), USER_M4_ORDER_EXECUTE)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", context.pathologyNo())
            .param("orderCategoryCode", "TSRS")), 200);
        assertThat(specialPending.path("total").asInt()).isEqualTo(1);
        assertThat(specialPending.path("items").get(0).path("orderCategoryCode").asText()).isEqualTo("TSRS");
    }

    @Test
    void shouldMatchLegacyOrdersByConservativeFallbackWhenSnapshotMissing() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-ORDER-LEGACY", "BC-M4-ORDER-LEGACY");
        LocalDateTime now = LocalDateTime.now();
        namedParameterJdbcTemplate.update("""
            insert into medical_orders
                (id, case_id, order_number, order_content, order_type, execution_scope, billing_status, status,
                 doctor_user_id, doctor_name, order_date, remarks, created_at, updated_at)
            values
                (:id, :caseId, :orderNumber, :orderContent, :orderType, 'TECHNICIAN', 'PENDING', 'PENDING',
                 :doctorUserId, :doctorName, :orderDate, :remarks, :createdAt, :updatedAt)
            """, Map.ofEntries(
            Map.entry("id", "MO-LEGACY-IHC"),
            Map.entry("caseId", context.caseId()),
            Map.entry("orderNumber", "MO-LEGACY-IHC"),
            Map.entry("orderContent", "legacy ihc"),
            Map.entry("orderType", "IHC"),
            Map.entry("doctorUserId", USER_M4_DIAGNOSIS),
            Map.entry("doctorName", "M4 Diagnosis"),
            Map.entry("orderDate", now),
            Map.entry("remarks", "legacy"),
            Map.entry("createdAt", now),
            Map.entry("updatedAt", now)));

        JsonNode pending = responseBody(mockMvc.perform(authorized(get("/api/v1/medical-orders/pending"), USER_M4_ORDER_EXECUTE)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", context.pathologyNo())
            .param("orderCategoryCode", "IHC")), 200);

        assertThat(pending.path("total").asInt()).isEqualTo(1);
        assertThat(pending.path("items").get(0).path("orderId").asText()).isEqualTo("MO-LEGACY-IHC");
        assertThat(pending.path("items").get(0).path("orderCategoryCode").isMissingNode()).isFalse();
        assertThat(pending.path("items").get(0).path("orderCategoryCode").isNull()).isTrue();
        assertThat(pending.path("items").get(0).path("targetType").isNull()).isTrue();
        assertThat(pending.path("items").get(0).path("canConfirm").asBoolean()).isTrue();
        assertThat(pending.path("items").get(0).path("canPrint").asBoolean()).isFalse();
        assertThat(pending.path("items").get(0).path("canRelease").asBoolean()).isFalse();
        assertThat(pending.path("items").get(0).path("canQc").asBoolean()).isFalse();
    }

    @Test
    void shouldRejectLegacyOperatorFieldsOnMedicalOrderRequests() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-ORDER-003", "BC-M4-ORDER-003");

        postJson("/api/v1/medical-orders", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "orderType":"RE_STAIN",
              "orderContent":"legacy operator fields should fail",
              "operatorUserId":"FORGED-USER",
              "operatorName":"forged-user",
              "terminalCode":"M4-ORD-21"
            }
            """.formatted(context.caseId()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("operatorUserId")));
    }

    @Test
    void shouldFilterPendingMedicalOrdersByWorkDate() throws Exception {
        StartedDiagnosticContext context = prepareStartedDiagnosticCase("APP-M4-ORDER-DATE-001", "BC-M4-ORDER-DATE-001");

        JsonNode todayOrder = createMedicalOrder(context.caseId(), "ODI_CGRS_HE_STAIN", "ROUTINE", "today order");
        JsonNode yesterdayOrder = createMedicalOrder(context.caseId(), "ODI_TSRS_PAS", "SPECIAL", "yesterday order");
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        namedParameterJdbcTemplate.update("""
            update medical_orders
            set order_date = :orderDate,
                created_at = :orderDate,
                updated_at = :orderDate
            where id = :orderId
            """, Map.of(
            "orderDate", yesterday.atTime(10, 30),
            "orderId", yesterdayOrder.path("orderId").asText()));

        JsonNode todayPending = responseBody(mockMvc.perform(authorized(get("/api/v1/medical-orders/pending"), USER_M4_ORDER_EXECUTE)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", context.pathologyNo())
            .param("workDate", today.toString())), 200);
        assertThat(todayPending.path("total").asInt()).isEqualTo(1);
        assertThat(todayPending.path("items").get(0).path("orderId").asText()).isEqualTo(todayOrder.path("orderId").asText());

        JsonNode yesterdayPending = responseBody(mockMvc.perform(authorized(get("/api/v1/medical-orders/pending"), USER_M4_ORDER_EXECUTE)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", context.pathologyNo())
            .param("workDate", yesterday.toString())), 200);
        assertThat(yesterdayPending.path("total").asInt()).isEqualTo(1);
        assertThat(yesterdayPending.path("items").get(0).path("orderId").asText()).isEqualTo(yesterdayOrder.path("orderId").asText());
    }

    private JsonNode createMedicalOrder(String caseId, String orderItemId, String orderType, String orderContent) throws Exception {
        return responseBody(postJson("/api/v1/medical-orders", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "orderType":"%s",
              "orderContent":"%s",
              "orderItemId":"%s",
              "terminalCode":"M4-ORD-DICT"
            }
            """.formatted(caseId, orderType, orderContent, orderItemId)), 200);
    }

    private JsonNode createMedicalOrderWithTarget(String caseId) throws Exception {
        return createMedicalOrderWithTarget(caseId, "ODI_CGRS_HE_STAIN", "ROUTINE", "HE补片");
    }

    private JsonNode createMedicalOrderWithTarget(String caseId, String orderItemId, String orderType, String orderContent) throws Exception {
        MedicalOrderTargetSnapshot targetSnapshot = queryMedicalOrderTargetSnapshot(caseId);
        return responseBody(postJson("/api/v1/medical-orders", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "orderType":"%s",
              "orderContent":"%s",
              "orderItemId":"%s",
              "targetType":"SLIDE",
              "targetSpecimenId":"%s",
              "targetSpecimenNo":"%s",
              "targetBlockId":"%s",
              "targetBlockNo":"%s",
              "targetSlideId":"%s",
              "targetSlideNo":"%s",
              "terminalCode":"M4-ORD-TARGET"
            }
            """.formatted(
            caseId,
            orderType,
            orderContent,
            orderItemId,
            targetSnapshot.specimenId(),
            targetSnapshot.specimenNo(),
            targetSnapshot.blockId(),
            targetSnapshot.blockNo(),
            targetSnapshot.slideId(),
            targetSnapshot.slideNo())), 200);
    }

    private MedicalOrderTargetSnapshot queryMedicalOrderTargetSnapshot(String caseId) {
        Map<String, Object> row = namedParameterJdbcTemplate.queryForMap("""
            select
                sp.id as specimen_id,
                sp.specimen_no,
                sb.id as block_id,
                sb.block_code,
                s.id as slide_id,
                s.slide_no
            from slides s
            join sampling_blocks sb on sb.id = s.sampling_block_id
            join specimens sp on sp.id = s.specimen_id
            where s.case_id = :caseId
            order by s.created_at desc
            limit 1
            """, Map.of("caseId", caseId));
        return new MedicalOrderTargetSnapshot(
            (String) row.get("specimen_id"),
            (String) row.get("specimen_no"),
            (String) row.get("block_id"),
            (String) row.get("block_code"),
            (String) row.get("slide_id"),
            (String) row.get("slide_no"));
    }

    private record MedicalOrderTargetSnapshot(
        String specimenId,
        String specimenNo,
        String blockId,
        String blockNo,
        String slideId,
        String slideNo
    ) {
    }
}
