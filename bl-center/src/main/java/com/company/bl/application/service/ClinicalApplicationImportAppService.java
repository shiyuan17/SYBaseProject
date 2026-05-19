package com.company.bl.application.service;

import com.company.bl.application.command.CreateApplicationCommand;
import com.company.bl.application.gateway.ClinicalApplicationGateway;
import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import com.company.bl.domain.valueobject.ApplicationId;
import com.company.bl.support.application.NumberingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClinicalApplicationImportAppService {

    private final ClinicalApplicationGateway clinicalApplicationGateway;
    private final SpecimenWorkflowRepository specimenWorkflowRepository;
    private final CreateApplicationAppService createApplicationAppService;
    private final NumberingService numberingService;

    @Transactional
    public ApplicationId importApplication(ImportClinicalApplicationCommand command) {
        if (specimenWorkflowRepository.existsApplicationByExternalSource(command.externalOrderNo(), command.thirdPartySource())) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Imported application already exists");
        }
        ClinicalApplicationGateway.ImportedClinicalApplication imported =
            clinicalApplicationGateway.fetch(command.thirdPartySource(), command.externalOrderNo());
        return createApplicationAppService.create(new CreateApplicationCommand(
            numberingService.generateApplicationNo(),
            imported.patientId(),
            imported.patientName(),
            imported.patientGender(),
            imported.patientAge(),
            imported.applicationType(),
            "DRAFT",
            imported.externalOrderNo(),
            imported.thirdPartySource(),
            imported.sourceHospitalId(),
            imported.sourceHospitalName(),
            imported.submittingDepartmentId(),
            imported.submittingDepartmentName(),
            imported.submittingDoctorUserId(),
            imported.submittingDoctorName(),
            imported.clinicalDiagnosis(),
            imported.clinicalSymptom(),
            imported.specimenSite(),
            null,
            null,
            "Imported from placeholder gateway"));
    }

    public record ImportClinicalApplicationCommand(String thirdPartySource, String externalOrderNo) {
    }
}
