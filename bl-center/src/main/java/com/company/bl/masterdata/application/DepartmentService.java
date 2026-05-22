package com.company.bl.masterdata.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.support.application.NumberingService;
import com.company.bl.masterdata.infrastructure.DepartmentJdbcRepository;
import com.company.bl.support.application.OperationAuditService;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    private final DepartmentJdbcRepository departmentJdbcRepository;
    private final NumberingService numberingService;
    private final OperationAuditService operationAuditService;

    public DepartmentService(DepartmentJdbcRepository departmentJdbcRepository,
                             NumberingService numberingService,
                             OperationAuditService operationAuditService) {
        this.departmentJdbcRepository = departmentJdbcRepository;
        this.numberingService = numberingService;
        this.operationAuditService = operationAuditService;
    }

    @Cacheable("departmentTree")
    @Transactional(readOnly = true)
    public List<DepartmentNode> listDepartments() {
        List<DepartmentJdbcRepository.DepartmentRow> rows = departmentJdbcRepository.findDepartments();
        var nodes = rows.stream().map(this::toNode)
            .collect(Collectors.toMap(DepartmentNode::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<DepartmentNode> roots = new ArrayList<>();
        nodes.values().forEach(node -> {
            if (node.parentId() == null) {
                roots.add(node);
            } else {
                DepartmentNode parent = nodes.get(node.parentId());
                if (parent != null) {
                    parent.children().add(node);
                } else {
                    roots.add(node);
                }
            }
        });
        return roots;
    }

    @CacheEvict(value = "departmentTree", allEntries = true)
    @Transactional
    public DepartmentNode createDepartment(CreateDepartmentCommand command) {
        String departmentCode = resolveCreateCode(
            command.departmentCode(),
            numberingService::generateDepartmentCode);
        return operationAuditService.audit("MASTERDATA", "DEPARTMENT", "create_department", () -> {
            try {
                var row = departmentJdbcRepository.insertDepartment(new DepartmentJdbcRepository.CreateDepartmentRow(
                    "DEPT-" + UUID.randomUUID(),
                    command.parentId(),
                    departmentCode,
                    command.departmentName(),
                    command.sortOrder(),
                    command.enabled(),
                    LocalDateTime.now(),
                    LocalDateTime.now()));
                return toNode(row);
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Department code already exists");
            }
        }, DepartmentNode::id, () -> departmentCode);
    }

    @CacheEvict(value = "departmentTree", allEntries = true)
    @Transactional
    public DepartmentNode updateDepartment(String id, UpdateDepartmentCommand command) {
        return operationAuditService.audit("MASTERDATA", "DEPARTMENT", "update_department", () -> {
            DepartmentJdbcRepository.DepartmentRow current = departmentJdbcRepository.findDepartmentById(id);
            if (current == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Department not found");
            }
            String departmentCode = resolveExistingCode(
                command.departmentCode(),
                current.departmentCode(),
                "Department code");
            try {
                departmentJdbcRepository.updateDepartment(id, new DepartmentJdbcRepository.UpdateDepartmentRow(
                    command.parentId(),
                    departmentCode,
                    command.departmentName(),
                    command.sortOrder(),
                    command.enabled()));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Department code already exists");
            }
            return toNode(departmentJdbcRepository.findDepartmentById(id));
        }, DepartmentNode::id, () -> id);
    }

    @CacheEvict(value = "departmentTree", allEntries = true)
    @Transactional
    public DepartmentNode updateDepartmentEnabled(String id, boolean enabled) {
        return operationAuditService.audit("MASTERDATA", "DEPARTMENT", "update_department_enabled", () -> {
            if (departmentJdbcRepository.findDepartmentById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Department not found");
            }
            departmentJdbcRepository.updateDepartmentEnabled(id, enabled);
            return toNode(departmentJdbcRepository.findDepartmentById(id));
        }, DepartmentNode::id, () -> id);
    }

    @CacheEvict(value = "departmentTree", allEntries = true)
    @Transactional
    public void deleteDepartment(String id) {
        operationAuditService.audit("MASTERDATA", "DEPARTMENT", "delete_department", () -> {
            if (departmentJdbcRepository.findDepartmentById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Department not found");
            }
            if (departmentJdbcRepository.countChildren(id) > 0) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Department still has child nodes");
            }
            if (departmentJdbcRepository.countUserReferences(id) > 0) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Department is referenced by users");
            }
            departmentJdbcRepository.deleteDepartment(id);
            return id;
        }, value -> id, () -> id);
    }

    private DepartmentNode toNode(DepartmentJdbcRepository.DepartmentRow row) {
        return new DepartmentNode(
            row.id(),
            row.parentId(),
            row.departmentCode(),
            row.departmentName(),
            row.sortOrder(),
            row.enabled(),
            new ArrayList<>());
    }

    @Schema(name = "DepartmentNode", description = "科室树节点")
    public record DepartmentNode(
        @Schema(description = "科室 ID") String id,
        @Schema(description = "父级科室 ID") String parentId,
        @Schema(description = "科室编码") String departmentCode,
        @Schema(description = "科室名称") String departmentName,
        @Schema(description = "排序号") int sortOrder,
        @Schema(description = "是否启用") boolean enabled,
        @Schema(description = "子节点列表") List<DepartmentNode> children
    ) {
    }

    public record CreateDepartmentCommand(
        String parentId,
        String departmentCode,
        String departmentName,
        int sortOrder,
        boolean enabled
    ) {
    }

    public record UpdateDepartmentCommand(
        String parentId,
        String departmentCode,
        String departmentName,
        int sortOrder,
        boolean enabled
    ) {
    }

    private String resolveCreateCode(String requestedCode, Supplier<String> generator) {
        String normalizedCode = normalizeCode(requestedCode);
        return normalizedCode == null ? generator.get() : normalizedCode;
    }

    private String resolveExistingCode(String requestedCode, String existingCode, String fieldLabel) {
        String normalizedCode = normalizeCode(requestedCode);
        if (normalizedCode == null || normalizedCode.equals(existingCode)) {
            return existingCode;
        }
        throw new BlBusinessException(
            BlErrorCode.INVALID_ARGUMENT,
            400,
            fieldLabel + " cannot be changed once created");
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
