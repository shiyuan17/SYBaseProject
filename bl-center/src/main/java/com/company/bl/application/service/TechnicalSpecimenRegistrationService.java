package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.ReceiptStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.ApplicationRegistrationWorkbenchRepository;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.MedicalOrderRepository;
import com.company.bl.domain.repository.SpecimenWorkflowCommandRepository;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import com.company.bl.domain.valueobject.ApplicationId;
import com.company.bl.support.application.NumberingService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
class TechnicalSpecimenRegistrationService {

    private static final String DEFAULT_COLLECTION_MODE = "SURGERY";
    private static final String DEFAULT_COLLECTION_SCENE = "OPERATING_ROOM";
    private static final String DEFAULT_CONTAINER_NAME = "Specimen Bottle";
    private static final String DEFAULT_FIXATIVE_TYPE = "FORMALIN";
    private static final String DEFAULT_QUALITY_CHECK_RESULT = "PASSED";
    private static final String DEFAULT_SPECIMEN_SIZE = "小标本";
    private static final String DEFAULT_SPECIMEN_TYPE = "活体";
    private static final int DEFAULT_TISSUE_COUNT = 1;
    private static final String REGISTRATION_STATUS_PENDING = "PENDING";
    private static final Set<String> LIST_REGISTRATION_STATUSES = Set.of("PENDING", "COMPLETED");
    private static final String REGISTRATION_MEDIA_OBJECT_TYPE = "TECHNICAL_SPECIMEN_REGISTRATION";
    private static final String REGISTRATION_MEDIA_TYPE = "REGISTRATION_IMAGE";

    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final TechnicalWorkflowSupport technicalWorkflowSupport;
    private final ApplicationRepository applicationRepository;
    private final ApplicationRegistrationWorkbenchAppService applicationRegistrationWorkbenchAppService;
    private final ApplicationRegistrationWorkbenchRepository workbenchRepository;
    private final MedicalOrderRepository medicalOrderRepository;
    private final SpecimenWorkflowCommandRepository specimenWorkflowCommandRepository;
    private final NumberingService numberingService;

    TechnicalSpecimenRegistrationService(TechnicalWorkflowRepository technicalWorkflowRepository,
                                         TechnicalWorkflowSupport technicalWorkflowSupport,
                                         ApplicationRepository applicationRepository,
                                         ApplicationRegistrationWorkbenchAppService applicationRegistrationWorkbenchAppService,
                                         ApplicationRegistrationWorkbenchRepository workbenchRepository,
                                         MedicalOrderRepository medicalOrderRepository,
                                         SpecimenWorkflowCommandRepository specimenWorkflowCommandRepository,
                                         NumberingService numberingService) {
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.technicalWorkflowSupport = technicalWorkflowSupport;
        this.applicationRepository = applicationRepository;
        this.applicationRegistrationWorkbenchAppService = applicationRegistrationWorkbenchAppService;
        this.workbenchRepository = workbenchRepository;
        this.medicalOrderRepository = medicalOrderRepository;
        this.specimenWorkflowCommandRepository = specimenWorkflowCommandRepository;
        this.numberingService = numberingService;
    }

    @Transactional(readOnly = true)
    TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationPage listPendingRegistrations(
        TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationQuery query
    ) {
        return listRegistrations(new TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationQuery(
            query.page(),
            query.size(),
            query.keyword(),
            query.applicationType(),
            REGISTRATION_STATUS_PENDING,
            query.receivedFrom(),
            query.receivedTo()));
    }

    @Transactional(readOnly = true)
    TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationPage listRegistrations(
        TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationQuery query
    ) {
        String registrationStatus = normalizeListRegistrationStatus(query.registrationStatus());
        TechnicalWorkflowRecords.PagedTechnicalSpecimenRegistrations paged =
            technicalWorkflowRepository.findTechnicalSpecimenRegistrations(
                new TechnicalWorkflowRecords.PendingTechnicalSpecimenRegistrationQuery(
                    query.page(),
                    query.size(),
                    query.keyword(),
                    query.applicationType(),
                    registrationStatus,
                    query.receivedFrom(),
                    query.receivedTo()));
        return new TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationPage(
            paged.items().stream().map(item -> new TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationItem(
                item.caseId(),
                item.applicationId(),
                item.applicationNo(),
                visiblePathologyNo(item, item.pathologyNo()),
                item.patientName(),
                item.patientGender(),
                item.patientAge(),
                item.patientId(),
                item.patientIdDisplay(),
                item.inpatientNo(),
                item.applicationType(),
                item.submittingDepartmentName(),
                item.checkItem(),
                item.registeredByName(),
                item.registrationStatus(),
                stringify(item.receivedAt()),
                stringify(item.registeredAt()))).toList(),
            query.page(),
            query.size(),
            paged.total());
    }

    @Transactional(readOnly = true)
    TechnicalWorkflowModels.TechnicalSpecimenRegistrationDetail getRegistrationDetail(String caseId) {
        return buildRegistrationDetail(loadWorkspaceContext(caseId, false));
    }

    @Transactional(readOnly = true)
    TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace getRegistrationWorkspace(String caseId) {
        return buildRegistrationWorkspace(loadWorkspaceContext(caseId, false));
    }

    @Transactional(readOnly = true)
    TechnicalWorkflowModels.TechnicalSpecimenRegistrationDetail getRegistrationDetailForGrossingContext(String caseId) {
        return buildRegistrationDetail(loadWorkspaceContext(caseId, true));
    }

    @Transactional(readOnly = true)
    TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace getRegistrationWorkspaceForGrossingContext(String caseId) {
        return buildRegistrationWorkspace(loadWorkspaceContext(caseId, true));
    }

