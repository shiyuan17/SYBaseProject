package com.company.bl.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateDehydrationBatchRequest {

    @NotBlank
    @Size(max = 64)
    private String caseId;

    @NotBlank
    @Size(max = 64)
    private String basketNo;

    @Size(max = 64)
    private String deviceNo;

    @Size(max = 64)
    private String operatorUserId;

    @NotBlank
    @Size(max = 100)
    private String operatorName;

    @Size(max = 64)
    private String terminalCode;

    @Size(max = 500)
    private String remarks;

    @NotEmpty
    private List<String> samplingBlockIds;
}
