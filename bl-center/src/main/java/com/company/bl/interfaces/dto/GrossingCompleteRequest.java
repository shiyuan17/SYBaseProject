package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(name = "GrossingCompleteRequest", description = "取材完成请求")
@RejectLegacyOperatorFields
public class GrossingCompleteRequest {

    @Schema(description = "技术任务 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String taskId;

    @Schema(description = "病例 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String caseId;



    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;

    @Schema(description = "取材标本明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty
    private List<SpecimenItem> specimens;

    @Getter
    @Setter
    @Schema(name = "GrossingSpecimenItem", description = "取材标本明细")
    public static class SpecimenItem {
        @Schema(description = "标本 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 64)
        private String specimenId;

        @Schema(description = "标本类型", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        private String specimenType;

        @Schema(description = "部位 ID")
        @Size(max = 64)
        private String bodyPartId;

        @Schema(description = "取材模板 ID")
        @Size(max = 64)
        private String samplingTemplateId;

        @Schema(description = "大小")
        @Size(max = 100)
        private String sizeText;

        @Schema(description = "切面特征")
        @Size(max = 500)
        private String cutSurfaceFeature;

        @Schema(description = "切缘标记")
        @Size(max = 500)
        private String marginMarking;

        @Schema(description = "取材块数")
        private Integer blockCount;

        @Schema(description = "大体描述")
        private String grossDescription;

        @Schema(description = "蜡块明细", requiredMode = Schema.RequiredMode.REQUIRED)
        @Valid
        @NotEmpty
        private List<BlockItem> blocks;

        @Schema(description = "附件列表")
        @Valid
        private List<MediaAssetItem> mediaAssets;

        @Schema(description = "取材阶段预确认包埋盒列表")
        @Valid
        private List<EmbeddingBoxItem> embeddingBoxes;
    }

    @Getter
    @Setter
    @Schema(name = "GrossingBlockItem", description = "取材蜡块明细")
    public static class BlockItem {
        @Schema(description = "蜡块部位")
        @Size(max = 200)
        private String blockSite;

        @Schema(description = "蜡块描述")
        @Size(max = 1000)
        private String blockDescription;

        @Schema(description = "特殊要求")
        @Size(max = 500)
        private String specialRequirement;
    }

    @Getter
    @Setter
    @Schema(name = "GrossingEmbeddingBoxItem", description = "取材阶段预确认包埋盒")
    public static class EmbeddingBoxItem {
        @Schema(description = "序号", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer sequenceNo;

        @Schema(description = "盒名称")
        @Size(max = 100)
        private String boxName;

        @Schema(description = "盒号", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 64)
        private String embeddingBoxNo;

        @Schema(description = "确认状态，PENDING/CONFIRMED", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Pattern(regexp = "PENDING|CONFIRMED")
        private String status;

        @Schema(description = "包埋备注")
        @Size(max = 500)
        private String embeddingRemarks;
    }

    @Getter
    @Setter
    @Schema(name = "MediaAssetItem", description = "附件信息")
    public static class MediaAssetItem {
        @Schema(description = "附件 URL", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 1000)
        private String fileUrl;

        @Schema(description = "附件名称")
        @Size(max = 255)
        private String fileName;
    }
}