    @Transactional(readOnly = true)
    ApplicationRegistrationWorkbenchAppService.WorkbenchRecord getApplicationWorkbench(String caseId) {
        WorkspaceContext context = loadWorkspaceContext(caseId, false);
        return applicationRegistrationWorkbenchAppService.getByApplicationId(context.application().getId().value());
    }

    @Transactional
    ApplicationRegistrationWorkbenchAppService.WorkbenchRecord saveApplicationWorkbenchPatientInfo(
        String caseId,
        ApplicationRegistrationWorkbenchAppService.SavePatientInfoCommand command
    ) {
        WorkspaceContext context = loadEditableWorkspaceContext(caseId);
        return applicationRegistrationWorkbenchAppService.savePatientInfoForTechnicalRegistration(
            context.application(),
            command);
    }

    private TechnicalWorkflowModels.TechnicalSpecimenRegistrationDetail buildRegistrationDetail(WorkspaceContext context) {
        TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace workspace = buildRegistrationWorkspace(context);
        return new TechnicalWorkflowModels.TechnicalSpecimenRegistrationDetail(
            workspace.pendingSummary().caseId(),
            workspace.pendingSummary().applicationId(),
            workspace.pendingSummary().applicationNo(),
            workspace.basicInfo().pathologyNo(),
            workspace.basicInfo().patientName(),
            workspace.basicInfo().patientId(),
            workspace.basicInfo().patientIdDisplay(),
            workspace.basicInfo().inpatientNo(),
            workspace.basicInfo().applicationType(),
            workspace.basicInfo().submittingDepartmentName(),
            context.application().getClinicalDiagnosis(),
            workspace.basicInfo().registrationStatus(),
            workspace.pendingSummary().registeredByName(),
            workspace.pendingSummary().registeredAt(),
            null,
            workspace.pendingSummary().receivedAt(),
            workspace.materials(),
            workspace.checkItems());
    }

    private TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace buildRegistrationWorkspace(
        WorkspaceContext context
    ) {
        boolean editable = isEditable(context.registration());
        String historySummary = valueOf(() -> context.extension().clinicalHistory());
        String clinicalExaminationAndSurgeryFindings = joinLabeledSections(
            labeledValue("临床检查", valueOf(() -> context.extension().clinicalFindings())),
            labeledValue("手术名称", valueOf(() -> context.extension().surgeryName())));
        String labAndImagingExaminations = joinLabeledSections(
            labeledValue("影像检查", valueOf(() -> context.extension().imagingResult())),
            labeledValue("内镜所见", valueOf(() -> context.extension().endoscopyDiagnosis())));
        String clinicalSubmissionRequirements =
            valueOf(() -> context.extension().deliveryRequirement());
        String infectiousAndPastHistorySummary = buildInfectiousSummary(context.extension());
        return new TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace(
            toPendingSummary(context.registration()),
            new TechnicalWorkflowModels.TechnicalSpecimenRegistrationBasicInfo(
                context.application().getPatientName(),
                context.application().getPatientGender(),
                context.application().getPatientAge(),
                context.application().getPatientId(),
                valueOf(() -> context.extension().idNo()),
                valueOf(() -> context.extension().inpatientNo()),
                context.application().getApplicationNo(),
                context.application().getSubmittingDepartmentName(),
                context.application().getSubmittingDoctorName(),
                stringify(context.application().getSubmissionDate()),
                resolveSpecimenRemovalTime(context),
                stringify(valueOf(() -> context.extension().fixationTime())),
                context.application().getApplicationType(),
                visiblePathologyNo(context.registration(), context.pathologyCase().pathologyNo()),
                context.registration().registrationStatus()),
            new TechnicalWorkflowModels.TechnicalSpecimenRegistrationDetailSections(
                overrideOrFallback(
                    valueOf(() -> context.detailSectionOverrides().historySummaryOverride()),
                    historySummary),
                overrideOrFallback(
                    valueOf(() -> context.detailSectionOverrides().clinicalExaminationAndSurgeryFindingsOverride()),
                    clinicalExaminationAndSurgeryFindings),
                overrideOrFallback(
                    valueOf(() -> context.detailSectionOverrides().labAndImagingExaminationsOverride()),
                    labAndImagingExaminations),
                overrideOrFallback(
                    valueOf(() -> context.detailSectionOverrides().clinicalSubmissionRequirementsOverride()),
                    clinicalSubmissionRequirements),
                overrideOrFallback(
                    valueOf(() -> context.detailSectionOverrides().infectiousAndPastHistorySummaryOverride()),
                    infectiousAndPastHistorySummary),
                normalizeSectionValue(
                    valueOf(() -> context.detailSectionOverrides().externalPathologyDiagnosisOverride()))),
            buildMaterials(context.specimens()),
            buildCheckItems(valueOf(() -> context.extension().checkItem()), context.registration().caseId()),
            buildMediaAssets(context.mediaAssets()),
            new TechnicalWorkflowModels.TechnicalSpecimenRegistrationActionFlags(
                editable,
                editable,
                editable,
                editable,
                editable));
    }

