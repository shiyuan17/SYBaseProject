package com.company.bl.masterdata.interfaces;

import com.company.bl.interfaces.auth.M1PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.masterdata.application.SystemConfigService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system-configs")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @RequirePermission(M1PermissionCodes.CONFIG_QUERY)
    @GetMapping
    public List<SystemConfigService.ConfigCategoryNode> listSystemConfigs() {
        return systemConfigService.listSystemConfigs();
    }

    @RequirePermission(M1PermissionCodes.CONFIG_UPDATE)
    @PostMapping("/categories")
    public SystemConfigService.ConfigCategoryNode createConfigCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return systemConfigService.createConfigCategory(new SystemConfigService.CreateConfigCategoryCommand(
            request.parentId(), request.categoryCode(), request.categoryName(), request.categoryType(),
            request.sortOrder(), request.enabled()));
    }

    @RequirePermission(M1PermissionCodes.CONFIG_UPDATE)
    @PostMapping("/items")
    public SystemConfigService.ConfigItemView createConfigItem(@Valid @RequestBody CreateItemRequest request) {
        return systemConfigService.createConfigItem(new SystemConfigService.CreateConfigItemCommand(
            request.categoryId(), request.configKey(), request.configName(), request.configValue(),
            request.valueType(), request.sortOrder(), request.enabled(), request.remarks()));
    }

    @RequirePermission(M1PermissionCodes.CONFIG_UPDATE)
    @PatchMapping("/items/{id}")
    public SystemConfigService.ConfigItemView updateConfigItem(@PathVariable("id") String id,
                                                               @Valid @RequestBody UpdateItemRequest request) {
        return systemConfigService.updateConfigItem(id, new SystemConfigService.UpdateConfigItemCommand(
            request.configValue(), request.enabled(), request.remarks()));
    }

    public record CreateCategoryRequest(
        String parentId,
        @NotBlank(message = "Category code must not be blank")
        @Size(max = 64, message = "Category code must not exceed 64 characters")
        String categoryCode,
        @NotBlank(message = "Category name must not be blank")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String categoryName,
        @Size(max = 50, message = "Category type must not exceed 50 characters")
        String categoryType,
        int sortOrder,
        boolean enabled
    ) {
    }

    public record CreateItemRequest(
        @NotBlank(message = "Category id must not be blank")
        String categoryId,
        @NotBlank(message = "Config key must not be blank")
        @Size(max = 100, message = "Config key must not exceed 100 characters")
        String configKey,
        @NotBlank(message = "Config name must not be blank")
        @Size(max = 100, message = "Config name must not exceed 100 characters")
        String configName,
        String configValue,
        @Size(max = 32, message = "Value type must not exceed 32 characters")
        String valueType,
        int sortOrder,
        boolean enabled,
        @Size(max = 500, message = "Remarks must not exceed 500 characters")
        String remarks
    ) {
    }

    public record UpdateItemRequest(
        String configValue,
        boolean enabled,
        @Size(max = 500, message = "Remarks must not exceed 500 characters")
        String remarks
    ) {
    }
}
