package com.company.bl.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReworkOrderRequest {

    @NotBlank
    @Size(max = 64)
    private String caseId;

    @Size(max = 64)
    private String specimenId;

    @Size(max = 64)
    private String samplingBlockId;

    @Size(max = 64)
    private String embeddingBoxId;

    @Size(max = 64)
    private String slideId;

    @NotBlank
    @Size(max = 50)
    private String reworkType;

    @NotBlank
    @Size(max = 500)
    private String reason;

    @Size(max = 50)
    private String qcType;

    @Size(max = 64)
    private String operatorUserId;

    @NotBlank
    @Size(max = 100)
    private String operatorName;

    @Size(max = 64)
    private String terminalCode;

    @Size(max = 500)
    private String remarks;
}
