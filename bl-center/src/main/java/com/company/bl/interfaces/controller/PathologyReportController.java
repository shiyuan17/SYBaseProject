package com.company.bl.interfaces.controller;

import com.company.bl.application.service.DiagnosticReportAppService;
import com.company.bl.application.service.DiagnosticReportModels;
import com.company.bl.application.service.ReportArtifactService;
import com.company.bl.interfaces.auth.M4PermissionCodes;
import com.company.bl.interfaces.auth.RbacPermissionRepository;
import com.company.bl.interfaces.auth.RequireAnyPermission;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CreatePathologyReportRequest;
import com.company.bl.interfaces.dto.DiagnosticTaskActionRequest;
import com.company.bl.interfaces.dto.FormalReportVersionBatchActionRequest;
import com.company.bl.interfaces.dto.RejectPathologyReportRequest;
import com.company.bl.interfaces.dto.UpdatePathologyReportDraftRequest;
import com.company.bl.interfaces.vo.FormalReportVersionBatchActionResponse;
import com.company.bl.interfaces.vo.PathologyReportOperationResponse;
import com.company.bl.interfaces.vo.ReportOfdArtifactResponse;
import com.company.bl.interfaces.vo.ReportOfdStatusResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pathology-reports")
@Tag(name = "医生流程", description = "病理报告草稿、审核、签发与发布接口")
public class PathologyReportController extends TechnicalControllerSupport {

    private final DiagnosticReportAppService diagnosticReportAppService;
    private final ObjectMapper objectMapper;
    private final RbacPermissionRepository permissionRepository;

