package com.company.bl.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArchiveApplicationFormRequest {

    @NotBlank
    @Size(max = 64)
    private String caseId;

    @NotBlank
    @Size(max = 64)
    private String archivePositionId;

    @Size(max = 64)
    private String operatorUserId;

    @NotBlank
    @Size(max = 100)
    private String operatorName;

    @Size(max = 64)
    private String terminalCode;

    @Size(max = 1000)
    private String fileUrl;

    @Size(max = 255)
    private String fileName;

    @Size(max = 500)
    private String remarks;
}
