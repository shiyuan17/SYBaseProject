package com.company.bl.masterdata.application;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public final class SamplingModels {

    private SamplingModels() {
    }

    @Schema(name = "TemplateCategoryNode", description = "取材模板分类树节点")
    public record TemplateCategoryNode(
        @Schema(description = "分类 ID") String id,
        @Schema(description = "父级分类 ID") String parentId,
        @Schema(description = "分类编码") String categoryCode,
        @Schema(description = "分类名称") String categoryName,
        @Schema(description = "排序号") int sortOrder,
        @Schema(description = "是否启用") boolean enabled,
        @Schema(description = "子分类列表") List<TemplateCategoryNode> children,
        @Schema(description = "分类下模板列表") List<TemplateSummaryView> templates) {
    }

    @Schema(name = "TemplateSummaryView", description = "取材模板摘要")
    public record TemplateSummaryView(
        @Schema(description = "模板 ID") String id,
        @Schema(description = "分类 ID") String categoryId,
        @Schema(description = "模板编码") String templateCode,
        @Schema(description = "模板名称") String templateName,
        @Schema(description = "分材份数") int splitPartCount,
        @Schema(description = "适用标本类型") String applicableSpecimenType,
        @Schema(description = "是否启用") boolean enabled,
        @Schema(description = "适用部位列表") List<TemplateSiteView> bodyParts) {
    }

    @Schema(name = "TemplateDetailView", description = "取材模板详情")
    public record TemplateDetailView(
        @Schema(description = "模板 ID") String id,
        @Schema(description = "分类 ID") String categoryId,
        @Schema(description = "模板编码") String templateCode,
        @Schema(description = "模板名称") String templateName,
        @Schema(description = "模板内容") String templateContent,
        @Schema(description = "分材份数") int splitPartCount,
        @Schema(description = "适用标本类型") String applicableSpecimenType,
        @Schema(description = "是否启用") boolean enabled,
        @Schema(description = "适用部位列表") List<TemplateSiteView> bodyParts) {
    }

    @Schema(name = "TemplateSiteView", description = "模板适用部位")
    public record TemplateSiteView(
        @Schema(description = "部位 ID") String bodyPartId,
        @Schema(description = "部位名称") String bodyPartName) {
    }

    public record CreateTemplateCategoryCommand(String parentId, String categoryCode, String categoryName,
                                                int sortOrder, boolean enabled) {
    }

    public record UpdateTemplateCategoryCommand(String parentId, String categoryCode, String categoryName,
                                                int sortOrder, boolean enabled) {
    }

    public record CreateTemplateCommand(String categoryId, String templateCode, String templateName,
                                        String templateContent, int splitPartCount, String applicableSpecimenType,
                                        boolean enabled, List<String> bodyPartIds) {
    }

    public record UpdateTemplateCommand(String categoryId, String templateCode, String templateName,
                                        String templateContent, int splitPartCount, String applicableSpecimenType,
                                        boolean enabled, List<String> bodyPartIds) {
    }

    @Schema(name = "GuidelineCategoryNode", description = "取材规范分类树节点")
    public record GuidelineCategoryNode(
        @Schema(description = "分类 ID") String id,
        @Schema(description = "父级分类 ID") String parentId,
        @Schema(description = "分类编码") String categoryCode,
        @Schema(description = "分类名称") String categoryName,
        @Schema(description = "排序号") int sortOrder,
        @Schema(description = "是否启用") boolean enabled,
        @Schema(description = "子分类列表") List<GuidelineCategoryNode> children,
        @Schema(description = "分类下规范列表") List<GuidelineSummaryView> guidelines) {
    }

    @Schema(name = "GuidelineSummaryView", description = "取材规范摘要")
    public record GuidelineSummaryView(
        @Schema(description = "规范 ID") String id,
        @Schema(description = "分类 ID") String categoryId,
        @Schema(description = "规范编码") String guidelineCode,
        @Schema(description = "规范名称") String guidelineName,
        @Schema(description = "版本号") String versionNo,
        @Schema(description = "是否启用") boolean enabled) {
    }

    @Schema(name = "GuidelineDetailView", description = "取材规范详情")
    public record GuidelineDetailView(
        @Schema(description = "规范 ID") String id,
        @Schema(description = "分类 ID") String categoryId,
        @Schema(description = "规范编码") String guidelineCode,
        @Schema(description = "规范名称") String guidelineName,
        @Schema(description = "规范内容") String guidelineContent,
        @Schema(description = "版本号") String versionNo,
        @Schema(description = "是否启用") boolean enabled) {
    }

    public record CreateGuidelineCategoryCommand(String parentId, String categoryCode, String categoryName,
                                                 int sortOrder, boolean enabled) {
    }

    public record UpdateGuidelineCategoryCommand(String parentId, String categoryCode, String categoryName,
                                                 int sortOrder, boolean enabled) {
    }

    public record CreateGuidelineCommand(String categoryId, String guidelineCode, String guidelineName,
                                         String guidelineContent, String versionNo, boolean enabled) {
    }

    public record UpdateGuidelineCommand(String categoryId, String guidelineCode, String guidelineName,
                                         String guidelineContent, String versionNo, boolean enabled) {
    }
}
