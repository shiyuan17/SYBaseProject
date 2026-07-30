package com.company.bl.support.interfaces;

import com.company.bl.interfaces.auth.M1PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.support.application.CheckItemRuleService;
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
@RequestMapping("/api/v1/check-item-rules")
@Tag(name = "基础资料", description = "检查项病理号规则查询与维护接口")
public class CheckItemRuleController {

    private final CheckItemRuleService service;

    public CheckItemRuleController(CheckItemRuleService service) {
        this.service = service;
    }

    @Operation(summary = "查询检查项规则")
    @RequirePermission(M1PermissionCodes.NUMBERING_QUERY)
    @GetMapping
    public List<CheckItemRuleService.CheckItemRuleView> listRules() {
        return service.listRules();
    }

    @Operation(summary = "新建检查项规则")
    @RequirePermission(M1PermissionCodes.NUMBERING_UPDATE)
    @PostMapping
    public CheckItemRuleService.CheckItemRuleView createRule(
        @Valid @RequestBody SaveCheckItemRuleRequest request
    ) {
        return service.createRule(request.toCommand());
    }

    @Operation(summary = "修改检查项规则")
    @RequirePermission(M1PermissionCodes.NUMBERING_UPDATE)
    @PatchMapping("/{id}")
    public CheckItemRuleService.CheckItemRuleView updateRule(
        @Parameter(description = "规则 ID") @PathVariable String id,
        @Valid @RequestBody SaveCheckItemRuleRequest request
    ) {
        return service.updateRule(id, request.toCommand());
    }

    @Operation(summary = "删除检查项规则")
    @RequirePermission(M1PermissionCodes.NUMBERING_UPDATE)
    @DeleteMapping("/{id}")
    public void deleteRule(@Parameter(description = "规则 ID") @PathVariable String id) {
        service.deleteRule(id);
    }

    @Schema(name = "SaveCheckItemRuleRequest", description = "检查项规则请求")
    public record SaveCheckItemRuleRequest(
        @Schema(description = "送检类型", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Application type must not be blank")
        @Size(max = 64, message = "Application type must not exceed 64 characters")
        String applicationType,
        @Schema(description = "编号周期 YEAR/MONTH/DAY/NONE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Number period must not be blank")
        String numberPeriod,
        @Schema(description = "是否自增")
        boolean autoIncrement,
        @Schema(description = "前缀规则，支持日期和序号占位符")
        @Size(max = 128, message = "Prefix rule must not exceed 128 characters")
        String prefixRule,
        @Schema(description = "当前已用最大序号")
        @Min(value = 0, message = "Current maximum sequence must not be negative")
        long currentMaxSequence
    ) {
        CheckItemRuleService.SaveCheckItemRuleCommand toCommand() {
            return new CheckItemRuleService.SaveCheckItemRuleCommand(
                applicationType, numberPeriod, autoIncrement, prefixRule, currentMaxSequence);
        }
    }
}
