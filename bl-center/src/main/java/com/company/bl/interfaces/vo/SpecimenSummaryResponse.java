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
    @Schema(description = "容器名称")
    String containerName,
    @Schema(description = "容器数量")
    Integer containerCount,
    @Schema(description = "标本状态")
    String specimenStatus,
    @Schema(description = "固定状态")
    String fixationStatus,
    @Schema(description = "标签打印状态")
    String labelPrintStatus,
    @Schema(description = "接收结果")
    String receiptStatus,
    @Schema(description = "质控结果")
    String qualityCheckResult,
    @Schema(description = "质控问题代码")
    List<String> qualityIssueCodes,
    @Schema(description = "异常原因")
    String abnormalReason
) {
}
