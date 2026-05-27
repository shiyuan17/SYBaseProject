package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ApplicationRegistrationWorkbenchResponse", description = "申请登记工作台记录")
public record ApplicationRegistrationWorkbenchResponse(
    @Schema(description = "申请单 ID")
    String applicationId,
    ContagiousSpecimenResponse contagiousSpecimen,
    GynecologyInfoResponse gynecologyInfo,
    PatientInfoResponse patientInfo,
    List<SpecimenItemResponse> specimenItems,
    SurgeryInfoResponse surgeryInfo
) {

    public record ContagiousSpecimenResponse(
        boolean hepatitis,
        boolean hiv,
        boolean isolation,
        boolean syphilis,
        boolean tuberculosis
    ) {
    }

    public record SpecialConditionsResponse(
        boolean abnormalBleeding,
        boolean birthControl,
        boolean hormoneReplacement,
        boolean hysterectomy,
        boolean iud,
        boolean lactation,
        boolean menopause,
        String other,
        boolean pregnancy,
        boolean radiotherapy
    ) {
    }

    public record GynecologyInfoResponse(
        String additionalNotes,
        String hpvResult,
        String lastMenstrualPeriod,
        boolean menopause,
        String previousCytology,
        String previousTreatment,
        SpecialConditionsResponse specialConditions
    ) {
    }

    public record PatientInfoResponse(
        String age,
        String applicationDate,
        String applicationNo,
        String applyDept,
        String applyDoctor,
        String bedNo,
        String checkItem,
        String clinicalDiagnosis,
        String clinicalHistory,
        String deliveryRequirement,
        String endoscopyDiagnosis,
        boolean frozenReminder,
        String gender,
        String idNo,
        String imagingResult,
        String inpatientNo,
        String patientName,
        boolean patientVerified,
        String phone,
        String registrationStatus,
        String remark,
        String specimenType,
        String wardName
    ) {
    }

    public record SpecimenItemResponse(
        String id,
        Integer quantity,
        String specimenName,
        String specimenNo,
        String specimenSite,
        String status
    ) {
    }

    public record SurgeryInfoResponse(
        String buildingId,
        String clinicalFindings,
        String fixativeType,
        String fixationPerson,
        String fixationTime,
        String roomId,
        String surgeryName
    ) {
    }
}
