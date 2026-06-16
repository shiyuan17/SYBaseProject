package com.company.bl.interfaces.controller;

import com.company.bl.application.service.MedicalWasteModels;
import com.company.bl.application.service.MedicalWasteService;
import com.company.bl.interfaces.auth.M5PermissionCodes;
import com.company.bl.interfaces.auth.RequireAnyPermission;
import com.company.bl.interfaces.dto.MedicalWasteReagentHandoverRequest;
import com.company.bl.interfaces.dto.MedicalWasteSpecimenPreviewRequest;
import com.company.bl.interfaces.dto.PrintMedicalWasteSpecimenBatchRequest;
import com.company.bl.interfaces.dto.SaveMedicalWasteReagentBagRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/medical-waste")
@RequireAnyPermission({
    M5PermissionCodes.REAGENT_QUERY,
    M5PermissionCodes.REAGENT_CREATE,
    M5PermissionCodes.REAGENT_UPDATE,
    M5PermissionCodes.REAGENT_STOCK_QUERY,
    M5PermissionCodes.REAGENT_STOCK_UPDATE,
    M5PermissionCodes.REAGENT_WARNING_QUERY,
    M5PermissionCodes.EQUIPMENT_QUERY,
    M5PermissionCodes.EQUIPMENT_CREATE,
    M5PermissionCodes.EQUIPMENT_UPDATE,
    M5PermissionCodes.EQUIPMENT_MAINTENANCE_CREATE,
    M5PermissionCodes.EQUIPMENT_WARNING_QUERY
})
public class MedicalWasteController extends TechnicalControllerSupport {

    private final MedicalWasteService medicalWasteService;

    public MedicalWasteController(MedicalWasteService medicalWasteService) {
        this.medicalWasteService = medicalWasteService;
    }

    @GetMapping("/specimen-batches")
    public List<MedicalWasteModels.SpecimenBatchView> listSpecimenBatches(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String createdByName,
        @RequestParam(required = false) LocalDate dateFrom,
        @RequestParam(required = false) LocalDate dateTo
    ) {
        return medicalWasteService.listSpecimenBatches(keyword, createdByName, dateFrom, dateTo);
    }

    @GetMapping("/specimen-options")
    public MedicalWasteModels.SpecimenOptionsView getSpecimenOptions() {
        return medicalWasteService.getSpecimenOptions();
    }

    @PostMapping("/specimen-batches/preview-labels")
    public List<MedicalWasteModels.SpecimenPreviewLabelView> previewSpecimenLabels(
        @Valid @RequestBody MedicalWasteSpecimenPreviewRequest request
    ) {
        return medicalWasteService.previewSpecimenLabels(new MedicalWasteModels.SpecimenPreviewRequest(
            request.getBagName(),
            request.getGrossingOperatorName(),
            request.getGrossingStationName(),
            request.getGrossingDate(),
            request.getGrossingPeriod()));
    }

    @PostMapping("/specimen-batches/print")
    public MedicalWasteModels.PrintSpecimenBatchResult printSpecimenBatch(
        @Valid @RequestBody PrintMedicalWasteSpecimenBatchRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return medicalWasteService.printSpecimenBatch(new MedicalWasteModels.PrintSpecimenBatchCommand(
            request.getBagName(),
            request.getGrossingOperatorName(),
            request.getGrossingStationName(),
            request.getGrossingDate(),
            request.getGrossingPeriod(),
            request.getWeightKg(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest)));
    }

    @PostMapping("/specimen-batches/{id}/destroy")
    public MedicalWasteModels.SpecimenBatchView destroySpecimenBatch(@PathVariable("id") String batchId,
                                                                     HttpServletRequest httpServletRequest) {
        return medicalWasteService.destroySpecimenBatch(new MedicalWasteModels.DestroySpecimenBatchCommand(
            batchId,
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest)));
    }

    @GetMapping("/reagent-bags")
    public List<MedicalWasteModels.ReagentBagView> listReagentBags(@RequestParam(required = false) String keyword,
                                                                   @RequestParam(required = false) LocalDate dateFrom,
                                                                   @RequestParam(required = false) LocalDate dateTo) {
        return medicalWasteService.listReagentBags(keyword, dateFrom, dateTo);
    }

    @PostMapping("/reagent-bags")
    public MedicalWasteModels.ReagentBagView saveReagentBag(@Valid @RequestBody SaveMedicalWasteReagentBagRequest request,
                                                            HttpServletRequest httpServletRequest) {
        return medicalWasteService.saveReagentBag(new MedicalWasteModels.SaveReagentBagCommand(
            request.getId(),
            request.getBagName(),
            request.getWasteType(),
            request.getWeightKg(),
            request.getVolumeMl(),
            request.getSource(),
            request.getRemarks(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest)));
    }

    @PostMapping("/reagent-bags/{id}/handover")
    public MedicalWasteModels.ReagentBagView handoverReagentBag(@PathVariable("id") String bagId,
                                                                @Valid @RequestBody MedicalWasteReagentHandoverRequest request,
                                                                HttpServletRequest httpServletRequest) {
        return medicalWasteService.handoverReagentBag(new MedicalWasteModels.HandoverReagentBagCommand(
            bagId,
            request.getHandedOverByName(),
            request.getHandedOverAt(),
            request.getHandoverRemarks(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest)));
    }
}
