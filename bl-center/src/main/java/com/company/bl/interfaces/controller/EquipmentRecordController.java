package com.company.bl.interfaces.controller;

import com.company.bl.application.service.OperationSupportModels;
import com.company.bl.application.service.OperationSupportService;
import com.company.bl.interfaces.auth.M5PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CreateEquipmentMaintenanceLogRequest;
import com.company.bl.interfaces.dto.CreateEquipmentRecordRequest;
import com.company.bl.interfaces.dto.UpdateEquipmentRecordRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/equipment-records")
public class EquipmentRecordController extends TechnicalControllerSupport {

    private final OperationSupportService operationSupportService;

    public EquipmentRecordController(OperationSupportService operationSupportService) {
        this.operationSupportService = operationSupportService;
    }

    @RequirePermission(M5PermissionCodes.EQUIPMENT_QUERY)
    @GetMapping
    public List<OperationSupportModels.EquipmentRecordView> listEquipmentRecords(@RequestParam(required = false) String keyword,
                                                                                 @RequestParam(required = false) String equipmentStatus) {
        return operationSupportService.listEquipmentRecords(keyword, equipmentStatus);
    }

    @RequirePermission(M5PermissionCodes.EQUIPMENT_CREATE)
    @PostMapping
    public OperationSupportModels.EquipmentRecordView createEquipmentRecord(@Valid @RequestBody CreateEquipmentRecordRequest request,
                                                                            HttpServletRequest httpServletRequest) {
        return operationSupportService.createEquipmentRecord(new OperationSupportModels.CreateEquipmentRecordCommand(
            request.getEquipmentCode(),
            request.getEquipmentName(),
            request.getEquipmentCategory(),
            request.getModelNo(),
            request.getEquipmentStatus(),
            request.getLocationDescription(),
            request.getEnabledAt(),
            request.getNextMaintenanceAt(),
            resolveUserId(request.getOperatorUserId(), httpServletRequest),
            request.getOperatorName(),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.EQUIPMENT_UPDATE)
    @PatchMapping("/{id}")
    public OperationSupportModels.EquipmentRecordView updateEquipmentRecord(@PathVariable("id") String equipmentId,
                                                                            @Valid @RequestBody UpdateEquipmentRecordRequest request,
                                                                            HttpServletRequest httpServletRequest) {
        return operationSupportService.updateEquipmentRecord(new OperationSupportModels.UpdateEquipmentRecordCommand(
            equipmentId,
            request.getEquipmentName(),
            request.getEquipmentCategory(),
            request.getModelNo(),
            request.getEquipmentStatus(),
            request.getLocationDescription(),
            request.getEnabledAt(),
            request.getNextMaintenanceAt(),
            resolveUserId(request.getOperatorUserId(), httpServletRequest),
            request.getOperatorName(),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.EQUIPMENT_QUERY)
    @GetMapping("/{id}/maintenance-logs")
    public List<OperationSupportModels.EquipmentMaintenanceLogView> listMaintenanceLogs(@PathVariable("id") String equipmentId) {
        return operationSupportService.listEquipmentMaintenanceLogs(equipmentId);
    }

    @RequirePermission(M5PermissionCodes.EQUIPMENT_MAINTENANCE_CREATE)
    @PostMapping("/{id}/maintenance-logs")
    public OperationSupportModels.EquipmentMaintenanceLogView createMaintenanceLog(@PathVariable("id") String equipmentId,
                                                                                   @Valid @RequestBody CreateEquipmentMaintenanceLogRequest request,
                                                                                   HttpServletRequest httpServletRequest) {
        return operationSupportService.createEquipmentMaintenanceLog(new OperationSupportModels.CreateEquipmentMaintenanceLogCommand(
            equipmentId,
            request.getMaintenanceType(),
            request.getMaintenanceStatus(),
            request.getPerformedAt(),
            request.getNextMaintenanceAt(),
            resolveUserId(request.getOperatorUserId(), httpServletRequest),
            request.getOperatorName(),
            request.getDescription(),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.EQUIPMENT_WARNING_QUERY)
    @GetMapping("/warnings")
    public List<OperationSupportModels.EquipmentWarningView> listEquipmentWarnings() {
        return operationSupportService.listEquipmentWarnings();
    }
}
