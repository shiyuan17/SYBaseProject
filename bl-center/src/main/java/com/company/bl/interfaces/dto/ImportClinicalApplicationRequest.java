package com.company.bl.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImportClinicalApplicationRequest {

    @NotBlank
    @Size(max = 64)
    private String thirdPartySource;

    @NotBlank
    @Size(max = 64)
    private String externalOrderNo;
}
