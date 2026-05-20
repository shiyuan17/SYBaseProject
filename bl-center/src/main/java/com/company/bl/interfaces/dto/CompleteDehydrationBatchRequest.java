package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(name = "CompleteDehydrationBatchRequest", description = "脱水批次完成请求")
public class CompleteDehydrationBatchRequest extends BatchOperatorRequest {

    @Schema(description = "附件列表")
    @Valid
    private List<GrossingCompleteRequest.MediaAssetItem> mediaAssets;
}
