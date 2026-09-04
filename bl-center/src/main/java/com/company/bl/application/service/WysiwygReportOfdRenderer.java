package com.company.bl.application.service;

import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.ofdrw.font.Font;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.element.canvas.Canvas;
import org.ofdrw.layout.element.canvas.DrawContext;
import org.ofdrw.layout.element.canvas.FontSetting;
import org.ofdrw.layout.element.canvas.TextAlign;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class WysiwygReportOfdRenderer {

    private static final double A4_WIDTH = 210d;
    private static final double PAGE_MARGIN = 8d;
    private static final double CANVAS_WIDTH = A4_WIDTH - PAGE_MARGIN * 2d;
    private static final double HEADER_HEIGHT = 23d;
    private static final double HEADER_SIDE_BOX_WIDTH = 30d;
    private static final double MAX_SECTION_MIN_HEIGHT_PX = 160d;
    private static final double MAX_SECTION_BLOCK_HEIGHT = 265d;
    private static final double GRID_PADDING = 1.2d;
    private static final double SECTION_PADDING = 1.8d;
    private static final double SECTION_CONTENT_INSET = 5.2d;
    private static final double SECTION_BOTTOM_PADDING = 2d;
    private static final double TEXT_FONT_SIZE = 3.2d;
    private static final double TEXT_LINE_HEIGHT = 4.2d;
    private static final double MIN_GRID_ROW_HEIGHT = 7d;
    private static final double FOOTER_HEIGHT = 22.5d;
    private static final int METADATA_COLUMNS = 4;
    private static final int[] WHITE = new int[]{255, 255, 255};
    private static final int[] VALUE_BACKGROUND = new int[]{221, 221, 221};
    private static final int[] TEXT_COLOR = new int[]{17, 17, 17};
    private static final int[] NOTE_COLOR = new int[]{0, 128, 0};

    private final ImageRenderer imageRenderer;
    private final StructuredBlockRenderer structuredBlockRenderer;

    WysiwygReportOfdRenderer(ImageRenderer imageRenderer, StructuredBlockRenderer structuredBlockRenderer) {
        this.imageRenderer = imageRenderer;
        this.structuredBlockRenderer = structuredBlockRenderer;
    }

    void render(OFDDoc document,
                JsonNode snapshot,
                DiagnosticReportRepository.PathologyReport report,
                String signedByName,
                String signedAt,
                Font font) throws IOException {
        if ("routine-pathology-report-v2".equals(snapshot.path("layoutCode").asText())) {
            renderRoutinePathologyV2(document, snapshot, report, signedByName, signedAt, font);
            return;
        }
        renderHeader(document, snapshot, report, font);
        renderFields(document, snapshot.path("metaFields"), font);

        JsonNode sections = snapshot.path("sections");
        if (sections.isArray()) {
            for (JsonNode section : sections) {
                renderSection(document, section, report.caseId(), font, text(snapshot, "accentColor", "#ff0000"));
            }
        }
        structuredBlockRenderer.render(document, snapshot.path("structuredBlocks"), font);
        renderFooter(document, snapshot, signedByName, signedAt, font);
    }

    private void renderRoutinePathologyV2(OFDDoc document,
                                          JsonNode snapshot,
                                          DiagnosticReportRepository.PathologyReport report,
                                          String signedByName,
                                          String signedAt,
                                          Font font) throws IOException {
        renderRoutineHeader(document, snapshot, report, font);
        renderRoutineFields(document, snapshot.path("metaFields"), font);
        JsonNode sections = snapshot.path("sections");
        if (sections.isArray()) {
            for (JsonNode section : sections) {
                renderRoutineSection(document, section, report.caseId(), font,
                    text(snapshot, "accentColor", "#ff0000"));
            }
        }
        renderRoutineFooter(document, snapshot, signedByName, signedAt, font);
    }

    private void renderRoutineHeader(OFDDoc document,
                                     JsonNode snapshot,
                                     DiagnosticReportRepository.PathologyReport report,
                                     Font font) {
        String hospitalName = text(snapshot, "hospitalName", "");
        String reportTitle = text(snapshot, "reportTitle", "病理检查报告单");
        String phone = fieldValue(snapshot.path("metaFields"), "联系电话");
        String reportNo = displayValue(report.reportNo());
        int[] accentColor = parseColor(text(snapshot, "accentColor", "#ff0000"), new int[]{255, 0, 0});
        Canvas canvas = new Canvas(CANVAS_WIDTH, HEADER_HEIGHT, context -> {
            fillBackground(context, CANVAS_WIDTH, HEADER_HEIGHT);
            context.setFont(new FontSetting(6d, font).setBold());
            context.setTextAlign(TextAlign.center);
            context.setFillColor(accentColor);
            context.fillText(hospitalName, CANVAS_WIDTH / 2d, 8d);
            context.setFont(new FontSetting(5d, font).setBold());
            context.setTextAlign(TextAlign.center);
            context.setFillColor(TEXT_COLOR);
            context.fillText(reportTitle, CANVAS_WIDTH / 2d, 14.5d);
            context.setTextAlign(TextAlign.left);
            context.setFont(new FontSetting(2.8d, font));
            if (!"-".equals(phone)) {
                context.fillText("手机：" + truncateToUnits(phone, 30), 3d, 20.2d);
            }
            context.setTextAlign(TextAlign.right);
            context.fillText(truncateToUnits(reportNo, 30), CANVAS_WIDTH - 3d, 20.2d);
            drawOuterFrame(context, HEADER_HEIGHT, true);
        });
        canvas.setIntegrity(true);
        document.add(canvas);
    }

    private void renderRoutineFields(OFDDoc document, JsonNode fields, Font font) {
        if (!fields.isArray()) {
            return;
        }
        List<JsonNode> visible = new ArrayList<>();
        fields.forEach(field -> {
            if (field.isObject() && !displayValue(field.path("value").asText()).equals("-")) {
                visible.add(field);
            }
        });
        if (visible.isEmpty()) {
            return;
        }
        int maxRow = visible.stream().mapToInt(this::routineRow).max().orElse(1);
        int firstRow = visible.stream().mapToInt(this::routineRow).filter(row -> row > 0).min().orElse(1);
        boolean hasWard = visible.stream().anyMatch(field -> normalizeLabel(field.path("label").asText()).contains("护理单元"));
        for (int row = 1; row <= maxRow; row++) {
            int currentRow = row;
            List<RoutineFieldCell> cells = new ArrayList<>();
            for (JsonNode field : visible) {
                if (routineRow(field) != currentRow) {
                    continue;
                }
                String label = field.path("label").asText();
                int[] placement = routinePlacement(label, currentRow, hasWard);
                cells.add(new RoutineFieldCell(
                    label,
                    safeDisplayFieldValue(label, field.path("value").asText()),
                    placement[0],
                    placement[1]));
            }
            if (!cells.isEmpty()) {
                addRoutineFieldRow(document, cells, 6, font,
                    row == firstRow,
                    row == maxRow);
            }
        }
    }

    private int[] routinePlacement(String label, int row, boolean hasWard) {
        String normalized = normalizeLabel(label);
        if (row == 1) {
            if (normalized.contains("姓名")) return new int[]{1, 1};
            if (normalized.contains("性别")) return new int[]{2, 1};
            if (normalized.contains("年龄")) return new int[]{3, 1};
            if (normalized.contains("病人ID")) return new int[]{4, 1};
            if (normalized.contains("床号")) return new int[]{5, 1};
            if (normalized.contains("住院号")) return new int[]{6, 1};
        }
        if (row == 2) {
            if (normalized.contains("申请科室")) return new int[]{1, 2};
            if (normalized.contains("送检医师")) return new int[]{3, 2};
            if (normalized.contains("检查日期")) return new int[]{5, 2};
        }
        if (row == 3) {
            if (normalized.contains("临床诊断")) return new int[]{1, hasWard ? 4 : 6};
            if (normalized.contains("护理单元")) return new int[]{5, 2};
        }
        return new int[]{1, 1};
    }

    private void addRoutineFieldRow(OFDDoc document, List<RoutineFieldCell> cells, int columns, Font font,
                                    boolean includeTop, boolean includeBottom) {
        double rowHeight = 7d;
        double columnWidth = CANVAS_WIDTH / columns;
        List<RoutinePreparedCell> prepared = new ArrayList<>(cells.size());
        for (RoutineFieldCell cell : cells) {
            double left = (cell.columnStart() - 1) * columnWidth;
            double width = columnWidth * cell.span();
            double labelWidth = Math.min(width * 0.44d, Math.max(10d, textWidth(cell.label()) + 1.5d));
            List<String> valueLines = wrapLines(displayValue(cell.value()), lineUnits(width - labelWidth - 2d));
            rowHeight = Math.max(rowHeight, GRID_PADDING * 2d + valueLines.size() * TEXT_LINE_HEIGHT);
            prepared.add(new RoutinePreparedCell(left, width, labelWidth, cell.label(), valueLines));
        }
        double canvasHeight = rowHeight;
        Canvas canvas = new Canvas(CANVAS_WIDTH, canvasHeight, context -> {
            fillBackground(context, CANVAS_WIDTH, canvasHeight);
            context.setTextAlign(TextAlign.left);
            for (RoutinePreparedCell cell : prepared) {
                context.setFillColor(TEXT_COLOR);
                context.setFont(new FontSetting(TEXT_FONT_SIZE, font).setBold());
                context.fillText(cell.label(), cell.left() + 1.5d, 4.6d);
                context.setFont(new FontSetting(TEXT_FONT_SIZE, font));
                double baseline = 4.6d;
                for (String line : cell.valueLines()) {
                    context.fillText(truncateToUnits(line, lineUnits(cell.width() - cell.labelWidth() - 2d)),
                        cell.left() + cell.labelWidth(), baseline);
                    baseline += TEXT_LINE_HEIGHT;
                }
            }
            context.beginPath();
            context.setStrokeColor(TEXT_COLOR).setLineWidth(0.18d);
            context.moveTo(0.15d, 0d).lineTo(0.15d, canvasHeight - 0.15d);
            context.moveTo(CANVAS_WIDTH - 0.15d, 0d)
                .lineTo(CANVAS_WIDTH - 0.15d, canvasHeight - 0.15d);
            if (includeTop) {
                context.moveTo(0.15d, 0.15d).lineTo(CANVAS_WIDTH - 0.15d, 0.15d);
            }
            if (includeBottom) {
                context.moveTo(0.15d, canvasHeight - 0.15d).lineTo(CANVAS_WIDTH - 0.15d, canvasHeight - 0.15d);
            }
            context.stroke();
        });
        canvas.setIntegrity(true);
        document.add(canvas);
    }

    private void renderRoutineSection(OFDDoc document,
                                      JsonNode section,
                                      String caseId,
                                      Font font,
                                      String accentColor) throws IOException {
        List<String> titleLines = wrapLines(section.path("label").asText(),
            lineUnits(CANVAS_WIDTH - SECTION_CONTENT_INSET * 2d));
        List<String> contentLines = wrapLines(displayValue(section.path("value").asText()),
            lineUnits(CANVAS_WIDTH - SECTION_CONTENT_INSET * 2d - SECTION_PADDING * 2d));
        double minimumContentHeight = pixelsToMillimeters(Math.min(
            MAX_SECTION_MIN_HEIGHT_PX, Math.max(0d, section.path("minHeight").asDouble())));
        int[] titleColor = parseColor(accentColor, new int[]{255, 0, 0});
        int offset = 0;
        boolean firstBlock = true;
        while (offset < contentLines.size()) {
            List<String> blockTitleLines = firstBlock ? titleLines : List.of();
            double titleBandHeight = titleBandHeight(blockTitleLines);
            double maximumContentHeight = MAX_SECTION_BLOCK_HEIGHT - titleBandHeight - SECTION_BOTTOM_PADDING;
            int maximumContentLines = Math.max(1, (int) Math.floor(
                (maximumContentHeight - SECTION_PADDING * 2d) / TEXT_LINE_HEIGHT));
            int end = Math.min(contentLines.size(), offset + maximumContentLines);
            List<String> blockLines = List.copyOf(contentLines.subList(offset, end));
            double naturalContentHeight = SECTION_PADDING * 2d + blockLines.size() * TEXT_LINE_HEIGHT;
            double contentHeight = firstBlock && end == contentLines.size()
                ? Math.max(naturalContentHeight, minimumContentHeight)
                : naturalContentHeight;
            addRoutineSectionBlock(document, blockTitleLines, blockLines, contentHeight, font, titleColor);
            offset = end;
            firstBlock = false;
        }
        imageRenderer.render(document, section.path("images"), caseId);
    }

    private void addRoutineSectionBlock(OFDDoc document,
                                        List<String> titleLines,
                                        List<String> contentLines,
                                        double contentHeight,
                                        Font font,
                                        int[] titleColor) {
        double titleBandHeight = titleBandHeight(titleLines);
        double height = titleBandHeight + contentHeight + SECTION_BOTTOM_PADDING;
        Canvas canvas = new Canvas(CANVAS_WIDTH, height, context -> {
            fillBackground(context, CANVAS_WIDTH, height);
            if (!titleLines.isEmpty()) {
                double baseline = SECTION_PADDING + TEXT_FONT_SIZE;
                context.setFont(new FontSetting(TEXT_FONT_SIZE, font).setBold());
                context.setFillColor(titleColor);
                for (String line : titleLines) {
                    context.fillText(line, SECTION_CONTENT_INSET, baseline);
                    baseline += TEXT_LINE_HEIGHT;
                }
            }
            double baseline = titleBandHeight + SECTION_PADDING + TEXT_FONT_SIZE;
            context.setFont(new FontSetting(TEXT_FONT_SIZE, font));
            context.setFillColor(TEXT_COLOR);
            for (String line : contentLines) {
                context.fillText(line, SECTION_CONTENT_INSET + SECTION_PADDING, baseline);
                baseline += TEXT_LINE_HEIGHT;
            }
            drawOuterFrame(context, height, false);
        });
        canvas.setIntegrity(true);
        document.add(canvas);
    }

    private void renderRoutineFooter(OFDDoc document,
                                     JsonNode snapshot,
                                     String signedByName,
                                     String signedAt,
                                     Font font) {
        List<FieldCell> fields = List.of(
            new FieldCell("审核医师：", fieldValue(snapshot.path("footerFields"), "审核医师"), 1),
            new FieldCell("诊断医师：", displayValue(signedByName), 1),
            new FieldCell("取材医师：", fieldValue(snapshot.path("footerFields"), "取材医师"), 1));
        String reportDate = fieldValue(snapshot.path("footerFields"), "报告日期");
        String note = snapshot.path("note").asText();
        Canvas canvas = new Canvas(CANVAS_WIDTH, FOOTER_HEIGHT, context -> {
            fillBackground(context, CANVAS_WIDTH, FOOTER_HEIGHT);
            double columnWidth = CANVAS_WIDTH / 3d;
            context.setTextAlign(TextAlign.left);
            context.setFont(new FontSetting(TEXT_FONT_SIZE, font).setBold());
            for (int index = 0; index < fields.size(); index++) {
                FieldCell field = fields.get(index);
                double left = index * columnWidth + 4.5d;
                context.setFillColor(TEXT_COLOR);
                if (!"-".equals(field.value())) {
                    context.fillText(field.label(), left, 6d);
                    context.setFont(new FontSetting(TEXT_FONT_SIZE, font));
                    context.fillText(truncateToUnits(field.value(), lineUnits(columnWidth - 18d)), left + 14d, 6d);
                }
                context.setFont(new FontSetting(TEXT_FONT_SIZE, font).setBold());
            }
            context.setFont(new FontSetting(TEXT_FONT_SIZE, font).setBold());
            if (!"-".equals(reportDate)) {
                context.fillText("报告日期：", CANVAS_WIDTH - 52d, 13d);
                context.setFont(new FontSetting(TEXT_FONT_SIZE, font));
                context.fillText(truncateToUnits(reportDate, 30), CANVAS_WIDTH - 38d, 13d);
            }
            if (!note.isBlank()) {
                context.setFillColor(NOTE_COLOR);
                context.setFont(new FontSetting(2.8d, font));
                context.fillText(truncateToUnits(OfdTextLayout.inline(note), lineUnits(CANVAS_WIDTH - 9d)),
                    4.5d, 19d);
            }
            drawOuterFrame(context, FOOTER_HEIGHT, false);
        });
        canvas.setIntegrity(true);
        document.add(canvas);
    }

    private int routineRow(JsonNode field) {
        String className = field.path("class").asText();
        if (className.contains("routine-header")) {
            return 0;
        }
        if (className.contains("routine-row-2")) {
            return 2;
        }
        if (className.contains("routine-row-3")) {
            return 3;
        }
        return 1;
    }

    private void renderHeader(OFDDoc document,
                              JsonNode snapshot,
                              DiagnosticReportRepository.PathologyReport report,
                              Font font) {
        String hospitalName = text(snapshot, "hospitalName", "");
        String reportTitle = text(snapshot, "reportTitle", "病理检查报告单");
        String deliveredAt = displayValue(snapshot.path("deliveredAt").asText());
        String reportNo = displayValue(report.reportNo());
        int[] accentColor = parseColor(text(snapshot, "accentColor", "#ff0000"), new int[]{255, 0, 0});
        Canvas canvas = new Canvas(CANVAS_WIDTH, HEADER_HEIGHT, context -> {
            fillBackground(context, CANVAS_WIDTH, HEADER_HEIGHT);
            context.setFont(new FontSetting(6d, font).setBold());
            context.setTextAlign(TextAlign.center);
            context.setFillColor(accentColor);
            context.fillText(hospitalName, CANVAS_WIDTH / 2d, 8d);
            context.setFont(new FontSetting(5d, font).setBold());
            context.setTextAlign(TextAlign.center);
            context.setFillColor(TEXT_COLOR);
            context.fillText(reportTitle, CANVAS_WIDTH / 2d, 14.5d);

            double boxTop = 16.2d;
            double boxHeight = 5.2d;
            context.setFillColor(VALUE_BACKGROUND);
            context.fillRect(2.8d, boxTop, HEADER_SIDE_BOX_WIDTH, boxHeight);
            context.fillRect(CANVAS_WIDTH - HEADER_SIDE_BOX_WIDTH - 2.8d, boxTop,
                HEADER_SIDE_BOX_WIDTH, boxHeight);
            context.setFont(new FontSetting(2.8d, font));
            context.setFillColor(TEXT_COLOR);
            context.setTextAlign(TextAlign.left);
            context.fillText(truncateToUnits(deliveredAt, 30), 3.8d, boxTop + 3.6d);
            context.fillText(truncateToUnits(reportNo, 30),
                CANVAS_WIDTH - HEADER_SIDE_BOX_WIDTH - 1.8d, boxTop + 3.6d);
            drawOuterFrame(context, HEADER_HEIGHT, true);
        });
        canvas.setIntegrity(true);
        document.add(canvas);
    }

    private void renderFooter(OFDDoc document,
                              JsonNode snapshot,
                              String signedByName,
                              String signedAt,
                              Font font) {
        List<FieldCell> fields = List.of(
            new FieldCell("审核医师：", fieldValue(snapshot.path("footerFields"), "审核医师"), 1),
            new FieldCell("诊断医师：", displayValue(signedByName), 1),
            new FieldCell("报告日期：", displayValue(signedAt), 1)
        );
        String note = snapshot.path("note").asText();
        Canvas canvas = new Canvas(CANVAS_WIDTH, FOOTER_HEIGHT, context -> {
            fillBackground(context, CANVAS_WIDTH, FOOTER_HEIGHT);
            double columnWidth = CANVAS_WIDTH / fields.size();
            double fieldTop = 3d;
            double fieldHeight = 6d;
            FontSetting labelFont = new FontSetting(TEXT_FONT_SIZE, font).setBold();
            FontSetting valueFont = new FontSetting(TEXT_FONT_SIZE, font);
            for (int index = 0; index < fields.size(); index++) {
                FieldCell field = fields.get(index);
                double left = index * columnWidth + 4.5d;
                double labelWidth = Math.min(20d, textWidth(field.label()) + 1d);
                double valueLeft = left + labelWidth;
                double valueWidth = columnWidth - labelWidth - 7d;
                context.setFillColor(VALUE_BACKGROUND);
                context.fillRect(valueLeft, fieldTop, valueWidth, fieldHeight);
                context.setFillColor(TEXT_COLOR);
                context.setFont(labelFont);
                context.fillText(field.label(), left, fieldTop + 4d);
                context.setFont(valueFont);
                context.fillText(truncateToUnits(displayValue(field.value()), lineUnits(valueWidth - 1.5d)),
                    valueLeft + 0.8d, fieldTop + 4d);
            }
            context.setTextAlign(TextAlign.left);
            context.setFillColor(TEXT_COLOR);
            context.setFont(new FontSetting(2.6d, font));
            context.fillText("签发时间：" + displayValue(signedAt), CANVAS_WIDTH - 48d, 13.5d);
            if (!note.isBlank()) {
                context.setFillColor(NOTE_COLOR);
                context.setFont(new FontSetting(2.8d, font));
                context.fillText(truncateToUnits(OfdTextLayout.inline(note), lineUnits(CANVAS_WIDTH - 9d)),
                    4.5d, 18.5d);
            }
            drawOuterFrame(context, FOOTER_HEIGHT, false);
        });
        canvas.setIntegrity(true);
        document.add(canvas);
    }

    private void renderFields(OFDDoc document, JsonNode fields, Font font) {
        if (!fields.isArray()) {
            return;
        }
        List<FieldCell> row = new ArrayList<>();
        int occupiedColumns = 0;
        for (JsonNode field : fields) {
            String label = field.path("label").asText();
            if (isAuthoritativeMetadataField(label)) {
                continue;
            }
            int span = field.path("class").asText().contains("span-2") ? 2 : 1;
            if (occupiedColumns + span > METADATA_COLUMNS) {
                addFieldRow(document, row, METADATA_COLUMNS, font);
                row = new ArrayList<>();
                occupiedColumns = 0;
            }
            row.add(new FieldCell(label, safeDisplayFieldValue(label, field.path("value").asText()), span));
            occupiedColumns += span;
            if (occupiedColumns == METADATA_COLUMNS) {
                addFieldRow(document, row, METADATA_COLUMNS, font);
                row = new ArrayList<>();
                occupiedColumns = 0;
            }
        }
        if (!row.isEmpty()) {
            addFieldRow(document, row, METADATA_COLUMNS, font);
        }
    }

    private void addFieldRow(OFDDoc document, List<FieldCell> cells, int columns, Font font) {
        if (cells.isEmpty()) {
            return;
        }
        List<PreparedCell> preparedCells = new ArrayList<>(cells.size());
        double rowHeight = MIN_GRID_ROW_HEIGHT;
        for (FieldCell cell : cells) {
            double width = CANVAS_WIDTH * cell.span() / columns;
            double labelWidth = Math.min(width * 0.48d, Math.max(11d, textWidth(cell.label()) + 2.5d));
            double valueWidth = width - labelWidth - GRID_PADDING * 2d;
            List<String> valueLines = wrapLines(displayValue(cell.value()), lineUnits(valueWidth));
            double naturalHeight = GRID_PADDING * 2d
                + valueLines.size() * TEXT_LINE_HEIGHT;
            rowHeight = Math.max(rowHeight, naturalHeight);
            preparedCells.add(new PreparedCell(width, labelWidth, cell.label(), valueLines));
        }
        double canvasHeight = rowHeight;
        Canvas canvas = new Canvas(CANVAS_WIDTH, canvasHeight, context -> {
            fillBackground(context, CANVAS_WIDTH, canvasHeight);
            double left = 0d;
            FontSetting labelFont = new FontSetting(TEXT_FONT_SIZE, font).setBold();
            FontSetting valueFont = new FontSetting(TEXT_FONT_SIZE, font);
            for (PreparedCell cell : preparedCells) {
                double valueLeft = left + cell.labelWidth();
                context.setFillColor(VALUE_BACKGROUND);
                context.fillRect(valueLeft, 0.7d, cell.width() - cell.labelWidth() - GRID_PADDING,
                    canvasHeight - 1.4d);
                double baseline = GRID_PADDING + TEXT_FONT_SIZE;
                context.setFillColor(TEXT_COLOR);
                context.setFont(labelFont);
                context.fillText(cell.label(), left + GRID_PADDING, baseline);
                baseline = GRID_PADDING + TEXT_FONT_SIZE;
                context.setFont(valueFont);
                for (String line : cell.valueLines()) {
                    context.fillText(line, valueLeft + 0.8d, baseline);
                    baseline += TEXT_LINE_HEIGHT;
                }
                left += cell.width();
            }
            drawOuterFrame(context, canvasHeight, false);
        });
        canvas.setIntegrity(true);
        document.add(canvas);
    }

    private void renderSection(OFDDoc document,
                               JsonNode section,
                               String caseId,
                               Font font,
                               String accentColor) throws IOException {
        List<String> titleLines = wrapLines(section.path("label").asText(),
            lineUnits(CANVAS_WIDTH - SECTION_CONTENT_INSET * 2d));
        List<String> contentLines = wrapLines(displayValue(section.path("value").asText()),
            lineUnits(CANVAS_WIDTH - SECTION_CONTENT_INSET * 2d - SECTION_PADDING * 2d));
        double minimumContentHeight = pixelsToMillimeters(Math.min(
            MAX_SECTION_MIN_HEIGHT_PX,
            Math.max(0d, section.path("minHeight").asDouble())));
        int[] titleColor = parseColor(accentColor, new int[]{255, 0, 0});
        int offset = 0;
        boolean firstBlock = true;
        while (offset < contentLines.size()) {
            List<String> blockTitleLines = firstBlock ? titleLines : List.of();
            double titleBandHeight = titleBandHeight(blockTitleLines);
            double maximumContentHeight = MAX_SECTION_BLOCK_HEIGHT - titleBandHeight - SECTION_BOTTOM_PADDING;
            int maximumContentLines = Math.max(1, (int) Math.floor(
                (maximumContentHeight - SECTION_PADDING * 2d) / TEXT_LINE_HEIGHT));
            int end = Math.min(contentLines.size(), offset + maximumContentLines);
            List<String> blockLines = List.copyOf(contentLines.subList(offset, end));
            double naturalContentHeight = SECTION_PADDING * 2d + blockLines.size() * TEXT_LINE_HEIGHT;
            double contentHeight = firstBlock && end == contentLines.size()
                ? Math.max(naturalContentHeight, minimumContentHeight)
                : naturalContentHeight;
            addSectionBlock(document, blockTitleLines, blockLines, contentHeight, font, titleColor);
            offset = end;
            firstBlock = false;
        }
        imageRenderer.render(document, section.path("images"), caseId);
    }

    private void addSectionBlock(OFDDoc document,
                                 List<String> titleLines,
                                 List<String> contentLines,
                                 double contentHeight,
                                 Font font,
                                 int[] titleColor) {
        double titleBandHeight = titleBandHeight(titleLines);
        double height = titleBandHeight + contentHeight + SECTION_BOTTOM_PADDING;
        Canvas canvas = new Canvas(CANVAS_WIDTH, height, context -> {
            fillBackground(context, CANVAS_WIDTH, height);
            double contentTop = titleBandHeight;
            context.setFillColor(VALUE_BACKGROUND);
            context.fillRect(SECTION_CONTENT_INSET, contentTop,
                CANVAS_WIDTH - SECTION_CONTENT_INSET * 2d, contentHeight);
            if (!titleLines.isEmpty()) {
                double baseline = SECTION_PADDING + TEXT_FONT_SIZE;
                context.setFont(new FontSetting(TEXT_FONT_SIZE, font).setBold());
                context.setFillColor(titleColor);
                for (String line : titleLines) {
                    context.fillText(line, SECTION_CONTENT_INSET, baseline);
                    baseline += TEXT_LINE_HEIGHT;
                }
            }
            double baseline = contentTop + SECTION_PADDING + TEXT_FONT_SIZE;
            context.setFont(new FontSetting(TEXT_FONT_SIZE, font));
            context.setFillColor(TEXT_COLOR);
            for (String line : contentLines) {
                context.fillText(line, SECTION_CONTENT_INSET + SECTION_PADDING, baseline);
                baseline += TEXT_LINE_HEIGHT;
            }
            drawOuterFrame(context, height, false);
        });
        canvas.setIntegrity(true);
        document.add(canvas);
    }

    private double titleBandHeight(List<String> titleLines) {
        return titleLines.isEmpty() ? SECTION_PADDING : SECTION_PADDING * 2d + titleLines.size() * TEXT_LINE_HEIGHT;
    }

    private void fillBackground(DrawContext context, double width, double height) {
        context.setFillColor(WHITE);
        context.fillRect(0d, 0d, width, height);
    }

    private void drawOuterFrame(DrawContext context, double height, boolean includeTop) {
        context.beginPath();
        context.setStrokeColor(TEXT_COLOR).setLineWidth(0.18d);
        if (includeTop) {
            context.moveTo(0.15d, 0.15d).lineTo(CANVAS_WIDTH - 0.15d, 0.15d);
        }
        context.moveTo(0.15d, 0d).lineTo(0.15d, height - 0.15d);
        context.moveTo(CANVAS_WIDTH - 0.15d, 0d).lineTo(CANVAS_WIDTH - 0.15d, height - 0.15d);
        context.moveTo(0.15d, height - 0.15d).lineTo(CANVAS_WIDTH - 0.15d, height - 0.15d);
        context.stroke();
    }

    private List<String> wrapLines(String value, int maximumUnits) {
        List<String> lines = new ArrayList<>();
        String normalized = value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
        for (String sourceLine : normalized.split("\n", -1)) {
            if (sourceLine.isEmpty()) {
                lines.add(" ");
                continue;
            }
            StringBuilder line = new StringBuilder();
            int units = 0;
            for (int index = 0; index < sourceLine.length(); index++) {
                char character = sourceLine.charAt(index);
                int characterUnits = character <= 0x7f ? 1 : 2;
                if (units + characterUnits > maximumUnits && !line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                    units = 0;
                }
                line.append(character);
                units += characterUnits;
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
        }
        return lines.isEmpty() ? List.of(" ") : List.copyOf(lines);
    }

    private String truncateToUnits(String value, int maximumUnits) {
        String normalized = OfdTextLayout.inline(value);
        if (visualUnits(normalized) <= maximumUnits) {
            return normalized;
        }
        StringBuilder result = new StringBuilder();
        int units = 0;
        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);
            int characterUnits = character <= 0x7f ? 1 : 2;
            if (units + characterUnits + 3 > maximumUnits) {
                break;
            }
            result.append(character);
            units += characterUnits;
        }
        return result.append("...").toString();
    }

    private double textWidth(String value) {
        return visualUnits(value) * TEXT_FONT_SIZE / 2d;
    }

    private int visualUnits(String value) {
        int units = 0;
        for (int index = 0; index < value.length(); index++) {
            units += value.charAt(index) <= 0x7f ? 1 : 2;
        }
        return units;
    }

    private int lineUnits(double width) {
        return Math.max(1, (int) Math.floor(width / (TEXT_FONT_SIZE / 2d)));
    }

    private int[] parseColor(String value, int[] fallback) {
        if (value == null || !value.matches("#[0-9a-fA-F]{6}")) {
            return fallback;
        }
        return new int[]{
            Integer.parseInt(value.substring(1, 3), 16),
            Integer.parseInt(value.substring(3, 5), 16),
            Integer.parseInt(value.substring(5, 7), 16)
        };
    }

    private String fieldValue(JsonNode fields, String expectedLabel) {
        if (fields.isArray()) {
            for (JsonNode field : fields) {
                if (normalizeLabel(field.path("label").asText()).contains(expectedLabel)) {
                    return displayValue(field.path("value").asText());
                }
            }
        }
        return "-";
    }

    private boolean isAuthoritativeMetadataField(String label) {
        String normalized = normalizeLabel(label);
        return normalized.contains("报告号") || normalized.contains("病理号");
    }

    private String safeDisplayFieldValue(String label, String value) {
        if (normalizeLabel(label).contains("病人ID") && looksLikeUuid(value)) {
            return "-";
        }
        return displayValue(value);
    }

    private boolean looksLikeUuid(String value) {
        return value != null && value.trim()
            .matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }

    private String displayValue(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String normalizeLabel(String label) {
        return label == null ? "" : label.replace(" ", "").replace(":", "").replace("：", "");
    }

    private double pixelsToMillimeters(double pixels) {
        return pixels * 25.4d / 96d;
    }

    private String text(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText();
        return value.isBlank() ? fallback : value;
    }

    private record FieldCell(String label, String value, int span) {
    }

    private record RoutineFieldCell(String label, String value, int columnStart, int span) {
    }

    private record RoutinePreparedCell(double left,
                                       double width,
                                       double labelWidth,
                                       String label,
                                       List<String> valueLines) {
    }

    private record PreparedCell(double width,
                                double labelWidth,
                                String label,
                                List<String> valueLines) {
    }

    @FunctionalInterface
    interface ImageRenderer {
        void render(OFDDoc document, JsonNode images, String caseId) throws IOException;
    }

    @FunctionalInterface
    interface StructuredBlockRenderer {
        void render(OFDDoc document, JsonNode blocks, Font font) throws IOException;
    }
}
