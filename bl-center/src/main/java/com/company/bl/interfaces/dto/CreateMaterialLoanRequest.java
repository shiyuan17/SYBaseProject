package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RejectLegacyOperatorFields
public class CreateMaterialLoanRequest {

    @NotBlank
    @Size(max = 32)
    private String materialType;

    @NotBlank
    @Size(max = 64)
    private String materialId;

    @Size(max = 64)
    private String borrowedByUserId;

    @NotBlank
    @Size(max = 100)
    private String borrowedByName;

    @Size(max = 500)
    private String borrowPurpose;

    @Size(max = 64)
    private String terminalCode;

    @Size(max = 500)
    private String remarks;
}
