package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@RejectLegacyOperatorFields
public class CreateEquipmentRecordRequest {

    @NotBlank
    @Size(max = 64)
    private String equipmentCode;

    @NotBlank
    @Size(max = 100)
    private String equipmentName;

    @Size(max = 64)
    private String equipmentCategory;

    @Size(max = 100)
    private String modelNo;

    @NotBlank
    @Size(max = 32)
    private String equipmentStatus;

    @Size(max = 200)
    private String locationDescription;

    @Size(max = 64)
    private String enabledAt;

    @Size(max = 64)
    private String nextMaintenanceAt;

    private Integer quantity;

    @Size(max = 32)
    private String purchaseDate;

    @Size(max = 100)
    private String purchaserName;

    @Size(max = 64)
    private String purchaserCode;

    @Size(max = 100)
    private String managementUnit;

    @Size(max = 64)
    private String managementCode;

    @Size(max = 100)
    private String useUnit;

    @Size(max = 64)
    private String principalCode;

    @Size(max = 100)
    private String principalName;

    @Size(max = 100)
    private String userName;

    @Size(max = 32)
    private String productionDate;

    @Size(max = 32)
    private String warrantyEndDate;

    @Size(max = 100)
    private String factoryNo;

    @Size(max = 100)
    private String depreciationMethod;

    private Integer serviceLifeYears;

    private BigDecimal price;

    @Size(max = 100)
    private String manufacturer;

    @Size(max = 64)
    private String portNo;

    @Size(max = 64)
    private String ipAddress;

    @Size(max = 16)
    private String commonStartupTime;

    @Size(max = 16)
    private String commonShutdownTime;

    @Size(max = 500)
    private String commonUsageContent;

    @NotNull
    private Boolean commonlyUsed;

    private BigDecimal setTemperature;

    private BigDecimal currentTemperature;

    @Size(max = 100)
    private String rfid;

    @Size(max = 500)
    private String remarks;
}
