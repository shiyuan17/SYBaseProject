package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "RoutineMedicalOrderMergeRequest", description = "常规医嘱相同项目合片请求")
@RejectLegacyOperatorFields
public class RoutineMedicalOrderMergeRequest {

    @Schema(description = "待合片医嘱 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private List<@Size(max = 64) String> orderIds;

    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;
}
