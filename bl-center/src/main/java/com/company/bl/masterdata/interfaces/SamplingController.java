package com.company.bl.masterdata.interfaces;

import com.company.bl.interfaces.auth.M1PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.masterdata.application.SamplingService;
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
@RequestMapping("/api/v1")
@Tag(name = "基础资料", description = "取材模板与取材规范维护接口")
public class SamplingController {

    private final SamplingService samplingService;

    public SamplingController(SamplingService samplingService) {
        this.samplingService = samplingService;
    }

    @Operation(summary = "查询取材模板树", description = "查询取材模板分类树及模板摘要。")
    @RequirePermission(M1PermissionCodes.TEMPLATE_QUERY)
    @GetMapping("/sampling-templates")
    public List<SamplingService.TemplateCategoryNode> listSamplingTemplates() {
        return samplingService.listSamplingTemplates();
    }

    @Operation(summary = "查询取材模板详情", description = "按模板 ID 查询取材模板详情。")
    @RequirePermission(M1PermissionCodes.TEMPLATE_QUERY)
    @GetMapping("/sampling-templates/{id}")
    public SamplingService.TemplateDetailView getSamplingTemplateDetail(@Parameter(description = "模板 ID") @PathVariable("id") String id) {
        return samplingService.getSamplingTemplateDetail(id);
    }

    @Operation(summary = "新增取材模板分类", description = "新增取材模板分类节点。")
    @RequirePermission(M1PermissionCodes.TEMPLATE_CREATE)
    @PostMapping("/sampling-templates/categories")
    public SamplingService.TemplateCategoryNode createTemplateCategory(@Valid @RequestBody CreateTemplateCategoryRequest request) {
        return samplingService.createSamplingTemplateCategory(new SamplingService.CreateTemplateCategoryCommand(
            request.parentId(), request.categoryCode(), request.categoryName(), request.sortOrder(), request.enabled()));
    }

    @Operation(summary = "更新取材模板分类", description = "更新取材模板分类节点。")
    @RequirePermission(M1PermissionCodes.TEMPLATE_CREATE)
    @PatchMapping("/sampling-templates/categories/{id}")
    public SamplingService.TemplateCategoryNode updateTemplateCategory(@Parameter(description = "分类 ID") @PathVariable("id") String id,
                                                                       @Valid @RequestBody UpdateTemplateCategoryRequest request) {
        return samplingService.updateSamplingTemplateCategory(id, new SamplingService.UpdateTemplateCategoryCommand(
            request.parentId(), request.categoryCode(), request.categoryName(), request.sortOrder(), request.enabled()));
    }

    @Operation(summary = "删除取材模板分类", description = "删除空模板分类。")
    @RequirePermission(M1PermissionCodes.TEMPLATE_CREATE)
    @DeleteMapping("/sampling-templates/categories/{id}")
    public void deleteTemplateCategory(@Parameter(description = "分类 ID") @PathVariable("id") String id) {
        samplingService.deleteSamplingTemplateCategory(id);
    }

