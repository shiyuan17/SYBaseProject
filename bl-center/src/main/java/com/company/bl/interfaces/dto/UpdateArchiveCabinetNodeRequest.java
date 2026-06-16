package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RejectLegacyOperatorFields
public class UpdateArchiveCabinetNodeRequest {

    @NotBlank
    @Size(max = 64)
    private String nodeCode;

    @Size(max = 32)
    private String cabinetType;

    @Min(0)
    private int capacity;

    @Size(max = 64)
    private String terminalCode;

    @Size(max = 200)
    private String pathLocation;

    @Size(max = 500)
    private String remarks;
}
