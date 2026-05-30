package com.company.bl.masterdata.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.masterdata.infrastructure.SamplingJdbcRepository;
import com.company.bl.support.application.NumberingService;
import com.company.bl.support.application.OperationAuditService;
import org.springframework.dao.DataAccessException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
class SamplingMutationSupport extends AbstractSamplingSupport {

    private final NumberingService numberingService;
    private final OperationAuditService operationAuditService;

    SamplingMutationSupport(SamplingJdbcRepository repository,
                            NumberingService numberingService,
                            OperationAuditService operationAuditService) {
        super(repository);
        this.numberingService = numberingService;
        this.operationAuditService = operationAuditService;
    }

    @CacheEvict(value = {"samplingTemplateTree", "samplingTemplateDetail"}, allEntries = true)
    @Transactional
    public SamplingModels.TemplateCategoryNode createSamplingTemplateCategory(SamplingModels.CreateTemplateCategoryCommand command) {
        String categoryCode = resolveCreateCode(command.categoryCode(), numberingService::generateTemplateCategoryCode);
        return operationAuditService.audit("MASTERDATA", "TEMPLATE_CATEGORY", "create_template_category", () -> {
            try {
                return toTemplateCategoryNode(repository.insertTemplateCategory(new SamplingJdbcRepository.CreateTemplateCategoryRow(
                    "STC-" + UUID.randomUUID(), command.parentId(), categoryCode, command.categoryName(),
                    command.sortOrder(), command.enabled(), LocalDateTime.now(), LocalDateTime.now())));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Template category code already exists");
            }
        }, SamplingModels.TemplateCategoryNode::id, () -> categoryCode);
    }

    @CacheEvict(value = {"samplingTemplateTree", "samplingTemplateDetail"}, allEntries = true)
    @Transactional
    public SamplingModels.TemplateCategoryNode updateSamplingTemplateCategory(String id, SamplingModels.UpdateTemplateCategoryCommand command) {
        return operationAuditService.audit("MASTERDATA", "TEMPLATE_CATEGORY", "update_template_category", () -> {
            SamplingJdbcRepository.TemplateCategoryRow current = repository.findTemplateCategoryById(id);
            if (current == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Template category not found");
            }
            String categoryCode = resolveExistingCode(command.categoryCode(), current.categoryCode(), "Template category code");
            try {
                repository.updateTemplateCategory(id, new SamplingJdbcRepository.UpdateTemplateCategoryRow(
                    command.parentId(), categoryCode, command.categoryName(), command.sortOrder(), command.enabled()));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Template category code already exists");
            }
            return toTemplateCategoryNode(repository.findTemplateCategoryById(id));
        }, SamplingModels.TemplateCategoryNode::id, () -> id);
    }

    @CacheEvict(value = {"samplingTemplateTree", "samplingTemplateDetail"}, allEntries = true)
    @Transactional
    public SamplingModels.TemplateDetailView createSamplingTemplate(SamplingModels.CreateTemplateCommand command) {
        String templateCode = resolveCreateCode(command.templateCode(), numberingService::generateTemplateCode);
        return operationAuditService.audit("MASTERDATA", "TEMPLATE", "create_template", () -> {
            try {
                var row = repository.insertTemplate(new SamplingJdbcRepository.CreateTemplateRow(
                    "TPL-" + UUID.randomUUID(), command.categoryId(), templateCode, command.templateName(),
                    command.templateContent(), command.splitPartCount(), command.applicableSpecimenType(),
                    command.enabled(), command.bodyPartIds() == null ? List.of() : command.bodyPartIds(),
                    LocalDateTime.now(), LocalDateTime.now()));
                return loadTemplateDetail(row.id());
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Template code already exists");
            }
        }, SamplingModels.TemplateDetailView::id, () -> templateCode);
    }

