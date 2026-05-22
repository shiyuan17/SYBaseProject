package com.company.bl.masterdata.interfaces;

import com.company.bl.interfaces.auth.M1PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.masterdata.application.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@Tag(name = "基础资料", description = "系统配置分类与配置项维护接口")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @Operation(summary = "查询系统配置树", description = "查询系统配置分类树及配置项。")
    @RequirePermission(M1PermissionCodes.CONFIG_QUERY)
    @GetMapping
    public List<SystemConfigService.ConfigCategoryNode> listSystemConfigs() {
        return systemConfigService.listSystemConfigs();
    }

    @Operation(summary = "新增系统配置分类", description = "新增系统配置分类节点。")
    @RequirePermission(M1PermissionCodes.CONFIG_UPDATE)
    @PostMapping("/categories")
    public SystemConfigService.ConfigCategoryNode createConfigCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return systemConfigService.createConfigCategory(new SystemConfigService.CreateConfigCategoryCommand(
            request.parentId(), request.categoryCode(), request.categoryName(), request.categoryType(),
            request.sortOrder(), request.enabled()));
    }

    @Operation(summary = "更新系统配置分类", description = "更新系统配置分类节点。")
    @RequirePermission(M1PermissionCodes.CONFIG_UPDATE)
    @PatchMapping("/categories/{id}")
    public SystemConfigService.ConfigCategoryNode updateConfigCategory(@Parameter(description = "分类 ID") @PathVariable("id") String id,
                                                                      @Valid @RequestBody UpdateCategoryRequest request) {
        return systemConfigService.updateConfigCategory(id, new SystemConfigService.UpdateConfigCategoryCommand(
            request.parentId(), request.categoryCode(), request.categoryName(), request.categoryType(),
            request.sortOrder(), request.enabled()));
    }

    @Operation(summary = "删除系统配置分类", description = "删除空配置分类。")
    @RequirePermission(M1PermissionCodes.CONFIG_UPDATE)
    @DeleteMapping("/categories/{id}")
    public void deleteConfigCategory(@Parameter(description = "分类 ID") @PathVariable("id") String id) {
        systemConfigService.deleteConfigCategory(id);
    }

    @Operation(summary = "新增系统配置项", description = "新增系统配置项。")
    @RequirePermission(M1PermissionCodes.CONFIG_UPDATE)
    @PostMapping("/items")
    public SystemConfigService.ConfigItemView createConfigItem(@Valid @RequestBody CreateItemRequest request) {
        return systemConfigService.createConfigItem(new SystemConfigService.CreateConfigItemCommand(
            request.categoryId(), request.configKey(), request.configName(), request.configValue(),
            request.valueType(), request.sortOrder(), request.enabled(), request.remarks()));
    }

    @Operation(summary = "更新系统配置项", description = "更新指定系统配置项的值、启用状态与备注。")
    @RequirePermission(M1PermissionCodes.CONFIG_UPDATE)
    @PatchMapping("/items/{id}")
    public SystemConfigService.ConfigItemView updateConfigItem(@Parameter(description = "配置项 ID") @PathVariable("id") String id,
                                                               @Valid @RequestBody UpdateItemRequest request) {
        return systemConfigService.updateConfigItem(id, new SystemConfigService.UpdateConfigItemCommand(
            request.configValue(), request.enabled(), request.remarks()));
    }

    @Operation(summary = "删除系统配置项", description = "删除系统配置项。")
    @RequirePermission(M1PermissionCodes.CONFIG_UPDATE)
    @DeleteMapping("/items/{id}")
    public void deleteConfigItem(@Parameter(description = "配置项 ID") @PathVariable("id") String id) {
        systemConfigService.deleteConfigItem(id);
    }

    @Schema(name = "CreateConfigCategoryRequest", description = "新增系统配置分类请求")
    public record CreateCategoryRequest(
        @Schema(description = "父级分类 ID，根节点可为空")
        String parentId,
        @Schema(description = "分类编码，可为空，由系统自动生成")
        @Size(max = 64, message = "Category code must not exceed 64 characters")
        String categoryCode,
        @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Category name must not be blank")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String categoryName,
        @Schema(description = "分类类型")
        @Size(max = 50, message = "Category type must not exceed 50 characters")
        String categoryType,
        @Schema(description = "排序号")
        int sortOrder,
        @Schema(description = "是否启用")
        boolean enabled
    ) {
    }

    @Schema(name = "UpdateConfigCategoryRequest", description = "更新系统配置分类请求")
    public record UpdateCategoryRequest(
        String parentId,
        @Schema(description = "分类编码，创建后不可修改")
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

    @Schema(name = "CreateConfigItemRequest", description = "新增系统配置项请求")
    public record CreateItemRequest(
        @Schema(description = "所属分类 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Category id must not be blank")
        String categoryId,
        @Schema(description = "配置键", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Config key must not be blank")
        @Size(max = 100, message = "Config key must not exceed 100 characters")
        String configKey,
        @Schema(description = "配置名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Config name must not be blank")
        @Size(max = 100, message = "Config name must not exceed 100 characters")
        String configName,
        @Schema(description = "配置值")
        String configValue,
        @Schema(description = "值类型")
        @Size(max = 32, message = "Value type must not exceed 32 characters")
        String valueType,
        @Schema(description = "排序号")
        int sortOrder,
        @Schema(description = "是否启用")
        boolean enabled,
        @Schema(description = "备注")
        @Size(max = 500, message = "Remarks must not exceed 500 characters")
        String remarks
    ) {
    }

    @Schema(name = "UpdateConfigItemRequest", description = "更新系统配置项请求")
    public record UpdateItemRequest(
        @Schema(description = "配置值")
        String configValue,
        @Schema(description = "是否启用")
        boolean enabled,
        @Schema(description = "备注")
        @Size(max = 500, message = "Remarks must not exceed 500 characters")
        String remarks
    ) {
    }
}