    @Transactional
    TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace saveRegistrationMaterials(
        TechnicalWorkflowModels.SaveTechnicalSpecimenRegistrationMaterialsCommand command
    ) {
        WorkspaceContext context = loadEditableWorkspaceContext(command.caseId());
        Map<String, Specimen> existingSpecimens = new LinkedHashMap<>();
        for (Specimen specimen : context.specimens()) {
            existingSpecimens.put(specimen.id(), specimen);
        }
        LinkedHashSet<String> retainedSpecimenIds = new LinkedHashSet<>();
        for (TechnicalWorkflowModels.TechnicalSpecimenRegistrationMaterialInput item : command.materials()) {
            MaterialPayload payload = normalizeMaterial(item);
            if (payload.specimenId() != null && existingSpecimens.containsKey(payload.specimenId())) {
                specimenWorkflowCommandRepository.updateSpecimenMaterial(
                    payload.specimenId(),
                    payload.specimenType(),
                    payload.specimenName(),
                    payload.sourcePart(),
                    payload.tissueCount(),
                    payload.specimenSize(),
                    payload.frozen(),
                    serializeEvaluationItems(payload.evaluationItems()),
                    "Updated during technical specimen registration");
                retainedSpecimenIds.add(payload.specimenId());
                continue;
            }
            Specimen created = createRegistrationSpecimen(context, payload, command);
            retainedSpecimenIds.add(created.id());
        }
        for (Specimen specimen : context.specimens()) {
            if (retainedSpecimenIds.contains(specimen.id())) {
                continue;
            }
            removeRegistrationSpecimen(context.pathologyCase(), specimen, command);
        }
        technicalWorkflowSupport.insertWorkflowEvent(
            context.application().getId().value(),
            null,
            context.pathologyCase().id(),
            TechnicalWorkflowConstants.NODE_SPECIMEN_REGISTRATION,
            "SAVE_MATERIALS",
            "SUCCESS",
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Technical specimen registration materials saved");
        return getRegistrationWorkspace(command.caseId());
    }

    @Transactional
    TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace verifyRegistrationMaterial(
        TechnicalWorkflowModels.TechnicalSpecimenRegistrationMaterialVerificationCommand command
    ) {
        WorkspaceContext context = loadEditableWorkspaceContext(command.caseId());
        Specimen specimen = requireMaterialSpecimen(context, command.specimenId());
        LocalDateTime now = LocalDateTime.now();
        specimenWorkflowCommandRepository.verifySpecimenImmediately(
            context.application().getId().value(),
            specimen.id(),
            command.operatorUserId(),
            command.operatorName(),
            now,
            command.terminalCode(),
            command.remarks());
        specimenWorkflowCommandRepository.insertWorkflowEvent(new TrackingEvent(
            "EVT-" + UUID.randomUUID(),
            specimen.applicationId(),
            specimen.id(),
            context.pathologyCase().id(),
            null,
            TechnicalWorkflowConstants.NODE_SPECIMEN_REGISTRATION,
            "VERIFY_MATERIAL",
            "SUCCESS",
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Verified specimen during technical specimen registration", null));
        return getRegistrationWorkspace(command.caseId());
    }

    @Transactional
    TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace cancelRegistrationMaterialVerification(
        TechnicalWorkflowModels.TechnicalSpecimenRegistrationMaterialVerificationCommand command
    ) {
        WorkspaceContext context = loadEditableWorkspaceContext(command.caseId());
        Specimen specimen = requireMaterialSpecimen(context, command.specimenId());
        LocalDateTime now = LocalDateTime.now();
        specimenWorkflowCommandRepository.cancelSpecimenVerification(
            specimen.id(),
            command.terminalCode(),
            command.remarks());
        specimenWorkflowCommandRepository.insertWorkflowEvent(new TrackingEvent(
            "EVT-" + UUID.randomUUID(),
            specimen.applicationId(),
            specimen.id(),
            context.pathologyCase().id(),
            null,
            TechnicalWorkflowConstants.NODE_SPECIMEN_REGISTRATION,
            "CANCEL_MATERIAL_VERIFICATION",
            "SUCCESS",
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Canceled specimen verification during technical specimen registration", null));
        return getRegistrationWorkspace(command.caseId());
    }

    @Transactional
    TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace saveRegistrationDetailSections(
        TechnicalWorkflowModels.SaveTechnicalSpecimenRegistrationDetailSectionsCommand command
    ) {
        WorkspaceContext context = loadEditableWorkspaceContext(command.caseId());
        TechnicalWorkflowModels.TechnicalSpecimenRegistrationDetailSections detailSections = command.detailSections();
        workbenchRepository.upsertTechnicalRegistrationDetailSectionOverrides(
            new ApplicationRegistrationWorkbenchRepository.SaveTechnicalRegistrationDetailSectionOverridesCommand(
                context.application().getId().value(),
                normalizeSectionValue(detailSections.historySummary()),
                normalizeSectionValue(detailSections.clinicalExaminationAndSurgeryFindings()),
                normalizeSectionValue(detailSections.labAndImagingExaminations()),
                normalizeSectionValue(detailSections.clinicalSubmissionRequirements()),
                normalizeSectionValue(detailSections.infectiousAndPastHistorySummary()),
                normalizeSectionValue(detailSections.externalPathologyDiagnosis())));
        technicalWorkflowSupport.insertWorkflowEvent(
            context.application().getId().value(),
            null,
            context.pathologyCase().id(),
            TechnicalWorkflowConstants.NODE_SPECIMEN_REGISTRATION,
            "SAVE_DETAIL_SECTIONS",
            "SUCCESS",
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Technical specimen registration detail sections saved");
        return getRegistrationWorkspace(command.caseId());
    }

    @Transactional
    TechnicalWorkflowModels.TechnicalSpecimenRegistrationMediaAsset uploadMediaAsset(
        TechnicalWorkflowModels.UploadTechnicalSpecimenRegistrationMediaAssetCommand command
    ) {
        WorkspaceContext context = loadEditableWorkspaceContext(command.caseId());
        LocalDateTime now = LocalDateTime.now();
        TechnicalWorkflowRecords.CaseMediaAsset asset = new TechnicalWorkflowRecords.CaseMediaAsset(
            technicalWorkflowSupport.nextId("MED"),
            context.pathologyCase().id(),
            null,
            REGISTRATION_MEDIA_OBJECT_TYPE,
            context.pathologyCase().id(),
            REGISTRATION_MEDIA_TYPE,
            command.fileUrl(),
            command.fileName(),
            now,
            command.operatorUserId(),
            command.operatorName(),
            null);
        technicalWorkflowRepository.insertCaseMediaAsset(
            new com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateCaseMediaAssetCommand(
                asset.id(),
                asset.caseId(),
                asset.specimenId(),
                asset.objectType(),
                asset.objectId(),
                asset.mediaType(),
                asset.fileUrl(),
                asset.fileName(),
                asset.capturedAt(),
                asset.capturedByUserId(),
                asset.capturedByName(),
                asset.remarks()));
        technicalWorkflowSupport.insertWorkflowEvent(
            context.application().getId().value(),
            null,
            context.pathologyCase().id(),
            TechnicalWorkflowConstants.NODE_SPECIMEN_REGISTRATION,
            "UPLOAD_MEDIA",
            "SUCCESS",
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Technical specimen registration image uploaded");
        return toMediaAsset(asset);
    }

    @Transactional
    void deleteMediaAsset(TechnicalWorkflowModels.DeleteTechnicalSpecimenRegistrationMediaAssetCommand command) {
        WorkspaceContext context = loadEditableWorkspaceContext(command.caseId());
        TechnicalWorkflowRecords.CaseMediaAsset asset = technicalWorkflowRepository.findCaseMediaAssetById(command.assetId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Technical specimen registration image not found"));
        if (!context.pathologyCase().id().equals(asset.caseId())
            || !REGISTRATION_MEDIA_OBJECT_TYPE.equals(asset.objectType())
            || !context.pathologyCase().id().equals(asset.objectId())
            || !REGISTRATION_MEDIA_TYPE.equals(asset.mediaType())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Technical specimen registration image does not belong to case");
        }
        technicalWorkflowRepository.deleteCaseMediaAsset(asset.id());
        technicalWorkflowSupport.insertWorkflowEvent(
            context.application().getId().value(),
            null,
            context.pathologyCase().id(),
            TechnicalWorkflowConstants.NODE_SPECIMEN_REGISTRATION,
            "DELETE_MEDIA",
            "SUCCESS",
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Technical specimen registration image deleted");
    }

    @Transactional
    TechnicalWorkflowModels.TechnicalSpecimenRegistrationCompleteResult completeRegistration(
        TechnicalWorkflowModels.CompleteTechnicalSpecimenRegistrationCommand command
    ) {
        TechnicalWorkflowRecords.TechnicalSpecimenRegistration registration = getRegistration(command.caseId());
        PathologyCase pathologyCase = technicalWorkflowSupport.getCase(command.caseId());
        Application application = applicationRepository.findById(
            new ApplicationId(pathologyCase.applicationId())
        ).orElseThrow(() -> new BlBusinessException(
            BlErrorCode.RESOURCE_NOT_FOUND,
            404,
            "Application not found"
        ));
        String normalizedApplicationType = normalizeApplicationType(
            command.applicationType(),
            application.getApplicationType()
        );
        if (!"COMPLETED".equals(registration.registrationStatus())) {
            technicalWorkflowRepository.completeTechnicalSpecimenRegistration(
                command.caseId(),
                command.operatorUserId(),
                command.operatorName(),
                command.remarks(),
                LocalDateTime.now());
            technicalWorkflowSupport.insertWorkflowEvent(
                pathologyCase.applicationId(),
                null,
                pathologyCase.id(),
                TechnicalWorkflowConstants.NODE_SPECIMEN_REGISTRATION,
                "COMPLETE",
                "SUCCESS",
                command.operatorUserId(),
                command.operatorName(),
                command.terminalCode(),
                "Technical specimen registration completed");
        }

        if (!normalizedApplicationType.equals(trimToEmpty(application.getApplicationType()))) {
            Application updatedApplication = new Application(
                application.getId(),
                application.getApplicationNo(),
                application.getPatientId(),
                application.getPatientName(),
                application.getPatientGender(),
                application.getPatientAge(),
                normalizedApplicationType,
                application.getStatus(),
                application.getApplicationFormStatus(),
                application.getExternalOrderNo(),
                application.getThirdPartySource(),
                application.getSourceHospitalId(),
                application.getSourceHospitalName(),
                application.getSubmittingDepartmentId(),
                application.getSubmittingDepartmentName(),
                application.getSubmittingDoctorUserId(),
                application.getSubmittingDoctorName(),
                application.getClinicalDiagnosis(),
                application.getClinicalSymptom(),
                application.getSpecimenSite(),
                application.getApplicationDate(),
                application.getSubmissionDate(),
                application.getSpecimenRemovalTime(),
                application.getRemarks(),
                application.getCreatedAt(),
                LocalDateTime.now()
            );
            applicationRepository.update(updatedApplication);
            application = updatedApplication;
        }

        String pathologyNo = resolveCompletionPathologyNo(
            command.caseId(),
            normalizedApplicationType,
            pathologyCase.pathologyNo(),
            command.pathologyNo());

        boolean grossingTaskCreated = false;
        if (technicalWorkflowRepository.findActiveTechnicalTasksByObject(
            TechnicalWorkflowConstants.NODE_GROSSING,
            TechnicalWorkflowConstants.OBJECT_CASE,
            command.caseId()).isEmpty()) {
            List<Specimen> specimens = technicalWorkflowRepository.findSpecimensByCaseId(command.caseId());
            technicalWorkflowSupport.createTechnicalTaskIfAbsent(
                pathologyCase.applicationId(),
                pathologyCase.id(),
                null,
                TechnicalWorkflowConstants.NODE_GROSSING,
                TechnicalWorkflowConstants.OBJECT_CASE,
                command.caseId(),
                null,
                "pathologyNo=" + pathologyNo + ";receivedCount=" + specimens.size() + ";processedCount=" + specimens.size(),
                null);
            technicalWorkflowSupport.insertWorkflowEvent(
                pathologyCase.applicationId(),
                null,
                pathologyCase.id(),
                TechnicalWorkflowConstants.NODE_GROSSING,
                "CREATE",
                "SUCCESS",
                command.operatorUserId(),
                command.operatorName(),
                command.terminalCode(),
                "Grossing task created after technical specimen registration");
            grossingTaskCreated = true;
        }

        return new TechnicalWorkflowModels.TechnicalSpecimenRegistrationCompleteResult(
            pathologyCase.id(),
            pathologyNo,
            "COMPLETED",
            grossingTaskCreated);
    }

    private TechnicalWorkflowRecords.TechnicalSpecimenRegistration getRegistration(String caseId) {
        return technicalWorkflowRepository.findTechnicalSpecimenRegistrationByCaseId(caseId)
            .orElseThrow(() -> new BlBusinessException(
                BlErrorCode.RESOURCE_NOT_FOUND,
                404,
                "Technical specimen registration not found"));
    }

    private WorkspaceContext loadWorkspaceContext(String caseId, boolean allowRegistrationFallback) {
        PathologyCase pathologyCase = technicalWorkflowSupport.getCase(caseId);
        Application application = applicationRepository.findById(new ApplicationId(pathologyCase.applicationId()))
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Application not found"));
        ApplicationRegistrationWorkbenchRepository.WorkbenchExtensionData extension =
            workbenchRepository.findExtensionByApplicationId(application.getId().value()).orElse(null);
        ApplicationRegistrationWorkbenchRepository.TechnicalRegistrationDetailSectionOverrides detailSectionOverrides =
            workbenchRepository.findTechnicalRegistrationDetailSectionOverridesByApplicationId(application.getId().value())
                .orElse(null);
        TechnicalWorkflowRecords.TechnicalSpecimenRegistration registration =
            resolveRegistration(caseId, pathologyCase, application, extension, allowRegistrationFallback);
        List<Specimen> specimens = technicalWorkflowRepository.findSpecimensByCaseId(caseId);
        List<TechnicalWorkflowRecords.CaseMediaAsset> mediaAssets = technicalWorkflowRepository.findCaseMediaAssets(
            caseId,
            REGISTRATION_MEDIA_OBJECT_TYPE,
            caseId,
            REGISTRATION_MEDIA_TYPE);
        return new WorkspaceContext(
            registration,
            pathologyCase,
            application,
            extension,
            detailSectionOverrides,
            specimens,
            mediaAssets);
    }

    private TechnicalWorkflowRecords.TechnicalSpecimenRegistration resolveRegistration(
        String caseId,
        PathologyCase pathologyCase,
        Application application,
        ApplicationRegistrationWorkbenchRepository.WorkbenchExtensionData extension,
        boolean allowRegistrationFallback
    ) {
        try {
            return getRegistration(caseId);
        } catch (BlBusinessException | DataAccessException exception) {
            if (!allowRegistrationFallback || !shouldFallbackToSyntheticRegistration(exception)) {
                throw exception;
            }
            return createSyntheticRegistration(pathologyCase, application, extension);
        }
    }

    private boolean shouldFallbackToSyntheticRegistration(RuntimeException exception) {
        if (exception instanceof BlBusinessException businessException) {
            return businessException.getErrorCode() == BlErrorCode.RESOURCE_NOT_FOUND;
        }
        if (exception instanceof DataAccessException dataAccessException) {
            return containsIgnoreCase(
                dataAccessException.getMostSpecificCause() == null
                    ? dataAccessException.getMessage()
                    : dataAccessException.getMostSpecificCause().getMessage(),
                "technical_specimen_registrations");
        }
        return false;
    }

    private TechnicalWorkflowRecords.TechnicalSpecimenRegistration createSyntheticRegistration(
        PathologyCase pathologyCase,
        Application application,
        ApplicationRegistrationWorkbenchRepository.WorkbenchExtensionData extension
    ) {
        LocalDateTime referenceTime = pathologyCase.receivedAt();
        return new TechnicalWorkflowRecords.TechnicalSpecimenRegistration(
            pathologyCase.id(),
            application.getId().value(),
            pathologyCase.pathologyNo(),
            application.getApplicationNo(),
            application.getPatientName(),
            application.getPatientGender(),
            application.getPatientAge(),
            application.getPatientId(),
            valueOf(() -> extension.idNo()),
            valueOf(() -> extension.inpatientNo()),
            application.getApplicationType(),
            application.getSubmittingDepartmentName(),
            valueOf(() -> extension.checkItem()),
            "COMPLETED",
            null,
            null,
            null,
            null,
            referenceTime,
            referenceTime,
            referenceTime);
    }

    private WorkspaceContext loadEditableWorkspaceContext(String caseId) {
        WorkspaceContext context = loadWorkspaceContext(caseId, false);
        if (!isEditable(context.registration())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Technical specimen registration is completed");
        }
        return context;
    }

    private boolean isEditable(TechnicalWorkflowRecords.TechnicalSpecimenRegistration registration) {
        return "PENDING".equalsIgnoreCase(registration.registrationStatus());
    }

    private String visiblePathologyNo(
        TechnicalWorkflowRecords.TechnicalSpecimenRegistration registration,
        String pathologyNo
    ) {
        return isEditable(registration) ? null : pathologyNo;
    }

    private TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationItem toPendingSummary(
        TechnicalWorkflowRecords.TechnicalSpecimenRegistration registration
    ) {
        return new TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationItem(
            registration.caseId(),
            registration.applicationId(),
            registration.applicationNo(),
            visiblePathologyNo(registration, registration.pathologyNo()),
            registration.patientName(),
            registration.patientGender(),
            registration.patientAge(),
            registration.patientId(),
            registration.patientIdDisplay(),
            registration.inpatientNo(),
            registration.applicationType(),
            registration.submittingDepartmentName(),
            registration.checkItem(),
            registration.registeredByName(),
            registration.registrationStatus(),
            stringify(registration.receivedAt()),
            stringify(registration.registeredAt()));
    }

    private List<TechnicalWorkflowModels.TechnicalSpecimenRegistrationMaterial> buildMaterials(List<Specimen> specimens) {
        List<TechnicalWorkflowModels.TechnicalSpecimenRegistrationMaterial> materials = new ArrayList<>();
        int sequenceNo = 0;
        for (Specimen specimen : specimens) {
            materials.add(new TechnicalWorkflowModels.TechnicalSpecimenRegistrationMaterial(
                specimen.id(),
                specimen.barcode(),
                ++sequenceNo,
                specimen.specimenType(),
                specimen.specimenNameStandardized(),
                specimen.specimenSite(),
                specimen.specimenCount() == null ? DEFAULT_TISSUE_COUNT : specimen.specimenCount(),
                specimen.specimenSize() == null ? DEFAULT_SPECIMEN_SIZE : specimen.specimenSize(),
                specimen.frozen(),
                deserializeEvaluationItems(specimen.registrationEvaluationItems()),
                specimen.verificationStatus(),
                stringify(specimen.verificationCompletedAt()),
                specimen.verifiedByName()));
        }
        return materials;
    }

    private List<TechnicalWorkflowModels.TechnicalSpecimenRegistrationMediaAsset> buildMediaAssets(
        List<TechnicalWorkflowRecords.CaseMediaAsset> mediaAssets
    ) {
        return mediaAssets.stream().map(this::toMediaAsset).toList();
    }

    private TechnicalWorkflowModels.TechnicalSpecimenRegistrationMediaAsset toMediaAsset(
        TechnicalWorkflowRecords.CaseMediaAsset asset
    ) {
        return new TechnicalWorkflowModels.TechnicalSpecimenRegistrationMediaAsset(
            asset.id(),
            asset.fileName(),
            asset.fileUrl(),
            stringify(asset.capturedAt()));
    }

    private List<TechnicalWorkflowModels.TechnicalSpecimenRegistrationCheckItem> buildCheckItems(
        String checkItemSource,
        String caseId
    ) {
        List<String> values = splitItems(checkItemSource);
        if (values.isEmpty()) {
            values = medicalOrderRepository.findMedicalOrdersByCaseId(caseId).stream()
                .map(MedicalOrderRepository.MedicalOrder::orderContent)
                .filter(value -> value != null && !value.isBlank())
                .toList();
        }
        List<TechnicalWorkflowModels.TechnicalSpecimenRegistrationCheckItem> result = new ArrayList<>();
        int sequenceNo = 0;
        for (String value : values) {
            result.add(new TechnicalWorkflowModels.TechnicalSpecimenRegistrationCheckItem(++sequenceNo, value));
        }
        return result;
    }

    private List<String> splitItems(String source) {
        if (source == null || source.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(source.split("[,，;；\\n\\r]+"))
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .distinct()
            .toList();
    }

    private Specimen createRegistrationSpecimen(
        WorkspaceContext context,
        MaterialPayload payload,
        TechnicalWorkflowModels.SaveTechnicalSpecimenRegistrationMaterialsCommand command
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime receivedAt = context.pathologyCase().receivedAt() == null ? now : context.pathologyCase().receivedAt();
        String specimenNo = numberingService.generateSpecimenNo(null);
        String barcode = context.application().getApplicationNo() + "-" + specimenNo;
        Specimen specimen = new Specimen(
            "SP-" + UUID.randomUUID(),
            context.application().getId().value(),
            context.pathologyCase().id(),
            specimenNo,
            barcode,
            payload.specimenType(),
            payload.specimenName(),
            payload.sourcePart(),
            DEFAULT_COLLECTION_MODE,
            payload.tissueCount(),
            payload.specimenSize(),
            payload.frozen(),
            serializeEvaluationItems(payload.evaluationItems()),
            DEFAULT_CONTAINER_NAME,
            1,
            SpecimenStatus.RECEIVED,
            FixationStatus.COMPLETED,
            "VERIFIED",
            receivedAt,
            receivedAt,
            command.operatorUserId(),
            command.operatorName(),
            context.application().getSpecimenRemovalTime(),
            null,
            null,
            null,
            "RECEIVED",
            receivedAt,
            context.pathologyCase().receivedByName(),
            true,
            null,
            ReceiptStatus.RECEIVED.name(),
            DEFAULT_QUALITY_CHECK_RESULT,
            null,
            context.application().getClinicalSymptom(),
            context.application().getSubmittingDepartmentId(),
            context.application().getSubmittingDepartmentName(),
            context.application().getSubmittingDoctorUserId(),
            context.application().getSubmittingDoctorName(),
            context.application().getSubmissionDate(),
            null,
            "SUCCESS",
            command.operatorUserId(),
            command.operatorName(),
            now,
            command.terminalCode(),
            "Created during technical specimen registration");
        specimenWorkflowCommandRepository.insertSpecimen(specimen);
        specimenWorkflowCommandRepository.insertCollectionRecord(
            context.application().getId().value(),
            specimen.id(),
            "COLLECTED",
            DEFAULT_COLLECTION_SCENE,
            specimen.collectionMode(),
            null,
            null,
            command.operatorUserId(),
            command.operatorName(),
            now,
            command.terminalCode(),
            "Created during technical specimen registration");
        specimenWorkflowCommandRepository.upsertFixationRecord(
            context.application().getId().value(),
            specimen.id(),
            FixationStatus.COMPLETED,
            DEFAULT_FIXATIVE_TYPE,
            receivedAt,
            receivedAt,
            command.operatorUserId(),
            command.operatorName(),
            receivedAt,
            command.terminalCode(),
            "Created during technical specimen registration");
        specimenWorkflowCommandRepository.insertSpecimenReceipt(
            context.application().getId().value(),
            context.pathologyCase().id(),
            specimen.id(),
            null,
            ReceiptStatus.RECEIVED,
            1,
            DEFAULT_QUALITY_CHECK_RESULT,
            null,
            barcode,
            command.operatorUserId(),
            command.operatorName(),
            null,
            now,
            command.terminalCode(),
            null,
            null,
            "Created during technical specimen registration");
        specimenWorkflowCommandRepository.insertWorkflowEvent(new TrackingEvent(
            "EVT-" + UUID.randomUUID(),
            context.application().getId().value(),
            specimen.id(),
            context.pathologyCase().id(),
            null,
            TechnicalWorkflowConstants.NODE_SPECIMEN_REGISTRATION,
            "CREATE_MATERIAL",
            "SUCCESS",
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Created specimen during technical specimen registration", null));
        return specimen;
    }

    private void removeRegistrationSpecimen(
        PathologyCase pathologyCase,
        Specimen specimen,
        TechnicalWorkflowModels.SaveTechnicalSpecimenRegistrationMaterialsCommand command
    ) {
        specimenWorkflowCommandRepository.updateSpecimenStatus(
            specimen.id(),
            SpecimenStatus.RETURNED,
            specimen.fixationStatus() == null ? FixationStatus.COMPLETED : specimen.fixationStatus(),
            "Removed during technical specimen registration",
            "Removed during technical specimen registration",
            pathologyCase.id());
        specimenWorkflowCommandRepository.insertWorkflowEvent(new TrackingEvent(
            "EVT-" + UUID.randomUUID(),
            specimen.applicationId(),
            specimen.id(),
            pathologyCase.id(),
            null,
            TechnicalWorkflowConstants.NODE_SPECIMEN_REGISTRATION,
            "REMOVE_MATERIAL",
            "SUCCESS",
            LocalDateTime.now(),
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Removed specimen during technical specimen registration", null));
    }

    private Specimen requireMaterialSpecimen(WorkspaceContext context, String specimenId) {
        String normalizedSpecimenId = trimToNull(specimenId);
        if (normalizedSpecimenId == null) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Specimen ID is required");
        }
        return context.specimens().stream()
            .filter(specimen -> normalizedSpecimenId.equals(specimen.id()))
            .findFirst()
            .orElseThrow(() -> new BlBusinessException(
                BlErrorCode.RESOURCE_NOT_FOUND,
                404,
                "Specimen not found in technical specimen registration workspace"));
    }

    private MaterialPayload normalizeMaterial(TechnicalWorkflowModels.TechnicalSpecimenRegistrationMaterialInput input) {
        String specimenType = trimToNull(input.specimenType());
        if (specimenType == null) {
            specimenType = DEFAULT_SPECIMEN_TYPE;
        }
        String specimenName = technicalWorkflowSupport.requireText(input.specimenName(), "Specimen name is required");
        String sourcePart = trimToNull(input.sourcePart());
        int tissueCount = input.tissueCount() == null || input.tissueCount() < 1
            ? DEFAULT_TISSUE_COUNT
            : input.tissueCount();
        String specimenSize = trimToNull(input.specimenSize());
        if (specimenSize == null) {
            specimenSize = DEFAULT_SPECIMEN_SIZE;
        }
        return new MaterialPayload(
            trimToNull(input.specimenId()),
            specimenType,
            specimenName,
            sourcePart,
            tissueCount,
            specimenSize,
            Boolean.TRUE.equals(input.frozen()),
            normalizeEvaluationItems(input.evaluationItems()));
    }

    private List<String> normalizeEvaluationItems(List<String> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
            .map(this::trimToNull)
            .filter(item -> item != null)
            .distinct()
            .toList();
    }

    private String serializeEvaluationItems(List<String> items) {
        List<String> normalizedItems = normalizeEvaluationItems(items);
        return normalizedItems.isEmpty() ? null : String.join("\n", normalizedItems);
    }

    private List<String> deserializeEvaluationItems(String value) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            return List.of();
        }
        return normalizeEvaluationItems(java.util.Arrays.asList(normalizedValue.split("\\R")));
    }

    private String resolveSpecimenRemovalTime(WorkspaceContext context) {
        LocalDateTime removalTime = context.application().getSpecimenRemovalTime();
        if (removalTime == null) {
            removalTime = context.specimens().stream()
                .map(Specimen::specimenRemovalAt)
                .filter(item -> item != null)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        }
        return stringify(removalTime);
    }

    private String buildInfectiousSummary(ApplicationRegistrationWorkbenchRepository.WorkbenchExtensionData extension) {
        if (extension == null) {
            return null;
        }
        List<String> sections = new ArrayList<>();
        List<String> contagiousFlags = new ArrayList<>();
        if (extension.contagiousIsolation()) {
            contagiousFlags.add("隔离");
        }
        if (extension.contagiousHiv()) {
            contagiousFlags.add("HIV");
        }
        if (extension.contagiousTuberculosis()) {
            contagiousFlags.add("结核");
        }
        if (extension.contagiousHepatitis()) {
            contagiousFlags.add("肝炎");
        }
        if (extension.contagiousSyphilis()) {
            contagiousFlags.add("梅毒");
        }
        if (!contagiousFlags.isEmpty()) {
            sections.add("传染信息: " + String.join("、", contagiousFlags));
        }
        appendSection(sections, labeledValue("HPV", extension.hpvResult()));
        appendSection(sections, labeledValue("既往细胞学", extension.previousCytology()));
        appendSection(sections, labeledValue("既往治疗", extension.previousTreatment()));
        appendSection(sections, labeledValue("补充说明", extension.additionalNotes()));
        return sections.isEmpty() ? null : String.join("；", sections);
    }

    private String joinLabeledSections(String... values) {
        List<String> sections = new ArrayList<>();
        for (String value : values) {
            appendSection(sections, value);
        }
        return sections.isEmpty() ? null : String.join("\n", sections);
    }

    private void appendSection(List<String> sections, String value) {
        if (value != null && !value.isBlank()) {
            sections.add(value);
        }
    }

    private String labeledValue(String label, String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return label + ": " + normalized;
    }

    private String overrideOrFallback(String overrideValue, String fallbackValue) {
        String normalizedOverride = normalizeSectionValue(overrideValue);
        return normalizedOverride != null ? normalizedOverride : trimToNull(fallbackValue);
    }

    private String normalizeSectionValue(String value) {
        return trimToNull(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeApplicationType(String selectedApplicationType, String fallbackApplicationType) {
        String normalizedSelectedApplicationType = trimToNull(selectedApplicationType);
        if (normalizedSelectedApplicationType != null) {
            return normalizedSelectedApplicationType;
        }
        String normalizedFallbackApplicationType = trimToNull(fallbackApplicationType);
        return normalizedFallbackApplicationType == null ? "ROUTINE" : normalizedFallbackApplicationType;
    }

    private String resolveCompletionPathologyNo(
        String caseId,
        String applicationType,
        String existingPathologyNo,
        String candidatePathologyNo
    ) {
        String normalizedCandidatePathologyNo = trimToNull(candidatePathologyNo);
        if (normalizedCandidatePathologyNo != null) {
            validateCandidatePathologyNo(caseId, applicationType, normalizedCandidatePathologyNo);
            specimenWorkflowCommandRepository.updatePathologyCasePathologyNo(caseId, normalizedCandidatePathologyNo);
            return normalizedCandidatePathologyNo;
        }

        String pathologyNo = trimToNull(existingPathologyNo);
        if (pathologyNo == null) {
            pathologyNo = numberingService.generatePathologyNo(applicationType);
            specimenWorkflowCommandRepository.updatePathologyCasePathologyNo(caseId, pathologyNo);
        }
        return pathologyNo;
    }

    private void validateCandidatePathologyNo(
        String caseId,
        String applicationType,
        String pathologyNo
    ) {
        numberingService.validateAndAcceptPathologyNo(caseId, applicationType, pathologyNo);
        technicalWorkflowRepository.findPathologyCaseByPathologyNo(pathologyNo)
            .filter(existingCase -> !caseId.equals(existingCase.id()))
            .ifPresent(existingCase -> {
                throw new BlBusinessException(
                    BlErrorCode.RESOURCE_CONFLICT,
                    409,
                    "Pathology number already exists");
            });
    }

    private String normalizeListRegistrationStatus(String registrationStatus) {
        String normalizedRegistrationStatus = trimToNull(registrationStatus);
        if (normalizedRegistrationStatus == null) {
            return REGISTRATION_STATUS_PENDING;
        }
        normalizedRegistrationStatus = normalizedRegistrationStatus.toUpperCase();
        if (!LIST_REGISTRATION_STATUSES.contains(normalizedRegistrationStatus)) {
            throw new BlBusinessException(
                BlErrorCode.INVALID_ARGUMENT,
                400,
                "Unsupported technical specimen registration status"
            );
        }
        return normalizedRegistrationStatus;
    }

    private boolean containsIgnoreCase(String source, String fragment) {
        if (source == null || fragment == null) {
            return false;
        }
        return source.toLowerCase(java.util.Locale.ROOT).contains(fragment.toLowerCase(java.util.Locale.ROOT));
    }

    private <T> T valueOf(java.util.function.Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (NullPointerException ignored) {
            return null;
        }
    }

    private String stringify(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }

    private record WorkspaceContext(
        TechnicalWorkflowRecords.TechnicalSpecimenRegistration registration,
        PathologyCase pathologyCase,
        Application application,
        ApplicationRegistrationWorkbenchRepository.WorkbenchExtensionData extension,
        ApplicationRegistrationWorkbenchRepository.TechnicalRegistrationDetailSectionOverrides detailSectionOverrides,
        List<Specimen> specimens,
        List<TechnicalWorkflowRecords.CaseMediaAsset> mediaAssets
    ) {
    }

    private record MaterialPayload(
        String specimenId,
        String specimenType,
        String specimenName,
        String sourcePart,
        int tissueCount,
        String specimenSize,
        boolean frozen,
        List<String> evaluationItems
    ) {
    }
}
