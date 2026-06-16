package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "BatchCreateArchiveCabinetRequest", description = "Batch create archive cabinet request")
@RejectLegacyOperatorFields
public class BatchCreateArchiveCabinetRequest {

    @Size(max = 64)
    private String parentId;

    @NotBlank
    @Size(max = 32)
    private String cabinetType;

    @NotBlank
    @Size(max = 48)
    private String cabinetCodePrefix;

    @Min(0)
    private int startNo;

    @Min(1)
    @Max(100)
    private int count;

    @Min(1)
    @Max(10)
    private int numberWidth;

    @Size(max = 80)
    private String cabinetNamePrefix;

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
