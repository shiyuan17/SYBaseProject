package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(name = "DirectReceiveSpecimensRequest", description = "按条码直接接收标本请求")
public class DirectReceiveSpecimensRequest {

    @Schema(description = "接收人用户 ID")
    @Size(max = 64)
    private String receivedByUserId;

    @Schema(description = "接收人姓名")
    @Size(max = 100)
    private String receivedByName;

    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "接收明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty
    private List<ReceiveSpecimensRequest.ReceiptItem> items;
}
