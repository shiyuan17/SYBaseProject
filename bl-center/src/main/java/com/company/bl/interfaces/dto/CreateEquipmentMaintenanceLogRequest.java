package com.company.bl.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateEquipmentMaintenanceLogRequest {

    @NotBlank
    @Size(max = 32)
    private String maintenanceType;

    @NotBlank
    @Size(max = 32)
    private String maintenanceStatus;

    @NotBlank
    @Size(max = 64)
    private String performedAt;

    @Size(max = 64)
    private String nextMaintenanceAt;

    @Size(max = 64)
    private String operatorUserId;

    @NotBlank
    @Size(max = 100)
    private String operatorName;

    @Size(max = 1000)
    private String description;

    @Size(max = 500)
    private String remarks;
}
