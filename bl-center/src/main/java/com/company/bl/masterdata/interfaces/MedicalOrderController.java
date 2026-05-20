package com.company.bl.masterdata.interfaces;

import com.company.bl.interfaces.auth.M1PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.masterdata.application.MedicalOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "基础资料", description = "医嘱字典、收费项目与套餐维护接口")
public class MedicalOrderController {

    private final MedicalOrderService medicalOrderService;

    public MedicalOrderController(MedicalOrderService medicalOrderService) {
        this.medicalOrderService = medicalOrderService;
    }

    @Operation(summary = "查询医嘱字典树", description = "查询医嘱分类树及其条目。")
    @RequirePermission(M1PermissionCodes.ORDER_DICT_QUERY)
    @GetMapping("/medical-order-dicts")
    public List<MedicalOrderService.MedicalOrderCategoryNode> listMedicalOrderDicts() {
        return medicalOrderService.listMedicalOrderDicts();
    }

    @Operation(summary = "新增医嘱字典分类", description = "新增医嘱字典分类节点。")
    @RequirePermission(M1PermissionCodes.ORDER_DICT_CREATE)
    @PostMapping("/medical-order-dicts/categories")
    public MedicalOrderService.MedicalOrderCategoryNode createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return medicalOrderService.createMedicalOrderCategory(new MedicalOrderService.CreateMedicalOrderCategoryCommand(
            request.parentId(), request.categoryCode(), request.categoryName(), request.sortOrder(), request.enabled()));
    }

    @Operation(summary = "新增医嘱字典条目", description = "新增医嘱字典叶子条目。")
    @RequirePermission(M1PermissionCodes.ORDER_DICT_CREATE)
    @PostMapping("/medical-order-dicts/items")
    public MedicalOrderService.MedicalOrderItemView createItem(@Valid @RequestBody CreateItemRequest request) {
        return medicalOrderService.createMedicalOrderItem(new MedicalOrderService.CreateMedicalOrderItemCommand(
            request.categoryId(), request.orderItemCode(), request.orderItemName(), request.orderType(),
            request.defaultContent(), request.executionScope(), request.sortOrder(), request.enabled()));
    }

    @Operation(summary = "更新医嘱字典条目启用状态", description = "更新指定医嘱条目的启停状态。")
    @RequirePermission(M1PermissionCodes.ORDER_DICT_CREATE)
    @PatchMapping("/medical-order-dicts/items/{id}/enabled")
    public MedicalOrderService.MedicalOrderItemView updateItemEnabled(@Parameter(description = "医嘱条目 ID") @PathVariable("id") String id,
                                                                      @Valid @RequestBody UpdateEnabledRequest request) {
        return medicalOrderService.updateMedicalOrderItemEnabled(id, request.enabled());
    }

    @Operation(summary = "查询收费项目列表", description = "查询全部收费项目列表。")
    @RequirePermission(M1PermissionCodes.ORDER_CHARGE_QUERY)
    @GetMapping("/medical-order-charge-items")
    public List<MedicalOrderService.ChargeItemView> listChargeItems() {
        return medicalOrderService.listChargeItems();
    }

    @Operation(summary = "分页查询收费项目", description = "按分页、关键字、启用状态和医嘱条目筛选收费项目。")
    @RequirePermission(M1PermissionCodes.ORDER_CHARGE_QUERY)
    @GetMapping("/medical-order-charge-items/page")
    public MedicalOrderService.PagedResult<MedicalOrderService.ChargeItemView> listChargeItemsPage(
        @Parameter(description = "页码，从 1 开始") @RequestParam(name = "page", defaultValue = "1") int page,
        @Parameter(description = "每页条数，默认 20") @RequestParam(name = "size", defaultValue = "20") int size,
        @Parameter(description = "是否启用") @RequestParam(name = "enabled", required = false) Boolean enabled,
        @Parameter(description = "关键字") @RequestParam(name = "keyword", required = false) String keyword,
        @Parameter(description = "医嘱条目 ID") @RequestParam(name = "orderDictItemId", required = false) String orderDictItemId) {
        return medicalOrderService.listChargeItemsPage(page, size, enabled, keyword, orderDictItemId);
    }

    @Operation(summary = "新增收费项目", description = "新增收费项目。")
    @RequirePermission(M1PermissionCodes.ORDER_CHARGE_CREATE)
    @PostMapping("/medical-order-charge-items")
    public MedicalOrderService.ChargeItemView createChargeItem(@Valid @RequestBody CreateChargeItemRequest request) {
        return medicalOrderService.createChargeItem(new MedicalOrderService.CreateChargeItemCommand(
            request.orderDictItemId(), request.chargeItemCode(), request.chargeItemName(), request.specification(),
            request.unit(), request.price(), request.sortOrder(), request.enabled()));
    }

    @Operation(summary = "更新收费项目启用状态", description = "更新指定收费项目的启停状态。")
    @RequirePermission(M1PermissionCodes.ORDER_CHARGE_CREATE)
    @PatchMapping("/medical-order-charge-items/{id}/enabled")
    public MedicalOrderService.ChargeItemView updateChargeEnabled(@Parameter(description = "收费项目 ID") @PathVariable("id") String id,
                                                                  @Valid @RequestBody UpdateEnabledRequest request) {
        return medicalOrderService.updateChargeItemEnabled(id, request.enabled());
    }

    @Operation(summary = "查询套餐列表", description = "查询全部套餐及其配置条目。")
    @RequirePermission(M1PermissionCodes.PACKAGE_QUERY)
    @GetMapping("/medical-order-packages")
    public List<MedicalOrderService.PackageView> listPackages() {
        return medicalOrderService.listPackages();
    }

    @Operation(summary = "分页查询套餐", description = "按分页、关键字、启用状态和套餐类型筛选套餐。")
    @RequirePermission(M1PermissionCodes.PACKAGE_QUERY)
    @GetMapping("/medical-order-packages/page")
    public MedicalOrderService.PagedResult<MedicalOrderService.PackageView> listPackagesPage(
        @Parameter(description = "页码，从 1 开始") @RequestParam(name = "page", defaultValue = "1") int page,
        @Parameter(description = "每页条数，默认 20") @RequestParam(name = "size", defaultValue = "20") int size,
        @Parameter(description = "是否启用") @RequestParam(name = "enabled", required = false) Boolean enabled,
        @Parameter(description = "关键字") @RequestParam(name = "keyword", required = false) String keyword,
        @Parameter(description = "套餐类型") @RequestParam(name = "packageType", required = false) String packageType) {
        return medicalOrderService.listPackagesPage(page, size, enabled, keyword, packageType);
    }

    @Operation(summary = "新增套餐", description = "新增医嘱套餐。")
    @RequirePermission(M1PermissionCodes.PACKAGE_CREATE)
    @PostMapping("/medical-order-packages")
    public MedicalOrderService.PackageView createPackage(@Valid @RequestBody CreatePackageRequest request) {
        return medicalOrderService.createPackage(new MedicalOrderService.CreatePackageCommand(
            request.packageCode(), request.packageName(), request.packageType(), request.ownerUserId(),
            request.enabled(), request.remarks(), request.itemIds()));
    }

    @Operation(summary = "更新套餐启用状态", description = "更新指定套餐的启停状态。")
    @RequirePermission(M1PermissionCodes.PACKAGE_CREATE)
    @PatchMapping("/medical-order-packages/{id}/enabled")
    public MedicalOrderService.PackageView updatePackageEnabled(@Parameter(description = "套餐 ID") @PathVariable("id") String id,
                                                                @Valid @RequestBody UpdateEnabledRequest request) {
        return medicalOrderService.updatePackageEnabled(id, request.enabled());
    }

    @Schema(name = "MedicalOrderUpdateEnabledRequest", description = "更新启用状态请求")
    public record UpdateEnabledRequest(@Schema(description = "是否启用") boolean enabled) {
    }

    @Schema(name = "MedicalOrderCreateCategoryRequest", description = "新增医嘱字典分类请求")
    public record CreateCategoryRequest(
        @Schema(description = "父级分类 ID，根节点可为空")
        String parentId,
        @Schema(description = "分类编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Category code must not be blank")
        @Size(max = 64, message = "Category code must not exceed 64 characters")
        String categoryCode,
        @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Category name must not be blank")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String categoryName,
        @Schema(description = "排序号")
        int sortOrder,
        @Schema(description = "是否启用")
        boolean enabled
    ) {
    }

    @Schema(name = "MedicalOrderCreateItemRequest", description = "新增医嘱字典条目请求")
    public record CreateItemRequest(
        @Schema(description = "所属分类 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Category id must not be blank")
        String categoryId,
        @Schema(description = "医嘱条目编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Order item code must not be blank")
        @Size(max = 64, message = "Order item code must not exceed 64 characters")
        String orderItemCode,
        @Schema(description = "医嘱条目名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Order item name must not be blank")
        @Size(max = 100, message = "Order item name must not exceed 100 characters")
        String orderItemName,
        @Schema(description = "医嘱类型")
        @Size(max = 50, message = "Order type must not exceed 50 characters")
        String orderType,
        @Schema(description = "默认内容")
        @Size(max = 1000, message = "Default content must not exceed 1000 characters")
        String defaultContent,
        @Schema(description = "执行范围")
        @Size(max = 50, message = "Execution scope must not exceed 50 characters")
        String executionScope,
        @Schema(description = "排序号")
        int sortOrder,
        @Schema(description = "是否启用")
        boolean enabled
    ) {
    }

    @Schema(name = "CreateChargeItemRequest", description = "新增收费项目请求")
    public record CreateChargeItemRequest(
        @Schema(description = "关联医嘱条目 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Order dict item id must not be blank")
        String orderDictItemId,
        @Schema(description = "收费项目编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Charge item code must not be blank")
        @Size(max = 64, message = "Charge item code must not exceed 64 characters")
        String chargeItemCode,
        @Schema(description = "收费项目名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Charge item name must not be blank")
        @Size(max = 100, message = "Charge item name must not exceed 100 characters")
        String chargeItemName,
        @Schema(description = "规格")
        @Size(max = 100, message = "Specification must not exceed 100 characters")
        String specification,
        @Schema(description = "计量单位")
        @Size(max = 32, message = "Unit must not exceed 32 characters")
        String unit,
        @Schema(description = "价格，最小为 0")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must not be negative")
        BigDecimal price,
        @Schema(description = "排序号")
        int sortOrder,
        @Schema(description = "是否启用")
        boolean enabled
    ) {
    }

    @Schema(name = "CreatePackageRequest", description = "新增医嘱套餐请求")
    public record CreatePackageRequest(
        @Schema(description = "套餐编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Package code must not be blank")
        @Size(max = 64, message = "Package code must not exceed 64 characters")
        String packageCode,
        @Schema(description = "套餐名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Package name must not be blank")
        @Size(max = 100, message = "Package name must not exceed 100 characters")
        String packageName,
        @Schema(description = "套餐类型")
        @Size(max = 50, message = "Package type must not exceed 50 characters")
        String packageType,
        @Schema(description = "负责人用户 ID")
        String ownerUserId,
        @Schema(description = "是否启用")
        boolean enabled,
        @Schema(description = "备注")
        @Size(max = 500, message = "Remarks must not exceed 500 characters")
        String remarks,
        @Schema(description = "套餐条目 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "Package items must not be empty")
        List<String> itemIds
    ) {
    }
}
