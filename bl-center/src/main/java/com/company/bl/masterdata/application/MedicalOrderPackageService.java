package com.company.bl.masterdata.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.masterdata.infrastructure.MedicalOrderJdbcRepository;
import com.company.bl.masterdata.infrastructure.MedicalOrderPageJdbcRepository;
import com.company.bl.support.application.NumberingService;
import com.company.bl.support.application.OperationAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class MedicalOrderPackageService {

    private final MedicalOrderJdbcRepository repository;
    private final MedicalOrderPageJdbcRepository pageRepository;
    private final NumberingService numberingService;
    private final OperationAuditService operationAuditService;

    public MedicalOrderPackageService(MedicalOrderJdbcRepository repository,
                                      MedicalOrderPageJdbcRepository pageRepository,
                                      NumberingService numberingService,
                                      OperationAuditService operationAuditService) {
        this.repository = repository;
        this.pageRepository = pageRepository;
        this.numberingService = numberingService;
        this.operationAuditService = operationAuditService;
    }

    @Transactional(readOnly = true)
    public List<MedicalOrderService.PackageView> listPackages() {
        var itemMap = repository.findPackageItems().stream().map(this::toPackageItemView)
            .collect(Collectors.groupingBy(MedicalOrderService.PackageItemView::packageId, LinkedHashMap::new, Collectors.toList()));
        return repository.findPackages().stream().map(row -> new MedicalOrderService.PackageView(
            row.id(), row.packageCode(), row.packageName(), row.packageType(), row.ownerUserId(),
            row.enabled(), row.remarks(), itemMap.getOrDefault(row.id(), List.of()))).toList();
    }

    @Transactional(readOnly = true)
    public MedicalOrderService.PagedResult<MedicalOrderService.PackageView> listPackagesPage(
        int page,
        int size,
        Boolean enabled,
        String keyword,
        String packageType
    ) {
        MedicalOrderPageJdbcRepository.PagedPackages pagedPackages =
            pageRepository.findPackagesPage(page, size, enabled, keyword, packageType);
        var packageIds = pagedPackages.items().stream().map(MedicalOrderPageJdbcRepository.PackageRow::id).toList();
        var itemMap = pageRepository.findPackageItemsByPackageIds(packageIds).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream().map(this::toPackageItemView).toList(),
                (left, right) -> left,
                LinkedHashMap::new));
        return new MedicalOrderService.PagedResult<>(
            pagedPackages.items().stream().map(row -> new MedicalOrderService.PackageView(
                row.id(), row.packageCode(), row.packageName(), row.packageType(), row.ownerUserId(),
                row.enabled(), row.remarks(), itemMap.getOrDefault(row.id(), List.of()))).toList(),
            page,
            size,
            pagedPackages.total());
    }

    @Transactional
    public MedicalOrderService.PackageView createPackage(MedicalOrderService.CreatePackageCommand command) {
        String packageCode = resolveCreateCode(command.packageCode(), numberingService::generatePackageCode);
        return operationAuditService.audit("MASTERDATA", "ORDER_PACKAGE", "create_package", () -> {
            try {
                repository.insertPackage(new MedicalOrderJdbcRepository.CreatePackageRow(
                    "PKG-" + UUID.randomUUID(),
                    packageCode,
                    command.packageName(),
                    command.packageType(),
                    command.ownerUserId(),
                    command.enabled(),
                    command.remarks(),
                    command.itemIds(),
                    LocalDateTime.now(),
                    LocalDateTime.now()));
                return listPackages().stream().filter(item -> item.packageCode().equals(packageCode)).findFirst()
                    .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Package not found after create"));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Package code already exists");
            }
        }, MedicalOrderService.PackageView::id, () -> packageCode);
    }

    @Transactional
    public MedicalOrderService.PackageView updatePackage(String id, MedicalOrderService.UpdatePackageCommand command) {
        return operationAuditService.audit("MASTERDATA", "ORDER_PACKAGE", "update_package", () -> {
            MedicalOrderJdbcRepository.PackageRow current = repository.findPackageById(id);
            if (current == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Package not found");
            }
            String packageCode = resolveExistingCode(command.packageCode(), current.packageCode(), "Package code");
            try {
                repository.updatePackage(id, new MedicalOrderJdbcRepository.UpdatePackageRow(
                    packageCode,
                    command.packageName(),
                    command.packageType(),
                    command.ownerUserId(),
                    command.enabled(),
                    command.remarks(),
                    command.itemIds()));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Package code already exists");
            }
            return listPackages().stream().filter(item -> item.id().equals(id)).findFirst()
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Package not found"));
        }, MedicalOrderService.PackageView::id, () -> id);
    }

    @Transactional
    public MedicalOrderService.PackageView updatePackageEnabled(String id, boolean enabled) {
        return operationAuditService.audit("MASTERDATA", "ORDER_PACKAGE", "update_package_enabled", () -> {
            if (repository.findPackageById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Package not found");
            }
            repository.updatePackageEnabled(id, enabled);
            return listPackages().stream().filter(item -> item.id().equals(id)).findFirst()
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Package not found"));
        }, MedicalOrderService.PackageView::id, () -> id);
    }

    @Transactional
    public void deletePackage(String id) {
        operationAuditService.audit("MASTERDATA", "ORDER_PACKAGE", "delete_package", () -> {
            if (repository.findPackageById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Package not found");
            }
            repository.deletePackage(id);
            return id;
        }, value -> id, () -> id);
    }

    private MedicalOrderService.PackageItemView toPackageItemView(MedicalOrderJdbcRepository.PackageItemRow row) {
        return new MedicalOrderService.PackageItemView(
            row.id(), row.packageId(), row.orderItemId(), row.orderItemCode(), row.orderItemName(), row.sortOrder(), row.remarks());
    }

    private MedicalOrderService.PackageItemView toPackageItemView(MedicalOrderPageJdbcRepository.PackageItemRow row) {
        return new MedicalOrderService.PackageItemView(
            row.id(), row.packageId(), row.orderItemId(), row.orderItemCode(), row.orderItemName(), row.sortOrder(), row.remarks());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveCreateCode(String requestedCode, Supplier<String> generator) {
        String normalizedCode = trimToNull(requestedCode);
        return normalizedCode == null ? generator.get() : normalizedCode;
    }

    private String resolveExistingCode(String requestedCode, String existingCode, String fieldLabel) {
        String normalizedCode = trimToNull(requestedCode);
        if (normalizedCode == null || normalizedCode.equals(existingCode)) {
            return existingCode;
        }
        throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, fieldLabel + " cannot be changed once created");
    }
}
