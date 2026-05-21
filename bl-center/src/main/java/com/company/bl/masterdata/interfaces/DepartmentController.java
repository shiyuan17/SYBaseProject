package com.company.bl.masterdata.interfaces;

import com.company.bl.interfaces.auth.M1PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.masterdata.application.DepartmentService;
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
@RequestMapping("/api/v1/departments")
@Tag(name = "基础资料", description = "科室树查询与维护接口")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @Operation(summary = "查询科室树", description = "查询系统科室树结构。")
    @RequirePermission(M1PermissionCodes.DEPARTMENT_QUERY)
    @GetMapping
    public List<DepartmentService.DepartmentNode> listDepartments() {
        return departmentService.listDepartments();
    }

    @Operation(summary = "新增科室", description = "新增科室节点。")
    @RequirePermission(M1PermissionCodes.DEPARTMENT_CREATE)
    @PostMapping
    public DepartmentService.DepartmentNode createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        return departmentService.createDepartment(new DepartmentService.CreateDepartmentCommand(
            request.parentId(),
            request.departmentCode(),
            request.departmentName(),
            request.sortOrder(),
            request.enabled()));
    }

    @Operation(summary = "更新科室", description = "更新科室节点基础信息。")
    @RequirePermission(M1PermissionCodes.DEPARTMENT_CREATE)
    @PatchMapping("/{id}")
    public DepartmentService.DepartmentNode updateDepartment(
        @Parameter(description = "科室 ID") @PathVariable("id") String id,
        @Valid @RequestBody UpdateDepartmentRequest request
    ) {
        return departmentService.updateDepartment(id, new DepartmentService.UpdateDepartmentCommand(
            request.parentId(),
            request.departmentCode(),
            request.departmentName(),
            request.sortOrder(),
            request.enabled()));
    }

    @Operation(summary = "更新科室启用状态", description = "更新指定科室节点的启停状态。")
    @RequirePermission(M1PermissionCodes.DEPARTMENT_CREATE)
    @PatchMapping("/{id}/enabled")
    public DepartmentService.DepartmentNode updateDepartmentEnabled(
        @Parameter(description = "科室 ID") @PathVariable("id") String id,
        @Valid @RequestBody UpdateEnabledRequest request
    ) {
        return departmentService.updateDepartmentEnabled(id, request.enabled());
    }

    @Operation(summary = "删除科室", description = "删除无子节点且未被用户引用的科室。")
    @RequirePermission(M1PermissionCodes.DEPARTMENT_CREATE)
    @DeleteMapping("/{id}")
    public void deleteDepartment(@Parameter(description = "科室 ID") @PathVariable("id") String id) {
        departmentService.deleteDepartment(id);
    }

    @Schema(name = "CreateDepartmentRequest", description = "新增科室请求")
    public record CreateDepartmentRequest(
        @Schema(description = "父级科室 ID，根节点可为空")
        String parentId,
        @Schema(description = "科室编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Department code must not be blank")
        @Size(max = 64, message = "Department code must not exceed 64 characters")
        String departmentCode,
        @Schema(description = "科室名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Department name must not be blank")
        @Size(max = 100, message = "Department name must not exceed 100 characters")
        String departmentName,
        @Schema(description = "排序号")
        int sortOrder,
        @Schema(description = "是否启用")
        boolean enabled
    ) {
    }

    @Schema(name = "UpdateDepartmentRequest", description = "更新科室请求")
    public record UpdateDepartmentRequest(
        @Schema(description = "父级科室 ID，根节点可为空")
        String parentId,
        @Schema(description = "科室编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Department code must not be blank")
        @Size(max = 64, message = "Department code must not exceed 64 characters")
        String departmentCode,
        @Schema(description = "科室名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Department name must not be blank")
        @Size(max = 100, message = "Department name must not exceed 100 characters")
        String departmentName,
        @Schema(description = "排序号")
        int sortOrder,
        @Schema(description = "是否启用")
        boolean enabled
    ) {
    }

    @Schema(name = "DepartmentUpdateEnabledRequest", description = "更新科室启用状态请求")
    public record UpdateEnabledRequest(@Schema(description = "是否启用") boolean enabled) {
    }
}
