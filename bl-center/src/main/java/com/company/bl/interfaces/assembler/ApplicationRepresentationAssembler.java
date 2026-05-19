package com.company.bl.interfaces.assembler;

import com.company.bl.application.command.CreateApplicationCommand;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.valueobject.ApplicationId;
import com.company.bl.interfaces.dto.CreateApplicationRequest;
import com.company.bl.interfaces.vo.ApplicationDetailResponse;
import com.company.bl.interfaces.vo.ApplicationIdResponse;
import org.springframework.stereotype.Component;

@Component
public class ApplicationRepresentationAssembler {

    public CreateApplicationCommand toCommand(CreateApplicationRequest request) {
        return new CreateApplicationCommand(
            request.getApplicationNo(),
            request.getPatientId(),
            request.getApplicationType(),
            request.getStatus(),
            request.getExternalOrderNo(),
            request.getThirdPartySource(),
            request.getClinicalDiagnosis(),
            request.getClinicalSymptom(),
            request.getSpecimenSite(),
            request.getApplicationDate(),
            request.getSubmissionDate(),
            request.getRemarks());
    }

    public ApplicationIdResponse toIdResponse(ApplicationId applicationId) {
        return new ApplicationIdResponse(applicationId.value());
    }

    public ApplicationDetailResponse toDetailResponse(Application application) {
        return new ApplicationDetailResponse(
            application.getId().value(),
            application.getApplicationNo(),
            application.getPatientId(),
            application.getApplicationType(),
            application.getStatus().name(),
            application.getApplicationFormStatus().name(),
            application.getExternalOrderNo(),
            application.getThirdPartySource(),
            application.getClinicalDiagnosis(),
            application.getClinicalSymptom(),
            application.getSpecimenSite(),
            stringify(application.getApplicationDate()),
            stringify(application.getSubmissionDate()),
            application.getRemarks(),
            stringify(application.getCreatedAt()),
            stringify(application.getUpdatedAt()));
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }
}
