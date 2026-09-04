package com.company.bl.interfaces.controller;

import com.company.bl.application.service.DiagnosticReportAppService;
import com.company.bl.application.service.DiagnosticReportModels;
import com.company.bl.application.service.ReportArtifactService;
import com.company.bl.interfaces.auth.M4PermissionCodes;
import com.company.bl.interfaces.auth.RequireAnyPermission;
import com.company.bl.interfaces.vo.ReportRenderAssetResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/pathology-report-assets")
public class PathologyReportAssetController {

    private final DiagnosticReportAppService diagnosticReportAppService;

    public PathologyReportAssetController(DiagnosticReportAppService diagnosticReportAppService) {
        this.diagnosticReportAppService = diagnosticReportAppService;
    }

    @PostMapping
    @RequireAnyPermission({M4PermissionCodes.REPORT_CREATE, M4PermissionCodes.REPORT_REVIEW})
    public ReportRenderAssetResponse upload(@RequestParam String caseId,
                                            @RequestParam("file") MultipartFile file,
                                            HttpServletRequest request) {
        DiagnosticReportModels.ReportRenderAssetResult result = diagnosticReportAppService.storeReportRenderAsset(
            caseId, RequestOperatorContext.currentUserId(request), file);
        return new ReportRenderAssetResponse(
            result.assetId(), result.caseId(), result.fileName(), result.fileUrl(), result.contentType(), result.byteSize());
    }

    @DeleteMapping("/{assetId}")
    @RequireAnyPermission({M4PermissionCodes.REPORT_CREATE, M4PermissionCodes.REPORT_REVIEW})
    public void delete(@PathVariable String assetId, HttpServletRequest request) {
        diagnosticReportAppService.deleteReportRenderAsset(assetId, RequestOperatorContext.currentUserId(request));
    }

    @GetMapping("/{assetId}/file")
    @RequireAnyPermission({
        M4PermissionCodes.WORKBENCH_QUERY,
        M4PermissionCodes.REPORT_CREATE,
        M4PermissionCodes.REPORT_REVIEW,
        M4PermissionCodes.REPORT_SIGN
    })
    public ResponseEntity<org.springframework.core.io.Resource> read(@PathVariable String assetId,
                                                                    HttpServletRequest request) {
        ReportArtifactService.StoredReportResource stored = diagnosticReportAppService.readReportRenderAsset(
            assetId,
            RequestOperatorContext.currentUserId(request),
            RequestOperatorContext.currentRoleCode(request));
        return ResponseEntity.ok()
            .contentType(stored.contentType())
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                .filename(stored.fileName(), StandardCharsets.UTF_8)
                .build().toString())
            .body(stored.resource());
    }
}
