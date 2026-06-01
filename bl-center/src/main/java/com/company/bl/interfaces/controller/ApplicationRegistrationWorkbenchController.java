package com.company.bl.interfaces.controller;

import com.company.bl.application.service.ApplicationRegistrationWorkbenchAppService;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.SaveApplicationRegistrationPatientInfoRequest;
import com.company.bl.interfaces.dto.SaveApplicationRegistrationWorkbenchRequest;
import com.company.bl.interfaces.vo.ApplicationRegistrationWorkbenchResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
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
                toContagiousSpecimen(request.getContagiousSpecimen()),
                toGynecologyInfo(request.getGynecologyInfo()),
                toPatientInfo(request.getPatientInfo()),
                request.getSpecimenItems().stream().map(item -> new ApplicationRegistrationWorkbenchAppService.SaveSpecimenItem(
                    null,
                    item.getQuantity(),
                    item.getSpecimenName(),
                    item.getSpecimenSite(),
                    item.getStatus()))
                    .toList(),
                toSurgeryInfo(request.getSurgeryInfo()),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest))));
    }

    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @PatchMapping("/{applicationId}/patient-info")
    public ApplicationRegistrationWorkbenchResponse savePatientInfo(
        @PathVariable("applicationId") String applicationId,
        @Valid @RequestBody SaveApplicationRegistrationPatientInfoRequest request
    ) {
        return toResponse(workbenchAppService.savePatientInfo(
            applicationId,
            new ApplicationRegistrationWorkbenchAppService.SavePatientInfoCommand(
                toContagiousSpecimen(request.getContagiousSpecimen()),
                toGynecologyInfo(request.getGynecologyInfo()),
                toPatientInfo(request.getPatientInfo()),
                toSurgeryInfo(request.getSurgeryInfo()))));
    }

    private ApplicationRegistrationWorkbenchAppService.ContagiousSpecimen toContagiousSpecimen(
        SaveApplicationRegistrationWorkbenchRequest.ContagiousSpecimen request
    ) {
        return new ApplicationRegistrationWorkbenchAppService.ContagiousSpecimen(
            request.isHepatitis(),
            request.isHiv(),
            request.isIsolation(),
            request.isSyphilis(),
            request.isTuberculosis());
    }

    private ApplicationRegistrationWorkbenchAppService.GynecologyInfo toGynecologyInfo(
        SaveApplicationRegistrationWorkbenchRequest.GynecologyInfo request
    ) {
        return new ApplicationRegistrationWorkbenchAppService.GynecologyInfo(
            request.getAdditionalNotes(),
            request.getHpvResult(),
            request.getLastMenstrualPeriod(),
            request.isMenopause(),
            request.getPreviousCytology(),
            request.getPreviousTreatment(),
            new ApplicationRegistrationWorkbenchAppService.SpecialConditions(
                request.getSpecialConditions().isAbnormalBleeding(),
                request.getSpecialConditions().isBirthControl(),
                request.getSpecialConditions().isHormoneReplacement(),
                request.getSpecialConditions().isHysterectomy(),
                request.getSpecialConditions().isIud(),
                request.getSpecialConditions().isLactation(),
                request.getSpecialConditions().isMenopause(),
                request.getSpecialConditions().getOther(),
                request.getSpecialConditions().isPregnancy(),
                request.getSpecialConditions().isRadiotherapy()));
    }

    private ApplicationRegistrationWorkbenchAppService.PatientInfo toPatientInfo(
        SaveApplicationRegistrationWorkbenchRequest.PatientInfo request
    ) {
        return new ApplicationRegistrationWorkbenchAppService.PatientInfo(
            request.getAge(),
            request.getApplicationDate(),
            request.getApplicationNo(),
            request.getApplyDept(),
            request.getApplyDoctor(),
            request.getBedNo(),
            request.getCheckItem(),
            request.getClinicalDiagnosis(),
            request.getClinicalHistory(),
            request.getDeliveryRequirement(),
            request.getEndoscopyDiagnosis(),
            request.isFrozenReminder(),
            request.getGender(),
            request.getIdNo(),
            request.getImagingResult(),
            request.getInpatientNo(),
            request.getPatientName(),
            request.isPatientVerified(),
            request.getPhone(),
            request.getRegistrationStatus(),
            request.getRemark(),
            request.getSpecimenType(),
            request.getWardName());
    }

    private ApplicationRegistrationWorkbenchAppService.SurgeryInfo toSurgeryInfo(
        SaveApplicationRegistrationWorkbenchRequest.SurgeryInfo request
    ) {
        return new ApplicationRegistrationWorkbenchAppService.SurgeryInfo(
            request.getBuildingId(),
            request.getClinicalFindings(),
            request.getFixativeType(),
            request.getFixationPerson(),
            request.getFixationTime(),
            request.getRoomId(),
            request.getSpecimenRemovalTime(),
            request.getSurgeryName());
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
