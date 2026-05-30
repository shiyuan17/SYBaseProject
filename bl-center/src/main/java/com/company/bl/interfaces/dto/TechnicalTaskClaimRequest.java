package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "TechnicalTaskClaimRequest", description = "技术任务领取请求")
@RejectLegacyOperatorFields
public class TechnicalTaskClaimRequest {

    @Schema(description = "责任技师用户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String assignedToUserId;

    @Schema(description = "责任技师姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 100)
    private String assignedToName;

    @Schema(description = "工作台编码")
    @Size(max = 64)
    private String stationCode;

    @Schema(description = "工作台名称")
    @Size(max = 100)
    private String stationName;



    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;
}