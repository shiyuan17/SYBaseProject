package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "SlicingSlidePrintMergeGroupPrintRequest", description = "切片工作站合片组玻片打印请求")
public class SlicingSlidePrintMergeGroupPrintRequest {

    @Schema(description = "未打印合片组 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String printGroupId;

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