    @CacheEvict(value = {"samplingTemplateTree", "samplingTemplateDetail"}, allEntries = true)
    @Transactional
    public SamplingModels.TemplateDetailView updateSamplingTemplate(String id, SamplingModels.UpdateTemplateCommand command) {
        return operationAuditService.audit("MASTERDATA", "TEMPLATE", "update_template", () -> {
            SamplingJdbcRepository.TemplateRow current = repository.findTemplateById(id);
            if (current == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Template not found");
            }
            String templateCode = resolveExistingCode(command.templateCode(), current.templateCode(), "Template code");
            try {
                repository.updateTemplate(id, new SamplingJdbcRepository.UpdateTemplateRow(
                    command.categoryId(), templateCode, command.templateName(), command.templateContent(),
                    command.splitPartCount(), command.applicableSpecimenType(), command.enabled(),
                    command.bodyPartIds() == null ? List.of() : command.bodyPartIds()));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Template code already exists");
            }
            return loadTemplateDetail(id);
        }, SamplingModels.TemplateDetailView::id, () -> id);
    }

    @CacheEvict(value = {"samplingTemplateTree", "samplingTemplateDetail"}, allEntries = true)
    @Transactional
    public SamplingModels.TemplateDetailView updateSamplingTemplateEnabled(String id, boolean enabled) {
        return operationAuditService.audit("MASTERDATA", "TEMPLATE", "update_template_enabled", () -> {
            if (repository.findTemplateById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Template not found");
            }
            repository.updateTemplateEnabled(id, enabled);
            return loadTemplateDetail(id);
        }, SamplingModels.TemplateDetailView::id, () -> id);
    }

