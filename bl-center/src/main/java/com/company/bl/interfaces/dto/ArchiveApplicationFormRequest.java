package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RejectLegacyOperatorFields
public class ArchiveApplicationFormRequest {

    @NotBlank
    @Size(max = 64)
    private String caseId;

    @NotBlank
    @Size(max = 64)
    private String archivePositionId;

    @Size(max = 64)
    private String terminalCode;

    @Size(max = 1000)
    private String fileUrl;

    @Size(max = 255)
    private String fileName;

    @Size(max = 500)
    private String remarks;
}
