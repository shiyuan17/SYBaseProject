package com.company.bl.masterdata.interfaces;

import com.company.bl.masterdata.application.MedicalOrderService;
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
public class MedicalOrderController {

    private final MedicalOrderService medicalOrderService;

    public MedicalOrderController(MedicalOrderService medicalOrderService) {
        this.medicalOrderService = medicalOrderService;
    }

    @GetMapping("/medical-order-dicts")
    public List<MedicalOrderService.MedicalOrderCategoryNode> listMedicalOrderDicts() {
        return medicalOrderService.listMedicalOrderDicts();
    }

    @PostMapping("/medical-order-dicts/categories")
    public MedicalOrderService.MedicalOrderCategoryNode createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return medicalOrderService.createMedicalOrderCategory(new MedicalOrderService.CreateMedicalOrderCategoryCommand(
            request.parentId(), request.categoryCode(), request.categoryName(), request.sortOrder(), request.enabled()));
    }

    @PostMapping("/medical-order-dicts/items")
    public MedicalOrderService.MedicalOrderItemView createItem(@Valid @RequestBody CreateItemRequest request) {
        return medicalOrderService.createMedicalOrderItem(new MedicalOrderService.CreateMedicalOrderItemCommand(
            request.categoryId(), request.orderItemCode(), request.orderItemName(), request.orderType(),
            request.defaultContent(), request.executionScope(), request.sortOrder(), request.enabled()));
    }

    @PatchMapping("/medical-order-dicts/items/{id}/enabled")
    public MedicalOrderService.MedicalOrderItemView updateItemEnabled(@PathVariable("id") String id,
                                                                      @Valid @RequestBody UpdateEnabledRequest request) {
        return medicalOrderService.updateMedicalOrderItemEnabled(id, request.enabled());
    }

    @GetMapping("/medical-order-charge-items")
    public List<MedicalOrderService.ChargeItemView> listChargeItems() {
        return medicalOrderService.listChargeItems();
    }

    @GetMapping("/medical-order-charge-items/page")
    public MedicalOrderService.PagedResult<MedicalOrderService.ChargeItemView> listChargeItemsPage(
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        @RequestParam(name = "enabled", required = false) Boolean enabled,
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "orderDictItemId", required = false) String orderDictItemId) {
        return medicalOrderService.listChargeItemsPage(page, size, enabled, keyword, orderDictItemId);
    }

    @PostMapping("/medical-order-charge-items")
    public MedicalOrderService.ChargeItemView createChargeItem(@Valid @RequestBody CreateChargeItemRequest request) {
        return medicalOrderService.createChargeItem(new MedicalOrderService.CreateChargeItemCommand(
            request.orderDictItemId(), request.chargeItemCode(), request.chargeItemName(), request.specification(),
            request.unit(), request.price(), request.sortOrder(), request.enabled()));
    }

    @PatchMapping("/medical-order-charge-items/{id}/enabled")
    public MedicalOrderService.ChargeItemView updateChargeEnabled(@PathVariable("id") String id,
                                                                  @Valid @RequestBody UpdateEnabledRequest request) {
        return medicalOrderService.updateChargeItemEnabled(id, request.enabled());
    }

    @GetMapping("/medical-order-packages")
    public List<MedicalOrderService.PackageView> listPackages() {
        return medicalOrderService.listPackages();
    }

    @GetMapping("/medical-order-packages/page")
    public MedicalOrderService.PagedResult<MedicalOrderService.PackageView> listPackagesPage(
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        @RequestParam(name = "enabled", required = false) Boolean enabled,
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "packageType", required = false) String packageType) {
        return medicalOrderService.listPackagesPage(page, size, enabled, keyword, packageType);
    }

    @PostMapping("/medical-order-packages")
    public MedicalOrderService.PackageView createPackage(@Valid @RequestBody CreatePackageRequest request) {
        return medicalOrderService.createPackage(new MedicalOrderService.CreatePackageCommand(
            request.packageCode(), request.packageName(), request.packageType(), request.ownerUserId(),
            request.enabled(), request.remarks(), request.itemIds()));
    }

    @PatchMapping("/medical-order-packages/{id}/enabled")
    public MedicalOrderService.PackageView updatePackageEnabled(@PathVariable("id") String id,
                                                                @Valid @RequestBody UpdateEnabledRequest request) {
        return medicalOrderService.updatePackageEnabled(id, request.enabled());
    }

    public record UpdateEnabledRequest(boolean enabled) {
    }

    public record CreateCategoryRequest(
        String parentId,
        @NotBlank(message = "Category code must not be blank")
        @Size(max = 64, message = "Category code must not exceed 64 characters")
        String categoryCode,
        @NotBlank(message = "Category name must not be blank")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String categoryName,
        int sortOrder,
        boolean enabled
    ) {
    }

    public record CreateItemRequest(
        @NotBlank(message = "Category id must not be blank")
        String categoryId,
        @NotBlank(message = "Order item code must not be blank")
        @Size(max = 64, message = "Order item code must not exceed 64 characters")
        String orderItemCode,
        @NotBlank(message = "Order item name must not be blank")
        @Size(max = 100, message = "Order item name must not exceed 100 characters")
        String orderItemName,
        @Size(max = 50, message = "Order type must not exceed 50 characters")
        String orderType,
        @Size(max = 1000, message = "Default content must not exceed 1000 characters")
        String defaultContent,
        @Size(max = 50, message = "Execution scope must not exceed 50 characters")
        String executionScope,
        int sortOrder,
        boolean enabled
    ) {
    }

    public record CreateChargeItemRequest(
        @NotBlank(message = "Order dict item id must not be blank")
        String orderDictItemId,
        @NotBlank(message = "Charge item code must not be blank")
        @Size(max = 64, message = "Charge item code must not exceed 64 characters")
        String chargeItemCode,
        @NotBlank(message = "Charge item name must not be blank")
        @Size(max = 100, message = "Charge item name must not exceed 100 characters")
        String chargeItemName,
        @Size(max = 100, message = "Specification must not exceed 100 characters")
        String specification,
        @Size(max = 32, message = "Unit must not exceed 32 characters")
        String unit,
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must not be negative")
        BigDecimal price,
        int sortOrder,
        boolean enabled
    ) {
    }

    public record CreatePackageRequest(
        @NotBlank(message = "Package code must not be blank")
        @Size(max = 64, message = "Package code must not exceed 64 characters")
        String packageCode,
        @NotBlank(message = "Package name must not be blank")
        @Size(max = 100, message = "Package name must not exceed 100 characters")
        String packageName,
        @Size(max = 50, message = "Package type must not exceed 50 characters")
        String packageType,
        String ownerUserId,
        boolean enabled,
        @Size(max = 500, message = "Remarks must not exceed 500 characters")
        String remarks,
        @NotEmpty(message = "Package items must not be empty")
        List<String> itemIds
    ) {
    }
}
