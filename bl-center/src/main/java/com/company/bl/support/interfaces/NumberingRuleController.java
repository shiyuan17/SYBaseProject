package com.company.bl.support.interfaces;

import com.company.bl.support.application.NumberingService;
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
public class NumberingRuleController {

    private final NumberingService numberingService;

    public NumberingRuleController(NumberingService numberingService) {
        this.numberingService = numberingService;
    }

    @GetMapping
    public List<NumberingService.NumberingRuleView> listRules() {
        return numberingService.listRules();
    }

    @PatchMapping("/{id}")
    public NumberingService.NumberingRuleView updateRule(@PathVariable("id") String id,
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

    public record UpdateNumberingRuleRequest(
        @Size(max = 64, message = "Prefix pattern must not exceed 64 characters")
        String prefixPattern,
        @Size(max = 32, message = "Date pattern must not exceed 32 characters")
        String datePattern,
        @Min(value = 1, message = "Sequence length must be at least 1")
        @Max(value = 12, message = "Sequence length must be at most 12")
        int seqLength,
        @NotBlank(message = "Reset policy must not be blank")
        @Size(max = 32, message = "Reset policy must not exceed 32 characters")
        String resetPolicy,
        @NotBlank(message = "Scope type must not be blank")
        @Size(max = 32, message = "Scope type must not exceed 32 characters")
        String scopeType,
        boolean enabled,
        @Size(max = 500, message = "Remarks must not exceed 500 characters")
        String remarks
    ) {
    }
}
