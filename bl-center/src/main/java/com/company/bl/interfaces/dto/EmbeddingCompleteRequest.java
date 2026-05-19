package com.company.bl.interfaces.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmbeddingCompleteRequest {

    @NotBlank
    @Size(max = 64)
    private String taskId;

    @NotBlank
    @Size(max = 64)
    private String samplingBlockId;

    @Size(max = 64)
    private String embeddingBoxNo;

    @Min(1)
    private int blockCount;

    @Size(max = 500)
    private String sliceNotice;

    @Size(max = 32)
    private String evaluationLevel;

    @Size(max = 500)
    private String samplingEvaluation;

    @Size(max = 64)
    private String deviceCode;

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
