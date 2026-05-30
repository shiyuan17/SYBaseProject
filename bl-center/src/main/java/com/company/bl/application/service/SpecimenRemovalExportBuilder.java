package com.company.bl.application.service;

import com.company.bl.domain.repository.SpecimenWorkflowRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class SpecimenRemovalExportBuilder {

    private static final DateTimeFormatter EXPORT_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    byte[] buildSpecimenRemovalExport(List<SpecimenWorkflowRepository.SpecimenRemovalListRow> rows) {
        List<List<String>> sheetRows = new ArrayList<>();
        sheetRows.add(List.of(
            "标本ID",
            "申请单",
            "标本编号",
            "姓名",
            "住院号",
            "性别",
            "手术间",
            "标本名称",
            "标本状态",
            "类型",
            "离体时间",
            "离体操作人",
            "添加时间",
            "添加人"
        ));
        for (SpecimenWorkflowRepository.SpecimenRemovalListRow row : rows) {
            sheetRows.add(List.of(
                defaultString(row.barcode()),
                defaultString(row.applicationNo()),
                defaultString(row.specimenNo()),
                defaultString(row.patientName()),
                defaultString(row.inpatientNo()),
                defaultString(row.patientGender()),
                defaultString(row.surgeryName()),
                defaultString(row.specimenName()),
                defaultString(row.specimenStatus()),
                defaultString(row.specimenType()),
                formatExportDateTime(row.specimenRemovalAt()),
                defaultString(row.specimenRemovalOperatorName()),
                formatExportDateTime(row.registeredAt()),
                defaultString(row.registeredByName())
            ));
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            writeZipEntry(zipOutputStream, "[Content_Types].xml", contentTypesXml());
            writeZipEntry(zipOutputStream, "_rels/.rels", rootRelsXml());
            writeZipEntry(zipOutputStream, "xl/workbook.xml", workbookXml());
            writeZipEntry(zipOutputStream, "xl/_rels/workbook.xml.rels", workbookRelsXml());
            writeZipEntry(zipOutputStream, "xl/worksheets/sheet1.xml", worksheetXml(sheetRows));
            zipOutputStream.finish();
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to build specimen removal export", exception);
        }
    }

    private void writeZipEntry(ZipOutputStream zipOutputStream, String entryName, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }

    private String contentTypesXml() {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
              <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            </Types>
            """;
    }

    private String rootRelsXml() {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
            """;
    }

    private String workbookXml() {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <sheets>
                <sheet name="离体时间设置" sheetId="1" r:id="rId1"/>
              </sheets>
            </workbook>
            """;
    }

    private String workbookRelsXml() {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
            </Relationships>
            """;
    }

    private String worksheetXml(List<List<String>> rows) {
        StringBuilder builder = new StringBuilder("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>
            """);
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            builder.append("<row r=\"").append(rowIndex + 1).append("\">");
            List<String> cells = rows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < cells.size(); columnIndex++) {
                builder.append("<c r=\"")
                    .append(excelColumnName(columnIndex))
                    .append(rowIndex + 1)
                    .append("\" t=\"inlineStr\"><is><t>")
                    .append(escapeXml(cells.get(columnIndex)))
                    .append("</t></is></c>");
            }
            builder.append("</row>");
        }
        builder.append("""
              </sheetData>
            </worksheet>
            """);
        return builder.toString();
    }

    private String excelColumnName(int columnIndex) {
        StringBuilder builder = new StringBuilder();
        int current = columnIndex;
        do {
            builder.insert(0, (char) ('A' + (current % 26)));
            current = current / 26 - 1;
        } while (current >= 0);
        return builder.toString();
    }

    private String escapeXml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    private String formatExportDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(EXPORT_TIME_FORMATTER);
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
