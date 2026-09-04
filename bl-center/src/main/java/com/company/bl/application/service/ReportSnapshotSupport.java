package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ReportSnapshotSupport {

    private static final long MAX_RENDER_SNAPSHOT_LENGTH = 2L * 1024L * 1024L;
    private static final int MAX_SNAPSHOT_DEPTH = 20;
    private static final int MAX_SNAPSHOT_NODES = 10_000;
    private static final int MAX_SECTIONS = 100;
    private static final int MAX_IMAGES_PER_SECTION = 100;
    private static final String WYSIWYG_TEMPLATE = "wysiwyg-template";

    private final DiagnosticReportRepository repository;
    private final ObjectMapper objectMapper;

    ReportSnapshotSupport(DiagnosticReportRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    String validateAndSerialize(JsonNode renderSnapshot, String caseId) {
        if (renderSnapshot == null || renderSnapshot.isNull()) {
            return null;
        }
        if (!renderSnapshot.isObject() || renderSnapshot.path("schemaVersion").asInt(-1) != 1) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400,
                "Unsupported report render snapshot version");
        }
        validateSnapshotStructure(renderSnapshot);
        collectSnapshotAssets(renderSnapshot, requireText(caseId, "Case ID is required"));
        try {
            String serialized = objectMapper.writeValueAsString(renderSnapshot);
            if (serialized.length() > MAX_RENDER_SNAPSHOT_LENGTH) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400,
                    "Report render snapshot is too large");
            }
            return serialized;
        } catch (JsonProcessingException exception) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Invalid report render snapshot");
        }
    }

    JsonNode parse(String renderSnapshot) {
        if (renderSnapshot == null || renderSnapshot.isBlank()) {
            return objectMapper.createObjectNode().put("schemaVersion", 1);
        }
        try {
            JsonNode snapshot = objectMapper.readTree(renderSnapshot);
            if (!snapshot.isObject() || snapshot.path("schemaVersion").asInt(-1) != 1) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400,
                    "Unsupported report render snapshot version");
            }
            validateSnapshotStructure(snapshot);
            return snapshot;
        } catch (JsonProcessingException exception) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Invalid report render snapshot");
        }
    }

    List<DiagnosticReportRepository.ReportRenderAsset> collectSnapshotAssets(JsonNode node, String caseId) {
        Map<String, DiagnosticReportRepository.ReportRenderAsset> assets = new LinkedHashMap<>();
        collectSnapshotAssets(node, caseId, assets);
        return List.copyOf(assets.values());
    }

    DiagnosticReportRepository.ReportRenderAsset getRenderAsset(String assetId) {
        return repository.findReportRenderAssetById(requireText(assetId, "Report image ID is required"))
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404,
                "Report image not found"));
    }

    String buildLegacyRenderSnapshot(DiagnosticReportRepository.PathologyReport report) {
        try {
            return objectMapper.createObjectNode()
                .put("schemaVersion", 1)
                .put("templateCode", WYSIWYG_TEMPLATE)
                .put("hospitalName", "")
                .put("reportTitle", "病理检查报告单")
                .put("reportNo", report.reportNo())
                .set("sections", objectMapper.createArrayNode()
                    .add(objectMapper.createObjectNode().put("label", "大体所见")
                        .put("minHeight", 82).put("value", nullToEmpty(report.grossExam())))
                    .add(objectMapper.createObjectNode().put("label", "镜下所见")
                        .put("minHeight", 160).put("value", nullToEmpty(report.microscopicExam())))
                    .add(objectMapper.createObjectNode().put("label", "病理诊断")
                        .put("minHeight", 92).put("value", nullToEmpty(report.finalDiagnosis()))))
                .toString();
        } catch (RuntimeException exception) {
            throw new BlBusinessException(BlErrorCode.REPORT_ARTIFACT_GENERATION_FAILED, 503,
                "报告 OFD 文件生成失败，签发未完成");
        }
    }

    DiagnosticReportRepository.PathologyReport copyWithRenderSnapshot(
        DiagnosticReportRepository.PathologyReport report,
        String renderSnapshot,
        String signedByName,
        LocalDateTime signedAt
    ) {
        return new DiagnosticReportRepository.PathologyReport(
            report.id(), report.caseId(), report.taskId(), report.reportNo(), report.pathologyNo(), report.reportScope(),
            report.reportSeq(), report.reportStatus(), report.versionNo(), report.specimenType(), report.patientName(),
            report.submittingDepartmentId(), report.submittingDepartmentName(), report.reportDate(), report.grossExam(),
            report.microscopicExam(), report.clinicalDiagnosis(), report.finalDiagnosis(), report.submittedAt(),
            report.reviewerUserId(), report.reviewerName(), report.reviewedAt(), report.signedByUserId(), signedByName,
            signedAt, report.publishedAt(), report.richTextContent(), renderSnapshot, report.remarks(), report.createdAt(),
            report.updatedAt());
    }

    private void collectSnapshotAssets(JsonNode node, String caseId,
                                       Map<String, DiagnosticReportRepository.ReportRenderAsset> assets) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject() && node.hasNonNull("assetId")) {
            DiagnosticReportRepository.ReportRenderAsset asset = getRenderAsset(node.path("assetId").asText());
            if (!caseId.equals(asset.caseId())) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409,
                    "Report image does not belong to case");
            }
            assets.putIfAbsent(asset.id(), asset);
        }
        node.elements().forEachRemaining(child -> collectSnapshotAssets(child, caseId, assets));
    }

    private void validateSnapshotStructure(JsonNode snapshot) {
        requireTextNode(snapshot, "templateCode", 100);
        validateOptionalTextNode(snapshot, "hospitalName", 500);
        validateOptionalTextNode(snapshot, "reportTitle", 500);
        validateOptionalTextNode(snapshot, "note", 20_000);
        validateOptionalTextNode(snapshot, "accentColor", 32);
        validateOptionalTextNode(snapshot, "deliveredAt", 100);
        validateOptionalTextNode(snapshot, "reportNo", 100);
        validateOptionalTextNode(snapshot, "layoutCode", 100);
        validateFieldArray(snapshot.path("metaFields"), "metaFields");
        validateFieldArray(snapshot.path("footerFields"), "footerFields");
        JsonNode sections = snapshot.path("sections");
        if (!sections.isArray() || sections.size() > MAX_SECTIONS) {
            throw invalidSnapshot();
        }
        for (JsonNode section : sections) {
            if (!section.isObject()) {
                throw invalidSnapshot();
            }
            requireTextNode(section, "label", 500);
            validateOptionalTextNode(section, "value", 200_000);
            validateBoundedNumber(section, "minHeight", 0, 5_000);
            JsonNode images = section.path("images");
            if (images.isMissingNode() || images.isNull()) {
                continue;
            }
            if (!images.isArray() || images.size() > MAX_IMAGES_PER_SECTION) {
                throw invalidSnapshot();
            }
            for (JsonNode image : images) {
                if (!image.isObject()) {
                    throw invalidSnapshot();
                }
                String assetId = requireTextNode(image, "assetId", 64);
                if (!assetId.matches("RRA-[0-9a-fA-F-]{36}")) {
                    throw invalidSnapshot();
                }
                validateOptionalTextNode(image, "title", 255);
                validateBoundedNumber(image, "left", 0, 10_000);
                validateBoundedNumber(image, "top", 0, 10_000);
            }
        }
        JsonNode structuredBlocks = snapshot.path("structuredBlocks");
        if (!structuredBlocks.isMissingNode() && !structuredBlocks.isNull() && !structuredBlocks.isObject()) {
            throw invalidSnapshot();
        }
        validateSnapshotTreeBounds(snapshot);
    }

    private void validateFieldArray(JsonNode fields, String fieldName) {
        if (fields.isMissingNode() || fields.isNull()) {
            return;
        }
        if (!fields.isArray() || fields.size() > 200) {
            throw invalidSnapshot();
        }
        for (JsonNode field : fields) {
            if (!field.isObject()) {
                throw invalidSnapshot();
            }
            requireTextNode(field, "label", 500);
            validateOptionalTextNode(field, "value", 100_000);
            validateOptionalTextNode(field, "class", 200);
        }
    }

    private void validateSnapshotTreeBounds(JsonNode snapshot) {
        Deque<SnapshotNode> nodes = new ArrayDeque<>();
        nodes.push(new SnapshotNode(snapshot, 0));
        int visited = 0;
        while (!nodes.isEmpty()) {
            SnapshotNode current = nodes.pop();
            if (++visited > MAX_SNAPSHOT_NODES || current.depth > MAX_SNAPSHOT_DEPTH) {
                throw invalidSnapshot();
            }
            JsonNode node = current.node;
            if (node.isTextual() && node.textValue().length() > 200_000) {
                throw invalidSnapshot();
            }
            if (node.isObject()) {
                var fields = node.fields();
                while (fields.hasNext()) {
                    var field = fields.next();
                    if ("fileUrl".equals(field.getKey()) || "objectUrl".equals(field.getKey())) {
                        throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400,
                            "Report render snapshot must reference images by assetId");
                    }
                    nodes.push(new SnapshotNode(field.getValue(), current.depth + 1));
                }
            } else if (node.isArray()) {
                node.elements().forEachRemaining(child -> nodes.push(new SnapshotNode(child, current.depth + 1)));
            }
        }
    }

    private String requireTextNode(JsonNode node, String fieldName, int maxLength) {
        JsonNode value = node.path(fieldName);
        if (!value.isTextual() || value.textValue().isBlank() || value.textValue().length() > maxLength) {
            throw invalidSnapshot();
        }
        return value.textValue();
    }

    private void validateOptionalTextNode(JsonNode node, String fieldName, int maxLength) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return;
        }
        if (!value.isTextual() || value.textValue().length() > maxLength) {
            throw invalidSnapshot();
        }
    }

    private void validateBoundedNumber(JsonNode node, String fieldName, double minimum, double maximum) {
        JsonNode value = node.path(fieldName);
        if (!value.isNumber()) {
            throw invalidSnapshot();
        }
        double number = value.asDouble();
        if (!Double.isFinite(number) || number < minimum || number > maximum) {
            throw invalidSnapshot();
        }
    }

    private BlBusinessException invalidSnapshot() {
        return new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Invalid report render snapshot");
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, message);
        }
        return value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record SnapshotNode(JsonNode node, int depth) {
    }
}
