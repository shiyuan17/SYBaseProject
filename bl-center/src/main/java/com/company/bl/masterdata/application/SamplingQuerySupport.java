package com.company.bl.masterdata.application;

import com.company.bl.masterdata.infrastructure.SamplingJdbcRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
class SamplingQuerySupport extends AbstractSamplingSupport {

    SamplingQuerySupport(SamplingJdbcRepository repository) {
        super(repository);
    }

    @Cacheable("samplingTemplateTree")
    @Transactional(readOnly = true)
    public List<SamplingModels.TemplateCategoryNode> listSamplingTemplates() {
        var categories = repository.findTemplateCategories();
        var templates = repository.findTemplates();
        var siteMap = repository.findTemplateSites().stream().collect(Collectors.groupingBy(
            SamplingJdbcRepository.TemplateSiteRow::templateId,
            java.util.LinkedHashMap::new,
            Collectors.mapping(site -> new SamplingModels.TemplateSiteView(site.bodyPartId(), site.bodyPartName()), Collectors.toList())));
        var nodes = categories.stream().map(this::toTemplateCategoryNode)
            .collect(Collectors.toMap(SamplingModels.TemplateCategoryNode::id, java.util.function.Function.identity(),
                (left, right) -> left, java.util.LinkedHashMap::new));
        templates.forEach(template -> {
            SamplingModels.TemplateCategoryNode parent = nodes.get(template.categoryId());
            if (parent != null) {
                parent.templates().add(toTemplateSummaryView(template, siteMap.getOrDefault(template.id(), List.of())));
            }
        });
        return buildTemplateTree(nodes);
    }

    @Cacheable(cacheNames = "samplingTemplateDetail", key = "#id")
    @Transactional(readOnly = true)
    public SamplingModels.TemplateDetailView getSamplingTemplateDetail(String id) {
        return loadTemplateDetail(id);
    }

    @Cacheable("samplingGuidelineTree")
    @Transactional(readOnly = true)
    public List<SamplingModels.GuidelineCategoryNode> listSamplingGuidelines() {
        var categories = repository.findGuidelineCategories();
        var guidelines = repository.findGuidelines();
        var nodes = categories.stream().map(this::toGuidelineCategoryNode)
            .collect(Collectors.toMap(SamplingModels.GuidelineCategoryNode::id, java.util.function.Function.identity(),
                (left, right) -> left, java.util.LinkedHashMap::new));
        guidelines.forEach(guideline -> {
            SamplingModels.GuidelineCategoryNode parent = nodes.get(guideline.categoryId());
            if (parent != null) {
                parent.guidelines().add(toGuidelineSummaryView(guideline));
            }
        });
        return buildGuidelineTree(nodes);
    }

    @Cacheable(cacheNames = "samplingGuidelineDetail", key = "#id")
    @Transactional(readOnly = true)
    public SamplingModels.GuidelineDetailView getSamplingGuidelineDetail(String id) {
        return loadGuidelineDetail(id);
    }
}
