package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "SlicingSlidePrintMergeGroupCancelRequest", description = "切片工作站取消未打印合片请求")
public class SlicingSlidePrintMergeGroupCancelRequest {

    @Schema(description = "未打印合片组 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private List<@Size(max = 64) String> printGroupIds;

    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;
}
