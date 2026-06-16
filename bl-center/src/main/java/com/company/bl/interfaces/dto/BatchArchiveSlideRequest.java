package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@RejectLegacyOperatorFields
public class BatchArchiveSlideRequest {

    @NotBlank
    @Size(max = 64)
    private String archiveCabinetId;

    @NotEmpty
    private List<@NotBlank @Size(max = 64) String> objectIds;

    @Size(max = 64)
    private String terminalCode;

    @Size(max = 500)
    private String remarks;
}
