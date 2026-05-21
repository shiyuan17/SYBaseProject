package com.company.bl.interfaces.controller;

import com.company.bl.application.service.ArchiveModels;
import com.company.bl.application.service.ArchiveQueryService;
import com.company.bl.application.service.ArchiveWorkflowService;
import com.company.bl.interfaces.auth.M5PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.ArchiveApplicationFormRequest;
import com.company.bl.interfaces.dto.ArchiveEmbeddingBoxRequest;
import com.company.bl.interfaces.dto.ArchiveSlideRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ArchiveRecordController extends TechnicalControllerSupport {

    private final ArchiveQueryService archiveQueryService;
    private final ArchiveWorkflowService archiveWorkflowService;

    public ArchiveRecordController(ArchiveQueryService archiveQueryService,
                                   ArchiveWorkflowService archiveWorkflowService) {
        this.archiveQueryService = archiveQueryService;
        this.archiveWorkflowService = archiveWorkflowService;
    }

    @RequirePermission(M5PermissionCodes.APPLICATION_FORM_ARCHIVE)
    @PostMapping("/archive/application-forms")
    public ArchiveModels.ArchiveActionResult archiveApplicationForm(@Valid @RequestBody ArchiveApplicationFormRequest request,
                                                                    HttpServletRequest httpServletRequest) {
        return archiveWorkflowService.archiveApplicationForm(new ArchiveModels.ArchiveObjectCommand(
            request.getCaseId(),
            request.getArchivePositionId(),
            resolveUserId(request.getOperatorUserId(), httpServletRequest),
            request.getOperatorName(),
            request.getTerminalCode(),
            request.getFileUrl(),
            request.getFileName(),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.EMBEDDING_BOX_ARCHIVE)
    @PostMapping("/archive/embedding-boxes")
    public ArchiveModels.ArchiveActionResult archiveEmbeddingBox(@Valid @RequestBody ArchiveEmbeddingBoxRequest request,
                                                                 HttpServletRequest httpServletRequest) {
        return archiveWorkflowService.archiveEmbeddingBox(new ArchiveModels.ArchiveObjectCommand(
            request.getEmbeddingBoxId(),
            request.getArchivePositionId(),
            resolveUserId(request.getOperatorUserId(), httpServletRequest),
            request.getOperatorName(),
            request.getTerminalCode(),
            null,
            null,
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.SLIDE_ARCHIVE)
    @PostMapping("/archive/slides")
    public ArchiveModels.ArchiveActionResult archiveSlide(@Valid @RequestBody ArchiveSlideRequest request,
                                                          HttpServletRequest httpServletRequest) {
        return archiveWorkflowService.archiveSlide(new ArchiveModels.ArchiveObjectCommand(
            request.getSlideId(),
            request.getArchivePositionId(),
            resolveUserId(request.getOperatorUserId(), httpServletRequest),
            request.getOperatorName(),
            request.getTerminalCode(),
            null,
            null,
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.ARCHIVE_QUERY)
    @GetMapping("/archive-records/search")
    public List<ArchiveModels.ArchiveRecordView> searchArchiveRecords(@RequestParam(required = false) String keyword,
                                                                      @RequestParam(required = false) String objectType,
                                                                      @RequestParam(required = false) String caseId) {
        return archiveQueryService.searchArchiveRecords(new ArchiveModels.SearchArchiveRecordsQuery(keyword, objectType, caseId));
    }
}
