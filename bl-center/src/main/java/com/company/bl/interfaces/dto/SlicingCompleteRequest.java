package com.company.bl.interfaces.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SlicingCompleteRequest {

    @NotBlank
    @Size(max = 64)
    private String taskId;

    @NotBlank
    @Size(max = 64)
    private String embeddingBoxId;

    @Min(1)
    private int slideCount;

    private Integer sliceCountPerSlide;

    @Size(max = 64)
    private String sliceThickness;

    @Size(max = 500)
    private String qualityIssue;

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
