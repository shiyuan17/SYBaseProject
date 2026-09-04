package com.company.bl.application.service;

import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.infrastructure.config.ReportStorageProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class OfdRenderWorkerMain {

    private OfdRenderWorkerMain() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.exit(2);
        }
        try {
            render(Path.of(args[0]));
        } catch (Throwable throwable) {
            System.err.println(OfdRenderWorkerFailure.classify(throwable).diagnosticLine());
            System.exit(2);
        }
    }

    private static void render(Path jobPath) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        OfdRenderWorkerJob job = objectMapper.readValue(jobPath.toFile(), OfdRenderWorkerJob.class);
        Path rootDirectory = Path.of(job.rootDirectory()).toAbsolutePath().normalize();
        Path target = Path.of(job.targetPath()).toAbsolutePath().normalize();
        if (!target.startsWith(rootDirectory.resolve(".tmp"))) {
            throw new IllegalArgumentException("OFD worker target must stay inside temporary storage");
        }
        Map<String, DiagnosticReportRepository.ReportRenderAsset> assets = job.assets().stream()
            .map(OfdRenderWorkerMain::toRenderAsset)
            .collect(Collectors.toUnmodifiableMap(DiagnosticReportRepository.ReportRenderAsset::id, Function.identity()));
        DiagnosticReportRepository repository = repositoryForAssets(assets);
        ReportStorageProperties properties = new ReportStorageProperties();
        properties.setRootDir(rootDirectory);
        ReportArtifactService service = new ReportArtifactService(repository, objectMapper, properties);
        LocalDateTime signedAt = LocalDateTime.parse(job.signedAt());
        DiagnosticReportRepository.PathologyReport report = reportFrom(job, signedAt);
        JsonNode snapshot = objectMapper.readTree(job.renderSnapshot());
        try {
            service.renderOfdForWorker(target, snapshot, report, job.signedByName(), signedAt);
        } catch (Throwable throwable) {
            Files.deleteIfExists(target);
            throw throwable;
        }
    }

    private static DiagnosticReportRepository repositoryForAssets(
        Map<String, DiagnosticReportRepository.ReportRenderAsset> assets
    ) {
        return (DiagnosticReportRepository) Proxy.newProxyInstance(
            DiagnosticReportRepository.class.getClassLoader(),
            new Class<?>[]{DiagnosticReportRepository.class},
            (proxy, method, args) -> {
                if ("findReportRenderAssetById".equals(method.getName())) {
                    return Optional.ofNullable(assets.get((String) args[0]));
                }
                if (method.getDeclaringClass() == Object.class) {
                    return method.invoke(assets, args);
                }
                throw new UnsupportedOperationException(method.getName());
            });
    }

    private static DiagnosticReportRepository.ReportRenderAsset toRenderAsset(OfdRenderWorkerJob.Asset asset) {
        return new DiagnosticReportRepository.ReportRenderAsset(
            asset.id(),
            asset.caseId(),
            asset.fileName(),
            asset.storageKey(),
            asset.contentType(),
            asset.byteSize(),
            asset.sha256(),
            LocalDateTime.now());
    }

    private static DiagnosticReportRepository.PathologyReport reportFrom(
        OfdRenderWorkerJob job,
        LocalDateTime signedAt
    ) {
        return new DiagnosticReportRepository.PathologyReport(
            job.reportId(), job.caseId(), null, job.reportNo(), job.pathologyNo(), null, 0,
            "SIGNED", job.versionNo(), null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, job.signedByName(), signedAt, null, null,
            job.renderSnapshot(), null, null, null);
    }
}
