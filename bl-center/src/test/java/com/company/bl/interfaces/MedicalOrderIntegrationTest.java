package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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

        JsonNode created = responseBody(postJson("/api/v1/medical-orders", USER_M4_DIAGNOSIS, """
            {
              "caseId":"%s",
              "orderType":"RE_STAIN",
              "orderContent":"restain with HE",
              "terminalCode":"M4-ORD-01"
            }
            """.formatted(context.caseId())), 200);
        String orderId = created.path("orderId").asText();
        assertThat(created.path("orderNumber").asText()).startsWith("MO-");
        assertThat(created.path("status").asText()).isEqualTo("PENDING");

        JsonNode pending = responseBody(mockMvc.perform(authorized(get("/api/v1/medical-orders/pending"), USER_M4_ORDER_EXECUTE)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", context.pathologyNo())), 200);
        assertThat(pending.path("total").asInt()).isEqualTo(1);

        postJson("/api/v1/medical-orders/%s/accept".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-02"}
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        JsonNode workbenchAfterAccept = diagnosticWorkbench(context.caseId(), USER_M4_DIAGNOSIS);
        assertThat(workbenchAfterAccept.path("medicalOrders").get(0).path("status").asText()).isEqualTo("IN_PROGRESS");

        postJson("/api/v1/medical-orders/%s/complete".formatted(orderId), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-03","remarks":"done"}
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        JsonNode tracking = reportTracking(context.caseId(), USER_M4_TRACKING);
        assertThat(tracking.path("medicalOrders").get(0).path("status").asText()).isEqualTo("COMPLETED");
        assertThat(tracking.toString()).contains("MEDICAL_ORDER_COMPLETE");
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
        JsonNode examOrder = createMedicalOrder(context.caseId(), "ODI_EXAM_DECALCIFICATION", "ROUTINE", "脱钙");
        createMedicalOrder(context.caseId(), "ODI_CGRS_HE_STAIN", "ROUTINE", "HE染色");
        createMedicalOrder(context.caseId(), "ODI_TSRS_PAS", "SPECIAL", "PAS染色");

        postJson("/api/v1/medical-orders/%s/accept".formatted(examOrder.path("orderId").asText()), USER_M4_ORDER_EXECUTE, """
            {"terminalCode":"M4-ORD-MULTI-A"}
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
}
