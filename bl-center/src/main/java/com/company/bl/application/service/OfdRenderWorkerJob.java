package com.company.bl.application.service;

import java.util.List;

public record OfdRenderWorkerJob(
    String rootDirectory,
    String targetPath,
    String reportId,
    String caseId,
    String reportNo,
    String pathologyNo,
    int versionNo,
    String renderSnapshot,
    String signedByName,
    String signedAt,
    List<Asset> assets
) {

    public record Asset(
        String id,
        String caseId,
        String fileName,
        String storageKey,
        String contentType,
        long byteSize,
        String sha256
    ) {
    }
}
