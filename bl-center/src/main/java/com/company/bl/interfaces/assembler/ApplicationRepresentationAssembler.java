package com.company.bl.interfaces.assembler;

import com.company.bl.application.command.CreateApplicationCommand;
import com.company.bl.application.command.UpdateApplicationCommand;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.valueobject.ApplicationId;
import com.company.bl.interfaces.dto.CreateApplicationRequest;
import com.company.bl.interfaces.dto.UpdateApplicationRequest;
import com.company.bl.interfaces.vo.ApplicationDetailResponse;
import com.company.bl.interfaces.vo.ApplicationIdResponse;
import com.company.bl.interfaces.vo.SpecimenSummaryResponse;
import com.company.bl.interfaces.vo.TrackingEventResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApplicationRepresentationAssembler {

    public CreateApplicationCommand toCommand(CreateApplicationRequest request) {
        return new CreateApplicationCommand(
            request.getApplicationNo(),
            request.getPatientId(),
            request.getPatientName(),
            request.getPatientGender(),
            request.getPatientAge(),
            request.getApplicationType(),
            request.getStatus(),
            request.getExternalOrderNo(),
            request.getThirdPartySource(),
            request.getSourceHospitalId(),
            request.getSourceHospitalName(),
            request.getSubmittingDepartmentId(),
            request.getSubmittingDepartmentName(),
            request.getSubmittingDoctorUserId(),
            request.getSubmittingDoctorName(),
            request.getClinicalDiagnosis(),
            request.getClinicalSymptom(),
            request.getSpecimenSite(),
            request.getApplicationDate(),
            request.getSubmissionDate(),
            request.getSpecimenRemovalTime(),
            request.getApplicationFormStatus(),
            request.getRemarks());
    }

    public UpdateApplicationCommand toCommand(UpdateApplicationRequest request) {
        return new UpdateApplicationCommand(
            request.getApplicationNo(),
            request.getPatientId(),
            request.getPatientName(),
            request.getPatientGender(),
            request.getPatientAge(),
            request.getApplicationType(),
            request.getExternalOrderNo(),
            request.getThirdPartySource(),
            request.getSourceHospitalId(),
            request.getSourceHospitalName(),
            request.getSubmittingDepartmentId(),
            request.getSubmittingDepartmentName(),
            request.getSubmittingDoctorUserId(),
            request.getSubmittingDoctorName(),
            request.getClinicalDiagnosis(),
            request.getClinicalSymptom(),
            request.getSpecimenSite(),
            request.getApplicationDate(),
            request.getSubmissionDate(),
            request.getSpecimenRemovalTime(),
            request.getApplicationFormStatus(),
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
            application.getPatientId(),
            application.getPatientId() == null || application.getPatientId().isBlank() ? null : "PENDING",
            application.getPatientName(),
            application.getPatientGender(),
            application.getPatientAge(),
            application.getApplicationType(),
            application.getStatus().name(),
            application.getApplicationFormStatus().name(),
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
            stringify(application.getApplicationDate()),
            stringify(application.getSubmissionDate()),
            stringify(application.getSpecimenRemovalTime()),
            null,
            null,
            application.getStatus().name(),
            false,
            false,
            false,
            application.getStatus().name().equals("VOIDED"),
            application.getStatus().name().equals("VOIDED") ? "申请单已作废，不能再编辑或作废" : null,
            null,
            false,
            null,
            0,
            List.<SpecimenSummaryResponse>of(),
            List.<TrackingEventResponse>of(),
            application.getRemarks(),
            stringify(application.getCreatedAt()),
            stringify(application.getUpdatedAt()));
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }
}
