package com.company.bl.masterdata.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.masterdata.infrastructure.SamplingJdbcRepository;
import com.company.bl.support.application.OperationAuditService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SamplingService {

    private final SamplingJdbcRepository repository;
    private final OperationAuditService operationAuditService;

    public SamplingService(SamplingJdbcRepository repository,
                           OperationAuditService operationAuditService) {
        this.repository = repository;
        this.operationAuditService = operationAuditService;
    }

    @Cacheable("samplingTemplateTree")
    @Transactional(readOnly = true)
    public List<TemplateCategoryNode> listSamplingTemplates() {
        var categories = repository.findTemplateCategories();
        var templates = repository.findTemplates();
        var siteMap = repository.findTemplateSites().stream().collect(Collectors.groupingBy(
            SamplingJdbcRepository.TemplateSiteRow::templateId,
            LinkedHashMap::new,
            Collectors.mapping(site -> new TemplateSiteView(site.bodyPartId(), site.bodyPartName()), Collectors.toList())));
        var nodes = categories.stream().map(this::toTemplateCategoryNode)
            .collect(Collectors.toMap(TemplateCategoryNode::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        templates.forEach(template -> {
            TemplateCategoryNode parent = nodes.get(template.categoryId());
            if (parent != null) {
                parent.templates().add(new TemplateSummaryView(template.id(), template.categoryId(), template.templateCode(),
                    template.templateName(), template.splitPartCount(), template.applicableSpecimenType(),
                    template.enabled(), siteMap.getOrDefault(template.id(), List.of())));
            }
        });
        return buildTree(nodes);
    }

    @Cacheable(cacheNames = "samplingTemplateDetail", key = "#id")
    @Transactional(readOnly = true)
    public TemplateDetailView getSamplingTemplateDetail(String id) {
        var row = repository.findTemplateById(id);
        if (row == null) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Sampling template not found");
        }
        List<TemplateSiteView> sites = repository.findTemplateSites().stream()
            .filter(site -> site.templateId().equals(id))
            .map(site -> new TemplateSiteView(site.bodyPartId(), site.bodyPartName()))
            .toList();
        return new TemplateDetailView(row.id(), row.categoryId(), row.templateCode(), row.templateName(),
            row.templateContent(), row.splitPartCount(), row.applicableSpecimenType(), row.enabled(), sites);
    }

    @CacheEvict(value = {"samplingTemplateTree", "samplingTemplateDetail"}, allEntries = true)
    @Transactional
    public TemplateCategoryNode createSamplingTemplateCategory(CreateTemplateCategoryCommand command) {
        return operationAuditService.audit("MASTERDATA", "TEMPLATE_CATEGORY", "create_template_category", () -> {
            try {
                return toTemplateCategoryNode(repository.insertTemplateCategory(new SamplingJdbcRepository.CreateTemplateCategoryRow(
                    "STC-" + UUID.randomUUID(), command.parentId(), command.categoryCode(), command.categoryName(),
                    command.sortOrder(), command.enabled(), LocalDateTime.now(), LocalDateTime.now())));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Template category code already exists");
            }
        }, TemplateCategoryNode::id, command::categoryCode);
    }

    @CacheEvict(value = {"samplingTemplateTree", "samplingTemplateDetail"}, allEntries = true)
    @Transactional
    public TemplateDetailView createSamplingTemplate(CreateTemplateCommand command) {
        return operationAuditService.audit("MASTERDATA", "TEMPLATE", "create_template", () -> {
            try {
                var row = repository.insertTemplate(new SamplingJdbcRepository.CreateTemplateRow(
                    "TPL-" + UUID.randomUUID(), command.categoryId(), command.templateCode(), command.templateName(),
                    command.templateContent(), command.splitPartCount(), command.applicableSpecimenType(),
                    command.enabled(), command.bodyPartIds() == null ? List.of() : command.bodyPartIds(),
                    LocalDateTime.now(), LocalDateTime.now()));
                return getSamplingTemplateDetail(row.id());
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Template code already exists");
            }
        }, TemplateDetailView::id, command::templateCode);
    }

    @CacheEvict(value = {"samplingTemplateTree", "samplingTemplateDetail"}, allEntries = true)
    @Transactional
    public TemplateDetailView updateSamplingTemplateEnabled(String id, boolean enabled) {
        return operationAuditService.audit("MASTERDATA", "TEMPLATE", "update_template_enabled", () -> {
            if (repository.findTemplateById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Template not found");
            }
            repository.updateTemplateEnabled(id, enabled);
            return getSamplingTemplateDetail(id);
        }, TemplateDetailView::id, () -> id);
    }

    @Cacheable("samplingGuidelineTree")
    @Transactional(readOnly = true)
    public List<GuidelineCategoryNode> listSamplingGuidelines() {
        var categories = repository.findGuidelineCategories();
        var guidelines = repository.findGuidelines();
        var nodes = categories.stream().map(this::toGuidelineCategoryNode)
            .collect(Collectors.toMap(GuidelineCategoryNode::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        guidelines.forEach(guideline -> {
            GuidelineCategoryNode parent = nodes.get(guideline.categoryId());
            if (parent != null) {
                parent.guidelines().add(new GuidelineSummaryView(guideline.id(), guideline.categoryId(),
                    guideline.guidelineCode(), guideline.guidelineName(), guideline.versionNo(), guideline.enabled()));
            }
        });
        return buildGuidelineTree(nodes);
    }

    @Cacheable(cacheNames = "samplingGuidelineDetail", key = "#id")
    @Transactional(readOnly = true)
    public GuidelineDetailView getSamplingGuidelineDetail(String id) {
        var row = repository.findGuidelineById(id);
        if (row == null) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Guideline not found");
        }
        return new GuidelineDetailView(row.id(), row.categoryId(), row.guidelineCode(),
            row.guidelineName(), row.guidelineContent(), row.versionNo(), row.enabled());
    }

    @CacheEvict(value = {"samplingGuidelineTree", "samplingGuidelineDetail"}, allEntries = true)
    @Transactional
    public GuidelineCategoryNode createGuidelineCategory(CreateGuidelineCategoryCommand command) {
        return operationAuditService.audit("MASTERDATA", "GUIDELINE_CATEGORY", "create_guideline_category", () -> {
            try {
                return toGuidelineCategoryNode(repository.insertGuidelineCategory(new SamplingJdbcRepository.CreateGuidelineCategoryRow(
                    "SGC-" + UUID.randomUUID(), command.parentId(), command.categoryCode(), command.categoryName(),
                    command.sortOrder(), command.enabled(), LocalDateTime.now(), LocalDateTime.now())));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Guideline category code already exists");
            }
        }, GuidelineCategoryNode::id, command::categoryCode);
    }

    @CacheEvict(value = {"samplingGuidelineTree", "samplingGuidelineDetail"}, allEntries = true)
    @Transactional
    public GuidelineDetailView createGuideline(CreateGuidelineCommand command) {
        return operationAuditService.audit("MASTERDATA", "GUIDELINE", "create_guideline", () -> {
            try {
                var row = repository.insertGuideline(new SamplingJdbcRepository.CreateGuidelineRow(
                    "GL-" + UUID.randomUUID(), command.categoryId(), command.guidelineCode(), command.guidelineName(),
                    command.guidelineContent(), command.versionNo(), command.enabled(), LocalDateTime.now(), LocalDateTime.now()));
                return getSamplingGuidelineDetail(row.id());
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Guideline code already exists");
            }
        }, GuidelineDetailView::id, command::guidelineCode);
    }

    @CacheEvict(value = {"samplingGuidelineTree", "samplingGuidelineDetail"}, allEntries = true)
    @Transactional
    public GuidelineDetailView updateGuidelineEnabled(String id, boolean enabled) {
        return operationAuditService.audit("MASTERDATA", "GUIDELINE", "update_guideline_enabled", () -> {
            if (repository.findGuidelineById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Guideline not found");
            }
            repository.updateGuidelineEnabled(id, enabled);
            return getSamplingGuidelineDetail(id);
        }, GuidelineDetailView::id, () -> id);
    }

    private TemplateCategoryNode toTemplateCategoryNode(SamplingJdbcRepository.TemplateCategoryRow row) {
        return new TemplateCategoryNode(row.id(), row.parentId(), row.categoryCode(), row.categoryName(),
            row.sortOrder(), row.enabled(), new ArrayList<>(), new ArrayList<>());
    }

    private GuidelineCategoryNode toGuidelineCategoryNode(SamplingJdbcRepository.GuidelineCategoryRow row) {
        return new GuidelineCategoryNode(row.id(), row.parentId(), row.categoryCode(), row.categoryName(),
            row.sortOrder(), row.enabled(), new ArrayList<>(), new ArrayList<>());
    }

    private List<TemplateCategoryNode> buildTree(LinkedHashMap<String, TemplateCategoryNode> nodes) {
        List<TemplateCategoryNode> roots = new ArrayList<>();
        nodes.values().forEach(node -> {
            if (node.parentId() == null) {
                roots.add(node);
            } else {
                TemplateCategoryNode parent = nodes.get(node.parentId());
                if (parent != null) {
                    parent.children().add(node);
                } else {
                    roots.add(node);
                }
            }
        });
        return roots;
    }

    private List<GuidelineCategoryNode> buildGuidelineTree(LinkedHashMap<String, GuidelineCategoryNode> nodes) {
        List<GuidelineCategoryNode> roots = new ArrayList<>();
        nodes.values().forEach(node -> {
            if (node.parentId() == null) {
                roots.add(node);
            } else {
                GuidelineCategoryNode parent = nodes.get(node.parentId());
                if (parent != null) {
                    parent.children().add(node);
                } else {
                    roots.add(node);
                }
            }
        });
        return roots;
    }

    public record TemplateCategoryNode(String id, String parentId, String categoryCode, String categoryName,
                                       int sortOrder, boolean enabled, List<TemplateCategoryNode> children,
                                       List<TemplateSummaryView> templates) {
    }

    public record TemplateSummaryView(String id, String categoryId, String templateCode, String templateName,
                                      int splitPartCount, String applicableSpecimenType, boolean enabled,
                                      List<TemplateSiteView> bodyParts) {
    }

    public record TemplateDetailView(String id, String categoryId, String templateCode, String templateName,
                                     String templateContent, int splitPartCount, String applicableSpecimenType,
                                     boolean enabled, List<TemplateSiteView> bodyParts) {
    }

    public record TemplateSiteView(String bodyPartId, String bodyPartName) {
    }

    public record CreateTemplateCategoryCommand(String parentId, String categoryCode, String categoryName,
                                                int sortOrder, boolean enabled) {
    }

    public record CreateTemplateCommand(String categoryId, String templateCode, String templateName,
                                        String templateContent, int splitPartCount, String applicableSpecimenType,
                                        boolean enabled, List<String> bodyPartIds) {
    }

    public record GuidelineCategoryNode(String id, String parentId, String categoryCode, String categoryName,
                                        int sortOrder, boolean enabled, List<GuidelineCategoryNode> children,
                                        List<GuidelineSummaryView> guidelines) {
    }

    public record GuidelineSummaryView(String id, String categoryId, String guidelineCode, String guidelineName,
                                       String versionNo, boolean enabled) {
    }

    public record GuidelineDetailView(String id, String categoryId, String guidelineCode, String guidelineName,
                                      String guidelineContent, String versionNo, boolean enabled) {
    }

    public record CreateGuidelineCategoryCommand(String parentId, String categoryCode, String categoryName,
                                                 int sortOrder, boolean enabled) {
    }

    public record CreateGuidelineCommand(String categoryId, String guidelineCode, String guidelineName,
                                         String guidelineContent, String versionNo, boolean enabled) {
    }
}
