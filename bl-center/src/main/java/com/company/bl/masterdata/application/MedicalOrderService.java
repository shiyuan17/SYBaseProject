package com.company.bl.masterdata.application;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MedicalOrderService {

    private final MedicalOrderDictionaryService medicalOrderDictionaryService;
    private final MedicalOrderChargeService medicalOrderChargeService;
    private final MedicalOrderPackageService medicalOrderPackageService;

    public MedicalOrderService(MedicalOrderDictionaryService medicalOrderDictionaryService,
                               MedicalOrderChargeService medicalOrderChargeService,
                               MedicalOrderPackageService medicalOrderPackageService) {
        this.medicalOrderDictionaryService = medicalOrderDictionaryService;
        this.medicalOrderChargeService = medicalOrderChargeService;
        this.medicalOrderPackageService = medicalOrderPackageService;
    }

    @Cacheable("medicalOrderDictTree")
    @Transactional(readOnly = true)
    public List<MedicalOrderCategoryNode> listMedicalOrderDicts() {
        return medicalOrderDictionaryService.listMedicalOrderDicts();
    }

    @CacheEvict(value = "medicalOrderDictTree", allEntries = true)
    @Transactional
    public MedicalOrderCategoryNode createMedicalOrderCategory(CreateMedicalOrderCategoryCommand command) {
        return medicalOrderDictionaryService.createMedicalOrderCategory(command);
    }

    @CacheEvict(value = "medicalOrderDictTree", allEntries = true)
    @Transactional
    public MedicalOrderCategoryNode updateMedicalOrderCategory(String id, UpdateMedicalOrderCategoryCommand command) {
        return medicalOrderDictionaryService.updateMedicalOrderCategory(id, command);
    }

    @CacheEvict(value = "medicalOrderDictTree", allEntries = true)
    @Transactional
    public MedicalOrderItemView createMedicalOrderItem(CreateMedicalOrderItemCommand command) {
        return medicalOrderDictionaryService.createMedicalOrderItem(command);
    }

    @CacheEvict(value = "medicalOrderDictTree", allEntries = true)
    @Transactional
    public MedicalOrderItemView updateMedicalOrderItem(String id, UpdateMedicalOrderItemCommand command) {
        return medicalOrderDictionaryService.updateMedicalOrderItem(id, command);
    }

    @CacheEvict(value = "medicalOrderDictTree", allEntries = true)
    @Transactional
    public MedicalOrderItemView updateMedicalOrderItemEnabled(String id, boolean enabled) {
        return medicalOrderDictionaryService.updateMedicalOrderItemEnabled(id, enabled);
    }

    @CacheEvict(value = "medicalOrderDictTree", allEntries = true)
    @Transactional
    public void deleteMedicalOrderCategory(String id) {
        medicalOrderDictionaryService.deleteMedicalOrderCategory(id);
    }

    @CacheEvict(value = "medicalOrderDictTree", allEntries = true)
    @Transactional
    public void deleteMedicalOrderItem(String id) {
        medicalOrderDictionaryService.deleteMedicalOrderItem(id);
    }

    @Cacheable("medicalOrderChargeItems")
    @Transactional(readOnly = true)
    public List<ChargeItemView> listChargeItems() {
        return medicalOrderChargeService.listChargeItems();
    }

    @Transactional(readOnly = true)
    public PagedResult<ChargeItemView> listChargeItemsPage(int page, int size, Boolean enabled, String keyword, String orderDictItemId) {
        return medicalOrderChargeService.listChargeItemsPage(page, size, enabled, keyword, orderDictItemId);
    }

    @CacheEvict(value = "medicalOrderChargeItems", allEntries = true)
    @Transactional
    public ChargeItemView createChargeItem(CreateChargeItemCommand command) {
        return medicalOrderChargeService.createChargeItem(command);
    }

    @CacheEvict(value = "medicalOrderChargeItems", allEntries = true)
    @Transactional
    public ChargeItemView updateChargeItem(String id, UpdateChargeItemCommand command) {
        return medicalOrderChargeService.updateChargeItem(id, command);
    }

    @CacheEvict(value = "medicalOrderChargeItems", allEntries = true)
    @Transactional
    public ChargeItemView updateChargeItemEnabled(String id, boolean enabled) {
        return medicalOrderChargeService.updateChargeItemEnabled(id, enabled);
    }

    @CacheEvict(value = "medicalOrderChargeItems", allEntries = true)
    @Transactional
    public void deleteChargeItem(String id) {
        medicalOrderChargeService.deleteChargeItem(id);
    }

    @Transactional(readOnly = true)
    public byte[] exportChargeItems(Boolean enabled, String keyword, String orderDictItemId) {
        return medicalOrderChargeService.exportChargeItems(enabled, keyword, orderDictItemId);
    }

    @Transactional
    public ImportResult importChargeItems(byte[] content) {
        return medicalOrderChargeService.importChargeItems(content);
    }

    @Transactional(readOnly = true)
    public List<PackageView> listPackages() {
        return medicalOrderPackageService.listPackages();
    }

    @Transactional(readOnly = true)
    public PagedResult<PackageView> listPackagesPage(int page, int size, Boolean enabled, String keyword, String packageType) {
        return medicalOrderPackageService.listPackagesPage(page, size, enabled, keyword, packageType);
    }

    @Transactional
    public PackageView createPackage(CreatePackageCommand command) {
        return medicalOrderPackageService.createPackage(command);
    }

    @Transactional
    public PackageView updatePackage(String id, UpdatePackageCommand command) {
        return medicalOrderPackageService.updatePackage(id, command);
    }

    @Transactional
    public PackageView updatePackageEnabled(String id, boolean enabled) {
        return medicalOrderPackageService.updatePackageEnabled(id, enabled);
    }

    @Transactional
    public void deletePackage(String id) {
        medicalOrderPackageService.deletePackage(id);
    }

    @Schema(name = "MedicalOrderCategoryNode")
    public record MedicalOrderCategoryNode(
        String id,
        String parentId,
        String categoryCode,
        String categoryName,
        int sortOrder,
        boolean enabled,
        List<MedicalOrderCategoryNode> children,
        List<MedicalOrderItemView> items
    ) {
    }

    @Schema(name = "MedicalOrderItemView")
    public record MedicalOrderItemView(
        String id,
        String categoryId,
        String orderItemCode,
        String orderItemName,
        String orderType,
        String defaultContent,
        String executionScope,
        int sortOrder,
        boolean enabled
    ) {
    }

    public record CreateMedicalOrderCategoryCommand(String parentId, String categoryCode, String categoryName, int sortOrder, boolean enabled) {
    }

    public record UpdateMedicalOrderCategoryCommand(String parentId, String categoryCode, String categoryName, int sortOrder, boolean enabled) {
    }

    public record CreateMedicalOrderItemCommand(
        String categoryId,
        String orderItemCode,
        String orderItemName,
        String orderType,
        String defaultContent,
        String executionScope,
        int sortOrder,
        boolean enabled
    ) {
    }

    public record UpdateMedicalOrderItemCommand(
        String categoryId,
        String orderItemCode,
        String orderItemName,
        String orderType,
        String defaultContent,
        String executionScope,
        int sortOrder,
        boolean enabled
    ) {
    }

    @Schema(name = "ChargeItemView")
    public record ChargeItemView(
        String id,
        String orderDictItemId,
        String orderItemName,
        String chargeItemCode,
        String chargeItemName,
        String specification,
        String unit,
        BigDecimal price,
        int sortOrder,
        boolean enabled
    ) {
    }

    @Schema(name = "MedicalOrderPagedResult")
    public record PagedResult<T>(List<T> items, int page, int size, long total) {
    }

    public record CreateChargeItemCommand(
        String orderDictItemId,
        String chargeItemCode,
        String chargeItemName,
        String specification,
        String unit,
        BigDecimal price,
        int sortOrder,
        boolean enabled
    ) {
    }

    public record UpdateChargeItemCommand(
        String orderDictItemId,
        String chargeItemCode,
        String chargeItemName,
        String specification,
        String unit,
        BigDecimal price,
        int sortOrder,
        boolean enabled
    ) {
    }

    @Schema(name = "PackageView")
    public record PackageView(
        String id,
        String packageCode,
        String packageName,
        String packageType,
        String ownerUserId,
        boolean enabled,
        String remarks,
        List<PackageItemView> items
    ) {
    }

    @Schema(name = "PackageItemView")
    public record PackageItemView(
        String id,
        String packageId,
        String orderItemId,
        String orderItemCode,
        String orderItemName,
        int sortOrder,
        String remarks
    ) {
    }

    public record CreatePackageCommand(
        String packageCode,
        String packageName,
        String packageType,
        String ownerUserId,
        boolean enabled,
        String remarks,
        List<String> itemIds
    ) {
    }

    public record UpdatePackageCommand(
        String packageCode,
        String packageName,
        String packageType,
        String ownerUserId,
        boolean enabled,
        String remarks,
        List<String> itemIds
    ) {
    }

    @Schema(name = "MedicalOrderImportResult")
    public record ImportResult(int successCount, int failureCount, List<ImportError> errors) {
    }

    @Schema(name = "MedicalOrderImportError")
    public record ImportError(int rowNumber, String field, String rejectedValue, String message) {
    }
}
