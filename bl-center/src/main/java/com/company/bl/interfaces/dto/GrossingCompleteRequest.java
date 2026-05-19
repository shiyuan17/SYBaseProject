package com.company.bl.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GrossingCompleteRequest {

    @NotBlank
    @Size(max = 64)
    private String taskId;

    @NotBlank
    @Size(max = 64)
    private String caseId;

    @Size(max = 64)
    private String operatorUserId;

    @NotBlank
    @Size(max = 100)
    private String operatorName;

    @Size(max = 64)
    private String terminalCode;

    @Size(max = 500)
    private String remarks;

    @Valid
    @NotEmpty
    private List<SpecimenItem> specimens;

    @Getter
    @Setter
    public static class SpecimenItem {
        @NotBlank
        @Size(max = 64)
        private String specimenId;

        @NotBlank
        @Size(max = 100)
        private String specimenType;

        @Size(max = 64)
        private String bodyPartId;

        @Size(max = 64)
        private String samplingTemplateId;

        private String grossDescription;

        @Valid
        @NotEmpty
        private List<BlockItem> blocks;

        @Valid
        private List<MediaAssetItem> mediaAssets;
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
    public static class MediaAssetItem {
        @NotBlank
        @Size(max = 1000)
        private String fileUrl;

        @Size(max = 255)
        private String fileName;
    }
}
