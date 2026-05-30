package com.company.bl.interfaces.controller;

import com.company.bl.application.service.ApplicationRegistrationWorkbenchAppService;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.SaveApplicationRegistrationWorkbenchRequest;
import com.company.bl.interfaces.vo.ApplicationRegistrationWorkbenchResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/application-registration-workbench")
@RequiredArgsConstructor
@Tag(name = "Application Registration Workbench", description = "申请登记工作台查询与保存")
public class ApplicationRegistrationWorkbenchController {

    private final ApplicationRegistrationWorkbenchAppService workbenchAppService;

    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @GetMapping("/lookup")
    public ApplicationRegistrationWorkbenchResponse lookup(
        @Parameter(description = "申请单号或者住院号关键字") @RequestParam("keyword") String keyword,
        @Parameter(description = "查询类型") @RequestParam(value = "queryType", required = false) String queryType
    ) {
        return toResponse(workbenchAppService.lookup(keyword, queryType));
    }

    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @PostMapping("/{applicationId}/save")
    public ApplicationRegistrationWorkbenchResponse save(
        @PathVariable("applicationId") String applicationId,
        @Valid @RequestBody SaveApplicationRegistrationWorkbenchRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return toResponse(workbenchAppService.save(
            applicationId,
            new ApplicationRegistrationWorkbenchAppService.SaveWorkbenchCommand(
                new ApplicationRegistrationWorkbenchAppService.ContagiousSpecimen(
                    request.getContagiousSpecimen().isHepatitis(),
                    request.getContagiousSpecimen().isHiv(),
                    request.getContagiousSpecimen().isIsolation(),
                    request.getContagiousSpecimen().isSyphilis(),
                    request.getContagiousSpecimen().isTuberculosis()),
                new ApplicationRegistrationWorkbenchAppService.GynecologyInfo(
                    request.getGynecologyInfo().getAdditionalNotes(),
                    request.getGynecologyInfo().getHpvResult(),
                    request.getGynecologyInfo().getLastMenstrualPeriod(),
                    request.getGynecologyInfo().isMenopause(),
                    request.getGynecologyInfo().getPreviousCytology(),
                    request.getGynecologyInfo().getPreviousTreatment(),
                    new ApplicationRegistrationWorkbenchAppService.SpecialConditions(
                        request.getGynecologyInfo().getSpecialConditions().isAbnormalBleeding(),
                        request.getGynecologyInfo().getSpecialConditions().isBirthControl(),
                        request.getGynecologyInfo().getSpecialConditions().isHormoneReplacement(),
                        request.getGynecologyInfo().getSpecialConditions().isHysterectomy(),
                        request.getGynecologyInfo().getSpecialConditions().isIud(),
                        request.getGynecologyInfo().getSpecialConditions().isLactation(),
                        request.getGynecologyInfo().getSpecialConditions().isMenopause(),
                        request.getGynecologyInfo().getSpecialConditions().getOther(),
                        request.getGynecologyInfo().getSpecialConditions().isPregnancy(),
                        request.getGynecologyInfo().getSpecialConditions().isRadiotherapy())),
                new ApplicationRegistrationWorkbenchAppService.PatientInfo(
                    request.getPatientInfo().getAge(),
                    request.getPatientInfo().getApplicationDate(),
                    request.getPatientInfo().getApplicationNo(),
                    request.getPatientInfo().getApplyDept(),
                    request.getPatientInfo().getApplyDoctor(),
                    request.getPatientInfo().getBedNo(),
                    request.getPatientInfo().getCheckItem(),
                    request.getPatientInfo().getClinicalDiagnosis(),
                    request.getPatientInfo().getClinicalHistory(),
                    request.getPatientInfo().getDeliveryRequirement(),
                    request.getPatientInfo().getEndoscopyDiagnosis(),
                    request.getPatientInfo().isFrozenReminder(),
                    request.getPatientInfo().getGender(),
                    request.getPatientInfo().getIdNo(),
                    request.getPatientInfo().getImagingResult(),
                    request.getPatientInfo().getInpatientNo(),
                    request.getPatientInfo().getPatientName(),
                    request.getPatientInfo().isPatientVerified(),
                    request.getPatientInfo().getPhone(),
                    request.getPatientInfo().getRegistrationStatus(),
                    request.getPatientInfo().getRemark(),
                    request.getPatientInfo().getSpecimenType(),
                    request.getPatientInfo().getWardName()),
                request.getSpecimenItems().stream().map(item -> new ApplicationRegistrationWorkbenchAppService.SaveSpecimenItem(
                    null,
                    item.getQuantity(),
                    item.getSpecimenName(),
                    item.getSpecimenSite(),
                    item.getStatus()))
                    .toList(),
                new ApplicationRegistrationWorkbenchAppService.SurgeryInfo(
                    request.getSurgeryInfo().getBuildingId(),
                    request.getSurgeryInfo().getClinicalFindings(),
                    request.getSurgeryInfo().getFixativeType(),
                    request.getSurgeryInfo().getFixationPerson(),
                    request.getSurgeryInfo().getFixationTime(),
                    request.getSurgeryInfo().getRoomId(),
                    request.getSurgeryInfo().getSpecimenRemovalTime(),
                    request.getSurgeryInfo().getSurgeryName()),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest))));
    }

    private ApplicationRegistrationWorkbenchResponse toResponse(
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

    private String resolveUserId(HttpServletRequest request) {
        return RequestOperatorContext.currentUserId(request);
    }

    private String resolveOperatorName(HttpServletRequest request) {
        return RequestOperatorContext.currentOperatorName(request);
    }
}
