package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(name = "TechnicalTaskAssignRequest", description = "技术任务分派请求")
public class TechnicalTaskAssignRequest {

    @Schema(description = "任务优先级")
    @Size(max = 32)
    private String priority;

    @Schema(description = "分派工作台编码")
    @Size(max = 64)
    private String stationCode;

    @Schema(description = "分派工作台名称")
    @Size(max = 100)
    private String stationName;

    @Schema(description = "责任技师用户 ID")
    @Size(max = 64)
    private String assignedToUserId;

    @Schema(description = "责任技师姓名")
    @Size(max = 100)
    private String assignedToName;

    @Schema(description = "期望完成时间")
    private LocalDateTime expectedCompletedAt;

    @Schema(description = "生产备注")
    @Size(max = 500)
    private String productionRemarks;

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
}
