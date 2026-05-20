package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "SlicingCompleteRequest", description = "切片完成请求")
public class SlicingCompleteRequest {

    @Schema(description = "技术任务 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String taskId;

    @Schema(description = "包埋盒 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String embeddingBoxId;

    @Schema(description = "切片数量，最小为 1", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(1)
    private int slideCount;

    @Schema(description = "单张切片切片数")
    private Integer sliceCountPerSlide;

    @Schema(description = "切片厚度")
    @Size(max = 64)
    private String sliceThickness;

    @Schema(description = "质量问题")
    @Size(max = 500)
    private String qualityIssue;

    @Schema(description = "设备编码")
    @Size(max = 64)
    private String deviceCode;

    @Schema(description = "操作人用户 ID")
    @Size(max = 64)
    private String operatorUserId;

    @Schema(description = "操作人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 100)
    private String operatorName;

    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;
}
