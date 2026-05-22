package com.company.bl.masterdata.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.support.application.NumberingService;
import com.company.bl.masterdata.infrastructure.SamplingJdbcRepository;
import com.company.bl.support.application.OperationAuditService;
import io.swagger.v3.oas.annotations.media.Schema;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class SamplingService {

    private final SamplingJdbcRepository repository;
    private final NumberingService numberingService;
    private final OperationAuditService operationAuditService;

    public SamplingService(SamplingJdbcRepository repository,
                           NumberingService numberingService,
                           OperationAuditService operationAuditService) {
        this.repository = repository;
        this.numberingService = numberingService;
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
        String categoryCode = resolveCreateCode(
            command.categoryCode(),
            numberingService::generateTemplateCategoryCode);
        return operationAuditService.audit("MASTERDATA", "TEMPLATE_CATEGORY", "create_template_category", () -> {
            try {
                return toTemplateCategoryNode(repository.insertTemplateCategory(new SamplingJdbcRepository.CreateTemplateCategoryRow(
                    "STC-" + UUID.randomUUID(), command.parentId(), categoryCode, command.categoryName(),
                    command.sortOrder(), command.enabled(), LocalDateTime.now(), LocalDateTime.now())));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Template category code already exists");
            }
        }, TemplateCategoryNode::id, () -> categoryCode);
    }

    @CacheEvict(value = {"samplingTemplateTree", "samplingTemplateDetail"}, allEntries = true)
    @Transactional
    public TemplateCategoryNode updateSamplingTemplateCategory(String id, UpdateTemplateCategoryCommand command) {
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
        }, TemplateCategoryNode::id, () -> id);
    }

    @CacheEvict(value = {"samplingTemplateTree", "samplingTemplateDetail"}, allEntries = true)
    @Transactional
    public TemplateDetailView createSamplingTemplate(CreateTemplateCommand command) {
        String templateCode = resolveCreateCode(command.templateCode(), numberingService::generateTemplateCode);
        return operationAuditService.audit("MASTERDATA", "TEMPLATE", "create_template", () -> {
            try {
                var row = repository.insertTemplate(new SamplingJdbcRepository.CreateTemplateRow(
                    "TPL-" + UUID.randomUUID(), command.categoryId(), templateCode, command.templateName(),
                    command.templateContent(), command.splitPartCount(), command.applicableSpecimenType(),
                    command.enabled(), command.bodyPartIds() == null ? List.of() : command.bodyPartIds(),
                    LocalDateTime.now(), LocalDateTime.now()));
                return getSamplingTemplateDetail(row.id());
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Template code already exists");
            }
        }, TemplateDetailView::id, () -> templateCode);
    }

    @CacheEvict(value = {"samplingTemplateTree", "samplingTemplateDetail"}, allEntries = true)
    @Transactional
    public TemplateDetailView updateSamplingTemplate(String id, UpdateTemplateCommand command) {
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
            return getSamplingTemplateDetail(id);
        }, TemplateDetailView::id, () -> id);
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
        String categoryCode = resolveCreateCode(
            command.categoryCode(),
            numberingService::generateGuidelineCategoryCode);
        return operationAuditService.audit("MASTERDATA", "GUIDELINE_CATEGORY", "create_guideline_category", () -> {
            try {
                return toGuidelineCategoryNode(repository.insertGuidelineCategory(new SamplingJdbcRepository.CreateGuidelineCategoryRow(
                    "SGC-" + UUID.randomUUID(), command.parentId(), categoryCode, command.categoryName(),
                    command.sortOrder(), command.enabled(), LocalDateTime.now(), LocalDateTime.now())));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Guideline category code already exists");
            }
        }, GuidelineCategoryNode::id, () -> categoryCode);
    }

    @CacheEvict(value = {"samplingGuidelineTree", "samplingGuidelineDetail"}, allEntries = true)
    @Transactional
    public GuidelineCategoryNode updateGuidelineCategory(String id, UpdateGuidelineCategoryCommand command) {
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
        }, GuidelineCategoryNode::id, () -> id);
    }

    @CacheEvict(value = {"samplingGuidelineTree", "samplingGuidelineDetail"}, allEntries = true)
    @Transactional
    public GuidelineDetailView createGuideline(CreateGuidelineCommand command) {
        String guidelineCode = resolveCreateCode(command.guidelineCode(), numberingService::generateGuidelineCode);
        return operationAuditService.audit("MASTERDATA", "GUIDELINE", "create_guideline", () -> {
            try {
                var row = repository.insertGuideline(new SamplingJdbcRepository.CreateGuidelineRow(
                    "GL-" + UUID.randomUUID(), command.categoryId(), guidelineCode, command.guidelineName(),
                    command.guidelineContent(), command.versionNo(), command.enabled(), LocalDateTime.now(), LocalDateTime.now()));
                return getSamplingGuidelineDetail(row.id());
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Guideline code already exists");
            }
        }, GuidelineDetailView::id, () -> guidelineCode);
    }

    @CacheEvict(value = {"samplingGuidelineTree", "samplingGuidelineDetail"}, allEntries = true)
    @Transactional
    public GuidelineDetailView updateGuideline(String id, UpdateGuidelineCommand command) {
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
            return getSamplingGuidelineDetail(id);
        }, GuidelineDetailView::id, () -> id);
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

    private String resolveCreateCode(String requestedCode, Supplier<String> generator) {
        String normalizedCode = normalizeCode(requestedCode);
        return normalizedCode == null ? generator.get() : normalizedCode;
    }

    private String resolveExistingCode(String requestedCode, String existingCode, String fieldLabel) {
        String normalizedCode = normalizeCode(requestedCode);
        if (normalizedCode == null || normalizedCode.equals(existingCode)) {
            return existingCode;
        }
        throw new BlBusinessException(
            BlErrorCode.INVALID_ARGUMENT,
            400,
            fieldLabel + " cannot be changed once created");
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