    @Operation(summary = "新增取材模板", description = "新增取材模板。")
    @RequirePermission(M1PermissionCodes.TEMPLATE_CREATE)
    @PostMapping("/sampling-templates")
    public SamplingService.TemplateDetailView createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        return samplingService.createSamplingTemplate(new SamplingService.CreateTemplateCommand(
            request.categoryId(), request.templateCode(), request.templateName(), request.templateContent(),
            request.splitPartCount(), request.applicableSpecimenType(), request.enabled(), request.bodyPartIds()));
    }

    @Operation(summary = "更新取材模板", description = "更新取材模板。")
    @RequirePermission(M1PermissionCodes.TEMPLATE_CREATE)
    @PatchMapping("/sampling-templates/{id}")
    public SamplingService.TemplateDetailView updateTemplate(@Parameter(description = "模板 ID") @PathVariable("id") String id,
                                                             @Valid @RequestBody UpdateTemplateRequest request) {
        return samplingService.updateSamplingTemplate(id, new SamplingService.UpdateTemplateCommand(
            request.categoryId(), request.templateCode(), request.templateName(), request.templateContent(),
            request.splitPartCount(), request.applicableSpecimenType(), request.enabled(), request.bodyPartIds()));
    }

    @Operation(summary = "更新取材模板启用状态", description = "更新指定取材模板的启停状态。")
    @RequirePermission(M1PermissionCodes.TEMPLATE_CREATE)
    @PatchMapping("/sampling-templates/{id}/enabled")
    public SamplingService.TemplateDetailView updateTemplateEnabled(@Parameter(description = "模板 ID") @PathVariable("id") String id,
                                                                    @Valid @RequestBody UpdateEnabledRequest request) {
        return samplingService.updateSamplingTemplateEnabled(id, request.enabled());
    }

    @Operation(summary = "删除取材模板", description = "删除取材模板。")
    @RequirePermission(M1PermissionCodes.TEMPLATE_CREATE)
    @DeleteMapping("/sampling-templates/{id}")
    public void deleteTemplate(@Parameter(description = "模板 ID") @PathVariable("id") String id) {
        samplingService.deleteSamplingTemplate(id);
    }

    @Operation(summary = "查询取材规范树", description = "查询取材规范分类树及规范摘要。")
    @RequirePermission(M1PermissionCodes.GUIDELINE_QUERY)
    @GetMapping("/sampling-guidelines")
    public List<SamplingService.GuidelineCategoryNode> listSamplingGuidelines() {
        return samplingService.listSamplingGuidelines();
    }

    @Operation(summary = "查询取材规范详情", description = "按规范 ID 查询取材规范详情。")
    @RequirePermission(M1PermissionCodes.GUIDELINE_QUERY)
    @GetMapping("/sampling-guidelines/{id}")
    public SamplingService.GuidelineDetailView getSamplingGuidelineDetail(@Parameter(description = "规范 ID") @PathVariable("id") String id) {
        return samplingService.getSamplingGuidelineDetail(id);
    }

    @Operation(summary = "新增取材规范分类", description = "新增取材规范分类节点。")
    @RequirePermission(M1PermissionCodes.GUIDELINE_CREATE)
    @PostMapping("/sampling-guidelines/categories")
    public SamplingService.GuidelineCategoryNode createGuidelineCategory(@Valid @RequestBody CreateGuidelineCategoryRequest request) {
        return samplingService.createGuidelineCategory(new SamplingService.CreateGuidelineCategoryCommand(
            request.parentId(), request.categoryCode(), request.categoryName(), request.sortOrder(), request.enabled()));
    }

    @Operation(summary = "更新取材规范分类", description = "更新取材规范分类节点。")
    @RequirePermission(M1PermissionCodes.GUIDELINE_CREATE)
    @PatchMapping("/sampling-guidelines/categories/{id}")
    public SamplingService.GuidelineCategoryNode updateGuidelineCategory(@Parameter(description = "分类 ID") @PathVariable("id") String id,
                                                                         @Valid @RequestBody UpdateGuidelineCategoryRequest request) {
        return samplingService.updateGuidelineCategory(id, new SamplingService.UpdateGuidelineCategoryCommand(
            request.parentId(), request.categoryCode(), request.categoryName(), request.sortOrder(), request.enabled()));
    }

    @Operation(summary = "删除取材规范分类", description = "删除空规范分类。")
    @RequirePermission(M1PermissionCodes.GUIDELINE_CREATE)
    @DeleteMapping("/sampling-guidelines/categories/{id}")
    public void deleteGuidelineCategory(@Parameter(description = "分类 ID") @PathVariable("id") String id) {
        samplingService.deleteGuidelineCategory(id);
    }

    @Operation(summary = "新增取材规范", description = "新增取材规范。")
    @RequirePermission(M1PermissionCodes.GUIDELINE_CREATE)
    @PostMapping("/sampling-guidelines")
    public SamplingService.GuidelineDetailView createGuideline(@Valid @RequestBody CreateGuidelineRequest request) {
        return samplingService.createGuideline(new SamplingService.CreateGuidelineCommand(
            request.categoryId(), request.guidelineCode(), request.guidelineName(), request.guidelineContent(),
            request.versionNo(), request.enabled()));
    }

    @Operation(summary = "更新取材规范", description = "更新取材规范。")
    @RequirePermission(M1PermissionCodes.GUIDELINE_CREATE)
    @PatchMapping("/sampling-guidelines/{id}")
    public SamplingService.GuidelineDetailView updateGuideline(@Parameter(description = "规范 ID") @PathVariable("id") String id,
                                                               @Valid @RequestBody UpdateGuidelineRequest request) {
        return samplingService.updateGuideline(id, new SamplingService.UpdateGuidelineCommand(
            request.categoryId(), request.guidelineCode(), request.guidelineName(), request.guidelineContent(),
            request.versionNo(), request.enabled()));
    }

    @Operation(summary = "更新取材规范启用状态", description = "更新指定取材规范的启停状态。")
    @RequirePermission(M1PermissionCodes.GUIDELINE_CREATE)
    @PatchMapping("/sampling-guidelines/{id}/enabled")
    public SamplingService.GuidelineDetailView updateGuidelineEnabled(@Parameter(description = "规范 ID") @PathVariable("id") String id,
                                                                      @Valid @RequestBody UpdateEnabledRequest request) {
        return samplingService.updateGuidelineEnabled(id, request.enabled());
    }

    @Operation(summary = "删除取材规范", description = "删除取材规范。")
    @RequirePermission(M1PermissionCodes.GUIDELINE_CREATE)
    @DeleteMapping("/sampling-guidelines/{id}")
    public void deleteGuideline(@Parameter(description = "规范 ID") @PathVariable("id") String id) {
        samplingService.deleteGuideline(id);
    }

    @Schema(name = "SamplingUpdateEnabledRequest", description = "更新启用状态请求")
    public record UpdateEnabledRequest(@Schema(description = "是否启用") boolean enabled) {
    }

    @Schema(name = "CreateTemplateCategoryRequest", description = "新增取材模板分类请求")
    public record CreateTemplateCategoryRequest(
        @Schema(description = "父级分类 ID，根节点可为空")
        String parentId,
        @Schema(description = "分类编码，可为空，由系统自动生成")
        @Size(max = 64, message = "Category code must not exceed 64 characters")
        String categoryCode,
        @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Category name must not be blank")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String categoryName,
        @Schema(description = "排序号")
        int sortOrder,
        @Schema(description = "是否启用")
        boolean enabled
    ) {
    }

    @Schema(name = "UpdateTemplateCategoryRequest", description = "更新取材模板分类请求")
    public record UpdateTemplateCategoryRequest(
        String parentId,
        @Schema(description = "分类编码，创建后不可修改")
        @Size(max = 64, message = "Category code must not exceed 64 characters")
        String categoryCode,
        @NotBlank(message = "Category name must not be blank")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String categoryName,
        int sortOrder,
        boolean enabled
    ) {
    }

    @Schema(name = "CreateTemplateRequest", description = "新增取材模板请求")
    public record CreateTemplateRequest(
        @Schema(description = "模板分类 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Category id must not be blank")
        String categoryId,
        @Schema(description = "模板编码，可为空，由系统自动生成")
        @Size(max = 64, message = "Template code must not exceed 64 characters")
        String templateCode,
        @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Template name must not be blank")
        @Size(max = 100, message = "Template name must not exceed 100 characters")
        String templateName,
        @Schema(description = "模板内容")
        String templateContent,
        @Schema(description = "默认分材份数，最小为 1")
        @Min(value = 1, message = "Split part count must be at least 1")
        int splitPartCount,
        @Schema(description = "适用标本类型")
        @Size(max = 100, message = "Applicable specimen type must not exceed 100 characters")
        String applicableSpecimenType,
        @Schema(description = "是否启用")
        boolean enabled,
        @Schema(description = "适用部位 ID 列表")
        List<String> bodyPartIds
    ) {
    }

    @Schema(name = "UpdateTemplateRequest", description = "更新取材模板请求")
    public record UpdateTemplateRequest(
        @NotBlank(message = "Category id must not be blank")
        String categoryId,
        @Schema(description = "模板编码，创建后不可修改")
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

    @Schema(name = "CreateGuidelineCategoryRequest", description = "新增取材规范分类请求")
    public record CreateGuidelineCategoryRequest(
        @Schema(description = "父级分类 ID，根节点可为空")
        String parentId,
        @Schema(description = "分类编码，可为空，由系统自动生成")
        @Size(max = 64, message = "Category code must not exceed 64 characters")
        String categoryCode,
        @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Category name must not be blank")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String categoryName,
        @Schema(description = "排序号")
        int sortOrder,
        @Schema(description = "是否启用")
        boolean enabled
    ) {
    }

    @Schema(name = "UpdateGuidelineCategoryRequest", description = "更新取材规范分类请求")
    public record UpdateGuidelineCategoryRequest(
        String parentId,
        @Schema(description = "分类编码，创建后不可修改")
        @Size(max = 64, message = "Category code must not exceed 64 characters")
        String categoryCode,
        @NotBlank(message = "Category name must not be blank")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String categoryName,
        int sortOrder,
        boolean enabled
    ) {
    }

    @Schema(name = "CreateGuidelineRequest", description = "新增取材规范请求")
    public record CreateGuidelineRequest(
        @Schema(description = "规范分类 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Category id must not be blank")
        String categoryId,
        @Schema(description = "规范编码，可为空，由系统自动生成")
        @Size(max = 64, message = "Guideline code must not exceed 64 characters")
        String guidelineCode,
        @Schema(description = "规范名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Guideline name must not be blank")
        @Size(max = 100, message = "Guideline name must not exceed 100 characters")
        String guidelineName,
        @Schema(description = "规范内容")
        String guidelineContent,
        @Schema(description = "版本号")
        @Size(max = 32, message = "Version must not exceed 32 characters")
        String versionNo,
        @Schema(description = "是否启用")
        boolean enabled
    ) {
    }

    @Schema(name = "UpdateGuidelineRequest", description = "更新取材规范请求")
    public record UpdateGuidelineRequest(
        @NotBlank(message = "Category id must not be blank")
        String categoryId,
        @Schema(description = "规范编码，创建后不可修改")
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
