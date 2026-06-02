package com.company.bl.interfaces.controller;

import com.company.bl.application.service.GrossingMediaStorageService;
import com.company.bl.application.service.ApplicationRegistrationWorkbenchAppService;
import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.application.service.TechnicalWorkflowModels;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.SaveApplicationRegistrationPatientInfoRequest;
import com.company.bl.interfaces.dto.TechnicalSpecimenRegistrationCompleteRequest;
import com.company.bl.interfaces.dto.TechnicalSpecimenRegistrationDetailSectionsSaveRequest;
import com.company.bl.interfaces.dto.TechnicalSpecimenRegistrationMaterialVerificationRequest;
import com.company.bl.interfaces.dto.TechnicalSpecimenRegistrationMaterialsSaveRequest;
import com.company.bl.interfaces.vo.ApplicationRegistrationWorkbenchResponse;
import com.company.bl.interfaces.vo.PendingTechnicalSpecimenRegistrationResponse;
import com.company.bl.interfaces.vo.PendingTechnicalSpecimenRegistrationPageResponse;
import com.company.bl.interfaces.vo.TechnicalSpecimenRegistrationActionFlagsResponse;
import com.company.bl.interfaces.vo.TechnicalSpecimenRegistrationBasicInfoResponse;
import com.company.bl.interfaces.vo.TechnicalSpecimenRegistrationCheckItemResponse;
import com.company.bl.interfaces.vo.TechnicalSpecimenRegistrationCompleteResponse;
import com.company.bl.interfaces.vo.TechnicalSpecimenRegistrationDetailSectionsResponse;
import com.company.bl.interfaces.vo.TechnicalSpecimenRegistrationDetailResponse;
import com.company.bl.interfaces.vo.TechnicalSpecimenRegistrationMediaAssetDeleteResponse;
import com.company.bl.interfaces.vo.TechnicalSpecimenRegistrationMediaAssetResponse;
import com.company.bl.interfaces.vo.TechnicalSpecimenRegistrationMaterialResponse;
import com.company.bl.interfaces.vo.TechnicalSpecimenRegistrationWorkspaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/technical-specimen-registrations")
@Tag(name = "技术流程", description = "接收后技术标本登记接口")
public class TechnicalSpecimenRegistrationController extends TechnicalControllerSupport {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;
    private final GrossingMediaStorageService grossingMediaStorageService;

    public TechnicalSpecimenRegistrationController(TechnicalWorkflowAppService technicalWorkflowAppService,
                                                   GrossingMediaStorageService grossingMediaStorageService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
        this.grossingMediaStorageService = grossingMediaStorageService;
    }

