package com.company.bl.interfaces.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CompleteDehydrationBatchRequest extends BatchOperatorRequest {

    @Valid
    private List<GrossingCompleteRequest.MediaAssetItem> mediaAssets;
}
