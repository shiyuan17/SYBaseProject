package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TechnicalSpecimenRegistrationDetailSectionsResponse", description = "技术标本登记详情分区")
public record TechnicalSpecimenRegistrationDetailSectionsResponse(
    @Schema(description = "病史摘要")
    String historySummary,
    @Schema(description = "临床检查及手术所见")
    String clinicalExaminationAndSurgeryFindings,
    @Schema(description = "检验和影像检查")
    String labAndImagingExaminations,
    @Schema(description = "临床送检要求")
    String clinicalSubmissionRequirements,
    @Schema(description = "传染/既往信息摘要")
    String infectiousAndPastHistorySummary,
    @Schema(description = "外院病理诊断")
    String externalPathologyDiagnosis
) {
}