    @Operation(summary = "查询待技术登记病例", description = "分页查询病理接收后待进入技术登记环节的病例。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @GetMapping("/pending")
    public PendingTechnicalSpecimenRegistrationPageResponse listPending(
        @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "每页条数，默认 20") @RequestParam(defaultValue = "20") int size,
        @Parameter(description = "病人 ID、病理号、姓名、住院号关键字") @RequestParam(required = false) String keyword,
        @Parameter(description = "接收开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receivedFrom,
        @Parameter(description = "接收结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receivedTo
    ) {
        TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationPage result =
            technicalWorkflowAppService.listPendingTechnicalSpecimenRegistrations(
                new TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationQuery(
                    page,
                    size,
                    keyword,
                    receivedFrom == null ? null : receivedFrom.atStartOfDay(),
                    receivedTo == null ? null : receivedTo.plusDays(1).atStartOfDay()));
        return new PendingTechnicalSpecimenRegistrationPageResponse(
            result.items().stream().map(item -> new PendingTechnicalSpecimenRegistrationResponse(
                item.caseId(),
                item.applicationId(),
                item.applicationNo(),
                item.pathologyNo(),
                item.patientName(),
                item.patientId(),
                item.inpatientNo(),
                item.applicationType(),
                item.submittingDepartmentName(),
                item.checkItem(),
                item.registeredByName(),
                item.registrationStatus(),
                item.receivedAt(),
                item.registeredAt())).toList(),
            result.page(),
            result.size(),
            result.total());
    }

    @Operation(summary = "查询技术标本登记详情", description = "查询右侧送检材料、临床诊断和检查项目详情。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @GetMapping("/{caseId}")
    public TechnicalSpecimenRegistrationDetailResponse detail(@PathVariable String caseId) {
        TechnicalWorkflowModels.TechnicalSpecimenRegistrationDetail detail =
            technicalWorkflowAppService.getTechnicalSpecimenRegistrationDetail(caseId);
        return new TechnicalSpecimenRegistrationDetailResponse(
            detail.caseId(),
            detail.applicationId(),
            detail.applicationNo(),
            detail.pathologyNo(),
            detail.patientName(),
            detail.patientId(),
            detail.inpatientNo(),
            detail.applicationType(),
            detail.submittingDepartmentName(),
            detail.clinicalDiagnosis(),
            detail.registrationStatus(),
            detail.registeredByName(),
            detail.registeredAt(),
            detail.registrationRemarks(),
            detail.receivedAt(),
            detail.materials().stream().map(item -> new TechnicalSpecimenRegistrationMaterialResponse(
                item.specimenId(),
                item.specimenBarcode(),
                item.sequenceNo(),
                item.specimenType(),
                item.specimenName(),
                item.sourcePart(),
                item.tissueCount(),
                item.specimenSize(),
                item.frozen(),
                item.evaluationItems(),
                item.verificationStatus(),
                item.verificationCompletedAt(),
                item.verifiedByName())).toList(),
            detail.checkItems().stream().map(item -> new TechnicalSpecimenRegistrationCheckItemResponse(
                item.sequenceNo(),
                item.name())).toList());
    }

    @Operation(summary = "查询技术标本登记工作台", description = "按工作台布局返回左侧摘要、中间详情与右侧图片区数据。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @GetMapping("/{caseId}/workspace")
    public TechnicalSpecimenRegistrationWorkspaceResponse workspace(@PathVariable String caseId) {
        TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace workspace =
            technicalWorkflowAppService.getTechnicalSpecimenRegistrationWorkspace(caseId);
        return new TechnicalSpecimenRegistrationWorkspaceResponse(
            new PendingTechnicalSpecimenRegistrationResponse(
                workspace.pendingSummary().caseId(),
                workspace.pendingSummary().applicationId(),
                workspace.pendingSummary().applicationNo(),
                workspace.pendingSummary().pathologyNo(),
                workspace.pendingSummary().patientName(),
                workspace.pendingSummary().patientId(),
                workspace.pendingSummary().inpatientNo(),
                workspace.pendingSummary().applicationType(),
                workspace.pendingSummary().submittingDepartmentName(),
                workspace.pendingSummary().checkItem(),
                workspace.pendingSummary().registeredByName(),
                workspace.pendingSummary().registrationStatus(),
                workspace.pendingSummary().receivedAt(),
                workspace.pendingSummary().registeredAt()),
            new TechnicalSpecimenRegistrationBasicInfoResponse(
                workspace.basicInfo().patientName(),
                workspace.basicInfo().patientGender(),
                workspace.basicInfo().patientAge(),
                workspace.basicInfo().patientId(),
                workspace.basicInfo().inpatientNo(),
                workspace.basicInfo().applicationNo(),
                workspace.basicInfo().submittingDepartmentName(),
                workspace.basicInfo().submittingDoctorName(),
                workspace.basicInfo().submissionDate(),
                workspace.basicInfo().specimenRemovalTime(),
                workspace.basicInfo().fixationTime(),
                workspace.basicInfo().applicationType(),
                workspace.basicInfo().pathologyNo(),
                workspace.basicInfo().registrationStatus()),
            new TechnicalSpecimenRegistrationDetailSectionsResponse(
                workspace.detailSections().historySummary(),
                workspace.detailSections().clinicalExaminationAndSurgeryFindings(),
                workspace.detailSections().labAndImagingExaminations(),
                workspace.detailSections().clinicalSubmissionRequirements(),
                workspace.detailSections().infectiousAndPastHistorySummary(),
                workspace.detailSections().externalPathologyDiagnosis()),
            workspace.materials().stream().map(item -> new TechnicalSpecimenRegistrationMaterialResponse(
                item.specimenId(),
                item.specimenBarcode(),
                item.sequenceNo(),
                item.specimenType(),
                item.specimenName(),
                item.sourcePart(),
                item.tissueCount(),
                item.specimenSize(),
                item.frozen(),
                item.evaluationItems(),
                item.verificationStatus(),
                item.verificationCompletedAt(),
                item.verifiedByName())).toList(),
            workspace.checkItems().stream().map(item -> new TechnicalSpecimenRegistrationCheckItemResponse(
                item.sequenceNo(),
                item.name())).toList(),
            workspace.mediaAssets().stream().map(item -> new TechnicalSpecimenRegistrationMediaAssetResponse(
                item.assetId(),
                item.fileName(),
                item.fileUrl(),
                item.capturedAt())).toList(),
            new TechnicalSpecimenRegistrationActionFlagsResponse(
                workspace.actionFlags().canCompleteRegistration(),
                workspace.actionFlags().canSaveDetailSections(),
                workspace.actionFlags().canSaveMaterials(),
                workspace.actionFlags().canUploadMediaAssets(),
                workspace.actionFlags().canDeleteMediaAssets()));
    }

    @Operation(summary = "查询技术登记申请工作台", description = "在接收权限下查询技术标本登记页右侧核对与编辑抽屉所需的申请富字段。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @GetMapping("/{caseId}/application-workbench")
    public ApplicationRegistrationWorkbenchResponse applicationWorkbench(@PathVariable String caseId) {
        return ApplicationRegistrationWorkbenchResponseAssembler.toResponse(
            technicalWorkflowAppService.getTechnicalSpecimenRegistrationApplicationWorkbench(caseId));
    }

    @Operation(summary = "保存技术登记申请患者信息", description = "在接收权限下保存技术标本登记页编辑申请抽屉中的患者信息。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @PatchMapping("/{caseId}/application-workbench/patient-info")
    public ApplicationRegistrationWorkbenchResponse saveApplicationWorkbenchPatientInfo(
        @PathVariable String caseId,
        @Valid @RequestBody SaveApplicationRegistrationPatientInfoRequest request
    ) {
        return ApplicationRegistrationWorkbenchResponseAssembler.toResponse(
            technicalWorkflowAppService.saveTechnicalSpecimenRegistrationApplicationWorkbenchPatientInfo(
                caseId,
                new ApplicationRegistrationWorkbenchAppService.SavePatientInfoCommand(
                    toContagiousSpecimen(request.getContagiousSpecimen()),
                    toGynecologyInfo(request.getGynecologyInfo()),
                    toPatientInfo(request.getPatientInfo()),
                    toSurgeryInfo(request.getSurgeryInfo()))));
    }

    @Operation(summary = "保存技术标本登记材料", description = "批量替换技术登记阶段维护的材料列表。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @PutMapping("/{caseId}/materials")
    public TechnicalSpecimenRegistrationWorkspaceResponse saveMaterials(@PathVariable String caseId,
                                                                       @Valid @RequestBody TechnicalSpecimenRegistrationMaterialsSaveRequest request,
                                                                       HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace workspace =
            technicalWorkflowAppService.saveTechnicalSpecimenRegistrationMaterials(
                new TechnicalWorkflowModels.SaveTechnicalSpecimenRegistrationMaterialsCommand(
                    caseId,
                    resolveUserId(httpServletRequest),
                    resolveOperatorName(httpServletRequest),
                    request.getTerminalCode(),
                    request.getMaterials().stream().map(item ->
                        new TechnicalWorkflowModels.TechnicalSpecimenRegistrationMaterialInput(
                            item.getSpecimenId(),
                            item.getSpecimenType(),
                            item.getSpecimenName(),
                            item.getSourcePart(),
                            item.getTissueCount(),
                            item.getSpecimenSize(),
                            item.getFrozen(),
                            item.getEvaluationItems())).toList()));
        return workspace(caseId);
    }

    @Operation(summary = "核对技术标本登记材料", description = "立即将选中材料标记为已核对，并记录核对时间与核对人。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @PostMapping("/{caseId}/materials/{specimenId}/verify")
    public TechnicalSpecimenRegistrationWorkspaceResponse verifyMaterial(@PathVariable String caseId,
                                                                         @PathVariable String specimenId,
                                                                         @Valid @RequestBody TechnicalSpecimenRegistrationMaterialVerificationRequest request,
                                                                         HttpServletRequest httpServletRequest) {
        technicalWorkflowAppService.verifyTechnicalSpecimenRegistrationMaterial(
            new TechnicalWorkflowModels.TechnicalSpecimenRegistrationMaterialVerificationCommand(
                caseId,
                specimenId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return workspace(caseId);
    }

    @Operation(summary = "取消技术标本登记材料核对", description = "清空选中材料核对时间与核对人，并回到待核对状态。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @PostMapping("/{caseId}/materials/{specimenId}/cancel-verification")
    public TechnicalSpecimenRegistrationWorkspaceResponse cancelMaterialVerification(@PathVariable String caseId,
                                                                                    @PathVariable String specimenId,
                                                                                    @Valid @RequestBody TechnicalSpecimenRegistrationMaterialVerificationRequest request,
                                                                                    HttpServletRequest httpServletRequest) {
        technicalWorkflowAppService.cancelTechnicalSpecimenRegistrationMaterialVerification(
            new TechnicalWorkflowModels.TechnicalSpecimenRegistrationMaterialVerificationCommand(
                caseId,
                specimenId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return workspace(caseId);
    }

    @Operation(summary = "保存技术标本登记摘要分区", description = "保存技术登记工作区 6 个摘要卡片的人工覆盖内容。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @PatchMapping("/{caseId}/detail-sections")
    public TechnicalSpecimenRegistrationWorkspaceResponse saveDetailSections(
        @PathVariable String caseId,
        @Valid @RequestBody TechnicalSpecimenRegistrationDetailSectionsSaveRequest request,
        HttpServletRequest httpServletRequest
    ) {
        technicalWorkflowAppService.saveTechnicalSpecimenRegistrationDetailSections(
            new TechnicalWorkflowModels.SaveTechnicalSpecimenRegistrationDetailSectionsCommand(
                caseId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                new TechnicalWorkflowModels.TechnicalSpecimenRegistrationDetailSections(
                    request.getDetailSections().getHistorySummary(),
                    request.getDetailSections().getClinicalExaminationAndSurgeryFindings(),
                    request.getDetailSections().getLabAndImagingExaminations(),
                    request.getDetailSections().getClinicalSubmissionRequirements(),
                    request.getDetailSections().getInfectiousAndPastHistorySummary(),
                    request.getDetailSections().getExternalPathologyDiagnosis())));
        return workspace(caseId);
    }

    @Operation(summary = "上传技术标本登记图片", description = "上传并挂接技术登记阶段的右侧图片区附件。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @PostMapping(value = "/{caseId}/media-assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TechnicalSpecimenRegistrationMediaAssetResponse uploadMediaAsset(@PathVariable String caseId,
                                                                            @RequestParam("file") MultipartFile file,
                                                                            HttpServletRequest httpServletRequest) {
        var stored = grossingMediaStorageService.store(file);
        TechnicalWorkflowModels.TechnicalSpecimenRegistrationMediaAsset asset =
            technicalWorkflowAppService.uploadTechnicalSpecimenRegistrationMediaAsset(
                new TechnicalWorkflowModels.UploadTechnicalSpecimenRegistrationMediaAssetCommand(
                    caseId,
                    resolveUserId(httpServletRequest),
                    resolveOperatorName(httpServletRequest),
                    null,
                    stored.fileName(),
                    stored.fileUrl()));
        return new TechnicalSpecimenRegistrationMediaAssetResponse(
            asset.assetId(),
            asset.fileName(),
            asset.fileUrl(),
            asset.capturedAt());
    }

    @Operation(summary = "删除技术标本登记图片", description = "删除技术登记阶段的右侧图片区附件。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @DeleteMapping("/{caseId}/media-assets/{assetId}")
    public TechnicalSpecimenRegistrationMediaAssetDeleteResponse deleteMediaAsset(@PathVariable String caseId,
                                                                                  @PathVariable String assetId,
                                                                                  HttpServletRequest httpServletRequest) {
        technicalWorkflowAppService.deleteTechnicalSpecimenRegistrationMediaAsset(
            new TechnicalWorkflowModels.DeleteTechnicalSpecimenRegistrationMediaAssetCommand(
                caseId,
                assetId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                null));
        return new TechnicalSpecimenRegistrationMediaAssetDeleteResponse(assetId, true);
    }

    @Operation(summary = "完成技术标本登记", description = "完成登记并生成病例取材任务。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @PostMapping("/{caseId}/complete")
    public TechnicalSpecimenRegistrationCompleteResponse complete(@PathVariable String caseId,
                                                                 @Valid @RequestBody TechnicalSpecimenRegistrationCompleteRequest request,
                                                                 HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.TechnicalSpecimenRegistrationCompleteResult result =
            technicalWorkflowAppService.completeTechnicalSpecimenRegistration(
                new TechnicalWorkflowModels.CompleteTechnicalSpecimenRegistrationCommand(
                    caseId,
                    resolveUserId(httpServletRequest),
                    resolveOperatorName(httpServletRequest),
                    request.getTerminalCode(),
                    request.getRemarks()));
        return new TechnicalSpecimenRegistrationCompleteResponse(
            result.caseId(),
            result.pathologyNo(),
            result.registrationStatus(),
            result.grossingTaskCreated());
    }

    private ApplicationRegistrationWorkbenchAppService.ContagiousSpecimen toContagiousSpecimen(
        com.company.bl.interfaces.dto.SaveApplicationRegistrationWorkbenchRequest.ContagiousSpecimen request
    ) {
        return new ApplicationRegistrationWorkbenchAppService.ContagiousSpecimen(
            request.isHepatitis(),
            request.isHiv(),
            request.isIsolation(),
            request.isSyphilis(),
            request.isTuberculosis());
    }

    private ApplicationRegistrationWorkbenchAppService.GynecologyInfo toGynecologyInfo(
        com.company.bl.interfaces.dto.SaveApplicationRegistrationWorkbenchRequest.GynecologyInfo request
    ) {
        return new ApplicationRegistrationWorkbenchAppService.GynecologyInfo(
            request.getAdditionalNotes(),
            request.getHpvResult(),
            request.getLastMenstrualPeriod(),
            request.isMenopause(),
            request.getPreviousCytology(),
            request.getPreviousTreatment(),
            new ApplicationRegistrationWorkbenchAppService.SpecialConditions(
                request.getSpecialConditions().isAbnormalBleeding(),
                request.getSpecialConditions().isBirthControl(),
                request.getSpecialConditions().isHormoneReplacement(),
                request.getSpecialConditions().isHysterectomy(),
                request.getSpecialConditions().isIud(),
                request.getSpecialConditions().isLactation(),
                request.getSpecialConditions().isMenopause(),
                request.getSpecialConditions().getOther(),
                request.getSpecialConditions().isPregnancy(),
                request.getSpecialConditions().isRadiotherapy()));
    }

    private ApplicationRegistrationWorkbenchAppService.PatientInfo toPatientInfo(
        com.company.bl.interfaces.dto.SaveApplicationRegistrationWorkbenchRequest.PatientInfo request
    ) {
        return new ApplicationRegistrationWorkbenchAppService.PatientInfo(
            request.getAge(),
            request.getApplicationDate(),
            request.getApplicationNo(),
            request.getApplyDept(),
            request.getApplyDoctor(),
            request.getBedNo(),
            request.getCheckItem(),
            request.getClinicalDiagnosis(),
            request.getClinicalHistory(),
            request.getDeliveryRequirement(),
            request.getEndoscopyDiagnosis(),
            request.isFrozenReminder(),
            request.getGender(),
            request.getIdNo(),
            request.getImagingResult(),
            request.getInpatientNo(),
            request.getPatientName(),
            request.isPatientVerified(),
            request.getPhone(),
            request.getRegistrationStatus(),
            request.getRemark(),
            request.getSpecimenType(),
            request.getWardName());
    }

    private ApplicationRegistrationWorkbenchAppService.SurgeryInfo toSurgeryInfo(
        com.company.bl.interfaces.dto.SaveApplicationRegistrationWorkbenchRequest.SurgeryInfo request
    ) {
        return new ApplicationRegistrationWorkbenchAppService.SurgeryInfo(
            request.getBuildingId(),
            request.getClinicalFindings(),
            request.getFixativeType(),
            request.getFixationPerson(),
            request.getFixationTime(),
            request.getRoomId(),
            request.getSpecimenRemovalTime(),
            request.getSurgeryName());
    }
}
