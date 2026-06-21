package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "SpecimenSummaryResponse", description = "标本摘要")
public record SpecimenSummaryResponse(
    @Schema(description = "标本 ID")
    String id,
    @Schema(description = "标本号")
    String specimenNo,
    @Schema(description = "标本条码")
    String barcode,
    @Schema(description = "标本名称")
    String specimenName,
    @Schema(description = "标本类型")
    String specimenType,
    @Schema(description = "标本部位")
    String specimenSite,
    @Schema(description = "采集方式")
    String collectionMode,
    @Schema(description = "临床症状")
    String clinicalSymptom,
    @Schema(description = "标本数量")
    Integer specimenCount,
    @Schema(description = "标本大小")
    String specimenSize,
    @Schema(description = "登记评价")
    String registrationEvaluationItems,
    @Schema(description = "容器名称")
    String containerName,
    @Schema(description = "容器数量")
    Integer containerCount,
    @Schema(description = "标本状态")
    String specimenStatus,
    @Schema(description = "固定状态")
    String fixationStatus,
    @Schema(description = "核对状态")
    String verificationStatus,
    @Schema(description = "开始核对时间")
    String verificationStartedAt,
    @Schema(description = "完成核对时间")
    String verificationCompletedAt,
    @Schema(description = "核对人")
    String verifiedByName,
    @Schema(description = "条码绑定状态")
    String barcodeBindingStatus,
    @Schema(description = "标签打印状态")
    String labelPrintStatus,
    @Schema(description = "离体时间")
    String specimenRemovalAt,
    @Schema(description = "离体操作人")
    String specimenRemovalOperatorName,
    @Schema(description = "标本确认时间")
    String specimenConfirmedAt,
    @Schema(description = "入库状态")
    String checkInStatus,
    @Schema(description = "入库时间")
    String checkedInAt,
    @Schema(description = "入库操作人")
    String checkedInByName,
    @Schema(description = "接收结果")
    String receiptStatus,
    @Schema(description = "质控结果")
    String qualityCheckResult,
    @Schema(description = "质控问题代码")
    List<String> qualityIssueCodes,
    @Schema(description = "登记人")
    String registeredByName,
    @Schema(description = "登记时间")
    String registeredAt,
    @Schema(description = "登记终端")
    String terminalCode,
    @Schema(description = "备注")
    String remarks,
    @Schema(description = "异常类型")
    String abnormalType,
    @Schema(description = "异常原因")
    String abnormalReason
) {
}
