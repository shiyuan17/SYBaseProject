package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(name = "GrossingDraftRequest", description = "取材整单草稿保存请求")
@RejectLegacyOperatorFields
public class GrossingDraftRequest {

    @NotBlank
    @Size(max = 64)
    private String caseId;

    @Size(max = 64)
    private String terminalCode;

    @Size(max = 500)
    private String remarks;

    @Valid
    private List<SpecimenItem> specimens;

    @Getter
    @Setter
    public static class SpecimenItem {
        @Size(max = 64)
        private String specimenId;
        @Size(max = 100)
        private String specimenType;
        @Size(max = 64)
        private String bodyPartId;
        @Size(max = 64)
        private String samplingTemplateId;
        @Size(max = 100)
        private String sizeText;
        @Size(max = 500)
        private String cutSurfaceFeature;
        @Size(max = 500)
        private String marginMarking;
        private Integer blockCount;
        private String grossDescription;
        @Valid
        private List<BlockItem> blocks;
        @Valid
        private List<MediaAssetItem> mediaAssets;
        @Valid
        private List<EmbeddingBoxItem> embeddingBoxes;
    }

    @Getter
    @Setter
    public static class BlockItem {
        @Size(max = 200)
        private String blockSite;
        @Size(max = 1000)
        private String blockDescription;
        @Size(max = 500)
        private String specialRequirement;
    }

    @Getter
    @Setter
    public static class EmbeddingBoxItem {
        private Integer sequenceNo;
        @Size(max = 100)
        private String boxName;
        @Size(max = 64)
        private String embeddingBoxNo;
        @Size(max = 16)
        private String status;
        @Size(max = 500)
        private String embeddingRemarks;
    }

    @Getter
    @Setter
    public static class MediaAssetItem {
        @Size(max = 1000)
        private String fileUrl;
        @Size(max = 255)
        private String fileName;
    }
}
