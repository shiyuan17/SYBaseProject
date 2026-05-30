package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "EmbeddingCompleteRequest", description = "包埋完成请求")
@RejectLegacyOperatorFields
public class EmbeddingCompleteRequest {

    @Schema(description = "技术任务 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String taskId;

    @Schema(description = "取材块 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String samplingBlockId;

    @Schema(description = "包埋盒编号")
    @Size(max = 64)
    private String embeddingBoxNo;

    @Schema(description = "蜡块数量，最小为 1", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(1)
    private int blockCount;

    @Schema(description = "切片提示")
    @Size(max = 500)
    private String sliceNotice;

    @Schema(description = "评估等级")
    @Size(max = 32)
    private String evaluationLevel;

    @Schema(description = "取材评估")
    @Size(max = 500)
    private String samplingEvaluation;

    @Schema(description = "设备编码")
    @Size(max = 64)
    private String deviceCode;



    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;
}