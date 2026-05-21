package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class ArchiveWorkflowIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    private static final String USER_M1_ARCHIVE = "USER_M1_ARCHIVE";

    @Test
    void shouldArchiveApplicationFormAndPhysicalMaterialsAndBackfillWorkbenchTracking() throws Exception {
        PublishedReportContext context = preparePublishedReportContext("APP-M5-ARCH-001", "BC-M5-ARCH-001");
        JsonNode cabinet = createArchiveCabinet("CAB-M5-A1");
        JsonNode positions = listAvailablePositions(cabinet.path("id").asText());

        String applicationPositionId = positions.get(0).path("id").asText();
        String applicationPositionCode = positions.get(0).path("positionCode").asText();
        String embeddingBoxPositionId = positions.get(1).path("id").asText();
        String embeddingBoxPositionCode = positions.get(1).path("positionCode").asText();
        String slidePositionId = positions.get(2).path("id").asText();
        String slidePositionCode = positions.get(2).path("positionCode").asText();

        String applicationId = queryApplicationId(context.caseId());
        Map<String, Object> embeddingBox = queryEmbeddingBox(context.caseId());
        Map<String, Object> slide = querySlide(context.caseId());

        responseBody(postJson("/api/v1/archive/application-forms", USER_M1_ARCHIVE, """
            {
              "caseId":"%s",
              "archivePositionId":"%s",
              "operatorName":"archive-user",
              "terminalCode":"M5-ARCH-APP-01",
              "fileUrl":"https://example.test/archive/app-form-001.jpg",
              "fileName":"app-form-001.jpg",
              "remarks":"paper form archived"
            }
            """.formatted(context.caseId(), applicationPositionId)), 200);

        responseBody(postJson("/api/v1/archive/embedding-boxes", USER_M1_ARCHIVE, """
            {
              "embeddingBoxId":"%s",
              "archivePositionId":"%s",
              "operatorName":"archive-user",
              "terminalCode":"M5-ARCH-BOX-01",
              "remarks":"embedding box archived"
            }
            """.formatted(embeddingBox.get("id"), embeddingBoxPositionId)), 200);

        responseBody(postJson("/api/v1/archive/slides", USER_M1_ARCHIVE, """
            {
              "slideId":"%s",
              "archivePositionId":"%s",
              "operatorName":"archive-user",
              "terminalCode":"M5-ARCH-SLIDE-01",
              "remarks":"slide archived"
            }
            """.formatted(slide.get("id"), slidePositionId)), 200);

        JsonNode boxRecords = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-records/search"), USER_M1_ARCHIVE)
            .param("keyword", String.valueOf(embeddingBox.get("embeddingBoxNo")))), 200);
        assertThat(boxRecords).hasSize(1);
        assertThat(boxRecords.get(0).path("objectType").asText()).isEqualTo("EMBEDDING_BOX");
        assertThat(boxRecords.get(0).path("archiveLocation").asText()).isEqualTo(embeddingBoxPositionCode);

        JsonNode slideRecords = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-records/search"), USER_M1_ARCHIVE)
            .param("keyword", String.valueOf(slide.get("slideNo")))), 200);
        assertThat(slideRecords).hasSize(1);
        assertThat(slideRecords.get(0).path("objectType").asText()).isEqualTo("SLIDE");
        assertThat(slideRecords.get(0).path("archiveLocation").asText()).isEqualTo(slidePositionCode);

        JsonNode workbench = diagnosticWorkbench(context.caseId(), USER_M4_DIAGNOSIS);
        assertThat(workbench.path("applicationFormArchiveStatus").asText()).isEqualTo("IN_STORAGE");
        assertThat(workbench.path("applicationFormArchiveLocation").asText()).isEqualTo(applicationPositionCode);
        assertThat(workbench.path("applicationFormImageUrl").asText()).isEqualTo("https://example.test/archive/app-form-001.jpg");
        assertThat(workbench.path("blocks").get(0).path("archiveStatus").asText()).isEqualTo("IN_STORAGE");
        assertThat(workbench.path("blocks").get(0).path("archiveLocation").asText()).isEqualTo(embeddingBoxPositionCode);
        assertThat(workbench.path("blocks").get(0).path("loanStatus").asText()).isEqualTo("NONE");
        assertThat(workbench.path("slides").get(0).path("archiveStatus").asText()).isEqualTo("IN_STORAGE");
        assertThat(workbench.path("slides").get(0).path("archiveLocation").asText()).isEqualTo(slidePositionCode);
        assertThat(workbench.path("slides").get(0).path("loanStatus").asText()).isEqualTo("NONE");

        JsonNode tracking = reportTracking(context.caseId(), USER_M4_TRACKING);
        assertThat(tracking.path("applicationFormArchiveStatus").asText()).isEqualTo("IN_STORAGE");
        assertThat(tracking.path("applicationFormArchiveLocation").asText()).isEqualTo(applicationPositionCode);
        assertThat(tracking.path("applicationFormImageUrl").asText()).isEqualTo("https://example.test/archive/app-form-001.jpg");

        assertThat(queryCaseMediaUrls(applicationId)).contains("https://example.test/archive/app-form-001.jpg");
    }

    @Test
    void shouldBorrowAndReturnArchivedSlideAndReflectStatus() throws Exception {
        PublishedReportContext context = preparePublishedReportContext("APP-M5-ARCH-002", "BC-M5-ARCH-002");
        JsonNode cabinet = createArchiveCabinet("CAB-M5-A2");
        JsonNode positions = listAvailablePositions(cabinet.path("id").asText());
        String slidePositionId = positions.get(0).path("id").asText();
        String slidePositionCode = positions.get(0).path("positionCode").asText();

        Map<String, Object> slide = querySlide(context.caseId());
        responseBody(postJson("/api/v1/archive/slides", USER_M1_ARCHIVE, """
            {
              "slideId":"%s",
              "archivePositionId":"%s",
              "operatorName":"archive-user",
              "terminalCode":"M5-ARCH-SLIDE-02"
            }
            """.formatted(slide.get("id"), slidePositionId)), 200);

        JsonNode loan = responseBody(postJson("/api/v1/material-loans", USER_M1_ARCHIVE, """
            {
              "materialType":"SLIDE",
              "materialId":"%s",
              "borrowedByUserId":"DOC-BORROW-01",
              "borrowedByName":"Borrow Doctor",
              "borrowPurpose":"case review",
              "operatorName":"archive-user",
              "terminalCode":"M5-LOAN-01"
            }
            """.formatted(slide.get("id"))), 200);
        String loanId = loan.path("loanId").asText();
        assertThat(loan.path("loanStatus").asText()).isEqualTo("BORROWED");

        JsonNode pendingLoans = responseBody(mockMvc.perform(authorized(get("/api/v1/material-loans/pending"), USER_M1_ARCHIVE)
            .param("keyword", String.valueOf(slide.get("slideNo")))), 200);
        assertThat(pendingLoans).hasSize(1);
        assertThat(pendingLoans.get(0).path("loanStatus").asText()).isEqualTo("BORROWED");

        JsonNode workbenchDuringLoan = diagnosticWorkbench(context.caseId(), USER_M4_DIAGNOSIS);
        assertThat(workbenchDuringLoan.path("slides").get(0).path("archiveStatus").asText()).isEqualTo("BORROWED");
        assertThat(workbenchDuringLoan.path("slides").get(0).path("loanStatus").asText()).isEqualTo("BORROWED");

        responseBody(postJson("/api/v1/material-loans/%s/return".formatted(loanId), USER_M1_ARCHIVE, """
            {
              "operatorName":"archive-user",
              "terminalCode":"M5-LOAN-02",
              "remarks":"returned to archive"
            }
            """), 200);

        JsonNode workbenchAfterReturn = diagnosticWorkbench(context.caseId(), USER_M4_DIAGNOSIS);
        assertThat(workbenchAfterReturn.path("slides").get(0).path("archiveStatus").asText()).isEqualTo("IN_STORAGE");
        assertThat(workbenchAfterReturn.path("slides").get(0).path("archiveLocation").asText()).isEqualTo(slidePositionCode);
        assertThat(workbenchAfterReturn.path("slides").get(0).path("loanStatus").asText()).isEqualTo("NONE");

        String occupyingObjectId = namedParameterJdbcTemplate.queryForObject("""
            select current_object_id
            from archive_positions
            where id = :positionId
            """, Map.of("positionId", slidePositionId), String.class);
        assertThat(occupyingObjectId).isEqualTo(slide.get("id"));
    }

    @Test
    void shouldRejectDuplicateOccupancyRepeatedLoanAndInvalidReturn() throws Exception {
        PublishedReportContext context = preparePublishedReportContext("APP-M5-ARCH-003", "BC-M5-ARCH-003");
        JsonNode cabinet = createArchiveCabinet("CAB-M5-A3");
        JsonNode positions = listAvailablePositions(cabinet.path("id").asText());
        String sharedPositionId = positions.get(0).path("id").asText();

        Map<String, Object> embeddingBox = queryEmbeddingBox(context.caseId());
        Map<String, Object> slide = querySlide(context.caseId());

        responseBody(postJson("/api/v1/archive/embedding-boxes", USER_M1_ARCHIVE, """
            {
              "embeddingBoxId":"%s",
              "archivePositionId":"%s",
              "operatorName":"archive-user",
              "terminalCode":"M5-ARCH-BOX-03"
            }
            """.formatted(embeddingBox.get("id"), sharedPositionId)), 200);

        postJson("/api/v1/archive/slides", USER_M1_ARCHIVE, """
            {
              "slideId":"%s",
              "archivePositionId":"%s",
              "operatorName":"archive-user",
              "terminalCode":"M5-ARCH-SLIDE-03"
            }
            """.formatted(slide.get("id"), sharedPositionId))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

        JsonNode loan = responseBody(postJson("/api/v1/material-loans", USER_M1_ARCHIVE, """
            {
              "materialType":"EMBEDDING_BOX",
              "materialId":"%s",
              "borrowedByUserId":"DOC-BORROW-02",
              "borrowedByName":"Borrow Doctor",
              "borrowPurpose":"peer review",
              "operatorName":"archive-user",
              "terminalCode":"M5-LOAN-03"
            }
            """.formatted(embeddingBox.get("id"))), 200);
        String loanId = loan.path("loanId").asText();

        postJson("/api/v1/material-loans", USER_M1_ARCHIVE, """
            {
              "materialType":"EMBEDDING_BOX",
              "materialId":"%s",
              "borrowedByUserId":"DOC-BORROW-03",
              "borrowedByName":"Borrow Doctor 2",
              "borrowPurpose":"double-loan",
              "operatorName":"archive-user",
              "terminalCode":"M5-LOAN-04"
            }
            """.formatted(embeddingBox.get("id")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

        responseBody(postJson("/api/v1/material-loans/%s/return".formatted(loanId), USER_M1_ARCHIVE, """
            {
              "operatorName":"archive-user",
              "terminalCode":"M5-LOAN-05"
            }
            """), 200);

        postJson("/api/v1/material-loans/%s/return".formatted(loanId), USER_M1_ARCHIVE, """
            {
              "operatorName":"archive-user",
              "terminalCode":"M5-LOAN-06"
            }
            """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));
    }

    private JsonNode createArchiveCabinet(String cabinetCode) throws Exception {
        return responseBody(postJson("/api/v1/archive-cabinets", USER_M1_ARCHIVE, """
            {
              "cabinetCode":"%s",
              "cabinetName":"Archive Cabinet %s",
              "cabinetType":"STANDARD",
              "layerCount":1,
              "slotCountPerLayer":3,
              "operatorName":"archive-user",
              "terminalCode":"M5-CAB-01",
              "locationDescription":"Room A"
            }
            """.formatted(cabinetCode, cabinetCode)), 200);
    }

    private JsonNode listAvailablePositions(String cabinetId) throws Exception {
        return responseBody(mockMvc.perform(authorized(get("/api/v1/archive-positions/available"), USER_M1_ARCHIVE)
            .param("cabinetId", cabinetId)), 200);
    }

    private String queryApplicationId(String caseId) {
        return namedParameterJdbcTemplate.queryForObject("""
            select application_id
            from pathology_cases
            where id = :caseId
            """, Map.of("caseId", caseId), String.class);
    }

    private Map<String, Object> queryEmbeddingBox(String caseId) {
        return namedParameterJdbcTemplate.queryForMap("""
            select id, embedding_box_no as embeddingBoxNo
            from embedding_boxes
            where case_id = :caseId
            order by created_at desc
            limit 1
            """, Map.of("caseId", caseId));
    }

    private Map<String, Object> querySlide(String caseId) {
        return namedParameterJdbcTemplate.queryForMap("""
            select id, slide_no as slideNo
            from slides
            where case_id = :caseId
            order by created_at desc
            limit 1
            """, Map.of("caseId", caseId));
    }

    private List<String> queryCaseMediaUrls(String applicationId) {
        return namedParameterJdbcTemplate.query("""
            select file_url
            from case_media_assets
            where object_type = 'APPLICATION_FORM'
              and object_id = :applicationId
            order by created_at desc
            """, Map.of("applicationId", applicationId), (rs, rowNum) -> rs.getString("file_url"));
    }
}
