package com.company.bl.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReceiveSpecimensRequest {

    @NotBlank
    @Size(max = 64)
    private String transportOrderId;

    @Size(max = 64)
    private String receivedByUserId;

    @NotBlank
    @Size(max = 100)
    private String receivedByName;

    @Size(max = 64)
    private String terminalCode;

    @Valid
    @NotEmpty
    private List<ReceiptItem> items;

    @Getter
    @Setter
    public static class ReceiptItem {
        @NotBlank
        @Size(max = 128)
        private String specimenBarcode;

        @NotBlank
        @Size(max = 32)
        private String receiptStatus;

        @NotNull
        private Integer containerCount;

        @Size(max = 500)
        private String reason;

        @Size(max = 500)
        private String remarks;
    }
}
