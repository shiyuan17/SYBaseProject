package com.company.bl.application.gateway;

public interface ClinicalApplicationGateway {

    ImportedClinicalApplication fetch(String thirdPartySource, String externalOrderNo);

    record ImportedClinicalApplication(
        String externalOrderNo,
        String thirdPartySource,
        String patientId,
        String patientName,
        String patientGender,
        String patientAge,
        String sourceHospitalId,
        String sourceHospitalName,
        String submittingDepartmentId,
        String submittingDepartmentName,
        String submittingDoctorUserId,
        String submittingDoctorName,
        String clinicalDiagnosis,
        String clinicalSymptom,
        String specimenSite,
        String applicationType
    ) {
    }
}
