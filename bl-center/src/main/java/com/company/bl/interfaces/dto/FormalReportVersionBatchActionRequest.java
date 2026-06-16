package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(name = "FormalReportVersionBatchActionRequest", description = "正式报告批量动作请求")
@RejectLegacyOperatorFields
public class FormalReportVersionBatchActionRequest {

    @Schema(description = "报告版本 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private List<@Size(max = 64) String> versionIds;

    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "发放模式：IMMEDIATE / DELAY_2_HOURS / DELAY_3_HOURS")
    @Size(max = 32)
    private String issueMode;

    @Schema(description = "计划发放时间，延时发放时可由后端按模式计算后回填")
    @Size(max = 64)
    private String plannedIssueAt;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;
}
