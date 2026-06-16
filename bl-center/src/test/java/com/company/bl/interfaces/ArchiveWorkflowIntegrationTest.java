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
              "terminalCode":"M5-ARCH-BOX-01",
              "remarks":"embedding box archived"
            }
            """.formatted(embeddingBox.get("id"), embeddingBoxPositionId)), 200);

        responseBody(postJson("/api/v1/archive/slides", USER_M1_ARCHIVE, """
            {
              "slideId":"%s",
              "archivePositionId":"%s",
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

        JsonNode lifecycle = lifecycleTracking(context.caseId(), USER_M4_TRACKING);
        assertThat(lifecycle.path("applicationForm").path("archiveStatus").asText()).isEqualTo("IN_STORAGE");
        assertThat(lifecycle.path("applicationForm").path("archiveLocation").asText()).isEqualTo(applicationPositionCode);
        assertThat(lifecycle.path("applicationForm").path("imageUrl").asText()).isEqualTo("https://example.test/archive/app-form-001.jpg");
        assertThat(lifecycle.path("specimens")).hasSize(1);
        JsonNode lifecycleSpecimen = lifecycle.path("specimens").get(0);
        assertThat(lifecycleSpecimen.path("archiveStatus").asText()).isNotBlank();
        assertThat(lifecycleSpecimen.path("blocks")).hasSize(1);
        JsonNode lifecycleBlock = lifecycleSpecimen.path("blocks").get(0);
        assertThat(lifecycleBlock.path("archiveStatus").asText()).isEqualTo("IN_STORAGE");
        assertThat(lifecycleBlock.path("archiveLocation").asText()).isEqualTo(embeddingBoxPositionCode);
        assertThat(lifecycleBlock.path("slides")).hasSize(1);
        JsonNode lifecycleSlide = lifecycleBlock.path("slides").get(0);
        assertThat(lifecycleSlide.path("archiveStatus").asText()).isEqualTo("IN_STORAGE");
        assertThat(lifecycleSlide.path("archiveLocation").asText()).isEqualTo(slidePositionCode);
        assertThat(lifecycle.path("reportLifecycle").path("currentReport").path("reportId").asText()).isEqualTo(context.reportId());
        assertThat(lifecycle.path("reportLifecycle").path("versions").isArray()).isTrue();

        assertThat(queryCaseMediaUrls(applicationId)).contains("https://example.test/archive/app-form-001.jpg");
    }

    @Test
    void shouldPageArchiveObjectsAcrossUnarchivedArchivedAndBorrowedStates() throws Exception {
        PublishedReportContext context = preparePublishedReportContext("APP-M5-ARCH-OBJECTS-001", "BC-M5-ARCH-OBJECTS-001");
        String applicationId = queryApplicationId(context.caseId());
        Map<String, Object> specimen = querySpecimen(context.caseId());
        Map<String, Object> embeddingBox = queryEmbeddingBox(context.caseId());
        Map<String, Object> slide = querySlide(context.caseId());

        JsonNode applicationObjectsBeforeArchive = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-objects"), USER_M1_ARCHIVE)
            .param("objectType", "APPLICATION_FORM")
            .param("keyword", "APP-M5-ARCH-OBJECTS-001")
            .param("page", "1")
            .param("size", "20")), 200);
        JsonNode applicationRowBeforeArchive = findArchiveObject(applicationObjectsBeforeArchive, applicationId);
        assertThat(applicationObjectsBeforeArchive.path("total").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(applicationRowBeforeArchive.path("objectType").asText()).isEqualTo("APPLICATION_FORM");
        assertThat(applicationRowBeforeArchive.path("objectCode").asText()).isEqualTo("APP-M5-ARCH-OBJECTS-001");
        assertThat(applicationRowBeforeArchive.path("applicantDoctorName").asText()).isNotBlank();
        assertThat(applicationRowBeforeArchive.path("applicationDate").asText()).isNotBlank();
        assertThat(applicationRowBeforeArchive.path("archiveStatus").isMissingNode()
            || applicationRowBeforeArchive.path("archiveStatus").isNull()).isTrue();
        assertThat(applicationRowBeforeArchive.path("loanStatus").asText()).isEqualTo("NONE");

        JsonNode embeddingObjectsBeforeArchive = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-objects"), USER_M1_ARCHIVE)
            .param("objectType", "EMBEDDING_BOX")
            .param("keyword", String.valueOf(embeddingBox.get("embeddingBoxNo")))
            .param("page", "1")
            .param("size", "20")), 200);
        JsonNode embeddingRowBeforeArchive = findArchiveObject(embeddingObjectsBeforeArchive, String.valueOf(embeddingBox.get("id")));
        assertThat(embeddingRowBeforeArchive.path("objectType").asText()).isEqualTo("EMBEDDING_BOX");
        assertThat(embeddingRowBeforeArchive.path("archiveStatus").isMissingNode()
            || embeddingRowBeforeArchive.path("archiveStatus").isNull()).isTrue();

        JsonNode slideObjectsBeforeArchive = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-objects"), USER_M1_ARCHIVE)
            .param("objectType", "SLIDE")
            .param("keyword", String.valueOf(slide.get("slideNo")))
            .param("page", "1")
            .param("size", "20")), 200);
        JsonNode slideRowBeforeArchive = findArchiveObject(slideObjectsBeforeArchive, String.valueOf(slide.get("id")));
        assertThat(slideRowBeforeArchive.path("objectType").asText()).isEqualTo("SLIDE");
        assertThat(slideRowBeforeArchive.path("archiveStatus").isMissingNode()
            || slideRowBeforeArchive.path("archiveStatus").isNull()).isTrue();

        JsonNode specimenObjectsBeforeArchive = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-objects"), USER_M1_ARCHIVE)
            .param("objectType", "SPECIMEN")
            .param("keyword", String.valueOf(specimen.get("specimenNo")))
            .param("page", "1")
            .param("size", "20")), 200);
        JsonNode specimenRowBeforeArchive = findArchiveObject(specimenObjectsBeforeArchive, String.valueOf(specimen.get("id")));
        assertThat(specimenRowBeforeArchive.path("objectType").asText()).isEqualTo("SPECIMEN");
        assertThat(specimenRowBeforeArchive.path("objectCode").asText()).isEqualTo(String.valueOf(specimen.get("specimenNo")));
        assertThat(specimenRowBeforeArchive.path("archiveStatus").isMissingNode()
            || specimenRowBeforeArchive.path("archiveStatus").isNull()).isTrue();

        JsonNode cabinet = createArchiveCabinet("CAB-M5-OBJECTS");
        JsonNode positions = listAvailablePositions(cabinet.path("id").asText());
        String applicationPositionId = positions.get(0).path("id").asText();
        String applicationPositionCode = positions.get(0).path("positionCode").asText();
        String embeddingPositionId = positions.get(1).path("id").asText();
        String slidePositionId = positions.get(2).path("id").asText();
        String specimenPositionId = positions.get(3).path("id").asText();
        String specimenPositionCode = positions.get(3).path("positionCode").asText();

        responseBody(postJson("/api/v1/archive/application-forms", USER_M1_ARCHIVE, """
            {
              "caseId":"%s",
              "archivePositionId":"%s",
              "terminalCode":"M5-ARCH-OBJECT-APP"
            }
            """.formatted(context.caseId(), applicationPositionId)), 200);
        responseBody(postJson("/api/v1/archive/embedding-boxes", USER_M1_ARCHIVE, """
            {
              "embeddingBoxId":"%s",
              "archivePositionId":"%s",
              "terminalCode":"M5-ARCH-OBJECT-BOX"
            }
            """.formatted(embeddingBox.get("id"), embeddingPositionId)), 200);
        responseBody(postJson("/api/v1/archive/slides", USER_M1_ARCHIVE, """
            {
              "slideId":"%s",
              "archivePositionId":"%s",
              "terminalCode":"M5-ARCH-OBJECT-SLIDE"
            }
            """.formatted(slide.get("id"), slidePositionId)), 200);
        responseBody(postJson("/api/v1/archive/specimens", USER_M1_ARCHIVE, """
            {
              "specimenId":"%s",
              "archivePositionId":"%s",
              "terminalCode":"M5-ARCH-OBJECT-SPECIMEN"
            }
            """.formatted(specimen.get("id"), specimenPositionId)), 200);

        JsonNode applicationObjectsAfterArchive = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-objects"), USER_M1_ARCHIVE)
            .param("objectType", "APPLICATION_FORM")
            .param("keyword", "APP-M5-ARCH-OBJECTS-001")
            .param("page", "1")
            .param("size", "20")), 200);
        JsonNode applicationRowAfterArchive = findArchiveObject(applicationObjectsAfterArchive, applicationId);
        assertThat(applicationRowAfterArchive.path("archiveStatus").asText()).isEqualTo("IN_STORAGE");
        assertThat(applicationRowAfterArchive.path("archiveLocation").asText()).isEqualTo(applicationPositionCode);
        assertThat(applicationRowAfterArchive.path("storedByName").asText()).isNotBlank();
        assertThat(applicationRowAfterArchive.path("archivedAt").asText()).isNotBlank();

        JsonNode specimenObjectsAfterArchive = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-objects"), USER_M1_ARCHIVE)
            .param("objectType", "SPECIMEN")
            .param("keyword", String.valueOf(specimen.get("specimenNo")))
            .param("page", "1")
            .param("size", "20")), 200);
        JsonNode specimenRowAfterArchive = findArchiveObject(specimenObjectsAfterArchive, String.valueOf(specimen.get("id")));
        assertThat(specimenRowAfterArchive.path("archiveStatus").asText()).isEqualTo("IN_STORAGE");
        assertThat(specimenRowAfterArchive.path("archiveLocation").asText()).isEqualTo(specimenPositionCode);
        assertThat(specimenRowAfterArchive.path("storedByName").asText()).isNotBlank();

        responseBody(postJson("/api/v1/material-loans", USER_M1_ARCHIVE, """
            {
              "materialType":"SLIDE",
              "materialId":"%s",
              "borrowedByUserId":"DOC-BORROW-ARCHIVE-OBJECTS",
              "borrowedByName":"Archive Object Borrower",
              "borrowPurpose":"archive object paging",
              "terminalCode":"M5-ARCH-OBJECT-LOAN"
            }
            """.formatted(slide.get("id"))), 200);

        JsonNode slideObjectsAfterLoan = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-objects"), USER_M1_ARCHIVE)
            .param("objectType", "SLIDE")
            .param("keyword", String.valueOf(slide.get("slideNo")))
            .param("page", "1")
            .param("size", "20")), 200);
        JsonNode slideRowAfterLoan = findArchiveObject(slideObjectsAfterLoan, String.valueOf(slide.get("id")));
        assertThat(slideRowAfterLoan.path("archiveStatus").asText()).isEqualTo("BORROWED");
        assertThat(slideRowAfterLoan.path("loanStatus").asText()).isEqualTo("BORROWED");
        assertThat(slideRowAfterLoan.path("borrowedByName").asText()).isEqualTo("Archive Object Borrower");
        assertThat(slideRowAfterLoan.path("borrowedAt").asText()).isNotBlank();
    }

    @Test
    void shouldRejectInvalidArchiveObjectQueryAndRequireArchiveQueryPermission() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/archive-objects"), USER_M1_ARCHIVE))
            .andExpect(status().isBadRequest());

        mockMvc.perform(authorized(get("/api/v1/archive-objects"), USER_M1_ARCHIVE)
            .param("objectType", "UNKNOWN"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        mockMvc.perform(authorized(get("/api/v1/archive-objects"), "USER_M1_REAGENT")
            .param("objectType", "SLIDE"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldBatchArchivePhysicalObjectsWithCabinetAutoAllocationAndSpecimenReminder() throws Exception {
        PublishedReportContext boxContext = preparePublishedReportContext("APP-M5-BATCH-BOX-001", "BC-M5-BATCH-BOX-001");
        PublishedReportContext specimenContext = preparePublishedReportContext("APP-M5-BATCH-SPECIMEN-001", "BC-M5-BATCH-SPECIMEN-001");
        Map<String, Object> embeddingBox = queryEmbeddingBox(boxContext.caseId());
        Map<String, Object> specimen = querySpecimen(specimenContext.caseId());
        JsonNode cabinet = createArchiveCabinet("CAB-M5-BATCH-OK");
        JsonNode positionsBefore = listAvailablePositions(cabinet.path("id").asText());

        responseBody(postJson("/api/v1/archive/embedding-boxes/batch", USER_M1_ARCHIVE, """
            {
              "archiveCabinetId":"%s",
              "objectIds":["%s"],
              "terminalCode":"M5-BATCH-BOX",
              "remarks":"batch box"
            }
            """.formatted(cabinet.path("id").asText(), embeddingBox.get("id"))), 200);
        responseBody(postJson("/api/v1/archive/specimens/batch", USER_M1_ARCHIVE, """
            {
              "archiveCabinetId":"%s",
              "objectIds":["%s"],
              "archiveExpiresAt":"2026-06-30T18:00:00",
              "archiveReminderDays":1,
              "terminalCode":"M5-BATCH-SPECIMEN",
              "remarks":"batch specimen"
            }
            """.formatted(cabinet.path("id").asText(), specimen.get("id"))), 200);

        JsonNode embeddingObjects = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-objects"), USER_M1_ARCHIVE)
            .param("objectType", "EMBEDDING_BOX")
            .param("keyword", String.valueOf(embeddingBox.get("embeddingBoxNo")))
            .param("page", "1")
            .param("size", "20")), 200);
        JsonNode embeddingRow = findArchiveObject(embeddingObjects, String.valueOf(embeddingBox.get("id")));
        assertThat(embeddingRow.path("archiveStatus").asText()).isEqualTo("IN_STORAGE");
        assertThat(embeddingRow.path("archiveLocation").asText()).isEqualTo(positionsBefore.get(0).path("positionCode").asText());
        assertThat(embeddingRow.path("objectStatus").asText()).isNotBlank();

        JsonNode specimenObjects = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-objects"), USER_M1_ARCHIVE)
            .param("objectType", "SPECIMEN")
            .param("keyword", String.valueOf(specimen.get("specimenNo")))
            .param("page", "1")
            .param("size", "20")), 200);
        JsonNode specimenRow = findArchiveObject(specimenObjects, String.valueOf(specimen.get("id")));
        assertThat(specimenRow.path("archiveStatus").asText()).isEqualTo("IN_STORAGE");
        assertThat(specimenRow.path("archiveLocation").asText()).isEqualTo(positionsBefore.get(1).path("positionCode").asText());
        assertThat(specimenRow.path("archiveExpiresAt").asText()).startsWith("2026-06-30T18:00");
        assertThat(specimenRow.path("archiveReminderDays").asInt()).isEqualTo(1);
        assertThat(specimenRow.path("contentDescribedByName").asText()).isNotBlank();
    }

    @Test
    void shouldRollbackWholeBatchArchiveWhenCabinetCapacityIsInsufficient() throws Exception {
        PublishedReportContext firstContext = preparePublishedReportContext("APP-M5-BATCH-ROLLBACK-001", "BC-M5-BATCH-ROLLBACK-001");
        PublishedReportContext secondContext = preparePublishedReportContext("APP-M5-BATCH-ROLLBACK-002", "BC-M5-BATCH-ROLLBACK-002");
        Map<String, Object> firstSlide = querySlide(firstContext.caseId());
        Map<String, Object> secondSlide = querySlide(secondContext.caseId());
        JsonNode cabinet = createArchiveCabinet("CAB-M5-BATCH-ROLLBACK");
        JsonNode positions = listAvailablePositions(cabinet.path("id").asText());

        responseBody(postJson("/api/v1/archive/application-forms", USER_M1_ARCHIVE, """
            {
              "caseId":"%s",
              "archivePositionId":"%s",
              "terminalCode":"M5-BATCH-FILL-1"
            }
            """.formatted(firstContext.caseId(), positions.get(0).path("id").asText())), 200);
        responseBody(postJson("/api/v1/archive/application-forms", USER_M1_ARCHIVE, """
            {
              "caseId":"%s",
              "archivePositionId":"%s",
              "terminalCode":"M5-BATCH-FILL-2"
            }
            """.formatted(secondContext.caseId(), positions.get(1).path("id").asText())), 200);
        responseBody(postJson("/api/v1/archive/specimens", USER_M1_ARCHIVE, """
            {
              "specimenId":"%s",
              "archivePositionId":"%s",
              "terminalCode":"M5-BATCH-FILL-3"
            }
            """.formatted(querySpecimen(firstContext.caseId()).get("id"), positions.get(2).path("id").asText())), 200);

        postJson("/api/v1/archive/slides/batch", USER_M1_ARCHIVE, """
            {
              "archiveCabinetId":"%s",
              "objectIds":["%s","%s"],
              "terminalCode":"M5-BATCH-ROLLBACK"
            }
            """.formatted(cabinet.path("id").asText(), firstSlide.get("id"), secondSlide.get("id")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

        JsonNode firstSlideObjects = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-objects"), USER_M1_ARCHIVE)
            .param("objectType", "SLIDE")
            .param("keyword", String.valueOf(firstSlide.get("slideNo")))
            .param("page", "1")
            .param("size", "20")), 200);
        JsonNode secondSlideObjects = responseBody(mockMvc.perform(authorized(get("/api/v1/archive-objects"), USER_M1_ARCHIVE)
            .param("objectType", "SLIDE")
            .param("keyword", String.valueOf(secondSlide.get("slideNo")))
            .param("page", "1")
            .param("size", "20")), 200);
        JsonNode firstSlideRow = findArchiveObject(firstSlideObjects, String.valueOf(firstSlide.get("id")));
        JsonNode secondSlideRow = findArchiveObject(secondSlideObjects, String.valueOf(secondSlide.get("id")));
        assertThat(firstSlideRow.path("archiveStatus").isNull()
            || firstSlideRow.path("archiveStatus").isMissingNode()).isTrue();
        assertThat(secondSlideRow.path("archiveStatus").isNull()
            || secondSlideRow.path("archiveStatus").isMissingNode()).isTrue();
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
              "terminalCode":"M5-ARCH-SLIDE-02"
            }
            """.formatted(slide.get("id"), slidePositionId)), 200);

        JsonNode loan = responseBody(postJson("/api/v1/material-loans", USER_M1_ARCHIVE, """
            {
              "materialType":"SLIDE",
              "materialId":"%s",
              "borrowedByUserId":"DOC-BORROW-01",
              "borrowedByName":"Borrow Doctor",
              "borrowerPhone":"13800000000",
              "borrowerUnit":"External Hospital",
              "borrowPurpose":"case review",
              "depositAmount":10,
              "terminalCode":"M5-LOAN-01"
            }
            """.formatted(slide.get("id"))), 200);
        String loanId = loan.path("loanId").asText();
        assertThat(loan.path("loanStatus").asText()).isEqualTo("BORROWED");
        assertThat(loan.path("borrowerPhone").asText()).isEqualTo("13800000000");
        assertThat(loan.path("borrowerUnit").asText()).isEqualTo("External Hospital");
        assertThat(loan.path("depositAmount").decimalValue()).isEqualByComparingTo("10");

        JsonNode borrowedLoans = responseBody(mockMvc.perform(authorized(get("/api/v1/material-loans"), USER_M1_ARCHIVE)
            .param("keyword", String.valueOf(slide.get("slideNo")))
            .param("loanStatus", "BORROWED")), 200);
        assertThat(borrowedLoans).hasSize(1);
        assertThat(borrowedLoans.get(0).path("loanStatus").asText()).isEqualTo("BORROWED");
        assertThat(borrowedLoans.get(0).path("borrowerPhone").asText()).isEqualTo("13800000000");
        assertThat(borrowedLoans.get(0).path("borrowerUnit").asText()).isEqualTo("External Hospital");
        assertThat(borrowedLoans.get(0).path("depositAmount").decimalValue()).isEqualByComparingTo("10");

        JsonNode pendingLoans = responseBody(mockMvc.perform(authorized(get("/api/v1/material-loans/pending"), USER_M1_ARCHIVE)
            .param("keyword", String.valueOf(slide.get("slideNo")))), 200);
        assertThat(pendingLoans).hasSize(1);
        assertThat(pendingLoans.get(0).path("loanStatus").asText()).isEqualTo("BORROWED");

        JsonNode workbenchDuringLoan = diagnosticWorkbench(context.caseId(), USER_M4_DIAGNOSIS);
        assertThat(workbenchDuringLoan.path("slides").get(0).path("archiveStatus").asText()).isEqualTo("BORROWED");
        assertThat(workbenchDuringLoan.path("slides").get(0).path("loanStatus").asText()).isEqualTo("BORROWED");

        responseBody(postJson("/api/v1/material-loans/%s/return".formatted(loanId), USER_M1_ARCHIVE, """
            {
              "terminalCode":"M5-LOAN-02",
              "remarks":"returned to archive"
            }
            """), 200);

        JsonNode pendingLoansAfterReturn = responseBody(mockMvc.perform(authorized(get("/api/v1/material-loans/pending"), USER_M1_ARCHIVE)
            .param("keyword", String.valueOf(slide.get("slideNo")))), 200);
        assertThat(pendingLoansAfterReturn).isEmpty();

        JsonNode returnedLoans = responseBody(mockMvc.perform(authorized(get("/api/v1/material-loans"), USER_M1_ARCHIVE)
            .param("keyword", String.valueOf(slide.get("slideNo")))
            .param("loanStatus", "RETURNED")), 200);
        assertThat(returnedLoans).hasSize(1);
        assertThat(returnedLoans.get(0).path("loanStatus").asText()).isEqualTo("RETURNED");
        assertThat(returnedLoans.get(0).path("returnedAt").asText()).isNotBlank();
        assertThat(returnedLoans.get(0).path("returnedByName").asText()).isNotBlank();

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
              "terminalCode":"M5-ARCH-BOX-03"
            }
            """.formatted(embeddingBox.get("id"), sharedPositionId)), 200);

        postJson("/api/v1/archive/slides", USER_M1_ARCHIVE, """
            {
              "slideId":"%s",
              "archivePositionId":"%s",
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
              "borrowerPhone":"13900000000",
              "borrowerUnit":"Peer Review Center",
              "borrowPurpose":"peer review",
              "depositAmount":20,
              "terminalCode":"M5-LOAN-03"
            }
            """.formatted(embeddingBox.get("id"))), 200);
        String loanId = loan.path("loanId").asText();
        assertThat(loan.path("borrowerPhone").asText()).isEqualTo("13900000000");
        assertThat(loan.path("borrowerUnit").asText()).isEqualTo("Peer Review Center");
        assertThat(loan.path("depositAmount").decimalValue()).isEqualByComparingTo("20");

        postJson("/api/v1/material-loans", USER_M1_ARCHIVE, """
            {
              "materialType":"EMBEDDING_BOX",
              "materialId":"%s",
              "borrowedByUserId":"DOC-BORROW-03",
              "borrowedByName":"Borrow Doctor 2",
              "borrowPurpose":"double-loan",
              "terminalCode":"M5-LOAN-04"
            }
            """.formatted(embeddingBox.get("id")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

        responseBody(postJson("/api/v1/material-loans/%s/return".formatted(loanId), USER_M1_ARCHIVE, """
            {
              "terminalCode":"M5-LOAN-05"
            }
            """), 200);

        postJson("/api/v1/material-loans/%s/return".formatted(loanId), USER_M1_ARCHIVE, """
            {
              "terminalCode":"M5-LOAN-06"
            }
            """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));
    }

    @Test
    void shouldRegisterMaterialLoanAbnormalRecordsForSlideAndEmbeddingBox() throws Exception {
        PublishedReportContext context = preparePublishedReportContext("APP-M5-LOAN-ABNORMAL-001", "BC-M5-LOAN-ABNORMAL-001");
        JsonNode cabinet = createArchiveCabinet("CAB-M5-ABNORMAL");
        JsonNode positions = listAvailablePositions(cabinet.path("id").asText());
        Map<String, Object> embeddingBox = queryEmbeddingBox(context.caseId());
        Map<String, Object> slide = querySlide(context.caseId());

        responseBody(postJson("/api/v1/archive/embedding-boxes", USER_M1_ARCHIVE, """
            {
              "embeddingBoxId":"%s",
              "archivePositionId":"%s",
              "terminalCode":"M5-ABNORMAL-BOX-ARCHIVE"
            }
            """.formatted(embeddingBox.get("id"), positions.get(0).path("id").asText())), 200);
        responseBody(postJson("/api/v1/archive/slides", USER_M1_ARCHIVE, """
            {
              "slideId":"%s",
              "archivePositionId":"%s",
              "terminalCode":"M5-ABNORMAL-SLIDE-ARCHIVE"
            }
            """.formatted(slide.get("id"), positions.get(1).path("id").asText())), 200);

        JsonNode loan = responseBody(postJson("/api/v1/material-loans", USER_M1_ARCHIVE, """
            {
              "materialType":"SLIDE",
              "materialId":"%s",
              "borrowedByUserId":"DOC-ABNORMAL",
              "borrowedByName":"Borrower For Abnormal",
              "borrowPurpose":"abnormal registration",
              "terminalCode":"M5-ABNORMAL-LOAN"
            }
            """.formatted(slide.get("id"))), 200);

        JsonNode slideAbnormal = responseBody(postJson("/api/v1/material-loans/abnormal-records", USER_M1_ARCHIVE, """
            {
              "materialType":"SLIDE",
              "materialId":"%s",
              "loanId":"%s",
              "abnormalReason":"玻片破损",
              "contacted":true,
              "contactResult":"已电话联系",
              "borrowedSlideNo":"%s",
              "borrowerName":"Borrower For Abnormal",
              "borrowerRelationship":"患者家属",
              "borrowerPhone":"13800000000",
              "borrowerUnit":"外院",
              "borrowerIdentityNo":"ID-ABNORMAL",
              "borrowedAt":"2026-04-16T09:38:06",
              "expectedReturnAt":"2026-05-16T09:38:06",
              "slideCount":1,
              "depositAmount":0,
              "borrowedContent":"HE 玻片 1 张",
              "returnAbnormalInfo":"边角缺损",
              "terminalCode":"M5-ABNORMAL-SLIDE"
            }
            """.formatted(slide.get("id"), loan.path("loanId").asText(), slide.get("slideNo"))), 200);

        assertThat(slideAbnormal.path("materialType").asText()).isEqualTo("SLIDE");
        assertThat(slideAbnormal.path("loanId").asText()).isEqualTo(loan.path("loanId").asText());
        assertThat(slideAbnormal.path("abnormalReason").asText()).isEqualTo("玻片破损");
        assertThat(slideAbnormal.path("registeredAt").asText()).isNotBlank();

        JsonNode boxAbnormal = responseBody(postJson("/api/v1/material-loans/abnormal-records", USER_M1_ARCHIVE, """
            {
              "materialType":"EMBEDDING_BOX",
              "materialId":"%s",
              "abnormalReason":"蜡块缺角",
              "terminalCode":"M5-ABNORMAL-BOX"
            }
            """.formatted(embeddingBox.get("id"))), 200);
        assertThat(boxAbnormal.path("materialType").asText()).isEqualTo("EMBEDDING_BOX");
        assertThat(boxAbnormal.path("loanId").isMissingNode() || boxAbnormal.path("loanId").isNull()).isTrue();

        Integer abnormalCount = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from material_loan_abnormal_records
            where case_id = :caseId
            """, Map.of("caseId", context.caseId()), Integer.class);
        assertThat(abnormalCount).isEqualTo(2);

        Map<String, Object> slideAbnormalRow = namedParameterJdbcTemplate.queryForMap("""
            select borrower_phone as borrowerPhone, contact_result as contactResult, return_abnormal_info as returnAbnormalInfo
            from material_loan_abnormal_records
            where id = :id
            """, Map.of("id", slideAbnormal.path("id").asText()));
        assertThat(slideAbnormalRow.get("borrowerPhone")).isEqualTo("13800000000");
        assertThat(slideAbnormalRow.get("contactResult")).isEqualTo("已电话联系");
        assertThat(slideAbnormalRow.get("returnAbnormalInfo")).isEqualTo("边角缺损");
    }

    @Test
    void shouldRejectInvalidMaterialLoanAbnormalRegistration() throws Exception {
        PublishedReportContext context = preparePublishedReportContext("APP-M5-LOAN-ABNORMAL-002", "BC-M5-LOAN-ABNORMAL-002");
        JsonNode cabinet = createArchiveCabinet("CAB-M5-ABNORMAL-REJECT");
        JsonNode positions = listAvailablePositions(cabinet.path("id").asText());
        Map<String, Object> slide = querySlide(context.caseId());

        responseBody(postJson("/api/v1/archive/slides", USER_M1_ARCHIVE, """
            {
              "slideId":"%s",
              "archivePositionId":"%s",
              "terminalCode":"M5-ABNORMAL-REJECT-ARCHIVE"
            }
            """.formatted(slide.get("id"), positions.get(0).path("id").asText())), 200);

        postJson("/api/v1/material-loans/abnormal-records", USER_M1_ARCHIVE, """
            {
              "materialType":"SLIDE",
              "materialId":"%s",
              "abnormalReason":""
            }
            """.formatted(slide.get("id")))
            .andExpect(status().isBadRequest());

        postJson("/api/v1/material-loans/abnormal-records", "USER_M1_REAGENT", """
            {
              "materialType":"SLIDE",
              "materialId":"%s",
              "abnormalReason":"无权限登记"
            }
            """.formatted(slide.get("id")))
            .andExpect(status().isForbidden());
    }

    private JsonNode createArchiveCabinet(String cabinetCode) throws Exception {
        return responseBody(postJson("/api/v1/archive-cabinets", USER_M1_ARCHIVE, """
            {
              "cabinetCode":"%s",
              "cabinetName":"Archive Cabinet %s",
              "cabinetType":"STANDARD",
              "layerCount":1,
              "slotCountPerLayer":4,
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

    private Map<String, Object> querySpecimen(String caseId) {
        return namedParameterJdbcTemplate.queryForMap("""
            select id, specimen_no as specimenNo
            from specimens
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

    private JsonNode findArchiveObject(JsonNode page, String objectId) {
        for (JsonNode item : page.path("items")) {
            if (objectId.equals(item.path("objectId").asText())) {
                return item;
            }
        }
        throw new AssertionError("Archive object not found: " + objectId);
    }
}
