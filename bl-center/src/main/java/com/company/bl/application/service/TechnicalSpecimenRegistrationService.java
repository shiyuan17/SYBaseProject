package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.repository.ApplicationRegistrationWorkbenchRepository;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.MedicalOrderRepository;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import com.company.bl.domain.valueobject.ApplicationId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
class TechnicalSpecimenRegistrationService {

    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final TechnicalWorkflowSupport technicalWorkflowSupport;
    private final ApplicationRepository applicationRepository;
    private final ApplicationRegistrationWorkbenchRepository workbenchRepository;
    private final MedicalOrderRepository medicalOrderRepository;

    TechnicalSpecimenRegistrationService(TechnicalWorkflowRepository technicalWorkflowRepository,
                                         TechnicalWorkflowSupport technicalWorkflowSupport,
                                         ApplicationRepository applicationRepository,
                                         ApplicationRegistrationWorkbenchRepository workbenchRepository,
                                         MedicalOrderRepository medicalOrderRepository) {
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.technicalWorkflowSupport = technicalWorkflowSupport;
        this.applicationRepository = applicationRepository;
        this.workbenchRepository = workbenchRepository;
        this.medicalOrderRepository = medicalOrderRepository;
    }

    @Transactional(readOnly = true)
    TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationPage listPendingRegistrations(
        TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationQuery query
    ) {
        TechnicalWorkflowRecords.PagedTechnicalSpecimenRegistrations paged =
            technicalWorkflowRepository.findPendingTechnicalSpecimenRegistrations(
                new TechnicalWorkflowRecords.PendingTechnicalSpecimenRegistrationQuery(
                    query.page(),
                    query.size(),
                    query.keyword()));
        return new TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationPage(
            paged.items().stream().map(item -> new TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationItem(
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
                stringify(item.receivedAt()),
                stringify(item.registeredAt()))).toList(),
            query.page(),
            query.size(),
            paged.total());
    }

    @Transactional(readOnly = true)
    TechnicalWorkflowModels.TechnicalSpecimenRegistrationDetail getRegistrationDetail(String caseId) {
        TechnicalWorkflowRecords.TechnicalSpecimenRegistration registration = getRegistration(caseId);
        PathologyCase pathologyCase = technicalWorkflowSupport.getCase(caseId);
        Application application = applicationRepository.findById(new ApplicationId(pathologyCase.applicationId()))
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Application not found"));
        List<Specimen> specimens = technicalWorkflowRepository.findSpecimensByCaseId(caseId);
        ApplicationRegistrationWorkbenchRepository.WorkbenchExtensionData extension =
            workbenchRepository.findExtensionByApplicationId(application.getId().value()).orElse(null);
        String checkItemSource = extension == null ? null : extension.checkItem();
        List<TechnicalWorkflowModels.TechnicalSpecimenRegistrationMaterial> materials = new ArrayList<>();
        int sequenceNo = 0;
        for (Specimen specimen : specimens) {
            materials.add(new TechnicalWorkflowModels.TechnicalSpecimenRegistrationMaterial(
                ++sequenceNo,
                specimen.specimenType(),
                specimen.specimenNameStandardized(),
                specimen.specimenSite()));
        }
        List<TechnicalWorkflowModels.TechnicalSpecimenRegistrationCheckItem> checkItems =
            buildCheckItems(checkItemSource, caseId);
        return new TechnicalWorkflowModels.TechnicalSpecimenRegistrationDetail(
            registration.caseId(),
            registration.applicationId(),
            registration.applicationNo(),
            registration.pathologyNo(),
            registration.patientName(),
            registration.patientId(),
            registration.inpatientNo(),
            registration.applicationType(),
            registration.submittingDepartmentName(),
            application.getClinicalDiagnosis(),
            registration.registrationStatus(),
            registration.registeredByName(),
            stringify(registration.registeredAt()),
            registration.registrationRemarks(),
            stringify(registration.receivedAt()),
            materials,
            checkItems);
    }

    @Transactional
    TechnicalWorkflowModels.TechnicalSpecimenRegistrationCompleteResult completeRegistration(
        TechnicalWorkflowModels.CompleteTechnicalSpecimenRegistrationCommand command
    ) {
        TechnicalWorkflowRecords.TechnicalSpecimenRegistration registration = getRegistration(command.caseId());
        PathologyCase pathologyCase = technicalWorkflowSupport.getCase(command.caseId());
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
                "pathologyNo=" + pathologyCase.pathologyNo() + ";receivedCount=" + specimens.size() + ";processedCount=" + specimens.size());
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
            pathologyCase.pathologyNo(),
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

    private String stringify(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
