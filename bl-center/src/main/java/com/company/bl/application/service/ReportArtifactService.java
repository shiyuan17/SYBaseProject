package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.infrastructure.config.ReportStorageProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ofdrw.converter.ConvertHelper;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.PageLayout;
import org.ofdrw.layout.element.Img;
import org.ofdrw.layout.element.Paragraph;
import org.ofdrw.layout.element.Position;
import org.ofdrw.layout.element.Clear;
import org.ofdrw.layout.element.Span;
import org.ofdrw.layout.element.canvas.Canvas;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.font.Font;
import org.ofdrw.font.FontName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Service
public class ReportArtifactService {
    private static final Logger log = LoggerFactory.getLogger(ReportArtifactService.class);
    private static final String OFD_CONTENT_TYPE = "application/ofd";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String OFD_FORMAT = "OFD";
    private static final String WYSIWYG_TEMPLATE = "wysiwyg-template";
    private static final String LEGACY_DEFAULT_TEMPLATE = "default";
    private static final String LEGACY_CLASSIC_DEFAULT_TEMPLATE = "classic-default";
    private static final int OFD_RETRY_AFTER_MS = 2_000;
    private static final DateTimeFormatter REPORT_DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
        "image/bmp", ".bmp",
        "image/jpeg", ".jpg",
        "image/png", ".png",
        "image/webp", ".webp"
    );
    private final DiagnosticReportRepository diagnosticReportRepository;
    private final ObjectMapper objectMapper;
    private final ReportStorageProperties properties;
    private final Path rootDirectory;
    private final Executor ofdRepairExecutor;
    private final OfdRenderWorkerExecutor ofdRenderWorkerExecutor;
    private final TransactionTemplate repairTransaction;
    private final ConcurrentHashMap<String, CompletableFuture<OfdRepairResult>> ofdRepairTasks = new ConcurrentHashMap<>();
    private final ReportSnapshotSupport snapshotSupport;
    private final ReportStorageCleanup storageCleanup;
    @Autowired
    public ReportArtifactService(DiagnosticReportRepository diagnosticReportRepository,
                                 ObjectMapper objectMapper,
                                 ReportStorageProperties properties,
                                 @Qualifier("reportOfdRepairExecutor") Executor ofdRepairExecutor,
                                 PlatformTransactionManager transactionManager) {
        this(diagnosticReportRepository, objectMapper, properties, ofdRepairExecutor,
            new OfdRenderWorkerExecutor(objectMapper, properties), new TransactionTemplate(transactionManager));
    }
    ReportArtifactService(DiagnosticReportRepository diagnosticReportRepository,
                          ObjectMapper objectMapper,
                          ReportStorageProperties properties) {
        this(diagnosticReportRepository, objectMapper, properties, Runnable::run, null, (TransactionTemplate) null);
    }
    ReportArtifactService(DiagnosticReportRepository diagnosticReportRepository,
                          ObjectMapper objectMapper,
                          ReportStorageProperties properties,
                          Executor ofdRepairExecutor,
                          OfdRenderWorkerExecutor ofdRenderWorkerExecutor,
                          TransactionTemplate repairTransaction) {
        this.diagnosticReportRepository = diagnosticReportRepository;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.rootDirectory = properties.getRootDir().toAbsolutePath().normalize();
        this.ofdRepairExecutor = ofdRepairExecutor;
        this.ofdRenderWorkerExecutor = ofdRenderWorkerExecutor;
        this.repairTransaction = repairTransaction;
        this.snapshotSupport = new ReportSnapshotSupport(diagnosticReportRepository, objectMapper);
        this.storageCleanup = new ReportStorageCleanup(diagnosticReportRepository, properties);
    }
    public boolean isOfdEnabled() {
        return properties.isOfdEnabled();
    }
    public String validateAndSerializeRenderSnapshot(JsonNode renderSnapshot, String caseId) {
        return snapshotSupport.validateAndSerialize(renderSnapshot, caseId);
    }
    @Transactional
    public DiagnosticReportModels.ReportRenderAssetResult storeRenderAsset(String caseId, String currentUserId, MultipartFile file) {
        String normalizedCaseId = requireText(caseId, "Case ID is required");
        ensureAssetEditor(normalizedCaseId, currentUserId);
        String contentType = normalizeContentType(file == null ? null : file.getContentType());
        if (file == null || file.isEmpty()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Report image file is required");
        }
        if (!IMAGE_EXTENSIONS.containsKey(contentType)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Unsupported report image content type");
        }
        if (file.getSize() > properties.getMaxImageSize().toBytes()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Report image file exceeds size limit");
        }

        String assetId = "RRA-" + UUID.randomUUID();
        String extension = IMAGE_EXTENSIONS.get(contentType);
        String dateDirectory = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String storageKey = "assets/" + dateDirectory + "/" + assetId + extension;
        Path target = resolveStorageKey(storageKey);
        Path temporary = temporaryPath(assetId, extension);
        try {
            Files.createDirectories(temporary.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            long byteSize = Files.size(temporary);
            if (byteSize == 0 || byteSize > properties.getMaxImageSize().toBytes()) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Report image file exceeds size limit");
            }
            if (!contentType.equals(detectImageContentType(temporary))) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Report image content does not match its type");
            }
            String sha256 = sha256(temporary);
            moveAtomically(temporary, target);
            registerRollbackCleanup(target);
            LocalDateTime now = LocalDateTime.now();
            String fileName = sanitizeFileName(file.getOriginalFilename(), "report-image" + extension);
            diagnosticReportRepository.insertReportRenderAsset(new DiagnosticReportRepository.CreateReportRenderAssetCommand(
                assetId, normalizedCaseId, fileName, storageKey, contentType, byteSize, sha256, now));
            return new DiagnosticReportModels.ReportRenderAssetResult(
                assetId, normalizedCaseId, fileName, "/api/v1/pathology-report-assets/" + assetId + "/file", contentType, byteSize);
        } catch (BlBusinessException exception) {
            deleteQuietly(temporary);
            throw exception;
        } catch (IOException exception) {
            deleteQuietly(temporary);
            throw artifactFailure();
        }
    }

    @Transactional
    public void deleteRenderAsset(String assetId, String currentUserId) {
        DiagnosticReportRepository.ReportRenderAsset asset = getRenderAsset(assetId);
        ensureAssetEditor(asset.caseId(), currentUserId);
        diagnosticReportRepository.deleteReportRenderAsset(asset.id());
        registerCommitCleanup(resolveStorageKey(asset.storageKey()));
    }

    public StoredReportResource readRenderAsset(String assetId, String currentUserId, String currentRoleCode) {
        DiagnosticReportRepository.ReportRenderAsset asset = getRenderAsset(assetId);
        ensureAssetViewer(asset.caseId(), currentUserId, currentRoleCode);
        Path file = resolveStorageKey(asset.storageKey());
        if (!Files.isRegularFile(file)) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Report image file not found");
        }
        return new StoredReportResource(new FileSystemResource(file), MediaType.parseMediaType(asset.contentType()), asset.fileName());
    }

    public StoredReportResource readOfdArtifact(String reportId, String currentUserId, String currentRoleCode) {
        DiagnosticReportRepository.PathologyReport report = getViewableFormalReport(
            reportId, currentUserId, currentRoleCode);
        DiagnosticReportRepository.ReportVersionArtifact artifact = diagnosticReportRepository
            .findReportVersionArtifact(report.id(), report.versionNo(), OFD_FORMAT)
            .filter(this::isStoredFile)
            .orElseThrow(() -> new BlBusinessException(
                BlErrorCode.REPORT_ARTIFACT_GENERATION_FAILED, 503, "报告 OFD 文件尚未就绪"));
        Path file = resolveStorageKey(artifact.storageKey());
        if (!Files.isRegularFile(file)) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Signed OFD report file not found");
        }
        return new StoredReportResource(new FileSystemResource(file), MediaType.parseMediaType(OFD_CONTENT_TYPE), artifact.fileName());
    }

    public TemporaryReportResource convertOfdArtifactToPdf(
        String reportId,
        String currentUserId,
        String currentRoleCode
    ) {
        StoredReportResource stored = readOfdArtifact(reportId, currentUserId, currentRoleCode);
        Path temporary = temporaryPath("ofd-preview", ".pdf");
        try {
            Files.createDirectories(temporary.getParent());
            Path source = stored.resource().getFile().toPath();
            ConvertHelper.toPdf(source, temporary);
            long byteSize = Files.size(temporary);
            if (byteSize == 0) {
                throw new IOException("Converted PDF is empty");
            }
            String sourceFileName = stored.fileName();
            String pdfFileName = sourceFileName.toLowerCase(Locale.ROOT).endsWith(".ofd")
                ? sourceFileName.substring(0, sourceFileName.length() - 4) + ".pdf"
                : sourceFileName + ".pdf";
            return new TemporaryReportResource(
                temporary,
                MediaType.parseMediaType(PDF_CONTENT_TYPE),
                pdfFileName,
                byteSize);
        } catch (IOException | RuntimeException exception) {
            deleteQuietly(temporary);
            log.error("OFD to PDF conversion failed reportId={} reason={}",
                reportId, exception.getClass().getSimpleName());
            throw artifactFailure("报告 OFD 转 PDF 失败，请稍后重试");
        }
    }

    public ReportOfdStatus prepareOfdArtifact(String reportId,
                                              String currentUserId,
                                              String currentRoleCode,
                                              boolean retryFailed) {
        DiagnosticReportRepository.PathologyReport report = getViewableFormalReport(
            reportId, currentUserId, currentRoleCode);
        Optional<DiagnosticReportRepository.ReportVersionArtifact> artifact = diagnosticReportRepository
            .findReportVersionArtifact(report.id(), report.versionNo(), OFD_FORMAT);
        if (artifact.filter(this::isStoredFile).isPresent()) {
            ofdRepairTasks.remove(report.id());
            return ReportOfdStatus.ready();
        }
        if (!isOfdEnabled()) {
            return ReportOfdStatus.failed("报告 OFD 生成功能当前不可用");
        }

        CompletableFuture<OfdRepairResult> current = ofdRepairTasks.get(report.id());
        if (current != null && current.isDone()) {
            OfdRepairResult result = current.handle((value, error) -> error == null
                ? value
                : OfdRepairResult.failed("报告 OFD 文件生成失败，请重试")).join();
            if (result.success()) {
                Optional<DiagnosticReportRepository.ReportVersionArtifact> refreshed = diagnosticReportRepository
                    .findReportVersionArtifact(report.id(), report.versionNo(), OFD_FORMAT);
                if (refreshed.filter(this::isStoredFile).isPresent()) {
                    ofdRepairTasks.remove(report.id(), current);
                    return ReportOfdStatus.ready();
                }
                result = OfdRepairResult.failed("报告 OFD 文件生成失败，请重试");
            }
            if (!retryFailed) {
                return ReportOfdStatus.failed(result.message());
            }
            ofdRepairTasks.remove(report.id(), current);
            current = null;
        }
        if (current == null) {
            startOfdRepair(report.id());
        }
        return ReportOfdStatus.generating();
    }

    private DiagnosticReportRepository.PathologyReport getViewableFormalReport(String reportId,
                                                                                String currentUserId,
                                                                                String currentRoleCode) {
        String normalizedReportId = requireText(reportId, "Report ID is required");
        DiagnosticReportRepository.PathologyReport report = diagnosticReportRepository
            .findPathologyReportById(normalizedReportId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Pathology report not found"));
        ensureAssetViewer(report.caseId(), currentUserId, currentRoleCode);
        if (!DiagnosticReportConstants.REPORT_SIGNED.equals(report.reportStatus())
            && !DiagnosticReportConstants.REPORT_PUBLISHED.equals(report.reportStatus())) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Signed OFD report not found");
        }
        return report;
    }

    private void startOfdRepair(String reportId) {
        try {
            ofdRepairTasks.compute(reportId, (key, existing) -> {
                if (existing != null && !existing.isDone()) {
                    return existing;
                }
                return CompletableFuture.supplyAsync(() -> repairMissingOfd(key), ofdRepairExecutor);
            });
        } catch (RejectedExecutionException exception) {
            log.error("OFD repair queue rejected reportId={}", reportId, exception);
            ofdRepairTasks.put(reportId, CompletableFuture.completedFuture(
                OfdRepairResult.failed("报告 OFD 生成任务繁忙，请稍后重试")));
        }
    }

    private OfdRepairResult repairMissingOfd(String reportId) {
        long startedAt = System.nanoTime();
        try {
            DiagnosticReportRepository.PathologyReport report = diagnosticReportRepository
                .findPathologyReportById(reportId)
                .orElseThrow(() -> new BlBusinessException(
                    BlErrorCode.RESOURCE_NOT_FOUND, 404, "Pathology report not found"));
            Optional<DiagnosticReportRepository.ReportVersionArtifact> artifact = diagnosticReportRepository
                .findReportVersionArtifact(report.id(), report.versionNo(), OFD_FORMAT);
            if (artifact.filter(this::isStoredFile).isEmpty()) {
                generateMissingOfdArtifact(report, artifact.orElse(null));
            }
            log.info("OFD repair completed reportId={} durationMs={}", reportId, elapsedMillis(startedAt));
            return OfdRepairResult.succeeded();
        } catch (RuntimeException exception) {
            log.error("OFD repair failed reportId={} durationMs={} reason={}",
                reportId, elapsedMillis(startedAt), exception.getClass().getSimpleName());
            return OfdRepairResult.failed(repairFailureMessage(exception));
        }
    }

    private String repairFailureMessage(RuntimeException exception) {
        if (exception instanceof BlBusinessException businessException
            && businessException.getErrorCode() == BlErrorCode.REPORT_ARTIFACT_GENERATION_FAILED
            && businessException.getMessage() != null
            && !businessException.getMessage().isBlank()) {
            return businessException.getMessage();
        }
        return "报告 OFD 文件生成失败，请重试";
    }

    private boolean isStoredFile(DiagnosticReportRepository.ReportVersionArtifact artifact) {
        return Files.isRegularFile(resolveStorageKey(artifact.storageKey()));
    }

    @Transactional(readOnly = true)
    public List<DiagnosticReportModels.ReportOfdArtifactView> listOfdArtifacts(
        String reportId,
        String currentUserId,
        String currentRoleCode
    ) {
        String normalizedReportId = requireText(reportId, "Report ID is required");
        DiagnosticReportRepository.PathologyReport report = diagnosticReportRepository
            .findPathologyReportById(normalizedReportId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Pathology report not found"));
        ensureAssetViewer(report.caseId(), currentUserId, currentRoleCode);
        return diagnosticReportRepository.findReportVersionArtifacts(normalizedReportId, OFD_FORMAT).stream()
            .map(artifact -> new DiagnosticReportModels.ReportOfdArtifactView(
                artifact.id(),
                artifact.reportId(),
                artifact.versionNo(),
                artifact.artifactFormat(),
                artifact.fileName(),
                artifact.contentType(),
                artifact.byteSize(),
                artifact.sha256(),
                artifact.generatedAt().toString(),
                "/api/v1/pathology-reports/" + normalizedReportId + "/ofd-artifacts/" + artifact.id() + "/file"))
            .toList();
    }

    @Transactional(readOnly = true)
    public StoredReportResource readOfdArtifactById(
        String reportId,
        String artifactId,
        String currentUserId,
        String currentRoleCode
    ) {
        String normalizedReportId = requireText(reportId, "Report ID is required");
        String normalizedArtifactId = requireText(artifactId, "Artifact ID is required");
        DiagnosticReportRepository.PathologyReport report = diagnosticReportRepository
            .findPathologyReportById(normalizedReportId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Pathology report not found"));
        ensureAssetViewer(report.caseId(), currentUserId, currentRoleCode);
        DiagnosticReportRepository.ReportVersionArtifact artifact = diagnosticReportRepository
            .findReportVersionArtifactById(normalizedArtifactId)
            .filter(item -> normalizedReportId.equals(item.reportId()) && OFD_FORMAT.equals(item.artifactFormat()))
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "OFD artifact not found"));
        Path file = resolveStorageKey(artifact.storageKey());
        if (!Files.isRegularFile(file)) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Signed OFD report file not found");
        }
        return new StoredReportResource(
            new FileSystemResource(file),
            MediaType.parseMediaType(artifact.contentType()),
            artifact.fileName());
    }

    public StoredReportResource ensureAndReadOfdArtifact(String reportId, String currentUserId, String currentRoleCode) {
        ReportOfdStatus status = prepareOfdArtifact(reportId, currentUserId, currentRoleCode, true);
        if (status.isReady()) {
            return readOfdArtifact(reportId, currentUserId, currentRoleCode);
        }
        throw new BlBusinessException(
            BlErrorCode.REPORT_ARTIFACT_GENERATION_FAILED, 503, status.message());
    }

    private DiagnosticReportRepository.ReportVersionArtifact generateMissingOfdArtifact(
        DiagnosticReportRepository.PathologyReport report,
        DiagnosticReportRepository.ReportVersionArtifact existingArtifact
    ) {
        if (!isOfdEnabled()) {
            throw artifactFailure();
        }
        DiagnosticReportRepository.ReportVersion version = diagnosticReportRepository
            .findLatestFormalReportVersion(report.id(), report.versionNo())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Signed OFD report not found"));
        LocalDateTime signedAt = version.signedAt() == null ? report.signedAt() : version.signedAt();
        if (signedAt == null) {
            throw artifactFailure();
        }
        String signedByName = version.signedByName() == null ? report.signedByName() : version.signedByName();
        String renderSnapshot = version.renderSnapshot();
        if (renderSnapshot == null || renderSnapshot.isBlank()) {
            renderSnapshot = snapshotSupport.buildLegacyRenderSnapshot(report);
        }
        renderSnapshot = restorePatientIdDisplay(report, renderSnapshot);
        DiagnosticReportRepository.PathologyReport snapshotReport = snapshotSupport.copyWithRenderSnapshot(
            report, renderSnapshot, signedByName, signedAt);
        String artifactId = existingArtifact == null ? "RVA-" + UUID.randomUUID() : existingArtifact.id();
        GeneratedReportArtifact generated = createOfdArtifact(snapshotReport, artifactId, signedByName, signedAt);
        DiagnosticReportRepository.ReportVersionArtifact artifact = new DiagnosticReportRepository.ReportVersionArtifact(
            generated.id(), generated.reportId(), generated.versionNo(), generated.artifactFormat(), generated.fileName(),
            generated.storageKey(), generated.contentType(), generated.byteSize(), generated.sha256(), generated.generatedAt());
        try {
            persistRegeneratedArtifact(report, artifact, existingArtifact == null);
            return artifact;
        } catch (RuntimeException exception) {
            deleteQuietly(resolveStorageKey(artifact.storageKey()));
            if (exception instanceof BlBusinessException businessException) {
                throw businessException;
            }
            throw artifactFailure();
        }
    }

    private String restorePatientIdDisplay(DiagnosticReportRepository.PathologyReport report,
                                           String renderSnapshot) {
        if (report.taskId() == null || report.taskId().isBlank()
            || renderSnapshot == null || renderSnapshot.isBlank()) {
            return renderSnapshot;
        }
        String patientIdDisplay = diagnosticReportRepository.findDiagnosticTaskById(report.taskId())
            .map(DiagnosticReportRepository.DiagnosticTask::patientIdDisplay)
            .map(String::trim)
            .filter(value -> !value.isBlank() && !looksLikeUuid(value))
            .orElse(null);
        if (patientIdDisplay == null) {
            return renderSnapshot;
        }

        JsonNode snapshot = snapshotSupport.parse(renderSnapshot);
        boolean changed = restorePatientIdDisplay(snapshot.path("metaFields"), patientIdDisplay);
        changed |= restorePatientIdDisplay(snapshot.path("footerFields"), patientIdDisplay);
        return changed ? snapshot.toString() : renderSnapshot;
    }

    private boolean restorePatientIdDisplay(JsonNode fields, String patientIdDisplay) {
        if (!fields.isArray()) {
            return false;
        }
        boolean changed = false;
        for (JsonNode field : fields) {
            if (!field.isObject()
                || !normalizeLabel(field.path("label").asText()).contains("病人ID")) {
                continue;
            }
            String value = field.path("value").asText("").trim();
            if (value.isBlank() || "-".equals(value) || looksLikeUuid(value)) {
                ((ObjectNode) field).put("value", patientIdDisplay);
                changed = true;
            }
        }
        return changed;
    }

    private void persistRegeneratedArtifact(DiagnosticReportRepository.PathologyReport report,
                                            DiagnosticReportRepository.ReportVersionArtifact artifact,
                                            boolean insert) {
        Runnable persistence = () -> {
            DiagnosticReportRepository.CreateReportVersionArtifactCommand command =
                new DiagnosticReportRepository.CreateReportVersionArtifactCommand(
                    artifact.id(), artifact.reportId(), artifact.versionNo(), artifact.artifactFormat(), artifact.fileName(),
                    artifact.storageKey(), artifact.contentType(), artifact.byteSize(), artifact.sha256(), artifact.generatedAt());
            if (insert) {
                diagnosticReportRepository.insertReportVersionArtifact(command);
            } else {
                diagnosticReportRepository.updateReportVersionArtifact(command);
            }
            diagnosticReportRepository.updateReportVersionArtifactId(report.id(), report.versionNo(), artifact.id());
        };
        if (repairTransaction == null) {
            persistence.run();
            return;
        }
        repairTransaction.executeWithoutResult(status -> persistence.run());
    }

    public GeneratedReportArtifact createOfdArtifact(DiagnosticReportRepository.PathologyReport report,
                                                      String artifactId,
                                                      String signedByName,
                                                      LocalDateTime signedAt) {
        if (!isOfdEnabled()) {
            throw artifactFailure();
        }
        JsonNode snapshot = snapshotSupport.parse(report.renderSnapshot());
        List<DiagnosticReportRepository.ReportRenderAsset> renderAssets =
            snapshotSupport.collectSnapshotAssets(snapshot, report.caseId());
        String dateDirectory = signedAt.format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String storageKey = "formal/" + dateDirectory + "/" + report.id() + "/" + artifactId + ".ofd";
        Path target = resolveStorageKey(storageKey);
        Path temporary = temporaryPath(artifactId, ".ofd");
        String stage = "render";
        try {
            Files.createDirectories(temporary.getParent());
            long stageStartedAt = System.nanoTime();
            if (ofdRenderWorkerExecutor == null) {
                renderOfd(temporary, snapshot, report, signedByName, signedAt);
            } else {
                ofdRenderWorkerExecutor.render(
                    temporary, snapshot, report, signedByName, signedAt, renderAssets);
            }
            log.info("OFD generation stage completed reportId={} artifactId={} stage=render durationMs={}",
                report.id(), artifactId, elapsedMillis(stageStartedAt));
            stage = "verify";
            stageStartedAt = System.nanoTime();
            try (OFDReader ignored = new OFDReader(temporary)) {
            }
            log.info("OFD generation stage completed reportId={} artifactId={} stage=verify durationMs={}",
                report.id(), artifactId, elapsedMillis(stageStartedAt));
            long byteSize = Files.size(temporary);
            stage = "checksum";
            stageStartedAt = System.nanoTime();
            String checksum = sha256(temporary);
            log.info("OFD generation stage completed reportId={} artifactId={} stage=checksum durationMs={} byteSize={}",
                report.id(), artifactId, elapsedMillis(stageStartedAt), byteSize);
            stage = "move";
            stageStartedAt = System.nanoTime();
            moveAtomically(temporary, target);
            log.info("OFD generation stage completed reportId={} artifactId={} stage=move durationMs={} byteSize={}",
                report.id(), artifactId, elapsedMillis(stageStartedAt), byteSize);
            registerRollbackCleanup(target);
            String fileName = sanitizeFileName(report.reportNo(), "report") + "-V" + report.versionNo() + ".ofd";
            return new GeneratedReportArtifact(
                artifactId, report.id(), report.versionNo(), OFD_FORMAT, fileName, storageKey,
                OFD_CONTENT_TYPE, byteSize, checksum, signedAt);
        } catch (IOException | RuntimeException exception) {
            log.error("OFD generation failed reportId={} artifactId={} stage={} reason={}",
                report.id(), artifactId, stage, exception.getClass().getSimpleName());
            deleteQuietly(temporary);
            deleteQuietly(target);
            if (exception instanceof BlBusinessException businessException) {
                throw businessException;
            }
            if (exception instanceof OfdRenderWorkerException workerException) {
                throw artifactFailure(workerException.getMessage());
            }
            throw artifactFailure();
        }
    }

    void renderOfdForWorker(Path target,
                            JsonNode snapshot,
                            DiagnosticReportRepository.PathologyReport report,
                            String signedByName,
                            LocalDateTime signedAt) throws IOException {
        renderOfd(target, snapshot, report, signedByName, signedAt);
    }

    private void renderOfd(Path target,
                           JsonNode snapshot,
                           DiagnosticReportRepository.PathologyReport report,
                           String signedByName,
                           LocalDateTime signedAt) throws IOException {
        Font font = reportFont();
        try (OFDDoc document = new OFDDoc(target)) {
            document.setDefaultPageLayout(PageLayout.A4().setMargin(8d));
            if ("routine-pathology-report-v2".equals(snapshot.path("layoutCode").asText())
                || WYSIWYG_TEMPLATE.equals(snapshot.path("templateCode").asText())
                || LEGACY_DEFAULT_TEMPLATE.equals(snapshot.path("templateCode").asText())
                || LEGACY_CLASSIC_DEFAULT_TEMPLATE.equals(snapshot.path("templateCode").asText())) {
                new WysiwygReportOfdRenderer(this::renderWysiwygImages, this::renderGenericBlocks)
                    .render(document, snapshot, report, signedByName, formatSignedAt(signedAt), font);
            } else {
                renderGenericReport(document, snapshot, report, signedByName, signedAt, font);
            }
        }
    }

    private void renderGenericReport(OFDDoc document,
                                     JsonNode snapshot,
                                     DiagnosticReportRepository.PathologyReport report,
                                     String signedByName,
                                     LocalDateTime signedAt,
                                     Font font) throws IOException {
        document.add(new Paragraph().add(new Span(text(snapshot, "hospitalName", "")).setFont(font).setBold(true).setFontSize(7d)));
        document.add(new Paragraph().add(new Span(text(snapshot, "reportTitle", "病理检查报告单")).setFont(font).setBold(true).setFontSize(8d)));
        document.add(new Paragraph("报告号：" + report.reportNo() + "    病理号：" + nullToEmpty(report.pathologyNo()), font));
        renderFields(document, snapshot.path("metaFields"), true, font);
        JsonNode sections = snapshot.path("sections");
        if (sections.isArray()) {
            for (JsonNode section : sections) {
                addLabelValue(document, section.path("label").asText(), section.path("value").asText(), font);
                renderImages(document, section.path("images"), report.caseId());
            }
        } else {
            addLabelValue(document, "大体所见", report.grossExam(), font);
            addLabelValue(document, "镜下所见", report.microscopicExam(), font);
            addLabelValue(document, "病理诊断", report.finalDiagnosis(), font);
        }
        renderGenericBlocks(document, snapshot.path("structuredBlocks"), font);
        renderFields(document, snapshot.path("footerFields"), false, font);
        document.add(new Paragraph("签发医师：" + nullToEmpty(signedByName)
            + "    签发时间：" + formatSignedAt(signedAt), font));
        String note = snapshot.path("note").asText();
        if (!note.isBlank()) {
            OfdTextLayout.paragraphs(note)
                .forEach(value -> document.add(new Paragraph(value, font).setIntegrity(true)));
        }
    }


    private String safeDisplayFieldValue(String label, String value) {
        if (normalizeLabel(label).contains("病人ID") && looksLikeUuid(value)) {
            return "-";
        }
        return displayValue(value);
    }

    private boolean looksLikeUuid(String value) {
        return value != null && value.trim().matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }

    private String displayValue(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String formatSignedAt(LocalDateTime signedAt) {
        return signedAt == null ? "-" : signedAt.format(REPORT_DATE_TIME_FORMATTER);
    }

    private String normalizeLabel(String label) {
        return label == null ? "" : label.replace(" ", "").replace(":", "").replace("：", "");
    }

    private void renderFields(OFDDoc document, JsonNode fields, boolean metadata, Font font) {
        if (!fields.isArray()) {
            return;
        }
        for (JsonNode field : fields) {
            String label = field.path("label").asText();
            if (isAuthoritativeField(label, metadata)) {
                continue;
            }
            addLabelValue(document, label, safeDisplayFieldValue(label, field.path("value").asText()), font);
        }
    }

    private void renderImages(OFDDoc document, JsonNode images, String caseId) throws IOException {
        if (!images.isArray()) {
            return;
        }
        for (JsonNode image : images) {
            String assetId = image.path("assetId").asText();
            if (assetId.isBlank()) {
                continue;
            }
            DiagnosticReportRepository.ReportRenderAsset asset = getRenderAsset(assetId);
            if (!caseId.equals(asset.caseId())) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Report image does not belong to case");
            }
            Img img = new Img(48d, 36d, resolveStorageKey(asset.storageKey()));
            img.setPosition(Position.Relative)
                .setLeft(pixelsToMillimeters(image.path("left").asDouble()))
                .setTop(pixelsToMillimeters(image.path("top").asDouble()));
            document.add(img);
        }
    }

    private void renderWysiwygImages(OFDDoc document, JsonNode images, String caseId) throws IOException {
        if (!images.isArray()) {
            return;
        }
        List<Path> imagePaths = new java.util.ArrayList<>();
        for (JsonNode image : images) {
            String assetId = image.path("assetId").asText();
            if (assetId.isBlank()) {
                continue;
            }
            DiagnosticReportRepository.ReportRenderAsset asset = getRenderAsset(assetId);
            if (!caseId.equals(asset.caseId())) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Report image does not belong to case");
            }
            imagePaths.add(resolveStorageKey(asset.storageKey()));
        }
        for (int index = 0; index < imagePaths.size(); index += 2) {
            Path left = imagePaths.get(index);
            Path right = index + 1 < imagePaths.size() ? imagePaths.get(index + 1) : null;
            Canvas row = new Canvas(117d, 42d, context -> {
                context.drawImage(left, 0d, 0d, 56d, 42d);
                if (right != null) {
                    context.drawImage(right, 61d, 0d, 56d, 42d);
                }
            });
            row.setClear(Clear.both).setMarginBottom(5d).setIntegrity(true);
            document.add(row);
        }
    }

    private void renderGenericBlocks(OFDDoc document, JsonNode node, Font font) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isTextual()) {
            if (!node.asText().isBlank()) {
                OfdTextLayout.paragraphs(node.asText())
                    .forEach(value -> document.add(new Paragraph(value, font).setIntegrity(true)));
            }
            return;
        }
        if (node.isArray() || node.isObject()) {
            node.elements().forEachRemaining(child -> renderGenericBlocks(document, child, font));
        }
    }

    private void addLabelValue(OFDDoc document, String label, String value, Font font) {
        String normalizedLabel = label == null ? "" : label;
        String normalizedValue = value == null ? "" : value;
        if (!normalizedLabel.isBlank() || !normalizedValue.isBlank()) {
            OfdTextLayout.paragraphs(normalizedLabel + normalizedValue)
                .forEach(text -> document.add(new Paragraph(text, font).setIntegrity(true)));
        }
    }

    private Font reportFont() {
        return FontName.SimSun.font();
    }

    private DiagnosticReportRepository.ReportRenderAsset getRenderAsset(String assetId) {
        return snapshotSupport.getRenderAsset(assetId);
    }

    private void ensureAssetEditor(String caseId, String currentUserId) {
        String normalizedUserId = requireText(currentUserId, "Current user ID is required");
        if (!isCaseReportParticipant(caseId, normalizedUserId)) {
            throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403,
                "User is not assigned to edit report assets for this case");
        }
    }

    private void ensureAssetViewer(String caseId, String currentUserId, String currentRoleCode) {
        String normalizedUserId = requireText(currentUserId, "Current user ID is required");
        boolean signingRole = "M4_SIGN".equals(currentRoleCode) || "PATHOLOGY_ADMIN".equals(currentRoleCode);
        if (!signingRole && !isCaseReportParticipant(caseId, normalizedUserId)) {
            throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403,
                "User is not allowed to read report assets for this case");
        }
    }

    private boolean isCaseReportParticipant(String caseId, String userId) {
        return diagnosticReportRepository.findDiagnosticTasksByCaseId(caseId).stream()
            .anyMatch(task -> userId.equals(task.diagnosisDoctorUserId())
                || userId.equals(task.primaryDoctorUserId())
                || userId.equals(task.reviewerUserId()));
    }

    private String detectImageContentType(Path path) throws IOException {
        byte[] header = new byte[12];
        int length;
        try (InputStream inputStream = Files.newInputStream(path)) {
            length = inputStream.read(header);
        }
        if (length >= 3 && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        if (length >= 8 && (header[0] & 0xff) == 0x89 && header[1] == 'P' && header[2] == 'N'
            && header[3] == 'G' && header[4] == 0x0d && header[5] == 0x0a && header[6] == 0x1a && header[7] == 0x0a) {
            return "image/png";
        }
        if (length >= 2 && header[0] == 'B' && header[1] == 'M') {
            return "image/bmp";
        }
        if (length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
            && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return "image/webp";
        }
        return "";
    }

    private boolean isAuthoritativeField(String label, boolean metadata) {
        String normalized = normalizeLabel(label);
        if (metadata) {
            return normalized.contains("报告号") || normalized.contains("病理号");
        }
        return normalized.contains("诊断医师") || normalized.contains("签发医师")
            || normalized.contains("报告日期") || normalized.contains("签发时间");
    }

    private double pixelsToMillimeters(double pixels) {
        return pixels * 25.4d / 96d;
    }

    private Path temporaryPath(String id, String extension) {
        Path path = rootDirectory.resolve(".tmp").resolve(id + "-" + UUID.randomUUID() + extension).normalize();
        ensureWithinRoot(path);
        return path;
    }

    private Path resolveStorageKey(String storageKey) {
        Path path = rootDirectory.resolve(storageKey).normalize();
        ensureWithinRoot(path);
        return path;
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void registerRollbackCleanup(Path path) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteQuietly(path);
                }
            }
        });
    }

    private void registerCommitCleanup(Path path) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(path);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(path);
            }
        });
    }

    @Scheduled(fixedDelayString = "${bl.file-storage.reports.cleanup-fixed-delay-ms:3600000}")
    public void cleanupExpiredStorageFiles() {
        storageCleanup.cleanupExpiredStorageFiles();
    }

    private String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void ensureWithinRoot(Path path) {
        if (!path.startsWith(rootDirectory)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Invalid report storage path");
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    BlBusinessException artifactFailure() {
        return new BlBusinessException(BlErrorCode.REPORT_ARTIFACT_GENERATION_FAILED, 503,
            "报告 OFD 文件生成失败，签发未完成");
    }

    private BlBusinessException artifactFailure(String message) {
        return new BlBusinessException(BlErrorCode.REPORT_ARTIFACT_GENERATION_FAILED, 503, message);
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, message);
        }
        return value.trim();
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
    }

    private String sanitizeFileName(String value, String fallback) {
        String candidate = value == null ? "" : value.replace('\\', '/');
        int lastSlash = candidate.lastIndexOf('/');
        if (lastSlash >= 0) {
            candidate = candidate.substring(lastSlash + 1);
        }
        candidate = candidate.replaceAll("[^0-9A-Za-z._\\-\\u4e00-\\u9fa5]", "_");
        return candidate.isBlank() ? fallback : candidate;
    }

    private String text(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText();
        return value.isBlank() ? fallback : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record GeneratedReportArtifact(
        String id,
        String reportId,
        int versionNo,
        String artifactFormat,
        String fileName,
        String storageKey,
        String contentType,
        long byteSize,
        String sha256,
        LocalDateTime generatedAt
    ) {
    }

    public record ReportOfdStatus(String status, int retryAfterMs, String message) {

        public static ReportOfdStatus ready() {
            return new ReportOfdStatus("READY", 0, "报告 OFD 文件已就绪");
        }

        public static ReportOfdStatus generating() {
            return new ReportOfdStatus("GENERATING", OFD_RETRY_AFTER_MS, "正在生成报告 OFD 文件");
        }

        public static ReportOfdStatus failed(String message) {
            return new ReportOfdStatus("FAILED", 0, message);
        }

        public boolean isReady() {
            return "READY".equals(status);
        }

        public boolean isFailed() {
            return "FAILED".equals(status);
        }
    }

    private record OfdRepairResult(boolean success, String message) {

        private static OfdRepairResult succeeded() {
            return new OfdRepairResult(true, "报告 OFD 文件已就绪");
        }

        private static OfdRepairResult failed(String message) {
            return new OfdRepairResult(false, message);
        }
    }

    public record StoredReportResource(Resource resource, MediaType contentType, String fileName) {
    }

    public record TemporaryReportResource(Path path, MediaType contentType, String fileName, long byteSize)
        implements AutoCloseable {

        @Override
        public void close() throws IOException {
            Files.deleteIfExists(path);
        }
    }
}