    public PathologyReportController(DiagnosticReportAppService diagnosticReportAppService,
                                     RbacPermissionRepository permissionRepository,
                                     ObjectMapper objectMapper) {
        this.diagnosticReportAppService = diagnosticReportAppService;
        this.permissionRepository = permissionRepository;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "创建病理报告草稿", description = "基于病例和诊断任务创建首份病理报告草稿。")
    @RequireAnyPermission({M4PermissionCodes.WORKBENCH_QUERY, M4PermissionCodes.REPORT_CREATE})
    @PostMapping
    public PathologyReportOperationResponse create(@Valid @RequestBody CreatePathologyReportRequest request,
                                                   HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.PathologyReportResult result = diagnosticReportAppService.createReport(
            new DiagnosticReportModels.CreatePathologyReportCommand(
                request.getCaseId(),
                request.getTaskId(),
                request.getClinicalDiagnosis(),
                request.getGrossExam(),
                request.getMicroscopicExam(),
                request.getFinalDiagnosis(),
                request.getRichTextContent(),
                request.getRenderSnapshot(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                workbenchOverrideAllowed(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new PathologyReportOperationResponse(
            result.reportId(), result.caseId(), result.reportNo(), result.reportStatus(), result.versionNo(), result.versionStatus());
    }

    @Operation(summary = "保存病理报告草稿", description = "更新当前病理报告草稿，不生成历史版本。")
    @RequireAnyPermission({
        M4PermissionCodes.WORKBENCH_QUERY,
        M4PermissionCodes.REPORT_CREATE,
        M4PermissionCodes.REPORT_REVIEW,
        M4PermissionCodes.REPORT_SIGN
    })
    @PostMapping("/{id}/save-draft")
    public PathologyReportOperationResponse saveDraft(@PathVariable("id") String reportId,
                                                      @Valid @RequestBody UpdatePathologyReportDraftRequest request,
                                                      HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.PathologyReportResult result = diagnosticReportAppService.saveDraft(
            new DiagnosticReportModels.UpdateReportDraftCommand(
                reportId,
                request.getClinicalDiagnosis(),
                request.getGrossExam(),
                request.getMicroscopicExam(),
                request.getFinalDiagnosis(),
                request.getRichTextContent(),
                request.getRenderSnapshot(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                RequestOperatorContext.currentRoleCode(httpServletRequest),
                workbenchOverrideAllowed(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new PathologyReportOperationResponse(
            result.reportId(), result.caseId(), result.reportNo(), result.reportStatus(), result.versionNo(), result.versionStatus());
    }

    @Operation(summary = "提交病理报告", description = "提交病理报告进入审核流程。")
    @RequireAnyPermission({M4PermissionCodes.WORKBENCH_QUERY, M4PermissionCodes.REPORT_SUBMIT})
    @PostMapping("/{id}/submit")
    public PathologyReportOperationResponse submit(@PathVariable("id") String reportId,
                                                   @Valid @RequestBody DiagnosticTaskActionRequest request,
                                                   HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.PathologyReportResult result = diagnosticReportAppService.submitReport(
            new DiagnosticReportModels.ReportActionCommand(
                reportId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                workbenchOverrideAllowed(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new PathologyReportOperationResponse(
            result.reportId(), result.caseId(), result.reportNo(), result.reportStatus(), result.versionNo(), result.versionStatus());
    }

    @Operation(summary = "审核病理报告", description = "审核通过已提交的病理报告。")
    @RequireAnyPermission({M4PermissionCodes.WORKBENCH_QUERY, M4PermissionCodes.REPORT_REVIEW})
    @PostMapping("/{id}/review")
    public PathologyReportOperationResponse review(@PathVariable("id") String reportId,
                                                   @Valid @RequestBody DiagnosticTaskActionRequest request,
                                                   HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.PathologyReportResult result = diagnosticReportAppService.reviewReport(
            new DiagnosticReportModels.ReportActionCommand(
                reportId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                workbenchOverrideAllowed(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new PathologyReportOperationResponse(
            result.reportId(), result.caseId(), result.reportNo(), result.reportStatus(), result.versionNo(), result.versionStatus());
    }

    @Operation(summary = "驳回病理报告", description = "将已提交病理报告驳回至草稿状态。")
    @RequirePermission(M4PermissionCodes.REPORT_REVIEW)
    @PostMapping("/{id}/reject")
    public PathologyReportOperationResponse reject(@PathVariable("id") String reportId,
                                                   @Valid @RequestBody RejectPathologyReportRequest request,
                                                   HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.PathologyReportResult result = diagnosticReportAppService.rejectReport(
            new DiagnosticReportModels.RejectReportCommand(
                reportId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRejectReason()));
        return new PathologyReportOperationResponse(
            result.reportId(), result.caseId(), result.reportNo(), result.reportStatus(), result.versionNo(), result.versionStatus());
    }

    @Operation(summary = "签发病理报告", description = "签发已审核通过的病理报告，并生成签发版本快照。")
    @RequireAnyPermission({M4PermissionCodes.WORKBENCH_QUERY, M4PermissionCodes.REPORT_SIGN})
    @PostMapping("/{id}/sign")
    public PathologyReportOperationResponse sign(@PathVariable("id") String reportId,
                                                 @Valid @RequestBody DiagnosticTaskActionRequest request,
                                                 HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.PathologyReportResult result = diagnosticReportAppService.signReport(
            new DiagnosticReportModels.ReportActionCommand(
                reportId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                workbenchOverrideAllowed(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new PathologyReportOperationResponse(
            result.reportId(), result.caseId(), result.reportNo(), result.reportStatus(), result.versionNo(), result.versionStatus());
    }

    @Operation(summary = "读取签发 OFD 报告", description = "返回当前签发版本已经生成的 OFD 文件。")
    @RequireAnyPermission({
        M4PermissionCodes.WORKBENCH_QUERY,
        M4PermissionCodes.REPORT_CREATE,
        M4PermissionCodes.REPORT_REVIEW,
        M4PermissionCodes.REPORT_SIGN,
        M4PermissionCodes.REPORT_PUBLISH
    })
    @GetMapping("/{id}/ofd")
    public ResponseEntity<Resource> readOfd(@PathVariable("id") String reportId,
                                            HttpServletRequest httpServletRequest) throws JsonProcessingException {
        ReportArtifactService.ReportOfdStatus status = diagnosticReportAppService.prepareReportOfd(
            reportId,
            RequestOperatorContext.currentUserId(httpServletRequest),
            RequestOperatorContext.currentRoleCode(httpServletRequest),
            false);
        if (!status.isReady()) {
            return toOfdUnavailableResponse(status);
        }
        ReportArtifactService.StoredReportResource stored = diagnosticReportAppService.readReportOfd(
            reportId,
            RequestOperatorContext.currentUserId(httpServletRequest),
            RequestOperatorContext.currentRoleCode(httpServletRequest));
        return toFileResponse(stored);
    }

    @Operation(summary = "将签发 OFD 报告转换为 PDF", description = "临时转换当前签发版本的 OFD 文件并以内联 PDF 返回。")
    @RequireAnyPermission({
        M4PermissionCodes.WORKBENCH_QUERY,
        M4PermissionCodes.REPORT_CREATE,
        M4PermissionCodes.REPORT_REVIEW,
        M4PermissionCodes.REPORT_SIGN,
        M4PermissionCodes.REPORT_PUBLISH
    })
    @GetMapping("/{id}/ofd/pdf")
    public ResponseEntity<StreamingResponseBody> readOfdPdf(
        @PathVariable("id") String reportId,
        HttpServletRequest httpServletRequest
    ) throws JsonProcessingException {
        String currentUserId = RequestOperatorContext.currentUserId(httpServletRequest);
        String currentRoleCode = RequestOperatorContext.currentRoleCode(httpServletRequest);
        ReportArtifactService.ReportOfdStatus status = diagnosticReportAppService.prepareReportOfd(
            reportId, currentUserId, currentRoleCode, false);
        if (!status.isReady()) {
            return toOfdUnavailableStreamResponse(status);
        }
        ReportArtifactService.TemporaryReportResource temporary =
            diagnosticReportAppService.convertReportOfdToPdf(reportId, currentUserId, currentRoleCode);
        StreamingResponseBody body = outputStream -> {
            try (temporary) {
                Files.copy(temporary.path(), outputStream);
            }
        };
        return ResponseEntity.ok()
            .contentType(temporary.contentType())
            .contentLength(temporary.byteSize())
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                .filename(temporary.fileName(), StandardCharsets.UTF_8)
                .build().toString())
            .body(body);
    }

    @Operation(summary = "查询报告 OFD 准备状态", description = "缺失时启动或复用后台修复任务，并返回当前准备状态。")
    @RequireAnyPermission({
        M4PermissionCodes.WORKBENCH_QUERY,
        M4PermissionCodes.REPORT_CREATE,
        M4PermissionCodes.REPORT_REVIEW,
        M4PermissionCodes.REPORT_SIGN,
        M4PermissionCodes.REPORT_PUBLISH
    })
    @GetMapping("/{id}/ofd/status")
    public ReportOfdStatusResponse readOfdStatus(
        @PathVariable("id") String reportId,
        @RequestParam(name = "retryFailed", defaultValue = "false") boolean retryFailed,
        HttpServletRequest httpServletRequest
    ) {
        ReportArtifactService.ReportOfdStatus status = diagnosticReportAppService.prepareReportOfd(
            reportId,
            RequestOperatorContext.currentUserId(httpServletRequest),
            RequestOperatorContext.currentRoleCode(httpServletRequest),
            retryFailed);
        return toOfdStatusResponse(status);
    }

    @Operation(summary = "查询报告 OFD 存档", description = "按版本号和生成时间倒序返回报告的 OFD 存档元数据。")
    @RequireAnyPermission({
        M4PermissionCodes.WORKBENCH_QUERY,
        M4PermissionCodes.REPORT_CREATE,
        M4PermissionCodes.REPORT_REVIEW,
        M4PermissionCodes.REPORT_SIGN,
        M4PermissionCodes.REPORT_PUBLISH
    })
    @GetMapping("/{id}/ofd-artifacts")
    public List<ReportOfdArtifactResponse> listOfdArtifacts(
        @PathVariable("id") String reportId,
        HttpServletRequest httpServletRequest
    ) {
        return diagnosticReportAppService.listReportOfdArtifacts(
            reportId,
            RequestOperatorContext.currentUserId(httpServletRequest),
            RequestOperatorContext.currentRoleCode(httpServletRequest)).stream()
            .map(item -> new ReportOfdArtifactResponse(
                item.artifactId(),
                item.reportId(),
                item.versionNo(),
                item.artifactFormat(),
                item.fileName(),
                item.contentType(),
                item.byteSize(),
                item.sha256(),
                item.generatedAt(),
                item.downloadUrl()))
            .toList();
    }

    @Operation(summary = "读取历史 OFD 存档", description = "返回指定报告下的指定 OFD 存档文件。")
    @RequireAnyPermission({
        M4PermissionCodes.WORKBENCH_QUERY,
        M4PermissionCodes.REPORT_CREATE,
        M4PermissionCodes.REPORT_REVIEW,
        M4PermissionCodes.REPORT_SIGN,
        M4PermissionCodes.REPORT_PUBLISH
    })
    @GetMapping("/{id}/ofd-artifacts/{artifactId}/file")
    public ResponseEntity<Resource> readOfdArtifact(
        @PathVariable("id") String reportId,
        @PathVariable String artifactId,
        HttpServletRequest httpServletRequest
    ) {
        ReportArtifactService.StoredReportResource stored = diagnosticReportAppService.readReportOfdArtifact(
            reportId,
            artifactId,
            RequestOperatorContext.currentUserId(httpServletRequest),
            RequestOperatorContext.currentRoleCode(httpServletRequest));
        return toFileResponse(stored);
    }

    private ResponseEntity<Resource> toFileResponse(ReportArtifactService.StoredReportResource stored) {
        return ResponseEntity.ok()
            .contentType(stored.contentType())
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                .filename(stored.fileName(), StandardCharsets.UTF_8)
                .build().toString())
            .body(stored.resource());
    }

    private ResponseEntity<Resource> toOfdUnavailableResponse(
        ReportArtifactService.ReportOfdStatus status
    ) throws JsonProcessingException {
        HttpStatus responseStatus = status.isFailed() ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.ACCEPTED;
        byte[] body = objectMapper.writeValueAsBytes(toOfdStatusResponse(status));
        ResponseEntity.BodyBuilder response = ResponseEntity.status(responseStatus)
            .contentType(MediaType.APPLICATION_JSON)
            .contentLength(body.length);
        if (!status.isFailed()) {
            response.header(HttpHeaders.RETRY_AFTER,
                Integer.toString(Math.max(1, (status.retryAfterMs() + 999) / 1000)));
        }
        return response.body(new ByteArrayResource(body));
    }

    private ResponseEntity<StreamingResponseBody> toOfdUnavailableStreamResponse(
        ReportArtifactService.ReportOfdStatus status
    ) throws JsonProcessingException {
        HttpStatus responseStatus = status.isFailed() ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.ACCEPTED;
        byte[] responseBody = objectMapper.writeValueAsBytes(toOfdStatusResponse(status));
        ResponseEntity.BodyBuilder response = ResponseEntity.status(responseStatus)
            .contentType(MediaType.APPLICATION_JSON)
            .contentLength(responseBody.length);
        if (!status.isFailed()) {
            response.header(HttpHeaders.RETRY_AFTER,
                Integer.toString(Math.max(1, (status.retryAfterMs() + 999) / 1000)));
        }
        return response.body(outputStream -> outputStream.write(responseBody));
    }

    private ReportOfdStatusResponse toOfdStatusResponse(ReportArtifactService.ReportOfdStatus status) {
        return new ReportOfdStatusResponse(status.status(), status.retryAfterMs(), status.message());
    }

    @Operation(summary = "发布病理报告", description = "发布已签发病理报告，并完成诊断任务闭环。")
    @RequirePermission(M4PermissionCodes.REPORT_PUBLISH)
    @PostMapping("/{id}/publish")
    public PathologyReportOperationResponse publish(@PathVariable("id") String reportId,
                                                    @Valid @RequestBody DiagnosticTaskActionRequest request,
                                                    HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.PathologyReportResult result = diagnosticReportAppService.publishReport(
            new DiagnosticReportModels.ReportActionCommand(
                reportId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                false,
                request.getTerminalCode(),
                request.getRemarks()));
        return new PathologyReportOperationResponse(
            result.reportId(), result.caseId(), result.reportNo(), result.reportStatus(), result.versionNo(), result.versionStatus());
    }

    @Operation(summary = "批量记录正式报告打印", description = "为正式报告版本记录打印时间并更新打印状态。")
    @RequirePermission(M4PermissionCodes.REPORT_PUBLISH)
    @PostMapping("/formal-versions/print")
    public FormalReportVersionBatchActionResponse printFormalVersions(
        @Valid @RequestBody FormalReportVersionBatchActionRequest request,
        HttpServletRequest httpServletRequest
    ) {
        DiagnosticReportModels.FormalReportVersionBatchActionResult result = diagnosticReportAppService.printFormalReportVersions(
            new DiagnosticReportModels.FormalReportVersionBatchActionCommand(
                request.getVersionIds(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                false,
                request.getTerminalCode(),
                request.getIssueMode(),
                request.getPlannedIssueAt(),
                request.getRemarks()));
        return toBatchActionResponse(result);
    }

    @Operation(summary = "批量发放正式报告", description = "允许对待发放的正式报告版本记录发放时间，打印不是发放前置条件。")
    @RequireAnyPermission({M4PermissionCodes.WORKBENCH_QUERY, M4PermissionCodes.REPORT_PUBLISH})
    @PostMapping("/formal-versions/issue")
    public FormalReportVersionBatchActionResponse issueFormalVersions(
        @Valid @RequestBody FormalReportVersionBatchActionRequest request,
        HttpServletRequest httpServletRequest
    ) {
        DiagnosticReportModels.FormalReportVersionBatchActionResult result = diagnosticReportAppService.issueFormalReportVersions(
            new DiagnosticReportModels.FormalReportVersionBatchActionCommand(
                request.getVersionIds(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                workbenchOverrideAllowed(httpServletRequest),
                request.getTerminalCode(),
                request.getIssueMode(),
                request.getPlannedIssueAt(),
                request.getRemarks()));
        return toBatchActionResponse(result);
    }

    @Operation(summary = "批量回收正式报告", description = "仅允许对已发放的正式报告版本记录回收时间。")
    @RequirePermission(M4PermissionCodes.REPORT_PUBLISH)
    @PostMapping("/formal-versions/recall")
    public FormalReportVersionBatchActionResponse recallFormalVersions(
        @Valid @RequestBody FormalReportVersionBatchActionRequest request,
        HttpServletRequest httpServletRequest
    ) {
        DiagnosticReportModels.FormalReportVersionBatchActionResult result = diagnosticReportAppService.recallFormalReportVersions(
            new DiagnosticReportModels.FormalReportVersionBatchActionCommand(
                request.getVersionIds(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                false,
                request.getTerminalCode(),
                request.getIssueMode(),
                request.getPlannedIssueAt(),
                request.getRemarks()));
        return toBatchActionResponse(result);
    }

    private FormalReportVersionBatchActionResponse toBatchActionResponse(
        DiagnosticReportModels.FormalReportVersionBatchActionResult result
    ) {
        return new FormalReportVersionBatchActionResponse(
            result.totalCount(),
            result.successCount(),
            result.failureCount(),
            result.items().stream()
                .map(item -> new FormalReportVersionBatchActionResponse.ItemResult(
                    item.versionId(),
                    item.success(),
                    item.message()))
                .toList());
    }

    private boolean workbenchOverrideAllowed(HttpServletRequest request) {
        return permissionRepository.hasPermission(
            resolveUserId(request),
            M4PermissionCodes.WORKBENCH_QUERY);
    }
}
