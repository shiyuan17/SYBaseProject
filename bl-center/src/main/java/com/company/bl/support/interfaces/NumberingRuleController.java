package com.company.bl.support.interfaces;

import com.company.bl.interfaces.auth.M1PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.support.application.NumberingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/numbering-rules")
@Tag(name = "基础资料", description = "业务编号规则查询与维护接口")
public class NumberingRuleController {

    private final NumberingService numberingService;

    public NumberingRuleController(NumberingService numberingService) {
        this.numberingService = numberingService;
    }

    @Operation(summary = "查询编号规则", description = "查询系统当前全部业务编号规则。")
    @RequirePermission(M1PermissionCodes.NUMBERING_QUERY)
    @GetMapping
    public List<NumberingService.NumberingRuleView> listRules() {
        return numberingService.listRules();
    }

    @Operation(summary = "更新编号规则", description = "更新指定编号规则的前缀、日期格式、序号长度与作用域。")
    @RequirePermission(M1PermissionCodes.NUMBERING_UPDATE)
    @PatchMapping("/{id}")
    public NumberingService.NumberingRuleView updateRule(@Parameter(description = "编号规则 ID") @PathVariable("id") String id,
                                                         @Valid @RequestBody UpdateNumberingRuleRequest request) {
        return numberingService.updateRule(id, new NumberingService.UpdateNumberingRuleCommand(
            request.prefixPattern(),
            request.datePattern(),
            request.seqLength(),
            request.resetPolicy(),
            request.scopeType(),
            request.enabled(),
            request.remarks()));
    }

    @Schema(name = "UpdateNumberingRuleRequest", description = "更新编号规则请求")
    public record UpdateNumberingRuleRequest(
        @Schema(description = "前缀模式")
        @Size(max = 64, message = "Prefix pattern must not exceed 64 characters")
        String prefixPattern,
        @Schema(description = "日期模式")
        @Size(max = 32, message = "Date pattern must not exceed 32 characters")
        String datePattern,
        @Schema(description = "流水号长度，取值 1 到 12")
        @Min(value = 1, message = "Sequence length must be at least 1")
        @Max(value = 12, message = "Sequence length must be at most 12")
        int seqLength,
        @Schema(description = "重置策略", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Reset policy must not be blank")
        @Size(max = 32, message = "Reset policy must not exceed 32 characters")
        String resetPolicy,
        @Schema(description = "作用域类型", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Scope type must not be blank")
        @Size(max = 32, message = "Scope type must not exceed 32 characters")
        String scopeType,
        @Schema(description = "是否启用")
        boolean enabled,
        @Schema(description = "备注")
        @Size(max = 500, message = "Remarks must not exceed 500 characters")
        String remarks
    ) {
    }
}
