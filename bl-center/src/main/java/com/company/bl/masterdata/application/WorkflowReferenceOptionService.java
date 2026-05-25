package com.company.bl.masterdata.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowReferenceOptionService {

    static final String ROOT_CATEGORY_WORKFLOW_REFERENCE = "WORKFLOW_REFERENCE";
    static final String CATEGORY_SPECIMEN_TYPE = "SPECIMEN_TYPE";
    static final String CATEGORY_COLLECTION_MODE = "COLLECTION_MODE";
    static final String CATEGORY_CLINICAL_SYMPTOM = "CLINICAL_SYMPTOM";
    static final String CATEGORY_FIXATION_LIQUID_TYPE = "FIXATION_LIQUID_TYPE";
    static final String CATEGORY_CONTAINER_NAME = "CONTAINER_NAME";
    static final String CATEGORY_SPECIMEN_IMAGE_SIZE = "SPECIMEN_IMAGE_SIZE";
    static final String CATEGORY_CUT_SURFACE_FEATURE = "CUT_SURFACE_FEATURE";
    static final String CATEGORY_MARGIN_MARKING = "MARGIN_MARKING";

    private final SystemConfigService systemConfigService;

    public WorkflowReferenceOptionService(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @Transactional(readOnly = true)
    public WorkflowReferenceOptionsResponse listWorkflowReferenceOptions() {
        SystemConfigService.ConfigCategoryNode rootCategory =
            findCategoryByCode(systemConfigService.listSystemConfigs(), ROOT_CATEGORY_WORKFLOW_REFERENCE);
        if (rootCategory == null || !rootCategory.enabled()) {
            return new WorkflowReferenceOptionsResponse(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
            );
        }

        Map<String, SystemConfigService.ConfigCategoryNode> categoriesByCode = new LinkedHashMap<>();
        collectCategories(rootCategory.children(), categoriesByCode);
        return new WorkflowReferenceOptionsResponse(
            extractOptions(categoriesByCode.get(CATEGORY_SPECIMEN_TYPE)),
            extractOptions(categoriesByCode.get(CATEGORY_COLLECTION_MODE)),
            extractOptions(categoriesByCode.get(CATEGORY_CLINICAL_SYMPTOM)),
            extractOptions(categoriesByCode.get(CATEGORY_FIXATION_LIQUID_TYPE)),
            extractOptions(categoriesByCode.get(CATEGORY_CONTAINER_NAME)),
            extractOptions(categoriesByCode.get(CATEGORY_SPECIMEN_IMAGE_SIZE)),
            extractOptions(categoriesByCode.get(CATEGORY_CUT_SURFACE_FEATURE)),
            extractOptions(categoriesByCode.get(CATEGORY_MARGIN_MARKING))
        );
    }

    private SystemConfigService.ConfigCategoryNode findCategoryByCode(
        List<SystemConfigService.ConfigCategoryNode> categories,
        String categoryCode
    ) {
        for (SystemConfigService.ConfigCategoryNode category : categories) {
            if (categoryCode.equals(category.categoryCode())) {
                return category;
            }
            SystemConfigService.ConfigCategoryNode nested = findCategoryByCode(category.children(), categoryCode);
            if (nested != null) {
                return nested;
            }
        }
        return null;
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
        List<WorkflowReferenceOption> fixationLiquidTypes,
        List<WorkflowReferenceOption> containerNames,
        List<WorkflowReferenceOption> specimenImageSizes,
        List<WorkflowReferenceOption> cutSurfaceFeatures,
        List<WorkflowReferenceOption> marginMarkings
    ) {
    }
}
