package com.company.bl.domain.repository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ApplicationRegistrationWorkbenchRepository {

    Optional<WorkbenchApplicationRow> findApplicationByKeyword(String keyword);

    Optional<WorkbenchApplicationRow> findApplicationByKeyword(String keyword, String queryType);

    Optional<WorkbenchExtensionData> findExtensionByApplicationId(String applicationId);

    void upsertExtension(SaveWorkbenchExtensionCommand command);

    void updateApplicationEditableFields(String applicationId,
                                         String clinicalDiagnosis,
                                         String remarks);

    boolean hasStartedDownstreamWorkflow(String applicationId);

    void clearPreDownstreamRegistrationData(String applicationId);

    record WorkbenchApplicationRow(
        String applicationId,
        String applicationNo,
        String patientId,
        String patientName,
        String patientGender,
        String patientAge,
        String submittingDepartmentName,
        String submittingDoctorName,
        String clinicalDiagnosis,
        String remarks,
        String status,
        java.time.LocalDate applicationDate,
        java.time.LocalDate submissionDate
    ) {
    }

    record WorkbenchExtensionData(
        String inpatientNo,
        String bedNo,
        String wardName,
        String phone,
        String idNo,
        String checkItem,
        String clinicalHistory,
        String imagingResult,
        String endoscopyDiagnosis,
        String deliveryRequirement,
        String specimenType,
        String surgeryName,
        String clinicalFindings,
        String fixativeType,
        String fixationPerson,
        LocalDateTime fixationTime,
        String buildingId,
        String roomId,
        boolean contagiousIsolation,
        boolean contagiousHiv,
        boolean contagiousTuberculosis,
        boolean contagiousHepatitis,
        boolean contagiousSyphilis,
        boolean gynecologyMenopause,
        String lastMenstrualPeriod,
        String hpvResult,
        String previousCytology,
        String previousTreatment,
        String additionalNotes,
        boolean conditionAbnormalBleeding,
        boolean conditionBirthControl,
        boolean conditionHormoneReplacement,
        boolean conditionHysterectomy,
        boolean conditionIud,
        boolean conditionLactation,
        boolean conditionPregnancy,
        boolean conditionRadiotherapy,
        String otherSpecialCondition
    ) {
    }

    record SaveWorkbenchExtensionCommand(
        String applicationId,
        String inpatientNo,
        String bedNo,
        String wardName,
        String phone,
        String idNo,
        String checkItem,
        String clinicalHistory,
        String imagingResult,
        String endoscopyDiagnosis,
        String deliveryRequirement,
        String specimenType,
        String surgeryName,
        String clinicalFindings,
        String fixativeType,
        String fixationPerson,
        LocalDateTime fixationTime,
        String buildingId,
        String roomId,
        boolean contagiousIsolation,
        boolean contagiousHiv,
        boolean contagiousTuberculosis,
        boolean contagiousHepatitis,
        boolean contagiousSyphilis,
        boolean gynecologyMenopause,
        String lastMenstrualPeriod,
        String hpvResult,
        String previousCytology,
        String previousTreatment,
        String additionalNotes,
        boolean conditionAbnormalBleeding,
        boolean conditionBirthControl,
        boolean conditionHormoneReplacement,
        boolean conditionHysterectomy,
        boolean conditionIud,
        boolean conditionLactation,
        boolean conditionPregnancy,
        boolean conditionRadiotherapy,
        String otherSpecialCondition
    ) {
    }
}
