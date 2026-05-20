package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(name = "CreateDehydrationBatchRequest", description = "脱水批次创建请求")
public class CreateDehydrationBatchRequest {

    @Schema(description = "病例 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String caseId;

    @Schema(description = "脱水筐编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String basketNo;

    @Schema(description = "设备编号")
    @Size(max = 64)
    private String deviceNo;

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

    @Schema(description = "取材块 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private List<String> samplingBlockIds;
}
