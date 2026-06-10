package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(name = "CreateTransportOrderRequest", description = "转运单创建请求")
public class CreateTransportOrderRequest {

    @Schema(description = "核对操作人登录确认 token；非当前登录人操作时传入")
    private String operatorVerificationToken;

    @Schema(description = "申请单 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String applicationId;

    @Schema(description = "标本 ID 列表；优先于标本条码列表")
    private List<String> specimenIds;

    @Schema(description = "标本条码列表")
    private List<String> specimenBarcodes;

    @Schema(description = "交接人用户 ID")
    @Size(max = 64)
    private String handoverUserId;

    @Schema(description = "交接人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 100)
    private String handoverUserName;

    @Schema(description = "交接科室 ID")
    @Size(max = 64)
    private String handoverDepartmentId;

    @Schema(description = "交接科室名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 100)
    private String handoverDepartmentName;

    @Schema(description = "接收科室 ID")
    @Size(max = 64)
    private String receiverDepartmentId;

    @Schema(description = "接收科室名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 100)
    private String receiverDepartmentName;

    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;
}
