package com.company.bl.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DirectReceiveSpecimensRequest {

    @Size(max = 64)
    private String receivedByUserId;

    @Size(max = 100)
    private String receivedByName;

    @Size(max = 64)
    private String terminalCode;

    @Valid
    @NotEmpty
    private List<ReceiveSpecimensRequest.ReceiptItem> items;
}
