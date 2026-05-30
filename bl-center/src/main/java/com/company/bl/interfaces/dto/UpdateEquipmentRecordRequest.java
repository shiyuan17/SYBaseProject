package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RejectLegacyOperatorFields
public class UpdateEquipmentRecordRequest {

    @NotBlank
    @Size(max = 100)
    private String equipmentName;

    @Size(max = 64)
    private String equipmentCategory;

    @Size(max = 100)
    private String modelNo;

    @NotBlank
    @Size(max = 32)
    private String equipmentStatus;

    @Size(max = 200)
    private String locationDescription;

    @Size(max = 64)
    private String enabledAt;

    @Size(max = 64)
    private String nextMaintenanceAt;

    @Size(max = 500)
    private String remarks;
}
