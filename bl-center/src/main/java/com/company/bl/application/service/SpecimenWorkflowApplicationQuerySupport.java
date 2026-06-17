package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.ApplicationDomainException;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.company.bl.application.service.SpecimenWorkflowQueryModels.*;

@Component
class SpecimenWorkflowApplicationQuerySupport extends AbstractSpecimenWorkflowQuerySupport {

    private final ApplicationPatientIdentityResolver patientIdentityResolver;

    SpecimenWorkflowApplicationQuerySupport(ApplicationRepository applicationRepository,
                                            SpecimenWorkflowQueryRepository specimenWorkflowRepository,
                                            SpecimenWorkflowSupport specimenWorkflowSupport,
                                            ApplicationPatientIdentityResolver patientIdentityResolver) {
        super(applicationRepository, specimenWorkflowRepository, specimenWorkflowSupport);
        this.patientIdentityResolver = patientIdentityResolver;
    }

    @Transactional(readOnly = true)
    ApplicationPage listApplications(ApplicationListQuery query) {
        int page = specimenWorkflowSupport.normalizePage(query.page());
        int size = specimenWorkflowSupport.normalizeSize(query.size());
        SpecimenWorkflowRepository.PagedApplications result =
            specimenWorkflowRepository.findApplications(
                new SpecimenWorkflowRepository.ApplicationListQuery(
                    page,
                    size,
                    specimenWorkflowSupport.trim(query.applicationNo()),
                    specimenWorkflowSupport.trim(query.pathologyNo()),
                    specimenWorkflowSupport.trim(query.patientName()),
                    specimenWorkflowSupport.trim(query.submittingDepartmentId()),
                    specimenWorkflowSupport.normalizeStatus(query.applicationType()),
                    specimenWorkflowSupport.normalizeStatus(query.applicationFormStatus()),
                    specimenWorkflowSupport.parseLocalDateFrom(query.dateFrom()),
                    specimenWorkflowSupport.parseLocalDateTo(query.dateTo())));
        return new ApplicationPage(
            result.items().stream().map(item -> new ApplicationListItem(
                item.id(),
                item.applicationNo(),
                item.pathologyNo(),
                item.patientName(),
                item.patientGender(),
                item.patientAge(),
                item.status(),
                item.submittingDepartmentName(),
                item.submittingDoctorName(),
                item.applicationType(),
                item.applicationFormStatus(),
                item.currentNode(),
                item.abnormalFlag(),
                item.registeredSpecimenCount(),
                item.latestLabelPrintStatus(),
                item.editable(),
                item.deletable(),
                item.voided(),
                item.operationDisabledReason(),
                item.applicationDate(),
                item.submissionDate(),
                item.createdAt(),
                item.updatedAt()))
                .toList(),
            page,
            size,
            result.total());
    }

    @Transactional(readOnly = true)
    DuplicateCheckResult checkApplicationDuplicate(DuplicateCheckCommand command) {
        String patientId = patientIdentityResolver.resolveExistingOrOriginal(command.patientId());
        String patientName = specimenWorkflowSupport.trim(command.patientName());
        String externalOrderNo = specimenWorkflowSupport.trim(command.externalOrderNo());
        LocalDate applicationDate = specimenWorkflowSupport.parseLocalDate(command.applicationDate());
        String applicationType = specimenWorkflowSupport.normalizeStatus(command.applicationType());
        String specimenSite = specimenWorkflowSupport.trim(command.specimenSite());
        if (patientId == null && patientName == null) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Patient id or patient name is required");
        }
        if (externalOrderNo == null && applicationDate == null) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "External order number or application date is required");
        }
        List<DuplicateCheckItem> items = specimenWorkflowRepository.findDuplicateApplications(
                new SpecimenWorkflowRepository.DuplicateApplicationQuery(
                    patientId,
                    patientName,
                    externalOrderNo,
                    applicationDate,
                    applicationType,
                    specimenSite))
            .stream()
            .map(item -> new DuplicateCheckItem(
                item.id(),
                item.applicationNo(),
                item.patientName(),
                item.applicationDate(),
                item.specimenSite(),
                item.status(),
                item.currentNode(),
                resolveMatchedBy(item.externalOrderMatched(), item.sameDaySiteMatched())))
            .toList();
        String suggestedAction = items.stream().anyMatch(item -> item.matchedBy().contains("EXTERNAL_ORDER_NO"))
            ? "BLOCK"
            : items.isEmpty() ? "ALLOW" : "CONFIRM";
        return new DuplicateCheckResult(items, suggestedAction);
    }

    @Transactional(readOnly = true)
    ApplicationListItem getRegistrationApplicationByApplicationNo(String applicationNo) {
        if (specimenWorkflowSupport.blank(applicationNo)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Application number is required");
        }
        return applicationRepository.findByApplicationNo(applicationNo.trim())
            .map(application -> specimenWorkflowSupport.toApplicationListItem(specimenWorkflowRepository.getApplicationTracking(
                application.getId().value(),
                application)))
            .orElseThrow(() -> new ApplicationDomainException(com.company.bl.domain.enums.ApplicationErrorCode.APPLICATION_NOT_FOUND, 404));
    }

    ApplicationOperationState resolveApplicationOperationState(Application application) {
        return specimenWorkflowSupport.resolveApplicationOperationState(application);
    }

    private List<String> resolveMatchedBy(boolean externalOrderMatched, boolean sameDaySiteMatched) {
        List<String> matchedBy = new ArrayList<>();
        if (externalOrderMatched) {
            matchedBy.add("EXTERNAL_ORDER_NO");
        }
        if (sameDaySiteMatched) {
            matchedBy.add("SAME_DAY_SAME_SITE");
        }
        return List.copyOf(matchedBy);
    }
}
