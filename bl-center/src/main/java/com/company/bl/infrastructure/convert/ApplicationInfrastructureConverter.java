package com.company.bl.infrastructure.convert;

import com.company.bl.domain.enums.ApplicationFormStatus;
import com.company.bl.domain.enums.ApplicationStatus;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.valueobject.ApplicationId;
import com.company.bl.infrastructure.persistence.ApplicationDataObject;
import org.springframework.stereotype.Component;

@Component
public class ApplicationInfrastructureConverter {

    public ApplicationDataObject toDataObject(Application application) {
        ApplicationDataObject dataObject = new ApplicationDataObject();
        dataObject.setId(application.getId().value());
        dataObject.setApplicationNo(application.getApplicationNo());
        dataObject.setPatientId(application.getPatientId());
        dataObject.setApplicationType(application.getApplicationType());
        dataObject.setStatus(application.getStatus().name());
        dataObject.setApplicationFormStatus(application.getApplicationFormStatus().name());
        dataObject.setExternalOrderNo(application.getExternalOrderNo());
        dataObject.setThirdPartySource(application.getThirdPartySource());
        dataObject.setClinicalDiagnosis(application.getClinicalDiagnosis());
        dataObject.setClinicalSymptom(application.getClinicalSymptom());
        dataObject.setSpecimenSite(application.getSpecimenSite());
        dataObject.setApplicationDate(application.getApplicationDate());
        dataObject.setSubmissionDate(application.getSubmissionDate());
        dataObject.setRemarks(application.getRemarks());
        dataObject.setCreatedAt(application.getCreatedAt());
        dataObject.setUpdatedAt(application.getUpdatedAt());
        return dataObject;
    }

    public Application toDomain(ApplicationDataObject dataObject) {
        return new Application(
            new ApplicationId(dataObject.getId()),
            dataObject.getApplicationNo(),
            dataObject.getPatientId(),
            dataObject.getApplicationType(),
            ApplicationStatus.from(dataObject.getStatus()),
            ApplicationFormStatus.from(dataObject.getApplicationFormStatus()),
            dataObject.getExternalOrderNo(),
            dataObject.getThirdPartySource(),
            dataObject.getClinicalDiagnosis(),
            dataObject.getClinicalSymptom(),
            dataObject.getSpecimenSite(),
            dataObject.getApplicationDate(),
            dataObject.getSubmissionDate(),
            dataObject.getRemarks(),
            dataObject.getCreatedAt(),
            dataObject.getUpdatedAt());
    }
}
