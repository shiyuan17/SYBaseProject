package com.company.bl.masterdata.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.masterdata.infrastructure.SamplingJdbcRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

abstract class AbstractSamplingSupport {

    protected final SamplingJdbcRepository repository;

    protected AbstractSamplingSupport(SamplingJdbcRepository repository) {
        this.repository = repository;
    }

    protected SamplingModels.TemplateCategoryNode toTemplateCategoryNode(SamplingJdbcRepository.TemplateCategoryRow row) {
        return new SamplingModels.TemplateCategoryNode(row.id(), row.parentId(), row.categoryCode(), row.categoryName(),
            row.sortOrder(), row.enabled(), new ArrayList<>(), new ArrayList<>());
    }

    protected SamplingModels.GuidelineCategoryNode toGuidelineCategoryNode(SamplingJdbcRepository.GuidelineCategoryRow row) {
        return new SamplingModels.GuidelineCategoryNode(row.id(), row.parentId(), row.categoryCode(), row.categoryName(),
            row.sortOrder(), row.enabled(), new ArrayList<>(), new ArrayList<>());
    }

    protected SamplingModels.TemplateDetailView toTemplateDetailView(SamplingJdbcRepository.TemplateRow row,
                                                                     List<SamplingModels.TemplateSiteView> sites) {
        return new SamplingModels.TemplateDetailView(
            row.id(),
            row.categoryId(),
            row.templateCode(),
            row.templateName(),
            row.templateContent(),
            row.splitPartCount(),
            row.applicableSpecimenType(),
            row.enabled(),
            sites);
    }

    protected SamplingModels.GuidelineDetailView toGuidelineDetailView(SamplingJdbcRepository.GuidelineRow row) {
        return new SamplingModels.GuidelineDetailView(
            row.id(),
            row.categoryId(),
            row.guidelineCode(),
            row.guidelineName(),
            row.guidelineContent(),
            row.versionNo(),
            row.enabled());
    }

    protected SamplingModels.TemplateDetailView loadTemplateDetail(String id) {
        SamplingJdbcRepository.TemplateRow row = repository.findTemplateById(id);
        if (row == null) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Sampling template not found");
        }
        List<SamplingModels.TemplateSiteView> sites = repository.findTemplateSites().stream()
            .filter(site -> site.templateId().equals(id))
            .map(site -> new SamplingModels.TemplateSiteView(site.bodyPartId(), site.bodyPartName()))
            .toList();
        return toTemplateDetailView(row, sites);
    }

    protected SamplingModels.GuidelineDetailView loadGuidelineDetail(String id) {
        SamplingJdbcRepository.GuidelineRow row = repository.findGuidelineById(id);
        if (row == null) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Guideline not found");
        }
        return toGuidelineDetailView(row);
    }

    protected SamplingModels.TemplateSummaryView toTemplateSummaryView(
        SamplingJdbcRepository.TemplateRow row,
        List<SamplingModels.TemplateSiteView> sites) {
        return new SamplingModels.TemplateSummaryView(
            row.id(),
            row.categoryId(),
            row.templateCode(),
            row.templateName(),
            row.splitPartCount(),
            row.applicableSpecimenType(),
            row.enabled(),
            sites);
    }

    protected SamplingModels.GuidelineSummaryView toGuidelineSummaryView(SamplingJdbcRepository.GuidelineRow row) {
        return new SamplingModels.GuidelineSummaryView(
            row.id(),
            row.categoryId(),
            row.guidelineCode(),
            row.guidelineName(),
            row.versionNo(),
            row.enabled());
    }

    protected List<SamplingModels.TemplateCategoryNode> buildTemplateTree(LinkedHashMap<String, SamplingModels.TemplateCategoryNode> nodes) {
        List<SamplingModels.TemplateCategoryNode> roots = new ArrayList<>();
        nodes.values().forEach(node -> {
            if (node.parentId() == null) {
                roots.add(node);
            } else {
                SamplingModels.TemplateCategoryNode parent = nodes.get(node.parentId());
                if (parent != null) {
                    parent.children().add(node);
                } else {
                    roots.add(node);
                }
            }
        });
        return roots;
    }

    protected List<SamplingModels.GuidelineCategoryNode> buildGuidelineTree(LinkedHashMap<String, SamplingModels.GuidelineCategoryNode> nodes) {
        List<SamplingModels.GuidelineCategoryNode> roots = new ArrayList<>();
        nodes.values().forEach(node -> {
            if (node.parentId() == null) {
                roots.add(node);
            } else {
                SamplingModels.GuidelineCategoryNode parent = nodes.get(node.parentId());
                if (parent != null) {
                    parent.children().add(node);
                } else {
                    roots.add(node);
                }
            }
        });
        return roots;
    }

    protected String resolveCreateCode(String requestedCode, java.util.function.Supplier<String> generator) {
        String normalizedCode = normalizeCode(requestedCode);
        return normalizedCode == null ? generator.get() : normalizedCode;
    }

    protected String resolveExistingCode(String requestedCode, String existingCode, String fieldLabel) {
        String normalizedCode = normalizeCode(requestedCode);
        if (normalizedCode == null || normalizedCode.equals(existingCode)) {
            return existingCode;
        }
        throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, fieldLabel + " cannot be changed once created");
    }

    protected String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
