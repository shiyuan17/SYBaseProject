package com.company.bl.application.service;

import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.infrastructure.config.ReportStorageProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

final class OfdRenderWorkerExecutor {

    private static final Logger log = LoggerFactory.getLogger(OfdRenderWorkerExecutor.class);
    private static final String WORKER_MAIN_CLASS = OfdRenderWorkerMain.class.getName();
    private static final String PROPERTIES_LAUNCHER = "org.springframework.boot.loader.launch.PropertiesLauncher";

    private final ObjectMapper objectMapper;
    private final Path rootDirectory;
    private final int maxHeapMb;
    private final Duration timeout;

    OfdRenderWorkerExecutor(ObjectMapper objectMapper, ReportStorageProperties properties) {
        this.objectMapper = objectMapper;
        this.rootDirectory = properties.getRootDir().toAbsolutePath().normalize();
        this.maxHeapMb = Math.max(64, Math.min(1024, properties.getOfdWorkerMaxHeapMb()));
        Duration configuredTimeout = properties.getOfdWorkerTimeout();
        this.timeout = configuredTimeout == null
            ? Duration.ofSeconds(60)
            : configuredTimeout.compareTo(Duration.ofSeconds(5)) < 0
                ? Duration.ofSeconds(5)
                : configuredTimeout.compareTo(Duration.ofMinutes(5)) > 0
                    ? Duration.ofMinutes(5)
                    : configuredTimeout;
    }

    void render(Path target,
                JsonNode snapshot,
                DiagnosticReportRepository.PathologyReport report,
                String signedByName,
                LocalDateTime signedAt,
                List<DiagnosticReportRepository.ReportRenderAsset> assets) throws IOException {
        validateAssets(assets);
        Path temporaryDirectory = rootDirectory.resolve(".tmp");
        Files.createDirectories(temporaryDirectory);
        Path jobFile = Files.createTempFile(temporaryDirectory, "ofd-worker-", ".json");
        Path diagnosticFile = Files.createTempFile(temporaryDirectory, "ofd-worker-", ".log");
        OfdRenderWorkerJob job = new OfdRenderWorkerJob(
            rootDirectory.toString(),
            target.toAbsolutePath().normalize().toString(),
            report.id(),
            report.caseId(),
            report.reportNo(),
            report.pathologyNo(),
            report.versionNo(),
            snapshot.toString(),
            signedByName,
            signedAt.toString(),
            assets.stream().map(this::toWorkerAsset).toList());
        try {
            objectMapper.writeValue(jobFile.toFile(), job);
            runWorker(jobFile, diagnosticFile, report.id());
        } finally {
            Files.deleteIfExists(jobFile);
            Files.deleteIfExists(diagnosticFile);
        }
    }

    private void validateAssets(List<DiagnosticReportRepository.ReportRenderAsset> assets) throws IOException {
        for (DiagnosticReportRepository.ReportRenderAsset asset : assets) {
            Path path;
            try {
                path = rootDirectory.resolve(asset.storageKey()).toAbsolutePath().normalize();
            } catch (RuntimeException exception) {
                throw new OfdRenderWorkerException(OfdRenderWorkerFailure.MISSING_RENDER_ASSET, exception);
            }
            if (!path.startsWith(rootDirectory) || !Files.isRegularFile(path)) {
                throw new OfdRenderWorkerException(OfdRenderWorkerFailure.MISSING_RENDER_ASSET);
            }
            if (!Files.isReadable(path)) {
                throw new OfdRenderWorkerException(OfdRenderWorkerFailure.STORAGE_ACCESS_DENIED);
            }
        }
    }

    private OfdRenderWorkerJob.Asset toWorkerAsset(DiagnosticReportRepository.ReportRenderAsset asset) {
        return new OfdRenderWorkerJob.Asset(
            asset.id(),
            asset.caseId(),
            asset.fileName(),
            asset.storageKey(),
            asset.contentType(),
            asset.byteSize(),
            asset.sha256());
    }

    private void runWorker(Path jobFile, Path diagnosticFile, String reportId) throws IOException {
        long startedAt = System.nanoTime();
        Process process;
        try {
            process = new ProcessBuilder(buildCommand(jobFile))
                .redirectErrorStream(true)
                .redirectOutput(diagnosticFile.toFile())
                .start();
        } catch (IOException exception) {
            log.error("OFD worker failed to start reportId={} durationMs={}", reportId, elapsedMillis(startedAt));
            throw new OfdRenderWorkerException(OfdRenderWorkerFailure.RENDER_FAILED, exception);
        }
        boolean completed;
        try {
            completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            terminate(process);
            throw new OfdRenderWorkerException(OfdRenderWorkerFailure.RENDER_FAILED, exception);
        }
        if (!completed) {
            terminate(process);
            log.error("OFD worker timed out reportId={} durationMs={}", reportId, elapsedMillis(startedAt));
            throw new OfdRenderWorkerException(OfdRenderWorkerFailure.TIMEOUT);
        }
        if (process.exitValue() != 0) {
            OfdRenderWorkerFailure failure = OfdRenderWorkerFailure.fromDiagnostic(readDiagnostic(diagnosticFile));
            log.error("OFD worker failed reportId={} exitCode={} durationMs={} reason={}",
                reportId, process.exitValue(), elapsedMillis(startedAt), failure.name());
            throw new OfdRenderWorkerException(failure);
        }
        log.info("OFD worker completed reportId={} durationMs={}", reportId, elapsedMillis(startedAt));
    }

    private String readDiagnostic(Path diagnosticFile) {
        try (InputStream inputStream = Files.newInputStream(diagnosticFile)) {
            return new String(inputStream.readNBytes(8_192), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }

    private List<String> buildCommand(Path jobFile) {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable().toString());
        command.add("-Xmx" + maxHeapMb + "m");
        command.add("-XX:+ExitOnOutOfMemoryError");
        command.add("-Djava.awt.headless=true");
        String classPath = effectiveClassPath();
        if (isPackagedApplication(classPath)) {
            command.add("-Dloader.main=" + WORKER_MAIN_CLASS);
            command.add("-cp");
            command.add(classPath);
            command.add(PROPERTIES_LAUNCHER);
        } else {
            command.add("-cp");
            command.add(classPath);
            command.add(WORKER_MAIN_CLASS);
        }
        command.add(jobFile.toAbsolutePath().normalize().toString());
        return command;
    }

    private String effectiveClassPath() {
        String testClassPath = System.getProperty("surefire.test.class.path", "");
        return testClassPath.isBlank() ? System.getProperty("java.class.path", "") : testClassPath;
    }

    private boolean isPackagedApplication(String classPath) {
        return !classPath.contains(File.pathSeparator)
            && classPath.toLowerCase(Locale.ROOT).endsWith(".jar");
    }

    private Path javaExecutable() {
        String executable = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
            ? "java.exe"
            : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable);
    }

    private void terminate(Process process) {
        process.toHandle().descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
}