    @CacheEvict(value = {"samplingTemplateTree", "samplingTemplateDetail"}, allEntries = true)
    @Transactional
    public void deleteSamplingTemplateCategory(String id) {
        operationAuditService.audit("MASTERDATA", "TEMPLATE_CATEGORY", "delete_template_category", () -> {
            if (repository.findTemplateCategoryById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Template category not found");
            }
            if (repository.countTemplateCategoryChildren(id) > 0 || repository.countTemplatesByCategory(id) > 0) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Template category still has children or templates");
            }
            repository.deleteTemplateCategory(id);
            return id;
        }, value -> id, () -> id);
    }

    @CacheEvict(value = {"samplingTemplateTree", "samplingTemplateDetail"}, allEntries = true)
    @Transactional
    public void deleteSamplingTemplate(String id) {
        operationAuditService.audit("MASTERDATA", "TEMPLATE", "delete_template", () -> {
            if (repository.findTemplateById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Template not found");
            }
            repository.deleteTemplate(id);
            return id;
        }, value -> id, () -> id);
    }

    @CacheEvict(value = {"samplingGuidelineTree", "samplingGuidelineDetail"}, allEntries = true)
    @Transactional
    public SamplingModels.GuidelineCategoryNode createGuidelineCategory(SamplingModels.CreateGuidelineCategoryCommand command) {
        String categoryCode = resolveCreateCode(command.categoryCode(), numberingService::generateGuidelineCategoryCode);
        return operationAuditService.audit("MASTERDATA", "GUIDELINE_CATEGORY", "create_guideline_category", () -> {
            try {
                return toGuidelineCategoryNode(repository.insertGuidelineCategory(new SamplingJdbcRepository.CreateGuidelineCategoryRow(
                    "SGC-" + UUID.randomUUID(), command.parentId(), categoryCode, command.categoryName(),
                    command.sortOrder(), command.enabled(), LocalDateTime.now(), LocalDateTime.now())));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Guideline category code already exists");
            }
        }, SamplingModels.GuidelineCategoryNode::id, () -> categoryCode);
    }

    @CacheEvict(value = {"samplingGuidelineTree", "samplingGuidelineDetail"}, allEntries = true)
    @Transactional
    public SamplingModels.GuidelineCategoryNode updateGuidelineCategory(String id, SamplingModels.UpdateGuidelineCategoryCommand command) {
        return operationAuditService.audit("MASTERDATA", "GUIDELINE_CATEGORY", "update_guideline_category", () -> {
            SamplingJdbcRepository.GuidelineCategoryRow current = repository.findGuidelineCategoryById(id);
            if (current == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Guideline category not found");
            }
            String categoryCode = resolveExistingCode(command.categoryCode(), current.categoryCode(), "Guideline category code");
            try {
                repository.updateGuidelineCategory(id, new SamplingJdbcRepository.UpdateGuidelineCategoryRow(
                    command.parentId(), categoryCode, command.categoryName(), command.sortOrder(), command.enabled()));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Guideline category code already exists");
            }
            return toGuidelineCategoryNode(repository.findGuidelineCategoryById(id));
        }, SamplingModels.GuidelineCategoryNode::id, () -> id);
    }

    @CacheEvict(value = {"samplingGuidelineTree", "samplingGuidelineDetail"}, allEntries = true)
    @Transactional
    public SamplingModels.GuidelineDetailView createGuideline(SamplingModels.CreateGuidelineCommand command) {
        String guidelineCode = resolveCreateCode(command.guidelineCode(), numberingService::generateGuidelineCode);
        return operationAuditService.audit("MASTERDATA", "GUIDELINE", "create_guideline", () -> {
            try {
                var row = repository.insertGuideline(new SamplingJdbcRepository.CreateGuidelineRow(
                    "GL-" + UUID.randomUUID(), command.categoryId(), guidelineCode, command.guidelineName(),
                    command.guidelineContent(), command.versionNo(), command.enabled(), LocalDateTime.now(), LocalDateTime.now()));
                return loadGuidelineDetail(row.id());
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Guideline code already exists");
            }
        }, SamplingModels.GuidelineDetailView::id, () -> guidelineCode);
    }

    @CacheEvict(value = {"samplingGuidelineTree", "samplingGuidelineDetail"}, allEntries = true)
    @Transactional
    public SamplingModels.GuidelineDetailView updateGuideline(String id, SamplingModels.UpdateGuidelineCommand command) {
        return operationAuditService.audit("MASTERDATA", "GUIDELINE", "update_guideline", () -> {
            SamplingJdbcRepository.GuidelineRow current = repository.findGuidelineById(id);
            if (current == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Guideline not found");
            }
            String guidelineCode = resolveExistingCode(command.guidelineCode(), current.guidelineCode(), "Guideline code");
            try {
                repository.updateGuideline(id, new SamplingJdbcRepository.UpdateGuidelineRow(
                    command.categoryId(), guidelineCode, command.guidelineName(), command.guidelineContent(),
                    command.versionNo(), command.enabled()));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Guideline code already exists");
            }
            return loadGuidelineDetail(id);
        }, SamplingModels.GuidelineDetailView::id, () -> id);
    }

    @CacheEvict(value = {"samplingGuidelineTree", "samplingGuidelineDetail"}, allEntries = true)
    @Transactional
    public SamplingModels.GuidelineDetailView updateGuidelineEnabled(String id, boolean enabled) {
        return operationAuditService.audit("MASTERDATA", "GUIDELINE", "update_guideline_enabled", () -> {
            if (repository.findGuidelineById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Guideline not found");
            }
            repository.updateGuidelineEnabled(id, enabled);
            return loadGuidelineDetail(id);
        }, SamplingModels.GuidelineDetailView::id, () -> id);
    }

    @CacheEvict(value = {"samplingGuidelineTree", "samplingGuidelineDetail"}, allEntries = true)
    @Transactional
    public void deleteGuidelineCategory(String id) {
        operationAuditService.audit("MASTERDATA", "GUIDELINE_CATEGORY", "delete_guideline_category", () -> {
            if (repository.findGuidelineCategoryById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Guideline category not found");
            }
            if (repository.countGuidelineCategoryChildren(id) > 0 || repository.countGuidelinesByCategory(id) > 0) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Guideline category still has children or guidelines");
            }
            repository.deleteGuidelineCategory(id);
            return id;
        }, value -> id, () -> id);
    }

    @CacheEvict(value = {"samplingGuidelineTree", "samplingGuidelineDetail"}, allEntries = true)
    @Transactional
    public void deleteGuideline(String id) {
        operationAuditService.audit("MASTERDATA", "GUIDELINE", "delete_guideline", () -> {
            if (repository.findGuidelineById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Guideline not found");
            }
            repository.deleteGuideline(id);
            return id;
        }, value -> id, () -> id);
    }
}
