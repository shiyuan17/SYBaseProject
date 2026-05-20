package com.company.bl.masterdata.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.masterdata.infrastructure.BodyPartJdbcRepository;
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
import java.util.stream.Collectors;

@Service
public class BodyPartService {

    private final BodyPartJdbcRepository bodyPartJdbcRepository;
    private final OperationAuditService operationAuditService;

    public BodyPartService(BodyPartJdbcRepository bodyPartJdbcRepository,
                           OperationAuditService operationAuditService) {
        this.bodyPartJdbcRepository = bodyPartJdbcRepository;
        this.operationAuditService = operationAuditService;
    }

    @Cacheable("bodyPartTree")
    @Transactional(readOnly = true)
    public List<BodyPartNode> listBodyParts() {
        List<BodyPartJdbcRepository.BodyPartRow> rows = bodyPartJdbcRepository.findBodyParts();
        var nodes = rows.stream().map(this::toNode)
            .collect(Collectors.toMap(BodyPartNode::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<BodyPartNode> roots = new ArrayList<>();
        nodes.values().forEach(node -> {
            if (node.parentId() == null) {
                roots.add(node);
            } else {
                BodyPartNode parent = nodes.get(node.parentId());
                if (parent != null) {
                    parent.children().add(node);
                } else {
                    roots.add(node);
                }
            }
        });
        return roots;
    }

    @CacheEvict(value = "bodyPartTree", allEntries = true)
    @Transactional
    public BodyPartNode createBodyPart(CreateBodyPartCommand command) {
        return operationAuditService.audit("MASTERDATA", "BODY_PART", "create_body_part", () -> {
            try {
                var row = bodyPartJdbcRepository.insertBodyPart(new BodyPartJdbcRepository.CreateBodyPartRow(
                    "BP-" + UUID.randomUUID(),
                    command.parentId(),
                    command.partCode(),
                    command.partName(),
                    command.partAlias(),
                    command.partLevel(),
                    command.sortOrder(),
                    command.enabled(),
                    LocalDateTime.now(),
                    LocalDateTime.now()));
                return toNode(row);
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Body part code already exists");
            }
        }, BodyPartNode::id, command::partCode);
    }

    @CacheEvict(value = "bodyPartTree", allEntries = true)
    @Transactional
    public BodyPartNode updateBodyPartEnabled(String id, boolean enabled) {
        return operationAuditService.audit("MASTERDATA", "BODY_PART", "update_body_part_enabled", () -> {
            if (bodyPartJdbcRepository.findBodyPartById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Body part not found");
            }
            bodyPartJdbcRepository.updateBodyPartEnabled(id, enabled);
            return toNode(bodyPartJdbcRepository.findBodyPartById(id));
        }, BodyPartNode::id, () -> id);
    }

    private BodyPartNode toNode(BodyPartJdbcRepository.BodyPartRow row) {
        return new BodyPartNode(row.id(), row.parentId(), row.partCode(), row.partName(), row.partAlias(),
            row.partLevel(), row.sortOrder(), row.enabled(), new ArrayList<>());
    }

    @Schema(name = "BodyPartNode", description = "部位树节点")
    public record BodyPartNode(
        @Schema(description = "部位 ID") String id,
        @Schema(description = "父级部位 ID") String parentId,
        @Schema(description = "部位编码") String partCode,
        @Schema(description = "部位名称") String partName,
        @Schema(description = "部位别名") String partAlias,
        @Schema(description = "部位层级") int partLevel,
        @Schema(description = "排序号") int sortOrder,
        @Schema(description = "是否启用") boolean enabled,
        @Schema(description = "子节点列表") List<BodyPartNode> children) {
    }

    public record CreateBodyPartCommand(String parentId, String partCode, String partName, String partAlias,
                                        int partLevel, int sortOrder, boolean enabled) {
    }
}
