package com.company.bl.masterdata.interfaces;

import com.company.bl.interfaces.auth.M1PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.masterdata.application.SamplingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SamplingController {

    private final SamplingService samplingService;

    public SamplingController(SamplingService samplingService) {
        this.samplingService = samplingService;
    }

    @RequirePermission(M1PermissionCodes.TEMPLATE_QUERY)
    @GetMapping("/sampling-templates")
    public List<SamplingService.TemplateCategoryNode> listSamplingTemplates() {
        return samplingService.listSamplingTemplates();
    }

    @RequirePermission(M1PermissionCodes.TEMPLATE_QUERY)
    @GetMapping("/sampling-templates/{id}")
    public SamplingService.TemplateDetailView getSamplingTemplateDetail(@PathVariable("id") String id) {
        return samplingService.getSamplingTemplateDetail(id);
    }

    @RequirePermission(M1PermissionCodes.TEMPLATE_CREATE)
    @PostMapping("/sampling-templates/categories")
    public SamplingService.TemplateCategoryNode createTemplateCategory(@Valid @RequestBody CreateTemplateCategoryRequest request) {
        return samplingService.createSamplingTemplateCategory(new SamplingService.CreateTemplateCategoryCommand(
            request.parentId(), request.categoryCode(), request.categoryName(), request.sortOrder(), request.enabled()));
    }

    @RequirePermission(M1PermissionCodes.TEMPLATE_CREATE)
    @PostMapping("/sampling-templates")
    public SamplingService.TemplateDetailView createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        return samplingService.createSamplingTemplate(new SamplingService.CreateTemplateCommand(
            request.categoryId(), request.templateCode(), request.templateName(), request.templateContent(),
            request.splitPartCount(), request.applicableSpecimenType(), request.enabled(), request.bodyPartIds()));
    }

    @RequirePermission(M1PermissionCodes.TEMPLATE_CREATE)
    @PatchMapping("/sampling-templates/{id}/enabled")
    public SamplingService.TemplateDetailView updateTemplateEnabled(@PathVariable("id") String id,
                                                                    @Valid @RequestBody UpdateEnabledRequest request) {
        return samplingService.updateSamplingTemplateEnabled(id, request.enabled());
    }

    @RequirePermission(M1PermissionCodes.GUIDELINE_QUERY)
    @GetMapping("/sampling-guidelines")
    public List<SamplingService.GuidelineCategoryNode> listSamplingGuidelines() {
        return samplingService.listSamplingGuidelines();
    }

    @RequirePermission(M1PermissionCodes.GUIDELINE_QUERY)
    @GetMapping("/sampling-guidelines/{id}")
    public SamplingService.GuidelineDetailView getSamplingGuidelineDetail(@PathVariable("id") String id) {
        return samplingService.getSamplingGuidelineDetail(id);
    }

    @RequirePermission(M1PermissionCodes.GUIDELINE_CREATE)
    @PostMapping("/sampling-guidelines/categories")
    public SamplingService.GuidelineCategoryNode createGuidelineCategory(@Valid @RequestBody CreateGuidelineCategoryRequest request) {
        return samplingService.createGuidelineCategory(new SamplingService.CreateGuidelineCategoryCommand(
            request.parentId(), request.categoryCode(), request.categoryName(), request.sortOrder(), request.enabled()));
    }

    @RequirePermission(M1PermissionCodes.GUIDELINE_CREATE)
    @PostMapping("/sampling-guidelines")
    public SamplingService.GuidelineDetailView createGuideline(@Valid @RequestBody CreateGuidelineRequest request) {
        return samplingService.createGuideline(new SamplingService.CreateGuidelineCommand(
            request.categoryId(), request.guidelineCode(), request.guidelineName(), request.guidelineContent(),
            request.versionNo(), request.enabled()));
    }

    @RequirePermission(M1PermissionCodes.GUIDELINE_CREATE)
    @PatchMapping("/sampling-guidelines/{id}/enabled")
    public SamplingService.GuidelineDetailView updateGuidelineEnabled(@PathVariable("id") String id,
                                                                      @Valid @RequestBody UpdateEnabledRequest request) {
        return samplingService.updateGuidelineEnabled(id, request.enabled());
    }

    public record UpdateEnabledRequest(boolean enabled) {
    }

    public record CreateTemplateCategoryRequest(
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

    public record CreateTemplateRequest(
        @NotBlank(message = "Category id must not be blank")
        String categoryId,
        @NotBlank(message = "Template code must not be blank")
        @Size(max = 64, message = "Template code must not exceed 64 characters")
        String templateCode,
        @NotBlank(message = "Template name must not be blank")
        @Size(max = 100, message = "Template name must not exceed 100 characters")
        String templateName,
        String templateContent,
        @Min(value = 1, message = "Split part count must be at least 1")
        int splitPartCount,
        @Size(max = 100, message = "Applicable specimen type must not exceed 100 characters")
        String applicableSpecimenType,
        boolean enabled,
        List<String> bodyPartIds
    ) {
    }

    public record CreateGuidelineCategoryRequest(
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

    public record CreateGuidelineRequest(
        @NotBlank(message = "Category id must not be blank")
        String categoryId,
        @NotBlank(message = "Guideline code must not be blank")
        @Size(max = 64, message = "Guideline code must not exceed 64 characters")
        String guidelineCode,
        @NotBlank(message = "Guideline name must not be blank")
        @Size(max = 100, message = "Guideline name must not exceed 100 characters")
        String guidelineName,
        String guidelineContent,
        @Size(max = 32, message = "Version must not exceed 32 characters")
        String versionNo,
        boolean enabled
    ) {
    }
}
