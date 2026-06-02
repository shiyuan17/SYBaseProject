package com.company.bl.interfaces.controller;

import com.company.bl.application.service.ApplicationRegistrationWorkbenchAppService;
import com.company.bl.interfaces.vo.ApplicationRegistrationWorkbenchResponse;

final class ApplicationRegistrationWorkbenchResponseAssembler {

    private ApplicationRegistrationWorkbenchResponseAssembler() {
    }

    static ApplicationRegistrationWorkbenchResponse toResponse(
        ApplicationRegistrationWorkbenchAppService.WorkbenchRecord record
    ) {
        return new ApplicationRegistrationWorkbenchResponse(
            record.applicationId(),
            new ApplicationRegistrationWorkbenchResponse.ContagiousSpecimenResponse(
                record.contagiousSpecimen().hepatitis(),
                record.contagiousSpecimen().hiv(),
                record.contagiousSpecimen().isolation(),
                record.contagiousSpecimen().syphilis(),
                record.contagiousSpecimen().tuberculosis()),
            new ApplicationRegistrationWorkbenchResponse.GynecologyInfoResponse(
                record.gynecologyInfo().additionalNotes(),
                record.gynecologyInfo().hpvResult(),
                record.gynecologyInfo().lastMenstrualPeriod(),
                record.gynecologyInfo().menopause(),
                record.gynecologyInfo().previousCytology(),
                record.gynecologyInfo().previousTreatment(),
                new ApplicationRegistrationWorkbenchResponse.SpecialConditionsResponse(
                    record.gynecologyInfo().specialConditions().abnormalBleeding(),
                    record.gynecologyInfo().specialConditions().birthControl(),
                    record.gynecologyInfo().specialConditions().hormoneReplacement(),
                    record.gynecologyInfo().specialConditions().hysterectomy(),
                    record.gynecologyInfo().specialConditions().iud(),
                    record.gynecologyInfo().specialConditions().lactation(),
                    record.gynecologyInfo().specialConditions().menopause(),
                    record.gynecologyInfo().specialConditions().other(),
                    record.gynecologyInfo().specialConditions().pregnancy(),
                    record.gynecologyInfo().specialConditions().radiotherapy())),
            new ApplicationRegistrationWorkbenchResponse.PatientInfoResponse(
                record.patientInfo().age(),
                record.patientInfo().applicationDate(),
                record.patientInfo().applicationNo(),
                record.patientInfo().applyDept(),
                record.patientInfo().applyDoctor(),
                record.patientInfo().bedNo(),
                record.patientInfo().checkItem(),
                record.patientInfo().clinicalDiagnosis(),
                record.patientInfo().clinicalHistory(),
                record.patientInfo().deliveryRequirement(),
                record.patientInfo().endoscopyDiagnosis(),
                record.patientInfo().frozenReminder(),
                record.patientInfo().gender(),
                record.patientInfo().idNo(),
                record.patientInfo().imagingResult(),
                record.patientInfo().inpatientNo(),
                record.patientInfo().patientName(),
                record.patientInfo().patientVerified(),
                record.patientInfo().phone(),
                record.patientInfo().registrationStatus(),
                record.patientInfo().remark(),
                record.patientInfo().specimenType(),
                record.patientInfo().wardName()),
            record.specimenItems().stream().map(item -> new ApplicationRegistrationWorkbenchResponse.SpecimenItemResponse(
                item.id(),
                item.quantity(),
                item.specimenName(),
                item.specimenNo(),
                item.specimenSite(),
                item.status()))
                .toList(),
            new ApplicationRegistrationWorkbenchResponse.SurgeryInfoResponse(
                record.surgeryInfo().buildingId(),
                record.surgeryInfo().clinicalFindings(),
                record.surgeryInfo().fixativeType(),
                record.surgeryInfo().fixationPerson(),
                record.surgeryInfo().fixationTime(),
                record.surgeryInfo().roomId(),
                record.surgeryInfo().specimenRemovalTime(),
                record.surgeryInfo().surgeryName()));
    }
}
