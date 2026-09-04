package com.company.bl.application.service;

import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.infrastructure.config.ReportStorageProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ofdrw.reader.ContentExtractor;
import org.ofdrw.reader.OFDReader;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ReportArtifactServiceTest {

    @TempDir
    Path storageRoot;

    @Mock
    DiagnosticReportRepository diagnosticReportRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ReportArtifactService reportArtifactService;

    @BeforeEach
    void setUp() {
        ReportStorageProperties properties = new ReportStorageProperties();
        properties.setOfdEnabled(true);
        properties.setRootDir(storageRoot);
        reportArtifactService = new ReportArtifactService(diagnosticReportRepository, objectMapper, properties);
    }

    @Test
    void shouldGenerateReadableA4OfdWithAuthoritativeFieldsChineseStructuredContentAndHash() throws Exception {
        String longChineseText = "镜下见异型细胞浸润性生长。".repeat(800);
        JsonNode snapshot = objectMapper.readTree("""
            {
              "schemaVersion": 1,
              "templateCode": "wysiwyg-template",
              "accentColor": "#1f4e79",
              "hospitalName": "南海人民医院病理科",
              "reportTitle": "病理检查报告单",
              "reportNo": "前端伪造报告号",
              "deliveredAt": "2026-08-25 09:00:00",
              "metaFields": [
                {"label": "报告号:", "value": "MALICIOUS-REPORT-NO"},
                {"label": "患者姓名:", "value": "张三"},
                {"label": "病人ID:", "value": "08305"}
              ],
              "footerFields": [
                {"label": "诊断医师:", "value": "前端伪造医生"},
                {"label": "审核医师:", "value": "审核医生"}
              ],
              "sections": [
                {"label": "大体所见：", "minHeight": 82, "value": "灰白组织一块"},
                {"label": "镜下所见：", "minHeight": 160, "value": "%s", "images": []},
                {"label": "病理诊断：", "minHeight": 92, "value": "中文诊断结论"}
              ],
              "structuredBlocks": {
                "fieldValues": {"分期": "II期"},
                "checkboxValues": {"检测结果": ["阳性"]},
                "sectionValues": {"补充说明": "结构化内容"},
                "tableValues": {"指标::结果": "已检出"}
              },
              "note": "本报告仅供临床参考"
            }
            """.formatted(longChineseText));
        String serialized = reportArtifactService.validateAndSerializeRenderSnapshot(snapshot, "CASE-1");
        LocalDateTime signedAt = LocalDateTime.of(2026, 8, 25, 10, 30);

        ReportArtifactService.GeneratedReportArtifact artifact = reportArtifactService.createOfdArtifact(
            report(serialized), "RVA-1", "权威签发医生", signedAt);

        Path ofd = storageRoot.resolve(artifact.storageKey());
        assertThat(ofd).isRegularFile();
        assertThat(artifact.artifactFormat()).isEqualTo("OFD");
        assertThat(artifact.contentType()).isEqualTo("application/ofd");
        assertThat(artifact.byteSize()).isEqualTo(Files.size(ofd));
        assertThat(artifact.sha256()).isEqualTo(sha256(ofd));

        try (OFDReader reader = new OFDReader(ofd)) {
            assertThat(reader.getNumberOfPages()).isGreaterThan(1);
            assertThat(reader.getPageList().get(0).getSize().getWidth()).isEqualTo(210d);
            assertThat(reader.getPageList().get(0).getSize().getHeight()).isEqualTo(297d);
            String extracted = String.join("", new ContentExtractor(reader).extractAll());
            assertThat(extracted)
                .contains("南海人民医院病理科")
                .contains("RPT-20260825-001")
                .contains("张三")
                .contains("病人ID:08305")
                .contains("中文诊断结论")
                .contains("结构化内容")
                .contains("权威签发医生")
                .contains("审核医生")
                .contains("签发时间：2026-08-25 10:30:00")
                .doesNotContain("MALICIOUS-REPORT-NO")
                .doesNotContain("前端伪造医生")
                .doesNotContain("2026-08-25T10:30");
        }
    }

    @Test
    void shouldReplaceUuidShapedPatientIdInOfd() throws Exception {
        JsonNode snapshot = objectMapper.readTree("""
            {
              "schemaVersion": 1,
              "templateCode": "wysiwyg-template",
              "hospitalName": "南方医科大学南方医院病理科",
              "reportTitle": "病理检查报告单",
              "metaFields": [
                {"label": "病人ID:", "value": "b79de586-d38a-4d9c-8158-a3dc5355886a"}
              ],
              "footerFields": [],
              "sections": [
                {"label": "病理诊断及建议:", "minHeight": 92, "value": "诊断结论"}
              ]
            }
            """);
        String serialized = reportArtifactService.validateAndSerializeRenderSnapshot(snapshot, "CASE-1");

        ReportArtifactService.GeneratedReportArtifact artifact = reportArtifactService.createOfdArtifact(
            report(serialized), "RVA-UUID", "权威签发医生", LocalDateTime.of(2026, 8, 25, 10, 30));

        try (OFDReader reader = new OFDReader(storageRoot.resolve(artifact.storageKey()))) {
            String extracted = String.join("", new ContentExtractor(reader).extractAll());
            assertThat(extracted)
                .contains("病人ID:-")
                .doesNotContain("b79de586-d38a-4d9c-8158-a3dc5355886a");
        }
    }

    @Test
    void shouldRestoreCurrentPatientIdDisplayWhenRegeneratingMissingOfd() throws Exception {
        String uuid = "b79de586-d38a-4d9c-8158-a3dc5355886a";
        String snapshot = """
            {
              "schemaVersion": 1,
              "templateCode": "wysiwyg-template",
              "metaFields": [
                {"label": "病人ID:", "value": "%s"}
              ],
              "footerFields": [
                {"label": "病人ID:", "value": "-"}
              ],
              "sections": [
                {"label": "病理诊断及建议:", "minHeight": 92, "value": "诊断结论"}
              ]
            }
            """.formatted(uuid);
        QueueingExecutor executor = new QueueingExecutor();
        ReportStorageProperties properties = new ReportStorageProperties();
        properties.setOfdEnabled(true);
        properties.setRootDir(storageRoot);
        ReportArtifactService service = new ReportArtifactService(
            diagnosticReportRepository, objectMapper, properties, executor, null, null);
        DiagnosticReportRepository.DiagnosticTask task = diagnosticTask("CASE-1", "USER-SIGN", "08310");
        when(diagnosticReportRepository.findDiagnosticTaskById("TASK-1")).thenReturn(Optional.of(task));
        when(diagnosticReportRepository.findPathologyReportById("RPT-1"))
            .thenReturn(Optional.of(report(snapshot)));
        when(diagnosticReportRepository.findReportVersionArtifact("RPT-1", 1, "OFD"))
            .thenReturn(Optional.empty());
        LocalDateTime signedAt = LocalDateTime.of(2026, 8, 25, 10, 30);
        when(diagnosticReportRepository.findLatestFormalReportVersion("RPT-1", 1))
            .thenReturn(Optional.of(new DiagnosticReportRepository.ReportVersion(
                "RV-1", "RPT-1", "CASE-1", "ROUTINE", 1, 1, "FORMAL",
                "诊断", "报告", snapshot, null, "SIGNER-1", "签发医生", signedAt, signedAt,
                "NOT_PRINTED", null, "PENDING", null, null, null, null)));

        assertThat(service.prepareOfdArtifact("RPT-1", "USER-SIGN", "M4_SIGN", false).status())
            .isEqualTo("GENERATING");
        executor.runNext();

        Path generated;
        try (var files = Files.walk(storageRoot.resolve("formal"))) {
            generated = files
                .filter(path -> path.toString().endsWith(".ofd"))
                .findFirst()
                .orElseThrow();
        }
        try (OFDReader reader = new OFDReader(generated)) {
            String extracted = String.join("", new ContentExtractor(reader).extractAll());
            assertThat(extracted)
                .contains("病人ID:08310")
                .doesNotContain(uuid);
        }
    }

    @Test
    void shouldKeepPatientIdMaskedWhenCurrentDisplayIdIsUnavailableDuringRegeneration() throws Exception {
        String uuid = "b79de586-d38a-4d9c-8158-a3dc5355886a";
        String snapshot = """
            {
              "schemaVersion": 1,
              "templateCode": "wysiwyg-template",
              "metaFields": [{"label": "病人ID:", "value": "%s"}],
              "footerFields": [],
              "sections": [{"label": "病理诊断:", "minHeight": 92, "value": "诊断结论"}]
            }
            """.formatted(uuid);
        QueueingExecutor executor = new QueueingExecutor();
        ReportStorageProperties properties = new ReportStorageProperties();
        properties.setOfdEnabled(true);
        properties.setRootDir(storageRoot);
        ReportArtifactService service = new ReportArtifactService(
            diagnosticReportRepository, objectMapper, properties, executor, null, null);
        DiagnosticReportRepository.DiagnosticTask task = diagnosticTask("CASE-1", "USER-SIGN", null);
        when(diagnosticReportRepository.findDiagnosticTaskById("TASK-1")).thenReturn(Optional.of(task));
        when(diagnosticReportRepository.findPathologyReportById("RPT-1"))
            .thenReturn(Optional.of(report(snapshot)));
        when(diagnosticReportRepository.findReportVersionArtifact("RPT-1", 1, "OFD"))
            .thenReturn(Optional.empty());
        LocalDateTime signedAt = LocalDateTime.of(2026, 8, 25, 10, 30);
        when(diagnosticReportRepository.findLatestFormalReportVersion("RPT-1", 1))
            .thenReturn(Optional.of(new DiagnosticReportRepository.ReportVersion(
                "RV-1", "RPT-1", "CASE-1", "ROUTINE", 1, 1, "FORMAL",
                "诊断", "报告", snapshot, null, "SIGNER-1", "签发医生", signedAt, signedAt,
                "NOT_PRINTED", null, "PENDING", null, null, null, null)));

        assertThat(service.prepareOfdArtifact("RPT-1", "USER-SIGN", "M4_SIGN", false).status())
            .isEqualTo("GENERATING");
        executor.runNext();

        Path generated;
        try (var files = Files.walk(storageRoot.resolve("formal"))) {
            generated = files.filter(path -> path.toString().endsWith(".ofd")).findFirst().orElseThrow();
        }
        try (OFDReader reader = new OFDReader(generated)) {
            String extracted = String.join("", new ContentExtractor(reader).extractAll());
            assertThat(extracted).contains("病人ID:-").doesNotContain(uuid);
        }
    }

    @Test
    void shouldUseUnifiedRoutineLayoutForSpecializedTemplateSnapshot() throws Exception {
        JsonNode snapshot = objectMapper.readTree("""
            {
              "schemaVersion": 1,
              "layoutCode": "routine-pathology-report-v2",
              "templateCode": "cell-dna-quantification",
              "accentColor": "#005c99",
              "hospitalName": "佛山市中医院病理科",
              "reportTitle": "病理检查报告单",
              "metaFields": [
                {"class": "routine-row-1 routine-meta-patientName", "label": "姓名:", "value": "张三"},
                {"class": "routine-row-1 routine-meta-bedNo", "label": "床号:", "value": "B-12"},
                {"class": "routine-row-3 routine-meta-wardName", "label": "护理单元:", "value": "外科病区"}
              ],
              "footerFields": [
                {"label": "取材医师:", "value": "取材医生甲、取材医生乙"}
              ],
              "sections": [
                {"label": "病理诊断及建议:", "minHeight": 92, "value": "DNA检测结论：未见异常。"}
              ],
              "note": ""
            }
            """);
        String serialized = reportArtifactService.validateAndSerializeRenderSnapshot(snapshot, "CASE-1");

        ReportArtifactService.GeneratedReportArtifact artifact = reportArtifactService.createOfdArtifact(
            report(serialized), "RVA-SPECIALIZED", "权威签发医生", LocalDateTime.of(2026, 8, 25, 10, 30));

        String contentXml;
        try (ZipFile archive = new ZipFile(storageRoot.resolve(artifact.storageKey()).toFile())) {
            var contentEntry = archive.stream()
                .filter(entry -> entry.getName().endsWith("Pages/Page_0/Content.xml"))
                .findFirst()
                .orElseThrow();
            contentXml = new String(archive.getInputStream(contentEntry).readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        }
        try (OFDReader reader = new OFDReader(storageRoot.resolve(artifact.storageKey()))) {
            String extracted = String.join("", new ContentExtractor(reader).extractAll());
            assertThat(extracted)
                .contains("佛山市中医院病理科")
                .contains("病理检查报告单")
                .contains("张三")
                .contains("床号:B-12")
                .contains("护理单元:外科病区")
                .contains("病理诊断及建议:")
                .contains("DNA检测结论：未见异常。")
                .doesNotContain("结构化内容");
        }
        assertThat(contentXml)
            .contains("<ofd:TextCode X=\"70\" Y=\"8\"")
            .contains("<ofd:TextCode X=\"79.500\" Y=\"14.500\"")
            .contains("M 0.150 0 L 0.150 6.850")
            .contains("M 193.850 0 L 193.850 6.850");
        assertThat(countOccurrences(contentXml, "M 0.150 0 L 0.150"))
            .isGreaterThanOrEqualTo(2);
        assertThat(countOccurrences(contentXml, "M 193.850 0 L 193.850"))
            .isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldNotEmbedAFontInGeneratedOfd() throws Exception {
        ReportArtifactService.GeneratedReportArtifact artifact = reportArtifactService.createOfdArtifact(
            report("{\"schemaVersion\":1,\"templateCode\":\"default\",\"sections\":[]}"),
            "RVA-FONT", "签发医生", LocalDateTime.now());

        try (ZipFile archive = new ZipFile(storageRoot.resolve(artifact.storageKey()).toFile())) {
            assertThat(archive.stream().anyMatch(entry -> entry.getName().endsWith(".ttf") || entry.getName().endsWith(".otf")))
                .isFalse();
        }
    }

    @Test
    void shouldReadExistingOfdWithoutTakingReportRowLock() throws Exception {
        Path storedFile = storageRoot.resolve("formal/2026/08/RPT-1/RVA-1.ofd");
        Files.createDirectories(storedFile.getParent());
        Files.writeString(storedFile, "ofd");
        DiagnosticReportRepository.ReportVersionArtifact artifact = new DiagnosticReportRepository.ReportVersionArtifact(
            "RVA-1", "RPT-1", 1, "OFD", "RPT-20260825-001-V1.ofd",
            "formal/2026/08/RPT-1/RVA-1.ofd", "application/ofd", 3, "a".repeat(64), LocalDateTime.now());
        when(diagnosticReportRepository.findPathologyReportById("RPT-1")).thenReturn(Optional.of(report(null)));
        when(diagnosticReportRepository.findReportVersionArtifact("RPT-1", 1, "OFD")).thenReturn(Optional.of(artifact));

        assertThat(reportArtifactService.ensureAndReadOfdArtifact("RPT-1", "USER-SIGN", "M4_SIGN").fileName())
            .isEqualTo("RPT-20260825-001-V1.ofd");
        verify(diagnosticReportRepository, never()).lockPathologyReport("RPT-1");
    }

    @Test
    void shouldConvertStoredOfdToTemporaryPdfAndDeleteItOnClose() throws Exception {
        ReportArtifactService.GeneratedReportArtifact generated = reportArtifactService.createOfdArtifact(
            report("{\"schemaVersion\":1,\"templateCode\":\"default\",\"sections\":[]}"),
            "RVA-PDF", "签发医生", LocalDateTime.now());
        DiagnosticReportRepository.ReportVersionArtifact artifact = new DiagnosticReportRepository.ReportVersionArtifact(
            generated.id(), generated.reportId(), generated.versionNo(), generated.artifactFormat(),
            generated.fileName(), generated.storageKey(), generated.contentType(), generated.byteSize(),
            generated.sha256(), generated.generatedAt());
        when(diagnosticReportRepository.findPathologyReportById("RPT-1")).thenReturn(Optional.of(report(null)));
        when(diagnosticReportRepository.findReportVersionArtifact("RPT-1", 1, "OFD"))
            .thenReturn(Optional.of(artifact));

        Path pdfPath;
        try (ReportArtifactService.TemporaryReportResource pdf = reportArtifactService.convertOfdArtifactToPdf(
            "RPT-1", "USER-SIGN", "M4_SIGN")) {
            pdfPath = pdf.path();
            assertThat(pdf.contentType().toString()).isEqualTo("application/pdf");
            assertThat(pdf.fileName()).endsWith(".pdf");
            assertThat(pdf.byteSize()).isEqualTo(Files.size(pdf.path()));
            assertThat(Files.readAllBytes(pdf.path()))
                .startsWith("%PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        }

        assertThat(pdfPath).doesNotExist();
    }

    @Test
    void shouldKeepOfdRepairSingleFlightExposeFailedStateAndRetryOnlyWhenExplicit() {
        QueueingExecutor executor = new QueueingExecutor();
        ReportStorageProperties properties = new ReportStorageProperties();
        properties.setOfdEnabled(true);
        properties.setRootDir(storageRoot);
        ReportArtifactService asynchronousService = new ReportArtifactService(
            diagnosticReportRepository, objectMapper, properties, executor, null, null);
        when(diagnosticReportRepository.findPathologyReportById("RPT-1"))
            .thenReturn(Optional.of(report(null)));
        when(diagnosticReportRepository.findReportVersionArtifact("RPT-1", 1, "OFD"))
            .thenReturn(Optional.empty());
        when(diagnosticReportRepository.findLatestFormalReportVersion("RPT-1", 1))
            .thenReturn(Optional.empty());

        ReportArtifactService.ReportOfdStatus first = asynchronousService.prepareOfdArtifact(
            "RPT-1", "USER-SIGN", "M4_SIGN", false);
        ReportArtifactService.ReportOfdStatus duplicate = asynchronousService.prepareOfdArtifact(
            "RPT-1", "USER-SIGN", "M4_SIGN", false);

        assertThat(first.status()).isEqualTo("GENERATING");
        assertThat(first.retryAfterMs()).isEqualTo(2_000);
        assertThat(duplicate.status()).isEqualTo("GENERATING");
        assertThat(executor.size()).isEqualTo(1);

        executor.runNext();

        ReportArtifactService.ReportOfdStatus failed = asynchronousService.prepareOfdArtifact(
            "RPT-1", "USER-SIGN", "M4_SIGN", false);
        assertThat(failed.status()).isEqualTo("FAILED");
        assertThat(executor.size()).isZero();

        ReportArtifactService.ReportOfdStatus retrying = asynchronousService.prepareOfdArtifact(
            "RPT-1", "USER-SIGN", "M4_SIGN", true);
        assertThat(retrying.status()).isEqualTo("GENERATING");
        assertThat(executor.size()).isEqualTo(1);
    }

    @Test
    void shouldExposeMissingSnapshotImageInsteadOfGenericWorkerFailure() throws Exception {
        String assetId = "RRA-33333333-3333-3333-3333-333333333333";
        String snapshot = """
            {
              "schemaVersion": 1,
              "templateCode": "wysiwyg-template",
              "sections": [{
                "label": "镜下所见：",
                "minHeight": 160,
                "value": "图片缺失",
                "images": [{"assetId": "%s", "left": 0, "top": 0}]
              }]
            }
            """.formatted(assetId);
        DiagnosticReportRepository.ReportRenderAsset missingAsset =
            new DiagnosticReportRepository.ReportRenderAsset(
                assetId, "CASE-1", "missing.png", "assets/missing.png", "image/png",
                1, "a".repeat(64), LocalDateTime.now());
        when(diagnosticReportRepository.findReportRenderAssetById(assetId)).thenReturn(Optional.of(missingAsset));
        ReportStorageProperties properties = new ReportStorageProperties();
        properties.setOfdEnabled(true);
        properties.setRootDir(storageRoot);
        QueueingExecutor executor = new QueueingExecutor();
        ReportArtifactService workerService = new ReportArtifactService(
            diagnosticReportRepository, objectMapper, properties, executor,
            new OfdRenderWorkerExecutor(objectMapper, properties), null);
        LocalDateTime signedAt = LocalDateTime.now();
        when(diagnosticReportRepository.findPathologyReportById("RPT-1"))
            .thenReturn(Optional.of(report(snapshot)));
        when(diagnosticReportRepository.findReportVersionArtifact("RPT-1", 1, "OFD"))
            .thenReturn(Optional.empty());
        when(diagnosticReportRepository.findLatestFormalReportVersion("RPT-1", 1))
            .thenReturn(Optional.of(new DiagnosticReportRepository.ReportVersion(
                "RV-1", "RPT-1", "CASE-1", "ROUTINE", 1, 1, "FORMAL",
                "诊断", "报告", snapshot, null, "SIGNER-1", "签发医生", signedAt, signedAt,
                "NOT_PRINTED", null, "PENDING", null, null, null, null)));

        assertThat(workerService.prepareOfdArtifact("RPT-1", "USER-SIGN", "M4_SIGN", false).status())
            .isEqualTo("GENERATING");
        executor.runNext();
        ReportArtifactService.ReportOfdStatus failed = workerService.prepareOfdArtifact(
            "RPT-1", "USER-SIGN", "M4_SIGN", false);

        assertThat(failed.status()).isEqualTo("FAILED");
        assertThat(failed.message()).contains("图片文件已丢失");

        assertThat(storageRoot.resolve("formal")).doesNotExist();
        try (var temporaryFiles = Files.list(storageRoot.resolve(".tmp"))) {
            assertThat(temporaryFiles).isEmpty();
        }
    }

    @Test
    void shouldRejectUnknownSnapshotVersionAndCrossCaseImageReference() throws Exception {
        JsonNode unknownVersion = objectMapper.readTree("""
            {"schemaVersion": 2, "templateCode": "default", "sections": []}
            """);
        assertThatThrownBy(() -> reportArtifactService.validateAndSerializeRenderSnapshot(unknownVersion, "CASE-1"))
            .isInstanceOf(BlBusinessException.class)
            .extracting("httpStatus")
            .isEqualTo(400);

        String assetId = "RRA-11111111-1111-1111-1111-111111111111";
        when(diagnosticReportRepository.findReportRenderAssetById(assetId)).thenReturn(Optional.of(
            new DiagnosticReportRepository.ReportRenderAsset(
                assetId, "CASE-2", "image.png", "assets/2026/08/image.png", "image/png", 100, "a".repeat(64),
                LocalDateTime.now())));
        JsonNode crossCaseSnapshot = objectMapper.readTree("""
            {
              "schemaVersion": 1,
              "templateCode": "default",
              "sections": [{
                "label": "镜下所见：",
                "minHeight": 160,
                "value": "",
                "images": [{
                  "assetId": "%s",
                  "title": "cross-case.png",
                  "left": 12,
                  "top": 12
                }]
              }]
            }
            """.formatted(assetId));

        assertThatThrownBy(() -> reportArtifactService.validateAndSerializeRenderSnapshot(crossCaseSnapshot, "CASE-1"))
            .isInstanceOf(BlBusinessException.class)
            .extracting("httpStatus")
            .isEqualTo(409);

        JsonNode browserUrlSnapshot = objectMapper.readTree("""
            {
              "schemaVersion": 1,
              "templateCode": "default",
              "sections": [{
                "label": "镜下所见",
                "minHeight": 160,
                "value": "",
                "images": [{
                  "assetId": "RRA-33333333-3333-3333-3333-333333333333",
                  "fileUrl": "https://untrusted.example/image.png",
                  "left": 0,
                  "top": 0
                }]
              }]
            }
            """);
        assertThatThrownBy(() -> reportArtifactService.validateAndSerializeRenderSnapshot(browserUrlSnapshot, "CASE-1"))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("assetId");
    }

    @Test
    void shouldStoreOnlyImageContentMatchingTheDeclaredType() throws Exception {
        allowAssetEditing("CASE-1", "USER-1");
        byte[] png = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0
        };
        MockMultipartFile validImage = new MockMultipartFile(
            "file", "../镜下图像.png", "image/png", png);

        DiagnosticReportModels.ReportRenderAssetResult stored = reportArtifactService.storeRenderAsset(
            "CASE-1", "USER-1", validImage);

        assertThat(stored.assetId()).startsWith("RRA-");
        assertThat(stored.fileName()).isEqualTo("镜下图像.png");
        assertThat(storageRoot.resolve("assets")).isDirectoryRecursivelyContaining("glob:**/*.png");
        verify(diagnosticReportRepository).insertReportRenderAsset(
            org.mockito.ArgumentMatchers.argThat(command -> command.byteSize() == png.length
                && command.contentType().equals("image/png")
                && command.storageKey().startsWith("assets/")));

        MockMultipartFile disguisedImage = new MockMultipartFile(
            "file", "disguised.png", "image/png", "not-an-image".getBytes());
        assertThatThrownBy(() -> reportArtifactService.storeRenderAsset("CASE-1", "USER-1", disguisedImage))
            .isInstanceOf(BlBusinessException.class)
            .extracting("httpStatus")
            .isEqualTo(400);
    }

    @Test
    void shouldEmbedCaseOwnedImageAndReopenTheGeneratedOfd() throws Exception {
        String assetId = "RRA-44444444-4444-4444-4444-444444444444";
        Path image = storageRoot.resolve("assets/2026/08/microscopic.png");
        Files.createDirectories(image.getParent());
        Files.write(image, Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="));
        when(diagnosticReportRepository.findReportRenderAssetById(assetId)).thenReturn(Optional.of(
            new DiagnosticReportRepository.ReportRenderAsset(
                assetId, "CASE-1", "microscopic.png", "assets/2026/08/microscopic.png", "image/png",
                Files.size(image), "a".repeat(64), LocalDateTime.now())));
        JsonNode snapshot = objectMapper.readTree("""
            {
              "schemaVersion": 1,
              "templateCode": "default",
              "sections": [{
                "label": "镜下所见",
                "minHeight": 160,
                "value": "含镜下图片",
                "images": [{
                  "assetId": "%s",
                  "title": "microscopic.png",
                  "left": 96,
                  "top": 48
                }]
              }]
            }
            """.formatted(assetId));
        String serialized = reportArtifactService.validateAndSerializeRenderSnapshot(snapshot, "CASE-1");

        ReportArtifactService.GeneratedReportArtifact artifact = reportArtifactService.createOfdArtifact(
            report(serialized), "RVA-IMAGE", "权威签发医生", LocalDateTime.now());

        try (OFDReader reader = new OFDReader(storageRoot.resolve(artifact.storageKey()))) {
            assertThat(reader.getNumberOfPages()).isPositive();
            assertThat(String.join("", new ContentExtractor(reader).extractAll())).contains("含镜下图片");
        }
    }

    @Test
    void shouldDeleteRenderAssetFileOnlyAfterTransactionCommit() throws Exception {
        String assetId = "RRA-22222222-2222-2222-2222-222222222222";
        Path storedFile = storageRoot.resolve("assets/2026/08/asset.png");
        Files.createDirectories(storedFile.getParent());
        Files.write(storedFile, new byte[]{1, 2, 3});
        when(diagnosticReportRepository.findReportRenderAssetById(assetId)).thenReturn(Optional.of(
            new DiagnosticReportRepository.ReportRenderAsset(
                assetId, "CASE-1", "asset.png", "assets/2026/08/asset.png", "image/png", 3,
                "a".repeat(64), LocalDateTime.now())));
        allowAssetEditing("CASE-1", "USER-1");

        TransactionSynchronizationManager.initSynchronization();
        try {
            reportArtifactService.deleteRenderAsset(assetId, "USER-1");
            assertThat(storedFile).exists();
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
            assertThat(storedFile).exists();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        TransactionSynchronizationManager.initSynchronization();
        try {
            reportArtifactService.deleteRenderAsset(assetId, "USER-1");
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
                synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            }
            assertThat(storedFile).doesNotExist();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldRejectUnassignedAssetReadersButAllowTheSigningRole() throws Exception {
        String assetId = "RRA-55555555-5555-5555-5555-555555555555";
        Path storedFile = storageRoot.resolve("assets/2026/08/readable.png");
        Files.createDirectories(storedFile.getParent());
        Files.write(storedFile, new byte[]{1, 2, 3});
        when(diagnosticReportRepository.findReportRenderAssetById(assetId)).thenReturn(Optional.of(
            new DiagnosticReportRepository.ReportRenderAsset(
                assetId, "CASE-1", "readable.png", "assets/2026/08/readable.png", "image/png", 3,
                "a".repeat(64), LocalDateTime.now())));
        allowAssetEditing("CASE-1", "USER-ASSIGNED");

        assertThatThrownBy(() -> reportArtifactService.readRenderAsset(
            assetId, "USER-OTHER", "M4_DIAGNOSIS"))
            .isInstanceOf(BlBusinessException.class)
            .extracting("httpStatus")
            .isEqualTo(403);
        assertThat(reportArtifactService.readRenderAsset(assetId, "USER-SIGN", "M4_SIGN").fileName())
            .isEqualTo("readable.png");
    }

    @Test
    void shouldReturnServiceUnavailableWhenLazyOfdGenerationIsDisabled() {
        ReportStorageProperties disabledProperties = new ReportStorageProperties();
        disabledProperties.setOfdEnabled(false);
        disabledProperties.setRootDir(storageRoot);
        ReportArtifactService disabledService = new ReportArtifactService(
            diagnosticReportRepository, objectMapper, disabledProperties);
        when(diagnosticReportRepository.findPathologyReportById("RPT-1"))
            .thenReturn(Optional.of(report(null)));
        when(diagnosticReportRepository.findReportVersionArtifact("RPT-1", 1, "OFD"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> disabledService.ensureAndReadOfdArtifact("RPT-1", "USER-SIGN", "M4_SIGN"))
            .isInstanceOf(BlBusinessException.class)
            .extracting("httpStatus")
            .isEqualTo(503);
    }

    @Test
    void shouldRejectOfdGenerationWhenSigningStorageIsDisabled() {
        ReportStorageProperties disabledProperties = new ReportStorageProperties();
        disabledProperties.setOfdEnabled(false);
        disabledProperties.setRootDir(storageRoot);
        ReportArtifactService disabledService = new ReportArtifactService(
            diagnosticReportRepository, objectMapper, disabledProperties);

        assertThatThrownBy(() -> disabledService.createOfdArtifact(
            report("{\"schemaVersion\":1,\"sections\":[]}"),
            "RVA-DISABLED",
            "签发医生",
            LocalDateTime.now()))
            .isInstanceOf(BlBusinessException.class)
            .extracting("httpStatus")
            .isEqualTo(503);
    }

    @Test
    void shouldCleanExpiredTemporaryAndOrphanFilesWithinTheConfiguredBatch() throws Exception {
        ReportStorageProperties cleanupProperties = new ReportStorageProperties();
        cleanupProperties.setRootDir(storageRoot);
        cleanupProperties.setTemporaryFileTtl(Duration.ZERO);
        cleanupProperties.setCleanupBatchSize(2);
        ReportArtifactService cleanupService = new ReportArtifactService(
            diagnosticReportRepository, objectMapper, cleanupProperties);
        Path temporaryDirectory = storageRoot.resolve(".tmp");
        Files.createDirectories(temporaryDirectory);
        Files.writeString(temporaryDirectory.resolve("one.tmp"), "1");
        Files.writeString(temporaryDirectory.resolve("two.tmp"), "2");
        Files.writeString(temporaryDirectory.resolve("three.tmp"), "3");

        cleanupService.cleanupExpiredStorageFiles();

        try (var files = Files.list(temporaryDirectory)) {
            assertThat(files.count()).isEqualTo(1);
        }

        cleanupProperties.setCleanupBatchSize(10);
        cleanupService.cleanupExpiredStorageFiles();
        Path orphan = storageRoot.resolve("formal/2026/08/REPORT-1/orphan.ofd");
        Path tracked = storageRoot.resolve("formal/2026/08/REPORT-1/tracked.ofd");
        Files.createDirectories(orphan.getParent());
        Files.writeString(orphan, "orphan");
        Files.writeString(tracked, "tracked");
        when(diagnosticReportRepository.existsReportVersionArtifactByStorageKey(
            "formal/2026/08/REPORT-1/orphan.ofd")).thenReturn(false);
        when(diagnosticReportRepository.existsReportVersionArtifactByStorageKey(
            "formal/2026/08/REPORT-1/tracked.ofd")).thenReturn(true);

        cleanupService.cleanupExpiredStorageFiles();

        assertThat(orphan).doesNotExist();
        assertThat(tracked).exists();
    }

    private void allowAssetEditing(String caseId, String userId) {
        allowAssetEditing(caseId, userId, "PATIENT-1");
    }

    private void allowAssetEditing(String caseId, String userId, String patientIdDisplay) {
        DiagnosticReportRepository.DiagnosticTask task = diagnosticTask(caseId, userId, patientIdDisplay);
        when(diagnosticReportRepository.findDiagnosticTasksByCaseId(caseId)).thenReturn(List.of(task));
    }

    private DiagnosticReportRepository.DiagnosticTask diagnosticTask(
        String caseId, String userId, String patientIdDisplay) {
        return new DiagnosticReportRepository.DiagnosticTask(
            "TASK-1", "APP-1", "APP-NO-1", "张三", "PATIENT-1", patientIdDisplay, caseId, "P-1",
            null, "ROUTINE", "病理检查", 1, "病理科", "组织", "PRIMARY", "STARTED", "DRAFT",
            "NORMAL", "MANUAL", "ASSIGNER-1", "分配医生", userId, "诊断医生", userId, "诊断医生",
            "REVIEWER-1", "审核医生", null, null, null, null, null, null, null, LocalDateTime.now());
    }

    private DiagnosticReportRepository.PathologyReport report(String renderSnapshot) {
        LocalDateTime now = LocalDateTime.of(2026, 8, 25, 10, 0);
        return new DiagnosticReportRepository.PathologyReport(
            "RPT-1", "CASE-1", "TASK-1", "RPT-20260825-001", "P20260001", "ROUTINE", 1,
            "SIGNED", 1, "ROUTINE", "张三", "DEPT-1", "病理科", now, "灰白组织一块", "镜下所见",
            "临床诊断", "中文诊断结论", now, "REVIEWER-1", "审核医生", now, "SIGNER-1", "权威签发医生",
            now, null, "<p>结构化报告</p>", renderSnapshot, "备注", now, now);
    }

    private String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private int countOccurrences(String value, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }

    private static final class QueueingExecutor implements Executor {

        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        int size() {
            return tasks.size();
        }

        void runNext() {
            tasks.remove().run();
        }
    }

}
