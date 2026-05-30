package com.company.bl.masterdata.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SamplingService {

    private final SamplingQuerySupport querySupport;
    private final SamplingMutationSupport mutationSupport;

    public SamplingService(SamplingQuerySupport querySupport, SamplingMutationSupport mutationSupport) {
        this.querySupport = querySupport;
        this.mutationSupport = mutationSupport;
    }

    @Transactional(readOnly = true)
    public List<SamplingModels.TemplateCategoryNode> listSamplingTemplates() {
        return querySupport.listSamplingTemplates();
    }

    @Transactional(readOnly = true)
    public SamplingModels.TemplateDetailView getSamplingTemplateDetail(String id) {
        return querySupport.getSamplingTemplateDetail(id);
    }

    @Transactional
    public SamplingModels.TemplateCategoryNode createSamplingTemplateCategory(SamplingModels.CreateTemplateCategoryCommand command) {
        return mutationSupport.createSamplingTemplateCategory(command);
    }

    @Transactional
    public SamplingModels.TemplateCategoryNode updateSamplingTemplateCategory(String id, SamplingModels.UpdateTemplateCategoryCommand command) {
        return mutationSupport.updateSamplingTemplateCategory(id, command);
    }

    @Transactional
    public SamplingModels.TemplateDetailView createSamplingTemplate(SamplingModels.CreateTemplateCommand command) {
        return mutationSupport.createSamplingTemplate(command);
    }

    @Transactional
    public SamplingModels.TemplateDetailView updateSamplingTemplate(String id, SamplingModels.UpdateTemplateCommand command) {
        return mutationSupport.updateSamplingTemplate(id, command);
    }

    @Transactional
    public SamplingModels.TemplateDetailView updateSamplingTemplateEnabled(String id, boolean enabled) {
        return mutationSupport.updateSamplingTemplateEnabled(id, enabled);
    }

    @Transactional
    public void deleteSamplingTemplateCategory(String id) {
        mutationSupport.deleteSamplingTemplateCategory(id);
    }

    @Transactional
    public void deleteSamplingTemplate(String id) {
        mutationSupport.deleteSamplingTemplate(id);
    }

    @Transactional(readOnly = true)
    public List<SamplingModels.GuidelineCategoryNode> listSamplingGuidelines() {
        return querySupport.listSamplingGuidelines();
    }

    @Transactional(readOnly = true)
    public SamplingModels.GuidelineDetailView getSamplingGuidelineDetail(String id) {
        return querySupport.getSamplingGuidelineDetail(id);
    }

    @Transactional
    public SamplingModels.GuidelineCategoryNode createGuidelineCategory(SamplingModels.CreateGuidelineCategoryCommand command) {
        return mutationSupport.createGuidelineCategory(command);
    }

    @Transactional
    public SamplingModels.GuidelineCategoryNode updateGuidelineCategory(String id, SamplingModels.UpdateGuidelineCategoryCommand command) {
        return mutationSupport.updateGuidelineCategory(id, command);
    }

    @Transactional
    public SamplingModels.GuidelineDetailView createGuideline(SamplingModels.CreateGuidelineCommand command) {
        return mutationSupport.createGuideline(command);
    }

    @Transactional
    public SamplingModels.GuidelineDetailView updateGuideline(String id, SamplingModels.UpdateGuidelineCommand command) {
        return mutationSupport.updateGuideline(id, command);
    }

    @Transactional
    public SamplingModels.GuidelineDetailView updateGuidelineEnabled(String id, boolean enabled) {
        return mutationSupport.updateGuidelineEnabled(id, enabled);
    }

    @Transactional
    public void deleteGuidelineCategory(String id) {
        mutationSupport.deleteGuidelineCategory(id);
    }

    @Transactional
    public void deleteGuideline(String id) {
        mutationSupport.deleteGuideline(id);
    }
}
