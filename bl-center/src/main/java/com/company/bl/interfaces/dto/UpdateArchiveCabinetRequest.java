package com.company.bl.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateArchiveCabinetRequest {

    @NotBlank
    @Size(max = 100)
    private String cabinetName;

    @NotBlank
    @Size(max = 32)
    private String cabinetStatus;

    @Size(max = 64)
    private String operatorUserId;

    @NotBlank
    @Size(max = 100)
    private String operatorName;

    @Size(max = 64)
    private String terminalCode;

    @Size(max = 200)
    private String locationDescription;

    @Size(max = 500)
    private String remarks;
}
