package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(name = "ReceiveSpecimensRequest", description = "按转运单接收标本请求")
public class ReceiveSpecimensRequest {

    @Schema(description = "转运单 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String transportOrderId;

    @Schema(description = "接收人用户 ID")
    @Size(max = 64)
    private String receivedByUserId;

    @Schema(description = "接收人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 100)
    private String receivedByName;

    @Schema(description = "物流人员姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 100)
    private String logisticsStaffName;

    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "接收明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty
    private List<ReceiptItem> items;

    @Getter
    @Setter
    @Schema(name = "ReceiptItem", description = "标本接收明细")
    public static class ReceiptItem {
        @Schema(description = "标本条码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 128)
        private String specimenBarcode;

        @Schema(description = "接收状态", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 32)
        private String receiptStatus;

        @Schema(description = "容器数量", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private Integer containerCount;

        @Schema(description = "质控结果，PASSED/FAILED", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 32)
        private String qualityCheckResult;

        @Schema(description = "质控问题代码")
        private List<@Size(max = 64) String> qualityIssueCodes;

        @Schema(description = "异常原因")
        @Size(max = 500)
        private String reason;

        @Schema(description = "备注")
        @Size(max = 500)
        private String remarks;
    }
}
