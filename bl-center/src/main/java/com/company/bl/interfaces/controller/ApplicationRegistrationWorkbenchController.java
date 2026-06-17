package com.company.bl.interfaces.controller;

import com.company.bl.application.service.ApplicationRegistrationWorkbenchAppService;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.SaveApplicationRegistrationPatientInfoRequest;
import com.company.bl.interfaces.dto.SaveApplicationRegistrationWorkbenchRequest;
import com.company.bl.interfaces.vo.ApplicationRegistrationWorkbenchResponse;
import com.company.bl.interfaces.vo.ApplicationRegistrationWorkbenchOperatingOptionsResponse;
import com.company.bl.interfaces.vo.ApplicationRegistrationSpecimenDictionaryResponse;
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
        return ApplicationRegistrationWorkbenchResponseAssembler.toResponse(
            workbenchAppService.lookup(keyword, queryType));
    }

    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @GetMapping("/operating-options")
    public ApplicationRegistrationWorkbenchOperatingOptionsResponse listOperatingOptions() {
        return new ApplicationRegistrationWorkbenchOperatingOptionsResponse(
            workbenchAppService.listOperatingBuildingOptions().stream()
                .map(building -> new ApplicationRegistrationWorkbenchOperatingOptionsResponse.OperatingBuildingResponse(
                    building.buildingId(),
                    building.buildingName(),
                    building.floors(),
                    building.location(),
                    building.operatingRooms().stream()
                        .map(room -> new ApplicationRegistrationWorkbenchOperatingOptionsResponse.OperatingRoomResponse(
                            room.buildingId(),
                            room.cleanLevel(),
                            room.floor(),
                            room.roomId(),
                            room.roomName(),
                            room.roomType()))
                        .toList()))
                .toList());
    }

    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @GetMapping("/specimen-dictionary")
    public ApplicationRegistrationSpecimenDictionaryResponse listSpecimenDictionary(
        @Parameter(description = "标本关键字") @RequestParam(value = "keyword", required = false) String keyword,
        HttpServletRequest request
    ) {
        ApplicationRegistrationWorkbenchAppService.SpecimenDictionaryResult result =
            workbenchAppService.listSpecimenDictionary(keyword, RequestOperatorContext.currentUserId(request));
        return new ApplicationRegistrationSpecimenDictionaryResponse(
            result.groups().stream()
                .map(group -> new ApplicationRegistrationSpecimenDictionaryResponse.SpecimenDictionaryGroupResponse(
                    group.systemId(),
                    group.systemName(),
                    group.subParts().stream()
                        .map(part -> new ApplicationRegistrationSpecimenDictionaryResponse.SpecimenDictionaryPartResponse(
                            part.partId(),
                            part.partName(),
                            part.specimens()))
                        .toList()))
                .toList(),
            result.entryOptions().stream()
                .map(this::toSpecimenDictionaryEntryOptionResponse)
                .toList(),
            result.commonOptions().stream()
                .map(this::toSpecimenDictionaryEntryOptionResponse)
                .toList(),
            result.departmentFiltered());
    }

    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @PostMapping("/{applicationId}/save")
    public ApplicationRegistrationWorkbenchResponse save(
        @PathVariable("applicationId") String applicationId,
        @Valid @RequestBody SaveApplicationRegistrationWorkbenchRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return ApplicationRegistrationWorkbenchResponseAssembler.toResponse(workbenchAppService.save(
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
        return ApplicationRegistrationWorkbenchResponseAssembler.toResponse(workbenchAppService.savePatientInfo(
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

    private String resolveUserId(HttpServletRequest request) {
        return RequestOperatorContext.currentUserId(request);
    }

    private String resolveOperatorName(HttpServletRequest request) {
        return RequestOperatorContext.currentOperatorName(request);
    }

    private ApplicationRegistrationSpecimenDictionaryResponse.SpecimenDictionaryEntryOptionResponse toSpecimenDictionaryEntryOptionResponse(
        ApplicationRegistrationWorkbenchAppService.SpecimenDictionaryEntry entry
    ) {
        return new ApplicationRegistrationSpecimenDictionaryResponse.SpecimenDictionaryEntryOptionResponse(
            entry.systemId(),
            entry.systemName(),
            entry.partId(),
            entry.partName(),
            entry.specimenName(),
            entry.searchKeywords());
    }
}
