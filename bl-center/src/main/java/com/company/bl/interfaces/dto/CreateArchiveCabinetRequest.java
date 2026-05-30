package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "CreateArchiveCabinetRequest", description = "Create archive cabinet request")
@RejectLegacyOperatorFields
public class CreateArchiveCabinetRequest {

    @NotBlank
    @Size(max = 64)
    private String cabinetCode;

    @NotBlank
    @Size(max = 100)
    private String cabinetName;

    @NotBlank
    @Size(max = 32)
    private String cabinetType;

    @Min(1)
    private int layerCount;

    @Min(1)
    private int slotCountPerLayer;

    @Size(max = 64)
    private String terminalCode;

    @Size(max = 200)
    private String locationDescription;

    @Size(max = 500)
    private String remarks;
}
