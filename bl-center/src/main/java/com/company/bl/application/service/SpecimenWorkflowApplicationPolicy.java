package com.company.bl.application.service;

import com.company.bl.domain.enums.ApplicationStatus;
import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.repository.ApplicationRegistrationWorkbenchRepository;

import static com.company.bl.application.service.SpecimenWorkflowModels.ApplicationOperationState;

class SpecimenWorkflowApplicationPolicy {

    private final ApplicationRegistrationWorkbenchRepository workbenchRepository;

    SpecimenWorkflowApplicationPolicy(ApplicationRegistrationWorkbenchRepository workbenchRepository) {
        this.workbenchRepository = workbenchRepository;
    }

    void validateApplicationCanRegister(Application application) {
        ApplicationStatus status = application.getStatus();
        if (status == ApplicationStatus.DRAFT || status == ApplicationStatus.SUBMITTED) {
            return;
        }
        throw new BlBusinessException(
            BlErrorCode.OPERATION_NOT_ALLOWED,
            409,
            "Application status does not allow specimen registration: " + status.name());
    }

    ApplicationOperationState resolveApplicationOperationState(Application application) {
        if (application.getStatus() == ApplicationStatus.VOIDED) {
            return new ApplicationOperationState(false, false, true, "申请单已作废，不能再编辑或作废");
        }
        if (workbenchRepository.hasStartedDownstreamWorkflow(application.getId().value())) {
            return new ApplicationOperationState(false, false, false, "申请单已进入下游流程，不能再编辑或作废");
        }
        return new ApplicationOperationState(true, true, false, null);
    }
}
