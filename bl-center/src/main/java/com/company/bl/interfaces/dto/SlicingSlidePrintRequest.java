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
@Schema(name = "SlicingSlidePrintRequest", description = "切片工作站玻片打印确认请求")
@RejectLegacyOperatorFields
public class SlicingSlidePrintRequest {

    @Schema(description = "技术任务 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String taskId;

    @Schema(description = "包埋盒 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String embeddingBoxId;

    @Schema(description = "打印前原始玻片数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(1)
    private int sourceSlideCount;

    @Schema(description = "是否按近邻两两合并")
    private boolean mergeAdjacent;

    @Schema(description = "打印机编码")
    @Size(max = 64)
    private String printerCode;

    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;
}
