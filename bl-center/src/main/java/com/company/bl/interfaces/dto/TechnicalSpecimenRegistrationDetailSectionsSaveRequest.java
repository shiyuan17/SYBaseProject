package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "TechnicalSpecimenRegistrationDetailSectionsSaveRequest", description = "技术标本登记摘要分区保存请求")
@RejectLegacyOperatorFields
public class TechnicalSpecimenRegistrationDetailSectionsSaveRequest {

    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Valid
    @NotNull
    @Schema(description = "摘要分区内容")
    private DetailSections detailSections;

    @Getter
    @Setter
    @Schema(name = "TechnicalSpecimenRegistrationDetailSectionsSaveItem", description = "技术标本登记摘要分区")
    public static class DetailSections {

        @Schema(description = "病史摘要")
        @Size(max = 1000)
        private String historySummary;

        @Schema(description = "临床检查及手术所见")
        @Size(max = 1000)
        private String clinicalExaminationAndSurgeryFindings;

        @Schema(description = "检验和影像检查")
        @Size(max = 1000)
        private String labAndImagingExaminations;

        @Schema(description = "临床送检要求")
        @Size(max = 1000)
        private String clinicalSubmissionRequirements;

        @Schema(description = "传染/既往信息摘要")
        @Size(max = 1000)
        private String infectiousAndPastHistorySummary;

        @Schema(description = "外院病理诊断")
        @Size(max = 1000)
        private String externalPathologyDiagnosis;
    }
}
