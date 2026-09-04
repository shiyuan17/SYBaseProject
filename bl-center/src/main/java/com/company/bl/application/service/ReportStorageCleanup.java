package com.company.bl.application.service;

import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.infrastructure.config.ReportStorageProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

final class ReportStorageCleanup {

    private final DiagnosticReportRepository repository;
    private final ReportStorageProperties properties;
    private final Path rootDirectory;

    ReportStorageCleanup(DiagnosticReportRepository repository, ReportStorageProperties properties) {
        this.repository = repository;
        this.properties = properties;
        this.rootDirectory = properties.getRootDir().toAbsolutePath().normalize();
    }

    void cleanupExpiredStorageFiles() {
        Instant cutoff = Instant.now().minus(properties.getTemporaryFileTtl());
        int remaining = Math.max(1, properties.getCleanupBatchSize());
        remaining -= cleanupDirectory(rootDirectory.resolve(".tmp"), cutoff, remaining, StorageArea.TEMPORARY);
        if (remaining > 0) {
            remaining -= cleanupDirectory(rootDirectory.resolve("formal"), cutoff, remaining, StorageArea.FORMAL);
        }
        if (remaining > 0) {
            cleanupDirectory(rootDirectory.resolve("assets"), cutoff, remaining, StorageArea.ASSET);
        }
    }

    private int cleanupDirectory(Path directory, Instant cutoff, int limit, StorageArea area) {
        if (!Files.isDirectory(directory) || limit <= 0) {
            return 0;
        }
        int cleaned = 0;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                if (cleaned >= limit) {
                    break;
                }
                if (Files.getLastModifiedTime(path).toInstant().isAfter(cutoff)) {
                    continue;
                }
                if (area != StorageArea.TEMPORARY && hasStorageMetadata(path, area)) {
                    continue;
                }
                if (Files.deleteIfExists(path)) {
                    cleaned++;
                }
            }
        } catch (IOException | RuntimeException ignored) {
            return cleaned;
        }
        return cleaned;
    }

    private boolean hasStorageMetadata(Path path, StorageArea area) {
        String storageKey = rootDirectory.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
        return area == StorageArea.FORMAL
            ? repository.existsReportVersionArtifactByStorageKey(storageKey)
            : repository.existsReportRenderAssetByStorageKey(storageKey);
    }

    private enum StorageArea {
        ASSET,
        FORMAL,
        TEMPORARY
    }
}
