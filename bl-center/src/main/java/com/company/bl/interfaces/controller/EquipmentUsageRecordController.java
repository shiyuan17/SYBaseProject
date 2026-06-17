package com.company.bl.interfaces.controller;

import com.company.bl.application.service.OperationSupportModels;
import com.company.bl.application.service.OperationSupportService;
import com.company.bl.interfaces.auth.M5PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CreateEquipmentUsageRecordRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/equipment-usage-records")
public class EquipmentUsageRecordController extends TechnicalControllerSupport {

    private final OperationSupportService operationSupportService;

    public EquipmentUsageRecordController(OperationSupportService operationSupportService) {
        this.operationSupportService = operationSupportService;
    }

    @RequirePermission(M5PermissionCodes.EQUIPMENT_QUERY)
    @GetMapping("/common-devices")
    public List<OperationSupportModels.EquipmentCommonDeviceView> listCommonDevices() {
        return operationSupportService.listEquipmentCommonDevices();
    }

    @RequirePermission(M5PermissionCodes.EQUIPMENT_UPDATE)
    @PostMapping
    public OperationSupportModels.EquipmentUsageRecordView createEquipmentUsageRecord(
        @Valid @RequestBody CreateEquipmentUsageRecordRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return operationSupportService.createEquipmentUsageRecord(new OperationSupportModels.CreateEquipmentUsageRecordCommand(
            request.getEquipmentId(),
            request.getEquipmentCategory(),
            request.getEquipmentName(),
            request.getCommonlyUsed(),
            request.getStartedAt(),
            request.getEndedAt(),
            request.getRuntimeHours(),
            request.getDiagnosisCount(),
            request.getEquipmentCondition(),
            resolveUserId(httpServletRequest),
            request.getUsageOperatorName(),
            request.getUsageContent(),
            request.getRemarks()));
    }
}
