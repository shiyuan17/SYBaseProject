package com.company.bl.masterdata.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowReferenceOptionService {

    static final String CATEGORY_SPECIMEN_TYPE = "SPECIMEN_TYPE";
    static final String CATEGORY_COLLECTION_MODE = "COLLECTION_MODE";
    static final String CATEGORY_CLINICAL_SYMPTOM = "CLINICAL_SYMPTOM";
    static final String CATEGORY_FIXATION_LIQUID_TYPE = "FIXATION_LIQUID_TYPE";

    private final SystemConfigService systemConfigService;

    public WorkflowReferenceOptionService(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @Transactional(readOnly = true)
    public WorkflowReferenceOptionsResponse listWorkflowReferenceOptions() {
        Map<String, SystemConfigService.ConfigCategoryNode> categoriesByCode = new LinkedHashMap<>();
        collectCategories(systemConfigService.listSystemConfigs(), categoriesByCode);
        return new WorkflowReferenceOptionsResponse(
            extractOptions(categoriesByCode.get(CATEGORY_SPECIMEN_TYPE)),
            extractOptions(categoriesByCode.get(CATEGORY_COLLECTION_MODE)),
            extractOptions(categoriesByCode.get(CATEGORY_CLINICAL_SYMPTOM)),
            extractOptions(categoriesByCode.get(CATEGORY_FIXATION_LIQUID_TYPE))
        );
    }

    private void collectCategories(
        List<SystemConfigService.ConfigCategoryNode> categories,
        Map<String, SystemConfigService.ConfigCategoryNode> categoriesByCode
    ) {
        for (SystemConfigService.ConfigCategoryNode category : categories) {
            categoriesByCode.put(category.categoryCode(), category);
            collectCategories(category.children(), categoriesByCode);
        }
    }

    private List<WorkflowReferenceOption> extractOptions(
        SystemConfigService.ConfigCategoryNode category
    ) {
        if (category == null || !category.enabled()) {
            return List.of();
        }
        return category.items().stream()
            .filter(SystemConfigService.ConfigItemView::enabled)
            .map(this::toWorkflowReferenceOption)
            .toList();
    }

    private WorkflowReferenceOption toWorkflowReferenceOption(
        SystemConfigService.ConfigItemView item
    ) {
        String label = normalizeText(item.configName());
        String value = normalizeText(item.configValue());
        return new WorkflowReferenceOption(label, value == null ? label : value);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record WorkflowReferenceOption(String label, String value) {
    }

    public record WorkflowReferenceOptionsResponse(
        List<WorkflowReferenceOption> specimenTypes,
        List<WorkflowReferenceOption> collectionModes,
        List<WorkflowReferenceOption> clinicalSymptoms,
        List<WorkflowReferenceOption> fixationLiquidTypes
    ) {
    }
}
