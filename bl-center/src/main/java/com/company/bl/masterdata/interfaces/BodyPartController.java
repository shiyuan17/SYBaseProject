package com.company.bl.masterdata.interfaces;

import com.company.bl.interfaces.auth.M1PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.masterdata.application.BodyPartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/api/v1/body-parts")
@Tag(name = "基础资料", description = "部位树查询与维护接口")
public class BodyPartController {

    private final BodyPartService bodyPartService;

    public BodyPartController(BodyPartService bodyPartService) {
        this.bodyPartService = bodyPartService;
    }

    @Operation(summary = "查询部位树", description = "查询系统部位树结构。")
    @RequirePermission(M1PermissionCodes.BODY_PART_QUERY)
    @GetMapping
    public List<BodyPartService.BodyPartNode> listBodyParts() {
        return bodyPartService.listBodyParts();
    }

    @Operation(summary = "新增部位", description = "新增部位节点。")
    @RequirePermission(M1PermissionCodes.BODY_PART_CREATE)
    @PostMapping
    public BodyPartService.BodyPartNode createBodyPart(@Valid @RequestBody CreateBodyPartRequest request) {
        return bodyPartService.createBodyPart(new BodyPartService.CreateBodyPartCommand(
            request.parentId(), request.partCode(), request.partName(), request.partAlias(),
            request.partLevel(), request.sortOrder(), request.enabled()));
    }

    @Operation(summary = "更新部位", description = "更新部位节点基础信息。")
    @RequirePermission(M1PermissionCodes.BODY_PART_CREATE)
    @PatchMapping("/{id}")
    public BodyPartService.BodyPartNode updateBodyPart(@Parameter(description = "部位 ID") @PathVariable("id") String id,
                                                       @Valid @RequestBody UpdateBodyPartRequest request) {
        return bodyPartService.updateBodyPart(id, new BodyPartService.UpdateBodyPartCommand(
            request.parentId(), request.partCode(), request.partName(), request.partAlias(),
            request.partLevel(), request.sortOrder(), request.enabled()));
    }

    @Operation(summary = "更新部位启用状态", description = "更新指定部位节点的启停状态。")
    @RequirePermission(M1PermissionCodes.BODY_PART_CREATE)
    @PatchMapping("/{id}/enabled")
    public BodyPartService.BodyPartNode updateBodyPartEnabled(@Parameter(description = "部位 ID") @PathVariable("id") String id,
                                                              @Valid @RequestBody UpdateEnabledRequest request) {
        return bodyPartService.updateBodyPartEnabled(id, request.enabled());
    }

    @Operation(summary = "删除部位", description = "删除无子节点且未被模板引用的部位。")
    @RequirePermission(M1PermissionCodes.BODY_PART_CREATE)
    @DeleteMapping("/{id}")
    public void deleteBodyPart(@Parameter(description = "部位 ID") @PathVariable("id") String id) {
        bodyPartService.deleteBodyPart(id);
    }

    @Schema(name = "CreateBodyPartRequest", description = "新增部位请求")
    public record CreateBodyPartRequest(
        @Schema(description = "父级部位 ID，根节点可为空")
        String parentId,
        @Schema(description = "部位编码，可为空，由系统自动生成")
        @Size(max = 64, message = "Part code must not exceed 64 characters")
        String partCode,
        @Schema(description = "部位名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Part name must not be blank")
        @Size(max = 100, message = "Part name must not exceed 100 characters")
        String partName,
        @Schema(description = "部位别名")
        @Size(max = 100, message = "Part alias must not exceed 100 characters")
        String partAlias,
        @Schema(description = "部位层级，最小为 0")
        @Min(value = 0, message = "Part level must not be negative")
        int partLevel,
        @Schema(description = "排序号")
        int sortOrder,
        @Schema(description = "是否启用")
        boolean enabled
    ) {
    }

    @Schema(name = "UpdateBodyPartRequest", description = "更新部位请求")
    public record UpdateBodyPartRequest(
        @Schema(description = "父级部位 ID，根节点可为空")
        String parentId,
        @Schema(description = "部位编码，创建后不可修改")
        @Size(max = 64, message = "Part code must not exceed 64 characters")
        String partCode,
        @Schema(description = "部位名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Part name must not be blank")
        @Size(max = 100, message = "Part name must not exceed 100 characters")
        String partName,
        @Schema(description = "部位别名")
        @Size(max = 100, message = "Part alias must not exceed 100 characters")
        String partAlias,
        @Schema(description = "部位层级，最小为 0")
        @Min(value = 0, message = "Part level must not be negative")
        int partLevel,
        @Schema(description = "排序号")
        int sortOrder,
        @Schema(description = "是否启用")
        boolean enabled
    ) {
    }

    @Schema(name = "BodyPartUpdateEnabledRequest", description = "更新部位启用状态请求")
    public record UpdateEnabledRequest(@Schema(description = "是否启用") boolean enabled) {
    }
}
