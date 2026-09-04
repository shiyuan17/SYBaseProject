package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "UpdatePathologyReportDraftRequest", description = "保存病理报告草稿请求")
@RejectLegacyOperatorFields
public class UpdatePathologyReportDraftRequest {

    @Schema(description = "临床诊断")
    @Size(max = 500)
    private String clinicalDiagnosis;

    @Schema(description = "大体检查")
    private String grossExam;

    @Schema(description = "镜下检查")
    private String microscopicExam;

    @Schema(description = "最终诊断")
    @Size(max = 2000)
    private String finalDiagnosis;

    @Schema(description = "富文本正文")
    private String richTextContent;

    @Schema(description = "版本化报告版式快照")
    private JsonNode renderSnapshot;

    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;
}
