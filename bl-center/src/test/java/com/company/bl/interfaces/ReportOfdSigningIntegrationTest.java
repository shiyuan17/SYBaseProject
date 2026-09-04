package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.company.bl.infrastructure.config.ReportStorageProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.ofdrw.reader.ContentExtractor;
import org.ofdrw.reader.OFDReader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class ReportOfdSigningIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    private static final Path STORAGE_ROOT = Path.of(System.getProperty("java.io.tmpdir"),
        "sybase-report-ofd-it-" + UUID.randomUUID()).toAbsolutePath().normalize();

    @Autowired
    ReportStorageProperties reportStorageProperties;

    @DynamicPropertySource
    static void reportStorageProperties(DynamicPropertyRegistry registry) {
        registry.add("bl.file-storage.reports.root-dir", STORAGE_ROOT::toString);
        registry.add("bl.file-storage.reports.cleanup-fixed-delay-ms", () -> "86400000");
    }

    @AfterAll
    static void cleanStorageRoot() throws Exception {
        if (!Files.exists(STORAGE_ROOT)) {
            return;
        }
        try (var paths = Files.walk(STORAGE_ROOT)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @Test
    void shouldGenerateOneOfdOnSignAndReuseItWhenPublishing() throws Exception {
        ReportContext context = insertReviewedReport("SUCCESS");
        saveSnapshotAsSigningDoctor(context);

        mockMvc.perform(authorized(get("/api/v1/pathology-reports/{id}/ofd", context.reportId()), USER_M4_SIGN))
            .andExpect(status().isNotFound());

        postJson("/api/v1/pathology-reports/%s/sign".formatted(context.reportId()), USER_M4_SIGN, "{}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reportStatus").value("SIGNED"));

        mockMvc.perform(authorized(get("/api/v1/pathology-reports/{id}/ofd/status", context.reportId()), USER_M4_SIGN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("READY"))
            .andExpect(jsonPath("$.data.retryAfterMs").value(0));

        ArtifactRow artifact = loadArtifact(context.reportId());
        Path ofd = STORAGE_ROOT.resolve(artifact.storageKey());
        assertThat(ofd).isRegularFile();
        assertThat(artifact.contentType()).isEqualTo("application/ofd");
        assertThat(artifact.byteSize()).isEqualTo(Files.size(ofd));
        assertThat(artifact.sha256()).hasSize(64);
        assertThat(queryInt("select count(*) from report_version_artifacts where report_id = :reportId", context.reportId()))
            .isEqualTo(1);

        JsonNode artifacts = responseBody(mockMvc.perform(authorized(
            get("/api/v1/pathology-reports/{id}/ofd-artifacts", context.reportId()), USER_M4_SIGN)), 200);
        assertThat(artifacts).hasSize(1);
        assertThat(artifacts.get(0).path("artifactId").asText()).isEqualTo(artifact.id());
        assertThat(artifacts.get(0).path("reportId").asText()).isEqualTo(context.reportId());
        assertThat(artifacts.get(0).path("versionNo").asInt()).isEqualTo(1);
        assertThat(artifacts.get(0).path("artifactFormat").asText()).isEqualTo("OFD");
        assertThat(artifacts.get(0).path("fileName").asText()).isEqualTo(artifact.fileName());
        assertThat(artifacts.get(0).path("contentType").asText()).isEqualTo("application/ofd");
        assertThat(artifacts.get(0).path("byteSize").asLong()).isEqualTo(Files.size(ofd));
        assertThat(artifacts.get(0).path("sha256").asText()).isEqualTo(artifact.sha256());
        assertThat(artifacts.get(0).path("generatedAt").asText()).isNotBlank();
        assertThat(artifacts.get(0).path("downloadUrl").asText()).isEqualTo(
            "/api/v1/pathology-reports/" + context.reportId() + "/ofd-artifacts/" + artifact.id() + "/file");
        assertThat(artifacts.get(0).has("storageKey")).isFalse();

        mockMvc.perform(authorized(get(
            "/api/v1/pathology-reports/{id}/ofd-artifacts/{artifactId}/file",
            context.reportId(), artifact.id()), USER_M4_SIGN))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/ofd"))
            .andExpect(content().bytes(Files.readAllBytes(ofd)));

        MvcResult pdfConversion = mockMvc.perform(authorized(
                get("/api/v1/pathology-reports/{id}/ofd/pdf", context.reportId()), USER_M4_SIGN))
            .andExpect(request().asyncStarted())
            .andReturn();
        MvcResult pdfResponse = mockMvc.perform(asyncDispatch(pdfConversion))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"))
            .andExpect(header().string("Content-Disposition", startsWith("inline")))
            .andExpect(header().string("Content-Disposition", containsString(".pdf")))
            .andReturn();
        assertThat(pdfResponse.getResponse().getContentAsByteArray())
            .startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));

        mockMvc.perform(authorized(
                get("/api/v1/pathology-reports/{id}/ofd/pdf", context.reportId()), USER_M4_NO_PERMISSION))
            .andExpect(status().isForbidden());

        try (OFDReader reader = new OFDReader(ofd)) {
            String text = String.join("", new ContentExtractor(reader).extractAll());
            assertThat(text)
                .contains(context.reportNo())
                .contains("集成测试中文诊断")
                .contains("签发医生");
        }

        mockMvc.perform(authorized(get("/api/v1/pathology-reports/{id}/ofd", context.reportId()), USER_M4_SIGN))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/ofd"))
            .andExpect(header().string("Content-Disposition", startsWith("inline")))
            .andExpect(header().string("Content-Disposition", containsString(".ofd")))
            .andExpect(content().bytes(Files.readAllBytes(ofd)));

        mockMvc.perform(authorized(get("/api/v1/pathology-reports/{id}/ofd", context.reportId()), USER_M4_NO_PERMISSION))
            .andExpect(status().isForbidden());

        postJson("/api/v1/pathology-reports/%s/publish".formatted(context.reportId()), USER_M4_SIGN, "{}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reportStatus").value("PUBLISHED"));

        assertThat(queryInt("select count(*) from report_version_artifacts where report_id = :reportId", context.reportId()))
            .isEqualTo(1);
        assertThat(queryInt("""
            select count(distinct artifact_id)
            from report_versions
            where report_id = :reportId and artifact_id is not null
            """, context.reportId())).isEqualTo(1);
    }

    @Test
    void shouldRollbackSigningWhenAtomicMoveCannotCreateTheFormalReportPath() throws Exception {
        ReportContext context = insertReviewedReport("MOVE-FAILURE");
        saveSnapshotAsSigningDoctor(context);
        String dateDirectory = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        Path conflictingReportDirectory = STORAGE_ROOT.resolve("formal")
            .resolve(dateDirectory)
            .resolve(context.reportId());
        Files.createDirectories(conflictingReportDirectory.getParent());
        Files.writeString(conflictingReportDirectory, "not-a-directory");

        postJson("/api/v1/pathology-reports/%s/sign".formatted(context.reportId()), USER_M4_SIGN, "{}")
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.message").value("报告 OFD 文件生成失败，签发未完成"));

        assertThat(queryString("select report_status from pathology_reports where id = :reportId", context.reportId()))
            .isEqualTo("REVIEWED");
        assertThat(queryString("select case_status from pathology_cases where id = :caseId", context.caseId()))
            .isEqualTo("DIAGNOSING");
        assertThat(queryInt("select count(*) from report_versions where report_id = :reportId", context.reportId()))
            .isZero();
        assertThat(queryInt("select count(*) from report_version_artifacts where report_id = :reportId", context.reportId()))
            .isZero();
        assertThat(queryInt("""
            select count(*) from workflow_events where case_id = :reportId and event_type = 'SIGN'
            """, context.caseId())).isZero();
        try (var paths = Files.walk(STORAGE_ROOT)) {
            assertThat(paths.noneMatch(path -> path.getFileName().toString().endsWith(".ofd")
                && path.toString().contains(context.reportId()))).isTrue();
        }
    }

    @Test
    void shouldReturnEmptyOfdArtifactListAndProtectTheQueryContract() throws Exception {
        ReportContext context = insertReviewedReport("QUERY-EMPTY");

        JsonNode artifacts = responseBody(mockMvc.perform(authorized(
            get("/api/v1/pathology-reports/{id}/ofd-artifacts", context.reportId()), USER_M4_SIGN)), 200);
        assertThat(artifacts).isEmpty();

        mockMvc.perform(authorized(
            get("/api/v1/pathology-reports/{id}/ofd-artifacts", context.reportId()), USER_M4_NO_PERMISSION))
            .andExpect(status().isForbidden());
        mockMvc.perform(authorized(
            get("/api/v1/pathology-reports/{id}/ofd-artifacts", "REPORT-NOT-FOUND"), USER_M4_SIGN))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldListHistoricalOfdArtifactsByVersionAndDownloadEachStoredFile() throws Exception {
        ReportContext context = insertReviewedReport("QUERY-HISTORY");
        saveSnapshotAsSigningDoctor(context);
        postJson("/api/v1/pathology-reports/%s/sign".formatted(context.reportId()), USER_M4_SIGN, "{}")
            .andExpect(status().isOk());
        ArtifactRow signedArtifact = loadArtifact(context.reportId());
        Path historicalFile = STORAGE_ROOT.resolve("formal/2026/08/" + context.reportId() + "/RVA-HISTORY.ofd");
        Files.createDirectories(historicalFile.getParent());
        Files.copy(STORAGE_ROOT.resolve(signedArtifact.storageKey()), historicalFile);
        String historicalChecksum = signedArtifact.sha256();
        namedParameterJdbcTemplate.update("""
            insert into report_version_artifacts
                (id, report_id, version_no, artifact_format, file_name, storage_key, content_type,
                 byte_size, sha256, generated_at)
            values
                (:id, :reportId, 2, 'OFD', 'history-V2.ofd', :storageKey, 'application/ofd',
                 :byteSize, :sha256, :generatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", "RVA-HISTORY")
            .addValue("reportId", context.reportId())
            .addValue("storageKey", STORAGE_ROOT.relativize(historicalFile).toString().replace('\\', '/'))
            .addValue("byteSize", Files.size(historicalFile))
            .addValue("sha256", historicalChecksum)
            .addValue("generatedAt", LocalDateTime.now().plusSeconds(1)));

        JsonNode artifacts = responseBody(mockMvc.perform(authorized(
            get("/api/v1/pathology-reports/{id}/ofd-artifacts", context.reportId()), USER_M4_SIGN)), 200);
        assertThat(artifacts).hasSize(2);
        assertThat(artifacts.get(0).path("versionNo").asInt()).isEqualTo(2);
        assertThat(artifacts.get(0).path("artifactId").asText()).isEqualTo("RVA-HISTORY");
        assertThat(artifacts.get(1).path("artifactId").asText()).isEqualTo(signedArtifact.id());

        mockMvc.perform(authorized(get(
            "/api/v1/pathology-reports/{id}/ofd-artifacts/{artifactId}/file",
            context.reportId(), "RVA-HISTORY"), USER_M4_SIGN))
            .andExpect(status().isOk())
            .andExpect(content().bytes(Files.readAllBytes(historicalFile)));
        mockMvc.perform(authorized(get(
            "/api/v1/pathology-reports/{id}/ofd-artifacts/{artifactId}/file",
            context.reportId(), signedArtifact.id()), USER_M4_NO_PERMISSION))
            .andExpect(status().isForbidden());
        mockMvc.perform(authorized(get(
            "/api/v1/pathology-reports/{id}/ofd-artifacts/{artifactId}/file",
            context.reportId(), "RVA-NOT-FOUND"), USER_M4_SIGN))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldRollbackSigningWhenOfdGenerationIsDisabled() throws Exception {
        ReportContext context = insertReviewedReport("DISABLED");
        saveSnapshotAsSigningDoctor(context);
        boolean previousEnabled = reportStorageProperties.isOfdEnabled();
        reportStorageProperties.setOfdEnabled(false);
        try {
            postJson("/api/v1/pathology-reports/%s/sign".formatted(context.reportId()), USER_M4_SIGN, "{}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("REPORT_ARTIFACT_GENERATION_FAILED"));
        } finally {
            reportStorageProperties.setOfdEnabled(previousEnabled);
        }

        assertSigningRolledBack(context);
    }

    @Test
    void shouldRollbackSigningWhenOfdMetadataInsertConflicts() throws Exception {
        ReportContext context = insertReviewedReport("DB-FAILURE");
        saveSnapshotAsSigningDoctor(context);
        namedParameterJdbcTemplate.update("""
            insert into report_version_artifacts
                (id, report_id, version_no, artifact_format, file_name, storage_key, content_type,
                 byte_size, sha256, generated_at)
            values
                ('RVA-CONFLICT', :reportId, 1, 'OFD', 'conflict.ofd', :storageKey, 'application/ofd',
                 1, :sha256, current_timestamp)
            """, new MapSqlParameterSource()
            .addValue("reportId", context.reportId())
            .addValue("storageKey", "formal/conflict/" + context.reportId() + ".ofd")
            .addValue("sha256", "a".repeat(64)));

        postJson("/api/v1/pathology-reports/%s/sign".formatted(context.reportId()), USER_M4_SIGN, "{}")
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("REPORT_ARTIFACT_GENERATION_FAILED"));

        assertSigningRolledBack(context);
        assertThat(queryInt("select count(*) from report_version_artifacts where report_id = :reportId", context.reportId()))
            .isEqualTo(1);
    }

    @Test
    void shouldLazilyRegenerateMissingOfdForAnAlreadySignedLegacyVersion() throws Exception {
        ReportContext context = insertReviewedReport("LAZY");
        saveSnapshotAsSigningDoctor(context);

        postJson("/api/v1/pathology-reports/%s/sign".formatted(context.reportId()), USER_M4_SIGN, "{}")
            .andExpect(status().isOk());
        ArtifactRow original = loadArtifact(context.reportId());
        Path originalFile = STORAGE_ROOT.resolve(original.storageKey());
        namedParameterJdbcTemplate.update(
            "update report_versions set artifact_id = null, render_snapshot = null where report_id = :reportId",
            new MapSqlParameterSource("reportId", context.reportId()));
        namedParameterJdbcTemplate.update(
            "delete from report_version_artifacts where report_id = :reportId",
            new MapSqlParameterSource("reportId", context.reportId()));
        Files.delete(originalFile);

        mockMvc.perform(authorized(get("/api/v1/pathology-reports/{id}/ofd", context.reportId()), USER_M4_SIGN))
            .andExpect(status().isAccepted())
            .andExpect(header().string("Retry-After", "2"))
            .andExpect(jsonPath("$.status").value("GENERATING"));
        awaitOfdReady(context.reportId());

        ArtifactRow regenerated = loadArtifact(context.reportId());
        assertThat(regenerated.storageKey()).isNotEqualTo(original.storageKey());
        assertThat(STORAGE_ROOT.resolve(regenerated.storageKey())).isRegularFile();
        assertThat(queryInt("select count(*) from report_version_artifacts where report_id = :reportId", context.reportId()))
            .isEqualTo(1);

        mockMvc.perform(authorized(get("/api/v1/pathology-reports/{id}/ofd", context.reportId()), USER_M4_SIGN))
            .andExpect(status().isOk())
            .andExpect(content().bytes(Files.readAllBytes(STORAGE_ROOT.resolve(regenerated.storageKey()))));
        assertThat(queryInt("select count(*) from report_version_artifacts where report_id = :reportId", context.reportId()))
            .isEqualTo(1);
    }

    @Test
    void shouldRegenerateOfdWhenTrackedFileWasRemoved() throws Exception {
        ReportContext context = insertReviewedReport("MISSING-FILE");
        saveSnapshotAsSigningDoctor(context);

        postJson("/api/v1/pathology-reports/%s/sign".formatted(context.reportId()), USER_M4_SIGN, "{}")
            .andExpect(status().isOk());
        ArtifactRow original = loadArtifact(context.reportId());
        Files.delete(STORAGE_ROOT.resolve(original.storageKey()));

        mockMvc.perform(authorized(get("/api/v1/pathology-reports/{id}/ofd", context.reportId()), USER_M4_SIGN))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("GENERATING"));
        awaitOfdReady(context.reportId());

        ArtifactRow regenerated = loadArtifact(context.reportId());
        assertThat(regenerated.storageKey()).isEqualTo(original.storageKey());
        assertThat(STORAGE_ROOT.resolve(regenerated.storageKey())).isRegularFile();
        assertThat(queryInt("select count(*) from report_version_artifacts where report_id = :reportId", context.reportId()))
            .isEqualTo(1);

        mockMvc.perform(authorized(get("/api/v1/pathology-reports/{id}/ofd", context.reportId()), USER_M4_SIGN))
            .andExpect(status().isOk())
            .andExpect(content().bytes(Files.readAllBytes(STORAGE_ROOT.resolve(regenerated.storageKey()))));
        assertThat(queryInt("select count(*) from report_version_artifacts where report_id = :reportId", context.reportId()))
            .isEqualTo(1);
    }

    @Test
    void shouldExposeFailedStatusAndCleanTemporaryFileWhenBackgroundRepairFails() throws Exception {
        ReportContext context = insertReviewedReport("REPAIR-FAILURE");
        saveSnapshotAsSigningDoctor(context);
        postJson("/api/v1/pathology-reports/%s/sign".formatted(context.reportId()), USER_M4_SIGN, "{}")
            .andExpect(status().isOk());
        ArtifactRow original = loadArtifact(context.reportId());
        Path target = STORAGE_ROOT.resolve(original.storageKey());
        Files.delete(target);
        Files.createDirectories(target);
        Files.writeString(target.resolve("move-blocker.txt"), "block move");

        mockMvc.perform(authorized(get("/api/v1/pathology-reports/{id}/ofd/status", context.reportId())
                .queryParam("retryFailed", "true"), USER_M4_SIGN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("GENERATING"));

        JsonNode failed = awaitOfdStatus(context.reportId(), "FAILED");
        assertThat(failed.path("message").asText()).contains("生成失败");
        mockMvc.perform(authorized(get("/api/v1/pathology-reports/{id}/ofd", context.reportId()), USER_M4_SIGN))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value("FAILED"));
        assertThat(loadArtifact(context.reportId()).storageKey()).isEqualTo(original.storageKey());
        assertThat(target).isDirectory();
        try (var temporaryFiles = Files.walk(STORAGE_ROOT.resolve(".tmp"))) {
            assertThat(temporaryFiles.filter(Files::isRegularFile)).isEmpty();
        }
    }

    @Test
    void shouldCreateOnlyOneOfdArtifactForConcurrentLazyRequests() throws Exception {
        ReportContext context = insertReviewedReport("LAZY-CONCURRENT");
        saveSnapshotAsSigningDoctor(context);
        postJson("/api/v1/pathology-reports/%s/sign".formatted(context.reportId()), USER_M4_SIGN, "{}")
            .andExpect(status().isOk());
        ArtifactRow original = loadArtifact(context.reportId());
        namedParameterJdbcTemplate.update(
            "update report_versions set artifact_id = null where report_id = :reportId",
            new MapSqlParameterSource("reportId", context.reportId()));
        namedParameterJdbcTemplate.update(
            "delete from report_version_artifacts where report_id = :reportId",
            new MapSqlParameterSource("reportId", context.reportId()));
        Files.delete(STORAGE_ROOT.resolve(original.storageKey()));

        var firstRequest = authorized(get("/api/v1/pathology-reports/{id}/ofd/status", context.reportId()), USER_M4_SIGN);
        var secondRequest = authorized(get("/api/v1/pathology-reports/{id}/ofd/status", context.reportId()), USER_M4_SIGN);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = executor.submit(() -> executeConcurrentOfdRequest(firstRequest, ready, start));
            Future<String> second = executor.submit(() -> executeConcurrentOfdRequest(secondRequest, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(first.get(15, TimeUnit.SECONDS)).isIn("GENERATING", "READY");
            assertThat(second.get(15, TimeUnit.SECONDS)).isIn("GENERATING", "READY");
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
        awaitOfdReady(context.reportId());
        assertThat(queryInt("select count(*) from report_version_artifacts where report_id = :reportId", context.reportId()))
            .isEqualTo(1);
    }

    private String executeConcurrentOfdRequest(
        MockHttpServletRequestBuilder request,
        CountDownLatch ready,
        CountDownLatch start
    ) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start concurrent OFD request");
        }
        return responseBody(mockMvc.perform(request), 200).path("status").asText();
    }

    private void awaitOfdReady(String reportId) throws Exception {
        awaitOfdStatus(reportId, "READY");
    }

    private JsonNode awaitOfdStatus(String reportId, String expectedStatus) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime() < deadline) {
            JsonNode response = responseBody(mockMvc.perform(authorized(
                get("/api/v1/pathology-reports/{id}/ofd/status", reportId), USER_M4_SIGN)), 200);
            String ofdStatus = response.path("status").asText();
            if (expectedStatus.equals(ofdStatus)) {
                return response;
            }
            assertThat(ofdStatus).isEqualTo("GENERATING");
            Thread.sleep(Math.max(50, Math.min(250, response.path("retryAfterMs").asInt(50))));
        }
        throw new AssertionError("Timed out waiting for OFD status " + expectedStatus + ": " + reportId);
    }

    private void saveSnapshotAsSigningDoctor(ReportContext context) throws Exception {
        postJson("/api/v1/pathology-reports/%s/save-draft".formatted(context.reportId()), USER_M4_SIGN, """
            {
              "clinicalDiagnosis": "临床诊断",
              "grossExam": "大体所见",
              "microscopicExam": "镜下所见",
              "finalDiagnosis": "集成测试中文诊断",
              "richTextContent": "<p>集成测试中文报告</p>",
              "renderSnapshot": {
                "schemaVersion": 1,
                "templateCode": "classic-default",
                "accentColor": "#1f4e79",
                "hospitalName": "南海人民医院病理科",
                "reportTitle": "病理检查报告单",
                "reportNo": "UNTRUSTED-REPORT-NO",
                "deliveredAt": "2026-08-25 09:00:00",
                "metaFields": [{"label": "患者姓名:", "value": "张三"}],
                "footerFields": [],
                "sections": [
                  {"label": "大体所见", "minHeight": 82, "value": "大体所见"},
                  {"label": "镜下所见", "minHeight": 160, "value": "镜下所见", "images": []},
                  {"label": "病理诊断", "minHeight": 92, "value": "集成测试中文诊断"}
                ],
                "note": "仅供临床参考"
              }
            }
            """).andExpect(status().isOk());
    }

    private ReportContext insertReviewedReport(String suffix) {
        String normalizedSuffix = suffix + "-" + UUID.randomUUID().toString().substring(0, 8);
        String applicationId = "APP-OFD-" + normalizedSuffix;
        String caseId = "CASE-OFD-" + normalizedSuffix;
        String taskId = "TASK-OFD-" + normalizedSuffix;
        String reportId = "REPORT-OFD-" + normalizedSuffix;
        String reportNo = "RP-OFD-" + normalizedSuffix;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("applicationId", applicationId)
            .addValue("applicationNo", "APPLY-OFD-" + normalizedSuffix)
            .addValue("caseId", caseId)
            .addValue("taskId", taskId)
            .addValue("reportId", reportId)
            .addValue("reportNo", reportNo)
            .addValue("pathologyNo", "P-OFD-" + normalizedSuffix);
        namedParameterJdbcTemplate.update("""
            insert into applications (id, application_no, application_type, status)
            values (:applicationId, :applicationNo, 'ROUTINE', 'RECEIVED')
            """, parameters);
        namedParameterJdbcTemplate.update("""
            insert into pathology_cases (id, application_id, pathology_no, case_status)
            values (:caseId, :applicationId, :pathologyNo, 'DIAGNOSING')
            """, parameters);
        namedParameterJdbcTemplate.update("""
            insert into diagnostic_tasks
                (id, case_id, pathology_no, task_type, status, diagnosis_doctor_user_id,
                 primary_doctor_user_id, reviewer_user_id, reviewer_name)
            values (:taskId, :caseId, :pathologyNo, 'PRIMARY', 'REVIEWED', 'USER_M4_DIAGNOSIS',
                    'USER_M4_DIAGNOSIS', 'USER_M4_REVIEW', 'M4 Review')
            """, parameters);
        namedParameterJdbcTemplate.update("""
            insert into pathology_reports
                (id, case_id, task_id, report_no, pathology_no, report_scope, report_seq,
                 report_status, version_no, gross_exam, microscopic_exam, clinical_diagnosis,
                 final_diagnosis, reviewer_user_id, reviewer_name, reviewed_at, rich_text_content)
            values (:reportId, :caseId, :taskId, :reportNo, :pathologyNo, 'ROUTINE', 1,
                    'REVIEWED', 1, '大体所见', '镜下所见', '临床诊断', '集成测试中文诊断',
                    'USER_M4_REVIEW', 'M4 Review', current_timestamp, '<p>集成测试中文报告</p>')
            """, parameters);
        return new ReportContext(caseId, reportId, reportNo);
    }

    private ArtifactRow loadArtifact(String reportId) {
        return namedParameterJdbcTemplate.queryForObject("""
            select id, file_name, storage_key, content_type, byte_size, sha256
            from report_version_artifacts
            where report_id = :reportId
            """, new MapSqlParameterSource("reportId", reportId), (resultSet, rowNum) -> new ArtifactRow(
            resultSet.getString("id"),
            resultSet.getString("file_name"),
            resultSet.getString("storage_key"),
            resultSet.getString("content_type"),
            resultSet.getLong("byte_size"),
            resultSet.getString("sha256")));
    }

    private void assertSigningRolledBack(ReportContext context) {
        assertThat(queryString("select report_status from pathology_reports where id = :reportId", context.reportId()))
            .isEqualTo("REVIEWED");
        assertThat(queryString("select case_status from pathology_cases where id = :caseId", context.caseId()))
            .isEqualTo("DIAGNOSING");
        assertThat(queryInt("select count(*) from report_versions where report_id = :reportId", context.reportId()))
            .isZero();
        assertThat(queryInt("""
            select count(*) from workflow_events where case_id = :reportId and event_type = 'SIGN'
            """, context.caseId())).isZero();
    }

    private int queryInt(String sql, String reportId) {
        Integer value = namedParameterJdbcTemplate.queryForObject(
            sql, new MapSqlParameterSource("reportId", reportId).addValue("caseId", reportId), Integer.class);
        return value == null ? 0 : value;
    }

    private String queryString(String sql, String id) {
        return namedParameterJdbcTemplate.queryForObject(
            sql, new MapSqlParameterSource("reportId", id).addValue("caseId", id), String.class);
    }

    private record ArtifactRow(
        String id,
        String fileName,
        String storageKey,
        String contentType,
        long byteSize,
        String sha256
    ) {
    }

    private record ReportContext(String caseId, String reportId, String reportNo) {
    }
}
